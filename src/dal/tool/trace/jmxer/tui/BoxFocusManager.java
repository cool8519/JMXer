package dal.tool.trace.jmxer.tui;

import java.util.ArrayList;
import java.util.List;

import com.googlecode.lanterna.gui2.Panel;

/**
 * Tracks which pane is focused and applies fill highlight on Output/Command panes only.
 */
final class BoxFocusManager {
	private final List<Panel> panes = new ArrayList<Panel>();
	private final List<Panel> fillHighlightPanes = new ArrayList<Panel>();

	void register(Panel p) {
		if(p != null) {
			panes.add(p);
		}
	}

	void registerFillHighlight(Panel p) {
		if(p != null) {
			fillHighlightPanes.add(p);
		}
	}

	void setActive(Panel active) {
		for(Panel p : panes) {
			boolean fill = (p == active) && fillHighlightPanes.contains(p);
			p.setFillColorOverride(fill ? JmxTuiTheme.FOCUS_PANE_BG : null);
			p.invalidate();
		}
	}
}
