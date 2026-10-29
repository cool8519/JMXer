package dal.tool.trace.jmxer.tui.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

/**
 * F1 도움말 섹션 빌더. 항목을 키 조합 순으로 정렬해 출력한다.
 *
 */
public final class TuiHelpSectionBuilder {

	public static final int PANE_ORDER_SEARCH = 10;
	public static final int PANE_ORDER_LIST = 20;
	public static final int PANE_ORDER_FEATURES = 30;
	public static final int PANE_ORDER_OUTPUT = 40;
	public static final int PANE_ORDER_COMMAND = 50;

	public static final KeyStroke KEY_HELP = new KeyStroke(KeyType.F1);
	public static final KeyStroke KEY_UI_MODE_CYCLE = new KeyStroke(KeyType.F3);
	public static final KeyStroke KEY_FOCUS_TAB = new KeyStroke(KeyType.Tab);
	public static final KeyStroke KEY_EXIT = new KeyStroke(Character.valueOf('c'), true, false);
	public static final KeyStroke KEY_REMAP_DIALOG = new KeyStroke(KeyType.F12);
	public static final KeyStroke KEY_RESIZE_HORIZONTAL = new KeyStroke(KeyType.ArrowLeft, false, false, true);
	public static final KeyStroke KEY_CTRL_P = new KeyStroke(Character.valueOf('p'), true, false);
	public static final KeyStroke KEY_SHIFT_TAB = new KeyStroke(KeyType.Tab, false, false, true);
	public static final KeyStroke KEY_RESIZE_VERTICAL = new KeyStroke(KeyType.ArrowUp, false, false, true);

	private final String sectionTitle;
	private final List<Entry> entries = new ArrayList<Entry>();

	public TuiHelpSectionBuilder(String sectionTitle) {
		this.sectionTitle = sectionTitle == null ? "" : sectionTitle;
	}

	public void addKeyLine(KeyStroke sortKey, String line) {
		if(line == null || line.length() == 0) {
			return;
		}
		entries.add(Entry.forKey(sortKey, line));
	}

	public void addActionLine(TuiKeyBindings bindings, TuiKeyAction action, String description) {
		if(action == null || description == null) {
			return;
		}
		KeyStroke sortKey = bindings == null ? action.getDefaultKeyStroke() : bindings.getBinding(action);
		addKeyLine(sortKey, TuiKeyBindings.label(action) + ": " + description);
	}

	public void addHorizontalResizeLine(TuiKeyBindings bindings, String description) {
		if(description == null) {
			return;
		}
		TuiKeyBindings active = bindings == null ? TuiKeyBindings.current() : bindings;
		addKeyLine(active.getHorizontalResizeSortKey(), active.getHorizontalResizeLabel() + ": " + description);
	}

	public void addVerticalResizeLine(TuiKeyBindings bindings, String description) {
		if(description == null) {
			return;
		}
		TuiKeyBindings active = bindings == null ? TuiKeyBindings.current() : bindings;
		addKeyLine(active.getVerticalResizeSortKey(), active.getVerticalResizeLabel() + ": " + description);
	}

	public void addPaneLine(int paneOrder, String line) {
		if(line == null || line.length() == 0) {
			return;
		}
		entries.add(Entry.forPane(paneOrder, line));
	}

	public void addSortedSlashKeysLine(TuiKeyBindings bindings, TuiKeyAction[] actions, String description) {
		if(actions == null || actions.length == 0) {
			return;
		}
		List<TuiKeyAction> sorted = new ArrayList<TuiKeyAction>();
		for(int i = 0; i < actions.length; i++) {
			if(actions[i] != null) {
				sorted.add(actions[i]);
			}
		}
		if(sorted.isEmpty()) {
			return;
		}
		TuiKeyBindingSort.sortByBinding(sorted, bindings);
		KeyStroke sortKey = bindings == null ? sorted.get(0).getDefaultKeyStroke() : bindings.getBinding(sorted.get(0));
		addKeyLine(sortKey, formatSortedSlashKeysLine(bindings, actions, description));
	}

	public void appendTo(StringBuilder sb) {
		if(sb == null) {
			return;
		}
		sb.append(sectionTitle).append('\n');
		Collections.sort(entries, ENTRY_COMPARATOR);
		for(int i = 0; i < entries.size(); i++) {
			sb.append(TuiHelpTextFormat.itemLine(entries.get(i).line)).append('\n');
		}
	}

	public static String joinSortedActionClauses(TuiKeyBindings bindings, ActionClause[] clauses) {
		if(clauses == null || clauses.length == 0) {
			return "";
		}
		List<ActionClause> sorted = new ArrayList<ActionClause>();
		for(int i = 0; i < clauses.length; i++) {
			if(clauses[i] != null) {
				sorted.add(clauses[i]);
			}
		}
		Collections.sort(sorted, new Comparator<ActionClause>() {
			@Override
			public int compare(ActionClause left, ActionClause right) {
				if(left == right) {
					return 0;
				}
				if(left == null) {
					return 1;
				}
				if(right == null) {
					return -1;
				}
				KeyStroke leftKey = bindings == null ? left.action.getDefaultKeyStroke() : bindings.getBinding(left.action);
				KeyStroke rightKey = bindings == null ? right.action.getDefaultKeyStroke() : bindings.getBinding(right.action);
				int keyCompare = TuiKeyBindingSort.compare(leftKey, rightKey);
				if(keyCompare != 0) {
					return keyCompare;
				}
				return actionLabel(bindings, left).compareToIgnoreCase(actionLabel(bindings, right));
			}
		});
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < sorted.size(); i++) {
			if(i > 0) {
				sb.append("; ");
			}
			ActionClause clause = sorted.get(i);
			sb.append(actionLabel(bindings, clause)).append(" — ").append(clause.description);
		}
		return sb.toString();
	}

	public static String formatSortedSlashKeysLine(TuiKeyBindings bindings, TuiKeyAction[] actions, String description) {
		if(actions == null || actions.length == 0) {
			return description == null ? "" : description;
		}
		List<TuiKeyAction> sorted = new ArrayList<TuiKeyAction>();
		for(int i = 0; i < actions.length; i++) {
			if(actions[i] != null) {
				sorted.add(actions[i]);
			}
		}
		TuiKeyBindingSort.sortByBinding(sorted, bindings);
		StringBuilder keys = new StringBuilder();
		for(int i = 0; i < sorted.size(); i++) {
			if(i > 0) {
				keys.append('/');
			}
			keys.append(bindings == null ? TuiKeyBindings.label(sorted.get(i)) : bindings.getActionLabel(sorted.get(i)));
		}
		if(description == null || description.length() == 0) {
			return keys.toString();
		}
		return keys.append(": ").append(description).toString();
	}

	public static ActionClause actionPair(TuiKeyBindings bindings, TuiKeyAction first, TuiKeyAction second,
			String description) {
		String label = actionLabel(bindings, first) + "/" + actionLabel(bindings, second);
		return new ActionClause(first, description, label);
	}

	private static String actionLabel(TuiKeyBindings bindings, ActionClause clause) {
		if(clause.customKeyLabel != null) {
			return clause.customKeyLabel;
		}
		return actionLabel(bindings, clause.action);
	}

	private static String actionLabel(TuiKeyBindings bindings, TuiKeyAction action) {
		if(action == null) {
			return "";
		}
		return bindings == null ? TuiKeyBindings.label(action) : bindings.getActionLabel(action);
	}

	public static final class ActionClause {
		public final TuiKeyAction action;
		public final String description;
		public final String customKeyLabel;

		public ActionClause(TuiKeyAction action, String description) {
			this(action, description, null);
		}

		public ActionClause(TuiKeyAction action, String description, String customKeyLabel) {
			this.action = action;
			this.description = description;
			this.customKeyLabel = customKeyLabel;
		}
	}

	private static final class Entry {
		private final KeyStroke sortKey;
		private final int paneOrder;
		private final String line;

		private Entry(KeyStroke sortKey, int paneOrder, String line) {
			this.sortKey = sortKey;
			this.paneOrder = paneOrder;
			this.line = line;
		}

		private static Entry forKey(KeyStroke sortKey, String line) {
			return new Entry(sortKey, 0, line);
		}

		private static Entry forPane(int paneOrder, String line) {
			return new Entry(null, paneOrder, line);
		}
	}

	private static final Comparator<Entry> ENTRY_COMPARATOR = new Comparator<Entry>() {
		@Override
		public int compare(Entry left, Entry right) {
			if(left == right) {
				return 0;
			}
			if(left == null) {
				return 1;
			}
			if(right == null) {
				return -1;
			}
			boolean leftPane = left.paneOrder > 0;
			boolean rightPane = right.paneOrder > 0;
			if(leftPane && rightPane) {
				int paneCompare = left.paneOrder - right.paneOrder;
				if(paneCompare != 0) {
					return paneCompare;
				}
				return left.line.compareToIgnoreCase(right.line);
			}
			if(leftPane != rightPane) {
				return leftPane ? 1 : -1;
			}
			int keyCompare = TuiKeyBindingSort.compare(left.sortKey, right.sortKey);
			if(keyCompare != 0) {
				return keyCompare;
			}
			return left.line.compareToIgnoreCase(right.line);
		}
	};

}
