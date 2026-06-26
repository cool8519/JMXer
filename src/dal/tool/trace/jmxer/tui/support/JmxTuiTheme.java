package dal.tool.trace.jmxer.tui.support;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.SimpleTheme;

/**
 * TUI 공통 색상과 TextBox 포커스·목록 선택 테마.
 *
 */
public final class JmxTuiTheme {
	public static final TextColor FOCUS_PANE_BG = TextColor.ANSI.BLUE;
	public static final TextColor SELECTED_ROW_BG = TextColor.ANSI.BLUE;
	public static final TextColor SELECTED_ROW_FG = TextColor.ANSI.WHITE;
	public static final TextColor KEY_REMAP_CAPTURE_BG = TextColor.ANSI.YELLOW;
	public static final TextColor KEY_REMAP_CAPTURE_FG = TextColor.ANSI.BLACK;
	public static final TextColor SEARCH_MATCH_BG = TextColor.ANSI.YELLOW;
	public static final TextColor SEARCH_MATCH_FG = TextColor.ANSI.BLACK;
	public static final TextColor THREAD_SELECTED_BG = TextColor.ANSI.CYAN;
	public static final TextColor THREAD_SELECTED_FG = TextColor.ANSI.BLACK;
	public static final TextColor THREAD_CREATED_FG = TextColor.ANSI.GREEN;
	public static final TextColor THREAD_DESTROYED_FG = TextColor.ANSI.RED;
	public static final TextColor THREAD_CREATED_ON_CURSOR_FG = SELECTED_ROW_FG;
	public static final TextColor THREAD_DESTROYED_ON_CURSOR_FG = TextColor.ANSI.RED_BRIGHT;
	public static final TextColor THREAD_CREATED_ON_SELECTED_FG = THREAD_SELECTED_FG;
	public static final TextColor THREAD_DESTROYED_ON_SELECTED_FG = TextColor.ANSI.RED;
	public static final TextColor OUTPUT_TRUNCATED_FG = TextColor.ANSI.WHITE_BRIGHT;
	public static final TextColor OUTPUT_TRUNCATED_BG = TextColor.ANSI.YELLOW;
	public static final SimpleTheme FOCUSED_TEXTBOX_THEME = new SimpleTheme(TextColor.ANSI.WHITE, TextColor.ANSI.BLUE);
	public static final SimpleTheme UNFOCUSED_TEXTBOX_THEME = new SimpleTheme(TextColor.ANSI.DEFAULT, TextColor.ANSI.BLACK_BRIGHT);
	public static final TextColor TEXTBOX_CARET_FG = TextColor.ANSI.WHITE_BRIGHT;
	public static final TextColor TEXTBOX_CARET_BG = TextColor.ANSI.BLACK;

	public static TextColor resolveThreadRowForeground(
			boolean created,
			boolean destroyed,
			boolean multiSelected,
			boolean cursorRow) {
		// 다중 선택이어도 커서 행은 파란 배경을 우선한다.
		if(created) {
			if(cursorRow) {
				return THREAD_CREATED_ON_CURSOR_FG;
			}
			if(multiSelected) {
				return THREAD_CREATED_ON_SELECTED_FG;
			}
			return THREAD_CREATED_FG;
		}
		if(destroyed) {
			if(cursorRow) {
				return THREAD_DESTROYED_ON_CURSOR_FG;
			}
			if(multiSelected) {
				return THREAD_DESTROYED_ON_SELECTED_FG;
			}
			return THREAD_DESTROYED_FG;
		}
		if(cursorRow) {
			return SELECTED_ROW_FG;
		}
		if(multiSelected) {
			return THREAD_SELECTED_FG;
		}
		return null;
	}

	private JmxTuiTheme() {}
}
