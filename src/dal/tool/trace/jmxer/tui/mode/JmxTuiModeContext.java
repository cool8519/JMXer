package dal.tool.trace.jmxer.tui.mode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanFeatureInfo;

import com.googlecode.lanterna.gui2.Border;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.LinearLayout.Alignment;
import com.googlecode.lanterna.gui2.LinearLayout.GrowPolicy;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox.Style;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.TerminalSize;

import dal.tool.trace.jmxer.tui.app.JmxTuiApp;
import dal.tool.trace.jmxer.tui.output.OutputTextBox;
import dal.tool.trace.jmxer.tui.support.BoxFocusManager;
import dal.tool.trace.jmxer.tui.support.JmxTuiTheme;
import dal.tool.trace.jmxer.tui.support.ThreadPaneTableFormat;
import dal.tool.trace.jmxer.tui.support.TuiStatusText;
import dal.tool.trace.jmxer.tui.widget.ArrowListItemRenderer;
import dal.tool.trace.jmxer.tui.widget.CommandInputBox;
import dal.tool.trace.jmxer.tui.widget.FocusActionListBox;
import dal.tool.trace.jmxer.tui.widget.HighlightArrowListItemRenderer;
import dal.tool.trace.jmxer.tui.widget.MbeanSearchInputBox;
import dal.tool.trace.jmxer.tui.widget.ThreadSelectListItemRenderer;

/**
 * Per-UI-mode pane and widget instances. Panes with the same border title in different modes
 * are independent objects so selection, scroll, filter, and output content do not leak across modes.
 */
public final class JmxTuiModeContext {
	public static final String SEARCH_PROMPT_LABEL = "Filter> ";
	public static final String THREAD_LIST_BORDER_TITLE = "Thread";
	public static final String THREAD_UNAVAILABLE_PLACEHOLDER = "(threads unavailable in offline session)";

	private static final String MBEAN_SEARCH_PROMPT_LABEL = SEARCH_PROMPT_LABEL;

	private final Panel mbeanSearchInner;
	private final Panel mbeansInner;
	private final Panel featuresInner;
	private final Panel outputInner;
	private final Panel listsPanel;
	private final Panel mbeansColumnPanel;
	private final Panel mbeanSearchRow;

	private final MbeanSearchInputBox mbeanSearch;
	private final FocusActionListBox mbeanList;
	private final FocusActionListBox featureList;
	private final OutputTextBox output;
	private final HighlightArrowListItemRenderer mbeanListRenderer;
	private final ThreadSelectListItemRenderer threadListRenderer;
	private final Label threadHeaderLabel;
	private final boolean threadMode;
	private final boolean mbeanUiMode;

	private final Map<Long, Long> prevThreadCpuNanos = new HashMap<Long, Long>();
	private final Map<Long, Long> prevThreadAllocatedBytes = new HashMap<Long, Long>();
	private final Map<Long, String> prevThreadNames = new HashMap<Long, String>();
	private long prevSnapshotWallNanos;
	private boolean hasPreviousThreadSnapshot;
	private String selectedExpression = "";
	private final Set<Long> activeModePreservedThreadIds = new HashSet<Long>();
	private String activeModePreservedExpression = "";
	private int lastCreatedThreadCount;
	private int lastDestroyedThreadCount;
	private boolean threadAutoRefreshEnabled = true;
	private int threadRefreshIntervalSeconds = 5;
	private int threadHeaderCols;
	private String threadHeaderFullText;

	private final Component mbeanSearchBordered;
	private Component mbeansListBordered;
	private final Component featuresBordered;
	private Border outputPaneBorder;

	private final List<String> displayedMbeanNames = new ArrayList<String>();
	private final List<ThreadSnapshotEntry> allThreadEntries = new ArrayList<ThreadSnapshotEntry>();
	private final List<Long> displayedThreadIds = new ArrayList<Long>();
	private final Set<Long> selectedThreadIds = new HashSet<Long>();
	private final List<MBeanFeatureInfo> currentFeatures = new ArrayList<MBeanFeatureInfo>();
	private String featuresForObjectName;

	boolean featuresVisible = true;
	boolean outputVisible = true;
	int userMbeansCols = -1;
	int userListsBandOuterRows = -1;
	int outputInnerCols = 10;

	private boolean focusPaneStored;
	private JmxTuiModeFocusPane lastFocusedPane = JmxTuiModeFocusPane.SEARCH;

	private boolean focusRegistered;

	public JmxTuiModeContext(WindowBasedTextGUI gui, BoxFocusManager focusManager, JmxTuiApp app, JmxTuiUiMode mode) {
		this.threadMode = mode != null && ThreadUiMode.ID.equals(mode.getId());
		this.mbeanUiMode = mode != null && MBeanUiMode.ID.equals(mode.getId());
		this.mbeanSearchInner = new Panel(new LinearLayout(Direction.VERTICAL));
		this.mbeansInner = new Panel(new LinearLayout(Direction.VERTICAL));
		this.featuresInner = new Panel(new LinearLayout(Direction.VERTICAL));
		this.outputInner = new Panel(new LinearLayout(Direction.VERTICAL));
		this.listsPanel = new Panel(new LinearLayout(Direction.HORIZONTAL).setSpacing(1));
		this.mbeansColumnPanel = new Panel(new LinearLayout(Direction.VERTICAL));

		if(threadMode) {
			this.mbeanList = new FocusActionListBox(gui, focusManager, mbeansInner, THREAD_LIST_BORDER_TITLE);
			this.mbeanListRenderer = null;
			this.threadListRenderer = new ThreadSelectListItemRenderer();
			this.threadListRenderer.setSelectedThreadIds(selectedThreadIds);
			this.mbeanList.setListItemRenderer(threadListRenderer);
			this.threadHeaderLabel = new Label("");
			this.mbeansInner.addComponent(this.threadHeaderLabel);
		} else {
			this.mbeanList = new FocusActionListBox(gui, focusManager, mbeansInner, "MBean");
			this.mbeanListRenderer = new HighlightArrowListItemRenderer();
			this.threadListRenderer = null;
			this.threadHeaderLabel = null;
			this.mbeanList.setListItemRenderer(mbeanListRenderer);
		}
		this.featureList = new FocusActionListBox(gui, focusManager, featuresInner, "Feature");
		this.featureList.setListItemRenderer(new ArrowListItemRenderer());

		this.mbeanSearch = new MbeanSearchInputBox(focusManager, mbeanSearchInner, new TerminalSize(1, 1));
		this.mbeanSearch.setCaretWarp(true);
		this.mbeanSearch.setVerticalFocusSwitching(false);
		this.mbeanSearch.setHorizontalFocusSwitching(false);
		this.mbeanSearch.setTheme(JmxTuiTheme.UNFOCUSED_TEXTBOX_THEME);

		this.mbeanSearchRow = new Panel(new LinearLayout(Direction.HORIZONTAL));
		this.mbeanSearchRow.addComponent(new com.googlecode.lanterna.gui2.Label(MBEAN_SEARCH_PROMPT_LABEL));
		this.mbeanSearchRow.addComponent(mbeanSearch, LinearLayout.createLayoutData(Alignment.Fill, GrowPolicy.CanGrow));
		this.mbeanSearchInner.addComponent(this.mbeanSearchRow, LinearLayout.createLayoutData(Alignment.Fill, GrowPolicy.CanGrow));

		this.mbeansInner.addComponent(this.mbeanList, LinearLayout.createLayoutData(Alignment.Fill, GrowPolicy.CanGrow));
		this.featuresInner.addComponent(this.featureList, LinearLayout.createLayoutData(Alignment.Fill, GrowPolicy.CanGrow));

		this.output = new OutputTextBox(focusManager, outputInner, new TerminalSize(1, 1), Style.MULTI_LINE, app);
		this.output.setReadOnly(true);
		this.output.setTheme(JmxTuiTheme.UNFOCUSED_TEXTBOX_THEME);
		this.outputInner.addComponent(this.output, LinearLayout.createLayoutData(Alignment.Fill, GrowPolicy.CanGrow));

		this.mbeanSearchBordered = mbeanSearchInner.withBorder(Borders.singleLine("Search"));
		this.mbeansListBordered = mbeansInner.withBorder(Borders.singleLine(threadMode ? THREAD_LIST_BORDER_TITLE : "MBeans"));
		this.featuresBordered = featuresInner.withBorder(Borders.singleLine("Features"));
		this.outputPaneBorder = outputInner.withBorder(Borders.singleLine("Output"));

		this.mbeansColumnPanel.addComponent(mbeanSearchBordered);
		this.mbeansColumnPanel.addComponent(mbeansListBordered, LinearLayout.createLayoutData(Alignment.Fill, GrowPolicy.CanGrow));

		app.wireModeContext(this);
	}

	public void activateFocus(BoxFocusManager focusManager) {
		if(focusRegistered) {
			return;
		}
		focusManager.register(mbeanSearchInner);
		focusManager.register(mbeansInner);
		focusManager.register(featuresInner);
		focusManager.register(outputInner);
		focusManager.registerFillHighlight(mbeanSearchInner);
		focusManager.registerFillHighlight(outputInner);
		focusRegistered = true;
	}

	public void deactivateFocus(BoxFocusManager focusManager) {
		if(!focusRegistered) {
			return;
		}
		focusManager.unregister(mbeanSearchInner);
		focusManager.unregister(mbeansInner);
		focusManager.unregister(featuresInner);
		focusManager.unregister(outputInner);
		focusManager.unregisterFillHighlight(mbeanSearchInner);
		focusManager.unregisterFillHighlight(outputInner);
		focusRegistered = false;
	}

	public Panel getListsPanel() {
		return listsPanel;
	}

	public Panel getMbeansColumnPanel() {
		return mbeansColumnPanel;
	}

	public Component getOutputPaneBordered() {
		return outputPaneBorder;
	}

	public void setOutputPaneBorder(Border border) {
		this.outputPaneBorder = border;
	}

	public Panel getOutputInner() {
		return outputInner;
	}

	public MbeanSearchInputBox getMbeanSearch() {
		return mbeanSearch;
	}

	public FocusActionListBox getMbeanList() {
		return mbeanList;
	}

	public FocusActionListBox getFeatureList() {
		return featureList;
	}

	public OutputTextBox getOutput() {
		return output;
	}

	public HighlightArrowListItemRenderer getMbeanListRenderer() {
		return mbeanListRenderer;
	}

	public ThreadSelectListItemRenderer getThreadListRenderer() {
		return threadListRenderer;
	}

	public boolean isThreadMode() {
		return threadMode;
	}

	public boolean isMbeanUiMode() {
		return mbeanUiMode;
	}

	public List<ThreadSnapshotEntry> getAllThreadEntries() {
		return allThreadEntries;
	}

	public List<Long> getDisplayedThreadIds() {
		return displayedThreadIds;
	}

	public Set<Long> getSelectedThreadIds() {
		return selectedThreadIds;
	}

	public Label getThreadHeaderLabel() {
		return threadHeaderLabel;
	}

	public boolean hasPreviousCpuSnapshot() {
		return prevSnapshotWallNanos > 0L && !prevThreadCpuNanos.isEmpty();
	}

	public long getPrevSnapshotWallNanos() {
		return prevSnapshotWallNanos;
	}

	public Long getPrevThreadCpuNanos(long threadId) {
		return prevThreadCpuNanos.get(Long.valueOf(threadId));
	}

	public void commitCpuSnapshot(Map<Long, Long> cpuNanosByThreadId, long wallNanos) {
		prevThreadCpuNanos.clear();
		if(cpuNanosByThreadId != null) {
			prevThreadCpuNanos.putAll(cpuNanosByThreadId);
		}
		prevSnapshotWallNanos = wallNanos;
	}

	public boolean hasPreviousMemSnapshot() {
		return prevSnapshotWallNanos > 0L && !prevThreadAllocatedBytes.isEmpty();
	}

	public Long getPrevThreadAllocatedBytes(long threadId) {
		return prevThreadAllocatedBytes.get(Long.valueOf(threadId));
	}

	public void commitMemSnapshot(Map<Long, Long> allocatedBytesByThreadId, long wallNanos) {
		prevThreadAllocatedBytes.clear();
		if(allocatedBytesByThreadId != null) {
			prevThreadAllocatedBytes.putAll(allocatedBytesByThreadId);
		}
		prevSnapshotWallNanos = wallNanos;
	}

	public void clearCpuSnapshot() {
		prevThreadCpuNanos.clear();
		prevThreadAllocatedBytes.clear();
		prevThreadNames.clear();
		prevSnapshotWallNanos = 0L;
		hasPreviousThreadSnapshot = false;
		lastCreatedThreadCount = 0;
		lastDestroyedThreadCount = 0;
	}

	public boolean hasPreviousThreadSnapshot() {
		return hasPreviousThreadSnapshot;
	}

	public Map<Long, String> getPrevThreadNamesSnapshot() {
		return new HashMap<Long, String>(prevThreadNames);
	}

	public void commitThreadSnapshot(Map<Long, String> threadNamesById) {
		prevThreadNames.clear();
		if(threadNamesById != null) {
			prevThreadNames.putAll(threadNamesById);
		}
		hasPreviousThreadSnapshot = true;
	}

	public String getSelectedExpression() {
		return selectedExpression == null ? "" : selectedExpression;
	}

	public void setSelectedExpression(String selectedExpression) {
		this.selectedExpression = selectedExpression == null ? "" : selectedExpression;
	}

	/** Saves current thread selection and clears visible selection for STACKTRACE_TARGET=ACTIVE. */
	public void stashThreadSelectionForActiveMode() {
		activeModePreservedThreadIds.clear();
		activeModePreservedThreadIds.addAll(selectedThreadIds);
		activeModePreservedExpression = selectedExpression == null ? "" : selectedExpression;
		selectedThreadIds.clear();
	}

	public Set<Long> getActiveModePreservedThreadIds() {
		return activeModePreservedThreadIds;
	}

	public String getActiveModePreservedExpression() {
		return activeModePreservedExpression == null ? "" : activeModePreservedExpression;
	}

	public int getLastCreatedThreadCount() {
		return lastCreatedThreadCount;
	}

	public void setLastCreatedThreadCount(int lastCreatedThreadCount) {
		this.lastCreatedThreadCount = lastCreatedThreadCount < 0 ? 0 : lastCreatedThreadCount;
	}

	public int getLastDestroyedThreadCount() {
		return lastDestroyedThreadCount;
	}

	public void setLastDestroyedThreadCount(int lastDestroyedThreadCount) {
		this.lastDestroyedThreadCount = lastDestroyedThreadCount < 0 ? 0 : lastDestroyedThreadCount;
	}

	public boolean isThreadAutoRefreshEnabled() {
		return threadAutoRefreshEnabled;
	}

	public void setThreadAutoRefreshEnabled(boolean threadAutoRefreshEnabled) {
		this.threadAutoRefreshEnabled = threadAutoRefreshEnabled;
	}

	public int getThreadRefreshIntervalSeconds() {
		return threadRefreshIntervalSeconds;
	}

	public void setThreadRefreshIntervalSeconds(int threadRefreshIntervalSeconds) {
		if(threadRefreshIntervalSeconds < 1) {
			threadRefreshIntervalSeconds = 1;
		} else if(threadRefreshIntervalSeconds > 3600) {
			threadRefreshIntervalSeconds = 3600;
		}
		this.threadRefreshIntervalSeconds = threadRefreshIntervalSeconds;
	}

	public void setThreadHeaderCols(int cols) {
		threadHeaderCols = cols < 0 ? 0 : cols;
		applyThreadHeaderLabelText();
	}

	public void setThreadHeaderFullText(String fullText) {
		threadHeaderFullText = fullText == null ? "" : fullText;
		applyThreadHeaderLabelText();
	}

	private void applyThreadHeaderLabelText() {
		if(threadHeaderLabel == null) {
			return;
		}
		String full = threadHeaderFullText == null ? "" : threadHeaderFullText;
		if(threadHeaderCols > 0) {
			threadHeaderLabel.setText(TuiStatusText.fitSingleLine(full, threadHeaderCols));
		} else {
			threadHeaderLabel.setText(full);
		}
	}

	public void refreshThreadTableHeader(List<ThreadSnapshotEntry> entries) {
		int tidWidth = ThreadPaneTableFormat.computeTidWidth(entries);
		int cpuWidth = ThreadPaneTableFormat.computeCpuWidth(entries);
		int memWidth = ThreadPaneTableFormat.computeMemWidth(entries);
		setThreadHeaderFullText(ThreadPaneTableFormat.buildHeaderLine(tidWidth, cpuWidth, memWidth));
	}

	public void setMbeansListBordered(Component bordered) {
		this.mbeansListBordered = bordered;
	}

	public enum ThreadRowState {
		NORMAL,
		CREATED,
		DESTROYED
	}

	public static final class ThreadSnapshotEntry {
		private final long threadId;
		private final String threadName;
		private final String stateAbbrev;
		private final double cpuCores;
		private final boolean cpuValid;
		private final double memBytesPerSec;
		private final boolean memValid;
		private final ThreadRowState rowState;

		public ThreadSnapshotEntry(
			long threadId,
			String threadName,
			String stateAbbrev,
			double cpuCores,
			boolean cpuValid,
			double memBytesPerSec,
			boolean memValid) {
			this(threadId, threadName, stateAbbrev, cpuCores, cpuValid, memBytesPerSec, memValid, ThreadRowState.NORMAL);
		}

		public ThreadSnapshotEntry(
			long threadId,
			String threadName,
			String stateAbbrev,
			double cpuCores,
			boolean cpuValid,
			double memBytesPerSec,
			boolean memValid,
			ThreadRowState rowState) {
			this.threadId = threadId;
			this.threadName = threadName == null ? "" : threadName;
			this.stateAbbrev = stateAbbrev == null ? "?" : stateAbbrev;
			this.cpuCores = cpuCores;
			this.cpuValid = cpuValid;
			this.memBytesPerSec = memBytesPerSec;
			this.memValid = memValid;
			this.rowState = rowState == null ? ThreadRowState.NORMAL : rowState;
		}

		public long getThreadId() {
			return threadId;
		}

		public String getThreadName() {
			return threadName;
		}

		public String getStateAbbrev() {
			return stateAbbrev;
		}

		public double getCpuCores() {
			return cpuCores;
		}

		public boolean isCpuValid() {
			return cpuValid;
		}

		public double getMemBytesPerSec() {
			return memBytesPerSec;
		}

		public boolean isMemValid() {
			return memValid;
		}

		public ThreadRowState getRowState() {
			return rowState;
		}

		public boolean isLive() {
			return rowState != ThreadRowState.DESTROYED;
		}
	}

	public Component getMbeanSearchBordered() {
		return mbeanSearchBordered;
	}

	public Component getMbeansListBordered() {
		return mbeansListBordered;
	}

	public Component getFeaturesBordered() {
		return featuresBordered;
	}

	public Panel getMbeanSearchRow() {
		return mbeanSearchRow;
	}

	public Panel getMbeanSearchInner() {
		return mbeanSearchInner;
	}

	public Panel getMbeansInner() {
		return mbeansInner;
	}

	public Panel getFeaturesInner() {
		return featuresInner;
	}

	public List<String> getDisplayedMbeanNames() {
		return displayedMbeanNames;
	}

	public List<MBeanFeatureInfo> getCurrentFeatures() {
		return currentFeatures;
	}

	public String getFeaturesForObjectName() {
		return featuresForObjectName;
	}

	public void setFeaturesForObjectName(String featuresForObjectName) {
		this.featuresForObjectName = featuresForObjectName;
	}

	public boolean isFeaturesVisible() {
		return featuresVisible;
	}

	public void setFeaturesVisible(boolean featuresVisible) {
		this.featuresVisible = featuresVisible;
	}

	public boolean isOutputVisible() {
		return outputVisible;
	}

	public void setOutputVisible(boolean outputVisible) {
		this.outputVisible = outputVisible;
	}

	public int getUserMbeansCols() {
		return userMbeansCols;
	}

	public void setUserMbeansCols(int userMbeansCols) {
		this.userMbeansCols = userMbeansCols;
	}

	public int getUserListsBandOuterRows() {
		return userListsBandOuterRows;
	}

	public void setUserListsBandOuterRows(int userListsBandOuterRows) {
		this.userListsBandOuterRows = userListsBandOuterRows;
	}

	public int getOutputInnerCols() {
		return outputInnerCols;
	}

	public void setOutputInnerCols(int outputInnerCols) {
		this.outputInnerCols = outputInnerCols;
	}

	public boolean isFocusPaneStored() {
		return focusPaneStored;
	}

	public JmxTuiModeFocusPane getLastFocusedPane() {
		return lastFocusedPane;
	}

	public void storeFocusedPane(JmxTuiModeFocusPane pane) {
		if(pane != null) {
			lastFocusedPane = pane;
			focusPaneStored = true;
		}
	}

	public JmxTuiModeFocusPane resolveRestorableFocusPane(JmxTuiUiMode mode) {
		JmxTuiModeFocusPane pane = focusPaneStored ? lastFocusedPane : JmxTuiModeFocusPane.SEARCH;
		if(pane == null) {
			pane = JmxTuiModeFocusPane.SEARCH;
		}
		if(pane == JmxTuiModeFocusPane.FEATURES && !isFeaturesFocusAvailable(mode)) {
			pane = JmxTuiModeFocusPane.MBEANS;
		}
		if(pane == JmxTuiModeFocusPane.OUTPUT && !isOutputFocusAvailable(mode)) {
			pane = JmxTuiModeFocusPane.MBEANS;
		}
		return pane;
	}

	public JmxTuiModeFocusPane getDefaultFocusPane() {
		return JmxTuiModeFocusPane.SEARCH;
	}

	public Interactable resolveInteractable(JmxTuiModeFocusPane pane, JmxTuiUiMode mode, CommandInputBox commandInput) {
		if(pane == null) {
			pane = getDefaultFocusPane();
		}
		if(pane == JmxTuiModeFocusPane.COMMAND) {
			return commandInput;
		}
		if(pane == JmxTuiModeFocusPane.MBEANS) {
			return mbeanList;
		}
		if(pane == JmxTuiModeFocusPane.FEATURES && isFeaturesFocusAvailable(mode)) {
			return featureList;
		}
		if(pane == JmxTuiModeFocusPane.OUTPUT && isOutputFocusAvailable(mode)) {
			return output;
		}
		return mbeanSearch;
	}

	public Panel resolvePanePanel(JmxTuiModeFocusPane pane, JmxTuiUiMode mode, Panel commandInner) {
		if(pane == null) {
			pane = getDefaultFocusPane();
		}
		if(pane == JmxTuiModeFocusPane.COMMAND) {
			return commandInner;
		}
		if(pane == JmxTuiModeFocusPane.MBEANS) {
			return mbeansInner;
		}
		if(pane == JmxTuiModeFocusPane.FEATURES && isFeaturesFocusAvailable(mode)) {
			return featuresInner;
		}
		if(pane == JmxTuiModeFocusPane.OUTPUT && isOutputFocusAvailable(mode)) {
			return outputInner;
		}
		return mbeanSearchInner;
	}

	private boolean isFeaturesFocusAvailable(JmxTuiUiMode mode) {
		return mode != null && mode.isFeaturesPaneAvailable() && featuresVisible;
	}

	private boolean isOutputFocusAvailable(JmxTuiUiMode mode) {
		return outputVisible;
	}
}
