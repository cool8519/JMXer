package dal.tool.trace.jmxer.tui.widget;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

import dal.tool.trace.jmxer.tui.support.JmxTuiKeys;
import dal.tool.trace.jmxer.tui.support.RecordViewExpressionHelper;
import dal.tool.trace.jmxer.tui.support.TuiRecordUiKeys;

/**
 * 표현식 유형을 고르는 대화상자 목록. Enter 시 대상 입력란에 dump 기반 예시를 채운다.
 *
 */
public final class TuiRecordExpressionTypeList extends ActionListBox {

	private static final Runnable NO_OP = new Runnable() {
		@Override
		public void run() {}
	};

	private final TuiDialogInputBox targetInput;
	private final RecordViewExpressionHelper helper;
	private final boolean rangeMode;

	public TuiRecordExpressionTypeList(
			TerminalSize size,
			TuiDialogInputBox targetInput,
			RecordViewExpressionHelper helper,
			boolean rangeMode) {
		super(size);
		this.targetInput = targetInput;
		this.helper = helper;
		this.rangeMode = rangeMode;
		setListItemRenderer(new DialogArrowListItemRenderer());
		addItem(TuiRecordUiKeys.VIEW_EXPRESSION_TYPE_ABSOLUTE_LABEL, NO_OP);
		addItem(TuiRecordUiKeys.VIEW_EXPRESSION_TYPE_RELATIVE_LABEL, NO_OP);
		addItem(TuiRecordUiKeys.VIEW_EXPRESSION_TYPE_SAMPLE_LABEL, NO_OP);
	}

	@Override
	public synchronized Interactable.Result handleKeyStroke(KeyStroke keyStroke) {
		if(JmxTuiKeys.isPlainSpaceKey(keyStroke)) {
			return Interactable.Result.HANDLED;
		}
		if(keyStroke != null && keyStroke.getKeyType() == KeyType.Enter) {
			applySelectedExample();
			return Interactable.Result.HANDLED;
		}
		return super.handleKeyStroke(keyStroke);
	}

	private void applySelectedExample() {
		RecordViewExpressionHelper.ExpressionType type =
			RecordViewExpressionHelper.ExpressionType.fromListIndex(getSelectedIndex());
		String example = rangeMode
			? helper.formatRangeExample(type)
			: helper.formatPointExample(type);
		targetInput.setText(example);
		targetInput.takeFocus();
	}
}
