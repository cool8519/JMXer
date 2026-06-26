package dal.tool.trace.jmxer.tui.widget;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.TextGUIGraphics;

import dal.tool.trace.jmxer.tui.support.JmxTuiTheme;

/**
 * 테마 배경 위에서 캐럿 셀을 고대비 색으로 그려 커서가 보이게 하는 TextBox 렌더러.
 *
 */
public class JmxTextBoxRenderer extends TextBox.DefaultTextBoxRenderer {

	@Override
	public void drawComponent(TextGUIGraphics graphics, TextBox component) {
		super.drawComponent(graphics, component);
		drawCaretCell(graphics, component);
	}

	private void drawCaretCell(TextGUIGraphics graphics, TextBox component) {
		if(!component.isFocused() || component.isReadOnly()) {
			return;
		}
		TerminalPosition caretOnScreen = getCursorLocation(component);
		if(caretOnScreen == null) {
			return;
		}
		TerminalSize size = graphics.getSize();
		if(caretOnScreen.getColumn() < 0 || caretOnScreen.getRow() < 0
				|| caretOnScreen.getColumn() >= size.getColumns() || caretOnScreen.getRow() >= size.getRows()) {
			return;
		}
		TerminalPosition caret = component.getCaretPosition();
		String line = component.getLine(caret.getRow());
		int column = caret.getColumn();
		if(column > line.length()) {
			column = line.length();
		}
		char caretChar = ' ';
		if(column < line.length()) {
			caretChar = line.charAt(column);
		}
		Character mask = component.getMask();
		if(mask != null) {
			caretChar = mask.charValue();
		}
		graphics.setForegroundColor(JmxTuiTheme.TEXTBOX_CARET_FG);
		graphics.setBackgroundColor(JmxTuiTheme.TEXTBOX_CARET_BG);
		graphics.putString(caretOnScreen.getColumn(), caretOnScreen.getRow(), String.valueOf(caretChar));
	}
}
