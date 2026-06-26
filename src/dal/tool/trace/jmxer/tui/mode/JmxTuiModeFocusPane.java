package dal.tool.trace.jmxer.tui.mode;

/**
 * JmxTuiModeContext 내 포커스 대상 패널 식별자.
 * UI Mode 복귀 시 키보드 포커스를 복원하는 데 사용한다.
 *
 */
public enum JmxTuiModeFocusPane {
	SEARCH,
	MBEANS,
	FEATURES,
	OUTPUT,
	COMMAND
}
