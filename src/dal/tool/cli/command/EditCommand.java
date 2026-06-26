package dal.tool.cli.command;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import dal.tool.cli.CommandExecutor;
import dal.tool.cli.ExternalUiHooks;
import dal.tool.cli.History;
import dal.tool.cli.Logger.Level;


/**
 * 외부 에디터로 명령 줄을 편집하는 EDIT 명령
 * @author 권영달
 *
 */
public class EditCommand extends Command {

	public static final CommandMeta commandMeta = new CommandMeta(EditCommand.class, "ED[IT]");

	public static final String TUI_NOT_AVAILABLE_MESSAGE =
		"EDIT is not available in TUI Interface Mode.";

	public static final String SCRIPT_NOT_AVAILABLE_MESSAGE =
		"EDIT is not available inside SCRIPT.";


	public EditCommand(String commandLine, CommandExecutor commandExecutor) {
		super(commandLine, commandExecutor);
	}
	

	public boolean saveToHistory() {
		return false;
	}
	
	
	public void printHelp() {
        logln("");
        logln(" EDIT");
        logln(" ----");
        logln("");
        logln(" Invokes a host operating system text editor to edit the history command.");
        logln(" The saved command can be executed by executing previous(/) command after the editor exits.");
        logln("");
        logln(" ED[IT] [histoy_number]");
	}

	
	public void doExecute() throws Exception {
        if(File.separator.equals("/")) {
            logln("Not implemented on unix.");
            return;
        }

		Command command = null;
		History history = getCommandExecutor().getHistory();
		if(commandArgs.size() == 0) {
			if(history.size() > 0) {				
				command = history.getLast();
			} else {
	            logln("Nothing to edit in previous command buffer.");
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
	        String savedCommandLine = editText(command.getCommandLine());
	        if(savedCommandLine != null) {
	        	command.setCommandLine(savedCommandLine);
	        }				
		}
	}

	
    public String editText(String buffer) {
        try {
            String fname = ".peedit.buf";
            String tmpdir = System.getProperty("java.io.tmpdir");
            String cmd = (File.separator.equals("/"))?"vi":"notepad";
            String abs_fname = tmpdir + File.separator + fname;
            File f = new File(abs_fname);

            BufferedWriter bw = null;
            try {
	            bw = new BufferedWriter(new FileWriter(f));
	            buffer = buffer.replaceAll("\r\n", "\n").replaceAll("\n", "\r\n");
	            bw.write(buffer);
            } catch(Exception e) {
            	throw e;
            } finally {
	            if(bw != null) try { bw.close(); } catch(IOException ioe) {}
            }

            try {
            	ExternalUiHooks.runBeforeBlockingExternalProcess();
	            Process p = Runtime.getRuntime().exec(cmd + " " + abs_fname);
	            p.waitFor();	
	            logln(Level.INFO, "Wrote file \"" + fname + "\"");
            } catch(Exception e) {
            	throw e;
            } finally {
            	ExternalUiHooks.runAfterBlockingExternalProcess();
            }

            StringBuffer result = new StringBuffer("");
            BufferedReader br = null;
            try {
	            br = new BufferedReader(new FileReader(f));
	            String s = null;
	            while((s = br.readLine()) != null) {
	            	result.append(s).append("\n");
	            }
	            return result.toString().trim();
            } catch(Exception e) {
            	throw e;
            } finally {
	            if(br != null) try { br.close(); } catch(IOException ioe) {}
	            if(f.exists()) try { f.delete(); } catch(Exception e) {}
            }
        } catch(Exception e) {
            logln(Level.ERROR, "Failed to get text by editor : " + e.getMessage());
        }
        return null;
    }
    
}
