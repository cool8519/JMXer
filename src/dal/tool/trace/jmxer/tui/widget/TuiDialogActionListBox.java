package dal.tool.trace.jmxer.tui.widget;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.input.KeyStroke;

import dal.tool.trace.jmxer.tui.support.JmxTuiKeys;

/**
 * 팝업 다이얼로그용 목록. Enter로만 항목을 활성화하고 Space는 무시한다.
 *
 */
public class TuiDialogActionListBox extends ActionListBox {

	public TuiDialogActionListBox() {
		super();
	}

	public TuiDialogActionListBox(TerminalSize preferredSize) {
		super(preferredSize);
	}

	@Override
	public synchronized Interactable.Result handleKeyStroke(KeyStroke keyStroke) {
		if(JmxTuiKeys.isPlainSpaceKey(keyStroke)) {
			return Interactable.Result.HANDLED;
		}
		return super.handleKeyStroke(keyStroke);
	}
}
