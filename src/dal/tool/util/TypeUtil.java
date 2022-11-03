package dal.tool.util;

import java.lang.reflect.Array;
import java.util.StringTokenizer;

/**
 * 메소드 및 객체의 타입을 식별하기 위한 Util 클래스 
 * @author 권영달
 *
 */
public class TypeUtil {

	private static final String[] primitiveNameList = new String[9];
	private static final Class<?>[] primitiveArrayWrapperList = new Class<?>[9];	
	private static final Class<?>[] primitiveWrapperList = new Class<?>[9];	
	private static final String[] primitiveEncodingList = new String[9];
	
	static {
		primitiveNameList[0] = "boolean";
		primitiveNameList[1] = "int";
		primitiveNameList[2] = "long";
		primitiveNameList[3] = "char";
		primitiveNameList[4] = "byte";
		primitiveNameList[5] = "float";
		primitiveNameList[6] = "double";
		primitiveNameList[7] = "short";
		primitiveNameList[8] = "void";

		primitiveArrayWrapperList[0] = boolean[].class;
		primitiveArrayWrapperList[1] = int[].class;
		primitiveArrayWrapperList[2] = long[].class;
		primitiveArrayWrapperList[3] = char[].class;
		primitiveArrayWrapperList[4] = byte[].class;
		primitiveArrayWrapperList[5] = float[].class;
		primitiveArrayWrapperList[6] = double[].class;
		primitiveArrayWrapperList[7] = short[].class;
		primitiveArrayWrapperList[8] = null;

		primitiveWrapperList[0] = Boolean.class;
		primitiveWrapperList[1] = Integer.class;
		primitiveWrapperList[2] = Long.class;
		primitiveWrapperList[3] = Character.class;
		primitiveWrapperList[4] = Byte.class;
		primitiveWrapperList[5] = Float.class;
		primitiveWrapperList[6] = Double.class;
		primitiveWrapperList[7] = Short.class;
		primitiveWrapperList[8] = Void.class;		
		
		primitiveEncodingList[0] = "Z";
		primitiveEncodingList[1] = "I";
		primitiveEncodingList[2] = "J";
		primitiveEncodingList[3] = "C";
		primitiveEncodingList[4] = "B";
		primitiveEncodingList[5] = "F";
		primitiveEncodingList[6] = "D";
		primitiveEncodingList[7] = "S";
		primitiveEncodingList[8] = "V";
	}

	/**
	 * 클래스명에 대한 Wrapper타입 클래스를 리턴한다.
	 * @param name 기본 타입을 나타내는 문자열
	 * @return Wrapper 클래스 객체
	 * @throws Exception 주어진 문자열이 기본 타입이 아니고, 해당 클래스를 찾을 수 없을 때 발생
	 */
	public static Class<?> getWrapperClass(String name) throws Exception {
		for(int i = 0; i < primitiveNameList.length; i++) {
			if(primitiveNameList[i].equals(name)) {
				return primitiveWrapperList[i];
			}
		}
		return Class.forName(name);
	}
	
	/**
	 * 클래스에 대한 Wrapper타입 클래스를 리턴한다.
	 * @param clazz 클래스 객체
	 * @return Wrapper 클래스 객체
	 * @throws Exception 주어진 문자열이 기본 타입이 아니고, 해당 클래스를 찾을 수 없을 때 발생
	 */
	public static Class<?> getWrapperClass(Class<?> clazz) throws Exception {
		return getWrapperClass(clazz.getName());
	}

	/**
	 * 객체가 배열을 나타내는 경우, 객체 배열 형태로 리턴한다.
	 * @param val 배열로 변환할 객체
	 * @return 객체 배열
	 */
	public static Object[] getObjectArray(Object val) {
	    Class<?> valKlass = val.getClass();
	    Object[] outputArray = null;
	    for(Class<?> arrKlass : primitiveArrayWrapperList) {
	    	if(arrKlass == null) continue;
	        if(valKlass.isAssignableFrom(arrKlass)) {
	            int arrlength = Array.getLength(val);
	            outputArray = new Object[arrlength];
	            for(int i = 0; i < arrlength; ++i) {
	                outputArray[i] = Array.get(val, i);
	            }
	            break;
	        }
	    }
	    if(outputArray == null) {
	        outputArray = (Object[])val;
	    }
	    return outputArray;
	}

	/**
	 * 메소드의 Arguments 및 Return 타입 목록을 Method Descriptor 문자열로 변환한다.
	 * @param methodTypes 메소드 Arguments 및 Return 타입 목록 문자열(형식:[예]"int,int,boolean[],java/lang/String)void")
	 * @return descriptor 문자열(형식:[예]"(II[ZLjava/lang/String;)V")
	 * @exception Exception 변환 과정에서 발생한 모든 Exception
	 */
	public static String typesToDesc(String methodTypes) throws Exception {
		// "(int,int,boolean[],java/lang/String)void" -> "(II[ZLjava/lang/String;)V"
		StringBuffer sb = new StringBuffer();
		String argType = methodTypes.substring(1, methodTypes.indexOf(')')).trim(); // int,int,boolean[],java/lang/String
		String retType = methodTypes.substring(methodTypes.indexOf(')')+1).trim();  // void
		StringTokenizer st = new StringTokenizer(argType,",");
		sb.append("(");
		while(st.hasMoreTokens()) {
			String token = st.nextToken();
			sb.append(typeToBinaryName(token));
		}
		sb.append(")");
		if(!retType.equals("")) {
			sb.append(typeToBinaryName(retType));
		}
		return sb.toString();
	}

	/**
	 * 클래스 타입 문자열을 바이너리명 문자열로 변환한다.
	 * @param classType 클래스 타입 문자열(형식:[예]"java/lang/String[]")
	 * @return 클래스 바이너리명 문자열(형식:[예]"[Ljava/lang/String;")
	 * @exception Exception 변환 과정에서 발생한 모든 Exception
	 */
	public static String typeToBinaryName(String classType) throws Exception {
		// "java/lang/String[]" -> "[Ljava/lang/String;"
		if(classType == null) {
			throw new Exception("ClassType cannot be null.");
		}
		StringBuffer sb = new StringBuffer();
		while(classType.endsWith("[]")) {
			sb.append("[");
			classType = classType.substring(0, classType.length()-2);
		}
		String encoding = null;
		for(int i = 0; i < primitiveNameList.length; i++) {
			if(primitiveNameList[i].equals(classType)) {
				encoding = primitiveEncodingList[i];
			}
		}
		if(encoding == null) {
			sb.append("L").append(classType).append(";");
		} else {
			sb.append(encoding);
		}
		return sb.toString();
	}
	
	/**
	 * Method Descriptor를  메소드의 Arguments 및 Return 타입 목록 문자열로 변환한다.
	 * @param methodDesc descriptor 문자열(형식:[예]"(II[ZLjava/lang/String;)V")
	 * @return 메소드 Arguments 및 Return 타입 목록 문자열(형식:[예]"int,int,boolean[],java/lang/String)void")
	 * @exception Exception 변환 과정에서 발생한 모든 Exception
	 */
	public static String descToTypes(String methodDesc) throws Exception {
		// "(II[ZLjava/lang/String;)V" -> "(int,int,boolean[],java/lang/String)void"
		String argDesc = methodDesc.substring(1, methodDesc.indexOf(')')); // II[ZLjava/lang/String;
		String retDesc = methodDesc.substring(methodDesc.indexOf(')')+1);  // V
		int length = argDesc.length();
		String[] binaryNameList = new String[length];
		int arrIdx = 0;
		int arrayDim = 0;
		boolean reading = false;
		int startIdx = -1;
		for(int i = 0; i < length; i++) {
			char c = argDesc.charAt(i);
			if(c == '[') {
				arrayDim++;
			} else if(c == 'L') {
				reading = true;
				startIdx = i;
			} else if(c == ';') {
				if(reading) {
					if(arrayDim > 0) {
						String arrType = "";
						for(int j = 0; j < arrayDim; j++) {
							arrType += "[";
						}
						binaryNameList[arrIdx++] = arrType+argDesc.substring(startIdx,i+1);
					} else {
						binaryNameList[arrIdx++] = argDesc.substring(startIdx,i+1);
					}
					startIdx = -1;
					reading = false;
				} else {
					throw new Exception("Descriptor is invalid : " + methodDesc);
				}
			} else if(reading) {
				continue;
			} else {
				if(arrayDim > 0) {
					String arrType = "";
					for(int j = 0; j < arrayDim; j++) {
						arrType += "[";
					}
					binaryNameList[arrIdx++] = arrType+c;
				} else {
					binaryNameList[arrIdx++] = ""+c;
				}
				arrayDim = 0;
			}
			if(length == i+1 && reading) throw new Exception("Descriptor is invalid : " + methodDesc);				
		}
		StringBuffer sb = new StringBuffer();
		sb.append("(");
		for(int i = 0; i < binaryNameList.length; i++) {
			String binaryName = binaryNameList[i];
			if(binaryName == null) break;
			if(i > 0) sb.append(",");
			sb.append(binaryNameToType(binaryName));
		}
		sb.append(")");
		sb.append(binaryNameToType(retDesc));
		return sb.toString();
	}

	/**
	 * 클래스 바이너리명 문자열을 타입 문자열로 변환한다.
	 * @param binaryName 클래스 바이너리명 문자열(형식:[예]"[Ljava/lang/String;")
	 * @return 클래스 타입 문자열(형식:[예]"java/lang/String[]")
	 * @exception Exception 변환 과정에서 발생한 모든 Exception
	 */
	public static String binaryNameToType(String binaryName) throws Exception {
		// "[Ljava/lang/String;" -> "java/lang/String[]"
		if(binaryName == null) {
			throw new Exception("BinaryName cannot be null.");
		}
		StringBuffer sb = new StringBuffer();
		int arrayDim = 0;
		boolean reading = false;
		int startIdx = -1;
		int length = binaryName.length();
		for(int i = 0; i < length; i++) {
			char c = binaryName.charAt(i);
			if(c == '[') {
				if(i > 0 && binaryName.charAt(i-1) != '[') {
					throw new Exception("BinaryName is invalid : " + binaryName);
				}
				arrayDim++;
			} else if(c == 'L') {
				if(length < i+2) {
					throw new Exception("BinaryName is invalid : " + binaryName);
				}
				reading = true;
				startIdx = i+1;
			} else if(c == ';') {
				if(reading) {
					if(length > i+1) {
						throw new Exception("BinaryName is invalid : " + binaryName);
					}
					sb.append(binaryName.substring(startIdx,i));
					break;
				} else {
					throw new Exception("BinaryName is invalid : " + binaryName);
				}
			} else if(reading) {
				continue;
			} else {
				String type = null;
				String find = String.valueOf(c);
				for(int j = 0; j < primitiveEncodingList.length; j++) {
					if(primitiveEncodingList[j].equals(find)) {
						type = primitiveNameList[j];
					}
				}
				if(type == null || length > i+1) {
					throw new Exception("BinaryName is invalid : " + binaryName);
				}
				sb.append(type);
				break;
			}
			if(length == i+1 && reading) {
				throw new Exception("BinaryName is invalid : " + binaryName);				
			}
		}
		if(arrayDim > 0) {
			for(int j = 0; j < arrayDim; j++) {
				sb.append("[]");
			}
		}
		return sb.toString();
	}
	
	/**
	 * 객체가 기본 타입인지 확인한다.
	 * @param obj 확인할 객체
	 * @return 기본 타입인 경우 true
	 */
	public static boolean isPrimitiveObject(Object obj) {
		if(obj == null) {
			return true;
		}
		for(int i = 0; i < primitiveWrapperList.length; i++) {
			if(primitiveWrapperList[i].equals(obj.getClass())) {
				return true;
			}
		}
		return obj.getClass().equals(String.class);
	}
	
	
	/**
	 * 바이너리명 문자열을 타입 문자열로 변환한다.
	 * 입력 문자열이 기본 타입인 경우는 그대로 리턴된다.
	 * @param arg 바이너리명 문자열
	 * @return 타입 문자열
	 */
	public static String toType(String arg) {
		for(int i = 0; i < primitiveNameList.length; i++) {
			if(primitiveNameList[i].equals(arg)) {
				return arg;
			}
		}
		for(int i = 0; i < primitiveEncodingList.length; i++) {
			if(primitiveEncodingList[i].equals(arg)) {
				return primitiveNameList[i];
			}
		}
		try {
			return binaryNameToType(arg);
		} catch(Exception e) {
			return arg;
		}
	}

}
