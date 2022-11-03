package dal.tool.cli.util;

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

import dal.tool.util.TypeUtil;

/**
 * 데이터를 출력하기 위한 Util 클래스
 * @author 권영달
 *
 */
public class PrintUtil {

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
			return result.replaceAll("\\n"+prefix, "").replaceAll("\\t", "");
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
			return buffer.append("{").append("\n")
					.append(prefix+"\t").append("null").append("\n")
					.append(prefix).append("}");
		}
		CompositeType cdType = cd.getCompositeType();
		buffer.append("{");
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
			return buffer.append("{").append("\n")
					.append(prefix+"\t").append("null").append("\n")
					.append(prefix).append("}");
		}
		TabularType tdType = td.getTabularType();
		buffer.append("{");
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
			return buffer.append("[").append("\n")
					.append(prefix+"\t").append("null").append("\n")
					.append(prefix).append("]");
		}
		buffer.append("[");
		int i = 0;
		try {
			for(; i < arr.length; i++) {
				buffer.append("\n").append(prefix+"\t");
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
			return buffer.append("{").append("\n")
					.append(prefix+"\t").append("null").append("\n")
					.append(prefix).append("}");
		}
		buffer.append("{");
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
			return buffer.append("{").append("\n")
					.append(prefix+"\t").append("null").append("\n")
					.append(prefix).append("}");
		}
		buffer.append("{");
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
		data = data.replaceAll("\\n", "\\\\n").replaceAll("\\t", "\\\\t");
		if(obj instanceof String) {
			buffer.append("\"").append(data).append("\"");
		} else {
			buffer.append(data);
		}
        return buffer;
	}


}
