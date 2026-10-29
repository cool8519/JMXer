package dal.tool.trace.jmxer.tui.mode;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.LinearLayout.Alignment;
import com.googlecode.lanterna.gui2.LinearLayout.GrowPolicy;
import com.googlecode.lanterna.gui2.Panel;

import dal.tool.trace.jmxer.JMXControl;
import dal.tool.trace.jmxer.cli.JmxSettings;
import dal.tool.trace.jmxer.cli.command.JmxRecordCommand;
import dal.tool.trace.jmxer.cli.data.RecordResult;
import dal.tool.trace.jmxer.cli.helper.ThreadListExpressionBuilder;
import dal.tool.util.DateUtil;
import dal.tool.trace.jmxer.tui.app.JmxTuiApp;
import dal.tool.trace.jmxer.tui.mode.JmxTuiModeContext.RecordViewState;
import dal.tool.trace.jmxer.tui.support.JmxTuiTheme;
import dal.tool.trace.jmxer.tui.support.TuiKeyAction;
import dal.tool.trace.jmxer.tui.support.TuiKeyBindings;
import dal.tool.trace.jmxer.tui.support.TuiPaneKeysHint;
import dal.tool.trace.jmxer.tui.support.TuiRecordUiKeys;
import dal.tool.trace.jmxer.tui.support.TuiThreadUiKeys;
import dal.tool.trace.jmxer.tui.support.TuiStatusSegmentLabel;
import dal.tool.trace.jmxer.tui.support.TuiStatusSettingLine;
import dal.tool.trace.jmxer.tui.support.TuiStatusText;

/**
 * Record UI Mode. Live/Dump 뷰·스레드 목록·Record 샘플링 UI를 구성한다.
 *
 */
public final class RecordUiMode extends JmxTuiUiMode {
	public static final String ID = "RECORD";
	public static final String DUMP_LIST_BORDER_TITLE = "Thread";
	public static final String RECORD_DUMP_BORDER_TIME_FORMAT = "yyyy.MM.dd/HH:mm:ss.SSS";
	private static final int DEFAULT_HORIZONTAL_LEFT_SHARE_NUMERATOR = 1;
	private static final int DEFAULT_HORIZONTAL_LEFT_SHARE_DENOMINATOR = 2;
	private static final RecordUiMode INSTANCE = new RecordUiMode();

	public static RecordUiMode getInstance() {
		return INSTANCE;
	}

	private RecordUiMode() {}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDisplayName() {
		return "Record";
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
		RecordViewState viewState = context == null ? RecordViewState.LIVE : context.getRecordViewState();

		List<TuiStatusSegmentLabel.Segment> segments = new ArrayList<TuiStatusSegmentLabel.Segment>();
		segments.add(new TuiStatusSegmentLabel.Segment("   - Threads:" + total, null));
		if(viewState == RecordViewState.LIVE) {
			appendDeltaSegments(segments, created, destroyed);
		} else {
			segments.add(new TuiStatusSegmentLabel.Segment(
				String.format(TuiRecordUiKeys.STATUS_SAMPLES_SEGMENT, resolveRecordSampleCount()),
				null));
		}
		String tail = TuiStatusText.appendBracketedExpression(
			segments,
			" | Selected(" + TuiKeyBindings.label(TuiKeyAction.THREAD_EDIT_SELECTION) + "):" + selected,
			buildSelectedThreadsExpression(context),
			statusInnerCols);
		segments.add(new TuiStatusSegmentLabel.Segment(tail, null));

		header2.setMaxColumns(statusInnerCols);
		header2.setSegments(fitSegments(segments, statusInnerCols));
	}

	@Override
	public void renderStatusDetailLine(Label header3, JmxSettings s, JmxTuiModeContext context, int statusInnerCols) {
		String detail;
		if(context != null && context.getRecordViewState() == RecordViewState.LIVE) {
			StringBuilder sb = new StringBuilder("   - ");
			if(context.isThreadAutoRefreshEnabled()) {
				sb.append("REFRESH(").append(TuiKeyBindings.label(TuiKeyAction.THREAD_AUTO_REFRESH)).append("):AUTO[")
					.append(context.getThreadRefreshIntervalSeconds()).append("s]");
			} else {
				sb.append("REFRESH(").append(TuiKeyBindings.label(TuiKeyAction.THREAD_AUTO_REFRESH)).append("):MANUAL");
			}
			sb.append(", SELECTED_TARGET(")
				.append(TuiKeyBindings.label(TuiKeyAction.THREAD_SELECTED_TARGET))
				.append("):")
				.append(context.getSelectedTarget().name());
			detail = sb.toString();
		} else {
			detail = TuiStatusSettingLine.recordDetailLine(s);
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
			boolean liveView = context == null || context.getRecordViewState() == RecordViewState.LIVE;
			if(liveView) {
				return TuiPaneKeysHint.recordThreadList(true);
			}
			return TuiPaneKeysHint.recordThreadListDumpView(false);
		}
		if(focusedPane == JmxTuiModeFocusPane.OUTPUT) {
			return TuiPaneKeysHint.output(wrapOn);
		}
		if(focusedPane == JmxTuiModeFocusPane.COMMAND) {
			return TuiPaneKeysHint.command();
		}
		boolean liveView = context == null || context.getRecordViewState() == RecordViewState.LIVE;
		return TuiPaneKeysHint.recordThreadSearch(liveView);
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
		boolean liveView = context == null || context.getRecordViewState() == RecordViewState.LIVE;
		if(focusedPane == JmxTuiModeFocusPane.MBEANS) {
			TuiPaneKeysHint.collectRecordThreadList(out, liveView);
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
		TuiPaneKeysHint.collectRecordThreadSearch(out, liveView);
	}

	public static String buildSelectedThreadsExpression(JmxTuiModeContext context) {
		if(context == null || !context.hasThreadPane()) {
			return "";
		}
		if(!JMXControl.isOfflineSession
				&& context.getRecordViewState() == RecordViewState.LIVE
				&& context.getSelectedTarget() == JmxTuiModeContext.SelectedTarget.TID) {
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

	private static int resolveRecordSampleCount() {
		RecordResult result = JmxRecordCommand.getLastRecordResult();
		return result == null ? 0 : result.sampleCount;
	}

	public static String formatRecordDumpBorderTime(RecordResult result) {
		if(result == null || result.startTime <= 0L) {
			return "N/A";
		}
		String start = DateUtil.dateToString(RECORD_DUMP_BORDER_TIME_FORMAT, new Date(result.startTime));
		if(result.endTime > result.startTime) {
			String end = DateUtil.dateToString(RECORD_DUMP_BORDER_TIME_FORMAT, new Date(result.endTime));
			return start + " ~ " + end;
		}
		return start;
	}

	public static String formatViewStateStatusSuffix(JmxTuiModeContext context) {
		RecordViewState viewState = context == null ? RecordViewState.LIVE : context.getRecordViewState();
		return "(" + viewStateLabel(viewState) + " View)";
	}

	private static String viewStateLabel(RecordViewState viewState) {
		if(viewState == RecordViewState.DUMP) {
			return TuiRecordUiKeys.VIEW_DUMP_LABEL;
		}
		return TuiRecordUiKeys.VIEW_LIVE_LABEL;
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
		truncated.add(new TuiStatusSegmentLabel.Segment(fitted, null));
		return truncated;
	}
}
