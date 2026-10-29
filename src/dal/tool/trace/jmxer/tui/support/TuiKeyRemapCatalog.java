package dal.tool.trace.jmxer.tui.support;

import java.util.ArrayList;
import java.util.List;

import dal.tool.trace.jmxer.cli.JmxSettings;
import dal.tool.trace.jmxer.tui.mode.JmxTuiModeContext;
import dal.tool.trace.jmxer.tui.mode.JmxTuiModeFocusPane;
import dal.tool.trace.jmxer.tui.mode.JmxTuiUiMode;

/**
 * 상태 줄 [Global]·[Window] 힌트와 동일한 조건으로 리매핑 다이얼로그 목록을 구성한다.
 *
 */
public final class TuiKeyRemapCatalog {

	private TuiKeyRemapCatalog() {}

	public static List<TuiKeyAction> listGlobalActions(JmxTuiUiMode mode, JmxTuiModeContext ctx, TuiKeyBindings bindings) {
		return listGlobalActions(mode, ctx, null, bindings);
	}

	public static List<TuiKeyAction> listGlobalActions(
			JmxTuiUiMode mode,
			JmxTuiModeContext ctx,
			JmxSettings settings,
			TuiKeyBindings bindings) {
		List<TuiKeyAction> actions = new ArrayList<TuiKeyAction>();
		if(mode == null) {
			return actions;
		}
		boolean featuresAvailable = mode.isFeaturesPaneAvailable();
		boolean samplingInProgress = ctx != null && ctx.isRecordSamplingInProgress();
		TuiGlobalKeysHint.collectRemappableGlobalActions(actions, mode, ctx, settings, featuresAvailable, samplingInProgress);
		TuiKeyBindingSort.sortByBinding(actions, bindings == null ? TuiKeyBindings.current() : bindings);
		return actions;
	}

	public static List<TuiKeyAction> listWindowActions(JmxTuiUiMode mode, JmxTuiModeContext ctx,
			JmxTuiModeFocusPane focusedPane, JmxSettings settings, TuiKeyBindings bindings) {
		List<TuiKeyAction> actions = new ArrayList<TuiKeyAction>();
		if(mode == null) {
			return actions;
		}
		if(focusedPane == null) {
			focusedPane = JmxTuiModeFocusPane.SEARCH;
		}
		mode.collectRemappableWindowActions(actions, focusedPane, settings, ctx);
		TuiKeyBindingSort.sortByBinding(actions, bindings == null ? TuiKeyBindings.current() : bindings);
		return actions;
	}

	public static void addUnique(List<TuiKeyAction> out, TuiKeyAction action) {
		if(out == null || action == null) {
			return;
		}
		for(int i = 0; i < out.size(); i++) {
			if(out.get(i) == action) {
				return;
			}
		}
		out.add(action);
	}
}
