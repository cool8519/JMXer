package dal.tool.trace.jmxer.tui.support;

import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;

import dal.tool.cli.ScriptUiHooks;
import dal.tool.trace.jmxer.tui.dialog.TuiDialogs;
import dal.tool.trace.jmxer.tui.dialog.TuiScriptProgressDialog;
import dal.tool.trace.jmxer.tui.support.TuiGuiThread.GuiCallable;

/**
 * Lanterna GUI 스레드에서 SCRIPT 진행 대화상자를 표시·갱신한다.
 *
 */
public final class TuiScriptProgress {

	private static volatile TuiScriptProgressDialog activeDialog;
	private static volatile String rootCommandLine;
	private static volatile String lastScriptFileName;
	private static volatile String lastScriptCommandLine;
	private static volatile boolean suspendedForUserInput;

	private TuiScriptProgress() {}

	public static boolean confirmBeforeScript(WindowBasedTextGUI gui, String scriptFilesDisplay) {
		if(gui == null) {
			return true;
		}
		final String trimmed = scriptFilesDisplay == null ? "" : scriptFilesDisplay.trim();
		final String message = trimmed.isEmpty()
			? TuiScriptUiKeys.CONFIRM_MESSAGE_PREFIX
			: TuiScriptUiKeys.CONFIRM_MESSAGE_PREFIX + "\n\n" + trimmed;
		MessageDialogButton b = TuiGuiThread.callOnGuiThread(gui, new GuiCallable<MessageDialogButton>() {
			@Override
			public MessageDialogButton call() {
				return TuiDialogs.showMessageDialog(
					gui,
					TuiScriptUiKeys.CONFIRM_TITLE,
					message,
					MessageDialogButton.Yes,
					MessageDialogButton.No
				);
			}
		});
		return b != null && b == MessageDialogButton.Yes;
	}

	public static void begin(WindowBasedTextGUI gui, String commandLine) {
		if(gui == null) {
			return;
		}
		rootCommandLine = commandLine == null ? "" : commandLine;
		lastScriptFileName = null;
		lastScriptCommandLine = null;
		suspendedForUserInput = false;
		TuiGuiThread.callOnGuiThread(gui, new GuiCallable<Void>() {
			@Override
			public Void call() {
				activeDialog = TuiScriptProgressDialog.show(gui, rootCommandLine);
				return null;
			}
		});
	}

	public static void setCurrentCommand(WindowBasedTextGUI gui, String scriptFileName, String scriptCommandLine) {
		if(gui == null) {
			return;
		}
		lastScriptFileName = scriptFileName;
		lastScriptCommandLine = scriptCommandLine;
		if(suspendedForUserInput || ScriptUiHooks.isCancelRequested()) {
			return;
		}
		TuiGuiThread.callOnGuiThread(gui, new GuiCallable<Void>() {
			@Override
			public Void call() {
				TuiScriptProgressDialog dialog = activeDialog;
				if(dialog == null) {
					return null;
				}
				dialog.updateCurrentCommand(scriptFileName, scriptCommandLine);
				return null;
			}
		});
	}

	public static void suspend(WindowBasedTextGUI gui) {
		if(gui == null || rootCommandLine == null) {
			return;
		}
		suspendedForUserInput = true;
		TuiGuiThread.callOnGuiThread(gui, new GuiCallable<Void>() {
			@Override
			public Void call() {
				TuiScriptProgressDialog dialog = activeDialog;
				if(dialog != null) {
					dialog.closeDialog();
					activeDialog = null;
				}
				return null;
			}
		});
	}

	public static void resume(WindowBasedTextGUI gui) {
		if(gui == null || rootCommandLine == null || ScriptUiHooks.isCancelRequested()) {
			suspendedForUserInput = false;
			return;
		}
		suspendedForUserInput = false;
		final String root = rootCommandLine;
		final String scriptFile = lastScriptFileName;
		final String commandLine = lastScriptCommandLine;
		TuiGuiThread.callOnGuiThread(gui, new GuiCallable<Void>() {
			@Override
			public Void call() {
				if(rootCommandLine == null || ScriptUiHooks.isCancelRequested()) {
					return null;
				}
				activeDialog = TuiScriptProgressDialog.show(gui, root);
				if(scriptFile != null || commandLine != null) {
					activeDialog.updateCurrentCommand(scriptFile, commandLine);
				}
				return null;
			}
		});
	}

	public static void finish(WindowBasedTextGUI gui, boolean completedSuccessfully, boolean cancelled) {
		if(gui == null) {
			return;
		}
		TuiGuiThread.callOnGuiThread(gui, new GuiCallable<Void>() {
			@Override
			public Void call() {
				TuiScriptProgressDialog dialog = activeDialog;
				activeDialog = null;
				rootCommandLine = null;
				lastScriptFileName = null;
				lastScriptCommandLine = null;
				suspendedForUserInput = false;
				if(dialog != null) {
					dialog.closeDialog();
				}
				if(completedSuccessfully) {
					TuiDialogs.showMessageDialog(
						gui,
						TuiScriptUiKeys.COMPLETED_TITLE,
						TuiScriptUiKeys.COMPLETED_MESSAGE,
						MessageDialogButton.OK
					);
				}
				return null;
			}
		});
	}
}
