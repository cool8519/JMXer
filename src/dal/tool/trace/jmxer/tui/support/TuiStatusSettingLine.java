package dal.tool.trace.jmxer.tui.support;

import dal.tool.trace.jmxer.cli.JmxSettings;

/**
 * 상태 줄 3 형식: 활성 UI 모드의 SET 연동 설정(값·토글 키).
 *
 */
public final class TuiStatusSettingLine {
	private static final String LINE_PREFIX = "   - ";
	private static final String ITEM_SEP = ", ";

	private TuiStatusSettingLine() {}

	public static String mbeanOnlineDetailLine(JmxSettings settings) {
		if(settings == null) {
			return LINE_PREFIX;
		}
		return join(item("PRETTY_PRINT", onOff(settings.prettyPrintInResult()),
			TuiKeyBindings.label(TuiKeyAction.OUTPUT_PRETTY_PRINT)));
	}

	public static String recordDetailLine(JmxSettings settings) {
		if(settings == null) {
			return LINE_PREFIX;
		}
		return join(
				item("DUMP_STACK_FILTER", settings.getDumpStackFilter().name(),
					TuiRecordUiKeys.dumpStackFilterToggleKeyLabel()),
				item("SHOW_EMPTY_THREAD", onOff(settings.showEmptyThreadInRecordView()),
					TuiRecordUiKeys.showEmptyThreadToggleKeyLabel()));
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
