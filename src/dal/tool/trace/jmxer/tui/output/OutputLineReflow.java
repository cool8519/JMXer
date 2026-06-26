package dal.tool.trace.jmxer.tui.output;

import java.util.ArrayList;
import java.util.List;

import com.googlecode.lanterna.TerminalTextUtils;

/**
 * 저장된 논리 출력 줄을 Output TextBox 표시 줄로 재배치한다.
 *
 */
public final class OutputLineReflow {
	private static final int MIN_WRAP_COLS = 1;

	private OutputLineReflow() {}

	public static List<String> expandedLines(String text) {
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
	 * 자연 너비가 maxCols 이하이면 그대로 두고, 초과 시 단어 경계를 우선해 maxCols에서 줄바꿈한다.
	 */
	public static List<String> formatWithinMaxColumns(String text, int maxCols) {
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

	private static final String HELP_SEGMENT_SEPARATOR = "; ";

	/**
	 * 도움말 텍스트 줄바꿈. 키맵/설명 세그먼트 구분자에서 끊어 한 세그먼트가 한 줄에 오도록 하고,
	 * 이어지는 줄에는 indent를 붙인다.
	 */
	public static List<String> formatHelpWithinMaxColumnsWithIndent(String text, int maxCols, String indent) {
		int cols = Math.max(MIN_WRAP_COLS, maxCols);
		String contIndent = indent == null ? "" : indent;
		List<String> out = new ArrayList<String>();
		List<String> natural = expandedLines(text);
		if(natural.isEmpty()) {
			out.add("");
			return out;
		}
		for(int i = 0; i < natural.size(); i++) {
			String line = natural.get(i);
			if(line == null || line.isEmpty()) {
				out.add("");
				continue;
			}
			if(TerminalTextUtils.getColumnWidth(line) <= cols) {
				out.add(clampToMaxColumns(line, cols));
				continue;
			}
			out.addAll(wrapHelpLineWithIndent(line, cols, contIndent));
		}
		return out;
	}

	/**
	 * 각 줄을 maxCols에서 줄바꿈하고, 이어지는 줄 앞에 indent를 붙인다.
	 */
	public static List<String> formatWithinMaxColumnsWithIndent(String text, int maxCols, String indent) {
		int cols = Math.max(MIN_WRAP_COLS, maxCols);
		String contIndent = indent == null ? "" : indent;
		int indentCols = TerminalTextUtils.getColumnWidth(contIndent);
		int contCols = Math.max(MIN_WRAP_COLS, cols - indentCols);
		List<String> out = new ArrayList<String>();
		List<String> natural = expandedLines(text);
		if(natural.isEmpty()) {
			out.add("");
			return out;
		}
		for(int i = 0; i < natural.size(); i++) {
			String line = natural.get(i);
			if(line == null || line.isEmpty()) {
				out.add("");
				continue;
			}
			if(TerminalTextUtils.getColumnWidth(line) <= cols) {
				out.add(clampToMaxColumns(line, cols));
				continue;
			}
			out.addAll(wrapLineWithIndent(line, cols, contIndent, contCols));
		}
		return out;
	}

	/**
	 * 개행으로 나누고 탭을 확장한 뒤 단어 경계를 우선해 표시용 줄로 감싼다.
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

	static List<String> wrapAndClampText(String text, int maxCols) {
		List<String> wrapped = wrapTextToLines(text, maxCols);
		int cols = Math.max(MIN_WRAP_COLS, maxCols);
		List<String> out = new ArrayList<String>(wrapped.size());
		for(String line : wrapped) {
			out.add(clampToMaxColumns(line, cols));
		}
		return out;
	}

	public static String joinLines(List<String> lines) {
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

	public static int maxDisplayWidth(List<String> lines) {
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

	private static List<String> wrapHelpLineWithIndent(String line, int maxCols, String indent) {
		if(line.indexOf(HELP_SEGMENT_SEPARATOR) < 0) {
			int indentCols = TerminalTextUtils.getColumnWidth(indent);
			int contCols = Math.max(MIN_WRAP_COLS, maxCols - indentCols);
			return wrapLineWithIndent(line, maxCols, indent, contCols);
		}
		return wrapSemicolonSeparatedLine(line, maxCols, indent);
	}

	private static List<String> wrapSemicolonSeparatedLine(String line, int maxCols, String indent) {
		String[] segments = line.split(HELP_SEGMENT_SEPARATOR, -1);
		if(segments.length <= 1) {
			int indentCols = TerminalTextUtils.getColumnWidth(indent);
			int contCols = Math.max(MIN_WRAP_COLS, maxCols - indentCols);
			return wrapLineWithIndent(line, maxCols, indent, contCols);
		}

		List<String> out = new ArrayList<String>();
		StringBuilder current = null;

		for(int i = 0; i < segments.length; i++) {
			String segment = segments[i];
			if(segment == null) {
				segment = "";
			}
			if(current == null) {
				String rowPrefix = out.isEmpty() ? "" : indent;
				String rowStart = rowPrefix + segment;
				if(TerminalTextUtils.getColumnWidth(rowStart) <= maxCols) {
					current = new StringBuilder(rowStart);
				} else {
					out.addAll(wrapOversizedHelpSegment(rowStart, maxCols, indent));
					current = null;
				}
				continue;
			}

			String candidate = current.toString() + HELP_SEGMENT_SEPARATOR + segment;
			if(TerminalTextUtils.getColumnWidth(candidate) <= maxCols) {
				current.setLength(0);
				current.append(candidate);
			} else {
				out.add(current.toString());
				current = null;
				i--;
			}
		}

		if(current != null && current.length() > 0) {
			out.add(current.toString());
		}
		return out;
	}

	private static List<String> wrapOversizedHelpSegment(String line, int maxCols, String indent) {
		int indentCols = TerminalTextUtils.getColumnWidth(indent);
		int contCols = Math.max(MIN_WRAP_COLS, maxCols - indentCols);
		return wrapLineWithIndent(line, maxCols, indent, contCols);
	}

	private static List<String> wrapLineWithIndent(String line, int maxCols, String indent, int contCols) {
		List<String> out = new ArrayList<String>();
		String remaining = line;
		boolean first = true;
		while(remaining != null && remaining.length() > 0) {
			int limit = first ? maxCols : contCols;
			if(TerminalTextUtils.getColumnWidth(remaining) <= limit) {
				out.add(first ? remaining : indent + remaining);
				break;
			}
			int breakAt = findReadableBreakIndex(remaining, limit);
			if(breakAt <= 0) {
				String fitted = TerminalTextUtils.fitString(remaining, limit);
				breakAt = fitted.length();
				if(breakAt <= 0) {
					breakAt = 1;
				}
			}
			String piece = remaining.substring(0, breakAt);
			out.add(first ? piece : indent + piece);
			remaining = remaining.substring(breakAt);
			int start = 0;
			while(start < remaining.length() && remaining.charAt(start) == ' ') {
				start++;
			}
			remaining = remaining.substring(start);
			first = false;
		}
		return out;
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
