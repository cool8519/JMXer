package dal.tool.trace.jmxer.cli.data;

import java.io.Serializable;

public class RecordThreadInfo implements Serializable {

	private static final long serialVersionUID = 8911427849221446844L;

	public String threadName;
	public long threadId;
	public long blockedTime;
	public long blockedCount;
	public long waitedTime;
	public long waitedCount;
	public String lockName;
	public long lockOwnerId;
	public String lockOwnerName;
	public boolean inNative;
	public boolean suspended;
	public String threadState;
	public StackTraceElement[] stackTrace;
	public long recordStartTime;
	public long recordEndTime;

	public long getThreadId() {
		return threadId;
	}

	public String getThreadName() {
		return threadName;
	}

	public String getThreadState() {
		return threadState;
	}

	public long getBlockedTime() {
		return blockedTime;
	}

	public long getBlockedCount() {
		return blockedCount;
	}

	public long getWaitedTime() {
		return waitedTime;
	}

	public long getWaitedCount() {
		return waitedCount;
	}

	public String getLockName() {
		return lockName;
	}

	public long getLockOwnerId() {
		return lockOwnerId;
	}

	public String getLockOwnerName() {
		return lockOwnerName;
	}

	public StackTraceElement[] getStackTrace() {
		return stackTrace;
	}

	public boolean isSuspended() {
		return suspended;
	}

	public boolean isInNative() {
		return inNative;
	}

	public long getRecordStartTime() {
		return recordStartTime;
	}

	public long getRecordEndTime() {
		return recordEndTime;
	}
	
}
