package dal.tool.trace.jmxer.tui;

import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

/**
 * MBeans/Features list with pane focus styling, F2 detail dialog (via {@link TuiF2Handler}), and selection-change hook.
 */
final class FocusActionListBox extends ActionListBox implements TuiF2Handler {
	private final WindowBasedTextGUI gui;
	private final BoxFocusManager mgr;
	private final Panel pane;
	private final String dialogTitle;
	private volatile Runnable selectionChanged;

	FocusActionListBox(WindowBasedTextGUI gui, BoxFocusManager mgr, Panel pane, String dialogTitle) {
		this.gui = gui;
		this.mgr = mgr;
		this.pane = pane;
		this.dialogTitle = dialogTitle;
	}

	void setSelectionChangedHandler(Runnable r) {
		this.selectionChanged = r;
	}

	@Override
	public boolean onF2Key() {
		Runnable selected = getSelectedItem();
		String text = selected == null ? "" : selected.toString();
		TuiDetailDialog.show(gui, dialogTitle, text);
		return true;
	}

	@Override
	protected void afterEnterFocus(Interactable.FocusChangeDirection direction, Interactable prev) {
		mgr.setActive(pane);
		super.afterEnterFocus(direction, prev);
		invalidate();
	}

	@Override
	protected void afterLeaveFocus(Interactable.FocusChangeDirection direction, Interactable next) {
		super.afterLeaveFocus(direction, next);
		invalidate();
	}

	@Override
	public synchronized Interactable.Result handleKeyStroke(KeyStroke keyStroke) {
		if(JmxTuiKeys.isGlobalShortcut(keyStroke)) {
			return Interactable.Result.UNHANDLED;
		}
		Interactable.Result r = super.handleKeyStroke(keyStroke);
		if(JmxTuiKeys.isFocusTraversalFromListNavigation(keyStroke, r)) {
			r = Interactable.Result.HANDLED;
		}
		if(r == Interactable.Result.HANDLED && keyStroke != null) {
			KeyType kt = keyStroke.getKeyType();
			if(kt == KeyType.ArrowUp || kt == KeyType.ArrowDown || kt == KeyType.PageUp || kt == KeyType.PageDown || kt == KeyType.Home || kt == KeyType.End) {
				Runnable h = selectionChanged;
				if(h != null) {
					h.run();
				}
			}
		}
		return r;
	}
}
