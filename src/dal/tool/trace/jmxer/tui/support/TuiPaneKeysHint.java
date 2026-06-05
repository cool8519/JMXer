package dal.tool.trace.jmxer.tui.support;

/**
 * Status-line window key hints for a single focused pane.
 */
public final class TuiPaneKeysHint {
	private static final String PREFIX = " [Window] ";

	private TuiPaneKeysHint() {}

	public static String search() {
		return PREFIX + "Search - Esc:Clear";
	}

	public static String threadSearch(boolean selectionKeysEnabled) {
		if(selectionKeysEnabled) {
			return PREFIX + "Search - Esc:Clear, Ctrl+Shift+A:Toggle all visible, Ctrl+Shift+D:Clear selected";
		}
		return PREFIX + "Search - Esc:Clear";
	}

	public static String mbeans() {
		return PREFIX + "MBeans - Enter:Set current MBean as default, F2:Info";
	}

	public static String threadList(boolean selectionKeysEnabled) {
		StringBuilder sb = new StringBuilder(PREFIX);
		sb.append("Thread - ");
		sb.append(TuiThreadUiKeys.FORCE_REFRESH_KEY_LABEL).append(":Refresh, F2:Info, Enter:Stacktrace");
		if(selectionKeysEnabled) {
			sb.append(", Space:Toggle current, Ctrl+Shift+A:Toggle all visible, Ctrl+Shift+D:Clear selected");
		}
		return sb.toString();
	}

	public static String features() {
		return PREFIX + "Features - Enter:Invoke, F2:Info";
	}

	public static String output(boolean wrapOn) {
		return PREFIX + "Output - Ctrl+S:Save to file, Ctrl+L:Wrap(" + (wrapOn ? "ON" : "OFF") + ")";
	}

	public static String command() {
		return PREFIX + "Command - Up/Down:History, Esc:Clear, Ctrl+V:Paste from clipboard";
	}
}
