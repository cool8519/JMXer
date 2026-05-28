package dal.tool.trace.jmxer.tui;

/**
 * Layout and border callbacks used by {@link JmxTuiOutputSink} while writing to the Output pane.
 */
interface JmxTuiOutputHost {
	int getOutputInnerColumnsAtWrite();

	void refreshOutputPaneBorderTime();
}
