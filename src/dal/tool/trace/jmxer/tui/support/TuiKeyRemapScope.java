package dal.tool.trace.jmxer.tui.support;

/**
 * TUI 키 리매핑 대상 범위.
 *
 */
public enum TuiKeyRemapScope {
	GLOBAL("Global"),
	PANE("Pane"),
	SEARCH("Search"),
	MBEAN("MBean"),
	FEATURES("Features"),
	THREAD("Thread"),
	OUTPUT("Output"),
	COMMAND("Command");

	private final String displayName;

	TuiKeyRemapScope(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
