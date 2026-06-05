package dal.tool.trace.jmxer.cli;

import java.io.IOException;

import dal.tool.cli.Logger;
import dal.tool.cli.Logger.Level;
import dal.tool.trace.jmxer.JMXControl;
import dal.tool.util.jmx.MBeanConnector;

/**
 * Reconnects to the target JVM after a JMX disconnect. In TUI online sessions the user is prompted
 * (no automatic {@link MBeanConnector#connect()} before the dialog — attach reconnect can abort the JVM).
 */
public final class JmxConnectionRecovery {

	private static final String DISCONNECTED_MSG = "The JMX Connection was disconnected : ";
	private static final String RECONNECT_DEBUG = "Reconnecting to JVM...";

	private JmxConnectionRecovery() {}

	public static IOException unwrapDisconnectCause(Throwable t) {
		while(t != null) {
			if(t instanceof IOException) {
				return (IOException)t;
			}
			t = t.getCause();
		}
		return null;
	}

	public static boolean isLikelyConnectionFailure(Throwable t) {
		if(unwrapDisconnectCause(t) != null) {
			return true;
		}
		while(t != null) {
			String cn = t.getClass().getName();
			if(cn.indexOf("Connect") >= 0 || cn.indexOf("Connection") >= 0) {
				return true;
			}
			String msg = t.getMessage();
			if(msg != null) {
				String lower = msg.toLowerCase();
				if(lower.indexOf("connection") >= 0
						|| lower.indexOf("disconnected") >= 0
						|| lower.indexOf("not connected") >= 0
						|| lower.indexOf("refused") >= 0
						|| lower.indexOf("connection reset") >= 0) {
					return true;
				}
			}
			t = t.getCause();
		}
		return false;
	}

	public static IOException asDisconnectIOException(Throwable t) {
		IOException io = unwrapDisconnectCause(t);
		if(io != null) {
			return io;
		}
		if(isLikelyConnectionFailure(t)) {
			String msg = t.getMessage();
			return new IOException(msg == null ? "JMX connection failure" : msg, t);
		}
		return null;
	}

	/**
	 * Marks the connector stale and shows the TUI Connection Lost dialog when {@code failure} is a disconnect.
	 *
	 * @param suppressPrompt {@code true} while already handling a disconnect (avoids duplicate dialogs)
	 */
	public static void handleTuiDisconnect(MBeanConnector connector, Throwable failure, boolean suppressPrompt) {
		IOException disconnectCause = asDisconnectIOException(failure);
		if(disconnectCause == null) {
			return;
		}
		if(connector != null) {
			connector.markConnectionStale();
		}
		if(suppressPrompt || !Logger.isTuiActive() || JMXControl.isOfflineSession) {
			return;
		}
		if(JmxTuiConnectionLossHandler.isExitScheduled()
				|| JmxTuiConnectionLossHandler.isLossDialogSessionActive()) {
			return;
		}
		JmxTuiConnectionLossHandler.notifyUnrecoverableLoss(disconnectCause.getMessage());
	}

	/**
	 * @throws Exception the reconnect failure, or {@code disconnectCause} when reconnect is not applicable
	 */
	public static void reconnectOrThrow(MBeanConnector connector, IOException disconnectCause) throws Exception {
		if(connector == null || JMXControl.isOfflineSession) {
			throw disconnectCause;
		}
		Logger.logln(Level.ERROR, DISCONNECTED_MSG + disconnectCause.getMessage());

		if(Logger.isTuiActive()) {
			connector.markConnectionStale();
			if(!JmxTuiConnectionLossHandler.isLossDialogSessionActive()) {
				JmxTuiConnectionLossHandler.notifyUnrecoverableLoss(disconnectCause.getMessage());
			}
			throw disconnectCause;
		}

		Logger.logln(Level.DEBUG, RECONNECT_DEBUG);
		try {
			connector.connect();
		} catch(Throwable t) {
			Exception e = (t instanceof Exception) ? (Exception)t : new Exception(t);
			throw e;
		}
	}
}
