package dal.tool.cli.command;

import java.util.List;

import dal.tool.cli.CommandExecutor;
import dal.tool.cli.Logger.Level;
import dal.tool.util.StringUtil;

public class MultiCommand extends Command {

	public static final CommandMeta commandMeta = new CommandMeta(MultiCommand.class);

	protected List<String> commandLines = null;
	protected String idxPrefix = "";

	
	public MultiCommand(String commandLine, CommandExecutor commandExecutor) {
		super(commandLine, commandExecutor);
	}


	public MultiCommand(String commandLine, CommandExecutor commandExecutor, String idxPrefix) {
		super(commandLine, commandExecutor);
		this.idxPrefix = idxPrefix;
	}
	
	
	protected void parseCommandLine() {
    	try {
    		this.commandLines = StringUtil.getTokenList(commandLine, ';', StringUtil.QUOTES);
    		for(int i = 0; i < commandLines.size(); i++) {
    			commandLines.set(i, commandLines.get(i).trim());
    		}
    	} catch(Exception e) {
    		logln(Level.ERROR, e.getMessage());
    	} finally {
    		needToParse = false;
    	}
	}

	
	public boolean saveToHistory() {
		return true;
	}

	
	public void printHelp() {}


	public void doExecute() throws Exception {
        if(commandLines != null) {
        	getCommandExecutor().processMultiCommand(commandLines, idxPrefix);
        }
	}

}
