package dal.tool.cli.command;

import dal.tool.cli.CommandExecutor;
import dal.tool.cli.Logger;
import dal.tool.cli.Logger.Level;
import dal.tool.cli.SleepUiHooks;

/**
 * 지정 시간만큼 대기하는 SLEEP 명령
 * @author 권영달
 *
 */
public class SleepCommand extends Command {

	public static final CommandMeta commandMeta = new CommandMeta(SleepCommand.class, "SLEEP", "WAIT");

	public static final String CANCELLED_MESSAGE = "Sleep cancelled.";

	
	public SleepCommand(String commandLine, CommandExecutor commandExecutor) {
		super(commandLine, commandExecutor);
	}

	
	public boolean saveToHistory() {
		return true;
	}

	
	public void printHelp() {
        logln(" SLEEP");
        logln(" -----");
        logln("");
        logln(" Waits the prompt for a while.");
        logln("");
        logln(" {SLEEP|WAIT} milliseconds");
	}

	
	public void doExecute() throws Exception {
    	if(!checkArgument(1)) return;
        String first = commandArgs.getArgument(1);
        long sleepMs = 0L;
        try {
        	sleepMs = Long.parseLong(first);
        	if(sleepMs < 0L || sleepMs > Long.MAX_VALUE) {
        		logln(Level.ERROR, "Invalid argument range. It should be 0 < argument > " + Long.MAX_VALUE); 
        	}
        } catch(NumberFormatException nfe) {
        	logln(Level.ERROR, "Cannot convert the argument \"" + first + "\" to milliseconds.");
        	return;
        }
        if(sleepMs > 0L) {
            logln("Sleeping for " + sleepMs + " ms.");
            boolean completed;
            if(Logger.isTuiActive() && !ScriptCommand.isNestedScriptExecution() && SleepUiHooks.hasHandler()) {
            	completed = SleepUiHooks.waitForSleep(sleepMs);
            } else {
            	completed = waitWithoutTuiDialog(sleepMs);
            }
            if(!completed) {
            	logln(Level.INFO, CANCELLED_MESSAGE);
            }
        }
	}

	private static boolean waitWithoutTuiDialog(long sleepMs) {
		try {
			Thread.sleep(sleepMs);
			return true;
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

}
