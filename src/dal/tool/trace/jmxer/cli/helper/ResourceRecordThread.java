package dal.tool.trace.jmxer.cli.helper;

import java.lang.management.ThreadInfo;

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

	private static final long INVALID_THREAD_COUNTER = -1L;
	private static final String TERMINATED_DURING_RECORDING = "Terminated during recording";
	private static final String TERMINATED_TID_REUSED_SUFFIX = " (tid reused by \"%s\")";

	protected ResourceUsage[] recordData;
	protected boolean threadCpuTimeEnabled;
	protected boolean threadAllocatedMemoryEnabled;

	public ResourceRecordThread(MBeanServerConnection mbeanConnection, String[] threadIds) throws Exception {
		super("ResourceRecorder", mbeanConnection, threadIds);
	}

	protected void afterInit() throws Exception {
		recordData = new ResourceUsage[threadIds.length];
		threadCpuTimeEnabled = (Boolean)JMXUtil.getJMXResult(mbeanConnection, "java.lang:type=Threading", "ThreadCpuTimeEnabled", null);
		threadAllocatedMemoryEnabled = (Boolean)JMXUtil.getJMXResult(mbeanConnection, "java.lang:type=Threading", "ThreadAllocatedMemoryEnabled", null);
		if(!threadCpuTimeEnabled && !threadAllocatedMemoryEnabled) {
			throw new Exception("Neither ThreadCpuTime nor ThreadAllocatedMemory are enabled.");
			
		}
	}

	protected void beforeStartRecording() {
		markActiveRecording();
		try {
			startTimeMS = collectResourceData();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void afterStopRecording() {
		try {
			endTimeMS = collectResourceData();
		} catch(Exception e) {
			e.printStackTrace();
		}
		clearActiveRecording();
	}
	
	protected long collectData() throws Exception {
		return System.currentTimeMillis();
	}
	
	private long collectResourceData() throws Exception {
		ObjectName objectName = new ObjectName("java.lang:type=Threading");
		long[] threadCpus = null;
		long[] threadMems = null;
		if(threadCpuTimeEnabled) {
			threadCpus = (long[])JMXUtil.invokeAttributeOrOperation(mbeanConnection, objectName, "getThreadCpuTime", new String[]{threadIdsString});
		}
		if(threadAllocatedMemoryEnabled) {
			threadMems = (long[])JMXUtil.invokeAttributeOrOperation(mbeanConnection, objectName, "getThreadAllocatedBytes", new String[]{threadIdsString});
		}
		long end = System.currentTimeMillis();
		for(int i = 0; i < threadIds.length; i++) {
			ResourceUsage usage = recordData[i];
			if(usage == null) {
				usage = new ResourceUsage(threadNames[i], threadCpus[i], threadMems[i]);
			} else {
				usage.update(threadCpus[i], threadMems[i]);
			}
			recordData[i] = usage;
		}
		return end;
	}

	public void printResult() {
		if(endTimeMS-startTimeMS < recordIntervalMS) {
			Logger.logln("No data collected.");
			return;
		}
		Logger.logln("Recorded for " + (endTimeMS-startTimeMS) + "ms");
		Logger.logln("");
		for(int i = 0; i < recordData.length; i++) {
			ResourceUsage usage = recordData[i];
			String thrStr = formatQuotedThreadName(usage.threadName);
			long cpuStr = (usage.currCpu - usage.startCpu) / 1000000;
			long memStr = (usage.currMem - usage.startMem);
			if(isThreadGoneDuringRecording(usage)) {
				Logger.logln(formatTerminatedDuringRecordingLine(usage, i, thrStr));
				continue;
			}
			if(!threadCpuTimeEnabled) {
				Logger.logln(String.format(" - tid [%5d] %-40s : AllocatedBytes=%d", Long.parseLong(threadIds[i]), thrStr, memStr));
			} else if(!threadAllocatedMemoryEnabled) {
				Logger.logln(String.format(" - tid [%5d] %-40s : CpuTime=%d", Long.parseLong(threadIds[i]), thrStr, cpuStr));
			} else {
				Logger.logln(String.format(" - tid [%5d] %-40s : CpuTime=%-10d, AllocatedBytes=%d", Long.parseLong(threadIds[i]), thrStr, cpuStr, memStr));
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

	private String formatTerminatedDuringRecordingLine(ResourceUsage usage, int index, String thrStr) {
		StringBuilder detail = new StringBuilder(TERMINATED_DURING_RECORDING);
		String reuseName = resolveCurrentThreadName(index);
		if(reuseName != null && !reuseName.equals(normalizeThreadName(usage.threadName))) {
			detail.append(String.format(TERMINATED_TID_REUSED_SUFFIX, reuseName));
		}
		return String.format(" - tid [%5d] %-40s : %s", Long.parseLong(threadIds[index]), thrStr, detail.toString());
	}

	private String resolveCurrentThreadName(int index) {
		try {
			ObjectName objectName = new ObjectName("java.lang:type=Threading");
			Object resultData = JMXUtil.invokeAttributeOrOperation(
				mbeanConnection, objectName, "getThreadInfo(long[])", new String[] { threadIds[index] });
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
