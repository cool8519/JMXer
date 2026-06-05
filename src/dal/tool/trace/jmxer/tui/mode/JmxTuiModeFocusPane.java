package dal.tool.trace.jmxer.tui.mode;

/**
 * Identifies a focus target pane within a {@link JmxTuiModeContext}.
 * Used to restore keyboard focus when returning to a UI mode.
 */
public enum JmxTuiModeFocusPane {
	SEARCH,
	MBEANS,
	FEATURES,
	OUTPUT,
	COMMAND
}
