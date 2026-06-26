package dal.tool.cli.command;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dal.tool.cli.CommandExecutor;
import dal.tool.cli.ExternalUiHooks;
import dal.tool.cli.Logger;
import dal.tool.cli.Logger.Level;
import dal.tool.cli.ScriptUiHooks;
import dal.tool.trace.jmxer.cli.helper.ListArgumentsHelper;

/**
 * 스크립트 파일의 명령을 순차 실행하는 SCRIPT(@) 명령
 * @author 권영달
 *
 */
public class ScriptCommand extends Command {

	public static final CommandMeta commandMeta = new CommandMeta(ScriptCommand.class, "<", "@", "SCRIPT");
	public static final String CANCELLED_OUTPUT_MESSAGE = "Script execution was cancelled by the user.";
	
	protected static ThreadLocal<List<String>> threadLocal = new ThreadLocal<List<String>>();
	protected static ThreadLocal<Integer> scriptExecutionDepth = new ThreadLocal<Integer>();

	public static boolean isNestedScriptExecution() {
		Integer depth = scriptExecutionDepth.get();
		return depth != null && depth.intValue() > 0;
	}

	private void enterScriptExecution() {
		Integer depth = scriptExecutionDepth.get();
		int next = depth == null ? 1 : depth.intValue() + 1;
		scriptExecutionDepth.set(Integer.valueOf(next));
	}

	private void leaveScriptExecution() {
		Integer depth = scriptExecutionDepth.get();
		if(depth == null || depth.intValue() <= 1) {
			scriptExecutionDepth.remove();
		} else {
			scriptExecutionDepth.set(Integer.valueOf(depth.intValue() - 1));
		}
	}

	
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


	@Override
	public boolean beforeExecute() throws Exception {
		if(Logger.isTuiActive() && !ScriptCommand.isNestedScriptExecution() && ScriptUiHooks.hasHandler()) {
			if(!ScriptUiHooks.confirmBeforeScript(formatScriptFilesForConfirm())) {
				Logger.abortTuiUserCaptureSession();
				return false;
			}
		}
		return true;
	}

	String formatScriptFilesForConfirm() {
		if(commandArgs == null) {
			return "";
		}
		List<String> scriptNames = ListArgumentsHelper.stripQuotes(commandArgs, new char[]{'"','\''});
		if(scriptNames == null || scriptNames.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < scriptNames.size(); i++) {
			if(i > 0) {
				sb.append('\n');
			}
			sb.append(scriptNames.get(i));
		}
		return sb.toString();
	}


	public void doExecute() throws Exception {
		if(!checkArgument(1, -1)) return;
		boolean useTuiScriptProgress = Logger.isTuiActive() && ScriptUiHooks.hasHandler()
			&& !ScriptCommand.isNestedScriptExecution();
		boolean scriptCompletedFully = true;
		boolean scriptCancelled = false;
		enterScriptExecution();
		try {
			if(useTuiScriptProgress) {
				ScriptUiHooks.beginScript(getCommandLine());
			}
			commandArgs.setArguments(ListArgumentsHelper.stripQuotes(commandArgs, new char[]{'"','\''}));
			List<String> scriptNames = commandArgs.getArguments();
	    	List<String> visitedScript; 
	        for(String fname : scriptNames) {
            if(ScriptUiHooks.isCancelRequested()) {
            	scriptCancelled = true;
            	scriptCompletedFully = false;
            	break;
            }
            if(fname.length() > 0) {
            	List<String> scriptCmdLines = null;
                File scriptFile = new File(fname);
                String absScriptPath = null;
                boolean suspendTerminalUi = !Logger.isTuiActive();
                try {
                	if(suspendTerminalUi) {
                		ExternalUiHooks.runBeforeBlockingExternalProcess();
                	}
	                if(scriptFile.exists() && scriptFile.isFile()) {
	                	absScriptPath = scriptFile.getCanonicalPath();
	                	visitedScript = threadLocal.get();
	                	if(visitedScript == null) {
	                		visitedScript = new ArrayList<String>(); 
	                	}
	                	if(visitedScript.contains(absScriptPath)) {
		                    logln(Level.ERROR, "Recurrsive script is founded. Already processed script file \"" + fname + "\". The script is aborted.");
		                    setStopCommand(true);
		                    scriptCompletedFully = false;
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
	                    scriptCompletedFully = false;
	                }
                } catch(Exception e) {
                	logln(Level.ERROR, "Failed to read script file \"" + fname + "\".");
                	scriptCompletedFully = false;
                } finally {
                	if(suspendTerminalUi) {
                		ExternalUiHooks.runAfterBlockingExternalProcess();
                	}
                }	                
                if(scriptCmdLines != null) {
                	if(!getCommandExecutor().processScriptCommand(fname, scriptCmdLines)) {
                		scriptCompletedFully = false;
                		if(ScriptUiHooks.isCancelRequested()) {
                			scriptCancelled = true;
                		}
                		break;
                	}
                }
                if(ScriptUiHooks.isCancelRequested()) {
                	scriptCancelled = true;
                	scriptCompletedFully = false;
                	break;
                }
            } else {
            	logln("Filename cannot be empty.");
            	scriptCompletedFully = false;
            }
            threadLocal.remove();
        }
	        if(isStopCommand()) {
	        	scriptCompletedFully = false;
	        }
		} finally {
			if(useTuiScriptProgress) {
				if(scriptCancelled) {
					logln(Level.INFO, "\n" + CANCELLED_OUTPUT_MESSAGE);
				}
				ScriptUiHooks.finishScript(scriptCompletedFully && !scriptCancelled, scriptCancelled);
			}
			leaveScriptExecution();
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
