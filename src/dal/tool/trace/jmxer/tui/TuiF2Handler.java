package dal.tool.trace.jmxer.tui;

/**
 * Pane-specific F2 action invoked from the global shortcut dispatcher ({@link JmxTuiApp#consumeGlobalShortcut}).
 * Panes that do not handle F2 return false.
 */
interface TuiF2Handler {
	/**
	 * @return true if F2 was consumed for this pane
	 */
	boolean onF2Key();
}
