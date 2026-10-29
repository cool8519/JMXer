package dal.tool.trace.jmxer.tui.mode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.LinearLayout.Alignment;
import com.googlecode.lanterna.gui2.LinearLayout.GrowPolicy;
import com.googlecode.lanterna.gui2.Panel;

import dal.tool.trace.jmxer.cli.JmxSettings;
import dal.tool.trace.jmxer.cli.JmxSettings.StacktraceTarget;
import dal.tool.trace.jmxer.cli.helper.ThreadListExpressionBuilder;
import dal.tool.trace.jmxer.tui.app.JmxTuiApp;
import dal.tool.trace.jmxer.tui.support.JmxTuiTheme;
import dal.tool.trace.jmxer.tui.support.TuiKeyAction;
import dal.tool.trace.jmxer.tui.support.TuiKeyBindings;
import dal.tool.trace.jmxer.tui.support.TuiKeyRemapCatalog;
import dal.tool.trace.jmxer.tui.support.TuiPaneKeysHint;
import dal.tool.trace.jmxer.tui.support.TuiStatusSegmentLabel;
import dal.tool.trace.jmxer.tui.support.TuiStatusText;

/**
 * Thread UI Mode. 스레드 검색·목록·자동 새로고침 UI를 구성한다.
 *
 */
public final class ThreadUiMode extends JmxTuiUiMode {
	public static final String ID = "THREAD";
	private static final int DEFAULT_HORIZONTAL_LEFT_SHARE_NUMERATOR = 1;
	private static final int DEFAULT_HORIZONTAL_LEFT_SHARE_DENOMINATOR = 2;
	private static final ThreadUiMode INSTANCE = new ThreadUiMode();

	public static ThreadUiMode getInstance() {
		return INSTANCE;
	}

	private ThreadUiMode() {}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDisplayName() {
		return "Thread";
	}

	@Override
	public boolean isOutputInRightColumn() {
		return true;
	}

	@Override
	public boolean isFeaturesPaneAvailable() {
		return false;
	}

	@Override
	public boolean canResizeVerticalSplit() {
		return false;
	}

	@Override
	public int getDefaultHorizontalLeftShareNumerator() {
		return DEFAULT_HORIZONTAL_LEFT_SHARE_NUMERATOR;
	}

	@Override
	public int getDefaultHorizontalLeftShareDenominator() {
		return DEFAULT_HORIZONTAL_LEFT_SHARE_DENOMINATOR;
	}

	@Override
	public void buildBody(JmxTuiApp app, Panel root) {
		root.addComponent(
			app.getListsPanel(),
			LinearLayout.createLayoutData(Alignment.Fill, GrowPolicy.CanGrow)
		);
	}

	@Override
	public void renderStatusObjectLine(TuiStatusSegmentLabel header2, JmxSettings s, JmxTuiModeContext context, int statusInnerCols) {
		int total = countLiveThreads(context);
		int selected = context == null ? 0 : context.getSelectedThreadIds().size();
		int created = context == null ? 0 : context.getLastCreatedThreadCount();
		int destroyed = context == null ? 0 : context.getLastDestroyedThreadCount();

		List<TuiStatusSegmentLabel.Segment> segments = new ArrayList<TuiStatusSegmentLabel.Segment>();
		segments.add(new TuiStatusSegmentLabel.Segment("   - Threads:" + total, null));
		appendDeltaSegments(segments, created, destroyed);
		if(s == null || s.getStacktraceTarget() != StacktraceTarget.ACTIVE) {
			String tail = TuiStatusText.appendBracketedExpression(
				segments,
				" | Selected(" + TuiKeyBindings.label(TuiKeyAction.THREAD_EDIT_SELECTION) + "):" + selected,
				buildSelectedThreadsExpression(context),
				statusInnerCols);
			segments.add(new TuiStatusSegmentLabel.Segment(tail, null));
		}

		header2.setMaxColumns(statusInnerCols);
		header2.setSegments(fitSegments(segments, statusInnerCols));
	}

	@Override
	public void renderStatusDetailLine(Label header3, JmxSettings s, JmxTuiModeContext context, int statusInnerCols) {
		String refreshPart;
		if(context != null && context.isThreadAutoRefreshEnabled()) {
			refreshPart = "REFRESH(" + TuiKeyBindings.label(TuiKeyAction.THREAD_AUTO_REFRESH) + "):AUTO["
				+ context.getThreadRefreshIntervalSeconds() + "s]";
		} else {
			refreshPart = "REFRESH(" + TuiKeyBindings.label(TuiKeyAction.THREAD_AUTO_REFRESH) + "):MANUAL";
		}
		String targetValue = (s == null) ? StacktraceTarget.SELECTED.name() : s.getStacktraceTarget().name();
		String detail = "   - " + refreshPart + ", STACKTRACE_TARGET("
			+ TuiKeyBindings.label(TuiKeyAction.THREAD_STACKTRACE_TARGET) + "):" + targetValue;
		if(s != null && s.getStacktraceTarget() == StacktraceTarget.ACTIVE) {
			String reqEntryFile = s.getReqEntryFile();
			if(reqEntryFile == null) {
				reqEntryFile = "";
			}
			detail += ", REQUEST_ENTRY_FILE("
				+ TuiKeyBindings.label(TuiKeyAction.THREAD_REQUEST_ENTRY_FILE) + "):\""
				+ reqEntryFile + "\"";
		} else {
			JmxTuiModeContext.SelectedTarget selectedTarget = context == null
				? JmxTuiModeContext.SelectedTarget.EXPRESSION
				: context.getSelectedTarget();
			detail += ", SELECTED_TARGET("
				+ TuiKeyBindings.label(TuiKeyAction.THREAD_SELECTED_TARGET) + "):"
				+ selectedTarget.name();
		}
		header3.setText(TuiStatusText.fitSingleLine(detail, statusInnerCols));
	}

	@Override
	public String getPaneKeysHint(
		JmxTuiModeFocusPane focusedPane,
		boolean wrapOn,
		JmxSettings settings,
		JmxTuiModeContext context
	) {
		if(focusedPane == null) {
			focusedPane = JmxTuiModeFocusPane.SEARCH;
		}
		if(focusedPane == JmxTuiModeFocusPane.MBEANS) {
			boolean stacktraceTargetActive = settings != null && settings.getStacktraceTarget() == StacktraceTarget.ACTIVE;
			boolean selectionKeysEnabled = !stacktraceTargetActive;
			return TuiPaneKeysHint.threadList(selectionKeysEnabled, stacktraceTargetActive);
		}
		if(focusedPane == JmxTuiModeFocusPane.OUTPUT) {
			return TuiPaneKeysHint.output(wrapOn);
		}
		if(focusedPane == JmxTuiModeFocusPane.COMMAND) {
			return TuiPaneKeysHint.command();
		}
		boolean stacktraceTargetActive = settings != null && settings.getStacktraceTarget() == StacktraceTarget.ACTIVE;
		boolean selectionKeysEnabled = !stacktraceTargetActive;
		return TuiPaneKeysHint.threadSearch(selectionKeysEnabled, stacktraceTargetActive);
	}

	@Override
	public void collectRemappableWindowActions(
			List<TuiKeyAction> out,
			JmxTuiModeFocusPane focusedPane,
			JmxSettings settings,
			JmxTuiModeContext context) {
		if(out == null) {
			return;
		}
		if(focusedPane == null) {
			focusedPane = JmxTuiModeFocusPane.SEARCH;
		}
		boolean stacktraceTargetActive = settings != null && settings.getStacktraceTarget() == StacktraceTarget.ACTIVE;
		boolean selectionKeysEnabled = !stacktraceTargetActive;
		if(focusedPane == JmxTuiModeFocusPane.MBEANS) {
			TuiPaneKeysHint.collectThreadList(out, selectionKeysEnabled);
			return;
		}
		if(focusedPane == JmxTuiModeFocusPane.OUTPUT) {
			TuiPaneKeysHint.collectOutput(out);
			return;
		}
		if(focusedPane == JmxTuiModeFocusPane.COMMAND) {
			TuiPaneKeysHint.collectCommand(out);
			return;
		}
		TuiPaneKeysHint.collectThreadSearch(out, selectionKeysEnabled);
	}

	public static Map<Long, String> buildThreadMap(JmxTuiModeContext context) {
		List<JmxTuiModeContext.ThreadSnapshotEntry> entries = context == null
			? new ArrayList<JmxTuiModeContext.ThreadSnapshotEntry>()
			: context.getAllThreadEntries();
		List<ThreadListExpressionBuilder.ThreadSnapshotEntry> builderEntries =
			new ArrayList<ThreadListExpressionBuilder.ThreadSnapshotEntry>();
		for(int i = 0; i < entries.size(); i++) {
			JmxTuiModeContext.ThreadSnapshotEntry entry = entries.get(i);
			if(entry == null || !entry.isLive()) {
				continue;
			}
			builderEntries.add(new ThreadListExpressionBuilder.ThreadSnapshotEntry(entry.getThreadId(), entry.getThreadName()));
		}
		return ThreadListExpressionBuilder.toThreadMap(builderEntries);
	}

	public static String buildSelectedThreadsExpression(JmxTuiModeContext context) {
		if(context == null || !context.hasThreadPane() || context.isRecordMode()) {
			return "";
		}
		if(context.getSelectedTarget() == JmxTuiModeContext.SelectedTarget.TID) {
			return ThreadListExpressionBuilder.buildTidExpression(context.getSelectedThreadIds());
		}
		if(context.isExplicitSelectedExpression()) {
			String explicit = context.getSelectedExpression();
			if(explicit != null && explicit.trim().length() > 0) {
				return explicit.trim();
			}
		}
		Set<Long> selected = context.getSelectedThreadIds();
		if(selected.isEmpty()) {
			return "";
		}
		return ThreadListExpressionBuilder.build(buildThreadMap(context), selected);
	}

	private static int countLiveThreads(JmxTuiModeContext context) {
		if(context == null) {
			return 0;
		}
		List<JmxTuiModeContext.ThreadSnapshotEntry> entries = context.getAllThreadEntries();
		int count = 0;
		for(int i = 0; i < entries.size(); i++) {
			JmxTuiModeContext.ThreadSnapshotEntry entry = entries.get(i);
			if(entry != null && entry.isLive()) {
				count++;
			}
		}
		return count;
	}

	private static void appendDeltaSegments(List<TuiStatusSegmentLabel.Segment> segments, int created, int destroyed) {
		if(created <= 0 && destroyed <= 0) {
			return;
		}
		segments.add(new TuiStatusSegmentLabel.Segment(" (", null));
		if(created > 0) {
			segments.add(new TuiStatusSegmentLabel.Segment("+" + created, JmxTuiTheme.THREAD_CREATED_FG));
		}
		if(created > 0 && destroyed > 0) {
			segments.add(new TuiStatusSegmentLabel.Segment(", ", null));
		}
		if(destroyed > 0) {
			segments.add(new TuiStatusSegmentLabel.Segment("-" + destroyed, JmxTuiTheme.THREAD_DESTROYED_FG));
		}
		segments.add(new TuiStatusSegmentLabel.Segment(")", null));
	}

	private static List<TuiStatusSegmentLabel.Segment> fitSegments(
			List<TuiStatusSegmentLabel.Segment> segments,
			int maxColumns) {
		if(segments == null || segments.isEmpty()) {
			List<TuiStatusSegmentLabel.Segment> empty = new ArrayList<TuiStatusSegmentLabel.Segment>();
			empty.add(new TuiStatusSegmentLabel.Segment("", null));
			return empty;
		}
		StringBuilder full = new StringBuilder();
		for(int i = 0; i < segments.size(); i++) {
			full.append(segments.get(i).getText());
		}
		String fitted = TuiStatusText.fitSingleLine(full.toString(), maxColumns);
		if(fitted.equals(full.toString())) {
			return segments;
		}
		List<TuiStatusSegmentLabel.Segment> truncated = new ArrayList<TuiStatusSegmentLabel.Segment>();
		truncated.add(new TuiStatusSegmentLabel.Segment(fitted, (TextColor) null));
		return truncated;
	}
}
