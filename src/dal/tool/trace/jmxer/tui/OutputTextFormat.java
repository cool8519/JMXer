package dal.tool.trace.jmxer.tui;

import com.googlecode.lanterna.TerminalTextUtils;

/**
 * Normalizes CLI-style output text for Lanterna TextBox display.
 * Terminals expand tab stops (typically every 8 columns); Lanterna treats {@code \t} as a single column.
 */
final class OutputTextFormat {
	/** Same default tab width as typical POSIX/Windows consoles. */
	static final int TAB_STOP_WIDTH = 8;

	private OutputTextFormat() {}

	/**
	 * Expands {@code \t} to spaces up to the next tab stop, using terminal column width for wide characters.
	 */
	static String expandTabs(String line) {
		if(line == null || line.isEmpty()) {
			return line == null ? "" : line;
		}
		if(line.indexOf('\t') < 0) {
			return line;
		}
		StringBuilder out = new StringBuilder(line.length() + TAB_STOP_WIDTH);
		int col = 0;
		for(int i = 0; i < line.length(); ) {
			int cp = line.codePointAt(i);
			if(cp == '\t') {
				int spaces = spacesToNextTabStop(col);
				for(int s = 0; s < spaces; s++) {
					out.append(' ');
				}
				col += spaces;
				i++;
				continue;
			}
			String ch = new String(Character.toChars(cp));
			out.append(ch);
			int w = TerminalTextUtils.getColumnWidth(ch);
			col += w <= 0 ? 1 : w;
			i += Character.charCount(cp);
		}
		return out.toString();
	}

	private static int spacesToNextTabStop(int column) {
		int rem = column % TAB_STOP_WIDTH;
		return rem == 0 ? TAB_STOP_WIDTH : TAB_STOP_WIDTH - rem;
	}
}
