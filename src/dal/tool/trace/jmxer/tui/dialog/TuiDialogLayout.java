package dal.tool.trace.jmxer.tui.dialog;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;

/**
 * Shared layout helpers for TUI dialogs.
 */
final class TuiDialogLayout {

	private TuiDialogLayout() {}

	/**
	 * Adds a description label and one blank row before an input control block.
	 * Trailing {@code \n} in label text does not reserve a row in {@link com.googlecode.lanterna.gui2.GridLayout}.
	 */
	static void addFormDescription(Panel panel, String description) {
		panel.addComponent(new Label(TuiDialogText.body(description)));
		panel.addComponent(new EmptySpace(TerminalSize.ONE));
	}
}
