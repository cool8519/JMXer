package dal.tool.trace.jmxer.tui.dialog;

/**
 * TUI 팝업 본문 텍스트 서식을 통일하는 유틸.
 *
 */
final class TuiDialogText {

	private TuiDialogText() {}

	/**
	 * 본문 앞에 빈 줄을 하나 붙여 제목 행 아래에 첫 메시지가 오도록 한다.<br>
	 * CLI 출력이 선행 줄바꿈(\r\n 포함)으로 시작하는 경우가 많아, 중복 빈 줄 없이 하나만 남기도록 선행 줄바꿈을 제거한다.
	 * @param text 원본 본문 텍스트
	 * @return 서식된 본문 텍스트
	 */
	static String body(String text) {
		if(text == null || text.isEmpty()) {
			return "\n";
		}
		String trimmed = stripLeadingLineBreaks(text);
		if(trimmed.isEmpty()) {
			return "\n";
		}
		return "\n" + trimmed;
	}

	private static String stripLeadingLineBreaks(String text) {
		int start = 0;
		while(start < text.length()) {
			char c = text.charAt(start);
			if(c == '\n') {
				start++;
				continue;
			}
			if(c == '\r') {
				if(start + 1 < text.length() && text.charAt(start + 1) == '\n') {
					start += 2;
				} else {
					start++;
				}
				continue;
			}
			break;
		}
		return text.substring(start);
	}
}
