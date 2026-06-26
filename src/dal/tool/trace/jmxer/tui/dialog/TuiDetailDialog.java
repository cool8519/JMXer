package dal.tool.trace.jmxer.tui.dialog;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.DialogWindow;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.screen.Screen;

import dal.tool.trace.jmxer.tui.output.OutputLineReflow;
import dal.tool.trace.jmxer.tui.support.TuiHelpTextFormat;

/**
 * Info 상세 팝업. MessageDialog와 동일한 레이아웃(본문·스크롤·버튼 우측 정렬)이며,
 * 터미널 너비의 80%까지 내용에 맞게 조절한다.
 *
 */
public final class TuiDetailDialog extends DialogWindow {
	static final int MAX_WIDTH_PERCENT = 80;
	static final int MAX_HEIGHT_PERCENT = 80;
	static final int MIN_DIALOG_COLUMNS = 40;
	static final int MIN_TEXT_ROWS = 3;
	static final int HORIZONTAL_PADDING_COLS = 6;
	static final int VERTICAL_CHROME_ROWS = 4;
	static final int BODY_VERTICAL_PADDING_ROWS = 1;
	static final int TEXTBOX_CARET_COLUMN_RESERVE = 1;
	static final int HELP_MAX_CONTENT_COLUMNS = 120;
	static final String HELP_CONTINUATION_INDENT = TuiHelpTextFormat.ITEM_CONTINUATION_INDENT;

	private final Button okButton;

	private TuiDetailDialog(WindowBasedTextGUI gui, String title, String text, boolean helpLayout) {
		super(title == null ? "Detail" : title);
		enableCenteredHint();

		TerminalSize term = resolveTerminalSize(gui);
		int termCols = Math.max(MIN_DIALOG_COLUMNS, term.getColumns());
		int termRows = Math.max(MIN_TEXT_ROWS, term.getRows());

		int maxDialogCols = Math.max(MIN_DIALOG_COLUMNS, termCols * MAX_WIDTH_PERCENT / 100);
		int maxTextBoxCols = Math.max(16, maxDialogCols - HORIZONTAL_PADDING_COLS);
		int maxWrapCols = Math.max(16, maxTextBoxCols - TEXTBOX_CARET_COLUMN_RESERVE);
		if(helpLayout) {
			maxWrapCols = Math.min(maxWrapCols, HELP_MAX_CONTENT_COLUMNS);
		}

		List<String> lines = helpLayout
			? OutputLineReflow.formatHelpWithinMaxColumnsWithIndent(text, maxWrapCols, HELP_CONTINUATION_INDENT)
			: OutputLineReflow.formatWithinMaxColumns(text, maxWrapCols);
		int contentWidth = OutputLineReflow.maxDisplayWidth(lines);
		int textBoxCols;
		if(contentWidth <= maxWrapCols) {
			textBoxCols = Math.max(16, Math.min(maxTextBoxCols, contentWidth + TEXTBOX_CARET_COLUMN_RESERVE));
		} else {
			textBoxCols = maxTextBoxCols;
		}

		int maxTextRows = Math.max(MIN_TEXT_ROWS, termRows * MAX_HEIGHT_PERCENT / 100 - VERTICAL_CHROME_ROWS - BODY_VERTICAL_PADDING_ROWS);
		int lineCount = Math.max(1, lines.size());
		boolean scrollBody = lineCount > maxTextRows;
		int bodyRows = scrollBody ? maxTextRows : lineCount;

		String displayText = OutputLineReflow.joinLines(lines);

		okButton = new Button(MessageDialogButton.OK.toString(), new Runnable() {
			@Override
			public void run() {
				close();
			}
		});

		Panel buttonPanel = new Panel();
		buttonPanel.setLayoutManager(new GridLayout(1).setHorizontalSpacing(1));
		buttonPanel.addComponent(okButton);

		Panel mainPanel = new Panel();
		mainPanel.setLayoutManager(new GridLayout(1).setLeftMarginSize(1).setRightMarginSize(1));
		mainPanel.addComponent(createBodyComponent(displayText, scrollBody, textBoxCols, bodyRows));
		mainPanel.addComponent(new EmptySpace(TerminalSize.ONE));
		buttonPanel.setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.END, GridLayout.Alignment.CENTER, false, false))
			.addTo(mainPanel);
		setComponent(mainPanel);
		TuiDialogEscFocus.install(this);
	}

	public static void show(WindowBasedTextGUI gui, String title, String text) {
		if(gui == null) {
			return;
		}
		new TuiDetailDialog(gui, title, TuiDialogText.body(text), false).showDialog(gui);
	}

	public static void showHelp(WindowBasedTextGUI gui, String title, String text) {
		if(gui == null) {
			return;
		}
		new TuiDetailDialog(gui, title, TuiDialogText.body(text), true).showDialog(gui);
	}

	@Override
	public Object showDialog(WindowBasedTextGUI textGUI) {
		textGUI.addWindow(this);
		textGUI.getGUIThread().invokeLater(new Runnable() {
			@Override
			public void run() {
				setFocusedInteractable(okButton);
				okButton.takeFocus();
			}
		});
		waitUntilClosed();
		return null;
	}

	private static Component createBodyComponent(String displayText, boolean scrollBody, int textBoxCols, int bodyRows) {
		if(scrollBody) {
			TextBox body = new TextBox(new TerminalSize(textBoxCols, bodyRows), displayText, TextBox.Style.MULTI_LINE);
			body.setReadOnly(true);
			body.setHorizontalFocusSwitching(false);
			resetTextBoxView(body);
			return body;
		}
		return new Label(displayText);
	}

	private void enableCenteredHint() {
		Set<Window.Hint> hints = new HashSet<Window.Hint>(getHints());
		hints.add(Window.Hint.CENTERED);
		setHints(hints);
	}

	private static void resetTextBoxView(TextBox box) {
		TextBox.TextBoxRenderer renderer = box.getRenderer();
		if(renderer instanceof TextBox.DefaultTextBoxRenderer) {
			TextBox.DefaultTextBoxRenderer defaultRenderer = (TextBox.DefaultTextBoxRenderer) renderer;
			defaultRenderer.setViewTopLeft(TerminalPosition.TOP_LEFT_CORNER);
		}
		if(box.getLineCount() > 0) {
			box.setCaretPosition(0, 0);
		}
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
