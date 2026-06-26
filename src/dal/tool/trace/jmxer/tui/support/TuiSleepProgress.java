package dal.tool.trace.jmxer.tui.support;

import com.googlecode.lanterna.gui2.WindowBasedTextGUI;

import dal.tool.trace.jmxer.tui.dialog.TuiSleepProgressDialog;
import dal.tool.trace.jmxer.tui.support.TuiGuiThread.GuiCallable;

/**
 * SLEEP이 끝나거나 사용자가 진행 대화상자를 취소할 때까지 명령 워커 스레드를 대기시킨다.
 *
 */
public final class TuiSleepProgress {

	private static final long POLL_MS = 200L;

	private TuiSleepProgress() {}

	public static boolean waitForSleep(WindowBasedTextGUI gui, long sleepMs) {
		if(gui == null || sleepMs <= 0L) {
			return true;
		}
		final TuiSleepProgressDialog[] dialogHolder = new TuiSleepProgressDialog[1];
		TuiGuiThread.callOnGuiThread(gui, new GuiCallable<Void>() {
			@Override
			public Void call() {
				dialogHolder[0] = TuiSleepProgressDialog.show(gui, sleepMs);
				return null;
			}
		});
		TuiSleepProgressDialog dialog = dialogHolder[0];
		if(dialog == null) {
			try {
				Thread.sleep(sleepMs);
				return true;
			} catch(InterruptedException e) {
				Thread.currentThread().interrupt();
				return false;
			}
		}
		final long startMs = System.currentTimeMillis();
		final long deadlineMs = startMs + sleepMs;
		boolean completed = false;
		try {
			while(true) {
				if(dialog.isCancelRequested()) {
					return false;
				}
				long now = System.currentTimeMillis();
				if(now >= deadlineMs) {
					completed = true;
					break;
				}
				final long elapsedMs = now - startMs;
				final long remainingMs = deadlineMs - now;
				TuiGuiThread.callOnGuiThread(gui, new GuiCallable<Void>() {
					@Override
					public Void call() {
						dialog.refreshProgress(elapsedMs, remainingMs);
						return null;
					}
				});
				long waitMs = Math.min(POLL_MS, deadlineMs - now);
				if(waitMs > 0L) {
					Thread.sleep(waitMs);
				}
			}
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		} finally {
			final TuiSleepProgressDialog toClose = dialog;
			TuiGuiThread.callOnGuiThread(gui, new GuiCallable<Void>() {
				@Override
				public Void call() {
					if(toClose != null) {
						toClose.closeDialog();
					}
					return null;
				}
			});
		}
		return completed;
	}
}
