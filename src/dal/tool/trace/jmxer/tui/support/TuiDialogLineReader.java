package dal.tool.trace.jmxer.tui.support;

import com.googlecode.lanterna.gui2.WindowBasedTextGUI;

import dal.tool.cli.Logger;
import dal.tool.cli.Logger.Level;
import dal.tool.cli.util.IOUtil;
import dal.tool.trace.jmxer.tui.dialog.TuiPromptDialog;

/**
 * TUI 키맵으로 시작하는 명령(모달 대화상자)용 IOUtil.LineReader 구현.
 *
 */
public final class TuiDialogLineReader implements IOUtil.LineReader {

	private final WindowBasedTextGUI gui;

	public TuiDialogLineReader(WindowBasedTextGUI gui) {
		this.gui = gui;
	}

	@Override
	public String readLine(String prompt, Level level) {
		IOUtil.beginTuiPanelCaptureReadLinePrompt(prompt, level);
		String result = TuiPromptDialog.show(gui, prompt);
		if(result == null) {
			result = "";
		}
		Logger.logOnlySpool(result + "\n", false);
		return result;
	}
}
