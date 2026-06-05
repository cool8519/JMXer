package dal.tool.trace.jmxer.tui.output;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.googlecode.lanterna.gui2.WindowBasedTextGUI;

import dal.tool.cli.Logger;

/**
 * Routes Logger output to the TUI Output pane during user-initiated command sessions.
 */
public final class JmxTuiOutputSink implements Logger.OutputSink {
	public static final String EMPTY_OUTPUT_PLACEHOLDER = "There is no output to display.";
	private static final int MIN_OUTPUT_SEPARATOR_COLS = 8;
	private static final long SHUTDOWN_AWAIT_MS = 500L;

	private final WindowBasedTextGUI gui;
	private OutputTextBox output;
	private final JmxTuiOutputHost host;
	private final ExecutorService outputPrepareExecutor;
	private final StringBuilder currentLine = new StringBuilder();
	private volatile boolean captureHadSubstantiveLine;
	private int captureEpoch;
	private int captureBaselineLineCount;
	private volatile boolean captureSessionAborted;
	private String pendingCommandLine;
	private volatile int captureSessionLinesEmitted;
	private volatile boolean truncationNoticePending;
	private volatile int captureSessionLinesDropped;
	private volatile boolean suppressEmptyOutputPlaceholder;
	private boolean batchAppendActive;
	private final List<String> sessionFullLines = new ArrayList<String>();
	private final Object sessionFullLinesLock = new Object();

	public JmxTuiOutputSink(WindowBasedTextGUI gui, OutputTextBox output, JmxTuiOutputHost host) {
		this.gui = gui;
		this.output = output;
		this.host = host;
		this.outputPrepareExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, "JmxTui-OutputPrepare");
				t.setDaemon(true);
				return t;
			}
		});
	}

	public void setOutput(OutputTextBox output) {
		this.output = output;
	}

	/** Clears the full-result buffer used by Ctrl+S (invoked when the Output pane is cleared). */
	public void resetSessionFullLines() {
		synchronized(sessionFullLinesLock) {
			sessionFullLines.clear();
		}
	}

	/**
	 * Lines to write when saving Output (Ctrl+S). Returns the complete captured command output when
	 * available; otherwise the lines currently shown in the pane.
	 */
	public List<String> getSaveableLogicalLines(OutputTextBox pane) {
		synchronized(sessionFullLinesLock) {
			if(!sessionFullLines.isEmpty()) {
				return new ArrayList<String>(sessionFullLines);
			}
		}
		if(pane == null) {
			return new ArrayList<String>();
		}
		List<String> shown = pane.getLogicalLines();
		return new ArrayList<String>(shown);
	}

	public void shutdown() {
		outputPrepareExecutor.shutdown();
		try {
			outputPrepareExecutor.awaitTermination(SHUTDOWN_AWAIT_MS, TimeUnit.MILLISECONDS);
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		if(!outputPrepareExecutor.isTerminated()) {
			outputPrepareExecutor.shutdownNow();
		}
	}

	public void prepareImplicitCommandOutput() {
		captureEpoch++;
		clearPartialLine();
		Logger.setTuiPanelCapture(false);
	}

	public void beginUserCaptureSession(String commandLine) {
		captureEpoch++;
		captureSessionAborted = false;
		suppressEmptyOutputPlaceholder = TuiOutputCommandPolicy.isClearOutputCommand(commandLine);
		clearPartialLine();
		captureHadSubstantiveLine = false;
		captureBaselineLineCount = output == null ? 0 : output.getLogicalLines().size();
		captureSessionLinesEmitted = 0;
		truncationNoticePending = false;
		captureSessionLinesDropped = 0;
		pendingCommandLine = commandLine;
		Logger.setTuiPanelCapture(true);
		if(output != null) {
			output.beginBatchAppend();
			batchAppendActive = true;
		}
	}

	/**
	 * Clears Output for a new command, then writes {@code > command} preamble.
	 * Invoked only after {@code beforeExecute} (and mutating-call confirmation) succeed.
	 */
	public void commitUserCapturePreamble() {
		if(captureSessionAborted || !Logger.isTuiPanelCapture()) {
			return;
		}
		captureBaselineLineCount = 0;
		captureHadSubstantiveLine = false;
		if(!suppressEmptyOutputPlaceholder) {
			writeRequestPreamble(pendingCommandLine);
		}
		pendingCommandLine = null;
	}

	/**
	 * User cancelled before the command produced output (e.g. mutating CALL declined).
	 * Removes preamble/output from this session and skips {@link #endUserCaptureSession} placeholder.
	 */
	public void abortUserCaptureSession() {
		captureSessionAborted = true;
		captureEpoch++;
		pendingCommandLine = null;
		Logger.setTuiPanelCapture(false);
		clearPartialLine();
		captureHadSubstantiveLine = false;
		final int epoch = captureEpoch;
		outputPrepareExecutor.execute(new Runnable() {
			@Override
			public void run() {
				gui.getGUIThread().invokeLater(new Runnable() {
					@Override
					public void run() {
						if(epoch != captureEpoch || output == null) {
							return;
						}
						endBatchOnGuiIfNeeded();
						resetSessionFullLines();
						int remove = output.getLogicalLines().size() - captureBaselineLineCount;
						if(remove > 0) {
							output.removeLastLogicalLines(remove);
						}
					}
				});
			}
		});
	}

	public void endUserCaptureSession() {
		if(captureSessionAborted) {
			captureSessionAborted = false;
			return;
		}
		final int epoch = captureEpoch;
		outputPrepareExecutor.execute(new Runnable() {
			@Override
			public void run() {
				gui.getGUIThread().invokeLater(new Runnable() {
					@Override
					public void run() {
						if(epoch != captureEpoch) {
							return;
						}
						try {
							if(output != null) {
								List<String> tail = buildSessionTailLines();
								if(!tail.isEmpty()) {
									output.appendLogicalLines(tail);
									host.refreshOutputPaneBorderTime();
								} else if(!captureHadSubstantiveLine && !suppressEmptyOutputPlaceholder) {
									output.appendLogicalLine(EMPTY_OUTPUT_PLACEHOLDER);
									host.refreshOutputPaneBorderTime();
								}
								endBatchOnGuiIfNeeded();
							}
						} finally {
							if(epoch == captureEpoch) {
								Logger.setTuiPanelCapture(false);
								captureHadSubstantiveLine = false;
								suppressEmptyOutputPlaceholder = false;
								clearPartialLine();
								captureSessionLinesEmitted = 0;
								truncationNoticePending = false;
								captureSessionLinesDropped = 0;
							}
						}
					}
				});
			}
		});
	}

	public void clearPartialLine() {
		synchronized(currentLine) {
			currentLine.setLength(0);
		}
	}

	@Override
	public void print(final Object msg, final boolean newline) {
		if(output == null) {
			return;
		}
		if(!Logger.isTuiPanelCapture() && !shouldPassthroughWithoutCapture(msg)) {
			return;
		}
		final String s = (msg == null) ? "null" : msg.toString();
		final int epoch = captureEpoch;
		final boolean capture = Logger.isTuiPanelCapture();
		outputPrepareExecutor.execute(new Runnable() {
			@Override
			public void run() {
				final List<String> prepared = prepareOutputChunk(epoch, s, newline, capture);
				if(prepared == null) {
					return;
				}
				gui.getGUIThread().invokeLater(new Runnable() {
					@Override
					public void run() {
						applyPreparedLines(epoch, prepared, capture);
					}
				});
			}
		});
	}

	private List<String> prepareOutputChunk(int epoch, String s, boolean newline, boolean capture) {
		if(epoch != captureEpoch) {
			return null;
		}
		String full;
		synchronized(currentLine) {
			currentLine.append(s);
			if(!newline) {
				return null;
			}
			full = currentLine.toString();
			currentLine.setLength(0);
		}
		String[] parts = full.split("\\r?\\n", -1);
		List<String> out = new ArrayList<String>();
		for(int i = 0; i < parts.length; i++) {
			collectEmitLine(parts[i], out, capture);
		}
		return out.isEmpty() ? null : out;
	}

	private void collectEmitLine(String line, List<String> displayOut, boolean capture) {
		if(shouldSkipTuiOutputLine(line)) {
			return;
		}
		if(capture) {
			appendSessionFullLine(line);
			if(captureSessionLinesEmitted >= TuiOutputConstant.MAX_LOGICAL_LINES_PER_CAPTURE) {
				captureSessionLinesDropped++;
				truncationNoticePending = true;
				return;
			}
			captureSessionLinesEmitted++;
		}
		displayOut.add(line);
	}

	private void appendSessionFullLine(String line) {
		synchronized(sessionFullLinesLock) {
			sessionFullLines.add(line);
		}
	}

	private void applyPreparedLines(int epoch, List<String> lines, boolean capture) {
		if(epoch != captureEpoch || output == null || lines == null || lines.isEmpty()) {
			return;
		}
		captureHadSubstantiveLine = true;
		output.appendLogicalLines(lines);
		host.refreshOutputPaneBorderTime();
	}

	private List<String> buildSessionTailLines() {
		List<String> tail = new ArrayList<String>();
		if(truncationNoticePending && captureSessionLinesDropped > 0) {
			tail.add(String.format(TuiOutputConstant.OUTPUT_TRUNCATED_MESSAGE,
					Integer.valueOf(captureSessionLinesDropped)));
			captureHadSubstantiveLine = true;
		}
		return tail;
	}

	private void writeRequestPreamble(String commandLine) {
		if(output == null || commandLine == null) {
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
			List<String> preamble = new ArrayList<String>(2);
			preamble.add("> " + trimmed);
			preamble.add(buildOutputRequestSeparator(contentCols));
			synchronized(sessionFullLinesLock) {
				sessionFullLines.clear();
				sessionFullLines.addAll(preamble);
			}
			output.appendLogicalLines(preamble);
			host.refreshOutputPaneBorderTime();
		} catch(Exception e) {}
	}

	private void endBatchOnGuiIfNeeded() {
		if(output == null || !batchAppendActive) {
			return;
		}
		output.endBatchAppend();
		batchAppendActive = false;
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
