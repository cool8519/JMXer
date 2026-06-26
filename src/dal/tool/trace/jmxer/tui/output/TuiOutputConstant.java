package dal.tool.trace.jmxer.tui.output;

/**
 * TUI Output 패널의 제한값과 사용자에게 보이는 메시지.
 *
 */
public final class TuiOutputConstant {

	/** 사용자 명령당 Output 패널에 표시할 논리 줄 상한. */
	public static final int MAX_LOGICAL_LINES_PER_CAPTURE = 2000;

	public static final String OUTPUT_TRUNCATED_PREFIX = "[Output truncated:";

	public static final String OUTPUT_TRUNCATED_MESSAGE =
			OUTPUT_TRUNCATED_PREFIX + " %d additional line(s) omitted from the pane. Press F10 in the Output pane to save the full result to a file.]";

	public static final String OUTPUT_SAVE_DEFAULT_FILENAME_PREFIX = "JMXer_Output_";
	public static final String OUTPUT_SAVE_DEFAULT_FILENAME_SUFFIX = ".txt";

	public static boolean isTruncationNoticeLine(String line) {
		return line != null && line.startsWith(OUTPUT_TRUNCATED_PREFIX);
	}

	private TuiOutputConstant() {}
}
