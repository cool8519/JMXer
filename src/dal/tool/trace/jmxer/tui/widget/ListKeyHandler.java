package dal.tool.trace.jmxer.tui.widget;

import com.googlecode.lanterna.input.KeyStroke;

/**
 * Optional key handler for {@link FocusActionListBox}. When {@link #handle} returns true,
 * the key is consumed and default list behavior is skipped.
 */
public interface ListKeyHandler {
	boolean handle(KeyStroke key);
}
