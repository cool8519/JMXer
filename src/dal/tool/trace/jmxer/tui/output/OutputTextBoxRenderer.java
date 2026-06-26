package dal.tool.trace.jmxer.tui.output;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TerminalTextUtils;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.TextGUIGraphics;

import dal.tool.trace.jmxer.tui.support.JmxTuiTheme;
import dal.tool.trace.jmxer.tui.widget.JmxTextBoxRenderer;

/**
 * 기본 TextBox 그리기 뒤 잘림 안내 줄(TuiOutputConstant.OUTPUT_TRUNCATED_PREFIX)을 강조한다.
 *
 */
final class OutputTextBoxRenderer extends JmxTextBoxRenderer {

	@Override
	public void drawComponent(TextGUIGraphics graphics, TextBox component) {
		super.drawComponent(graphics, component);
		drawTruncationNoticeHighlight(graphics, component);
	}

	private void drawTruncationNoticeHighlight(TextGUIGraphics graphics, TextBox component) {
		TerminalSize paneSize = graphics.getSize();
		if(paneSize.getRows() == 0 || paneSize.getColumns() == 0) {
			return;
		}
		TerminalPosition viewTopLeft = getViewTopLeft();
		TerminalSize textArea = OutputTextBoxViewport.contentAreaSize(paneSize, component);
		if(textArea.getRows() == 0 || textArea.getColumns() == 0) {
			return;
		}
		TextGUIGraphics textGraphics = graphics.newTextGraphics(TerminalPosition.TOP_LEFT_CORNER, textArea);
		textGraphics.setForegroundColor(JmxTuiTheme.OUTPUT_TRUNCATED_FG);
		textGraphics.setBackgroundColor(JmxTuiTheme.OUTPUT_TRUNCATED_BG);
		for(int row = 0; row < textArea.getRows(); row++) {
			int rowIndex = row + viewTopLeft.getRow();
			if(rowIndex >= component.getLineCount()) {
				continue;
			}
			String line = component.getLine(rowIndex);
			if(!TuiOutputConstant.isTruncationNoticeLine(line)) {
				continue;
			}
			int cols = textArea.getColumns();
			String visible = TerminalTextUtils.fitString(line, viewTopLeft.getColumn(), cols);
			textGraphics.putString(0, row, spaces(cols));
			textGraphics.putString(0, row, visible);
		}
	}

	private static String spaces(int count) {
		if(count <= 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder(count);
		for(int i = 0; i < count; i++) {
			sb.append(' ');
		}
		return sb.toString();
	}
}
