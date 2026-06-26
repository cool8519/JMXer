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

import dal.tool.trace.jmxer.tui.support.RecordProgressKind;
import dal.tool.trace.jmxer.tui.support.TuiRecordUiKeys;

/**
 * Record UI 샘플링(스택트레이스·리소스) 비차단 진행 표시 다이얼로그.
 *
 */
public final class TuiRecordProgressDialog extends DialogWindow {

	private static final int NO_SAMPLE_COUNT = -1;

	private final AtomicBoolean stopRequested = new AtomicBoolean(false);
	private final RecordProgressKind kind;
	private final long durationMs;
	private final long startTimeMs;
	private final Label statusLabel;

	private TuiRecordProgressDialog(WindowBasedTextGUI gui, RecordProgressKind kind, long durationMs) {
		super(TuiRecordUiKeys.PROGRESS_TITLE);
		this.kind = kind == null ? RecordProgressKind.STACKTRACE : kind;
		this.durationMs = durationMs < 0L ? 0L : durationMs;
		this.startTimeMs = System.currentTimeMillis();
		enableCenteredHint();

		statusLabel = new Label(TuiDialogText.body(formatStatus(0L, NO_SAMPLE_COUNT)));

		Panel mainPanel = new Panel();
		mainPanel.setLayoutManager(new GridLayout(1).setLeftMarginSize(1).setRightMarginSize(1));
		mainPanel.addComponent(statusLabel);
		mainPanel.addComponent(new EmptySpace(TerminalSize.ONE));

		if(showStopButton()) {
			Button stopButton = new Button(TuiRecordUiKeys.PROGRESS_STOP_LABEL, new Runnable() {
				@Override
				public void run() {
					stopRequested.set(true);
					close();
				}
			});
			Panel buttonPanel = new Panel();
			buttonPanel.setLayoutManager(new GridLayout(1).setHorizontalSpacing(1));
			buttonPanel.addComponent(stopButton);
			buttonPanel.setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.CENTER, GridLayout.Alignment.CENTER, false, false))
				.addTo(mainPanel);
		}

		setComponent(mainPanel);
		TuiDialogEscFocus.install(this);
	}

	public static TuiRecordProgressDialog show(WindowBasedTextGUI gui, RecordProgressKind kind, long durationMs) {
		TuiRecordProgressDialog dialog = new TuiRecordProgressDialog(gui, kind, durationMs);
		gui.addWindow(dialog);
		return dialog;
	}

	public boolean isStopRequested() {
		return stopRequested.get();
	}

	public boolean isManualStop() {
		return durationMs == 0L;
	}

	public boolean isTimedDuration() {
		return durationMs > 0L;
	}

	public boolean showStopButton() {
		return isManualStop() || isTimedDuration();
	}

	public void refreshProgress(long elapsedMs, int sampleCount) {
		statusLabel.setText(TuiDialogText.body(formatStatus(elapsedMs, sampleCount)));
	}

	public void updateSampleCount(int sampleCount) {
		long elapsedMs = Math.max(0L, System.currentTimeMillis() - startTimeMs);
		refreshProgress(elapsedMs, sampleCount);
	}

	public void closeDialog() {
		close();
	}

	private String formatStatus(long elapsedMs, int sampleCount) {
		StringBuilder sb = new StringBuilder();
		sb.append(headerLine()).append('\n');
		if(isManualStop()) {
			sb.append(String.format(TuiRecordUiKeys.PROGRESS_ELAPSED_LINE, toElapsedSeconds(elapsedMs)));
			if(showSampleCount(sampleCount)) {
				sb.append('\n').append(String.format(TuiRecordUiKeys.PROGRESS_SAMPLES_LINE, sampleCount));
			}
			sb.append("\n\n").append(TuiRecordUiKeys.PROGRESS_STOP_HINT);
		} else {
			sb.append(String.format(
				TuiRecordUiKeys.PROGRESS_DURATION_LINE,
				toTotalSeconds(durationMs),
				toRemainingSeconds(durationMs, elapsedMs)
			));
			if(showSampleCount(sampleCount)) {
				sb.append('\n').append(String.format(TuiRecordUiKeys.PROGRESS_SAMPLES_LINE, sampleCount));
			}
			sb.append("\n\n").append(TuiRecordUiKeys.PROGRESS_STOP_EARLY_HINT);
		}
		return sb.toString();
	}

	private String headerLine() {
		if(kind == RecordProgressKind.RESOURCE) {
			return TuiRecordUiKeys.PROGRESS_HEADER_RESOURCE;
		}
		return TuiRecordUiKeys.PROGRESS_HEADER_STACKTRACE;
	}

	private boolean showSampleCount(int sampleCount) {
		return kind == RecordProgressKind.STACKTRACE && sampleCount >= 0;
	}

	private static int toTotalSeconds(long durationMs) {
		if(durationMs <= 0L) {
			return 0;
		}
		return (int)((durationMs + 999L) / 1000L);
	}

	private static int toElapsedSeconds(long elapsedMs) {
		if(elapsedMs <= 0L) {
			return 0;
		}
		return (int)(elapsedMs / 1000L);
	}

	private static int toRemainingSeconds(long durationMs, long elapsedMs) {
		long remainingMs = durationMs - elapsedMs;
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
