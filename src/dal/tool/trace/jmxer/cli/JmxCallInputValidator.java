package dal.tool.trace.jmxer.cli;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.OpenType;

import dal.tool.util.StringUtil;
import dal.tool.util.TypeUtil;
import dal.tool.util.jmx.JMXHelper;

/**
 * CALL 속성 값·오퍼레이션 인자를 JMX 호출 전에 검증한다(CLI/TUI 공통).
 */
public final class JmxCallInputValidator {

	private JmxCallInputValidator() {}

	/**
	 * @return 오류 메시지, 유효하거나 검증 대상이 아니면 null
	 */
	public static String validate(MBeanServerConnection conn, ObjectName oName, String targetName, String[] targetValues) {
		if(conn == null || oName == null || targetName == null) {
			return null;
		}
		try {
			MBeanInfo mbeanInfo = conn.getMBeanInfo(oName);
			if(targetName.indexOf('(') <= 0) {
				MBeanAttributeInfo attribute = findAttribute(mbeanInfo, targetName);
				if(attribute != null) {
					if(targetValues == null || targetValues.length == 0) {
						return null;
					}
					return validateAttributeValue(attribute, targetValues[0]);
				}
			}
			MBeanOperationInfo operation = findOperation(mbeanInfo, targetName, targetValues);
			if(operation != null) {
				return validateOperationArguments(operation.getSignature(), targetValues);
			}
			return null;
		} catch(Exception e) {
			return e.getMessage();
		}
	}

	public static String validateAttributeValue(MBeanAttributeInfo attribute, String value) {
		if(attribute == null) {
			return "Attribute is not available.";
		}
		String name = attribute.getName();
		String fieldLabel = (name == null || name.trim().length() == 0) ? "Attribute value" : "Attribute \"" + name + "\"";
		String requiredError = validateRequired(fieldLabel, value);
		if(requiredError != null) {
			return requiredError;
		}
		String trimmed = value.trim();
		String typeDisplay = JMXHelper.toTypeString(attribute.getType());
		try {
			JMXHelper.createInstance(attribute.getType(), trimmed);
			return null;
		} catch(Exception e) {
			return formatConversionError(fieldLabel, typeDisplay, trimmed, e);
		}
	}

	public static String validateOperationArguments(MBeanParameterInfo[] signature, String[] values) {
		if(signature == null || signature.length == 0) {
			if(values != null && values.length > 0) {
				return "Operation takes no arguments, but " + values.length + " value(s) were given.";
			}
			return null;
		}
		if(values == null || values.length != signature.length) {
			return "Argument count does not match the operation signature (expected "
				+ signature.length + ", got " + (values == null ? 0 : values.length) + ").";
		}
		String[] normalized = normalizeTrimmed(values);
		for(int i = 0; i < signature.length; i++) {
			String fieldLabel = formatParameterFieldLabel(signature[i], i);
			String requiredError = validateRequired(fieldLabel, normalized[i]);
			if(requiredError != null) {
				return requiredError;
			}
			String typeDisplay = JMXHelper.toTypeString(signature[i].getType());
			try {
				validateSingleParameter(signature[i], normalized[i]);
			} catch(Exception e) {
				return formatConversionError(fieldLabel, typeDisplay, normalized[i], e);
			}
		}
		return null;
	}

	public static String formatCallConfirmationDetails(MBeanServerConnection conn, ObjectName oName, String targetName,
			String[] targetValues) throws Exception {
		if(conn == null || oName == null || targetName == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		MBeanInfo mbeanInfo = conn.getMBeanInfo(oName);
		if(targetName.indexOf('(') <= 0) {
			MBeanAttributeInfo attribute = findAttribute(mbeanInfo, targetName);
			if(attribute != null && targetValues != null && targetValues.length > 0) {
				sb.append("\nValue (").append(JMXHelper.toTypeString(attribute.getType())).append("): ");
				sb.append(formatScalarDisplayValue(attribute.getType(), targetValues[0]));
				return sb.toString();
			}
		}
		MBeanOperationInfo operation = findOperation(mbeanInfo, targetName, targetValues);
		if(operation != null) {
			sb.append("\nTarget: ").append(JMXHelper.toOperationCallTargetName(operation));
			if(targetValues != null && targetValues.length > 0) {
				sb.append("\nArguments:");
				MBeanParameterInfo[] signature = operation.getSignature();
				for(int i = 0; i < signature.length; i++) {
					String value = i < targetValues.length ? targetValues[i] : "";
					sb.append("\n  ");
					sb.append(formatParameterFieldLabel(signature[i], i));
					sb.append(" (").append(JMXHelper.toTypeString(signature[i].getType())).append("): ");
					sb.append(formatParameterDisplayValue(signature[i], value));
				}
			}
			return sb.toString();
		}
		sb.append("\nTarget: ").append(targetName);
		if(targetValues != null && targetValues.length > 0) {
			sb.append("\nValue/arguments: ");
			for(int i = 0; i < targetValues.length; i++) {
				if(i > 0) {
					sb.append(", ");
				}
				sb.append(targetValues[i]);
			}
		}
		return sb.toString();
	}

	private static String formatParameterDisplayValue(MBeanParameterInfo param, String value) {
		if(value == null) {
			return "";
		}
		if(JMXHelper.isArrayParameter(param)) {
			return "[" + JMXHelper.normalizeArrayArgumentInput(value) + "]";
		}
		return formatScalarDisplayValue(param.getType(), value);
	}

	private static String formatScalarDisplayValue(String typeName, String value) {
		if(value == null) {
			return "";
		}
		String trimmed = value.trim();
		if("java.lang.String".equals(typeName) && trimmed.indexOf(' ') >= 0) {
			return "\"" + trimmed + "\"";
		}
		return trimmed;
	}

	private static MBeanAttributeInfo findAttribute(MBeanInfo mbeanInfo, String targetName) {
		MBeanAttributeInfo[] attrs = mbeanInfo.getAttributes();
		if(attrs == null) {
			return null;
		}
		for(int i = 0; i < attrs.length; i++) {
			if(attrs[i].getName().equals(targetName)) {
				return attrs[i];
			}
		}
		return null;
	}

	private static MBeanOperationInfo findOperation(MBeanInfo mbeanInfo, String targetName, String[] targetValues) throws Exception {
		MBeanOperationInfo[] operations = mbeanInfo.getOperations();
		if(operations == null) {
			return null;
		}
		boolean isOperation = (targetName.indexOf('(') > 0);
		for(int i = 0; i < operations.length; i++) {
			MBeanOperationInfo op = operations[i];
			if(isOperation) {
				String opString = targetName.substring(0, targetName.indexOf('('));
				String argsString = targetName.substring(targetName.indexOf('(') + 1, targetName.indexOf(')'));
				if(!op.getName().equals(opString)) {
					continue;
				}
				String[] argsArr = StringUtil.getTokenArray(argsString, ',', new String[]{"()"});
				MBeanParameterInfo[] paramInfo = op.getSignature();
				if(argsArr.length != paramInfo.length) {
					continue;
				}
				boolean isMatch = true;
				for(int j = 0; j < argsArr.length; j++) {
					String type = TypeUtil.toType(paramInfo[j].getType());
					if(!type.equals(argsArr[j])) {
						isMatch = false;
						break;
					}
				}
				if(isMatch) {
					return op;
				}
			} else if(op.getName().equals(targetName)) {
				int sigLen = op.getSignature().length;
				int valLen = targetValues == null ? 0 : targetValues.length;
				if(valLen == sigLen) {
					return op;
				}
			}
		}
		return null;
	}

	private static void validateSingleParameter(MBeanParameterInfo paramInfo, String value) throws Exception {
		OpenType<?> openType = (OpenType<?>)paramInfo.getDescriptor().getFieldValue("openType");
		if(openType != null && openType.isArray()) {
			JMXHelper.getArrayFromString(openType, JMXHelper.normalizeArrayArgumentInput(value));
		} else if(JMXHelper.isArrayParameter(paramInfo)) {
			throw new Exception("Array parameter requires openType metadata.");
		} else {
			String clsName = (openType == null) ? paramInfo.getType() : openType.getClassName();
			JMXHelper.createInstance(clsName, value);
		}
	}

	private static String validateRequired(String fieldLabel, String value) {
		if(value == null || value.trim().length() == 0) {
			return fieldLabel + " must not be empty.";
		}
		return null;
	}

	private static String formatParameterFieldLabel(MBeanParameterInfo param, int index) {
		if(param == null) {
			return "Argument " + (index + 1);
		}
		String name = param.getName();
		if(name == null || name.trim().length() == 0) {
			return "Argument " + (index + 1);
		}
		return "Argument \"" + name + "\"";
	}

	private static String formatConversionError(String fieldLabel, String typeDisplay, String value, Exception e) {
		String detail = extractMessage(e);
		return fieldLabel + " (" + typeDisplay + "): invalid value \"" + abbreviate(value) + "\". " + detail;
	}

	private static String extractMessage(Exception e) {
		if(e == null) {
			return "Value cannot be converted to the required type.";
		}
		String message = e.getMessage();
		if(message != null && message.trim().length() > 0) {
			return message.trim();
		}
		Throwable cause = e.getCause();
		if(cause != null) {
			String causeMessage = cause.getMessage();
			if(causeMessage != null && causeMessage.trim().length() > 0) {
				return causeMessage.trim();
			}
		}
		return "Value cannot be converted to the required type.";
	}

	private static String abbreviate(String value) {
		if(value == null) {
			return "";
		}
		final int maxLen = 48;
		if(value.length() <= maxLen) {
			return value;
		}
		return value.substring(0, maxLen - 3) + "...";
	}

	private static String[] normalizeTrimmed(String[] values) {
		String[] normalized = new String[values.length];
		for(int i = 0; i < values.length; i++) {
			normalized[i] = values[i] == null ? "" : values[i].trim();
		}
		return normalized;
	}
}
