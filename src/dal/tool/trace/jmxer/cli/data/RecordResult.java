package dal.tool.trace.jmxer.cli.data;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dal.tool.cli.Logger;
import dal.tool.cli.Logger.Level;
import dal.tool.trace.jmxer.JMXerConstant;
import dal.tool.trace.jmxer.cli.helper.RecordResultViewer;
import dal.tool.util.FileUtil;

public class RecordResult implements Serializable {

	private static final long serialVersionUID = -6154997163441944090L;
	
	public String dmpFilePath = null;
	public long startTime = 0L;
	public long endTime = 0L;
	public long recordLimitMS = 0L;
	public long recordIntervalMS = 0L;
	public int sampleCount = 0;
	public Map<String,String> toolInfo;
	public Map<String,String> vmInfo;
	public Map<Long,List<RecordThreadInfo>> recordData;
	public Map<Long,ResourceUsage> resourceData;


	public RecordResult() {}
	
	public void printResult(List<String> viewArgs) {
		RecordResultViewer viewer = new RecordResultViewer(this);
		viewer.printResult(viewArgs);			
	}

	public boolean saveToFile(File f) {
		try {
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
