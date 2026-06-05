package dal.tool.trace.jmxer.tui.support;

/**
 * Status-line global key hints for the current UI mode.
 * SET-linked toggles (e.g. PRETTY_PRINT) are shown on status line 3, not here.
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
			sb.append(", Ctrl+F:Features(").append(featuresOn ? "ON" : "OFF").append(")");
		}
		return sb.toString();
	}

	public static String thread(boolean outputOn, boolean stacktraceTargetActive) {
		StringBuilder sb = new StringBuilder(PREFIX);
		sb.append("Thread - ");
		appendShared(sb, outputOn);
		String bulkHint = stacktraceTargetActive ? "Stacktrace active" : "Stacktrace selected";
		sb.append(", ").append(TuiThreadUiKeys.BULK_STACKTRACE_KEY_LABEL).append(":").append(bulkHint);
		return sb.toString();
	}

	public static String record(boolean outputOn) {
		return PREFIX + "Record - " + sharedKeys(outputOn);
	}

	private static String sharedKeys(boolean outputOn) {
		StringBuilder sb = new StringBuilder();
		appendShared(sb, outputOn);
		return sb.toString();
	}

	private static void appendShared(StringBuilder sb, boolean outputOn) {
		sb.append("F1:Help, F3:UI Mode, Tab:Move Window, Shift+Arrows:Resize, Ctrl+C:Exit");
		sb.append(", Ctrl+O:Output(").append(outputOn ? "ON" : "OFF").append(")");
	}
}
