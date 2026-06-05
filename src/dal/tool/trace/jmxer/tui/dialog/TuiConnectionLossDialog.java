package dal.tool.trace.jmxer.tui.dialog;

import java.util.HashSet;
import java.util.Set;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.DialogWindow;

import dal.tool.trace.jmxer.cli.JmxTuiConnectionLossHandler;

/**
 * Connection Lost dialog with explicit Reconnect / Close actions (not {@code MessageDialogButton}).
 */
public final class TuiConnectionLossDialog extends DialogWindow {

	public enum Choice {
		RECONNECT,
		CLOSE
	}

	private Choice choice;

	private TuiConnectionLossDialog(String title, String bodyText) {
		super(title == null ? JmxTuiConnectionLossHandler.DIALOG_TITLE : title);
		enableCenteredHint();

		Button reconnectButton = new Button(JmxTuiConnectionLossHandler.ACTION_RECONNECT, new Runnable() {
			@Override
			public void run() {
				choice = Choice.RECONNECT;
				close();
			}
		});
		Button closeButton = new Button(JmxTuiConnectionLossHandler.ACTION_CLOSE, new Runnable() {
			@Override
			public void run() {
				choice = Choice.CLOSE;
				close();
			}
		});

		Panel buttonPanel = new Panel();
		buttonPanel.setLayoutManager(new GridLayout(2).setHorizontalSpacing(1));
		buttonPanel.addComponent(reconnectButton);
		buttonPanel.addComponent(closeButton);

		Panel mainPanel = new Panel();
		mainPanel.setLayoutManager(new GridLayout(1).setLeftMarginSize(1).setRightMarginSize(1));
		mainPanel.addComponent(new Label(TuiDialogText.body(bodyText)));
		mainPanel.addComponent(new EmptySpace(TerminalSize.ONE));
		buttonPanel.setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.END, GridLayout.Alignment.CENTER, false, false))
			.addTo(mainPanel);
		setComponent(mainPanel);
		TuiDialogEscFocus.install(this);
	}

	public static Choice show(WindowBasedTextGUI gui, String title, String bodyText) {
		if(gui == null) {
			return null;
		}
		TuiConnectionLossDialog dialog = new TuiConnectionLossDialog(title, bodyText);
		dialog.showDialog(gui);
		return dialog.choice;
	}

	private void enableCenteredHint() {
		Set<Window.Hint> hints = new HashSet<Window.Hint>(getHints());
		hints.add(Window.Hint.CENTERED);
		setHints(hints);
	}
}
