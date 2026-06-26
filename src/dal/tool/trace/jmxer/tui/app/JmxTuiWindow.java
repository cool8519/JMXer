package dal.tool.trace.jmxer.tui.app;

import java.util.concurrent.atomic.AtomicBoolean;

import com.googlecode.lanterna.gui2.BasePaneListener;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.input.KeyStroke;

/**
 * TUI 메인 윈도우. BasePaneListener로 전역 단축키를 포커스 패널보다 먼저 처리한다.
 *
 */
public class JmxTuiWindow extends BasicWindow {
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
			}
		});
	}

	public void setGlobalShortcutHandler(GlobalShortcutHandler handler) {
		this.globalShortcutHandler = handler;
	}
}
