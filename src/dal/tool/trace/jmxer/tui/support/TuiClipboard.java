package dal.tool.trace.jmxer.tui.support;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;

/**
 * TUI 붙여넣기용 OS 클립보드 읽기(데스크톱 JVM; headless·불가 시 null).
 *
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
		}
		return null;
	}
}
