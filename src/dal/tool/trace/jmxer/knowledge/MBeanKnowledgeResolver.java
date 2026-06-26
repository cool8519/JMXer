package dal.tool.trace.jmxer.knowledge;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanFeatureInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ObjectName;

import dal.tool.util.jmx.JMXHelper;

/**
 * 런타임 JMX 메타데이터 위에 번들 지식을 겹쳐 표시용 설명·예시를 결정한다.<br>
 * 우선순위: 번들 지식 &gt; 런타임 메타데이터
 *
 */
public final class MBeanKnowledgeResolver {

	private static final MBeanKnowledgeResolver INSTANCE = new MBeanKnowledgeResolver();

	private MBeanKnowledgeResolver() {
	}

	public static MBeanKnowledgeResolver getInstance() {
		return INSTANCE;
	}

	public String resolveMBeanDescription(ObjectName objectName, MBeanInfo mbeanInfo) {
		MBeanKnowledge knowledge = findKnowledge(objectName, mbeanInfo);
		if(knowledge != null && hasText(knowledge.getDescription())) {
			return knowledge.getDescription();
		}
		if(mbeanInfo == null) {
			return null;
		}
		return mbeanInfo.getDescription();
	}

	public String resolveAttributeDescription(ObjectName objectName, MBeanInfo mbeanInfo, MBeanAttributeInfo attribute) {
		if(attribute == null) {
			return null;
		}
		FeatureKnowledge feature = findAttributeKnowledge(objectName, mbeanInfo, attribute.getName());
		if(feature != null && hasText(feature.getDescription())) {
			return feature.getDescription();
		}
		return attribute.getDescription();
	}

	public String resolveAttributeExample(ObjectName objectName, MBeanInfo mbeanInfo, MBeanAttributeInfo attribute) {
		if(attribute == null) {
			return null;
		}
		FeatureKnowledge feature = findAttributeKnowledge(objectName, mbeanInfo, attribute.getName());
		if(feature != null && hasText(feature.getExample())) {
			return feature.getExample();
		}
		return null;
	}

	public String resolveOperationDescription(ObjectName objectName, MBeanInfo mbeanInfo, MBeanOperationInfo operation) {
		if(operation == null) {
			return null;
		}
		FeatureKnowledge feature = findOperationKnowledge(objectName, mbeanInfo, operation);
		if(feature != null && hasText(feature.getDescription())) {
			return feature.getDescription();
		}
		return operation.getDescription();
	}

	public String resolveOperationReturnDescription(ObjectName objectName, MBeanInfo mbeanInfo, MBeanOperationInfo operation) {
		if(operation == null) {
			return null;
		}
		FeatureKnowledge feature = findOperationKnowledge(objectName, mbeanInfo, operation);
		if(feature != null && hasText(feature.getReturnDescription())) {
			return feature.getReturnDescription();
		}
		return null;
	}

	public String resolveParamName(ObjectName objectName, MBeanInfo mbeanInfo, MBeanOperationInfo operation, int index) {
		MBeanParameterInfo[] signature = operation == null ? null : operation.getSignature();
		if(signature == null || index < 0 || index >= signature.length) {
			return null;
		}
		ParamKnowledge paramKnowledge = findParamKnowledge(objectName, mbeanInfo, operation, index);
		if(paramKnowledge != null && hasText(paramKnowledge.getName()) && !isGenericParamName(paramKnowledge.getName())) {
			return paramKnowledge.getName();
		}
		String runtimeName = signature[index].getName();
		if(hasText(runtimeName) && !isGenericParamName(runtimeName)) {
			return runtimeName;
		}
		if(paramKnowledge != null && hasText(paramKnowledge.getName())) {
			return paramKnowledge.getName();
		}
		return runtimeName;
	}

	public String resolveParamDescription(ObjectName objectName, MBeanInfo mbeanInfo, MBeanOperationInfo operation, int index) {
		ParamKnowledge paramKnowledge = findParamKnowledge(objectName, mbeanInfo, operation, index);
		if(paramKnowledge != null && hasText(paramKnowledge.getDescription())) {
			return paramKnowledge.getDescription();
		}
		MBeanParameterInfo[] signature = operation == null ? null : operation.getSignature();
		if(signature != null && index >= 0 && index < signature.length) {
			return signature[index].getDescription();
		}
		return null;
	}

	/**
	 * 지식 베이스의 attribute 설명만 반환한다. 입력 대화상자용이며 런타임 메타데이터로 대체하지 않는다.
	 * @param objectName MBean ObjectName
	 * @param mbeanInfo MBean 정보
	 * @param attribute 대상 attribute
	 * @return 지식 베이스 설명. 없으면 null
	 */
	public String resolveKnowledgeAttributeDescription(ObjectName objectName, MBeanInfo mbeanInfo, MBeanAttributeInfo attribute) {
		if(attribute == null) {
			return null;
		}
		FeatureKnowledge feature = findAttributeKnowledge(objectName, mbeanInfo, attribute.getName());
		if(feature != null && hasText(feature.getDescription())) {
			return feature.getDescription();
		}
		return null;
	}

	/**
	 * 지식 베이스의 파라미터 설명만 반환한다. 입력 대화상자용이며 런타임 메타데이터로 대체하지 않는다.
	 * @param objectName MBean ObjectName
	 * @param mbeanInfo MBean 정보
	 * @param operation 대상 operation
	 * @param index 파라미터 인덱스
	 * @return 지식 베이스 설명. 없으면 null
	 */
	public String resolveKnowledgeParamDescription(ObjectName objectName, MBeanInfo mbeanInfo, MBeanOperationInfo operation, int index) {
		ParamKnowledge paramKnowledge = findParamKnowledge(objectName, mbeanInfo, operation, index);
		if(paramKnowledge != null && hasText(paramKnowledge.getDescription())) {
			return paramKnowledge.getDescription();
		}
		return null;
	}

	public String resolveParamExample(ObjectName objectName, MBeanInfo mbeanInfo, MBeanOperationInfo operation, int index) {
		ParamKnowledge paramKnowledge = findParamKnowledge(objectName, mbeanInfo, operation, index);
		if(paramKnowledge != null && hasText(paramKnowledge.getExample())) {
			return paramKnowledge.getExample();
		}
		return null;
	}

	public String formatParametersLine(ObjectName objectName, MBeanInfo mbeanInfo, MBeanOperationInfo operation) {
		if(operation == null) {
			return "";
		}
		MBeanParameterInfo[] signature = operation.getSignature();
		if(signature == null || signature.length == 0) {
			return "";
		}
		StringBuilder buffer = new StringBuilder();
		for(int i = 0; i < signature.length; i++) {
			if(i > 0) {
				buffer.append(',');
			}
			String name = resolveParamName(objectName, mbeanInfo, operation, i);
			if(name == null || name.trim().length() == 0) {
				name = "arg" + (i + 1);
			}
			buffer.append(name).append('(').append(JMXHelper.toTypeString(signature[i].getType())).append(')');
		}
		return buffer.toString();
	}

	public String formatParameterDetailsBlock(ObjectName objectName, MBeanInfo mbeanInfo, MBeanOperationInfo operation) {
		if(operation == null) {
			return null;
		}
		if(findOperationKnowledge(objectName, mbeanInfo, operation) == null) {
			return null;
		}
		MBeanParameterInfo[] signature = operation.getSignature();
		if(signature == null || signature.length == 0) {
			return null;
		}
		StringBuilder buffer = new StringBuilder();
		for(int i = 0; i < signature.length; i++) {
			String name = resolveParamName(objectName, mbeanInfo, operation, i);
			if(name == null || name.trim().length() == 0) {
				name = "arg" + (i + 1);
			}
			String description = resolveKnowledgeParamDescription(objectName, mbeanInfo, operation, i);
			String example = resolveParamExample(objectName, mbeanInfo, operation, i);
			if(!hasText(description) && !hasText(example)) {
				continue;
			}
			buffer.append("\n     - ").append(name).append(" (").append(JMXHelper.toTypeString(signature[i].getType())).append(')');
			if(hasText(description)) {
				buffer.append("\n       . Description : ").append(description);
			}
			if(hasText(example)) {
				buffer.append("\n       . Example     : ").append(example);
			}
		}
		if(buffer.length() == 0) {
			return null;
		}
		return buffer.toString();
	}

	public String formatFeatureDescriptionBlock(ObjectName objectName, MBeanInfo mbeanInfo, MBeanFeatureInfo feature) {
		if(feature instanceof MBeanAttributeInfo) {
			MBeanAttributeInfo attribute = (MBeanAttributeInfo)feature;
			String description = resolveAttributeDescription(objectName, mbeanInfo, attribute);
			String example = resolveAttributeExample(objectName, mbeanInfo, attribute);
			if(!hasText(description) && !hasText(example)) {
				return null;
			}
			StringBuilder buffer = new StringBuilder();
			if(hasText(description)) {
				buffer.append("\n   . Description : ").append(description);
			}
			if(hasText(example)) {
				buffer.append("\n   . Example     : ").append(example);
			}
			return buffer.toString();
		}
		if(feature instanceof MBeanOperationInfo) {
			MBeanOperationInfo operation = (MBeanOperationInfo)feature;
			StringBuilder buffer = new StringBuilder();
			String description = resolveOperationDescription(objectName, mbeanInfo, operation);
			if(hasText(description)) {
				buffer.append("\n   . Description : ").append(description);
			}
			String returnDescription = resolveOperationReturnDescription(objectName, mbeanInfo, operation);
			if(hasText(returnDescription)) {
				buffer.append("\n   . Return note : ").append(returnDescription);
			}
			String paramDetails = formatParameterDetailsBlock(objectName, mbeanInfo, operation);
			if(paramDetails != null) {
				buffer.append("\n   . Parameter details :").append(paramDetails);
			}
			return buffer.length() == 0 ? null : buffer.toString();
		}
		return null;
	}

	public boolean hasKnowledgeContributionForMBean(ObjectName objectName, MBeanInfo mbeanInfo) {
		MBeanKnowledge knowledge = findKnowledge(objectName, mbeanInfo);
		return knowledge != null && hasText(knowledge.getDescription());
	}

	public boolean hasKnowledgeContributionForFeature(ObjectName objectName, MBeanInfo mbeanInfo, MBeanFeatureInfo feature) {
		if(feature == null) {
			return false;
		}
		if(feature instanceof MBeanAttributeInfo) {
			FeatureKnowledge attributeKnowledge = findAttributeKnowledge(objectName, mbeanInfo, feature.getName());
			if(attributeKnowledge == null) {
				return false;
			}
			return hasText(attributeKnowledge.getDescription()) || hasText(attributeKnowledge.getExample());
		}
		if(feature instanceof MBeanOperationInfo) {
			MBeanOperationInfo operation = (MBeanOperationInfo)feature;
			FeatureKnowledge operationKnowledge = findOperationKnowledge(objectName, mbeanInfo, operation);
			if(operationKnowledge == null) {
				return false;
			}
			if(hasText(operationKnowledge.getDescription()) || hasText(operationKnowledge.getReturnDescription())) {
				return true;
			}
			if(formatParameterDetailsBlock(objectName, mbeanInfo, operation) != null) {
				return true;
			}
			MBeanParameterInfo[] signature = operation.getSignature();
			if(signature == null) {
				return false;
			}
			for(int i = 0; i < signature.length; i++) {
				ParamKnowledge paramKnowledge = findParamKnowledge(objectName, mbeanInfo, operation, i);
				if(paramKnowledge == null || !hasText(paramKnowledge.getName())) {
					continue;
				}
				if(!isGenericParamName(paramKnowledge.getName()) && isGenericParamName(signature[i].getName())) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isGenericParamName(String name) {
		if(name == null) {
			return true;
		}
		String trimmed = name.trim();
		if(trimmed.length() == 0) {
			return true;
		}
		if(trimmed.matches("p\\d+")) {
			return true;
		}
		if(trimmed.matches("arg\\d+")) {
			return true;
		}
		if(trimmed.matches("param\\d+")) {
			return true;
		}
		return false;
	}

	private MBeanKnowledge findKnowledge(ObjectName objectName, MBeanInfo mbeanInfo) {
		if(objectName == null) {
			return null;
		}
		return KnowledgeBase.getInstance().findBestMatch(objectName, mbeanInfo);
	}

	private FeatureKnowledge findAttributeKnowledge(ObjectName objectName, MBeanInfo mbeanInfo, String attributeName) {
		MBeanKnowledge knowledge = findKnowledge(objectName, mbeanInfo);
		if(knowledge == null || attributeName == null) {
			return null;
		}
		return knowledge.getAttribute(attributeName);
	}

	private FeatureKnowledge findOperationKnowledge(ObjectName objectName, MBeanInfo mbeanInfo, MBeanOperationInfo operation) {
		MBeanKnowledge knowledge = findKnowledge(objectName, mbeanInfo);
		if(knowledge == null || operation == null) {
			return null;
		}
		String signatureKey = JMXHelper.toOperationCallTargetName(operation);
		return knowledge.getOperation(signatureKey);
	}

	private ParamKnowledge findParamKnowledge(ObjectName objectName, MBeanInfo mbeanInfo, MBeanOperationInfo operation, int index) {
		FeatureKnowledge feature = findOperationKnowledge(objectName, mbeanInfo, operation);
		if(feature == null) {
			return null;
		}
		return feature.getParam(index);
	}

	private static boolean hasText(String value) {
		return value != null && value.trim().length() > 0;
	}

}
