package dal.tool.trace.jmxer.cli.data;

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
