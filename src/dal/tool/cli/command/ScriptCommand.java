package dal.tool.cli.command;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dal.tool.cli.CommandExecutor;
import dal.tool.cli.Logger.Level;

public class ScriptCommand extends Command {

	public static final CommandMeta commandMeta = new CommandMeta(ScriptCommand.class, "<", "@", "SCRIPT");
	
	protected static ThreadLocal<List<String>> threadLocal = new ThreadLocal<List<String>>();

	
	public ScriptCommand(String commandLine, CommandExecutor commandExecutor) {
		super(commandLine, commandExecutor);
	}

	
	public boolean saveToHistory() {
		return true;
	}

	
	public void printHelp() {
        logln(" @ (SCRIPT)");
        logln(" ----------");
        logln("");
        logln(" Executes the script files included commands.");
        logln(" Use the name of file with '@' at the command prompt.");
        logln(" One or more script files can be specified with space delimiters.");
        logln("");
        logln(" {<|@|SCRIPT} [file_name[.ext]]");
	}


	public void doExecute() throws Exception {
		if(!checkArgument(1, -1)) return;
		List<String> scriptNames = commandArgs.getArguments();
    	List<String> visitedScript; 
        for(String fname : scriptNames) {
            if(fname.length() > 0) {
            	List<String> scriptCmdLines = null;
                File scriptFile = new File(fname);
                String absScriptPath = null;
                try {
	                if(scriptFile.exists() && scriptFile.isFile()) {
	                	absScriptPath = scriptFile.getCanonicalPath();
	                	visitedScript = threadLocal.get();
	                	if(visitedScript == null) {
	                		visitedScript = new ArrayList<String>(); 
	                	}
	                	if(visitedScript.contains(absScriptPath)) {
		                    logln(Level.ERROR, "Recurrsive script is founded. Already processed script file \"" + fname + "\". The script is aborted.");
		                    setStopCommand(true);
		                    break;
	                	} else {
		                	visitedScript.add(absScriptPath);
		                	threadLocal.set(visitedScript);
	                	}
	                	logln("Found script file \"" + absScriptPath + "\"");
	                	logln("");
	                	scriptCmdLines = readScript(scriptFile);
	                } else {
	                    logln(Level.ERROR, "Cannot find script file \"" + fname + "\".");
	                }
                } catch(Exception e) {
                	logln(Level.ERROR, "Failed to read script file \"" + fname + "\".");
                }	                
                if(scriptCmdLines != null) {
                	if(!getCommandExecutor().processScriptCommand(fname, scriptCmdLines)) {
                		break;
                	}
                }
            } else {
            	logln("Filename cannot be empty.");
            }
            threadLocal.remove();
        }
	}


    /**
     * 스크립트 파일을 라인 단위로 읽어들인다.
     * @param f 스크립트 파일 객체
     * @return 명령어 목록
     */
    protected List<String> readScript(File f) {
        List<String> l = new ArrayList<String>();
        FileReader fr = null;
        BufferedReader br = null;
        try {
            fr = new FileReader(f);
            br = new BufferedReader(fr);
            String s = null;
            while((s = br.readLine()) != null) {
                if(s.trim().length() > 0) {
                    s = s.trim();
                    l.add(s);
                }
            }
        } catch(IOException ioe) {
        } finally {
            if(br != null)
                try { br.close(); } catch(Exception e) {}
        }
        return l;
    }

    
	public final boolean afterExecute() throws Exception {
        if(getSettings().showTimingOnExecute()) {
            logln("Script execute Finish : " + getElapsedTime() + " ms");
        }
		threadLocal.remove();
		return true;
	}

}
