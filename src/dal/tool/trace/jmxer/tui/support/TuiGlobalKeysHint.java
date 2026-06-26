package dal.tool.trace.jmxer.tui.support;

import java.util.List;

import dal.tool.trace.jmxer.JMXControl;
import dal.tool.trace.jmxer.tui.mode.JmxTuiModeContext;
import dal.tool.trace.jmxer.tui.mode.JmxTuiModeContext.RecordViewState;
import dal.tool.trace.jmxer.tui.mode.JmxTuiUiMode;
import dal.tool.trace.jmxer.tui.mode.MBeanUiMode;
import dal.tool.trace.jmxer.tui.mode.RecordUiMode;
import dal.tool.trace.jmxer.tui.mode.ThreadUiMode;

/**
 * 현재 UI 모드에 맞는 상태 줄 전역 키 힌트.
 * SET 연동 토글(PRETTY_PRINT 등)은 상태 줄 3에 표시하며 여기에는 넣지 않는다.
 *
 */
public final class TuiGlobalKeysHint {
	private static final String PREFIX = " [Global] ";

	private TuiGlobalKeysHint() {}

	public static String mbean(boolean outputOn, boolean featuresOn, boolean featuresAvailable,
			boolean canResizeVerticalSplit) {
		StringBuilder sb = new StringBuilder(PREFIX);
		sb.append("MBean - ");
		appendShared(sb, outputOn);
		if(featuresAvailable) {
			sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.FEATURES_PANE_TOGGLE))
				.append(":Features(").append(featuresOn ? "ON" : "OFF").append(")");
		}
		return sb.toString();
	}

	public static String thread(boolean outputOn) {
		StringBuilder sb = new StringBuilder(PREFIX);
		sb.append("Thread - ");
		appendShared(sb, outputOn);
		return sb.toString();
	}

	public static String recordOfflineDump(boolean outputOn) {
		StringBuilder sb = new StringBuilder(PREFIX);
		sb.append("Record (Dump) - ");
		sb.append("F1:Help");
		sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.RECORD_DUMP_LOAD)).append(":Load");
		sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.RECORD_DUMP_SAVE)).append(":Save");
		sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.RECORD_DUMP_INFO)).append(":Dump Info");
		appendSharedTail(sb, outputOn);
		return sb.toString();
	}

	public static String record(boolean outputOn, boolean liveView, boolean samplingInProgress) {
		StringBuilder sb = new StringBuilder(PREFIX);
		sb.append("Record - ");
		sb.append("F1:Help, F3:UI Mode");
		if(!JMXControl.isOfflineSession) {
			sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.RECORD_VIEW_TYPE_TOGGLE)).append(":View Type");
		}
		appendSharedTail(sb, outputOn);
		if(!liveView && !samplingInProgress) {
			sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.RECORD_DUMP_LOAD)).append(":Load");
			sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.RECORD_DUMP_SAVE)).append(":Save");
			sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.RECORD_DUMP_INFO)).append(":Dump Info");
		}
		return sb.toString();
	}

	public static String recordDumpViewSecondLine() {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < RECORD_DUMP_VIEW_LINE_INDENT_COLS; i++) {
			sb.append(' ');
		}
		sb.append(TuiKeyBindings.formatRecordDumpViewKeysHint());
		return sb.toString();
	}

	/**
	 * {@link #mbean}·{@link #thread}·{@link #record}·상태 줄 3·Record Dump 보조 줄과 동일한 리매핑 Global 동작.
	 */
	public static void collectRemappableGlobalActions(
			List<TuiKeyAction> out,
			JmxTuiUiMode mode,
			JmxTuiModeContext ctx,
			boolean featuresAvailable,
			boolean samplingInProgress) {
		if(out == null || mode == null) {
			return;
		}
		TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.OUTPUT_PANE_TOGGLE);
		String modeId = mode.getId();
		if(MBeanUiMode.ID.equals(modeId)) {
			if(featuresAvailable) {
				TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.FEATURES_PANE_TOGGLE);
			}
			if(!JMXControl.isOfflineSession) {
				TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.OUTPUT_PRETTY_PRINT);
			}
			return;
		}
		if(ThreadUiMode.ID.equals(modeId)) {
			TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.THREAD_AUTO_REFRESH);
			TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.THREAD_STACKTRACE_TARGET);
			TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.THREAD_REQUEST_ENTRY_FILE);
			return;
		}
		if(RecordUiMode.ID.equals(modeId)) {
			if(JMXControl.isOfflineSession) {
				TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.RECORD_DUMP_LOAD);
				TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.RECORD_DUMP_SAVE);
				TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.RECORD_DUMP_INFO);
				if(!samplingInProgress) {
					appendRecordDumpViewActions(out);
				}
				TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.FEATURES_PANE_TOGGLE);
				TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.RECORD_SHOW_EMPTY_THREAD_TOGGLE);
				return;
			}
			RecordViewState view = ctx == null ? RecordViewState.LIVE : ctx.getRecordViewState();
			boolean liveView = view == RecordViewState.LIVE;
			TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.RECORD_VIEW_TYPE_TOGGLE);
			if(!liveView && !samplingInProgress) {
				TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.RECORD_DUMP_LOAD);
				TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.RECORD_DUMP_SAVE);
				TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.RECORD_DUMP_INFO);
				appendRecordDumpViewActions(out);
			}
			if(liveView) {
				TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.THREAD_AUTO_REFRESH);
			} else {
				TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.FEATURES_PANE_TOGGLE);
				TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.RECORD_SHOW_EMPTY_THREAD_TOGGLE);
			}
		}
	}

	private static void appendRecordDumpViewActions(List<TuiKeyAction> out) {
		TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.RECORD_VIEW_METHOD);
		TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.RECORD_VIEW_THREAD);
		TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.RECORD_VIEW_STACK);
		TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.RECORD_VIEW_SEARCH);
	}

	private static final int RECORD_DUMP_VIEW_LINE_INDENT_COLS = 19;

	private static void appendShared(StringBuilder sb, boolean outputOn) {
		sb.append("F1:Help, F3:UI Mode");
		appendSharedTail(sb, outputOn);
	}

	private static void appendSharedTail(StringBuilder sb, boolean outputOn) {
		sb.append(", F12:Remap Keys, Tab:Move Window, Shift+Arrows:Resize, Ctrl+C:Exit");
		sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.OUTPUT_PANE_TOGGLE))
			.append(":Output(").append(outputOn ? "ON" : "OFF").append(")");
	}
}
