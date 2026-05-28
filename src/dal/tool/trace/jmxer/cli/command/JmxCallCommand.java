package dal.tool.trace.jmxer.cli.command;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.management.MBeanFeatureInfo;
import javax.management.ObjectName;

import dal.tool.cli.Logger;
import dal.tool.cli.Logger.Level;
import dal.tool.cli.command.CommandMeta;
import dal.tool.trace.jmxer.JMXControl;
import dal.tool.trace.jmxer.cli.JmxCommandExecutor;
import dal.tool.trace.jmxer.cli.JmxMutatingCallConfirmation;
import dal.tool.trace.jmxer.util.JMXPrintUtil;
import dal.tool.util.ObjectDataUtil;
import dal.tool.util.StringUtil;
import dal.tool.util.jmx.JMXUtil;
import dal.tool.util.jmx.MBeanNotFoundException;

public class JmxCallCommand extends JmxCommand {

	public static final CommandMeta commandMeta = new CommandMeta(JmxCallCommand.class, "CALL");
	
	protected String mbeanObjectName = null;
	protected String callTargetName = null;
	protected String[] callTargetValues = null;
	
	
	public JmxCallCommand(String commandLine, JmxCommandExecutor commandExecutor) {
		super(commandLine, commandExecutor);
	}


	public void printHelp() {
        logln(" CALL");
        logln(" ----");
        logln("");
        logln(" Views MBean attributes or Invokes MBean operations.");
        logln(" ObjectName could be omitted if you set current ObjectName with 'OBJECT' command");
        logln("");
        logln(" CALL ObjectName {AttributeName|OperationName} [ValueOrArguments]");
        logln("");
        logln(" Following is the list of available value.");
        logln("     ObjectName - '*' means to select all");
        logln("                - MBean ObjectName");
        logln("     AttributeName or OperationName - '?'  means to view list of all Attributes and Operation");
        logln("                                    - '??' means to view detailed information of all Attributes and Operation");
        logln("                                    - '#'  means to view the values of all Attributes");
        logln("     ValueOrArguments - '?' means to view detailed information of the Attributes or Operation");
        logln("                      - MBean Attribute value to set");
        logln("                      - MBean Operation arguments. It should be formatted \"Argument(,Argument...)\"");
        logln("                        'Argument' could be array type. ex) [a,b,c]");
        logln("");
        logln(" Examples)");
        logln(" CALL [{*|ObjectName}] {?|??|#}");
        logln(" CALL [{*|ObjectName}] AttributeName [{?|AttributeValue}]");
        logln(" CALL [{*|ObjectName}] OperationName [{?|\"Argument(,Argument...)\"}]");
        logln("");
        logln(" Attribute writes and operation invokes require confirmation (Y/N) because they may change the target JVM.");
	}


	private boolean parseCallArguments() throws Exception {
		if(commandArgs.size() == 1 && commandArgs.getArgument(1).equalsIgnoreCase("help")) {
			return true;
		}
		
		if(!commandArgs.hasMoreArgument()) {
			logln("Need more arguments.");
			return false;
		}
		
		String setMBeanOName = getSettings().getMBeanObjectName();
		if(commandArgs.size() == 1) {
			if(setMBeanOName == null) {
				logln("Need more arguments.");
				return false;
			} else {
				mbeanObjectName = setMBeanOName;
				callTargetName = StringUtil.stripOuterFilenameQuotes(commandArgs.nextArgument());
			}
		} else if(commandArgs.size() == 2) {
			String first = commandArgs.getArgument(1);
			first = StringUtil.stripQuote(first, new char[]{'"','\''}, true);
			if((first.indexOf(':') > 0 && first.indexOf('=') > 0) || first.equals("*")) {
				mbeanObjectName = StringUtil.stripQuote(commandArgs.nextArgument(), new char[]{'"','\''}, true);
				callTargetName = StringUtil.stripOuterFilenameQuotes(commandArgs.nextArgument());
			} else {
				if(setMBeanOName == null) {
					mbeanObjectName = StringUtil.stripQuote(commandArgs.nextArgument(), new char[]{'"','\''}, true);
					callTargetName = StringUtil.stripOuterFilenameQuotes(commandArgs.nextArgument());
				} else {
					mbeanObjectName = setMBeanOName;
					callTargetName = StringUtil.stripOuterFilenameQuotes(commandArgs.nextArgument());
				}
			}			
		} else if(commandArgs.size() == 3) {
			mbeanObjectName = StringUtil.stripQuote(commandArgs.nextArgument(), new char[]{'"','\''}, true);
			callTargetName = StringUtil.stripOuterFilenameQuotes(commandArgs.nextArgument());
			if(callTargetName.equals("?") || callTargetName.equals("??") || callTargetName.equals("#")) {
				logln("Too many arguments.");
				return false;
			}
		} else {
            logln("Too many arguments.");
    		return false;
		}

		if(commandArgs.hasMoreArgument()) {
			callTargetValues = StringUtil.getTokenArray(commandArgs.nextArgument(), ',', new String[]{"[]", "\"\"", "''", "``"});
			if(commandArgs != null) {
		        for(int i = 0; i < callTargetValues.length; i++) {
		            logln(Level.DEBUG, "CALLARGS[" + (i+1) + "]: " + callTargetValues[i]);
		        }
			}
		}
		
		commandArgs.toFirstIndex();
		return true;
	}


	private Set<ObjectName> resolveMbeanObjectNameSet() throws Exception {
		Set<ObjectName> mbeanObjectNameSet = new HashSet<ObjectName>();
		if(mbeanObjectName.equals("*")) {
			if(!callTargetName.equals("?") && !callTargetName.equals("??") && !callTargetName.equals("#")) {
				logln("Multi-ObjectName is alloweded only if target name is one of '?', '??', '#'.");
				return mbeanObjectNameSet;
			}
			mbeanObjectNameSet = getMBeanConnection().queryNames(null, null);
		} else {
			if(JMXUtil.existsObjectName(getMBeanConnection(), mbeanObjectName)) {
				mbeanObjectNameSet.add(new ObjectName(mbeanObjectName));
			} else {
				Set<ObjectName> tempSet = getMBeanConnection().queryNames(new ObjectName(mbeanObjectName), null);
				if(tempSet.size() < 1) {
					logln("Not found ObjectName '" + mbeanObjectName + "'");
					return mbeanObjectNameSet;
				}
				for(ObjectName oName : tempSet) {
					mbeanObjectNameSet.add(oName);
				}
			}
		}
		return mbeanObjectNameSet;
	}

	private boolean confirmMutatingCallIfNeeded() throws Exception {
		if(isReadOnlyCallTarget() || getMBeanConnection() == null) {
			return true;
		}
		Set<ObjectName> mbeanObjectNameSet = resolveMbeanObjectNameSet();
		if(mbeanObjectNameSet.isEmpty()) {
			return true;
		}
		java.util.Iterator<ObjectName> it = mbeanObjectNameSet.iterator();
		ObjectName sample = it.next();
		return JmxMutatingCallConfirmation.confirmIfNeeded(getMBeanConnection(), sample, callTargetName,
				callTargetValues, mbeanObjectNameSet.size());
	}

	public void doExecute() throws Exception {
        if(getMBeanConnection() == null) {
            logln(Level.ERROR, "Not connected.");
            return;
        }

        Set<ObjectName> mbeanObjectNameSet = resolveMbeanObjectNameSet();
        if(mbeanObjectNameSet.isEmpty()) {
        	return;
        }

        for(ObjectName oName : mbeanObjectNameSet) {
        	if(mbeanObjectNameSet.size() > 1) {
		        logln("");
	        	logln("# ObjectName(\"" + oName + "\")");
        	}
	        try {
				if("??".equals(callTargetName)) {
					JMXPrintUtil.printMBeanInfo(getMBeanConnection(), oName, null);
				} else if("?".equals(callTargetName)) {
					JMXPrintUtil.printAttributeAndOperationList(new ArrayList<MBeanFeatureInfo>(getMBeanInfoList(oName.toString()).values()), oName.toString(), null);
				} else if("#".equals(callTargetName)) {
					JMXPrintUtil.printAttributeValue(getMBeanConnection(), oName, null);
				} else {
					if(callTargetValues != null && callTargetValues.length == 1 && "?".equals(callTargetValues[0])) {
						JMXPrintUtil.printMBeanInfo(getMBeanConnection(), oName, callTargetName);
					} else {
			        	Object opRet = JMXUtil.getJMXResult(getMBeanConnection(), oName.toString(), callTargetName, callTargetValues);
			    		if(opRet != null) {
			    			logln(ObjectDataUtil.toString(opRet, getSettings().prettyPrintInResult()));
			        	}
					}
				}
	        } catch(MBeanNotFoundException mbnfe) {
	        	logln(mbnfe.getMessage());
	        } catch(Exception e) {
	        	Logger.logStackTrace(e);
	            logln(Level.ERROR, "Failed to execute command : " + e.getMessage());
	        }
        	if(mbeanObjectNameSet.size() > 1) {
        		logln("");
        	}
        }
	}

	
	private boolean isReadOnlyCallTarget() {
		if("?".equals(callTargetName) || "??".equals(callTargetName) || "#".equals(callTargetName)) {
			return true;
		}
		if(callTargetValues != null && callTargetValues.length == 1 && "?".equals(callTargetValues[0])) {
			return true;
		}
		return false;
	}


	public final boolean beforeExecute() throws Exception {
		if(JMXControl.isAnalyzeMode) {
			logln("JMX commands are not allowed in analyze mode.");
			return false;
		}
		if(!parseCallArguments()) {
			setStopCommand(true);
			return true;
		}
		if(commandArgs.size() == 1 && commandArgs.getArgument(1).equalsIgnoreCase("help")) {
			return initMBeanConnector();
		}
		if(!initMBeanConnector()) {
			return false;
		}
		if(!confirmMutatingCallIfNeeded()) {
			setSaveToHistory(false);
			Logger.abortTuiUserCaptureSession();
			return false;
		}
		return true;
	}

}
