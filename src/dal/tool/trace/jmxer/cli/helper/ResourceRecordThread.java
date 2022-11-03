package dal.tool.trace.jmxer.cli.helper;

import java.lang.management.ThreadInfo;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import dal.tool.cli.Logger;
import dal.tool.util.StringUtil;
import dal.tool.util.jmx.JMXUtil;

public class ResourceRecordThread implements Runnable {

	private boolean stop = false;
	private MBeanServerConnection mbeanConnection = null;
	private long recordLimitMS = 0L;
	private long recordIntervalMS = 1000L;
	private String[] threadIds = null;
	private String threadIdsString = null;
	private String[] threadNames = null;
	private ResourceUsage[] recordData = null;
	private long startTimeMS = 0L;
	private long endTimeMS = 0L;
	private boolean threadCpuTimeEnabled;
	private boolean threadAllocatedMemoryEnabled;

	private class ResourceUsage {
		String threadName;
		long startCpu;
		long startMem;
		long currCpu;
		long currMem;
		public ResourceUsage(String threadName, long cpu, long mem) {
			this.threadName = threadName;
			this.startCpu = cpu;
			this.startMem = mem;
		}
		public void update(long cpu, long mem) {
			this.currCpu = cpu;
			this.currMem = mem;
		}
	}
	
	public ResourceRecordThread(MBeanServerConnection mbeanConnection, String[] threadIds) throws Exception {
		this.mbeanConnection = mbeanConnection;
	    this.threadIds = threadIds;
	    init();
	}

	private void init() throws Exception {
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
		recordData = new ResourceUsage[threadIds.length];
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
		threadCpuTimeEnabled = (Boolean)JMXUtil.getJMXResult(mbeanConnection, "java.lang:type=Threading", "ThreadCpuTimeEnabled", null);
		threadAllocatedMemoryEnabled = (Boolean)JMXUtil.getJMXResult(mbeanConnection, "java.lang:type=Threading", "ThreadAllocatedMemoryEnabled", null);
	}

	public void startRecording(long interval, long limit) {
	    this.recordIntervalMS = interval;
		this.recordLimitMS = limit;
		if(threadCpuTimeEnabled || threadAllocatedMemoryEnabled) {
			Thread t = new Thread(this, "ResourceRecorder");
			t.start();
			if(recordLimitMS > 0) {
				while(!stop) {
					try {
						Thread.sleep(1000L);
					} catch(InterruptedException e) {				
					}
				}
			}
		} else {
			Logger.logln("Neither ThreadCpuTime nor ThreadAllocatedMemory are enabled.");
		}
	}
	
	public void stopRecording() {
		try {
			endTimeMS = collectData();
		} catch(Exception e) {
			e.printStackTrace();
		}
		this.stop = true;
	}
	
	public void run() {
		try {
			startTimeMS = collectData();
		} catch(Exception e) {
			e.printStackTrace();
		}
		while(!stop) {
			System.out.print(".");
			long total_elapsed = System.currentTimeMillis() - startTimeMS;
			if(recordLimitMS > 0 && total_elapsed >= recordLimitMS) {
				stopRecording(); break;
			}
			try {
				Thread.sleep(recordIntervalMS);
			} catch(InterruptedException e) {				
			}
		}
		System.out.println();
	}

	private long collectData() throws Exception {
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
					newThreadStr = threadInfo.getThreadName();
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
