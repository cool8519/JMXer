package dal.tool.cli.command;

import java.util.HashSet;
import java.util.Set;

import dal.tool.cli.Logger;
import dal.tool.cli.Logger.Level;
import dal.tool.cli.util.IOUtil;

public class CommandMeta {

	private Class<? extends Command> commandClass;
	private String[] commandMatchStrings;
	private Set<CommandMatch> commandMatchSet = new HashSet<CommandMatch>(1);

	
	/**
	 * 명령어 메타정보를 생성한다.<br/>
	 * @param cls 명령어 구현 클래스
	 * @param args 명령어 매칭 문자열 목록. Optional 문자열은 [] 내에 표시한다. 예) "EXT[ERNAL]"
	 */
	public CommandMeta(Class<? extends Command> cls, String ... args) {
		this.commandClass = cls;
		this.commandMatchStrings = args;
		try {
			parseCommandMatchStrings(args);
		} catch(Exception e) {
			Logger.logln(Level.ERROR, "Failed to create '" + cls.getSimpleName() + "' metadata : " + e.getMessage());
		}
	}
	
	private void parseCommandMatchStrings(String[] stringArr) {
		String ess, opt;
		int idx;
		for(String s : stringArr) {
			idx = s.indexOf("[");
			if(idx > 0 && s.endsWith("]")) {
				ess = s.substring(0, idx);
				opt = s.substring(idx+1, s.length()-1);
			} else {
				ess = s;
				opt = null;
			}
			commandMatchSet.add(new CommandMatch(ess, opt));
		}
	}
	
	public Class<? extends Command> getCommandClass() {
		return commandClass;
	}
	
	public String[] getCommandMatchStrings() {
		return commandMatchStrings;
	}
	
	public boolean isMatchCommand(String inputStr) {
		for(CommandMatch commandMatch : commandMatchSet) {
			if(commandMatch.isMatch(inputStr)) {
				return true;
			}
		}
		return false;
	}
	
	
	public class CommandMatch {
		private String essential = null;
		private String optional = null;
		public CommandMatch(String essential, String optional) {
			this.essential = essential;
			this.optional = optional;
		}
		public String getEssential() {
			return essential;
		}
		public String getOptional() {
			return optional;
		}
		public boolean isMatch(String inputStr) {
			if(optional != null) {
				return IOUtil.isIncludeEquals(inputStr.toLowerCase(), essential.toLowerCase(), optional.toLowerCase());
			} else {
				return inputStr.toLowerCase().equals(essential.toLowerCase());
			}
		}
	}

}
