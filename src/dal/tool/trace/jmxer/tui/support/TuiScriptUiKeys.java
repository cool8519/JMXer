package dal.tool.trace.jmxer.tui.support;

/**
 * TUI SCRIPT 대화상자용 사용자 표시 문자열.
 *
 */
public final class TuiScriptUiKeys {

	public static final String DIALOG_TITLE = "Script";
	public static final String CONFIRM_TITLE = "Script";
	public static final String CONFIRM_MESSAGE_PREFIX = "Run this script?";
	public static final String HEADER_LINE = "Executing script...";
	public static final String CANCEL_PENDING_LINE = "Cancellation requested. Finishing the current command...";
	public static final String ROOT_COMMAND_PREFIX = "Command: ";
	public static final String SCRIPT_FILE_PREFIX = "Script file: ";
	public static final String CURRENT_COMMAND_PREFIX = "Current command> ";
	public static final String OUTPUT_HINT = "Command output is shown in the Output pane.";
	public static final String CANCEL_LABEL = "Cancel";
	public static final String CANCEL_HINT = "Press Cancel to stop after the current command finishes.";
	public static final String COMPLETED_TITLE = "Script";
	public static final String COMPLETED_MESSAGE = "Script execution completed successfully.";

	public static final int DIALOG_COMMAND_MAX_COLUMNS = 72;

	private TuiScriptUiKeys() {}
}
