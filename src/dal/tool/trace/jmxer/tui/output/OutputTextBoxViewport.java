package dal.tool.trace.jmxer.tui.output;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TerminalTextUtils;
import com.googlecode.lanterna.gui2.TextBox;

/**
 * Scrollable text area inside an Output {@link TextBox}, matching Lanterna scrollbar layout.
 */
final class OutputTextBoxViewport {

	private OutputTextBoxViewport() {}

	/**
	 * Visible content size after vertical/horizontal scrollbars (same rules as
	 * {@link TextBox.DefaultTextBoxRenderer#drawComponent}).
	 */
	static TerminalSize contentAreaSize(TerminalSize paneSize, TextBox component) {
		TerminalSize textArea = paneSize;
		int lineCount = component.getLineCount();
		int longestRow = longestDisplayWidth(component);
		boolean drawVerticalScrollBar = lineCount > textArea.getRows() && textArea.getColumns() > 1;
		if(drawVerticalScrollBar) {
			textArea = textArea.withRelativeColumns(-1);
		}
		if(longestRow > textArea.getColumns() && textArea.getRows() > 1) {
			textArea = textArea.withRelativeRows(-1);
			if(lineCount > textArea.getRows() && !drawVerticalScrollBar) {
				textArea = textArea.withRelativeColumns(-1);
			}
		}
		return textArea;
	}

	/** Topmost view row so the last line is visible in the content area. */
	static int maxViewTopRow(TextBox component, TerminalSize paneSize) {
		TerminalSize textArea = contentAreaSize(paneSize, component);
		int textRows = textArea.getRows();
		if(textRows <= 0) {
			return 0;
		}
		return Math.max(0, component.getLineCount() - textRows);
	}

	private static int longestDisplayWidth(TextBox component) {
		int max = 0;
		int count = component.getLineCount();
		for(int i = 0; i < count; i++) {
			String line = component.getLine(i);
			if(line != null) {
				max = Math.max(max, TerminalTextUtils.getColumnWidth(line));
			}
		}
		return max;
	}
}
