package dal.tool.cli.command;

import java.lang.reflect.Constructor;

import dal.tool.cli.CommandExecutor;
import dal.tool.cli.Logger.Level;
import dal.tool.util.StringUtil;

public class HelpCommand extends Command {

	public static final CommandMeta commandMeta = new CommandMeta(HelpCommand.class, "HELP");

	
	public HelpCommand(String commandLine, CommandExecutor commandExecutor) {
		super(commandLine, commandExecutor);
	}


	public boolean saveToHistory() {
		return true;
	}

	
	public void printHelp() {
		if(commandArgs == null) {
			return;
		} else {
			String topic = commandArgs.getArgument(1);
			if(topic == null || topic.equalsIgnoreCase("help")) {
		        logln(" HELP");
		        logln(" ----");
		        logln("");
		        logln(" Accesses this command line help system. Enter HELP INDEX for a list of topics.");
		        logln("");
		        logln(" HELP [topic]");
	            logln("");
	            logln(" Following is the list of available common topic.");
	            logln("");	            
	            for(CommandMeta meta : getCommandExecutor().getBaseCommandMetaList()) {
	            	if(meta != commandMeta && meta.getCommandMatchStrings().length > 0) {
	            		logln("   " + StringUtil.arrayToString(meta.getCommandMatchStrings(), " | "));
	            	}
	            }
	        } else {
				try {
		            for(CommandMeta meta : getCommandExecutor().getBaseCommandMetaList()) {
		    			if(meta.isMatchCommand(topic)) {
	    					Constructor<? extends Command> cons = meta.getCommandClass().getConstructor(new Class[]{ String.class, CommandExecutor.class });
	    					Command command = cons.newInstance(null, null);
	    					command.printHelp();
		    				return;
		    			}
		    		}	        	
		            logln("No HELP available.");
				} catch(Exception e) {
					logln(Level.ERROR, "Failed to print Help message : " + e.getMessage());
				}
	        }
		}
	}

	
	public void doExecute() throws Exception {
		printHelp();
	}

	
	public boolean beforeExecute() throws Exception {
		logln("");
		return true;
	}

	
	public boolean afterExecute() throws Exception {
		logln("");
		return true;
	}

}
