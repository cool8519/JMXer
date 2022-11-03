package dal.tool.cli.command;

import dal.tool.cli.CommandExecutor;
import dal.tool.cli.Logger;

public class SpoolCommand extends Command {

	public static final CommandMeta commandMeta = new CommandMeta(SpoolCommand.class, ">", "SPO[OL]");

	
	public SpoolCommand(String commandLine, CommandExecutor commandExecutor) {
		super(commandLine, commandExecutor);
	}


	public boolean saveToHistory() {
		return true;
	}

	
	public void printHelp() {
        logln(" SPOOL");
        logln(" -----");
        logln("");
        logln(" Stores command results in a file.");
        logln(" Use the name of file with '>' at the command prompt.");
        logln("");
        logln(" {>|SPO[OL]} {file_name[.ext]|OFF} [APPEND]");
	}

	
	public void doExecute() throws Exception {
        if(commandArgs.size() == 0) {
            if(Logger.getSpoolName() == null) {
                logln("Not spooling currently.");
            } else {
                logln("Currently spooling to " + Logger.getSpoolName());
            }
        } else if(commandArgs.size() == 1) {
            String first = commandArgs.getArgument(1);
            if(first.equalsIgnoreCase("off")) {
                if(Logger.getSpoolName() == null) {
                    logln("Not spooling currently.");
                } else {
                	Logger.setSpoolOff();
                }
            } else {
            	Logger.setSpoolName(first, false);
            }
        } else if(commandArgs.size() == 2) {
            String first = commandArgs.getArgument(1);
            String second = commandArgs.getArgument(2);
            if(second.equalsIgnoreCase("append")) {
            	Logger.setSpoolName(first, true);
            } else {
                logln("Unknown command \"" + second + "\" - only append option is allowed.");
            }
        } else {
        	logln("Invalid arguments for spool command.");
        }
	}

}
