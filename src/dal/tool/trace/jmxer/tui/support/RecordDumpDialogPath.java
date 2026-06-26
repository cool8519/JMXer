package dal.tool.trace.jmxer.tui.support;

import dal.tool.trace.jmxer.JMXerConstant;

/**
 * Record UI Save/Load 대화상자에 마지막으로 입력한 dump 파일 경로를 기억한다.
 *
 */
public final class RecordDumpDialogPath {

	private static String lastEnteredPath;

	private RecordDumpDialogPath() {}

	public static String getInitialPath() {
		if(lastEnteredPath != null && lastEnteredPath.trim().length() > 0) {
			return lastEnteredPath;
		}
		return JMXerConstant.DEFAULT_DUMP_FILE_PATH;
	}

	public static void rememberEnteredPath(String path) {
		if(path == null) {
			return;
		}
		String trimmed = path.trim();
		if(trimmed.length() > 0) {
			lastEnteredPath = trimmed;
		}
	}
}
