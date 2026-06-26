package dal.tool.cli;

/**
 * SleepCommand용 선택적 UI 연동(TUI 진행 대화상자)
 */
public final class SleepUiHooks {

	public interface Handler {
		/**
		 * @param sleepMs 대기 시간(밀리초)
		 * @return 전체 시간이 경과하면 true, 취소되면 false
		 */
		boolean waitForSleep(long sleepMs);
	}

	private SleepUiHooks() {}

	public static volatile Handler handler;

	public static void setHandler(Handler h) {
		handler = h;
	}

	public static void clearHandler() {
		handler = null;
	}

	public static boolean hasHandler() {
		return handler != null;
	}

	/**
	 * sleepMs 동안 대기한다. 핸들러가 있으면 그것을 쓰고, 없으면 Thread.sleep을 사용한다.
	 */
	public static boolean waitForSleep(long sleepMs) {
		if(sleepMs <= 0L) {
			return true;
		}
		Handler h = handler;
		if(h != null && Logger.isTuiActive()) {
			return h.waitForSleep(sleepMs);
		}
		try {
			Thread.sleep(sleepMs);
			return true;
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}
}
