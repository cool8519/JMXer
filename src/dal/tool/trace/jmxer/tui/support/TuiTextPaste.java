package dal.tool.trace.jmxer.tui.support;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.gui2.TextBox;

/**
 * Inserts clipboard or other text into a {@link TextBox} at the caret.
 */
public final class TuiTextPaste {

	private TuiTextPaste() {}

	public static boolean pasteIntoCommandLine(TextBox box) {
		String clip = TuiClipboard.readString();
		if(clip == null || clip.length() == 0) {
			return false;
		}
		String insert = normalizeCommandPaste(clip);
		if(insert.length() == 0) {
			return false;
		}
		insertAtCaret(box, insert);
		return true;
	}

	static String normalizeCommandPaste(String raw) {
		if(raw == null || raw.length() == 0) {
			return "";
		}
		String line = raw.replace('\r', '\n');
		int newline = line.indexOf('\n');
		if(newline >= 0) {
			line = line.substring(0, newline);
		}
		StringBuilder buffer = new StringBuilder(line.length());
		for(int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if(c == '\t' || !Character.isISOControl(c)) {
				buffer.append(c);
			}
		}
		return buffer.toString();
	}

	static void insertAtCaret(TextBox box, String insert) {
		if(box == null || insert == null || insert.length() == 0) {
			return;
		}
		String text = box.getText();
		if(text == null) {
			text = "";
		}
		int col = 0;
		TerminalPosition caret = box.getCaretPosition();
		if(caret != null) {
			col = caret.getColumn();
		}
		if(col < 0) {
			col = 0;
		}
		if(col > text.length()) {
			col = text.length();
		}
		String newText = text.substring(0, col) + insert + text.substring(col);
		box.setText(newText);
		box.setCaretPosition(0, col + insert.length());
	}
}
