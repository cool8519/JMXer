package dal.tool.trace.jmxer.tui.mode;

import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;

import java.util.List;

import dal.tool.trace.jmxer.JMXControl;
import dal.tool.trace.jmxer.cli.JmxSettings;
import dal.tool.trace.jmxer.tui.app.JmxTuiApp;
import dal.tool.trace.jmxer.tui.support.TuiKeyAction;
import dal.tool.trace.jmxer.tui.support.TuiPaneKeysHint;
import dal.tool.trace.jmxer.tui.support.TuiStatusSettingLine;
import dal.tool.trace.jmxer.tui.support.TuiStatusText;

/**
 * MBean UI Mode. MBean 검색·목록·기능 패널 레이아웃과 상태 표시를 구성한다.
 *
 */
public final class MBeanUiMode extends JmxTuiUiMode {
	public static final String ID = "MBEAN";
	private static final MBeanUiMode INSTANCE = new MBeanUiMode();

	public static MBeanUiMode getInstance() {
		return INSTANCE;
	}

	private MBeanUiMode() {}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDisplayName() {
		return "MBean";
	}

	@Override
	public boolean isOutputInRightColumn() {
		return false;
	}

	@Override
	public boolean isFeaturesPaneAvailable() {
		return true;
	}

	@Override
	public boolean canResizeVerticalSplit() {
		return true;
	}

	@Override
	public void buildBody(JmxTuiApp app, Panel root) {
		root.addComponent(app.getListsPanel());
		if(app.isOutputVisible()) {
			root.addComponent(app.getOutputPaneBordered(), LinearLayout.createLayoutData(LinearLayout.Alignment.Fill, LinearLayout.GrowPolicy.CanGrow));
		}
	}

	@Override
	public void renderStatusDetailLine(Label header3, JmxSettings s, JmxTuiModeContext context, int statusInnerCols) {
		if(JMXControl.isOfflineSession || s == null) {
			header3.setText(TuiStatusText.fitSingleLine("", statusInnerCols));
			return;
		}
		header3.setText(TuiStatusText.fitSingleLine(
			TuiStatusSettingLine.mbeanOnlineDetailLine(s), statusInnerCols));
	}

	@Override
	public String getPaneKeysHint(
		JmxTuiModeFocusPane focusedPane,
		boolean wrapOn,
		JmxSettings settings,
		JmxTuiModeContext context
	) {
		if(focusedPane == null) {
			focusedPane = JmxTuiModeFocusPane.SEARCH;
		}
		if(focusedPane == JmxTuiModeFocusPane.MBEANS) {
			return TuiPaneKeysHint.mbeans();
		}
		if(focusedPane == JmxTuiModeFocusPane.FEATURES) {
			return TuiPaneKeysHint.features();
		}
		if(focusedPane == JmxTuiModeFocusPane.OUTPUT) {
			return TuiPaneKeysHint.output(wrapOn);
		}
		if(focusedPane == JmxTuiModeFocusPane.COMMAND) {
			return TuiPaneKeysHint.command();
		}
		return TuiPaneKeysHint.search();
	}

	@Override
	public void collectRemappableWindowActions(
			List<TuiKeyAction> out,
			JmxTuiModeFocusPane focusedPane,
			JmxSettings settings,
			JmxTuiModeContext context) {
		if(out == null) {
			return;
		}
		if(focusedPane == null) {
			focusedPane = JmxTuiModeFocusPane.SEARCH;
		}
		if(focusedPane == JmxTuiModeFocusPane.MBEANS) {
			TuiPaneKeysHint.collectMbeans(out);
			return;
		}
		if(focusedPane == JmxTuiModeFocusPane.FEATURES) {
			TuiPaneKeysHint.collectFeatures(out);
			return;
		}
		if(focusedPane == JmxTuiModeFocusPane.OUTPUT) {
			TuiPaneKeysHint.collectOutput(out);
			return;
		}
		if(focusedPane == JmxTuiModeFocusPane.COMMAND) {
			TuiPaneKeysHint.collectCommand(out);
		}
	}
}
