package dal.tool.trace.jmxer.cli;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import dal.tool.cli.Logger;
import dal.tool.cli.util.IOUtil;

/**
 * Warns and asks for confirmation before CALL commands that may modify the target JVM
 * (attribute write or operation invoke). Used from CLI and TUI ({@link #setHandler}).
 */
public final class JmxMutatingCallConfirmation {

	/**
	 * UI-specific confirmation (TUI dialog). When unset, CLI uses {@link IOUtil#readLine}.
	 */
	public interface Handler {
		/**
		 * @return true to perform the CALL, false to cancel
		 */
		boolean confirm(String title, String message);
	}

	private static final String CONFIRM_TITLE = "Confirm";
	private static volatile Handler handler;

	private JmxMutatingCallConfirmation() {}

	public static void setHandler(Handler h) {
		handler = h;
	}

	public static void clearHandler() {
		handler = null;
	}

	/**
	 * @return true if the user chose to proceed (or no confirmation was required)
	 */
	public static boolean confirmIfNeeded(MBeanServerConnection conn, ObjectName objectName, String targetName,
			String[] targetValues, int mbeanTargetCount) throws Exception {
		if(conn == null || objectName == null || targetName == null) {
			return true;
		}
		if(!isMutatingCall(conn, objectName, targetName, targetValues)) {
			return true;
		}
		return askUser(CONFIRM_TITLE, buildWarningMessage(objectName, targetName, targetValues, mbeanTargetCount));
	}

	public static boolean isMutatingCall(MBeanServerConnection conn, ObjectName objectName, String targetName,
			String[] targetValues) throws Exception {
		if(conn == null || objectName == null || targetName == null) {
			return false;
		}
		if(isReadOnlyCallTarget(targetName, targetValues)) {
			return false;
		}
		if(targetValues != null && targetValues.length > 0) {
			return true;
		}
		if(targetName.indexOf('(') > 0) {
			return true;
		}
		MBeanInfo mbeanInfo = conn.getMBeanInfo(objectName);
		MBeanAttributeInfo[] attrs = mbeanInfo.getAttributes();
		if(attrs != null) {
			for(int i = 0; i < attrs.length; i++) {
				if(attrs[i].getName().equals(targetName)) {
					return false;
				}
			}
		}
		MBeanOperationInfo[] ops = mbeanInfo.getOperations();
		if(ops != null) {
			for(int i = 0; i < ops.length; i++) {
				if(ops[i].getName().equals(targetName)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean isReadOnlyCallTarget(String targetName, String[] targetValues) {
		if("?".equals(targetName) || "??".equals(targetName) || "#".equals(targetName)) {
			return true;
		}
		if(targetValues != null && targetValues.length == 1 && "?".equals(targetValues[0])) {
			return true;
		}
		return false;
	}

	private static String buildWarningMessage(ObjectName objectName, String targetName, String[] targetValues,
			int mbeanTargetCount) {
		StringBuilder sb = new StringBuilder();
		sb.append("[CAUTION] This CALL may modify the target JVM. Changes can affect production systems.");
		sb.append("\n          Review carefully before proceeding.");
		if(mbeanTargetCount > 1) {
			sb.append("\n\nThis CALL applies to ").append(mbeanTargetCount).append(" MBean(s).");
		}
		sb.append("\n\nObjectName: ").append(objectName);
		sb.append("\nTarget: ").append(targetName);
		if(targetValues != null && targetValues.length > 0) {
			sb.append("\nValue/arguments: ");
			for(int i = 0; i < targetValues.length; i++) {
				if(i > 0) {
					sb.append(", ");
				}
				sb.append(targetValues[i]);
			}
		}
		sb.append("\n\nProceed with this change?");
		return sb.toString();
	}

	private static boolean askUser(String title, String message) {
		Handler h = handler;
		if(h != null) {
			return h.confirm(title, message);
		}
		Logger.logln(message);
		String yn = null;
		while(true) {
			yn = IOUtil.readLine("Proceed? (Y/N): ", Logger.Level.RESULT);
			if(yn == null) {
				yn = "";
			}
			yn = yn.trim().toUpperCase();
			if("Y".equals(yn) || "N".equals(yn)) {
				break;
			}
			Logger.logln("Enter Y to proceed or N to cancel.");
		}
		return "Y".equals(yn);
	}
}
