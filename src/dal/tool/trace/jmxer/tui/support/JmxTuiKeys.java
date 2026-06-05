package dal.tool.trace.jmxer.tui.support;

import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

/**
 * Global and list-navigation key detection for the TUI.
 */
public final class JmxTuiKeys {
	private JmxTuiKeys() {}

	/** ASCII ETX — Ctrl+C. */
	private static final char CTRL_C_CHAR = 3;
	/** ASCII SUB — Ctrl+Z (must not be treated as exit). */
	private static final char CTRL_Z_CHAR = 26;

	/**
	 * Ctrl+Z / suspend. Must be swallowed so the process is not backgrounded (Unix) or mistaken for exit (EOF on Windows).
	 */
	public static boolean isCtrlSuspend(KeyStroke key) {
		if(key == null) {
			return false;
		}
		KeyType type = key.getKeyType();
		if(type == KeyType.EOF) {
			Character ch = key.getCharacter();
			return ch != null && ch.charValue() == CTRL_Z_CHAR;
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
	 * Detect Ctrl+C across platforms. Windows native console delivers it as KeyType.Character
	 * with isCtrlDown=true, but some environments translate the 0x03 byte into KeyType.EOF.
	 */
	public static boolean isExitKey(KeyStroke key) {
		if(key == null || isCtrlSuspend(key)) {
			return false;
		}
		KeyType type = key.getKeyType();
		if(type == KeyType.EOF) {
			Character ch = key.getCharacter();
			if(ch == null) {
				// Legacy: bare EOF without a control byte — treat as Ctrl+C.
				return true;
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
	 * Shortcuts handled for every pane before input reaches the focused component
	 * (see {@link dal.tool.trace.jmxer.tui.app.JmxTuiWindow}). Tab / Shift+Tab are not listed here: they follow default focus traversal.
	 */
	public static boolean isGlobalShortcut(KeyStroke key) {
		if(key == null) {
			return false;
		}
		if(key.getKeyType() == KeyType.F1) {
			return true;
		}
		if(key.getKeyType() == KeyType.F2) {
			return true;
		}
		if(key.getKeyType() == KeyType.F3) {
			return true;
		}
		if(isCtrlToggleThreadRefresh(key)) {
			return true;
		}
		if(isExitKey(key)) {
			return true;
		}
		if(isCtrlFeaturesToggle(key)) {
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

	/** Shift+arrow — resize resizable panes between Status and Command (see {@link dal.tool.trace.jmxer.tui.app.JmxTuiApp}). */
	public static boolean isPaneResizeKey(KeyStroke key) {
		if(key == null || !key.isShiftDown()) {
			return false;
		}
		KeyType type = key.getKeyType();
		return type == KeyType.ArrowLeft || type == KeyType.ArrowRight
			|| type == KeyType.ArrowUp || type == KeyType.ArrowDown;
	}

	public static boolean isPaneResizeHorizontal(KeyStroke key) {
		if(key == null || !key.isShiftDown()) {
			return false;
		}
		KeyType type = key.getKeyType();
		return type == KeyType.ArrowLeft || type == KeyType.ArrowRight;
	}

	public static boolean isPaneResizeVertical(KeyStroke key) {
		if(key == null || !key.isShiftDown()) {
			return false;
		}
		KeyType type = key.getKeyType();
		return type == KeyType.ArrowUp || type == KeyType.ArrowDown;
	}

	public static boolean isF2Key(KeyStroke key) {
		return key != null && key.getKeyType() == KeyType.F2;
	}

	/** F3 — global: cycle UI modes. */
	public static boolean isF3Key(KeyStroke key) {
		return key != null && key.getKeyType() == KeyType.F3;
	}

	/** F4 — Thread UI mode: edit thread selection via ThreadList expression. */
	public static boolean isF4Key(KeyStroke key) {
		return key != null && key.getKeyType() == KeyType.F4;
	}

	/** F5 — Thread pane: force thread list refresh. */
	public static boolean isF5Key(KeyStroke key) {
		return key != null && key.getKeyType() == KeyType.F5;
	}

	/** Ctrl+R — Thread UI mode: toggle auto/manual refresh policy. */
	public static boolean isCtrlToggleThreadRefresh(KeyStroke key) {
		if(key == null || !key.isCtrlDown() || key.getKeyType() != KeyType.Character) {
			return false;
		}
		Character ch = key.getCharacter();
		if(ch == null) {
			return false;
		}
		char c = ch.charValue();
		return c == 'r' || c == 'R';
	}

	/** Ctrl+Shift+Enter — Thread UI mode: stacktrace for all selected threads (with confirmation). */
	public static boolean isCtrlShiftEnter(KeyStroke key) {
		if(key == null || !key.isCtrlDown() || !key.isShiftDown()) {
			return false;
		}
		return key.getKeyType() == KeyType.Enter;
	}

	/** Ctrl+T — Thread UI mode: toggle {@code STACKTRACE_TARGET}; shortcut shown on status line 3. */
	public static boolean isCtrlToggleStacktraceTarget(KeyStroke key) {
		if(key == null || !key.isCtrlDown() || key.getKeyType() != KeyType.Character) {
			return false;
		}
		Character ch = key.getCharacter();
		if(ch == null) {
			return false;
		}
		char c = ch.charValue();
		return c == 't' || c == 'T';
	}

	/** Ctrl+E — Thread UI mode (ACTIVE): edit {@code REQUEST_ENTRY_FILE}; shortcut shown on status line 3. */
	public static boolean isCtrlEditReqEntryFile(KeyStroke key) {
		if(key == null || !key.isCtrlDown() || key.getKeyType() != KeyType.Character) {
			return false;
		}
		Character ch = key.getCharacter();
		if(ch == null) {
			return false;
		}
		char c = ch.charValue();
		return c == 'e' || c == 'E';
	}

	/** Ctrl+P — MBean UI mode (Online): toggle {@code PRETTY_PRINT}; shortcut shown on status line 3. */
	public static boolean isCtrlTogglePrettyPrint(KeyStroke key) {
		if(key == null || !key.isCtrlDown() || key.getKeyType() != KeyType.Character) {
			return false;
		}
		Character ch = key.getCharacter();
		if(ch == null) {
			return false;
		}
		char c = ch.charValue();
		return c == 'p' || c == 'P';
	}

	public static boolean isCtrlFeaturesToggle(KeyStroke key) {
		if(key == null || !key.isCtrlDown() || key.getKeyType() != KeyType.Character) {
			return false;
		}
		Character ch = key.getCharacter();
		if(ch == null) {
			return false;
		}
		char c = ch.charValue();
		return c == 'f' || c == 'F';
	}

	/** Ctrl+O — global: toggle Output pane show/hide. */
	public static boolean isCtrlToggleOutputPane(KeyStroke key) {
		if(key == null || !key.isCtrlDown() || key.getKeyType() != KeyType.Character) {
			return false;
		}
		Character ch = key.getCharacter();
		if(ch == null) {
			return false;
		}
		char c = ch.charValue();
		return c == 'o' || c == 'O';
	}

	/** Ctrl+L — local to the Output pane: toggle wrap vs single-line display. */
	public static boolean isCtrlToggleOutputWrap(KeyStroke key) {
		if(key == null || !key.isCtrlDown() || key.getKeyType() != KeyType.Character) {
			return false;
		}
		Character ch = key.getCharacter();
		if(ch == null) {
			return false;
		}
		char c = ch.charValue();
		return c == 'l' || c == 'L';
	}

	/** Ctrl+S — local to the Output pane only: save all output to a file. */
	public static boolean isCtrlSaveOutput(KeyStroke key) {
		if(key == null || !key.isCtrlDown() || key.getKeyType() != KeyType.Character) {
			return false;
		}
		Character ch = key.getCharacter();
		if(ch == null) {
			return false;
		}
		char c = ch.charValue();
		return c == 's' || c == 'S';
	}

	/** Ctrl+V — paste from the system clipboard into a text input (Command pane). */
	public static boolean isCtrlPaste(KeyStroke key) {
		if(key == null || !key.isCtrlDown() || key.getKeyType() != KeyType.Character) {
			return false;
		}
		Character ch = key.getCharacter();
		if(ch == null) {
			return false;
		}
		char c = ch.charValue();
		return c == 'v' || c == 'V';
	}

	/** Plain space (no modifiers). Lanterna {@code ActionListBox} treats it like Enter; callers must handle it explicitly. */
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

	/** Ctrl+Shift+Space — Thread pane: select all displayed threads. */
	public static boolean isCtrlShiftSpace(KeyStroke key) {
		if(key == null || !key.isCtrlDown() || !key.isShiftDown()) {
			return false;
		}
		if(key.getKeyType() == KeyType.Character) {
			Character ch = key.getCharacter();
			return ch != null && ch.charValue() == ' ';
		}
		return false;
	}

	/** Ctrl+Shift+A — Thread UI: toggle all displayed threads (Thread or Search pane; fallback when Ctrl+Shift+Space is not delivered by the terminal). */
	public static boolean isCtrlShiftA(KeyStroke key) {
		if(key == null || !key.isCtrlDown() || !key.isShiftDown() || key.getKeyType() != KeyType.Character) {
			return false;
		}
		Character ch = key.getCharacter();
		if(ch == null) {
			return false;
		}
		char c = ch.charValue();
		return c == 'a' || c == 'A';
	}

	/** Ctrl+Shift+D — Thread UI: clear all thread selections (Thread or Search pane). */
	public static boolean isCtrlShiftD(KeyStroke key) {
		if(key == null || !key.isCtrlDown() || !key.isShiftDown() || key.getKeyType() != KeyType.Character) {
			return false;
		}
		Character ch = key.getCharacter();
		if(ch == null) {
			return false;
		}
		char c = ch.charValue();
		return c == 'd' || c == 'D';
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
