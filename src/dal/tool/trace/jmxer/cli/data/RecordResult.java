package dal.tool.trace.jmxer.cli.data;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dal.tool.cli.Logger;
import dal.tool.cli.Logger.Level;
import dal.tool.trace.jmxer.JMXerConstant;
import dal.tool.trace.jmxer.cli.JmxSettings.DumpStackFilter;
import dal.tool.trace.jmxer.cli.helper.RecordResultViewer;
import dal.tool.util.FileUtil;
import dal.tool.util.StringUtil;

/**
 * RECORD 수집 결과 전체(dump 파일 직렬화 대상)
 *
 */
public class RecordResult implements Serializable {

	private static final long serialVersionUID = -6154997163441944090L;
	
	public String dmpFilePath = null;
	public long startTime = 0L;
	public long endTime = 0L;
	public long recordLimitMS = 0L;
	public long recordIntervalMS = 0L;
	public int sampleCount = 0;
	public long[] sampleStartTimes;
	public long[] sampleEndTimes;
	public Map<String,String> toolInfo;
	public Map<String,String> vmInfo;
	public Map<Long,List<RecordThreadInfo>> recordData;
	public Map<Long,ResourceUsage> resourceData;


	public RecordResult() {}
	
	public void printInfo(DumpStackFilter filter, boolean includeThreadList) {
		Logger.logln(RecordResultViewer.formatInfoString(this, filter, includeThreadList));
	}

	public void printResult(List<String> viewArgs, DumpStackFilter filter, boolean showEmptyThread, String reqEntryFile) {
		if(viewArgs != null && !viewArgs.isEmpty()) {
			String type = StringUtil.stripQuote(viewArgs.get(0), new char[] { '"', '\'' }, true);
			if(type.equalsIgnoreCase("info")) {
				if(viewArgs.size() < 1) {
					Logger.logln("Need more argument.");
					return;
				}
				if(viewArgs.size() > 1) {
					Logger.logln("Too many arguments.");
					return;
				}
				printInfo(filter, true);
				return;
			}
		}
		RecordResultViewer viewer = new RecordResultViewer(this, filter, showEmptyThread, reqEntryFile);
		viewer.printResult(viewArgs);
	}

	public boolean saveToFile(File f) {
		try {
			FileUtil.createFileDirectory(f);
			List<Object> writeData = new ArrayList<Object>();
			writeData.add(JMXerConstant.DUMP_FILE_VALIDATION_HEADER);
			writeData.add(this);
			FileUtil.writeObjectFile(f, writeData);
			this.dmpFilePath = f.getCanonicalPath();
			return true;
		} catch(Exception e) {
			Logger.logln(Level.ERROR, "Failed to save the record result to the file : " + e.getMessage());
		}
		return false;
	}

	public static RecordResult loadFromFile(File f) {
		try {
			List<Object> loadData = FileUtil.readObjectFile(f);
			if(loadData.size() != 2 || loadData.get(0).getClass() != String.class || loadData.get(1).getClass() != RecordResult.class || 
			   ((String)loadData.get(0)).equals(JMXerConstant.DUMP_FILE_VALIDATION_HEADER) == false) {
				throw new Exception("Invalid Setting File.");
			}
			RecordResult result = (RecordResult)loadData.get(1);
			result.dmpFilePath = f.getCanonicalPath();
			return result;			
		} catch(Exception e) {
			Logger.logln(Level.ERROR, "Failed to load record result from the file : " + e.getMessage());
			return null;
		}
	}

}
