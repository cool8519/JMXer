package dal.tool.trace.jmxer.tui.widget;

import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

import dal.tool.trace.jmxer.tui.support.JmxTuiKeys;

/**
 * 대화상자 선택 목록. 화살표로만 이동하며 Space는 항목을 활성화하지 않는다.
 * Enter 시 선택적 대상(예: OK 버튼)으로 포커스를 옮긴다.
 *
 */
public final class TuiDialogSelectionList extends ActionListBox {

	private volatile Interactable enterFocusTarget;

	public TuiDialogSelectionList() {
		setListItemRenderer(new DialogArrowListItemRenderer());
	}

	public void setEnterFocusTarget(Interactable target) {
		this.enterFocusTarget = target;
	}

	@Override
	public synchronized Interactable.Result handleKeyStroke(KeyStroke keyStroke) {
		if(JmxTuiKeys.isPlainSpaceKey(keyStroke)) {
			return Interactable.Result.HANDLED;
		}
		if(keyStroke != null && keyStroke.getKeyType() == KeyType.Enter) {
			Interactable target = enterFocusTarget;
			if(target != null) {
				target.takeFocus();
			}
			return Interactable.Result.HANDLED;
		}
		return super.handleKeyStroke(keyStroke);
	}
}
