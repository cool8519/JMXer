package dal.tool.trace.jmxer.tui.widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.TerminalTextUtils;
import com.googlecode.lanterna.gui2.AbstractListBox;
import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.gui2.TextGUIGraphics;
import com.googlecode.lanterna.graphics.ThemeDefinition;

import dal.tool.trace.jmxer.tui.mode.JmxTuiModeContext.ThreadRowState;
import dal.tool.trace.jmxer.tui.support.JmxTuiTheme;
import dal.tool.trace.jmxer.tui.support.ThreadPaneTableFormat;

/**
 * Thread list row renderer with cursor arrow, multi-select background, and search-term highlighting.
 */
public final class ThreadSelectListItemRenderer extends AbstractListBox.ListItemRenderer<Runnable, ActionListBox> {
	private volatile String highlightQuery = "";
	private volatile List<Long> displayedThreadIds = new ArrayList<Long>();
	private volatile Set<Long> selectedThreadIds;
	private volatile Map<Long, ThreadRowState> displayedRowStates = new HashMap<Long, ThreadRowState>();

	public void setHighlightQuery(String query) {
		highlightQuery = (query == null) ? "" : query;
	}

	public void setDisplayedThreadIds(List<Long> ids) {
		displayedThreadIds = (ids == null) ? new ArrayList<Long>() : ids;
	}

	public void setSelectedThreadIds(Set<Long> selected) {
		selectedThreadIds = selected;
	}

	public void setDisplayedRowStates(Map<Long, ThreadRowState> rowStates) {
		if(rowStates == null) {
			displayedRowStates = new HashMap<Long, ThreadRowState>();
		} else {
			displayedRowStates = new HashMap<Long, ThreadRowState>(rowStates);
		}
	}

	@Override
	public void drawItem(TextGUIGraphics graphics, ActionListBox listBox, int index, Runnable item, boolean selected, boolean focused) {
		ThemeDefinition themeDefinition = listBox.getThemeDefinition();
		String label = getLabel(listBox, index, item);
		if(label == null) {
			label = "";
		}
		ThreadRowState rowState = resolveRowState(index);
		String prefix = buildRowPrefix(selected, rowState);
		int maxCols = graphics.getSize().getColumns();
		int prefixCols = ThreadPaneTableFormat.HEADER_ROW_PREFIX_COLS;
		String fittedPrefix = prefixCols <= maxCols ? prefix : TerminalTextUtils.fitString(prefix, maxCols);
		String fittedLabel = TerminalTextUtils.fitString(label, Math.max(0, maxCols - prefixCols));

		boolean multiSelected = isMultiSelected(index);
		applyRowStyle(graphics, themeDefinition, selected, focused, multiSelected, rowState);
		graphics.putString(0, 0, fittedPrefix);

		String query = highlightQuery.trim();
		if(query.isEmpty() || isPlaceholderLabel(label)) {
			applyRowStyle(graphics, themeDefinition, selected, focused, multiSelected, rowState);
			graphics.putString(prefixCols, 0, fittedLabel);
			padRow(graphics, themeDefinition, selected, focused, multiSelected, rowState,
				prefixCols + TerminalTextUtils.getColumnWidth(fittedLabel), maxCols);
			return;
		}

		List<int[]> ranges = findMatchRanges(fittedLabel, query);
		if(ranges.isEmpty()) {
			applyRowStyle(graphics, themeDefinition, selected, focused, multiSelected, rowState);
			graphics.putString(prefixCols, 0, fittedLabel);
			padRow(graphics, themeDefinition, selected, focused, multiSelected, rowState,
				prefixCols + TerminalTextUtils.getColumnWidth(fittedLabel), maxCols);
			return;
		}

		int col = prefixCols;
		int pos = 0;
		for(int r = 0; r < ranges.size(); r++) {
			int start = ranges.get(r)[0];
			int end = ranges.get(r)[1];
			if(start > pos) {
				applyRowStyle(graphics, themeDefinition, selected, focused, multiSelected, rowState);
				String seg = fittedLabel.substring(pos, start);
				graphics.putString(col, 0, seg);
				col += TerminalTextUtils.getColumnWidth(seg);
			}
			graphics.setBackgroundColor(JmxTuiTheme.SEARCH_MATCH_BG);
			graphics.setForegroundColor(JmxTuiTheme.SEARCH_MATCH_FG);
			String match = fittedLabel.substring(start, end);
			graphics.putString(col, 0, match);
			col += TerminalTextUtils.getColumnWidth(match);
			pos = end;
		}
		if(pos < fittedLabel.length()) {
			applyRowStyle(graphics, themeDefinition, selected, focused, multiSelected, rowState);
			String tail = fittedLabel.substring(pos);
			graphics.putString(col, 0, tail);
			col += TerminalTextUtils.getColumnWidth(tail);
		}
		padRow(graphics, themeDefinition, selected, focused, multiSelected, rowState, col, maxCols);
	}

	private static String buildRowPrefix(boolean cursorRow, ThreadRowState rowState) {
		boolean created = rowState == ThreadRowState.CREATED;
		if(cursorRow) {
			return created
					? ThreadPaneTableFormat.THREAD_ROW_PREFIX_CREATED_CURSOR
					: ThreadPaneTableFormat.THREAD_ROW_PREFIX_CURSOR;
		}
		return created
				? ThreadPaneTableFormat.THREAD_ROW_PREFIX_CREATED
				: ThreadPaneTableFormat.THREAD_ROW_PREFIX_NORMAL;
	}

	private ThreadRowState resolveRowState(int index) {
		List<Long> displayed = displayedThreadIds;
		Map<Long, ThreadRowState> rowStates = displayedRowStates;
		if(displayed == null || rowStates == null || index < 0 || index >= displayed.size()) {
			return ThreadRowState.NORMAL;
		}
		Long id = displayed.get(index);
		if(id == null) {
			return ThreadRowState.NORMAL;
		}
		ThreadRowState state = rowStates.get(id);
		return state == null ? ThreadRowState.NORMAL : state;
	}

	private static void applyRowForeground(
			TextGUIGraphics graphics,
			ThemeDefinition themeDefinition,
			ThreadRowState rowState,
			boolean multiSelected,
			boolean cursorRow) {
		boolean created = rowState == ThreadRowState.CREATED;
		boolean destroyed = rowState == ThreadRowState.DESTROYED;
		TextColor fg = JmxTuiTheme.resolveThreadRowForeground(created, destroyed, multiSelected, cursorRow);
		if(fg != null) {
			graphics.setForegroundColor(fg);
			return;
		}
		graphics.applyThemeStyle(themeDefinition.getNormal());
	}

	private boolean isMultiSelected(int index) {
		Set<Long> selected = selectedThreadIds;
		List<Long> displayed = displayedThreadIds;
		if(selected == null || displayed == null || index < 0 || index >= displayed.size()) {
			return false;
		}
		Long id = displayed.get(index);
		return id != null && selected.contains(id);
	}

	private static void padRow(
			TextGUIGraphics graphics,
			ThemeDefinition themeDefinition,
			boolean selected,
			boolean focused,
			boolean multiSelected,
			ThreadRowState rowState,
			int usedCols,
			int maxCols) {
		applyRowStyle(graphics, themeDefinition, selected, focused, multiSelected, rowState);
		while(usedCols < maxCols) {
			graphics.putString(usedCols, 0, " ");
			usedCols++;
		}
	}

	private static void applyRowStyle(
			TextGUIGraphics graphics,
			ThemeDefinition themeDefinition,
			boolean selected,
			boolean focused,
			boolean multiSelected,
			ThreadRowState rowState) {
		boolean cursorRow = selected && focused;
		if(multiSelected) {
			graphics.setBackgroundColor(JmxTuiTheme.THREAD_SELECTED_BG);
		} else if(cursorRow) {
			graphics.setBackgroundColor(JmxTuiTheme.SELECTED_ROW_BG);
		} else {
			graphics.applyThemeStyle(themeDefinition.getNormal());
		}
		applyRowForeground(graphics, themeDefinition, rowState, multiSelected, cursorRow);
	}

	private static boolean isPlaceholderLabel(String label) {
		return label != null && label.startsWith("(");
	}

	private static List<int[]> findMatchRanges(String text, String query) {
		List<int[]> ranges = new ArrayList<int[]>();
		if(text == null || text.isEmpty() || query == null || query.isEmpty()) {
			return ranges;
		}
		String lowerText = text.toLowerCase();
		String lowerQuery = query.toLowerCase();
		int from = 0;
		while(from <= lowerText.length() - lowerQuery.length()) {
			int i = lowerText.indexOf(lowerQuery, from);
			if(i < 0) {
				break;
			}
			ranges.add(new int[] { i, i + query.length() });
			from = i + lowerQuery.length();
		}
		return ranges;
	}
}
