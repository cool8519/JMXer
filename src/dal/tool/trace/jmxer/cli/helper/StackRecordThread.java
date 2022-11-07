package dal.tool.trace.jmxer.cli.helper;

import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import dal.tool.cli.Logger;
import dal.tool.trace.jmxer.cli.data.RecordResult;
import dal.tool.trace.jmxer.cli.data.RecordThreadInfo;
import dal.tool.trace.jmxer.cli.data.ResourceUsage;
import dal.tool.util.StringUtil;
import dal.tool.util.jmx.JMXUtil;

public class StackRecordThread extends AbstractRecordThread {

	protected HashMap<Long,List<RecordThreadInfo>> recordData;
	protected int sampleCount = 0;
	protected HashMap<Long,ResourceUsage> resourceData;
	protected boolean threadCpuTimeEnabled;
	protected boolean threadAllocatedMemoryEnabled;

	public StackRecordThread(MBeanServerConnection mbeanConnection, String[] threadIds) throws Exception {
		super("StackRecorder", mbeanConnection, threadIds);
	}

	protected void afterInit() throws Exception {
		recordData = new HashMap<Long,List<RecordThreadInfo>>();
		resourceData = new HashMap<Long,ResourceUsage>();
		threadCpuTimeEnabled = (Boolean)JMXUtil.getJMXResult(mbeanConnection, "java.lang:type=Threading", "ThreadCpuTimeEnabled", null);
		threadAllocatedMemoryEnabled = (Boolean)JMXUtil.getJMXResult(mbeanConnection, "java.lang:type=Threading", "ThreadAllocatedMemoryEnabled", null);
	}
	
	public int getSampleCount() {
		return sampleCount;
	}

	protected void beforeStartRecording() {
		try {
			collectResourceData();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void afterStopRecording() {
		try {
			collectResourceData();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private void collectResourceData() throws Exception {
		if(!threadCpuTimeEnabled && !threadAllocatedMemoryEnabled) {
			return;
		}
		ObjectName objectName = new ObjectName("java.lang:type=Threading");
		long[] threadCpus = null;
		long[] threadMems = null;
		if(threadCpuTimeEnabled) {
			threadCpus = (long[])JMXUtil.invokeAttributeOrOperation(mbeanConnection, objectName, "getThreadCpuTime", new String[]{threadIdsString});
		}
		if(threadAllocatedMemoryEnabled) {
			threadMems = (long[])JMXUtil.invokeAttributeOrOperation(mbeanConnection, objectName, "getThreadAllocatedBytes", new String[]{threadIdsString});
		}
		for(int i = 0; i < threadIds.length; i++) {
			Long tid = Long.parseLong(threadIds[i]);
			ResourceUsage usage = resourceData.get(tid);
			if(usage == null) {
				usage = new ResourceUsage(threadNames[i], threadCpus[i], threadMems[i]);
			} else {
				usage.update(threadCpus[i], threadMems[i]);
			}
			resourceData.put(tid, usage);
		}
	}
	
	protected long collectData() throws Exception {
		Object resultData = null;
		long start = System.currentTimeMillis();
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
		result.resourceData = resourceData;
		return result;
	}

}
