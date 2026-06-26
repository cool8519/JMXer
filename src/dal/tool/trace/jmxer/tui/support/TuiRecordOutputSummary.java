package dal.tool.trace.jmxer.tui.support;

import dal.tool.trace.jmxer.tui.dialog.TuiRecordSamplingDialog.SamplingParams;

/**
 * Record UI 키맵 샘플링 대화상자에서 고른 값의 Output 패널 요약.
 *
 */
public final class TuiRecordOutputSummary {

	private TuiRecordOutputSummary() {}

	public static String formatStacktraceSampling(SamplingParams params) {
		if(params == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("Sampling interval: ").append(params.intervalMs).append(" ms");
		sb.append('\n');
		sb.append(formatSamplingTimeLine(params.timeMs));
		return sb.toString();
	}

	public static String formatResourceSampling(long timeMs) {
		return formatSamplingTimeLine(timeMs);
	}

	private static String formatSamplingTimeLine(long timeMs) {
		if(timeMs == 0L) {
			return "Sampling time: 0 ms (until stopped)";
		}
		return "Sampling time: " + timeMs + " ms";
	}
}
