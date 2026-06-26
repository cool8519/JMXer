package dal.tool.trace.jmxer.tui.widget;

import com.googlecode.lanterna.input.KeyStroke;

/**
 * MbeanSearchInputBox용 선택적 키 핸들러.
 *
 */
public interface MbeanSearchKeyHandler {
	boolean handle(KeyStroke key);
}
