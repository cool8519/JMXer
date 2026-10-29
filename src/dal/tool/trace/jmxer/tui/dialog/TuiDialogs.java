package dal.tool.trace.jmxer.tui.dialog;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TerminalTextUtils;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.ListSelectDialog;
import com.googlecode.lanterna.gui2.dialogs.ListSelectDialogBuilder;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogBuilder;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialog;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialogBuilder;
import com.googlecode.lanterna.screen.Screen;

/**
 * TUI 다이얼로그 진입점. 본문 여백·Enter 버튼 활성화·Esc 취소 포커스를 통일한다.
 *
 */
public final class TuiDialogs {

	private static final int MIN_TEXT_INPUT_COLUMNS = 40;
	private static final int MAX_TEXT_INPUT_COLUMNS = 72;

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
		String safeContent = initialContent == null ? "" : initialContent;
		TextInputDialogBuilder builder = new TextInputDialogBuilder()
			.setTitle(title)
			.setDescription(TuiDialogText.body(description))
			.setInitialContent(safeContent);
		if(safeContent.indexOf('\n') < 0) {
			builder.setTextBoxSize(new TerminalSize(resolveSingleLineInputColumns(gui, safeContent), 1));
		}
		TextInputDialog dialog = builder.build();
		TuiDialogEscFocus.install(dialog);
		return dialog.showDialog(gui);
	}

	private static int resolveSingleLineInputColumns(WindowBasedTextGUI gui, String initialContent) {
		int termCols = 120;
		if(gui != null) {
			try {
				Screen screen = gui.getScreen();
				if(screen != null && screen.getTerminalSize() != null) {
					termCols = screen.getTerminalSize().getColumns();
				}
			} catch(Exception e) {}
		}
		int maxCols = Math.max(24, Math.min(MAX_TEXT_INPUT_COLUMNS, termCols * 80 / 100 - 6));
		int minCols = Math.min(MIN_TEXT_INPUT_COLUMNS, maxCols);
		int contentCols = TerminalTextUtils.getColumnWidth(initialContent) + 1;
		if(contentCols < minCols) {
			return minCols;
		}
		if(contentCols > maxCols) {
			return maxCols;
		}
		return contentCols;
	}
}
