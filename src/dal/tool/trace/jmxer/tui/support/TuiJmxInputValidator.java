package dal.tool.trace.jmxer.tui.support;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanParameterInfo;

import dal.tool.trace.jmxer.cli.JmxCallInputValidator;

/**
 * TUI에서 CALL 입력 검증 진입점(JmxCallInputValidator에 위임).
 *
 */
public final class TuiJmxInputValidator {

	private TuiJmxInputValidator() {}

	public static String validateAttributeValue(MBeanAttributeInfo attribute, String value) {
		return JmxCallInputValidator.validateAttributeValue(attribute, value);
	}

	public static String validateOperationArguments(MBeanParameterInfo[] signature, String[] values) {
		return JmxCallInputValidator.validateOperationArguments(signature, values);
	}
}
