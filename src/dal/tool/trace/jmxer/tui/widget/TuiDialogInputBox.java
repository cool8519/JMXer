package dal.tool.trace.jmxer.tui.widget;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.TextBox;

import dal.tool.trace.jmxer.tui.support.JmxTuiTheme;

/**
 * 메인 패널(예: Search Pane)과 동일한 포커스/비포커스 테마를 쓰는 대화상자 한 줄 입력 상자.
 *
 */
public final class TuiDialogInputBox extends TextBox {
	public TuiDialogInputBox(TerminalSize size) {
		super(size);
		setTheme(JmxTuiTheme.UNFOCUSED_TEXTBOX_THEME);
	}

	@Override
	protected TextBoxRenderer createDefaultRenderer() {
		return new JmxTextBoxRenderer();
	}

	@Override
	protected void afterEnterFocus(Interactable.FocusChangeDirection direction, Interactable prev) {
		setTheme(JmxTuiTheme.FOCUSED_TEXTBOX_THEME);
		super.afterEnterFocus(direction, prev);
		invalidate();
	}

	@Override
	protected void afterLeaveFocus(Interactable.FocusChangeDirection direction, Interactable next) {
		setTheme(JmxTuiTheme.UNFOCUSED_TEXTBOX_THEME);
		super.afterLeaveFocus(direction, next);
		invalidate();
	}
}
