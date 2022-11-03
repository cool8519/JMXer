package dal.tool.cli;



public abstract class AbstractArguments {

    protected String cmdLine = null;


    public AbstractArguments(String cmdLine) throws Exception {
    	this.cmdLine = cmdLine;
    	try {
    		parseCommandLineInternal();
    	} catch(Exception e) {
            throw new Exception("Arguments parsing error : " + e.getMessage());
    	}
    }
    
    public void setCommandLine(String s) {
    	cmdLine = s;
    }

    public String getCommandLine() {
        return cmdLine;
    }

    private void parseCommandLineInternal() throws Exception {
    	parseCommandLine();
    	if(!checkCommandOptions()) {
    		throw new Exception("Validation check failed.");
    	}
    }
    

    protected boolean checkCommandOptions() throws Exception {
    	return true;
    }
    


    public abstract AbstractArguments clone();

    	
    protected abstract void parseCommandLine() throws Exception;
    

    public abstract Object getArguments();

    
    public abstract void printArguments();

    
    public abstract int size();
    

    public abstract Object nextArgument();


    public abstract Object previousArgument();

}