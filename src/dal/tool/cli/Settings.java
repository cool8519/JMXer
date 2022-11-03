package dal.tool.cli;

import dal.tool.cli.Logger.Level;

public class Settings {

	private Level logLevel = Level.INFO;
	private boolean showTime = false;
	private boolean showTiming = false;
	

	public Settings() {
		Logger.setShowLevel(logLevel);
	}
	
	public Level getLogLevel() {
		return logLevel;
	}

	public void setLogLevel(String value) {
		Level level = null;
		try {
			level = Level.valueOf(value.toUpperCase());
			if(level != null) {
				logLevel = level;
				Logger.setShowLevel(logLevel);
			}
		} catch(IllegalArgumentException iae) {
			Logger.logln("The value must be one of \"RESULT,ERROR,INFO,DEBUG\"");
		}
	}

	public boolean showTimeInPrompt() {
		return showTime;
	}
	
	public boolean setShowTimeInPrompt(String value) {
        if(value == null || (!value.equalsIgnoreCase("on") && !value.equalsIgnoreCase("off"))) {
        	Logger.logln("The value must be set ON or OFF");
            return false;
        } else {
        	showTime = (value.trim().equalsIgnoreCase("on"))?true:false;
        	return true;
        }
	}

	public boolean showTimingOnExecute() {
		return showTiming;
	}

	public boolean setShowTimingOnExecute(String value) {
        if(value == null || (!value.equalsIgnoreCase("on") && !value.equalsIgnoreCase("off"))) {
        	Logger.logln("The value must be set ON or OFF");
            return false;
        } else {
        	showTiming = (value.trim().equalsIgnoreCase("on"))?true:false;
        	return true;
        }
	}

	
	public void printSettings() {
		Logger.logln(" Common");
		Logger.logln("   - LOGLEVEL\t\t" + logLevel.name());
		Logger.logln("   - TIME\t\t" + ((showTime)?"ON":"OFF"));
		Logger.logln("   - TIMING\t\t" + ((showTiming)?"ON":"OFF"));
	}
	
}
