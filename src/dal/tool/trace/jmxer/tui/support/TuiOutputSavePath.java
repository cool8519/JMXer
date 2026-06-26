package dal.tool.trace.jmxer.tui.support;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import dal.tool.trace.jmxer.tui.output.TuiOutputConstant;
import dal.tool.util.StringUtil;

/**
 * Output 저장 경로를 기억하고 기본 저장 파일명을 만든다.
 * 팝업 기본값은 항상 현재 시각이 반영된 파일명이며, 이전에 저장한 디렉터리만 재사용한다.
 *
 */
public final class TuiOutputSavePath {

	private static final DateTimeFormatter DEFAULT_FILENAME_TIMESTAMP =
		DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

	private static String lastEnteredPath;

	private TuiOutputSavePath() {}

	public static String getInitialPath() {
		String defaultFileName = buildDefaultFileName();
		if(lastEnteredPath == null || lastEnteredPath.trim().length() == 0) {
			return defaultFileName;
		}
		String trimmed = StringUtil.stripOuterFilenameQuotes(lastEnteredPath.trim());
		File last = new File(trimmed);
		if(last.isDirectory()) {
			return new File(last, defaultFileName).getPath();
		}
		File parent = last.getParentFile();
		if(parent != null) {
			return new File(parent, defaultFileName).getPath();
		}
		return defaultFileName;
	}

	public static String buildDefaultFileName() {
		String ts = LocalDateTime.now().format(DEFAULT_FILENAME_TIMESTAMP);
		return TuiOutputConstant.OUTPUT_SAVE_DEFAULT_FILENAME_PREFIX + ts
			+ TuiOutputConstant.OUTPUT_SAVE_DEFAULT_FILENAME_SUFFIX;
	}

	public static void rememberEnteredPath(String path) {
		if(path == null) {
			return;
		}
		String trimmed = path.trim();
		if(trimmed.length() > 0) {
			lastEnteredPath = trimmed;
		}
	}

	public static File resolveSaveFile(String pathInput) throws IOException {
		String resolvedPath = pathInput;
		if(resolvedPath == null || resolvedPath.trim().isEmpty()) {
			resolvedPath = buildDefaultFileName();
		} else {
			resolvedPath = StringUtil.stripOuterFilenameQuotes(resolvedPath.trim());
		}
		File file = new File(resolvedPath);
		if(file.exists() && file.isDirectory()) {
			file = new File(resolvedPath + File.separator + buildDefaultFileName());
		}
		return file.getAbsoluteFile();
	}
}
