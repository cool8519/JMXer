package dal.tool.cli.command;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;

import dal.tool.cli.CommandExecutor;
import dal.tool.cli.Logger.Level;

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
	}

	
	public void doExecute() throws Exception {
		List<String> tokens = commandArgs.getArguments();
        if(File.separator.equals("/")) {
        	tokens.add(0, "/bin/sh");
        	tokens.add(1, "-c");
        } else {
        	tokens.add(0, "cmd.exe");
        	tokens.add(1, "/C");
        }  
        String[] command = tokens.toArray(new String[] {});
        try {
    		logln("");
	    	logln("############################ Result of External-Process ##########################");
	        runExternalProcess(command);
        } catch(Exception e) {
        	throw e;
        } finally {
	    	logln("##################################################################################");
        }
	}


    /**
     * 외부 명령어를 수행하고 결과를 출력한다.<br/>
     * 별도의 프로세스가 기동되어 수행된다.
     * @param command 호출할 명령어와 argument 목록
     */
    public void runExternalProcess(String[] command) {
        BufferedReader isb = null;
        BufferedReader esb = null;
        try {
            String results;
            Process p = Runtime.getRuntime().exec(command);
            isb = new BufferedReader(new InputStreamReader(p.getInputStream()));
            esb = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            while((results=isb.readLine()) != null) {
                logln(results);
            }
            while((results=esb.readLine()) != null) {
                logln(results);
            }
        } catch(Exception e) {
            e.printStackTrace();
            logln(Level.ERROR, e.getMessage());
        } finally {
            if(isb != null)
                try { isb.close(); } catch(Exception e) {}
            if(esb != null)
                try { esb.close(); } catch(Exception e) {}
        }
    }

    
	public final boolean afterExecute() throws Exception {
        if(getSettings().showTimingOnExecute()) {
            logln("Command execute Finish : " + getElapsedTime() + " ms");
        }
        return true;
	}

}
