package dal.tool.trace.jmxer.tui.support;

import com.googlecode.lanterna.TerminalTextUtils;

/**
 * Status 패널 라벨 텍스트를 고정 열 너비에 맞춰 한 줄로 자른다(줄바꿈·스크롤 없음).
 *
 */
public final class TuiStatusText {
	private static final String ELLIPSIS = "...";

	private TuiStatusText() {}

	/**
	 * text가 maxColumns 이내면 그대로 반환하고, 넘치면 앞을 잘라 ...을 붙인다.
	 * @param text 대상 문자열
	 * @param maxColumns 최대 열 너비
	 * @return 한 줄로 맞춘 문자열
	 */
	public static String fitSingleLine(String text, int maxColumns) {
		if(text == null) {
			text = "";
		}
		if(maxColumns <= 0) {
			return "";
		}
		if(TerminalTextUtils.getColumnWidth(text) <= maxColumns) {
			return text;
		}
		int ellipsisCols = TerminalTextUtils.getColumnWidth(ELLIPSIS);
		int headCols = Math.max(0, maxColumns - ellipsisCols);
		if(headCols == 0) {
			return TerminalTextUtils.fitString(ELLIPSIS, maxColumns);
		}
		return TerminalTextUtils.fitString(text, 0, headCols) + ELLIPSIS;
	}
}
