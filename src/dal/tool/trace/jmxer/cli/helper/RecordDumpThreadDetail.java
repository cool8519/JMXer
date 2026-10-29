package dal.tool.trace.jmxer.cli.helper;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import dal.tool.trace.jmxer.cli.JmxSettings.DumpStackFilter;
import dal.tool.trace.jmxer.cli.data.RecordResult;
import dal.tool.trace.jmxer.cli.data.RecordThreadInfo;
import dal.tool.trace.jmxer.cli.data.RecordThreadSampleState;
import dal.tool.trace.jmxer.cli.data.ResourceUsage;

/**
 * Record UI Dump 뷰(TUI Info 상세)용 스레드 요약 포맷
 *
 */
public final class RecordDumpThreadDetail {

	private static final String TOP_BULLET = " - ";
	private static final String SUB_BULLET = "   . ";
	private static final String NESTED_BULLET = "     . ";
	private static final int STATE_LABEL_WIDTH = 18;

	private RecordDumpThreadDetail() {}

	public static String formatDetail(
			RecordResult result,
			long threadId,
			DumpStackFilter dumpStackFilter,
			String reqEntryFile) {
		if(result == null || result.recordData == null) {
			return "No stacktrace samples for this thread in dump.";
		}
		List<RecordThreadInfo> samples = result.recordData.get(Long.valueOf(threadId));
		if(samples == null || samples.isEmpty()) {
			return "No stacktrace samples for this thread in dump.";
		}

		DumpStackFilter filter = dumpStackFilter == null ? DumpStackFilter.NO_REQUEST_WAIT : dumpStackFilter;
		Set<String> userSignatures = resolveUserSignatures(filter, reqEntryFile);

		RecordThreadInfo first = samples.get(0);
		String threadName = first.getThreadName() == null ? "" : first.getThreadName();

		int totalSamples = samples.size();
		int includedSamples = 0;
		int activeSamples = 0;
		int waitingRequestSamples = 0;
		Map<String, Integer> stateCounts = new TreeMap<String, Integer>();

		for(int i = 0; i < samples.size(); i++) {
			RecordThreadInfo sample = samples.get(i);
			if(sample == null) {
				continue;
			}
			if(includeSample(sample, filter, userSignatures)) {
				includedSamples++;
			}
			RecordThreadSampleState sampleState = RecordRequestWaitClassifier.classify(
				sample.getStackTrace(),
				userSignatures);
			if(sampleState == RecordThreadSampleState.ACTIVE) {
				activeSamples++;
			} else if(sampleState == RecordThreadSampleState.WAITING_REQUEST) {
				waitingRequestSamples++;
			}
			String stateName = sample.getThreadState();
			if(stateName == null || stateName.length() == 0) {
				stateName = "?";
			}
			Integer count = stateCounts.get(stateName);
			stateCounts.put(stateName, count == null ? Integer.valueOf(1) : Integer.valueOf(count.intValue() + 1));
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Thread id ").append(threadId).append("\n");
		sb.append(TOP_BULLET).append("Name : ").append(threadName).append("\n");
		sb.append(TOP_BULLET).append("Record session\n");
		sb.append(SUB_BULLET).append("Samples        : ").append(totalSamples).append(" total, ")
			.append(includedSamples).append(" included\n");
		sb.append(TOP_BULLET).append("Resource usage over recording\n");
		appendResourceUsage(sb, result, threadId);
		sb.append(TOP_BULLET).append("Sample states\n");
		sb.append(SUB_BULLET).append("Thread state\n");
		for(Map.Entry<String, Integer> entry : stateCounts.entrySet()) {
			sb.append(NESTED_BULLET).append(padRight(entry.getKey(), STATE_LABEL_WIDTH)).append(": ")
				.append(entry.getValue().intValue()).append("\n");
		}
		sb.append(SUB_BULLET).append("Request state\n");
		sb.append(NESTED_BULLET).append(padRight("ACTIVE", STATE_LABEL_WIDTH)).append(": ")
			.append(activeSamples).append("\n");
		sb.append(NESTED_BULLET).append(padRight("WAITING_REQUEST", STATE_LABEL_WIDTH)).append(": ")
			.append(waitingRequestSamples).append("\n");

		return sb.toString();
	}

	private static void appendResourceUsage(StringBuilder sb, RecordResult result, long threadId) {
		ResourceUsage usage = result.resourceData == null
			? null
			: result.resourceData.get(Long.valueOf(threadId));
		if(usage == null) {
			sb.append(SUB_BULLET).append("CpuTime(ns)    : Unknown\n");
			sb.append(SUB_BULLET).append("AllocatedBytes : Unknown\n");
			return;
		}
		long totalCpu = (usage.startCpu >= 0L && usage.currCpu >= usage.startCpu) ? (usage.currCpu - usage.startCpu) : -1L;
		long totalMem = (usage.startMem >= 0L && usage.currMem >= usage.startMem) ? (usage.currMem - usage.startMem) : -1L;
		sb.append(SUB_BULLET).append("CpuTime(ns)    : ").append(formatResourceValue(totalCpu)).append("\n");
		sb.append(SUB_BULLET).append("AllocatedBytes : ").append(formatResourceValue(totalMem)).append("\n");
	}

	private static String formatResourceValue(long value) {
		return value > -1L ? String.valueOf(value) : "Unknown";
	}

	private static boolean includeSample(
			RecordThreadInfo sample,
			DumpStackFilter filter,
			Set<String> userSignatures) {
		if(filter == DumpStackFilter.FULL) {
			return true;
		}
		return RecordRequestWaitClassifier.classify(sample.getStackTrace(), userSignatures)
			!= RecordThreadSampleState.WAITING_REQUEST;
	}

	private static Set<String> resolveUserSignatures(DumpStackFilter filter, String reqEntryFile) {
		if(filter == DumpStackFilter.FULL) {
			return null;
		}
		RequestEntryUserSignatures loaded = RequestEntryUserSignatures.load(reqEntryFile);
		return loaded.getSignatures();
	}

	private static String padRight(String value, int width) {
		if(value == null) {
			value = "";
		}
		if(value.length() >= width) {
			return value;
		}
		StringBuilder sb = new StringBuilder(width);
		sb.append(value);
		while(sb.length() < width) {
			sb.append(' ');
		}
		return sb.toString();
	}
}
