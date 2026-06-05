package dal.tool.trace.jmxer.tui.output;

/**
 * Limits and user-visible messages for the TUI Output pane.
 */
public final class TuiOutputConstant {

	/** Maximum substantive logical lines shown in the Output pane per user command. */
	public static final int MAX_LOGICAL_LINES_PER_CAPTURE = 2000;

	public static final String OUTPUT_TRUNCATED_PREFIX = "[Output truncated:";

	public static final String OUTPUT_TRUNCATED_MESSAGE =
			OUTPUT_TRUNCATED_PREFIX + " %d additional line(s) omitted from the pane. Press Ctrl+S in the Output pane to save the full result to a file.]";

	public static boolean isTruncationNoticeLine(String line) {
		return line != null && line.startsWith(OUTPUT_TRUNCATED_PREFIX);
	}

	private TuiOutputConstant() {}
}
