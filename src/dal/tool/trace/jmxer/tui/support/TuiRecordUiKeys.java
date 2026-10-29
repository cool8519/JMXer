package dal.tool.trace.jmxer.tui.support;

import dal.tool.trace.jmxer.cli.command.JmxThreadCommand;

/**
 * Record UI 모드 전역 키 라벨과 대화상자 문구.
 *
 */
public final class TuiRecordUiKeys {

	public static String dumpStackFilterToggleKeyLabel() {
		return TuiKeyBindings.label(TuiKeyAction.FEATURES_PANE_TOGGLE);
	}

	public static String showEmptyThreadToggleKeyLabel() {
		return TuiKeyBindings.label(TuiKeyAction.RECORD_SHOW_EMPTY_THREAD_TOGGLE);
	}

	public static String viewToggleKeyLabel() {
		return TuiKeyBindings.label(TuiKeyAction.RECORD_VIEW_TYPE_TOGGLE);
	}

	public static final String VIEW_TOGGLE_KEY_LABEL = "F4";
	public static final String SAMPLE_KEY_LABEL = "F11";
	public static final String DUMP_STACK_FILTER_TOGGLE_KEY_LABEL = "Ctrl+F";
	public static final String DUMP_SAVE_KEY_LABEL = "F8";
	public static final String DUMP_LOAD_KEY_LABEL = "F7";
	public static final String DUMP_INFO_KEY_LABEL = "F9";
	public static final String VIEW_LIVE_LABEL = "Live";
	public static final String VIEW_DUMP_LABEL = "Dump";

	public static final String NO_SELECTION_TITLE = "Record";
	public static final String NO_SELECTION_MESSAGE = "Select one or more threads before starting sampling.";
	public static final String NO_CURSOR_THREAD_MESSAGE = "No thread is under the cursor. Move to a thread in the Thread pane first.";

	public static String sampleKeyLabel() {
		return TuiKeyBindings.label(TuiKeyAction.RECORD_SAMPLE);
	}

	public static String sampleSelectedKeyLabel() {
		return TuiKeyBindings.label(TuiKeyAction.RECORD_SAMPLE_SELECTED);
	}

	public static final String SAMPLE_TYPE_DIALOG_TITLE = "Record Sampling";
	public static final String SAMPLE_TYPE_DIALOG_DESCRIPTION = "Choose the sample type:";
	public static final String SAMPLE_TYPE_STACKTRACE_LABEL = "Stacktrace";
	public static final String SAMPLE_TYPE_RESOURCE_LABEL = "Resource";

	public static final String SAMPLING_DIALOG_TITLE = "Stacktrace Sampling";
	public static final String RESOURCE_DURATION_DIALOG_TITLE = "Resource Sampling";
	public static final String SAMPLING_INTERVAL_DESCRIPTION =
		"Sampling interval in milliseconds (default 500):";
	public static final String SAMPLING_TIME_DESCRIPTION =
		"Sampling time in milliseconds (0 = until stopped, default 0):";

	public static String formatSamplingTargetLine(int threadCount) {
		return formatSamplingTargetLine(threadCount, null);
	}

	public static String formatSamplingTargetLine(int threadCount, String threadExpr) {
		if(threadExpr != null) {
			String trimmed = threadExpr.trim();
			if("*".equals(trimmed)) {
				return "Target: all threads (currently " + threadCount + ", includes newly created)";
			}
			if(JmxThreadCommand.isDynamicThreadExpression(trimmed)) {
				return "Target: " + trimmed + " (currently " + threadCount + " thread(s), includes newly created)";
			}
		}
		StringBuilder sb = new StringBuilder();
		sb.append("Target: ").append(threadCount).append(" thread(s)");
		return sb.toString();
	}

	public static final String WARNING_TITLE = "Warning";
	public static final String WARNING_SLOW =
		"- If the sampling interval is short or the number of threads is large, it may be slow.";
	public static final String WARNING_UNLIMITED =
		"- Do not record for too long. Recording will continue until you stop it.";

	public static final String CONFIRM_SAMPLE_TITLE = "Confirm";
	public static final String CONFIRM_SAMPLE_CONTINUE = "Continue";
	public static final String CONFIRM_SAMPLE_CANCEL = "Cancel";

	public static final String PROGRESS_TITLE = "Recording";
	public static final String PROGRESS_STOP_LABEL = "Stop";
	public static final String PROGRESS_HEADER_STACKTRACE = "Recording stacktrace samples...";
	public static final String PROGRESS_HEADER_RESOURCE = "Recording resource usage...";
	public static final String PROGRESS_DURATION_LINE = "Duration: %d s (remaining: %d s)";
	public static final String PROGRESS_ELAPSED_LINE = "Elapsed: %d s";
	public static final String PROGRESS_SAMPLES_LINE = "Samples: %d";
	public static final String PROGRESS_STOP_HINT = "Press Stop when finished.";
	public static final String PROGRESS_STOP_EARLY_HINT = "Press Stop to cancel early.";

	public static final String OUTPUT_STOPPED_BY_USER = "Recording stopped by user request.";

	public static final String STATUS_SAMPLES_SEGMENT = " | Samples:%d";

	public static final String COMPLETE_TITLE = "Recording Complete";
	public static final String COMPLETE_MESSAGE_TEMPLATE =
		"Recorded for %d ms (%d sampled). Switch to Dump view?";

	public static final String NO_DATA_TITLE = "Recording";
	public static final String NO_DATA_MESSAGE = "No data collected.";

	public static final String DUMP_SAVE_DIALOG_TITLE = "Save Dump";
	public static final String DUMP_LOAD_DIALOG_TITLE = "Load Dump";
	public static final String DUMP_FILE_DIALOG_DESCRIPTION = "Dump file path:";
	public static final String DUMP_NO_DATA_TITLE = "Save Dump";
	public static final String DUMP_NO_DATA_MESSAGE =
		"No record result in memory. Please record a stacktrace or load a dump file first.";
	public static final String DUMP_FILE_NOT_FOUND_TITLE = "Load Dump";
	public static final String DUMP_FILE_NOT_FOUND_MESSAGE_TEMPLATE = "The dump file to load does not exist:\n%s";
	public static final String DUMP_ALREADY_LOADED_TITLE = "Load Dump";
	public static final String DUMP_ALREADY_LOADED_MESSAGE_TEMPLATE =
		"The record data has already been loaded from the dump file:\n%s";
	public static final String DUMP_OVERWRITE_CONFIRM_TITLE = "Save Dump";
	public static final String DUMP_OVERWRITE_CONFIRM_MESSAGE_TEMPLATE =
		"The dump file already exists. Overwrite it?\n%s";
	public static final String DUMP_DISCARD_UNSAVED_TITLE = "Load Dump";
	public static final String DUMP_DISCARD_UNSAVED_MESSAGE =
		"Current record data will be lost because it has not yet been saved. Continue?";
	public static final String DUMP_SAVE_SUCCESS_TITLE = "Save Dump";
	public static final String DUMP_SAVE_SUCCESS_MESSAGE_TEMPLATE =
		"Successfully saved stacktrace data to the dump file:\n%s";
	public static final String DUMP_SAVE_FAILED_TITLE = "Save Dump";
	public static final String DUMP_SAVE_FAILED_MESSAGE_TEMPLATE = "Failed to save the dump file: %s";
	public static final String DUMP_LOAD_SUCCESS_TITLE = "Load Dump";
	public static final String DUMP_LOAD_SUCCESS_MESSAGE =
		"Successfully loaded stacktrace data from the dump file.";
	public static final String DUMP_LOAD_FAILED_TITLE = "Load Dump";
	public static final String DUMP_LOAD_FAILED_MESSAGE =
		"Failed to load stacktrace data from the dump file.";

	public static String bulkThreadAnalysisKeyLabel() {
		return TuiKeyBindings.label(TuiKeyAction.THREAD_BULK_STACKTRACE);
	}

	public static final String BULK_THREAD_ANALYSIS_KEY_LABEL = "Ctrl+B";
	public static final String BULK_THREAD_ANALYSIS_CONFIRM_TITLE = "Thread Analysis for Selected Threads";

	public static String bulkThreadAnalysisNoneSelectedMessage() {
		return "No threads are selected. Select threads in the Thread pane or edit the selection with "
			+ TuiKeyBindings.label(TuiKeyAction.THREAD_EDIT_SELECTION) + ".";
	}

	public static final String VIEW_METHOD_KEY_LABEL = "Shift+F1";
	public static final String VIEW_THREAD_KEY_LABEL = "Shift+F2";
	public static final String VIEW_STACK_KEY_LABEL = "Shift+F3";
	public static final String VIEW_SEARCH_KEY_LABEL = "Shift+F4";
	public static final String VIEW_DUMP_KEYS_HINT =
		VIEW_METHOD_KEY_LABEL + ":Method call statistics, "
		+ VIEW_THREAD_KEY_LABEL + ":Thread stacktrace tree, "
		+ VIEW_STACK_KEY_LABEL + ":Stacktrace at point, "
		+ VIEW_SEARCH_KEY_LABEL + ":Search method/class";
	public static final String VIEW_DUMP_HELP_LINE =
		VIEW_METHOD_KEY_LABEL + "/" + VIEW_THREAD_KEY_LABEL + "/" + VIEW_STACK_KEY_LABEL + "/"
		+ VIEW_SEARCH_KEY_LABEL
		+ ": record view method/thread/stack/search for selected threads (Dump view only)";

	public static final String VIEW_METHOD_DIALOG_TITLE = "Record View Method";
	public static final String VIEW_METHOD_DIALOG_DESCRIPTION = "Aggregate method call statistics for the selected threads.";
	public static final String VIEW_THREAD_DIALOG_TITLE = "Record View Thread";
	public static final String VIEW_THREAD_DIALOG_DESCRIPTION = "Aggregate stacktrace results per thread for the selected threads.";

	public static final String VIEW_RANGE_TITLE = "RangeExpression:";
	public static final String VIEW_RANGE_HINT = "[Optional] Time range to aggregate.";
	public static final String VIEW_EXPRESSION_TYPE_CAPTION = "- Type: (Press Enter to input an example range value)";
	public static final String VIEW_EXPRESSION_VALUE_CAPTION = "- Value: (Leave empty for the full record)";
	public static final String VIEW_EXPRESSION_POINT_TYPE_CAPTION = "- Type: (Press Enter to input an example point value)";
	public static final String VIEW_EXPRESSION_POINT_VALUE_CAPTION = "- Value:";
	public static final String VIEW_EXPRESSION_TYPE_ABSOLUTE_LABEL = "Absolute time";
	public static final String VIEW_EXPRESSION_TYPE_RELATIVE_LABEL = "Relative time";
	public static final String VIEW_EXPRESSION_TYPE_SAMPLE_LABEL = "Sample order";
	public static final String VIEW_THREAD_MERGE_TITLE = "Merge option:";
	public static final String VIEW_THREAD_MERGE_HINT = "Split shows each thread separately. Merge combines all selected threads.";
	public static final String VIEW_MERGE_SELECTION_CAPTION = "- Selection:";
	public static final String VIEW_THREAD_MERGE_SPLIT_LABEL = "Split (default)";
	public static final String VIEW_THREAD_MERGE_MERGE_LABEL = "Merge";

	public static final String VIEW_STACK_DIALOG_TITLE = "Record View Stack";
	public static final String VIEW_STACK_DIALOG_DESCRIPTION = "Show stacktraces at a specific point in time for the selected threads.";
	public static final String VIEW_STACK_POINT_TITLE = "PointExpression:";
	public static final String VIEW_STACK_POINT_HINT = "[Required] Time point to show stacktrace.";

	public static final String VIEW_SEARCH_DIALOG_TITLE = "Record View Search";
	public static final String VIEW_SEARCH_DIALOG_DESCRIPTION = "Find method or class call information within the record result for the selected threads.";
	public static final String VIEW_SEARCH_NAME_TITLE = "NameExpression:";
	public static final String VIEW_SEARCH_NAME_HINT = "[Required] Search pattern for method or class.";
	public static final String VIEW_SEARCH_VALUE_CAPTION = "- Value: (Case-sensitive pattern with at least 3 characters. '*' and '?' supported)";

	public static final int VIEW_SEARCH_MIN_PATTERN_LENGTH = 3;
	public static final int VIEW_EXPRESSION_TYPE_LIST_ROWS = 3;
	public static final int VIEW_MERGE_LIST_ROWS = 2;

	public static final String VIEW_VALIDATION_TITLE = "Validation";
	public static final String VIEW_RANGE_INVALID_FORMAT_MESSAGE =
		"Invalid RangeExpression format. Use absolute time (yyyy.MM.dd/HH:mm:ss~...), "
		+ "relative time (1000ms~5000ms), or sample order (10~20).";
	public static final String VIEW_RANGE_RELATIVE_OUT_OF_RANGE_MESSAGE =
		"Relative time range is out of bounds. It must be 0 to %dms.";
	public static final String VIEW_RANGE_SAMPLE_OUT_OF_RANGE_MESSAGE =
		"Sample order range is out of bounds. It must be 1 to %d.";
	public static final String VIEW_RANGE_ABSOLUTE_OUT_OF_RANGE_MESSAGE =
		"Absolute time range is out of bounds. It must be %s to %s.";
	public static final String VIEW_POINT_REQUIRED_MESSAGE =
		"PointExpression is required. Enter an absolute time, relative time (e.g. 1500ms), or sample order (e.g. 13).";
	public static final String VIEW_POINT_INVALID_FORMAT_MESSAGE =
		"Invalid PointExpression format. Use absolute time (yyyy.MM.dd/HH:mm:ss), relative time (e.g. 1500ms), or sample order (e.g. 13).";
	public static final String VIEW_POINT_RELATIVE_OUT_OF_RANGE_MESSAGE =
		"Relative time is out of bounds. It must be 0 to %dms.";
	public static final String VIEW_POINT_SAMPLE_OUT_OF_RANGE_MESSAGE =
		"Sample order is out of bounds. It must be 1 to %d.";
	public static final String VIEW_POINT_ABSOLUTE_OUT_OF_RANGE_MESSAGE =
		"Absolute time is out of bounds. It must be %s to %s.";
	public static final String VIEW_SEARCH_PATTERN_REQUIRED_MESSAGE =
		"NameExpression is required. Enter a search pattern with at least 3 characters.";

	private TuiRecordUiKeys() {}
}
