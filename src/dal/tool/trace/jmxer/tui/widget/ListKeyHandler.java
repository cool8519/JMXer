package dal.tool.trace.jmxer.tui.widget;

import com.googlecode.lanterna.input.KeyStroke;

/**
 * FocusActionListBox용 선택적 키 핸들러.
 * handle이 true를 반환하면 키를 소비하고 기본 목록 동작을 건너뛴다.
 *
 */
public interface ListKeyHandler {
	boolean handle(KeyStroke key);
}
