package dal.tool.trace.jmxer.tui.support;

import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

/**
 * TUI 전역·목록 탐색 키 입력 판별.
 *
 */
public final class JmxTuiKeys {
	private JmxTuiKeys() {}

	private static final char CTRL_C_CHAR = 3;
	private static final char CTRL_Z_CHAR = 26;

	/**
	 * Ctrl+Z/일시 중지 키. Unix에서 프로세스가 백그라운드로 가거나 Windows에서 EOF로 오인되는 것을 막기 위해 삼킨다.
	 */
	public static boolean isCtrlSuspend(KeyStroke key) {
		if(key == null) {
			return false;
		}
		KeyType type = key.getKeyType();
		if(type == KeyType.EOF) {
			Character ch = key.getCharacter();
			// Windows에서 Ctrl+Z가 character 없는 EOF로 오는 경우가 많다.
			if(ch == null) {
				return true;
			}
			return ch.charValue() == CTRL_Z_CHAR;
		}
		if(type == KeyType.Character) {
			Character ch = key.getCharacter();
			if(ch == null) {
				return false;
			}
			char c = ch.charValue();
			if(c == CTRL_Z_CHAR) {
				return true;
			}
			return key.isCtrlDown() && (c == 'z' || c == 'Z');
		}
		return false;
	}

	/**
	 * 플랫폼별 Ctrl+C 감지. Windows 네이티브 콘솔은 Character+Ctrl이지만, 일부 환경은 0x03을 EOF로 넘긴다.
	 */
	public static boolean isExitKey(KeyStroke key) {
		if(key == null || isCtrlSuspend(key)) {
			return false;
		}
		KeyType type = key.getKeyType();
		if(type == KeyType.EOF) {
			Character ch = key.getCharacter();
			if(ch == null) {
				return false;
			}
			return ch.charValue() == CTRL_C_CHAR;
		}
		if(type == KeyType.Character && key.isCtrlDown()) {
			Character ch = key.getCharacter();
			if(ch != null) {
				char c = ch.charValue();
				if(c == 'c' || c == 'C' || c == CTRL_C_CHAR) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 포커스된 컴포넌트보다 먼저 처리하는 전역 단축키.
	 * JmxTuiWindow에서 선처리한다. Tab/Shift+Tab은 기본 포커스 이동이므로 여기에 포함하지 않는다.
	 */
	public static boolean isGlobalShortcut(KeyStroke key) {
		if(key == null) {
			return false;
		}
		if(key.getKeyType() == KeyType.F1) {
			return true;
		}
		if(TuiKeyBindings.current().matches(TuiKeyAction.INFO, key)) {
			return true;
		}
		if(key.getKeyType() == KeyType.F3) {
			return true;
		}
		if(TuiKeyBindings.current().matches(TuiKeyAction.RECORD_VIEW_TYPE_TOGGLE, key)) {
			return true;
		}
		if(isKeyRemapDialogKey(key)) {
			return true;
		}
		if(isCtrlToggleThreadRefresh(key)) {
			return true;
		}
		if(isCtrlToggleSelectedTarget(key)) {
			return true;
		}
		if(isExitKey(key)) {
			return true;
		}
		if(isCtrlFeaturesToggle(key)) {
			return true;
		}
		if(isCtrlToggleShowEmptyThread(key)) {
			return true;
		}
		if(isCtrlToggleOutputPane(key)) {
			return true;
		}
		if(isPaneResizeKey(key)) {
			return true;
		}
		return false;
	}

	public static boolean isPaneResizeKey(KeyStroke key) {
		return TuiKeyBindings.current().matches(TuiKeyAction.PANE_RESIZE, key);
	}

	public static boolean isPaneResizeHorizontal(KeyStroke key) {
		if(!isPaneResizeKey(key)) {
			return false;
		}
		KeyType type = key.getKeyType();
		return type == KeyType.ArrowLeft || type == KeyType.ArrowRight;
	}

	public static boolean isPaneResizeVertical(KeyStroke key) {
		if(!isPaneResizeKey(key)) {
			return false;
		}
		KeyType type = key.getKeyType();
		return type == KeyType.ArrowUp || type == KeyType.ArrowDown;
	}

	public static boolean isInfoKey(KeyStroke key) {
		return TuiKeyBindings.current().matches(TuiKeyAction.INFO, key);
	}

	public static boolean isF2Key(KeyStroke key) {
		return isInfoKey(key);
	}

	public static boolean isF3Key(KeyStroke key) {
		return key != null && key.getKeyType() == KeyType.F3 && !key.isShiftDown();
	}

	public static boolean isF4Key(KeyStroke key) {
		return TuiKeyBindings.current().matches(TuiKeyAction.RECORD_VIEW_TYPE_TOGGLE, key);
	}

	public static boolean isF5Key(KeyStroke key) {
		return TuiKeyBindings.current().matches(TuiKeyAction.THREAD_FORCE_REFRESH, key);
	}

	public static boolean isRecordSampleKey(KeyStroke key) {
		return TuiKeyBindings.current().matches(TuiKeyAction.RECORD_SAMPLE, key);
	}

	public static boolean isRecordSampleSelectedKey(KeyStroke key) {
		return TuiKeyBindings.current().matches(TuiKeyAction.RECORD_SAMPLE_SELECTED, key);
	}

	public static boolean isThreadSortKey(KeyStroke key) {
		return TuiKeyBindings.current().matches(TuiKeyAction.THREAD_SORT, key);
	}

	public static boolean isF7Key(KeyStroke key) {
		return TuiKeyBindings.current().matches(TuiKeyAction.RECORD_DUMP_LOAD, key);
	}

	public static boolean isF8Key(KeyStroke key) {
		return TuiKeyBindings.current().matches(TuiKeyAction.RECORD_DUMP_SAVE, key);
	}

	public static boolean isF9Key(KeyStroke key) {
		return TuiKeyBindings.current().matches(TuiKeyAction.RECORD_DUMP_INFO, key);
	}

	public static boolean isF10Key(KeyStroke key) {
		return key != null && key.getKeyType() == KeyType.F10;
	}

	public static boolean isF12Key(KeyStroke key) {
		return key != null && key.getKeyType() == KeyType.F12;
	}

	public static boolean isKeyRemapDialogKey(KeyStroke key) {
		return isF12Key(key) && !key.isShiftDown() && !key.isCtrlDown() && !key.isAltDown();
	}

	public static boolean isShiftRecordViewMethod(KeyStroke key) {
		return TuiKeyBindings.current().matches(TuiKeyAction.RECORD_VIEW_METHOD, key);
	}

	public static boolean isShiftRecordViewThread(KeyStroke key) {
		return TuiKeyBindings.current().matches(TuiKeyAction.RECORD_VIEW_THREAD, key);
	}

	public static boolean isShiftRecordViewStack(KeyStroke key) {
		return TuiKeyBindings.current().matches(TuiKeyAction.RECORD_VIEW_STACK, key);
	}

	public static boolean isShiftRecordViewSearch(KeyStroke key) {
		return TuiKeyBindings.current().matches(TuiKeyAction.RECORD_VIEW_SEARCH, key);
	}

	public static boolean isF1HelpKey(KeyStroke key) {
		return key != null && key.getKeyType() == KeyType.F1 && !key.isShiftDown();
	}

	public static boolean isCtrlToggleThreadRefresh(KeyStroke key) {
		return TuiKeyBindings.current().matches(TuiKeyAction.THREAD_AUTO_REFRESH, key);
	}

	public static boolean isThreadBulkStacktraceKey(KeyStroke key) {
		return TuiKeyBindings.current().matches(TuiKeyAction.THREAD_BULK_STACKTRACE, key);
	}

	public static boolean isThreadPrimaryActionKey(KeyStroke key) {
		return TuiKeyBindings.current().matches(TuiKeyAction.THREAD_PRIMARY_ACTION, key);
	}

	public static boolean isMbeanSetDefaultKey(KeyStroke key) {
		return TuiKeyBindings.current().matches(TuiKeyAction.MBEAN_SET_DEFAULT, key);
	}

	public static boolean isFeatureInvokeKey(KeyStroke key) {
		return TuiKeyBindings.current().matches(TuiKeyAction.FEATURE_INVOKE, key);
	}

	public static boolean isCtrlToggleStacktraceTarget(KeyStroke key) {
		return TuiKeyBindings.current().matches(TuiKeyAction.THREAD_STACKTRACE_TARGET, key);
	}

	public static boolean isCtrlToggleSelectedTarget(KeyStroke key) {
		return TuiKeyBindings.current().matches(TuiKeyAction.THREAD_SELECTED_TARGET, key);
	}

	public static boolean isCtrlEditThreadSelection(KeyStroke key) {
		return TuiKeyBindings.current().matches(TuiKeyAction.THREAD_EDIT_SELECTION, key);
	}

	public static boolean isCtrlEditRequestEntryFile(KeyStroke key) {
		return TuiKeyBindings.current().matches(TuiKeyAction.THREAD_REQUEST_ENTRY_FILE, key);
	}

	public static boolean isCtrlTogglePrettyPrint(KeyStroke key) {
		return TuiKeyBindings.current().matches(TuiKeyAction.OUTPUT_PRETTY_PRINT, key);
	}

	public static boolean isCtrlFeaturesToggle(KeyStroke key) {
		return TuiKeyBindings.current().matches(TuiKeyAction.FEATURES_PANE_TOGGLE, key);
	}

	public static boolean isCtrlToggleShowEmptyThread(KeyStroke key) {
		return TuiKeyBindings.current().matches(TuiKeyAction.RECORD_SHOW_EMPTY_THREAD_TOGGLE, key);
	}

	public static boolean isCtrlToggleOutputPane(KeyStroke key) {
		return TuiKeyBindings.current().matches(TuiKeyAction.OUTPUT_PANE_TOGGLE, key);
	}

	public static boolean isCtrlToggleOutputWrap(KeyStroke key) {
		return TuiKeyBindings.current().matches(TuiKeyAction.OUTPUT_WRAP, key);
	}

	public static boolean isSaveOutputKey(KeyStroke key) {
		return TuiKeyBindings.current().matches(TuiKeyAction.OUTPUT_SAVE, key);
	}

	public static boolean isCtrlPaste(KeyStroke key) {
		return TuiKeyBindings.current().matches(TuiKeyAction.COMMAND_PASTE, key);
	}

	/**
	 * 수정자 없는 Space. Lanterna ActionListBox가 Enter처럼 처리하므로 호출 측에서 별도 처리해야 한다.
	 */
	public static boolean isPlainSpaceKey(KeyStroke key) {
		if(key == null || key.getKeyType() != KeyType.Character) {
			return false;
		}
		if(key.isCtrlDown() || key.isShiftDown() || key.isAltDown()) {
			return false;
		}
		Character ch = key.getCharacter();
		return ch != null && ch.charValue() == ' ';
	}

	public static boolean isThreadToggleCurrentKey(KeyStroke key) {
		return TuiKeyBindings.current().matches(TuiKeyAction.THREAD_TOGGLE_CURRENT, key);
	}

	public static boolean isThreadToggleAllKey(KeyStroke key) {
		return TuiKeyBindings.current().matches(TuiKeyAction.THREAD_TOGGLE_ALL, key);
	}

	public static boolean isThreadClearSelectedKey(KeyStroke key) {
		return TuiKeyBindings.current().matches(TuiKeyAction.THREAD_CLEAR_SELECTED, key);
	}

	public static boolean isFocusTraversalFromListNavigation(KeyStroke keyStroke, Interactable.Result r) {
		if(keyStroke == null || r == null) {
			return false;
		}
		KeyType kt = keyStroke.getKeyType();
		boolean navKey = kt == KeyType.ArrowUp || kt == KeyType.ArrowDown
			|| kt == KeyType.ArrowLeft || kt == KeyType.ArrowRight
			|| kt == KeyType.PageUp || kt == KeyType.PageDown
			|| kt == KeyType.Home || kt == KeyType.End;
		if(!navKey) {
			return false;
		}
		return r == Interactable.Result.MOVE_FOCUS_UP
			|| r == Interactable.Result.MOVE_FOCUS_DOWN
			|| r == Interactable.Result.MOVE_FOCUS_LEFT
			|| r == Interactable.Result.MOVE_FOCUS_RIGHT
			|| r == Interactable.Result.MOVE_FOCUS_NEXT
			|| r == Interactable.Result.MOVE_FOCUS_PREVIOUS;
	}
}
