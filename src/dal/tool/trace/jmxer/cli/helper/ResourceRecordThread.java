package dal.tool.trace.jmxer.cli.helper;

import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import dal.tool.cli.Logger;
import dal.tool.trace.jmxer.cli.data.ResourceUsage;
import dal.tool.util.jmx.JMXUtil;

/**
 * RECORD RESOURCE 샘플링 스레드(CPU·할당 메모리)
 *
 */
public class ResourceRecordThread extends AbstractRecordThread {

	private static final String TERMINATED_DURING_RECORDING = "Terminated during recording";
	private static final String TERMINATED_TID_REUSED_SUFFIX = " (tid reused by \"%s\")";

	protected Map<Long, ResourceUsage> recordData;
	protected boolean threadCpuTimeEnabled;
	protected boolean threadAllocatedMemoryEnabled;

	public ResourceRecordThread(MBeanServerConnection mbeanConnection, String[] threadIds) throws Exception {
		super("ResourceRecorder", mbeanConnection, threadIds);
	}

	public ResourceRecordThread(MBeanServerConnection mbeanConnection, String[] threadIds, String threadExpression) throws Exception {
		super("ResourceRecorder", mbeanConnection, threadIds, threadExpression);
	}

	protected void afterInit() throws Exception {
		recordData = new TreeMap<Long, ResourceUsage>();
		threadCpuTimeEnabled = (Boolean)JMXUtil.getJMXResult(mbeanConnection, "java.lang:type=Threading", "ThreadCpuTimeEnabled", null);
		threadAllocatedMemoryEnabled = (Boolean)JMXUtil.getJMXResult(mbeanConnection, "java.lang:type=Threading", "ThreadAllocatedMemoryEnabled", null);
		if(!threadCpuTimeEnabled && !threadAllocatedMemoryEnabled) {
			throw new Exception("Neither ThreadCpuTime nor ThreadAllocatedMemory are enabled.");
		}
	}

	protected void beforeStartRecording() {
		markActiveRecording();
		try {
			captureInitialAllThreadIds();
			startTimeMS = collectInitialResourceData();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	protected void afterStopRecording() {
		try {
			if(dynamicTarget) {
				discoverNewTargetThreads();
			}
			endTimeMS = collectFinalResourceData();
		} catch(Exception e) {
			e.printStackTrace();
		}
		clearActiveRecording();
	}

	protected long collectData() throws Exception {
		if(dynamicTarget) {
			discoverNewTargetThreads();
		}
		return System.currentTimeMillis();
	}

	private long collectInitialResourceData() throws Exception {
		Map<Long, String> initialTargets = resolveCurrentTargetThreads();
		registerQueriedBaselines(initialTargets);
		return System.currentTimeMillis();
	}

	private void discoverNewTargetThreads() throws Exception {
		Map<Long, String> currentTargets = resolveCurrentTargetThreads();
		if(currentTargets.isEmpty()) {
			return;
		}
		Map<Long, String> newlyMatchedPreExisting = null;
		for(Map.Entry<Long, String> entry : currentTargets.entrySet()) {
			Long tid = entry.getKey();
			if(tid == null || recordData.containsKey(tid)) {
				continue;
			}
			String tName = entry.getValue();
			if(!initialAllThreadIds.contains(tid)) {
				long startCpu = threadCpuTimeEnabled ? 0L : -1L;
				long startMem = threadAllocatedMemoryEnabled ? 0L : -1L;
				recordData.put(tid, new ResourceUsage(tName, startCpu, startMem));
			} else {
				if(newlyMatchedPreExisting == null) {
					newlyMatchedPreExisting = new LinkedHashMap<Long, String>();
				}
				newlyMatchedPreExisting.put(tid, tName);
			}
		}
		if(newlyMatchedPreExisting != null && !newlyMatchedPreExisting.isEmpty()) {
			registerQueriedBaselines(newlyMatchedPreExisting);
		}
	}

	private void registerQueriedBaselines(Map<Long, String> targetsById) throws Exception {
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
			recordData.put(tid, new ResourceUsage(targetsById.get(tid), cpu, mem));
		}
	}

	private long collectFinalResourceData() throws Exception {
		if(recordData.isEmpty()) {
			return System.currentTimeMillis();
		}
		List<Long> tids = new ArrayList<Long>(recordData.keySet());
		long[] threadCpus = threadCpuTimeEnabled ? fetchThreadCpuTimes(tids) : null;
		long[] threadMems = threadAllocatedMemoryEnabled ? fetchThreadAllocatedBytes(tids) : null;
		long end = System.currentTimeMillis();
		for(int i = 0; i < tids.size(); i++) {
			Long tid = tids.get(i);
			ResourceUsage usage = recordData.get(tid);
			if(usage != null) {
				long cpu = (threadCpuTimeEnabled && threadCpus != null && i < threadCpus.length) ? threadCpus[i] : -1L;
				long mem = (threadAllocatedMemoryEnabled && threadMems != null && i < threadMems.length) ? threadMems[i] : -1L;
				usage.update(cpu, mem);
			}
		}
		return end;
	}

	public void printResult() {
		if(endTimeMS-startTimeMS < recordIntervalMS || recordData == null || recordData.isEmpty()) {
			Logger.logln("No data collected.");
			return;
		}
		Logger.logln("Recorded for " + (endTimeMS-startTimeMS) + "ms");
		Logger.logln("");
		for(Map.Entry<Long, ResourceUsage> entry : recordData.entrySet()) {
			long tid = entry.getKey().longValue();
			ResourceUsage usage = entry.getValue();
			String thrStr = formatQuotedThreadName(usage.threadName);
			long cpuStr = (usage.currCpu - usage.startCpu) / 1000000;
			long memStr = (usage.currMem - usage.startMem);
			if(isThreadGoneDuringRecording(usage)) {
				Logger.logln(formatTerminatedDuringRecordingLine(usage, tid, thrStr));
				continue;
			}
			if(!threadCpuTimeEnabled) {
				Logger.logln(String.format(" - tid [%5d] %-40s : AllocatedBytes=%d", tid, thrStr, memStr));
			} else if(!threadAllocatedMemoryEnabled) {
				Logger.logln(String.format(" - tid [%5d] %-40s : CpuTime=%d", tid, thrStr, cpuStr));
			} else {
				Logger.logln(String.format(" - tid [%5d] %-40s : CpuTime=%-10d, AllocatedBytes=%d", tid, thrStr, cpuStr, memStr));
			}
		}
	}

	private boolean isThreadGoneDuringRecording(ResourceUsage usage) {
		boolean cpuGone = threadCpuTimeEnabled && isCounterRegressed(usage.startCpu, usage.currCpu);
		boolean memGone = threadAllocatedMemoryEnabled && isCounterRegressed(usage.startMem, usage.currMem);
		return cpuGone || memGone;
	}

	/** 종료·tid 재사용 시 카운터가 감소하거나 -1이 되면 해당 스레드 인스턴스가 사라진 것으로 본다. */
	private static boolean isCounterRegressed(long start, long current) {
		if(current < 0L) {
			return start >= 0L;
		}
		return start > current;
	}

	private String formatTerminatedDuringRecordingLine(ResourceUsage usage, long tid, String thrStr) {
		StringBuilder detail = new StringBuilder(TERMINATED_DURING_RECORDING);
		String reuseName = resolveCurrentThreadName(tid);
		if(reuseName != null && !reuseName.equals(normalizeThreadName(usage.threadName))) {
			detail.append(String.format(TERMINATED_TID_REUSED_SUFFIX, reuseName));
		}
		return String.format(" - tid [%5d] %-40s : %s", tid, thrStr, detail.toString());
	}

	private String resolveCurrentThreadName(long tid) {
		try {
			ObjectName objectName = new ObjectName("java.lang:type=Threading");
			Object resultData = JMXUtil.invokeAttributeOrOperation(
				mbeanConnection, objectName, "getThreadInfo(long[])", new String[] { String.valueOf(tid) });
			if(!(resultData instanceof CompositeData[])) {
				return null;
			}
			CompositeData[] compositeData = (CompositeData[]) resultData;
			if(compositeData.length < 1 || compositeData[0] == null) {
				return null;
			}
			ThreadInfo threadInfo = ThreadInfo.from(compositeData[0]);
			if(threadInfo == null) {
				return null;
			}
			String name = threadInfo.getThreadName();
			if(name == null || name.length() == 0) {
				return null;
			}
			return name;
		} catch(Exception e) {
			return null;
		}
	}

}
