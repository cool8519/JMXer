package dal.tool.trace.jmxer.tui.widget;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.input.KeyStroke;

import dal.tool.trace.jmxer.tui.support.BoxFocusManager;
import dal.tool.trace.jmxer.tui.support.JmxTuiKeys;
import dal.tool.trace.jmxer.tui.support.JmxTuiTheme;
import dal.tool.trace.jmxer.tui.support.TuiF2Handler;
import dal.tool.trace.jmxer.tui.support.TuiTextPaste;

/**
 * 포커스 테마와 선택적 CommandInputKeyHandler를 갖는 명령줄 TextBox.
 *
 */
public final class CommandInputBox extends TextBox implements TuiF2Handler {
	private volatile CommandInputKeyHandler handler;
	private final BoxFocusManager mgr;
	private final Panel pane;

	public CommandInputBox(BoxFocusManager mgr, Panel pane, TerminalSize size) {
		super(size);
		this.mgr = mgr;
		this.pane = pane;
	}

	public void setKeyHandler(CommandInputKeyHandler h) {
		this.handler = h;
	}

	@Override
	public boolean onF2Key() {
		return false;
	}

	@Override
	protected TextBoxRenderer createDefaultRenderer() {
		return new JmxTextBoxRenderer();
	}

	@Override
	protected void afterEnterFocus(Interactable.FocusChangeDirection direction, Interactable prev) {
		setTheme(JmxTuiTheme.FOCUSED_TEXTBOX_THEME);
		mgr.setActive(pane);
		super.afterEnterFocus(direction, prev);
		pane.invalidate();
		invalidate();
	}

	@Override
	protected void afterLeaveFocus(Interactable.FocusChangeDirection direction, Interactable next) {
		setTheme(JmxTuiTheme.UNFOCUSED_TEXTBOX_THEME);
		super.afterLeaveFocus(direction, next);
		pane.invalidate();
		invalidate();
	}

	@Override
	public synchronized Interactable.Result handleKeyStroke(KeyStroke keyStroke) {
		if(JmxTuiKeys.isGlobalShortcut(keyStroke)) {
			return Interactable.Result.UNHANDLED;
		}
		if(JmxTuiKeys.isCtrlSuspend(keyStroke)) {
			return Interactable.Result.HANDLED;
		}
		if(JmxTuiKeys.isSaveOutputKey(keyStroke) || JmxTuiKeys.isCtrlToggleOutputWrap(keyStroke)
				|| JmxTuiKeys.isCtrlTogglePrettyPrint(keyStroke)
				|| JmxTuiKeys.isCtrlToggleThreadRefresh(keyStroke)) {
			// Output 전용 단축키가 다른 패널에 문자로 입력되지 않도록 소비한다.
			return Interactable.Result.HANDLED;
		}
		if(JmxTuiKeys.isCtrlPaste(keyStroke)) {
			TuiTextPaste.pasteIntoCommandLine(this);
			return Interactable.Result.HANDLED;
		}
		CommandInputKeyHandler h = handler;
		if(h != null && h.handle(keyStroke)) {
			return Interactable.Result.HANDLED;
		}
		return super.handleKeyStroke(keyStroke);
	}
}
