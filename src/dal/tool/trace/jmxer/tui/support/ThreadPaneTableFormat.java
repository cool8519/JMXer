package dal.tool.trace.jmxer.tui.support;

import java.lang.management.ThreadInfo;
import java.util.List;

import dal.tool.trace.jmxer.tui.mode.JmxTuiModeContext.ThreadRowState;
import dal.tool.trace.jmxer.tui.mode.JmxTuiModeContext.ThreadSnapshotEntry;

/**
 * Thread UI 모드 목록 패널 열 배치(TID | STATE | CPU | MEM/s | NAME).
 *
 */
public final class ThreadPaneTableFormat {

	public static final String COL_SEPARATOR = " | ";

	public static final String HEADER_TID = "TID";
	public static final String HEADER_STATE = "STATE";
	public static final String HEADER_CPU = "CPU";
	public static final String HEADER_MEM = "MEM/s";
	public static final String HEADER_MEM_TOTAL = "MEM";
	public static final String HEADER_NAME = "NAME";

	public static final String CPU_UNAVAILABLE = "-1";
	public static final String MEM_UNAVAILABLE = "-1";

	public static final String CPU_DESTROYED_MARKER = "-";

	public static final int HEADER_ROW_PREFIX_COLS = 3;

	public static final String THREAD_ROW_PREFIX_NORMAL = "   ";
	public static final String THREAD_ROW_PREFIX_CURSOR = TuiListArrowPrefix.THREAD_CURSOR;
	public static final String THREAD_ROW_PREFIX_CREATED = "  +";
	public static final String THREAD_ROW_PREFIX_CREATED_CURSOR = TuiListArrowPrefix.THREAD_CREATED_CURSOR;

	private static final String CPU_CORE_SUFFIX = "c";

	public static final int CPU_COL_WIDTH = 5;

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
		if(cpuCores < 0.005d) {
			return "0" + CPU_CORE_SUFFIX;
		}
		return String.format("%.2f", cpuCores) + CPU_CORE_SUFFIX;
	}

	public static String formatDumpCpuDisplay(boolean cpuValid, double totalCpuNanos) {
		if(!cpuValid) {
			return CPU_UNAVAILABLE;
		}
		if(Double.isNaN(totalCpuNanos) || Double.isInfinite(totalCpuNanos) || totalCpuNanos < 0d) {
			return CPU_UNAVAILABLE;
		}

		String[] units = new String[] { "n", "u", "m", "s" };
		double[] thresholds = new double[] { 1d, 1000d, 1000000d, 1000000000d };
		int unitIdx = units.length - 1;
		double v = totalCpuNanos;
		for(int i = thresholds.length - 1; i >= 0; i--) {
			if(totalCpuNanos >= thresholds[i]) {
				unitIdx = i;
				v = totalCpuNanos / thresholds[i];
				break;
			}
		}

		long rounded = Math.round(v);
		if(rounded > 9999L) {
			rounded = 9999L;
		}
		if(rounded < 0L) {
			rounded = 0L;
		}
		return String.valueOf(rounded) + units[unitIdx];
	}

	public static String formatDumpMemDisplay(boolean memValid, double totalAllocatedBytes) {
		return formatBinaryMagnitudeDisplay(memValid, totalAllocatedBytes);
	}

	public static String formatMemDisplay(boolean memValid, double memBytesPerSec) {
		return formatBinaryMagnitudeDisplay(memValid, memBytesPerSec);
	}

	private static String formatBinaryMagnitudeDisplay(boolean valid, double value) {
		if(!valid) {
			return MEM_UNAVAILABLE;
		}
		if(Double.isNaN(value) || Double.isInfinite(value) || value < 0d) {
			return MEM_UNAVAILABLE;
		}

		String[] units = new String[] { "B", "K", "M", "G", "T", "P", "E" };
		int unitIdx = 0;
		double v = value;
		while(v >= 1024d && unitIdx < units.length - 1) {
			v /= 1024d;
			unitIdx++;
		}

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
		return MIN_CPU_COL_WIDTH;
	}

	public static int computeMemWidth(List<ThreadSnapshotEntry> entries) {
		return MIN_MEM_COL_WIDTH;
	}

	public static String buildHeaderLine(int tidWidth, int cpuWidth, int memWidth) {
		return buildHeaderLine(tidWidth, cpuWidth, memWidth, false);
	}

	public static String buildHeaderLine(int tidWidth, int cpuWidth, int memWidth, boolean dumpView) {
		StringBuilder pad = new StringBuilder(HEADER_ROW_PREFIX_COLS);
		for(int i = 0; i < HEADER_ROW_PREFIX_COLS; i++) {
			pad.append(' ');
		}
		String memHeader = dumpView ? HEADER_MEM_TOTAL : HEADER_MEM;
		return pad.toString() + formatColumns(
			HEADER_TID, HEADER_STATE, HEADER_CPU, memHeader, HEADER_NAME, tidWidth, cpuWidth, memWidth);
	}

	public static String buildRowLine(ThreadSnapshotEntry entry, int tidWidth, int cpuWidth, int memWidth) {
		return buildRowLine(entry, tidWidth, cpuWidth, memWidth, false);
	}

	public static String buildRowLine(ThreadSnapshotEntry entry, int tidWidth, int cpuWidth, int memWidth, boolean dumpView) {
		if(entry == null) {
			return formatDataRow(0L, "?", CPU_UNAVAILABLE, MEM_UNAVAILABLE, "", tidWidth, cpuWidth, memWidth);
		}
		String state = entry.getStateAbbrev();
		if(state == null || state.length() == 0) {
			state = "?";
		}
		String cpu;
		String mem;
		if(dumpView) {
			cpu = formatDumpCpuDisplay(entry.isCpuValid(), entry.getCpuCores());
			mem = formatDumpMemDisplay(entry.isMemValid(), entry.getMemBytesPerSec());
		} else {
			cpu = formatCpuCell(entry);
			mem = formatMemDisplay(entry.isMemValid(), entry.getMemBytesPerSec());
		}
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
