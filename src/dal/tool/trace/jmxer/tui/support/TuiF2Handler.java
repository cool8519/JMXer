package dal.tool.trace.jmxer.tui.support;

/**
 * 패널별 Info 동작. JmxTuiApp 전역 단축키 처리에서 호출된다.
 * Info를 처리하지 않는 패널은 false를 반환한다.
 *
 */
public interface TuiF2Handler {
	boolean onF2Key();
}
