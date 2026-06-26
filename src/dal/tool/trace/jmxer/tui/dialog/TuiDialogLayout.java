package dal.tool.trace.jmxer.tui.dialog;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;

/**
 * TUI 다이얼로그 공통 레이아웃 헬퍼.
 *
 */
final class TuiDialogLayout {

	private static final int FIELD_HINT_INDENT_COLS = 2;
	private static final int WIDGET_LEFT_MARGIN = 4;

	private TuiDialogLayout() {}

	/**
	 * 설명 라벨과 입력 영역 앞 빈 행을 추가한다.<br>
	 * 라벨 텍스트 끝의 \n은 GridLayout에서 별도 행을 차지하지 않는다.
	 * @param panel 대상 패널
	 * @param description 설명 텍스트
	 */
	static void addFormDescription(Panel panel, String description) {
		panel.addComponent(new Label(TuiDialogText.body(description)));
		panel.addComponent(new EmptySpace(TerminalSize.ONE));
	}

	static void addFormFieldTitle(Panel panel, String title) {
		panel.addComponent(new Label(title));
	}

	static void addFormFieldHint(Panel panel, String hint) {
		panel.addComponent(new Label(indentFieldText(hint)));
	}

	static void addFormFieldSectionHeader(Panel panel, String title, String hint) {
		addFormFieldTitle(panel, title);
		addFormFieldHint(panel, hint);
	}

	static void addFormControlCaption(Panel panel, String caption) {
		panel.addComponent(new Label(indentFieldText(caption)));
	}

	static Panel createWidgetIndentPanel() {
		Panel widgetPanel = new Panel();
		widgetPanel.setLayoutManager(new GridLayout(1)
			.setLeftMarginSize(WIDGET_LEFT_MARGIN)
			.setRightMarginSize(0));
		return widgetPanel;
	}

	static void addCaptionWithControl(Panel panel, String caption, Component control) {
		addFormControlCaption(panel, caption);
		Panel widgetPanel = createWidgetIndentPanel();
		widgetPanel.addComponent(control);
		panel.addComponent(widgetPanel);
	}

	static void addFormFieldGap(Panel panel) {
		panel.addComponent(new EmptySpace(TerminalSize.ONE));
	}

	static void addFormFieldControl(Panel panel, Component control) {
		Panel widgetPanel = new Panel();
		widgetPanel.setLayoutManager(new GridLayout(1)
			.setLeftMarginSize(FIELD_HINT_INDENT_COLS)
			.setRightMarginSize(0));
		widgetPanel.addComponent(control);
		panel.addComponent(widgetPanel);
	}

	private static String indentFieldText(String text) {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < FIELD_HINT_INDENT_COLS; i++) {
			sb.append(' ');
		}
		sb.append(text == null ? "" : text);
		return sb.toString();
	}
}
