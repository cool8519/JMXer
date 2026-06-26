package dal.tool.trace.jmxer.tui.dialog;

import java.util.HashSet;
import java.util.Set;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.DialogWindow;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;

import dal.tool.trace.jmxer.tui.support.TuiThreadUiKeys;
import dal.tool.trace.jmxer.tui.widget.TuiDialogInputBox;

/**
 * Thread UI 모드에서 수동→자동 갱신 전환 시 갱신 주기를 입력하는 다이얼로그.
 *
 */
public final class TuiThreadRefreshDialog extends DialogWindow {

	private Integer result;
	private final WindowBasedTextGUI gui;
	private final TextBox intervalInput;

	private TuiThreadRefreshDialog(WindowBasedTextGUI textGUI, int initialIntervalSeconds) {
		super(TuiThreadUiKeys.REFRESH_DIALOG_TITLE);
		this.gui = textGUI;
		enableCenteredHint();

		Panel fieldsPanel = new Panel();
		fieldsPanel.setLayoutManager(new GridLayout(1).setLeftMarginSize(1).setRightMarginSize(1));
		TuiDialogLayout.addFormDescription(fieldsPanel, TuiThreadUiKeys.REFRESH_DIALOG_DESCRIPTION);

		fieldsPanel.addComponent(new Label("Interval (seconds):"));
		intervalInput = new TuiDialogInputBox(new TerminalSize(8, 1));
		intervalInput.setText(String.valueOf(initialIntervalSeconds));
		fieldsPanel.addComponent(intervalInput);

		Button applyButton = new Button(TuiThreadUiKeys.REFRESH_DIALOG_APPLY_LABEL, new Runnable() {
			@Override
			public void run() {
				Integer parsed = parseIntervalSeconds(intervalInput.getText());
				if(parsed == null) {
					TuiDialogs.showMessageDialog(
						TuiThreadRefreshDialog.this.gui,
						TuiThreadUiKeys.REFRESH_DIALOG_VALIDATION_TITLE,
						TuiThreadUiKeys.REFRESH_DIALOG_VALIDATION_MESSAGE,
						MessageDialogButton.OK
					);
					return;
				}
				result = parsed;
				close();
			}
		});
		Button cancelButton = new Button(TuiThreadUiKeys.REFRESH_DIALOG_CANCEL_LABEL, new Runnable() {
			@Override
			public void run() {
				result = null;
				close();
			}
		});

		Panel buttonPanel = new Panel();
		buttonPanel.setLayoutManager(new GridLayout(2).setHorizontalSpacing(1));
		buttonPanel.addComponent(applyButton);
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

	public static Integer show(WindowBasedTextGUI gui, int initialIntervalSeconds) {
		if(gui == null) {
			return null;
		}
		TuiThreadRefreshDialog dialog = new TuiThreadRefreshDialog(gui, initialIntervalSeconds);
		dialog.showDialog(gui);
		return dialog.result;
	}

	private static Integer parseIntervalSeconds(String raw) {
		if(raw == null) {
			return null;
		}
		String trimmed = raw.trim();
		if(trimmed.length() == 0) {
			return null;
		}
		try {
			int value = Integer.parseInt(trimmed);
			if(value < TuiThreadUiKeys.MIN_REFRESH_INTERVAL_SECONDS
				|| value > TuiThreadUiKeys.MAX_REFRESH_INTERVAL_SECONDS) {
				return null;
			}
			return Integer.valueOf(value);
		} catch(NumberFormatException e) {
			return null;
		}
	}

	private void enableCenteredHint() {
		Set<Window.Hint> hints = new HashSet<Window.Hint>(getHints());
		hints.add(Window.Hint.CENTERED);
		setHints(hints);
	}
}
