package dal.tool.trace.jmxer.cli.data;

/**
 * RECORD VIEW SEARCH 결과 한 항목(스택 문자열·샘플 구간)
 *
 */
public class RecordSearch {

	public int fromIndex;
	public int toIndex;
	public int count;
	public String foundString;

	public RecordSearch(String foundString, int fromIndex) {
		this.foundString = foundString;
		this.fromIndex = fromIndex;
		this.toIndex = fromIndex;
		this.count = 1;
	}	
	
}
