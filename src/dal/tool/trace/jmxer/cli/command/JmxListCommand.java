package dal.tool.trace.jmxer.cli.command;

import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanFeatureInfo;
import javax.management.ObjectName;

import dal.tool.cli.Logger.Level;
import dal.tool.cli.command.CommandMeta;
import dal.tool.trace.jmxer.JMXControl;
import dal.tool.trace.jmxer.cli.JmxCommandExecutor;
import dal.tool.trace.jmxer.cli.helper.ListArgumentsHelper;
import dal.tool.trace.jmxer.util.JMXPrintUtil;
import dal.tool.util.StringUtil;
import dal.tool.util.jmx.JMXUtil;

public class JmxListCommand extends JmxCommand {

	public static final CommandMeta commandMeta = new CommandMeta(JmxListCommand.class, "LS", "LIST");

	
	public JmxListCommand(String commandLine, JmxCommandExecutor commandExecutor) {
		super(commandLine, commandExecutor);
	}


	public void printHelp() {
        logln(" LIST");
        logln(" ----");
        logln("");
        logln(" Displays ObjectName of all MBeans");
        logln("");
        logln(" {LS|LIST} [value]");
        logln("");
        logln(" Following is the list of available value.");
        logln("     no_value    - show all MBean Attributes and Operations if an ObjectName is set.");
        logln("                   show all ObjectName(s) if an ObjectName is not set.");
        logln("     target_path - show MBean ObjectName, Attributes or Operations.");
        logln("                   the path format would be '[../|/]MBean_Name_Pattern[/Name_Pattern]'");
        logln("                   the path can include '..' or '/' and pattern can include '?' or '*'");
        logln("                   ex) ls ../*GarbageCollector*/*Count");
	}

	
	public void doExecute() throws Exception {
		if(!checkArgument(0, 1)) return;
		commandArgs.setArguments(ListArgumentsHelper.stripQuotes(commandArgs, new char[]{'"','\''}));
		JmxCommandExecutor executor = (JmxCommandExecutor)commandExecutor;
    	String setOName = getSettings().getMBeanObjectName();
		try {
	    	if(commandArgs.size() == 0) {
	        	if(setOName == null) {
	        		executor.refreshObjectNameList(JMXUtil.getObjectNames(getMBeanConnection(), null));
	        		JMXPrintUtil.printObjectNames(executor.getObjectNameList(), null, null);
	        	} else {
					JMXPrintUtil.printAttributeAndOperationList(new ArrayList<MBeanFeatureInfo>(getMBeanInfoList(setOName).values()), setOName, null);
	        	}
	    	} else if(commandArgs.size() == 1) {
	    		List<String> targets = null;
	    		try {
	    			targets = StringUtil.getTokenList(commandArgs.getArgument(1), '/', StringUtil.QUOTES);
	    			targets = parseListTarget(targets, setOName);
	    		} catch(Exception e) {
	    			logln("Target path parsing error : " + e.getMessage());
	    			return;
	    		}
	    		if(targets != null) {
		    		if(targets.size() == 0) {
		        		executor.refreshObjectNameList(JMXUtil.getObjectNames(getMBeanConnection(), null));
		        		JMXPrintUtil.printObjectNames(executor.getObjectNameList(), null, null);
		    		} else if(targets.size() == 1) {
		    			String mbeanNamePattern = (targets.get(0)==null||targets.get(0).equals("*")) ? null : targets.get(0);
		    			List<ObjectName> oNames = JMXUtil.getObjectNames(getMBeanConnection(), mbeanNamePattern);
		    			JMXPrintUtil.printObjectNames(executor.getObjectNameList(), oNames, mbeanNamePattern);
		    		} else if(targets.size() == 2) {
		    			String mbeanNamePattern = (targets.get(0)==null||targets.get(0).equals("*")) ? null : targets.get(0);
		    			String namePattern = (targets.get(1)==null||targets.get(1).equals("*")) ? null : targets.get(1);
		    			List<ObjectName> oNames = JMXUtil.getObjectNames(getMBeanConnection(), mbeanNamePattern);
		    			for(ObjectName oName : oNames) {
							JMXPrintUtil.printAttributeAndOperationList(new ArrayList<MBeanFeatureInfo>(getMBeanInfoList(oName.toString()).values()), oName.toString(), namePattern);
		    			}
		    		}
	    		}
	    	}
		} catch(Exception e) {
			logln(Level.ERROR, "Failed to execute list command : " + e.getMessage());
			throw e;
		}
	}

	
	private List<String> parseListTarget(List<String> targets, String setOName) throws Exception {
		List<String> ret = new ArrayList<String>(2);
		ret.add("");
		ret.add("");
		int depth = -1;
		if(targets.get(0).equals("")) {
			depth = 0;
			targets.remove(0);
		} else {
			if(setOName == null) {
				depth = 0;
			} else {
				depth = 1;
				ret.set(0, setOName);
			}
		}
		for(int i = 0; i < targets.size(); i++) {
			String target = targets.get(i);
			if(target.equals("")) {
				if(i+1 >= targets.size()) {
					if(depth < ret.size()) {
						ret.set(depth, "*");
					} else {
						throw new Exception("Invalid target path. Depth is over 2.");
					}
				} else {
					throw new Exception("Target cannot be empty.");
				}
			} else if(target.equals(".")) {
				continue;
			} else if(target.equals("..")) {
				if(--depth > -1) {
					ret.set(depth, "");
				} else {
					throw new Exception("Invalid target path. Depth is under 0.");
				}
			} else {
				if(depth < 2) {
					ret.set(depth++, target);
				} else {
					throw new Exception("Invalid target path. Depth is over 2.");
				}
			}
		}
		if(ret.get(1).equals("")) {
			ret.remove(1);
			if(ret.get(0).equals("")) {
				ret.remove(0);
			}			
		}
		return ret;		
	}
	
	
	public boolean beforeExecute() throws Exception {
		if(JMXControl.isAnalyzeMode) {
			logln("JMX commands are not allowed in analyze mode.");
			return false;
		}
		return super.beforeExecute();
	}
	
}
