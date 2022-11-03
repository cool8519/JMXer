package dal.tool.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularType;


/**
 * 객체의 데이터를 문자열로 변환하기 위한 Util 클래스
 * @author 권영달
 *
 */
public class ObjectDataUtil {


	/**
	 * 객체를 문자열로 변환하여 리턴한다.<br/>
	 * JMX의 CompositeData 및 TabularData의 변환이 가능하다.
	 * @param obj 변환할 객체
	 * @param prettyPrint 결과 출력시 newline 및 indentation을 포함하려면 true
	 * @return 변환된 객체의 값 문자열
	 */
	public static String toString(Object obj, boolean prettyPrint) {
		return toString(obj, prettyPrint, null);
	}

	/**
	 * 객체를 문자열로 변환하여 리턴한다.<br/>
	 * JMX의 CompositeData 및 TabularData의 변환이 가능하다.
	 * @param obj 변환할 객체
	 * @param prettyPrint 결과 출력시 newline 및 indentation을 포함하려면 true
	 * @param prefix 결과 출력시 newline 다음에 추가할 indentation 문자열. null이면 공백 없이 출력.
	 * @return 변환된 객체의 값 문자열
	 */
	public static String toString(Object obj, boolean prettyPrint, String prefix) {
		if(obj == null) return null;
		if(prefix == null) prefix = "";
		String result = writeObject(obj, prefix);
		if(prettyPrint) {
			return result;
		} else {
			return result.replaceAll("\\n", "").replaceAll("\\t", "");
		}
	}

	/**
	 * 객체를 문자열로 변환하여 리턴한다.
	 * @param obj 변환할 객체
	 * @param prefix 결과 출력시 newline 다음에 추가할 indentation 문자열
	 * @return 변환된 객체의 값 문자열
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static String writeObject(Object obj, String prefix) {
		if(obj == null) {
			return "null";
		}
		StringBuffer buffer = new StringBuffer();
		if(obj.getClass().isArray()) {
			buffer = writeArrayData(buffer, TypeUtil.getObjectArray(obj), prefix);
		} else if(obj instanceof CompositeData) {
			buffer = writeCompositeData(buffer, (CompositeData)obj, prefix);
		} else if(obj instanceof TabularData) {
			buffer = writeTabularData(buffer, (TabularData)obj, prefix);
		} else if(obj instanceof Map) {
			buffer = writeMapData(buffer, (Map)obj, prefix);
		} else if(obj instanceof Collection) {
			buffer = writeCollectionData(buffer, (Collection)obj, prefix);
		} else {
			buffer = writeObjectData(buffer, obj);
		}
		return buffer.toString();
	}

	/**
	 * JMX의 CompositeData 객체를 문자열로 변환하여 리턴한다.
	 * @param buffer 데이터 값을 저장할 문자열 버퍼
	 * @param cd 변환할 CompositeData 객체
	 * @param prefix 결과 출력시 newline 다음에 추가할 indentation 문자열
	 * @return 변환된 객체의 값 문자열
	 */
	private static StringBuffer writeCompositeData(StringBuffer buffer, CompositeData cd, String prefix) {
		if(cd == null) {
			return buffer.append("javax.management.openmbean.CompositeData(null)");
		}
		CompositeType cdType = cd.getCompositeType();
		buffer.append("javax.management.openmbean.CompositeData@").append(Integer.toHexString(cd.hashCode())).append("(").append(cdType.getTypeName()).append(")={");
		try {
			Iterator<String> keyIter = cdType.keySet().iterator();
			Object content;
			while(keyIter.hasNext()) {
				buffer.append("\n").append(prefix+"\t");			
				String key = keyIter.next();
				OpenType<?> type = cdType.getType(key);
				content = cd.get(key);
				buffer.append(key).append("=");
				if(type instanceof SimpleType) {
					if(content == null) {
						buffer.append("null");
					} else {
						buffer.append(writeObject(content, prefix+"\t"));
					}
				} else {
					buffer.append(writeObject(content, prefix+"\t"));
				}
				if(keyIter.hasNext()) {
					buffer.append(", ");
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			buffer.append("\n").append(prefix).append("}");
		}
		return buffer;
	}

	/**
	 * JMX의 TabularData 객체를 문자열로 변환하여 리턴한다.
	 * @param buffer 데이터 값을 저장할 문자열 버퍼
	 * @param td 변환할 TabularData 객체
	 * @param prefix 결과 출력시 newline 다음에 추가할 indentation 문자열
	 * @return 변환된 객체의 값 문자열
	 */
	private static StringBuffer writeTabularData(StringBuffer buffer, TabularData td, String prefix) {
		if(td == null) {
			return buffer.append("javax.management.openmbean.TabularData(null)");
		}
		TabularType tdType = td.getTabularType();
		buffer.append("javax.management.openmbean.TabularData@").append(Integer.toHexString(td.hashCode())).append("(").append(tdType.getTypeName()).append(")={");
		buffer.append("\n");
		int cnt = 0;
		try {
			buffer.append(prefix+"\t").append("index=").append(writeObject(tdType.getIndexNames(), prefix+"\t")).append(", ").append("\n");
			buffer.append(prefix+"\t").append("data={");
	        List<String> keyNames = td.getTabularType().getIndexNames();
	        int indexCount = keyNames.size();
	        for(Object keys : td.keySet()) {
	        	cnt++;
				buffer.append("\n").append(prefix+"\t\t").append("{");
				Object[] keyValues = ((List<?>)keys).toArray();
				CompositeData cd = null;
	            for(int i = 0; i < indexCount; i++) {
	                buffer.append("\n").append(prefix+"\t\t\t");
	                buffer.append("[").append(keyNames.get(i)).append("=").append(keyValues[i]).append("]=");
	                cd = td.get(keyValues);
					if(cd == null) {
						buffer.append("null");
					} else {
						buffer.append(writeObject(cd, prefix+"\t\t\t"));
					}
					if(i+1 < indexCount) {
						buffer.append(", ");
					}
	            }				
	            buffer.append("\n").append(prefix+"\t\t").append("}");
	        }
	        buffer.append("\n").append(prefix+"\t").append("}");
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			if(cnt > 0) {
				buffer.append("\n").append(prefix);
			}
			buffer.append("}");
		}
		return buffer;
	}
	
	/**
	 * 배열 객체를 문자열로 변환하여 리턴한다.
	 * @param buffer 데이터 값을 저장할 문자열 버퍼
	 * @param arr 변환할 배열 객체
	 * @param prefix 결과 출력시 newline 다음에 추가할 indentation 문자열
	 * @return 변환된 객체의 값 문자열
	 */
	private static StringBuffer writeArrayData(StringBuffer buffer, Object[] arr, String prefix) {
		if(arr == null) {
			return buffer.append("Array(null)");
		}
		buffer.append("Array@").append(Integer.toHexString(arr.hashCode())).append("=[");
		int i = 0;
		try {
			for(; i < arr.length; i++) {
				buffer.append("\n").append(prefix+"\t");
				buffer.append("[").append(i).append("]=");
				if(arr[i] == null) {
					buffer.append("null");
				} else {
					buffer.append(writeObject(arr[i], prefix+"\t"));
				}				
				if(i+1 < arr.length) {
					buffer.append(", ");
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			if(i > 0) {
				buffer.append("\n").append(prefix);
			}
			buffer.append("]");
		}
		return buffer;
	}

	/**
	 * Collection 객체를 문자열로 변환하여 리턴한다.
	 * @param buffer 데이터 값을 저장할 문자열 버퍼
	 * @param col 변환할 Collection 객체
	 * @param prefix 결과 출력시 newline 다음에 추가할 indentation 문자열
	 * @return 변환된 객체의 값 문자열
	 */
	private static StringBuffer writeCollectionData(StringBuffer buffer, Collection<Object> col, String prefix) {
		if(col == null) {
			return buffer.append("Collection(null)");
		}
		buffer.append("Collection@").append(Integer.toHexString(col.hashCode())).append("={");
		Iterator<Object> iter = col.iterator();
		try {
			while(iter.hasNext()) {
				Object obj = iter.next();
				buffer.append("\n").append(prefix+"\t");
				if(obj == null) {
					buffer.append("null");
				} else {
					buffer.append(writeObject(obj, prefix+"\t"));
				}				
				if(iter.hasNext()) {
					buffer.append(", ");
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			if(col.size() > 0) {
				buffer.append("\n").append(prefix);
			}
			buffer.append("}");
		}
		return buffer;
	}

	/**
	 * Map 객체를 문자열로 변환하여 리턴한다.
	 * @param buffer 데이터 값을 저장할 문자열 버퍼
	 * @param map 변환할 Map 객체
	 * @param prefix 결과 출력시 newline 다음에 추가할 indentation 문자열
	 * @return 변환된 객체의 값 문자열
	 */
	private static StringBuffer writeMapData(StringBuffer buffer, Map<Object,Object> map, String prefix) {
		if(map == null) {
			return buffer.append("Map(null)");
		}
		buffer.append("Map@").append(Integer.toHexString(map.hashCode())).append("={");
		try {
			Iterator<Object> keyIter = map.keySet().iterator();
			Object key;
			Object content;
			while(keyIter.hasNext()) {
				buffer.append("\n").append(prefix+"\t");			
				key = keyIter.next();
				content = map.get(key);				
				if(key != null) {
					buffer.append("[").append(key).append("]=");
					if(content == null) {
						buffer.append("null");
					} else {
						buffer.append(writeObject(content, prefix+"\t"));
					}
				}
				if(keyIter.hasNext()) {
					buffer.append(", ");
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			buffer.append("\n").append(prefix).append("}");
		}
        return buffer;
	}
	
	/**
	 * 일반 객체를 문자열로 변환하여 리턴한다.
	 * @param buffer 데이터 값을 저장할 문자열 버퍼
	 * @param obj 변환할 객체
	 * @return 변환된 객체의 값 문자열
	 */
	private static StringBuffer writeObjectData(StringBuffer buffer, Object obj) {
		String data = obj.toString();
		data = data.replaceAll("\\n", "\\\\n").replaceAll("\\t", "\\\\t").replaceAll("\\\"", "\\\\\"");
		if(obj instanceof String) {
			buffer.append("\"").append(data).append("\"");
		} else {
			buffer.append(data);
		}
        return buffer;
	}

	
	/**
	 * long 타입 배열의 데이터를 문자열로 리턴한다.<br/>
	 * 결과가 최대 문자열 길이를 초과하는 경우, 이후 데이터는 생략된다.
	 * @param a long 타입의 배열
	 * @param maxSize 최대 문자열 길이(0보다 작으면, 무제한)
	 * @return 데이터 문자열
	 */
	public static String getDataString(long[] a, long maxSize) {
		if(a == null)
			return "null";
		int iMax = a.length - 1;
		if(iMax == -1)
			return "[]";

		StringBuilder b = new StringBuilder();
		b.append('[');
		for(int i = 0;; i++) {
			if(appendToMaxSize(b, String.valueOf(a[i]), maxSize)) {
				i = iMax;
			}
			if(i == iMax)
				return b.append(']').toString();
			b.append(", ");
		}
	}

	/**
	 * int 타입 배열의 데이터를 문자열로 리턴한다.<br/>
	 * 결과가 최대 문자열 길이를 초과하는 경우, 이후 데이터는 생략된다.
	 * @param a int 타입의 배열
	 * @param maxSize 최대 문자열 길이(0보다 작으면, 무제한)
	 * @return 데이터 문자열
	 */
	public static String getDataString(int[] a, long maxSize) {
		if(a == null)
			return "null";
		int iMax = a.length - 1;
		if(iMax == -1)
			return "[]";

		StringBuilder b = new StringBuilder();
		b.append('[');
		for(int i = 0;; i++) {
			if(appendToMaxSize(b, String.valueOf(a[i]), maxSize)) {
				i = iMax;
			}
			if(i == iMax)
				return b.append(']').toString();
			b.append(", ");
		}
	}

	/**
	 * short 타입 배열의 데이터를 문자열로 리턴한다.<br/>
	 * 결과가 최대 문자열 길이를 초과하는 경우, 이후 데이터는 생략된다.
	 * @param a short 타입의 배열
	 * @param maxSize 최대 문자열 길이(0보다 작으면, 무제한)
	 * @return 데이터 문자열
	 */
	public static String getDataString(short[] a, long maxSize) {
		if(a == null)
			return "null";
		int iMax = a.length - 1;
		if(iMax == -1)
			return "[]";

		StringBuilder b = new StringBuilder();
		b.append('[');
		for(int i = 0;; i++) {
			if(appendToMaxSize(b, String.valueOf(a[i]), maxSize)) {
				i = iMax;
			}
			if(i == iMax)
				return b.append(']').toString();
			b.append(", ");
		}
	}

	/**
	 * char 타입 배열의 데이터를 문자열로 리턴한다.<br/>
	 * 결과가 최대 문자열 길이를 초과하는 경우, 이후 데이터는 생략된다.
	 * @param a char 타입의 배열
	 * @param maxSize 최대 문자열 길이(0보다 작으면, 무제한)
	 * @return 데이터 문자열
	 */
	public static String getDataString(char[] a, long maxSize) {
		if(a == null)
			return "null";
		int iMax = a.length - 1;
		if(iMax == -1)
			return "[]";

		StringBuilder b = new StringBuilder();
		b.append('[');
		for(int i = 0;; i++) {
			if(appendToMaxSize(b, String.valueOf(a[i]), maxSize)) {
				i = iMax;
			}
			if(i == iMax)
				return b.append(']').toString();
			b.append(", ");
		}
	}

	/**
	 * byte 타입 배열의 데이터를 문자열로 리턴한다.<br/>
	 * 결과가 최대 문자열 길이를 초과하는 경우, 이후 데이터는 생략된다.
	 * @param a byte 타입의 배열
	 * @param maxSize 최대 문자열 길이(0보다 작으면, 무제한)
	 * @return 데이터 문자열
	 */
	public static String getDataString(byte[] a, long maxSize) {
		if(a == null)
			return "null";
		int iMax = a.length - 1;
		if(iMax == -1)
			return "[]";

		StringBuilder b = new StringBuilder();
		b.append('[');
		for(int i = 0;; i++) {
			if(appendToMaxSize(b, String.valueOf(a[i]), maxSize)) {
				i = iMax;
			}
			if(i == iMax)
				return b.append(']').toString();
			b.append(", ");
		}
	}

	/**
	 * boolean 타입 배열의 데이터를 문자열로 리턴한다.<br/>
	 * 결과가 최대 문자열 길이를 초과하는 경우, 이후 데이터는 생략된다.
	 * @param a boolean 타입의 배열
	 * @param maxSize 최대 문자열 길이(0보다 작으면, 무제한)
	 * @return 데이터 문자열
	 */
	public static String getDataString(boolean[] a, long maxSize) {
		if(a == null)
			return "null";
		int iMax = a.length - 1;
		if(iMax == -1)
			return "[]";

		StringBuilder b = new StringBuilder();
		b.append('[');
		for(int i = 0;; i++) {
			if(appendToMaxSize(b, String.valueOf(a[i]), maxSize)) {
				i = iMax;
			}
			if(i == iMax)
				return b.append(']').toString();
			b.append(", ");
		}
	}

	/**
	 * float 타입 배열의 데이터를 문자열로 리턴한다.<br/>
	 * 결과가 최대 문자열 길이를 초과하는 경우, 이후 데이터는 생략된다.
	 * @param a float 타입의 배열
	 * @param maxSize 최대 문자열 길이(0보다 작으면, 무제한)
	 * @return 데이터 문자열
	 */
	public static String getDataString(float[] a, long maxSize) {
		if(a == null)
			return "null";

		int iMax = a.length - 1;
		if(iMax == -1)
			return "[]";

		StringBuilder b = new StringBuilder();
		b.append('[');
		for(int i = 0;; i++) {
			if(appendToMaxSize(b, String.valueOf(a[i]), maxSize)) {
				i = iMax;
			}
			if(i == iMax)
				return b.append(']').toString();
			b.append(", ");
		}
	}

	/**
	 * double 타입 배열의 데이터를 문자열로 리턴한다.<br/>
	 * 결과가 최대 문자열 길이를 초과하는 경우, 이후 데이터는 생략된다.
	 * @param a double 타입의 배열
	 * @param maxSize 최대 문자열 길이(0보다 작으면, 무제한)
	 * @return 데이터 문자열
	 */
	public static String getDataString(double[] a, long maxSize) {
		if(a == null)
			return "null";
		int iMax = a.length - 1;
		if(iMax == -1)
			return "[]";

		StringBuilder b = new StringBuilder();
		b.append('[');
		for(int i = 0;; i++) {
			if(appendToMaxSize(b, String.valueOf(a[i]), maxSize)) {
				i = iMax;
			}
			if(i == iMax)
				return b.append(']').toString();
			b.append(", ");
		}
	}

	/**
	 * Object 타입 배열의 데이터를 문자열로 리턴한다.<br/>
	 * 결과가 최대 문자열 길이를 초과하는 경우, 이후 데이터는 생략된다.
	 * @param a Object 타입의 배열
	 * @param maxSize 최대 문자열 길이(0보다 작으면, 무제한)
	 * @return 데이터 문자열
	 */
	public static String getDataString(Object[] a, long maxSize) {
		if(a == null)
			return "null";

		int iMax = a.length - 1;
		if(iMax == -1)
			return "[]";

		StringBuilder b = new StringBuilder();
		b.append('[');
		for(int i = 0;; i++) {
			if(appendToMaxSize(b, String.valueOf(a[i]), maxSize)) {
				i = iMax;
			}
			if(i == iMax)
				return b.append(']').toString();
			b.append(", ");
		}
	}

	/**
	 * 문자열 버퍼에 다른 문자열을 추가한다.<br/>
	 * 결과가 최대 문자열 길이를 초과하는 경우, 이후 데이터는 생략된다.
	 * @param buf 문자열 버퍼
	 * @param str 추가할 문자열
	 * @param maxSize 최대 문자열 길이(0보다 작으면, 무제한)
	 * @return 결과 문자열 버퍼가 최대 길이를 초과한 경우 true
	 */
	private static boolean appendToMaxSize(StringBuilder buf, String str, long maxSize) {
		if(buf == null) {
			return true;
		}
		int prevLength = buf.length();
		if(maxSize < 0) {
			buf.append(str);
			return false;
		} else if(prevLength >= maxSize) {
			return true;
		} else {
			if(prevLength+str.length() > maxSize) {
				buf.append(str.substring(0, (int)(maxSize-prevLength))).append("... and more");
				return true;
			} else {
				buf.append(str);
				return false;
			}
		}
	}

	
}
