package dal.tool.trace.jmxer.cli.command;

import java.io.IOException;
import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServerConnection;

import dal.tool.cli.Logger.Level;
import dal.tool.cli.command.CommandMeta;
import dal.tool.cli.util.IOUtil;
import dal.tool.trace.jmxer.JMXControl;
import dal.tool.trace.jmxer.cli.JmxCommandExecutor;
import dal.tool.trace.jmxer.cli.helper.ListArgumentsHelper;
import dal.tool.trace.jmxer.util.JMXPrintUtil;
import dal.tool.util.NumberUtil;
import dal.tool.util.StringUtil;
import dal.tool.util.jmx.JMXUtil;

public class JmxThreadCommand extends JmxCommand {

	public static final CommandMeta commandMeta = new CommandMeta(JmxThreadCommand.class, "THR[EAD]");

	
	public JmxThreadCommand(String commandLine, JmxCommandExecutor commandExecutor) {
		super(commandLine, commandExecutor);
	}


	public boolean beforeExecute() throws Exception {
		if(JMXControl.isAnalyzeMode) {
			logln("JMX commands are not allowed in analyze mode.");
			return false;
		}
		return super.beforeExecute();
	}
	
	
	public void printHelp() {
        logln(" THREAD");
        logln(" ------");
        logln("");
        logln(" Views threads info.");
        logln("");
        logln(" THR[EAD] [LIST]");
        logln(" THR[EAD] INFO ThreadList");
        logln(" THR[EAD] STACK[TRACE] ThreadList [MaxDepth]");
        logln("");
        logln(" Following is the list of available argument.");
        logln("     LIST       - show list of all threads.");
        logln("     INFO       - get detail thread info.");
        logln("     STACKTRACE - view current stacktrace of threads. Without MaxDepth, show all entries in stack.");
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
	}

	
	public void doExecute() throws Exception {
		String subCmd = StringUtil.stripQuote(commandArgs.getArgument(1), new char[]{'"','\''}, true);
		if(commandArgs.size() == 0 || (commandArgs.size() == 1 && subCmd.equalsIgnoreCase("list"))) {
			commandArgs.setArguments(ListArgumentsHelper.stripQuotes(commandArgs, new char[]{'"','\''}));
			logln("");
			try {
				JMXPrintUtil.printAllThreadList(getMBeanConnection());
			} catch(Exception e) {
				if(e instanceof IOException) {
					throw e;
				}
				logln(Level.ERROR, "Failed to get list of threads : " + e.getMessage());
			}
		} else {
			commandArgs.setArguments(ListArgumentsHelper.concatSpaceWithQuotes(commandArgs, '"'));
			//commandArgs.setArguments(ListArgumentsHelper.stripQuotes(commandArgs, new char[]{'"','\''}));
			String arg = commandArgs.nextArgument();
			if(!checkArgument(2, -1)) return;
			String[] threadIds = null;
			try {
				String threadArgs = commandArgs.nextArgument().trim();
				threadIds = getTargetThreads(getMBeanConnection(), threadArgs);
				if(!threadArgs.equals("*") && threadIds != null && threadIds.length > 0) {
					logln(Level.DEBUG, "List of Thread ID : Count=" + threadIds.length + ", List=[" + StringUtil.arrayToString(threadIds, ",") + "]");
				}
			} catch(Exception e) {
				logln(Level.ERROR, "Failed to get thread id from the argument : " + e.getMessage());
				return;
			}
			arg = StringUtil.stripQuote(arg, new char[]{'"','\''}, true);
			if(arg.equalsIgnoreCase("info")) {
				if(!checkArgument(2)) return;
		    	if(threadIds == null || threadIds.length < 1) {
		    		logln("No target thread(s) specified.");
		    		return;
		    	}
				try {
					JMXPrintUtil.printThreadInfo(getMBeanConnection(), threadIds);
				} catch(Exception e) {
					if(e instanceof IOException) {
						throw e;
					}
					logln(Level.ERROR, "Failed to get thread info : \"" + StringUtil.arrayToString(threadIds,",") + "\" : " + e.getMessage());
				}
			} else if(IOUtil.isIncludeEquals(arg, "stack", "trace")) {
				if(!checkArgument(2, 3)) return;
		    	if(threadIds == null || threadIds.length < 1) {
		    		logln("No target thread(s) specified.");
		    		return;
		    	}
				int maxDepth = -1;
				String maxDepthString = null;
				if(commandArgs.hasMoreArgument()) {
					maxDepthString = commandArgs.nextArgument();
					try {
						maxDepth = Integer.parseInt(maxDepthString);
					} catch(NumberFormatException nfe) {
						logln("Invalid argument for MaxDepth : " + maxDepthString);
						return;
					}
				}
				try {
					JMXPrintUtil.printStackTrace(getMBeanConnection(), threadIds, maxDepth);
				} catch(Exception e) {
					if(e instanceof IOException) {
						throw e;
					}
					logln(Level.ERROR, "Failed to get stacktrace of thread : \"" + StringUtil.arrayToString(threadIds,",") + "\" : " + e.getMessage());
				}
			} else {
				logln("Invalid argument for thread command.");
			}
		}
	}


	public static String[] getTargetThreads(MBeanServerConnection mbeanServerConnection, String args) throws Exception {
		if(args == null || args.equals("")) {
			return null;
		}
		if(args.equals("*")) {
			String[] thrListArr = { "*" };
			return thrListArr;
		}
		Long[] thrIds = getTargetThreadIds(mbeanServerConnection, args);
		String[] result = new String[thrIds.length];
		for(int i = 0; i < thrIds.length; i++) {
			result[i] = String.valueOf(thrIds[i]);
		}
		return result;
	}
	
	public static Long[] getTargetThreadIds(MBeanServerConnection mbeanServerConnection, String args) throws Exception {
		if(args == null || args.equals("")) {
			return null;
		}
		Map<Long,ThreadInfo> threadInfoMap = JMXUtil.getAllThreadInfo(mbeanServerConnection);
		Map<Long,String> threadMap = new HashMap<Long,String>();
		Iterator<Long> iter = threadInfoMap.keySet().iterator();
		while(iter.hasNext()) {
			Long id = iter.next();
			threadMap.put(id, threadInfoMap.get(id).getThreadName());
		}
		return getTargetThreadIds(threadMap, args);
	}
	
	public static Long[] getTargetThreadIds(Map<Long,String> threadMap, String args) throws Exception {
		if(args == null || args.equals("")) {
			return null;
		}
		Set<Long> thrSet = new HashSet<Long>();
		if(args.equals("*")) {
			thrSet = threadMap.keySet();
		} else {		
			String[] targetThreads = args.split(",");
			for(String target : targetThreads) {
				target = target.trim();
				if(target.length() == 0) continue;
				if(NumberUtil.isNumber(target)) {
					// thread id
					if(threadMap.containsKey(Long.valueOf(target))) {
						thrSet.add(Long.valueOf(target));
					}
				} else if(Character.isDigit(target.charAt(0))) {
					// thread id range
					String[] range = target.split("-");
					if(range.length == 2) {
						String s_from = range[0];
						String s_to = range[1];
						if(!NumberUtil.isNumber(s_from) || !NumberUtil.isNumber(s_to)) {
							throw new Exception("Thread id range('" + target + "') contains invalid characters.");
						} else {
							int from = Integer.parseInt(s_from);
							int to = Integer.parseInt(s_to);
							if(from > to) {
								throw new Exception("The thread id range('" + target + "') must be consist of small to large.");
							} else {
								for(int i = from; i <= to; i++) {
									if(threadMap.containsKey(Long.valueOf(i))) {
										thrSet.add(Long.valueOf(i));
									}
								}
							}
						}
					} else {
						throw new Exception("Thread id range('" + target + "') could not be resolved.");						
					}					
				} else if(target.startsWith("\"")) {
					// thread name
					if(target.length() > 1 && target.endsWith("\"")) {
						target = target.substring(1, target.length()-1);
						Iterator<Long> iter = threadMap.keySet().iterator();
						while(iter.hasNext()) {
							Long id = iter.next();
							String name = threadMap.get(id);
							if(target.indexOf('*') < 0 && target.indexOf('?') < 0) {
								if(name.equals(target)) {
									thrSet.add(id);
								}
							} else {
								// thread name pattern
								if(StringUtil.isMatchStringWithPattern(name, target)) {
									thrSet.add(id);
								}
							}
						}
					} else {
						throw new Exception("Thread name('" + target + "') must be enclosed in double quotation marks.");
					}
				} else if(target.equals("*")) {
					throw new Exception("'*' must be used alone.");
				} else {
					throw new Exception("The argument('" + target + "') could not be resolved.");					
				}				
			}
		}
		ArrayList<Long> thrList = new ArrayList<Long>(thrSet);
		Collections.sort(thrList);
		return thrList.toArray(new Long[0]);
	}
		
}
