package dal.tool.trace.jmxer.cli.command;

import java.util.List;

import dal.tool.cli.command.CommandMeta;
import dal.tool.trace.jmxer.JMXControl;
import dal.tool.trace.jmxer.cli.JmxCommandExecutor;
import dal.tool.trace.jmxer.cli.JmxSettings;
import dal.tool.trace.jmxer.cli.helper.ListArgumentsHelper;
import dal.tool.util.NumberUtil;
import dal.tool.util.jmx.JMXUtil;

public class JmxObjectCommand extends JmxCommand {

	public static final CommandMeta commandMeta = new CommandMeta(JmxObjectCommand.class, "OBJ[ECT]");

	
	public JmxObjectCommand(String commandLine, JmxCommandExecutor commandExecutor) {
		super(commandLine, commandExecutor);
	}


	public boolean saveToHistory() {
		return true;
	}

	
	public void printHelp() {
        logln(" OBJECT");
        logln(" ------");
        logln("");
        logln(" Set current ObjectName");
        logln("");
        logln(" OBJ[ECT] [ObjectName]");
        logln("");
        logln(" Enter no argument to show current ObjectName");
        logln(" Enter null value or ObjectName as the argument");
        logln(" ObjectName can be a string or an index");
        logln("");
        logln(" OBJ[ECT]");
        logln(" OBJ[ECT] {-|NULL}");
        logln(" OBJ[ECT] ObjectName");
        logln(" OBJ[ECT] #Index");
	}

	
	public void doExecute() throws Exception {
		if(!checkArgument(0, 1)) return;
		commandArgs.setArguments(ListArgumentsHelper.stripQuotes(commandArgs, new char[]{'"','\''}));
		JmxSettings settings = getSettings();
    	if(commandArgs.size() == 0) {
    		String setOName = settings.getMBeanObjectName();
        	if(setOName == null) {
        		logln("Current ObjectName is unset.");
        	} else {
        		logln("Current ObjectName is '" + setOName + "'");
        	}
        } else if(commandArgs.size() == 1) {
        	String oName = commandArgs.getArgument(1);
        	if(oName.equals("") || oName.equals("-") || oName.equalsIgnoreCase("null")) {
        		if(settings.setMBeanObjectName(null)) {
        			logln("Unset current ObjectName.");
        		}
        	} else {
        		if(oName.startsWith("#")) {
        			if(NumberUtil.isNumber(oName.substring(1))) {
        				List<String> list = ((JmxCommandExecutor)commandExecutor).getObjectNameList();
        				oName = list.get(Integer.parseInt(oName.substring(1))-1);
        			} else {
        				logln("Index must be a number");
        				return;
        			}
        		}        		
        		if(JMXUtil.existsObjectName(getMBeanConnection(), oName)) {
            		if(settings.setMBeanObjectName(oName)) {
            			logln("Set current ObjectName '" + settings.getMBeanObjectName() + "'");
            		}
        		} else {
            		logln("Not found ObjectName '" + oName + "'");	
        		}
        	}
        }
	}

	
	public boolean beforeExecute() throws Exception {
		if(JMXControl.isAnalyzeMode) {
			logln("JMX commands are not allowed in analyze mode.");
			return false;
		}
		return super.beforeExecute();
	}

}
