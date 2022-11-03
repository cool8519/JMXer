package dal.tool.trace.jmxer.cli.command;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanFeatureInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import dal.tool.cli.Logger;
import dal.tool.cli.Logger.Level;
import dal.tool.cli.command.Command;
import dal.tool.trace.jmxer.cli.JmxCommandExecutor;
import dal.tool.trace.jmxer.cli.JmxSettings;
import dal.tool.util.StringUtil;
import dal.tool.util.jmx.JMXHelper;
import dal.tool.util.jmx.JMXUtil;
import dal.tool.util.jmx.MBeanConnector;
import dal.tool.util.jmx.MBeanNotFoundException;


public abstract class JmxCommand extends Command {

	protected static Map<String,Map<String,MBeanFeatureInfo>> mbeanInfoCache = new HashMap<String,Map<String,MBeanFeatureInfo>>();

	protected MBeanConnector mbeanConnector = null;
	
	
	public JmxCommand(String commandLine, JmxCommandExecutor commandExecutor) {
		super(commandLine, commandExecutor);
		if(commandExecutor != null) {
			this.mbeanConnector = commandExecutor.getMBeanConnector();
		}
	}

	
	public boolean saveToHistory() {
		return true;
	}

	
	public JmxCommandExecutor getCommandExecutor() {
		return (JmxCommandExecutor)commandExecutor;
	}
	
	
	public JmxSettings getSettings() {
		return getCommandExecutor().getSettings();
	}
	

	public MBeanServerConnection getMBeanConnection() {
		if(mbeanConnector != null) {
			return mbeanConnector.getMBeanConnection();
		} else {
			return null;
		}
	}

	
	protected boolean initMBeanConnector() {
		if(mbeanConnector != null) {
	    	if(!mbeanConnector.isInitialized()) {
	    		try {
	    			mbeanConnector.connect();
	    		} catch(Exception e) {
	    			logln(Level.ERROR, "Failed to connect JMX connection : " + e.getMessage());
	    		}
	    	}
		}
		return true;
	}

	
	public boolean beforeExecute() throws Exception {
		return initMBeanConnector();
	}
	
	
	public final boolean afterExecute() throws Exception {
        if(getCommandExecutor().getSettings().showTimingOnExecute()) {
            logln("Command execute Finish : " + getElapsedTime() + " ms");
        }
        return true;
	}


	/**
	 * 주어진 명령어와 인자값에 맞는 동작을 수행한다.
	 * @throws Exception 명령어 수행중 발생한 모든 Exception
	 */
	public void execute() throws Exception {
		if(needToParse) {
			parseCommandLine();
		}
		
		if(Logger.isShowLevel(Level.DEBUG)) {
			if(commandLine != null && commandLine.trim().length() > 0) {
				String setMBeanOName = getCommandExecutor().getSettings().getMBeanObjectName();
		        logln(Level.DEBUG, "------------------------------------------------");
				logln(Level.DEBUG, "COMMAND    : " + getClass().getSimpleName());
				if(setMBeanOName != null) {
					logln(Level.DEBUG, "SET MBEAN  : " + setMBeanOName);
				}
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
		}
		
        if(isStopCommand()) return;
        
        try {
        	if(!this.beforeExecute()) {
        		return;
        	}
        } catch(Exception e) {
        	logln(Level.ERROR, "Error occurred during beforeExecute : " + e.getMessage());
        }

        if(isStopCommand()) return;

        try {
			startTime = System.currentTimeMillis();
			String subCmd = StringUtil.stripQuote(commandArgs.getArgument(1), new char[]{'"','\''}, true);
			if(commandArgs.size() == 1 && subCmd.equalsIgnoreCase("help")) {
				logln("");
				this.printHelp();
				logln("");
			} else {
				this.doExecute();
			}
		} catch(Exception ex) {
			if(ex instanceof IOException) {
				logln(Level.ERROR, "The JMX Connection was disconnected : " + ex.getMessage());
				logln(Level.DEBUG, "Reconnecting to JVM...");
				try {
					mbeanConnector.connect();
					logln(Level.DEBUG, "Executing failed command again...");
					commandArgs.toFirstIndex();
					this.doExecute();
				} catch(Exception e) {
					throw e;
				}
			} else {
				throw ex;
			}
		} finally {
			endTime = System.currentTimeMillis();
		}

        if(isStopCommand()) return;

        try {
        	this.afterExecute();
        } catch(Exception e) {
        	logln(Level.ERROR, "Error occurred during afterExecute : " + e.getMessage());
        }
	}
	
	
	protected Map<String,MBeanFeatureInfo> getMBeanInfoList(String objectName) throws Exception {
		Map<String,MBeanFeatureInfo> infos = mbeanInfoCache.get(objectName);
		if(infos == null) {
			List<MBeanFeatureInfo> featureMap = JMXUtil.getAttributeAndOperationList(getMBeanConnection(), new ObjectName(objectName));
			infos = new LinkedHashMap<String,MBeanFeatureInfo>(featureMap.size());
			for(MBeanFeatureInfo info : featureMap) {
				if(info instanceof MBeanAttributeInfo) {
					infos.put(info.getName(), info);
				} else if(info instanceof MBeanOperationInfo) {
					MBeanOperationInfo opInfo = (MBeanOperationInfo)info;
					infos.put(opInfo.getName()+":"+JMXHelper.toTypeString(opInfo.getSignature()), info);
				}
			}
			mbeanInfoCache.put(objectName, infos);
		}
		return infos;
	}
	
	
	protected MBeanAttributeInfo getMBeanAttributeInfo(String objectName, String attributeName) throws Exception {
		Map<String,MBeanFeatureInfo> infos = getMBeanInfoList(objectName);
		MBeanFeatureInfo info = infos.get(attributeName);		
		if(info != null && (info instanceof MBeanAttributeInfo)) {
			return (MBeanAttributeInfo)info;
		}
		throw new MBeanNotFoundException();
	}

	
	protected MBeanOperationInfo getMBeanOperationInfo(String objectName, String operationName, String paramString) throws Exception {
		Map<String,MBeanFeatureInfo> infos = getMBeanInfoList(objectName);
		MBeanFeatureInfo info = infos.get(operationName+":"+paramString);		
		if(info != null && (info instanceof MBeanOperationInfo)) {
			return (MBeanOperationInfo)info;
		}
		throw new MBeanNotFoundException();
	}
	
}
