package dal.tool.trace.jmxer.tui.support;

import java.util.ArrayList;
import java.util.List;

import com.googlecode.lanterna.gui2.Panel;

/**
 * 포커스된 패널을 추적하고 Output/Command 패널에만 채움 하이라이트를 적용한다.
 *
 */
public final class BoxFocusManager {
	private final List<Panel> panes = new ArrayList<Panel>();
	private final List<Panel> fillHighlightPanes = new ArrayList<Panel>();
	private volatile Runnable activePaneListener;
	private Panel lastActive;

	public void register(Panel p) {
		if(p != null) {
			panes.add(p);
		}
	}

	public void registerFillHighlight(Panel p) {
		if(p != null) {
			fillHighlightPanes.add(p);
		}
	}

	public void unregister(Panel p) {
		if(p != null) {
			panes.remove(p);
		}
	}

	public void unregisterFillHighlight(Panel p) {
		if(p != null) {
			fillHighlightPanes.remove(p);
		}
	}

	public void setActivePaneListener(Runnable listener) {
		this.activePaneListener = listener;
	}

	public Panel getActivePane() {
		return lastActive;
	}

	public void setActive(Panel active) {
		for(Panel p : panes) {
			boolean fill = (p == active) && fillHighlightPanes.contains(p);
			p.setFillColorOverride(fill ? JmxTuiTheme.FOCUS_PANE_BG : null);
			p.invalidate();
		}
		if(active == lastActive) {
			return;
		}
		lastActive = active;
		Runnable listener = activePaneListener;
		if(listener != null) {
			listener.run();
		}
	}
}
