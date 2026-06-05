package dal.tool.trace.jmxer.tui.mode;

import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;

import dal.tool.trace.jmxer.cli.JmxSettings;
import dal.tool.trace.jmxer.tui.app.JmxTuiApp;
import dal.tool.trace.jmxer.tui.mode.JmxTuiModeContext;
import dal.tool.trace.jmxer.tui.support.TuiStatusSegmentLabel;
import dal.tool.trace.jmxer.tui.support.TuiStatusText;

public abstract class JmxTuiUiMode {
	private static final int DEFAULT_HORIZONTAL_LEFT_SHARE_NUMERATOR = 3;
	private static final int DEFAULT_HORIZONTAL_LEFT_SHARE_DENOMINATOR = 5;
	private static final int DEFAULT_VERTICAL_LISTS_BAND_SHARE_NUMERATOR = 1;
	private static final int DEFAULT_VERTICAL_LISTS_BAND_SHARE_DENOMINATOR = 4;

	private JmxTuiModeContext context;

	public abstract String getId();
	public abstract String getDisplayName();

	/**
	 * Returns the lazily initialized pane context for this mode. The same mode instance
	 * keeps its context for the lifetime of the TUI session.
	 */
	public final JmxTuiModeContext getOrCreateContext(JmxTuiApp app) {
		if(context == null) {
			context = app.createModeContext(this);
		}
		return context;
	}

	/**
	 * @return true if Output is placed in the right column (Thread/Record),
	 *         false if Output is placed as a full-width pane below lists band (JMX).
	 */
	public abstract boolean isOutputInRightColumn();

	/**
	 * @return true if Features pane exists in this mode (JMX).
	 */
	public abstract boolean isFeaturesPaneAvailable();

	/**
	 * @return true if vertical resize (lists band vs bottom Output) is available.
	 */
	public abstract boolean canResizeVerticalSplit();

	/**
	 * Default horizontal split for the left column (Search/lists band) before min-width clamping.
	 * Left width is {@code availableCols * numerator / denominator}.
	 */
	public int getDefaultHorizontalLeftShareNumerator() {
		return DEFAULT_HORIZONTAL_LEFT_SHARE_NUMERATOR;
	}

	public int getDefaultHorizontalLeftShareDenominator() {
		return DEFAULT_HORIZONTAL_LEFT_SHARE_DENOMINATOR;
	}

	/**
	 * Default vertical split baseline for the lists band (MBean mode: lists band vs bottom Output).
	 * Inner list row count uses {@code availableForListAndOutput * numerator / denominator} before clamping.
	 */
	public int getDefaultVerticalListsBandShareNumerator() {
		return DEFAULT_VERTICAL_LISTS_BAND_SHARE_NUMERATOR;
	}

	public int getDefaultVerticalListsBandShareDenominator() {
		return DEFAULT_VERTICAL_LISTS_BAND_SHARE_DENOMINATOR;
	}

	/**
	 * Rebuild the mode-specific body layout under the root container.
	 * Implementations should attach panes from {@link #getOrCreateContext(JmxTuiApp)}.
	 */
	public abstract void buildBody(JmxTuiApp app, Panel root);

	/**
	 * Render mode-specific status object line (2nd status line).
	 */
	public void renderStatusObjectLine(TuiStatusSegmentLabel header2, JmxSettings s, JmxTuiModeContext context, int statusInnerCols) {
		String mbeanName = s.getMBeanObjectName() == null ? "" : s.getMBeanObjectName();
		header2.setMaxColumns(statusInnerCols);
		header2.setText(TuiStatusText.fitSingleLine("   - Current MBean:" + mbeanName, statusInnerCols));
	}

	/**
	 * Render mode-specific status detail line (3rd status line).
	 */
	public abstract void renderStatusDetailLine(Label header3, JmxSettings s, JmxTuiModeContext context, int statusInnerCols);

	/**
	 * Render window keys hint line (shortcuts for the currently focused pane only).
	 */
	public abstract String getPaneKeysHint(JmxTuiModeFocusPane focusedPane, boolean wrapOn, JmxSettings settings);
}

