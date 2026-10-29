package dal.tool.trace.jmxer.tui.support;

/**
 * Thread UI 모드 단축키 라벨과 사용자 표시 문자열.
 *
 */
public final class TuiThreadUiKeys {
	public static final String EDIT_SELECTION_KEY_LABEL = "Ctrl+E";
	public static final String STACKTRACE_TARGET_TOGGLE_KEY_LABEL = "Ctrl+T";
	public static final String SELECTED_TARGET_TOGGLE_KEY_LABEL = "Ctrl+G";
	public static final String REFRESH_TOGGLE_KEY_LABEL = "Ctrl+R";
	public static final String FORCE_REFRESH_KEY_LABEL = "F5";
	public static final String SORT_KEY_LABEL = "F6";
	public static String bulkStacktraceKeyLabel() {
		return TuiKeyBindings.label(TuiKeyAction.THREAD_BULK_STACKTRACE);
	}

	public static final String BULK_STACKTRACE_KEY_LABEL = "Ctrl+B";

	public static final int DEFAULT_REFRESH_INTERVAL_SECONDS = 5;
	public static final int MIN_REFRESH_INTERVAL_SECONDS = 1;
	public static final int MAX_REFRESH_INTERVAL_SECONDS = 3600;

	public static final String REFRESH_DIALOG_TITLE = "Thread Auto Refresh";
	public static final String REFRESH_DIALOG_DESCRIPTION =
		"Set the auto-refresh interval in seconds.\n"
		+ "Apply enables auto refresh at the chosen interval.\n"
		+ "Cancel keeps manual refresh.";
	public static final String REFRESH_DIALOG_APPLY_LABEL = "Apply (Auto Refresh)";
	public static final String REFRESH_DIALOG_CANCEL_LABEL = "Cancel (Manual)";
	public static final String REFRESH_DIALOG_VALIDATION_TITLE = "Invalid Refresh Interval";
	public static final String REFRESH_DIALOG_VALIDATION_MESSAGE =
		"The interval must be an integer between "
		+ MIN_REFRESH_INTERVAL_SECONDS + " and " + MAX_REFRESH_INTERVAL_SECONDS + " seconds.";

	public static final String SELECTION_REQUIRES_SELECTED_TITLE = "Thread Selection Unavailable";

	public static String selectionRequiresSelectedMessage() {
		return "Thread selection is disabled while STACKTRACE_TARGET is ACTIVE.\n\n"
			+ "Switch STACKTRACE_TARGET to SELECTED to select threads manually?\n"
			+ "Use " + TuiKeyBindings.label(TuiKeyAction.THREAD_STACKTRACE_TARGET)
			+ " or SET STACKTRACE_TARGET SELECTED.";
	}

	public static final String BULK_STACKTRACE_CONFIRM_TITLE = "Stacktrace for Selected Threads";
	public static final String BULK_STACKTRACE_ACTIVE_CONFIRM_TITLE = "Stacktrace for Active Threads";
	public static final String BULK_STACKTRACE_ACTIVE_CONFIRM_MESSAGE =
		"Stack traces will be fetched for all threads that passed a known WAS/framework entry point "
		+ "and written to the Output pane.\n\n"
		+ "This may temporarily increase load on the target JVM and affect its performance.\n\n"
		+ "Continue?";
	public static String bulkStacktraceNoneSelectedMessage() {
		return "No threads are selected. Select threads in the Thread pane or edit the selection with "
			+ TuiKeyBindings.label(TuiKeyAction.THREAD_EDIT_SELECTION) + ".";
	}
	public static final String BULK_STACKTRACE_CONFIRM_MESSAGE_PREFIX =
		"Stack traces will be fetched for ";
	public static final String BULK_STACKTRACE_CONFIRM_MESSAGE_SUFFIX =
		" selected thread(s) and written to the Output pane.\n\n"
		+ "This may temporarily increase load on the target JVM and affect its performance.\n\n"
		+ "Continue?";

	public static final String DIALOG_TITLE = "Edit Thread Selection";
	public static final String DIALOG_DESCRIPTION =
		"ThreadList expression (same as THREAD command):\n"
		+ "  * — all threads\n"
		+ "  \"name\" or \"pattern*\" — thread names\n"
		+ "  id or id-range — thread IDs (e.g. 41, 43-44)\n"
		+ "Leave empty and press OK to clear all selections.";
	public static final String VALIDATION_DIALOG_TITLE = "Invalid ThreadList Expression";
	public static final String PARSE_FALLBACK_MESSAGE = "The ThreadList expression could not be parsed.";

	public static final String SORT_DIALOG_TITLE = "Sort Thread List";
	public static final String SORT_DIALOG_DESCRIPTION =
		"Choose a column and sort order, then press Apply.\n"
		+ "Unavailable CPU or MEM/s values are listed after measurable values.";
	public static final String SORT_DIALOG_COLUMN_LABEL = "Column:";
	public static final String SORT_DIALOG_ORDER_LABEL = "Order:";
	public static final String SORT_DIALOG_SELECTION_LABEL_PREFIX = "Selection: ";
	public static final String SORT_DIALOG_APPLY_LABEL = "Apply";
	public static final String SORT_DIALOG_CANCEL_LABEL = "Cancel";

	public static final String REQUEST_ENTRY_FILE_DIALOG_TITLE = "Request-Entry File";
	public static final String REQUEST_ENTRY_FILE_DIALOG_DESCRIPTION =
		"Path to a file of user-defined request-entry signatures.\n\n"
		+ "Leave empty to disable user-defined entries.\n\n"
		+ "Examples: (Write one 'package.Class.method' per line.)\n"
		+ "  com.example.web.DispatcherServlet.service\n"
		+ "  org.springframework.web.servlet.FrameworkServlet.doGet";
	public static final String REQUEST_ENTRY_FILE_DIALOG_PATH_LABEL = "File path:";
	public static final String REQUEST_ENTRY_FILE_STATUS_DISABLED = "Disabled";
	public static final String REQUEST_ENTRY_FILE_STATUS_EXISTS = "Detected file";
	public static final String REQUEST_ENTRY_FILE_STATUS_NOT_FOUND = "Not found file";

	private TuiThreadUiKeys() {}
}
