package dal.tool.trace.jmxer.tui.output;

/**
 * Layout and border callbacks used by {@link JmxTuiOutputSink} while writing to the Output pane.
 */
public interface JmxTuiOutputHost {
	int getOutputInnerColumnsAtWrite();

	void refreshOutputPaneBorderTime();
}
