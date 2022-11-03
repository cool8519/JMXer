package dal.tool.cli;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import dal.tool.cli.Logger.Level;
import dal.tool.cli.command.ClearCommand;
import dal.tool.cli.command.Command;
import dal.tool.cli.command.CommandMeta;
import dal.tool.cli.command.EditCommand;
import dal.tool.cli.command.ExitCommand;
import dal.tool.cli.command.ExternalCommand;
import dal.tool.cli.command.HelpCommand;
import dal.tool.cli.command.HistoryCommand;
import dal.tool.cli.command.MultiCommand;
import dal.tool.cli.command.NoopCommand;
import dal.tool.cli.command.PreviousCommand;
import dal.tool.cli.command.ScriptCommand;
import dal.tool.cli.command.SettingCommand;
import dal.tool.cli.command.SleepCommand;
import dal.tool.cli.command.SpoolCommand;
import dal.tool.cli.util.IOUtil;
import dal.tool.util.StringUtil;


/**
 * 텍스트 기반 Client의 프롬프트 모드 수행을 담당하는 추상클래스
 * @author 권영달
 *
 */
public abstract class CommandExecutor {

	private static List<CommandMeta> baseCommandMetaList = new ArrayList<CommandMeta>();
	static {
		baseCommandMetaList.add(HelpCommand.commandMeta);
		baseCommandMetaList.add(NoopCommand.commandMeta);
		baseCommandMetaList.add(MultiCommand.commandMeta);
		baseCommandMetaList.add(ExitCommand.commandMeta);
		baseCommandMetaList.add(ClearCommand.commandMeta);
		baseCommandMetaList.add(HistoryCommand.commandMeta);
		baseCommandMetaList.add(EditCommand.commandMeta);
		baseCommandMetaList.add(SleepCommand.commandMeta);
		baseCommandMetaList.add(SettingCommand.commandMeta);
		baseCommandMetaList.add(PreviousCommand.commandMeta);
		baseCommandMetaList.add(ExternalCommand.commandMeta);
		baseCommandMetaList.add(ScriptCommand.commandMeta);
		baseCommandMetaList.add(SpoolCommand.commandMeta);
	}

	private boolean loop = true;
	protected String prompt;
	protected String commandLine;
	protected History commandHistory;
	protected Settings settings = null;

	
	/**
	 * 명령어 수행을 위한 인스턴스를 생성한다.
	 */
	public CommandExecutor() {
		this.prompt = "";
		this.commandHistory = new History();
		this.settings = new Settings();
	}


	/**
	 * 기본 명령어 메타정보를 가져온다.
	 * @return 기본 명령어 메타정보 Set
	 */
	public List<CommandMeta> getBaseCommandMetaList() {
		return baseCommandMetaList;
	}
	
	
	/**
	 * 프롬프트 환경설정을 가져온다.
	 * @return 환경설정 객체
	 */
	public Settings getSettings() {
		return settings;
	}
	

	/**
	 * 입력받은 명령어에 맞게 수행할 명령어를 가져온다.
	 * @param commandLine 명령어
	 * @return 수행할 명령어 객체
	 * @throws 수행과정에서 발생한 모든 Exception
	 */
	protected abstract Command getCommand(String commandLine) throws Exception;

	
	/**
	 * 프롬프트 모드가 아닐 때, 허용되는 명령어인지 확인한다.<br/>
	 * 구현체에서 특정 Command를 제한하거나 허용할 수 있다.
	 * 해당 Executor에서 구현한 명령어가 아닌 경우는 반드시 false를 리턴해야 한다.
	 * @param command 명령어 객체
	 * @return 허용되는 명령어이면 true. 해당 Executor에서 구현한 명령어가 아니거나 금지된 명령어이면 false.
	 */
	protected abstract boolean isAllowedInNonPrompt(Command command);
	
	
	/**
	 * 명령어 히스토리를 가져온다.
	 * @return History 객체
	 */
	public History getHistory() {
		return commandHistory;
	}
	

	/**
	 * 프롬프트 문자열을 설정한다.
	 * @param prompt 프롬프트 문자열
	 */
	public void setPrompt(String prompt) {
		this.prompt = prompt;
	}

	
	/**
	 * 명령어 히스토리 최대 저장개수를 설정한다.
	 * @param size 최대 저장개수
	 */
	public void setMaxHistory(int size) {
		commandHistory.setMaxSize(size);
	}

	
	/**
	 * 프롬프트 모드를 수행한다.<br/>
	 * 프롬프트를 출력하고 명령어를 입력받아 결과를 출력한다.  
	 */
	public void startPrompt() {
        try {
            String tm = "";
            while(loop) {
            	tm = settings.showTimeInPrompt() ? (IOUtil.getCurrentTime()+" ") : "";
                commandLine = IOUtil.readLine(tm + prompt + "> ", Level.RESULT).trim();
                if(commandLine.trim().length() > 0) {
                	processCommand(commandLine);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
            logln(Level.ERROR, e.getMessage());
        }        
	}

	
	/**
	 * 프롬프트 모드를 수행한다.<br/>
	 * 프롬프트를 출력하고 명령어를 입력받아 결과를 출력한다.
	 * @param prompt 프롬프트 문자열
	 */
	public void startPrompt(String prompt) {
		setPrompt(prompt);
		startPrompt();
	}

	
	/**
	 * 프롬프트 모드를 종료한다.
	 */
	public void stopPrompt() {
		loop = false;
	}
	

	/**
	 * 명령어를 수행한다.
	 * @param commandLine 명령어
	 */
	protected void processCommand(String commandLine) {
        Command command = getCommandInternal(commandLine);
		try {
			command.execute();
		} catch(Exception e) {
			logln(Level.ERROR, "Error occurred during executing command " + command.toString() + " : " + e.getMessage());
		}
		if(!(command instanceof NoopCommand)) {
			logln("");
		}
		if(command.isStopPrompt()) {
			loop = false;
		}
    	commandHistory.add(command);
	}

	
	/**
	 * 프롬프트 모드가 아닐 때, 명령어를 수행한다.
	 * @param commandLine 명령어
	 */
	public void processCommandInNonPrompt(String commandLine) {
        Command command = getCommandInternal(commandLine);
        if(isAllowedInNonPrompt(command) || isAllowedBaseInNonPrompt(command)) {
			try {
				command.execute();
			} catch(Exception e) {
				logln(Level.ERROR, "Error occurred during executing command " + command.toString() + " : " + e.getMessage());
			}
			if(!(command instanceof NoopCommand)) {
				logln("");
			}
        } else {
        	logln("Not allowed command in non-prompt mode.");
        }
	}
	
	
	/**
	 * 입력받은 명령어 목록을 읽어 명령어를 수행한다.
	 * @param multiCommandLines 명령어 문자열 목록
	 * @param idxPrefix depth를 나타내기 위한 index prefix 문자열
	 */
    public void processMultiCommand(List<String> multiCommandLines, String idxPrefix) {
    	int i = 1;
    	logln("");
        for(String cmdLine : multiCommandLines) {
        	if(!cmdLine.equals("")) {
	            Command command = getCommandInternal(cmdLine, idxPrefix+i+"-");
            	logln("[" + idxPrefix + i + "] " + command.getCommandLine());
	            if(command instanceof NoopCommand && command.getCommandLine().startsWith("--")) {
	            	break;
	            }
	    		try {
	    			command.execute();
	    		} catch(Exception e) {
	    			logln(Level.ERROR, "Error occurred during executing command : " + command.toString());
	    		}
	    		if(command.isStopPrompt()) {
	    			return;
	    		}
	        	logln("");
	        	if(i+1 <= multiCommandLines.size() && !multiCommandLines.get(i).equals("")) {
	        		logln("");
	        	}
	        	i++;
        	}
        }
    }


	/**
	 * 입력받은 스크립트 명령어 목록을 읽어 명령어를 수행한다.
	 * @param scriptFileName 스크립트 파일명
	 * @param scriptCommandLines 스크립트 명령어 문자열 목록
	 * @return 스크립트 성공 여부. 다음 스크립트를 계속 수행하려면 true.
	 */
    public boolean processScriptCommand(String scriptFileName, List<String> scriptCommandLines) {        
        for(String cmdLine : scriptCommandLines) {
            String tm = settings.showTimeInPrompt() ? (IOUtil.getCurrentTime()+" ") : "";
            logln(tm + prompt + ".SCRIPT@'" + scriptFileName + "'> " + cmdLine);
            Command command = getCommandInternal(cmdLine);
    		try {
    			command.execute();
    		} catch(Exception e) {
    			logln(Level.ERROR, "Error occurred during executing command : " + command.toString());
    		}
    		if(command.isStopPrompt()) {
    			return false;
    		}
        	logln("");
        }
        return true;
    }
	

	private Command getCommandInternal(String commandLine) {
		return getCommandInternal(commandLine, "");
	}
	
	
	/**
	 * 입력받은 명령어에 맞게 수행할 명령어를 가져온다.<br/>
	 * 구현체의 getCommand()를 우선 확인한다.
	 * @param commandLine 명령어
	 * @return 수행할 명령어 객체
	 */
	private Command getCommandInternal(String commandLine, String multiCommandIdxPrefix) {
        if(commandLine.trim().length() < 1) {
            return new NoopCommand(commandLine, this);
        }
		List<String> cmdLines = StringUtil.getTokenList(commandLine, ';', StringUtil.QUOTES);
		StringUtil.removeEmptyString(cmdLines);
		if(cmdLines.size() > 1 || cmdLines.get(0).trim().indexOf(';') > -1) {
			return new MultiCommand(commandLine, this, multiCommandIdxPrefix);
		} else {
	        Command command = null;
	        try {
	        	command = getCommand(commandLine);
	        } catch(Exception e) {
				logln(Level.ERROR, "Failed to get command : " + e.getMessage());
				return new NoopCommand(null, null);
	        }
	        
	        if(command == null) {
	            if(commandLine.startsWith("@") || commandLine.startsWith("!") || commandLine.startsWith(">") || commandLine.startsWith("<")) {
	            	commandLine = commandLine.charAt(0) + " " + commandLine.substring(1);
	            }

	            String first = commandLine.trim().split(" ")[0];

    			for(CommandMeta meta : baseCommandMetaList) {
    				if(meta.isMatchCommand(first)) {
    					try {
	    					Constructor<? extends Command> cons = meta.getCommandClass().getConstructor(new Class[]{ String.class, CommandExecutor.class });
	    					command = cons.newInstance(commandLine, this);
    					} catch(Exception e) {
    						logln(Level.ERROR, "Failed to create command '" + meta.getCommandClass().getSimpleName() + "' : " + e.getMessage());
    					}
    					break;
    				}
    			}
	        }
	        return (command==null) ? new NoopCommand(commandLine, this) : command;
		}
    }


	/**
	 * 프롬프트 모드가 아닐 때, 허용되는 기본 명령어인지 확인한다.
	 * @param command 명령어 객체
	 * @return 허용되는 명령어이면 true. 기본 명령어가 아니거나 금지된 명령어이면 false.
	 */
	private boolean isAllowedBaseInNonPrompt(Command command) {
		if(!containsCommand(baseCommandMetaList, command)) {
			return false;
		}
		if(command instanceof NoopCommand) {
			return true;
		} else if(command instanceof ScriptCommand) {
			return true;
		} else if(command instanceof MultiCommand) {
			return true;
		}
		return false;
	}
	
	
	/**
	 * 명령어 목록에 있는지 확인한다. 
	 * @param commandMetaSet 명령어 메타정보 Set
	 * @param command 명령어 객체
	 * @return 목록에 있으면 true
	 */
	protected boolean containsCommand(List<CommandMeta> commandMetaSet, Command command) {
		for(CommandMeta meta : commandMetaSet) {
			if(meta.getCommandClass().equals(command.getClass())) {
				return true;
			}
		}
		return false;
	}
	
	
    /**
     * 로그를 기록한다. 마지막에 newline 문자를 추가한다.
     * @param msg 로그 메시지
     */
    protected void logln(Object msg) {
    	Logger.logln(Level.RESULT, msg);
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
