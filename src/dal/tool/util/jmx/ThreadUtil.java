package dal.tool.util.jmx;

import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.List;


/**
 * 쓰레드 조작을 쉽게 하기 위한 Util 클래스
 * @author 권영달
 *
 */
public class ThreadUtil {
	
	private static final String INDENT = "    ";

	
	/**
	 * 쓰레드 별 덤프를 리턴한다.
	 * @param ti JMX의 ThreadInfo 객체
	 * @return 쓰레드 덤프 문자열 목록
	 */
	public static List<String> getThreadDump(ThreadInfo ti) {
		List<String> dump = new ArrayList<String>();
		StringBuilder sb = new StringBuilder("\"" + ti.getThreadName() + "\"" + " Id=" + ti.getThreadId() + " in " + ti.getThreadState());
		if(ti.getLockName() != null) {
			sb.append(" on lock=" + ti.getLockName());
		}
		if(ti.isSuspended()) {
			sb.append(" (suspended)");
		}
		if(ti.isInNative()) {
			sb.append(" (running in native)");
		}
		dump.add(sb.toString());

		if(ti.getLockOwnerName() != null) {
			dump.add(INDENT + " owned by " + ti.getLockOwnerName() + " Id=" + ti.getLockOwnerId());
		}
		
		StackTraceElement[] stacktrace = ti.getStackTrace();
		MonitorInfo[] monitors = ti.getLockedMonitors();
		for(int i = 0; i < stacktrace.length; i++) {
			StackTraceElement ste = stacktrace[i];
			dump.add(INDENT + "at " + ste.toString());
			for(MonitorInfo mi : monitors) {
				if(mi.getLockedStackDepth() == i) {
					dump.add(INDENT + "  - locked " + mi);
				}
			}
		}
		return dump;
	}

}
