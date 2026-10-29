package dal.tool.trace.jmxer.tui.support;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

/**
 * TUI에서 리매핑 가능한 단축키 동작.
 *
 */
public enum TuiKeyAction {
	INFO(TuiKeyRemapScope.PANE, "Info", new KeyStroke(KeyType.F2)),
	PANE_RESIZE(TuiKeyRemapScope.GLOBAL, "Resize", new KeyStroke(KeyType.ArrowLeft, false, false, true)),
	OUTPUT_PANE_TOGGLE(TuiKeyRemapScope.GLOBAL, "Output Pane Toggle", new KeyStroke(Character.valueOf('o'), true, false)),
	FEATURES_PANE_TOGGLE(TuiKeyRemapScope.GLOBAL, "Features Pane Toggle", new KeyStroke(Character.valueOf('f'), true, false)),
	RECORD_SHOW_EMPTY_THREAD_TOGGLE(TuiKeyRemapScope.GLOBAL, "Show Empty Thread Toggle", new KeyStroke(Character.valueOf('y'), true, false)),
	RECORD_VIEW_TYPE_TOGGLE(TuiKeyRemapScope.GLOBAL, "Record View Type Toggle", new KeyStroke(KeyType.F4)),

	RECORD_SAMPLE(TuiKeyRemapScope.THREAD, "Record Sample (Cursor Thread)", new KeyStroke(KeyType.F11)),
	RECORD_SAMPLE_SELECTED(TuiKeyRemapScope.THREAD, "Record Sample (Selected Threads)", new KeyStroke(KeyType.F11, true, false)),
	RECORD_DUMP_LOAD(TuiKeyRemapScope.GLOBAL, "Record Dump Load", new KeyStroke(KeyType.F7)),
	RECORD_DUMP_SAVE(TuiKeyRemapScope.GLOBAL, "Record Dump Save", new KeyStroke(KeyType.F8)),
	RECORD_DUMP_INFO(TuiKeyRemapScope.GLOBAL, "Record Dump Info", new KeyStroke(KeyType.F9)),
	RECORD_VIEW_METHOD(TuiKeyRemapScope.GLOBAL, "Record View Method", new KeyStroke(KeyType.F1, false, false, true)),
	RECORD_VIEW_THREAD(TuiKeyRemapScope.GLOBAL, "Record View Thread", new KeyStroke(KeyType.F2, false, false, true)),
	RECORD_VIEW_STACK(TuiKeyRemapScope.GLOBAL, "Record View Stack", new KeyStroke(KeyType.F3, false, false, true)),
	RECORD_VIEW_SEARCH(TuiKeyRemapScope.GLOBAL, "Record View Search", new KeyStroke(KeyType.F4, false, false, true)),

	THREAD_AUTO_REFRESH(TuiKeyRemapScope.GLOBAL, "Thread Auto Refresh Toggle", new KeyStroke(Character.valueOf('r'), true, false)),
	THREAD_STACKTRACE_TARGET(TuiKeyRemapScope.GLOBAL, "Stacktrace Target Toggle", new KeyStroke(Character.valueOf('t'), true, false)),
	THREAD_SELECTED_TARGET(TuiKeyRemapScope.GLOBAL, "Selected Target Toggle", new KeyStroke(Character.valueOf('g'), true, false)),
	THREAD_EDIT_SELECTION(TuiKeyRemapScope.GLOBAL, "Edit Thread Selection", new KeyStroke(Character.valueOf('e'), true, false)),
	THREAD_REQUEST_ENTRY_FILE(TuiKeyRemapScope.GLOBAL, "Request-Entry File", new KeyStroke(Character.valueOf('u'), true, false)),
	OUTPUT_PRETTY_PRINT(TuiKeyRemapScope.GLOBAL, "Pretty Print Toggle", new KeyStroke(Character.valueOf('p'), true, false)),

	MBEAN_SET_DEFAULT(TuiKeyRemapScope.MBEAN, "Set Current MBean as Default", new KeyStroke(KeyType.Enter)),
	FEATURE_INVOKE(TuiKeyRemapScope.FEATURES, "Invoke Feature", new KeyStroke(KeyType.Enter)),

	THREAD_FORCE_REFRESH(TuiKeyRemapScope.THREAD, "Force Refresh", new KeyStroke(KeyType.F5)),
	THREAD_PRIMARY_ACTION(TuiKeyRemapScope.THREAD, "Stacktrace / Analysis", new KeyStroke(KeyType.Enter)),
	THREAD_TOGGLE_CURRENT(TuiKeyRemapScope.THREAD, "Toggle Current Thread", new KeyStroke(Character.valueOf(' '), false, false)),
	THREAD_BULK_STACKTRACE(TuiKeyRemapScope.THREAD, "Bulk Stacktrace / Thread Analysis", new KeyStroke(Character.valueOf('b'), true, false)),
	THREAD_TOGGLE_ALL(TuiKeyRemapScope.THREAD, "Toggle All Visible Threads", new KeyStroke(Character.valueOf('a'), true, false)),
	THREAD_CLEAR_SELECTED(TuiKeyRemapScope.THREAD, "Clear Selected Threads", new KeyStroke(Character.valueOf('d'), true, false)),

	THREAD_SORT(TuiKeyRemapScope.THREAD, "Sort", new KeyStroke(KeyType.F6)),

	OUTPUT_SAVE(TuiKeyRemapScope.OUTPUT, "Output Save to File", new KeyStroke(KeyType.F10)),
	OUTPUT_WRAP(TuiKeyRemapScope.OUTPUT, "Output Wrap Toggle", new KeyStroke(Character.valueOf('l'), true, false)),

	COMMAND_PASTE(TuiKeyRemapScope.COMMAND, "Command Paste from Clipboard", new KeyStroke(Character.valueOf('v'), true, false));

	private final TuiKeyRemapScope scope;
	private final String displayName;
	private final KeyStroke defaultKeyStroke;

	TuiKeyAction(TuiKeyRemapScope scope, String displayName, KeyStroke defaultKeyStroke) {
		this.scope = scope;
		this.displayName = displayName;
		this.defaultKeyStroke = defaultKeyStroke;
	}

	public TuiKeyRemapScope getScope() {
		return scope;
	}

	public String getDisplayName() {
		return displayName;
	}

	public KeyStroke getDefaultKeyStroke() {
		return defaultKeyStroke;
	}
}
