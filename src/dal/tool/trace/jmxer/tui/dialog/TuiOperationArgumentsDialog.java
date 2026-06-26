package dal.tool.trace.jmxer.tui.dialog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

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

import dal.tool.trace.jmxer.knowledge.KnowledgeConstant;
import dal.tool.trace.jmxer.knowledge.MBeanKnowledgeResolver;
import dal.tool.trace.jmxer.tui.support.TuiJmxInputValidator;
import dal.tool.trace.jmxer.tui.widget.TuiDialogInputBox;
import dal.tool.util.jmx.JMXHelper;

/**
 * MBean 오퍼레이션 인자 입력 다이얼로그. 파라미터마다 입력 필드를 제공한다.
 *
 */
public final class TuiOperationArgumentsDialog extends DialogWindow {

	private static final String DIALOG_BASE_TITLE = "Arguments";
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

	private TuiOperationArgumentsDialog(WindowBasedTextGUI textGUI, String objectName, MBeanInfo mbeanInfo,
			MBeanOperationInfo operation, MBeanParameterInfo[] signature) {
		super(resolveDialogTitle(objectName, mbeanInfo, operation));
		this.gui = textGUI;
		this.signature = signature;
		enableCenteredHint();

		int inputCols = resolveInputColumns(textGUI);
		String description = buildDescription(operation);
		MBeanKnowledgeResolver resolver = MBeanKnowledgeResolver.getInstance();
		ObjectName oName = parseObjectName(objectName);

		Panel fieldsPanel = new Panel();
		fieldsPanel.setLayoutManager(new GridLayout(1).setLeftMarginSize(1).setRightMarginSize(1));
		TuiDialogLayout.addFormDescription(fieldsPanel, description);

		for(int i = 0; i < signature.length; i++) {
			MBeanParameterInfo param = signature[i];
			String paramLabel = formatParameterLabel(resolver, oName, mbeanInfo, operation, param, i);
			fieldsPanel.addComponent(new Label(paramLabel));
			String paramDescription = resolver.resolveKnowledgeParamDescription(oName, mbeanInfo, operation, i);
			if(hasText(paramDescription)) {
				TuiDialogLayout.addFormFieldHint(fieldsPanel, paramDescription);
			}
			String example = resolver.resolveParamExample(oName, mbeanInfo, operation, i);
			if(hasText(example)) {
				TuiDialogLayout.addFormFieldHint(fieldsPanel, "Example: " + example);
			}
			TextBox input = new TuiDialogInputBox(new TerminalSize(inputCols, 1));
			if(hasText(example)) {
				input.setText(example);
			}
			inputs.add(input);
			fieldsPanel.addComponent(input);
			TuiDialogLayout.addFormFieldGap(fieldsPanel);
		}

		Button okButton = new Button(MessageDialogButton.OK.toString(), new Runnable() {
			@Override
			public void run() {
				String[] values = collectValues();
				String validationError = TuiJmxInputValidator.validateOperationArguments(TuiOperationArgumentsDialog.this.signature, values);
				if(validationError != null) {
					TuiDialogs.showMessageDialog(TuiOperationArgumentsDialog.this.gui, VALIDATION_TITLE, validationError, MessageDialogButton.OK);
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

	public static String[] show(WindowBasedTextGUI gui, MBeanServerConnection mbeanConn, String objectName, MBeanOperationInfo operation) {
		if(gui == null || operation == null) {
			return null;
		}
		MBeanParameterInfo[] signature = operation.getSignature();
		if(signature == null || signature.length == 0) {
			return null;
		}
		MBeanInfo mbeanInfo = null;
		if(mbeanConn != null && objectName != null) {
			try {
				mbeanInfo = mbeanConn.getMBeanInfo(new ObjectName(objectName));
			} catch(Exception e) {
				mbeanInfo = null;
			}
		}
		TuiOperationArgumentsDialog dialog = new TuiOperationArgumentsDialog(gui, objectName, mbeanInfo, operation, signature);
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

	private static String resolveDialogTitle(String objectName, MBeanInfo mbeanInfo, MBeanOperationInfo operation) {
		ObjectName oName = parseObjectName(objectName);
		boolean knowledgeUsed = false;
		if(oName != null && operation != null) {
			knowledgeUsed = MBeanKnowledgeResolver.getInstance().hasKnowledgeContributionForFeature(oName, mbeanInfo, operation);
		}
		return KnowledgeConstant.formatTuiDetailTitle(DIALOG_BASE_TITLE, knowledgeUsed);
	}

	private static String buildDescription(MBeanOperationInfo operation) {
		if(operation == null) {
			return "Input operation arguments:";
		}
		String name = operation.getName();
		if(name == null || name.trim().length() == 0) {
			return "Input operation arguments:";
		}
		return "Input arguments for " + name + ":";
	}

	private static String formatParameterLabel(MBeanKnowledgeResolver resolver, ObjectName objectName, MBeanInfo mbeanInfo,
			MBeanOperationInfo operation, MBeanParameterInfo param, int index) {
		if(param == null) {
			return "Argument " + (index + 1) + ":";
		}
		String name = resolver.resolveParamName(objectName, mbeanInfo, operation, index);
		if(name == null || name.trim().length() == 0) {
			name = "arg" + (index + 1);
		}
		return name + " (" + JMXHelper.toTypeString(param.getType()) + "):";
	}

	private static ObjectName parseObjectName(String objectName) {
		if(objectName == null || objectName.trim().length() == 0) {
			return null;
		}
		try {
			return new ObjectName(objectName.trim());
		} catch(Exception e) {
			return null;
		}
	}

	private static boolean hasText(String value) {
		return value != null && value.trim().length() > 0;
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
