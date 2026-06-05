package dal.tool.trace.jmxer.tui.support;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanParameterInfo;

import dal.tool.trace.jmxer.cli.JmxCallInputValidator;

/**
 * TUI entry point for CALL input validation (delegates to {@link JmxCallInputValidator}).
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
