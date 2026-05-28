package dal.tool.trace.jmxer.tui;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

/**
 * Single-line MBean filter input with focus theme and optional key handler.
 */
final class MbeanSearchInputBox extends TextBox implements TuiF2Handler {
	private volatile MbeanSearchKeyHandler handler;
	private volatile Runnable textChangedListener;
	private final BoxFocusManager mgr;
	private final Panel pane;

	MbeanSearchInputBox(BoxFocusManager mgr, Panel pane, TerminalSize size) {
		super(size);
		this.mgr = mgr;
		this.pane = pane;
	}

	void setKeyHandler(MbeanSearchKeyHandler h) {
		this.handler = h;
	}

	void setTextChangedListener(Runnable listener) {
		this.textChangedListener = listener;
	}

	@Override
	public boolean onF2Key() {
		return false;
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
		MbeanSearchKeyHandler h = handler;
		if(h != null && h.handle(keyStroke)) {
			fireTextChangedIfNeeded();
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
