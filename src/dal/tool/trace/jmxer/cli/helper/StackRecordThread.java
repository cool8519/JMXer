package dal.tool.trace.jmxer.cli.helper;

import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.openmbean.CompositeData;

import dal.tool.cli.Logger;
import dal.tool.trace.jmxer.cli.data.RecordResult;
import dal.tool.trace.jmxer.cli.data.RecordThreadInfo;
import dal.tool.util.StringUtil;
import dal.tool.util.jmx.JMXUtil;

public class StackRecordThread implements Runnable {

	private boolean stop = false;
	private MBeanServerConnection mbeanConnection = null;
	private long recordLimitMS = 0L;
	private long recordIntervalMS = 1000L;
	private String[] threadIds = null;
	private long startTimeMS = 0L;
	private long endTimeMS = 0L;
	private int sampleCount = 0;
	private HashMap<Long,List<RecordThreadInfo>> recordData = null;

	public StackRecordThread(MBeanServerConnection mbeanConnection, String[] threadIds) throws Exception {
		this.mbeanConnection = mbeanConnection;
	    this.threadIds = threadIds;
	    init();
	}

	private void init() throws Exception {
		if(threadIds == null) {
			long[] ids = JMXUtil.getAllThreadIds(mbeanConnection, true);
			threadIds = new String[ids.length];
			for(int i = 0; i < ids.length; i++) {
				threadIds[i] = String.valueOf(ids[i]);
			}
		}
		recordData = new HashMap<Long,List<RecordThreadInfo>>();
	}

	public int getSampleCount() {
		return sampleCount;
	}
	
	public void startRecording(long interval, long limit) {
	    this.recordIntervalMS = interval;
		this.recordLimitMS = limit;
		Thread t = new Thread(this, "StackRecorder");
		t.start();
		if(recordLimitMS > 0) {
			while(!stop) {
				try {
					Thread.sleep(1000L);
				} catch(InterruptedException e) {				
				}
			}
		}
	}
	
	public void stopRecording() {
		endTimeMS = System.currentTimeMillis();
		this.stop = true;
	}
	
	public void run() {
		startTimeMS = System.currentTimeMillis();
		long collectStart = -1L;
		long collectEnd = -1L;
		while(!stop) {
			collectStart = System.currentTimeMillis();
			try {
				collectEnd = collectData(collectStart);
				System.out.print(".");
			} catch(Exception e) {
				e.printStackTrace();
			}
			long total_elapsed = collectEnd - startTimeMS;
			if(recordLimitMS > 0 && total_elapsed >= recordLimitMS) {
				stopRecording(); break;
			}
			long elapsed = collectEnd - collectStart;
			if(elapsed < recordIntervalMS) {
				try {
					Thread.sleep(recordIntervalMS - elapsed);
				} catch(InterruptedException e) {				
				}
			}
		}
		System.out.println();
	}

	private long collectData(long start) throws Exception {
		Object resultData = null;		
		if(threadIds.length == 1 && threadIds[0].equals("*")) {
			resultData = JMXUtil.getJMXResult(mbeanConnection, "java.lang:type=Threading", "dumpAllThreads", new String[]{"false","false"});
		} else {
			String threadIdsString = StringUtil.arrayToString(threadIds, ",");
			String maxDepthString = String.valueOf(Integer.MAX_VALUE);
    		resultData = JMXUtil.getJMXResult(mbeanConnection, "java.lang:type=Threading", "getThreadInfo(long[],int)", new String[]{threadIdsString,maxDepthString});
		}
		long end = System.currentTimeMillis();
		CompositeData[] resultDataArr = null;
		if(resultData == null) {
			throw new Exception("Could not get thread info.");
		} else {
			resultDataArr = (CompositeData[])resultData;
			if(resultDataArr == null || resultDataArr.length < 1) {
				throw new Exception("Could not get thread info.");
			}
		}		
    	for(CompositeData cd : resultDataArr) {
    		if(cd == null) {
    			continue;
    		}
    		ThreadInfo threadInfo = ThreadInfo.from(cd);
    		List<RecordThreadInfo> threadInfoList = recordData.get(threadInfo.getThreadId());
    		if(threadInfoList == null) {
    			threadInfoList = new ArrayList<RecordThreadInfo>();
    		}
    		RecordThreadInfo recThrInfo = toRecordThreadInfo(threadInfo);
    		recThrInfo.recordStartTime = start;
    		recThrInfo.recordEndTime = end;
    		threadInfoList.add(recThrInfo);
    		recordData.put(threadInfo.getThreadId(), threadInfoList);
    	}
		sampleCount++;
		return end;
	}

	public void printResult() {
		if(sampleCount < 1) {
			Logger.logln("No data collected.");
			return;
		}
		Logger.logln("Recorded for " + (endTimeMS-startTimeMS) + "ms (" + sampleCount + " sampled)");
	}

	public RecordThreadInfo toRecordThreadInfo(ThreadInfo thrInfo) {
		RecordThreadInfo recThrInfo = new RecordThreadInfo();
		recThrInfo.threadName = thrInfo.getThreadName();
		recThrInfo.threadId = thrInfo.getThreadId();
		recThrInfo.blockedTime = thrInfo.getBlockedTime();
		recThrInfo.blockedCount = thrInfo.getBlockedCount();
		recThrInfo.waitedTime = thrInfo.getWaitedTime();
		recThrInfo.waitedCount = thrInfo.getWaitedCount();
		recThrInfo.lockName = thrInfo.getLockName();
		recThrInfo.lockOwnerId = thrInfo.getLockOwnerId();
		recThrInfo.lockOwnerName = thrInfo.getLockOwnerName();
		recThrInfo.inNative = thrInfo.isInNative();
		recThrInfo.suspended = thrInfo.isSuspended();
		recThrInfo.threadState = thrInfo.getThreadState().name();
		recThrInfo.stackTrace = thrInfo.getStackTrace();				
		return recThrInfo;		
	}
	
	public RecordResult getRecordResult() {
		RecordResult result = new RecordResult();
		result.startTime = this.startTimeMS;
		result.endTime = this.endTimeMS;
		result.recordLimitMS = this.recordLimitMS;
		result.recordIntervalMS = this.recordIntervalMS;
		result.sampleCount = this.sampleCount;
		result.recordData = recordData;
		return result;
	}

}
