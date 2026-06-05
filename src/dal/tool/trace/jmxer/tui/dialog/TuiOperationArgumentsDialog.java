package dal.tool.trace.jmxer.tui.dialog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.management.MBeanParameterInfo;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.DialogWindow;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.screen.Screen;

import dal.tool.trace.jmxer.tui.support.TuiJmxInputValidator;
import dal.tool.util.jmx.JMXHelper;

/**
 * Operation argument input dialog: one field per MBean parameter.
 */
public final class TuiOperationArgumentsDialog extends DialogWindow {

	private static final String VALIDATION_TITLE = "Validation";

	static final int MAX_WIDTH_PERCENT = 80;
	static final int MIN_DIALOG_COLUMNS = 40;
	static final int MIN_INPUT_COLUMNS = 24;
	static final int MAX_INPUT_COLUMNS = 72;
	static final int HORIZONTAL_PADDING_COLS = 6;

	private String[] result;
	private final List<TextBox> inputs = new ArrayList<TextBox>();
	private final WindowBasedTextGUI gui;
	private final MBeanParameterInfo[] signature;

	private TuiOperationArgumentsDialog(WindowBasedTextGUI gui, String operationName, MBeanParameterInfo[] signature) {
		super("Arguments");
		this.gui = gui;
		this.signature = signature;
		enableCenteredHint();

		int inputCols = resolveInputColumns(gui);
		String description = buildDescription(operationName);

		Panel fieldsPanel = new Panel();
		fieldsPanel.setLayoutManager(new GridLayout(1).setLeftMarginSize(1).setRightMarginSize(1));
		TuiDialogLayout.addFormDescription(fieldsPanel, description);

		for(int i = 0; i < signature.length; i++) {
			MBeanParameterInfo param = signature[i];
			String paramLabel = formatParameterLabel(param, i);
			fieldsPanel.addComponent(new Label(paramLabel));
			TextBox input = new TextBox(new TerminalSize(inputCols, 1));
			inputs.add(input);
			fieldsPanel.addComponent(input);
		}

		Button okButton = new Button(MessageDialogButton.OK.toString(), new Runnable() {
			@Override
			public void run() {
				String[] values = collectValues();
				String validationError = TuiJmxInputValidator.validateOperationArguments(signature, values);
				if(validationError != null) {
					TuiDialogs.showMessageDialog(gui, VALIDATION_TITLE, validationError, MessageDialogButton.OK);
					return;
				}
				result = values;
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

	public static String[] show(WindowBasedTextGUI gui, String operationName, MBeanParameterInfo[] signature) {
		if(gui == null || signature == null || signature.length == 0) {
			return null;
		}
		TuiOperationArgumentsDialog dialog = new TuiOperationArgumentsDialog(gui, operationName, signature);
		dialog.showDialog(gui);
		return dialog.result;
	}

	public static String joinOperationArguments(MBeanParameterInfo[] signature, String[] values) {
		return JMXHelper.formatCallArgumentList(signature, values);
	}

	@Override
	public Object showDialog(WindowBasedTextGUI textGUI) {
		textGUI.addWindow(this);
		textGUI.getGUIThread().invokeLater(new Runnable() {
			@Override
			public void run() {
				Interactable first = inputs.isEmpty() ? null : inputs.get(0);
				if(first != null) {
					setFocusedInteractable(first);
					first.takeFocus();
				}
			}
		});
		waitUntilClosed();
		return result;
	}

	private String[] collectValues() {
		String[] values = new String[inputs.size()];
		for(int i = 0; i < inputs.size(); i++) {
			values[i] = inputs.get(i).getText();
		}
		return values;
	}

	private static String buildDescription(String operationName) {
		String name = operationName == null ? "" : operationName.trim();
		if(name.length() == 0) {
			return "Input operation arguments:";
		}
		return "Input arguments for " + name + ":";
	}

	private static String formatParameterLabel(MBeanParameterInfo param, int index) {
		if(param == null) {
			return "Argument " + (index + 1) + ":";
		}
		String name = param.getName();
		if(name == null || name.trim().length() == 0) {
			name = "arg" + (index + 1);
		}
		return name + " (" + JMXHelper.toTypeString(param.getType()) + "):";
	}

	private static int resolveInputColumns(WindowBasedTextGUI gui) {
		TerminalSize term = resolveTerminalSize(gui);
		int termCols = Math.max(MIN_DIALOG_COLUMNS, term.getColumns());
		int maxDialogCols = Math.max(MIN_DIALOG_COLUMNS, termCols * MAX_WIDTH_PERCENT / 100);
		int inputCols = maxDialogCols - HORIZONTAL_PADDING_COLS;
		if(inputCols < MIN_INPUT_COLUMNS) {
			inputCols = MIN_INPUT_COLUMNS;
		}
		if(inputCols > MAX_INPUT_COLUMNS) {
			inputCols = MAX_INPUT_COLUMNS;
		}
		return inputCols;
	}

	private void enableCenteredHint() {
		Set<Window.Hint> hints = new HashSet<Window.Hint>(getHints());
		hints.add(Window.Hint.CENTERED);
		setHints(hints);
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
