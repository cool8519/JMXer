package dal.tool.cli.command;

import dal.tool.cli.CommandExecutor;
import dal.tool.cli.Logger;


public class ExitCommand extends Command {

	public static final CommandMeta commandMeta = new CommandMeta(ExitCommand.class, "EXIT", "Q[UIT]", "BY[E]");
	

	public ExitCommand(String commandLine, CommandExecutor commandExecutor) {
		super(commandLine, commandExecutor);
	}
	

	public boolean saveToHistory() {
		return false;
	}

	
	public void printHelp() {
        logln(" EXIT");
        logln(" ----");
        logln("");
        logln(" Terminates this program and returns control to the operating system.");
        logln("");
        logln(" {EXIT|Q[UIT]|BY[E]}");
	}

	
	public void doExecute() throws Exception {
        Logger.setSpoolOff();
        setStopPrompt(true);
	}

}
