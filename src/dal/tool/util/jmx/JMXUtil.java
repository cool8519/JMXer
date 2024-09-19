package dal.tool.util.jmx;

import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.Attribute;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanFeatureInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.RuntimeMBeanException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXConnector;

import dal.tool.cli.Logger;
import dal.tool.util.PatternUtil;
import dal.tool.util.PatternUtil.PATTERN_TYPE;
import dal.tool.util.StringUtil;
import dal.tool.util.TypeUtil;
import dal.tool.util.jmx.MBeanConnector.ConnectType;


/**
 * JMX 호출을 위해 필요한 Utility 클래스
 * @author 권영달
 *
 */
public class JMXUtil {


	/**
	 * 전체 MBean ObjectName을 가져온다.
	 * @param mbeanConn 접속할 JVM의 MBeanServerConnection 객체 
	 * @return MBean ObjectName 객체 목록
	 * @throws Exception 수행 과정에서 발생한 모든 Exception
	 */
	public static List<ObjectName> getObjectNames(MBeanServerConnection mbeanConn) throws Exception {
        Set<ObjectName> queryNames = mbeanConn.queryNames(null, null);
        List<ObjectName> ret = new ArrayList<ObjectName>(queryNames);
        Collections.sort(ret);
        return ret;
	}

	
	/**
	 * 이름이 패턴에 맞는 MBean ObjectName을 가져온다.
	 * @param mbeanConn 접속할 JVM의 MBeanServerConnection 객체 
	 * @param objectNamePattern 가져올 ObjectName 이름 패턴. null이면 전체 목록 출력 
	 * @return MBean ObjectName 객체 목록
	 * @throws Exception 수행 과정에서 발생한 모든 Exception
	 */
	public static List<ObjectName> getObjectNames(MBeanServerConnection mbeanConn, String objectNamePattern) throws Exception {
        Set<ObjectName> queryNames = mbeanConn.queryNames(null, null);
        List<ObjectName> ret = new ArrayList<ObjectName>();
        for(ObjectName oName : queryNames) {
        	if(objectNamePattern == null || PatternUtil.isMatchPattern(PATTERN_TYPE.STRING, oName.toString(), objectNamePattern)) {
        		ret.add(oName);
        	}
        }
        Collections.sort(ret);
        return ret;
	}

	
    /**
     * 전체 MBean ObjectName을 가져온다.
	 * @param connect_type 연결 방식
	 * @param connect_value 연결 대상 문자열. connect_type에 따라 의미가 다르다.
	 * @param authenticate 인증 문자열 배열(user/password)
	 * @return MBean ObjectName 객체 목록
	 * @throws Exception 수행 과정에서 발생한 모든 Exception
     */
    public static List<ObjectName> getObjectNames(ConnectType connect_type, String connect_value, String[] authenticate) throws Exception {
		JMXConnector connector = JMXHelper.connectToVM(connect_type, connect_value, authenticate);
    	MBeanServerConnection mbeanConn = connector.getMBeanServerConnection();
    	return getObjectNames(mbeanConn);
    }

    
    /**
     * MBean ObjectName이 존재하는지 확인한다.
	 * @param mbeanConn 접속할 JVM의 MBeanServerConnection 객체 
	 * @param object_name 확인할 MBean ObjectName 문자열
     * @return ObjectName이 존재하면 true
     */
    public static boolean existsObjectName(MBeanServerConnection mbeanConn, String object_name) throws Exception {
    	return existsObjectName(getObjectNames(mbeanConn), object_name);
    }

    
    /**
     * MBean ObjectName이 존재하는지 확인한다.
	 * @param objectNames MBean ObjectName 객체 목록 
	 * @param object_name 확인할 MBean ObjectName 문자열
     * @return ObjectName이 존재하면 true
     */
    public static boolean existsObjectName(List<ObjectName> objectNames, String object_name) throws Exception {
		for(ObjectName objectName : objectNames) {
			if(object_name.equals(objectName.toString())) {
    			return true;
			}
		}
		return false;
    }

    
    /**
     * MBean 객체에 대한 전체 Attribute 정보를 가져온다.
     * @param mbeanConn 접속할 JVM의 MBeanServerConnection 객체 
     * @param oName MBean ObjectName 객체
     * @return MBeanAttributeInfo 객체 목록
     * @throws Exception 수행 과정에서 발생한 모든 Exception
     */
    public static List<MBeanAttributeInfo> getAttributeList(MBeanServerConnection mbeanConn, ObjectName oName) throws Exception {
    	MBeanInfo info = mbeanConn.getMBeanInfo(oName);
    	List<MBeanAttributeInfo> ret = Arrays.asList(info.getAttributes());
    	Collections.sort(ret, new Comparator<MBeanAttributeInfo>() {
    		@Override
    		public int compare(MBeanAttributeInfo o1, MBeanAttributeInfo o2) {
    			return o1.getName().compareTo(o2.getName());
    		}
		});
    	return ret;
    }
 
    
    /**
     * MBean 객체에 대한 전체 Operation 정보를 가져온다.
     * @param mbeanConn 접속할 JVM의 MBeanServerConnection 객체 
     * @param oName MBean ObjectName 객체
     * @return MBeanOperationInfo 객체 목록
     * @throws Exception 수행 과정에서 발생한 모든 Exception
     */
    public static List<MBeanOperationInfo> getOperationList(MBeanServerConnection mbeanConn, ObjectName oName) throws Exception {
    	MBeanInfo info = mbeanConn.getMBeanInfo(oName);
    	List<MBeanOperationInfo> ret = Arrays.asList(info.getOperations());
    	Collections.sort(ret, new Comparator<MBeanOperationInfo>() {
    		@Override
    		public int compare(MBeanOperationInfo o1, MBeanOperationInfo o2) {
    			return o1.getName().compareTo(o2.getName());
    		}
		});    	
    	return ret;
    }

    
    /**
     * MBean 객체에 대해 전체 Attribute와 Operation 정보를 가져온다.
     * @param mbeanConn 접속할 JVM의 MBeanServerConnection 객체 
     * @param oName MBean ObjectName 객체
     * @return MBeanFeatureInfo 객체 목록
     * @throws Exception 수행 과정에서 발생한 모든 Exception
     */
    public static List<MBeanFeatureInfo> getAttributeAndOperationList(MBeanServerConnection mbeanConn, ObjectName oName) throws Exception {
    	List<MBeanFeatureInfo> result = new ArrayList<MBeanFeatureInfo>();
    	result.addAll(getAttributeList(mbeanConn, oName));
    	result.addAll(getOperationList(mbeanConn, oName));
    	return result;
    }
 
    
    /**
     * MBean 객체에 대해 이름 패턴에 맞는 Attribute와 Operation 정보를 가져온다.
     * @param mbeanConn 접속할 JVM의 MBeanServerConnection 객체 
     * @param oName MBean ObjectName 객체
	 * @param namePattern 가져올 Attribute 또는 Operation 이름 패턴. null이면 전체 목록 출력 
     * @return MBeanFeatureInfo 객체 목록
     * @throws Exception 수행 과정에서 발생한 모든 Exception
     */
    public static List<MBeanFeatureInfo> getAttributeAndOperationList(MBeanServerConnection mbeanConn, ObjectName oName, String namePattern) throws Exception {
    	List<MBeanFeatureInfo> featureInfos = getAttributeAndOperationList(mbeanConn, oName);
        List<MBeanFeatureInfo> ret = new ArrayList<MBeanFeatureInfo>();
    	for(MBeanFeatureInfo featureInfo : featureInfos) {
    		if(featureInfo instanceof MBeanAttributeInfo) {
	    		if(namePattern == null || PatternUtil.isMatchPattern(PATTERN_TYPE.STRING, featureInfo.getName(), namePattern)) {
	    			ret.add(featureInfo);
	    		}
    		}
    	}
    	for(MBeanFeatureInfo featureInfo : featureInfos) {
    		if(featureInfo instanceof MBeanOperationInfo) {
	    		if(namePattern == null || PatternUtil.isMatchPattern(PATTERN_TYPE.STRING, featureInfo.getName(), namePattern)) {
	    			ret.add(featureInfo);
	    		}
    		}
    	}
    	return ret;
    }

    
	/**
	 * MBean 객체의 Attribute 값을 가져온다.
     * @param mbeanConn 접속할 JVM의 MBeanServerConnection 객체 
     * @param oName MBean ObjectName 객체
	 * @param targetName Attribute명
	 * @return Attribute값 객체. UnsupportedOperationException이 발생하는 경우, "#UNSUPPORT" 문자열 리턴
	 * @throws Exception 수행 과정에서 발생한 모든 Exception
	 */
	public static Object getAttributeValue(MBeanServerConnection mbeanConn, ObjectName oName, String targetName) throws Exception {
		if(targetName == null) {
			return null;
		}
		try {
			return mbeanConn.getAttribute(oName, targetName);
		} catch(RuntimeMBeanException rmbe) {
			if(rmbe.getTargetException() instanceof UnsupportedOperationException) {
				return "#UNSUPPORT";
			} else {
				throw rmbe;
			}
		} catch(Exception e) {
			throw e;
		}
	}

	
	/**
	 * MBean 객체의 Attribute 값을 변경한다.
     * @param mbeanConn 접속할 JVM의 MBeanServerConnection 객체 
     * @param oName MBean ObjectName 객체
	 * @param mbeanAttrInfo 변경할 Attribute에 대한 MBeanAttributeInfo 객체
	 * @param value 변경할 Attribute 값. 문자열로 입력하면 Attribute 타입에 맞는 Class로 변환되어 저장된다.
	 * @throws Exception 수행 과정에서 발생한 모든 Exception
	 */
	public static void setAttributeValue(MBeanServerConnection mbeanConn, ObjectName oName, MBeanAttributeInfo mbeanAttrInfo, String value) throws Exception {
		if(!mbeanAttrInfo.isWritable()) {
			throw new Exception("The attribute '" + mbeanAttrInfo.getName() + "' isn't writable.");
		}
		Object valueObj = JMXHelper.createInstance(mbeanAttrInfo.getType(), value);
		Logger.logln(Logger.Level.DEBUG, "setting the attribute '" + mbeanAttrInfo.getName() + "' to " + valueObj.toString() + "' in ObjectName '" + oName.toString() + "'");
		Attribute attr = new Attribute(mbeanAttrInfo.getName(), valueObj);
		mbeanConn.setAttribute(oName, attr);
	}

	
	/**
	 * Attribute 값을 가져온다.
	 * @param mbeanConn 접속할 JVM의 MBeanServerConnection 객체 
	 * @param oName MBean ObjectName 객체
	 * @param targetName Attribute명 또는 Operation명. null이면 전체 정보를 가져온다.
	 * @return MBean Attribute 값들의 Map.
	 * @throws Exception 수행 과정에서 발생한 모든 Exception
	 */
	public static Map<MBeanAttributeInfo,Object> getAttributeValuesWithInfo(MBeanServerConnection mbeanConn, ObjectName oName, String targetName) throws Exception {
		Map<MBeanAttributeInfo,Object> resultMap = new HashMap<MBeanAttributeInfo,Object>();
		for(MBeanAttributeInfo info : getAttributeList(mbeanConn, oName)) {
			if(targetName == null || targetName.equals(info.getName())) {
				if(info.isReadable()) {
					resultMap.put(info, getAttributeValue(mbeanConn, oName, info.getName()));
				}
			}
		}
		return resultMap;
	}

	
	/**
	 * Attribute 값을 가져온다.
	 * @param mbeanConn 접속할 JVM의 MBeanServerConnection 객체 
	 * @param oName MBean ObjectName 객체
	 * @param targetNames Attribute명 또는 Operation명 배열. null이면 전체 정보를 가져온다.
	 * @return MBean Attribute 값들의 Map.
	 * @throws Exception 수행 과정에서 발생한 모든 Exception
	 */
	public static Map<String,Object> getAttributeValuesWithName(MBeanServerConnection mbeanConn, ObjectName oName, String[] targetNames) throws Exception {
		Map<String,Object> resultMap = new HashMap<String,Object>();
		if(targetNames == null) {
			for(MBeanAttributeInfo info : getAttributeList(mbeanConn, oName)) {
				if(info.isReadable()) {
					resultMap.put(info.getName(), getAttributeValue(mbeanConn, oName, info.getName()));
				}
			}
		} else {
			for(String name : targetNames) {
				resultMap.put(name, getAttributeValue(mbeanConn, oName, name));
			}
		}
		return resultMap;
	}

	
	/**
	 * MBean 객체의 Operation을 수행한다.
     * @param mbeanConn 접속할 JVM의 MBeanServerConnection 객체 
     * @param oName MBean ObjectName 객체
	 * @param mbeanOperInfo 수행할 Operation에 대한 MBeanOperationInfo 객체
	 * @param argList Operation 수행에 필요한 argument 값 배열. 문자열로 입력하면 Operation 파라미터에 맞는 Class로 변환되어 수행된다.
	 * @return Operation 수행 결과 객체
	 * @throws Exception 수행 과정에서 발생한 모든 Exception
	 */
	public static Object invokeOperation(MBeanServerConnection mbeanConn, ObjectName oName, MBeanOperationInfo mbeanOperInfo, String[] argList) throws Exception {
		MBeanParameterInfo[] paramInfos = mbeanOperInfo.getSignature();
	    if(paramInfos.length > 0 && argList == null) {
	    	throw new Exception("Invalid argument size : should be " + paramInfos.length + ", but " + ((argList==null)?0:argList.length));
	    }
	    String[] opSig = new String[paramInfos.length];
	    for(int i = 0; i < opSig.length; i++){
	    	opSig[i] = paramInfos[i].getType();
	    }
	    Object[] opParam = new String[paramInfos.length];
    	opParam = JMXHelper.createParams(paramInfos, argList);
    	Logger.logln(Logger.Level.DEBUG, "Invoking operation '" + mbeanOperInfo.getName() + "' in ObjectName '" + oName.toString() + "'");
    	Logger.logln(Logger.Level.DEBUG, "Operation : " + mbeanOperInfo.getName() + "(" + JMXHelper.toTypeString(opSig) + "), returnType=" + JMXHelper.toTypeString(mbeanOperInfo.getReturnType()));
	    Object opRet = mbeanConn.invoke(oName, mbeanOperInfo.getName(), opParam, opSig);
	    if(!"void".equals(mbeanOperInfo.getReturnType())) {
	    	return opRet;
	    } else {
	    	return null;
	    }
	}

	
	/**
	 * Attribute를 조회/변경하거나  Operation을 호출한다.
	 * @param mbeanConn 접속할 JVM의 MBeanServerConnection 객체 
	 * @param oName MBean ObjectName 객체
	 * @param targetName Attribute명 또는 Operation명.
	 * @param target_values 호출 대상 값. target_name에 따라 의미가 다르다.
	 *         <xmp>
	 *         - Attribute 값을 조회하는 경우, null
	 *         - Attribute 값을 변경하는 경우, 변경할 Attribute 값
	 *         - Operation을 호출하는 경우, Operation Arguments 문자열 배열. Argument가 없을 경우는 null
	 *         </xmp>
	 * @return JMX 호출 결과 Object 객체
	 * @throws Exception 수행 과정에서 발생한 모든 Exception
	 */
	public static Object invokeAttributeOrOperation(MBeanServerConnection mbeanConn, ObjectName oName, String target_name, String[] target_values) throws Exception {
		if(target_name == null) {
			throw new Exception("Target name cannot be null.");
		}
		MBeanInfo mbeanInfo = mbeanConn.getMBeanInfo(oName);
		boolean isOperation = (target_name.indexOf("(") > 0);
		if(!isOperation) {
			for(MBeanAttributeInfo info : mbeanInfo.getAttributes()) {
	            if(info.getName().equals(target_name)) {
	            	if(target_values == null) {
	            		return getAttributeValue(mbeanConn, oName, target_name);
	            	} else {
	            		if(target_values.length > 0) {
	            			setAttributeValue(mbeanConn, oName, info, target_values[0]);
	            			return null;
	            		}
	            	}
	            }
			}
		}
		for(MBeanOperationInfo info : mbeanInfo.getOperations()) {
        	if(isOperation) {
        		String opString = target_name.substring(0, target_name.indexOf("("));
        		String argsString = target_name.substring(target_name.indexOf("(")+1, target_name.indexOf(")"));
        		if(info.getName().equals(opString)) {
	        		String[] argsArr = StringUtil.getTokenArray(argsString, ',', new String[]{"()"});
	        		MBeanParameterInfo[] paramInfo = info.getSignature();
	        		if(argsArr.length != paramInfo.length) {
	        			continue;
	        		}
	        		boolean isMatch = true;
	        		for(int i = 0; i < argsArr.length; i++) {
	        			String type = TypeUtil.toType(paramInfo[i].getType());
	        			if(!type.equals(argsArr[i])) {
	        				isMatch = false;
	        				break;
	        			}
	        		}
	        		if(isMatch) {
	        			try {
	        				return invokeOperation(mbeanConn, oName, info, target_values);
	        			} catch(Exception e) {
	        				throw e;
	        			}
	        		}
        		}
        	} else {
	            if(info.getName().equals(target_name)) {
	            	if((target_values == null && info.getSignature().length == 0) || 
	            	   (target_values != null && target_values.length == info.getSignature().length)) {
	            		try {
            				return invokeOperation(mbeanConn, oName, info, target_values);
	            		} catch(Exception e) {
	            			if(e.getCause() == null || !(e.getCause() instanceof NumberFormatException)) {
	            				throw e;
	            			}
	            		}
	            	}
	            }
        	}
		}
		throw new MBeanNotFoundException("No such attribute or operation '" + target_name + "' found");
	}

	
    /**
	 * JVM에 접속하여 JMX 호출 결과를 가져온다.
     * @param mbeanConn 접속할 JVM의 MBeanServerConnection 객체 
	 * @param object_name MBean ObjectName 문자열
	 * @param target_name 호출 대상 이름. 가능한 값은 다음과 같다.
	 *         <xmp>
	 *         - Attribute명
	 *         - Operation명
	 *         - "?" 전체 Attribute 및 Operation 정보
	 *         - "#" 전체 Attribute 값
	 *         </xmp>
	 * @param target_values 호출 대상 값. target_name에 따라 의미가 다르다.
	 *         <xmp>
	 *         - target_name이 Attribute인 경우, 변경할 Attribute 값
	 *         - target_name이 Operation인 경우, Operation Arguments 문자열 배열
	 *         - target_name이 "?" 또는 "#"인 경우, null이어야 함
	 *         </xmp>
	 * @return JMX 호출 결과 Object 객체. 입력값에 따라 객체의 형태가 다르다.
	 *         <xmp>
	 *         - MBean 정보를 조회하는 경우, MBeanInfo
	 *         - MBean의 Attribute와 Operation 목록을 조회하는 경우, List<MBeanFeatureInfo>
	 *         - Attribute 값을 조회하는 경우, Map<MBeanAttributeInfo,Object> (Attribute 값들의 배열)
	 *         - Attribute 값을 변경하는 경우, null
	 *         - Operation을 호출하는 경우, Object (Operation 호출 결과)
	 *         </xmp>
     * @throws MBeanNotFoundException object_name을 찾을 수 없을 때 발생
	 * @throws Exception 호출 과정에서 발생한 모든 Exception
     */
    public static Object getJMXResult(MBeanServerConnection mbeanConn, String object_name, String target_name, String[] target_values) throws MBeanNotFoundException,Exception {
    	if(object_name == null) {
    		return null;
    	}
        ObjectName oName = new ObjectName(object_name);
		MBeanInfo mbeanInfo = mbeanConn.getMBeanInfo(oName);
		if(target_name == null || "??".equals(target_name)) {
			return mbeanInfo;
		} else if("?".equals(target_name)) {
			if(target_values != null) {
				throw new Exception("To view all info, argument should be null. To invoke a operation, select a name of Operation or Attribute.");
			}
			return getAttributeAndOperationList(mbeanConn, oName);
		} else if("#".equals(target_name)) {
			if(target_values != null) {
				throw new Exception("To view all of attribute values, argument should be null. To invoke a operation, select a name of Operation or Attribute.");
			}
			return getAttributeValuesWithInfo(mbeanConn, oName, null);
		} else {
			return invokeAttributeOrOperation(mbeanConn, oName, target_name, target_values);
		}
    }
    
    /**
	 * JVM에 접속하여 모든 Thread ID를 가져온다.
     * @param mbeanConn 접속할 JVM의 MBeanServerConnection 객체 
	 * @param sort Thread ID를 정렬할 지 여부
	 * @return Thread ID 목록
	 * @throws Exception 호출 과정에서 발생한 모든 Exception
     */
    public static long[] getAllThreadIds(MBeanServerConnection mbeanConn, boolean sort) throws Exception {
		ObjectName objectName = new ObjectName("java.lang:type=Threading");		
		long[] threadIds = (long[])JMXUtil.invokeAttributeOrOperation(mbeanConn, objectName, "AllThreadIds", null);
		if(threadIds == null) {
			throw new Exception("Could not get thread info.");
		}
		if(sort) {
			Arrays.sort(threadIds);
		}
    	return threadIds;
    }

    /**
	 * JVM에 접속하여 Thread ID에 대한 ThreadInfo Map을 가져온다.
     * @param mbeanConn 접속할 JVM의 MBeanServerConnection 객체
     * @param threadIds 가져올 ThreadID 문자열 배열. null이면 모든 Thread를 가져온다. 
	 * @return Thread ID,ThreadInfo 목록
	 * @throws Exception 호출 과정에서 발생한 모든 Exception
     */
    public static Map<Long,ThreadInfo> getThreadInfo(MBeanServerConnection mbeanConn, String[] threadIds) throws Exception {
		ObjectName objectName = new ObjectName("java.lang:type=Threading");	
		
		String threadIdsString;
		if(threadIds == null) {
			long[] tids = JMXUtil.getAllThreadIds(mbeanConn, true);
			String[] tidStrArr = new String[tids.length];
			for(int i = 0; i < tids.length; i++) {
				tidStrArr[i] = String.valueOf(tids[i]);
			}
			threadIdsString = StringUtil.arrayToString(tidStrArr, ",");
		} else {
			threadIdsString = StringUtil.arrayToString(threadIds, ",");
		}
		Object resultData = JMXUtil.invokeAttributeOrOperation(mbeanConn, objectName, "getThreadInfo(long[])", new String[]{threadIdsString});
		
		CompositeData[] resultDataArr = null;
		if(resultData == null) {
			throw new Exception("Could not get thread info.");
		} else {
			resultDataArr = (CompositeData[])resultData;
			if(resultDataArr == null || resultDataArr.length < 1) {
				throw new Exception("Could not get thread info.");
			}
		}

		Map<Long,ThreadInfo> thrMap = new HashMap<Long,ThreadInfo>();
		for(CompositeData cd : resultDataArr) {
			if(cd == null) {
				continue;
			}
			ThreadInfo threadInfo = ThreadInfo.from(cd);
			thrMap.put(threadInfo.getThreadId(), threadInfo);
		}
		return thrMap;
	}
    
    /**
	 * JVM에 접속하여 모든 Thread ID에 대한 ThreadInfo Map을 가져온다.
     * @param mbeanConn 접속할 JVM의 MBeanServerConnection 객체 
	 * @return Thread ID,ThreadInfo 목록
	 * @throws Exception 호출 과정에서 발생한 모든 Exception
     */
    public static Map<Long,ThreadInfo> getAllThreadInfo(MBeanServerConnection mbeanConn) throws Exception {
    	return getThreadInfo(mbeanConn, null);
	}

    /**
	 * JMX를 이용하여 대상 프로세스의 SystemProperties를 가져온다.
     * @param mbeanConn 접속할 JVM의 MBeanServerConnection 객체
     * @param keys 가져오려는 Property 키 목록. null이면 모두 가져온다.
	 * @return 가져온 SystemProperties의 key와 value가 담겨진 Properties 객체
	 * @throws Exception 호출 과정에서 발생한 모든 Exception
     */
    public static Map<String,String> getRemoteSystemProperties(MBeanServerConnection mbeanConn, List<String> keys) throws Exception {
		Map<String,Object> target_results = getAttributeValuesWithName(mbeanConn, new ObjectName("java.lang:type=Runtime"), new String[]{ "SystemProperties" });
		TabularData target_props = (TabularData)target_results.get("SystemProperties");
		Map<String,String> prop = new HashMap<String,String>();
		if(keys == null) {
			Iterator<?> iter = target_props.keySet().iterator();
			while(iter.hasNext()) {
				String key = (String)iter.next();
				prop.put(key, (String)target_props.get(new String[]{ key }).get("value"));
			}
		} else {
			for(String key : keys) {
				CompositeData target_name_o = target_props.get(new String[]{ key });
				if(target_name_o != null) {
					prop.put(key, (String)target_name_o.get("value"));
				}
			}
		}
		return prop;
    }
    
}
