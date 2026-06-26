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
 * 사용자 명령 세션 동안 Logger 출력을 TUI Output 패널로 보낸다.
 *
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
	private boolean capturePreambleCommitted;
	private boolean batchAppendActive;
	private final List<String> sessionFullLines = new ArrayList<String>();
	private final Object sessionFullLinesLock = new Object();
	private OutputTextBox sessionFullLinesOwner;

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

	public void resetSessionFullLines() {
		synchronized(sessionFullLinesLock) {
			sessionFullLines.clear();
		}
		sessionFullLinesOwner = null;
	}

	/**
	 * Output 저장 시 쓸 줄 목록. 전체 캡처가 있으면 그것을, 없으면 패널에 보이는 줄을 반환한다.
	 */
	public List<String> getSaveableLogicalLines(OutputTextBox pane) {
		synchronized(sessionFullLinesLock) {
			if(!sessionFullLines.isEmpty() && sessionFullLinesOwner == pane) {
				return copyWithoutTruncationNotice(sessionFullLines);
			}
		}
		if(pane == null) {
			return new ArrayList<String>();
		}
		return copyWithoutTruncationNotice(pane.getLogicalLines());
	}

	private static List<String> copyWithoutTruncationNotice(List<String> lines) {
		if(lines == null || lines.isEmpty()) {
			return new ArrayList<String>();
		}
		List<String> out = new ArrayList<String>(lines.size());
		for(int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			if(!TuiOutputConstant.isTruncationNoticeLine(line)) {
				out.add(line);
			}
		}
		return out;
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
		capturePreambleCommitted = false;
		pendingCommandLine = commandLine;
		resetSessionFullLines();
		sessionFullLinesOwner = output;
		Logger.setTuiPanelCapture(true);
		if(output != null) {
			output.beginBatchAppend();
			batchAppendActive = true;
		}
	}

	/**
	 * Output을 비운 뒤 "> command" 프리앰블을 쓴다.
	 * beforeExecute(및 변경 CALL 확인) 성공 후에만 호출된다.
	 */
	public void commitUserCapturePreamble() {
		if(captureSessionAborted || !Logger.isTuiPanelCapture()) {
			return;
		}
		if(capturePreambleCommitted) {
			return;
		}
		capturePreambleCommitted = true;
		captureBaselineLineCount = 0;
		captureHadSubstantiveLine = false;
		sessionFullLinesOwner = output;
		if(!suppressEmptyOutputPlaceholder) {
			writeRequestPreamble(pendingCommandLine);
		}
		pendingCommandLine = null;
		refreshCaptureDisplayIfBatching();
	}

	/**
	 * 출력 전에 사용자가 취소한 경우(예: 변경 CALL 거부). 이번 세션 프리앰블·출력을 제거하고 빈 Output 플레이스홀더를 건너뛴다.
	 */
	public void abortUserCaptureSession() {
		captureSessionAborted = true;
		captureEpoch++;
		pendingCommandLine = null;
		capturePreambleCommitted = false;
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
								if(captureHadSubstantiveLine || !tail.isEmpty()) {
									output.scrollDisplayToEnd();
								}
							}
						} finally {
							if(epoch == captureEpoch) {
								Logger.setTuiPanelCapture(false);
								captureHadSubstantiveLine = false;
								suppressEmptyOutputPlaceholder = false;
								capturePreambleCommitted = false;
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
	public void appendInlineFragment(final String fragment) {
		if(output == null || fragment == null || fragment.length() == 0) {
			return;
		}
		if(!Logger.isTuiPanelCapture() && !shouldPassthroughWithoutCapture(fragment)) {
			return;
		}
		final int epoch = captureEpoch;
		gui.getGUIThread().invokeLater(new Runnable() {
			@Override
			public void run() {
				if(epoch != captureEpoch || output == null) {
					return;
				}
				output.appendToLastLogicalLine(fragment);
				appendSessionFullLineSuffix(fragment);
				captureHadSubstantiveLine = true;
				host.refreshOutputPaneBorderTime();
			}
		});
	}

	public void appendStdinResponseToLastLine(String response) {
		if(response == null || response.length() == 0) {
			return;
		}
		appendInlineFragment(response);
	}

	private void appendSessionFullLineSuffix(String suffix) {
		synchronized(sessionFullLinesLock) {
			if(sessionFullLines.isEmpty()) {
				sessionFullLines.add(suffix);
			} else {
				int last = sessionFullLines.size() - 1;
				sessionFullLines.set(last, sessionFullLines.get(last) + suffix);
			}
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
		if(line == null) {
			return;
		}
		if(line.trim().isEmpty()) {
			// 의도적 빈 줄 유지(CLI와 동일한 간격; Logger.logBlankLine 참고).
			line = "\u00a0";
		} else if(shouldSkipTuiOutputLine(line)) {
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
		refreshCaptureDisplayIfBatching();
	}

	private List<String> buildSessionTailLines() {
		List<String> tail = new ArrayList<String>();
		if(hasTruncationNoticeLine()) {
			return tail;
		}
		int dropped = captureSessionLinesDropped;
		if(dropped <= 0) {
			dropped = estimateDroppedLinesFromSessionBuffer();
		}
		if(dropped > 0 || truncationNoticePending) {
			if(dropped <= 0) {
				dropped = 1;
			}
			tail.add(String.format(TuiOutputConstant.OUTPUT_TRUNCATED_MESSAGE,
					Integer.valueOf(dropped)));
			captureHadSubstantiveLine = true;
		}
		return tail;
	}

	private boolean hasTruncationNoticeLine() {
		if(output == null) {
			return false;
		}
		List<String> logical = output.getLogicalLines();
		if(logical.isEmpty()) {
			return false;
		}
		return TuiOutputConstant.isTruncationNoticeLine(logical.get(logical.size() - 1));
	}

	private int estimateDroppedLinesFromSessionBuffer() {
		if(output == null) {
			return 0;
		}
		int shown = output.getLogicalLines().size();
		synchronized(sessionFullLinesLock) {
			int full = sessionFullLines.size();
			int extra = full - shown;
			return extra > 0 ? extra : 0;
		}
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
			sessionFullLinesOwner = output;
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

	private void refreshCaptureDisplayIfBatching() {
		if(output != null && batchAppendActive) {
			output.reflowDisplayScrollToEnd();
		}
	}

	/**
	 * SCRIPT 캡처 중 진행 대화상자가 열린 채로도 결과가 보이도록 매 스크립트 줄마다 Output 패널을 재배치한다.
	 */
	public void refreshLiveCaptureDisplay() {
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
						refreshCaptureDisplayIfBatching();
						if(!batchAppendActive) {
							output.scrollDisplayToEnd();
						}
					}
				});
			}
		});
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
