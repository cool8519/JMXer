package dal.tool.cli.command;

import dal.tool.cli.CommandExecutor;
import dal.tool.cli.History;


/**
 * 히스토리에서 이전 명령을 다시 실행하는 PREVIOUS 명령
 * @author 권영달
 *
 */
public class PreviousCommand extends Command {

	public static final CommandMeta commandMeta = new CommandMeta(PreviousCommand.class, "/", "PRE[VIOUS]");

	public static final String SCRIPT_NOT_AVAILABLE_MESSAGE =
		"PREVIOUS is not available inside SCRIPT.";


	public PreviousCommand(String commandLine, CommandExecutor commandExecutor) {
		super(commandLine, commandExecutor);
	}
	
	
	public boolean saveToHistory() {
		return false;
	}

	
	public void printHelp() {
        logln(" PREVIOUS");
        logln(" --------");
        logln("");
        logln(" Executes the most recently executed command.");
        logln(" Use '/' at the command prompt in command line.");
        logln("");
        logln(" {/|PRE[VIOUS]} [histoy_number]");
	}

	
	public void doExecute() throws Exception {
		Command command = null;
		History history = getCommandExecutor().getHistory();
		if(commandArgs.size() == 0) {
			if(history.size() > 0) {
				command = history.getLast();
			} else {
	            logln("No lines in previous command buffer.");
	            return;
	        }
		} else {
			int historyIdx = -1;
			try {
				historyIdx = Integer.parseInt(commandArgs.nextArgument());
				if(historyIdx < 1 || historyIdx > history.size()) {
					logln("Invalid history range.");
					return;
				}
				command = history.get(historyIdx);
			} catch(NumberFormatException nfe) {
				logln("Invalid histroy number.");
			}
		}
		if(command != null) {
			command.setStopCommand(false);
			command.setStopPrompt(false);
			if(command.commandArgs != null) {
				command.commandArgs.toFirstIndex();
			}
			command.execute();
		}
	}

}
