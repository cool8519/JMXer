package dal.tool.trace.jmxer.tui;

import java.util.concurrent.atomic.AtomicBoolean;

import com.googlecode.lanterna.gui2.BasePaneListener;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.input.KeyStroke;

/**
 * Main TUI window: installs a {@link BasePaneListener} so global shortcuts are handled
 * before the focused pane (see {@link JmxTuiApp#consumeGlobalShortcut(KeyStroke)}).
 */
public class JmxTuiWindow extends BasicWindow {

	/** Return true if the key was fully handled (focused pane must not see it). */
	public interface GlobalShortcutHandler {
		boolean consumeGlobalShortcut(KeyStroke keyStroke);
	}

	private volatile GlobalShortcutHandler globalShortcutHandler;

	public JmxTuiWindow() {
		super("");
		addBasePaneListener(new BasePaneListener<Window>() {
			@Override
			public void onInput(Window pane, KeyStroke keyStroke, AtomicBoolean deliverEvent) {
				GlobalShortcutHandler h = globalShortcutHandler;
				if(h != null && keyStroke != null && h.consumeGlobalShortcut(keyStroke)) {
					deliverEvent.set(false);
				}
			}

			@Override
			public void onUnhandledInput(Window pane, KeyStroke keyStroke, AtomicBoolean consumeEvent) {
				// no-op
			}
		});
	}

	public void setGlobalShortcutHandler(GlobalShortcutHandler handler) {
		this.globalShortcutHandler = handler;
	}
}
