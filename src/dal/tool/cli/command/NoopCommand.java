package dal.tool.cli.command;

import dal.tool.cli.CommandExecutor;

public class NoopCommand extends Command {

	public static final CommandMeta commandMeta = new CommandMeta(NoopCommand.class);

	
	public NoopCommand(String commandLine, CommandExecutor commandExecutor) {
		super(commandLine, commandExecutor);
	}

	
	public boolean saveToHistory() {
    	if(commandLine != null && commandLine.trim().length() > 0) {
    		return true;
    	} else {
    		return false;
    	}
	}

	
	public void printHelp() {}

	
	public void doExecute() throws Exception {
    	if(commandLine != null && !commandLine.startsWith("--") && commandLine.trim().length() > 0) {
            logln("Unknown command \"" + commandLine + "\".");
    	}
	}

	
	public boolean afterExecute() throws Exception {
		return true;
	}

}
