package dal.tool.trace.jmxer.tui;

/**
 * Consistent body text for TUI popups (MessageDialog, ListSelectDialog, TextInputDialog, {@link TuiDetailDialog}).
 */
final class TuiDialogText {

	private TuiDialogText() {}

	/**
	 * Prefixes one blank line before body content so the first message line sits below the title row.
	 */
	static String body(String text) {
		if(text == null || text.isEmpty()) {
			return "\n";
		}
		if(text.charAt(0) == '\n') {
			return text;
		}
		return "\n" + text;
	}
}
