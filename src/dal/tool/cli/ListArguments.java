package dal.tool.cli;

import java.util.ArrayList;
import java.util.List;

import dal.tool.util.StringUtil;


public class ListArguments extends AbstractArguments {

	protected String command;
    protected List<String> argList;
    protected int currIdx = 0;


    public ListArguments(String cmdLine) throws Exception {
    	super(cmdLine);
    }
    
    public ListArguments(List<String> argList, String cmdLine) throws Exception {
    	super(cmdLine);
    	this.argList = argList;
    }

    public ListArguments clone() {
    	try {
    		return new ListArguments(new ArrayList<String>(argList), cmdLine);
    	} catch(Exception e) {
    		return null;
    	}
    }
    
    protected void parseCommandLine() throws Exception {
    	argList = StringUtil.getTokenList(cmdLine, ' ', StringUtil.QUOTES, false);
    	if(argList != null && argList.size() > 0) {
    		command = argList.remove(0);
    	}
    }

    public List<String> getArguments() {
        return argList;
    }

    public void setArguments(List<String> argList) {
    	this.argList = argList;
    }
    
    public int size() {
        return argList.size();
    }

    public void printArguments() {
    	if(argList.size() > 0) {
	        for(int i = 0; i < argList.size(); i++) {
	        	Logger.logln((i+1) + "\t: " + argList.get(i));
	        }
    	}
    }

    public boolean hasMoreArgument() {
    	return (argList.size() > currIdx);
    }
    
    public String nextArgument() {
    	return getArgument(++currIdx);
    }
    
    public String previousArgument() {
    	return getArgument(--currIdx);
    }
    
    public void toFirstIndex() {
    	currIdx = 0;
    }

    public void toLastIndex() {
    	currIdx = argList.size();
    }

    
    public String getCommand() {
    	return command;
    }

    public String getArgument(int idx) {
    	if(idx < 1 || idx > argList.size()) {
    		return null;
    	} else {
    		return argList.get(idx-1);
    	}
    }
    
    protected void addArgument(String arg) {
        argList.add(arg);
    }

    public void updateArgument(int idx, String arg) throws Exception {
    	if(idx < 1 || idx > argList.size()) {
            throw new Exception("Can not update the argument : invalid index " + idx);
    		
    	}
    	argList.set(idx-1, arg);
    }
    
    public void removeArgument(String arg) {
    	argList.remove(arg);
    }

    public boolean contains(String arg) {
    	return argList.contains(arg);
    }

}