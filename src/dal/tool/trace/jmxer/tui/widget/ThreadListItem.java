package dal.tool.trace.jmxer.tui.widget;

/**
 * ActionListBox 스레드 행용 Runnable 플레이스홀더.
 *
 */
public final class ThreadListItem implements Runnable {
	private final long threadId;
	private final String displayLabel;

	public ThreadListItem(long threadId, String displayLabel) {
		this.threadId = threadId;
		this.displayLabel = displayLabel == null ? "" : displayLabel;
	}

	public long getThreadId() {
		return threadId;
	}

	public String getDisplayLabel() {
		return displayLabel;
	}

	@Override
	public void run() {}

	@Override
	public String toString() {
		return displayLabel;
	}
}
