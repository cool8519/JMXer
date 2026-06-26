package dal.tool.cli;

/**
 * 풀스크린 TUI 등에서 외부 프로세스(에디터, 셸) 실행 전후로 터미널을 일시 정지/복구하기 위한 훅.
 */
public final class ExternalUiHooks {

	private static final String TUI_EXTERNAL_FINISHED_MESSAGE =
			"[External finished: exit code %d, %d ms]";

	private ExternalUiHooks() {}

	public static volatile Runnable beforeBlockingExternalProcess;
	public static volatile Runnable afterBlockingExternalProcess;

	public static void runBeforeBlockingExternalProcess() {
		Runnable r = beforeBlockingExternalProcess;
		if(r != null) {
			try {
				r.run();
			} catch(Exception e) {}
		}
	}

	public static void runAfterBlockingExternalProcess() {
		Runnable r = afterBlockingExternalProcess;
		if(r != null) {
			try {
				r.run();
			} catch(Exception e) {}
		}
	}

	/** TUI Output: 외부 명령 stdout/stderr 종료 후 빈 줄과 괄호 푸터를 출력한다. */
	public static void notifyTuiExternalCommandFinished(int exitCode, long elapsedMs) {
		if(!Logger.isTuiActive()) {
			return;
		}
		Logger.logBlankLine();
		Logger.logln(String.format(TUI_EXTERNAL_FINISHED_MESSAGE,
				Integer.valueOf(exitCode), Long.valueOf(elapsedMs)));
	}
}
