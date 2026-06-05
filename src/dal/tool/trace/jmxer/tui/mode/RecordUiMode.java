package dal.tool.trace.jmxer.tui.mode;

import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;

import dal.tool.trace.jmxer.cli.JmxSettings;
import dal.tool.trace.jmxer.tui.app.JmxTuiApp;
import dal.tool.trace.jmxer.tui.support.TuiPaneKeysHint;
import dal.tool.trace.jmxer.tui.support.TuiStatusSettingLine;
import dal.tool.trace.jmxer.tui.support.TuiStatusText;

public final class RecordUiMode extends JmxTuiUiMode {
	public static final String ID = "RECORD";
	/** Left (Search+Record list) vs right (Output) default split: 1:1. */
	private static final int DEFAULT_HORIZONTAL_LEFT_SHARE_NUMERATOR = 1;
	private static final int DEFAULT_HORIZONTAL_LEFT_SHARE_DENOMINATOR = 2;
	private static final RecordUiMode INSTANCE = new RecordUiMode();

	public static RecordUiMode getInstance() {
		return INSTANCE;
	}

	private RecordUiMode() {}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDisplayName() {
		return "Record";
	}

	@Override
	public boolean isOutputInRightColumn() {
		return true;
	}

	@Override
	public boolean isFeaturesPaneAvailable() {
		return false;
	}

	@Override
	public boolean canResizeVerticalSplit() {
		return false;
	}

	@Override
	public int getDefaultHorizontalLeftShareNumerator() {
		return DEFAULT_HORIZONTAL_LEFT_SHARE_NUMERATOR;
	}

	@Override
	public int getDefaultHorizontalLeftShareDenominator() {
		return DEFAULT_HORIZONTAL_LEFT_SHARE_DENOMINATOR;
	}

	@Override
	public void buildBody(JmxTuiApp app, Panel root) {
		root.addComponent(app.getListsPanel());
	}

	@Override
	public void renderStatusDetailLine(Label header3, JmxSettings s, JmxTuiModeContext context, int statusInnerCols) {
		header3.setText(TuiStatusText.fitSingleLine(TuiStatusSettingLine.recordDetailLine(s), statusInnerCols));
	}

	@Override
	public String getPaneKeysHint(JmxTuiModeFocusPane focusedPane, boolean wrapOn, JmxSettings settings) {
		if(focusedPane == null) {
			focusedPane = JmxTuiModeFocusPane.SEARCH;
		}
		if(focusedPane == JmxTuiModeFocusPane.MBEANS) {
			return TuiPaneKeysHint.mbeans();
		}
		if(focusedPane == JmxTuiModeFocusPane.OUTPUT) {
			return TuiPaneKeysHint.output(wrapOn);
		}
		if(focusedPane == JmxTuiModeFocusPane.COMMAND) {
			return TuiPaneKeysHint.command();
		}
		return TuiPaneKeysHint.search();
	}
}
