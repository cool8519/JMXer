package dal.tool.trace.jmxer.tui.dialog;

/**
 * Consistent body text for TUI popups (MessageDialog, ListSelectDialog, TextInputDialog, {@link TuiDetailDialog}).
 */
final class TuiDialogText {

	private TuiDialogText() {}

	/**
	 * Prefixes one blank line before body content so the first message line sits below the title row.
	 * Strips any leading line breaks from {@code text} first (CLI capture often starts with {@code logln("")}
	 * and may use {@code \r\n} on Windows) so the dialog shows exactly one blank line, not two.
	 */
	static String body(String text) {
		if(text == null || text.isEmpty()) {
			return "\n";
		}
		String trimmed = stripLeadingLineBreaks(text);
		if(trimmed.isEmpty()) {
			return "\n";
		}
		return "\n" + trimmed;
	}

	private static String stripLeadingLineBreaks(String text) {
		int start = 0;
		while(start < text.length()) {
			char c = text.charAt(start);
			if(c == '\n') {
				start++;
				continue;
			}
			if(c == '\r') {
				if(start + 1 < text.length() && text.charAt(start + 1) == '\n') {
					start += 2;
				} else {
					start++;
				}
				continue;
			}
			break;
		}
		return text.substring(start);
	}
}
