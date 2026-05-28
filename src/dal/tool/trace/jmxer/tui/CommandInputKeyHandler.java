package dal.tool.trace.jmxer.tui;

import com.googlecode.lanterna.input.KeyStroke;

/**
 * Optional key handler for {@link CommandInputBox} (Command pane).
 */
interface CommandInputKeyHandler {
	boolean handle(KeyStroke key);
}
