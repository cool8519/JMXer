package dal.tool.trace.jmxer.tui.dialog;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.DialogWindow;

import dal.tool.cli.ScriptUiHooks;
import dal.tool.trace.jmxer.tui.support.TuiScriptUiKeys;
import dal.tool.trace.jmxer.tui.support.TuiStatusText;

/**
 * TUI 최상위 SCRIPT 명령 비차단 진행 표시 다이얼로그.
 *
 */
public final class TuiScriptProgressDialog extends DialogWindow {

	private final String rootCommandLine;
	private final AtomicBoolean cancelRequested = new AtomicBoolean(false);
	private final Label statusLabel;
	private final Panel buttonPanel;
	private final Button cancelButton;
	private volatile boolean cancelPending;

	private TuiScriptProgressDialog(String rootCommandLine) {
		super(TuiScriptUiKeys.DIALOG_TITLE);
		this.rootCommandLine = rootCommandLine == null ? "" : rootCommandLine.trim();
		if(ScriptUiHooks.isCancelRequested()) {
			cancelRequested.set(true);
			cancelPending = true;
		}
		enableCenteredHint();
		statusLabel = new Label(TuiDialogText.body(formatStatus(this.rootCommandLine, null, null, cancelPending)));

		Panel mainPanel = new Panel();
		mainPanel.setLayoutManager(new GridLayout(1).setLeftMarginSize(1).setRightMarginSize(1));
		mainPanel.addComponent(statusLabel);
		mainPanel.addComponent(new EmptySpace(TerminalSize.ONE));

		cancelButton = new Button(TuiScriptUiKeys.CANCEL_LABEL, new Runnable() {
			@Override
			public void run() {
				cancelRequested.set(true);
				cancelPending = true;
				ScriptUiHooks.requestCancel();
				refreshStatus();
				hideCancelButton();
			}
		});
		buttonPanel = new Panel();
		buttonPanel.setLayoutManager(new GridLayout(1).setHorizontalSpacing(1));
		buttonPanel.addComponent(cancelButton);
		buttonPanel.setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.CENTER, GridLayout.Alignment.CENTER, false, false))
			.addTo(mainPanel);
		if(cancelPending) {
			hideCancelButton();
		}

		setComponent(mainPanel);
		TuiDialogEscFocus.install(this);
	}

	public static TuiScriptProgressDialog show(WindowBasedTextGUI gui, String rootCommandLine) {
		TuiScriptProgressDialog dialog = new TuiScriptProgressDialog(rootCommandLine);
		gui.addWindow(dialog);
		return dialog;
	}

	public boolean isCancelRequested() {
		return cancelRequested.get();
	}

	public void updateCurrentCommand(String scriptFileName, String scriptCommandLine) {
		refreshStatus(scriptFileName, scriptCommandLine);
	}

	public void closeDialog() {
		close();
	}

	private void refreshStatus() {
		refreshStatus(null, null);
	}

	private void refreshStatus(String scriptFileName, String scriptCommandLine) {
		statusLabel.setText(TuiDialogText.body(formatStatus(rootCommandLine, scriptFileName, scriptCommandLine, cancelPending)));
	}

	private void hideCancelButton() {
		if(buttonPanel != null) {
			buttonPanel.removeComponent(cancelButton);
		}
	}

	private static String formatStatus(String rootCommandLine, String scriptFileName, String scriptCommandLine,
			boolean cancelPending) {
		StringBuilder sb = new StringBuilder();
		sb.append(cancelPending ? TuiScriptUiKeys.CANCEL_PENDING_LINE : TuiScriptUiKeys.HEADER_LINE);
		if(scriptFileName != null && scriptFileName.length() > 0) {
			sb.append('\n').append('\n');
			sb.append(TuiScriptUiKeys.SCRIPT_FILE_PREFIX);
			sb.append(TuiStatusText.fitSingleLine(scriptFileName,
					TuiScriptUiKeys.DIALOG_COMMAND_MAX_COLUMNS));
		}
		if(scriptCommandLine != null && scriptCommandLine.length() > 0) {
			sb.append('\n').append('\n');
			sb.append(TuiScriptUiKeys.CURRENT_COMMAND_PREFIX);
			sb.append(TuiStatusText.fitSingleLine(scriptCommandLine,
					TuiScriptUiKeys.DIALOG_COMMAND_MAX_COLUMNS));
		}
		if(!cancelPending) {
			sb.append("\n\n").append(TuiScriptUiKeys.CANCEL_HINT);
		}
		return sb.toString();
	}

	private void enableCenteredHint() {
		Set<Window.Hint> hints = new HashSet<Window.Hint>(getHints());
		hints.add(Window.Hint.CENTERED);
		setHints(hints);
	}
}
