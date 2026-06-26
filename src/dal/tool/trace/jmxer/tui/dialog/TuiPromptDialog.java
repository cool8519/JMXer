package dal.tool.trace.jmxer.tui.dialog;

import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;

/**
 * TUI 키맵 명령 실행 시 IOUtil.readLine과 연동되는 프롬프트 입력 다이얼로그.
 *
 */
public final class TuiPromptDialog {

	private static final String INPUT_TITLE = "Input";
	private static final String CONFIRM_TITLE = "Confirm";
	private static final String STOP_TITLE = "Recording";

	private TuiPromptDialog() {}

	public static String show(WindowBasedTextGUI gui, String prompt) {
		if(gui == null) {
			return "";
		}
		String p = prompt == null ? "" : prompt;
		if(isStopWaitPrompt(p)) {
			return showStopWait(gui, p);
		}
		if(isYesNoPrompt(p)) {
			return showYesNo(gui, p);
		}
		String value = TuiDialogs.showTextInputDialog(gui, INPUT_TITLE, p, "");
		return value == null ? "" : value;
	}

	private static String showStopWait(WindowBasedTextGUI gui, String prompt) {
		MessageDialogButton b = TuiDialogs.showMessageDialog(
			gui,
			STOP_TITLE,
			prompt.trim(),
			MessageDialogButton.OK
		);
		if(b == null) {
			return "";
		}
		return "";
	}

	private static String showYesNo(WindowBasedTextGUI gui, String prompt) {
		MessageDialogButton b = TuiDialogs.showMessageDialog(
			gui,
			CONFIRM_TITLE,
			prompt.trim(),
			MessageDialogButton.Yes,
			MessageDialogButton.No
		);
		if(b == MessageDialogButton.Yes) {
			return "Y";
		}
		if(b == MessageDialogButton.No) {
			return "N";
		}
		return "";
	}

	static boolean isYesNoPrompt(String prompt) {
		String upper = prompt.toUpperCase();
		return upper.indexOf("(Y/N)") >= 0 || upper.indexOf("Y/N") >= 0;
	}

	static boolean isStopWaitPrompt(String prompt) {
		String lower = prompt.toLowerCase();
		return lower.indexOf("press enter") >= 0 && lower.indexOf("stop") >= 0;
	}
}
