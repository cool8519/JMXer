package dal.tool.cli.command;

import java.util.List;

import dal.tool.cli.CommandExecutor;
import dal.tool.cli.History;
import dal.tool.cli.util.IOUtil;
import dal.tool.trace.jmxer.cli.helper.ListArgumentsHelper;

public class HistoryCommand extends Command {

	public static final CommandMeta commandMeta = new CommandMeta(HistoryCommand.class, "HI[STORY]");


	public HistoryCommand(String commandLine, CommandExecutor commandExecutor) {
		super(commandLine, commandExecutor);
	}
	
	
	public boolean saveToHistory() {
		return false;
	}

	
	public void printHelp() {
        logln(" HISTORY");
        logln(" -------");
        logln("");
        logln(" Displays or Controls history of command");
        logln("");
        logln(" HI[STORY] [option]");
        logln("");
        logln(" Following is the list of available option.");
        logln("     clear - clear all command history");
	}

	
	public void doExecute() throws Exception {
		History history = getCommandExecutor().getHistory();
		commandArgs.setArguments(ListArgumentsHelper.stripQuotes(commandArgs, new char[]{'"','\''}));
		if(commandArgs.size() == 0) {
			if(history == null) {
	            logln("No reference history of command.");
				return;
			}
			List<Command> historyList = history.getHistoryList();
	        if(historyList.size() > 0) {
		        String line = "";
		        int digit = 0;
				logln("");
		        for(int i = 1; i <= historyList.size(); i++) {
		            line = (String)(historyList.get(i-1).getCommandLine());
		            digit = String.valueOf(i).length();
		            logln(IOUtil.getColumnString(null, 3-digit, " ", "", true) + String.valueOf(i) + ((i==historyList.size())?"* ":"  ") + line);
		        }
	        } else {
	            logln("No history of command.");
	        }
		} else if(commandArgs.getArgument(1).trim().equalsIgnoreCase("clear")) {
			history.clear();
			logln("Cleared history of command.");
		} else {
			logln("Invalid argument for history command.");
		}
	}

}
