package dal.tool.trace.jmxer.tui.output;

/**
 * Output TextBox 콘텐츠의 열 예산.
 * Lanterna는 가장 넓은 줄 너비에 1열을 더해 longestRow를 잡으므로, 줄이 패널 너비에 딱 맞아도 가로 스크롤이 생긴다.
 *
 */
final class OutputTextLayout {
	/** Lanterna longestRow에 가장 넓은 줄 너비 외에 더하는 열 수. */
	static final int LANTERN_LONGEST_ROW_EXTRA = 1;
	/** 세로 스크롤바가 보일 때 예약하는 열 수. */
	static final int VERTICAL_SCROLLBAR_COLUMN = 1;

	private OutputTextLayout() {}

	/**
	 * 가로 스크롤 없이 한 줄이 들어갈 수 있는 최대 표시 너비(터미널 열).
	 *
	 * @param paneColumns Output TextBox 내부 너비
	 * @param paneRows Output TextBox 표시 행 수
	 * @param displayLineCount 줄바꿈 후 표시될 줄 수(세로 스크롤바 여부 판단)
	 */
	static int contentMaxColumns(int paneColumns, int paneRows, int displayLineCount) {
		int cols = Math.max(1, paneColumns);
		int rows = Math.max(1, paneRows);
		int reserve = LANTERN_LONGEST_ROW_EXTRA;
		if(displayLineCount > rows) {
			reserve += VERTICAL_SCROLLBAR_COLUMN;
		}
		return Math.max(1, cols - reserve);
	}
}
