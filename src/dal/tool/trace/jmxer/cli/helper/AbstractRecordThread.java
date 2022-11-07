package dal.tool.trace.jmxer.cli.helper;

import java.lang.management.ThreadInfo;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import dal.tool.util.StringUtil;
import dal.tool.util.jmx.JMXUtil;

public abstract class AbstractRecordThread implements Runnable {

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
		int idx = 0;
		for(CompositeData cd : resultDataArr) {
			if(cd == null) continue;
			ThreadInfo threadInfo = ThreadInfo.from(cd);
			threadNames[idx++] = threadInfo.getThreadName();
		}
		afterInit();
	}

	public void startRecording(long interval, long limit) {
		beforeStartRecording();
	    this.recordIntervalMS = interval;
		this.recordLimitMS = limit;
		Thread t = new Thread(this, recordThreadName);
		t.start();
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
		afterStopRecording();
	}
	
	public void run() {
		startTimeMS = System.currentTimeMillis();
		long collectStart = -1L;
		long collectEnd = -1L;
		while(!stop) {
			collectStart = System.currentTimeMillis();
			try {
				collectEnd = collectData();
				System.out.print(".");
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
		System.out.println();
	}

	protected void beforeStartRecording() {}
	
	protected void afterStartRecording() {}
	
	protected void beforeStopRecording() {}
	
	protected void afterStopRecording() {}
	
	protected void afterInit() throws Exception {};
	
	abstract long collectData() throws Exception;

	abstract void printResult();
}
