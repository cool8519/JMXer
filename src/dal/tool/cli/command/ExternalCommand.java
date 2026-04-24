package dal.tool.cli.command;

import java.io.File;
import java.util.List;

import dal.tool.cli.CommandExecutor;
import dal.tool.cli.Logger.Level;
import dal.tool.cli.util.ExternalProcessRunner;
import dal.tool.cli.util.ExternalProcessRunner.Sink;

public class ExternalCommand extends Command {

	public static final CommandMeta commandMeta = new CommandMeta(ExternalCommand.class, "!", "EXT[ERNAL]");

	
	public ExternalCommand(String commandLine, CommandExecutor commandExecutor) {
		super(commandLine, commandExecutor);
	}

	
	public boolean saveToHistory() {
		return true;
	}

	
	public void printHelp() {
        logln(" EXTERNAL");
        logln(" --------");
        logln("");
        logln(" Executes external(OS) command.");
        logln(" Use the command line with '!' at the command prompt in command line.");
        logln("");
        logln(" {!|EXT[ERNAL]} [command_line]");
        logln("");
        logln(" The command line is passed to the system shell as a single line (same as typing it in a shell).");
        logln(" On Unix, /bin/sh runs the line. On Windows, cmd runs the line.");
	}

	
	public void doExecute() throws Exception {
		List<String> tokens = commandArgs.getArguments();
		if (!checkArgument(1, -1)) {
			return;
		}
		String joined = joinTokens(tokens);
		boolean unix = File.separatorChar == '/';
		try {
    		logln("");
	    	logln("############################ Result of External-Process ##########################");
	    	final ExternalCommand self = this;
	    	Sink sink = new Sink() {
				@Override
				public void line(String s) {
					self.logln(s);
				}
				@Override
				public void logError(String message) {
					self.logln(Level.ERROR, message);
				}
	    	};
	    	int exit = ExternalProcessRunner.run(joined, unix, sink);
	    	if (exit != 0) {
	    		logln(Level.ERROR, "External process exited with code " + exit);
	    	}
        } finally {
	    	logln("##################################################################################");
        }
	}

	private static String joinTokens(List<String> tokens) {
		if (tokens == null || tokens.isEmpty()) {
			return "";
		}
		return String.join(" ", tokens);
	}

    
	public final boolean afterExecute() throws Exception {
        if(getSettings().showTimingOnExecute()) {
            logln("Command execute Finish : " + getElapsedTime() + " ms");
        }
        return true;
	}

}
