package dal.tool.trace.jmxer.tui;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.SimpleTheme;

/**
 * Shared TUI colors and TextBox themes for focus and list selection.
 */
final class JmxTuiTheme {
	static final TextColor FOCUS_PANE_BG = TextColor.ANSI.BLUE;
	static final TextColor SELECTED_ROW_BG = TextColor.ANSI.BLUE;
	static final TextColor SELECTED_ROW_FG = TextColor.ANSI.WHITE;
	static final TextColor SEARCH_MATCH_BG = TextColor.ANSI.YELLOW;
	static final TextColor SEARCH_MATCH_FG = TextColor.ANSI.BLACK;
	static final SimpleTheme FOCUSED_TEXTBOX_THEME = new SimpleTheme(TextColor.ANSI.WHITE, TextColor.ANSI.BLUE);
	static final SimpleTheme UNFOCUSED_TEXTBOX_THEME = new SimpleTheme(TextColor.ANSI.DEFAULT, TextColor.ANSI.BLACK_BRIGHT);

	private JmxTuiTheme() {}
}
