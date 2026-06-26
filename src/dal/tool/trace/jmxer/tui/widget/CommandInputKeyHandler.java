package dal.tool.trace.jmxer.tui.widget;

import com.googlecode.lanterna.input.KeyStroke;

/**
 * CommandInputBox(Command 패널)용 선택적 키 핸들러.
 *
 */
public interface CommandInputKeyHandler {
	boolean handle(KeyStroke key);
}
