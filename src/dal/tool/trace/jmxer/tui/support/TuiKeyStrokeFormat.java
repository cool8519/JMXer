package dal.tool.trace.jmxer.tui.support;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

/**
 * Lanterna KeyStroke 라벨 변환과 리매핑 유효성 검사.
 *
 */
public final class TuiKeyStrokeFormat {

	private TuiKeyStrokeFormat() {}

	public static boolean matches(KeyStroke bound, KeyStroke pressed) {
		if(bound == null || pressed == null) {
			return false;
		}
		if(bound.isCtrlDown() != pressed.isCtrlDown()
				|| bound.isAltDown() != pressed.isAltDown()
				|| bound.isShiftDown() != pressed.isShiftDown()) {
			return false;
		}
		KeyType boundType = bound.getKeyType();
		KeyType pressedType = pressed.getKeyType();
		if(boundType != pressedType) {
			return false;
		}
		if(boundType == KeyType.Character) {
			Character boundChar = bound.getCharacter();
			Character pressedChar = pressed.getCharacter();
			if(boundChar == null || pressedChar == null) {
				return boundChar == pressedChar;
			}
			return Character.toUpperCase(boundChar.charValue()) == Character.toUpperCase(pressedChar.charValue());
		}
		return true;
	}

	public static boolean isArrowKeyType(KeyType type) {
		return type == KeyType.ArrowLeft || type == KeyType.ArrowRight
			|| type == KeyType.ArrowUp || type == KeyType.ArrowDown;
	}

	public static boolean hasAnyModifier(KeyStroke keyStroke) {
		return keyStroke != null
			&& (keyStroke.isCtrlDown() || keyStroke.isAltDown() || keyStroke.isShiftDown());
	}

	public static boolean matchesArrowGroup(KeyStroke bound, KeyStroke pressed) {
		if(bound == null || pressed == null || !isArrowKeyType(pressed.getKeyType())) {
			return false;
		}
		return bound.isCtrlDown() == pressed.isCtrlDown()
			&& bound.isAltDown() == pressed.isAltDown()
			&& bound.isShiftDown() == pressed.isShiftDown();
	}

	public static String formatModifierPrefix(KeyStroke keyStroke) {
		if(keyStroke == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		if(keyStroke.isCtrlDown()) {
			sb.append("Ctrl+");
		}
		if(keyStroke.isAltDown()) {
			sb.append("Alt+");
		}
		if(keyStroke.isShiftDown()) {
			sb.append("Shift+");
		}
		return sb.toString();
	}

	public static String formatArrowGroupLabel(KeyStroke keyStroke) {
		if(keyStroke == null) {
			return "";
		}
		return formatModifierPrefix(keyStroke) + "Arrows";
	}

	public static String formatHorizontalArrowsLabel(KeyStroke keyStroke) {
		if(keyStroke == null) {
			return "";
		}
		return formatModifierPrefix(keyStroke) + "Left/Right";
	}

	public static String formatVerticalArrowsLabel(KeyStroke keyStroke) {
		if(keyStroke == null) {
			return "";
		}
		return formatModifierPrefix(keyStroke) + "Up/Down";
	}

	public static String formatLabel(KeyStroke keyStroke) {
		if(keyStroke == null) {
			return "";
		}
		if(JmxTuiKeys.isExitKey(keyStroke)) {
			return "Ctrl+C";
		}
		if(JmxTuiKeys.isCtrlSuspend(keyStroke)) {
			return "Ctrl+Z";
		}
		KeyType type = keyStroke.getKeyType();
		if(type == KeyType.ReverseTab && !keyStroke.isShiftDown()) {
			return formatModifierPrefix(keyStroke) + "Shift+Tab";
		}
		StringBuilder sb = new StringBuilder(formatModifierPrefix(keyStroke));
		if(type == KeyType.Character) {
			Character ch = keyStroke.getCharacter();
			if(ch != null) {
				char c = ch.charValue();
				if(c == ' ') {
					sb.append("Space");
				} else if(c < 32) {
					sb.append("Ctrl+").append((char)('@' + c));
				} else {
					sb.append(Character.toUpperCase(c));
				}
			}
		} else {
			sb.append(formatKeyType(type));
		}
		return sb.toString();
	}

	public static boolean isReservedForRemap(KeyStroke keyStroke) {
		if(keyStroke == null) {
			return true;
		}
		if(JmxTuiKeys.isCtrlSuspend(keyStroke) || JmxTuiKeys.isExitKey(keyStroke)) {
			return true;
		}
		if(JmxTuiKeys.isKeyRemapDialogKey(keyStroke)) {
			return true;
		}
		KeyType type = keyStroke.getKeyType();
		if(type == KeyType.Escape || type == KeyType.Tab || type == KeyType.ReverseTab) {
			return true;
		}
		if(type == KeyType.ArrowUp || type == KeyType.ArrowDown
				|| type == KeyType.ArrowLeft || type == KeyType.ArrowRight
				|| type == KeyType.PageUp || type == KeyType.PageDown
				|| type == KeyType.Home || type == KeyType.End) {
			return true;
		}
		if(type == KeyType.F12) {
			return true;
		}
		if(isReservedFunctionKey(keyStroke, KeyType.F1)
				|| isReservedFunctionKey(keyStroke, KeyType.F3)) {
			return true;
		}
		return false;
	}

	private static boolean isReservedFunctionKey(KeyStroke keyStroke, KeyType functionKey) {
		if(keyStroke.getKeyType() != functionKey) {
			return false;
		}
		return !keyStroke.isShiftDown() && !keyStroke.isCtrlDown() && !keyStroke.isAltDown();
	}

	private static String formatKeyType(KeyType type) {
		if(type == null) {
			return "";
		}
		String name = type.name();
		if(name.startsWith("F") && name.length() > 1) {
			return name;
		}
		if(type == KeyType.Enter) {
			return "Enter";
		}
		if(type == KeyType.Backspace) {
			return "Backspace";
		}
		if(type == KeyType.Insert) {
			return "Insert";
		}
		if(type == KeyType.Delete) {
			return "Delete";
		}
		if(type == KeyType.ReverseTab) {
			return "Tab";
		}
		if(type == KeyType.ArrowLeft) {
			return "Left";
		}
		if(type == KeyType.ArrowRight) {
			return "Right";
		}
		if(type == KeyType.ArrowUp) {
			return "Up";
		}
		if(type == KeyType.ArrowDown) {
			return "Down";
		}
		return name;
	}
}
