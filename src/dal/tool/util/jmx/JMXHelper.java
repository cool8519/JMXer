package dal.tool.util.jmx;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.sun.tools.attach.spi.AttachProvider;

import dal.tool.cli.Logger;
import dal.tool.cli.Logger.Level;
import dal.tool.util.StringUtil;
import dal.tool.util.TypeUtil;
import dal.tool.util.jmx.MBeanConnector.ConnectType;


/**
 * JMX 접속/호출/결과출력을 위한 Helper 클래스
 * @author 권영달
 *
 */
public class JMXHelper {

	
	/**
	 * JVM에 접속한다.<br>
	 * JDK 1.5 이하에서는 JVM Attach를 지원하지 않으므로, Address를 통한 RMI 방식의 접속만 허용된다.
	 * @param connect_type 연결 방식
	 * @param connect_value 연결 대상 문자열. connect_type에 따라 의미가 다르다.
	 * @param authenticate 인증 문자열 배열(user/password)
	 * @return 접속한 JVM에 대한 JMXConnector 객체
	 * @throws Exception 수행 과정에서 발생한 모든 Exception
	 */
	public static JMXConnector connectToVM(ConnectType connect_type, String connect_value, String[] authenticate) throws Exception {
		if(connect_type == ConnectType.ATTACH_BY_PID || connect_type == ConnectType.ATTACH_BY_NAME) {
			try {
				addAttachToolsToClasspath();				
			} catch(Exception e) {
				Logger.logln(Level.ERROR, "Failed to load classes for attach : " + e.getMessage());
				throw e;
			}			
		}		
    	JMXServiceURL serviceURL = null;
    	try {
    		Float vm_version = Float.parseFloat(System.getProperty("java.specification.version"));
    		if(vm_version < 1.6F) {
    			serviceURL = getJMXServiceURLForOldVM(connect_type, connect_value);
    		} else {
    			serviceURL = getJMXServiceURL(connect_type, connect_value);
    		}
    	} catch(Throwable t) {
    		if((t instanceof NoClassDefFoundError) && ("com/sun/tools/attach/spi/AttachProvider".equals(t.getMessage()))) {
    			Logger.logln(Level.ERROR, "Cannot find JDK attach classes. Run JMXer on a full JDK runtime (Java 8: tools.jar, Java 9+: jdk.attach module).");
    		} else {
    			Logger.logln(Level.ERROR, "Failed to get serviceURL : " + getAttachFailureReason(t));
    		}
    		throw new Exception("Failed to get JMX service URL: " + getAttachFailureReason(t));
    	}
    	HashMap<String, Object> env = new HashMap<String, Object>();
    	env.put("jmx.remote.x.request.waiting.timeout",
    			String.valueOf(dal.tool.trace.jmxer.JMXerConstant.JMX_REMOTE_REQUEST_TIMEOUT_MS));
    	if(authenticate != null) {
			String[] credentials = new String[] {authenticate[0], authenticate[1]};
			env.put(JMXConnector.CREDENTIALS, credentials);
    	}
    	JMXConnector connector = JMXConnectorFactory.connect(serviceURL, env);
        return connector;        
	}
	

	/**
	 * Local JVM에 Attach 하기 위한 Service URL 문자열을 가져온다.
	 * @param virtualMachine 접속할 JVM에 대한 VirtualMachine 객체
	 * @return Service URL 문자열
	 * @throws Exception 수행 과정에서 발생한 모든 Exception
	 */
	private static String getLocalConnectorAddress(VirtualMachine virtualMachine) throws Exception {
		Float tool_version = Float.parseFloat(System.getProperty("java.specification.version"));
		Properties vmProperties = virtualMachine.getSystemProperties();
		Float target_version = Float.parseFloat(vmProperties.getProperty("java.specification.version"));
        String connectorAddr = virtualMachine.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress");
        if (connectorAddr == null) {
        	if(target_version <= 1.8F) {
	            String agent = vmProperties.getProperty("java.home")+File.separator+"lib"+File.separator+"management-agent.jar";
	            virtualMachine.loadAgent(agent);
	            connectorAddr = virtualMachine.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress");
        	} else {
        		if(tool_version > 1.8F) {
	        		Method m = VirtualMachine.class.getMethod("startLocalManagementAgent");
	        		connectorAddr = (String)m.invoke(virtualMachine);
        		} else {
        			throw new Exception("The vm version of the tool is too old. Please use vm version 1.9 or higher.");
        		}
        	}
        }
        return connectorAddr;
	}

	private static String getAttachFailureReason(Throwable t) {
		Throwable root = t;
		while(root.getCause() != null && root.getCause() != root) {
			root = root.getCause();
		}
		String message = root.getMessage();
		if(message == null || message.trim().equals("")) {
			message = root.toString();
		}
		if(message.contains("No AttachProvider exists")) {
			return "No AttachProvider exists. Attach mode requires a full JDK runtime.";
		}
		if(message.contains("Not support attach mode in JRE")) {
			return "Attach mode is not supported in JRE. Please run JMXer with JDK.";
		}
		if(message.contains("Failed to attach")) {
			return message + " (Check PID validity and ensure same OS user permission.)";
		}
		if(root instanceof IllegalAccessException || message.contains("does not export sun.tools.attach")) {
			return "Attach access is blocked by Java module boundaries. Internal sun.tools.attach access is not allowed.";
		}
		return message;
	}
	

	/**
	 * JVM에 접속하기 위한 JMXServiceURL 객체를 가져온다.
	 * @param connect_type 연결 방식
	 * @param connect_value 연결 대상 문자열. connect_type에 따라 의미가 다르다.
	 * @return 접속할 JVM의 JMXServiceURL 객체
	 * @throws Exception 수행 과정에서 발생한 모든 Exception
	 */
	private static JMXServiceURL getJMXServiceURL(ConnectType connect_type, String connect_value) throws Exception {
		JMXServiceURL serviceURL = null;
    	if(connect_type.equals(ConnectType.ATTACH_BY_PID)) {
    		List<AttachProvider> availableProviders = AttachProvider.providers();
    		if(availableProviders == null || availableProviders.size() < 1) {
            	throw new Exception("No AttachProvider exists : Attach is not available in JRE.");
    		}
            AttachProvider attachProvider = availableProviders.get(0);
            VirtualMachine virtualMachine = null;
            String connectorAddress = null;
            try {
            	virtualMachine = attachProvider.attachVirtualMachine(connect_value);            	
            	connectorAddress = getLocalConnectorAddress(virtualMachine);
            } catch(Exception e) {
            	if(virtualMachine == null) throw e;
            	Float target_version = Float.parseFloat(getVmVersion(virtualMachine));
            	if(target_version <= 1.8F) {
                    try {
                    	virtualMachine = attachProvider.attachVirtualMachine(connect_value);            	
                    	connectorAddress = getLocalConnectorAddress(virtualMachine);
                    } catch(Exception e2) {
                    	throw new Exception("Failed to attach '" + connect_value + "' : " + e.toString());
                    }
            	} else {            	
            		throw new Exception("Failed to attach '" + connect_value + "' : " + e.toString());
            	}
            }
            serviceURL = new JMXServiceURL(connectorAddress);
    	} else if(connect_type.equals(ConnectType.ATTACH_BY_NAME)) {
    		List<AttachProvider> availableProviders = AttachProvider.providers();
    		if(availableProviders == null || availableProviders.size() < 1) {
            	throw new Exception("No AttachProvider exists : Attach is not available in JRE.");
    		}
    		AttachProvider attachProvider = availableProviders.get(0);
    		VirtualMachineDescriptor descriptor = null;
            for(VirtualMachineDescriptor virtualMachineDescriptor : attachProvider.listVirtualMachines()) {
            	String dispName = virtualMachineDescriptor.displayName();
                if(dispName != null && dispName.split(" ")[0].equals(connect_value)) {
                    descriptor = virtualMachineDescriptor;
                    Logger.logln("Found the process named '" + connect_value + "' : PID=" + descriptor.id());
                    break;
                }
            }
            if(descriptor == null) {
                throw new Exception("Could not find the process named '" + connect_value + "'");
            }
            VirtualMachine virtualMachine = null;
            String connectorAddress = null;
            try {
            	virtualMachine = attachProvider.attachVirtualMachine(descriptor);            	
            	connectorAddress = getLocalConnectorAddress(virtualMachine);
            } catch(Exception e) {
            	if(virtualMachine == null) throw e;
            	Float target_version = Float.parseFloat(getVmVersion(virtualMachine));
            	if(target_version <= 1.8F) {
                    try {
                    	virtualMachine = attachProvider.attachVirtualMachine(descriptor);            	
                    	connectorAddress = getLocalConnectorAddress(virtualMachine);
                    } catch(Exception e2) {
                    	throw new Exception("Failed to attach '" + connect_value + "' : " + e.toString());
                    }
            	} else {            	
            		throw new Exception("Failed to attach '" + connect_value + "' : " + e.toString());
            	}
            }
            serviceURL = new JMXServiceURL(connectorAddress);
    	} else if(connect_type.equals(ConnectType.RMI_BY_ADDRESS)) {
        	String remoteConnectorAddress = "service:jmx:rmi:///jndi/rmi://"+connect_value+"/jmxrmi";
        	serviceURL = new JMXServiceURL(remoteConnectorAddress);
    	} else {
    		throw new Exception("Unknown type to find VM : " + connect_type);
    	}
    	return serviceURL;
	}
	
	
	/**
	 * 1.5 이하 버전의 JVM에 접속하기 위한 JMXServiceURL 객체를 가져온다.
	 * @param connect_type 연결 방식
	 * @param connect_value 연결 대상 문자열. connect_type에 따라 의미가 다르다.
	 * @return 접속할 JVM의 JMXServiceURL 객체
	 * @throws Exception 수행 과정에서 발생한 모든 Exception
	 */
	private static JMXServiceURL getJMXServiceURLForOldVM(ConnectType connect_type, String connect_value) throws Exception {
		JMXServiceURL serviceURL = null;
    	if(connect_type.equals(ConnectType.ATTACH_BY_PID) || connect_type.equals(ConnectType.ATTACH_BY_NAME)) {
    		throw new Exception("To attach to JVM by PID and NAME is only available on JVM version of 1.6 or higher");
    	} else if(connect_type.equals(ConnectType.RMI_BY_ADDRESS)) {
        	String remoteConnectorAddress = "service:jmx:rmi:///jndi/rmi://"+connect_value+"/jmxrmi";
        	serviceURL = new JMXServiceURL(remoteConnectorAddress);
    	} else {
    		throw new Exception("Unknown type to find VM : " + connect_type);
    	}
    	return serviceURL;
	}


	/**
	 * MBean Operation 호출을 위해 입력받은 문자열을 파라미터에 맞는 Class의 객체로 변환한다. 
	 * @param paramInfos 파라미터 정보를 나타내는 MBeanParameterInfo 목록
	 * @param argList Operation 수행에 필요한 argument 값 배열
	 * @return 변환된 Argument 객체 배열
	 * @throws Exception 수행 과정에서 발생한 모든 Exception
	 */
	public static Object[] createParams(MBeanParameterInfo[] paramInfos, String[] argList) throws Exception {
		Object[] params = new Object[paramInfos.length];
	    for(int i = 0; i < params.length; i++) {
	    	MBeanParameterInfo paramInfo = paramInfos[i];
	    	String value = argList[i];
			OpenType<?> openType = (OpenType<?>)paramInfo.getDescriptor().getFieldValue("openType");
			if(openType != null && openType.isArray()) {
				params[i] = getArrayFromString(openType, value);
			} else {
				String clsName = (openType==null) ? paramInfo.getType() : openType.getClassName();
				params[i] = createInstance(clsName, value);
			}
	    }
	    return params;
	}
	
	
	/**
	 * 입력받은 문자열을 파라미터에 맞는 Class의 객체 배열로 변환한다. 
	 * @param openType Class의 Type을 나타내는 Descriptor 문자열. "["로 시작되어야 한다.
	 * @param value 배열을 나타내는 문자열(구분자:",") 
	 * @return 변환된 배열 Object 객체
	 * @throws Exception 수행 과정에서 발생한 모든 Exception
	 */
	public static Object getArrayFromString(OpenType<?> openType, String value) throws Exception {
		String[] strArrValue = StringUtil.getTokenArray(value, ',', new String[]{"[]"});
		String clsName = openType.getClassName();
		String elClsName = ((ArrayType<?>)openType).getElementOpenType().getClassName();
		Object objArrValue;
		if(clsName.equals("[B")) {
			byte[] arr = new byte[strArrValue.length];
			for(int i = 0; i < strArrValue.length; i++) {
				arr[i] = (Byte)createInstance(elClsName, strArrValue[i]);
			}
			objArrValue = arr;
		} else if(clsName.equals("[C")) {
			char[] arr = new char[strArrValue.length];
			for(int i = 0; i < strArrValue.length; i++) {
				arr[i] = (Character)createInstance(elClsName, strArrValue[i]);
			}
			objArrValue = arr;
		} else if(clsName.equals("[D")) {
			double[] arr = new double[strArrValue.length];
			for(int i = 0; i < strArrValue.length; i++) {
				arr[i] = (Double)createInstance(elClsName, strArrValue[i]);
			}
			objArrValue = arr;
		} else if(clsName.equals("[F")) {
			float[] arr = new float[strArrValue.length];
			for(int i = 0; i < strArrValue.length; i++) {
				arr[i] = (Float)createInstance(elClsName, strArrValue[i]);
			}
			objArrValue = arr;
		} else if(clsName.equals("[I")) {
			int[] arr = new int[strArrValue.length];
			for(int i = 0; i < strArrValue.length; i++) {
				arr[i] = (Integer)createInstance(elClsName, strArrValue[i]);
			}
			objArrValue = arr;
		} else if(clsName.equals("[J")) {
			long[] arr = new long[strArrValue.length];			
			for(int i = 0; i < strArrValue.length; i++) {
				arr[i] = (Long)createInstance(elClsName, strArrValue[i]);
			}
			objArrValue = arr;
		} else if(clsName.equals("[S")) {
			short[] arr = new short[strArrValue.length];
			for(int i = 0; i < strArrValue.length; i++) {
				arr[i] = (Short)createInstance(elClsName, strArrValue[i]);
			}
			objArrValue = arr;
		} else if(clsName.equals("[Z")) {
			boolean[] arr = new boolean[strArrValue.length];
			for(int i = 0; i < strArrValue.length; i++) {
				arr[i] = (Boolean)createInstance(elClsName, strArrValue[i]);
			}
			objArrValue = arr;
		} else if(clsName.startsWith("[L")) {
			Object[] arr = new Object[strArrValue.length];
			for(int i = 0; i < strArrValue.length; i++) {
				arr[i] = createInstance(elClsName, strArrValue[i]);
			}
			objArrValue = arr;
		} else {
			throw new Exception("Unsupported array value");			
		}
		return objArrValue;
	}

	
	/**
	 * 불리언 텍스트를 파싱한다. true와 false만 허용한다(대소문자 무시).
	 * @param value 입력 문자열
	 * @return 파싱된 값
	 * @throws Exception true 또는 false가 아닌 경우
	 */
	public static boolean parseStrictBoolean(String value) throws Exception {
		if(value == null) {
			throw new Exception("Boolean value cannot be null.");
		}
		String trimmed = value.trim();
		if(trimmed.length() == 0) {
			throw new Exception("Boolean value cannot be empty. Use true or false.");
		}
		if("true".equalsIgnoreCase(trimmed)) {
			return true;
		}
		if("false".equalsIgnoreCase(trimmed)) {
			return false;
		}
		throw new Exception("Invalid boolean value \"" + trimmed + "\". Use true or false.");
	}

	/** 입력받은 문자열을 Class에 맞는 Type의 객체로 변환한다. 
	 * @param clsName 클래스명(구분자:".")
	 * @param value 변환할 문자열
	 * @return 변환된  Class의 객체
	 * @throws Exception 수행 과정에서 발생한 모든 Exception
	 */
	public static Object createInstance(String clsName, String value) throws Exception {
		if(value == null) {
			return null;
		}
		if("java.lang.String".equals(clsName)) {
			return value;
		}
		if(isBooleanClassName(clsName)) {
			Boolean ret = Boolean.valueOf(parseStrictBoolean(value));
			Logger.logln(Logger.Level.DEBUG, "String(" + value + ") is transformed to " + ret.getClass().getName());
			return ret;
		}
		Class<?> cl = TypeUtil.getWrapperClass(clsName);
		Constructor<?> co = cl.getConstructor(new Class[] {String.class});
		Object ret = co.newInstance(value.trim());
		Logger.logln(Logger.Level.DEBUG, "String(" + value + ") is transformed to " + ret.getClass().getName());
		return ret;
	}

	private static boolean isBooleanClassName(String clsName) {
		return "boolean".equals(clsName) || "java.lang.Boolean".equals(clsName);
	}
	
	
	/**
	 * MBean Attribute의 접근권한을 나타내는 문자열을 가져온다.
	 * @param mbeanAttrInfo Attribute에 대한 MBeanAttributeInfo 객체
	 * @return 접근권한을 나타내는 문자열.<br/>
	 *         - 읽기 전용 : "R "<br/>
	 *         - 읽기/쓰기 : "RW"<br/>
	 *         - 권한없음 : "  "
	 */
	public static String getPermissionStringSimple(MBeanAttributeInfo mbeanAttrInfo) {
		String perm = "";
		perm += mbeanAttrInfo.isReadable() ? "R" : " ";
		perm += mbeanAttrInfo.isWritable() ? "W" : " ";
		return perm;
	}

	
	/**
	 * MBean Attribute의 접근권한을 나타내는 문자열을 가져온다.
	 * @param mbeanAttrInfo Attribute에 대한 MBeanAttributeInfo 객체
	 * @return 접근권한을 나타내는 문자열.<br/>
	 *         - 읽기 전용 : "Read Only"<br/>
	 *         - 읽기/쓰기 : "Read / Write"<br/>
	 *         - 권한없음 : "No Permission"
	 */
	public static String getPermissionString(MBeanAttributeInfo mbeanAttrInfo) {
		if(mbeanAttrInfo.isReadable()) {
			if(mbeanAttrInfo.isWritable()) {
				return "Read / Write";
			} else {
				return "Read Only";
			}
		} else {
			if(mbeanAttrInfo.isWritable()) {
				return "Write Only";
			} else {
				return "No Permission";
			}
		}
	}

	
	/**
	 * Argument의 타입을 표현하는 String을 리턴한다.
	 * @param paramInfos parameter 정보 배열
	 * @return 타입을 나타내는 문자열 (형식:"arg1_type,arg2_type,arg3_type,...")
	 */
	public static String toTypeString(MBeanParameterInfo[] paramInfos) {
	    String[] opSig = new String[paramInfos.length];
	    for(int i = 0; i < opSig.length; i++){
	    	opSig[i] = paramInfos[i].getType();
	    }
	    return toTypeString(opSig);		
	}

	/**
	 * CALL 명령 대상명을 연산 시그니처 타입과 함께 만든다.
	 * 예) getThreadInfo(long[],boolean,int)
	 * @param operationInfo Operation 메타데이터
	 * @return CALL ObjectName TargetName ... 형식의 대상명
	 */
	public static String toOperationCallTargetName(MBeanOperationInfo operationInfo) {
		if(operationInfo == null) {
			return "";
		}
		return toOperationCallTargetName(operationInfo.getName(), operationInfo.getSignature());
	}

	/**
	 * CALL 명령 대상명을 연산 시그니처 타입과 함께 만든다.
	 * @param operationName 연산 이름
	 * @param signature 파라미터 메타데이터. 비어 있거나 null이면 operationName()
	 * @return CALL ObjectName TargetName ... 형식의 대상명
	 */
	public static String toOperationCallTargetName(String operationName, MBeanParameterInfo[] signature) {
		if(operationName == null) {
			return "";
		}
		String name = operationName.trim();
		if(name.length() == 0) {
			return "";
		}
		if(signature == null || signature.length == 0) {
			return name + "()";
		}
		return name + "(" + toTypeString(signature) + ")";
	}

	/**
	 * CALL ArgumentList용 연산 인자 값을 포맷한다(쉼표 구분, 한 토큰).
	 * 배열 파라미터는 []로 감싸고, 쉼표·공백이 있는 문자열은 인자별로 따옴표 처리한다.
	 * @param signature 연산 파라미터 메타데이터
	 * @param values 파라미터 순서와 동일한 trim된 값
	 * @return 바깥 따옴표 없는 ArgumentList 텍스트. 예) [1,5],7
	 */
	public static String formatCallArgumentList(MBeanParameterInfo[] signature, String[] values) {
		if(signature == null || values == null || signature.length == 0) {
			return "";
		}
		if(signature.length != values.length) {
			throw new IllegalArgumentException("Argument count does not match the operation signature.");
		}
		StringBuilder buffer = new StringBuilder();
		for(int i = 0; i < signature.length; i++) {
			if(i > 0) {
				buffer.append(',');
			}
			buffer.append(formatCallArgument(signature[i], values[i]));
		}
		return buffer.toString();
	}

	public static boolean isArrayParameter(MBeanParameterInfo paramInfo) {
		if(paramInfo == null) {
			return false;
		}
		OpenType<?> openType = (OpenType<?>)paramInfo.getDescriptor().getFieldValue("openType");
		if(openType != null && openType.isArray()) {
			return true;
		}
		String type = paramInfo.getType();
		return type != null && type.startsWith("[");
	}

	public static String normalizeArrayArgumentInput(String value) {
		if(value == null) {
			return "";
		}
		String trimmed = value.trim();
		if(trimmed.length() >= 2 && trimmed.charAt(0) == '[' && trimmed.charAt(trimmed.length() - 1) == ']') {
			return trimmed.substring(1, trimmed.length() - 1).trim();
		}
		return trimmed;
	}

	private static String formatCallArgument(MBeanParameterInfo paramInfo, String value) {
		String trimmed = value == null ? "" : value.trim();
		if(isArrayParameter(paramInfo)) {
			return "[" + normalizeArrayArgumentInput(trimmed) + "]";
		}
		if(needsCallArgumentQuoting(paramInfo, trimmed)) {
			return quoteCallArgument(trimmed);
		}
		return trimmed;
	}

	private static boolean needsCallArgumentQuoting(MBeanParameterInfo paramInfo, String value) {
		if(value.length() == 0) {
			return false;
		}
		if("java.lang.String".equals(paramInfo.getType())) {
			return value.indexOf(',') >= 0 || value.indexOf(' ') >= 0 || value.indexOf('"') >= 0;
		}
		return value.indexOf(',') >= 0;
	}

	private static String quoteCallArgument(String value) {
		String escaped = value.replace("\\", "\\\\");
		escaped = escaped.replace("\"", "\\\"");
		return "\"" + escaped + "\"";
	}

	
	/**
	 * Argument의 이름과 타입을 표현하는 String을 리턴한다.
	 * @param paramInfos parameter 정보 배열
	 * @return 타입을 나타내는 문자열 (형식:"arg1_name(arg1_type),arg2_name(arg2_type),arg3_name(arg3_type),...")
	 */
	public static String toNameAndTypeString(MBeanParameterInfo[] paramInfos) {
		StringBuffer buffer = new StringBuffer();
		for(int i = 0; i < paramInfos.length; i++) {
			buffer.append(paramInfos[i].getName()).append("(").append(toTypeString(paramInfos[i].getType())).append(")");
			if(i+1 < paramInfos.length) {
				buffer.append(",");
			}
		}
		return buffer.toString();
	}	
	
	
	/**
	 * Arguments 또는 Return Type을 표현하는 String을 리턴한다.
	 * @param args 문자열 배열(타입의 Binary명 또는 PrimitiveType명을 포함)
	 * @return 타입을 나타내는 문자열 (형식:"arg1_type,arg2_type,arg3_type,...")
	 */
	public static String toTypeString(String[] args) {
		if(args == null || args.length == 0) {
			return "";
		}
		StringBuffer sb = new StringBuffer("");
		for(int i = 0; i < args.length; i++) {
			sb.append(TypeUtil.toType(args[i]));
			if(i+1 < args.length) {
				sb.append(",");
			}
		}
		return sb.toString();
	}

	
	/**
	 * Arguments 또는 Return Type을 표현하는 String을 리턴한다.
	 * @param args 문자열(타입의 Binary명 또는 PrimitiveType명을 포함)
	 * @return 타입을 나타내는 문자열 (형식:"arg1_type,arg2_type,arg3_type,...")
	 */
	public static String toTypeString(String arg) {
		if(arg == null) {
			return "";
		}
		return TypeUtil.toType(arg);
	}

	
	/**
	 * MBean Operation Impact에대한 문자열을 리턴한다.
	 * @param impact MBean Operation의 Impact 번호(0~3)
	 * @return Impact 타입을 나타내는 문자열(INFO, ACTION, ACTION_INFO, UNKNOWN);
	 */
	public static String getOperationImpactString(int impact) {
        switch(impact) {
	        case 0: return "INFO";
	        case 1: return "ACTION";
	        case 2: return "ACTION_INFO";
	        case 3: return "UNKNOWN";
	        default: return "(" + impact + ")";
        }		
	}
	
	
	/**
	 * MBean을 통해 VM의 JAVA MajorVersion에 대한 문자열을 리턴한다.
	 * @param mbeanConn 접속할 JVM의 MBeanServerConnection 객체 
	 * @return JAVA MajorVersion을 나타내는 문자열("1.6", "1.7", "1.8", ...). MBean 호출중 에러가 발생할 경우, null 리턴
	 */
	public static String getVmVersion(MBeanServerConnection mbeanConnection) {
		try {
			TabularData td = (TabularData)JMXUtil.getAttributeValue(mbeanConnection, new ObjectName("java.lang:type=Runtime"), "SystemProperties");
			CompositeData cd = td.get(new String[]{"java.specification.version"});
			return (String)cd.get("value");
		} catch(Exception e) {
			return null;
		}
	}
	
	/**
	 * VirtualMachine을 통해 VM의 JAVA MajorVersion에 대한 문자열을 리턴한다.
	 * @param virtualMachine 접속할 JVM의 VirtualMachine 객체 
	 * @return JAVA MajorVersion을 나타내는 문자열("1.6", "1.7", "1.8", ...). 호출중 에러가 발생할 경우, null 리턴
	 */
	public static String getVmVersion(VirtualMachine virtualMachine) {
		try {
			Properties vmProperties = virtualMachine.getSystemProperties();
			return vmProperties.getProperty("java.specification.version");
		} catch(Exception e) {
			return null;
		}
	}


    public static void addAttachToolsToClasspath() throws Exception {
		Float vm_version = Float.parseFloat(System.getProperty("java.specification.version"));
		if(vm_version > 1.8F) {
			Logger.logln(Level.DEBUG, "In Java 9 and later, you don't need to add the library for Attach to the classpath.");
			return;
		}
		URLClassLoader loader = (URLClassLoader)ClassLoader.getSystemClassLoader();
		final Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
		method.setAccessible(true);

		String java_home = System.getProperty("java.home");
		File f_javahome = new File(java_home);
		if(java_home.endsWith("jre")) {
			f_javahome = f_javahome.getParentFile();
		}
		File f_tools = new File(f_javahome.getAbsoluteFile() + File.separator + "lib" + File.separator + "tools.jar");
		if(f_tools.exists()) {
			try {
				method.invoke(loader, new Object[]{f_tools.toURI().toURL()});
				Logger.logln(Level.DEBUG, "Added library to classpath : " + f_tools.getCanonicalPath());
			} catch(Exception e) {
				Logger.logln(Level.ERROR, "Failed to add library to classpath : " + f_tools.getCanonicalPath());
				throw e;
			}
		} else {
			throw new Exception("Not support attach mode in JRE : JAVA_HOME='" + java_home + "'");
		}
    }

}
