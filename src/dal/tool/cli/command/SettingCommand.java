package dal.tool.cli.command;

import dal.tool.cli.CommandExecutor;
import dal.tool.cli.Settings;

public class SettingCommand extends Command {

	public static final CommandMeta commandMeta = new CommandMeta(SettingCommand.class, "SET");

	
	public SettingCommand(String commandLine, CommandExecutor commandExecutor) {
		super(commandLine, commandExecutor);
	}


	public boolean saveToHistory() {
		return true;
	}

	
	public void printHelp() {
        logln(" SET");
        logln(" ---");
        logln("");
        logln(" Sets a common variable to alter settings");
        logln("");
        logln(" SET [variable] [value]");
        logln("");
        logln(" Following is the list of available common variable.");
        logln("     TIMING - number of seconds waiting for executing command");
        logln("     TIME   - display current time in the prompt");
	}

	
	public void doExecute() throws Exception {
		Settings settings = getSettings();
		if(commandArgs.size() == 0) {
			settings.printSettings();
		} else if(commandArgs.size() == 2) {
			String variable = commandArgs.getArgument(1);
			String value = commandArgs.getArgument(2);
            if(variable.equalsIgnoreCase("loglevel")) {
            	settings.setLogLevel(value);
            } else if(variable.equalsIgnoreCase("time")) {
            	settings.setShowTimeInPrompt(value);
            } else if(variable.equalsIgnoreCase("timing")) {
            	settings.setShowTimingOnExecute(value);
            } else {
                logln("Unknown SET variable \"" + variable + "\"");
            }			
		} else {
        	logln("Invalid arguments for setting command.");
		}
	}

}
