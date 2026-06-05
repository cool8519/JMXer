package dal.tool.trace.jmxer.cli.helper;

import java.util.Set;

import dal.tool.trace.jmxer.cli.data.RecordThreadSampleState;

public class RecordRequestWaitClassifier {

	private RecordRequestWaitClassifier() {}

	public static RecordThreadSampleState classify(StackTraceElement[] stackTrace) {
		return classify(stackTrace, null);
	}

	public static RecordThreadSampleState classify(StackTraceElement[] stackTrace, Set<String> userSignatures) {
		if(stackTrace == null || stackTrace.length < 1) {
			return RecordThreadSampleState.ACTIVE;
		}
		boolean hasWaitingSignature = false;
		for(StackTraceElement el : stackTrace) {
			String className = el.getClassName();
			String methodName = el.getMethodName();
			if(isRequestEntry(className, methodName, userSignatures)) {
				return RecordThreadSampleState.ACTIVE;
			}
			if(RecordRequestWaitSignatures.isWaitingSignature(className, methodName)) {
				hasWaitingSignature = true;
			}
		}
		return hasWaitingSignature ? RecordThreadSampleState.WAITING_REQUEST : RecordThreadSampleState.ACTIVE;
	}

	public static boolean hasRequestEntry(StackTraceElement[] stackTrace) {
		return hasRequestEntry(stackTrace, null);
	}

	public static boolean hasRequestEntry(StackTraceElement[] stackTrace, Set<String> userSignatures) {
		if(stackTrace == null || stackTrace.length < 1) {
			return false;
		}
		for(StackTraceElement el : stackTrace) {
			if(isRequestEntry(el.getClassName(), el.getMethodName(), userSignatures)) {
				return true;
			}
		}
		return false;
	}

	/** Thread STACKTRACE_TARGET=ACTIVE: whitelist only (no application-frame heuristic). */
	public static boolean hasWhitelistedRequestEntry(StackTraceElement[] stackTrace) {
		return hasWhitelistedRequestEntry(stackTrace, null);
	}

	public static boolean hasWhitelistedRequestEntry(StackTraceElement[] stackTrace, Set<String> userSignatures) {
		if(stackTrace == null || stackTrace.length < 1) {
			return false;
		}
		for(StackTraceElement el : stackTrace) {
			if(RequestEntrySignatures.isEntrySignature(el.getClassName(), el.getMethodName(), userSignatures)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isRequestEntry(String className, String methodName) {
		return isRequestEntry(className, methodName, null);
	}

	private static boolean isRequestEntry(String className, String methodName, Set<String> userSignatures) {
		return RequestEntrySignatures.isEntrySignature(className, methodName, userSignatures)
			|| isApplicationFrame(className, methodName);
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
