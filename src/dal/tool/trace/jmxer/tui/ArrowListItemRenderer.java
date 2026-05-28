package dal.tool.trace.jmxer.tui;

import com.googlecode.lanterna.TerminalTextUtils;
import com.googlecode.lanterna.gui2.AbstractListBox;
import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.gui2.TextGUIGraphics;
import com.googlecode.lanterna.graphics.ThemeDefinition;

/**
 * List row renderer with selection arrow and focus-aware highlight colors.
 */
final class ArrowListItemRenderer extends AbstractListBox.ListItemRenderer<Runnable, ActionListBox> {
	@Override
	public void drawItem(TextGUIGraphics graphics, ActionListBox listBox, int index, Runnable item, boolean selected, boolean focused) {
		ThemeDefinition themeDefinition = listBox.getThemeDefinition();
		String label = getLabel(listBox, index, item);
		String prefix = selected ? "\u25b6 " : "  ";
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
