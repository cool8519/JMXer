package dal.tool.trace.jmxer.tui.widget;

import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.InteractableRenderer;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextGUIGraphics;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

import dal.tool.trace.jmxer.tui.support.BoxFocusManager;
import dal.tool.trace.jmxer.tui.support.JmxTuiKeys;
import dal.tool.trace.jmxer.tui.support.TuiF2Handler;
import dal.tool.trace.jmxer.tui.support.TuiKeyAction;
import dal.tool.trace.jmxer.tui.support.TuiKeyBindings;
import dal.tool.trace.jmxer.tui.dialog.TuiDetailDialog;

/**
 * MBeans/Features 목록. 패널 포커스 스타일, Info 상세 대화상자, 선택 변경 훅을 제공한다.
 *
 */
public final class FocusActionListBox extends ActionListBox implements TuiF2Handler {
	private final WindowBasedTextGUI gui;
	private final BoxFocusManager mgr;
	private final Panel pane;
	private final String dialogTitle;
	private volatile Runnable selectionChanged;
	private volatile ListKeyHandler keyHandler;
	private volatile TuiKeyAction activateKeyAction;

	public FocusActionListBox(WindowBasedTextGUI gui, BoxFocusManager mgr, Panel pane, String dialogTitle) {
		this.gui = gui;
		this.mgr = mgr;
		this.pane = pane;
		this.dialogTitle = dialogTitle;
	}

	public void setSelectionChangedHandler(Runnable r) {
		this.selectionChanged = r;
	}

	public void setKeyHandler(ListKeyHandler handler) {
		this.keyHandler = handler;
	}

	public void setActivateKeyAction(TuiKeyAction activateKeyAction) {
		this.activateKeyAction = activateKeyAction;
	}

	public int getScrollTopIndex() {
		return scrollOffset == null ? 0 : Math.max(0, -scrollOffset.getRow());
	}

	@Override
	protected InteractableRenderer<ActionListBox> createDefaultRenderer() {
		return new DefaultListBoxRenderer<Runnable, ActionListBox>() {
			@Override
			public void drawComponent(TextGUIGraphics graphics, ActionListBox listBox) {
				int rows = graphics.getSize().getRows();
				if(rows > 0 && listBox.getItemCount() <= rows && getScrollTopIndex() > 0) {
					// 항목 수가 화면 행 수 이하로 줄어들었을 때 이전 스크롤 위치가 남지 않도록 상단 기준을 초기화한다.
					int savedSelectedIndex = listBox.getSelectedIndex();
					if(savedSelectedIndex > 0) {
						listBox.setSelectedIndex(0);
						super.drawComponent(graphics, listBox);
						listBox.setSelectedIndex(savedSelectedIndex);
					}
				}
				super.drawComponent(graphics, listBox);
			}
		};
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
		if(JmxTuiKeys.isCtrlSuspend(keyStroke)) {
			return Interactable.Result.HANDLED;
		}
		if(JmxTuiKeys.isSaveOutputKey(keyStroke) || JmxTuiKeys.isCtrlToggleOutputWrap(keyStroke)
				|| JmxTuiKeys.isCtrlTogglePrettyPrint(keyStroke)
				|| JmxTuiKeys.isCtrlToggleThreadRefresh(keyStroke)) {
			// Output 전용 단축키가 다른 패널에 문자로 입력되지 않도록 소비한다.
			return Interactable.Result.HANDLED;
		}
		ListKeyHandler listKeyHandler = keyHandler;
		if(listKeyHandler != null && listKeyHandler.handle(keyStroke)) {
			return Interactable.Result.HANDLED;
		}
		TuiKeyAction activateAction = activateKeyAction;
		if(activateAction != null) {
			if(TuiKeyBindings.current().matches(activateAction, keyStroke)) {
				Runnable selected = getSelectedItem();
				if(selected != null) {
					selected.run();
				}
				return Interactable.Result.HANDLED;
			}
			if(keyStroke != null && keyStroke.getKeyType() == KeyType.Enter) {
				return Interactable.Result.HANDLED;
			}
		}
		if(JmxTuiKeys.isPlainSpaceKey(keyStroke) || JmxTuiKeys.isThreadToggleCurrentKey(keyStroke)) {
			// Space로도 항목이 활성화되므로 바인딩된 동작 키 외 Enter·Space 기본 활성화를 막는다.
			return Interactable.Result.HANDLED;
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
