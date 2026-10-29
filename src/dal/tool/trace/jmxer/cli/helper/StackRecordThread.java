package dal.tool.trace.jmxer.cli.helper;

import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.openmbean.CompositeData;

import dal.tool.cli.Logger;
import dal.tool.trace.jmxer.cli.data.RecordResult;
import dal.tool.trace.jmxer.cli.data.RecordThreadInfo;
import dal.tool.trace.jmxer.cli.data.ResourceUsage;
import dal.tool.util.jmx.JMXUtil;

/**
 * RECORD STACKTRACE 샘플링 스레드
 *
 */
public class StackRecordThread extends AbstractRecordThread {

	protected HashMap<Long,List<RecordThreadInfo>> recordData;
	protected int sampleCount = 0;
	protected HashMap<Long,ResourceUsage> resourceData;
	protected List<Long> sampleStartTimeList;
	protected List<Long> sampleEndTimeList;
	protected boolean threadCpuTimeEnabled;
	protected boolean threadAllocatedMemoryEnabled;

	public StackRecordThread(MBeanServerConnection mbeanConnection, String[] threadIds) throws Exception {
		super("StackRecorder", mbeanConnection, threadIds);
	}

	public StackRecordThread(MBeanServerConnection mbeanConnection, String[] threadIds, String threadExpression) throws Exception {
		super("StackRecorder", mbeanConnection, threadIds, threadExpression);
	}

	protected void afterInit() throws Exception {
		recordData = new HashMap<Long,List<RecordThreadInfo>>();
		resourceData = new HashMap<Long,ResourceUsage>();
		sampleStartTimeList = new ArrayList<Long>();
		sampleEndTimeList = new ArrayList<Long>();
		threadCpuTimeEnabled = (Boolean)JMXUtil.getJMXResult(mbeanConnection, "java.lang:type=Threading", "ThreadCpuTimeEnabled", null);
		threadAllocatedMemoryEnabled = (Boolean)JMXUtil.getJMXResult(mbeanConnection, "java.lang:type=Threading", "ThreadAllocatedMemoryEnabled", null);
	}

	public int getSampleCount() {
		return sampleCount;
	}

	public boolean hasCollectedData() {
		return sampleCount > 0 && recordData != null && !recordData.isEmpty();
	}

	public static int getActiveSampleCount() {
		AbstractRecordThread active = getActiveRecording();
		if(!(active instanceof StackRecordThread)) {
			return -1;
		}
		return ((StackRecordThread) active).getSampleCount();
	}

	protected void beforeStartRecording() {
		markActiveRecording();
		try {
			captureInitialAllThreadIds();
			collectInitialResourceData();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	protected void afterStopRecording() {
		try {
			collectFinalResourceData();
		} catch(Exception e) {
			e.printStackTrace();
		}
		clearActiveRecording();
	}

	private void collectInitialResourceData() throws Exception {
		if(!threadCpuTimeEnabled && !threadAllocatedMemoryEnabled) {
			return;
		}
		Map<Long, String> initialTargets = resolveCurrentTargetThreads();
		if(initialTargets.isEmpty()) {
			return;
		}
		registerResourceBaselines(initialTargets);
	}

	private void registerResourceBaselines(Map<Long, String> targetsById) throws Exception {
		if(targetsById == null || targetsById.isEmpty()) {
			return;
		}
		List<Long> tids = new ArrayList<Long>(targetsById.keySet());
		long[] threadCpus = threadCpuTimeEnabled ? fetchThreadCpuTimes(tids) : null;
		long[] threadMems = threadAllocatedMemoryEnabled ? fetchThreadAllocatedBytes(tids) : null;
		for(int i = 0; i < tids.size(); i++) {
			Long tid = tids.get(i);
			long cpu = (threadCpuTimeEnabled && threadCpus != null && i < threadCpus.length) ? threadCpus[i] : -1L;
			long mem = (threadAllocatedMemoryEnabled && threadMems != null && i < threadMems.length) ? threadMems[i] : -1L;
			resourceData.put(tid, new ResourceUsage(targetsById.get(tid), cpu, mem));
		}
	}

	private void collectFinalResourceData() throws Exception {
		if((!threadCpuTimeEnabled && !threadAllocatedMemoryEnabled) || resourceData.isEmpty()) {
			return;
		}
		List<Long> tids = new ArrayList<Long>(resourceData.keySet());
		long[] threadCpus = threadCpuTimeEnabled ? fetchThreadCpuTimes(tids) : null;
		long[] threadMems = threadAllocatedMemoryEnabled ? fetchThreadAllocatedBytes(tids) : null;
		for(int i = 0; i < tids.size(); i++) {
			Long tid = tids.get(i);
			ResourceUsage usage = resourceData.get(tid);
			if(usage != null) {
				long cpu = (threadCpuTimeEnabled && threadCpus != null && i < threadCpus.length) ? threadCpus[i] : -1L;
				long mem = (threadAllocatedMemoryEnabled && threadMems != null && i < threadMems.length) ? threadMems[i] : -1L;
				usage.update(cpu, mem);
			}
		}
	}

	protected long collectData() throws Exception {
		CompositeData[] resultDataArr = null;
		long start = System.currentTimeMillis();
		if(allThreadsTarget) {
			Object resultData = JMXUtil.getJMXResult(mbeanConnection, "java.lang:type=Threading", "dumpAllThreads", new String[]{"false","false"});
			if(resultData == null) {
				throw new Exception("Could not get thread info.");
			}
			resultDataArr = (CompositeData[])resultData;
			if(resultDataArr == null || resultDataArr.length < 1) {
				throw new Exception("Could not get thread info.");
			}
		} else if(dynamicTarget) {
			Map<Long, String> currentTargets = resolveCurrentTargetThreads();
			if(currentTargets.isEmpty()) {
				resultDataArr = new CompositeData[0];
			} else {
				String currentIdsStr = joinThreadIds(new ArrayList<Long>(currentTargets.keySet()));
				String maxDepthString = String.valueOf(Integer.MAX_VALUE);
				Object resultData = JMXUtil.getJMXResult(mbeanConnection, "java.lang:type=Threading", "getThreadInfo(long[],int)", new String[]{currentIdsStr,maxDepthString});
				if(resultData == null) {
					throw new Exception("Could not get thread info.");
				}
				resultDataArr = (CompositeData[])resultData;
			}
		} else {
			String maxDepthString = String.valueOf(Integer.MAX_VALUE);
			Object resultData = JMXUtil.getJMXResult(mbeanConnection, "java.lang:type=Threading", "getThreadInfo(long[],int)", new String[]{threadIdsString,maxDepthString});
			if(resultData == null) {
				throw new Exception("Could not get thread info.");
			}
			resultDataArr = (CompositeData[])resultData;
			if(resultDataArr == null || resultDataArr.length < 1) {
				throw new Exception("Could not get thread info.");
			}
		}
		long end = System.currentTimeMillis();
		Map<Long, String> newlyMatchedPreExisting = null;
		if(resultDataArr != null) {
			for(CompositeData cd : resultDataArr) {
				if(cd == null) {
					continue;
				}
				ThreadInfo threadInfo = ThreadInfo.from(cd);
				if(threadInfo == null) {
					continue;
				}
				Long tid = Long.valueOf(threadInfo.getThreadId());
				List<RecordThreadInfo> threadInfoList = recordData.get(tid);
				if(threadInfoList == null) {
					threadInfoList = new ArrayList<RecordThreadInfo>();
				}
				RecordThreadInfo recThrInfo = toRecordThreadInfo(threadInfo);
				recThrInfo.recordStartTime = start;
				recThrInfo.recordEndTime = end;
				threadInfoList.add(recThrInfo);
				recordData.put(tid, threadInfoList);
				if((threadCpuTimeEnabled || threadAllocatedMemoryEnabled) && !resourceData.containsKey(tid)) {
					String tName = normalizeThreadName(threadInfo.getThreadName());
					if(!initialAllThreadIds.contains(tid)) {
						long startCpu = threadCpuTimeEnabled ? 0L : -1L;
						long startMem = threadAllocatedMemoryEnabled ? 0L : -1L;
						resourceData.put(tid, new ResourceUsage(tName, startCpu, startMem));
					} else {
						if(newlyMatchedPreExisting == null) {
							newlyMatchedPreExisting = new LinkedHashMap<Long, String>();
						}
						newlyMatchedPreExisting.put(tid, tName);
					}
				}
			}
		}
		if(newlyMatchedPreExisting != null && !newlyMatchedPreExisting.isEmpty()) {
			registerResourceBaselines(newlyMatchedPreExisting);
		}
		sampleStartTimeList.add(Long.valueOf(start));
		sampleEndTimeList.add(Long.valueOf(end));
		sampleCount++;
		return end;
	}

	public void printResult() {
		if(!hasCollectedData()) {
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
		result.sampleStartTimes = toLongArray(this.sampleStartTimeList);
		result.sampleEndTimes = toLongArray(this.sampleEndTimeList);
		result.recordData = recordData;
		result.resourceData = resourceData;
		return result;
	}

	private static long[] toLongArray(List<Long> values) {
		if(values == null) {
			return new long[0];
		}
		long[] arr = new long[values.size()];
		for(int i = 0; i < values.size(); i++) {
			Long v = values.get(i);
			arr[i] = (v == null) ? 0L : v.longValue();
		}
		return arr;
	}

}
