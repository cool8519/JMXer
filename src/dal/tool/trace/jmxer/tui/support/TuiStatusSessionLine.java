package dal.tool.trace.jmxer.tui.support;

/**
 * 상태 줄 1 형식: Session Mode, Target JVM 요약, UI Mode, Record view 접미사.
 *
 */
public final class TuiStatusSessionLine {
	private static final String ITEM_SEP = " | ";

	private TuiStatusSessionLine() {}

	public static String build(String sessionMode, String connTarget, boolean online,
			String targetJvmSuffix, String uiMode, String recordViewSuffix) {
		StringBuilder sb = new StringBuilder();
		sb.append(" Session Mode: ");
		sb.append(sessionMode == null ? "" : sessionMode);
		if(online && connTarget != null && connTarget.length() > 0) {
			sb.append("(").append(connTarget).append(")");
		}
		if(targetJvmSuffix != null && targetJvmSuffix.length() > 0) {
			sb.append(ITEM_SEP).append(targetJvmSuffix);
		}
		sb.append(ITEM_SEP).append("UI Mode: ");
		sb.append(uiMode == null ? "" : uiMode);
		if(recordViewSuffix != null && recordViewSuffix.length() > 0) {
			sb.append(" ").append(recordViewSuffix);
		}
		return sb.toString();
	}
}
