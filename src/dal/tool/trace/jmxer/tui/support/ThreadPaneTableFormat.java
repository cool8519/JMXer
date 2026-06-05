package dal.tool.trace.jmxer.tui.support;

import java.lang.management.ThreadInfo;
import java.util.List;

import dal.tool.trace.jmxer.tui.mode.JmxTuiModeContext.ThreadRowState;
import dal.tool.trace.jmxer.tui.mode.JmxTuiModeContext.ThreadSnapshotEntry;

/**
 * Column layout for the Thread UI mode list pane ({@code TID | STATE | CPU | MEM/s | NAME}).
 */
public final class ThreadPaneTableFormat {

	public static final String COL_SEPARATOR = " | ";

	public static final String HEADER_TID = "TID";
	public static final String HEADER_STATE = "STATE";
	public static final String HEADER_CPU = "CPU";
	public static final String HEADER_MEM = "MEM/s";
	public static final String HEADER_NAME = "NAME";

	public static final String CPU_UNAVAILABLE = "-1";
	public static final String MEM_UNAVAILABLE = "-1";

	public static final String CPU_DESTROYED_MARKER = "-";

	/** Thread list row prefix width (cursor / created markers); must match renderer. */
	public static final int HEADER_ROW_PREFIX_COLS = 3;

	public static final String THREAD_ROW_PREFIX_NORMAL = "   ";
	public static final String THREAD_ROW_PREFIX_CURSOR = "\u25b6  ";
	/** Two spaces + {@code +}; exactly {@link #HEADER_ROW_PREFIX_COLS} terminal columns. */
	public static final String THREAD_ROW_PREFIX_CREATED = "  +";
	public static final String THREAD_ROW_PREFIX_CREATED_CURSOR = "\u25b6 +";

	private static final String CPU_CORE_SUFFIX = "c";

	/**
	 * Fixed CPU column width.
	 * <p>
	 * Display is average logical-core usage over the refresh interval, with {@code c} suffix
	 * (e.g. {@code 0c}, {@code 0.85c}, {@code 12c}). Unavailable values stay {@code -1} without suffix.
	 */
	public static final int CPU_COL_WIDTH = 5;

	/**
	 * Fixed MEM column width.
	 * <p>
	 * Display is binary (1024-based) rate using B/K/M/G/T as an integer (rounded),
	 * and the formatted output is clamped to fit {@link #MEM_COL_WIDTH}.
	 */
	public static final int MEM_COL_WIDTH = 5;

	public static final int STATE_COL_WIDTH = 5;

	private static final int MIN_TID_COL_WIDTH = HEADER_TID.length();
	private static final int MIN_CPU_COL_WIDTH = CPU_COL_WIDTH;
	private static final int MIN_MEM_COL_WIDTH = MEM_COL_WIDTH;

	private ThreadPaneTableFormat() {}

	public static String abbreviateState(ThreadInfo info) {
		if(info == null || info.getThreadState() == null) {
			return "?";
		}
		return abbreviateStateName(info.getThreadState().name());
	}

	public static String abbreviateStateName(String stateName) {
		if(stateName == null || stateName.length() == 0) {
			return "?";
		}
		if("NEW".equals(stateName)) {
			return "NEW";
		}
		if("RUNNABLE".equals(stateName)) {
			return "RUN";
		}
		if("BLOCKED".equals(stateName)) {
			return "BLOCK";
		}
		if("WAITING".equals(stateName)) {
			return "WAIT";
		}
		if("TIMED_WAITING".equals(stateName)) {
			return "TIMED";
		}
		if("TERMINATED".equals(stateName)) {
			return "TERM";
		}
		return "?";
	}

	public static String formatCpuDisplay(boolean cpuValid, double cpuCores) {
		if(!cpuValid) {
			return CPU_UNAVAILABLE;
		}
		if(Double.isNaN(cpuCores) || Double.isInfinite(cpuCores) || cpuCores < 0d) {
			return CPU_UNAVAILABLE;
		}

		// Keep within 5 columns: "X.XXc" below 10 cores, else up to "9999c".
		if(cpuCores >= 9999.5d) {
			return "9999" + CPU_CORE_SUFFIX;
		}
		if(cpuCores >= 10.0d) {
			long rounded = Math.round(cpuCores);
			if(rounded > 9999L) {
				rounded = 9999L;
			}
			return String.valueOf(rounded) + CPU_CORE_SUFFIX;
		}
		// Rounds to 0.00 at two decimals (e.g. idle thread) → compact "0c".
		if(cpuCores < 0.005d) {
			return "0" + CPU_CORE_SUFFIX;
		}
		return String.format("%.2f", cpuCores) + CPU_CORE_SUFFIX;
	}

	public static String formatMemDisplay(boolean memValid, double memBytesPerSec) {
		if(!memValid) {
			return MEM_UNAVAILABLE;
		}
		if(Double.isNaN(memBytesPerSec) || Double.isInfinite(memBytesPerSec) || memBytesPerSec < 0d) {
			return MEM_UNAVAILABLE;
		}

		// Binary (1024-based) rate units.
		String[] units = new String[] { "B", "K", "M", "G", "T", "P", "E" };
		int unitIdx = 0;
		double v = memBytesPerSec;
		while(v >= 1024d && unitIdx < units.length - 1) {
			v /= 1024d;
			unitIdx++;
		}

		// Keep within 5 columns: up to 4 digits + unit (e.g. "9999K").
		long rounded = Math.round(v);
		if(rounded > 9999L) {
			rounded = 9999L;
		}
		if(rounded < 0L) {
			rounded = 0L;
		}
		return String.valueOf(rounded) + units[unitIdx];
	}

	public static int computeTidWidth(List<ThreadSnapshotEntry> entries) {
		int width = MIN_TID_COL_WIDTH;
		if(entries == null) {
			return width;
		}
		for(int i = 0; i < entries.size(); i++) {
			ThreadSnapshotEntry entry = entries.get(i);
			if(entry == null) {
				continue;
			}
			int len = String.valueOf(entry.getThreadId()).length();
			if(len > width) {
				width = len;
			}
		}
		return width;
	}

	public static int computeCpuWidth(List<ThreadSnapshotEntry> entries) {
		// Fixed to prevent CPU from expanding the pane.
		return MIN_CPU_COL_WIDTH;
	}

	public static int computeMemWidth(List<ThreadSnapshotEntry> entries) {
		// Fixed to prevent MEM from expanding the pane.
		return MIN_MEM_COL_WIDTH;
	}

	public static String buildHeaderLine(int tidWidth, int cpuWidth, int memWidth) {
		StringBuilder pad = new StringBuilder(HEADER_ROW_PREFIX_COLS);
		for(int i = 0; i < HEADER_ROW_PREFIX_COLS; i++) {
			pad.append(' ');
		}
		return pad.toString() + formatColumns(
			HEADER_TID, HEADER_STATE, HEADER_CPU, HEADER_MEM, HEADER_NAME, tidWidth, cpuWidth, memWidth);
	}

	public static String buildRowLine(ThreadSnapshotEntry entry, int tidWidth, int cpuWidth, int memWidth) {
		if(entry == null) {
			return formatDataRow(0L, "?", CPU_UNAVAILABLE, MEM_UNAVAILABLE, "", tidWidth, cpuWidth, memWidth);
		}
		String state = entry.getStateAbbrev();
		if(state == null || state.length() == 0) {
			state = "?";
		}
		String cpu = formatCpuCell(entry);
		String mem = formatMemDisplay(entry.isMemValid(), entry.getMemBytesPerSec());
		String name = entry.getThreadName() == null ? "" : entry.getThreadName();
		return formatDataRow(entry.getThreadId(), state, cpu, mem, name, tidWidth, cpuWidth, memWidth);
	}

	public static String formatCpuCell(ThreadSnapshotEntry entry) {
		if(entry == null) {
			return CPU_UNAVAILABLE;
		}
		ThreadRowState rowState = entry.getRowState();
		if(rowState == ThreadRowState.DESTROYED) {
			return fitCpuMarker(CPU_DESTROYED_MARKER);
		}
		return formatCpuDisplay(entry.isCpuValid(), entry.getCpuCores());
	}

	public static int computeCpuColumnStartInLabel(int tidWidth) {
		int tidW = tidWidth < MIN_TID_COL_WIDTH ? MIN_TID_COL_WIDTH : tidWidth;
		return tidW + COL_SEPARATOR.length() + STATE_COL_WIDTH + COL_SEPARATOR.length();
	}

	public static int computeCpuColumnEndInLabel(int tidWidth, int cpuWidth) {
		int cpuW = cpuWidth < MIN_CPU_COL_WIDTH ? MIN_CPU_COL_WIDTH : cpuWidth;
		return computeCpuColumnStartInLabel(tidWidth) + cpuW;
	}

	private static String fitCpuMarker(String marker) {
		if(marker == null) {
			marker = "";
		}
		if(marker.length() >= CPU_COL_WIDTH) {
			return marker.substring(0, CPU_COL_WIDTH);
		}
		StringBuilder sb = new StringBuilder(CPU_COL_WIDTH);
		sb.append(marker);
		while(sb.length() < CPU_COL_WIDTH) {
			sb.append(' ');
		}
		return sb.toString();
	}

	private static String formatColumns(
			String tid,
			String state,
			String cpu,
			String mem,
			String name,
			int tidWidth,
			int cpuWidth,
			int memWidth) {
		int tidW = tidWidth < MIN_TID_COL_WIDTH ? MIN_TID_COL_WIDTH : tidWidth;
		int cpuW = cpuWidth < MIN_CPU_COL_WIDTH ? MIN_CPU_COL_WIDTH : cpuWidth;
		int memW = memWidth < MIN_MEM_COL_WIDTH ? MIN_MEM_COL_WIDTH : memWidth;
		String stateCell = fitFixedLeft(state, STATE_COL_WIDTH);
		return String.format(
			"%" + tidW + "s" + COL_SEPARATOR + "%-" + STATE_COL_WIDTH + "s" + COL_SEPARATOR + "%" + cpuW + "s" + COL_SEPARATOR
				+ "%" + memW + "s" + COL_SEPARATOR + "%s",
			tid,
			stateCell,
			cpu,
			mem,
			name);
	}

	private static String formatDataRow(
			long threadId,
			String state,
			String cpu,
			String mem,
			String name,
			int tidWidth,
			int cpuWidth,
			int memWidth) {
		int tidW = tidWidth < MIN_TID_COL_WIDTH ? MIN_TID_COL_WIDTH : tidWidth;
		int cpuW = cpuWidth < MIN_CPU_COL_WIDTH ? MIN_CPU_COL_WIDTH : cpuWidth;
		int memW = memWidth < MIN_MEM_COL_WIDTH ? MIN_MEM_COL_WIDTH : memWidth;
		String stateCell = fitFixedLeft(state, STATE_COL_WIDTH);
		return String.format(
			"%" + tidW + "d" + COL_SEPARATOR + "%-" + STATE_COL_WIDTH + "s" + COL_SEPARATOR + "%" + cpuW + "s" + COL_SEPARATOR
				+ "%" + memW + "s" + COL_SEPARATOR + "%s",
			threadId,
			stateCell,
			cpu,
			mem,
			name);
	}

	private static String fitFixedLeft(String value, int width) {
		if(value == null) {
			value = "";
		}
		if(value.length() == width) {
			return value;
		}
		if(value.length() > width) {
			return value.substring(0, width);
		}
		StringBuilder sb = new StringBuilder(width);
		sb.append(value);
		while(sb.length() < width) {
			sb.append(' ');
		}
		return sb.toString();
	}
}
