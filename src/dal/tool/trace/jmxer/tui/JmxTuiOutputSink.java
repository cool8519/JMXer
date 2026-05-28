package dal.tool.trace.jmxer.tui;

import com.googlecode.lanterna.gui2.WindowBasedTextGUI;

import dal.tool.cli.Logger;

/**
 * Routes Logger output to the TUI Output pane during user-initiated command sessions.
 */
final class JmxTuiOutputSink implements Logger.OutputSink {
	static final int MAX_OUTPUT_LINES = 2000;
	static final String EMPTY_OUTPUT_PLACEHOLDER = "There is no output to display.";
	private static final int MIN_OUTPUT_SEPARATOR_COLS = 8;

	private final WindowBasedTextGUI gui;
	private final OutputTextBox output;
	private final JmxTuiOutputHost host;
	private final StringBuilder currentLine = new StringBuilder();
	private volatile boolean captureHadSubstantiveLine;
	private int captureEpoch;
	private int captureBaselineLineCount;
	private volatile boolean captureSessionAborted;
	private String pendingCommandLine;

	JmxTuiOutputSink(WindowBasedTextGUI gui, OutputTextBox output, JmxTuiOutputHost host) {
		this.gui = gui;
		this.output = output;
		this.host = host;
	}

	void prepareImplicitCommandOutput() {
		captureEpoch++;
		clearPartialLine();
		Logger.setTuiPanelCapture(false);
	}

	void beginUserCaptureSession(String commandLine) {
		captureEpoch++;
		captureSessionAborted = false;
		clearPartialLine();
		captureHadSubstantiveLine = false;
		captureBaselineLineCount = output.getLogicalLines().size();
		pendingCommandLine = commandLine;
		Logger.setTuiPanelCapture(true);
	}

	/**
	 * Clears Output for a new command, then writes {@code > command} preamble.
	 * Invoked only after {@code beforeExecute} (and mutating-call confirmation) succeed.
	 */
	void commitUserCapturePreamble() {
		if(captureSessionAborted || !Logger.isTuiPanelCapture()) {
			return;
		}
		captureBaselineLineCount = 0;
		captureHadSubstantiveLine = false;
		writeRequestPreamble(pendingCommandLine);
		pendingCommandLine = null;
	}

	/**
	 * User cancelled before the command produced output (e.g. mutating CALL declined).
	 * Removes preamble/output from this session and skips {@link #endUserCaptureSession} placeholder.
	 */
	void abortUserCaptureSession() {
		captureSessionAborted = true;
		captureEpoch++;
		pendingCommandLine = null;
		Logger.setTuiPanelCapture(false);
		clearPartialLine();
		captureHadSubstantiveLine = false;
		int remove = output.getLogicalLines().size() - captureBaselineLineCount;
		if(remove > 0) {
			output.removeLastLogicalLines(remove);
		}
	}

	void endUserCaptureSession() {
		if(captureSessionAborted) {
			captureSessionAborted = false;
			return;
		}
		final int epoch = captureEpoch;
		gui.getGUIThread().invokeLater(new Runnable() {
			@Override
			public void run() {
				if(epoch != captureEpoch) {
					return;
				}
				try {
					if(!captureHadSubstantiveLine) {
						output.appendLogicalLine(EMPTY_OUTPUT_PLACEHOLDER);
						host.refreshOutputPaneBorderTime();
					}
				} finally {
					if(epoch == captureEpoch) {
						Logger.setTuiPanelCapture(false);
						captureHadSubstantiveLine = false;
						clearPartialLine();
					}
				}
			}
		});
	}

	void clearPartialLine() {
		currentLine.setLength(0);
	}

	@Override
	public void print(final Object msg, final boolean newline) {
		if(!Logger.isTuiPanelCapture() && !shouldPassthroughWithoutCapture(msg)) {
			return;
		}
		final String s = (msg == null) ? "null" : msg.toString();
		gui.getGUIThread().invokeLater(new Runnable() {
			@Override
			public void run() {
				if(!Logger.isTuiPanelCapture() && !shouldPassthroughWithoutCapture(s)) {
					return;
				}
				appendInternal(s, newline);
			}
		});
	}

	private void writeRequestPreamble(String commandLine) {
		if(commandLine == null) {
			return;
		}
		String trimmed = commandLine.trim();
		if(trimmed.isEmpty()) {
			return;
		}
		try {
			int paneCols = host.getOutputInnerColumnsAtWrite();
			int paneRows = output.getPaneRowsForLayout();
			int lineHint = output.getLogicalLines().size() + 2;
			int contentCols = OutputTextLayout.contentMaxColumns(paneCols, paneRows, lineHint);
			output.appendLogicalLine("> " + trimmed);
			output.appendLogicalLine(buildOutputRequestSeparator(contentCols));
			host.refreshOutputPaneBorderTime();
		} catch(Exception e) {}
	}

	private void appendInternal(String s, boolean newline) {
		currentLine.append(s);
		if(!newline) {
			return;
		}
		String full = currentLine.toString();
		currentLine.setLength(0);
		String[] parts = full.split("\\r?\\n", -1);
		for(int i = 0; i < parts.length; i++) {
			emitTuiOutputLine(parts[i]);
		}
	}

	private void emitTuiOutputLine(String line) {
		if(shouldSkipTuiOutputLine(line)) {
			return;
		}
		captureHadSubstantiveLine = true;
		output.appendLogicalLine(line);
		host.refreshOutputPaneBorderTime();
	}

	private static boolean shouldPassthroughWithoutCapture(Object msg) {
		if(!Logger.isTuiActive() || msg == null) {
			return false;
		}
		String s = msg.toString().trim();
		if(s.regionMatches(true, 0, "[ERROR]", 0, 7)) {
			return true;
		}
		if(s.regionMatches(true, 0, "[WARNING]", 0, 9)) {
			return true;
		}
		return false;
	}

	private static boolean shouldSkipTuiOutputLine(String line) {
		if(line == null) {
			return true;
		}
		if(line.trim().isEmpty()) {
			return true;
		}
		String t = line.trim();
		if(t.regionMatches(true, 0, "[DEBUG]", 0, 7)) {
			return true;
		}
		if(t.startsWith("ObjectName :")) {
			return true;
		}
		if(t.matches("^[-=]{3,}$")) {
			return true;
		}
		if(t.matches("^#{8,}$")) {
			return true;
		}
		return false;
	}

	static String buildOutputRequestSeparator(int columns) {
		int n = Math.max(MIN_OUTPUT_SEPARATOR_COLS, columns);
		StringBuilder sb = new StringBuilder(n);
		for(int i = 0; i < n; i++) {
			sb.append('-');
		}
		return sb.toString();
	}
}
