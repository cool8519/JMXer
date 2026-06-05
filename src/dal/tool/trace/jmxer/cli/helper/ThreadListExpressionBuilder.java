package dal.tool.trace.jmxer.cli.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dal.tool.trace.jmxer.cli.command.JmxThreadCommand;
import dal.tool.util.StringUtil;

/**
 * Builds a compact ThreadList expression from a set of selected thread IDs.
 */
public final class ThreadListExpressionBuilder {
	private static final String ALL_THREADS = "*";

	private ThreadListExpressionBuilder() {}

	public static String build(Map<Long, String> threadMap, Set<Long> selectedIds) {
		if(selectedIds == null || selectedIds.isEmpty()) {
			return "";
		}
		if(threadMap == null || threadMap.isEmpty()) {
			return compressIds(selectedIds);
		}
		if(selectedIds.size() == threadMap.size()) {
			boolean allSelected = true;
			for(Long id : threadMap.keySet()) {
				if(!selectedIds.contains(id)) {
					allSelected = false;
					break;
				}
			}
			if(allSelected) {
				return ALL_THREADS;
			}
		}

		Set<Long> remaining = new HashSet<Long>(selectedIds);
		List<String> parts = new ArrayList<String>();

		Map<String, List<Long>> patternGroups = groupByNamePattern(threadMap, remaining);
		for(Map.Entry<String, List<Long>> entry : patternGroups.entrySet()) {
			String pattern = entry.getKey();
			List<Long> groupIds = entry.getValue();
			if(groupIds.size() < 2) {
				continue;
			}
			Set<Long> matchedInSnapshot = findMatchingIds(threadMap, pattern);
			Set<Long> groupSet = new HashSet<Long>(groupIds);
			if(matchedInSnapshot.equals(groupSet)) {
				parts.add("\"" + pattern + "\"");
				remaining.removeAll(groupSet);
			}
		}

		if(!remaining.isEmpty()) {
			parts.add(compressIds(remaining));
		}

		String expr = joinParts(parts);
		if(verifyExpression(threadMap, expr, selectedIds)) {
			return expr;
		}
		return compressIds(selectedIds);
	}

	private static Map<String, List<Long>> groupByNamePattern(Map<Long, String> threadMap, Set<Long> selectedIds) {
		Map<String, List<Long>> groups = new LinkedHashMap<String, List<Long>>();
		for(Long id : selectedIds) {
			String name = threadMap.get(id);
			if(name == null) {
				continue;
			}
			String pattern = generalizeNamePattern(name);
			if(pattern == null) {
				continue;
			}
			List<Long> list = groups.get(pattern);
			if(list == null) {
				list = new ArrayList<Long>();
				groups.put(pattern, list);
			}
			list.add(id);
		}
		return groups;
	}

	/**
	 * Replace trailing digits with '*' when the name ends with a numeric suffix.
	 */
	private static String generalizeNamePattern(String name) {
		if(name == null || name.length() == 0) {
			return null;
		}
		int end = name.length();
		while(end > 0 && Character.isDigit(name.charAt(end - 1))) {
			end--;
		}
		if(end == name.length() || end == 0) {
			return null;
		}
		return name.substring(0, end) + "*";
	}

	private static Set<Long> findMatchingIds(Map<Long, String> threadMap, String pattern) {
		Set<Long> matched = new HashSet<Long>();
		Iterator<Map.Entry<Long, String>> iter = threadMap.entrySet().iterator();
		while(iter.hasNext()) {
			Map.Entry<Long, String> entry = iter.next();
			String name = entry.getValue();
			if(name != null && StringUtil.isMatchStringWithPattern(name, pattern)) {
				matched.add(entry.getKey());
			}
		}
		return matched;
	}

	private static String compressIds(Set<Long> ids) {
		if(ids == null || ids.isEmpty()) {
			return "";
		}
		List<Long> sorted = new ArrayList<Long>(ids);
		Collections.sort(sorted);
		StringBuilder sb = new StringBuilder();
		int i = 0;
		while(i < sorted.size()) {
			long start = sorted.get(i).longValue();
			long end = start;
			int j = i + 1;
			while(j < sorted.size() && sorted.get(j).longValue() == end + 1) {
				end = sorted.get(j).longValue();
				j++;
			}
			if(sb.length() > 0) {
				sb.append(',');
			}
			if(start == end) {
				sb.append(start);
			} else {
				sb.append(start).append('-').append(end);
			}
			i = j;
		}
		return sb.toString();
	}

	private static String joinParts(List<String> parts) {
		if(parts == null || parts.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < parts.size(); i++) {
			if(i > 0) {
				sb.append(',');
			}
			sb.append(parts.get(i));
		}
		return sb.toString();
	}

	private static boolean verifyExpression(Map<Long, String> threadMap, String expr, Set<Long> selectedIds) {
		if(expr == null || expr.length() == 0) {
			return selectedIds.isEmpty();
		}
		try {
			Long[] parsed = JmxThreadCommand.getTargetThreadIds(threadMap, expr);
			if(parsed == null) {
				return false;
			}
			Set<Long> parsedSet = new HashSet<Long>();
			for(int i = 0; i < parsed.length; i++) {
				parsedSet.add(parsed[i]);
			}
			return parsedSet.equals(selectedIds);
		} catch(Exception e) {
			return false;
		}
	}

	public static Map<Long, String> toThreadMap(List<ThreadSnapshotEntry> entries) {
		Map<Long, String> map = new HashMap<Long, String>();
		if(entries == null) {
			return map;
		}
		for(int i = 0; i < entries.size(); i++) {
			ThreadSnapshotEntry entry = entries.get(i);
			if(entry != null) {
				map.put(Long.valueOf(entry.getThreadId()), entry.getThreadName());
			}
		}
		return map;
	}

	/** Minimal thread snapshot entry for expression building. */
	public static final class ThreadSnapshotEntry {
		private final long threadId;
		private final String threadName;

		public ThreadSnapshotEntry(long threadId, String threadName) {
			this.threadId = threadId;
			this.threadName = threadName == null ? "" : threadName;
		}

		public long getThreadId() {
			return threadId;
		}

		public String getThreadName() {
			return threadName;
		}
	}
}
