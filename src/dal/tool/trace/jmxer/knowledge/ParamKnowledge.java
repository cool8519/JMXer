package dal.tool.trace.jmxer.knowledge;

/**
 * operation 파라미터에 대한 지식(이름·타입·설명·예시)
 *
 */
public final class ParamKnowledge {

	private final String name;
	private final String type;
	private final String description;
	private final String example;

	public ParamKnowledge(String name, String type, String description, String example) {
		this.name = name;
		this.type = type;
		this.description = description;
		this.example = example;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public String getDescription() {
		return description;
	}

	public String getExample() {
		return example;
	}

}
