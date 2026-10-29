package dal.tool.trace.jmxer.tui.support;

import java.util.List;

import com.googlecode.lanterna.TerminalTextUtils;

/**
 * Status 패널 라벨 텍스트를 고정 열 너비에 맞춰 한 줄로 자른다(줄바꿈·스크롤 없음).
 *
 */
public final class TuiStatusText {
	private static final String ELLIPSIS = "...";
	public static final int MAX_BRACKET_INNER_COLUMNS = 60;
	public static final int MIN_BRACKET_COLUMNS = 5;

	private TuiStatusText() {}

	/**
	 * 문자열이 최대 열 너비 이내면 그대로 반환하고, 넘치면 뒷부분을 잘라 말줄임표를 붙인다.
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

	/**
	 * 앞선 상태 세그먼트와 접두 문자열 뒤에 대괄호 표현식을 덧붙인다.
	 * @param prefixSegments 앞선 상태 세그먼트 목록
	 * @param tailPrefix 선택 개수 접두 문자열
	 * @param expr 대괄호 안에 표시할 표현식 문자열
	 * @param statusInnerCols 상태 영역 전체 내부 열 너비
	 * @return 대괄호 표현식이 결합된 문자열
	 */
	public static String appendBracketedExpression(
			List<TuiStatusSegmentLabel.Segment> prefixSegments,
			String tailPrefix,
			String expr,
			int statusInnerCols) {
		String base = tailPrefix == null ? "" : tailPrefix;
		if(expr == null || expr.length() == 0) {
			return base;
		}
		int usedCols = TerminalTextUtils.getColumnWidth(base);
		if(prefixSegments != null) {
			for(int i = 0; i < prefixSegments.size(); i++) {
				TuiStatusSegmentLabel.Segment seg = prefixSegments.get(i);
				if(seg != null) {
					usedCols += TerminalTextUtils.getColumnWidth(seg.getText());
				}
			}
		}
		int availableBracketCols = statusInnerCols - usedCols - 1;
		if(availableBracketCols >= MIN_BRACKET_COLUMNS) {
			return base + " " + formatBracketedExpression(expr, availableBracketCols);
		}
		return base + " [" + expr + "]";
	}

	/**
	 * 대괄호로 감싼 표현식을 사용 가능한 열 너비와 최대 표시 폭에 맞춰 뒷부분을 생략한 문자열로 반환한다.
	 * @param expr 대괄호 안에 표시할 표현식 문자열
	 * @param availableBracketCols 대괄호 포함 사용 가능한 최대 열 너비
	 * @return 대괄호가 포함된 표시용 문자열
	 */
	public static String formatBracketedExpression(String expr, int availableBracketCols) {
		if(expr == null || expr.length() == 0) {
			return "";
		}
		int maxInnerCols = Math.min(MAX_BRACKET_INNER_COLUMNS, availableBracketCols - 2);
		if(maxInnerCols <= 0) {
			return "[" + ELLIPSIS + "]";
		}
		return "[" + fitExpressionInner(expr, maxInnerCols) + "]";
	}

	private static String fitExpressionInner(String expr, int maxInnerCols) {
		if(TerminalTextUtils.getColumnWidth(expr) <= maxInnerCols) {
			return expr;
		}
		int ellipsisCols = TerminalTextUtils.getColumnWidth(ELLIPSIS);
		if(maxInnerCols <= ellipsisCols) {
			return TerminalTextUtils.fitString(ELLIPSIS, maxInnerCols);
		}
		boolean inQuotes = false;
		String bestCommaCut = null;
		for(int i = 0; i < expr.length(); i++) {
			char ch = expr.charAt(i);
			if(ch == '"') {
				inQuotes = !inQuotes;
				continue;
			}
			if(!inQuotes && ch == ',') {
				String candidate = expr.substring(0, i + 1) + ELLIPSIS;
				if(TerminalTextUtils.getColumnWidth(candidate) <= maxInnerCols) {
					bestCommaCut = candidate;
				} else {
					break;
				}
			}
		}
		if(bestCommaCut != null) {
			return bestCommaCut;
		}
		return fitSingleLine(expr, maxInnerCols);
	}
}
