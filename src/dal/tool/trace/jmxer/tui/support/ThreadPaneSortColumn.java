package dal.tool.trace.jmxer.tui.support;

/**
 * Thread 패널에서 정렬 가능한 열(ThreadPaneTableFormat 헤더와 대응).
 *
 */
public enum ThreadPaneSortColumn {
	TID(ThreadPaneTableFormat.HEADER_TID),
	STATE(ThreadPaneTableFormat.HEADER_STATE),
	CPU(ThreadPaneTableFormat.HEADER_CPU),
	MEM(ThreadPaneTableFormat.HEADER_MEM),
	NAME(ThreadPaneTableFormat.HEADER_NAME);

	private final String displayLabel;

	ThreadPaneSortColumn(String displayLabel) {
		this.displayLabel = displayLabel == null ? name() : displayLabel;
	}

	public String getDisplayLabel() {
		return displayLabel;
	}

	@Override
	public String toString() {
		return displayLabel;
	}
}
