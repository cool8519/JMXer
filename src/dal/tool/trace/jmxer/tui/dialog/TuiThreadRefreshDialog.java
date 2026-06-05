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

/**
 * Thread UI mode refresh-interval dialog when switching from manual to auto refresh.
 */
public final class TuiThreadRefreshDialog extends DialogWindow {

	private Integer result;
	private final WindowBasedTextGUI gui;
	private final TextBox intervalInput;

	private TuiThreadRefreshDialog(WindowBasedTextGUI gui, int initialIntervalSeconds) {
		super(TuiThreadUiKeys.REFRESH_DIALOG_TITLE);
		this.gui = gui;
		enableCenteredHint();

		Panel fieldsPanel = new Panel();
		fieldsPanel.setLayoutManager(new GridLayout(1).setLeftMarginSize(1).setRightMarginSize(1));
		TuiDialogLayout.addFormDescription(fieldsPanel, TuiThreadUiKeys.REFRESH_DIALOG_DESCRIPTION);

		fieldsPanel.addComponent(new Label("Interval (seconds):"));
		intervalInput = new TextBox(new TerminalSize(8, 1));
		intervalInput.setText(String.valueOf(initialIntervalSeconds));
		fieldsPanel.addComponent(intervalInput);

		Button applyButton = new Button(TuiThreadUiKeys.REFRESH_DIALOG_APPLY_LABEL, new Runnable() {
			@Override
			public void run() {
				Integer parsed = parseIntervalSeconds(intervalInput.getText());
				if(parsed == null) {
					TuiDialogs.showMessageDialog(
						gui,
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

	/**
	 * @return chosen interval in seconds, or {@code null} when cancelled (stay manual)
	 */
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
