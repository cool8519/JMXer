package dal.tool.trace.jmxer.tui.support;

/**
 * Thread UI Mode 목록 패널의 정렬 방향.
 *
 */
public enum ThreadPaneSortOrder {
	ASCENDING("Ascending"),
	DESCENDING("Descending");

	private final String displayLabel;

	ThreadPaneSortOrder(String displayLabel) {
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
