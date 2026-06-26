package dal.tool.trace.jmxer.tui.widget;

import java.util.ArrayList;
import java.util.List;

import com.googlecode.lanterna.TerminalTextUtils;
import com.googlecode.lanterna.gui2.AbstractListBox;
import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.gui2.TextGUIGraphics;
import com.googlecode.lanterna.graphics.ThemeDefinition;

import dal.tool.trace.jmxer.tui.support.JmxTuiTheme;
import dal.tool.trace.jmxer.tui.support.TuiListArrowPrefix;

/**
 * 선택 화살표와 검색어 강조를 적용하는 목록 행 렌더러.
 *
 */
public final class HighlightArrowListItemRenderer extends AbstractListBox.ListItemRenderer<Runnable, ActionListBox> {
	private volatile String highlightQuery = "";

	public void setHighlightQuery(String query) {
		highlightQuery = (query == null) ? "" : query;
	}

	@Override
	public void drawItem(TextGUIGraphics graphics, ActionListBox listBox, int index, Runnable item, boolean selected, boolean focused) {
		ThemeDefinition themeDefinition = listBox.getThemeDefinition();
		String label = getLabel(listBox, index, item);
		if(label == null) {
			label = "";
		}
		String prefix = selected ? TuiListArrowPrefix.LIST_SELECTED : TuiListArrowPrefix.LIST_UNSELECTED;
		int maxCols = graphics.getSize().getColumns();
		String fittedPrefix = TerminalTextUtils.fitString(prefix, maxCols);
		int prefixCols = TerminalTextUtils.getColumnWidth(fittedPrefix);
		String fittedLabel = TerminalTextUtils.fitString(label, Math.max(0, maxCols - prefixCols));

		applyRowStyle(graphics, themeDefinition, selected, focused);
		graphics.putString(0, 0, fittedPrefix);

		String query = highlightQuery.trim();
		if(query.isEmpty() || isPlaceholderLabel(label)) {
			graphics.putString(prefixCols, 0, fittedLabel);
			padRow(graphics, themeDefinition, selected, focused,
				prefixCols + TerminalTextUtils.getColumnWidth(fittedLabel), maxCols);
			return;
		}

		List<int[]> ranges = findMatchRanges(fittedLabel, query);
		if(ranges.isEmpty()) {
			graphics.putString(prefixCols, 0, fittedLabel);
			padRow(graphics, themeDefinition, selected, focused,
				prefixCols + TerminalTextUtils.getColumnWidth(fittedLabel), maxCols);
			return;
		}

		int col = prefixCols;
		int pos = 0;
		for(int r = 0; r < ranges.size(); r++) {
			int start = ranges.get(r)[0];
			int end = ranges.get(r)[1];
			if(start > pos) {
				applyRowStyle(graphics, themeDefinition, selected, focused);
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
			applyRowStyle(graphics, themeDefinition, selected, focused);
			String tail = fittedLabel.substring(pos);
			graphics.putString(col, 0, tail);
			col += TerminalTextUtils.getColumnWidth(tail);
		}
		padRow(graphics, themeDefinition, selected, focused, col, maxCols);
	}

	private static void padRow(
			TextGUIGraphics graphics,
			ThemeDefinition themeDefinition,
			boolean selected,
			boolean focused,
			int usedCols,
			int maxCols) {
		applyRowStyle(graphics, themeDefinition, selected, focused);
		while(usedCols < maxCols) {
			graphics.putString(usedCols, 0, " ");
			usedCols++;
		}
	}

	private static void applyRowStyle(TextGUIGraphics graphics, ThemeDefinition themeDefinition, boolean selected, boolean focused) {
		if(selected && focused) {
			graphics.setBackgroundColor(JmxTuiTheme.SELECTED_ROW_BG);
			graphics.setForegroundColor(JmxTuiTheme.SELECTED_ROW_FG);
		} else {
			graphics.applyThemeStyle(themeDefinition.getNormal());
		}
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
