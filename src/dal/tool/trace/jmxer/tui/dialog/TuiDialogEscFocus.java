package dal.tool.trace.jmxer.tui.dialog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Container;
import com.googlecode.lanterna.gui2.InputFilter;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.LocalizedString;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowListener;
import com.googlecode.lanterna.gui2.dialogs.DialogWindow;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

import dal.tool.trace.jmxer.cli.JmxTuiConnectionLossHandler;
import dal.tool.trace.jmxer.tui.support.JmxTuiKeys;

/**
 * 다이얼로그 키보드 동작. Esc는 취소/닫기 버튼으로 포커스 이동, 버튼은 Enter로만 활성화한다.
 *
 */
final class TuiDialogEscFocus {

	private static final char SPACE_CHAR = ' ';

	private TuiDialogEscFocus() {}

	static void install(final DialogWindow window) {
		if(window == null) {
			return;
		}
		installEnterOnlyButtonActivation(window);
		window.setCloseWindowWithEscape(false);
		window.addWindowListener(new WindowListener() {
			@Override
			public void onInput(Window pane, KeyStroke keyStroke, AtomicBoolean deliverEvent) {
				if(deliverEvent != null && !deliverEvent.get()) {
					return;
				}
				if(JmxTuiKeys.isCtrlSuspend(keyStroke)) {
					deliverEvent.set(false);
					return;
				}
				if(shouldConsumeSpaceOnDialog(pane, keyStroke)) {
					deliverEvent.set(false);
					return;
				}
				if(keyStroke == null || keyStroke.getKeyType() != KeyType.Escape) {
					return;
				}
				Button cancel = resolveCancelButton(pane);
				if(cancel == null) {
					return;
				}
				pane.setFocusedInteractable(cancel);
				cancel.takeFocus();
				deliverEvent.set(false);
			}

			@Override
			public void onUnhandledInput(Window pane, KeyStroke keyStroke, AtomicBoolean consumeEvent) {
			}

			@Override
			public void onResized(Window window, com.googlecode.lanterna.TerminalSize oldSize, com.googlecode.lanterna.TerminalSize newSize) {
			}

			@Override
			public void onMoved(Window window, com.googlecode.lanterna.TerminalPosition oldPosition, com.googlecode.lanterna.TerminalPosition newPosition) {
			}
		});
	}

	private static void installEnterOnlyButtonActivation(Window window) {
		List<Button> buttons = new ArrayList<Button>();
		Component root = window.getComponent();
		if(root != null) {
			collectButtons(root, buttons);
		}
		for(int i = 0; i < buttons.size(); i++) {
			final Button button = buttons.get(i);
			button.setInputFilter(new InputFilter() {
				@Override
				public boolean onInput(Interactable interactable, KeyStroke keyStroke) {
					return !isSpaceKey(keyStroke);
				}
			});
		}
	}

	private static boolean isSpaceKey(KeyStroke keyStroke) {
		if(keyStroke == null || keyStroke.getKeyType() != KeyType.Character) {
			return false;
		}
		Character character = keyStroke.getCharacter();
		return character != null && character.charValue() == SPACE_CHAR;
	}

	private static boolean shouldConsumeSpaceOnDialog(Window window, KeyStroke keyStroke) {
		if(!isSpaceKey(keyStroke) || window == null) {
			return false;
		}
		Interactable focused = window.getFocusedInteractable();
		return focused instanceof ActionListBox || focused instanceof Button;
	}

	private static Button resolveCancelButton(Window window) {
		List<Button> buttons = new ArrayList<Button>();
		Component root = window.getComponent();
		if(root != null) {
			collectButtons(root, buttons);
		}
		if(buttons.isEmpty()) {
			return null;
		}
		for(int i = 0; i < buttons.size(); i++) {
			if(isCancelLabel(buttons.get(i).getLabel())) {
				return buttons.get(i);
			}
		}
		if(buttons.size() == 1) {
			return buttons.get(0);
		}
		return buttons.get(buttons.size() - 1);
	}

	private static boolean isCancelLabel(String label) {
		if(label == null) {
			return false;
		}
		String trimmed = label.trim();
		return equalsIgnoreCase(trimmed, MessageDialogButton.Cancel.toString())
			|| equalsIgnoreCase(trimmed, MessageDialogButton.No.toString())
			|| equalsIgnoreCase(trimmed, MessageDialogButton.Close.toString())
			|| equalsIgnoreCase(trimmed, LocalizedString.Cancel.toString())
			|| equalsIgnoreCase(trimmed, LocalizedString.No.toString())
			|| equalsIgnoreCase(trimmed, JmxTuiConnectionLossHandler.ACTION_CLOSE);
	}

	private static boolean equalsIgnoreCase(String a, String b) {
		return a != null && b != null && a.equalsIgnoreCase(b);
	}

	private static void collectButtons(Component component, List<Button> buttons) {
		if(component instanceof Button) {
			buttons.add((Button) component);
			return;
		}
		if(component instanceof Container) {
			for(Component child : ((Container) component).getChildren()) {
				collectButtons(child, buttons);
			}
		}
	}
}
