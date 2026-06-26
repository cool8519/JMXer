package dal.tool.trace.jmxer.tui.support;

import java.util.EnumSet;
import java.util.Set;

import dal.tool.trace.jmxer.tui.mode.MBeanUiMode;
import dal.tool.trace.jmxer.tui.mode.RecordUiMode;
import dal.tool.trace.jmxer.tui.mode.ThreadUiMode;

/**
 * TuiKeyAction이 어떤 UI Mode에서 동시에 활성화될 수 있는지 정의한다.
 * 리매핑 충돌 검사는 같은 UI Mode 안에서만 수행한다.
 *
 */
public final class TuiKeyActionUiMode {

	private static final Set<TuiKeyAction> ALL_UI_MODES = EnumSet.of(
		TuiKeyAction.OUTPUT_PANE_TOGGLE,
		TuiKeyAction.OUTPUT_SAVE,
		TuiKeyAction.OUTPUT_WRAP,
		TuiKeyAction.COMMAND_PASTE
	);

	private static final Set<TuiKeyAction> MBEAN_UI_MODE = EnumSet.of(
		TuiKeyAction.OUTPUT_PRETTY_PRINT,
		TuiKeyAction.FEATURES_PANE_TOGGLE,
		TuiKeyAction.MBEAN_SET_DEFAULT,
		TuiKeyAction.FEATURE_INVOKE,
		TuiKeyAction.INFO
	);

	private static final Set<TuiKeyAction> THREAD_UI_MODE = EnumSet.of(
		TuiKeyAction.THREAD_AUTO_REFRESH,
		TuiKeyAction.THREAD_STACKTRACE_TARGET,
		TuiKeyAction.THREAD_EDIT_SELECTION,
		TuiKeyAction.THREAD_REQUEST_ENTRY_FILE,
		TuiKeyAction.THREAD_FORCE_REFRESH,
		TuiKeyAction.THREAD_PRIMARY_ACTION,
		TuiKeyAction.THREAD_TOGGLE_CURRENT,
		TuiKeyAction.THREAD_BULK_STACKTRACE,
		TuiKeyAction.THREAD_TOGGLE_ALL,
		TuiKeyAction.THREAD_CLEAR_SELECTED,
		TuiKeyAction.THREAD_SORT,
		TuiKeyAction.INFO
	);

	private static final Set<TuiKeyAction> RECORD_UI_MODE = EnumSet.of(
		TuiKeyAction.RECORD_VIEW_TYPE_TOGGLE,
		TuiKeyAction.RECORD_SAMPLE,
		TuiKeyAction.RECORD_SAMPLE_SELECTED,
		TuiKeyAction.RECORD_DUMP_LOAD,
		TuiKeyAction.RECORD_DUMP_SAVE,
		TuiKeyAction.RECORD_DUMP_INFO,
		TuiKeyAction.RECORD_VIEW_METHOD,
		TuiKeyAction.RECORD_VIEW_THREAD,
		TuiKeyAction.RECORD_VIEW_STACK,
		TuiKeyAction.RECORD_VIEW_SEARCH,
		TuiKeyAction.FEATURES_PANE_TOGGLE,
		TuiKeyAction.RECORD_SHOW_EMPTY_THREAD_TOGGLE,
		TuiKeyAction.THREAD_AUTO_REFRESH,
		TuiKeyAction.THREAD_EDIT_SELECTION,
		TuiKeyAction.THREAD_FORCE_REFRESH,
		TuiKeyAction.THREAD_PRIMARY_ACTION,
		TuiKeyAction.THREAD_TOGGLE_CURRENT,
		TuiKeyAction.THREAD_BULK_STACKTRACE,
		TuiKeyAction.THREAD_TOGGLE_ALL,
		TuiKeyAction.THREAD_CLEAR_SELECTED,
		TuiKeyAction.THREAD_SORT,
		TuiKeyAction.INFO
	);

	private TuiKeyActionUiMode() {}

	public static boolean shareActiveUiMode(String uiModeId, TuiKeyAction left, TuiKeyAction right) {
		if(left == null || right == null || uiModeId == null || uiModeId.length() == 0) {
			return true;
		}
		return isActiveInUiMode(uiModeId, left) && isActiveInUiMode(uiModeId, right);
	}

	public static boolean isActiveInUiMode(String uiModeId, TuiKeyAction action) {
		if(action == null || uiModeId == null) {
			return false;
		}
		if(ALL_UI_MODES.contains(action)) {
			return true;
		}
		if(MBeanUiMode.ID.equals(uiModeId)) {
			return MBEAN_UI_MODE.contains(action);
		}
		if(ThreadUiMode.ID.equals(uiModeId)) {
			return THREAD_UI_MODE.contains(action);
		}
		if(RecordUiMode.ID.equals(uiModeId)) {
			return RECORD_UI_MODE.contains(action);
		}
		return false;
	}
}
