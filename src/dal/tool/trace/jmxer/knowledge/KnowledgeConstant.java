package dal.tool.trace.jmxer.knowledge;

/**
 * MBean 지식 베이스 리소스 경로 및 TUI 표시용 상수
 *
 */
public final class KnowledgeConstant {

	public static final String RESOURCE_BASE = "knowledge/";
	public static final String MBEANS_DIR_NAME = "mbeans";
	public static final String MBEANS_SUBDIR = MBEANS_DIR_NAME + "/";
	public static final String TUI_DETAIL_TITLE_KB_SUFFIX = " (KB)";

	private KnowledgeConstant() {
	}

	public static String formatTuiDetailTitle(String baseTitle, boolean knowledgeUsed) {
		if(baseTitle == null) {
			baseTitle = "Detail";
		}
		if(!knowledgeUsed) {
			return baseTitle;
		}
		return baseTitle + TUI_DETAIL_TITLE_KB_SUFFIX;
	}

	public static String bundledYamlResourcePath(String fileName) {
		return RESOURCE_BASE + MBEANS_SUBDIR + fileName;
	}

}
