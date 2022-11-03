package dal.tool.cli.command;

import dal.tool.cli.CommandExecutor;
import dal.tool.cli.util.IOUtil;

public class ClearCommand extends Command {

	public static final CommandMeta commandMeta = new CommandMeta(ClearCommand.class, "CLEAR", "CLS");

	
	public ClearCommand(String commandLine, CommandExecutor commandExecutor) {
		super(commandLine, commandExecutor);
	}

	
	public boolean saveToHistory() {
		return false;
	}

	
	public void printHelp() {
        logln(" CLEAR");
        logln(" -----");
        logln("");
        logln(" Clear the current display screen.");
        logln("");
        logln(" {CLEAR|CLS}");
	}

	
	public void doExecute() throws Exception {
        IOUtil.clearScreen();
	}

	public boolean afterExecute() throws Exception {
		return true;
	}

	
}
