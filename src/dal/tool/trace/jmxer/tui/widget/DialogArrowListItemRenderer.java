package dal.tool.trace.jmxer.tui.widget;

import dal.tool.trace.jmxer.tui.support.JmxTuiTheme;
import dal.tool.trace.jmxer.tui.support.TuiListArrowPrefix;

import com.googlecode.lanterna.TerminalTextUtils;
import com.googlecode.lanterna.gui2.AbstractListBox;
import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.gui2.TextGUIGraphics;
import com.googlecode.lanterna.graphics.ThemeDefinition;

/**
 * 대화상자 목록 행 렌더러. 선택 화살표를 항상 표시하고 포커스된 행만 강조한다.
 *
 */
public final class DialogArrowListItemRenderer extends AbstractListBox.ListItemRenderer<Runnable, ActionListBox> {
	@Override
	public void drawItem(TextGUIGraphics graphics, ActionListBox listBox, int index, Runnable item, boolean selected, boolean focused) {
		ThemeDefinition themeDefinition = listBox.getThemeDefinition();
		String label = getLabel(listBox, index, item);
		String prefix = selected ? TuiListArrowPrefix.LIST_SELECTED : TuiListArrowPrefix.LIST_UNSELECTED;
		String combined = prefix + label;
		combined = TerminalTextUtils.fitString(combined, graphics.getSize().getColumns());
		while(TerminalTextUtils.getColumnWidth(combined) < graphics.getSize().getColumns()) {
			combined = combined + " ";
		}
		if(selected && focused) {
			graphics.setBackgroundColor(JmxTuiTheme.SELECTED_ROW_BG);
			graphics.setForegroundColor(JmxTuiTheme.SELECTED_ROW_FG);
		} else {
			graphics.applyThemeStyle(themeDefinition.getNormal());
		}
		graphics.putString(0, 0, combined);
	}
}
