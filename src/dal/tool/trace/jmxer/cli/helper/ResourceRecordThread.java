package dal.tool.trace.jmxer.cli.helper;

import java.lang.management.ThreadInfo;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import dal.tool.cli.Logger;
import dal.tool.trace.jmxer.cli.data.ResourceUsage;
import dal.tool.util.jmx.JMXUtil;

public class ResourceRecordThread extends AbstractRecordThread {

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
			String thrStr = "\"" + (usage.threadName.length()>38?(usage.threadName.substring(0,35)+"..."):usage.threadName) + "\"";
			long cpuStr = (usage.currCpu - usage.startCpu) / 1000000;
			long memStr = (usage.currMem - usage.startMem);
			if(usage.startCpu > usage.currCpu || usage.startMem > usage.currMem) {
				String newThreadStr = "";
				try {
					ObjectName objectName = new ObjectName("java.lang:type=Threading");
					Object resultData = JMXUtil.invokeAttributeOrOperation(mbeanConnection, objectName, "getThreadInfo(long[])", new String[]{threadIds[i]});
					if(resultData == null || ((CompositeData[])resultData).length < 1) {
						throw new Exception("Could not get thread info.");
					}
					CompositeData[] cd = (CompositeData[])resultData;
					ThreadInfo threadInfo = ThreadInfo.from(cd[0]);
					if(threadInfo != null) {
						newThreadStr = threadInfo.getThreadName();
					}
				} catch(Exception e) {
					e.printStackTrace();
				}
				Logger.logln(String.format(" - tid [%5d] %-40s : Changed thread to \"%s\"", Long.parseLong(threadIds[i]), thrStr, newThreadStr));
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

}
