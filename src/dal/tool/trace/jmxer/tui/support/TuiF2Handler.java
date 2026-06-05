package dal.tool.trace.jmxer.tui.support;

/**
 * Pane-specific F2 action invoked from the global shortcut dispatcher ({@link dal.tool.trace.jmxer.tui.app.JmxTuiApp#consumeGlobalShortcut}).
 * Panes that do not handle F2 return false.
 */
public interface TuiF2Handler {
	/**
	 * @return true if F2 was consumed for this pane
	 */
	boolean onF2Key();
}
