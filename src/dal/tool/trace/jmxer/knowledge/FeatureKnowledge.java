package dal.tool.trace.jmxer.knowledge;

import java.util.Collections;
import java.util.List;

/**
 * MBean attribute 또는 operation에 대한 지식(설명·예시·파라미터) 오버레이
 *
 */
public final class FeatureKnowledge {

	private final String description;
	private final String returnDescription;
	private final String example;
	private final List<ParamKnowledge> params;

	public FeatureKnowledge(String description, String returnDescription, String example, List<ParamKnowledge> params) {
		this.description = description;
		this.returnDescription = returnDescription;
		this.example = example;
		if(params == null || params.isEmpty()) {
			this.params = Collections.emptyList();
		} else {
			this.params = Collections.unmodifiableList(params);
		}
	}

	public String getDescription() {
		return description;
	}

	public String getReturnDescription() {
		return returnDescription;
	}

	public String getExample() {
		return example;
	}

	public List<ParamKnowledge> getParams() {
		return params;
	}

	public ParamKnowledge getParam(int index) {
		if(index < 0 || index >= params.size()) {
			return null;
		}
		return params.get(index);
	}

}
