package dal.tool.trace.jmxer.tui.dialog;

import java.util.HashSet;
import java.util.Set;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.DialogWindow;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.screen.Screen;

import dal.tool.trace.jmxer.tui.widget.TuiDialogInputBox;

/**
 * Record UI Dump 뷰 dump 파일 저장/불러오기 경로 입력 다이얼로그.
 *
 */
public final class TuiRecordDumpFileDialog extends DialogWindow {

	private static final int DIALOG_WIDTH_PERCENT = 50;
	private static final int MIN_DIALOG_COLUMNS = 40;
	private static final int HORIZONTAL_PADDING_COLS = 6;
	private static final int MIN_INPUT_COLUMNS = 24;

	private final TextBox pathInput;
	private String result;

	private TuiRecordDumpFileDialog(WindowBasedTextGUI gui, String title, String description, String initialPath) {
		super(title == null ? "" : title);
		enableCenteredHint();

		int inputCols = resolveInputColumns(gui);
		Panel fieldsPanel = new Panel();
		fieldsPanel.setLayoutManager(new GridLayout(1).setLeftMarginSize(1).setRightMarginSize(1));
		TuiDialogLayout.addFormDescription(fieldsPanel, description == null ? "" : description);
		pathInput = new TuiDialogInputBox(new TerminalSize(inputCols, 1));
		pathInput.setText(initialPath == null ? "" : initialPath);
		fieldsPanel.addComponent(pathInput);

		Button okButton = new Button(MessageDialogButton.OK.toString(), new Runnable() {
			@Override
			public void run() {
				String text = pathInput.getText();
				result = text == null ? "" : text;
				close();
			}
		});
		Button cancelButton = new Button(MessageDialogButton.Cancel.toString(), new Runnable() {
			@Override
			public void run() {
				result = null;
				close();
			}
		});

		Panel buttonPanel = new Panel();
		buttonPanel.setLayoutManager(new GridLayout(2).setHorizontalSpacing(1));
		buttonPanel.addComponent(okButton);
		buttonPanel.addComponent(cancelButton);

		Panel mainPanel = new Panel();
		mainPanel.setLayoutManager(new GridLayout(1).setLeftMarginSize(1).setRightMarginSize(1));
		mainPanel.addComponent(fieldsPanel);
		mainPanel.addComponent(new EmptySpace(TerminalSize.ONE));
		buttonPanel.setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.END, GridLayout.Alignment.CENTER, false, false))
			.addTo(mainPanel);
		setComponent(mainPanel);
		TuiDialogEscFocus.install(this);
	}

	public static String show(WindowBasedTextGUI gui, String title, String description, String initialPath) {
		if(gui == null) {
			return null;
		}
		TuiRecordDumpFileDialog dialog = new TuiRecordDumpFileDialog(gui, title, description, initialPath);
		dialog.showDialog(gui);
		return dialog.result;
	}

	@Override
	public Object showDialog(WindowBasedTextGUI textGUI) {
		textGUI.addWindow(this);
		textGUI.getGUIThread().invokeLater(new Runnable() {
			@Override
			public void run() {
				Interactable first = pathInput;
				if(first != null) {
					setFocusedInteractable(first);
					first.takeFocus();
				}
			}
		});
		waitUntilClosed();
		return result;
	}

	private void enableCenteredHint() {
		Set<Window.Hint> hints = new HashSet<Window.Hint>(getHints());
		hints.add(Window.Hint.CENTERED);
		setHints(hints);
	}

	private static int resolveInputColumns(WindowBasedTextGUI gui) {
		TerminalSize term = resolveTerminalSize(gui);
		int termCols = Math.max(MIN_DIALOG_COLUMNS, term.getColumns());
		int maxDialogCols = Math.max(MIN_DIALOG_COLUMNS, termCols * DIALOG_WIDTH_PERCENT / 100);
		int inputCols = maxDialogCols - HORIZONTAL_PADDING_COLS;
		if(inputCols < MIN_INPUT_COLUMNS) {
			inputCols = MIN_INPUT_COLUMNS;
		}
		return inputCols;
	}

	private static TerminalSize resolveTerminalSize(WindowBasedTextGUI gui) {
		try {
			Screen screen = gui.getScreen();
			if(screen != null) {
				TerminalSize ts = screen.getTerminalSize();
				if(ts != null) {
					return ts;
				}
			}
		} catch(Exception e) {}
		return new TerminalSize(120, 40);
	}
}
