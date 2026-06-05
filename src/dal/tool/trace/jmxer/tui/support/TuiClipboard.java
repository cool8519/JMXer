package dal.tool.trace.jmxer.tui.support;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;

/**
 * Reads the OS clipboard for TUI paste (desktop JVM only; returns null when headless or unavailable).
 */
public final class TuiClipboard {

	private TuiClipboard() {}

	public static String readString() {
		if(GraphicsEnvironment.isHeadless()) {
			return null;
		}
		try {
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			if(clipboard == null || !clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
				return null;
			}
			Object data = clipboard.getData(DataFlavor.stringFlavor);
			if(data instanceof String) {
				return (String) data;
			}
		} catch(Exception e) {
			// HeadlessException, UnsupportedFlavorException, SecurityException, etc.
		}
		return null;
	}
}
