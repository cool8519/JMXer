package dal.tool.trace.jmxer.tui.support;

/**
 * TUI Sleep 진행 대화상자용 사용자 표시 문자열.
 *
 */
public final class TuiSleepUiKeys {

	public static final String DIALOG_TITLE = "Sleep";
	public static final String HEADER_LINE = "Waiting for sleep to complete...";
	public static final String DURATION_LINE = "Duration: %d s (remaining: %d s)";
	public static final String CANCEL_LABEL = "Cancel";
	public static final String CANCEL_HINT = "Press Cancel to stop waiting.";

	private TuiSleepUiKeys() {}
}
