package dal.tool.trace.jmxer.tui.support;

/**
 * 목록·다이얼로그 선택 행 커서 마커. ASCII 1칸 문자로 SSH 터미널 폭 불일치를 피한다.
 *
 */
public final class TuiListArrowPrefix {

	public static final char SELECTED_MARKER = '>';

	public static final String LIST_SELECTED = "> ";
	public static final String LIST_UNSELECTED = "  ";

	public static final String THREAD_CURSOR = ">  ";
	public static final String THREAD_CREATED_CURSOR = "> +";

	private TuiListArrowPrefix() {}
}
