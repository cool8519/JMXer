package dal.tool.trace.jmxer.knowledge;

import java.util.Collections;
import java.util.Map;

/**
 * 단일 MBean에 대한 지식(매칭 패턴·설명·attribute/operation 오버레이)
 *
 */
public final class MBeanKnowledge {

	private final String objectNamePattern;
	private final String classNamePattern;
	private final String description;
	private final Map<String, FeatureKnowledge> attributes;
	private final Map<String, FeatureKnowledge> operations;
	private final int specificityScore;

	public MBeanKnowledge(String objectNamePattern, String classNamePattern, String description,
			Map<String, FeatureKnowledge> attributes, Map<String, FeatureKnowledge> operations, int specificityScore) {
		this.objectNamePattern = objectNamePattern;
		this.classNamePattern = classNamePattern;
		this.description = description;
		this.attributes = attributes == null ? Collections.<String, FeatureKnowledge>emptyMap() : Collections.unmodifiableMap(attributes);
		this.operations = operations == null ? Collections.<String, FeatureKnowledge>emptyMap() : Collections.unmodifiableMap(operations);
		this.specificityScore = specificityScore;
	}

	public String getObjectNamePattern() {
		return objectNamePattern;
	}

	public String getClassNamePattern() {
		return classNamePattern;
	}

	public String getDescription() {
		return description;
	}

	public Map<String, FeatureKnowledge> getAttributes() {
		return attributes;
	}

	public Map<String, FeatureKnowledge> getOperations() {
		return operations;
	}

	public int getSpecificityScore() {
		return specificityScore;
	}

	public FeatureKnowledge getAttribute(String name) {
		if(name == null) {
			return null;
		}
		return attributes.get(name);
	}

	public FeatureKnowledge getOperation(String signatureKey) {
		if(signatureKey == null) {
			return null;
		}
		return operations.get(signatureKey);
	}

}
