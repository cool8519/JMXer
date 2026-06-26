package dal.tool.trace.jmxer.tui.support;

import java.util.ArrayList;
import java.util.List;

import com.googlecode.lanterna.gui2.WindowBasedTextGUI;

import dal.tool.cli.Logger;
import dal.tool.cli.Logger.Level;
import dal.tool.cli.util.IOUtil;
import dal.tool.trace.jmxer.tui.dialog.TuiPromptDialog;

/**
 * 미리 정한 답변을 순서대로 반환하고, 필요 시 외부 중지 신호(Record 진행 Stop 등)를 기다린 뒤
 * 빈 줄을 반환하는 IOUtil.LineReader 구현.
 *
 */
public final class TuiQueuedLineReader implements IOUtil.LineReader {

	/**
	 * Record 진행 중 사용자 Stop을 기다리는 콜백.
	 *
	 */
	public interface StopAwaiter {
		void awaitStop() throws InterruptedException;
	}

	private final WindowBasedTextGUI gui;
	private final List<String> presetAnswers;
	private final StopAwaiter stopAwaiter;
	private final String summaryBlock;
	private final boolean suppressPrompts;
	private boolean summaryWritten;

	public TuiQueuedLineReader(WindowBasedTextGUI gui, List<String> presetAnswers, StopAwaiter stopAwaiter) {
		this(gui, presetAnswers, stopAwaiter, null, false);
	}

	public TuiQueuedLineReader(
			WindowBasedTextGUI gui,
			List<String> presetAnswers,
			StopAwaiter stopAwaiter,
			String summaryBlock,
			boolean suppressPrompts) {
		this.gui = gui;
		this.presetAnswers = presetAnswers == null
			? new ArrayList<String>()
			: new ArrayList<String>(presetAnswers);
		this.stopAwaiter = stopAwaiter;
		this.summaryBlock = summaryBlock;
		this.suppressPrompts = suppressPrompts;
	}

	@Override
	public String readLine(String prompt, Level level) {
		if(suppressPrompts) {
			writeSummaryOnce();
		} else {
			IOUtil.beginTuiPanelCaptureReadLinePrompt(prompt, level);
		}
		if(!presetAnswers.isEmpty()) {
			String answer = presetAnswers.remove(0);
			Logger.logOnlySpool(answer + "\n", false);
			return answer;
		}
		if(stopAwaiter != null) {
			try {
				stopAwaiter.awaitStop();
			} catch(InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			Logger.logOnlySpool("\n", false);
			return "";
		}
		String result = TuiPromptDialog.show(gui, prompt);
		if(result == null) {
			result = "";
		}
		Logger.logOnlySpool(result + "\n", false);
		return result;
	}

	private void writeSummaryOnce() {
		if(summaryWritten || summaryBlock == null || summaryBlock.trim().length() == 0) {
			return;
		}
		summaryWritten = true;
		String[] lines = summaryBlock.split("\\r?\\n", -1);
		for(int i = 0; i < lines.length; i++) {
			Logger.logln(lines[i]);
		}
	}
}
