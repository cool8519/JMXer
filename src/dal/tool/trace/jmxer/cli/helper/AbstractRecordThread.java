package dal.tool.trace.jmxer.cli.helper;

import java.lang.management.ThreadInfo;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import dal.tool.cli.Logger;
import dal.tool.trace.jmxer.cli.command.JmxThreadCommand;
import dal.tool.util.StringUtil;
import dal.tool.util.jmx.JMXUtil;

/**
 * RECORD 스택·리소스 샘플링 스레드 공통 베이스
 *
 */
public abstract class AbstractRecordThread implements Runnable {

	protected static final String UNKNOWN_THREAD_NAME = "<unknown>";

	protected volatile boolean stop = false;
	private final Object stopLock = new Object();
	private boolean stopHandled = false;
	private boolean stopCompleted = false;
	protected String recordThreadName;
	protected MBeanServerConnection mbeanConnection = null;
	protected long recordLimitMS = 0L;
	protected long recordIntervalMS = 1000L;
	protected String[] threadIds = null;
	protected String threadIdsString = null;
	protected String[] threadNames = null;
	protected String threadExpression = null;
	protected boolean dynamicTarget = false;
	protected boolean allThreadsTarget = false;
	protected Set<Long> initialAllThreadIds = Collections.emptySet();
	protected long startTimeMS = 0L;
	protected long endTimeMS = 0L;
	protected Thread recordRunnerThread = null;
	private boolean tuiSampleProgressLineStarted;
	private static volatile AbstractRecordThread activeRecording;

	public AbstractRecordThread(String recordThreadName, MBeanServerConnection mbeanConnection, String[] threadIds) throws Exception {
		this(recordThreadName, mbeanConnection, threadIds,
			(threadIds != null && threadIds.length == 1 && "*".equals(threadIds[0])) ? "*" : null);
	}

	/**
	 * ThreadList 원본 표현식을 함께 전달받아 고정 ID 대상인지 동적 재탐색 대상인지 구분하여 초기화한다.
	 * @param recordThreadName 샘플링 스레드 이름
	 * @param mbeanConnection 대상 JVM MBean 연결
	 * @param threadIds 초기 해석된 스레드 ID 배열
	 * @param threadExpression 원본 ThreadList 표현식
	 * @throws Exception 초기화 중 JMX 조회 실패 시
	 */
	public AbstractRecordThread(String recordThreadName, MBeanServerConnection mbeanConnection, String[] threadIds, String threadExpression) throws Exception {
		this.recordThreadName = recordThreadName;
		this.mbeanConnection = mbeanConnection;
		this.threadIds = threadIds;
		this.threadExpression = (threadExpression == null) ? null : threadExpression.trim();
		this.allThreadsTarget = "*".equals(this.threadExpression)
			|| (threadIds != null && threadIds.length == 1 && "*".equals(threadIds[0]));
		this.dynamicTarget = this.allThreadsTarget || JmxThreadCommand.isDynamicThreadExpression(this.threadExpression);
		init();
	}

	protected void init() throws Exception {
		if(dynamicTarget) {
			if(allThreadsTarget) {
				threadIds = new String[] { "*" };
				threadIdsString = "*";
				threadNames = new String[0];
			} else {
				if(threadIds == null) {
					threadIds = new String[0];
				}
				threadIdsString = StringUtil.arrayToString(threadIds, ",");
				threadNames = new String[0];
			}
			afterInit();
			return;
		}
		ObjectName objectName = new ObjectName("java.lang:type=Threading");
		if(threadIds == null) {
			long[] ids = JMXUtil.getAllThreadIds(mbeanConnection, true);
			threadIds = new String[ids.length];
			for(int i = 0; i < ids.length; i++) {
				threadIds[i] = String.valueOf(ids[i]);
			}
		}
		if(threadIds.length < 1) {
			throw new Exception("Could not get thread info.");
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

	/**
	 * 레코딩 시작 시점에 살아 있는 전체 스레드 ID 집합을 스냅샷으로 저장한다.
	 * @throws Exception JMX 조회 실패 시
	 */
	protected void captureInitialAllThreadIds() throws Exception {
		long[] allIds = JMXUtil.getAllThreadIds(mbeanConnection, false);
		Set<Long> idSet = new HashSet<Long>();
		if(allIds != null) {
			for(int i = 0; i < allIds.length; i++) {
				idSet.add(Long.valueOf(allIds[i]));
			}
		}
		this.initialAllThreadIds = idSet;
	}

	/**
	 * 현재 시점에 수집 대상이 되는 스레드(ID 오름차순, ID -> 스레드명)를 계산한다.
	 * @return 대상 스레드 ID와 이름 매핑
	 * @throws Exception JMX 조회 또는 표현식 평가 실패 시
	 */
	protected Map<Long, String> resolveCurrentTargetThreads() throws Exception {
		Map<Long, String> resolved = new LinkedHashMap<Long, String>();
		if(allThreadsTarget) {
			Map<Long, ThreadInfo> infoMap = JMXUtil.getAllThreadInfo(mbeanConnection);
			for(Long tid : new TreeSet<Long>(infoMap.keySet())) {
				ThreadInfo info = infoMap.get(tid);
				resolved.put(tid, info == null ? UNKNOWN_THREAD_NAME : normalizeThreadName(info.getThreadName()));
			}
			return resolved;
		}
		if(dynamicTarget) {
			Map<Long, ThreadInfo> infoMap = JMXUtil.getAllThreadInfo(mbeanConnection);
			Map<Long, String> nameMap = new HashMap<Long, String>();
			for(Map.Entry<Long, ThreadInfo> entry : infoMap.entrySet()) {
				ThreadInfo info = entry.getValue();
				nameMap.put(entry.getKey(), info == null ? null : info.getThreadName());
			}
			Long[] matchedIds = JmxThreadCommand.getTargetThreadIds(nameMap, threadExpression);
			if(matchedIds != null) {
				for(int i = 0; i < matchedIds.length; i++) {
					Long tid = matchedIds[i];
					if(tid != null) {
						resolved.put(tid, normalizeThreadName(nameMap.get(tid)));
					}
				}
			}
			return resolved;
		}
		if(threadIds != null) {
			for(int i = 0; i < threadIds.length; i++) {
				Long tid = Long.valueOf(Long.parseLong(threadIds[i]));
				String name = (threadNames != null && i < threadNames.length) ? threadNames[i] : UNKNOWN_THREAD_NAME;
				resolved.put(tid, normalizeThreadName(name));
			}
		}
		return resolved;
	}

	protected static String joinThreadIds(List<Long> tids) {
		if(tids == null || tids.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < tids.size(); i++) {
			Long tid = tids.get(i);
			if(tid == null) {
				continue;
			}
			if(sb.length() > 0) {
				sb.append(',');
			}
			sb.append(tid.longValue());
		}
		return sb.toString();
	}

	protected long[] fetchThreadCpuTimes(List<Long> tids) throws Exception {
		if(tids == null || tids.isEmpty()) {
			return new long[0];
		}
		String idsArg = joinThreadIds(tids);
		if(idsArg.length() == 0) {
			return new long[0];
		}
		ObjectName objectName = new ObjectName("java.lang:type=Threading");
		Object res = JMXUtil.invokeAttributeOrOperation(
			mbeanConnection, objectName, "getThreadCpuTime(long[])", new String[]{idsArg});
		if(res instanceof long[]) {
			return (long[])res;
		}
		if(res instanceof Long && tids.size() == 1) {
			return new long[]{((Long)res).longValue()};
		}
		return null;
	}

	protected long[] fetchThreadAllocatedBytes(List<Long> tids) throws Exception {
		if(tids == null || tids.isEmpty()) {
			return new long[0];
		}
		String idsArg = joinThreadIds(tids);
		if(idsArg.length() == 0) {
			return new long[0];
		}
		ObjectName objectName = new ObjectName("java.lang:type=Threading");
		Object res = JMXUtil.invokeAttributeOrOperation(
			mbeanConnection, objectName, "getThreadAllocatedBytes(long[])", new String[]{idsArg});
		if(res instanceof long[]) {
			return (long[])res;
		}
		if(res instanceof Long && tids.size() == 1) {
			return new long[]{((Long)res).longValue()};
		}
		return null;
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
			if(recordRunnerThread != null && recordRunnerThread != Thread.currentThread()) {
				try {
					recordRunnerThread.join(5000L);
				} catch(InterruptedException ie) {
					Thread.currentThread().interrupt();
				}
			}
			awaitStopCompleted();
		}
		afterStartRecording();
	}

	public void stopRecording() {
		synchronized(stopLock) {
			if(stopHandled) {
				return;
			}
			stopHandled = true;
		}
		beforeStopRecording();
		endTimeMS = System.currentTimeMillis();
		this.stop = true;
		if(recordRunnerThread != null && recordRunnerThread != Thread.currentThread()) {
			recordRunnerThread.interrupt();
			try {
				recordRunnerThread.join(5000L);
			} catch(InterruptedException ie) {
				Thread.currentThread().interrupt();
			}
		}
		try {
			afterStopRecording();
		} finally {
			synchronized(stopLock) {
				stopCompleted = true;
				stopLock.notifyAll();
			}
		}
	}

	private void awaitStopCompleted() {
		synchronized(stopLock) {
			while(!stopCompleted) {
				try {
					stopLock.wait(200L);
				} catch(InterruptedException ie) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		}
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
