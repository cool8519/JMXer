package dal.tool.trace.jmxer.tui.widget;

import java.util.List;

import com.googlecode.lanterna.TerminalTextUtils;
import com.googlecode.lanterna.gui2.AbstractListBox;
import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.gui2.TextGUIGraphics;
import com.googlecode.lanterna.graphics.ThemeDefinition;

import dal.tool.trace.jmxer.tui.support.JmxTuiTheme;
import dal.tool.trace.jmxer.tui.support.TuiKeyAction;

/**
 * 키 리매핑 다이얼로그 목록 행 렌더러. 포커스된 행은 파란색, 키 캡처 대기 중인 행은 별도 색으로 강조한다.
 *
 */
public final class KeyRemapListItemRenderer extends AbstractListBox.ListItemRenderer<Runnable, ActionListBox> {

	private TuiDialogActionListBox globalList;
	private List<TuiKeyAction> globalActions;
	private TuiDialogActionListBox windowList;
	private List<TuiKeyAction> windowActions;
	private TuiKeyAction captureTarget;

	public void bindLists(
			TuiDialogActionListBox globalList,
			List<TuiKeyAction> globalActions,
			TuiDialogActionListBox windowList,
			List<TuiKeyAction> windowActions) {
		this.globalList = globalList;
		this.globalActions = globalActions;
		this.windowList = windowList;
		this.windowActions = windowActions;
	}

	public void setCaptureTarget(TuiKeyAction captureTarget) {
		this.captureTarget = captureTarget;
	}

	@Override
	public void drawItem(TextGUIGraphics graphics, ActionListBox listBox, int index, Runnable item,
			boolean selected, boolean focused) {
		ThemeDefinition themeDefinition = listBox.getThemeDefinition();
		String label = getLabel(listBox, index, item);
		String line = TerminalTextUtils.fitString(label, graphics.getSize().getColumns());
		while(TerminalTextUtils.getColumnWidth(line) < graphics.getSize().getColumns()) {
			line = line + " ";
		}
		if(isCaptureRow(listBox, index)) {
			graphics.setBackgroundColor(JmxTuiTheme.KEY_REMAP_CAPTURE_BG);
			graphics.setForegroundColor(JmxTuiTheme.KEY_REMAP_CAPTURE_FG);
		} else if(selected && focused) {
			graphics.setBackgroundColor(JmxTuiTheme.SELECTED_ROW_BG);
			graphics.setForegroundColor(JmxTuiTheme.SELECTED_ROW_FG);
		} else {
			graphics.applyThemeStyle(themeDefinition.getNormal());
		}
		graphics.putString(0, 0, line);
	}

	private boolean isCaptureRow(ActionListBox listBox, int index) {
		if(captureTarget == null || listBox == null || index < 0) {
			return false;
		}
		List<TuiKeyAction> actions = resolveActions(listBox);
		if(actions == null || index >= actions.size()) {
			return false;
		}
		return captureTarget == actions.get(index);
	}

	private List<TuiKeyAction> resolveActions(ActionListBox listBox) {
		if(listBox == globalList) {
			return globalActions;
		}
		if(listBox == windowList) {
			return windowActions;
		}
		return null;
	}
}
