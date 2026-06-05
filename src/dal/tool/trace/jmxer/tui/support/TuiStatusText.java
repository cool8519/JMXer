package dal.tool.trace.jmxer.tui.support;

import com.googlecode.lanterna.TerminalTextUtils;

/**
 * Fits Status pane label text to a fixed column width without wrapping (no extra rows / scroll).
 */
public final class TuiStatusText {
	private static final String ELLIPSIS = "...";

	private TuiStatusText() {}

	/**
	 * Returns {@code text} if it fits in {@code maxColumns}; otherwise truncates the start and appends {@code ...}.
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
