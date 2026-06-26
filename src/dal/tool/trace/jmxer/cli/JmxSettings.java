package dal.tool.trace.jmxer.cli;

import dal.tool.cli.Logger;
import dal.tool.cli.Settings;
import dal.tool.trace.jmxer.JMXControl;

/**
 * JMXer CLI/TUI 세션 설정(MBean ObjectName, RECORD VIEW 필터 등)
 *
 */
public class JmxSettings extends Settings {

	/** RECORD VIEW 스택 덤프 필터 모드 */
	public static enum DumpStackFilter {
		NO_REQUEST_WAIT,
		FULL
	}

	/** THREAD STACKTRACE 대상(선택 스레드 / 활성 스레드) */
	public static enum StacktraceTarget {
		SELECTED,
		ACTIVE
	}

	protected String setOName = null;
	protected boolean prettyPrint = true;	
	protected DumpStackFilter dumpStackFilter = DumpStackFilter.NO_REQUEST_WAIT;
	protected boolean showEmptyThreadInRecordView = true;
	protected StacktraceTarget stacktraceTarget = StacktraceTarget.SELECTED;
	protected String reqEntryFile = ".jmxer/request-entries.txt";

	
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

	public boolean togglePrettyPrintInResult() {
		prettyPrint = !prettyPrint;
		return prettyPrint;
	}

	public DumpStackFilter getDumpStackFilter() {
		return dumpStackFilter;
	}

	public boolean setDumpStackFilter(String value) {
		if(value == null) {
			Logger.logln("The value must be one of \"NO_REQUEST_WAIT,FULL\"");
			return false;
		}
		try {
			dumpStackFilter = DumpStackFilter.valueOf(value.trim().toUpperCase());
			return true;
		} catch(IllegalArgumentException iae) {
			Logger.logln("The value must be one of \"NO_REQUEST_WAIT,FULL\"");
			return false;
		}
	}

	public DumpStackFilter toggleDumpStackFilter() {
		dumpStackFilter = (dumpStackFilter == DumpStackFilter.NO_REQUEST_WAIT)
			? DumpStackFilter.FULL
			: DumpStackFilter.NO_REQUEST_WAIT;
		return dumpStackFilter;
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

	public boolean toggleShowEmptyThreadInRecordView() {
		showEmptyThreadInRecordView = !showEmptyThreadInRecordView;
		return showEmptyThreadInRecordView;
	}

	public StacktraceTarget getStacktraceTarget() {
		return stacktraceTarget;
	}

	public boolean setStacktraceTarget(String value) {
		if(value == null) {
			Logger.logln("The value must be one of \"SELECTED,ACTIVE\"");
			return false;
		}
		try {
			stacktraceTarget = StacktraceTarget.valueOf(value.trim().toUpperCase());
			return true;
		} catch(IllegalArgumentException iae) {
			Logger.logln("The value must be one of \"SELECTED,ACTIVE\"");
			return false;
		}
	}

	public StacktraceTarget toggleStacktraceTarget() {
		stacktraceTarget = (stacktraceTarget == StacktraceTarget.SELECTED)
			? StacktraceTarget.ACTIVE
			: StacktraceTarget.SELECTED;
		return stacktraceTarget;
	}

	public String getReqEntryFile() {
		return reqEntryFile;
	}

	public boolean setReqEntryFile(String value) {
		if(value == null || value.trim().isEmpty()) {
			reqEntryFile = "";
			return true;
		}
		reqEntryFile = value.trim();
		return true;
	}

	public void printSettings() {
		super.printSettings();
		Logger.logln("   - REQUEST_ENTRY_FILE\t" + reqEntryFile);
		if(JMXControl.isOfflineSession) {
			Logger.logln(" Record");
			Logger.logln("   - DUMP_STACK_FILTER\t" + dumpStackFilter.name());
			Logger.logln("   - SHOW_EMPTY_THREAD\t" + (showEmptyThreadInRecordView?"ON":"OFF"));
			return;
		}
		Logger.logln(" JMX");
		Logger.logln("   - MBEAN_ONAME\t" + ((setOName==null)?"":setOName));
		Logger.logln("   - PRETTY_PRINT\t" + ((prettyPrint)?"ON":"OFF"));
		Logger.logln(" Thread");
		Logger.logln("   - STACKTRACE_TARGET\t" + stacktraceTarget.name());
		Logger.logln(" Record");
		Logger.logln("   - DUMP_STACK_FILTER\t" + dumpStackFilter.name());
		Logger.logln("   - SHOW_EMPTY_THREAD\t" + (showEmptyThreadInRecordView?"ON":"OFF"));
	}

}
