package dal.tool.cli;

import dal.tool.cli.command.Command;
import dal.tool.cli.command.EditCommand;
import dal.tool.cli.command.PreviousCommand;
import dal.tool.cli.command.ScriptCommand;

/**
 * ScriptCommand 내부에서 실행할 수 없는 명령 정책(; 다중 명령 포함)
 */
public final class ScriptCommandPolicy {

	private ScriptCommandPolicy() {}

	public static boolean isBlocked(Command command) {
		if(command == null) {
			return false;
		}
		return command instanceof EditCommand || command instanceof PreviousCommand;
	}

	public static String getBlockedMessage(Command command) {
		if(command instanceof EditCommand) {
			return EditCommand.SCRIPT_NOT_AVAILABLE_MESSAGE;
		}
		if(command instanceof PreviousCommand) {
			return PreviousCommand.SCRIPT_NOT_AVAILABLE_MESSAGE;
		}
		return "This command is not available inside SCRIPT.";
	}

	public static boolean shouldReject(Command command) {
		return ScriptCommand.isNestedScriptExecution() && isBlocked(command);
	}
}
