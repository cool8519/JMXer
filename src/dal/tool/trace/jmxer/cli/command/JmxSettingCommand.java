package dal.tool.trace.jmxer.cli.command;

import dal.tool.cli.command.CommandMeta;
import dal.tool.cli.command.SettingCommand;
import dal.tool.trace.jmxer.JMXControl;
import dal.tool.trace.jmxer.cli.JmxCommandExecutor;
import dal.tool.trace.jmxer.cli.JmxSettings;

public class JmxSettingCommand extends SettingCommand {

	public static final CommandMeta commandMeta = new CommandMeta(JmxSettingCommand.class, "SET");

	private JmxCommandExecutor commandExecutor = null;
	
	
	public JmxSettingCommand(String commandLine, JmxCommandExecutor commandExecutor) {
		super(commandLine, commandExecutor);
		this.commandExecutor = commandExecutor;
	}

	
	public void printHelp() {
		super.printHelp();
		if(JMXControl.isAnalyzeMode) {
	        logln("");
	        logln(" Following is the list of available record variable.");
	        logln("     RECORD_VIEW_MODE - record view mode (NO_REQUEST_WAIT, FULL)");
	        logln("     SHOW_EMPTY_THREAD - show empty thread in record view (ON, OFF)");
			return;
		}
        logln("");
        logln(" Following is the list of available JMX variable.");
        logln("     MBEAN_ONAME  - set current MBean ObjectName");
        logln("     PRETTY_PRINT - display mbean object with newline and indentation");
        logln("");
        logln(" Following is the list of available record variable.");
        logln("     RECORD_VIEW_MODE - record view mode (NO_REQUEST_WAIT, FULL)");
        logln("     SHOW_EMPTY_THREAD - show empty thread in record view (ON, OFF)");
	}

	
	public void doExecute() throws Exception {
		JmxSettings settings = commandExecutor.getSettings();
		if(commandArgs.size() == 0) {
			settings.printSettings();
		} else if(commandArgs.size() == 2) {
			String variable = commandArgs.getArgument(1);
			String value = commandArgs.getArgument(2);
			if(JMXControl.isAnalyzeMode
					&& (variable.equalsIgnoreCase("mbean_oname") || variable.equalsIgnoreCase("pretty_print"))) {
				logln("JMX settings are not available in analyze mode.");
				return;
			}
            if(variable.equalsIgnoreCase("mbean_oname")) {
            	if(value.equals("")) {
            		settings.setMBeanObjectName(null);
            	} else {
            		settings.setMBeanObjectName(value);
            	}
            } else if(variable.equalsIgnoreCase("pretty_print")) {
            	settings.setPrettyPrintInResult(value);
            } else if(variable.equalsIgnoreCase("record_view_mode")) {
            	settings.setRecordViewMode(value);
            } else if(variable.equalsIgnoreCase("show_empty_thread")) {
            	settings.setShowEmptyThreadInRecordView(value);
            } else {
            	super.doExecute();
            }			
		} else {
        	logln("Invalid arguments for setting command.");
		}
	}

}
