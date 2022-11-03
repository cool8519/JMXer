package dal.tool.cli.command;

import java.util.List;

import dal.tool.cli.CommandExecutor;
import dal.tool.cli.ListArguments;
import dal.tool.cli.Logger;
import dal.tool.cli.Logger.Level;
import dal.tool.cli.Settings;


public abstract class Command {

	protected String commandLine = null;
	protected CommandExecutor commandExecutor = null;
	protected ListArguments commandArgs = null;
	protected long startTime = 0L;
	protected long endTime = 0L;
	protected boolean stopPrompt = false;
	protected boolean stopCommand = false;
	protected boolean needToParse = true;


	public String toString() {
		return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());
	}
	
	
	/**
	 * Help 메시지를 출력한다.
	 */
	public abstract void printHelp();

	
	/**
	 * 명령어에 대한 동작을 수행한다.
	 * @throws Exception 수행 과정에서 발생한 모든 Exception
	 */
	public abstract void doExecute() throws Exception;
	

	/**
	 * 명령어 수행 후 히스토리에 남길지 여부를 확인한다.
	 * @return 히스토리에 남기려면 true
	 */
	public abstract boolean saveToHistory();
	
	
	/**
	 * 명령어 객체를 생성한다.
	 * @param commandLine 명령어
	 * @param commandExecutor 수행한 CommandExecutor 구현 객체
	 */
	protected Command(String commandLine, CommandExecutor commandExecutor) {
    	this.commandLine = commandLine;
    	this.commandExecutor = commandExecutor;
    }

	
	/**
	 * 인자값의 수를 확인한다.
	 * @param min 최소 인자값 수
	 * @param max 최대 인자값 수. -1이면 무제한
	 * @return min보다 작거나 max보다 크면 false
	 */
	protected boolean checkArgument(int min, int max) {
		if(commandArgs.size() < min) {
			logln("Need more argument.");
			return false;
		} else if(max > -1 && commandArgs.size() > max) {
    		logln("Too many arguments.");
    		return false;
    	}
		return true;
	}

	
	/**
	 * 인자값의 수를 확인한다.
	 * @param count 인자값 수
	 * @return count와 같지 않으면 false
	 */
	protected boolean checkArgument(int count) {
		return checkArgument(count, count);
	}

	
	/**
	 * 명령어 구문을 파싱하여 Argument 객체를 생성한다.<br/>
	 * 일반적으로 Command 객체가 생성될때 수행되나, 명령어 구문이 변경되면 수행시 이 메소드가 다시 수행된다. 
	 */
	protected void parseCommandLine() {
		if(commandLine != null && commandLine.endsWith(";")) {
			commandLine = commandLine.substring(0, commandLine.length()-1);
		}
    	try {
    		this.commandArgs = new ListArguments(commandLine);
    	} catch(Exception e) {
    		logln(Level.ERROR, e.getMessage());
    	} finally {
    		needToParse = false;
    	}
	}
	
	
	/**
	 * 명령어 문자열을 리턴한다.
	 * @return 멸령어 문자열
	 */
	public String getCommandLine() {
		return commandLine;
	}
	
	
	/**
	 * 명령어 문자열을 저장한다.
	 * @param commandLine 명령어 문자열
	 */
	public void setCommandLine(String commandLine) {
		this.commandLine = commandLine;
		this.needToParse = true;
	}
	
	
	/**
	 * 명령어 수행기 객체를 가져온다.
	 * @return CommandExecutor 구현 객체
	 */
	public CommandExecutor getCommandExecutor() {
		return commandExecutor;
	}
	
	
	/**
	 * 프롬프트 환경설정을 가져온다.
	 * @return 환경설정 객체
	 */
	public Settings getSettings() {
		return commandExecutor.getSettings();
	}
	
	
	/**
	 * 명령어 수행기를 설정한다.
	 * @param commandExecutor CommandExecutor 구현 객체
	 */
	public void setCommandExecutor(CommandExecutor commandExecutor) {
		this.commandExecutor = commandExecutor;
	}
	
	
	/**
	 * 주어진 명령어와 인자값에 맞는 동작을 수행한다.
	 * @throws Exception 명령어 수행중 발생한 모든 Exception
	 */
	public void execute() throws Exception {
		if(needToParse) {
			parseCommandLine();
		}
		
		if(commandLine != null && commandLine.trim().length() > 0) {
	        logln(Level.DEBUG, "------------------------------------------------");
			logln(Level.DEBUG, "COMMAND    : " + getClass().getSimpleName());
			logln(Level.DEBUG, "CMDLINE    : " + commandLine);
			if(commandArgs != null) {
				List<String> argList = commandArgs.getArguments();
		    	if(argList.size() > 0 && Logger.isShowLevel(Level.DEBUG)) {
			        for(int i = 0; i < argList.size(); i++) {
			            logln(Level.DEBUG, "CMDARGS[" + (i+1) + "] : " + argList.get(i));
			        }
		    	}
			}
	        logln(Level.DEBUG, "------------------------------------------------");
		}
		
        if(isStopCommand()) return;
        
        try {
        	if(!this.beforeExecute()) {
        		return;
        	}
        } catch(Exception e) {
        	logln(Level.ERROR, "Error occurred during beforeExecute.");
        }

        if(isStopCommand()) return;

        try {
			startTime = System.currentTimeMillis();
			if(commandArgs != null && !(this instanceof NoopCommand) && !(this instanceof HelpCommand) && commandArgs.size() == 1 && commandArgs.getArgument(1).equalsIgnoreCase("help")) {
				logln("");
				this.printHelp();
				logln("");
			} else {
				this.doExecute();
			}
		} catch(Exception ex) {
			throw ex;
		} finally {
			endTime = System.currentTimeMillis();
		}

        if(isStopCommand()) return;

        try {
        	this.afterExecute();
        } catch(Exception e) {
        	logln(Level.ERROR, "Error occurred during afterExecute.");
        }
	}

	
	/**
	 * 명령어 수행 후 프롬프트를 중지할 지 여부를 설정한다.
	 * @param flag 프롬프트를 중지하려면 true.
	 */
	public void setStopPrompt(boolean flag) {
		this.stopPrompt = flag;
	}
	
	
	/**
	 * 명령어 수행 후 프롬프트를 중지할 지 여부를 확인한다.
	 * @return 프롬프트가 중지 되어야 하면 true.
	 */
	public boolean isStopPrompt() {
		return stopPrompt;
	}
	

	/**
	 * 명령어 수행을 중지할 지 여부를 설정한다.
	 * @param flag 명령어를 중지하려면 true.
	 */
	public void setStopCommand(boolean flag) {
		this.stopCommand = flag;
	}
	
	
	/**
	 * 명령어 수행을 중지할 지 여부를 확인한다.
	 * @return 명령어가 중지 되어야 하면 true.  
	 */
	public boolean isStopCommand() {
		return stopCommand;
	}

	
	/**
	 * 수행시간을 가져온다. (단위:ms)
	 * @return 명령어 수행이 완료된 경우는 응답시간, 수행되지 않았거나 미완료 된 경우는 -1
	 */
	public long getElapsedTime() {
		if(startTime > 0L && endTime > 0L) {
			return endTime - startTime;
		} else {
			return -1L;
		}
	}
	
	
	/**
	 * 명령어 수행 전에 호출된다.
	 * @return 수행에 성공하면 true. false이면 명령어를 수행하지 않는다.
	 */
	public boolean beforeExecute() throws Exception {
		return true;
	}

	
	/**
	 * 명령어 수행 후에 호출된다.
	 * @return 수행에 성공하면 true.
	 */
	public boolean afterExecute() throws Exception {
		return true;
	}

	
    /**
     * 로그를 기록한다. 마지막에 newline 문자를 추가한다.
     * @param msg 로그 메시지
     */
    protected void logln(Object msg) {
    	Logger.logln(dal.tool.cli.Logger.Level.RESULT, msg);
    }


    /**
     * 로그를 기록한다. 마지막에 newline 문자를 추가한다.
     * @param level 로그 레벨
     * @param msg 로그 메시지
     */
    protected void logln(Level level, Object msg) {
    	Logger.logln(level, msg);
    }

}