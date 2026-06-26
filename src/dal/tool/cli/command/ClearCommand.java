package dal.tool.cli.command;

import dal.tool.cli.CommandExecutor;
import dal.tool.cli.Logger;

/**
 * 화면을 지우는 CLEAR/CLS 명령
 * @author 권영달
 *
 */
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
        Logger.clearScreenDisplay();
	}

	public boolean afterExecute() throws Exception {
		return true;
	}

	
}
