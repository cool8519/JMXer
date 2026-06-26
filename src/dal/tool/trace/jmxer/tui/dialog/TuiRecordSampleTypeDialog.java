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

import dal.tool.trace.jmxer.tui.support.TuiRecordUiKeys;
import dal.tool.trace.jmxer.tui.widget.DialogArrowListItemRenderer;
import dal.tool.trace.jmxer.tui.widget.TuiDialogActionListBox;

/**
 * Record UI Live 뷰에서 스택트레이스/리소스 샘플링 유형을 선택하는 다이얼로그.
 *
 */
public final class TuiRecordSampleTypeDialog extends DialogWindow {

	/**
	 * 샘플링 유형.
	 *
	 */
	public enum SampleType {
		STACKTRACE,
		RESOURCE
	}

	private static final int LIST_COLUMNS = 28;
	private static final int LIST_ROWS = 2;

	private final TuiDialogActionListBox typeList;
	private SampleType result;

	private TuiRecordSampleTypeDialog() {
		super(TuiRecordUiKeys.SAMPLE_TYPE_DIALOG_TITLE);
		enableCenteredHint();

		Panel fieldsPanel = new Panel();
		fieldsPanel.setLayoutManager(new GridLayout(1).setLeftMarginSize(1).setRightMarginSize(1));
		TuiDialogLayout.addFormDescription(fieldsPanel, TuiRecordUiKeys.SAMPLE_TYPE_DIALOG_DESCRIPTION);

		typeList = new TuiDialogActionListBox(new TerminalSize(LIST_COLUMNS, LIST_ROWS));
		typeList.setListItemRenderer(new DialogArrowListItemRenderer());
		typeList.addItem(TuiRecordUiKeys.SAMPLE_TYPE_STACKTRACE_LABEL, new Runnable() {
			@Override
			public void run() {
				applySelection(SampleType.STACKTRACE);
			}
		});
		typeList.addItem(TuiRecordUiKeys.SAMPLE_TYPE_RESOURCE_LABEL, new Runnable() {
			@Override
			public void run() {
				applySelection(SampleType.RESOURCE);
			}
		});
		fieldsPanel.addComponent(typeList);

		Button cancelButton = new Button(MessageDialogButton.Cancel.toString(), new Runnable() {
			@Override
			public void run() {
				result = null;
				close();
			}
		});

		Panel buttonPanel = new Panel();
		buttonPanel.setLayoutManager(new GridLayout(1).setHorizontalSpacing(1));
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

	public static SampleType show(WindowBasedTextGUI gui) {
		if(gui == null) {
			return null;
		}
		TuiRecordSampleTypeDialog dialog = new TuiRecordSampleTypeDialog();
		dialog.showDialog(gui);
		return dialog.result;
	}

	private void applySelection(SampleType type) {
		result = type;
		close();
	}

	@Override
	public Object showDialog(WindowBasedTextGUI textGUI) {
		textGUI.addWindow(this);
		textGUI.getGUIThread().invokeLater(new Runnable() {
			@Override
			public void run() {
				Interactable first = typeList;
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
}
