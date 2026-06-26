package dal.tool.trace.jmxer.tui.output;

import com.googlecode.lanterna.TerminalTextUtils;

/**
 * CLI 스타일 출력 텍스트를 Lanterna TextBox 표시에 맞게 정규화한다.
 * 터미널은 탭 정지(보통 8열)를 확장하지만 Lanterna는 탭을 한 열로 취급한다.
 *
 */
public final class OutputTextFormat {
	/** 일반 POSIX/Windows 콘솔과 같은 기본 탭 너비. */
	static final int TAB_STOP_WIDTH = 8;

	private OutputTextFormat() {}

	/**
	 * 탭을 다음 탭 정지까지 공백으로 확장한다. 넓은 문자는 터미널 열 너비로 계산한다.
	 */
	public static String expandTabs(String line) {
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
