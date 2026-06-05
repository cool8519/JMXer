package dal.tool.trace.jmxer.tui.support;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.SimpleTheme;

/**
 * Shared TUI colors and TextBox themes for focus and list selection.
 */
public final class JmxTuiTheme {
	public static final TextColor FOCUS_PANE_BG = TextColor.ANSI.BLUE;
	public static final TextColor SELECTED_ROW_BG = TextColor.ANSI.BLUE;
	public static final TextColor SELECTED_ROW_FG = TextColor.ANSI.WHITE;
	public static final TextColor SEARCH_MATCH_BG = TextColor.ANSI.YELLOW;
	public static final TextColor SEARCH_MATCH_FG = TextColor.ANSI.BLACK;
	public static final TextColor THREAD_SELECTED_BG = TextColor.ANSI.CYAN;
	public static final TextColor THREAD_SELECTED_FG = TextColor.ANSI.BLACK;
	/** Created thread on default row background. */
	public static final TextColor THREAD_CREATED_FG = TextColor.ANSI.GREEN;
	/** Destroyed thread on default row background. */
	public static final TextColor THREAD_DESTROYED_FG = TextColor.ANSI.RED;
	/**
	 * Created thread when the row has cursor focus (blue background).
	 * Use the same high-contrast fg as normal cursor rows; creation is shown via row prefix {@code +}.
	 */
	public static final TextColor THREAD_CREATED_ON_CURSOR_FG = SELECTED_ROW_FG;
	/** Destroyed thread when the row has cursor focus (blue background). */
	public static final TextColor THREAD_DESTROYED_ON_CURSOR_FG = TextColor.ANSI.RED_BRIGHT;
	/**
	 * Created thread when the row is multi-selected (cyan background).
	 * Use the same high-contrast fg as normal selected rows; creation is shown via row prefix {@code +}.
	 */
	public static final TextColor THREAD_CREATED_ON_SELECTED_FG = THREAD_SELECTED_FG;
	/** Destroyed thread when the row is multi-selected (cyan background). */
	public static final TextColor THREAD_DESTROYED_ON_SELECTED_FG = TextColor.ANSI.RED;
	/** Output pane truncation notice (bright white on yellow; readable over TextBox blue theme). */
	public static final TextColor OUTPUT_TRUNCATED_FG = TextColor.ANSI.WHITE_BRIGHT;
	public static final TextColor OUTPUT_TRUNCATED_BG = TextColor.ANSI.YELLOW;
	public static final SimpleTheme FOCUSED_TEXTBOX_THEME = new SimpleTheme(TextColor.ANSI.WHITE, TextColor.ANSI.BLUE);
	public static final SimpleTheme UNFOCUSED_TEXTBOX_THEME = new SimpleTheme(TextColor.ANSI.DEFAULT, TextColor.ANSI.BLACK_BRIGHT);

	public static TextColor resolveThreadRowForeground(
			boolean created,
			boolean destroyed,
			boolean multiSelected,
			boolean cursorRow) {
		// Multi-select uses cyan background even when the cursor is on the same row.
		// Resolve foreground for that background before cursor-only (blue) colors.
		if(created) {
			if(multiSelected) {
				return THREAD_CREATED_ON_SELECTED_FG;
			}
			if(cursorRow) {
				return THREAD_CREATED_ON_CURSOR_FG;
			}
			return THREAD_CREATED_FG;
		}
		if(destroyed) {
			if(multiSelected) {
				return THREAD_DESTROYED_ON_SELECTED_FG;
			}
			if(cursorRow) {
				return THREAD_DESTROYED_ON_CURSOR_FG;
			}
			return THREAD_DESTROYED_FG;
		}
		if(multiSelected) {
			return THREAD_SELECTED_FG;
		}
		if(cursorRow) {
			return SELECTED_ROW_FG;
		}
		return null;
	}

	private JmxTuiTheme() {}
}
