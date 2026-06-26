package dal.tool.trace.jmxer.tui.dialog;

import java.util.HashSet;
import java.util.Set;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.DialogWindow;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.screen.Screen;

import dal.tool.trace.jmxer.cli.data.RecordResult;
import dal.tool.trace.jmxer.tui.support.RecordViewExpressionHelper;
import dal.tool.trace.jmxer.tui.support.TuiRecordUiKeys;
import dal.tool.trace.jmxer.tui.widget.TuiDialogInputBox;
import dal.tool.trace.jmxer.tui.widget.TuiDialogSelectionList;
import dal.tool.trace.jmxer.tui.widget.TuiRecordExpressionTypeList;

/**
 * Record UI Dump 뷰에서 RECORD VIEW METHOD/THREAD/STACK/SEARCH 입력을 받는 다이얼로그.
 *
 */
public final class TuiRecordViewDialog extends DialogWindow {

	/**
	 * RECORD VIEW 유형.
	 *
	 */
	public enum ViewType {
		METHOD,
		THREAD,
		STACK,
		SEARCH
	}

	private static final int DIALOG_WIDTH_PERCENT = 80;
	private static final int MIN_DIALOG_COLUMNS = 40;
	private static final int HORIZONTAL_PADDING_COLS = 6;
	private static final int MIN_INPUT_COLUMNS = 24;
	private static final int MAX_INPUT_COLUMNS = 72;
	private static final int TYPE_LIST_COLUMNS = 28;

	private final WindowBasedTextGUI gui;
	private final ViewType viewType;
	private final RecordViewExpressionHelper expressionHelper;

	private TuiDialogInputBox rangeInput;
	private TuiRecordExpressionTypeList rangeTypeList;
	private TuiDialogInputBox pointInput;
	private TuiRecordExpressionTypeList pointTypeList;
	private TuiDialogInputBox nameInput;
	private TuiDialogSelectionList mergeList;

	private ViewParams result;

	private TuiRecordViewDialog(WindowBasedTextGUI gui, ViewType viewType, RecordResult recordResult) {
		super(resolveTitle(viewType));
		this.gui = gui;
		this.viewType = viewType;
		this.expressionHelper = new RecordViewExpressionHelper(recordResult);
		enableCenteredHint();

		int inputCols = resolveInputColumns(gui);
		Panel fieldsPanel = new Panel();
		fieldsPanel.setLayoutManager(new GridLayout(1).setLeftMarginSize(1).setRightMarginSize(1));
		TuiDialogLayout.addFormDescription(fieldsPanel, resolveDescription(viewType));
		buildFields(fieldsPanel, inputCols);

		Button okButton = new Button(MessageDialogButton.OK.toString(), new Runnable() {
			@Override
			public void run() {
				applyOk();
			}
		});
		if(mergeList != null) {
			mergeList.setEnterFocusTarget(okButton);
		}
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

	public static ViewParams show(WindowBasedTextGUI gui, ViewType viewType, RecordResult recordResult) {
		if(gui == null || viewType == null) {
			return null;
		}
		TuiRecordViewDialog dialog = new TuiRecordViewDialog(gui, viewType, recordResult);
		dialog.showDialog(gui);
		return dialog.result;
	}

	private void buildFields(Panel fieldsPanel, int inputCols) {
		if(viewType == ViewType.METHOD || viewType == ViewType.THREAD) {
			addRangeExpressionSection(fieldsPanel, inputCols);
		}
		if(viewType == ViewType.THREAD) {
			addMergeOptionSection(fieldsPanel);
		}
		if(viewType == ViewType.STACK) {
			addPointExpressionSection(fieldsPanel, inputCols);
		}
		if(viewType == ViewType.SEARCH) {
			addNameExpressionSection(fieldsPanel, inputCols);
		}
	}

	private void addRangeExpressionSection(Panel fieldsPanel, int inputCols) {
		TuiDialogLayout.addFormFieldSectionHeader(
			fieldsPanel,
			TuiRecordUiKeys.VIEW_RANGE_TITLE,
			TuiRecordUiKeys.VIEW_RANGE_HINT
		);
		rangeInput = new TuiDialogInputBox(new TerminalSize(inputCols, 1));
		rangeTypeList = new TuiRecordExpressionTypeList(
			new TerminalSize(TYPE_LIST_COLUMNS, TuiRecordUiKeys.VIEW_EXPRESSION_TYPE_LIST_ROWS),
			rangeInput,
			expressionHelper,
			true
		);
		TuiDialogLayout.addCaptionWithControl(fieldsPanel, TuiRecordUiKeys.VIEW_EXPRESSION_TYPE_CAPTION, rangeTypeList);
		TuiDialogLayout.addCaptionWithControl(fieldsPanel, TuiRecordUiKeys.VIEW_EXPRESSION_VALUE_CAPTION, rangeInput);
		TuiDialogLayout.addFormFieldGap(fieldsPanel);
	}

	private void addPointExpressionSection(Panel fieldsPanel, int inputCols) {
		TuiDialogLayout.addFormFieldSectionHeader(
			fieldsPanel,
			TuiRecordUiKeys.VIEW_STACK_POINT_TITLE,
			TuiRecordUiKeys.VIEW_STACK_POINT_HINT
		);
		pointInput = new TuiDialogInputBox(new TerminalSize(inputCols, 1));
		pointTypeList = new TuiRecordExpressionTypeList(
			new TerminalSize(TYPE_LIST_COLUMNS, TuiRecordUiKeys.VIEW_EXPRESSION_TYPE_LIST_ROWS),
			pointInput,
			expressionHelper,
			false
		);
		TuiDialogLayout.addCaptionWithControl(fieldsPanel, TuiRecordUiKeys.VIEW_EXPRESSION_POINT_TYPE_CAPTION, pointTypeList);
		TuiDialogLayout.addCaptionWithControl(fieldsPanel, TuiRecordUiKeys.VIEW_EXPRESSION_POINT_VALUE_CAPTION, pointInput);
		TuiDialogLayout.addFormFieldGap(fieldsPanel);
	}

	private void addNameExpressionSection(Panel fieldsPanel, int inputCols) {
		TuiDialogLayout.addFormFieldSectionHeader(
			fieldsPanel,
			TuiRecordUiKeys.VIEW_SEARCH_NAME_TITLE,
			TuiRecordUiKeys.VIEW_SEARCH_NAME_HINT
		);
		nameInput = new TuiDialogInputBox(new TerminalSize(inputCols, 1));
		TuiDialogLayout.addCaptionWithControl(fieldsPanel, TuiRecordUiKeys.VIEW_SEARCH_VALUE_CAPTION, nameInput);
		TuiDialogLayout.addFormFieldGap(fieldsPanel);
	}

	private void addMergeOptionSection(Panel fieldsPanel) {
		TuiDialogLayout.addFormFieldSectionHeader(
			fieldsPanel,
			TuiRecordUiKeys.VIEW_THREAD_MERGE_TITLE,
			TuiRecordUiKeys.VIEW_THREAD_MERGE_HINT
		);
		mergeList = new TuiDialogSelectionList();
		mergeList.setPreferredSize(new TerminalSize(TYPE_LIST_COLUMNS, TuiRecordUiKeys.VIEW_MERGE_LIST_ROWS));
		mergeList.addItem(TuiRecordUiKeys.VIEW_THREAD_MERGE_SPLIT_LABEL, new Runnable() {
			@Override
			public void run() {}
		});
		mergeList.addItem(TuiRecordUiKeys.VIEW_THREAD_MERGE_MERGE_LABEL, new Runnable() {
			@Override
			public void run() {}
		});
		TuiDialogLayout.addCaptionWithControl(fieldsPanel, TuiRecordUiKeys.VIEW_MERGE_SELECTION_CAPTION, mergeList);
		TuiDialogLayout.addFormFieldGap(fieldsPanel);
	}

	private void applyOk() {
		String expression = "";
		boolean mergeThreads = false;

		if(viewType == ViewType.METHOD || viewType == ViewType.THREAD) {
			String rangeText = rangeInput == null ? "" : rangeInput.getText();
			String validationError = expressionHelper.validateRangeExpression(rangeText);
			if(validationError != null) {
				showValidationError(validationError, rangeInput);
				return;
			}
			expression = normalizeExpression(rangeText);
		}
		if(viewType == ViewType.STACK) {
			String pointText = pointInput == null ? "" : pointInput.getText();
			String validationError = expressionHelper.validatePointExpression(pointText);
			if(validationError != null) {
				showValidationError(validationError, pointInput);
				return;
			}
			expression = normalizeExpression(pointText);
		}
		if(viewType == ViewType.SEARCH) {
			String nameText = nameInput == null ? "" : nameInput.getText();
			String validationError = expressionHelper.validateNameExpression(nameText);
			if(validationError != null) {
				showValidationError(validationError, nameInput);
				return;
			}
			expression = normalizeExpression(nameText);
		}
		if(viewType == ViewType.THREAD && mergeList != null) {
			mergeThreads = mergeList.getSelectedIndex() == 1;
		}

		result = new ViewParams(expression, mergeThreads);
		close();
	}

	private void showValidationError(String message, Interactable focusTarget) {
		TuiDialogs.showMessageDialog(gui, TuiRecordUiKeys.VIEW_VALIDATION_TITLE, message, MessageDialogButton.OK);
		if(focusTarget != null) {
			setFocusedInteractable(focusTarget);
			focusTarget.takeFocus();
		}
	}

	private static String normalizeExpression(String raw) {
		if(raw == null) {
			return "";
		}
		return raw.trim();
	}

	@Override
	public Object showDialog(WindowBasedTextGUI textGUI) {
		textGUI.addWindow(this);
		textGUI.getGUIThread().invokeLater(new Runnable() {
			@Override
			public void run() {
				Interactable first = resolveInitialFocus();
				if(first != null) {
					setFocusedInteractable(first);
					first.takeFocus();
				}
			}
		});
		waitUntilClosed();
		return result;
	}

	private Interactable resolveInitialFocus() {
		if(viewType == ViewType.SEARCH) {
			return nameInput;
		}
		if(viewType == ViewType.STACK) {
			return pointTypeList;
		}
		return rangeTypeList;
	}

	private static String resolveTitle(ViewType viewType) {
		if(viewType == ViewType.METHOD) {
			return TuiRecordUiKeys.VIEW_METHOD_DIALOG_TITLE;
		}
		if(viewType == ViewType.THREAD) {
			return TuiRecordUiKeys.VIEW_THREAD_DIALOG_TITLE;
		}
		if(viewType == ViewType.STACK) {
			return TuiRecordUiKeys.VIEW_STACK_DIALOG_TITLE;
		}
		return TuiRecordUiKeys.VIEW_SEARCH_DIALOG_TITLE;
	}

	private static String resolveDescription(ViewType viewType) {
		if(viewType == ViewType.METHOD) {
			return TuiRecordUiKeys.VIEW_METHOD_DIALOG_DESCRIPTION;
		}
		if(viewType == ViewType.THREAD) {
			return TuiRecordUiKeys.VIEW_THREAD_DIALOG_DESCRIPTION;
		}
		if(viewType == ViewType.STACK) {
			return TuiRecordUiKeys.VIEW_STACK_DIALOG_DESCRIPTION;
		}
		return TuiRecordUiKeys.VIEW_SEARCH_DIALOG_DESCRIPTION;
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
		if(inputCols > MAX_INPUT_COLUMNS) {
			inputCols = MAX_INPUT_COLUMNS;
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
	 * RECORD VIEW 다이얼로그 입력 결과.
	 *
	 */
	public static final class ViewParams {
		public final String expression;
		public final boolean mergeThreads;

		public ViewParams(String expression, boolean mergeThreads) {
			this.expression = expression == null ? "" : expression;
			this.mergeThreads = mergeThreads;
		}
	}
}
