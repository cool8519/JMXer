package dal.tool.trace.jmxer.tui.support;

/**
 * TUI Output 패널 내용을 파일로 저장할 때 쓰는 사용자 표시 문자열.
 *
 */
public final class TuiOutputUiKeys {

	public static final String SAVE_KEY_LABEL = "F10";

	public static final String SAVE_DIALOG_TITLE = "Save Output";
	public static final String SAVE_DIALOG_DESCRIPTION = "Output file path:";
	public static final String NO_OUTPUT_TITLE = "No output";
	public static final String NO_OUTPUT_MESSAGE = "There is no output to save.";
	public static final String OVERWRITE_CONFIRM_TITLE = "Save Output";
	public static final String OVERWRITE_CONFIRM_MESSAGE_TEMPLATE =
		"The file already exists. Overwrite it?\n%s";
	public static final String SAVE_SUCCESS_TITLE = "Saved";
	public static final String SAVE_SUCCESS_MESSAGE_TEMPLATE = "Output saved to file:\n%s";
	public static final String SAVE_FAILED_TITLE = "Save failed";
	public static final String SAVE_FAILED_MESSAGE_TEMPLATE = "Failed to save output: %s";

	private TuiOutputUiKeys() {}
}
