package dal.tool.trace.jmxer.cli.helper;

import java.util.ArrayList;
import java.util.List;

import dal.tool.cli.ListArguments;
import dal.tool.util.StringUtil;

public class ListArgumentsHelper {

	
	public static List<String> stripQuotes(ListArguments listArgs, char[] bindChars) {
		List<String> args = listArgs.getArguments();
		for(int i = 0; i < args.size(); i++) {
			String arg = args.get(i);
			for(char bindChar : bindChars) {
				if(arg.startsWith(""+bindChar) && arg.endsWith(""+bindChar)) {
					args.set(i, arg.substring(1, arg.length()-1));
					break;
				}
			}
		}
		return args;
	}

	
	public static List<String> concatSpaceWithQuotes(ListArguments listArgs, char bindChar) throws Exception {
		List<String> newArgs = new ArrayList<String>();
		List<String> oldArgs = listArgs.getArguments();
		String temp = null;
		for(String arg : oldArgs) {
			if(temp == null) {
				if(arg.indexOf(bindChar) < 0 || StringUtil.countCharacters(arg, bindChar)%2 == 0) {
					newArgs.add(arg);
				} else {
					temp = arg;
				}
			} else {
				if(StringUtil.countCharacters(arg, bindChar)%2 == 0) {
					temp += " " + arg;
				} else {
					newArgs.add(temp + " " + arg);
					temp = null;
				}
			}
		}
		if(temp != null) {
			throw new Exception("The quote pairs do not match.");
		}
		return newArgs;
	}
	
	
	
}
