package dal.tool.trace.jmxer.tui.support;

import dal.tool.trace.jmxer.cli.JmxSettings;

/**
 * Formats status line 3: SET-linked settings for the active UI mode (value + optional toggle key).
 */
public final class TuiStatusSettingLine {
	private static final String LINE_PREFIX = "   - ";
	/** Wide separator between multiple settings on one line. */
	private static final String ITEM_SEP = ", ";
	private static final String PRETTY_PRINT_TOGGLE_KEY = "Ctrl+P";

	private TuiStatusSettingLine() {}

	public static String mbeanOnlineDetailLine(JmxSettings settings) {
		if(settings == null) {
			return LINE_PREFIX;
		}
		return join(item("PRETTY_PRINT", onOff(settings.prettyPrintInResult()), PRETTY_PRINT_TOGGLE_KEY));
	}

	public static String recordDetailLine(JmxSettings settings) {
		if(settings == null) {
			return LINE_PREFIX;
		}
		return join(
				item("RECORD_VIEW", settings.getRecordViewMode().name(), null),
				item("SHOW_EMPTY_THREAD", onOff(settings.showEmptyThreadInRecordView()), null));
	}

	private static String onOff(boolean on) {
		return on ? "ON" : "OFF";
	}

	private static String item(String name, String value, String keyHint) {
		String text = name;
		if(keyHint != null && keyHint.length() > 0) {
			text += "(" + keyHint + ")";
		}
		text += ":" + value;
		return text;
	}

	private static String join(String... items) {
		if(items == null || items.length == 0) {
			return LINE_PREFIX;
		}
		StringBuilder sb = new StringBuilder(LINE_PREFIX);
		for(int i = 0; i < items.length; i++) {
			if(i > 0) {
				sb.append(ITEM_SEP);
			}
			sb.append(items[i]);
		}
		return sb.toString();
	}
}
