package dal.tool.trace.jmxer.tui.support;

import java.util.Date;

import dal.tool.trace.jmxer.cli.data.RecordResult;
import dal.tool.util.DateUtil;
import dal.tool.util.NumberUtil;

/**
 * TUI 대화상자에서 Record VIEW 범위·시점·이름 표현식의 예시 값과 검증을 제공한다.
 *
 */
public final class RecordViewExpressionHelper {

	/**
	 * Record VIEW 표현식 입력 유형(절대 시각, 상대 시각, 샘플 순서).
	 *
	 */
	public enum ExpressionType {
		ABSOLUTE_TIME(0),
		RELATIVE_TIME(1),
		SAMPLE_ORDER(2);

		public static ExpressionType fromListIndex(int index) {
			for(ExpressionType type : values()) {
				if(type.listIndex == index) {
					return type;
				}
			}
			return ABSOLUTE_TIME;
		}

		private final int listIndex;

		ExpressionType(int listIndex) {
			this.listIndex = listIndex;
		}
	}

	private final RecordResult result;

	public RecordViewExpressionHelper(RecordResult result) {
		this.result = result;
	}

	public String formatRangeExample(ExpressionType type) {
		if(type == ExpressionType.ABSOLUTE_TIME) {
			return getStartTimeString() + "~" + getEndTimeString();
		}
		if(type == ExpressionType.RELATIVE_TIME) {
			return "0ms~" + getRelativeMaxMs() + "ms";
		}
		return "1~" + getSampleCount();
	}

	public String formatPointExample(ExpressionType type) {
		if(type == ExpressionType.ABSOLUTE_TIME) {
			return getStartTimeString();
		}
		if(type == ExpressionType.RELATIVE_TIME) {
			return "0ms";
		}
		return "1";
	}

	public String validateRangeExpression(String raw) {
		if(raw == null || raw.trim().length() == 0) {
			return null;
		}
		String expStr = raw.trim();
		String[] range = expStr.split("~");
		if(range.length != 2) {
			return TuiRecordUiKeys.VIEW_RANGE_INVALID_FORMAT_MESSAGE;
		}
		String fromStr = range[0].trim();
		String toStr = range[1].trim();
		if(fromStr.endsWith("ms") && NumberUtil.isNumber(fromStr.substring(0, fromStr.length() - 2))
				&& toStr.endsWith("ms") && NumberUtil.isNumber(toStr.substring(0, toStr.length() - 2))) {
			long fromMs = Long.parseLong(fromStr.substring(0, fromStr.length() - 2));
			long toMs = Long.parseLong(toStr.substring(0, toStr.length() - 2));
			long maxMs = getRelativeMaxMs();
			if(fromMs < 0L || fromMs > maxMs || toMs < 0L || toMs > maxMs) {
				return String.format(TuiRecordUiKeys.VIEW_RANGE_RELATIVE_OUT_OF_RANGE_MESSAGE, Long.valueOf(maxMs));
			}
			return null;
		}
		if(NumberUtil.isNumber(fromStr) && NumberUtil.isNumber(toStr)) {
			int fromIdx = Integer.parseInt(fromStr);
			int toIdx = Integer.parseInt(toStr);
			int sampleCount = getSampleCount();
			if(fromIdx < 1 || fromIdx > sampleCount || toIdx < 1 || toIdx > sampleCount) {
				return String.format(TuiRecordUiKeys.VIEW_RANGE_SAMPLE_OUT_OF_RANGE_MESSAGE, Integer.valueOf(sampleCount));
			}
			return null;
		}
		Date fromDt = DateUtil.stringToDate(DateUtil.FORMAT_DATETIME_SEC, fromStr);
		Date toDt = DateUtil.stringToDate(DateUtil.FORMAT_DATETIME_SEC, toStr);
		if(fromDt != null && toDt != null) {
			String startTimeStr = getStartTimeString();
			String endTimeStr = getEndTimeString();
			long startTime = DateUtil.stringToDate(DateUtil.FORMAT_DATETIME_SEC, startTimeStr).getTime();
			long endTime = DateUtil.stringToDate(DateUtil.FORMAT_DATETIME_SEC, endTimeStr).getTime() + 999L;
			long from = fromDt.getTime();
			long to = toDt.getTime() + 999L;
			if(from < startTime || from > endTime || to < startTime || to > endTime) {
				return String.format(TuiRecordUiKeys.VIEW_RANGE_ABSOLUTE_OUT_OF_RANGE_MESSAGE, startTimeStr, endTimeStr);
			}
			return null;
		}
		return TuiRecordUiKeys.VIEW_RANGE_INVALID_FORMAT_MESSAGE;
	}

	public String validatePointExpression(String raw) {
		if(raw == null || raw.trim().length() == 0) {
			return TuiRecordUiKeys.VIEW_POINT_REQUIRED_MESSAGE;
		}
		String expStr = raw.trim();
		if(expStr.endsWith("ms") && expStr.length() > 2 && NumberUtil.isNumber(expStr.substring(0, expStr.length() - 2))) {
			long ms = Long.parseLong(expStr.substring(0, expStr.length() - 2));
			long maxMs = getRelativeMaxMs();
			if(ms < 0L || ms > maxMs) {
				return String.format(TuiRecordUiKeys.VIEW_POINT_RELATIVE_OUT_OF_RANGE_MESSAGE, Long.valueOf(maxMs));
			}
			return null;
		}
		if(NumberUtil.isNumber(expStr)) {
			int order = Integer.parseInt(expStr);
			int sampleCount = getSampleCount();
			if(order < 1 || order > sampleCount) {
				return String.format(TuiRecordUiKeys.VIEW_POINT_SAMPLE_OUT_OF_RANGE_MESSAGE, Integer.valueOf(sampleCount));
			}
			return null;
		}
		Date dt = DateUtil.stringToDate(DateUtil.FORMAT_DATETIME_SEC, expStr);
		if(dt == null) {
			return TuiRecordUiKeys.VIEW_POINT_INVALID_FORMAT_MESSAGE;
		}
		String startTimeStr = getStartTimeString();
		String endTimeStr = getEndTimeString();
		long startTime = DateUtil.stringToDate(DateUtil.FORMAT_DATETIME_SEC, startTimeStr).getTime();
		long endTime = DateUtil.stringToDate(DateUtil.FORMAT_DATETIME_SEC, endTimeStr).getTime() + 999L;
		long rangeTimeFrom = dt.getTime();
		long rangeTimeTo = rangeTimeFrom + 999L;
		if(rangeTimeFrom < startTime || rangeTimeFrom > endTime || rangeTimeTo < startTime || rangeTimeTo > endTime) {
			return String.format(TuiRecordUiKeys.VIEW_POINT_ABSOLUTE_OUT_OF_RANGE_MESSAGE, startTimeStr, endTimeStr);
		}
		return null;
	}

	public String validateNameExpression(String raw) {
		if(raw == null || raw.trim().length() < TuiRecordUiKeys.VIEW_SEARCH_MIN_PATTERN_LENGTH) {
			return TuiRecordUiKeys.VIEW_SEARCH_PATTERN_REQUIRED_MESSAGE;
		}
		return null;
	}

	private int getSampleCount() {
		if(result == null || result.sampleCount < 1) {
			return 1;
		}
		return result.sampleCount;
	}

	private long getRelativeMaxMs() {
		if(result == null) {
			return 0L;
		}
		long span = result.endTime - result.startTime;
		if(span < 0L) {
			return 0L;
		}
		return span;
	}

	private String getStartTimeString() {
		if(result == null) {
			return "1970.01.01/00:00:00";
		}
		return DateUtil.dateToString(DateUtil.FORMAT_DATETIME_SEC, new Date(result.startTime));
	}

	private String getEndTimeString() {
		if(result == null) {
			return "1970.01.01/00:00:00";
		}
		return DateUtil.dateToString(DateUtil.FORMAT_DATETIME_SEC, new Date(result.endTime));
	}
}
