package dal.tool.trace.jmxer.tui.output;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TerminalTextUtils;
import com.googlecode.lanterna.gui2.TextBox;

/**
 * Output TextBox 안의 스크롤 가능 텍스트 영역. Lanterna 스크롤바 배치 규칙과 맞춘다.
 *
 */
final class OutputTextBoxViewport {

	private OutputTextBoxViewport() {}

	/**
	 * 세로/가로 스크롤바를 뺀 뒤 보이는 콘텐츠 크기(TextBox.DefaultTextBoxRenderer.drawComponent와 동일 규칙).
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
