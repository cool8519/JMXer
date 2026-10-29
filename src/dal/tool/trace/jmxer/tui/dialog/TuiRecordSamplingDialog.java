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

import dal.tool.trace.jmxer.cli.command.JmxThreadCommand;
import dal.tool.trace.jmxer.tui.support.TuiRecordUiKeys;
import dal.tool.trace.jmxer.tui.widget.TuiDialogInputBox;
import dal.tool.util.NumberUtil;

/**
 * Record UI 스택트레이스 샘플링 간격·시간 입력 다이얼로그.
 *
 */
public final class TuiRecordSamplingDialog extends DialogWindow {

	private static final String VALIDATION_TITLE = "Validation";
	private static final long DEFAULT_INTERVAL_MS = 500L;
	private static final long DEFAULT_TIME_MS = 0L;

	private final WindowBasedTextGUI gui;
	private final TextBox intervalInput;
	private final TextBox timeInput;
	private SamplingParams result;

	private TuiRecordSamplingDialog(WindowBasedTextGUI gui, int threadCount, String threadExpr) {
		super(TuiRecordUiKeys.SAMPLING_DIALOG_TITLE);
		this.gui = gui;
		enableCenteredHint();

		int inputCols = resolveInputColumns(gui);
		Panel fieldsPanel = new Panel();
		fieldsPanel.setLayoutManager(new GridLayout(1).setLeftMarginSize(1).setRightMarginSize(1));
		TuiDialogLayout.addFormDescription(fieldsPanel, TuiRecordUiKeys.formatSamplingTargetLine(threadCount, threadExpr));
		TuiDialogLayout.addFormFieldTitle(fieldsPanel, TuiRecordUiKeys.SAMPLING_INTERVAL_DESCRIPTION);
		intervalInput = new TuiDialogInputBox(new TerminalSize(inputCols, 1));
		intervalInput.setText(String.valueOf(DEFAULT_INTERVAL_MS));
		fieldsPanel.addComponent(intervalInput);
		TuiDialogLayout.addFormFieldGap(fieldsPanel);
		TuiDialogLayout.addFormFieldTitle(fieldsPanel, TuiRecordUiKeys.SAMPLING_TIME_DESCRIPTION);
		timeInput = new TuiDialogInputBox(new TerminalSize(inputCols, 1));
		timeInput.setText(String.valueOf(DEFAULT_TIME_MS));
		fieldsPanel.addComponent(timeInput);

		Button okButton = new Button(MessageDialogButton.OK.toString(), new Runnable() {
			@Override
			public void run() {
				SamplingParams parsed = parseInputs();
				if(parsed == null) {
					return;
				}
				result = parsed;
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

	public static SamplingParams show(WindowBasedTextGUI gui, int threadCount, String threadExpr) {
		if(gui == null || threadExpr == null || threadExpr.trim().length() == 0) {
			return null;
		}
		boolean dynamicTarget = JmxThreadCommand.isDynamicThreadExpression(threadExpr);
		if(threadCount < 0 || (!dynamicTarget && threadCount < 1)) {
			return null;
		}
		TuiRecordSamplingDialog dialog = new TuiRecordSamplingDialog(gui, threadCount, threadExpr);
		dialog.showDialog(gui);
		return dialog.result;
	}

	private SamplingParams parseInputs() {
		String intervalText = intervalInput.getText();
		if(intervalText == null) {
			intervalText = "";
		}
		long intervalMs = DEFAULT_INTERVAL_MS;
		if(intervalText.trim().length() > 0) {
			if(!NumberUtil.isNumber(intervalText.trim())) {
				TuiDialogs.showMessageDialog(gui, VALIDATION_TITLE,
					"Invalid value for interval. The default of 500ms will be applied.",
					MessageDialogButton.OK);
				return null;
			}
			intervalMs = Long.parseLong(intervalText.trim());
		}

		String timeText = timeInput.getText();
		if(timeText == null) {
			timeText = "";
		}
		long timeMs = DEFAULT_TIME_MS;
		if(timeText.trim().length() > 0) {
			if(!NumberUtil.isNumber(timeText.trim()) || Long.parseLong(timeText.trim()) < 0L) {
				TuiDialogs.showMessageDialog(gui, VALIDATION_TITLE,
					"Invalid value for time. The default of 0 will be applied.",
					MessageDialogButton.OK);
				return null;
			}
			timeMs = Long.parseLong(timeText.trim());
		}
		return new SamplingParams(intervalMs, timeMs);
	}

	@Override
	public Object showDialog(WindowBasedTextGUI textGUI) {
		textGUI.addWindow(this);
		textGUI.getGUIThread().invokeLater(new Runnable() {
			@Override
			public void run() {
				Interactable first = intervalInput;
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
		int termCols = Math.max(40, term.getColumns());
		int maxDialogCols = Math.max(40, termCols * 80 / 100);
		int inputCols = maxDialogCols - 6;
		if(inputCols < 24) {
			inputCols = 24;
		}
		if(inputCols > 72) {
			inputCols = 72;
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

	/**
	 * 스택트레이스 샘플링 간격·시간 입력 결과.
	 *
	 */
	public static final class SamplingParams {
		public final long intervalMs;
		public final long timeMs;

		public SamplingParams(long intervalMs, long timeMs) {
			this.intervalMs = intervalMs;
			this.timeMs = timeMs;
		}
	}
}
