package dal.tool.trace.jmxer.cli.command;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import dal.tool.cli.Logger;
import dal.tool.cli.Logger.Level;
import dal.tool.cli.command.CommandMeta;
import dal.tool.trace.jmxer.JMXControl;
import dal.tool.trace.jmxer.JMXerConstant;
import dal.tool.trace.jmxer.cli.JmxCommandExecutor;
import dal.tool.util.DateUtil;
import dal.tool.util.jmx.JMXUtil;

public class InfoCommand extends JmxCommand {

	public static final CommandMeta commandMeta = new CommandMeta(InfoCommand.class, "INFO");

	
	public InfoCommand(String commandLine, JmxCommandExecutor commandExecutor) {
		super(commandLine, commandExecutor);
	}


	public void printHelp() {
        logln(" INFO");
        logln(" ----");
        logln("");
        logln(" Displays information of the current status");
        logln("");
        logln(" INFO");
	}

	
	public void doExecute() throws Exception {
		try {
			Map<String,String> tool_result = getToolInfo();
			Logger.logln(" Tool");
			Logger.logln("   - Version : " + tool_result.get("version"));
			Logger.logln("   - OS      : " + tool_result.get("os"));
			Logger.logln("   - Java    : " + tool_result.get("java"));
			Logger.logln("   - PID     : " + tool_result.get("pid"));
			Logger.logln("   - MODE    : " + (JMXControl.isAnalyzeMode ? "Analyze" : "Attach("+JMXControl.connectType.name()+")"));
			Logger.logln("   - TIME    : " + DateUtil.dateToString(DateUtil.FORMAT_DATETIME_SEC, new Date(Long.parseLong(tool_result.get("time")))));
			if(JMXControl.isAnalyzeMode) return;
			Map<String,String> target_result = getTargetVMInfo(getMBeanConnection());
			Logger.logln(" Target JVM");
			Logger.logln("   - OS      : " + target_result.get("os"));
			Logger.logln("   - Java    : " + target_result.get("java"));
			Logger.logln("   - PID     : " + target_result.get("pid"));
			Logger.logln("   - Name    : " + target_result.get("name"));
			Logger.logln("   - TIME    : " + DateUtil.dateToString(DateUtil.FORMAT_DATETIME_SEC, new Date(Long.parseLong(target_result.get("time")))));
		} catch(Exception e) {
			logln(Level.ERROR, "Failed to execute info command : " + e.getMessage());
			throw e;
		}
	}

	public static Map<String,String> getToolInfo() throws Exception {
		RuntimeMXBean tool_runtime = ManagementFactory.getRuntimeMXBean();
		String tool_os = System.getProperty("os.name");
		String tool_java_vendor = System.getProperty("java.vendor");
		String tool_java_vm = System.getProperty("java.vm.name");
		String tool_java_version = System.getProperty("java.runtime.version");
		String tool_pid = tool_runtime.getName().split("@")[0];
		String tool_time = String.valueOf(System.currentTimeMillis());
		
		Map<String,String> result = new HashMap<String,String>();
		result.put("version", JMXerConstant.JMXER_VERSION_STRING);
		result.put("os", tool_os);
		result.put("java", "["+tool_java_vendor+"] "+tool_java_vm+" "+tool_java_version);
		result.put("pid", tool_pid);
		result.put("time", tool_time);
		return result;
	}

	
	public static Map<String,String> getTargetVMInfo(MBeanServerConnection mbeanConn) throws Exception {
		List<String> keys = new ArrayList<String>();
		keys.add("sun.java.command");
		keys.add("os.name");
		keys.add("java.vendor");
		keys.add("java.vm.name");
		keys.add("java.runtime.version");
		Map<String,String> target_prop = JMXUtil.getRemoteSystemProperties(mbeanConn, keys);
		Map<String,Object> target_results = JMXUtil.getAttributeValuesWithName(mbeanConn, new ObjectName("java.lang:type=Runtime"), new String[]{ "Name", "StartTime", "Uptime" });
		String java_command = target_prop.get("sun.java.command");
		String target_name = (java_command==null) ? "" : java_command.split(" ")[0];
		String target_os = target_prop.get("os.name");
		String target_java_vendor = target_prop.get("java.vendor");
		String target_java_vm = target_prop.get("java.vm.name");
		String target_java_version = target_prop.get("java.runtime.version");
		String target_pid = ((String)target_results.get("Name")).split("@")[0];
		Long target_time = (Long)target_results.get("StartTime") + (Long)target_results.get("Uptime");

		Map<String,String> result = new HashMap<String,String>();
		result.put("name", target_name);
		result.put("os", target_os);
		result.put("java", "["+target_java_vendor+"] "+target_java_vm+" "+target_java_version);
		result.put("pid", target_pid);
		result.put("time", String.valueOf(target_time));
		return result;
	}

}
