package dal.tool.trace.jmxer.cli;

import dal.tool.cli.Logger;
import dal.tool.trace.jmxer.JMXControl;

/**
 * 자동 재연결에 실패한 JMX 연결 끊김을 TUI에서 처리한다.
 * TuiBootstrap에서 핸들러를 등록한다.
 */
public final class JmxTuiConnectionLossHandler {

	/** TUI 연결 끊김 이벤트 콜백 */
	public interface Handler {
		void onConnectionLoss(String message);
	}

	public static final String DIALOG_TITLE = "Connection Lost";
	public static final String ACTION_RECONNECT = "Reconnect";
	public static final String ACTION_CLOSE = "Close";
	public static final String DIALOG_BODY_PREFIX =
			"The JMX connection to the target JVM was lost and could not be restored automatically.";
	public static final String DIALOG_BODY_SUFFIX =
			"Choose Reconnect to try again, or Close to quit JMXer.";

	private static volatile Handler handler;
	private static volatile boolean prompting;
	private static volatile boolean connectionLossUiActive;
	private static volatile boolean lossDialogSessionActive;
	private static volatile boolean exitScheduled;

	private JmxTuiConnectionLossHandler() {}

	public static void setHandler(Handler h) {
		handler = h;
	}

	public static void clearHandler() {
		handler = null;
		prompting = false;
		connectionLossUiActive = false;
		lossDialogSessionActive = false;
		exitScheduled = false;
	}

	public static boolean isConnectionLossUiActive() {
		return connectionLossUiActive;
	}

	public static boolean isLossDialogSessionActive() {
		return lossDialogSessionActive;
	}

	public static void setLossDialogSessionActive(boolean active) {
		lossDialogSessionActive = active;
	}

	public static boolean isExitScheduled() {
		return exitScheduled;
	}

	public static void markExitChosen() {
		exitScheduled = true;
	}

	/**
	 * TUI 핸들러가 이벤트를 처리했으면 true
	 */
	public static boolean notifyUnrecoverableLoss(String message) {
		if(!Logger.isTuiActive() || JMXControl.isOfflineSession) {
			return false;
		}
		if(prompting || connectionLossUiActive || exitScheduled || lossDialogSessionActive) {
			return true;
		}
		Handler h = handler;
		if(h == null) {
			return false;
		}
		prompting = true;
		connectionLossUiActive = true;
		try {
			h.onConnectionLoss(message);
		} catch(Exception e) {
			Logger.logln(Logger.Level.ERROR,
					"Connection loss handler failed: " + e.getClass().getSimpleName()
					+ (e.getMessage() != null ? (" - " + e.getMessage()) : ""));
		}
		finally {
			connectionLossUiActive = false;
			prompting = false;
		}
		return true;
	}

	public static String formatDialogBody(String detailMessage) {
		StringBuilder sb = new StringBuilder();
		sb.append(DIALOG_BODY_PREFIX);
		if(detailMessage != null && detailMessage.trim().length() > 0) {
			sb.append("\n\n").append(detailMessage.trim());
		}
		sb.append("\n\n").append(DIALOG_BODY_SUFFIX);
		return sb.toString();
	}
}
