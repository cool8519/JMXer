package dal.tool.trace.jmxer.tui.app;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.lang.management.ThreadInfo;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
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
import dal.tool.cli.util.IOUtil;
import dal.tool.util.FileUtil;
import dal.tool.trace.jmxer.JMXControl;
import dal.tool.trace.jmxer.JMXerConstant;
import dal.tool.trace.jmxer.tui.support.TuiStatusSessionLine;
import dal.tool.trace.jmxer.tui.support.TuiTargetVmStatusCache;
import dal.tool.trace.jmxer.cli.command.JmxCommand;
import dal.tool.trace.jmxer.cli.command.JmxRecordCommand;
import dal.tool.trace.jmxer.cli.command.JmxThreadCommand;
import dal.tool.trace.jmxer.cli.data.RecordResult;
import dal.tool.trace.jmxer.cli.data.RecordThreadInfo;
import dal.tool.trace.jmxer.cli.data.ResourceUsage;
import dal.tool.trace.jmxer.cli.helper.AbstractRecordThread;
import dal.tool.trace.jmxer.cli.helper.RecordDumpThreadDetail;
import dal.tool.trace.jmxer.cli.helper.StackRecordThread;
import dal.tool.cli.ScriptUiHooks;
import dal.tool.trace.jmxer.cli.JmxCommandExecutor;
import dal.tool.trace.jmxer.cli.JmxConnectionRecovery;
import dal.tool.trace.jmxer.cli.JmxSettings;
import dal.tool.trace.jmxer.cli.JmxSettings.StacktraceTarget;
import dal.tool.trace.jmxer.cli.JmxTuiConnectionLossHandler;
import dal.tool.trace.jmxer.knowledge.KnowledgeConstant;
import dal.tool.trace.jmxer.knowledge.MBeanKnowledgeResolver;
import dal.tool.trace.jmxer.tui.support.BoxFocusManager;
import dal.tool.trace.jmxer.tui.support.JmxTuiKeys;
import dal.tool.trace.jmxer.tui.support.JmxTuiTheme;
import dal.tool.trace.jmxer.tui.support.TuiJmxInputValidator;
import dal.tool.trace.jmxer.tui.support.TuiCommandStdin;
import dal.tool.trace.jmxer.tui.support.TuiDialogLineReader;
import dal.tool.trace.jmxer.tui.support.TuiGlobalKeysHint;
import dal.tool.trace.jmxer.tui.support.TuiHelpSectionBuilder;
import dal.tool.trace.jmxer.tui.support.TuiKeyAction;
import dal.tool.trace.jmxer.tui.support.TuiKeyBindings;
import dal.tool.trace.jmxer.tui.support.TuiKeyRemapCatalog;
import dal.tool.trace.jmxer.tui.support.TuiKeyRemapUiKeys;
import dal.tool.trace.jmxer.tui.support.TuiGuiThread;
import dal.tool.trace.jmxer.tui.support.RecordProgressKind;
import dal.tool.trace.jmxer.tui.support.TuiQueuedLineReader;
import dal.tool.trace.jmxer.tui.support.TuiRecordOutputSummary;
import dal.tool.trace.jmxer.tui.support.TuiOutputSavePath;
import dal.tool.trace.jmxer.tui.support.TuiOutputUiKeys;
import dal.tool.trace.jmxer.tui.support.TuiPaneKeysHint;
import dal.tool.trace.jmxer.tui.support.TuiRecordUiKeys;
import dal.tool.trace.jmxer.tui.support.TuiThreadUiKeys;
import dal.tool.trace.jmxer.tui.support.ThreadAutoRefreshScheduler;
import dal.tool.trace.jmxer.tui.dialog.TuiThreadSortDialog;
import dal.tool.trace.jmxer.tui.support.ThreadPaneSortSettings;
import dal.tool.trace.jmxer.tui.support.ThreadPaneSorter;
import dal.tool.trace.jmxer.tui.support.RecordDumpDialogPath;
import dal.tool.trace.jmxer.tui.support.ThreadPaneTableFormat;
import dal.tool.trace.jmxer.tui.support.TuiStatusSegmentLabel;
import dal.tool.trace.jmxer.tui.support.TuiStatusText;
import dal.tool.trace.jmxer.tui.dialog.TuiConnectionLossDialog;
import dal.tool.trace.jmxer.tui.dialog.TuiDetailDialog;
import dal.tool.trace.jmxer.tui.dialog.TuiKeyRemapDialog;
import dal.tool.trace.jmxer.tui.dialog.TuiDialogs;
import dal.tool.trace.jmxer.tui.dialog.TuiOperationArgumentsDialog;
import dal.tool.trace.jmxer.tui.dialog.TuiRecordDumpFileDialog;
import dal.tool.trace.jmxer.tui.dialog.TuiRecordProgressDialog;
import dal.tool.trace.jmxer.tui.dialog.TuiRecordResourceDurationDialog;
import dal.tool.trace.jmxer.tui.dialog.TuiRecordSampleTypeDialog;
import dal.tool.trace.jmxer.tui.dialog.TuiRecordSamplingDialog;
import dal.tool.trace.jmxer.tui.dialog.TuiRecordViewDialog;
import dal.tool.trace.jmxer.tui.dialog.TuiRequestEntryFileDialog;
import dal.tool.trace.jmxer.tui.dialog.TuiThreadRefreshDialog;
import dal.tool.trace.jmxer.tui.mode.RecordUiMode;
import dal.tool.trace.jmxer.tui.mode.JmxTuiModeContext;
import dal.tool.trace.jmxer.tui.mode.JmxTuiModeContext.RecordViewState;
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

/**
 * JMX TUI 메인 애플리케이션. 패널 레이아웃·포커스·명령 실행·모드 전환을 담당한다.
 *
 */
public class JmxTuiApp implements JmxTuiOutputHost {
	private static final int HEADER_INNER_LINES = 5;
	private static final int MIN_LIST_ROWS = 6;
	private static final int MAX_LIST_ROWS = 18;
	private static final int BORDER_V_OVERHEAD = 2;
	private static final int WINDOW_DECORATION_ROWS = 1;
	private static final DateTimeFormatter OUTPUT_PANE_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
	private static final String ATTR_ACTION_READ = "Read";
	private static final String ATTR_ACTION_WRITE = "Write";
	private static final int MIN_FEATURES_COLS = 20;
	private static final int MIN_MBEANS_COLS = 28;
	private static final int HORIZONTAL_SPLIT_STEP_COLS = 2;
	private static final int VERTICAL_SPLIT_STEP_ROWS = 1;
	private static final int MIN_OUTPUT_OUTER_ROWS = 3;
	private static final String THREADING_JMX_NAME = "java.lang:type=Threading";
	private static final String COMMAND_STDIN_PROMPT_TEXT = "input)";

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
	private final Label headerKeysGlobalLine2;
	private final Label headerKeysPane;
	private final Label headerKeysPaneLine2;

	private final Panel commandInner;
	private final CommandInputBox input;
	private final Panel inputRow;
	private final Label commandPromptLabel;
	private final String defaultCommandPromptText;
	private final TuiCommandStdin commandStdin;
	private final ExecutorService commandWorker;

	private final JmxTuiOutputSink outputSink;
	private final BoxFocusManager focusManager;
	private final TuiKeyBindings keyBindings;
	private volatile boolean suppressActivePaneUpdates;

	private JmxTuiUiMode currentMode;
	private JmxTuiModeContext activeContext;

	private volatile List<String> currentMbeanNames = new ArrayList<String>();
	private volatile TerminalSize lastTerminalSize = null;
	private volatile int statusInnerCols = 10;

	private int historyCursor = -1;
	private String historyScratch = "";
	private volatile boolean connectionLossDialogQueued;
	private volatile TuiRecordProgressDialog recordProgressDialog;
	private final ThreadAutoRefreshScheduler threadRefreshScheduler;
	private final TuiTargetVmStatusCache targetVmStatusCache = new TuiTargetVmStatusCache();
	private StacktraceTarget lastAppliedStacktraceTargetForSelection = StacktraceTarget.SELECTED;
	private volatile boolean commandPaneCommandRunning;
	private volatile boolean awaitingCommandStdin;

	public JmxTuiApp(WindowBasedTextGUI gui, JmxCommandExecutor executor, String prompt) {
		this(gui, executor, prompt, null);
	}

	public JmxTuiApp(WindowBasedTextGUI gui, JmxCommandExecutor executor, String prompt, Runnable closeAction) {
		this.gui = gui;
		this.executor = executor;
		this.prompt = prompt;
		this.closeAction = closeAction;

		this.focusManager = new BoxFocusManager();
		this.keyBindings = new TuiKeyBindings();
		TuiKeyBindings.setCurrent(this.keyBindings);
		this.keyBindings.setChangeListener(new Runnable() {
			@Override
			public void run() {
				gui.getGUIThread().invokeLater(new Runnable() {
					@Override
					public void run() {
						refreshHeader();
					}
				});
			}
		});

		this.titleLabel = new Label("");
		this.header1 = new Label("");
		this.header2 = new TuiStatusSegmentLabel();
		this.header3 = new Label("");
		this.headerKeysGlobal = new Label("");
		this.headerKeysGlobalLine2 = new Label("");
		this.headerKeysPane = new Label("");
		this.headerKeysPaneLine2 = new Label("");

		this.commandInner = new Panel(new LinearLayout(Direction.VERTICAL));
		this.input = new CommandInputBox(focusManager, commandInner, new TerminalSize(1, 1));
		this.input.setCaretWarp(true);
		this.input.setVerticalFocusSwitching(false);
		this.input.setHorizontalFocusSwitching(false);
		this.input.setTheme(JmxTuiTheme.UNFOCUSED_TEXTBOX_THEME);

		this.defaultCommandPromptText = (prompt == null ? "" : prompt) + "> ";
		this.commandPromptLabel = new Label(defaultCommandPromptText);
		this.commandStdin = new TuiCommandStdin();
		this.commandStdin.setAwaitingListener(new TuiCommandStdin.AwaitingListener() {
			@Override
			public void onAwaitingInput(final boolean awaiting) {
				gui.getGUIThread().invokeLater(new Runnable() {
					@Override
					public void run() {
						awaitingCommandStdin = awaiting;
						updateCommandPromptForStdinState();
						if(ScriptUiHooks.isSessionActive()) {
							if(awaiting) {
								ScriptUiHooks.suspendForUserInput();
								input.takeFocus();
							} else {
								ScriptUiHooks.resumeAfterUserInput();
							}
						}
					}
				});
			}
		});
		this.commandWorker = Executors.newSingleThreadExecutor(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, "JmxTui-CommandWorker");
				t.setDaemon(true);
				return t;
			}
		});

		this.inputRow = new Panel(new LinearLayout(Direction.HORIZONTAL));
		this.inputRow.addComponent(commandPromptLabel);
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
		if(JMXControl.isOfflineSession) {
			JmxTuiModeContext recordCtx = RecordUiMode.getInstance().getOrCreateContext(this);
			recordCtx.setRecordViewState(RecordViewState.DUMP);
			setMode(RecordUiMode.getInstance());
		} else {
			setMode(MBeanUiMode.getInstance());
			refreshMBeans();
		}
		refreshHeader();
	}

	/**
	 * Offline DUMPFILE TUI 부트스트랩 후 덤프 데이터를 Record Dump View에 반영한다.
	 */
	public void finishOfflineSessionBootstrap() {
		if(!JMXControl.isOfflineSession) {
			return;
		}
		JmxTuiModeContext recordCtx = RecordUiMode.getInstance().getOrCreateContext(this);
		recordCtx.setRecordViewState(RecordViewState.DUMP);
		if(currentMode == null || !RecordUiMode.ID.equals(currentMode.getId())) {
			setMode(RecordUiMode.getInstance());
		} else {
			refreshRecordView(recordCtx);
		}
		refreshHeader();
	}

	public JmxTuiModeContext createModeContext(JmxTuiUiMode mode) {
		return new JmxTuiModeContext(gui, focusManager, this, mode);
	}

	public void wireModeContext(final JmxTuiModeContext ctx) {
		if(ctx.hasThreadPane()) {
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
		ctx.getMbeanList().setActivateKeyAction(TuiKeyAction.MBEAN_SET_DEFAULT);
		ctx.getFeatureList().setActivateKeyAction(TuiKeyAction.FEATURE_INVOKE);
	}

	private JmxTuiModeContext ctx() {
		return activeContext;
	}

	/**
	 * 레이아웃에서 모드 패널을 떼기 전에 Command로 포커스를 옮긴다.
	 * Lanterna는 조상 패널 제거만으로는 포커스를 비우지 않는다.
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
		JmxTuiModeContext c = activeContext;
		if(c != null) {
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
		}
		if(input.isFocused()) {
			return JmxTuiModeFocusPane.COMMAND;
		}
		JmxTuiModeFocusPane fromActivePane = resolveFocusPaneFromActivePanel(focusManager.getActivePane());
		if(fromActivePane != null) {
			return fromActivePane;
		}
		if(c == null) {
			return JmxTuiModeFocusPane.COMMAND;
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

	public void requestExitWithConfirm() {
		requestExitWithConfirm(true);
	}

	/**
	 * 종료 확인 후 닫는다.
	 * @param runExitCommand true이면 EXIT 명령을 먼저 실행한다(Ctrl+C 등). 이미 exit를 실행한 경우 false
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
		} catch(Exception e) {}
		if(runExitCommand) {
			try {
				processCommandForTuiWithDialogReader("EXIT");
			} catch(Exception e) {}
		}
		runCloseAction();
	}

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
			processCommandForTuiWithDialogReader("EXIT");
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

	private void refreshAfterReconnect() throws Exception {
		JmxCommand.clearMbeanInfoCache();
		reloadObjectNamesFromServer();

		JmxTuiModeContext mbeanCtx = MBeanUiMode.getInstance().getOrCreateContext(this);
		clearFeatures(mbeanCtx);
		applyMbeanSearchFilter(mbeanCtx);

		JmxTuiModeContext recordCtx = RecordUiMode.getInstance().getOrCreateContext(this);
		refreshRecordView(recordCtx);

		JmxTuiModeContext threadCtx = ThreadUiMode.getInstance().getOrCreateContext(this);
		threadCtx.clearCpuSnapshot();
		refreshThreads(threadCtx, false, true);

		reapplyCurrentMbeanObjectAfterReconnect();
		targetVmStatusCache.refresh(executor.getMBeanConnector());
		refreshHeader();
		invalidatePanesAfterReconnect();
	}

	private void reapplyCurrentMbeanObjectAfterReconnect() {
		String objectName = null;
		JmxTuiModeContext ctx = activeContext;
		if(ctx != null && !ctx.hasThreadPane()) {
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
			processCommandForTuiWithDialogReader("OBJECT \"" + objectName + "\"");
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
	 * 전역 단축키를 처리했다면 true. true이면 포커스된 패널로 키를 넘기지 않는다.
	 */
	public boolean consumeGlobalShortcut(KeyStroke key) {
		if(JmxTuiKeys.isCtrlSuspend(key)) {
			return true;
		}
		if(JmxTuiKeys.isKeyRemapDialogKey(key)) {
			showKeyRemapDialog();
			return true;
		}
		if(JmxTuiKeys.isInfoKey(key)) {
			if(dispatchInfoToFocusedPane()) {
				return true;
			}
		}
		if(JmxTuiKeys.isShiftRecordViewMethod(key) || JmxTuiKeys.isShiftRecordViewThread(key)
				|| JmxTuiKeys.isShiftRecordViewStack(key) || JmxTuiKeys.isShiftRecordViewSearch(key)) {
			if(currentMode != null && RecordUiMode.ID.equals(currentMode.getId())) {
				JmxTuiModeContext recordCtx = ctx();
				if(recordCtx != null && recordCtx.getRecordViewState() == RecordViewState.DUMP
						&& !recordCtx.isRecordSamplingInProgress()) {
					if(JmxTuiKeys.isShiftRecordViewMethod(key)) {
						showRecordViewFromUi(recordCtx, TuiRecordViewDialog.ViewType.METHOD);
					} else if(JmxTuiKeys.isShiftRecordViewThread(key)) {
						showRecordViewFromUi(recordCtx, TuiRecordViewDialog.ViewType.THREAD);
					} else if(JmxTuiKeys.isShiftRecordViewStack(key)) {
						showRecordViewFromUi(recordCtx, TuiRecordViewDialog.ViewType.STACK);
					} else {
						showRecordViewFromUi(recordCtx, TuiRecordViewDialog.ViewType.SEARCH);
					}
				}
				return true;
			}
			return false;
		}
		if(JmxTuiKeys.isF3Key(key)) {
			cycleUiMode();
			return true;
		}
		if(JmxTuiKeys.isF4Key(key)) {
			if(currentMode != null && RecordUiMode.ID.equals(currentMode.getId())) {
				toggleRecordViewState(ctx());
				return true;
			}
			return false;
		}
		if(JmxTuiKeys.isF7Key(key)) {
			if(currentMode != null && RecordUiMode.ID.equals(currentMode.getId())) {
				JmxTuiModeContext recordCtx = ctx();
				if(recordCtx != null && recordCtx.getRecordViewState() == RecordViewState.DUMP
						&& !recordCtx.isRecordSamplingInProgress()) {
					loadRecordDumpFromUi(recordCtx);
					return true;
				}
			}
			return false;
		}
		if(JmxTuiKeys.isF8Key(key)) {
			if(currentMode != null && RecordUiMode.ID.equals(currentMode.getId())) {
				JmxTuiModeContext recordCtx = ctx();
				if(recordCtx != null && recordCtx.getRecordViewState() == RecordViewState.DUMP
						&& !recordCtx.isRecordSamplingInProgress()) {
					saveRecordDumpFromUi(recordCtx);
					return true;
				}
			}
			return false;
		}
		if(JmxTuiKeys.isF9Key(key)) {
			if(currentMode != null && RecordUiMode.ID.equals(currentMode.getId())) {
				JmxTuiModeContext recordCtx = ctx();
				if(recordCtx != null && recordCtx.getRecordViewState() == RecordViewState.DUMP
						&& !recordCtx.isRecordSamplingInProgress()) {
					showRecordDumpInfoInOutput(recordCtx);
					return true;
				}
			}
			return false;
		}
		if(JmxTuiKeys.isCtrlEditThreadSelection(key)) {
			if(currentMode != null && ThreadUiMode.ID.equals(currentMode.getId())) {
				if(isStacktraceTargetActive()) {
					return true;
				}
				showEditThreadSelectionDialog(ctx());
				return true;
			}
			if(currentMode != null && RecordUiMode.ID.equals(currentMode.getId())) {
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
		if(JmxTuiKeys.isCtrlEditRequestEntryFile(key)) {
			if(currentMode != null && ThreadUiMode.ID.equals(currentMode.getId())) {
				showRequestEntryFileDialog();
				return true;
			}
			return false;
		}
		if(JmxTuiKeys.isCtrlToggleThreadRefresh(key)) {
			if(currentMode != null && RecordUiMode.ID.equals(currentMode.getId())) {
				JmxTuiModeContext recordCtx = ctx();
				if(recordCtx != null && recordCtx.getRecordViewState() == RecordViewState.LIVE
						&& !recordCtx.isRecordSamplingInProgress()) {
					toggleThreadRefreshPolicy(recordCtx);
				}
				return true;
			}
			if(currentMode != null && ThreadUiMode.ID.equals(currentMode.getId())) {
				toggleThreadRefreshPolicy(ctx());
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
		if(JmxTuiKeys.isCtrlFeaturesToggle(key)) {
			if(toggleDumpStackFilterSetting()) {
				return true;
			}
		}
		if(JmxTuiKeys.isCtrlToggleShowEmptyThread(key)) {
			if(toggleShowEmptyThreadSetting()) {
				return true;
			}
		}
		if(JmxTuiKeys.isPaneResizeKey(key)) {
			return handlePaneResizeKey(key);
		}
		if(!JmxTuiKeys.isGlobalShortcut(key)) {
			return false;
		}
		if(JmxTuiKeys.isF1HelpKey(key)) {
			showHelp();
			return true;
		}
		if(JmxTuiKeys.isExitKey(key)) {
			requestExitWithConfirm();
			return true;
		}
		if(JmxTuiKeys.isCtrlFeaturesToggle(key)) {
			toggleFeaturesPane();
			return true;
		}
		return false;
	}

	private boolean dispatchInfoToFocusedPane() {
		JmxTuiModeContext c = ctx();
		if(c == null) {
			return false;
		}
		if(c.getMbeanList().isFocused()) {
			if(currentMode != null && c.hasThreadPane()) {
				if(c.isRecordMode() && c.getRecordViewState() == RecordViewState.DUMP) {
					return showRecordDumpThreadDetail(c);
				}
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
		// refreshThreadListView가 refreshHeader를 호출해 재진입하므로 먼저 적용한다.
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

	private boolean toggleDumpStackFilterSetting() {
		if(!isRecordDumpView()) {
			return false;
		}
		JmxSettings settings = executor.getSettings();
		if(settings == null) {
			return false;
		}
		settings.toggleDumpStackFilter();
		refreshHeader();
		return true;
	}

	private boolean toggleShowEmptyThreadSetting() {
		if(!isRecordDumpView()) {
			return false;
		}
		JmxSettings settings = executor.getSettings();
		if(settings == null) {
			return false;
		}
		settings.toggleShowEmptyThreadInRecordView();
		refreshHeader();
		return true;
	}

	private boolean isRecordDumpView() {
		if(currentMode == null || !RecordUiMode.ID.equals(currentMode.getId())) {
			return false;
		}
		JmxTuiModeContext ctx = ctx();
		return ctx != null && ctx.getRecordViewState() == RecordViewState.DUMP;
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
	 * Shift+화살표로 Status와 Command 사이 패널 크기를 조절한다.
	 * Features 숨김 등 분할이 없는 레이아웃에서는 무시한다.
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
		int statusOuterH = resolveStatusInnerLineCount() + BORDER_V_OVERHEAD;
		int cmdOuterH = 1 + BORDER_V_OVERHEAD;
		int available = contentRows - titleH - statusOuterH - cmdOuterH;
		if(available < MIN_LIST_ROWS + BORDER_V_OVERHEAD + MIN_OUTPUT_OUTER_ROWS + 3) {
			available = MIN_LIST_ROWS + BORDER_V_OVERHEAD + MIN_OUTPUT_OUTER_ROWS + 3;
		}
		return available;
	}

	/**
	 * bodyRoot에 배정되는 외곽 행 수.
	 * Thread/Record 모드는 Output을 lists band 안에 두므로 Command 위 빈 행이 생기지 않게 lists band 높이와 맞춘다.
	 */
	private int computeBodyRootOuterRows(TerminalSize size) {
		int termRows = Math.max(12, size.getRows());
		int titleH = 1;
		int statusOuterH = resolveStatusInnerLineCount() + BORDER_V_OVERHEAD;
		int cmdOuterH = 1 + BORDER_V_OVERHEAD;
		int rows = termRows - titleH - statusOuterH - cmdOuterH;
		int minRows = MIN_LIST_ROWS + BORDER_V_OVERHEAD + (1 + BORDER_V_OVERHEAD);
		return rows < minRows ? minRows : rows;
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

	public void refreshLiveScriptCaptureDisplay() {
		outputSink.refreshLiveCaptureDisplay();
	}

	public TuiCommandStdin getCommandStdin() {
		return commandStdin;
	}

	public void shutdownCommandWorker() {
		commandStdin.closeBridge();
		commandWorker.shutdownNow();
	}

	private void updateCommandPromptForStdinState() {
		if(awaitingCommandStdin) {
			commandPromptLabel.setText(COMMAND_STDIN_PROMPT_TEXT);
		} else {
			commandPromptLabel.setText(defaultCommandPromptText);
		}
		inputRow.invalidate();
	}

	private boolean processCommandForTuiWithDialogReader(String commandLine) throws Exception {
		return processCommandForTuiWithLineReader(commandLine, new TuiDialogLineReader(gui));
	}

	private boolean processCommandForTuiWithLineReader(String commandLine, IOUtil.LineReader lineReader) throws Exception {
		IOUtil.setTuiLineReader(lineReader);
		try {
			return executor.processCommandForTui(commandLine);
		} finally {
			IOUtil.clearTuiLineReader();
		}
	}

	private void runCommandFromCommandPane(final String line) {
		if(awaitingCommandStdin) {
			String text = input.getText();
			if(text == null) {
				text = "";
			}
			input.setText("");
			outputSink.appendStdinResponseToLastLine(text);
			commandStdin.feedLine(text);
			return;
		}
		if(commandPaneCommandRunning) {
			return;
		}
		historyCursor = -1;
		historyScratch = "";
		input.setText("");
		if(isExitLikeCommand(line)) {
			requestExitWithConfirm(true);
			return;
		}
		commandPaneCommandRunning = true;
		ensureOutputPaneVisible();
		outputSink.beginUserCaptureSession(line);
		commandWorker.execute(new Runnable() {
			@Override
			public void run() {
				boolean stopRequested = false;
				try {
					stopRequested = executor.processCommandForTui(line);
				} catch(Exception e) {
					Logger.logStackTrace(e);
					Logger.logln(Logger.Level.ERROR, "Failed to run command: " + e.getMessage());
				}
				final boolean stop = stopRequested;
				gui.getGUIThread().invokeLater(new Runnable() {
					@Override
					public void run() {
						outputSink.endUserCaptureSession();
						commandPaneCommandRunning = false;
						awaitingCommandStdin = false;
						updateCommandPromptForStdinState();
						if(JmxTuiConnectionLossHandler.isExitScheduled()) {
							return;
						}
						refreshHeader();
						refreshMBeansIfNeeded(line);
						if(stop) {
							requestExitWithConfirm(false);
						}
					}
				});
			}
		});
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
				TuiGuiThread.callOnGuiThread(gui, new TuiGuiThread.GuiCallable<Void>() {
					@Override
					public Void call() {
						clearOutput();
						outputSink.commitUserCapturePreamble();
						return null;
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
		statusInner.addComponent(headerKeysGlobalLine2);
		statusInner.addComponent(headerKeysPane);
		statusInner.addComponent(headerKeysPaneLine2);
		main.addComponent(statusInner.withBorder(Borders.singleLine("Status")));

		main.addComponent(bodyRoot, LinearLayout.createLayoutData(Alignment.Fill, GrowPolicy.CanGrow));

		main.addComponent(commandInner.withBorder(Borders.singleLine("Command")));

		return main;
	}

	private void setMode(JmxTuiUiMode mode) {
		if(mode == null || mode == currentMode) {
			return;
		}
		if(JMXControl.isOfflineSession && !RecordUiMode.ID.equals(mode.getId())) {
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
			} else if(RecordUiMode.ID.equals(mode.getId())) {
				refreshRecordView(activeContext);
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
			restoreCurrentModeFocus();
			refreshHeader();
		} finally {
			suppressActivePaneUpdates = prevSuppress;
		}
	}

	private void cycleUiMode() {
		if(JMXControl.isOfflineSession) {
			return;
		}
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
		refreshHeader();
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
		int statusOuterH = resolveStatusInnerLineCount() + BORDER_V_OVERHEAD;
		int cmdOuterH = 1 + BORDER_V_OVERHEAD;
		int mbeanSearchOuterH = 1 + BORDER_V_OVERHEAD;

		int termRows = Math.max(12, size.getRows());
		int contentRows = Math.max(8, termRows - WINDOW_DECORATION_ROWS);
		int availableBodyOuterH = contentRows - titleH - statusOuterH - cmdOuterH;
		if(availableBodyOuterH < MIN_LIST_ROWS + BORDER_V_OVERHEAD + mbeanSearchOuterH) {
			availableBodyOuterH = MIN_LIST_ROWS + BORDER_V_OVERHEAD + mbeanSearchOuterH;
		}

		int listsBandOuterH = availableBodyOuterH;
		if(m != null && m.isOutputInRightColumn()) {
			listsBandOuterH = computeBodyRootOuterRows(size);
		}
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

		if(c.hasThreadPane()) {
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
		headerKeysGlobalLine2.setLabelWidth(cols - BORDER_V_OVERHEAD);
		headerKeysPane.setLabelWidth(cols - BORDER_V_OVERHEAD);
		headerKeysPaneLine2.setLabelWidth(cols - BORDER_V_OVERHEAD);
		statusInner.setPreferredSize(new TerminalSize(cols - BORDER_V_OVERHEAD, resolveStatusInnerLineCount()));
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
	 * Lanterna는 테두리 제목을 바꿀 수 없으므로 동일 inner 패널로 테두리 컴포넌트를 교체한다.
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
			if(!hasSaveableOutput(lines)) {
				TuiDialogs.showMessageDialog(
					gui,
					TuiOutputUiKeys.NO_OUTPUT_TITLE,
					TuiOutputUiKeys.NO_OUTPUT_MESSAGE,
					MessageDialogButton.OK
				);
				return;
			}

			String pathInput = TuiRecordDumpFileDialog.show(
				gui,
				TuiOutputUiKeys.SAVE_DIALOG_TITLE,
				TuiOutputUiKeys.SAVE_DIALOG_DESCRIPTION,
				TuiOutputSavePath.getInitialPath()
			);
			if(pathInput == null) {
				return;
			}
			TuiOutputSavePath.rememberEnteredPath(pathInput);

			File outFile;
			try {
				outFile = TuiOutputSavePath.resolveSaveFile(pathInput);
			} catch(IOException e) {
				TuiDialogs.showMessageDialog(
					gui,
					TuiOutputUiKeys.SAVE_FAILED_TITLE,
					String.format(TuiOutputUiKeys.SAVE_FAILED_MESSAGE_TEMPLATE, e.getMessage()),
					MessageDialogButton.OK
				);
				return;
			}
			if(outFile.exists()) {
				MessageDialogButton overwrite = TuiDialogs.showMessageDialog(
					gui,
					TuiOutputUiKeys.OVERWRITE_CONFIRM_TITLE,
					String.format(TuiOutputUiKeys.OVERWRITE_CONFIRM_MESSAGE_TEMPLATE, outFile.getAbsolutePath()),
					MessageDialogButton.Yes,
					MessageDialogButton.No
				);
				if(overwrite != MessageDialogButton.Yes) {
					return;
				}
			}

			FileUtil.createFileDirectory(outFile);
			Files.write(outFile.toPath(), buildOutputSaveContent(lines).getBytes(StandardCharsets.UTF_8));

			TuiDialogs.showMessageDialog(
				gui,
				TuiOutputUiKeys.SAVE_SUCCESS_TITLE,
				String.format(TuiOutputUiKeys.SAVE_SUCCESS_MESSAGE_TEMPLATE, outFile.getAbsolutePath()),
				MessageDialogButton.OK
			);
		} catch(Exception e) {
			TuiDialogs.showMessageDialog(
				gui,
				TuiOutputUiKeys.SAVE_FAILED_TITLE,
				String.format(TuiOutputUiKeys.SAVE_FAILED_MESSAGE_TEMPLATE, e.getMessage()),
				MessageDialogButton.OK
			);
		}
	}

	private static boolean hasSaveableOutput(List<String> lines) {
		if(lines == null || lines.isEmpty()) {
			return false;
		}
		if(lines.size() == 1) {
			String only = lines.get(0);
			if(only == null || only.trim().isEmpty()
					|| JmxTuiOutputSink.EMPTY_OUTPUT_PLACEHOLDER.equals(only.trim())) {
				return false;
			}
		}
		return true;
	}

	private static String buildOutputSaveContent(List<String> lines) {
		String nl = System.getProperty("line.separator", "\n");
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < lines.size(); i++) {
			if(i > 0) {
				sb.append(nl);
			}
			String line = lines.get(i);
			sb.append(line == null ? "" : OutputTextFormat.expandTabs(line));
		}
		return sb.toString();
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
					if(awaitingCommandStdin) {
						runCommandFromCommandPane("");
						return true;
					}
					if(commandPaneCommandRunning) {
						return true;
					}
					String line = input.getText();
					if(line != null) {
						line = line.trim();
					}
					if(line == null || line.length() == 0) {
						return true;
					}
					runCommandFromCommandPane(line);
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
					if(ctx.hasThreadPane()) {
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
				if(handleThreadCtrlShiftEnterKey(ctx, key)) {
					return true;
				}
				if(handleRecordSampleSelectedKey(ctx, key)) {
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
		ctx.getMbeanList().setActivateKeyAction(TuiKeyAction.THREAD_PRIMARY_ACTION);
		ctx.getMbeanList().setKeyHandler(new ListKeyHandler() {
			@Override
			public boolean handle(KeyStroke key) {
				return handleThreadListKey(ctx, key);
			}
		});
	}

	private boolean handleThreadCtrlShiftEnterKey(JmxTuiModeContext ctx, KeyStroke key) {
		if(key == null || ctx == null || !ctx.hasThreadPane() || !JmxTuiKeys.isThreadBulkStacktraceKey(key)) {
			return false;
		}
		if(ctx.isRecordMode()) {
			if(ctx.isRecordSamplingInProgress()) {
				return true;
			}
			if(ctx.getRecordViewState() == RecordViewState.LIVE) {
				showSelectedThreadsStackTraceInOutput(ctx);
			} else {
				showRecordDumpSelectedThreadsAnalysisInOutput(ctx);
			}
			return true;
		}
		if(ctx.isThreadMode()) {
			showSelectedThreadsStackTraceInOutput(ctx);
			return true;
		}
		return false;
	}

	private boolean handleThreadBulkSelectionKeys(JmxTuiModeContext ctx, KeyStroke key) {
		if(key == null || ctx == null || !ctx.hasThreadPane()) {
			return false;
		}
		if(JmxTuiKeys.isThreadToggleAllKey(key)) {
			if(ctx.isThreadMode() && isStacktraceTargetActive()) {
				return true;
			}
			toggleAllDisplayedThreadsSelection(ctx);
			return true;
		}
		if(JmxTuiKeys.isThreadClearSelectedKey(key)) {
			if(ctx.isThreadMode() && isStacktraceTargetActive()) {
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
		if(key == null || ctx == null || !ctx.hasThreadPane()) {
			return false;
		}
		if(ctx.isRecordMode()) {
			return handleRecordThreadListKey(ctx, key);
		}
		if(JmxTuiKeys.isF5Key(key)) {
			forceRefreshThreads(ctx);
			return true;
		}
		if(JmxTuiKeys.isThreadSortKey(key)) {
			showThreadSortColumnDialog(ctx);
			return true;
		}
		if(handleThreadCtrlShiftEnterKey(ctx, key)) {
			return true;
		}
		if(handleThreadBulkSelectionKeys(ctx, key)) {
			return true;
		}
		if(JmxTuiKeys.isThreadPrimaryActionKey(key)) {
			showCursorThreadStackTraceInOutput(ctx);
			return true;
		}
		if(key.getKeyType() == KeyType.Enter) {
			return true;
		}
		if(JmxTuiKeys.isThreadToggleCurrentKey(key)) {
			if(isStacktraceTargetActive()) {
				return true;
			}
			toggleCurrentThreadSelection(ctx);
			return true;
		}
		if(JmxTuiKeys.isPlainSpaceKey(key)) {
			return true;
		}
		return false;
	}

	private boolean handleRecordThreadListKey(JmxTuiModeContext ctx, KeyStroke key) {
		if(ctx.isRecordSamplingInProgress()) {
			return true;
		}
		if(JmxTuiKeys.isF5Key(key)) {
			if(ctx.getRecordViewState() == RecordViewState.LIVE) {
				forceRefreshThreads(ctx);
			}
			return true;
		}
		if(JmxTuiKeys.isThreadSortKey(key)) {
			showThreadSortColumnDialog(ctx);
			return true;
		}
		if(ctx.getRecordViewState() == RecordViewState.LIVE) {
			if(JmxTuiKeys.isRecordSampleKey(key)) {
				startRecordSampleCursorFromUi(ctx);
				return true;
			}
			if(handleRecordSampleSelectedKey(ctx, key)) {
				return true;
			}
		}
		if(handleThreadCtrlShiftEnterKey(ctx, key)) {
			return true;
		}
		if(handleThreadBulkSelectionKeys(ctx, key)) {
			return true;
		}
		if(JmxTuiKeys.isThreadPrimaryActionKey(key)) {
			if(ctx.getRecordViewState() == RecordViewState.LIVE) {
				showCursorThreadStackTraceInOutput(ctx);
			} else {
				showRecordDumpThreadAnalysisInOutput(ctx);
			}
			return true;
		}
		if(key.getKeyType() == KeyType.Enter) {
			return true;
		}
		if(JmxTuiKeys.isThreadToggleCurrentKey(key)) {
			toggleCurrentThreadSelection(ctx);
			return true;
		}
		if(JmxTuiKeys.isPlainSpaceKey(key)) {
			return true;
		}
		return false;
	}

	private void toggleRecordViewState(JmxTuiModeContext ctx) {
		if(JMXControl.isOfflineSession) {
			return;
		}
		if(ctx == null || !ctx.isRecordMode() || ctx.isRecordSamplingInProgress()) {
			return;
		}
		RecordViewState next = ctx.getRecordViewState() == RecordViewState.LIVE
			? RecordViewState.DUMP
			: RecordViewState.LIVE;
		ctx.setRecordViewState(next);
		refreshRecordView(ctx);
	}

	private void refreshRecordView(JmxTuiModeContext ctx) {
		if(ctx == null || !ctx.isRecordMode()) {
			return;
		}
		if(ctx.getRecordViewState() == RecordViewState.LIVE) {
			refreshThreads(ctx);
			updateThreadAutoRefreshScheduler(ctx);
		} else {
			stopThreadAutoRefreshScheduler();
			refreshRecordAnalysisThreads(ctx);
		}
		refreshHeader();
	}

	private void showRecordDumpInfoInOutput(JmxTuiModeContext ctx) {
		if(ctx == null || !ctx.isRecordMode() || ctx.getRecordViewState() != RecordViewState.DUMP) {
			return;
		}
		RecordResult result = JmxRecordCommand.getLastRecordResult();
		if(result == null) {
			TuiDialogs.showMessageDialog(
				gui,
				TuiRecordUiKeys.DUMP_NO_DATA_TITLE,
				TuiRecordUiKeys.DUMP_NO_DATA_MESSAGE,
				MessageDialogButton.OK
			);
			return;
		}
		ensureOutputPaneVisible();
		outputSink.beginUserCaptureSession("RECORD VIEW INFO");
		clearOutput();
		outputSink.commitUserCapturePreamble();
		try {
			JmxSettings settings = executor.getSettings();
			result.printInfo(
				settings == null ? null : settings.getDumpStackFilter(),
				false);
		} catch(Exception e) {
			Logger.logln(Logger.Level.ERROR, "Failed to print record info: " + e.getMessage());
		} finally {
			outputSink.endUserCaptureSession();
		}
		refreshHeader();
	}

	private void saveRecordDumpFromUi(JmxTuiModeContext ctx) {
		if(ctx == null || !ctx.isRecordMode() || ctx.getRecordViewState() != RecordViewState.DUMP) {
			return;
		}
		RecordResult result = JmxRecordCommand.getLastRecordResult();
		if(result == null) {
			TuiDialogs.showMessageDialog(
				gui,
				TuiRecordUiKeys.DUMP_NO_DATA_TITLE,
				TuiRecordUiKeys.DUMP_NO_DATA_MESSAGE,
				MessageDialogButton.OK
			);
			return;
		}
		String pathInput = TuiRecordDumpFileDialog.show(
			gui,
			TuiRecordUiKeys.DUMP_SAVE_DIALOG_TITLE,
			TuiRecordUiKeys.DUMP_FILE_DIALOG_DESCRIPTION,
			RecordDumpDialogPath.getInitialPath()
		);
		if(pathInput == null) {
			return;
		}
		RecordDumpDialogPath.rememberEnteredPath(pathInput);
		File dumpFile;
		try {
			dumpFile = resolveRecordDumpFile(pathInput);
		} catch(IOException e) {
			TuiDialogs.showMessageDialog(
				gui,
				TuiRecordUiKeys.DUMP_SAVE_FAILED_TITLE,
				String.format(TuiRecordUiKeys.DUMP_SAVE_FAILED_MESSAGE_TEMPLATE, e.getMessage()),
				MessageDialogButton.OK
			);
			return;
		}
		if(dumpFile.exists()) {
			MessageDialogButton overwrite = TuiDialogs.showMessageDialog(
				gui,
				TuiRecordUiKeys.DUMP_OVERWRITE_CONFIRM_TITLE,
				String.format(TuiRecordUiKeys.DUMP_OVERWRITE_CONFIRM_MESSAGE_TEMPLATE, dumpFile.getAbsolutePath()),
				MessageDialogButton.Yes,
				MessageDialogButton.No
			);
			if(overwrite != MessageDialogButton.Yes) {
				return;
			}
		}
		try {
			if(result.saveToFile(dumpFile)) {
				TuiDialogs.showMessageDialog(
					gui,
					TuiRecordUiKeys.DUMP_SAVE_SUCCESS_TITLE,
					String.format(TuiRecordUiKeys.DUMP_SAVE_SUCCESS_MESSAGE_TEMPLATE, dumpFile.getAbsolutePath()),
					MessageDialogButton.OK
				);
				refreshRecordView(ctx);
			} else {
				TuiDialogs.showMessageDialog(
					gui,
					TuiRecordUiKeys.DUMP_SAVE_FAILED_TITLE,
					String.format(
						TuiRecordUiKeys.DUMP_SAVE_FAILED_MESSAGE_TEMPLATE,
						"See the log for details."
					),
					MessageDialogButton.OK
				);
			}
		} catch(Exception e) {
			TuiDialogs.showMessageDialog(
				gui,
				TuiRecordUiKeys.DUMP_SAVE_FAILED_TITLE,
				String.format(TuiRecordUiKeys.DUMP_SAVE_FAILED_MESSAGE_TEMPLATE, e.getMessage()),
				MessageDialogButton.OK
			);
		}
	}

	private void loadRecordDumpFromUi(JmxTuiModeContext ctx) {
		if(ctx == null || !ctx.isRecordMode() || ctx.getRecordViewState() != RecordViewState.DUMP) {
			return;
		}
		RecordResult current = JmxRecordCommand.getLastRecordResult();
		String pathInput = TuiRecordDumpFileDialog.show(
			gui,
			TuiRecordUiKeys.DUMP_LOAD_DIALOG_TITLE,
			TuiRecordUiKeys.DUMP_FILE_DIALOG_DESCRIPTION,
			RecordDumpDialogPath.getInitialPath()
		);
		if(pathInput == null) {
			return;
		}
		RecordDumpDialogPath.rememberEnteredPath(pathInput);
		File dumpFile;
		try {
			dumpFile = resolveRecordDumpFile(pathInput);
		} catch(Exception e) {
			TuiDialogs.showMessageDialog(
				gui,
				TuiRecordUiKeys.DUMP_LOAD_FAILED_TITLE,
				String.format(TuiRecordUiKeys.DUMP_SAVE_FAILED_MESSAGE_TEMPLATE, e.getMessage()),
				MessageDialogButton.OK
			);
			return;
		}
		if(!dumpFile.exists()) {
			TuiDialogs.showMessageDialog(
				gui,
				TuiRecordUiKeys.DUMP_FILE_NOT_FOUND_TITLE,
				String.format(TuiRecordUiKeys.DUMP_FILE_NOT_FOUND_MESSAGE_TEMPLATE, dumpFile.getAbsolutePath()),
				MessageDialogButton.OK
			);
			return;
		}
		try {
			if(current != null && dumpFile.getCanonicalPath().equals(current.dmpFilePath)) {
				TuiDialogs.showMessageDialog(
					gui,
					TuiRecordUiKeys.DUMP_ALREADY_LOADED_TITLE,
					String.format(TuiRecordUiKeys.DUMP_ALREADY_LOADED_MESSAGE_TEMPLATE, current.dmpFilePath),
					MessageDialogButton.OK
				);
				return;
			}
		} catch(IOException e) {
			TuiDialogs.showMessageDialog(
				gui,
				TuiRecordUiKeys.DUMP_LOAD_FAILED_TITLE,
				String.format(TuiRecordUiKeys.DUMP_SAVE_FAILED_MESSAGE_TEMPLATE, e.getMessage()),
				MessageDialogButton.OK
			);
			return;
		}
		if(current != null && current.dmpFilePath == null) {
			MessageDialogButton discard = TuiDialogs.showMessageDialog(
				gui,
				TuiRecordUiKeys.DUMP_DISCARD_UNSAVED_TITLE,
				TuiRecordUiKeys.DUMP_DISCARD_UNSAVED_MESSAGE,
				MessageDialogButton.Yes,
				MessageDialogButton.No
			);
			if(discard != MessageDialogButton.Yes) {
				return;
			}
		}
		RecordResult loadedResult = RecordResult.loadFromFile(dumpFile);
		if(loadedResult == null) {
			TuiDialogs.showMessageDialog(
				gui,
				TuiRecordUiKeys.DUMP_LOAD_FAILED_TITLE,
				TuiRecordUiKeys.DUMP_LOAD_FAILED_MESSAGE,
				MessageDialogButton.OK
			);
			return;
		}
		JmxRecordCommand.setLastRecordResult(loadedResult);
		refreshRecordView(ctx);
		showRecordDumpInfoInOutput(ctx);
		TuiDialogs.showMessageDialog(
			gui,
			TuiRecordUiKeys.DUMP_LOAD_SUCCESS_TITLE,
			TuiRecordUiKeys.DUMP_LOAD_SUCCESS_MESSAGE,
			MessageDialogButton.OK
		);
	}

	private static File resolveRecordDumpFile(String path) throws IOException {
		String resolvedPath = path;
		if(resolvedPath == null || resolvedPath.trim().isEmpty()) {
			resolvedPath = JMXerConstant.DEFAULT_DUMP_FILE_PATH;
		} else {
			resolvedPath = resolvedPath.trim();
		}
		File dumpFile = new File(resolvedPath);
		if(dumpFile.exists() && dumpFile.isDirectory()) {
			dumpFile = new File(resolvedPath + File.separator + JMXerConstant.DEFAULT_DUMP_FILE_NAME);
		}
		return dumpFile.getAbsoluteFile();
	}

	private void refreshRecordAnalysisThreads(JmxTuiModeContext ctx) {
		if(ctx == null || !ctx.isRecordMode()) {
			return;
		}
		Set<Long> preservedSelection = new HashSet<Long>(ctx.getSelectedThreadIds());
		List<ThreadSnapshotEntry> entries = ctx.getAllThreadEntries();
		entries.clear();
		ctx.setLastCreatedThreadCount(0);
		ctx.setLastDestroyedThreadCount(0);

		RecordResult result = JmxRecordCommand.getLastRecordResult();
		if(result == null || result.recordData == null || result.recordData.isEmpty()) {
			applyThreadSearchFilter(ctx);
			refreshThreadListBorderTime(ctx);
			reapplyThreadSelectionAfterRefresh(ctx, preservedSelection);
			return;
		}

		for(Map.Entry<Long, List<RecordThreadInfo>> recordEntry : result.recordData.entrySet()) {
			Long threadId = recordEntry.getKey();
			List<RecordThreadInfo> samples = recordEntry.getValue();
			if(threadId == null || samples == null || samples.isEmpty()) {
				continue;
			}
			RecordThreadInfo latest = samples.get(samples.size() - 1);
			String threadName = latest.getThreadName() == null ? "" : latest.getThreadName();
			String stateAbbrev = ThreadPaneTableFormat.abbreviateStateName(latest.getThreadState());
			double cpuCores = 0.0;
			boolean cpuValid = false;
			double memBytesPerSec = 0.0;
			boolean memValid = false;
			if(result.resourceData != null) {
				ResourceUsage usage = result.resourceData.get(threadId);
				if(usage != null) {
					long totalCpuNanos = usage.currCpu - usage.startCpu;
					long totalMemBytes = usage.currMem - usage.startMem;
					if(totalCpuNanos > -1L) {
						cpuCores = (double) totalCpuNanos;
						cpuValid = true;
					}
					if(totalMemBytes > -1L) {
						memBytesPerSec = (double) totalMemBytes;
						memValid = true;
					}
				}
			}
			entries.add(new ThreadSnapshotEntry(
				threadId.longValue(),
				threadName,
				stateAbbrev,
				cpuCores,
				cpuValid,
				memBytesPerSec,
				memValid));
		}

		reapplyThreadSelectionAfterRefresh(ctx, preservedSelection);
		applyThreadSearchFilter(ctx);
		refreshThreadListBorderTime(ctx);
	}

	private boolean handleRecordSampleSelectedKey(JmxTuiModeContext ctx, KeyStroke key) {
		if(key == null || ctx == null || !ctx.isRecordMode()
				|| ctx.getRecordViewState() != RecordViewState.LIVE
				|| ctx.isRecordSamplingInProgress()) {
			return false;
		}
		if(!JmxTuiKeys.isRecordSampleSelectedKey(key)) {
			return false;
		}
		startRecordSampleSelectedFromUi(ctx);
		return true;
	}

	private void startRecordSampleCursorFromUi(final JmxTuiModeContext ctx) {
		if(!canStartRecordSampleFromUi(ctx)) {
			return;
		}
		Long threadId = resolveCursorThreadId(ctx);
		if(threadId == null) {
			TuiDialogs.showMessageDialog(
				gui,
				TuiRecordUiKeys.NO_SELECTION_TITLE,
				TuiRecordUiKeys.NO_CURSOR_THREAD_MESSAGE,
				MessageDialogButton.OK
			);
			return;
		}
		startRecordSampleFromUi(ctx, String.valueOf(threadId.longValue()), 1);
	}

	private void startRecordSampleSelectedFromUi(final JmxTuiModeContext ctx) {
		if(!canStartRecordSampleFromUi(ctx)) {
			return;
		}
		if(ctx.getSelectedThreadIds().isEmpty()) {
			TuiDialogs.showMessageDialog(
				gui,
				TuiRecordUiKeys.NO_SELECTION_TITLE,
				TuiRecordUiKeys.NO_SELECTION_MESSAGE,
				MessageDialogButton.OK
			);
			return;
		}
		final String threadExpr = RecordUiMode.buildSelectedThreadsExpression(ctx);
		if(threadExpr == null || threadExpr.trim().length() == 0) {
			return;
		}
		startRecordSampleFromUi(ctx, threadExpr, ctx.getSelectedThreadIds().size());
	}

	private boolean canStartRecordSampleFromUi(JmxTuiModeContext ctx) {
		if(ctx == null || !ctx.isRecordMode() || ctx.getRecordViewState() != RecordViewState.LIVE) {
			return false;
		}
		if(JMXControl.isOfflineSession) {
			TuiDialogs.showMessageDialog(
				gui,
				TuiRecordUiKeys.NO_SELECTION_TITLE,
				"JMX commands are not allowed in offline session.",
				MessageDialogButton.OK
			);
			return false;
		}
		if(ctx.isRecordSamplingInProgress()) {
			return false;
		}
		return true;
	}

	private void startRecordSampleFromUi(final JmxTuiModeContext ctx, final String threadExpr, final int threadCount) {
		if(ctx == null || threadExpr == null || threadExpr.trim().length() == 0 || threadCount < 1) {
			return;
		}
		final TuiRecordSampleTypeDialog.SampleType sampleType = TuiRecordSampleTypeDialog.show(gui);
		if(sampleType == null) {
			return;
		}
		if(sampleType == TuiRecordSampleTypeDialog.SampleType.STACKTRACE) {
			startRecordStacktraceFromUi(ctx, threadExpr, threadCount);
			return;
		}
		startRecordResourceFromUi(ctx, threadExpr, threadCount);
	}

	private void startRecordStacktraceFromUi(final JmxTuiModeContext ctx, final String threadExpr, final int threadCount) {
		final TuiRecordSamplingDialog.SamplingParams params = TuiRecordSamplingDialog.show(gui, threadCount, threadExpr);
		if(params == null) {
			return;
		}
		if(!confirmRecordSampling(params, threadCount)) {
			return;
		}
		commandWorker.execute(new Runnable() {
			@Override
			public void run() {
				runRecordSampling(ctx, threadExpr, params);
			}
		});
	}

	private void startRecordResourceFromUi(final JmxTuiModeContext ctx, final String threadExpr, final int threadCount) {
		final Long timeMs = TuiRecordResourceDurationDialog.show(gui, threadCount, threadExpr);
		if(timeMs == null) {
			return;
		}
		commandWorker.execute(new Runnable() {
			@Override
			public void run() {
				runRecordResource(ctx, threadExpr, timeMs.longValue());
			}
		});
	}

	private boolean confirmRecordSampling(TuiRecordSamplingDialog.SamplingParams params, int threadCount) {
		StringBuilder message = new StringBuilder();
		message.append(TuiRecordUiKeys.WARNING_SLOW);
		if(params.timeMs == 0L) {
			message.append('\n').append(TuiRecordUiKeys.WARNING_UNLIMITED);
		}
		message.append("\n\nSample stacktrace for ").append(threadCount).append(" thread");
		if(threadCount != 1) {
			message.append('s');
		}
		message.append('?');
		message.append("\n\n").append(TuiRecordUiKeys.CONFIRM_SAMPLE_CONTINUE)
			.append(" to proceed or ").append(TuiRecordUiKeys.CONFIRM_SAMPLE_CANCEL).append(" to abort.");
		MessageDialogButton chosen = TuiGuiThread.callOnGuiThread(gui, new TuiGuiThread.GuiCallable<MessageDialogButton>() {
			@Override
			public MessageDialogButton call() {
				return TuiDialogs.showMessageDialog(
					gui,
					TuiRecordUiKeys.WARNING_TITLE,
					message.toString(),
					MessageDialogButton.Yes,
					MessageDialogButton.No
				);
			}
		});
		return chosen == MessageDialogButton.Yes;
	}

	private void runRecordSampling(final JmxTuiModeContext ctx, final String threadExpr,
			final TuiRecordSamplingDialog.SamplingParams params) {
		final boolean unlimitedDuration = params.timeMs == 0L;
		final String commandLine = "RECORD STACKTRACE " + threadExpr;
		final long recordStartMs = System.currentTimeMillis();
		TuiGuiThread.callOnGuiThread(gui, new TuiGuiThread.GuiCallable<Void>() {
			@Override
			public Void call() {
				ctx.setRecordSamplingInProgress(true);
				refreshHeader();
				recordProgressDialog = TuiRecordProgressDialog.show(
					gui, RecordProgressKind.STACKTRACE, params.timeMs);
				return null;
			}
		});
		final AtomicBoolean progressActive = new AtomicBoolean(true);
		Thread progressUpdater = new Thread(new Runnable() {
			@Override
			public void run() {
				while(progressActive.get()) {
					refreshRecordProgressDialog(recordStartMs, StackRecordThread.getActiveSampleCount());
					try {
						Thread.sleep(500L);
					} catch(InterruptedException e) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}
		}, "JmxTuiRecordProgress");
		progressUpdater.setDaemon(true);
		progressUpdater.start();
		Thread cancelWatcher = null;
		if(!unlimitedDuration) {
			cancelWatcher = startTimedRecordCancelWatcher(progressActive);
		}
		try {
			ensureOutputPaneVisible();
			outputSink.beginUserCaptureSession(commandLine);
			JmxRecordCommand.setLastRecordResult(null);
			List<String> presetAnswers = new ArrayList<String>();
			presetAnswers.add(String.valueOf(params.intervalMs));
			presetAnswers.add(String.valueOf(params.timeMs));
			presetAnswers.add("Y");
			TuiQueuedLineReader.StopAwaiter stopAwaiter = null;
			if(unlimitedDuration) {
				stopAwaiter = new TuiQueuedLineReader.StopAwaiter() {
					@Override
					public void awaitStop() throws InterruptedException {
						while(true) {
							TuiRecordProgressDialog progress = recordProgressDialog;
							if(progress != null && progress.isStopRequested()) {
								return;
							}
							Thread.sleep(200L);
						}
					}
				};
			}
			String summary = TuiRecordOutputSummary.formatStacktraceSampling(params);
			TuiQueuedLineReader reader = new TuiQueuedLineReader(
				gui, presetAnswers, stopAwaiter, summary, true);
			processCommandForTuiWithLineReader(commandLine, reader);
		} catch(Exception e) {
			Logger.logStackTrace(e);
			Logger.logln(Logger.Level.ERROR, "Failed to record stacktrace: " + e.getMessage());
		} finally {
			progressActive.set(false);
			joinRecordHelperThread(cancelWatcher);
			try {
				progressUpdater.join(3000L);
			} catch(InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			outputSink.endUserCaptureSession();
			TuiGuiThread.callOnGuiThread(gui, new TuiGuiThread.GuiCallable<Void>() {
				@Override
				public Void call() {
					ctx.setRecordSamplingInProgress(false);
					TuiRecordProgressDialog dialog = recordProgressDialog;
					recordProgressDialog = null;
					if(dialog != null) {
						dialog.closeDialog();
					}
					handleRecordStacktraceComplete(ctx);
					return null;
				}
			});
		}
	}

	private void runRecordResource(final JmxTuiModeContext ctx, final String threadExpr, final long timeMs) {
		final boolean unlimitedDuration = timeMs == 0L;
		final String commandLine = "RECORD RESOURCE " + threadExpr;
		final long recordStartMs = System.currentTimeMillis();
		TuiGuiThread.callOnGuiThread(gui, new TuiGuiThread.GuiCallable<Void>() {
			@Override
			public Void call() {
				ctx.setRecordSamplingInProgress(true);
				refreshHeader();
				recordProgressDialog = TuiRecordProgressDialog.show(
					gui, RecordProgressKind.RESOURCE, timeMs);
				return null;
			}
		});
		final AtomicBoolean progressActive = new AtomicBoolean(true);
		Thread progressUpdater = new Thread(new Runnable() {
			@Override
			public void run() {
				while(progressActive.get()) {
					refreshRecordProgressDialog(recordStartMs, -1);
					try {
						Thread.sleep(500L);
					} catch(InterruptedException e) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}
		}, "JmxTuiRecordProgress");
		progressUpdater.setDaemon(true);
		progressUpdater.start();
		Thread cancelWatcher = null;
		if(!unlimitedDuration) {
			cancelWatcher = startTimedRecordCancelWatcher(progressActive);
		}
		try {
			ensureOutputPaneVisible();
			outputSink.beginUserCaptureSession(commandLine);
			List<String> presetAnswers = new ArrayList<String>();
			presetAnswers.add(String.valueOf(timeMs));
			presetAnswers.add("Y");
			TuiQueuedLineReader.StopAwaiter stopAwaiter = null;
			if(unlimitedDuration) {
				stopAwaiter = new TuiQueuedLineReader.StopAwaiter() {
					@Override
					public void awaitStop() throws InterruptedException {
						while(true) {
							TuiRecordProgressDialog progress = recordProgressDialog;
							if(progress != null && progress.isStopRequested()) {
								return;
							}
							Thread.sleep(200L);
						}
					}
				};
			}
			String summary = TuiRecordOutputSummary.formatResourceSampling(timeMs);
			TuiQueuedLineReader reader = new TuiQueuedLineReader(
				gui, presetAnswers, stopAwaiter, summary, true);
			processCommandForTuiWithLineReader(commandLine, reader);
		} catch(Exception e) {
			Logger.logStackTrace(e);
			Logger.logln(Logger.Level.ERROR, "Failed to record resource: " + e.getMessage());
		} finally {
			progressActive.set(false);
			joinRecordHelperThread(cancelWatcher);
			try {
				progressUpdater.join(3000L);
			} catch(InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			outputSink.endUserCaptureSession();
			TuiGuiThread.callOnGuiThread(gui, new TuiGuiThread.GuiCallable<Void>() {
				@Override
				public Void call() {
					ctx.setRecordSamplingInProgress(false);
					TuiRecordProgressDialog dialog = recordProgressDialog;
					recordProgressDialog = null;
					if(dialog != null) {
						dialog.closeDialog();
					}
					refreshHeader();
					return null;
				}
			});
		}
	}

	private Thread startTimedRecordCancelWatcher(final AtomicBoolean progressActive) {
		Thread watcher = new Thread(new Runnable() {
			@Override
			public void run() {
				while(progressActive.get()) {
					TuiRecordProgressDialog progress = recordProgressDialog;
					if(progress != null && progress.isStopRequested()) {
						Logger.logln(TuiRecordUiKeys.OUTPUT_STOPPED_BY_USER);
						AbstractRecordThread.stopActiveRecording();
						return;
					}
					try {
						Thread.sleep(200L);
					} catch(InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
				}
			}
		}, "JmxTuiRecordCancel");
		watcher.setDaemon(true);
		watcher.start();
		return watcher;
	}

	private void joinRecordHelperThread(Thread helper) {
		if(helper == null) {
			return;
		}
		try {
			helper.join(3000L);
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void refreshRecordProgressDialog(final long recordStartMs, final int sampleCount) {
		final long elapsedMs = Math.max(0L, System.currentTimeMillis() - recordStartMs);
		TuiGuiThread.callOnGuiThread(gui, new TuiGuiThread.GuiCallable<Void>() {
			@Override
			public Void call() {
				TuiRecordProgressDialog dialog = recordProgressDialog;
				if(dialog != null) {
					dialog.refreshProgress(elapsedMs, sampleCount);
				}
				return null;
			}
		});
	}

	private void handleRecordStacktraceComplete(JmxTuiModeContext ctx) {
		RecordResult result = JmxRecordCommand.getLastRecordResult();
		int sampleCount = result == null ? 0 : result.sampleCount;
		if(sampleCount < 1) {
			TuiDialogs.showMessageDialog(
				gui,
				TuiRecordUiKeys.NO_DATA_TITLE,
				TuiRecordUiKeys.NO_DATA_MESSAGE,
				MessageDialogButton.OK
			);
			refreshHeader();
			return;
		}
		long elapsedMs = result.endTime - result.startTime;
		if(elapsedMs < 0L) {
			elapsedMs = 0L;
		}
		String completeMessage = String.format(
			TuiRecordUiKeys.COMPLETE_MESSAGE_TEMPLATE,
			Long.valueOf(elapsedMs),
			Integer.valueOf(sampleCount)
		);
		MessageDialogButton chosen = TuiDialogs.showMessageDialog(
			gui,
			TuiRecordUiKeys.COMPLETE_TITLE,
			completeMessage,
			MessageDialogButton.Yes,
			MessageDialogButton.No
		);
		if(chosen == MessageDialogButton.Yes) {
			ctx.setRecordViewState(RecordViewState.DUMP);
			refreshRecordView(ctx);
			showRecordDumpInfoInOutput(ctx);
		} else {
			refreshHeader();
		}
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
		if(ctx == null || !ctx.hasThreadPane()) {
			return;
		}
		if(ctx.isRecordMode()) {
			if(ctx.getRecordViewState() != RecordViewState.LIVE || ctx.isRecordSamplingInProgress()) {
				return;
			}
		} else if(!ctx.isThreadMode()) {
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
				TuiThreadUiKeys.bulkStacktraceNoneSelectedMessage(),
				MessageDialogButton.OK
			);
			return;
		}
		if(!confirmSelectedThreadsStackTrace(selectedCount)) {
			return;
		}
		String expr = ctx.isRecordMode()
			? RecordUiMode.buildSelectedThreadsExpression(ctx)
			: ThreadUiMode.buildSelectedThreadsExpression(ctx);
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
				TuiThreadUiKeys.selectionRequiresSelectedMessage(),
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
		} catch(Exception e) {}
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

	private void showThreadSortColumnDialog(JmxTuiModeContext ctx) {
		if(ctx == null || !ctx.hasThreadPane()) {
			return;
		}
		ThreadPaneSortSettings chosen = TuiThreadSortDialog.show(gui, ctx.getThreadSortSettings());
		if(chosen == null) {
			return;
		}
		if(chosen.equals(ctx.getThreadSortSettings())) {
			return;
		}
		ctx.setThreadSortSettings(chosen);
		applyThreadSearchFilter(ctx);
		refreshThreadListBorderTime(ctx);
	}

	private void showEditThreadSelectionDialog(JmxTuiModeContext ctx) {
		if(ctx == null || !ctx.hasThreadPane()) {
			return;
		}
		String initial = ctx.isRecordMode()
			? RecordUiMode.buildSelectedThreadsExpression(ctx)
			: ThreadUiMode.buildSelectedThreadsExpression(ctx);
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

	private void showRequestEntryFileDialog() {
		JmxSettings settings = executor.getSettings();
		if(settings == null) {
			return;
		}
		String current = settings.getReqEntryFile();
		if(current == null) {
			current = "";
		}
		String edited = TuiRequestEntryFileDialog.show(gui, current);
		if(edited == null) {
			return;
		}
		settings.setReqEntryFile(edited);
		refreshHeader();
	}

	private void applyThreadSelectionFromExpression(JmxTuiModeContext ctx, String expr) {
		if(ctx == null || !ctx.hasThreadPane()) {
			return;
		}
		String trimmed = expr == null ? "" : expr.trim();
		Set<Long> newSelection = new HashSet<Long>();
		if(trimmed.length() > 0) {
			Map<Long, String> threadMap = ctx.isRecordMode()
				? RecordUiMode.buildThreadMap(ctx)
				: ThreadUiMode.buildThreadMap(ctx);
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
		if(ctx == null || !ctx.hasThreadPane()) {
			return;
		}
		String expr;
		if(ctx.isRecordMode()) {
			expr = RecordUiMode.buildSelectedThreadsExpression(ctx);
		} else {
			expr = ThreadUiMode.buildSelectedThreadsExpression(ctx);
		}
		ctx.setSelectedExpression(expr == null ? "" : expr);
	}

	private void onThreadAutoRefreshTick() {
		if(currentMode == null) {
			return;
		}
		boolean threadMode = ThreadUiMode.ID.equals(currentMode.getId());
		boolean recordRecording = RecordUiMode.ID.equals(currentMode.getId())
			&& activeContext != null && activeContext.getRecordViewState() == RecordViewState.LIVE;
		if(!threadMode && !recordRecording) {
			return;
		}
		JmxTuiModeContext ctx = activeContext;
		if(ctx == null || !ctx.hasThreadPane() || !ctx.isThreadAutoRefreshEnabled()) {
			return;
		}
		refreshThreads(ctx, false, false, true);
	}

	private void forceRefreshThreads(JmxTuiModeContext ctx) {
		if(ctx == null || !ctx.hasThreadPane()) {
			return;
		}
		if(ctx.isRecordMode() && ctx.getRecordViewState() == RecordViewState.DUMP) {
			refreshRecordAnalysisThreads(ctx);
			return;
		}
		refreshThreads(ctx, false, false, true);
	}

	private void toggleThreadRefreshPolicy(JmxTuiModeContext ctx) {
		if(!isLiveThreadRefreshContext(ctx)) {
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
		if(!isLiveThreadRefreshContext(ctx) || !ctx.isThreadAutoRefreshEnabled()) {
			stopThreadAutoRefreshScheduler();
			return;
		}
		startThreadAutoRefreshScheduler(ctx);
	}

	private void startThreadAutoRefreshScheduler(JmxTuiModeContext ctx) {
		if(!isLiveThreadRefreshContext(ctx) || !ctx.isThreadAutoRefreshEnabled()) {
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

	private boolean isLiveThreadRefreshContext(JmxTuiModeContext ctx) {
		if(ctx == null || !ctx.hasThreadPane()) {
			return false;
		}
		if(ctx.isThreadMode()) {
			return true;
		}
		return ctx.isRecordMode() && ctx.getRecordViewState() == RecordViewState.LIVE;
	}

	private void refreshThreads(JmxTuiModeContext ctx, boolean afterDisconnectHandling, boolean forceOnlineFetch, boolean markDelta) {
		if(!isLiveThreadRefreshContext(ctx)) {
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
			Map<Long, String> threadMap = ctx.isRecordMode()
				? RecordUiMode.buildThreadMap(ctx)
				: ThreadUiMode.buildThreadMap(ctx);
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
			} catch(Exception e) {}
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
		if(ctx == null || !ctx.hasThreadPane()) {
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
		boolean dumpView = ctx.isRecordMode() && ctx.getRecordViewState() == RecordViewState.DUMP;
		ThreadPaneSorter.sort(allEntries, ctx.getThreadSortSettings());
		int tidWidth = ThreadPaneTableFormat.computeTidWidth(allEntries);
		int cpuWidth = ThreadPaneTableFormat.computeCpuWidth(allEntries);
		int memWidth = ThreadPaneTableFormat.computeMemWidth(allEntries);
		ctx.refreshThreadTableHeader(allEntries, dumpView);

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
			String emptyLabel = "(no threads)";
			if(ctx.isRecordMode() && ctx.getRecordViewState() == RecordViewState.DUMP) {
				emptyLabel = "(no record data)";
			}
			threadList.addItem(emptyLabel, NOOP_LIST_ACTION);
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
			String label = ThreadPaneTableFormat.buildRowLine(entry, tidWidth, cpuWidth, memWidth, dumpView);
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
		if(c == null || !c.hasThreadPane()) {
			return;
		}
		boolean dumpView = c.isRecordMode() && c.getRecordViewState() == RecordViewState.DUMP;
		String borderBase = dumpView
			? RecordUiMode.DUMP_LIST_BORDER_TITLE
			: JmxTuiModeContext.THREAD_LIST_BORDER_TITLE;
		String formattedTime;
		if(dumpView) {
			formattedTime = RecordUiMode.formatRecordDumpBorderTime(JmxRecordCommand.getLastRecordResult());
		} else {
			formattedTime = LocalTime.now().format(OUTPUT_PANE_TIME_FMT);
		}
		tryRedockThreadListBorder(
			c,
			ThreadPaneSortSettings.buildListBorderTitle(
				borderBase,
				formattedTime,
				c.getThreadSortSettings()
			)
		);
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
		if(ctx == null || ctx.hasThreadPane()) {
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
		TuiDetailDialog.showHelp(gui, "Help", buildHelpText(m, c, keyBindings));
	}

	private void showKeyRemapDialog() {
		JmxTuiModeContext c = ctx();
		JmxTuiModeFocusPane focusedPane = resolveCurrentFocusPane();
		List<TuiKeyAction> globalActions = TuiKeyRemapCatalog.listGlobalActions(currentMode, c, keyBindings);
		List<TuiKeyAction> windowActions = TuiKeyRemapCatalog.listWindowActions(
			currentMode, c, focusedPane, executor.getSettings(), keyBindings);
		if(globalActions.isEmpty() && windowActions.isEmpty()) {
			TuiDialogs.showMessageDialog(
				gui,
				TuiKeyRemapUiKeys.NO_KEYS_TITLE,
				TuiKeyRemapUiKeys.NO_KEYS_MESSAGE,
				MessageDialogButton.OK
			);
			return;
		}
		TuiKeyRemapDialog.show(gui, keyBindings, currentMode, c, focusedPane, executor.getSettings());
	}

	private static String buildHelpText(JmxTuiUiMode mode, JmxTuiModeContext ctx, TuiKeyBindings bindings) {
		String body;
		if(mode == null) {
			body = buildHelpTextMBeanMode(bindings);
		} else {
			String id = mode.getId();
			if(ThreadUiMode.ID.equals(id)) {
				body = buildHelpTextThreadMode(bindings);
			} else if(RecordUiMode.ID.equals(id)) {
				body = buildHelpTextRecordMode(bindings);
			} else {
				body = buildHelpTextMBeanMode(bindings);
			}
		}
		return body + "\n\n\n" + JMXerConstant.JMXER_COPYRIGHT_STRING;
	}

	private static String buildHelpTextMBeanMode(TuiKeyBindings bindings) {
		StringBuilder sb = new StringBuilder();
		sb.append("UI Mode : MBean\n\n");

		TuiHelpSectionBuilder global = new TuiHelpSectionBuilder("[Global]");
		global.addKeyLine(TuiHelpSectionBuilder.KEY_HELP, "F1: this help");
		global.addKeyLine(TuiHelpSectionBuilder.KEY_UI_MODE_CYCLE, "F3: cycle UI mode (MBean → Thread → Record)");
		global.addKeyLine(TuiHelpSectionBuilder.KEY_FOCUS_TAB, "Tab / Shift+Tab: move focus between windows");
		global.addActionLine(bindings, TuiKeyAction.FEATURES_PANE_TOGGLE, "toggle 'Features' window");
		global.addActionLine(bindings, TuiKeyAction.OUTPUT_PANE_TOGGLE, "toggle 'Output' window");
		if(!JMXControl.isOfflineSession) {
			global.addActionLine(bindings, TuiKeyAction.OUTPUT_PRETTY_PRINT, "toggle Pretty Print");
		}
		global.addKeyLine(TuiHelpSectionBuilder.KEY_EXIT, "Ctrl+C: exit");
		global.addKeyLine(TuiHelpSectionBuilder.KEY_REMAP_DIALOG,
			"F12: remap keys for current UI mode, view, and focused window (session only)");
		global.addKeyLine(TuiHelpSectionBuilder.KEY_RESIZE_HORIZONTAL,
			"Shift+Left/Right: resize Search/MBeans vs Features width");
		global.addKeyLine(TuiHelpSectionBuilder.KEY_RESIZE_VERTICAL, "Shift+Up/Down: resize lists vs Output height");
		global.appendTo(sb);
		sb.append('\n');

		TuiHelpSectionBuilder window = new TuiHelpSectionBuilder("[Window — depends on focus]");
		window.addPaneLine(TuiHelpSectionBuilder.PANE_ORDER_SEARCH, "Search: type to filter list; Esc — clear search");
		window.addPaneLine(TuiHelpSectionBuilder.PANE_ORDER_LIST, "MBeans: arrows, PgUp/Dn, Home/End — navigate; "
			+ TuiHelpSectionBuilder.joinSortedActionClauses(bindings, new TuiHelpSectionBuilder.ActionClause[] {
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.MBEAN_SET_DEFAULT, "set OBJECT"),
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.INFO, "information")
			}));
		window.addPaneLine(TuiHelpSectionBuilder.PANE_ORDER_FEATURES, "Features: arrows, PgUp/Dn, Home/End — navigate; "
			+ TuiHelpSectionBuilder.joinSortedActionClauses(bindings, new TuiHelpSectionBuilder.ActionClause[] {
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.FEATURE_INVOKE, "invoke"),
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.INFO, "information")
			}));
		window.addPaneLine(TuiHelpSectionBuilder.PANE_ORDER_OUTPUT, "Output: arrows & scroll; "
			+ TuiHelpSectionBuilder.joinSortedActionClauses(bindings, new TuiHelpSectionBuilder.ActionClause[] {
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.OUTPUT_SAVE, "save"),
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.OUTPUT_WRAP, "wrap")
			}));
		window.addPaneLine(TuiHelpSectionBuilder.PANE_ORDER_COMMAND,
			"Command: type JMXer commands; Up/Down — history; Esc — clear line; "
			+ TuiHelpSectionBuilder.joinSortedActionClauses(bindings, new TuiHelpSectionBuilder.ActionClause[] {
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.COMMAND_PASTE, "paste")
			}));
		window.appendTo(sb);
		return sb.toString();
	}

	private static String buildHelpTextThreadMode(TuiKeyBindings bindings) {
		StringBuilder sb = new StringBuilder();
		sb.append("UI Mode : Thread\n\n");

		TuiHelpSectionBuilder global = new TuiHelpSectionBuilder("[Global]");
		global.addKeyLine(TuiHelpSectionBuilder.KEY_HELP, "F1: this help");
		global.addKeyLine(TuiHelpSectionBuilder.KEY_UI_MODE_CYCLE, "F3: cycle UI mode (MBean → Thread → Record)");
		global.addActionLine(bindings, TuiKeyAction.THREAD_EDIT_SELECTION, "edit thread selection");
		global.addKeyLine(TuiHelpSectionBuilder.KEY_FOCUS_TAB, "Tab / Shift+Tab: move focus between windows");
		global.addActionLine(bindings, TuiKeyAction.THREAD_AUTO_REFRESH, "toggle auto/manual thread refresh");
		global.addActionLine(bindings, TuiKeyAction.THREAD_STACKTRACE_TARGET,
			"toggle STACKTRACE_TARGET (SELECTED / ACTIVE)");
		global.addActionLine(bindings, TuiKeyAction.THREAD_REQUEST_ENTRY_FILE,
			"edit REQUEST_ENTRY_FILE path");
		global.addActionLine(bindings, TuiKeyAction.OUTPUT_PANE_TOGGLE, "toggle 'Output' window");
		global.addKeyLine(TuiHelpSectionBuilder.KEY_EXIT, "Ctrl+C: exit");
		global.addKeyLine(TuiHelpSectionBuilder.KEY_REMAP_DIALOG,
			"F12: remap keys for current UI mode, view, and focused window (session only)");
		global.addKeyLine(TuiHelpSectionBuilder.KEY_RESIZE_HORIZONTAL,
			"Shift+Left/Right: resize Search/Thread vs Output width");
		global.appendTo(sb);
		sb.append('\n');

		TuiHelpSectionBuilder window = new TuiHelpSectionBuilder("[Window — depends on focus]");
		window.addPaneLine(TuiHelpSectionBuilder.PANE_ORDER_SEARCH,
			"Search: type to filter list; Esc — clear search; "
			+ TuiHelpSectionBuilder.joinSortedActionClauses(bindings, new TuiHelpSectionBuilder.ActionClause[] {
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.THREAD_BULK_STACKTRACE,
					"stacktrace of selected threads (SELECTED) or active threads (ACTIVE)"),
				TuiHelpSectionBuilder.actionPair(bindings, TuiKeyAction.THREAD_TOGGLE_ALL,
					TuiKeyAction.THREAD_CLEAR_SELECTED, "select all/clear selected")
			}));
		window.addPaneLine(TuiHelpSectionBuilder.PANE_ORDER_LIST, "Thread: arrows — navigate; "
			+ TuiHelpSectionBuilder.joinSortedActionClauses(bindings, new TuiHelpSectionBuilder.ActionClause[] {
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.INFO, "information"),
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.THREAD_FORCE_REFRESH, "refresh list"),
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.THREAD_SORT, "sort (column and order)"),
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.THREAD_BULK_STACKTRACE,
					"stacktrace of selected threads (SELECTED) or active threads (ACTIVE)"),
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.THREAD_PRIMARY_ACTION, "stacktrace"),
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.THREAD_TOGGLE_CURRENT, "select"),
				TuiHelpSectionBuilder.actionPair(bindings, TuiKeyAction.THREAD_TOGGLE_ALL,
					TuiKeyAction.THREAD_CLEAR_SELECTED, "select all/clear selected")
			}));
		window.addPaneLine(TuiHelpSectionBuilder.PANE_ORDER_OUTPUT, "Output: arrows & scroll; "
			+ TuiHelpSectionBuilder.joinSortedActionClauses(bindings, new TuiHelpSectionBuilder.ActionClause[] {
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.OUTPUT_SAVE, "save"),
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.OUTPUT_WRAP, "wrap")
			}));
		window.addPaneLine(TuiHelpSectionBuilder.PANE_ORDER_COMMAND,
			"Command: type JMXer commands; Up/Down — history; Esc — clear line; "
			+ TuiHelpSectionBuilder.joinSortedActionClauses(bindings, new TuiHelpSectionBuilder.ActionClause[] {
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.COMMAND_PASTE, "paste")
			}));
		window.appendTo(sb);
		return sb.toString();
	}

	private static String buildHelpTextRecordMode(TuiKeyBindings bindings) {
		if(JMXControl.isOfflineSession) {
			return buildHelpTextRecordModeOffline(bindings);
		}
		StringBuilder sb = new StringBuilder();
		sb.append("UI Mode : Record\n\n");

		TuiHelpSectionBuilder global = new TuiHelpSectionBuilder("[Global]");
		global.addKeyLine(TuiHelpSectionBuilder.KEY_HELP, "F1: this help");
		global.addKeyLine(TuiHelpSectionBuilder.KEY_UI_MODE_CYCLE, "F3: cycle UI mode (MBean → Thread → Record)");
		global.addActionLine(bindings, TuiKeyAction.RECORD_VIEW_TYPE_TOGGLE, "toggle Live/Dump view type");
		global.addActionLine(bindings, TuiKeyAction.THREAD_AUTO_REFRESH,
			"toggle auto/manual thread refresh (Live view only)");
		global.addActionLine(bindings, TuiKeyAction.THREAD_EDIT_SELECTION, "edit thread selection");
		global.addActionLine(bindings, TuiKeyAction.FEATURES_PANE_TOGGLE,
			"toggle dump stack filter (Dump view only)");
		global.addActionLine(bindings, TuiKeyAction.RECORD_SHOW_EMPTY_THREAD_TOGGLE,
			"toggle show empty thread (Dump view only)");
		global.addActionLine(bindings, TuiKeyAction.RECORD_DUMP_LOAD, "load dump file (Dump view only)");
		global.addActionLine(bindings, TuiKeyAction.RECORD_DUMP_SAVE, "save dump file (Dump view only)");
		global.addActionLine(bindings, TuiKeyAction.RECORD_DUMP_INFO, "dump info to Output (Dump view only)");
		global.addSortedSlashKeysLine(bindings, new TuiKeyAction[] {
			TuiKeyAction.RECORD_VIEW_METHOD,
			TuiKeyAction.RECORD_VIEW_THREAD,
			TuiKeyAction.RECORD_VIEW_STACK,
			TuiKeyAction.RECORD_VIEW_SEARCH
		}, "record view method/thread/stack/search for selected threads (Dump view only)");
		global.addKeyLine(TuiHelpSectionBuilder.KEY_FOCUS_TAB, "Tab / Shift+Tab: move focus between windows");
		global.addActionLine(bindings, TuiKeyAction.OUTPUT_PANE_TOGGLE, "toggle 'Output' window");
		global.addKeyLine(TuiHelpSectionBuilder.KEY_EXIT, "Ctrl+C: exit");
		global.addKeyLine(TuiHelpSectionBuilder.KEY_REMAP_DIALOG,
			"F12: remap keys for current UI mode, view, and focused window (session only)");
		global.addKeyLine(TuiHelpSectionBuilder.KEY_RESIZE_HORIZONTAL,
			"Shift+Left/Right: resize Search/Thread vs Output width");
		global.appendTo(sb);
		sb.append('\n');

		TuiHelpSectionBuilder window = new TuiHelpSectionBuilder("[Window — depends on focus]");
		window.addPaneLine(TuiHelpSectionBuilder.PANE_ORDER_SEARCH,
			"Search: type to filter list; Esc — clear search; "
			+ TuiHelpSectionBuilder.joinSortedActionClauses(bindings, new TuiHelpSectionBuilder.ActionClause[] {
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.RECORD_SAMPLE_SELECTED,
					"sample selected threads (Live view only)"),
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.THREAD_BULK_STACKTRACE,
					"stacktrace of selected threads (Live view only) / selected thread analysis (Dump view only)"),
				TuiHelpSectionBuilder.actionPair(bindings, TuiKeyAction.THREAD_TOGGLE_ALL,
					TuiKeyAction.THREAD_CLEAR_SELECTED, "select all/clear selected")
			}));
		window.addPaneLine(TuiHelpSectionBuilder.PANE_ORDER_LIST, "Thread: arrows — navigate; "
			+ TuiHelpSectionBuilder.joinSortedActionClauses(bindings, new TuiHelpSectionBuilder.ActionClause[] {
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.INFO,
					"dump thread info (Dump view) / information (Live view)"),
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.THREAD_FORCE_REFRESH, "refresh list (Live view only)"),
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.THREAD_SORT, "sort"),
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.RECORD_SAMPLE,
					"sample cursor thread (Live view only)"),
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.RECORD_SAMPLE_SELECTED,
					"sample selected threads (Live view only)"),
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.THREAD_PRIMARY_ACTION,
					"thread analysis (Dump view) / stacktrace (Live view)"),
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.THREAD_BULK_STACKTRACE,
					"stacktrace of selected threads (Live view only) / selected thread analysis (Dump view only)"),
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.THREAD_TOGGLE_CURRENT, "select"),
				TuiHelpSectionBuilder.actionPair(bindings, TuiKeyAction.THREAD_TOGGLE_ALL,
					TuiKeyAction.THREAD_CLEAR_SELECTED, "select all/clear selected")
			}));
		window.addPaneLine(TuiHelpSectionBuilder.PANE_ORDER_OUTPUT, "Output: arrows & scroll; "
			+ TuiHelpSectionBuilder.joinSortedActionClauses(bindings, new TuiHelpSectionBuilder.ActionClause[] {
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.OUTPUT_SAVE, "save"),
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.OUTPUT_WRAP, "wrap")
			}));
		window.addPaneLine(TuiHelpSectionBuilder.PANE_ORDER_COMMAND,
			"Command: type JMXer commands; Up/Down — history; Esc — clear line; "
			+ TuiHelpSectionBuilder.joinSortedActionClauses(bindings, new TuiHelpSectionBuilder.ActionClause[] {
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.COMMAND_PASTE, "paste")
			}));
		window.appendTo(sb);
		return sb.toString();
	}

	private static String buildHelpTextRecordModeOffline(TuiKeyBindings bindings) {
		StringBuilder sb = new StringBuilder();
		sb.append("UI Mode : Record (Offline — Dump view only)\n\n");

		TuiHelpSectionBuilder global = new TuiHelpSectionBuilder("[Global]");
		global.addKeyLine(TuiHelpSectionBuilder.KEY_HELP, "F1: this help");
		global.addActionLine(bindings, TuiKeyAction.FEATURES_PANE_TOGGLE, "toggle dump stack filter");
		global.addActionLine(bindings, TuiKeyAction.RECORD_SHOW_EMPTY_THREAD_TOGGLE, "toggle show empty thread");
		global.addActionLine(bindings, TuiKeyAction.RECORD_DUMP_LOAD, "load dump file");
		global.addActionLine(bindings, TuiKeyAction.RECORD_DUMP_SAVE, "save dump file");
		global.addActionLine(bindings, TuiKeyAction.RECORD_DUMP_INFO, "dump info to Output");
		global.addSortedSlashKeysLine(bindings, new TuiKeyAction[] {
			TuiKeyAction.RECORD_VIEW_METHOD,
			TuiKeyAction.RECORD_VIEW_THREAD,
			TuiKeyAction.RECORD_VIEW_STACK,
			TuiKeyAction.RECORD_VIEW_SEARCH
		}, "record view method/thread/stack/search for selected threads (Dump view only)");
		global.addActionLine(bindings, TuiKeyAction.THREAD_EDIT_SELECTION, "edit thread selection");
		global.addKeyLine(TuiHelpSectionBuilder.KEY_FOCUS_TAB, "Tab / Shift+Tab: move focus between windows");
		global.addActionLine(bindings, TuiKeyAction.OUTPUT_PANE_TOGGLE, "toggle 'Output' window");
		global.addKeyLine(TuiHelpSectionBuilder.KEY_EXIT, "Ctrl+C: exit");
		global.addKeyLine(TuiHelpSectionBuilder.KEY_REMAP_DIALOG,
			"F12: remap keys for current UI mode, view, and focused window (session only)");
		global.addKeyLine(TuiHelpSectionBuilder.KEY_RESIZE_HORIZONTAL,
			"Shift+Left/Right: resize Search/Thread vs Output width");
		global.appendTo(sb);
		sb.append('\n');

		TuiHelpSectionBuilder window = new TuiHelpSectionBuilder("[Window — depends on focus]");
		window.addPaneLine(TuiHelpSectionBuilder.PANE_ORDER_SEARCH,
			"Search: type to filter list; Esc — clear search; "
			+ TuiHelpSectionBuilder.joinSortedActionClauses(bindings, new TuiHelpSectionBuilder.ActionClause[] {
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.THREAD_BULK_STACKTRACE, "selected thread analysis"),
				TuiHelpSectionBuilder.actionPair(bindings, TuiKeyAction.THREAD_TOGGLE_ALL,
					TuiKeyAction.THREAD_CLEAR_SELECTED, "select all/clear selected")
			}));
		window.addPaneLine(TuiHelpSectionBuilder.PANE_ORDER_LIST, "Thread: arrows — navigate; "
			+ TuiHelpSectionBuilder.joinSortedActionClauses(bindings, new TuiHelpSectionBuilder.ActionClause[] {
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.INFO, "dump thread info"),
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.THREAD_SORT, "sort"),
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.THREAD_PRIMARY_ACTION, "thread analysis"),
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.THREAD_BULK_STACKTRACE, "selected thread analysis"),
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.THREAD_TOGGLE_CURRENT, "select"),
				TuiHelpSectionBuilder.actionPair(bindings, TuiKeyAction.THREAD_TOGGLE_ALL,
					TuiKeyAction.THREAD_CLEAR_SELECTED, "select all/clear selected")
			}));
		window.addPaneLine(TuiHelpSectionBuilder.PANE_ORDER_OUTPUT, "Output: arrows & scroll; "
			+ TuiHelpSectionBuilder.joinSortedActionClauses(bindings, new TuiHelpSectionBuilder.ActionClause[] {
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.OUTPUT_SAVE, "save"),
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.OUTPUT_WRAP, "wrap")
			}));
		window.addPaneLine(TuiHelpSectionBuilder.PANE_ORDER_COMMAND,
			"Command: type JMXer commands; Up/Down — history; Esc — clear line; "
			+ TuiHelpSectionBuilder.joinSortedActionClauses(bindings, new TuiHelpSectionBuilder.ActionClause[] {
				new TuiHelpSectionBuilder.ActionClause(TuiKeyAction.COMMAND_PASTE, "paste")
			}));
		window.appendTo(sb);
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

	private int resolveStatusInnerLineCount() {
		JmxTuiModeContext c = ctx();
		if(currentMode != null && RecordUiMode.ID.equals(currentMode.getId()) && c != null
				&& !c.isRecordSamplingInProgress()) {
			if(c.getRecordViewState() == RecordViewState.DUMP) {
				return HEADER_INNER_LINES + 1;
			}
			if(c.getRecordViewState() == RecordViewState.LIVE
					&& resolveCurrentFocusPane() == JmxTuiModeFocusPane.MBEANS) {
				return HEADER_INNER_LINES + 1;
			}
		}
		return HEADER_INNER_LINES;
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
		String targetJvmSuffix = "";
		if(!JMXControl.isOfflineSession) {
			targetJvmSuffix = targetVmStatusCache.getTargetJvmSuffix(executor.getMBeanConnector());
		}
		String recordViewSuffix = "";
		if(currentMode != null && RecordUiMode.ID.equals(currentMode.getId())) {
			recordViewSuffix = RecordUiMode.formatViewStateStatusSuffix(c);
		}
		header1.setText(TuiStatusText.fitSingleLine(
				TuiStatusSessionLine.build(sessionMode, connTarget, !JMXControl.isOfflineSession,
						targetJvmSuffix, uiMode, recordViewSuffix),
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
		boolean showRecordDumpGlobalLine2 = false;
		boolean showRecordLivePaneLine2 = false;
		String globalKeys;
		if(m == null || MBeanUiMode.ID.equals(m.getId())) {
			globalKeys = TuiGlobalKeysHint.mbean(outputOn, featuresOn, featuresAvailable, canResizeVerticalSplit);
		} else if(ThreadUiMode.ID.equals(m.getId())) {
			globalKeys = TuiGlobalKeysHint.thread(outputOn);
		} else if(RecordUiMode.ID.equals(m.getId())) {
			boolean liveView = c != null && c.getRecordViewState() == RecordViewState.LIVE;
			boolean samplingInProgress = c != null && c.isRecordSamplingInProgress();
			if(JMXControl.isOfflineSession) {
				globalKeys = TuiGlobalKeysHint.recordOfflineDump(outputOn);
				showRecordDumpGlobalLine2 = !samplingInProgress;
			} else {
				globalKeys = TuiGlobalKeysHint.record(outputOn, liveView, samplingInProgress);
				if(!liveView && !samplingInProgress) {
					showRecordDumpGlobalLine2 = true;
				}
			}
		} else {
			globalKeys = TuiGlobalKeysHint.mbean(outputOn, featuresOn, featuresAvailable, canResizeVerticalSplit);
		}
		headerKeysGlobal.setText(TuiStatusText.fitSingleLine(globalKeys, statusInnerCols));
		if(showRecordDumpGlobalLine2) {
			headerKeysGlobalLine2.setText(TuiStatusText.fitSingleLine(
				TuiGlobalKeysHint.recordDumpViewSecondLine(), statusInnerCols));
		} else {
			headerKeysGlobalLine2.setText("");
		}
		headerKeysGlobalLine2.setVisible(showRecordDumpGlobalLine2);
		int statusInnerLines = resolveStatusInnerLineCount();
		if(statusInner.getPreferredSize().getRows() != statusInnerLines) {
			statusInner.setPreferredSize(new TerminalSize(statusInnerCols, statusInnerLines));
			statusInner.invalidate();
		}
		boolean wrapOn = c != null && c.getOutput().isWrapMode();
		JmxTuiModeFocusPane focusedPane = resolveCurrentFocusPane();
		String paneKeys = currentMode == null ? "" : currentMode.getPaneKeysHint(focusedPane, wrapOn, s, c);
		headerKeysPane.setText(TuiStatusText.fitSingleLine(paneKeys, statusInnerCols));
		if(currentMode != null && RecordUiMode.ID.equals(currentMode.getId()) && c != null
				&& c.getRecordViewState() == RecordViewState.LIVE && !c.isRecordSamplingInProgress()
				&& focusedPane == JmxTuiModeFocusPane.MBEANS) {
			showRecordLivePaneLine2 = true;
			headerKeysPaneLine2.setText(TuiStatusText.fitSingleLine(
				TuiPaneKeysHint.recordThreadListSecondLine(true), statusInnerCols));
		} else {
			headerKeysPaneLine2.setText("");
		}
		headerKeysPaneLine2.setVisible(showRecordLivePaneLine2);
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
		if(!c.hasThreadPane()) {
			applyMbeanSearchFilter(c);
		}
	}

	private void clearFeatures(JmxTuiModeContext ctx) {
		ctx.getFeatureList().clearItems();
		ctx.getCurrentFeatures().clear();
		ctx.setFeaturesForObjectName(null);
	}

	private boolean isFeaturesLoadedForObjectName(JmxTuiModeContext ctx, String objectName) {
		if(objectName == null || objectName.length() == 0 || ctx == null) {
			return false;
		}
		String loaded = ctx.getFeaturesForObjectName();
		return objectName.equals(loaded) && !ctx.getCurrentFeatures().isEmpty();
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
			if(isFeaturesLoadedForObjectName(ctx, objectName)) {
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
			processCommandForTuiWithDialogReader("OBJECT \"" + objectName + "\"");
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
			stopRequested = processCommandForTuiWithDialogReader(commandLine);
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

	private boolean showRecordDumpThreadDetail(JmxTuiModeContext ctx) {
		Long threadId = resolveCursorThreadId(ctx);
		if(threadId == null) {
			TuiDetailDialog.show(gui, "Thread", "No thread selected.");
			return true;
		}
		RecordResult result = JmxRecordCommand.getLastRecordResult();
		if(result == null) {
			TuiDetailDialog.show(gui, "Thread", "No record result in memory. Please record a stacktrace or load a dump file first.");
			return true;
		}
		JmxSettings settings = executor.getSettings();
		String detailText = RecordDumpThreadDetail.formatDetail(
			result,
			threadId.longValue(),
			settings == null ? null : settings.getDumpStackFilter(),
			settings == null ? null : settings.getReqEntryFile());
		if(detailText == null || detailText.trim().isEmpty()) {
			detailText = "No stacktrace samples for this thread in dump.";
		}
		TuiDetailDialog.show(gui, "Thread", detailText);
		return true;
	}

	private void showRecordDumpThreadAnalysisInOutput(JmxTuiModeContext ctx) {
		Long threadId = resolveCursorThreadId(ctx);
		if(threadId == null) {
			return;
		}
		if(JmxRecordCommand.getLastRecordResult() == null) {
			TuiDialogs.showMessageDialog(
				gui,
				TuiRecordUiKeys.DUMP_NO_DATA_TITLE,
				TuiRecordUiKeys.DUMP_NO_DATA_MESSAGE,
				MessageDialogButton.OK
			);
			return;
		}
		executeCommandFromListSelection("RECORD VIEW THREAD " + threadId.longValue());
	}

	private void showRecordDumpSelectedThreadsAnalysisInOutput(JmxTuiModeContext ctx) {
		if(ctx == null || !ctx.isRecordMode() || ctx.getRecordViewState() != RecordViewState.DUMP) {
			return;
		}
		if(JmxRecordCommand.getLastRecordResult() == null) {
			TuiDialogs.showMessageDialog(
				gui,
				TuiRecordUiKeys.DUMP_NO_DATA_TITLE,
				TuiRecordUiKeys.DUMP_NO_DATA_MESSAGE,
				MessageDialogButton.OK
			);
			return;
		}
		if(ctx.getSelectedThreadIds().isEmpty()) {
			TuiDialogs.showMessageDialog(
				gui,
				TuiRecordUiKeys.BULK_THREAD_ANALYSIS_CONFIRM_TITLE,
				TuiRecordUiKeys.bulkThreadAnalysisNoneSelectedMessage(),
				MessageDialogButton.OK
			);
			return;
		}
		String expr = RecordUiMode.buildSelectedThreadsExpression(ctx);
		if(expr == null || expr.trim().length() == 0) {
			return;
		}
		executeCommandFromListSelection("RECORD VIEW THREAD " + expr);
	}

	private void showRecordViewFromUi(JmxTuiModeContext ctx, TuiRecordViewDialog.ViewType viewType) {
		if(ctx == null || !ctx.isRecordMode() || ctx.getRecordViewState() != RecordViewState.DUMP || viewType == null) {
			return;
		}
		if(JmxRecordCommand.getLastRecordResult() == null) {
			TuiDialogs.showMessageDialog(
				gui,
				TuiRecordUiKeys.DUMP_NO_DATA_TITLE,
				TuiRecordUiKeys.DUMP_NO_DATA_MESSAGE,
				MessageDialogButton.OK
			);
			return;
		}
		if(ctx.getSelectedThreadIds().isEmpty()) {
			TuiDialogs.showMessageDialog(
				gui,
				TuiRecordUiKeys.BULK_THREAD_ANALYSIS_CONFIRM_TITLE,
				TuiRecordUiKeys.bulkThreadAnalysisNoneSelectedMessage(),
				MessageDialogButton.OK
			);
			return;
		}
		TuiRecordViewDialog.ViewParams params = TuiRecordViewDialog.show(gui, viewType, JmxRecordCommand.getLastRecordResult());
		if(params == null) {
			return;
		}
		String threadExpr = RecordUiMode.buildSelectedThreadsExpression(ctx);
		if(threadExpr == null || threadExpr.trim().length() == 0) {
			return;
		}
		String command = buildRecordViewCommand(viewType, threadExpr, params);
		if(command == null || command.length() == 0) {
			return;
		}
		executeCommandFromListSelection(command);
	}

	private static String buildRecordViewCommand(
			TuiRecordViewDialog.ViewType viewType,
			String threadExpr,
			TuiRecordViewDialog.ViewParams params) {
		if(viewType == null || threadExpr == null || params == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		if(viewType == TuiRecordViewDialog.ViewType.METHOD) {
			sb.append("RECORD VIEW METHOD ").append(threadExpr);
			if(params.expression.length() > 0) {
				sb.append(" ").append(params.expression);
			}
		} else if(viewType == TuiRecordViewDialog.ViewType.THREAD) {
			sb.append("RECORD VIEW THREAD ").append(threadExpr);
			if(params.expression.length() > 0) {
				sb.append(" ").append(params.expression);
			}
			if(params.mergeThreads) {
				sb.append(" --merge");
			}
		} else if(viewType == TuiRecordViewDialog.ViewType.STACK) {
			sb.append("RECORD VIEW STACK ").append(threadExpr).append(" ").append(params.expression);
		} else {
			sb.append("RECORD VIEW SEARCH ").append(threadExpr).append(" \"").append(params.expression).append("\"");
		}
		return sb.toString();
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
		TuiDetailDialog.show(gui, resolveF2MbeanDetailTitle(objectName), detailText);
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
		TuiDetailDialog.show(gui, resolveF2FeatureDetailTitle(objectName, feature), detailText);
		return true;
	}

	private String resolveF2MbeanDetailTitle(String objectName) {
		try {
			MBeanConnector connector = executor.getMBeanConnector();
			if(connector != null && connector.getMBeanConnection() != null) {
				ObjectName oName = new ObjectName(objectName);
				MBeanInfo mbeanInfo = connector.getMBeanConnection().getMBeanInfo(oName);
				boolean knowledgeUsed = MBeanKnowledgeResolver.getInstance().hasKnowledgeContributionForMBean(oName, mbeanInfo);
				return KnowledgeConstant.formatTuiDetailTitle("MBean", knowledgeUsed);
			}
		} catch(Exception e) {}
		return "MBean";
	}

	private String resolveF2FeatureDetailTitle(String objectName, MBeanFeatureInfo feature) {
		return resolveFeatureKnowledgeTitle("Feature", objectName, feature);
	}

	private String resolveFeatureKnowledgeTitle(String baseTitle, String objectName, MBeanFeatureInfo feature) {
		try {
			MBeanConnector connector = executor.getMBeanConnector();
			if(connector != null && connector.getMBeanConnection() != null) {
				ObjectName oName = new ObjectName(objectName);
				MBeanInfo mbeanInfo = connector.getMBeanConnection().getMBeanInfo(oName);
				boolean knowledgeUsed = MBeanKnowledgeResolver.getInstance().hasKnowledgeContributionForFeature(oName, mbeanInfo, feature);
				return KnowledgeConstant.formatTuiDetailTitle(baseTitle, knowledgeUsed);
			}
		} catch(Exception e) {}
		return baseTitle;
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
	 * Features 패널에서 속성 Invoke Feature: 읽기 전용은 조회, 쓰기 전용은 값 입력 후 set, 읽기+쓰기는 동작을 먼저 선택한다.
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
		String prompt = buildAttributeWritePrompt(objectName, attribute);
		String value = TuiDialogs.showTextInputDialog(
			gui,
			resolveFeatureKnowledgeTitle("Attribute value", objectName, attribute),
			prompt,
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

	private String buildAttributeWritePrompt(String objectName, MBeanAttributeInfo attribute) {
		StringBuilder prompt = new StringBuilder();
		prompt.append("Input new value for ").append(attribute.getName())
			.append(" (").append(attribute.getType()).append("):");
		try {
			MBeanServerConnection conn = executor.getMBeanConnector().getMBeanConnection();
			ObjectName oName = new ObjectName(objectName);
			MBeanInfo mbeanInfo = conn.getMBeanInfo(oName);
			MBeanKnowledgeResolver resolver = MBeanKnowledgeResolver.getInstance();
			String description = resolver.resolveKnowledgeAttributeDescription(oName, mbeanInfo, attribute);
			if(description != null && description.trim().length() > 0) {
				prompt.append("\n").append(description);
			}
			String example = resolver.resolveAttributeExample(oName, mbeanInfo, attribute);
			if(example != null && example.trim().length() > 0) {
				prompt.append("\nExample: ").append(example);
			}
		} catch(Exception e) {}
		return prompt.toString();
	}

	private void refreshFeatures(final JmxTuiModeContext ctx, String objectName) {
		FocusActionListBox featureList = ctx.getFeatureList();
		List<MBeanFeatureInfo> currentFeatures = ctx.getCurrentFeatures();
		String previousObjectName = ctx.getFeaturesForObjectName();
		int previousIndex = getSelectedIndexSafe(featureList);
		boolean preserveSelection = objectName != null && objectName.equals(previousObjectName)
			&& previousIndex > 0 && !currentFeatures.isEmpty();
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
								MBeanServerConnection conn = executor.getMBeanConnector().getMBeanConnection();
								String[] argValues = TuiOperationArgumentsDialog.show(gui, conn, objectName, op);
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
			if(preserveSelection && !currentFeatures.isEmpty()) {
				setSelectedIndexSafe(featureList, Math.min(previousIndex, currentFeatures.size() - 1));
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
