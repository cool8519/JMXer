package dal.tool.trace.jmxer.tui;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanFeatureInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ObjectName;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TerminalTextUtils;
import com.googlecode.lanterna.gui2.Border;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LayoutData;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.LinearLayout.Alignment;
import com.googlecode.lanterna.gui2.LinearLayout.GrowPolicy;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.TextBox.Style;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

import dal.tool.cli.History;
import dal.tool.cli.Logger;
import dal.tool.trace.jmxer.JMXControl;
import dal.tool.trace.jmxer.JMXerConstant;
import dal.tool.trace.jmxer.cli.JmxCommandExecutor;
import dal.tool.trace.jmxer.cli.JmxSettings;
import dal.tool.trace.jmxer.util.JMXPrintUtil;
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
	/** Left prompt for the MBean Search input row (same style as Command {@code prompt + "> "}). */
	private static final String MBEAN_SEARCH_PROMPT_LABEL = "Filter> ";
	private static final String ATTR_ACTION_READ = "Read";
	private static final String ATTR_ACTION_WRITE = "Write";
	private static final int MIN_FEATURES_COLS = 20;
	private static final int MIN_MBEANS_COLS = 28;
	private static final int HORIZONTAL_SPLIT_STEP_COLS = 2;
	private static final int VERTICAL_SPLIT_STEP_ROWS = 1;
	private static final int MIN_OUTPUT_OUTER_ROWS = 3;

	private final WindowBasedTextGUI gui;
	private final JmxCommandExecutor executor;
	private final String prompt;
	private final Runnable closeAction;

	private final Panel root;
	private final Label titleLabel;
	private final Panel statusInner;
	private final Panel listsPanel;
	private final Panel mbeansColumnPanel;
	private final Component mbeanSearchBordered;
	private final Component mbeansListBordered;
	private final Component featuresBordered;
	private final Label header1;
	private final Label header2;
	private final Label header3;
	private final Label headerKeysGlobal;
	private final Label headerKeysPane;

	private final Panel mbeanSearchInner;
	private final Panel mbeansInner;
	private final Panel featuresInner;
	private final Panel outputInner;
	private final Panel commandInner;

	private final MbeanSearchInputBox mbeanSearch;
	private final FocusActionListBox mbeanList;
	private final FocusActionListBox featureList;
	private final OutputTextBox output;
	private final CommandInputBox input;
	private final HighlightArrowListItemRenderer mbeanListRenderer;
	private final Panel inputRow;
	private final Panel mbeanSearchRow;

	private final JmxTuiOutputSink outputSink;
	private final BoxFocusManager focusManager;

	/** Titled border wrapping {@link #outputInner}; recreated when the title time is updated. */
	private Border outputPaneBorder;

	/** Full MBean ObjectName list from the executor (unfiltered). */
	private volatile List<String> currentMbeanNames = new ArrayList<String>();
	/** ObjectNames currently shown in the MBeans list (after search filter). */
	private final List<String> displayedMbeanNames = new ArrayList<String>();
	/** ObjectName used to build the current Features list (F2 detail). */
	private volatile String featuresForObjectName;
	/** Features parallel to {@link #featureList} items (F2 detail). */
	private final List<MBeanFeatureInfo> currentFeatures = new ArrayList<MBeanFeatureInfo>();

	private volatile boolean featuresVisible = true;
	private volatile TerminalSize lastTerminalSize = null;
	/** Output TextBox inner column count from the latest {@link #applyTerminalSize}. */
	private volatile int outputInnerCols = 10;
	/** Status pane inner column count from the latest {@link #applyTerminalSize}. */
	private volatile int statusInnerCols = 10;

	private int historyCursor = -1;
	private String historyScratch = "";

	/** User width for Search/MBeans column; {@code <0} uses default 3:2 split when Features is visible. */
	private int userMbeansCols = -1;
	/** User outer height for the lists band (Search+MBeans+Features); {@code <0} uses default formula. */
	private int userListsBandOuterRows = -1;

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
		this.header2 = new Label("");
		this.header3 = new Label("");
		this.headerKeysGlobal = new Label("");
		this.headerKeysPane = new Label("");

		this.mbeanSearchInner = new Panel(new LinearLayout(Direction.VERTICAL));
		this.mbeansInner = new Panel(new LinearLayout(Direction.VERTICAL));
		this.featuresInner = new Panel(new LinearLayout(Direction.VERTICAL));
		this.outputInner = new Panel(new LinearLayout(Direction.VERTICAL));
		this.commandInner = new Panel(new LinearLayout(Direction.VERTICAL));

		this.mbeanList = new FocusActionListBox(gui, focusManager, mbeansInner, "MBean");
		this.featureList = new FocusActionListBox(gui, focusManager, featuresInner, "Feature");
		this.mbeanList.setSelectionChangedHandler(new Runnable() {
			@Override
			public void run() {
				syncFeaturesToMBeanSelection();
			}
		});

		this.mbeanListRenderer = new HighlightArrowListItemRenderer();
		this.mbeanList.setListItemRenderer(mbeanListRenderer);
		this.featureList.setListItemRenderer(new ArrowListItemRenderer());

		this.mbeanSearch = new MbeanSearchInputBox(focusManager, mbeanSearchInner, new TerminalSize(1, 1));
		this.mbeanSearch.setCaretWarp(true);
		this.mbeanSearch.setVerticalFocusSwitching(false);
		this.mbeanSearch.setHorizontalFocusSwitching(false);
		this.mbeanSearch.setTheme(JmxTuiTheme.UNFOCUSED_TEXTBOX_THEME);

		this.mbeanSearchRow = new Panel(new LinearLayout(Direction.HORIZONTAL));
		this.mbeanSearchRow.addComponent(new Label(MBEAN_SEARCH_PROMPT_LABEL));
		this.mbeanSearchRow.addComponent(mbeanSearch, LinearLayout.createLayoutData(Alignment.Fill, GrowPolicy.CanGrow));
		this.mbeanSearchInner.addComponent(this.mbeanSearchRow, LinearLayout.createLayoutData(Alignment.Fill, GrowPolicy.CanGrow));

		this.mbeansInner.addComponent(this.mbeanList, LinearLayout.createLayoutData(Alignment.Fill, GrowPolicy.CanGrow));
		this.featuresInner.addComponent(this.featureList, LinearLayout.createLayoutData(Alignment.Fill, GrowPolicy.CanGrow));

		this.output = new OutputTextBox(focusManager, outputInner, new TerminalSize(1, 1), Style.MULTI_LINE, this);
		this.output.setReadOnly(true);
		// Force default colors for initial render (before any focus event occurs).
		this.output.setTheme(JmxTuiTheme.UNFOCUSED_TEXTBOX_THEME);
		this.outputInner.addComponent(this.output, LinearLayout.createLayoutData(Alignment.Fill, GrowPolicy.CanGrow));
		this.output.setSaveOutputAction(new Runnable() {
			@Override
			public void run() {
				copyOutputToFile();
			}
		});
		this.output.setWrapModeChangeListener(new Runnable() {
			@Override
			public void run() {
				refreshHeader();
			}
		});

		this.input = new CommandInputBox(focusManager, commandInner, new TerminalSize(1, 1));
		this.input.setCaretWarp(true);
		// Keep focus inside the command line; pane switching is Tab-only (see FocusActionListBox / Lanterna defaults).
		this.input.setVerticalFocusSwitching(false);
		this.input.setHorizontalFocusSwitching(false);
		// Force default colors for initial render (before any focus event occurs).
		this.input.setTheme(JmxTuiTheme.UNFOCUSED_TEXTBOX_THEME);

		this.outputSink = new JmxTuiOutputSink(gui, output, this);

		this.inputRow = new Panel(new LinearLayout(Direction.HORIZONTAL));
		this.inputRow.addComponent(new Label(prompt + "> "));
		this.inputRow.addComponent(input, LinearLayout.createLayoutData(Alignment.Fill, GrowPolicy.CanGrow));
		this.commandInner.addComponent(this.inputRow, LinearLayout.createLayoutData(Alignment.Fill, GrowPolicy.CanGrow));

		focusManager.register(mbeanSearchInner);
		focusManager.register(mbeansInner);
		focusManager.register(featuresInner);
		focusManager.register(outputInner);
		focusManager.register(commandInner);
		focusManager.registerFillHighlight(mbeanSearchInner);
		focusManager.registerFillHighlight(outputInner);
		focusManager.registerFillHighlight(commandInner);
		// Clear any pane fill highlight before first focus event arrives.
		focusManager.setActive(null);

		this.statusInner = new Panel(new LinearLayout(Direction.VERTICAL));
		this.listsPanel = new Panel(new LinearLayout(Direction.HORIZONTAL).setSpacing(1));
		this.mbeanSearchBordered = mbeanSearchInner.withBorder(Borders.singleLine("Search"));
		this.mbeansListBordered = mbeansInner.withBorder(Borders.singleLine("MBeans"));
		this.mbeansColumnPanel = new Panel(new LinearLayout(Direction.VERTICAL));
		this.mbeansColumnPanel.addComponent(mbeanSearchBordered);
		this.mbeansColumnPanel.addComponent(mbeansListBordered, LinearLayout.createLayoutData(Alignment.Fill, GrowPolicy.CanGrow));
		this.featuresBordered = featuresInner.withBorder(Borders.singleLine("Features"));
		this.root = buildLayout();
		wireKeyBindings();
		wireMbeanSearchBindings();
		refreshHeader();
		refreshMBeans();
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

	public Panel getRoot() {
		return root;
	}

	private void runCloseAction() {
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
		if(mbeanList.isFocused()) {
			return showMbeanDetail();
		}
		if(featureList.isFocused()) {
			return showFeatureDetail();
		}
		if(output.isFocused()) {
			return output.onF2Key();
		}
		if(input.isFocused()) {
			return input.onF2Key();
		}
		if(mbeanSearch.isFocused()) {
			return mbeanSearch.onF2Key();
		}
		return false;
	}

	private void toggleFeaturesPane() {
		setFeaturesPaneVisible(!featuresVisible);
	}

	private void ensureFeaturesPaneVisible() {
		setFeaturesPaneVisible(true);
	}

	private void setFeaturesPaneVisible(boolean visible) {
		if(featuresVisible == visible) {
			return;
		}
		featuresVisible = visible;
		featuresBordered.setVisible(visible);
		if(!visible && featureList.isFocused()) {
			mbeanList.takeFocus();
		}
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
		return featuresVisible;
	}

	/** Up/down split: lists band vs Output (requires stacked resizable band above Command). */
	private boolean canResizeVerticalSplit() {
		return listsPanel != null && outputPaneBorder != null;
	}

	private void adjustHorizontalSplit(int deltaCols) {
		int gap = 1;
		int availableCols = Math.max(20, lastTerminalSize.getColumns() - gap);
		int maxMbeansCols = availableCols - MIN_FEATURES_COLS;
		int current = userMbeansCols >= 0 ? userMbeansCols : computeDefaultMbeansCols(availableCols);
		int next = clamp(current + deltaCols, MIN_MBEANS_COLS, maxMbeansCols);
		if(next == current) {
			return;
		}
		userMbeansCols = next;
		applyTerminalSize(lastTerminalSize);
	}

	private void adjustVerticalSplit(int deltaRows) {
		int available = computeAvailableForListAndOutput(lastTerminalSize);
		int minListsBand = computeMinListsBandOuterRows();
		int maxListsBand = available - MIN_OUTPUT_OUTER_ROWS;
		int current = userListsBandOuterRows >= 0 ? userListsBandOuterRows : computeDefaultListsBandOuterRows(available);
		int next = clamp(current + deltaRows, minListsBand, maxListsBand);
		if(next == current) {
			return;
		}
		userListsBandOuterRows = next;
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

	private static int computeDefaultMbeansCols(int availableCols) {
		return clamp((availableCols * 3) / 5, MIN_MBEANS_COLS, Math.max(MIN_MBEANS_COLS, availableCols - MIN_FEATURES_COLS));
	}

	private int computeDefaultListsBandOuterRows(int availableForListAndOutput) {
		int mbeanSearchOuterH = 1 + BORDER_V_OVERHEAD;
		int listRowsInner = clamp(availableForListAndOutput / 4, MIN_LIST_ROWS,
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
			Thread guiThread = gui.getGUIThread().getThread();
			if(guiThread != null && Thread.currentThread() == guiThread) {
				action.run();
			} else {
				gui.getGUIThread().invokeAndWait(action);
			}
		} catch(Exception e) {
			try {
				gui.getGUIThread().invokeLater(action);
			} catch(Exception e2) {}
		}
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

		listsPanel.addComponent(mbeansColumnPanel, LinearLayout.createLayoutData(Alignment.Fill, GrowPolicy.CanGrow));
		listsPanel.addComponent(featuresBordered, LinearLayout.createLayoutData(Alignment.Fill, GrowPolicy.CanGrow));
		main.addComponent(listsPanel);

		this.outputPaneBorder = outputInner.withBorder(Borders.singleLine("Output"));
		main.addComponent(this.outputPaneBorder, LinearLayout.createLayoutData(Alignment.Fill, GrowPolicy.CanGrow));

		main.addComponent(commandInner.withBorder(Borders.singleLine("Command")));

		return main;
	}

	public void applyTerminalSize(TerminalSize size) {
		if(size == null) {
			return;
		}
		lastTerminalSize = size;

		int cols = Math.max(20, size.getColumns());

		String titleText = JMXerConstant.JMXER_VERSION_STRING + " (TUI)";
		int titlePad = Math.max(0, (cols - TerminalTextUtils.getColumnWidth(titleText)) / 2);
		titleLabel.setText(spaces(titlePad) + titleText);
		titleLabel.setPreferredSize(new TerminalSize(cols, 1));

		int gap = featuresVisible ? 1 : 0;
		int availableCols = Math.max(20, cols - gap);
		int mbeansCols;
		int featuresCols;
		if(featuresVisible) {
			if(userMbeansCols >= 0) {
				mbeansCols = clamp(userMbeansCols, MIN_MBEANS_COLS, Math.max(MIN_MBEANS_COLS, availableCols - MIN_FEATURES_COLS));
			} else {
				mbeansCols = computeDefaultMbeansCols(availableCols);
			}
			featuresCols = Math.max(MIN_FEATURES_COLS, availableCols - mbeansCols);
			mbeansCols = availableCols - featuresCols;
		} else {
			mbeansCols = cols;
			featuresCols = 0;
		}

		int titleH = 1;
		int statusOuterH = HEADER_INNER_LINES + BORDER_V_OVERHEAD;
		int cmdOuterH = 1 + BORDER_V_OVERHEAD;
		int mbeanSearchOuterH = 1 + BORDER_V_OVERHEAD;

		int availableForListAndOutput = computeAvailableForListAndOutput(size);
		int minListsBandOuterRows = computeMinListsBandOuterRows();
		int maxListsBandOuterRows = availableForListAndOutput - MIN_OUTPUT_OUTER_ROWS;
		int listsBandOuterH;
		if(userListsBandOuterRows >= 0) {
			listsBandOuterH = clamp(userListsBandOuterRows, minListsBandOuterRows, maxListsBandOuterRows);
		} else {
			listsBandOuterH = computeDefaultListsBandOuterRows(availableForListAndOutput);
		}

		int mbeansListOuterH = Math.max(MIN_LIST_ROWS + BORDER_V_OVERHEAD, listsBandOuterH - mbeanSearchOuterH);
		int listRowsInner = clamp(mbeansListOuterH - BORDER_V_OVERHEAD, MIN_LIST_ROWS, MAX_LIST_ROWS);
		int mbeansColumnOuterH = mbeanSearchOuterH + mbeansListOuterH;
		int outputOuterH = availableForListAndOutput - mbeansColumnOuterH;
		if(outputOuterH < MIN_OUTPUT_OUTER_ROWS) {
			outputOuterH = MIN_OUTPUT_OUTER_ROWS;
			mbeansColumnOuterH = availableForListAndOutput - outputOuterH;
			mbeansListOuterH = mbeansColumnOuterH - mbeanSearchOuterH;
			listRowsInner = clamp(mbeansListOuterH - BORDER_V_OVERHEAD, MIN_LIST_ROWS, MAX_LIST_ROWS);
		}
		if(userMbeansCols >= 0 && featuresVisible) {
			userMbeansCols = mbeansCols;
		}
		if(userListsBandOuterRows >= 0) {
			userListsBandOuterRows = mbeansColumnOuterH;
		}

		mbeansColumnPanel.setPreferredSize(new TerminalSize(mbeansCols, mbeansColumnOuterH));
		mbeanSearchBordered.setPreferredSize(new TerminalSize(mbeansCols, mbeanSearchOuterH));
		mbeansListBordered.setPreferredSize(new TerminalSize(mbeansCols, mbeansListOuterH));
		featuresBordered.setPreferredSize(new TerminalSize(featuresCols, mbeansColumnOuterH));

		int listInnerWLeft = Math.max(8, mbeansCols - BORDER_V_OVERHEAD);
		int listInnerWRight = Math.max(8, featuresCols - BORDER_V_OVERHEAD);
		int mbeanSearchInnerW = listInnerWLeft;
		int mbeanSearchPromptCols = MBEAN_SEARCH_PROMPT_LABEL.length();
		int mbeanSearchInputCols = Math.max(10, mbeanSearchInnerW - mbeanSearchPromptCols);

		mbeanSearch.setPreferredSize(new TerminalSize(mbeanSearchInputCols, 1));
		mbeanSearchRow.setPreferredSize(new TerminalSize(mbeanSearchInnerW, 1));
		mbeanSearchInner.setPreferredSize(new TerminalSize(mbeanSearchInnerW, 1));

		mbeanList.setPreferredSize(new TerminalSize(listInnerWLeft, listRowsInner));
		if(featuresVisible) {
			featureList.setPreferredSize(new TerminalSize(listInnerWRight, listRowsInner));
		}

		int outputInnerH = Math.max(1, outputOuterH - BORDER_V_OVERHEAD);
		outputInnerCols = Math.max(10, cols - BORDER_V_OVERHEAD);
		output.setPreferredSize(new TerminalSize(outputInnerCols, outputInnerH));

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
		listsPanel.setPreferredSize(new TerminalSize(cols, mbeansColumnOuterH));
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
		outputSink.clearPartialLine();
		output.clearAll();
		resetOutputPaneBorderTitle();
	}

	/**
	 * Border title is immutable in Lanterna; replace the border component with the same inner panel and a new title.
	 */
	private void tryRedockOutputBorder(String borderTitle) {
		if(root == null || outputPaneBorder == null || borderTitle == null) {
			return;
		}
		try {
			LayoutData ld = outputPaneBorder.getLayoutData();
			if(ld == null) {
				ld = LinearLayout.createLayoutData(Alignment.Fill, GrowPolicy.CanGrow);
			}
			List<Component> list = root.getChildrenList();
			int idx = list.indexOf(outputPaneBorder);
			if(idx < 0) {
				return;
			}
			root.removeComponent(outputPaneBorder);
			outputPaneBorder = outputInner.withBorder(Borders.singleLine(borderTitle));
			outputPaneBorder.setLayoutData(ld);
			root.addComponent(idx, outputPaneBorder);
			root.invalidate();
		} catch(Exception e) {}
	}

	private void resetOutputPaneBorderTitle() {
		tryRedockOutputBorder("Output");
	}

	@Override
	public void refreshOutputPaneBorderTime() {
		tryRedockOutputBorder("Output [" + LocalTime.now().format(OUTPUT_PANE_TIME_FMT) +"]");
	}

	/** Column width of the Output text area at the current moment (after the latest layout / resize). */
	@Override
	public int getOutputInnerColumnsAtWrite() {
		try {
			TerminalSize sz = output.getSize();
			if(sz != null && sz.getColumns() >= 8) {
				return sz.getColumns();
			}
		} catch(Exception e) {}
		int cached = outputInnerCols;
		if(cached >= 8) {
			return cached;
		}
		TerminalSize ts = lastTerminalSize;
		if(ts != null) {
			return Math.max(10, ts.getColumns() - BORDER_V_OVERHEAD);
		}
		return 10;
	}

	private void copyOutputToFile() {
		try {
			String nl = System.getProperty("line.separator", "\n");
			StringBuilder sb = new StringBuilder();
			List<String> lines = output.getLogicalLines();
			for(int i = 0; i < lines.size(); i++) {
				if(i > 0) {
					sb.append(nl);
				}
				String line = lines.get(i);
				sb.append(line == null ? "" : OutputTextFormat.expandTabs(line));
			}

			Path baseDir = Paths.get(System.getProperty("user.dir", "."));
			String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
			long ms = System.currentTimeMillis();
			Path outFile = baseDir.resolve("JMXer_Output_" + ts + "_" + ms + ".txt");
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

	private void wireMbeanSearchBindings() {
		mbeanSearch.setTextChangedListener(new Runnable() {
			@Override
			public void run() {
				applyMbeanSearchFilter();
			}
		});
		mbeanSearch.setKeyHandler(new MbeanSearchKeyHandler() {
			@Override
			public boolean handle(KeyStroke key) {
				if(key == null) {
					return false;
				}
				if(JmxTuiKeys.isGlobalShortcut(key)) {
					return false;
				}
				if(key.getKeyType() == KeyType.Escape) {
					mbeanSearch.setText("");
					return true;
				}
				return false;
			}
		});
	}

	private void applyMbeanSearchFilter() {
		String query = mbeanSearch.getText();
		if(query == null) {
			query = "";
		}
		mbeanListRenderer.setHighlightQuery(query);

		String previousSelection = resolveSelectedMbeanObjectName();
		mbeanList.clearItems();
		displayedMbeanNames.clear();

		List<String> names = currentMbeanNames;
		if(names == null || names.isEmpty()) {
			mbeanList.addItem("(no ObjectName list)", new Runnable() {
				@Override
				public void run() {}
			});
			mbeanList.invalidate();
			syncFeaturesToMBeanSelection();
			return;
		}

		boolean noFilter = query.trim().isEmpty();
		String lowerQuery = query.toLowerCase();
		for(final String o : names) {
			if(noFilter || o.toLowerCase().contains(lowerQuery)) {
				displayedMbeanNames.add(o);
				mbeanList.addItem(o, new Runnable() {
					@Override
					public void run() {
						selectMBean(o);
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
		syncFeaturesToMBeanSelection();
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
		TuiDialogs.showMessageDialog(
			gui,
			"Help",
			"[Global]\n"
				+ "F1: this help\n"
				+ "Tab / Shift+Tab: move focus between windows\n"
				+ "Ctrl+F: toggle 'Features' window\n"
				+ "Shift+Left/Right: resize Search/MBeans vs Features width (when Features visible)\n"
				+ "Shift+Up/Down: resize lists vs Output height\n"
				+ "F2: context action (depends on focused pane)\n"
				+ "Ctrl+C: exit\n\n"
				+ "[Window — depends on focus]\n"
				+ "Search: type to filter MBeans list; Esc — clear search\n"
				+ "MBeans: arrows, PgUp/Dn, Home/End — navigate; Enter — set OBJECT; F2 — information\n"
				+ "Features: arrows, PgUp/Dn, Home/End — navigate; Enter — CALL; F2 — information\n"
				+ "Output: arrows & scroll; Ctrl+O save; Ctrl+L wrap/line toggle\n"
				+ "Command: type JMXer commands; Up/Down — history; Esc — clear line",
			MessageDialogButton.OK
		);
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
		JmxSettings s = executor.getSettings();
		String mode = JMXControl.isAnalyzeMode ? "ANALYZE" : "ATTACH";
		String uiMode = "JMX";
		String conn = (executor.getMBeanConnector() != null && executor.getMBeanConnector().isInitialized()) ? "OK" : "N/A";
		String connTarget = "";
		if(executor.getMBeanConnector() != null) {
			connTarget = executor.getMBeanConnector().getConnectType().name() + ":" + executor.getMBeanConnector().getTargetName();
		}
		header1.setText(TuiStatusText.fitSingleLine(
			" Execution Mode: " + mode + (JMXControl.isAnalyzeMode ? "" : ("(" + connTarget + ")") + " | UI Mode: " + uiMode),
			statusInnerCols));
		String mbeanName = s.getMBeanObjectName() == null ? "" : s.getMBeanObjectName();
		header2.setText(TuiStatusText.fitSingleLine("   - Current MBean: " + mbeanName, statusInnerCols));
		if(uiMode.equals("JMX")) {
			header3.setText(TuiStatusText.fitSingleLine(
				"   - Pretty Print : " + (s.prettyPrintInResult() ? "ON" : "OFF"), statusInnerCols));
		} else if(uiMode.equals("Analyze")) {
			header3.setText(TuiStatusText.fitSingleLine(
				"   - REC_VIEW=" + s.getRecordViewMode().name() + ", EMPTY_THREAD=" + (s.showEmptyThreadInRecordView() ? "ON" : "OFF"),
				statusInnerCols));
		}
		headerKeysGlobal.setText(TuiStatusText.fitSingleLine(
			" [Global] F1 Help | Tab Move Window | Shift+Arrows Resize | F2 Detail | Ctrl+C Exit | Ctrl+F Features(" + (featuresVisible ? "ON" : "OFF") + ")",
			statusInnerCols));
		headerKeysPane.setText(TuiStatusText.fitSingleLine(
			" [Window] MBeans: Enter OBJECT, F2 Overview | Features: Enter CALL, F2 Info | Output: Ctrl+O Save, Ctrl+L Wrap(" + (output.isWrapMode() ? "ON" : "OFF") + ")",
			statusInnerCols));
	}

	private void refreshMBeans() {
		clearFeatures();

		List<String> names = executor.getObjectNameList();
		if(names == null) {
			currentMbeanNames = new ArrayList<String>();
			applyMbeanSearchFilter();
			return;
		}

		currentMbeanNames = names;
		applyMbeanSearchFilter();
	}

	private void clearFeatures() {
		featureList.clearItems();
		currentFeatures.clear();
		featuresForObjectName = null;
	}

	private void syncFeaturesToMBeanSelection() {
		String objectName = resolveSelectedMbeanObjectName();
		if(objectName != null && objectName.length() > 0) {
			if(!featuresVisible) {
				return;
			}
			refreshFeatures(objectName);
		} else {
			clearFeatures();
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

	private void selectMBean(String objectName) {
		outputSink.prepareImplicitCommandOutput();
		try {
			executor.processCommandForTui("OBJECT \"" + objectName + "\"");
		} catch(Exception e) {
			Logger.logStackTrace(e);
			Logger.logln(Logger.Level.ERROR, "Failed to set OBJECT: " + e.getMessage());
		}
		ensureFeaturesPaneVisible();
		refreshHeader();
		refreshFeatures(objectName);
		featureList.takeFocus();
	}

	private void executeCommandFromListSelection(String commandLine) {
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

	private boolean showMbeanDetail() {
		String objectName = resolveSelectedMbeanObjectName();
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

	private boolean showFeatureDetail() {
		int idx = getSelectedIndexSafe(featureList);
		String objectName = featuresForObjectName;
		if(objectName == null || objectName.length() == 0) {
			objectName = resolveSelectedMbeanObjectName();
		}
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

	private String resolveSelectedMbeanObjectName() {
		if(displayedMbeanNames.isEmpty()) {
			return null;
		}
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
		String cmd = "CALL \"" + objectName + "\" " + attribute.getName() + " \"" + value + "\"";
		executeCommandFromListSelection(cmd);
	}

	private void refreshFeatures(String objectName) {
		featureList.clearItems();
		currentFeatures.clear();
		featuresForObjectName = objectName;
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
							if(op.getSignature() != null && op.getSignature().length > 0) {
								String args = TuiDialogs.showTextInputDialog(
									gui,
									"Arguments",
									"Input arguments for " + op.getName() + " (comma separated, array=[a,b]):",
									"");
								if(args == null) {
									return;
								}
								args = args.trim();
								if(args.length() == 0) {
									cmd = "CALL \"" + objectName + "\" " + op.getName();
								} else {
									cmd = "CALL \"" + objectName + "\" " + op.getName() + " \"" + args + "\"";
								}
							} else {
								cmd = "CALL \"" + objectName + "\" " + op.getName();
							}
							if(cmd != null && cmd.trim().length() > 0) {
								executeCommandFromListSelection(cmd);
							}
						}
					});
				}
			}
		} catch(Exception e) {
			currentFeatures.clear();
			featureList.addItem("(failed to load features)", new Runnable() {
				@Override
				public void run() {}
			});
			Logger.logln(Logger.Level.ERROR, "Failed to load feature list: " + e.getMessage());
		}
	}

}
