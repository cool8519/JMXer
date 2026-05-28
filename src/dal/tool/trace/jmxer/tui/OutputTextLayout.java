package dal.tool.trace.jmxer.tui;

/**
 * Column budget for Output {@link com.googlecode.lanterna.gui2.TextBox} content.
 * Lanterna tracks {@code longestRow = max(line display width) + 1} for the caret and shows a
 * horizontal scrollbar when {@code longestRow &gt; visible text columns}, so a line that fills the
 * pane exactly still scrolls by one column.
 */
final class OutputTextLayout {
	/** Extra column Lanterna adds to {@code longestRow} beyond the widest line. */
	static final int LANTERN_LONGEST_ROW_EXTRA = 1;
	/** One column reserved when a vertical scrollbar is shown. */
	static final int VERTICAL_SCROLLBAR_COLUMN = 1;

	private OutputTextLayout() {}

	/**
	 * Maximum display width (terminal columns) for a single stored line so it fits without horizontal scroll.
	 *
	 * @param paneColumns       inner width of the Output TextBox
	 * @param paneRows          visible rows of the Output TextBox
	 * @param displayLineCount  number of lines that will be shown (after wrap), used to detect vertical scrollbar
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
