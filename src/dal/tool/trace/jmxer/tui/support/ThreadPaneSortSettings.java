package dal.tool.trace.jmxer.tui.support;

/**
 * Thread UI 모드에서 선택한 목록 정렬 열과 방향.
 *
 */
public final class ThreadPaneSortSettings {

	public static final ThreadPaneSortSettings DEFAULT =
		new ThreadPaneSortSettings(ThreadPaneSortColumn.TID, ThreadPaneSortOrder.ASCENDING);

	private final ThreadPaneSortColumn column;
	private final ThreadPaneSortOrder order;

	public ThreadPaneSortSettings(ThreadPaneSortColumn column, ThreadPaneSortOrder order) {
		this.column = column == null ? ThreadPaneSortColumn.TID : column;
		this.order = order == null ? ThreadPaneSortOrder.ASCENDING : order;
	}

	public ThreadPaneSortColumn getColumn() {
		return column;
	}

	public ThreadPaneSortOrder getOrder() {
		return order;
	}

	public String formatBorderSortClause() {
		return "(" + column.getDisplayLabel() + formatBorderOrderArrow() + ")";
	}

	public static String buildListBorderTitle(String paneTitle, String formattedTime, ThreadPaneSortSettings settings) {
		String title = paneTitle == null ? "" : paneTitle;
		String time = formattedTime == null ? "" : formattedTime;
		ThreadPaneSortSettings sortSettings = settings == null ? DEFAULT : settings;
		return title + " " + sortSettings.formatBorderSortClause() + " [" + time + "]";
	}

	private char formatBorderOrderArrow() {
		return order == ThreadPaneSortOrder.DESCENDING ? '▼' : '▲';
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj) {
			return true;
		}
		if(!(obj instanceof ThreadPaneSortSettings)) {
			return false;
		}
		ThreadPaneSortSettings other = (ThreadPaneSortSettings) obj;
		return column == other.column && order == other.order;
	}

	@Override
	public int hashCode() {
		int hash = 17;
		hash = 31 * hash + column.hashCode();
		hash = 31 * hash + order.hashCode();
		return hash;
	}
}
