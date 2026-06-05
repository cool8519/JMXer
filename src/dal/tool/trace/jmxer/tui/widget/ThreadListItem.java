package dal.tool.trace.jmxer.tui.widget;

/**
 * Runnable placeholder for a thread row in {@link com.googlecode.lanterna.gui2.ActionListBox}.
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
