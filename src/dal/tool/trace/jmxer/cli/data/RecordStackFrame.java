package dal.tool.trace.jmxer.cli.data;

import java.util.HashSet;

import dal.tool.util.NumberUtil;
import dal.tool.util.StringUtil;

public class RecordStackFrame {

	public StackTraceElement stackTraceElement;
	public int totalCount = -1;
	public long totalRecordTime = -1;
	
	public int hitCount;
	public long realRecordTime;
	public long sampleRecordTime;
	public HashSet<Long> threadSet;

	
	public RecordStackFrame(StackTraceElement stackTraceElement) {
		this.stackTraceElement = stackTraceElement;
		this.hitCount = 0;
		this.realRecordTime = 0;
		this.sampleRecordTime = 0;
		this.threadSet = new HashSet<Long>();
	}

	public RecordStackFrame(StackTraceElement stackTraceElement, int totalCount, long totalRecordTime) {
		this.stackTraceElement = stackTraceElement;
		this.hitCount = 0;
		this.realRecordTime = 0;
		this.sampleRecordTime = 0;
		this.threadSet = new HashSet<Long>();
		setTotal(totalCount, totalRecordTime);
	}

	public void hit(long realTime, long sampleTime) {
		hitCount++;
		realRecordTime += realTime;
		sampleRecordTime += sampleTime;
	}
	
	public void setTotal(int totalCount, long totalRecordTime) {
		this.totalCount = totalCount;
		this.totalRecordTime = totalRecordTime;
	}

	public String toMethodHeaderString() {
		int countDigit = String.valueOf(totalCount).length();
		int timeDigit = String.valueOf(totalRecordTime).length();
		return String.format("[%8s](%"+(2*countDigit+1)+"s)(%"+(2*timeDigit+1)+"s) [%s]   %s", "Ratio", "Count", "Time_ms", "ThrC", "Class/Method");
	}

	public String toMethodRootString(String name) {
		String pctCountStr = NumberUtil.numberToString((double)hitCount/totalCount*100, "0.000'%'");
		int countDigit = String.valueOf(totalCount).length();
		int timeDigit = String.valueOf(totalRecordTime).length();
		int thrCountDigit = 4;
		return String.format("[%8s](%"+countDigit+"d/%d)(%"+timeDigit+"d/%d) [%"+thrCountDigit+"d]   %s", pctCountStr, hitCount, totalCount, sampleRecordTime, totalRecordTime, threadSet.size(), name);
	}

	public String toMethodLineString() {
		String pctCountStr = NumberUtil.numberToString((double)hitCount/totalCount*100, "0.000'%'");
		int countDigit = String.valueOf(totalCount).length();
		int timeDigit = String.valueOf(totalRecordTime).length();
		String stackStr = stackTraceElement.toString();
		String lineStr = stackStr.substring(stackStr.indexOf('(')+1, stackStr.indexOf(')'));
		String threadListStr = "ThreadList=" + StringUtil.arrayToString(threadSet.toArray(), ",");
		return String.format("      - line %-30s [%8s](%"+countDigit+"d/%d)(%"+timeDigit+"d/%d) (%s)", lineStr, pctCountStr, hitCount, totalCount, sampleRecordTime, totalRecordTime, threadListStr);
	}

	public String toMethodString(String name, boolean root) {
		if(name == null) {
			return toMethodHeaderString();
		} else {
			if(root) {
				return toMethodRootString(name);
			} else {
				return toMethodLineString();
			}
		}
	}

	public String toThreadHeaderString() {
		int countNumW = Math.max(1, Math.max(String.valueOf(hitCount).length(), String.valueOf(totalCount).length()));
		int timeNumW = Math.max(1, Math.max(String.valueOf(sampleRecordTime).length(), String.valueOf(totalRecordTime).length()));
		return toThreadHeaderString(countNumW, timeNumW);
	}

	/**
	 * THREAD 뷰 헤더. {@code countNumW}/{@code timeNumW}는 각각 (hit/total), (sample/total)에 쓰는 숫자 최소 자릿수(우측 정렬 공통 폭).
	 */
	public String toThreadHeaderString(int countNumW, int timeNumW) {
		int countCol = Math.max(5, 2 * countNumW + 1);
		int timeCol = Math.max(7, 2 * timeNumW + 1);
		return String.format("[%8s] (%"+countCol+"s) (%"+timeCol+"s)", "Ratio", "Count", "Time_ms");
	}

	public String toThreadBranchString() {
		int countNumW = Math.max(1, Math.max(String.valueOf(hitCount).length(), String.valueOf(totalCount).length()));
		int timeNumW = Math.max(1, Math.max(String.valueOf(sampleRecordTime).length(), String.valueOf(totalRecordTime).length()));
		return toThreadBranchString(countNumW, timeNumW);
	}

	public String toThreadBranchString(int countNumW, int timeNumW) {
		String pctCountStr = NumberUtil.numberToString((double)hitCount/totalCount*100, "0.000'%'");
		int countCol = Math.max(5, 2 * countNumW + 1);
		int timeCol = Math.max(7, 2 * timeNumW + 1);
		String countInner = String.format("%" + countCol + "s", hitCount + "/" + totalCount);
		String timeInner = String.format("%" + timeCol + "s", sampleRecordTime + "/" + totalRecordTime);
		return String.format("[%8s] (%" + countCol + "s) (%" + timeCol + "s)", pctCountStr, countInner, timeInner);
	}

	public String toThreadPathString() {
		return (stackTraceElement == null) ? "" : stackTraceElement.toString();
	}
	
}
