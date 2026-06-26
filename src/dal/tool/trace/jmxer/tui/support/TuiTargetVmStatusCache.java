package dal.tool.trace.jmxer.tui.support;

import java.util.Map;

import dal.tool.trace.jmxer.cli.command.InfoCommand;
import dal.tool.util.jmx.MBeanConnector;

/**
 * TUI 상태 줄용 Target JVM 요약을 캐시한다(InfoCommand.getTargetVMInfo 기반).
 *
 */
public final class TuiTargetVmStatusCache {
	private static final String LABEL = "Target JVM";
	private static final String PAIR_SEP = ", ";
	private static final String FIELD_PID = "PID";
	private static final String FIELD_NAME = "Name";
	private static final String FIELD_OS = "OS";
	private static final String FIELD_JAVA = "Java";
	private static final int MAX_VALUE_CHARS = 32;

	private String cachedConnectorKey = "";
	private String cachedSuffix = "";

	public void refresh(MBeanConnector connector) {
		cachedConnectorKey = connectorKey(connector);
		cachedSuffix = fetchSuffix(connector);
	}

	public String getTargetJvmSuffix(MBeanConnector connector) {
		String key = connectorKey(connector);
		if(key.length() == 0) {
			return "";
		}
		if(!key.equals(cachedConnectorKey)) {
			refresh(connector);
		}
		return cachedSuffix;
	}

	private static String connectorKey(MBeanConnector connector) {
		if(connector == null || connector.getMBeanConnection() == null) {
			return "";
		}
		return connector.getConnectType().name() + ":" + connector.getTargetName();
	}

	private static String fetchSuffix(MBeanConnector connector) {
		if(connector == null || connector.getMBeanConnection() == null) {
			return "";
		}
		try {
			Map<String, String> info = InfoCommand.getTargetVMInfo(connector.getMBeanConnection());
			return formatTargetJvm(info);
		} catch(Exception e) {
			return "";
		}
	}

	private static String formatTargetJvm(Map<String, String> info) {
		if(info == null || info.isEmpty()) {
			return "";
		}
		StringBuilder pairs = new StringBuilder();
		appendPair(pairs, FIELD_PID, nullToEmpty(info.get("pid")));
		appendPair(pairs, FIELD_NAME, displayName(nullToEmpty(info.get("name"))));
		appendPair(pairs, FIELD_OS, nullToEmpty(info.get("os")));
		appendPair(pairs, FIELD_JAVA, displayJava(nullToEmpty(info.get("java"))));
		if(pairs.length() == 0) {
			return "";
		}
		return LABEL + ": [" + pairs + "]";
	}

	private static void appendPair(StringBuilder pairs, String field, String value) {
		if(value == null || value.length() == 0) {
			return;
		}
		if(pairs.length() > 0) {
			pairs.append(PAIR_SEP);
		}
		pairs.append(field).append('=').append(fitMaxChars(value, MAX_VALUE_CHARS));
	}

	private static String displayName(String name) {
		if(name.length() == 0) {
			return "";
		}
		int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
		if(slash >= 0 && slash < name.length() - 1) {
			return name.substring(slash + 1);
		}
		return name;
	}

	private static String displayJava(String javaFull) {
		if(javaFull.length() == 0) {
			return "";
		}
		int lastSpace = javaFull.lastIndexOf(' ');
		if(lastSpace >= 0 && lastSpace < javaFull.length() - 1) {
			return javaFull.substring(lastSpace + 1);
		}
		return javaFull;
	}

	private static String fitMaxChars(String text, int maxChars) {
		if(text.length() <= maxChars) {
			return text;
		}
		if(maxChars <= 3) {
			return text.substring(0, maxChars);
		}
		return text.substring(0, maxChars - 3) + "...";
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}
}
