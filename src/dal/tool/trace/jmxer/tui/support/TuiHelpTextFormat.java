package dal.tool.trace.jmxer.tui.support;

/**
 * F1 도움말 본문 들여쓰기. 카테고리([Global] 등) 아래 항목은 2칸, 줄바꿈 이어쓰기는 4칸.
 *
 */
public final class TuiHelpTextFormat {

	public static final int ITEM_INDENT_COLS = 2;
	public static final int CONTINUATION_EXTRA_INDENT_COLS = 2;
	public static final int ITEM_CONTINUATION_INDENT_COLS = ITEM_INDENT_COLS + CONTINUATION_EXTRA_INDENT_COLS;
	public static final String ITEM_CONTINUATION_INDENT = "    ";

	public static String itemLine(String text) {
		return TuiKeyRemapUiKeys.indentKeyListLabel(text, false);
	}

	private TuiHelpTextFormat() {}
}
