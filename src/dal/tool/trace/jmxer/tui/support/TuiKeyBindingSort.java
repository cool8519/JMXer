package dal.tool.trace.jmxer.tui.support;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

/**
 * TUI 키 바인딩 표시 순서. 수정자(없음 → Ctrl → Shift → …) 후 키 종류(F키·Enter·문자 등)로 정렬한다.
 *
 */
public final class TuiKeyBindingSort {

	private static final int ORDER_FUNCTION_KEY_BASE = 1000;
	private static final int ORDER_SPECIAL_KEY_BASE = 2000;
	private static final int ORDER_LETTER_KEY_BASE = 3000;
	private static final int ORDER_OTHER_KEY_BASE = 5000;

	private TuiKeyBindingSort() {}

	public static int compare(KeyStroke left, KeyStroke right) {
		if(left == right) {
			return 0;
		}
		if(left == null) {
			return 1;
		}
		if(right == null) {
			return -1;
		}
		int modifier = modifierOrder(left) - modifierOrder(right);
		if(modifier != 0) {
			return modifier;
		}
		return baseKeyOrder(left) - baseKeyOrder(right);
	}

	public static void sortByBinding(List<TuiKeyAction> actions, final TuiKeyBindings bindings) {
		if(actions == null || actions.size() < 2) {
			return;
		}
		Collections.sort(actions, new Comparator<TuiKeyAction>() {
			@Override
			public int compare(TuiKeyAction left, TuiKeyAction right) {
				if(left == right) {
					return 0;
				}
				if(left == null) {
					return 1;
				}
				if(right == null) {
					return -1;
				}
				KeyStroke leftKey = bindings == null ? null : bindings.getBinding(left);
				KeyStroke rightKey = bindings == null ? null : bindings.getBinding(right);
				int keyCompare = TuiKeyBindingSort.compare(leftKey, rightKey);
				if(keyCompare != 0) {
					return keyCompare;
				}
				return left.getDisplayName().compareToIgnoreCase(right.getDisplayName());
			}
		});
	}

	private static int modifierOrder(KeyStroke keyStroke) {
		boolean ctrl = keyStroke.isCtrlDown();
		boolean shift = keyStroke.isShiftDown();
		boolean alt = keyStroke.isAltDown();
		if(!ctrl && !shift && !alt) {
			return 0;
		}
		if(ctrl && !shift && !alt) {
			return 1;
		}
		if(!ctrl && shift && !alt) {
			return 2;
		}
		if(ctrl && shift && !alt) {
			return 3;
		}
		if(!ctrl && !shift && alt) {
			return 4;
		}
		if(ctrl && !shift && alt) {
			return 5;
		}
		if(!ctrl && shift && alt) {
			return 6;
		}
		return 7;
	}

	private static int baseKeyOrder(KeyStroke keyStroke) {
		KeyType type = keyStroke.getKeyType();
		if(type == KeyType.Character) {
			Character ch = keyStroke.getCharacter();
			if(ch != null) {
				char c = ch.charValue();
				if(c == ' ') {
					return ORDER_SPECIAL_KEY_BASE + 100;
				}
				return ORDER_LETTER_KEY_BASE + Character.toUpperCase(c);
			}
		}
		if(type != null) {
			String name = type.name();
			if(name.startsWith("F") && name.length() > 1) {
				int functionNumber = parseFunctionKeyNumber(name);
				if(functionNumber > 0) {
					return ORDER_FUNCTION_KEY_BASE + functionNumber;
				}
			}
		}
		if(type == KeyType.Enter) {
			return ORDER_SPECIAL_KEY_BASE;
		}
		if(type == KeyType.Tab) {
			return ORDER_SPECIAL_KEY_BASE + 10;
		}
		if(type == KeyType.Escape) {
			return ORDER_SPECIAL_KEY_BASE + 20;
		}
		if(type == KeyType.ArrowLeft || type == KeyType.ArrowRight
				|| type == KeyType.ArrowUp || type == KeyType.ArrowDown) {
			return ORDER_OTHER_KEY_BASE + type.ordinal();
		}
		return ORDER_OTHER_KEY_BASE + 100 + (type == null ? 0 : type.ordinal());
	}

	private static int parseFunctionKeyNumber(String name) {
		try {
			return Integer.parseInt(name.substring(1));
		} catch(NumberFormatException e) {
			return -1;
		}
	}
}
