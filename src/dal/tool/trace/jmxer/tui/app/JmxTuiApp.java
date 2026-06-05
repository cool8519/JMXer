package dal.tool.trace.jmxer.tui.app;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.lang.management.ThreadInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServerConnection;
import javax.management.MBeanFeatureInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ObjectName;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TerminalTextUtils;
import com.googlecode.lanterna.gui2.BasePane;
import com.googlecode.lanterna.gui2.Border;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LayoutData;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.LinearLayout.Alignment;
import com.googlecode.lanterna.gui2.LinearLayout.GrowPolicy;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

import dal.tool.cli.History;
import dal.tool.cli.Logger;
import dal.tool.trace.jmxer.JMXControl;
import dal.tool.trace.jmxer.JMXerConstant;
import dal.tool.trace.jmxer.cli.command.JmxCommand;
import dal.tool.trace.jmxer.cli.command.JmxThreadCommand;
import dal.tool.trace.jmxer.cli.JmxCommandExecutor;
import dal.tool.trace.jmxer.cli.JmxConnectionRecovery;
import dal.tool.trace.jmxer.cli.JmxSettings;
import dal.tool.trace.jmxer.cli.JmxSettings.StacktraceTarget;
import dal.tool.trace.jmxer.cli.JmxTuiConnectionLossHandler;
import dal.tool.trace.jmxer.tui.support.BoxFocusManager;
import dal.tool.trace.jmxer.tui.support.JmxTuiKeys;
import dal.tool.trace.jmxer.tui.support.JmxTuiTheme;
import dal.tool.trace.jmxer.tui.support.TuiJmxInputValidator;
import dal.tool.trace.jmxer.tui.support.TuiGlobalKeysHint;
import dal.tool.trace.jmxer.tui.support.TuiThreadUiKeys;
import dal.tool.trace.jmxer.tui.support.ThreadAutoRefreshScheduler;
import dal.tool.trace.jmxer.tui.support.ThreadPaneTableFormat;
import dal.tool.trace.jmxer.tui.support.TuiStatusSegmentLabel;
import dal.tool.trace.jmxer.tui.support.TuiStatusText;
import dal.tool.trace.jmxer.tui.dialog.TuiConnectionLossDialog;
import dal.tool.trace.jmxer.tui.dialog.TuiDetailDialog;
import dal.tool.trace.jmxer.tui.dialog.TuiDialogs;
import dal.tool.trace.jmxer.tui.dialog.TuiOperationArgumentsDialog;
import dal.tool.trace.jmxer.tui.dialog.TuiThreadRefreshDialog;
import dal.tool.trace.jmxer.tui.mode.RecordUiMode;
import dal.tool.trace.jmxer.tui.mode.JmxTuiModeContext;
import dal.tool.trace.jmxer.tui.mode.JmxTuiModeContext.ThreadRowState;
import dal.tool.trace.jmxer.tui.mode.JmxTuiModeContext.ThreadSnapshotEntry;
import dal.tool.trace.jmxer.tui.mode.JmxTuiModeFocusPane;
import dal.tool.trace.jmxer.tui.mode.JmxTuiUiMode;
import dal.tool.trace.jmxer.tui.mode.MBeanUiMode;
import dal.tool.trace.jmxer.tui.mode.ThreadUiMode;
import dal.tool.trace.jmxer.tui.output.JmxTuiOutputHost;
import dal.tool.trace.jmxer.tui.output.JmxTuiOutputSink;
import dal.tool.trace.jmxer.tui.output.OutputTextBox;
import dal.tool.trace.jmxer.tui.output.OutputTextFormat;
import dal.tool.trace.jmxer.util.JMXPrintUtil;
import dal.tool.trace.jmxer.tui.widget.CommandInputBox;
import dal.tool.trace.jmxer.tui.widget.CommandInputKeyHandler;
import dal.tool.trace.jmxer.tui.widget.FocusActionListBox;
import dal.tool.trace.jmxer.tui.widget.ListKeyHandler;
import dal.tool.trace.jmxer.tui.widget.MbeanSearchInputBox;
import dal.tool.trace.jmxer.tui.widget.MbeanSearchKeyHandler;
import dal.tool.trace.jmxer.tui.widget.ThreadListItem;
import dal.tool.trace.jmxer.tui.widget.ThreadSelectListItemRenderer;
import dal.tool.util.jmx.JMXHelper;
import dal.tool.util.jmx.JMXUtil;
import dal.tool.util.jmx.MBeanConnector;

public class JmxTuiApp implements JmxTuiOutputHost {
	private static final int HEADER_INNER_LINES = 5;
	private static final int MIN_LIST_ROWS = 6;
	private static final int MAX_LIST_ROWS = 18;
	/** Outer border vertical overhead for single-line titled border (top+bottom frame lines). */
	private static final int BORDER_V_OVERHEAD = 2;
	private static final int WINDOW_DECORATION_ROWS = 1;
	/** Shown in the Output pane border title after each write (local time). */
	private static final DateTimeFormatter OUTPUT_PANE_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
	private static final String ATTR_ACTION_READ = "Read";
	private static final String ATTR_ACTION_WRITE = "Write";
	private static final int MIN_FEATURES_COLS = 20;
	private static final int MIN_MBEANS_COLS = 28;
	private static final int HORIZONTAL_SPLIT_STEP_COLS = 2;
	private static final int VERTICAL_SPLIT_STEP_ROWS = 1;
	private static final int MIN_OUTPUT_OUTER_ROWS = 3;
	private static final String THREADING_JMX_NAME = "java.lang:type=Threading";

	private final WindowBasedTextGUI gui;
	private final JmxCommandExecutor executor;
	private final String prompt;
	private final Runnable closeAction;

	private final Panel root;
	private final Panel bodyRoot;
	private final Label titleLabel;
	private final Panel statusInner;
	private final Label header1;
	private final TuiStatusSegmentLabel header2;
	private final Label header3;
	private final Label headerKeysGlobal;
	private final Label headerKeysPane;

	private final Panel commandInner;
	private final CommandInputBox input;
	private final Panel inputRow;

	private final JmxTuiOutputSink outputSink;
	private final BoxFocusManager focusManager;
	/**
	 * During internal layout changes we temporarily move focus to Command to avoid Lanterna focus artifacts.
	 * That focus hop must not overwrite the per-mode last-focused pane.
	 */
	private volatile boolean suppressActivePaneUpdates;

	private JmxTuiUiMode currentMode;
	private JmxTuiModeContext activeContext;

	/** Full MBean ObjectName list from the executor (unfiltered). */
	private volatile List<String> currentMbeanNames = new ArrayList<String>();
	private volatile TerminalSize lastTerminalSize = null;
	/** Status pane inner column count from the latest {@link #applyTerminalSize}. */
	private volatile int statusInnerCols = 10;

	private int historyCursor = -1;
	private String historyScratch = "";
	private volatile boolean connectionLossDialogQueued;
	private final ThreadAutoRefreshScheduler threadRefreshScheduler;
	private StacktraceTarget lastAppliedStacktraceTargetForSelection = StacktraceTarget.SELECTED;

	public JmxTuiApp(WindowBasedTextGUI gui, JmxCommandExecutor executor, String prompt) {
		this(gui, executor, prompt, null);
	}

	public JmxTuiApp(WindowBasedTextGUI gui, JmxCommandExecutor executor, String prompt, Runnable closeAction) {
		this.gui = gui;
		this.executor = executor;
		this.prompt = prompt;
		this.closeAction = closeAction;

		this.focusManager = new BoxFocusManager();

		this.titleLabel = new Label("");
		this.header1 = new Label("");
		this.header2 = new TuiStatusSegmentLabel();
		this.header3 = new Label("");
		this.headerKeysGlobal = new Label("");
		this.headerKeysPane = new Label("");

		this.commandInner = new Panel(new LinearLayout(Direction.VERTICAL));
		this.input = new CommandInputBox(focusManager, commandInner, new TerminalSize(1, 1));
		this.input.setCaretWarp(true);
		this.input.setVerticalFocusSwitching(false);
		this.input.setHorizontalFocusSwitching(false);
		this.input.setTheme(JmxTuiTheme.UNFOCUSED_TEXTBOX_THEME);

		this.inputRow = new Panel(new LinearLayout(Direction.HORIZONTAL));
		this.inputRow.addComponent(new Label(prompt + "> "));
		this.inputRow.addComponent(input, LinearLayout.createLayoutData(Alignment.Fill, GrowPolicy.CanGrow));
		this.commandInner.addComponent(this.inputRow, LinearLayout.createLayoutData(Alignment.Fill, GrowPolicy.CanGrow));

		focusManager.register(commandInner);
		focusManager.registerFillHighlight(commandInner);
		focusManager.setActivePaneListener(new Runnable() {
			@Override
			public void run() {
				onActivePaneChanged();
			}
		});
		focusManager.setActive(null);

		this.outputSink = new JmxTuiOutputSink(gui, null, this);
		this.threadRefreshScheduler = new ThreadAutoRefreshScheduler(new Runnable() {
			@Override
			public void run() {
				gui.getGUIThread().invokeLater(new Runnable() {
					@Override
					public void run() {
						onThreadAutoRefreshTick();
					}
				});
			}
		});

		this.statusInner = new Panel(new LinearLayout(Direction.VERTICAL));
		this.bodyRoot = new Panel(new LinearLayout(Direction.VERTICAL));
		this.root = buildLayout();
		wireKeyBindings();
		setMode(MBeanUiMode.getInstance());
		refreshHeader();
		refreshMBeans();
	}

	public JmxTuiModeContext createModeContext(JmxTuiUiMode mode) {
		return new JmxTuiModeContext(gui, focusManager, this, mode);
	}

	public void wireModeContext(final JmxTuiModeContext ctx) {
		if(ctx.isThreadMode()) {
			wireThreadModeContext(ctx);
			return;
		}
		ctx.getMbeanList().setSelectionChangedHandler(new Runnable() {
			@Override
			public void run() {
				if(ctx == activeContext) {
					syncFeaturesToMBeanSelection(ctx);
				}
			}
		});
		ctx.getOutput().setSaveOutputAction(new Runnable() {
			@Override
			public void run() {
				copyOutputToFile(ctx);
			}
		});
		ctx.getOutput().setWrapModeChangeListener(new Runnable() {
			@Override
			public void run() {
				refreshHeader();
			}
		});
		wireMbeanSearchBindings(ctx);
	}

	private JmxTuiModeContext ctx() {
		return activeContext;
	}

	/**
	 * Move keyboard focus to Command before detaching mode panes from the layout tree.
	 * Lanterna only clears focus when removing the focused component itself, not an ancestor.
	 */
	private void detachLayoutFocusSafely() {
		boolean prevSuppress = suppressActivePaneUpdates;
		suppressActivePaneUpdates = true;
		try {
			BasePane basePane = input.getBasePane();
			if(basePane != null) {
				basePane.setFocusedInteractable(input);
			} else {
				input.takeFocus();
			}
		} catch(Exception e) {
			try {
				input.takeFocus();
			} catch(Exception e2) {}
		} finally {
			suppressActivePaneUpdates = prevSuppress;
		}
	}

	private void saveCurrentModeFocus() {
		if(suppressActivePaneUpdates) {
			return;
		}
		JmxTuiModeContext c = activeContext;
		if(c != null) {
			JmxTuiModeFocusPane p = resolveCurrentFocusPane();
			c.storeFocusedPane(p);
		}
	}

	private JmxTuiModeFocusPane resolveCurrentFocusPane() {
		JmxTuiModeFocusPane fromActivePane = resolveFocusPaneFromActivePanel(focusManager.getActivePane());
		if(fromActivePane != null) {
			return fromActivePane;
		}
		JmxTuiModeContext c = activeContext;
		if(c == null) {
			return JmxTuiModeFocusPane.COMMAND;
		}
		if(input.isFocused()) {
			return JmxTuiModeFocusPane.COMMAND;
		}
		if(c.getMbeanSearch().isFocused()) {
			return JmxTuiModeFocusPane.SEARCH;
		}
		if(c.getMbeanList().isFocused()) {
			return JmxTuiModeFocusPane.MBEANS;
		}
		if(c.getFeatureList().isFocused()) {
			return JmxTuiModeFocusPane.FEATURES;
		}
		if(c.getOutput().isFocused()) {
			return JmxTuiModeFocusPane.OUTPUT;
		}
		if(c.isFocusPaneStored()) {
			return c.getLastFocusedPane();
		}
		return c.getDefaultFocusPane();
	}

	private JmxTuiModeFocusPane resolveFocusPaneFromActivePanel(Panel activePane) {
		if(activePane == null) {
			return null;
		}
		if(activePane == commandInner) {
			return JmxTuiModeFocusPane.COMMAND;
		}
		JmxTuiModeContext c = activeContext;
		if(c == null) {
			return null;
		}
		if(activePane == c.getMbeanSearchInner()) {
			return JmxTuiModeFocusPane.SEARCH;
		}
		if(activePane == c.getMbeansInner()) {
			return JmxTuiModeFocusPane.MBEANS;
		}
		if(activePane == c.getFeaturesInner()) {
			return JmxTuiModeFocusPane.FEATURES;
		}
		if(activePane == c.getOutputInner()) {
			return JmxTuiModeFocusPane.OUTPUT;
		}
		return null;
	}

	private void onActivePaneChanged() {
		if(suppressActivePaneUpdates) {
			return;
		}
		saveCurrentModeFocus();
		refreshHeader();
	}

	private void restoreCurrentModeFocus() {
		JmxTuiModeContext c = activeContext;
		JmxTuiUiMode mode = currentMode;
		if(c == null || mode == null) {
			return;
		}
		JmxTuiModeFocusPane paneId = c.isFocusPaneStored()
			? c.resolveRestorableFocusPane(mode)
			: c.getDefaultFocusPane();
		Interactable target = c.resolveInteractable(paneId, mode, input);
		Panel pane = c.resolvePanePanel(paneId, mode, commandInner);
		applyFocus(target, pane);
	}

	private void applyFocus(Interactable target, Panel pane) {
		if(target == null) {
			return;
		}
		try {
			BasePane basePane = input.getBasePane();
			if(basePane != null) {
				basePane.setFocusedInteractable(target);
			} else {
				target.takeFocus();
			}
		} catch(Exception e) {
			try {
				target.takeFocus();
			} catch(Exception e2) {}
		}
		focusManager.setActive(pane);
	}

	/** Same confirmation as Ctrl+C; runs {@code EXIT} unless the caller already did. */
	public void requestExitWithConfirm() {
		requestExitWithConfirm(true);
	}

	/**
	 * @param runExitCommand {@code true} to run the EXIT command before closing (e.g. Ctrl+C);
	 *                       {@code false} when EXIT was already executed (e.g. after {@code exit} in Command)
	 */
	public void requestExitWithConfirm(boolean runExitCommand) {
		try {
			MessageDialogButton b = TuiDialogs.showMessageDialog(
				gui,
				"Exit Confirmation",
				"Are you sure you want to exit?",
				MessageDialogButton.Yes,
				MessageDialogButton.No
			);
			if(b != MessageDialogButton.Yes) {
				return;
			}
		} catch(Exception e) {
			// If dialog fails for any reason, fall back to exiting (best-effort behavior).
		}
		if(runExitCommand) {
			try {
				executor.processCommandForTui("EXIT");
			} catch(Exception e) {}
		}
		runCloseAction();
	}

	/** Prompts for reconnect or exit after unrecoverable JMX connection loss. */
	public void handleConnectionLoss(String message) {
		if(connectionLossDialogQueued) {
			return;
		}
		connectionLossDialogQueued = true;
		Runnable showDialog = new Runnable() {
			@Override
			public void run() {
				try {
					promptConnectionLossUntilResolved(message);
				} finally {
					connectionLossDialogQueued = false;
				}
			}
		};
		if(isGuiThread()) {
			showDialog.run();
		} else {
			try {
				gui.getGUIThread().invokeLater(showDialog);
			} catch(Exception e) {
				connectionLossDialogQueued = false;
			}
		}
	}

	private boolean isGuiThread() {
		try {
			Thread guiThread = gui.getGUIThread().getThread();
			return guiThread != null && Thread.currentThread() == guiThread;
		} catch(Exception e) {
			return false;
		}
	}

	private void promptConnectionLossUntilResolved(String detailMessage) {
		JmxTuiConnectionLossHandler.setLossDialogSessionActive(true);
		showConnectionLossDialog(detailMessage == null ? "" : detailMessage);
	}

	private void showConnectionLossDialog(String detail) {
		TuiConnectionLossDialog.Choice choice;
		try {
			choice = TuiConnectionLossDialog.show(
				gui,
				JmxTuiConnectionLossHandler.DIALOG_TITLE,
				JmxTuiConnectionLossHandler.formatDialogBody(detail));
		} catch(Exception e) {
			Logger.logln(Logger.Level.ERROR,
					"Connection loss dialog could not be shown: " + e.getClass().getSimpleName()
					+ (e.getMessage() != null ? (" - " + e.getMessage()) : ""));
			choice = null;
		}
		if(choice == TuiConnectionLossDialog.Choice.CLOSE) {
			endConnectionLossDialogSession();
			exitAfterConnectionLoss();
			return;
		}
		if(choice == TuiConnectionLossDialog.Choice.RECONNECT) {
			beginUserInitiatedReconnectAsync();
			return;
		}
		showConnectionLossDialog(detail);
	}

	private void endConnectionLossDialogSession() {
		JmxTuiConnectionLossHandler.setLossDialogSessionActive(false);
	}

	private void beginUserInitiatedReconnectAsync() {
		final MBeanConnector connector = executor.getMBeanConnector();
		if(JMXControl.isOfflineSession || connector == null) {
			showConnectionLossDialog("Reconnect is not available in the current session.");
			return;
		}
		Thread worker = new Thread(new Runnable() {
			@Override
			public void run() {
				final String error = performReconnectOnWorker(connector);
				try {
					gui.getGUIThread().invokeLater(new Runnable() {
						@Override
						public void run() {
							onUserReconnectFinished(connector, error);
						}
					});
				} catch(Exception e) {
					Logger.logln(Logger.Level.ERROR,
							"Could not update UI after reconnect: " + e.getMessage());
				}
			}
		}, "JmxTui-Reconnect");
		worker.setDaemon(true);
		worker.start();
	}

	private void onUserReconnectFinished(MBeanConnector connector, String error) {
		if(error != null) {
			showConnectionLossDialog(error);
			return;
		}
		try {
			clearAllModeOutputs();
			refreshAfterReconnect();
			endConnectionLossDialogSession();
		} catch(Exception e) {
			if(connector != null) {
				connector.markConnectionStale();
			}
			showConnectionLossDialog(formatReconnectError(e));
		}
	}

	private static String performReconnectOnWorker(MBeanConnector connector) {
		try {
			connector.reconnect();
			if(!connector.isInitialized()) {
				return "Reconnect failed. Check that the target JVM is running and reachable.";
			}
			connector.verifySession();
			return null;
		} catch(Exception e) {
			connector.markConnectionStale();
			return formatReconnectError(e);
		}
	}

	private void exitAfterConnectionLoss() {
		JmxTuiConnectionLossHandler.markExitChosen();
		try {
			executor.processCommandForTui("EXIT");
		} catch(Exception e) {}
		runCloseAction();
	}

	private boolean attemptOnlineJmxReconnect(MBeanConnector connector, Exception cause) {
		if(Logger.isTuiActive()
				|| JmxTuiConnectionLossHandler.isConnectionLossUiActive()
				|| JmxTuiConnectionLossHandler.isLossDialogSessionActive()) {
			return false;
		}
		IOException disconnectCause = JmxConnectionRecovery.asDisconnectIOException(cause);
		if(JMXControl.isOfflineSession || connector == null || disconnectCause == null) {
			return false;
		}
		try {
			JmxConnectionRecovery.reconnectOrThrow(connector, disconnectCause);
			return connector.isInitialized();
		} catch(Exception e) {
			return false;
		}
	}

	private static String formatReconnectError(Exception e) {
		if(e == null) {
			return "Reconnect failed.";
		}
		String msg = e.getMessage();
		if(msg != null && msg.trim().length() > 0) {
			return msg;
		}
		return e.getClass().getSimpleName() + ": Reconnect failed.";
	}

	private void reloadObjectNamesFromServer() {
		try {
			MBeanConnector connector = executor.getMBeanConnector();
			if(connector != null && connector.getMBeanConnection() != null) {
				executor.refreshObjectNameList(JMXUtil.getObjectNames(connector.getMBeanConnection(), null));
			}
		} catch(Exception e) {
			Logger.logln(Logger.Level.ERROR, "Failed to refresh MBean list after reconnect: " + e.getMessage());
		}
		List<String> names = executor.getObjectNameList();
		if(names == null) {
			currentMbeanNames = new ArrayList<String>();
		} else {
			currentMbeanNames = names;
		}
	}

	/**
	 * Refreshes list/status panes in every UI mode after a successful user reconnect.
	 */
	private void refreshAfterReconnect() throws Exception {
		JmxCommand.clearMbeanInfoCache();
		reloadObjectNamesFromServer();

		JmxTuiModeContext mbeanCtx = MBeanUiMode.getInstance().getOrCreateContext(this);
		clearFeatures(mbeanCtx);
		applyMbeanSearchFilter(mbeanCtx);

		JmxTuiModeContext recordCtx = RecordUiMode.getInstance().getOrCreateContext(this);
		applyMbeanSearchFilter(recordCtx);

		JmxTuiModeContext threadCtx = ThreadUiMode.getInstance().getOrCreateContext(this);
		threadCtx.clearCpuSnapshot();
		refreshThreads(threadCtx, false, true);

		reapplyCurrentMbeanObjectAfterReconnect();
		refreshHeader();
		invalidatePanesAfterReconnect();
	}

	private void reapplyCurrentMbeanObjectAfterReconnect() {
		String objectName = null;
		JmxTuiModeContext ctx = activeContext;
		if(ctx != null && !ctx.isThreadMode()) {
			objectName = resolveSelectedMbeanObjectName(ctx);
		}
		if(objectName == null || objectName.length() == 0) {
			JmxSettings settings = executor.getSettings();
			if(settings != null) {
				objectName = settings.getMBeanObjectName();
			}
		}
		if(objectName == null || objectName.length() == 0) {
			return;
		}
		try {
			executor.processCommandForTui("OBJECT \"" + objectName + "\"");
		} catch(Exception e) {
			Logger.logln(Logger.Level.ERROR,
					"Failed to re-apply OBJECT after reconnect: " + e.getMessage());
		}
		JmxTuiModeContext mbeanCtx = MBeanUiMode.getInstance().getOrCreateContext(this);
		if(mbeanCtx.isFeaturesVisible()) {
			refreshFeatures(mbeanCtx, objectName);
		}
	}

	private void invalidatePanesAfterReconnect() {
		if(root != null) {
			root.invalidate();
		}
		JmxTuiModeContext[] contexts = new JmxTuiModeContext[] {
			MBeanUiMode.getInstance().getOrCreateContext(this),
			ThreadUiMode.getInstance().getOrCreateContext(this),
			RecordUiMode.getInstance().getOrCreateContext(this)
		};
		for(int i = 0; i < contexts.length; i++) {
			JmxTuiModeContext c = contexts[i];
			if(c == null) {
				continue;
			}
			c.getListsPanel().invalidate();
			c.getMbeanList().invalidate();
			if(c.isMbeanUiMode()) {
				c.getFeatureList().invalidate();
			}
			if(c.getThreadHeaderLabel() != null) {
				c.getThreadHeaderLabel().invalidate();
			}
		}
	}

	public Panel getRoot() {
		return root;
	}

	public Panel getListsPanel() {
		return activeContext == null ? null : activeContext.getListsPanel();
	}

	public Component getOutputPaneBordered() {
		return activeContext == null ? null : activeContext.getOutputPaneBordered();
	}

	public boolean isOutputVisible() {
		return activeContext != null && activeContext.isOutputVisible();
	}

	private void runCloseAction() {
		threadRefreshScheduler.shutdown();
		Runnable r = closeAction;
		if(r != null) {
			try {
				r.run();
			} catch(Exception e) {}
		}
	}

	/**
	 * @return true if the stroke was consumed and must not be passed to the focused pane
	 */
	public boolean consumeGlobalShortcut(KeyStroke key) {
		if(JmxTuiKeys.isCtrlSuspend(key)) {
			return true;
		}
		if(JmxTuiKeys.isF3Key(key)) {
			cycleUiMode();
			return true;
		}
		if(JmxTuiKeys.isF4Key(key)) {
			if(currentMode != null && ThreadUiMode.ID.equals(currentMode.getId())) {
				if(isStacktraceTargetActive()) {
					return true;
				}
				showEditThreadSelectionDialog(ctx());
				return true;
			}
			return false;
		}
		if(JmxTuiKeys.isCtrlToggleStacktraceTarget(key)) {
			if(toggleStacktraceTargetSetting()) {
				return true;
			}
			return false;
		}
		if(JmxTuiKeys.isCtrlEditReqEntryFile(key)) {
			if(editReqEntryFileSetting()) {
				return true;
			}
			return false;
		}
		if(JmxTuiKeys.isCtrlToggleThreadRefresh(key)) {
			if(currentMode != null && ThreadUiMode.ID.equals(currentMode.getId())) {
				toggleThreadRefreshPolicy(ctx());
				return true;
			}
			return false;
		}
		if(JmxTuiKeys.isCtrlShiftEnter(key)) {
			if(currentMode != null && ThreadUiMode.ID.equals(currentMode.getId())) {
				showSelectedThreadsStackTraceInOutput(ctx());
				return true;
			}
			return false;
		}
		if(JmxTuiKeys.isCtrlToggleOutputPane(key)) {
			toggleOutputPane();
			return true;
		}
		if(JmxTuiKeys.isCtrlTogglePrettyPrint(key)) {
			if(togglePrettyPrintSetting()) {
				return true;
			}
			return false;
		}
		if(JmxTuiKeys.isPaneResizeKey(key)) {
			return handlePaneResizeKey(key);
		}
		if(!JmxTuiKeys.isGlobalShortcut(key)) {
			return false;
		}
		if(key.getKeyType() == KeyType.F1) {
			showHelp();
			return true;
		}
		if(JmxTuiKeys.isExitKey(key)) {
			requestExitWithConfirm();
			return true;
		}
		if(JmxTuiKeys.isF2Key(key)) {
			return dispatchF2ToFocusedPane();
		}
		if(JmxTuiKeys.isCtrlFeaturesToggle(key)) {
			toggleFeaturesPane();
			return true;
		}
		return false;
	}

	/**
	 * Global F2: delegates to the focused pane's {@link TuiF2Handler} implementation.
	 */
	private boolean dispatchF2ToFocusedPane() {
		JmxTuiModeContext c = ctx();
		if(c == null) {
			return false;
		}
		if(c.getMbeanList().isFocused()) {
			if(currentMode != null && ThreadUiMode.ID.equals(currentMode.getId())) {
				return showThreadDetail(c);
			}
			return showMbeanDetail(c);
		}
		if(c.getFeatureList().isFocused()) {
			return showFeatureDetail(c);
		}
		OutputTextBox output = c.getOutput();
		if(output.isFocused()) {
			return output.onF2Key();
		}
		if(input.isFocused()) {
			return input.onF2Key();
		}
		if(c.getMbeanSearch().isFocused()) {
			return c.getMbeanSearch().onF2Key();
		}
		return false;
	}

	private void toggleFeaturesPane() {
		JmxTuiModeContext c = ctx();
		if(c == null) {
			return;
		}
		setFeaturesPaneVisible(!c.isFeaturesVisible());
	}

	private void toggleOutputPane() {
		JmxTuiModeContext c = ctx();
		if(c == null) {
			return;
		}
		setOutputPaneVisible(!c.isOutputVisible());
	}

	/**
	 * MBean UI mode (Online session only): toggles {@link JmxSettings#togglePrettyPrintInResult()},
	 * the same value as {@code SET PRETTY_PRINT ON|OFF}.
	 */
	private boolean isStacktraceTargetActive() {
		JmxSettings settings = executor == null ? null : executor.getSettings();
		return settings != null && settings.getStacktraceTarget() == StacktraceTarget.ACTIVE;
	}

	private void syncStacktraceTargetSelectionIfChanged(JmxTuiModeContext ctx) {
		if(ctx == null || !ctx.isThreadMode()) {
			return;
		}
		JmxSettings settings = executor.getSettings();
		if(settings == null) {
			return;
		}
		StacktraceTarget current = settings.getStacktraceTarget();
		if(current == lastAppliedStacktraceTargetForSelection) {
			return;
		}
		// Apply before refreshThreadListView: it calls refreshHeader() which re-enters this method.
		lastAppliedStacktraceTargetForSelection = current;
		if(current == StacktraceTarget.ACTIVE) {
			syncSelectedExpression(ctx);
			ctx.stashThreadSelectionForActiveMode();
			refreshThreadListView(ctx);
		} else {
			ctx.setSelectedExpression(ctx.getActiveModePreservedExpression());
			reapplyThreadSelectionAfterRefresh(ctx, new HashSet<Long>(ctx.getActiveModePreservedThreadIds()));
			refreshThreadListView(ctx);
		}
	}

	/**
	 * Thread UI mode (Online session only): toggles {@link JmxSettings#toggleStacktraceTarget()},
	 * the same value as {@code SET STACKTRACE_TARGET SELECTED|ACTIVE}.
	 */
	private boolean toggleStacktraceTargetSetting() {
		if(currentMode == null || !ThreadUiMode.ID.equals(currentMode.getId())) {
			return false;
		}
		if(JMXControl.isOfflineSession) {
			return false;
		}
		JmxSettings settings = executor.getSettings();
		if(settings == null) {
			return false;
		}
		settings.toggleStacktraceTarget();
		refreshHeader();
		return true;
	}

	/**
	 * Thread UI mode when {@code STACKTRACE_TARGET} is ACTIVE: edit {@code REQUEST_ENTRY_FILE}
	 * (same as {@code SET REQUEST_ENTRY_FILE}).
	 */
	private boolean editReqEntryFileSetting() {
		if(currentMode == null || !ThreadUiMode.ID.equals(currentMode.getId())) {
			return false;
		}
		if(!isStacktraceTargetActive()) {
			return false;
		}
		JmxSettings settings = executor.getSettings();
		if(settings == null) {
			return false;
		}
		String current = settings.getReqEntryFile();
		if(current == null) {
			current = "";
		}
		String edited = TuiDialogs.showTextInputDialog(
			gui,
			TuiThreadUiKeys.REQUEST_ENTRY_FILE_DIALOG_TITLE,
			TuiThreadUiKeys.REQUEST_ENTRY_FILE_DIALOG_DESCRIPTION,
			current
		);
		if(edited == null) {
			return true;
		}
		settings.setReqEntryFile(edited);
		refreshHeader();
		return true;
	}

	private boolean togglePrettyPrintSetting() {
		if(currentMode == null || !MBeanUiMode.ID.equals(currentMode.getId())) {
			return false;
		}
		if(JMXControl.isOfflineSession) {
			return false;
		}
		JmxSettings settings = executor.getSettings();
		if(settings == null) {
			return false;
		}
		settings.togglePrettyPrintInResult();
		refreshHeader();
		return true;
	}

	private void ensureOutputPaneVisible() {
		JmxTuiModeContext c = ctx();
		if(c != null && !c.isOutputVisible()) {
			setOutputPaneVisible(true);
		}
	}

	private void ensureFeaturesPaneVisible() {
		setFeaturesPaneVisible(true);
	}

	private void setFeaturesPaneVisible(boolean visible) {
		JmxTuiModeContext c = ctx();
		if(c == null || currentMode != null && !currentMode.isFeaturesPaneAvailable()) {
			return;
		}
		if(c.isFeaturesVisible() == visible) {
			return;
		}
		c.setFeaturesVisible(visible);
		if(currentMode != null && currentMode.isFeaturesPaneAvailable()) {
			c.getFeaturesBordered().setVisible(visible);
			if(!visible && c.getFeatureList().isFocused()) {
				c.getMbeanList().takeFocus();
			}
		}
		TerminalSize ts = lastTerminalSize;
		if(ts != null) {
			applyTerminalSize(ts);
		} else {
			root.invalidate();
		}
		refreshHeader();
	}

	private void setOutputPaneVisible(boolean visible) {
		JmxTuiModeContext c = ctx();
		if(c == null || c.isOutputVisible() == visible) {
			return;
		}
		c.setOutputVisible(visible);
		rebuildBodyLayout();
		TerminalSize ts = lastTerminalSize;
		if(ts != null) {
			applyTerminalSize(ts);
		} else {
			root.invalidate();
		}
		refreshHeader();
	}

	/**
	 * Shift+arrows resize panes between Status and Command. Ignored when the current layout
	 * has no horizontal or vertical split (e.g. Features hidden → no left/right resize).
	 */
	private boolean handlePaneResizeKey(KeyStroke key) {
		if(key == null) {
			return false;
		}
		TerminalSize ts = lastTerminalSize;
		if(ts == null) {
			return false;
		}
		if(JmxTuiKeys.isPaneResizeHorizontal(key)) {
			if(!canResizeHorizontalSplit()) {
				return false;
			}
			int deltaCols = key.getKeyType() == KeyType.ArrowLeft
				? -HORIZONTAL_SPLIT_STEP_COLS : HORIZONTAL_SPLIT_STEP_COLS;
			adjustHorizontalSplit(deltaCols);
			return true;
		}
		if(JmxTuiKeys.isPaneResizeVertical(key)) {
			if(!canResizeVerticalSplit()) {
				return false;
			}
			int deltaRows = key.getKeyType() == KeyType.ArrowUp
				? -VERTICAL_SPLIT_STEP_ROWS : VERTICAL_SPLIT_STEP_ROWS;
			adjustVerticalSplit(deltaRows);
			return true;
		}
		return false;
	}

	/** Left/right split: Search/MBeans column vs Features (requires side-by-side panes). */
	private boolean canResizeHorizontalSplit() {
		JmxTuiModeContext c = ctx();
		JmxTuiUiMode m = currentMode;
		if(m == null || c == null) {
			return false;
		}
		if(m.isOutputInRightColumn()) {
			return c.isOutputVisible();
		}
		return m.isFeaturesPaneAvailable() && c.isFeaturesVisible();
	}

	/** Up/down split: lists band vs Output (requires stacked resizable band above Command). */
	private boolean canResizeVerticalSplit() {
		JmxTuiModeContext c = ctx();
		JmxTuiUiMode m = currentMode;
		return m != null && c != null && m.canResizeVerticalSplit() && c.isOutputVisible()
			&& c.getListsPanel() != null && c.getOutputPaneBordered() != null;
	}

	private void adjustHorizontalSplit(int deltaCols) {
		JmxTuiModeContext c = ctx();
		if(c == null) {
			return;
		}
		int gap = 1;
		int availableCols = Math.max(20, lastTerminalSize.getColumns() - gap);
		int minRightCols = (currentMode != null && currentMode.isOutputInRightColumn()) ? 20 : MIN_FEATURES_COLS;
		int maxMbeansCols = availableCols - minRightCols;
		int current = c.getUserMbeansCols() >= 0 ? c.getUserMbeansCols() : computeDefaultMbeansCols(availableCols);
		int next = clamp(current + deltaCols, MIN_MBEANS_COLS, maxMbeansCols);
		if(next == current) {
			return;
		}
		c.setUserMbeansCols(next);
		applyTerminalSize(lastTerminalSize);
	}

	private void adjustVerticalSplit(int deltaRows) {
		JmxTuiModeContext c = ctx();
		if(c == null) {
			return;
		}
		int available = computeAvailableForListAndOutput(lastTerminalSize);
		int minListsBand = computeMinListsBandOuterRows();
		int maxListsBand = available - MIN_OUTPUT_OUTER_ROWS;
		int current = c.getUserListsBandOuterRows() >= 0 ? c.getUserListsBandOuterRows() : computeDefaultListsBandOuterRows(available);
		int next = clamp(current + deltaRows, minListsBand, maxListsBand);
		if(next == current) {
			return;
		}
		c.setUserListsBandOuterRows(next);
		applyTerminalSize(lastTerminalSize);
	}

	private int computeAvailableForListAndOutput(TerminalSize size) {
		int termRows = Math.max(12, size.getRows());
		int contentRows = Math.max(8, termRows - WINDOW_DECORATION_ROWS);
		int titleH = 1;
		int statusOuterH = HEADER_INNER_LINES + BORDER_V_OVERHEAD;
		int cmdOuterH = 1 + BORDER_V_OVERHEAD;
		int available = contentRows - titleH - statusOuterH - cmdOuterH;
		if(available < MIN_LIST_ROWS + BORDER_V_OVERHEAD + MIN_OUTPUT_OUTER_ROWS + 3) {
			available = MIN_LIST_ROWS + BORDER_V_OVERHEAD + MIN_OUTPUT_OUTER_ROWS + 3;
		}
		return available;
	}

	private int computeDefaultMbeansCols(int availableCols) {
		int numerator = 3;
		int denominator = 5;
		int minRightCols = MIN_FEATURES_COLS;
		if(currentMode != null) {
			numerator = currentMode.getDefaultHorizontalLeftShareNumerator();
			denominator = currentMode.getDefaultHorizontalLeftShareDenominator();
			if(currentMode.isOutputInRightColumn()) {
				minRightCols = 20;
			}
		}
		if(denominator < 1) {
			denominator = 1;
		}
		if(numerator < 0) {
			numerator = 0;
		}
		return clamp((availableCols * numerator) / denominator, MIN_MBEANS_COLS,
			Math.max(MIN_MBEANS_COLS, availableCols - minRightCols));
	}

	private int computeDefaultListsBandOuterRows(int availableForListAndOutput) {
		int numerator = 1;
		int denominator = 4;
		if(currentMode != null) {
			numerator = currentMode.getDefaultVerticalListsBandShareNumerator();
			denominator = currentMode.getDefaultVerticalListsBandShareDenominator();
		}
		if(denominator < 1) {
			denominator = 1;
		}
		if(numerator < 0) {
			numerator = 0;
		}
		int mbeanSearchOuterH = 1 + BORDER_V_OVERHEAD;
		int listRowsInner = clamp((availableForListAndOutput * numerator) / denominator, MIN_LIST_ROWS,
			Math.min(MAX_LIST_ROWS, availableForListAndOutput - BORDER_V_OVERHEAD - 2)) + 2;
		return listRowsInner + BORDER_V_OVERHEAD + mbeanSearchOuterH;
	}

	private int computeMinListsBandOuterRows() {
		int mbeanSearchOuterH = 1 + BORDER_V_OVERHEAD;
		return mbeanSearchOuterH + MIN_LIST_ROWS + BORDER_V_OVERHEAD;
	}

	public Logger.OutputSink getOutputSink() {
		return outputSink;
	}

	public Runnable getClearHandler() {
		return new Runnable() {
			@Override
			public void run() {
				gui.getGUIThread().invokeLater(new Runnable() {
					@Override
					public void run() {
						clearOutput();
					}
				});
			}
		};
	}

	public Runnable getAbortUserCaptureRunnable() {
		return new Runnable() {
			@Override
			public void run() {
				runOnGuiThread(new Runnable() {
					@Override
					public void run() {
						outputSink.abortUserCaptureSession();
					}
				});
			}
		};
	}

	public Runnable getCommitUserCapturePreambleRunnable() {
		return new Runnable() {
			@Override
			public void run() {
				runOnGuiThread(new Runnable() {
					@Override
					public void run() {
						clearOutput();
						outputSink.commitUserCapturePreamble();
					}
				});
			}
		};
	}

	private void runOnGuiThread(Runnable action) {
		if(action == null) {
			return;
		}
		try {
			if(isGuiThread()) {
				action.run();
			} else {
				gui.getGUIThread().invokeLater(action);
			}
		} catch(Exception e) {}
	}

	private Panel buildLayout() {
		Panel main = new Panel(new LinearLayout(Direction.VERTICAL));

		main.addComponent(titleLabel);

		statusInner.addComponent(header1);
		statusInner.addComponent(header2);
		statusInner.addComponent(header3);
		statusInner.addComponent(headerKeysGlobal);
		statusInner.addComponent(headerKeysPane);
		main.addComponent(statusInner.withBorder(Borders.singleLine("Status")));

		main.addComponent(bodyRoot, LinearLayout.createLayoutData(Alignment.Fill, GrowPolicy.CanGrow));

		main.addComponent(commandInner.withBorder(Borders.singleLine("Command")));

		return main;
	}

	private void setMode(JmxTuiUiMode mode) {
		if(mode == null || mode == currentMode) {
			return;
		}
		saveCurrentModeFocus();
		boolean prevSuppress = suppressActivePaneUpdates;
		suppressActivePaneUpdates = true;
		try {
			detachLayoutFocusSafely();
			if(activeContext != null) {
				activeContext.deactivateFocus(focusManager);
			}
			currentMode = mode;
			activeContext = mode.getOrCreateContext(this);
			activeContext.activateFocus(focusManager);
			outputSink.setOutput(activeContext.getOutput());
			if(ThreadUiMode.ID.equals(mode.getId())) {
				refreshThreads(activeContext);
				updateThreadAutoRefreshScheduler(activeContext);
			} else {
				stopThreadAutoRefreshScheduler();
				if(currentMbeanNames != null && !currentMbeanNames.isEmpty()) {
					applyMbeanSearchFilter(activeContext);
				}
			}
			rebuildBodyLayoutInternal();
			TerminalSize ts = lastTerminalSize;
			if(ts != null) {
				applyTerminalSize(ts);
			} else {
				root.invalidate();
			}
			refreshHeader();
			restoreCurrentModeFocus();
		} finally {
			suppressActivePaneUpdates = prevSuppress;
		}
	}

	private void cycleUiMode() {
		JmxTuiUiMode m = currentMode;
		if(m == null || MBeanUiMode.ID.equals(m.getId())) {
			setMode(ThreadUiMode.getInstance());
			return;
		}
		if(ThreadUiMode.ID.equals(m.getId())) {
			setMode(RecordUiMode.getInstance());
			return;
		}
		setMode(MBeanUiMode.getInstance());
	}

	private void rebuildBodyLayout() {
		saveCurrentModeFocus();
		detachLayoutFocusSafely();
		rebuildBodyLayoutInternal();
		restoreCurrentModeFocus();
	}

	private void rebuildBodyLayoutInternal() {
		JmxTuiModeContext c = ctx();
		if(c == null) {
			return;
		}
		bodyRoot.removeAllComponents();
		rebuildListsBand(c);
		JmxTuiUiMode m = currentMode;
		if(m != null) {
			m.buildBody(this, bodyRoot);
		} else {
			bodyRoot.addComponent(c.getListsPanel());
		}
		bodyRoot.invalidate();
	}

	private void rebuildListsBand(JmxTuiModeContext c) {
		Panel listsPanel = c.getListsPanel();
		listsPanel.removeAllComponents();
		listsPanel.addComponent(c.getMbeansColumnPanel(), LinearLayout.createLayoutData(Alignment.Fill, GrowPolicy.CanGrow));

		JmxTuiUiMode m = currentMode;
		if(m == null) {
			return;
		}
		if(m.isFeaturesPaneAvailable()) {
			listsPanel.addComponent(c.getFeaturesBordered(), LinearLayout.createLayoutData(Alignment.Fill, GrowPolicy.CanGrow));
			c.getFeaturesBordered().setVisible(c.isFeaturesVisible());
			return;
		}
		if(m.isOutputInRightColumn() && c.isOutputVisible()) {
			listsPanel.addComponent(c.getOutputPaneBordered(), LinearLayout.createLayoutData(Alignment.Fill, GrowPolicy.CanGrow));
		}
	}

	public void applyTerminalSize(TerminalSize size) {
		if(size == null) {
			return;
		}
		lastTerminalSize = size;

		JmxTuiModeContext c = ctx();
		if(c == null) {
			return;
		}

		int cols = Math.max(20, size.getColumns());

		String titleText = JMXerConstant.JMXER_VERSION_STRING + " (TUI)";
		int titlePad = Math.max(0, (cols - TerminalTextUtils.getColumnWidth(titleText)) / 2);
		titleLabel.setText(spaces(titlePad) + titleText);
		titleLabel.setPreferredSize(new TerminalSize(cols, 1));

		JmxTuiUiMode m = currentMode;
		boolean rightColumnVisible = false;
		int minRightCols = MIN_FEATURES_COLS;
		if(m != null) {
			if(m.isFeaturesPaneAvailable()) {
				rightColumnVisible = c.isFeaturesVisible();
				minRightCols = MIN_FEATURES_COLS;
			} else if(m.isOutputInRightColumn()) {
				rightColumnVisible = c.isOutputVisible();
				minRightCols = 20;
			}
		}

		int gap = rightColumnVisible ? 1 : 0;
		int availableCols = Math.max(20, cols - gap);
		int mbeansCols;
		int featuresCols;
		if(rightColumnVisible) {
			if(c.getUserMbeansCols() >= 0) {
				mbeansCols = clamp(c.getUserMbeansCols(), MIN_MBEANS_COLS, Math.max(MIN_MBEANS_COLS, availableCols - minRightCols));
			} else {
				mbeansCols = computeDefaultMbeansCols(availableCols);
			}
			featuresCols = Math.max(minRightCols, availableCols - mbeansCols);
			mbeansCols = availableCols - featuresCols;
		} else {
			mbeansCols = cols;
			featuresCols = 0;
		}

		int titleH = 1;
		int statusOuterH = HEADER_INNER_LINES + BORDER_V_OVERHEAD;
		int cmdOuterH = 1 + BORDER_V_OVERHEAD;
		int mbeanSearchOuterH = 1 + BORDER_V_OVERHEAD;

		int termRows = Math.max(12, size.getRows());
		int contentRows = Math.max(8, termRows - WINDOW_DECORATION_ROWS);
		int availableBodyOuterH = contentRows - titleH - statusOuterH - cmdOuterH;
		if(availableBodyOuterH < MIN_LIST_ROWS + BORDER_V_OVERHEAD + mbeanSearchOuterH) {
			availableBodyOuterH = MIN_LIST_ROWS + BORDER_V_OVERHEAD + mbeanSearchOuterH;
		}

		int listsBandOuterH = availableBodyOuterH;
		int outputOuterH = 0;
		if(m != null && m.canResizeVerticalSplit() && c.isOutputVisible()) {
			int availableForListAndOutput = computeAvailableForListAndOutput(size);
			int minListsBandOuterRows = computeMinListsBandOuterRows();
			int maxListsBandOuterRows = availableForListAndOutput - MIN_OUTPUT_OUTER_ROWS;
			if(c.getUserListsBandOuterRows() >= 0) {
				listsBandOuterH = clamp(c.getUserListsBandOuterRows(), minListsBandOuterRows, maxListsBandOuterRows);
			} else {
				listsBandOuterH = computeDefaultListsBandOuterRows(availableForListAndOutput);
			}
			outputOuterH = Math.max(MIN_OUTPUT_OUTER_ROWS, availableForListAndOutput - listsBandOuterH);
		}

		int mbeansListOuterH = Math.max(MIN_LIST_ROWS + BORDER_V_OVERHEAD, listsBandOuterH - mbeanSearchOuterH);
		int listRowsInner = clamp(mbeansListOuterH - BORDER_V_OVERHEAD, MIN_LIST_ROWS, MAX_LIST_ROWS);
		int mbeansColumnOuterH = mbeanSearchOuterH + mbeansListOuterH;
		if(c.getUserMbeansCols() >= 0 && rightColumnVisible) {
			c.setUserMbeansCols(mbeansCols);
		}
		if(c.getUserListsBandOuterRows() >= 0 && outputOuterH > 0) {
			c.setUserListsBandOuterRows(mbeansColumnOuterH);
		}

		Panel mbeansColumnPanel = c.getMbeansColumnPanel();
		Component mbeanSearchBordered = c.getMbeanSearchBordered();
		Component mbeansListBordered = c.getMbeansListBordered();
		Component featuresBordered = c.getFeaturesBordered();
		Component outputPaneBorder = c.getOutputPaneBordered();
		OutputTextBox output = c.getOutput();

		mbeansColumnPanel.setPreferredSize(new TerminalSize(mbeansCols, mbeansColumnOuterH));
		mbeanSearchBordered.setPreferredSize(new TerminalSize(mbeansCols, mbeanSearchOuterH));
		mbeansListBordered.setPreferredSize(new TerminalSize(mbeansCols, mbeansListOuterH));
		featuresBordered.setPreferredSize(new TerminalSize(featuresCols, mbeansColumnOuterH));
		if(m != null && m.isOutputInRightColumn()) {
			outputPaneBorder.setPreferredSize(new TerminalSize(featuresCols, mbeansColumnOuterH));
		} else {
			outputPaneBorder.setPreferredSize(new TerminalSize(cols, outputOuterH > 0 ? outputOuterH : MIN_OUTPUT_OUTER_ROWS));
		}

		int listInnerWLeft = Math.max(8, mbeansCols - BORDER_V_OVERHEAD);
		int listInnerWRight = Math.max(8, featuresCols - BORDER_V_OVERHEAD);
		int mbeanSearchInnerW = listInnerWLeft;
		int mbeanSearchPromptCols = JmxTuiModeContext.SEARCH_PROMPT_LABEL.length();
		int mbeanSearchInputCols = Math.max(10, mbeanSearchInnerW - mbeanSearchPromptCols);

		MbeanSearchInputBox mbeanSearch = c.getMbeanSearch();
		mbeanSearch.setPreferredSize(new TerminalSize(mbeanSearchInputCols, 1));
		c.getMbeanSearchRow().setPreferredSize(new TerminalSize(mbeanSearchInnerW, 1));
		c.getMbeanSearchInner().setPreferredSize(new TerminalSize(mbeanSearchInnerW, 1));

		if(c.isThreadMode()) {
			Label threadHeader = c.getThreadHeaderLabel();
			if(threadHeader != null) {
				threadHeader.setPreferredSize(new TerminalSize(listInnerWLeft, 1));
				c.setThreadHeaderCols(listInnerWLeft);
			}
			int threadListInnerRows = Math.max(1, listRowsInner - 1);
			c.getMbeanList().setPreferredSize(new TerminalSize(listInnerWLeft, threadListInnerRows));
		} else {
			c.getMbeanList().setPreferredSize(new TerminalSize(listInnerWLeft, listRowsInner));
		}
		if(m != null && m.isFeaturesPaneAvailable() && rightColumnVisible) {
			c.getFeatureList().setPreferredSize(new TerminalSize(listInnerWRight, listRowsInner));
		}

		int outputInnerCols;
		if(m != null && m.isOutputInRightColumn()) {
			int outputInnerH = Math.max(1, mbeansColumnOuterH - BORDER_V_OVERHEAD);
			outputInnerCols = Math.max(10, featuresCols - BORDER_V_OVERHEAD);
			output.setPreferredSize(new TerminalSize(outputInnerCols, outputInnerH));
		} else {
			int outputInnerH = Math.max(1, outputOuterH - BORDER_V_OVERHEAD);
			outputInnerCols = Math.max(10, cols - BORDER_V_OVERHEAD);
			output.setPreferredSize(new TerminalSize(outputInnerCols, outputInnerH));
		}
		c.setOutputInnerCols(outputInnerCols);

		int promptCols = (prompt == null ? 0 : prompt.length()) + 3;
		int inputCols = Math.max(10, cols - BORDER_V_OVERHEAD - promptCols);
		input.setPreferredSize(new TerminalSize(inputCols, 1));
		inputRow.setPreferredSize(new TerminalSize(Math.max(10, cols - BORDER_V_OVERHEAD), 1));

		titleLabel.setPreferredSize(new TerminalSize(cols, titleH));

		statusInnerCols = Math.max(10, cols - BORDER_V_OVERHEAD);
		header1.setLabelWidth(statusInnerCols);
		header2.setLabelWidth(statusInnerCols);
		header3.setLabelWidth(cols - BORDER_V_OVERHEAD);
		headerKeysGlobal.setLabelWidth(cols - BORDER_V_OVERHEAD);
		headerKeysPane.setLabelWidth(cols - BORDER_V_OVERHEAD);
		statusInner.setPreferredSize(new TerminalSize(cols - BORDER_V_OVERHEAD, HEADER_INNER_LINES));
		c.getListsPanel().setPreferredSize(new TerminalSize(cols, mbeansColumnOuterH));
		commandInner.setPreferredSize(new TerminalSize(cols - BORDER_V_OVERHEAD, 1));

		output.reflowDisplay();

		refreshHeader();
		root.invalidate();
	}

	private static String spaces(int n) {
		if(n <= 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder(n);
		for(int i = 0; i < n; i++) {
			sb.append(' ');
		}
		return sb.toString();
	}

	private static int clamp(int v, int min, int max) {
		if(v < min) {
			return min;
		}
		if(v > max) {
			return max;
		}
		return v;
	}

	private void clearOutput() {
		JmxTuiModeContext c = ctx();
		if(c == null) {
			return;
		}
		outputSink.clearPartialLine();
		outputSink.resetSessionFullLines();
		c.getOutput().clearAll();
		resetOutputPaneBorderTitle(c);
	}

	/** Clears Output in every UI mode after a successful reconnect (drops prior error logs). */
	private void clearAllModeOutputs() {
		outputSink.clearPartialLine();
		outputSink.resetSessionFullLines();
		JmxTuiModeContext[] contexts = new JmxTuiModeContext[] {
			MBeanUiMode.getInstance().getOrCreateContext(this),
			ThreadUiMode.getInstance().getOrCreateContext(this),
			RecordUiMode.getInstance().getOrCreateContext(this)
		};
		for(int i = 0; i < contexts.length; i++) {
			JmxTuiModeContext c = contexts[i];
			if(c == null) {
				continue;
			}
			c.getOutput().clearAll();
			resetOutputPaneBorderTitle(c);
		}
		if(activeContext != null) {
			outputSink.setOutput(activeContext.getOutput());
		}
	}

	/**
	 * Border title is immutable in Lanterna; replace the border component with the same inner panel and a new title.
	 */
	private void tryRedockOutputBorder(JmxTuiModeContext c, String borderTitle) {
		if(root == null || c == null || borderTitle == null) {
			return;
		}
		Component outputPaneBorder = c.getOutputPaneBordered();
		if(outputPaneBorder == null) {
			return;
		}
		try {
			Component prev = outputPaneBorder;
			LayoutData ld = prev.getLayoutData();
			if(ld == null) {
				ld = LinearLayout.createLayoutData(Alignment.Fill, GrowPolicy.CanGrow);
			}
			Component next = c.getOutputInner().withBorder(Borders.singleLine(borderTitle));
			next.setLayoutData(ld);
			c.setOutputPaneBorder((Border)next);

			saveCurrentModeFocus();
			detachLayoutFocusSafely();
			Panel listsPanel = c.getListsPanel();
			if(tryReplaceChild(listsPanel, prev, next) || tryReplaceChild(bodyRoot, prev, next)) {
				restoreCurrentModeFocus();
				root.invalidate();
				return;
			}
			rebuildBodyLayoutInternal();
			restoreCurrentModeFocus();
			root.invalidate();
		} catch(Exception e) {}
	}

	private static boolean tryReplaceChild(Panel parent, Component prev, Component next) {
		if(parent == null || prev == null || next == null) {
			return false;
		}
		try {
			List<Component> list = parent.getChildrenList();
			int idx = list.indexOf(prev);
			if(idx < 0) {
				return false;
			}
			parent.removeComponent(prev);
			parent.addComponent(idx, next);
			return true;
		} catch(Exception e) {}
		return false;
	}

	private void resetOutputPaneBorderTitle(JmxTuiModeContext c) {
		tryRedockOutputBorder(c, "Output");
	}

	@Override
	public void refreshOutputPaneBorderTime() {
		JmxTuiModeContext c = ctx();
		if(c != null) {
			tryRedockOutputBorder(c, "Output [" + LocalTime.now().format(OUTPUT_PANE_TIME_FMT) +"]");
		}
	}

	/** Column width of the Output text area at the current moment (after the latest layout / resize). */
	@Override
	public int getOutputInnerColumnsAtWrite() {
		JmxTuiModeContext c = ctx();
		if(c == null) {
			return 10;
		}
		OutputTextBox output = c.getOutput();
		try {
			TerminalSize sz = output.getSize();
			if(sz != null && sz.getColumns() >= 8) {
				return sz.getColumns();
			}
		} catch(Exception e) {}
		int cached = c.getOutputInnerCols();
		if(cached >= 8) {
			return cached;
		}
		TerminalSize ts = lastTerminalSize;
		if(ts != null) {
			return Math.max(10, ts.getColumns() - BORDER_V_OVERHEAD);
		}
		return 10;
	}

	private void copyOutputToFile(JmxTuiModeContext c) {
		if(c == null) {
			return;
		}
		try {
			List<String> lines = outputSink.getSaveableLogicalLines(c.getOutput());
			if(lines == null || lines.isEmpty()) {
				TuiDialogs.showMessageDialog(gui, "No output", "There is no output to save.", MessageDialogButton.OK);
				return;
			}
			if(lines.size() == 1) {
				String only = lines.get(0);
				if(only == null || only.trim().isEmpty() || JmxTuiOutputSink.EMPTY_OUTPUT_PLACEHOLDER.equals(only.trim())) {
					TuiDialogs.showMessageDialog(gui, "No output", "There is no output to save.", MessageDialogButton.OK);
					return;
				}
			}

			String nl = System.getProperty("line.separator", "\n");
			StringBuilder sb = new StringBuilder();
			for(int i = 0; i < lines.size(); i++) {
				if(i > 0) {
					sb.append(nl);
				}
				String line = lines.get(i);
				sb.append(line == null ? "" : OutputTextFormat.expandTabs(line));
			}

			Path baseDir = Paths.get(System.getProperty("user.dir", "."));
			String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
			Path outFile = baseDir.resolve("JMXer_Output_" + ts + ".txt");
			Files.write(outFile, sb.toString().getBytes(StandardCharsets.UTF_8));

			TuiDialogs.showMessageDialog(
				gui,
				"Saved",
				"Output saved to file:\n" + outFile.toAbsolutePath().toString(),
				MessageDialogButton.OK
			);
		} catch(Exception e) {
			TuiDialogs.showMessageDialog(gui, "Save failed", "Failed to save output: " + e.getMessage(), MessageDialogButton.OK);
		}
	}

	private void wireKeyBindings() {
		input.setKeyHandler(new CommandInputKeyHandler() {
			@Override
			public boolean handle(KeyStroke key) {
				if(key == null) {
					return false;
				}
				if(JmxTuiKeys.isGlobalShortcut(key)) {
					return false;
				}
				if(key.getKeyType() == KeyType.Enter) {
					String line = input.getText();
					if(line != null) {
						line = line.trim();
					}
					if(line == null || line.length() == 0) {
						return true;
					}
					historyCursor = -1;
					historyScratch = "";
					input.setText("");
					if(isExitLikeCommand(line)) {
						requestExitWithConfirm(true);
						return true;
					}
					ensureOutputPaneVisible();
					outputSink.beginUserCaptureSession(line);
					boolean stopRequested = false;
					try {
						stopRequested = executor.processCommandForTui(line);
					} catch(Exception e) {
						Logger.logStackTrace(e);
						Logger.logln(Logger.Level.ERROR, "Failed to run command: " + e.getMessage());
					}
					finally {
						outputSink.endUserCaptureSession();
					}
					if(JmxTuiConnectionLossHandler.isExitScheduled()) {
						return true;
					}
					refreshHeader();
					refreshMBeansIfNeeded(line);
					if(stopRequested) {
						requestExitWithConfirm(false);
					}
					return true;
				}
				if(key.getKeyType() == KeyType.ArrowUp) {
					historyUp();
					return true;
				}
				if(key.getKeyType() == KeyType.ArrowDown) {
					historyDown();
					return true;
				}
				if(key.getKeyType() == KeyType.Escape) {
					// Clear only the visible buffer; do not touch command history.
					input.setText("");
					historyCursor = -1;
					historyScratch = "";
					return true;
				}
				return false;
			}
		});
	}

	private void wireMbeanSearchBindings(final JmxTuiModeContext ctx) {
		ctx.getMbeanSearch().setTextChangedListener(new Runnable() {
			@Override
			public void run() {
				if(ctx == activeContext) {
					if(ctx.isThreadMode()) {
						applyThreadSearchFilter(ctx);
					} else {
						applyMbeanSearchFilter(ctx);
					}
				}
			}
		});
		ctx.getMbeanSearch().setKeyHandler(new MbeanSearchKeyHandler() {
			@Override
			public boolean handle(KeyStroke key) {
				if(key == null) {
					return false;
				}
				if(JmxTuiKeys.isGlobalShortcut(key)) {
					return false;
				}
				if(key.getKeyType() == KeyType.Escape) {
					ctx.getMbeanSearch().setText("");
					return true;
				}
				if(handleThreadBulkSelectionKeys(ctx, key)) {
					return true;
				}
				return false;
			}
		});
	}

	private void wireThreadModeContext(final JmxTuiModeContext ctx) {
		ctx.getOutput().setSaveOutputAction(new Runnable() {
			@Override
			public void run() {
				copyOutputToFile(ctx);
			}
		});
		ctx.getOutput().setWrapModeChangeListener(new Runnable() {
			@Override
			public void run() {
				refreshHeader();
			}
		});
		wireMbeanSearchBindings(ctx);
		ctx.getMbeanList().setKeyHandler(new ListKeyHandler() {
			@Override
			public boolean handle(KeyStroke key) {
				return handleThreadListKey(ctx, key);
			}
		});
	}

	private boolean handleThreadBulkSelectionKeys(JmxTuiModeContext ctx, KeyStroke key) {
		if(key == null || ctx == null || !ctx.isThreadMode()) {
			return false;
		}
		if(JmxTuiKeys.isCtrlShiftSpace(key) || JmxTuiKeys.isCtrlShiftA(key)) {
			if(isStacktraceTargetActive()) {
				return true;
			}
			toggleAllDisplayedThreadsSelection(ctx);
			return true;
		}
		if(JmxTuiKeys.isCtrlShiftD(key)) {
			if(isStacktraceTargetActive()) {
				return true;
			}
			if(confirmClearAllThreadSelections()) {
				clearAllThreadSelections(ctx);
			}
			return true;
		}
		return false;
	}

	private boolean handleThreadListKey(final JmxTuiModeContext ctx, KeyStroke key) {
		if(key == null || ctx == null || !ctx.isThreadMode()) {
			return false;
		}
		if(JmxTuiKeys.isF5Key(key)) {
			forceRefreshThreads(ctx);
			return true;
		}
		if(handleThreadBulkSelectionKeys(ctx, key)) {
			return true;
		}
		if(key.getKeyType() == KeyType.Enter) {
			showCursorThreadStackTraceInOutput(ctx);
			return true;
		}
		if(JmxTuiKeys.isPlainSpaceKey(key)) {
			if(isStacktraceTargetActive()) {
				return true;
			}
			toggleCurrentThreadSelection(ctx);
			return true;
		}
		return false;
	}

	private void showCursorThreadStackTraceInOutput(JmxTuiModeContext ctx) {
		Long threadId = resolveCursorThreadId(ctx);
		if(threadId == null) {
			return;
		}
		if(isStacktraceTargetActive()) {
			executor.setStacktraceSelectedBypassOnce();
		}
		executeCommandFromListSelection("THREAD STACKTRACE " + threadId);
	}

	private void showSelectedThreadsStackTraceInOutput(JmxTuiModeContext ctx) {
		if(ctx == null || !ctx.isThreadMode()) {
			return;
		}
		if(isStacktraceTargetActive()) {
			if(!confirmActiveThreadsStackTrace()) {
				return;
			}
			executeCommandFromListSelection("THREAD STACKTRACE *");
			return;
		}
		int selectedCount = ctx.getSelectedThreadIds().size();
		if(selectedCount < 1) {
			TuiDialogs.showMessageDialog(
				gui,
				TuiThreadUiKeys.BULK_STACKTRACE_CONFIRM_TITLE,
				TuiThreadUiKeys.BULK_STACKTRACE_NONE_SELECTED_MESSAGE,
				MessageDialogButton.OK
			);
			return;
		}
		if(!confirmSelectedThreadsStackTrace(selectedCount)) {
			return;
		}
		String expr = ThreadUiMode.buildSelectedThreadsExpression(ctx);
		if(expr == null || expr.trim().length() == 0) {
			return;
		}
		executeCommandFromListSelection("THREAD STACKTRACE " + expr);
	}

	private boolean confirmActiveThreadsStackTrace() {
		try {
			MessageDialogButton b = TuiDialogs.showMessageDialog(
				gui,
				TuiThreadUiKeys.BULK_STACKTRACE_ACTIVE_CONFIRM_TITLE,
				TuiThreadUiKeys.BULK_STACKTRACE_ACTIVE_CONFIRM_MESSAGE,
				MessageDialogButton.Yes,
				MessageDialogButton.No
			);
			return b == MessageDialogButton.Yes;
		} catch(Exception e) {
			return false;
		}
	}

	private void promptSwitchToSelectedForThreadSelection(JmxTuiModeContext ctx) {
		try {
			MessageDialogButton b = TuiDialogs.showMessageDialog(
				gui,
				TuiThreadUiKeys.SELECTION_REQUIRES_SELECTED_TITLE,
				TuiThreadUiKeys.SELECTION_REQUIRES_SELECTED_MESSAGE,
				MessageDialogButton.Yes,
				MessageDialogButton.No
			);
			if(b == MessageDialogButton.Yes) {
				JmxSettings settings = executor.getSettings();
				if(settings != null) {
					settings.setStacktraceTarget("SELECTED");
				}
				refreshHeader();
			}
		} catch(Exception e) {
			// ignore
		}
	}

	private boolean confirmSelectedThreadsStackTrace(int selectedCount) {
		String message = TuiThreadUiKeys.BULK_STACKTRACE_CONFIRM_MESSAGE_PREFIX
			+ selectedCount
			+ TuiThreadUiKeys.BULK_STACKTRACE_CONFIRM_MESSAGE_SUFFIX;
		try {
			MessageDialogButton b = TuiDialogs.showMessageDialog(
				gui,
				TuiThreadUiKeys.BULK_STACKTRACE_CONFIRM_TITLE,
				message,
				MessageDialogButton.Yes,
				MessageDialogButton.No
			);
			return b == MessageDialogButton.Yes;
		} catch(Exception e) {
			return false;
		}
	}

	private void toggleCurrentThreadSelection(JmxTuiModeContext ctx) {
		Long threadId = resolveCursorThreadId(ctx);
		if(threadId == null) {
			return;
		}
		Set<Long> selected = ctx.getSelectedThreadIds();
		if(selected.contains(threadId)) {
			selected.remove(threadId);
		} else {
			selected.add(threadId);
		}
		refreshThreadListView(ctx);
	}

	private void toggleAllDisplayedThreadsSelection(JmxTuiModeContext ctx) {
		List<Long> displayed = ctx == null ? null : ctx.getDisplayedThreadIds();
		if(displayed == null || displayed.isEmpty()) {
			return;
		}
		Set<Long> selected = ctx.getSelectedThreadIds();
		boolean allVisibleSelected = true;
		for(int i = 0; i < displayed.size(); i++) {
			Long id = displayed.get(i);
			if(id != null && !selected.contains(id)) {
				allVisibleSelected = false;
				break;
			}
		}
		if(allVisibleSelected) {
			deselectDisplayedThreads(ctx);
		} else {
			selectAllDisplayedThreads(ctx);
		}
	}

	private void selectAllDisplayedThreads(JmxTuiModeContext ctx) {
		List<Long> displayed = ctx.getDisplayedThreadIds();
		Set<Long> selected = ctx.getSelectedThreadIds();
		for(int i = 0; i < displayed.size(); i++) {
			Long id = displayed.get(i);
			if(id != null) {
				selected.add(id);
			}
		}
		refreshThreadListView(ctx);
	}

	private void deselectDisplayedThreads(JmxTuiModeContext ctx) {
		List<Long> displayed = ctx.getDisplayedThreadIds();
		Set<Long> selected = ctx.getSelectedThreadIds();
		for(int i = 0; i < displayed.size(); i++) {
			selected.remove(displayed.get(i));
		}
		refreshThreadListView(ctx);
	}

	private void clearAllThreadSelections(JmxTuiModeContext ctx) {
		ctx.getSelectedThreadIds().clear();
		refreshThreadListView(ctx);
	}

	private void showEditThreadSelectionDialog(JmxTuiModeContext ctx) {
		if(ctx == null || !ctx.isThreadMode()) {
			return;
		}
		String initial = ThreadUiMode.buildSelectedThreadsExpression(ctx);
		if(initial == null) {
			initial = "";
		}
		String edited = TuiDialogs.showTextInputDialog(
			gui,
			TuiThreadUiKeys.DIALOG_TITLE,
			TuiThreadUiKeys.DIALOG_DESCRIPTION,
			initial
		);
		if(edited == null) {
			return;
		}
		applyThreadSelectionFromExpression(ctx, edited);
	}

	private void applyThreadSelectionFromExpression(JmxTuiModeContext ctx, String expr) {
		if(ctx == null || !ctx.isThreadMode()) {
			return;
		}
		String trimmed = expr == null ? "" : expr.trim();
		Set<Long> newSelection = new HashSet<Long>();
		if(trimmed.length() > 0) {
			Map<Long, String> threadMap = ThreadUiMode.buildThreadMap(ctx);
			try {
				Long[] ids = JmxThreadCommand.getTargetThreadIds(threadMap, trimmed);
				if(ids != null) {
					for(int i = 0; i < ids.length; i++) {
						if(ids[i] != null) {
							newSelection.add(ids[i]);
						}
					}
				}
			} catch(Exception e) {
				String msg = e.getMessage();
				if(msg == null || msg.trim().length() == 0) {
					msg = TuiThreadUiKeys.PARSE_FALLBACK_MESSAGE;
				}
				TuiDialogs.showMessageDialog(
					gui,
					TuiThreadUiKeys.VALIDATION_DIALOG_TITLE,
					msg,
					MessageDialogButton.OK
				);
				return;
			}
		}
		ctx.getSelectedThreadIds().clear();
		ctx.getSelectedThreadIds().addAll(newSelection);
		ctx.setSelectedExpression(trimmed.length() > 0 ? trimmed : "");
		refreshThreadListView(ctx);
	}

	private boolean confirmClearAllThreadSelections() {
		try {
			MessageDialogButton b = TuiDialogs.showMessageDialog(
				gui,
				"Clear Selections",
				"This will clear all selected threads. Continue?",
				MessageDialogButton.Yes,
				MessageDialogButton.No
			);
			return b == MessageDialogButton.Yes;
		} catch(Exception e) {
			return true;
		}
	}

	private void refreshThreadListView(JmxTuiModeContext ctx) {
		syncSelectedExpression(ctx);
		ThreadSelectListItemRenderer renderer = ctx.getThreadListRenderer();
		if(renderer != null) {
			renderer.setDisplayedThreadIds(ctx.getDisplayedThreadIds());
			renderer.setSelectedThreadIds(ctx.getSelectedThreadIds());
		}
		ctx.getMbeanList().invalidate();
		refreshHeader();
	}

	private void syncSelectedExpression(JmxTuiModeContext ctx) {
		if(ctx == null || !ctx.isThreadMode()) {
			return;
		}
		String expr = ThreadUiMode.buildSelectedThreadsExpression(ctx);
		ctx.setSelectedExpression(expr == null ? "" : expr);
	}

	private void onThreadAutoRefreshTick() {
		if(currentMode == null || !ThreadUiMode.ID.equals(currentMode.getId())) {
			return;
		}
		JmxTuiModeContext ctx = activeContext;
		if(ctx == null || !ctx.isThreadMode() || !ctx.isThreadAutoRefreshEnabled()) {
			return;
		}
		refreshThreads(ctx, false, false, true);
	}

	private void forceRefreshThreads(JmxTuiModeContext ctx) {
		if(ctx == null || !ctx.isThreadMode()) {
			return;
		}
		refreshThreads(ctx, false, false, true);
	}

	private void toggleThreadRefreshPolicy(JmxTuiModeContext ctx) {
		if(ctx == null || !ctx.isThreadMode()) {
			return;
		}
		if(ctx.isThreadAutoRefreshEnabled()) {
			ctx.setThreadAutoRefreshEnabled(false);
			stopThreadAutoRefreshScheduler();
		} else {
			Integer interval = TuiThreadRefreshDialog.show(gui, ctx.getThreadRefreshIntervalSeconds());
			if(interval == null) {
				ctx.setThreadAutoRefreshEnabled(false);
				stopThreadAutoRefreshScheduler();
				refreshHeader();
				return;
			}
			ctx.setThreadRefreshIntervalSeconds(interval.intValue());
			ctx.setThreadAutoRefreshEnabled(true);
			forceRefreshThreads(ctx);
			startThreadAutoRefreshScheduler(ctx);
		}
		refreshHeader();
	}

	private void updateThreadAutoRefreshScheduler(JmxTuiModeContext ctx) {
		if(ctx == null || !ctx.isThreadMode() || !ctx.isThreadAutoRefreshEnabled()) {
			stopThreadAutoRefreshScheduler();
			return;
		}
		startThreadAutoRefreshScheduler(ctx);
	}

	private void startThreadAutoRefreshScheduler(JmxTuiModeContext ctx) {
		if(ctx == null || !ctx.isThreadMode() || !ctx.isThreadAutoRefreshEnabled()) {
			return;
		}
		threadRefreshScheduler.start(ctx.getThreadRefreshIntervalSeconds());
	}

	private void stopThreadAutoRefreshScheduler() {
		threadRefreshScheduler.stop();
	}

	private Long resolveCursorThreadId(JmxTuiModeContext ctx) {
		if(ctx == null) {
			return null;
		}
		List<Long> displayed = ctx.getDisplayedThreadIds();
		if(displayed.isEmpty()) {
			return null;
		}
		int idx = getSelectedIndexSafe(ctx.getMbeanList());
		if(idx < 0 || idx >= displayed.size()) {
			return null;
		}
		return displayed.get(idx);
	}

	private void refreshThreads(JmxTuiModeContext ctx) {
		refreshThreads(ctx, false, false, false);
	}

	private void refreshThreads(JmxTuiModeContext ctx, boolean afterDisconnectHandling) {
		refreshThreads(ctx, afterDisconnectHandling, false, false);
	}

	private void refreshThreads(JmxTuiModeContext ctx, boolean afterDisconnectHandling, boolean forceOnlineFetch) {
		refreshThreads(ctx, afterDisconnectHandling, forceOnlineFetch, false);
	}

	private void refreshThreads(JmxTuiModeContext ctx, boolean afterDisconnectHandling, boolean forceOnlineFetch, boolean markDelta) {
		if(ctx == null || !ctx.isThreadMode()) {
			return;
		}
		Set<Long> preservedSelection = new HashSet<Long>(ctx.getSelectedThreadIds());
		Map<Long, String> prevThreadNames = ctx.getPrevThreadNamesSnapshot();
		boolean hadPreviousThreadSnapshot = ctx.hasPreviousThreadSnapshot();
		List<ThreadSnapshotEntry> entries = ctx.getAllThreadEntries();
		entries.clear();
		if(!markDelta) {
			ctx.setLastCreatedThreadCount(0);
			ctx.setLastDestroyedThreadCount(0);
		}

		if(JMXControl.isOfflineSession) {
			reapplyThreadSelectionAfterRefresh(ctx, preservedSelection);
			applyThreadSearchFilter(ctx);
			refreshThreadListBorderTime(ctx);
			return;
		}

		MBeanConnector connector = executor.getMBeanConnector();
		if(connector == null) {
			reapplyThreadSelectionAfterRefresh(ctx, preservedSelection);
			applyThreadSearchFilter(ctx);
			refreshThreadListBorderTime(ctx);
			return;
		}

		if(!forceOnlineFetch && JmxTuiConnectionLossHandler.isConnectionLossUiActive()) {
			reapplyThreadSelectionAfterRefresh(ctx, preservedSelection);
			applyThreadSearchFilter(ctx);
			refreshThreadListBorderTime(ctx);
			return;
		}

		if(!forceOnlineFetch && JmxTuiConnectionLossHandler.isLossDialogSessionActive()) {
			reapplyThreadSelectionAfterRefresh(ctx, preservedSelection);
			applyThreadSearchFilter(ctx);
			refreshThreadListBorderTime(ctx);
			return;
		}

		if(!connector.isInitialized()) {
			reapplyThreadSelectionAfterRefresh(ctx, preservedSelection);
			applyThreadSearchFilter(ctx);
			refreshThreadListBorderTime(ctx);
			return;
		}

		MBeanServerConnection mbeanConn = connector.getMBeanConnection();
		if(mbeanConn == null) {
			connector.markConnectionStale();
			reapplyThreadSelectionAfterRefresh(ctx, preservedSelection);
			applyThreadSearchFilter(ctx);
			refreshThreadListBorderTime(ctx);
			return;
		}

		try {
			Map<Long, ThreadInfo> threadInfoMap = JMXUtil.getAllThreadInfo(mbeanConn);
			List<Long> ids = new ArrayList<Long>(threadInfoMap.keySet());
			Collections.sort(ids);

			boolean threadCpuTimeEnabled = false;
			long[] threadCpuTimes = null;
			boolean threadAllocatedMemoryEnabled = false;
			long[] threadAllocatedBytes = null;
			if(!ids.isEmpty()) {
				try {
					Object enabledFlag = JMXUtil.getJMXResult(mbeanConn, THREADING_JMX_NAME, "ThreadCpuTimeEnabled", null);
					threadCpuTimeEnabled = enabledFlag instanceof Boolean && ((Boolean) enabledFlag).booleanValue();
				} catch(Exception e) {
					threadCpuTimeEnabled = false;
				}
				if(threadCpuTimeEnabled) {
					String threadIdsString = buildThreadIdsArgument(ids);
					ObjectName threadingName = new ObjectName(THREADING_JMX_NAME);
					Object cpuResult = JMXUtil.invokeAttributeOrOperation(
						mbeanConn, threadingName, "getThreadCpuTime", new String[] { threadIdsString });
					if(cpuResult instanceof long[]) {
						threadCpuTimes = (long[]) cpuResult;
					}
				}

				try {
					Object enabledFlag = JMXUtil.getJMXResult(mbeanConn, THREADING_JMX_NAME, "ThreadAllocatedMemoryEnabled", null);
					threadAllocatedMemoryEnabled = enabledFlag instanceof Boolean && ((Boolean) enabledFlag).booleanValue();
				} catch(Exception e) {
					threadAllocatedMemoryEnabled = false;
				}
				if(threadAllocatedMemoryEnabled) {
					String threadIdsString = buildThreadIdsArgument(ids);
					ObjectName threadingName = new ObjectName(THREADING_JMX_NAME);
					Object memResult = JMXUtil.invokeAttributeOrOperation(
						mbeanConn, threadingName, "getThreadAllocatedBytes", new String[] { threadIdsString });
					if(memResult instanceof long[]) {
						threadAllocatedBytes = (long[]) memResult;
					}
				}
			}

			long wallNowNanos = System.nanoTime();
			boolean hasPreviousCpuSnapshot = ctx.hasPreviousCpuSnapshot();
			boolean hasPreviousMemSnapshot = ctx.hasPreviousMemSnapshot();
			long prevWallNanos = ctx.getPrevSnapshotWallNanos();
			Map<Long, Long> currentCpuNanos = new HashMap<Long, Long>();
			Map<Long, Long> currentAllocatedBytes = new HashMap<Long, Long>();

			Set<Long> currentThreadIds = new HashSet<Long>();
			int createdCount = 0;
			for(int i = 0; i < ids.size(); i++) {
				Long id = ids.get(i);
				ThreadInfo info = threadInfoMap.get(id);
				if(info == null) {
					continue;
				}
				currentThreadIds.add(id);
				String threadName = info.getThreadName() == null ? "" : info.getThreadName();
				String stateAbbrev = ThreadPaneTableFormat.abbreviateState(info);
				boolean cpuValid = false;
				double cpuCores = 0.0;
				long cpuNowNanos = -1L;
				double memBytesPerSec = 0.0;
				boolean memValid = false;
				long memNowBytes = -1L;
				if(threadCpuTimes != null && i < threadCpuTimes.length) {
					cpuNowNanos = threadCpuTimes[i];
				}
				if(cpuNowNanos >= 0L) {
					currentCpuNanos.put(id, Long.valueOf(cpuNowNanos));
				}
				if(threadCpuTimeEnabled && hasPreviousCpuSnapshot && cpuNowNanos >= 0L) {
					Long prevCpuNanos = ctx.getPrevThreadCpuNanos(id.longValue());
					if(prevCpuNanos != null) {
						long deltaCpu = cpuNowNanos - prevCpuNanos.longValue();
						long deltaWall = wallNowNanos - prevWallNanos;
						if(deltaCpu >= 0L && deltaWall > 0L) {
							cpuCores = (double) deltaCpu / (double) deltaWall;
							cpuValid = true;
						}
					}
				}

				if(threadAllocatedBytes != null && i < threadAllocatedBytes.length) {
					memNowBytes = threadAllocatedBytes[i];
				}
				if(memNowBytes >= 0L) {
					currentAllocatedBytes.put(id, Long.valueOf(memNowBytes));
				}
				if(threadAllocatedMemoryEnabled && hasPreviousMemSnapshot && memNowBytes >= 0L) {
					Long prevMemBytes = ctx.getPrevThreadAllocatedBytes(id.longValue());
					if(prevMemBytes != null) {
						long deltaBytes = memNowBytes - prevMemBytes.longValue();
						long deltaWall = wallNowNanos - prevWallNanos;
						if(deltaBytes >= 0L && deltaWall > 0L) {
							double deltaWallSeconds = (double) deltaWall / 1000000000.0d;
							memBytesPerSec = (double) deltaBytes / deltaWallSeconds;
							memValid = true;
						}
					}
				}

				ThreadRowState rowState = resolveThreadRowState(
					id.longValue(),
					threadName,
					cpuNowNanos,
					memNowBytes,
					threadCpuTimeEnabled,
					threadAllocatedMemoryEnabled,
					prevThreadNames,
					ctx,
					markDelta && hadPreviousThreadSnapshot);
				if(rowState == ThreadRowState.CREATED) {
					createdCount++;
				}

				entries.add(new ThreadSnapshotEntry(
					id.longValue(),
					threadName,
					stateAbbrev,
					cpuCores,
					cpuValid,
					memBytesPerSec,
					memValid,
					rowState));
			}

			int destroyedCount = 0;
			if(markDelta && hadPreviousThreadSnapshot) {
				for(Map.Entry<Long, String> prevEntry : prevThreadNames.entrySet()) {
					Long prevId = prevEntry.getKey();
					if(prevId == null || currentThreadIds.contains(prevId)) {
						continue;
					}
					destroyedCount++;
				}
			}
			if(markDelta && hadPreviousThreadSnapshot) {
				ctx.setLastCreatedThreadCount(createdCount);
				ctx.setLastDestroyedThreadCount(destroyedCount);
			}

			Map<Long, String> liveThreadNames = new HashMap<Long, String>();
			for(int i = 0; i < entries.size(); i++) {
				ThreadSnapshotEntry entry = entries.get(i);
				if(entry != null && entry.isLive()) {
					liveThreadNames.put(Long.valueOf(entry.getThreadId()), entry.getThreadName());
				}
			}
			ctx.commitThreadSnapshot(liveThreadNames);
			ctx.commitCpuSnapshot(currentCpuNanos, wallNowNanos);
			ctx.commitMemSnapshot(currentAllocatedBytes, wallNowNanos);
		} catch(Exception e) {
			JmxConnectionRecovery.handleTuiDisconnect(connector, e, afterDisconnectHandling);
			if(!afterDisconnectHandling && !Logger.isTuiActive() && attemptOnlineJmxReconnect(connector, e)) {
				refreshThreads(ctx, true);
				return;
			}
			if(JmxTuiConnectionLossHandler.isExitScheduled()) {
				return;
			}
			if(JmxConnectionRecovery.asDisconnectIOException(e) == null) {
				Logger.logln(Logger.Level.ERROR, "Failed to get thread list: " + e.getMessage());
			}
		}

		reapplyThreadSelectionAfterRefresh(ctx, preservedSelection);
		applyThreadSearchFilter(ctx);
		refreshThreadListBorderTime(ctx);
	}

	private static ThreadRowState resolveThreadRowState(
			long threadId,
			String threadName,
			long cpuNowNanos,
			long memNowBytes,
			boolean threadCpuTimeEnabled,
			boolean threadAllocatedMemoryEnabled,
			Map<Long, String> prevThreadNames,
			JmxTuiModeContext ctx,
			boolean compareDelta) {
		if(!compareDelta || prevThreadNames == null) {
			return ThreadRowState.NORMAL;
		}
		String prevName = prevThreadNames.get(Long.valueOf(threadId));
		if(prevName == null) {
			return ThreadRowState.CREATED;
		}
		String currentName = threadName == null ? "" : threadName;
		if(!prevName.equals(currentName)) {
			return ThreadRowState.CREATED;
		}
		if(threadCpuTimeEnabled && cpuNowNanos >= 0L) {
			Long prevCpuNanos = ctx.getPrevThreadCpuNanos(threadId);
			if(prevCpuNanos != null && cpuNowNanos < prevCpuNanos.longValue()) {
				return ThreadRowState.CREATED;
			}
		}
		if(threadAllocatedMemoryEnabled && memNowBytes >= 0L) {
			Long prevMemBytes = ctx.getPrevThreadAllocatedBytes(threadId);
			if(prevMemBytes != null && memNowBytes < prevMemBytes.longValue()) {
				return ThreadRowState.CREATED;
			}
		}
		return ThreadRowState.NORMAL;
	}

	private void reapplyThreadSelectionAfterRefresh(JmxTuiModeContext ctx, Set<Long> preservedSelection) {
		if(ctx == null) {
			return;
		}
		String expr = ctx.getSelectedExpression();
		if(expr != null && expr.trim().length() > 0) {
			Map<Long, String> threadMap = ThreadUiMode.buildThreadMap(ctx);
			try {
				Long[] ids = JmxThreadCommand.getTargetThreadIds(threadMap, expr.trim());
				Set<Long> newSelection = new HashSet<Long>();
				if(ids != null) {
					for(int i = 0; i < ids.length; i++) {
						if(ids[i] != null) {
							newSelection.add(ids[i]);
						}
					}
				}
				ctx.getSelectedThreadIds().clear();
				ctx.getSelectedThreadIds().addAll(newSelection);
				return;
			} catch(Exception e) {
				// Fall back to preserving explicit IDs.
			}
		}
		preserveThreadSelection(ctx, preservedSelection);
	}

	private static void preserveThreadSelection(JmxTuiModeContext ctx, Set<Long> preservedSelection) {
		Set<Long> selected = ctx.getSelectedThreadIds();
		selected.clear();
		List<ThreadSnapshotEntry> entries = ctx.getAllThreadEntries();
		for(Long id : preservedSelection) {
			if(id == null) {
				continue;
			}
			for(int i = 0; i < entries.size(); i++) {
				if(entries.get(i).getThreadId() == id.longValue()) {
					selected.add(id);
					break;
				}
			}
		}
	}

	private void applyThreadSearchFilter(JmxTuiModeContext ctx) {
		if(ctx == null || !ctx.isThreadMode()) {
			return;
		}
		String query = ctx.getMbeanSearch().getText();
		if(query == null) {
			query = "";
		}
		ThreadSelectListItemRenderer renderer = ctx.getThreadListRenderer();
		if(renderer != null) {
			renderer.setHighlightQuery(query);
			renderer.setSelectedThreadIds(ctx.getSelectedThreadIds());
		}

		Long previousCursorId = resolveCursorThreadId(ctx);
		FocusActionListBox threadList = ctx.getMbeanList();
		threadList.clearItems();
		List<Long> displayedThreadIds = ctx.getDisplayedThreadIds();
		displayedThreadIds.clear();

		List<ThreadSnapshotEntry> allEntries = ctx.getAllThreadEntries();
		int tidWidth = ThreadPaneTableFormat.computeTidWidth(allEntries);
		int cpuWidth = ThreadPaneTableFormat.computeCpuWidth(allEntries);
		int memWidth = ThreadPaneTableFormat.computeMemWidth(allEntries);
		ctx.refreshThreadTableHeader(allEntries);

		if(JMXControl.isOfflineSession && allEntries.isEmpty()) {
			threadList.addItem(JmxTuiModeContext.THREAD_UNAVAILABLE_PLACEHOLDER, NOOP_LIST_ACTION);
			if(renderer != null) {
				renderer.setDisplayedThreadIds(displayedThreadIds);
			}
			threadList.invalidate();
			refreshHeader();
			return;
		}

		if(allEntries.isEmpty()) {
			threadList.addItem("(no threads)", NOOP_LIST_ACTION);
			if(renderer != null) {
				renderer.setDisplayedThreadIds(displayedThreadIds);
			}
			threadList.invalidate();
			refreshHeader();
			return;
		}

		boolean noFilter = query.trim().isEmpty();
		String lowerQuery = query.toLowerCase();
		Map<Long, ThreadRowState> displayedRowStates = new HashMap<Long, ThreadRowState>();
		for(int i = 0; i < allEntries.size(); i++) {
			ThreadSnapshotEntry entry = allEntries.get(i);
			String label = ThreadPaneTableFormat.buildRowLine(entry, tidWidth, cpuWidth, memWidth);
			if(noFilter || label.toLowerCase().contains(lowerQuery)) {
				Long threadId = Long.valueOf(entry.getThreadId());
				displayedThreadIds.add(threadId);
				displayedRowStates.put(threadId, entry.getRowState());
				threadList.addItem(label, new ThreadListItem(entry.getThreadId(), label));
			}
		}

		if(displayedThreadIds.isEmpty()) {
			threadList.addItem("(no matching threads)", NOOP_LIST_ACTION);
		} else if(previousCursorId != null) {
			int idx = displayedThreadIds.indexOf(previousCursorId);
			if(idx >= 0) {
				setSelectedIndexSafe(threadList, idx);
			}
		}

		if(renderer != null) {
			renderer.setDisplayedThreadIds(displayedThreadIds);
			renderer.setDisplayedRowStates(displayedRowStates);
		}
		threadList.invalidate();
		refreshHeader();
	}

	private static String buildThreadIdsArgument(List<Long> ids) {
		if(ids == null || ids.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < ids.size(); i++) {
			Long id = ids.get(i);
			if(id == null) {
				continue;
			}
			if(sb.length() > 0) {
				sb.append(',');
			}
			sb.append(id.longValue());
		}
		return sb.toString();
	}

	private void refreshThreadListBorderTime(JmxTuiModeContext c) {
		if(c == null || !c.isThreadMode()) {
			return;
		}
		tryRedockThreadListBorder(c, JmxTuiModeContext.THREAD_LIST_BORDER_TITLE + " [" + LocalTime.now().format(OUTPUT_PANE_TIME_FMT) + "]");
	}

	private void tryRedockThreadListBorder(JmxTuiModeContext c, String borderTitle) {
		if(root == null || c == null || borderTitle == null) {
			return;
		}
		Component threadListBorder = c.getMbeansListBordered();
		if(threadListBorder == null) {
			return;
		}
		try {
			Component prev = threadListBorder;
			LayoutData ld = prev.getLayoutData();
			if(ld == null) {
				ld = LinearLayout.createLayoutData(Alignment.Fill, GrowPolicy.CanGrow);
			}
			Component next = c.getMbeansInner().withBorder(Borders.singleLine(borderTitle));
			next.setLayoutData(ld);
			c.setMbeansListBordered(next);

			saveCurrentModeFocus();
			detachLayoutFocusSafely();
			Panel columnPanel = c.getMbeansColumnPanel();
			if(tryReplaceChild(columnPanel, prev, next)) {
				restoreCurrentModeFocus();
				root.invalidate();
				return;
			}
			rebuildBodyLayoutInternal();
			restoreCurrentModeFocus();
			root.invalidate();
		} catch(Exception e) {}
	}

	private static final Runnable NOOP_LIST_ACTION = new Runnable() {
		@Override
		public void run() {}
	};

	private void applyMbeanSearchFilter(JmxTuiModeContext ctx) {
		if(ctx == null || ctx.isThreadMode()) {
			return;
		}
		if(ctx.getMbeanListRenderer() == null) {
			return;
		}
		String query = ctx.getMbeanSearch().getText();
		if(query == null) {
			query = "";
		}
		ctx.getMbeanListRenderer().setHighlightQuery(query);

		String previousSelection = resolveSelectedMbeanObjectName(ctx);
		FocusActionListBox mbeanList = ctx.getMbeanList();
		mbeanList.clearItems();
		List<String> displayedMbeanNames = ctx.getDisplayedMbeanNames();
		displayedMbeanNames.clear();

		List<String> names = currentMbeanNames;
		if(names == null || names.isEmpty()) {
			mbeanList.addItem("(no ObjectName list)", new Runnable() {
				@Override
				public void run() {}
			});
			mbeanList.invalidate();
			syncFeaturesToMBeanSelection(ctx);
			return;
		}

		boolean noFilter = query.trim().isEmpty();
		String lowerQuery = query.toLowerCase();
		int maxIndex = names.size();
		for(int i = 0; i < names.size(); i++) {
			final String o = names.get(i);
			if(noFilter || o.toLowerCase().contains(lowerQuery)) {
				displayedMbeanNames.add(o);
				final int listIndex = i + 1;
				final String label = JMXPrintUtil.formatMbeanListPaneLine(listIndex, maxIndex, o);
				mbeanList.addItem(label, new Runnable() {
					@Override
					public void run() {
						selectMBean(ctx, o);
					}
				});
			}
		}

		if(displayedMbeanNames.isEmpty()) {
			mbeanList.addItem("(no matching MBeans)", new Runnable() {
				@Override
				public void run() {}
			});
		} else if(previousSelection != null) {
			int idx = displayedMbeanNames.indexOf(previousSelection);
			if(idx >= 0) {
				setSelectedIndexSafe(mbeanList, idx);
			}
		} else {
			String current = executor.getSettings() == null ? null : executor.getSettings().getMBeanObjectName();
			if(current != null && current.length() > 0) {
				int idx = displayedMbeanNames.indexOf(current);
				if(idx >= 0) {
					setSelectedIndexSafe(mbeanList, idx);
				}
			}
		}

		mbeanList.invalidate();
		syncFeaturesToMBeanSelection(ctx);
	}

	private void refreshMBeansIfNeeded(String line) {
		if(line == null) {
			return;
		}
		String up = line.trim().toUpperCase();
		if(up.startsWith("OBJECT") || up.startsWith("OBJ") || up.startsWith("LIST") || up.startsWith("LS")) {
			refreshMBeans();
		}
	}

	private void showHelp() {
		JmxTuiUiMode m = currentMode;
		JmxTuiModeContext c = ctx();
		TuiDialogs.showMessageDialog(
			gui,
			"Help",
			buildHelpText(m, c),
			MessageDialogButton.OK
		);
	}

	private static String buildHelpText(JmxTuiUiMode mode, JmxTuiModeContext ctx) {
		String body;
		if(mode == null) {
			body = buildHelpTextMBeanMode();
		} else {
			String id = mode.getId();
			if(ThreadUiMode.ID.equals(id)) {
				body = buildHelpTextThreadMode();
			} else if(RecordUiMode.ID.equals(id)) {
				body = buildHelpTextRecordMode();
			} else {
				body = buildHelpTextMBeanMode();
			}
		}
		return body + "\n\n\n" + JMXerConstant.JMXER_COPYRIGHT_STRING;
	}

	private static String buildHelpTextMBeanMode() {
		StringBuilder sb = new StringBuilder();
		sb.append("UI Mode : MBean\n\n");

		sb.append("[Global]\n");
		sb.append("F1: this help\n");
		sb.append("F3: cycle UI mode (MBean → Thread → Record)\n");
		sb.append("Tab / Shift+Tab: move focus between windows\n");
		sb.append("Ctrl+F: toggle 'Features' window\n");
		sb.append("Ctrl+O: toggle 'Output' window\n");
		if(!JMXControl.isOfflineSession) {
			sb.append("Ctrl+P: toggle Pretty Print\n");
		}
		sb.append("Ctrl+C: exit\n");
		sb.append("Shift+Left/Right: resize Search/MBeans vs Features width\n");
		sb.append("Shift+Up/Down: resize lists vs Output height\n");
		sb.append("\n");

		sb.append("[Window — depends on focus]\n");
		sb.append("Search: type to filter list; Esc — clear search\n");
		sb.append("MBeans: arrows, PgUp/Dn, Home/End — navigate; Enter — set OBJECT; F2 — information\n");
		sb.append("Features: arrows, PgUp/Dn, Home/End — navigate; Enter — invoke; F2 — information\n");
		sb.append("Output: arrows & scroll; Ctrl+S — save; Ctrl+L — wrap\n");
		sb.append("Command: type JMXer commands; Up/Down — history; Esc — clear line; Ctrl+V — paste");
		return sb.toString();
	}

	private static String buildHelpTextThreadMode() {
		StringBuilder sb = new StringBuilder();
		sb.append("UI Mode : Thread\n\n");

		sb.append("[Global]\n");
		sb.append("F1: this help\n");
		sb.append("F3: cycle UI mode (MBean → Thread → Record)\n");
		sb.append("F4: edit thread selection\n");
		sb.append("Tab / Shift+Tab: move focus between windows\n");
		sb.append(TuiThreadUiKeys.REFRESH_TOGGLE_KEY_LABEL).append(": toggle auto/manual thread refresh\n");
		sb.append(TuiThreadUiKeys.STACKTRACE_TARGET_TOGGLE_KEY_LABEL).append(": toggle STACKTRACE_TARGET (SELECTED / ACTIVE)\n");
		sb.append(TuiThreadUiKeys.REQUEST_ENTRY_FILE_EDIT_KEY_LABEL).append(": edit REQUEST_ENTRY_FILE (when STACKTRACE_TARGET is ACTIVE)\n");
		sb.append("Ctrl+Shift+Enter: SELECTED — stacktrace of selected threads; ACTIVE — all active threads\n");
		sb.append("Ctrl+O: toggle 'Output' window\n");
		sb.append("Ctrl+C: exit\n");
		sb.append("Shift+Left/Right: resize Search/Thread vs Output width\n");
		sb.append("\n");

		sb.append("[Window — depends on focus]\n");
		sb.append("Search: type to filter list; Esc — clear search; Ctrl+Shift+A — select all; Ctrl+Shift+D — clear selected\n");
		sb.append("Thread: arrows — navigate; F2 — information; F5 — refresh list; Enter — stacktrace; Space — select; Ctrl+Shift+A/D — select all/clear seleted\n");
		sb.append("Output: arrows & scroll; Ctrl+S — save; Ctrl+L — wrap\n");
		sb.append("Command: type JMXer commands; Up/Down — history; Esc — clear line; Ctrl+V — paste");
		return sb.toString();
	}

	private static String buildHelpTextRecordMode() {
		StringBuilder sb = new StringBuilder();
		sb.append("UI Mode : Record\n\n");

		sb.append("[Global]\n");
		sb.append("F1: this help\n");
		sb.append("F3: cycle UI mode (MBean → Thread → Record)\n");
		sb.append("Tab / Shift+Tab: move focus between windows\n");
		sb.append("Ctrl+O: toggle 'Output' window\n");
		sb.append("Ctrl+C: exit\n");
		//sb.append("Shift+Left/Right: resize Search/Record vs Output width\n");
		sb.append("\n");

		sb.append("[Window — depends on focus]\n");
		sb.append("Search: type to filter list; Esc — clear search\n");
		//sb.append("MBeans: arrows, PgUp/Dn, Home/End — navigate; Enter — set OBJECT; F2 — information\n");
		sb.append("Output: arrows & scroll; Ctrl+S — save; Ctrl+L — wrap\n");
		sb.append("Command: type JMXer commands; Up/Down — history; Esc — clear line; Ctrl+V — paste");
		return sb.toString();
	}

	private void historyUp() {
		History hist = executor.getHistory();
		if(hist == null || hist.size() == 0) {
			return;
		}
		if(historyCursor < 0) {
			historyScratch = input.getText();
			historyCursor = hist.size();
		} else if(historyCursor > 1) {
			historyCursor--;
		}
		input.setText(hist.get(historyCursor).getCommandLine());
	}

	private void historyDown() {
		History hist = executor.getHistory();
		if(hist == null || hist.size() == 0) {
			return;
		}
		if(historyCursor < 0) {
			return;
		}
		if(historyCursor < hist.size()) {
			historyCursor++;
			input.setText(hist.get(historyCursor).getCommandLine());
		} else {
			historyCursor = -1;
			input.setText(historyScratch == null ? "" : historyScratch);
		}
	}

	private void refreshHeader() {
		JmxTuiModeContext c = ctx();
		if(currentMode != null && ThreadUiMode.ID.equals(currentMode.getId())) {
			syncStacktraceTargetSelectionIfChanged(c);
		}
		JmxSettings s = executor.getSettings();
		String sessionMode = JMXControl.isOfflineSession ? "Offline" : "Online";
		String uiMode = currentMode == null ? "MBean" : currentMode.getDisplayName();
		String connTarget = "";
		if(executor.getMBeanConnector() != null) {
			connTarget = executor.getMBeanConnector().getConnectType().name() + ":" + executor.getMBeanConnector().getTargetName();
		}
		header1.setText(TuiStatusText.fitSingleLine(
			" Session Mode: " + sessionMode + (JMXControl.isOfflineSession ? "" : ("(" + connTarget + ")")) + " | UI Mode: " + uiMode,
			statusInnerCols));
		String mbeanName = s.getMBeanObjectName() == null ? "" : s.getMBeanObjectName();
		if(currentMode != null) {
			currentMode.renderStatusObjectLine(header2, s, c, statusInnerCols);
		} else {
			header2.setText(TuiStatusText.fitSingleLine("   - Current MBean:" + mbeanName, statusInnerCols));
		}
		if(currentMode != null) {
			currentMode.renderStatusDetailLine(header3, s, c, statusInnerCols);
		} else {
			header3.setText(TuiStatusText.fitSingleLine("", statusInnerCols));
		}
		boolean outputOn = c != null && c.isOutputVisible();
		boolean featuresOn = c != null && c.isFeaturesVisible();
		JmxTuiUiMode m = currentMode;
		boolean featuresAvailable = m != null && m.isFeaturesPaneAvailable();
		boolean canResizeVerticalSplit = m != null && m.canResizeVerticalSplit();
		String globalKeys;
		if(m == null || MBeanUiMode.ID.equals(m.getId())) {
			globalKeys = TuiGlobalKeysHint.mbean(outputOn, featuresOn, featuresAvailable, canResizeVerticalSplit);
		} else if(ThreadUiMode.ID.equals(m.getId())) {
			boolean stacktraceTargetActive = s.getStacktraceTarget() == StacktraceTarget.ACTIVE;
			globalKeys = TuiGlobalKeysHint.thread(outputOn, stacktraceTargetActive);
		} else if(RecordUiMode.ID.equals(m.getId())) {
			globalKeys = TuiGlobalKeysHint.record(outputOn);
		} else {
			globalKeys = TuiGlobalKeysHint.mbean(outputOn, featuresOn, featuresAvailable, canResizeVerticalSplit);
		}
		headerKeysGlobal.setText(TuiStatusText.fitSingleLine(globalKeys, statusInnerCols));
		boolean wrapOn = c != null && c.getOutput().isWrapMode();
		JmxTuiModeFocusPane focusedPane = resolveCurrentFocusPane();
		String paneKeys = currentMode == null ? "" : currentMode.getPaneKeysHint(focusedPane, wrapOn, s);
		headerKeysPane.setText(TuiStatusText.fitSingleLine(paneKeys, statusInnerCols));
	}

	private void refreshMBeans() {
		JmxTuiModeContext c = ctx();
		if(c == null) {
			return;
		}
		clearFeatures(c);

		List<String> names = executor.getObjectNameList();
		if(names == null) {
			currentMbeanNames = new ArrayList<String>();
		} else {
			currentMbeanNames = names;
		}
		if(!c.isThreadMode()) {
			applyMbeanSearchFilter(c);
		}
	}

	private void clearFeatures(JmxTuiModeContext ctx) {
		ctx.getFeatureList().clearItems();
		ctx.getCurrentFeatures().clear();
		ctx.setFeaturesForObjectName(null);
	}

	private void syncFeaturesToMBeanSelection(JmxTuiModeContext ctx) {
		String objectName = resolveSelectedMbeanObjectName(ctx);
		if(objectName != null && objectName.length() > 0) {
			if(currentMode != null && !currentMode.isFeaturesPaneAvailable()) {
				return;
			}
			if(!ctx.isFeaturesVisible()) {
				return;
			}
			refreshFeatures(ctx, objectName);
		} else {
			clearFeatures(ctx);
		}
	}

	private static int getSelectedIndexSafe(FocusActionListBox list) {
		try {
			java.lang.reflect.Method m = list.getClass().getMethod("getSelectedIndex");
			Object r = m.invoke(list);
			if(r instanceof Integer) {
				return ((Integer)r).intValue();
			}
		} catch(Exception e) {}
		return 0;
	}

	private static void setSelectedIndexSafe(FocusActionListBox list, int idx) {
		try {
			java.lang.reflect.Method m = list.getClass().getMethod("setSelectedIndex", int.class);
			m.invoke(list, Integer.valueOf(idx));
		} catch(Exception e) {}
	}

	private void selectMBean(JmxTuiModeContext ctx, String objectName) {
		ensureOutputPaneVisible();
		outputSink.prepareImplicitCommandOutput();
		try {
			executor.processCommandForTui("OBJECT \"" + objectName + "\"");
		} catch(Exception e) {
			Logger.logStackTrace(e);
			Logger.logln(Logger.Level.ERROR, "Failed to set OBJECT: " + e.getMessage());
		}
		if(currentMode != null && currentMode.isFeaturesPaneAvailable()) {
			ensureFeaturesPaneVisible();
			refreshFeatures(ctx, objectName);
			ctx.getFeatureList().takeFocus();
		}
		refreshHeader();
	}

	private void executeCommandFromListSelection(String commandLine) {
		ensureOutputPaneVisible();
		outputSink.beginUserCaptureSession(commandLine);
		boolean stopRequested = false;
		try {
			stopRequested = executor.processCommandForTui(commandLine);
		} catch(Exception e) {
			Logger.logStackTrace(e);
			Logger.logln(Logger.Level.ERROR, "Failed to run command: " + e.getMessage());
		}
		finally {
			outputSink.endUserCaptureSession();
		}
		if(JmxTuiConnectionLossHandler.isExitScheduled()) {
			return;
		}
		refreshHeader();
		refreshMBeansIfNeeded(commandLine);
		if(stopRequested) {
			requestExitWithConfirm(false);
		}
	}

	private static boolean isExitLikeCommand(String line) {
		if(line == null) {
			return false;
		}
		String t = line.trim();
		if(t.isEmpty()) {
			return false;
		}
		int sp = t.indexOf(' ');
		String verb = (sp < 0 ? t : t.substring(0, sp)).toUpperCase();
		return "EXIT".equals(verb) || "QUIT".equals(verb) || "Q".equals(verb) || "BYE".equals(verb) || "BY".equals(verb);
	}

	private boolean showThreadDetail(JmxTuiModeContext ctx) {
		if(JMXControl.isOfflineSession) {
			TuiDetailDialog.show(gui, "Thread", JmxTuiModeContext.THREAD_UNAVAILABLE_PLACEHOLDER);
			return true;
		}
		Long threadId = resolveCursorThreadId(ctx);
		if(threadId == null) {
			TuiDetailDialog.show(gui, "Thread", "No thread selected.");
			return true;
		}
		String detailText = fetchThreadInfoText(threadId.longValue());
		if(detailText == null || detailText.trim().isEmpty()) {
			detailText = "No thread information available for thread id " + threadId + ".";
		}
		TuiDetailDialog.show(gui, "Thread", detailText);
		return true;
	}

	private String fetchThreadInfoText(long threadId) {
		final StringBuilder buf = new StringBuilder();
		final Logger.OutputSink captureSink = new Logger.OutputSink() {
			@Override
			public void print(Object msg, boolean newline) {
				if(msg != null) {
					buf.append(msg.toString());
				}
				if(newline) {
					buf.append(System.getProperty("line.separator", "\n"));
				}
			}
		};
		Logger.OutputSink prevSink = Logger.getOutputSink();
		boolean prevCapture = Logger.isTuiPanelCapture();
		try {
			Logger.setOutputSink(captureSink);
			Logger.setTuiPanelCapture(false);
			MBeanConnector connector = executor.getMBeanConnector();
			if(connector == null || connector.getMBeanConnection() == null) {
				buf.append("Not connected.");
				return buf.toString();
			}
			JMXPrintUtil.printThreadInfo(connector.getMBeanConnection(), new String[] { String.valueOf(threadId) });
		} catch(Exception e) {
			if(buf.length() == 0) {
				buf.append("Failed to load thread info: ").append(e.getMessage());
			}
		} finally {
			Logger.setTuiPanelCapture(prevCapture);
			Logger.setOutputSink(prevSink);
		}
		return buf.toString();
	}

	private boolean showMbeanDetail(JmxTuiModeContext ctx) {
		String objectName = resolveSelectedMbeanObjectName(ctx);
		if(objectName == null || objectName.length() == 0) {
			TuiDetailDialog.show(gui, "MBean", "No MBean selected.");
			return true;
		}
		String detailText = fetchMbeanOverviewText(objectName);
		if(detailText == null || detailText.trim().isEmpty()) {
			detailText = "No information available for MBean:\n" + objectName;
		}
		TuiDetailDialog.show(gui, "MBean", detailText);
		return true;
	}

	private String fetchMbeanOverviewText(String objectName) {
		final StringBuilder buf = new StringBuilder();
		final Logger.OutputSink captureSink = new Logger.OutputSink() {
			@Override
			public void print(Object msg, boolean newline) {
				if(msg != null) {
					buf.append(msg.toString());
				}
				if(newline) {
					buf.append(System.getProperty("line.separator", "\n"));
				}
			}
		};
		Logger.OutputSink prevSink = Logger.getOutputSink();
		boolean prevCapture = Logger.isTuiPanelCapture();
		try {
			Logger.setOutputSink(captureSink);
			Logger.setTuiPanelCapture(false);
			MBeanConnector connector = executor.getMBeanConnector();
			if(connector == null || connector.getMBeanConnection() == null) {
				buf.append("Not connected.");
				return buf.toString();
			}
			JMXPrintUtil.printMBeanOverview(connector.getMBeanConnection(), new ObjectName(objectName));
		} catch(Exception e) {
			if(buf.length() == 0) {
				buf.append("Failed to load MBean overview: ").append(e.getMessage());
			}
		} finally {
			Logger.setTuiPanelCapture(prevCapture);
			Logger.setOutputSink(prevSink);
		}
		return buf.toString();
	}

	private boolean showFeatureDetail(JmxTuiModeContext ctx) {
		FocusActionListBox featureList = ctx.getFeatureList();
		int idx = getSelectedIndexSafe(featureList);
		String objectName = ctx.getFeaturesForObjectName();
		if(objectName == null || objectName.length() == 0) {
			objectName = resolveSelectedMbeanObjectName(ctx);
		}
		List<MBeanFeatureInfo> currentFeatures = ctx.getCurrentFeatures();
		if(idx < 0 || currentFeatures.isEmpty() || idx >= currentFeatures.size()) {
			TuiDetailDialog.show(gui, "Feature", "No feature selected.");
			return true;
		}
		MBeanFeatureInfo feature = currentFeatures.get(idx);
		if(feature == null) {
			TuiDetailDialog.show(gui, "Feature", "No feature information is available for this row.");
			return true;
		}
		if(objectName == null || objectName.length() == 0) {
			TuiDetailDialog.show(gui, "Feature", "No MBean is selected. Choose an MBean in the MBeans pane first.");
			return true;
		}
		String detailText = fetchFeatureDetailText(objectName, feature);
		if(detailText == null || detailText.trim().isEmpty()) {
			detailText = "No information available for feature:\n" + feature.getName();
		}
		TuiDetailDialog.show(gui, "Feature", detailText);
		return true;
	}

	private String fetchFeatureDetailText(String objectName, MBeanFeatureInfo feature) {
		final StringBuilder buf = new StringBuilder();
		final Logger.OutputSink captureSink = new Logger.OutputSink() {
			@Override
			public void print(Object msg, boolean newline) {
				if(msg != null) {
					buf.append(msg.toString());
				}
				if(newline) {
					buf.append(System.getProperty("line.separator", "\n"));
				}
			}
		};
		Logger.OutputSink prevSink = Logger.getOutputSink();
		boolean prevCapture = Logger.isTuiPanelCapture();
		try {
			Logger.setOutputSink(captureSink);
			Logger.setTuiPanelCapture(false);
			MBeanConnector connector = executor.getMBeanConnector();
			if(connector == null || connector.getMBeanConnection() == null) {
				buf.append("Not connected.");
				return buf.toString();
			}
			JMXPrintUtil.printMBeanFeatureDetail(connector.getMBeanConnection(), new ObjectName(objectName), feature);
		} catch(Exception e) {
			if(buf.length() == 0) {
				buf.append("Failed to load feature detail: ").append(e.getMessage());
			}
		} finally {
			Logger.setTuiPanelCapture(prevCapture);
			Logger.setOutputSink(prevSink);
		}
		return buf.toString();
	}

	private String resolveSelectedMbeanObjectName(JmxTuiModeContext ctx) {
		List<String> displayedMbeanNames = ctx.getDisplayedMbeanNames();
		if(displayedMbeanNames.isEmpty()) {
			return null;
		}
		FocusActionListBox mbeanList = ctx.getMbeanList();
		int idx = getSelectedIndexSafe(mbeanList);
		if(idx < 0) {
			idx = 0;
		}
		if(idx >= displayedMbeanNames.size()) {
			idx = displayedMbeanNames.size() - 1;
		}
		String name = displayedMbeanNames.get(idx);
		if(name != null && name.startsWith("(")) {
			return null;
		}
		return name;
	}

	/**
	 * Features pane Enter on an attribute: read-only → view; write-only → value dialog then set;
	 * read+write → choose read or write first.
	 */
	private void invokeAttributeFromFeatures(String objectName, MBeanAttributeInfo attribute) {
		if(objectName == null || objectName.length() == 0 || attribute == null) {
			return;
		}
		boolean readable = attribute.isReadable();
		boolean writable = attribute.isWritable();
		if(!readable && !writable) {
			TuiDialogs.showMessageDialog(
				gui,
				"Attribute",
				"Attribute \"" + attribute.getName() + "\" is not readable or writable.",
				MessageDialogButton.OK
			);
			return;
		}
		if(readable && writable) {
			String choice = TuiDialogs.showListSelectDialog(
				gui,
				"Attribute",
				"Attribute \"" + attribute.getName() + "\" (" + attribute.getType() + ")\nChoose action:",
				ATTR_ACTION_READ,
				ATTR_ACTION_WRITE
			);
			if(choice == null) {
				return;
			}
			if(ATTR_ACTION_WRITE.equals(choice)) {
				invokeAttributeWrite(objectName, attribute);
			} else if(ATTR_ACTION_READ.equals(choice)) {
				invokeAttributeRead(objectName, attribute);
			}
			return;
		}
		if(readable) {
			invokeAttributeRead(objectName, attribute);
		} else {
			invokeAttributeWrite(objectName, attribute);
		}
	}

	private void invokeAttributeRead(String objectName, MBeanAttributeInfo attribute) {
		executeCommandFromListSelection("CALL \"" + objectName + "\" " + attribute.getName());
	}

	private void invokeAttributeWrite(String objectName, MBeanAttributeInfo attribute) {
		String value = TuiDialogs.showTextInputDialog(
			gui,
			"Attribute value",
			"Input new value for " + attribute.getName() + " (" + attribute.getType() + "):",
			""
		);
		if(value == null) {
			return;
		}
		String validationError = TuiJmxInputValidator.validateAttributeValue(attribute, value);
		if(validationError != null) {
			TuiDialogs.showMessageDialog(gui, "Validation", validationError, MessageDialogButton.OK);
			return;
		}
		String cmd = "CALL \"" + objectName + "\" " + attribute.getName() + " \"" + value + "\"";
		executeCommandFromListSelection(cmd);
	}

	private void refreshFeatures(final JmxTuiModeContext ctx, String objectName) {
		FocusActionListBox featureList = ctx.getFeatureList();
		List<MBeanFeatureInfo> currentFeatures = ctx.getCurrentFeatures();
		featureList.clearItems();
		currentFeatures.clear();
		ctx.setFeaturesForObjectName(objectName);
		try {
			List<MBeanFeatureInfo> features = JMXUtil.getAttributeAndOperationList(executor.getMBeanConnector().getMBeanConnection(), new ObjectName(objectName), null);
			final List<MBeanFeatureInfo> ordered = new ArrayList<MBeanFeatureInfo>(features);

			for(final MBeanFeatureInfo f : ordered) {
				if(f instanceof MBeanAttributeInfo) {
					final MBeanAttributeInfo a = (MBeanAttributeInfo)f;
					currentFeatures.add(a);
					String perm = (a.isReadable() ? "R" : "-") + (a.isWritable() ? "W" : "-");
					String label = "Attribute(" + perm + ") " + a.getName() + " : " + a.getType();
					featureList.addItem(label, new Runnable() {
						@Override
						public void run() {
							invokeAttributeFromFeatures(objectName, a);
						}
					});
				} else if(f instanceof MBeanOperationInfo) {
					final MBeanOperationInfo op = (MBeanOperationInfo)f;
					currentFeatures.add(op);
					String label = "Operation " + op.getName() + "(" + op.getSignature().length + ") : " + op.getReturnType();
					featureList.addItem(label, new Runnable() {
						@Override
						public void run() {
							String cmd = null;
							String opTarget = JMXHelper.toOperationCallTargetName(op);
							MBeanParameterInfo[] signature = op.getSignature();
							if(signature != null && signature.length > 0) {
								String[] argValues = TuiOperationArgumentsDialog.show(gui, op.getName(), signature);
								if(argValues == null) {
									return;
								}
								String args = TuiOperationArgumentsDialog.joinOperationArguments(signature, argValues);
								cmd = "CALL \"" + objectName + "\" " + opTarget + " " + args;
							} else {
								cmd = "CALL \"" + objectName + "\" " + opTarget;
							}
							if(cmd != null && cmd.trim().length() > 0) {
								executeCommandFromListSelection(cmd);
							}
						}
					});
				}
			}
		} catch(Exception e) {
			JmxConnectionRecovery.handleTuiDisconnect(executor.getMBeanConnector(), e, false);
			if(!Logger.isTuiActive() && attemptOnlineJmxReconnect(executor.getMBeanConnector(), e)) {
				refreshFeatures(ctx, objectName);
				return;
			}
			if(JmxTuiConnectionLossHandler.isExitScheduled()) {
				return;
			}
			currentFeatures.clear();
			featureList.addItem("(failed to load features)", new Runnable() {
				@Override
				public void run() {}
			});
			Logger.logln(Logger.Level.ERROR, "Failed to load feature list: " + e.getMessage());
		}
	}

}
