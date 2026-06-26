package dal.tool.trace.jmxer.knowledge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.management.MBeanInfo;
import javax.management.ObjectName;

import dal.tool.cli.Logger;

/**
 * 번들 MBean 지식 항목을 보관·조회하는 싱글톤 레지스트리
 *
 */
public final class KnowledgeBase {

	private static final KnowledgeBase INSTANCE = new KnowledgeBase();

	private final List<MBeanKnowledge> bundledEntries = new ArrayList<MBeanKnowledge>();
	private volatile boolean bundledLoaded = false;

	private KnowledgeBase() {
	}

	public static KnowledgeBase getInstance() {
		return INSTANCE;
	}

	public synchronized void loadBundled() {
		if(bundledLoaded) {
			return;
		}
		bundledEntries.clear();
		try {
			List<String> fileNames = KnowledgeYamlLoader.listBundledYamlFileNames();
			for(String fileName : fileNames) {
				String resourcePath = KnowledgeConstant.bundledYamlResourcePath(fileName);
				List<MBeanKnowledge> entries = KnowledgeYamlLoader.loadBundledFile(resourcePath);
				bundledEntries.addAll(entries);
			}
		} catch(Exception e) {
			Logger.logln(Logger.Level.WARNING, "Failed to load bundled MBean knowledge: " + e.getMessage());
		}
		bundledLoaded = true;
	}

	public MBeanKnowledge findBestMatch(ObjectName objectName, MBeanInfo mbeanInfo) {
		ensureBundledLoaded();
		return findBestInList(bundledEntries, objectName, mbeanInfo);
	}

	public List<MBeanKnowledge> getBundledEntries() {
		ensureBundledLoaded();
		return Collections.unmodifiableList(bundledEntries);
	}

	private void ensureBundledLoaded() {
		if(!bundledLoaded) {
			loadBundled();
		}
	}

	private static MBeanKnowledge findBestInList(List<MBeanKnowledge> entries, ObjectName objectName, MBeanInfo mbeanInfo) {
		MBeanKnowledge best = null;
		for(MBeanKnowledge entry : entries) {
			if(!matches(entry, objectName, mbeanInfo)) {
				continue;
			}
			if(best == null || entry.getSpecificityScore() > best.getSpecificityScore()) {
				best = entry;
			}
		}
		return best;
	}

	private static boolean matches(MBeanKnowledge entry, ObjectName objectName, MBeanInfo mbeanInfo) {
		boolean objectNameMatched = false;
		boolean classNameMatched = false;
		if(entry.getObjectNamePattern() != null && entry.getObjectNamePattern().trim().length() > 0) {
			try {
				ObjectName pattern = new ObjectName(entry.getObjectNamePattern().trim());
				objectNameMatched = pattern.apply(objectName);
			} catch(Exception e) {
				objectNameMatched = false;
			}
		}
		if(entry.getClassNamePattern() != null && entry.getClassNamePattern().trim().length() > 0 && mbeanInfo != null) {
			classNameMatched = matchesClassName(entry.getClassNamePattern().trim(), mbeanInfo.getClassName());
		}
		if(entry.getObjectNamePattern() != null && entry.getObjectNamePattern().trim().length() > 0) {
			return objectNameMatched;
		}
		return classNameMatched;
	}

	private static boolean matchesClassName(String pattern, String className) {
		if(className == null) {
			return false;
		}
		if(pattern.indexOf('*') < 0 && pattern.indexOf('?') < 0) {
			return className.equals(pattern);
		}
		return className.matches(toRegex(pattern));
	}

	private static String toRegex(String pattern) {
		StringBuilder regex = new StringBuilder("^");
		for(int i = 0; i < pattern.length(); i++) {
			char ch = pattern.charAt(i);
			if(ch == '*') {
				regex.append(".*");
			} else if(ch == '?') {
				regex.append('.');
			} else if(".[]{}()+-^$|".indexOf(ch) >= 0) {
				regex.append('\\').append(ch);
			} else {
				regex.append(ch);
			}
		}
		regex.append('$');
		return regex.toString();
	}

}
