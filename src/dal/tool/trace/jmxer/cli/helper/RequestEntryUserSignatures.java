package dal.tool.trace.jmxer.cli.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import dal.tool.cli.Logger;

/**
 * 사용자 정의 request-entry 시그니처 파일 로드 결과
 *
 */
public final class RequestEntryUserSignatures {

	private static final String LOAD_SUMMARY_PREFIX = "Loaded ";
	private static final String LOAD_SUMMARY_SUFFIX = " user-defined request-entry signature(s) from '";

	private final Set<String> signatures;
	private final boolean readError;
	private final boolean fileRead;
	private final String resolvedPath;

	private RequestEntryUserSignatures(Set<String> signatures, boolean readError, boolean fileRead, String resolvedPath) {
		this.signatures = signatures == null
			? Collections.<String>emptySet()
			: Collections.unmodifiableSet(signatures);
		this.readError = readError;
		this.fileRead = fileRead;
		this.resolvedPath = resolvedPath;
	}

	public Set<String> getSignatures() {
		return signatures;
	}

	public boolean isReadError() {
		return readError;
	}

	public boolean wasFileRead() {
		return fileRead;
	}

	public String getResolvedPath() {
		return resolvedPath;
	}

	public void logLoadSummaryIfRead() {
		if(!fileRead || readError) {
			return;
		}
		Logger.logln(LOAD_SUMMARY_PREFIX + signatures.size() + LOAD_SUMMARY_SUFFIX + resolvedPath + "'.\n");
	}

	public static RequestEntryUserSignatures load(String path) {
		if(path == null || path.trim().isEmpty()) {
			return new RequestEntryUserSignatures(Collections.<String>emptySet(), false, false, null);
		}
		String resolvedPath = resolvePath(path.trim());
		File file = new File(resolvedPath);
		if(!file.isFile()) {
			return new RequestEntryUserSignatures(Collections.<String>emptySet(), false, false, resolvedPath);
		}
		BufferedReader reader = null;
		try {
			Set<String> loaded = new HashSet<String>();
			reader = new BufferedReader(new InputStreamReader(
				new FileInputStream(file), StandardCharsets.UTF_8));
			String line;
			while((line = reader.readLine()) != null) {
				String trimmed = line.trim();
				if(trimmed.isEmpty() || trimmed.startsWith("#")) {
					continue;
				}
				loaded.add(trimmed);
			}
			return new RequestEntryUserSignatures(loaded, false, true, resolvedPath);
		} catch(Exception e) {
			return new RequestEntryUserSignatures(Collections.<String>emptySet(), true, false, resolvedPath);
		} finally {
			if(reader != null) {
				try {
					reader.close();
				} catch(Exception e) {}
			}
		}
	}

	public static boolean isEmptyPath(String path) {
		return path == null || path.trim().isEmpty();
	}

	public static boolean isExistingFile(String path) {
		if(isEmptyPath(path)) {
			return false;
		}
		return new File(resolvePath(path.trim())).isFile();
	}

	static String resolvePath(String path) {
		if(path.startsWith("~/")) {
			return new File(System.getProperty("user.home"), path.substring(2)).getPath();
		}
		if("~".equals(path)) {
			return System.getProperty("user.home");
		}
		return path;
	}
}
