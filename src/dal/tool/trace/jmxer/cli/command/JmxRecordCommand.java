package dal.tool.trace.jmxer.cli.command;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import dal.tool.cli.Logger;
import dal.tool.cli.Logger.Level;
import dal.tool.cli.command.CommandMeta;
import dal.tool.cli.util.IOUtil;
import dal.tool.trace.jmxer.JMXControl;
import dal.tool.trace.jmxer.JMXerConstant;
import dal.tool.trace.jmxer.cli.JmxCommandExecutor;
import dal.tool.trace.jmxer.cli.data.RecordResult;
import dal.tool.trace.jmxer.cli.helper.ListArgumentsHelper;
import dal.tool.trace.jmxer.cli.helper.ResourceRecordThread;
import dal.tool.trace.jmxer.cli.helper.StackRecordThread;
import dal.tool.util.NumberUtil;
import dal.tool.util.StringUtil;

public class JmxRecordCommand extends JmxCommand {

	public static final CommandMeta commandMeta = new CommandMeta(JmxRecordCommand.class, "REC[ORD]");

	private static RecordResult lastRecordResult = null;
	
	
	public JmxRecordCommand(String commandLine, JmxCommandExecutor commandExecutor) {
		super(commandLine, commandExecutor);
	}

	
	public boolean beforeExecute() throws Exception {
		if(JMXControl.isAnalyzeMode && commandArgs.size() > 0) {
			String arg = commandArgs.getArguments().get(0);
			if(!arg.equalsIgnoreCase("info") && !arg.equalsIgnoreCase("view") && !arg.equalsIgnoreCase("load") && !arg.equalsIgnoreCase("save")) {
				logln("JMX commands are not allowed in analyze mode.");
				return false;
			}
		}
		return super.beforeExecute();
	}
	
	
	public void printHelp() {
        logln(" RECORD");
        logln(" ------");
        logln("");
        logln(" Profile changes in the state of threads.");
        logln("");
        logln(" REC[ORD] RES[OURCE] ThreadList");
        logln(" REC[ORD] STACK[TRACE] ThreadList");
        logln(" REC[ORD] VIEW ViewType [ViewTypeArgs...]");
        logln(" REC[ORD] SAVE [FileName]");
        logln(" REC[ORD] LOAD [FileName]");
        logln("");
        logln(" Following is the list of available argument.");
        logln("     RESOURCE    - record resource usage of threads for a while.");
        logln("     STACKTRACE  - record stacktrace sample of threads for a while.");
        logln("     VIEW        - display the result of the last recorded stacktrace sample data.");
        logln("     SAVE        - save the last recorded stacktrace sample data to a file.");
        logln("     LOAD        - load stacktrace sample data from a file.");
        logln("");
        logln(" ThreadList : {*|ThreadIds|ThreadNames}");
        logln("   '*' means ALL_THREADS");
        logln("   'ThreadIds' is a list of THREAD_ID");
        logln("      You can specify a range with the '-' character. ex) 1-5");
        logln("   'ThreadNames' is a list of THREAD_NAME");
        logln("      Each THREAD_NAME must be enclosed in double quotation marks.");
        logln("      You can use the '*' and '?' as a pattern character. ex) WorkerThread-*");
        logln("   'ThreadIds' and 'ThreadNames' could be array type. ex) 1,3,21-30 or \"main\",\"Thread-*\",\"pool-1-thread-*\"");
        logln("   'ThreadIds' and 'ThreadNames' could also be used in combination. ex) 1,3-5,\"Thread-*\"");
        logln("");
        logln(" ViewType : {INFO|METHOD|THREAD|STACK|SEARCH}");
        logln("   'INFO' means to show the information of the last recorded stacktrace sample.");
        logln("      Usage) record view info");
        logln("   'METHOD' means to aggregate the results of all methods in threads.");
        logln("      Usage) record view method ThreadList [RangeExpression]");
        logln("        arg 1) ThreadList : {*|ThreadIds|ThreadNames}");
        logln("        arg 2) RangeExpression : {AbsoluteTimeRange|RelativeTimeRange|SampleOrderRange}");
        logln("   'THREAD' means to aggregate the results per thread.");
        logln("      Usage) record view thread ThreadList [RangeExpression]");
        logln("        arg 1) ThreadList : {*|ThreadIds|ThreadNames}");
        logln("        arg 2) RangeExpression : {AbsoluteTimeRange|RelativeTimeRange|SampleOrderRange}");
        logln("   'STACK' means to show the stacktraces of a specific point in time or period.");
        logln("      Usage) record view stack TargetThread PointExpression");
        logln("        arg 1) TargetThread : {ThreadIds|ThreadNames}");
        logln("        arg 2) PointExpression : {AbsoluteTimePoint|RelativeTimePoint|SampleOrderPoint}");
        logln("   'SEARCH' means to find method or class call information within the record result.");
        logln("      Usage) record view search ThreadList NameExpression");
        logln("        arg 1) ThreadList : {*|ThreadIds|ThreadNames}");
        logln("        arg 2) NameExpression : '*' and '?' as name pattern characters. (case-sensitive, at least 3 characters)");
        logln("   * RangeExpression :");
        logln("       - AbsoluteTimeRange : time_pattern~[time_pattern]  ex) 2022.10.28/12:35:00~2022.10.28/12:35:10");
        logln("       - RelativeTimeRange : relative_ms~[relative_ms]    ex) 1000ms~5000ms");
        logln("       - SampleOrderRange  : sample_order~[sample_order]  ex) 10~20");
        logln("   * PointExpression :");
        logln("       - AbsoluteTimePoint : time_pattern  ex) 2022.10.28/12:35:01");
        logln("       - RelativeTimePoint : relative_ms   ex) 1500ms");
        logln("       - SampleOrderPoint  : sample_order  ex) 13");
	}

	
	public void doExecute() throws Exception {
		if(!checkArgument(1, -1)) return;
		String arg = StringUtil.stripQuote(commandArgs.nextArgument(), new char[]{'"','\''}, true);
		if(IOUtil.isIncludeEquals(arg, "res", "ource") || IOUtil.isIncludeEquals(arg, "stack", "trace")) {
			commandArgs.setArguments(ListArgumentsHelper.concatSpaceWithQuotes(commandArgs, '"'));
			if(!checkArgument(2)) {
				if(IOUtil.isIncludeEquals(arg, "res", "ource")) {
			        logln("  Usage) REC[ORD] RES[OURCE] ThreadList");					
				} else {
			        logln("  Usage) REC[ORD] STACK[TRACE] ThreadList");					
				}
				return;
			}
			String[] threadIds = null;
			try {
				String threadArgs = commandArgs.nextArgument().trim();
				threadIds = JmxThreadCommand.getTargetThreads(getMBeanConnection(), threadArgs);
				if(!threadArgs.equals("*") && threadIds != null && threadIds.length > 0) {
					logln(Level.DEBUG, "List of Thread ID : Count=" + threadIds.length + ", List=[" + StringUtil.arrayToString(threadIds, ",") + "]");
				}
			} catch(Exception e) {
				logln(Level.ERROR, "Failed to get thread id from the argument : " + e.getMessage());
				return;
			}
			if(IOUtil.isIncludeEquals(arg, "res", "ource")) {
		    	if(threadIds == null || threadIds.length < 1) {
		    		logln("No target thread(s) specified.");
		    		return;
		    	}
				long interval_ms = 1000L;
				String time = IOUtil.readLine("Sampling Time in milliseconds(0, Until the enter key is pressed): ", Logger.Level.RESULT);
				long time_ms = 0L;
				if("".equals(time.trim())) {
				} else if(NumberUtil.isNumber(time) && Integer.parseInt(time) > -1) {
					time_ms = Integer.valueOf(time);
				} else {
					logln("Invalid value for time. The default of 0 will be applied.");
					return;
				}				
				String yn = null;
				while(true) {
					yn = IOUtil.readLine("Do you want to monitor resource usage of " + ("*".equals(threadIds[0])?"all":threadIds.length) + " threads(Y/N)? ", Logger.Level.RESULT);
					yn = yn.trim().toUpperCase();
					if("Y".equals(yn) || "N".equals(yn)) {
						break;
					}
				}
				if("Y".equals(yn)) {
					ResourceRecordThread recordThread = new ResourceRecordThread(getMBeanConnection(), threadIds);
					recordThread.startRecording(interval_ms, time_ms);
					if(time_ms == 0) {
						IOUtil.readLine("Press enter key to stop recording.\n", Logger.Level.RESULT);
						recordThread.stopRecording();
					}
					recordThread.printResult();
				}
			} else if(IOUtil.isIncludeEquals(arg, "stack", "trace")) {
		    	if(threadIds == null || threadIds.length < 1) {
		    		logln("No target thread(s) specified.");
		    		return;
		    	}
				String interval = IOUtil.readLine("Sampling Interval in milliseconds(500): ", Logger.Level.RESULT);
				long interval_ms = 500L;
				if("".equals(interval.trim())) {
				} else if(NumberUtil.isNumber(interval)) {
					interval_ms = Integer.valueOf(interval);
				} else {
					logln("Invalid value for interval. The default of 500ms will be applied.");
					return;
				}
				String time = IOUtil.readLine("Sampling Time in milliseconds(0, Until the enter key is pressed): ", Logger.Level.RESULT);
				long time_ms = 0L;
				if("".equals(time.trim())) {
				} else if(NumberUtil.isNumber(time) && Integer.parseInt(time) > -1) {
					time_ms = Integer.valueOf(time);
				} else {
					logln("Invalid value for time. The default of 0 will be applied.");
					return;
				}
				logln("");
				logln("[WARNING]");
				logln(" - If the sampling interval is short or the number of threads is large, it may be slow.");
				if(time_ms == 0) {
					logln(" - Do not record for too long. Recording will continue until you press the enter key.");
				}
				logln("");
				String yn = null;
				while(true) {
					yn = IOUtil.readLine("Do you want to sample stacktrace for " + ("*".equals(threadIds[0])?"all":threadIds.length) + " threads(Y/N)? ", Logger.Level.RESULT);
					yn = yn.trim().toUpperCase();
					if("Y".equals(yn) || "N".equals(yn)) {
						break;
					}
				}
				if("Y".equals(yn)) {	
					StackRecordThread recordThread = new StackRecordThread(getMBeanConnection(), threadIds);
					recordThread.startRecording(interval_ms, time_ms);
					if(time_ms == 0) {
						IOUtil.readLine("Press enter key to stop recording.\n", Logger.Level.RESULT);
						recordThread.stopRecording();
					}
					recordThread.printResult();
					if(recordThread.getSampleCount() > 0) {
						lastRecordResult = recordThread.getRecordResult();
						lastRecordResult.vmInfo = InfoCommand.getTargetVMInfo(getMBeanConnection());
						lastRecordResult.toolInfo = InfoCommand.getToolInfo();
					}
				}
			}			
		} else {
			if(arg.equalsIgnoreCase("view")) {
				commandArgs.setArguments(ListArgumentsHelper.concatSpaceWithQuotes(commandArgs, '"'));
				if(!checkArgument(2, 4)) {
					logln("  Usage) REC[ORD] VIEW ViewType [ViewTypeArgs...]");					
					return;
				}
				if(lastRecordResult == null) {
					logln("No record result in memory. Please record a stacktrace or load a dump file.");
					return;
				}
				List<String> viewArgs = new ArrayList<String>();
				while(commandArgs.hasMoreArgument()) {
					viewArgs.add(commandArgs.nextArgument());
				}					
				lastRecordResult.printResult(viewArgs);
			} else if(arg.equalsIgnoreCase("save")) {
				commandArgs.setArguments(ListArgumentsHelper.stripQuotes(commandArgs, new char[]{'"','\''}));
				if(!checkArgument(1, 2)) {
			        logln("  Usage) REC[ORD] SAVE [FileName]");					
					return;
				}
				if(lastRecordResult == null) {
					logln("No record result in memory. Please record a stacktrace or load a dump file.");
					return;
				} else if(lastRecordResult.dmpFilePath != null && new File(lastRecordResult.dmpFilePath).exists()) {
					logln("The record data is already written in the dump file : " + lastRecordResult.dmpFilePath);
					return;
				}
				String path = commandArgs.hasMoreArgument() ? commandArgs.nextArgument().trim() : JMXerConstant.DEFAULT_DUMP_FILE_PATH;
				String yn = "Y";
				if(new File(path).exists()) {
					while(true) {
						yn = IOUtil.readLine("The dump file to be saved already exists. Do you want to overwrite the dump file(Y/N)? ", Logger.Level.RESULT);
						yn = yn.trim().toUpperCase();
						if("Y".equals(yn) || "N".equals(yn)) {
							break;
						}
					}
				}
				if("Y".equals(yn)) {	
					lastRecordResult.saveToFile(new File(path));
					logln("Successfully saved stacktrace data to the dump file : " + new File(path).getAbsolutePath());
				}
			} else if(arg.equalsIgnoreCase("load")) {
				if(!checkArgument(1, 2)) {
			        logln("  Usage) REC[ORD] LOAD [FileName]");					
					return;
				}
				String path = commandArgs.hasMoreArgument() ? commandArgs.nextArgument().trim() : JMXerConstant.DEFAULT_DUMP_FILE_PATH;
				File dumpfile = new File(path);
				if(!dumpfile.exists()) {
					logln("The dump file to load does not exist : " + dumpfile.getAbsolutePath());
					return;
				}
				if(lastRecordResult != null && dumpfile.getCanonicalPath().equals(lastRecordResult.dmpFilePath)) {
					logln("The record data has already been loaded from the dump file : " + lastRecordResult.dmpFilePath);
					return;
				}
				String yn = "Y";
				if(lastRecordResult != null && lastRecordResult.dmpFilePath == null) {
					while(true) {
						yn = IOUtil.readLine("Current record data will be lost because it has not yet been saved. Do you want to continue(Y/N)? ", Logger.Level.RESULT);
						yn = yn.trim().toUpperCase();
						if("Y".equals(yn) || "N".equals(yn)) {
							break;
						}
					}
				}
				if("Y".equals(yn)) {	
					RecordResult loadedResult = RecordResult.loadFromFile(dumpfile);
					if(loadedResult != null) {
						lastRecordResult = loadedResult;
						logln("Successfully loaded stacktrace data from the dump file. It can be analyzed with the print command.");
					}
				}
			} else {
				logln("Invalid argument for record command.");
			}
		}
	}

}
