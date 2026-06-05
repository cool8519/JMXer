package dal.tool.trace.jmxer.tui.output;

/**
 * TUI Output pane behavior for commands that intentionally produce no captured lines.
 */
public final class TuiOutputCommandPolicy {

	private TuiOutputCommandPolicy() {}

	/**
	 * @return true when the command clears the display and should leave Output empty without a placeholder
	 */
	public static boolean isClearOutputCommand(String commandLine) {
		if(commandLine == null) {
			return false;
		}
		String trimmed = commandLine.trim();
		if(trimmed.length() == 0) {
			return false;
		}
		int sp = trimmed.indexOf(' ');
		String verb = (sp < 0 ? trimmed : trimmed.substring(0, sp));
		return "CLS".equalsIgnoreCase(verb) || "CLEAR".equalsIgnoreCase(verb);
	}
}
