package dal.tool.trace.jmxer.tui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Container;
import com.googlecode.lanterna.gui2.LocalizedString;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowListener;
import com.googlecode.lanterna.gui2.dialogs.DialogWindow;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

/**
 * On Escape, moves focus to the dialog cancel/dismiss button (Cancel, No, or the only button).
 */
final class TuiDialogEscFocus {

	private TuiDialogEscFocus() {}

	static void install(final DialogWindow window) {
		if(window == null) {
			return;
		}
		window.setCloseWindowWithEscape(false);
		window.addWindowListener(new WindowListener() {
			@Override
			public void onInput(Window pane, KeyStroke keyStroke, AtomicBoolean deliverEvent) {
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
				// no-op
			}

			@Override
			public void onResized(Window window, com.googlecode.lanterna.TerminalSize oldSize, com.googlecode.lanterna.TerminalSize newSize) {
				// no-op
			}

			@Override
			public void onMoved(Window window, com.googlecode.lanterna.TerminalPosition oldPosition, com.googlecode.lanterna.TerminalPosition newPosition) {
				// no-op
			}
		});
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
			|| equalsIgnoreCase(trimmed, LocalizedString.Cancel.toString())
			|| equalsIgnoreCase(trimmed, LocalizedString.No.toString());
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
