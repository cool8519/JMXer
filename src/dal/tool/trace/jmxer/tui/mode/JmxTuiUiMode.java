package dal.tool.trace.jmxer.tui.mode;

import java.util.List;

import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;

import dal.tool.trace.jmxer.cli.JmxSettings;
import dal.tool.trace.jmxer.tui.app.JmxTuiApp;
import dal.tool.trace.jmxer.tui.support.TuiKeyAction;
import dal.tool.trace.jmxer.tui.support.TuiStatusSegmentLabel;
import dal.tool.trace.jmxer.tui.support.TuiStatusText;

/**
 * TUI UI Mode 추상 클래스. 모드별 패널 레이아웃·상태 표시·단축키 힌트 계약을 정의한다.
 *
 */
public abstract class JmxTuiUiMode {
	private static final int DEFAULT_HORIZONTAL_LEFT_SHARE_NUMERATOR = 3;
	private static final int DEFAULT_HORIZONTAL_LEFT_SHARE_DENOMINATOR = 5;
	private static final int DEFAULT_VERTICAL_LISTS_BAND_SHARE_NUMERATOR = 1;
	private static final int DEFAULT_VERTICAL_LISTS_BAND_SHARE_DENOMINATOR = 4;

	private JmxTuiModeContext context;

	public abstract String getId();
	public abstract String getDisplayName();

	public final JmxTuiModeContext getOrCreateContext(JmxTuiApp app) {
		if(context == null) {
			context = app.createModeContext(this);
		}
		return context;
	}

	public abstract boolean isOutputInRightColumn();

	public abstract boolean isFeaturesPaneAvailable();

	public abstract boolean canResizeVerticalSplit();

	public int getDefaultHorizontalLeftShareNumerator() {
		return DEFAULT_HORIZONTAL_LEFT_SHARE_NUMERATOR;
	}

	public int getDefaultHorizontalLeftShareDenominator() {
		return DEFAULT_HORIZONTAL_LEFT_SHARE_DENOMINATOR;
	}

	public int getDefaultVerticalListsBandShareNumerator() {
		return DEFAULT_VERTICAL_LISTS_BAND_SHARE_NUMERATOR;
	}

	public int getDefaultVerticalListsBandShareDenominator() {
		return DEFAULT_VERTICAL_LISTS_BAND_SHARE_DENOMINATOR;
	}

	public abstract void buildBody(JmxTuiApp app, Panel root);

	public void renderStatusObjectLine(TuiStatusSegmentLabel header2, JmxSettings s, JmxTuiModeContext context, int statusInnerCols) {
		String mbeanName = s.getMBeanObjectName() == null ? "" : s.getMBeanObjectName();
		header2.setMaxColumns(statusInnerCols);
		header2.setText(TuiStatusText.fitSingleLine("   - Current MBean:" + mbeanName, statusInnerCols));
	}

	public abstract void renderStatusDetailLine(Label header3, JmxSettings s, JmxTuiModeContext context, int statusInnerCols);

	public abstract String getPaneKeysHint(
		JmxTuiModeFocusPane focusedPane,
		boolean wrapOn,
		JmxSettings settings,
		JmxTuiModeContext context
	);

	/**
	 * {@link #getPaneKeysHint}와 동일 조건으로 리매핑 가능한 Window 동작을 채운다.
	 */
	public abstract void collectRemappableWindowActions(
		List<TuiKeyAction> out,
		JmxTuiModeFocusPane focusedPane,
		JmxSettings settings,
		JmxTuiModeContext context
	);
}
