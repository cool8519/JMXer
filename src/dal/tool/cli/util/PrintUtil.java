package dal.tool.cli.util;

import dal.tool.util.ObjectDataUtil;

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
		return ObjectDataUtil.toString(obj, prettyPrint);
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
		return ObjectDataUtil.toString(obj, prettyPrint, prefix);
	}

}
