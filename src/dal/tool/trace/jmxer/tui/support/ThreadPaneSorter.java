package dal.tool.trace.jmxer.tui.support;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import dal.tool.trace.jmxer.tui.mode.JmxTuiModeContext.ThreadSnapshotEntry;

/**
 * Thread UI 모드 목록 패널의 스레드 스냅샷 행을 정렬한다.
 *
 */
public final class ThreadPaneSorter {

	private ThreadPaneSorter() {}

	public static void sort(List<ThreadSnapshotEntry> entries, ThreadPaneSortSettings settings) {
		if(entries == null || entries.size() < 2) {
			return;
		}
		ThreadPaneSortSettings sortSettings = settings == null ? ThreadPaneSortSettings.DEFAULT : settings;
		Collections.sort(entries, comparatorFor(sortSettings.getColumn(), sortSettings.getOrder()));
	}

	private static Comparator<ThreadSnapshotEntry> comparatorFor(ThreadPaneSortColumn column, ThreadPaneSortOrder order) {
		final ThreadPaneSortColumn sortColumn = column == null ? ThreadPaneSortColumn.TID : column;
		final ThreadPaneSortOrder sortOrder = order == null ? ThreadPaneSortOrder.ASCENDING : order;
		return new Comparator<ThreadSnapshotEntry>() {
			@Override
			public int compare(ThreadSnapshotEntry a, ThreadSnapshotEntry b) {
				int cmp = comparePrimary(a, b, sortColumn, sortOrder);
				if(cmp != 0) {
					return cmp;
				}
				return Long.compare(threadId(a), threadId(b));
			}
		};
	}

	private static int comparePrimary(
			ThreadSnapshotEntry a,
			ThreadSnapshotEntry b,
			ThreadPaneSortColumn column,
			ThreadPaneSortOrder order) {
		switch(column) {
			case STATE:
				return compareStrings(stateOf(a), stateOf(b), order);
			case CPU:
				return compareMetric(a == null ? false : a.isCpuValid(), cpuOf(a), b == null ? false : b.isCpuValid(), cpuOf(b), order);
			case MEM:
				return compareMetric(a == null ? false : a.isMemValid(), memOf(a), b == null ? false : b.isMemValid(), memOf(b), order);
			case NAME:
				return compareStringsIgnoreCase(nameOf(a), nameOf(b), order);
			case TID:
			default:
				return compareLong(threadId(a), threadId(b), order);
		}
	}

	private static int compareLong(long a, long b, ThreadPaneSortOrder order) {
		if(order == ThreadPaneSortOrder.DESCENDING) {
			return Long.compare(b, a);
		}
		return Long.compare(a, b);
	}

	private static int compareStrings(String a, String b, ThreadPaneSortOrder order) {
		if(a == null) {
			a = "";
		}
		if(b == null) {
			b = "";
		}
		if(order == ThreadPaneSortOrder.DESCENDING) {
			return b.compareTo(a);
		}
		return a.compareTo(b);
	}

	private static int compareStringsIgnoreCase(String a, String b, ThreadPaneSortOrder order) {
		if(a == null) {
			a = "";
		}
		if(b == null) {
			b = "";
		}
		if(order == ThreadPaneSortOrder.DESCENDING) {
			return b.compareToIgnoreCase(a);
		}
		return a.compareToIgnoreCase(b);
	}

	private static int compareMetric(boolean aValid, double aValue, boolean bValid, double bValue, ThreadPaneSortOrder order) {
		if(aValid != bValid) {
			return aValid ? -1 : 1;
		}
		if(!aValid) {
			return 0;
		}
		if(order == ThreadPaneSortOrder.DESCENDING) {
			return Double.compare(bValue, aValue);
		}
		return Double.compare(aValue, bValue);
	}

	private static long threadId(ThreadSnapshotEntry entry) {
		return entry == null ? 0L : entry.getThreadId();
	}

	private static String stateOf(ThreadSnapshotEntry entry) {
		return entry == null ? "" : entry.getStateAbbrev();
	}

	private static double cpuOf(ThreadSnapshotEntry entry) {
		return entry == null ? 0d : entry.getCpuCores();
	}

	private static double memOf(ThreadSnapshotEntry entry) {
		return entry == null ? 0d : entry.getMemBytesPerSec();
	}

	private static String nameOf(ThreadSnapshotEntry entry) {
		return entry == null ? "" : entry.getThreadName();
	}
}
