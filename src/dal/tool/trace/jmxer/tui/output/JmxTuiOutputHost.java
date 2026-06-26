package dal.tool.trace.jmxer.tui.output;

/**
 * JmxTuiOutputSink가 Output 패널에 쓸 때 사용하는 레이아웃·테두리 콜백.
 *
 */
public interface JmxTuiOutputHost {
	int getOutputInnerColumnsAtWrite();

	void refreshOutputPaneBorderTime();
}
