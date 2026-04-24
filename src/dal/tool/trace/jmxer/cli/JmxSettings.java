package dal.tool.trace.jmxer.cli;

import dal.tool.cli.Logger;
import dal.tool.cli.Settings;
import dal.tool.trace.jmxer.JMXControl;

public class JmxSettings extends Settings {

	public static enum RecordViewMode {
		NO_REQUEST_WAIT,
		FULL
	}

	protected String setOName = null;
	protected boolean prettyPrint = true;	
	protected RecordViewMode recordViewMode = RecordViewMode.NO_REQUEST_WAIT;
	protected boolean showEmptyThreadInRecordView = true;

	
	public String getMBeanObjectName() {
		return setOName;
	}

	public boolean setMBeanObjectName(String value) {
        if(value != null && (value.indexOf(':') < 0 || value.indexOf('=') < 0)) {
        	Logger.logln("Invalid MBean ObjectName Format \"" + value + "\"");
            return false;
        } else {
        	setOName = value;
        	return true;
        }
	}

	
	public boolean prettyPrintInResult() {
		return prettyPrint;
	}

	public boolean setPrettyPrintInResult(String value) {
        if(value == null || (!value.equalsIgnoreCase("on") && !value.equalsIgnoreCase("off"))) {
        	Logger.logln("The value must be set ON or OFF");
            return false;
        } else {
        	prettyPrint = (value.trim().equalsIgnoreCase("on"))?true:false;
        	return true;
        }
	}

	public RecordViewMode getRecordViewMode() {
		return recordViewMode;
	}

	public boolean setRecordViewMode(String value) {
		if(value == null) {
			Logger.logln("The value must be one of \"NO_REQUEST_WAIT,FULL\"");
			return false;
		}
		try {
			recordViewMode = RecordViewMode.valueOf(value.trim().toUpperCase());
			return true;
		} catch(IllegalArgumentException iae) {
			Logger.logln("The value must be one of \"NO_REQUEST_WAIT,FULL\"");
			return false;
		}
	}

	public boolean showEmptyThreadInRecordView() {
		return showEmptyThreadInRecordView;
	}

	public boolean setShowEmptyThreadInRecordView(String value) {
		if(value == null || (!value.equalsIgnoreCase("on") && !value.equalsIgnoreCase("off"))) {
			Logger.logln("The value must be set ON or OFF");
			return false;
		}
		showEmptyThreadInRecordView = value.trim().equalsIgnoreCase("on");
		return true;
	}

	
	public void printSettings() {
		super.printSettings();
		if(JMXControl.isAnalyzeMode) {
			Logger.logln(" Record");
			Logger.logln("   - RECORD_VIEW_MODE\t" + recordViewMode.name());
			Logger.logln("   - SHOW_EMPTY_THREAD\t" + (showEmptyThreadInRecordView?"ON":"OFF"));
			return;
		}
		Logger.logln(" JMX");
		Logger.logln("   - MBEAN_ONAME\t" + ((setOName==null)?"":setOName));
		Logger.logln("   - PRETTY_PRINT\t" + ((prettyPrint)?"ON":"OFF"));
		Logger.logln(" Record");
		Logger.logln("   - RECORD_VIEW_MODE\t" + recordViewMode.name());
		Logger.logln("   - SHOW_EMPTY_THREAD\t" + (showEmptyThreadInRecordView?"ON":"OFF"));
	}

}
