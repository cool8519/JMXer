package dal.tool.trace.jmxer.cli.helper;

import dal.tool.trace.jmxer.cli.data.RecordThreadSampleState;

public class RecordRequestWaitClassifier {

	private RecordRequestWaitClassifier() {}

	public static RecordThreadSampleState classify(StackTraceElement[] stackTrace) {
		if(stackTrace == null || stackTrace.length < 1) {
			return RecordThreadSampleState.ACTIVE;
		}
		boolean hasWaitingSignature = false;
		for(StackTraceElement el : stackTrace) {
			String className = el.getClassName();
			String methodName = el.getMethodName();
			if(isApplicationFrame(className, methodName)) {
				return RecordThreadSampleState.ACTIVE;
			}
			if(RecordRequestWaitSignatures.isWaitingSignature(className, methodName)) {
				hasWaitingSignature = true;
			}
		}
		return hasWaitingSignature ? RecordThreadSampleState.WAITING_REQUEST : RecordThreadSampleState.ACTIVE;
	}

	private static boolean isApplicationFrame(String className, String methodName) {
		String lowerClass = className.toLowerCase();
		return lowerClass.contains("controller")
			|| lowerClass.contains("service")
			|| lowerClass.contains("repository")
			|| lowerClass.contains("dispatcherservlet")
			|| "doFilter".equals(methodName);
	}
}
