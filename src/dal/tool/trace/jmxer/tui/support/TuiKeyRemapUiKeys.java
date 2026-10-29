package dal.tool.trace.jmxer.tui.support;

/**
 * TUI 키 리매핑 다이얼로그 문구.
 *
 */
public final class TuiKeyRemapUiKeys {

	public static final String DIALOG_TITLE = "Remap Keys";
	public static final String DIALOG_DESCRIPTION =
		"Select an action and press Enter to remap its key.\n"
		+ "Keys that are already in use cannot be assigned.";
	public static final String GLOBAL_SECTION_LABEL = "[Global]";
	public static final int KEY_LIST_INDENT_COLS = 2;
	public static final String REMAPPED_MARKER = "*";
	public static final String NO_GLOBAL_KEYS_MESSAGE =
		indentKeyListLabel("(No remappable global keys for the current UI mode and view.)");
	public static final String WINDOW_SECTION_LABEL_PREFIX = "[Window - ";
	public static final String WINDOW_SECTION_LABEL_SUFFIX = "]";
	public static final String CAPTURE_HINT = "Press a new key combination (Esc to cancel).";
	public static final String RETRY_CAPTURE_HINT = "Press another key combination (Esc to cancel).";
	public static final String RETRY_RESIZE_CAPTURE_HINT =
		"Press Shift/Ctrl/Alt + Arrow key (Esc to cancel).";
	public static final String RESET_LABEL = "Reset Listed Keys";
	public static final String CLOSE_LABEL = "Close";
	public static final String RESERVED_MESSAGE = "[!] That key cannot be assigned because it is reserved.";
	public static final String INVALID_RESIZE_KEY_MESSAGE =
		"[!] Resize requires a modifier (Shift, Ctrl, or Alt) + Arrow key.";
	public static final String CONFLICT_MESSAGE_PREFIX = "[!] That key is already assigned to: ";
	public static final String FAILED_PREFIX = "[FAILED] Cannot assign [";
	public static final String FAILED_RESERVED_SUFFIX = "]: reserved shortcut.";
	public static final String FAILED_INVALID_RESIZE_SUFFIX = "]: requires Shift/Ctrl/Alt + Arrow.";
	public static final String FAILED_CONFLICT_INFIX = "]: already assigned to ";
	public static final String NO_KEYS_TITLE = "Key Remap";
	public static final String NO_KEYS_MESSAGE =
		"No remappable keys for the current UI mode, view, and window focus.\n"
		+ "Switch UI mode or focus Thread, Output, or Command and press F12.";

	public static String formatReservedFailure(String keyLabel) {
		return FAILED_PREFIX + safeKeyLabel(keyLabel) + FAILED_RESERVED_SUFFIX;
	}

	public static String formatInvalidResizeFailure(String keyLabel) {
		return FAILED_PREFIX + safeKeyLabel(keyLabel) + FAILED_INVALID_RESIZE_SUFFIX;
	}

	public static String formatConflictFailure(String keyLabel, String conflictActionLabel) {
		String target = (conflictActionLabel == null || conflictActionLabel.length() == 0)
			? "another action"
			: conflictActionLabel;
		return FAILED_PREFIX + safeKeyLabel(keyLabel) + FAILED_CONFLICT_INFIX + target + ".";
	}

	private static String safeKeyLabel(String keyLabel) {
		return (keyLabel == null || keyLabel.length() == 0) ? "Key" : keyLabel;
	}

	public static String indentKeyListLabel(String label) {
		return indentKeyListLabel(label, false);
	}

	public static String indentKeyListLabel(String label, boolean remappedFromDefault) {
		StringBuilder sb = new StringBuilder();
		if(remappedFromDefault) {
			sb.append(' ');
			sb.append(REMAPPED_MARKER);
		} else {
			for(int i = 0; i < KEY_LIST_INDENT_COLS; i++) {
				sb.append(' ');
			}
		}
		sb.append(label == null ? "" : label);
		return sb.toString();
	}

	private TuiKeyRemapUiKeys() {}
}
