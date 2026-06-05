package dal.tool.trace.jmxer.tui.widget;

import com.googlecode.lanterna.input.KeyStroke;

/**
 * Optional key handler for {@link CommandInputBox} (Command pane).
 */
public interface CommandInputKeyHandler {
	boolean handle(KeyStroke key);
}
