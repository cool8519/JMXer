package dal.tool.trace.jmxer.cli;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import javax.management.ObjectName;

import dal.tool.cli.CommandExecutor;
import dal.tool.cli.Logger;
import dal.tool.cli.command.Command;
import dal.tool.cli.command.CommandMeta;
import dal.tool.trace.jmxer.cli.command.InfoCommand;
import dal.tool.trace.jmxer.cli.command.JmxCallCommand;
import dal.tool.trace.jmxer.cli.command.JmxHelpCommand;
import dal.tool.trace.jmxer.cli.command.JmxListCommand;
import dal.tool.trace.jmxer.cli.command.JmxObjectCommand;
import dal.tool.trace.jmxer.cli.command.JmxRecordCommand;
import dal.tool.trace.jmxer.cli.command.JmxSettingCommand;
import dal.tool.trace.jmxer.cli.command.JmxThreadCommand;
import dal.tool.util.jmx.JMXUtil;
import dal.tool.util.jmx.MBeanConnector;


/**
 * JMX Client의 프롬프트 모드 수행을 담당하는 클래스
 * @author 권영달
 *
 */
public class JmxCommandExecutor extends CommandExecutor {

	private static List<String> objectNameList;
	private static List<CommandMeta> commandMetaList = new ArrayList<CommandMeta>();
	static {
		commandMetaList.add(JmxHelpCommand.commandMeta);
		commandMetaList.add(InfoCommand.commandMeta);
		commandMetaList.add(JmxSettingCommand.commandMeta);
		commandMetaList.add(JmxObjectCommand.commandMeta);
		commandMetaList.add(JmxListCommand.commandMeta);
		commandMetaList.add(JmxCallCommand.commandMeta);
		commandMetaList.add(JmxThreadCommand.commandMeta);
		commandMetaList.add(JmxRecordCommand.commandMeta);
	}
	
	protected MBeanConnector mbeanConnector = null;
    
   
	/**
	 * JMX 명령어 수행을 위한 인스턴스를 생성한다.
	 * @param mbeanConnector 접속된 JVM의 MBeanConnector 객체
	 */
	public JmxCommandExecutor(MBeanConnector mbeanConnector) {
		super();
		this.settings = new JmxSettings();
		this.mbeanConnector = mbeanConnector;
		try {
			refreshObjectNameList(JMXUtil.getObjectNames(mbeanConnector.getMBeanConnection(), null));
		} catch(Exception e) {
			Logger.logln(Logger.Level.ERROR, "Failed to get list of ObjectName.");
		}
	}
	

	public JmxSettings getSettings() {
		return (JmxSettings)settings;
	}

	
	public MBeanConnector getMBeanConnector() {
		return mbeanConnector;
	}

	
	public List<CommandMeta> getCommandMetaList() {
		return commandMetaList;
	}
	
	public List<String> getObjectNameList() {
		return objectNameList;
	}
	
	public void refreshObjectNameList(List<ObjectName> orderedList) {
		try {
			List<String> list = new ArrayList<String>();
			for(ObjectName oName : orderedList) {
				list.add(oName.toString());
			}
			objectNameList = list;
		} catch(Exception e) {
			objectNameList = null;
		}
	}
	
	protected Command getCommand(String commandLine) throws Exception {
        Command command = null;
        String first = commandLine.trim().split(" ")[0];
        if(first.endsWith(";")) {
        	first = first.substring(0, first.length()-1).trim();
        }

        for(CommandMeta meta : commandMetaList) {
			if(meta.isMatchCommand(first)) {
				try {
					Constructor<? extends Command> cons = meta.getCommandClass().getConstructor(new Class[]{ String.class, JmxCommandExecutor.class });
					command = cons.newInstance(commandLine, this);
				} catch(Exception e) {
					throw e;
				}
				break;
			}
		}
        return command;
    }


	protected boolean isAllowedInNonPrompt(Command command) {
		if(!containsCommand(commandMetaList, command)) {
			return false;
		}
		if(command instanceof JmxHelpCommand) {
			return false;
		}
		return true;
	}

}
