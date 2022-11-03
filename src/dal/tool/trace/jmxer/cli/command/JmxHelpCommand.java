package dal.tool.trace.jmxer.cli.command;

import java.lang.reflect.Constructor;

import dal.tool.cli.Logger.Level;
import dal.tool.cli.command.Command;
import dal.tool.cli.command.CommandMeta;
import dal.tool.cli.command.HelpCommand;
import dal.tool.trace.jmxer.cli.JmxCommandExecutor;
import dal.tool.trace.jmxer.cli.helper.ListArgumentsHelper;
import dal.tool.util.StringUtil;

public class JmxHelpCommand extends HelpCommand {

	public static final CommandMeta commandMeta = new CommandMeta(JmxHelpCommand.class, "HELP");

	
	public JmxHelpCommand(String commandLine, JmxCommandExecutor commandExecutor) {
		super(commandLine, commandExecutor);
	}


	public void printHelp() {
		commandArgs.setArguments(ListArgumentsHelper.stripQuotes(commandArgs, new char[]{'"','\''}));
		if(commandArgs == null) {
			super.printHelp();
		} else {
			String topic = commandArgs.getArgument(1);
			if(topic == null || topic.equalsIgnoreCase("help")) {
	        	super.printHelp();
	            logln("");
	            logln(" Following is the list of available JMX topic.");
	            logln("");
	            for(CommandMeta meta : ((JmxCommandExecutor)getCommandExecutor()).getCommandMetaList()) {
	            	if(meta != commandMeta && meta.getCommandMatchStrings().length > 0 && !meta.getCommandMatchStrings()[0].equals("SET")) {
	            		logln("   " + StringUtil.arrayToString(meta.getCommandMatchStrings(), " | "));
	            	}
	            }
	        } else {
				try {
		            for(CommandMeta meta : ((JmxCommandExecutor)getCommandExecutor()).getCommandMetaList()) {
		    			if(meta.isMatchCommand(topic)) {
	    					Constructor<? extends Command> cons = meta.getCommandClass().getConstructor(new Class[]{ String.class, JmxCommandExecutor.class });
	    					Command command = cons.newInstance(null, null);
	    					command.printHelp();
		    				return;
		    			}
		    		}	        	
		        	super.printHelp();
				} catch(Exception e) {
					logln(Level.ERROR, "Failed to print Help message : " + e.getMessage());
				}
	        }	            
		}
	}

	
	public void doExecute() throws Exception {
		printHelp();
	}

}
