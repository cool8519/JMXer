package dal.tool.trace.jmxer.tui;

import com.googlecode.lanterna.input.KeyStroke;

/**
 * Optional key handler for {@link MbeanSearchInputBox}.
 */
interface MbeanSearchKeyHandler {
	boolean handle(KeyStroke key);
}
