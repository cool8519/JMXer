package dal.tool.trace.jmxer.tui.dialog;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.DialogWindow;

import dal.tool.trace.jmxer.tui.support.TuiSleepUiKeys;

/**
 * TUI 최상위 SLEEP 명령 진행 다이얼로그. SCRIPT 내부에서는 사용하지 않는다.
 *
 */
public final class TuiSleepProgressDialog extends DialogWindow {

	private final long sleepMs;
	private final AtomicBoolean cancelRequested = new AtomicBoolean(false);
	private final Label statusLabel;

	private TuiSleepProgressDialog(long sleepMs) {
		super(TuiSleepUiKeys.DIALOG_TITLE);
		this.sleepMs = sleepMs < 0L ? 0L : sleepMs;
		enableCenteredHint();

		statusLabel = new Label(TuiDialogText.body(formatStatus(sleepMs)));

		Panel mainPanel = new Panel();
		mainPanel.setLayoutManager(new GridLayout(1).setLeftMarginSize(1).setRightMarginSize(1));
		mainPanel.addComponent(statusLabel);
		mainPanel.addComponent(new EmptySpace(TerminalSize.ONE));

		Button cancelButton = new Button(TuiSleepUiKeys.CANCEL_LABEL, new Runnable() {
			@Override
			public void run() {
				cancelRequested.set(true);
				close();
			}
		});
		Panel buttonPanel = new Panel();
		buttonPanel.setLayoutManager(new GridLayout(1).setHorizontalSpacing(1));
		buttonPanel.addComponent(cancelButton);
		buttonPanel.setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.CENTER, GridLayout.Alignment.CENTER, false, false))
			.addTo(mainPanel);

		setComponent(mainPanel);
		TuiDialogEscFocus.install(this);
	}

	public static TuiSleepProgressDialog show(WindowBasedTextGUI gui, long sleepMs) {
		TuiSleepProgressDialog dialog = new TuiSleepProgressDialog(sleepMs);
		gui.addWindow(dialog);
		return dialog;
	}

	public boolean isCancelRequested() {
		return cancelRequested.get();
	}

	public void refreshProgress(long elapsedMs, long remainingMs) {
		statusLabel.setText(TuiDialogText.body(formatStatus(remainingMs)));
	}

	public void closeDialog() {
		close();
	}

	private String formatStatus(long remainingMs) {
		long remaining = remainingMs < 0L ? 0L : remainingMs;
		if(remaining > sleepMs) {
			remaining = sleepMs;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(TuiSleepUiKeys.HEADER_LINE).append('\n');
		sb.append(String.format(
			TuiSleepUiKeys.DURATION_LINE,
			Integer.valueOf(toTotalSeconds(sleepMs)),
			Integer.valueOf(toRemainingSeconds(remaining))
		));
		sb.append("\n\n").append(TuiSleepUiKeys.CANCEL_HINT);
		return sb.toString();
	}

	private static int toTotalSeconds(long durationMs) {
		if(durationMs <= 0L) {
			return 0;
		}
		return (int)((durationMs + 999L) / 1000L);
	}

	private static int toRemainingSeconds(long remainingMs) {
		if(remainingMs <= 0L) {
			return 0;
		}
		return (int)(remainingMs / 1000L);
	}

	private void enableCenteredHint() {
		Set<Window.Hint> hints = new HashSet<Window.Hint>(getHints());
		hints.add(Window.Hint.CENTERED);
		setHints(hints);
	}
}
