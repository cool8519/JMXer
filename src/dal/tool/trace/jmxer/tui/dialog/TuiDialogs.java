package dal.tool.trace.jmxer.tui.dialog;

import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.ListSelectDialog;
import com.googlecode.lanterna.gui2.dialogs.ListSelectDialogBuilder;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogBuilder;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialog;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialogBuilder;

/**
 * TUI 다이얼로그 진입점. 본문 여백·Enter 버튼 활성화·Esc 취소 포커스를 통일한다.
 *
 */
public final class TuiDialogs {

	private TuiDialogs() {}

	public static MessageDialogButton showMessageDialog(
			WindowBasedTextGUI gui,
			String title,
			String text,
			MessageDialogButton... buttons) {
		MessageDialogBuilder builder = new MessageDialogBuilder()
			.setTitle(title)
			.setText(TuiDialogText.body(text));
		if(buttons == null || buttons.length == 0) {
			builder.addButton(MessageDialogButton.OK);
		} else {
			for(int i = 0; i < buttons.length; i++) {
				builder.addButton(buttons[i]);
			}
		}
		MessageDialog dialog = builder.build();
		TuiDialogEscFocus.install(dialog);
		return dialog.showDialog(gui);
	}

	@SafeVarargs
	public static <T> T showListSelectDialog(
			WindowBasedTextGUI gui,
			String title,
			String description,
			T... items) {
		ListSelectDialog<T> dialog = new ListSelectDialogBuilder<T>()
			.setTitle(title)
			.setDescription(TuiDialogText.body(description))
			.addListItems(items)
			.build();
		TuiDialogEscFocus.install(dialog);
		return dialog.showDialog(gui);
	}

	public static String showTextInputDialog(
			WindowBasedTextGUI gui,
			String title,
			String description,
			String initialContent) {
		TextInputDialog dialog = new TextInputDialogBuilder()
			.setTitle(title)
			.setDescription(TuiDialogText.body(description))
			.setInitialContent(initialContent)
			.build();
		TuiDialogEscFocus.install(dialog);
		return dialog.showDialog(gui);
	}
}
