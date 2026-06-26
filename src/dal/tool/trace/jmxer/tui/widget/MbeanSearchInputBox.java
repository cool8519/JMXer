package dal.tool.trace.jmxer.tui.widget;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

import dal.tool.trace.jmxer.tui.support.BoxFocusManager;
import dal.tool.trace.jmxer.tui.support.JmxTuiKeys;
import dal.tool.trace.jmxer.tui.support.JmxTuiTheme;
import dal.tool.trace.jmxer.tui.support.TuiF2Handler;

/**
 * 포커스 테마와 선택적 키 핸들러를 갖는 MBean 필터 한 줄 입력 상자.
 * Enter 키는 포커스를 이동시키지 않는다.
 *
 */
public final class MbeanSearchInputBox extends TextBox implements TuiF2Handler {
	private volatile MbeanSearchKeyHandler handler;
	private volatile Runnable textChangedListener;
	private final BoxFocusManager mgr;
	private final Panel pane;

	public MbeanSearchInputBox(BoxFocusManager mgr, Panel pane, TerminalSize size) {
		super(size);
		this.mgr = mgr;
		this.pane = pane;
	}

	public void setKeyHandler(MbeanSearchKeyHandler h) {
		this.handler = h;
	}

	public void setTextChangedListener(Runnable listener) {
		this.textChangedListener = listener;
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
		if(keyStroke == null) {
			return Interactable.Result.UNHANDLED;
		}
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
		MbeanSearchKeyHandler h = handler;
		if(h != null && h.handle(keyStroke)) {
			fireTextChangedIfNeeded();
			return Interactable.Result.HANDLED;
		}
		if(keyStroke.getKeyType() == KeyType.Enter) {
			return Interactable.Result.HANDLED;
		}
		String before = getText();
		Interactable.Result r = super.handleKeyStroke(keyStroke);
		String after = getText();
		if(before == null) {
			before = "";
		}
		if(after == null) {
			after = "";
		}
		if(!before.equals(after)) {
			fireTextChangedIfNeeded();
		}
		return r;
	}

	private void fireTextChangedIfNeeded() {
		Runnable listener = textChangedListener;
		if(listener != null) {
			listener.run();
		}
	}
}
