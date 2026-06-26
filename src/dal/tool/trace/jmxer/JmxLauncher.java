package dal.tool.trace.jmxer;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * JAR 진입점. 런타임 Java 버전을 확인한 뒤 JMXControl을 기동한다.
 *
 */
public class JmxLauncher {

	private static final String ENTRY_CLASS = "dal.tool.trace.jmxer.JMXControl";
	private static final String MANIFEST_MIN_RUNTIME = "JMXer-Min-Runtime";
	private static final String MANIFEST_ALT_JAR = "JMXer-Alt-Jar";
	private static final int DEFAULT_MIN_RUNTIME = 17;

	public static void main(String[] args) throws Exception {
		int minRuntime = readMinRuntimeMajor();
		int currentRuntime = getRuntimeMajorVersion();
		if(currentRuntime < minRuntime) {
			printVersionError(minRuntime, currentRuntime);
			System.exit(1);
		}
		invokeEntryMain(args);
	}

	private static void invokeEntryMain(String[] args) throws Exception {
		try {
			Class<?> entry = Class.forName(ENTRY_CLASS);
			Method main = entry.getMethod("main", new Class[] { String[].class });
			main.invoke(null, new Object[] { args });
		} catch(InvocationTargetException e) {
			Throwable cause = e.getCause();
			if(cause instanceof Error) {
				throw (Error)cause;
			}
			if(cause instanceof Exception) {
				throw (Exception)cause;
			}
			throw e;
		} catch(UnsupportedClassVersionError e) {
			int minRuntime = readMinRuntimeMajor();
			printVersionError(minRuntime, getRuntimeMajorVersion());
			System.exit(1);
		}
	}

	private static void printVersionError(int requiredMajor, int currentMajor) {
		String runtimeVersion = System.getProperty("java.runtime.version");
		if(runtimeVersion == null || runtimeVersion.trim().equals("")) {
			runtimeVersion = System.getProperty("java.version");
		}
		if(runtimeVersion == null) {
			runtimeVersion = "unknown";
		}

		System.err.println("ERROR: This JAR requires Java " + requiredMajor + " or newer.");
		System.err.println("       Current runtime: Java " + formatRuntimeMajor(currentMajor) + " (" + runtimeVersion.trim() + ").");
		String altJar = readAltJarName();
		if(altJar != null && altJar.trim().length() > 0) {
			System.err.println("       For Java 8 through Java " + (requiredMajor - 1) + ", use " + altJar.trim() + " instead.");
			System.err.println("       Example: java -jar " + altJar.trim() + " [ARGUMENTS]");
		} else {
			System.err.println("       Install JDK " + requiredMajor + "+ and run this JAR again.");
		}
	}

	private static String formatRuntimeMajor(int major) {
		if(major <= 0) {
			return "unknown";
		}
		return String.valueOf(major);
	}

	private static int readMinRuntimeMajor() {
		String value = readManifestAttribute(MANIFEST_MIN_RUNTIME);
		if(value == null || value.trim().equals("")) {
			return DEFAULT_MIN_RUNTIME;
		}
		try {
			return Integer.parseInt(value.trim());
		} catch(NumberFormatException e) {
			return DEFAULT_MIN_RUNTIME;
		}
	}

	private static String readAltJarName() {
		return readManifestAttribute(MANIFEST_ALT_JAR);
	}

	private static String readManifestAttribute(String name) {
		InputStream in = null;
		try {
			URL codeUrl = JmxLauncher.class.getProtectionDomain().getCodeSource().getLocation();
			if(codeUrl == null) {
				return null;
			}
			String code = codeUrl.toString();
			URL manifestUrl = null;
			if(code.startsWith("jar:")) {
				manifestUrl = new URL(code.substring(0, code.indexOf('!') + 1) + "/META-INF/MANIFEST.MF");
			} else if(code.endsWith(".jar") || code.endsWith(".jar/")) {
				String jarPath = code;
				if(jarPath.endsWith("/")) {
					jarPath = jarPath.substring(0, jarPath.length() - 1);
				}
				manifestUrl = new URL("jar:" + jarPath + "!/META-INF/MANIFEST.MF");
			}
			if(manifestUrl == null) {
				return null;
			}
			in = manifestUrl.openStream();
			Manifest manifest = new Manifest(in);
			Attributes attrs = manifest.getMainAttributes();
			return attrs.getValue(name);
		} catch(Exception e) {
			return null;
		} finally {
			if(in != null) {
				try {
					in.close();
				} catch(Exception ignored) {
				}
			}
		}
	}

	static int getRuntimeMajorVersion() {
		String spec = System.getProperty("java.specification.version");
		if(spec == null || spec.trim().equals("")) {
			return 0;
		}
		spec = spec.trim();
		if(spec.startsWith("1.")) {
			try {
				return Integer.parseInt(spec.substring(2));
			} catch(NumberFormatException e) {
				return 0;
			}
		}
		int dot = spec.indexOf('.');
		String major = dot > 0 ? spec.substring(0, dot) : spec;
		try {
			return Integer.parseInt(major);
		} catch(NumberFormatException e) {
			return 0;
		}
	}
}
