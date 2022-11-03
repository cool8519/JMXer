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

	public String toMethodString(String name, boolean root) {
		int countDigit = String.valueOf(totalCount).length();
		int timeDigit = String.valueOf(totalRecordTime).length();
		if(name == null) {
			// title
			return String.format("[%8s](%"+(2*countDigit+1)+"s)(%"+(2*timeDigit+1)+"s) [%s]   %s", "Ratio", "Count", "Time_ms", "ThrC", "Class/Method");
		} else {
			String pctCountStr = NumberUtil.numberToString((double)hitCount/totalCount*100, "0.000'%'"); 
			int thrCountDigit = 4;
			if(root) {
				// root data
				return String.format("[%8s](%"+countDigit+"d/%d)(%"+timeDigit+"d/%d) [%"+thrCountDigit+"d]   %s", pctCountStr, hitCount, totalCount, sampleRecordTime, totalRecordTime, threadSet.size(), name);
			} else {
				// line data
				String stackStr = stackTraceElement.toString();
				String lineStr = ". " + stackStr.substring(stackStr.indexOf('(')+1, stackStr.indexOf(')'));
				String threadListStr = "ThreadList=" + StringUtil.arrayToString(threadSet.toArray(), ",");
				return String.format("[%8s](%"+countDigit+"d/%d)(%"+timeDigit+"d/%d)           %-30s (%s)", pctCountStr, hitCount, totalCount, sampleRecordTime, totalRecordTime, lineStr, threadListStr);
			}
		}
	}

	public String toThreadString(int depth, int maxDepth) {
		String pctCountStr = NumberUtil.numberToString((double)hitCount/totalCount*100, "0.000'%'"); 
		String indent = StringUtil.getRepeatString("  ", depth);
		int countDigit = String.valueOf(totalCount).length();
		int timeDigit = String.valueOf(totalRecordTime).length();
		int depthDigit = String.valueOf(maxDepth).length();
		if(depth < 0) {
			// title
			return String.format("[%8s](%"+(2*countDigit+1)+"s)(%"+(2*timeDigit+1)+"s) [%s]%"+depthDigit+"s%s", "Ratio", "Count", "Time_ms", "Depth", " ", "Class/Method");
		} else {
			// data
			String stackNameStr = stackTraceElement.toString();
			return String.format("[%8s](%"+countDigit+"d/%d)(%"+timeDigit+"d/%d) [%"+depthDigit+"d]%s   %s", pctCountStr, hitCount, totalCount, sampleRecordTime, totalRecordTime, depth, indent, stackNameStr);
		}
	}
	
}
