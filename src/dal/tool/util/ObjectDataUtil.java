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

import dal.tool.cli.Logger;


/**
 * 객체의 데이터를 문자열로 변환하기 위한 Util 클래스
 * @author 권영달
 *
 */
public class ObjectDataUtil {

	private static final String VOID_OPERATION_MESSAGE = "(operation completed; no return value)";


	/**
	 * void operation 완료 메시지를 리턴한다.
	 * @return void operation 출력 문자열
	 */
	public static String voidOperationMessage() {
		return VOID_OPERATION_MESSAGE;
	}

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
		if(obj == null) {
			return null;
		}
		if(prefix == null) {
			prefix = "";
		}
		String result = writeObject(obj, prefix);
		if(prettyPrint) {
			return result;
		}
		if(prefix.length() > 0) {
			return result.replaceAll("\\n" + prefix, "").replaceAll("\\t", "");
		}
		return result.replaceAll("\\n", "").replaceAll("\\t", "");
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static String writeObject(Object obj, String prefix) {
		if(obj == null) {
			return "null";
		}
		StringBuffer buffer = new StringBuffer();
		if(obj.getClass().isArray()) {
			buffer = writeArrayData(buffer, TypeUtil.getObjectArray(obj), prefix);
		} else if(obj instanceof CompositeData) {
			buffer = writeCompositeData(buffer, (CompositeData) obj, prefix);
		} else if(obj instanceof TabularData) {
			buffer = writeTabularData(buffer, (TabularData) obj, prefix);
		} else if(obj instanceof Map) {
			buffer = writeMapData(buffer, (Map) obj, prefix);
		} else if(obj instanceof Collection) {
			buffer = writeCollectionData(buffer, (Collection) obj, prefix);
		} else {
			buffer = writeObjectData(buffer, obj);
		}
		return buffer.toString();
	}

	private static StringBuffer writeCompositeData(StringBuffer buffer, CompositeData cd, String prefix) {
		if(cd == null) {
			return buffer.append("(CompositeData)(null)");
		}
		CompositeType cdType = cd.getCompositeType();
		String typeName = cdType.getTypeName();
		buffer.append("(").append(typeName).append("){");
		try {
			Iterator<String> keyIter = cdType.keySet().iterator();
			Object content;
			while(keyIter.hasNext()) {
				buffer.append("\n").append(prefix + "\t");
				String key = keyIter.next();
				OpenType<?> type = cdType.getType(key);
				content = cd.get(key);
				buffer.append(key).append("=");
				if(type instanceof SimpleType) {
					if(content == null) {
						buffer.append("null");
					} else {
						buffer.append(writeObject(content, prefix + "\t"));
					}
				} else {
					buffer.append(writeObject(content, prefix + "\t"));
				}
				if(keyIter.hasNext()) {
					buffer.append(", ");
				}
			}
		} catch(Exception e) {
			Logger.logStackTrace(e);
			buffer.append("\n").append(prefix + "\t").append("#ERROR: ").append(e.getMessage());
		} finally {
			buffer.append("\n").append(prefix).append("}");
		}
		return buffer;
	}

	private static StringBuffer writeTabularData(StringBuffer buffer, TabularData td, String prefix) {
		if(td == null) {
			return buffer.append("(TabularData)(null)");
		}
		TabularType tdType = td.getTabularType();
		String typeName = tdType.getTypeName();
		buffer.append("(").append(typeName).append("){");
		buffer.append("\n");
		int totalRows = td.size();
		int displayedRows = 0;
		try {
			buffer.append(prefix + "\t").append("index=").append(formatIndexNames(tdType.getIndexNames())).append(",\n");
			buffer.append(prefix + "\t").append("rows=").append(totalRows).append(",\n");
			buffer.append(prefix + "\t").append("data={");
			List<String> keyNames = td.getTabularType().getIndexNames();
			int indexCount = keyNames.size();
			for(Object keys : td.keySet()) {
				displayedRows++;
				Object[] keyValues = ((List<?>) keys).toArray();
				CompositeData cd = td.get(keyValues);
				buffer.append("\n").append(prefix + "\t\t");
				appendTabularRowKey(buffer, keyNames, keyValues);
				buffer.append("=");
				if(cd == null) {
					buffer.append("null");
				} else if(indexCount > 0) {
					buffer.append(writeCompositeDataForTabularRow(new StringBuffer(), cd, prefix + "\t\t\t"));
				} else {
					buffer.append(writeObject(cd, prefix + "\t\t\t"));
				}
			}
			buffer.append("\n").append(prefix + "\t").append("}");
		} catch(Exception e) {
			Logger.logStackTrace(e);
			buffer.append("\n").append(prefix + "\t").append("#ERROR: ").append(e.getMessage());
		} finally {
			if(displayedRows > 0) {
				buffer.append("\n").append(prefix);
			}
			buffer.append("}");
		}
		return buffer;
	}

	private static String formatIndexNames(List<String> keyNames) {
		if(keyNames == null || keyNames.isEmpty()) {
			return "[]";
		}
		StringBuilder b = new StringBuilder();
		b.append('[');
		for(int i = 0; i < keyNames.size(); i++) {
			if(i > 0) {
				b.append(", ");
			}
			b.append(keyNames.get(i));
		}
		b.append(']');
		return b.toString();
	}

	private static void appendTabularRowKey(StringBuffer buffer, List<String> keyNames, Object[] keyValues) {
		buffer.append('[');
		for(int i = 0; i < keyNames.size(); i++) {
			if(i > 0) {
				buffer.append(", ");
			}
			buffer.append(keyNames.get(i)).append('=').append(keyValues[i]);
		}
		buffer.append(']');
	}

	/**
	 * TabularData 행의 Composite 본문만 출력한다(타입 헤더·중괄호 한 겹).
	 */
	private static String writeCompositeDataForTabularRow(StringBuffer buffer, CompositeData cd, String prefix) {
		CompositeType cdType = cd.getCompositeType();
		buffer.append("{");
		try {
			Iterator<String> keyIter = cdType.keySet().iterator();
			while(keyIter.hasNext()) {
				buffer.append("\n").append(prefix + "\t");
				String key = keyIter.next();
				Object content = cd.get(key);
				buffer.append(key).append("=");
				if(content == null) {
					buffer.append("null");
				} else {
					buffer.append(writeObject(content, prefix + "\t"));
				}
				if(keyIter.hasNext()) {
					buffer.append(", ");
				}
			}
		} catch(Exception e) {
			Logger.logStackTrace(e);
			buffer.append("\n").append(prefix + "\t").append("#ERROR: ").append(e.getMessage());
		} finally {
			buffer.append("\n").append(prefix).append("}");
		}
		return buffer.toString();
	}

	private static StringBuffer writeArrayData(StringBuffer buffer, Object[] arr, String prefix) {
		if(arr == null) {
			return buffer.append("[\n").append(prefix + "\t").append("null\n").append(prefix).append("]");
		}
		buffer.append("[");
		int i = 0;
		try {
			for(; i < arr.length; i++) {
				buffer.append("\n").append(prefix + "\t");
				buffer.append("[").append(i).append("]=");
				if(arr[i] == null) {
					buffer.append("null");
				} else {
					buffer.append(writeObject(arr[i], prefix + "\t"));
				}
				if(i + 1 < arr.length) {
					buffer.append(", ");
				}
			}
		} catch(Exception e) {
			Logger.logStackTrace(e);
			buffer.append("\n").append(prefix + "\t").append("#ERROR: ").append(e.getMessage());
		} finally {
			if(i > 0) {
				buffer.append("\n").append(prefix);
			}
			buffer.append("]");
		}
		return buffer;
	}

	private static StringBuffer writeCollectionData(StringBuffer buffer, Collection<Object> col, String prefix) {
		if(col == null) {
			return buffer.append("{\n").append(prefix + "\t").append("null\n").append(prefix).append("}");
		}
		buffer.append("{");
		Iterator<Object> iter = col.iterator();
		try {
			while(iter.hasNext()) {
				Object obj = iter.next();
				buffer.append("\n").append(prefix + "\t");
				if(obj == null) {
					buffer.append("null");
				} else {
					buffer.append(writeObject(obj, prefix + "\t"));
				}
				if(iter.hasNext()) {
					buffer.append(", ");
				}
			}
		} catch(Exception e) {
			Logger.logStackTrace(e);
			buffer.append("\n").append(prefix + "\t").append("#ERROR: ").append(e.getMessage());
		} finally {
			if(col.size() > 0) {
				buffer.append("\n").append(prefix);
			}
			buffer.append("}");
		}
		return buffer;
	}

	private static StringBuffer writeMapData(StringBuffer buffer, Map<Object, Object> map, String prefix) {
		if(map == null) {
			return buffer.append("{\n").append(prefix + "\t").append("null\n").append(prefix).append("}");
		}
		buffer.append("{");
		try {
			Iterator<Object> keyIter = map.keySet().iterator();
			Object key;
			Object content;
			while(keyIter.hasNext()) {
				buffer.append("\n").append(prefix + "\t");
				key = keyIter.next();
				content = map.get(key);
				if(key != null) {
					buffer.append("[").append(key).append("]=");
					if(content == null) {
						buffer.append("null");
					} else {
						buffer.append(writeObject(content, prefix + "\t"));
					}
				}
				if(keyIter.hasNext()) {
					buffer.append(", ");
				}
			}
		} catch(Exception e) {
			Logger.logStackTrace(e);
			buffer.append("\n").append(prefix + "\t").append("#ERROR: ").append(e.getMessage());
		} finally {
			buffer.append("\n").append(prefix).append("}");
		}
		return buffer;
	}

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
			if(prevLength + str.length() > maxSize) {
				buf.append(str.substring(0, (int) (maxSize - prevLength))).append("... and more");
				return true;
			} else {
				buf.append(str);
				return false;
			}
		}
	}


}
