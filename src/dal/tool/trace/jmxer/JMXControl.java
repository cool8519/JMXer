package dal.tool.trace.jmxer;

import dal.tool.cli.Logger;
import dal.tool.trace.jmxer.cli.JmxCommandExecutor;
import dal.tool.util.jmx.MBeanConnector;
import dal.tool.util.jmx.MBeanConnector.ConnectType;


/**
 * JMX를 통해 MBean 객체 접근을 수행하는 클래스<br/>
 * 사용자 편의를 위해 간단 모드 및 대화형 모드를 제공한다.<br/>
 * 등록된 MBean의 Attribute를 조회/변경하거나 Operation을 수행할 수 있다. 
 * @author 권영달
 *
 */
public class JMXControl {

	public static ConnectType connectType = ConnectType.NOT_SET;
	public static boolean isAnalyzeMode = false;
	

	/**
	 * MBean 조회 및 조작을 수행한다. 
     * @param args MBean 조작을 위한 Argument 배열
     * @throws Exception 수행 과정에서 발생한 모든 Exception
	 */
	public static void main(String[] args) throws Exception {
    	if(args.length == 0) {
    		printUsageAndExit(false, args);
    	}
    	
    	if(args.length < 1) {
    		printUsageAndExit(true, args);
    	}

    	String[] target = new String[2];
    	int idx = args[0].indexOf('=');
    	if(idx < 0 && !args[0].trim().equalsIgnoreCase("DUMPFILE")) {
    		printUsageAndExit(false, args);
    	} else {
        	if(idx < 0) {
        		target[0] = "DUMPFILE";
        	} else {
        		target[0] = args[0].substring(0, idx).toUpperCase();
        		target[1] = args[0].substring(idx+1);
        	}
    	}

    	if("PID".equals(target[0])) {
    		connectType = ConnectType.ATTACH_BY_PID;
    	} else if("NAME".equals(target[0])) {
    		connectType = ConnectType.ATTACH_BY_NAME;    		
    	} else if("ADDRESS".equals(target[0])) {
    		connectType = ConnectType.RMI_BY_ADDRESS;
    	} else if("DUMPFILE".equals(target[0])) {
    		isAnalyzeMode = true;
    	} else {
    		connectType = ConnectType.UNKNOWN;
    	}
    	
    	MBeanConnector mbeanConnector = null;
    	String promptString = "";
    	String commandLine = "";
    	boolean isPromptMode = true;
    	String[] auth = null;
    	
    	if(connectType == ConnectType.ATTACH_BY_PID || connectType == ConnectType.ATTACH_BY_NAME || connectType == ConnectType.RMI_BY_ADDRESS) {
    		// attach mode
    		int idxOffset = 0;
    		if(args.length > 1 && (args[1].startsWith("AUTH=") || args[1].startsWith("auth="))) {
    			auth = args[1].substring(5).split("/");
    			if(auth.length != 2) {
    				throw new Exception("Invalid Arguments : " + args[1]);
    			}
    			idxOffset = 1;
    		}    		
    		for(int i = 1+idxOffset; i < args.length; i++) {
    			commandLine += args[i] + " ";
    		}
    		promptString = JMXerConstant.JMXER_PROMPT_STRING;
			isPromptMode = (args.length == 1+idxOffset); 
    	} else if(connectType == ConnectType.NOT_SET && isAnalyzeMode) {
    		// analyze mode
    		for(int i = 1; i < args.length; i++) {
    			commandLine += args[i] + " ";
    		}
    		promptString = JMXerConstant.JMXER_PROMPT_NOT_CONNECTED_STRING;
    		isPromptMode = (args.length == 1);
    	} else {
    		// unknown
			Logger.logln(Logger.Level.ERROR, "Invalid argument '" + target[0] + "'");
			System.exit(-1);
    	}
    	
    	try {
    		if(isAnalyzeMode) {
        		Logger.logln("Running as ANALYZE mode.\n");
        		if(target[1] != null) {
        			if(target[1].trim().equals("")) {
        				Logger.logln(Logger.Level.ERROR, "No dump file was specified.");
        				System.exit(-1);
        			}
        			String dumpFilePath = target[1];
        			if(dumpFilePath.length() >= 2 && (dumpFilePath.startsWith("\"") && dumpFilePath.endsWith("\""))) {
        				dumpFilePath = dumpFilePath.substring(1, dumpFilePath.length()-1);
        			}
        			commandLine = "RECORD LOAD \"" + dumpFilePath + "\"; " + commandLine;
        		}
	       		if(isPromptMode) {
	       			executeCommandInPrompt(mbeanConnector, promptString, commandLine.trim());
	       		} else {
	       			executeCommand(mbeanConnector, promptString, commandLine.trim());
	       		}
    		} else {
        		Logger.logln("Running as ATTACH mode.\n");
        		try {
        			mbeanConnector = new MBeanConnector(connectType, target[1], auth);
        			mbeanConnector.connect();
        		} catch(Exception e) {
        			Logger.logln(Logger.Level.ERROR, "Failed to connect to JVM");
        			System.exit(-1);
        		}
	       		if(isPromptMode) {
	       			executeCommandInPrompt(mbeanConnector, promptString);
	       		} else {
	       			executeCommand(mbeanConnector, promptString, commandLine.trim());
	       		}
    		}
    	} catch(Exception e) {
    		e.printStackTrace();
    		Logger.logln(Logger.Level.ERROR, "Error occurred while executing command");
    	} finally {
    		if(mbeanConnector != null) {
    			mbeanConnector.close();
    		}
    	}
    }

	
	/**
     * 명령어를 수행한다.
	 * @param mbeanConnector 접속된 JVM의 MBeanConnector 객체
	 * @param prompt 프롬프트 문자열
	 * @param commandLine 수행할 명령어 문자열 
     * @throws Exception 수행 과정에서 발생한 모든 Exception
	 */
	private static void executeCommand(MBeanConnector mbeanConnector, String prompt, String commandLine) throws Exception {
		JmxCommandExecutor executor = new JmxCommandExecutor(mbeanConnector);
		executor.setPrompt(prompt);
		executor.processCommandInNonPrompt(commandLine);
	}


	/**
     * 대화형 모드를 수행한다. 
	 * @param mbeanConnector 접속된 JVM의 MBeanConnector 객체 
	 * @param prompt 프롬프트 문자열
	 * @param commandLine 프롬프트 기동 전에 미리 수행할 명령어 문자열 
     * @throws Exception 수행 과정에서 발생한 모든 Exception
	 */
	private static void executeCommandInPrompt(MBeanConnector mbeanConnector, String prompt, String commandLine) throws Exception {
		JmxCommandExecutor executor = new JmxCommandExecutor(mbeanConnector);
		executor.setPrompt(prompt);
		executor.processCommandInNonPrompt(commandLine);
		executor.startPrompt();
	}

	
	/**
     * 대화형 모드를 수행한다.
	 * @param mbeanConnector 접속된 JVM의 MBeanConnector 객체 
	 * @param prompt 프롬프트 문자열
     * @throws Exception 수행 과정에서 발생한 모든 Exception
	 */
	private static void executeCommandInPrompt(MBeanConnector mbeanConnector, String prompt) throws Exception {
		JmxCommandExecutor executor = new JmxCommandExecutor(mbeanConnector);
		executor.setPrompt(prompt);
		executor.startPrompt();
	}


    /**
     * Usage를 출력하고 프로세스를 종료한다.
     * @param isError 에러 여부
     * @param args 메인메소드에서 전달받은 argument 배열
     */
	private static void printUsageAndExit(boolean isError, String[] args) {
    	if(isError) {
    		Logger.logln(Logger.Level.ERROR, "Invalid Arguments : size=" + args.length);
    		Logger.logln(Logger.Level.ERROR, "");
    	}
		printUsage();
		System.exit(0);
    }
    
	
    /**
     * Usage를 화면에 출력한다.
     */
    public static void printUsage() {
    	StringBuffer args = new StringBuffer();
    	args.append("{PID=<pid>|NAME=<proc_name>|ADDRESS=<ip>:<port> [AUTH=<user>/<pass>]|DUMPFILE[=<dumpfile_path>]} ");
    	args.append("[CommandLine_To_Execute]");
   	
    	Logger.logln("");
    	Logger.logln(JMXerConstant.JMXER_VERSION_STRING + " - " + JMXerConstant.JMXER_COPYRIGHT_STRING);
    	Logger.logln("");
    	Logger.logln("");
    	Logger.logln("Usage       : java -jar JMXer.jar [ARGUMENTS]");
    	Logger.logln("");
    	Logger.logln("Arguments   : " + args.toString());
    	Logger.logln("");
    	Logger.logln(" *) There are two modes to run JMXer.");
    	Logger.logln("    - Attach Mode  : To connect to a remote or local JVM to execute JMX commands. All functions including analysis are available.");
    	Logger.logln("    - Analyze Mode : To only analyze record dump files without JVM connection. Unable to execute JMX commands.");
    	Logger.logln(" *) If 'CommandLine_To_Execute' arguments are omitted, it will run as INTERACTIVE_MODE.");
    	Logger.logln("");
    	Logger.logln("Attach Mode Example :");
    	Logger.logln(" 1) Attach to JVM with PID. To run INTERACTIVE_MODE.");
    	Logger.logln("    args> PID=12345");
    	Logger.logln(" 2) Attach to JVM with NAME. To view list of Attribute and operation in ObjectName 'java.lang:type=Runtime'.");
    	Logger.logln("    args> NAME=my.pkg.MyProgram LIST java.lang:type=Runtime/*");
    	Logger.logln(" 3) Attach to JVM with ADDRESS. To set Attribute 'ThreadContentionMonitoringEnabled' to 'true' in ObjectName 'java.lang:type=Threading'.");
    	Logger.logln("    args> ADDRESS=127.0.0.1:10001 CALL java.lang:type=Threading ThreadContentionMonitoringEnabled true");
    	Logger.logln(" 4) Attach to JVM with ADDRESS with auth. To invoke Operation 'getThreadInfo' with arguments '2,10' in ObjectName 'java.lang:type=Threading'.");
    	Logger.logln("    args> ADDRESS=127.0.0.1:10001 AUTH=myuser/mypass CALL java.lang:type=Threading getThreadInfo 2,10");
    	Logger.logln(" 5) Attach to JVM with PID. To execute script file 'jmx_script.txt' in current directory.");
    	Logger.logln("    args> PID=12345 @'./jmx_script.txt'");
    	Logger.logln("");
    	Logger.logln("Analyze Mode Example :");
    	Logger.logln(" 1) Load the dump file 'JMXer_Record_Trace.dmp' in current directory and analyze it. To run INTERACTIVE_MODE.");
    	Logger.logln("    args> DUMPFILE='JMXer_Record_Trace.dmp'");
    	Logger.logln(" 2) Load the dump file '/home/dump/JMXer_Record_Trace.dmp' and execute script file '/home/script/jmx_script.txt'");
    	Logger.logln("    args> DUMPFILE='/home/dump/JMXer_Record_Trace.dmp' @'/home/script/view_script.txt'");
    	Logger.logln("");
    }
	
}
