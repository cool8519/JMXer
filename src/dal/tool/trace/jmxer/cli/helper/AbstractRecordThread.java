package dal.tool.trace.jmxer.cli.helper;

import java.lang.management.ThreadInfo;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import dal.tool.cli.Logger;
import dal.tool.util.StringUtil;
import dal.tool.util.jmx.JMXUtil;

/**
 * RECORD 스택·리소스 샘플링 스레드 공통 베이스
 *
 */
public abstract class AbstractRecordThread implements Runnable {

	protected static final String UNKNOWN_THREAD_NAME = "<unknown>";

	protected boolean stop = false;
	protected String recordThreadName;
	protected MBeanServerConnection mbeanConnection = null;
	protected long recordLimitMS = 0L;
	protected long recordIntervalMS = 1000L;
	protected String[] threadIds = null;
	protected String threadIdsString = null;
	protected String[] threadNames = null;
	protected long startTimeMS = 0L;
	protected long endTimeMS = 0L;
	protected Thread recordRunnerThread = null;
	private boolean tuiSampleProgressLineStarted;
	private static volatile AbstractRecordThread activeRecording;

	public AbstractRecordThread(String recordThreadName, MBeanServerConnection mbeanConnection, String[] threadIds) throws Exception {
		this.recordThreadName = recordThreadName;
		this.mbeanConnection = mbeanConnection;
	    this.threadIds = threadIds;
	    init();
	}

	protected void init() throws Exception {
		ObjectName objectName = new ObjectName("java.lang:type=Threading");
		if(threadIds == null || threadIds.length == 1 && "*".equals(threadIds[0])) {
			long[] ids = JMXUtil.getAllThreadIds(mbeanConnection, true);
			threadIds = new String[ids.length];
			for(int i = 0; i < ids.length; i++) {
				threadIds[i] = String.valueOf(ids[i]);
			}
		}
		threadIdsString = StringUtil.arrayToString(threadIds, ",");
		threadNames = new String[threadIds.length];
		Object resultData = JMXUtil.invokeAttributeOrOperation(mbeanConnection, objectName, "getThreadInfo(long[])", new String[]{threadIdsString});
		if(resultData == null || ((CompositeData[])resultData).length < 1) {
			throw new Exception("Could not get thread info.");
		}
		CompositeData[] resultDataArr = (CompositeData[])resultData;
		if(resultDataArr.length != threadIds.length) {
			throw new Exception("Could not get thread info.");
		}
		for(int i = 0; i < threadIds.length; i++) {
			threadNames[i] = resolveThreadName(resultDataArr[i]);
		}
		afterInit();
	}

	protected static String resolveThreadName(CompositeData compositeData) {
		if(compositeData == null) {
			return UNKNOWN_THREAD_NAME;
		}
		ThreadInfo threadInfo = ThreadInfo.from(compositeData);
		if(threadInfo == null) {
			return UNKNOWN_THREAD_NAME;
		}
		String name = threadInfo.getThreadName();
		if(name == null || name.length() == 0) {
			return UNKNOWN_THREAD_NAME;
		}
		return name;
	}

	protected static String formatQuotedThreadName(String threadName) {
		String normalized = normalizeThreadName(threadName);
		return "\"" + StringUtil.shortenStringWithSuffix(normalized, 38, "...") + "\"";
	}

	protected static String normalizeThreadName(String threadName) {
		if(threadName == null || threadName.length() == 0) {
			return UNKNOWN_THREAD_NAME;
		}
		return threadName;
	}

	public static AbstractRecordThread getActiveRecording() {
		return activeRecording;
	}

	public static void stopActiveRecording() {
		AbstractRecordThread active = activeRecording;
		if(active != null) {
			active.stopRecording();
		}
	}

	protected void markActiveRecording() {
		activeRecording = this;
	}

	protected void clearActiveRecording() {
		if(activeRecording == this) {
			activeRecording = null;
		}
	}

	public void startRecording(long interval, long limit) {
		beforeStartRecording();
	    this.recordIntervalMS = interval;
		this.recordLimitMS = limit;
		recordRunnerThread = new Thread(this, recordThreadName);
		recordRunnerThread.start();
		if(recordLimitMS > 0) {
			while(!stop) {
				try {
					Thread.sleep(1000L);
				} catch(InterruptedException e) {				
				}
			}
		}
		afterStartRecording();
	}
	
	public void stopRecording() {
		beforeStopRecording();
		endTimeMS = System.currentTimeMillis();
		this.stop = true;
		if(recordRunnerThread != null && recordRunnerThread != Thread.currentThread()) {
			recordRunnerThread.interrupt();
			try {
				recordRunnerThread.join(2000L);
			} catch(InterruptedException ie) {
				Thread.currentThread().interrupt();
			}
		}
		afterStopRecording();
	}
	
	public void run() {
		tuiSampleProgressLineStarted = false;
		startTimeMS = System.currentTimeMillis();
		long collectStart = -1L;
		long collectEnd = -1L;
		while(!stop) {
			collectStart = System.currentTimeMillis();
			try {
				collectEnd = collectData();
				emitSampleProgressMarker();
			} catch(Exception e) {
				e.printStackTrace();
			}
			long total_elapsed = collectEnd - startTimeMS;
			if(recordLimitMS > 0 && total_elapsed >= recordLimitMS) {
				stopRecording(); break;
			}
			long elapsed = collectEnd - collectStart;
			if(elapsed < 0) {
				try {
					Thread.sleep(recordIntervalMS);
				} catch(InterruptedException e) {				
				}
			} else {
				if(elapsed < recordIntervalMS) {
					try {
						Thread.sleep(recordIntervalMS - elapsed);
					} catch(InterruptedException e) {				
					}
				}
			}
		}
		emitSampleProgressEnd();
	}

	private void emitSampleProgressMarker() {
		if(Logger.isTuiActive() && Logger.isTuiPanelCapture()) {
			if(!tuiSampleProgressLineStarted) {
				Logger.logln(".");
				tuiSampleProgressLineStarted = true;
			} else {
				Logger.appendTuiInlineFragment(".");
			}
			return;
		}
		System.out.print(".");
	}

	private void emitSampleProgressEnd() {
		if(!Logger.isTuiActive() || !Logger.isTuiPanelCapture()) {
			System.out.println();
		}
	}

	protected void beforeStartRecording() {}
	
	protected void afterStartRecording() {}
	
	protected void beforeStopRecording() {}
	
	protected void afterStopRecording() {}
	
	protected void afterInit() throws Exception {};
	
	abstract long collectData() throws Exception;

	abstract void printResult();
}
