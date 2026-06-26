package dal.tool.cli;

/**
 * 최상위 ScriptCommand 실행용 선택적 TUI 연동
 */
public final class ScriptUiHooks {

	public interface Handler {
		boolean confirmBeforeScript(String scriptFilesDisplay);

		void beginScript(String rootCommandLine);

		void setCurrentCommand(String scriptFileName, String scriptCommandLine);

		void flushOutputCapture();

		void suspendForUserInput();

		void resumeAfterUserInput();

		void finishScript(boolean completedSuccessfully, boolean cancelled);
	}

	private static volatile Handler handler;
	private static volatile boolean sessionActive;
	private static volatile boolean cancelRequested;
	private static int userInputSuspendDepth;

	private ScriptUiHooks() {}

	public static void setHandler(Handler h) {
		handler = h;
	}

	public static void clearHandler() {
		handler = null;
		sessionActive = false;
		cancelRequested = false;
		userInputSuspendDepth = 0;
	}

	public static boolean hasHandler() {
		return handler != null;
	}

	public static boolean isSessionActive() {
		return sessionActive;
	}

	public static boolean isCancelRequested() {
		return cancelRequested;
	}

	public static void requestCancel() {
		cancelRequested = true;
	}

	public static boolean confirmBeforeScript(String scriptFilesDisplay) {
		Handler h = handler;
		if(h == null || !Logger.isTuiActive()) {
			return true;
		}
		return h.confirmBeforeScript(scriptFilesDisplay);
	}

	public static void beginScript(String rootCommandLine) {
		Handler h = handler;
		if(h == null || !Logger.isTuiActive()) {
			return;
		}
		sessionActive = true;
		cancelRequested = false;
		userInputSuspendDepth = 0;
		h.beginScript(rootCommandLine);
	}

	public static void setCurrentCommand(String scriptFileName, String scriptCommandLine) {
		if(!sessionActive) {
			return;
		}
		Handler h = handler;
		if(h == null || !Logger.isTuiActive()) {
			return;
		}
		h.setCurrentCommand(scriptFileName, scriptCommandLine);
	}

	public static void flushOutputCapture() {
		if(!sessionActive) {
			return;
		}
		Handler h = handler;
		if(h == null || !Logger.isTuiActive()) {
			return;
		}
		h.flushOutputCapture();
	}

	public static void suspendForUserInput() {
		if(!sessionActive) {
			return;
		}
		Handler h = handler;
		if(h == null || !Logger.isTuiActive()) {
			return;
		}
		userInputSuspendDepth++;
		if(userInputSuspendDepth == 1) {
			h.suspendForUserInput();
		}
	}

	public static void resumeAfterUserInput() {
		if(!sessionActive || userInputSuspendDepth <= 0) {
			return;
		}
		Handler h = handler;
		if(h == null || !Logger.isTuiActive()) {
			return;
		}
		userInputSuspendDepth--;
		if(userInputSuspendDepth == 0) {
			h.resumeAfterUserInput();
		}
	}

	public static void finishScript(boolean completedSuccessfully, boolean cancelled) {
		if(!sessionActive) {
			return;
		}
		sessionActive = false;
		userInputSuspendDepth = 0;
		Handler h = handler;
		if(h == null || !Logger.isTuiActive()) {
			cancelRequested = false;
			return;
		}
		h.finishScript(completedSuccessfully, cancelled);
		cancelRequested = false;
	}
}
