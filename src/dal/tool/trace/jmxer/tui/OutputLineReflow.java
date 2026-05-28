package dal.tool.trace.jmxer.tui;

import java.util.ArrayList;
import java.util.List;

import com.googlecode.lanterna.TerminalTextUtils;

/**
 * Reflows stored logical output lines into display lines for the Output TextBox.
 */
final class OutputLineReflow {
	private static final int MIN_WRAP_COLS = 1;

	private OutputLineReflow() {}

	/**
	 * Splits on newlines and expands tabs only (no wrapping).
	 */
	static List<String> expandedLines(String text) {
		List<String> out = new ArrayList<String>();
		if(text == null) {
			return out;
		}
		String[] paragraphs = text.split("\\r?\\n", -1);
		for(int p = 0; p < paragraphs.length; p++) {
			out.add(OutputTextFormat.expandTabs(paragraphs[p]));
		}
		return out;
	}

	/**
	 * Uses natural line widths when they fit {@code maxCols}; otherwise wraps at {@code maxCols} (word breaks preferred).
	 */
	static List<String> formatWithinMaxColumns(String text, int maxCols) {
		int cols = Math.max(MIN_WRAP_COLS, maxCols);
		List<String> natural = expandedLines(text);
		if(natural.isEmpty()) {
			natural.add("");
		}
		if(maxDisplayWidth(natural) <= cols) {
			List<String> out = new ArrayList<String>(natural.size());
			for(String line : natural) {
				out.add(clampToMaxColumns(line, cols));
			}
			return out;
		}
		return wrapAndClampText(text, cols);
	}

	/**
	 * Splits text on newlines, expands tabs, and wraps each line for display (word breaks preferred).
	 */
	static List<String> wrapTextToLines(String text, int maxCols) {
		List<String> out = new ArrayList<String>();
		if(text == null) {
			return out;
		}
		int cols = Math.max(MIN_WRAP_COLS, maxCols);
		String[] paragraphs = text.split("\\r?\\n", -1);
		for(int p = 0; p < paragraphs.length; p++) {
			String line = OutputTextFormat.expandTabs(paragraphs[p]);
			if(line.isEmpty()) {
				out.add("");
			} else {
				out.addAll(wrapLogicalLineReadable(line, cols));
			}
		}
		return out;
	}

	/** Wraps then clamps each line so Lanterna TextBox does not show a horizontal scrollbar. */
	static List<String> wrapAndClampText(String text, int maxCols) {
		List<String> wrapped = wrapTextToLines(text, maxCols);
		int cols = Math.max(MIN_WRAP_COLS, maxCols);
		List<String> out = new ArrayList<String>(wrapped.size());
		for(String line : wrapped) {
			out.add(clampToMaxColumns(line, cols));
		}
		return out;
	}

	static String joinLines(List<String> lines) {
		if(lines == null || lines.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < lines.size(); i++) {
			if(i > 0) {
				sb.append('\n');
			}
			String line = lines.get(i);
			sb.append(line == null ? "" : line);
		}
		return sb.toString();
	}

	static int maxDisplayWidth(List<String> lines) {
		int max = 0;
		if(lines == null) {
			return max;
		}
		for(String line : lines) {
			if(line == null) {
				continue;
			}
			max = Math.max(max, TerminalTextUtils.getColumnWidth(line));
		}
		return max;
	}

	static List<String> toDisplayLines(List<String> logicalLines, boolean wrapMode, int paneColumns) {
		List<String> display = new ArrayList<String>();
		if(logicalLines == null) {
			return display;
		}
		int cols = Math.max(MIN_WRAP_COLS, paneColumns);
		for(String logical : logicalLines) {
			if(logical == null) {
				display.add("");
				continue;
			}
			String line = OutputTextFormat.expandTabs(logical);
			if(!wrapMode) {
				display.add(line);
			} else {
				display.addAll(wrapLogicalLine(line, cols));
			}
		}
		return display;
	}

	private static List<String> wrapLogicalLineReadable(String line, int maxCols) {
		List<String> out = new ArrayList<String>();
		String remaining = line;
		while(remaining != null && remaining.length() > 0) {
			if(TerminalTextUtils.getColumnWidth(remaining) <= maxCols) {
				out.add(remaining);
				break;
			}
			int breakAt = findReadableBreakIndex(remaining, maxCols);
			if(breakAt <= 0) {
				breakAt = TerminalTextUtils.fitString(remaining, maxCols).length();
				if(breakAt <= 0) {
					breakAt = 1;
				}
			}
			out.add(remaining.substring(0, breakAt));
			remaining = remaining.substring(breakAt);
			int start = 0;
			while(start < remaining.length() && remaining.charAt(start) == ' ') {
				start++;
			}
			remaining = remaining.substring(start);
		}
		return out;
	}

	private static int findReadableBreakIndex(String line, int maxCols) {
		int col = 0;
		int lastSpaceEnd = -1;
		for(int i = 0; i < line.length(); ) {
			int cp = line.codePointAt(i);
			String ch = new String(Character.toChars(cp));
			int w = TerminalTextUtils.getColumnWidth(ch);
			if(w <= 0) {
				w = 1;
			}
			if(cp == ' ') {
				lastSpaceEnd = i + Character.charCount(cp);
			}
			if(col + w > maxCols) {
				return lastSpaceEnd;
			}
			col += w;
			i += Character.charCount(cp);
		}
		return -1;
	}

	private static List<String> wrapLogicalLine(String line, int maxCols) {
		List<String> out = new ArrayList<String>();
		if(line.isEmpty()) {
			out.add("");
			return out;
		}
		StringBuilder chunk = new StringBuilder();
		int chunkCols = 0;
		for(int i = 0; i < line.length(); ) {
			int cp = line.codePointAt(i);
			String ch = new String(Character.toChars(cp));
			int w = TerminalTextUtils.getColumnWidth(ch);
			if(w <= 0) {
				w = 1;
			}
			if(chunkCols + w > maxCols && chunk.length() > 0) {
				out.add(chunk.toString());
				chunk.setLength(0);
				chunkCols = 0;
			}
			chunk.append(ch);
			chunkCols += w;
			i += Character.charCount(cp);
		}
		if(chunk.length() > 0) {
			out.add(clampToMaxColumns(chunk.toString(), maxCols));
		}
		return out;
	}

	private static String clampToMaxColumns(String line, int maxCols) {
		if(line == null || line.isEmpty()) {
			return line == null ? "" : line;
		}
		if(TerminalTextUtils.getColumnWidth(line) <= maxCols) {
			return line;
		}
		return TerminalTextUtils.fitString(line, maxCols);
	}
}
