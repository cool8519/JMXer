package dal.tool.trace.jmxer.tui.support;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

import dal.tool.trace.jmxer.tui.mode.JmxTuiUiMode;
import dal.tool.trace.jmxer.tui.mode.JmxTuiModeFocusPane;
import dal.tool.trace.jmxer.tui.mode.MBeanUiMode;
import dal.tool.trace.jmxer.tui.mode.RecordUiMode;
import dal.tool.trace.jmxer.tui.mode.ThreadUiMode;

/**
 * TUI 세션 단위 키 바인딩 레지스트리.
 *
 */
public final class TuiKeyBindings {

	public enum RebindResult {
		OK,
		RESERVED,
		CONFLICT
	}

	private static volatile TuiKeyBindings current;

	private final Map<TuiKeyAction, KeyStroke> bindings = new EnumMap<TuiKeyAction, KeyStroke>(TuiKeyAction.class);
	private Runnable changeListener;

	public TuiKeyBindings() {
		resetDefaults();
	}

	public static TuiKeyBindings current() {
		TuiKeyBindings bindings = current;
		if(bindings == null) {
			bindings = new TuiKeyBindings();
			current = bindings;
		}
		return bindings;
	}

	public static void setCurrent(TuiKeyBindings bindings) {
		current = bindings;
	}

	public void setChangeListener(Runnable changeListener) {
		this.changeListener = changeListener;
	}

	public boolean matches(TuiKeyAction action, KeyStroke keyStroke) {
		if(action == null) {
			return false;
		}
		KeyStroke bound = bindings.get(action);
		return TuiKeyStrokeFormat.matches(bound, keyStroke);
	}

	public String getActionLabel(TuiKeyAction action) {
		if(action == null) {
			return "";
		}
		KeyStroke bound = bindings.get(action);
		return TuiKeyStrokeFormat.formatLabel(bound);
	}

	public KeyStroke getBinding(TuiKeyAction action) {
		return action == null ? null : bindings.get(action);
	}

	public boolean isRemappedFromDefault(TuiKeyAction action) {
		if(action == null) {
			return false;
		}
		KeyStroke bound = bindings.get(action);
		return !TuiKeyStrokeFormat.matches(bound, action.getDefaultKeyStroke());
	}

	public RebindResult rebind(TuiKeyAction action, KeyStroke keyStroke) {
		return rebind(action, keyStroke, null);
	}

	public RebindResult rebind(TuiKeyAction action, KeyStroke keyStroke, String uiModeId) {
		if(action == null || keyStroke == null) {
			return RebindResult.RESERVED;
		}
		if(TuiKeyStrokeFormat.isReservedForRemap(keyStroke)) {
			return RebindResult.RESERVED;
		}
		TuiKeyRemapScope scope = action.getScope();
		for(TuiKeyAction other : TuiKeyAction.values()) {
			if(other == action || !TuiKeyActionUiMode.shareActiveUiMode(uiModeId, action, other)) {
				continue;
			}
			if(!scopesConflict(scope, other.getScope())) {
				continue;
			}
			KeyStroke otherStroke = bindings.get(other);
			if(TuiKeyStrokeFormat.matches(otherStroke, keyStroke)) {
				return RebindResult.CONFLICT;
			}
		}
		bindings.put(action, cloneKeyStroke(keyStroke));
		notifyChanged();
		return RebindResult.OK;
	}

	/**
	 * 리매핑 충돌 검사 범위. Global은 Window와 겹치면 안 되고, Window는 동일 pane scope 내에서만 충돌한다.
	 */
	public static boolean scopesConflict(TuiKeyRemapScope left, TuiKeyRemapScope right) {
		if(left == null || right == null) {
			return false;
		}
		if(left == right) {
			return true;
		}
		return left == TuiKeyRemapScope.GLOBAL || right == TuiKeyRemapScope.GLOBAL;
	}

	public void resetDefaults() {
		for(TuiKeyAction action : TuiKeyAction.values()) {
			bindings.put(action, cloneKeyStroke(action.getDefaultKeyStroke()));
		}
		notifyChanged();
	}

	public void resetScope(TuiKeyRemapScope scope) {
		if(scope == null) {
			return;
		}
		for(TuiKeyAction action : TuiKeyAction.values()) {
			if(action.getScope() == scope) {
				bindings.put(action, cloneKeyStroke(action.getDefaultKeyStroke()));
			}
		}
		notifyChanged();
	}

	public void resetActions(List<TuiKeyAction> actions) {
		if(actions == null || actions.isEmpty()) {
			return;
		}
		for(int i = 0; i < actions.size(); i++) {
			TuiKeyAction action = actions.get(i);
			if(action != null) {
				bindings.put(action, cloneKeyStroke(action.getDefaultKeyStroke()));
			}
		}
		notifyChanged();
	}

	public List<TuiKeyAction> getActionsForScope(TuiKeyRemapScope scope) {
		List<TuiKeyAction> actions = new ArrayList<TuiKeyAction>();
		if(scope == null) {
			return actions;
		}
		for(TuiKeyAction action : TuiKeyAction.values()) {
			if(action.getScope() == scope) {
				actions.add(action);
			}
		}
		return actions;
	}

	public static TuiKeyRemapScope resolveWindowScope(JmxTuiModeFocusPane focusedPane, JmxTuiUiMode mode) {
		if(focusedPane == null) {
			return null;
		}
		if(focusedPane == JmxTuiModeFocusPane.OUTPUT) {
			return TuiKeyRemapScope.OUTPUT;
		}
		if(focusedPane == JmxTuiModeFocusPane.COMMAND) {
			return TuiKeyRemapScope.COMMAND;
		}
		if(focusedPane == JmxTuiModeFocusPane.SEARCH) {
			if(mode != null && (ThreadUiMode.ID.equals(mode.getId()) || RecordUiMode.ID.equals(mode.getId()))) {
				return TuiKeyRemapScope.SEARCH;
			}
			return null;
		}
		if(focusedPane == JmxTuiModeFocusPane.FEATURES) {
			if(mode != null && MBeanUiMode.ID.equals(mode.getId())) {
				return TuiKeyRemapScope.FEATURES;
			}
			return null;
		}
		if(focusedPane == JmxTuiModeFocusPane.MBEANS) {
			if(mode != null && MBeanUiMode.ID.equals(mode.getId())) {
				return TuiKeyRemapScope.MBEAN;
			}
			if(mode != null && (ThreadUiMode.ID.equals(mode.getId()) || RecordUiMode.ID.equals(mode.getId()))) {
				return TuiKeyRemapScope.THREAD;
			}
		}
		return null;
	}

	public static String formatRecordDumpViewKeysHint() {
		return label(TuiKeyAction.RECORD_VIEW_METHOD) + ":Method call statistics, "
			+ label(TuiKeyAction.RECORD_VIEW_THREAD) + ":Thread stacktrace tree, "
			+ label(TuiKeyAction.RECORD_VIEW_STACK) + ":Stacktrace at point, "
			+ label(TuiKeyAction.RECORD_VIEW_SEARCH) + ":Search method/class";
	}

	public static String formatRecordDumpViewHelpLine() {
		return label(TuiKeyAction.RECORD_VIEW_METHOD) + "/" + label(TuiKeyAction.RECORD_VIEW_THREAD) + "/"
			+ label(TuiKeyAction.RECORD_VIEW_STACK) + "/" + label(TuiKeyAction.RECORD_VIEW_SEARCH)
			+ ": record view method/thread/stack/search for selected threads (Dump view only)";
	}

	public static String label(TuiKeyAction action) {
		return current().getActionLabel(action);
	}

	private void notifyChanged() {
		Runnable listener = changeListener;
		if(listener != null) {
			listener.run();
		}
	}

	private static KeyStroke cloneKeyStroke(KeyStroke keyStroke) {
		if(keyStroke == null) {
			return null;
		}
		KeyType type = keyStroke.getKeyType();
		if(type == KeyType.Character) {
			return new KeyStroke(keyStroke.getCharacter(), keyStroke.isCtrlDown(), keyStroke.isAltDown(), keyStroke.isShiftDown());
		}
		return new KeyStroke(type, keyStroke.isCtrlDown(), keyStroke.isAltDown(), keyStroke.isShiftDown());
	}
}
