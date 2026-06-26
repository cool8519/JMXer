package dal.tool.trace.jmxer.cli.command;

import dal.tool.cli.command.CommandMeta;
import dal.tool.cli.command.SettingCommand;
import dal.tool.trace.jmxer.JMXControl;
import dal.tool.trace.jmxer.cli.JmxCommandExecutor;
import dal.tool.trace.jmxer.cli.JmxSettings;

/**
 * JMXer SET 명령(JMX·Thread·Record 설정)
 *
 */
public class JmxSettingCommand extends SettingCommand {

	public static final CommandMeta commandMeta = new CommandMeta(JmxSettingCommand.class, "SET");

	private JmxCommandExecutor commandExecutor = null;
	
	
	public JmxSettingCommand(String commandLine, JmxCommandExecutor commandExecutor) {
		super(commandLine, commandExecutor);
		this.commandExecutor = commandExecutor;
	}

	
	public void printHelp() {
		super.printHelp();
		logln("     REQUEST_ENTRY_FILE - file of user-defined request-entry signatures (one 'package.Class.method' per line)");
		if(JMXControl.isOfflineSession) {
	        logln("");
	        logln(" Following is the list of available record variable.");
	        logln("     DUMP_STACK_FILTER - dump stack filter for RECORD VIEW (NO_REQUEST_WAIT, FULL)");
	        logln("     SHOW_EMPTY_THREAD - show empty thread in record view (ON, OFF)");
			return;
		}
        logln("");
        logln(" Following is the list of available JMX variable.");
        logln("     MBEAN_ONAME  - set current MBean ObjectName");
        logln("     PRETTY_PRINT - display mbean object with newline and indentation");
        logln("");
        logln(" Following is the list of available thread variable.");
        logln("     STACKTRACE_TARGET - thread stacktrace target (SELECTED, ACTIVE)");
        logln("");
        logln(" Following is the list of available record variable.");
        logln("     DUMP_STACK_FILTER - dump stack filter for RECORD VIEW (NO_REQUEST_WAIT, FULL)");
        logln("     SHOW_EMPTY_THREAD - show empty thread in record view (ON, OFF)");
	}

	
	public void doExecute() throws Exception {
		JmxSettings settings = commandExecutor.getSettings();
		if(commandArgs.size() == 0) {
			settings.printSettings();
		} else if(commandArgs.size() == 2) {
			String variable = commandArgs.getArgument(1);
			String value = commandArgs.getArgument(2);
			if(JMXControl.isOfflineSession
					&& (variable.equalsIgnoreCase("mbean_oname")
						|| variable.equalsIgnoreCase("pretty_print")
						|| variable.equalsIgnoreCase("stacktrace_target"))) {
				logln("JMX settings are not available in offline session.");
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
            } else if(variable.equalsIgnoreCase("dump_stack_filter")) {
            	settings.setDumpStackFilter(value);
            } else if(variable.equalsIgnoreCase("show_empty_thread")) {
            	settings.setShowEmptyThreadInRecordView(value);
            } else if(variable.equalsIgnoreCase("stacktrace_target")) {
            	settings.setStacktraceTarget(value);
            } else if(variable.equalsIgnoreCase("request_entry_file")) {
            	settings.setReqEntryFile(value);
            } else {
            	super.doExecute();
            }			
		} else {
        	logln("Invalid arguments for setting command.");
		}
	}

}
