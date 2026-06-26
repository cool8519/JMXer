package dal.tool.trace.jmxer.tui.output;

/**
 * 의도적으로 캡처 줄이 없는 명령에 대한 TUI Output 패널 동작.
 *
 */
public final class TuiOutputCommandPolicy {

	private TuiOutputCommandPolicy() {}

	public static boolean isClearOutputCommand(String commandLine) {
		if(commandLine == null) {
			return false;
		}
		String trimmed = commandLine.trim();
		if(trimmed.length() == 0) {
			return false;
		}
		int sp = trimmed.indexOf(' ');
		String verb = (sp < 0 ? trimmed : trimmed.substring(0, sp));
		return "CLS".equalsIgnoreCase(verb) || "CLEAR".equalsIgnoreCase(verb);
	}
}
