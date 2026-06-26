package dal.tool.trace.jmxer.tui.support;

import java.util.List;

/**
 * 포커스된 단일 패널용 상태 줄 Window 키 힌트.
 *
 */
public final class TuiPaneKeysHint {
	private static final String PREFIX = " [Window] ";

	private TuiPaneKeysHint() {}

	public static String search() {
		return PREFIX + "Search - Esc:Clear";
	}

	public static String threadSearch(boolean selectionKeysEnabled, boolean stacktraceTargetActive) {
		StringBuilder sb = new StringBuilder(PREFIX);
		sb.append("Search - Esc:Clear");
		if(selectionKeysEnabled) {
			sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.THREAD_TOGGLE_ALL)).append(":Toggle all visible");
			sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.THREAD_CLEAR_SELECTED)).append(":Clear selected");
		}
		sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.THREAD_BULK_STACKTRACE)).append(":").append(stacktraceTargetActive?"Stacktrace active":"Stacktrace selected");
		return sb.toString();
	}

	public static void collectThreadSearch(List<TuiKeyAction> out, boolean selectionKeysEnabled) {
		TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.THREAD_BULK_STACKTRACE);
		if(selectionKeysEnabled) {
			TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.THREAD_TOGGLE_ALL);
			TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.THREAD_CLEAR_SELECTED);
		}
	}

	public static String mbeans() {
		StringBuilder sb = new StringBuilder(PREFIX);
		sb.append("MBeans - ").append(TuiKeyBindings.label(TuiKeyAction.INFO)).append(":Info");
		sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.MBEAN_SET_DEFAULT)).append(":Set current MBean as default");
		return sb.toString();
	}

	public static void collectMbeans(List<TuiKeyAction> out) {
		TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.MBEAN_SET_DEFAULT);
		TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.INFO);
	}

	public static String threadList(boolean selectionKeysEnabled, boolean stacktraceTargetActive) {
		StringBuilder sb = new StringBuilder(PREFIX);
		sb.append("Thread - ").append(TuiKeyBindings.label(TuiKeyAction.INFO)).append(":Info, ");
		sb.append(TuiKeyBindings.label(TuiKeyAction.THREAD_FORCE_REFRESH)).append(":Refresh, ");
		sb.append(TuiKeyBindings.label(TuiKeyAction.THREAD_SORT)).append(":Sort");
		if(selectionKeysEnabled) {
			sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.THREAD_TOGGLE_CURRENT)).append(":Toggle current");
			sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.THREAD_TOGGLE_ALL)).append(":Toggle all visible");
			sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.THREAD_CLEAR_SELECTED)).append(":Clear selected");
		}
		sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.THREAD_PRIMARY_ACTION)).append(":Stacktrace");
		sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.THREAD_BULK_STACKTRACE)).append(":").append(stacktraceTargetActive?"Stacktrace active":"Stacktrace selected");
		return sb.toString();
	}

	public static void collectThreadList(List<TuiKeyAction> out, boolean selectionKeysEnabled) {
		TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.INFO);
		TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.THREAD_FORCE_REFRESH);
		TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.THREAD_SORT);
		TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.THREAD_BULK_STACKTRACE);
		if(selectionKeysEnabled) {
			TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.THREAD_PRIMARY_ACTION);
			TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.THREAD_TOGGLE_CURRENT);
			TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.THREAD_TOGGLE_ALL);
			TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.THREAD_CLEAR_SELECTED);
		}
	}

	public static String recordThreadSearch(boolean liveView) {
		StringBuilder sb = new StringBuilder(PREFIX);
		sb.append("Search - Esc:Clear");
		sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.THREAD_TOGGLE_ALL)).append(":Toggle all visible");
		sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.THREAD_CLEAR_SELECTED)).append(":Clear selected");
		if(liveView) {
			sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.RECORD_SAMPLE_SELECTED)).append(":Sample selected");
			sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.THREAD_BULK_STACKTRACE)).append(":Stacktrace selected");
		} else {
			sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.THREAD_BULK_STACKTRACE)).append(":Analysis selected");
		}
		return sb.toString();
	}

	public static void collectRecordThreadSearch(List<TuiKeyAction> out, boolean liveView) {
		if(liveView) {
			TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.RECORD_SAMPLE_SELECTED);
		}
		TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.THREAD_BULK_STACKTRACE);
		TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.THREAD_TOGGLE_ALL);
		TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.THREAD_CLEAR_SELECTED);
	}

	public static String recordThreadList(boolean liveView) {
		StringBuilder sb = new StringBuilder(PREFIX);
		sb.append("Thread - ").append(TuiKeyBindings.label(TuiKeyAction.INFO)).append(":Info");
		if(liveView) {
			sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.THREAD_FORCE_REFRESH)).append(":Refresh");
		}
		sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.THREAD_SORT)).append(":Sort");
		if(liveView) {
			sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.RECORD_SAMPLE)).append(":Sample");
			sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.RECORD_SAMPLE_SELECTED)).append(":Sample selected");
		}
		return sb.toString();
	}

	public static String recordThreadListSecondLine(boolean liveView) {
		if(!liveView) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < RECORD_THREAD_LIST_LINE_INDENT_COLS; i++) {
			sb.append(' ');
		}
		sb.append(TuiKeyBindings.label(TuiKeyAction.THREAD_TOGGLE_CURRENT)).append(":Toggle current");
		sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.THREAD_TOGGLE_ALL)).append(":Toggle all visible");
		sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.THREAD_CLEAR_SELECTED)).append(":Clear selected");
		sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.THREAD_PRIMARY_ACTION)).append(":Stacktrace");
		sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.THREAD_BULK_STACKTRACE)).append(":Stacktrace selected");
		return sb.toString();
	}

	public static String recordThreadListDumpView(boolean liveView) {
		StringBuilder sb = new StringBuilder(PREFIX);
		sb.append("Thread - ").append(TuiKeyBindings.label(TuiKeyAction.INFO)).append(":Info");
		sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.THREAD_SORT)).append(":Sort");
		sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.THREAD_TOGGLE_CURRENT)).append(":Toggle current");
		sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.THREAD_TOGGLE_ALL)).append(":Toggle all visible");
		sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.THREAD_CLEAR_SELECTED)).append(":Clear selected");
		sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.THREAD_PRIMARY_ACTION)).append(":Analysis");
		sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.THREAD_BULK_STACKTRACE)).append(":Analysis selected");
		return sb.toString();
	}

	private static final int RECORD_THREAD_LIST_LINE_INDENT_COLS = PREFIX.length() + "Thread - ".length();

	public static void collectRecordThreadList(List<TuiKeyAction> out, boolean liveView) {
		TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.INFO);
		if(liveView) {
			TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.THREAD_FORCE_REFRESH);
			TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.RECORD_SAMPLE);
			TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.RECORD_SAMPLE_SELECTED);
		}
		TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.THREAD_SORT);
		TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.THREAD_BULK_STACKTRACE);
		TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.THREAD_PRIMARY_ACTION);
		TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.THREAD_TOGGLE_CURRENT);
		TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.THREAD_TOGGLE_ALL);
		TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.THREAD_CLEAR_SELECTED);
	}

	public static String features() {
		StringBuilder sb = new StringBuilder(PREFIX);
		sb.append("Features - ").append(TuiKeyBindings.label(TuiKeyAction.FEATURE_INVOKE)).append(":Invoke");
		sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.INFO)).append(":Info");
		return sb.toString();
	}

	public static void collectFeatures(List<TuiKeyAction> out) {
		TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.FEATURE_INVOKE);
		TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.INFO);
	}

	public static String output(boolean wrapOn) {
		StringBuilder sb = new StringBuilder(PREFIX);
		sb.append("Output - ").append(TuiKeyBindings.label(TuiKeyAction.OUTPUT_SAVE)).append(":Save to file");
		sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.OUTPUT_WRAP)).append(":Wrap(").append(wrapOn ? "ON" : "OFF").append(")");
		return sb.toString();
	}

	public static void collectOutput(List<TuiKeyAction> out) {
		TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.OUTPUT_SAVE);
		TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.OUTPUT_WRAP);
	}

	public static String command() {
		StringBuilder sb = new StringBuilder(PREFIX);
		sb.append("Command - Up/Down:History, Esc:Clear");
		sb.append(", ").append(TuiKeyBindings.label(TuiKeyAction.COMMAND_PASTE)).append(":Paste from clipboard");
		return sb.toString();
	}

	public static void collectCommand(List<TuiKeyAction> out) {
		TuiKeyRemapCatalog.addUnique(out, TuiKeyAction.COMMAND_PASTE);
	}
}
