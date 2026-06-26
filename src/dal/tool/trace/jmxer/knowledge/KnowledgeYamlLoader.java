package dal.tool.trace.jmxer.knowledge;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.yaml.snakeyaml.Yaml;

/**
 * YAML 파일에서 MBean 지식 항목을 읽어 파싱한다.
 *
 */
public final class KnowledgeYamlLoader {

	private KnowledgeYamlLoader() {
	}

	public static List<MBeanKnowledge> loadFromStream(InputStream in) throws Exception {
		if(in == null) {
			return new ArrayList<MBeanKnowledge>();
		}
		Yaml yaml = new Yaml();
		Object root = yaml.load(in);
		return parseRoot(root);
	}

	public static List<MBeanKnowledge> loadFromFile(File file) throws Exception {
		if(file == null || !file.isFile()) {
			return new ArrayList<MBeanKnowledge>();
		}
		FileInputStream fis = new FileInputStream(file);
		try {
			return loadFromStream(fis);
		} finally {
			fis.close();
		}
	}

	public static List<MBeanKnowledge> loadBundledFile(String resourcePath) throws Exception {
		InputStream in = KnowledgeYamlLoader.class.getClassLoader().getResourceAsStream(resourcePath);
		if(in == null) {
			return new ArrayList<MBeanKnowledge>();
		}
		try {
			return loadFromStream(in);
		} finally {
			in.close();
		}
	}

	public static List<String> listYamlFileNames(File yamlDir) {
		List<String> names = new ArrayList<String>();
		if(yamlDir == null || !yamlDir.isDirectory()) {
			return names;
		}
		File[] yamlFiles = yamlDir.listFiles();
		if(yamlFiles == null) {
			return names;
		}
		for(File file : yamlFiles) {
			if(file == null || !file.isFile()) {
				continue;
			}
			if(isYamlFileName(file.getName())) {
				names.add(file.getName());
			}
		}
		Collections.sort(names);
		return names;
	}

	public static List<String> listBundledYamlFileNames() {
		return listBundledYamlFileNames(KnowledgeYamlLoader.class.getClassLoader());
	}

	public static List<String> listBundledYamlFileNames(ClassLoader classLoader) {
		if(classLoader == null) {
			return new ArrayList<String>();
		}
		String prefix = KnowledgeConstant.RESOURCE_BASE + KnowledgeConstant.MBEANS_SUBDIR;
		List<String> names = listBundledYamlFromResourceUrl(classLoader, prefix);
		if(!names.isEmpty()) {
			return names;
		}
		return listBundledYamlFromCodeSourceJar(prefix);
	}

	private static List<String> listBundledYamlFromResourceUrl(ClassLoader classLoader, String prefix) {
		URL dirUrl = classLoader.getResource(prefix);
		if(dirUrl == null) {
			return new ArrayList<String>();
		}
		if("file".equals(dirUrl.getProtocol())) {
			try {
				File dir = new File(dirUrl.toURI());
				return listYamlFileNames(dir);
			} catch(Exception e) {
				return new ArrayList<String>();
			}
		}
		if("jar".equals(dirUrl.getProtocol())) {
			try {
				JarURLConnection connection = (JarURLConnection)dirUrl.openConnection();
				return collectYamlNamesFromJar(connection.getJarFile(), prefix);
			} catch(Exception e) {
				return new ArrayList<String>();
			}
		}
		return new ArrayList<String>();
	}

	private static List<String> listBundledYamlFromCodeSourceJar(String prefix) {
		CodeSource codeSource = KnowledgeYamlLoader.class.getProtectionDomain().getCodeSource();
		if(codeSource == null || codeSource.getLocation() == null) {
			return new ArrayList<String>();
		}
		try {
			File jarFile = new File(codeSource.getLocation().toURI());
			if(!jarFile.isFile()) {
				return new ArrayList<String>();
			}
			JarFile jar = new JarFile(jarFile);
			try {
				return collectYamlNamesFromJar(jar, prefix);
			} finally {
				jar.close();
			}
		} catch(Exception e) {
			return new ArrayList<String>();
		}
	}

	private static List<String> collectYamlNamesFromJar(JarFile jar, String prefix) {
		List<String> names = new ArrayList<String>();
		if(jar == null) {
			return names;
		}
		Enumeration<JarEntry> entries = jar.entries();
		while(entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			if(entry == null || entry.isDirectory()) {
				continue;
			}
			String entryName = entry.getName();
			if(!entryName.startsWith(prefix)) {
				continue;
			}
			String relativeName = entryName.substring(prefix.length());
			if(relativeName.indexOf('/') >= 0) {
				continue;
			}
			if(isYamlFileName(relativeName)) {
				names.add(relativeName);
			}
		}
		Collections.sort(names);
		return names;
	}

	static boolean isYamlFileName(String fileName) {
		if(fileName == null) {
			return false;
		}
		String lower = fileName.toLowerCase();
		return lower.endsWith(".yaml") || lower.endsWith(".yml");
	}

	@SuppressWarnings("unchecked")
	private static List<MBeanKnowledge> parseRoot(Object root) {
		List<MBeanKnowledge> result = new ArrayList<MBeanKnowledge>();
		if(!(root instanceof Map)) {
			return result;
		}
		Map<String, Object> rootMap = (Map<String, Object>)root;
		Object entriesObj = rootMap.get("entries");
		if(!(entriesObj instanceof List)) {
			return result;
		}
		List<Object> entries = (List<Object>)entriesObj;
		for(Object entryObj : entries) {
			if(entryObj instanceof Map) {
				MBeanKnowledge entry = parseEntry((Map<String, Object>)entryObj);
				if(entry != null) {
					result.add(entry);
				}
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private static MBeanKnowledge parseEntry(Map<String, Object> entryMap) {
		String objectNamePattern = null;
		String classNamePattern = null;
		Object matchObj = entryMap.get("match");
		if(matchObj instanceof Map) {
			Map<String, Object> matchMap = (Map<String, Object>)matchObj;
			objectNamePattern = asString(matchMap.get("objectName"));
			classNamePattern = asString(matchMap.get("className"));
		}
		if((objectNamePattern == null || objectNamePattern.trim().length() == 0)
				&& (classNamePattern == null || classNamePattern.trim().length() == 0)) {
			return null;
		}
		String description = asString(entryMap.get("description"));
		Map<String, FeatureKnowledge> attributes = parseFeatureMap(entryMap.get("attributes"), false);
		Map<String, FeatureKnowledge> operations = parseFeatureMap(entryMap.get("operations"), true);
		int specificity = computeSpecificityScore(objectNamePattern, classNamePattern);
		return new MBeanKnowledge(objectNamePattern, classNamePattern, description, attributes, operations, specificity);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, FeatureKnowledge> parseFeatureMap(Object featureObj, boolean operation) {
		Map<String, FeatureKnowledge> result = new LinkedHashMap<String, FeatureKnowledge>();
		if(!(featureObj instanceof Map)) {
			return result;
		}
		Map<String, Object> featureMap = (Map<String, Object>)featureObj;
		for(Map.Entry<String, Object> e : featureMap.entrySet()) {
			if(!(e.getValue() instanceof Map)) {
				continue;
			}
			Map<String, Object> valueMap = (Map<String, Object>)e.getValue();
			String description = asString(valueMap.get("description"));
			String returnDescription = operation ? asString(valueMap.get("return")) : null;
			String example = asString(valueMap.get("example"));
			List<ParamKnowledge> params = parseParams(valueMap.get("params"));
			result.put(e.getKey(), new FeatureKnowledge(description, returnDescription, example, params));
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private static List<ParamKnowledge> parseParams(Object paramsObj) {
		List<ParamKnowledge> result = new ArrayList<ParamKnowledge>();
		if(!(paramsObj instanceof List)) {
			return result;
		}
		List<Object> params = (List<Object>)paramsObj;
		for(Object paramObj : params) {
			if(!(paramObj instanceof Map)) {
				continue;
			}
			Map<String, Object> paramMap = (Map<String, Object>)paramObj;
			String name = asString(paramMap.get("name"));
			String type = asString(paramMap.get("type"));
			String description = asString(paramMap.get("description"));
			String example = asString(paramMap.get("example"));
			result.add(new ParamKnowledge(name, type, description, example));
		}
		return result;
	}

	private static String asString(Object value) {
		if(value == null) {
			return null;
		}
		String text = String.valueOf(value).trim();
		return text.length() == 0 ? null : text;
	}

	public static int computeSpecificityScore(String objectNamePattern, String classNamePattern) {
		int score = 0;
		if(objectNamePattern != null && objectNamePattern.trim().length() > 0) {
			score += 1000;
			score += objectNamePattern.length();
			score -= countChar(objectNamePattern, '*') * 10;
			score -= countChar(objectNamePattern, '?') * 5;
		}
		if(classNamePattern != null && classNamePattern.trim().length() > 0) {
			score += 100;
			score += classNamePattern.length();
			score -= countChar(classNamePattern, '*') * 5;
		}
		return score;
	}

	private static int countChar(String text, char ch) {
		int count = 0;
		for(int i = 0; i < text.length(); i++) {
			if(text.charAt(i) == ch) {
				count++;
			}
		}
		return count;
	}

}
