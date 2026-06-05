package dal.tool.trace.jmxer.tui.widget;

import com.googlecode.lanterna.input.KeyStroke;

/**
 * Optional key handler for {@link MbeanSearchInputBox}.
 */
public interface MbeanSearchKeyHandler {
	boolean handle(KeyStroke key);
}
