package dal.tool.trace.jmxer.tui;

import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

/**
 * Global and list-navigation key detection for the TUI.
 */
final class JmxTuiKeys {
	private JmxTuiKeys() {}

	/**
	 * Detect Ctrl+C across platforms. Windows native console delivers it as KeyType.Character
	 * with isCtrlDown=true, but some environments translate the 0x03 byte into KeyType.EOF.
	 */
	static boolean isExitKey(KeyStroke key) {
		if(key == null) {
			return false;
		}
		KeyType type = key.getKeyType();
		if(type == KeyType.EOF) {
			return true;
		}
		if(type == KeyType.Character && key.isCtrlDown()) {
			Character ch = key.getCharacter();
			if(ch != null) {
				char c = ch.charValue();
				if(c == 'c' || c == 'C') {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Shortcuts handled for every pane before input reaches the focused component
	 * (see {@link JmxTuiWindow}). Tab / Shift+Tab are not listed here: they follow default focus traversal.
	 */
	static boolean isGlobalShortcut(KeyStroke key) {
		if(key == null) {
			return false;
		}
		if(key.getKeyType() == KeyType.F1) {
			return true;
		}
		if(key.getKeyType() == KeyType.F2) {
			return true;
		}
		if(isExitKey(key)) {
			return true;
		}
		if(isCtrlFeaturesToggle(key)) {
			return true;
		}
		if(isPaneResizeKey(key)) {
			return true;
		}
		return false;
	}

	/** Shift+arrow — resize resizable panes between Status and Command (see {@link JmxTuiApp}). */
	static boolean isPaneResizeKey(KeyStroke key) {
		if(key == null || !key.isShiftDown()) {
			return false;
		}
		KeyType type = key.getKeyType();
		return type == KeyType.ArrowLeft || type == KeyType.ArrowRight
			|| type == KeyType.ArrowUp || type == KeyType.ArrowDown;
	}

	static boolean isPaneResizeHorizontal(KeyStroke key) {
		if(key == null || !key.isShiftDown()) {
			return false;
		}
		KeyType type = key.getKeyType();
		return type == KeyType.ArrowLeft || type == KeyType.ArrowRight;
	}

	static boolean isPaneResizeVertical(KeyStroke key) {
		if(key == null || !key.isShiftDown()) {
			return false;
		}
		KeyType type = key.getKeyType();
		return type == KeyType.ArrowUp || type == KeyType.ArrowDown;
	}

	static boolean isF2Key(KeyStroke key) {
		return key != null && key.getKeyType() == KeyType.F2;
	}

	static boolean isCtrlFeaturesToggle(KeyStroke key) {
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

	/** Ctrl+L — local to the Output pane: toggle wrap vs single-line display. */
	static boolean isCtrlToggleOutputWrap(KeyStroke key) {
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

	/** Ctrl+O — local to the Output pane only. */
	static boolean isCtrlSaveOutput(KeyStroke key) {
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

	static boolean isFocusTraversalFromListNavigation(KeyStroke keyStroke, Interactable.Result r) {
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
