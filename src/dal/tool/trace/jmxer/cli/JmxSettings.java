package dal.tool.trace.jmxer.cli;

import dal.tool.cli.Logger;
import dal.tool.cli.Settings;

public class JmxSettings extends Settings {

	protected String setOName = null;
	protected boolean prettyPrint = true;	

	
	public String getMBeanObjectName() {
		return setOName;
	}

	public boolean setMBeanObjectName(String value) {
        if(value != null && (value.indexOf(':') < 0 || value.indexOf('=') < 0)) {
        	Logger.logln("Invalid MBean ObjectName Format \"" + value + "\"");
            return false;
        } else {
        	setOName = value;
        	return true;
        }
	}

	
	public boolean prettyPrintInResult() {
		return prettyPrint;
	}

	public boolean setPrettyPrintInResult(String value) {
        if(value == null || (!value.equalsIgnoreCase("on") && !value.equalsIgnoreCase("off"))) {
        	Logger.logln("The value must be set ON or OFF");
            return false;
        } else {
        	prettyPrint = (value.trim().equalsIgnoreCase("on"))?true:false;
        	return true;
        }
	}

	
	public void printSettings() {
		super.printSettings();
		Logger.logln(" JMX");
		Logger.logln("   - MBEAN_ONAME\t" + ((setOName==null)?"":setOName));
		Logger.logln("   - PRETTY_PRINT\t" + ((prettyPrint)?"ON":"OFF"));
	}

}
