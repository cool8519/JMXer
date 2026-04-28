package dal.tool.trace.jmxer.cli.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import dal.tool.cli.Logger;
import dal.tool.cli.Logger.Level;
import dal.tool.trace.jmxer.cli.JmxSettings.RecordViewMode;
import dal.tool.trace.jmxer.cli.command.JmxThreadCommand;
import dal.tool.trace.jmxer.cli.data.RecordResult;
import dal.tool.trace.jmxer.cli.data.RecordSearch;
import dal.tool.trace.jmxer.cli.data.RecordStackFrame;
import dal.tool.trace.jmxer.cli.data.RecordThreadInfo;
import dal.tool.trace.jmxer.cli.data.RecordThreadSampleState;
import dal.tool.trace.jmxer.cli.data.ResourceUsage;
import dal.tool.trace.jmxer.cli.data.Tree;
import dal.tool.trace.jmxer.cli.data.TreeNode;
import dal.tool.util.DateUtil;
import dal.tool.util.NumberUtil;
import dal.tool.util.StringUtil;

public class RecordResultViewer {

	/** THREAD 뷰에서 스레드당 1개 이상의 분리된 트리(구간)를 담는다. */
	private static final class ThreadViewSegment {
		final Tree<RecordStackFrame> tree;
		/** NO_REQUEST_WAIT일 때만 stacktrace time 계산에 사용; FULL이면 비어 있음 */
		final List<RecordThreadInfo> viewSamples;

		ThreadViewSegment(Tree<RecordStackFrame> tree, List<RecordThreadInfo> viewSamples) {
			this.tree = tree;
			this.viewSamples = viewSamples;
		}
	}

	/** THREAD 뷰 stacktrace time 구간 표기(절대시간, 24시간). */
	private static final String STACK_TRACE_ABS_TIME_FORMAT = "yyyy.MM.dd/HH:mm:ss.SSS";

	RecordResult result;
	RecordViewMode recordViewMode;
	boolean showEmptyThread;
	HashMap<Long,String> threadList = new HashMap<Long,String>();
	
	
	public RecordResultViewer(RecordResult result, RecordViewMode mode, boolean showEmptyThread) {
		this.result = result;
		this.recordViewMode = (mode == null) ? RecordViewMode.NO_REQUEST_WAIT : mode;
		this.showEmptyThread = showEmptyThread;
		extractThreadList();
	}

	private void extractThreadList() {
		for(Long id : new TreeSet<Long>(result.recordData.keySet())) {
			List<RecordThreadInfo> threadInfoList = result.recordData.get(id);
			if(threadInfoList != null && threadInfoList.size() > 0) {
				threadList.put(id, threadInfoList.get(0).getThreadName());
			}
		}
	}

	private boolean checkArgument(List<String> args, int min, int max) {
		if(args.size() < min) {
			Logger.logln("Need more argument.");
			return false;
		} else if(args.size() > max) {
    		Logger.logln("Too many arguments.");
    		return false;
    	}
		return true;
	}

	private boolean includeSample(RecordThreadInfo recThrInfo) {
		if(recordViewMode == RecordViewMode.FULL) {
			return true;
		}
		RecordThreadSampleState state = recThrInfo.getSampleState();
		if(state == null) {
			return true;
		}
		return state != RecordThreadSampleState.WAITING_REQUEST;
	}

	private boolean isWaitingRequestSample(RecordThreadInfo recThrInfo) {
		return recThrInfo.getSampleState() == RecordThreadSampleState.WAITING_REQUEST;
	}

	/**
	 * {@link #makeThreadResultTree}와 동일: 뷰에 포함되는 샘플(모드 필터 + 시간 구간).
	 */
	private boolean isSampleInThreadView(RecordThreadInfo recThrInfo, long from, long to) {
		if(!includeSample(recThrInfo)) {
			return false;
		}
		if((from > -1L && from > recThrInfo.recordStartTime) || (to > -1L && to < recThrInfo.recordEndTime)) {
			return false;
		}
		return true;
	}

	private boolean isThreadViewMergeOption(String token) {
		return "--merge".equalsIgnoreCase(token) || "-m".equalsIgnoreCase(token);
	}

	private boolean isThreadViewSplitOption(String token) {
		return "--split".equalsIgnoreCase(token) || "-s".equalsIgnoreCase(token);
	}

	/**
	 * NO_REQUEST_WAIT에서 요청 대기 샘플 사이를 기준으로, THREAD 뷰에 넣을 연속 샘플 구간을 나눈다.
	 * (시간 구간 필터는 {@link #isSampleInThreadView}에 포함.)
	 */
	private List<List<RecordThreadInfo>> splitIntoNonWaitViewSegments(List<RecordThreadInfo> fullList, long from, long to) {
		List<List<RecordThreadInfo>> segments = new ArrayList<List<RecordThreadInfo>>();
		if(fullList == null || fullList.isEmpty()) {
			return segments;
		}
		List<RecordThreadInfo> current = null;
		for(RecordThreadInfo r : fullList) {
			if(isSampleInThreadView(r, from, to)) {
				if(current == null) {
					current = new ArrayList<RecordThreadInfo>();
				}
				current.add(r);
			} else if(isWaitingRequestSample(r)) {
				if(current != null) {
					segments.add(current);
					current = null;
				}
			}
		}
		if(current != null) {
			segments.add(current);
		}
		return segments;
	}

	private String formatAbsoluteStackTraceTimeRange(long startMs, long endMs) {
		return DateUtil.dateToString(STACK_TRACE_ABS_TIME_FORMAT, new Date(startMs)) + " ~ "
				+ DateUtil.dateToString(STACK_TRACE_ABS_TIME_FORMAT, new Date(endMs));
	}

	private String formatAbsoluteStackTraceTimeSingle(long timeMs) {
		return DateUtil.dateToString(STACK_TRACE_ABS_TIME_FORMAT, new Date(timeMs));
	}

	/**
	 * Time_ms와 동일한 기준(샘플 가중 시간 합계)으로 구간 시각을 계산한다.
	 * NOTE: recordStart~recordEnd 실제 구간과 다를 수 있으며, THREAD 뷰의 Time_ms와 일치시키기 위한 값이다.
	 */
	private String formatSegmentStackTraceTime(List<RecordThreadInfo> viewSamples) {
		if(viewSamples == null || viewSamples.isEmpty()) {
			return "";
		}
		long segStartMs = viewSamples.get(0).recordStartTime;
		long weightedDuration = 0L;
		for(int i = 0; i < viewSamples.size(); i++) {
			RecordThreadInfo recThrInfo = viewSamples.get(i);
			long realTime = recThrInfo.recordEndTime - recThrInfo.recordStartTime;
			weightedDuration += getSampleTime(viewSamples, i, realTime);
		}
		if(weightedDuration <= 0L) {
			return formatAbsoluteStackTraceTimeSingle(segStartMs);
		}
		long segEndMs = segStartMs + weightedDuration;
		return formatAbsoluteStackTraceTimeRange(segStartMs, segEndMs);
	}

	private List<RecordThreadInfo> filterSamples(List<RecordThreadInfo> recThrInfoList) {
		List<RecordThreadInfo> filtered = new ArrayList<RecordThreadInfo>();
		for(RecordThreadInfo recThrInfo : recThrInfoList) {
			if(includeSample(recThrInfo)) {
				filtered.add(recThrInfo);
			}
		}
		return filtered;
	}

	private long getSampleTime(List<RecordThreadInfo> recThrInfoList, int idx, long realTime) {
		for(int i = idx + 1; i < recThrInfoList.size(); i++) {
			RecordThreadInfo next = recThrInfoList.get(i);
			if(includeSample(next)) {
				long sampleTime = next.recordStartTime - recThrInfoList.get(idx).recordStartTime;
				if(sampleTime > 0L) {
					return sampleTime;
				}
				break;
			}
		}
		if(result.recordIntervalMS > 0L) {
			return result.recordIntervalMS;
		}
		return realTime;
	}

	private Long[] getThreadIdsWithPattern(String pattern) {
		try {
			Long[] targetThreads = JmxThreadCommand.getTargetThreadIds(threadList, pattern);
			if(targetThreads != null && targetThreads.length > 0) {
				Logger.logln(Level.DEBUG, "List of Thread ID : Count=" + targetThreads.length + ", List=[" + StringUtil.arrayToString(targetThreads, ",") + "]");
			}				
			return targetThreads;
		} catch(Exception e) {
			Logger.logln(Level.ERROR, "Failed to get thread id from the argument : " + e.getMessage());
			return null;
		}
	}
	
	public void printResult(List<String> viewArgs) {
		String type = StringUtil.stripQuote(viewArgs.get(0), new char[]{'"','\''}, true);
		if(type.equalsIgnoreCase("info")) {
			if(!checkArgument(viewArgs, 1, 1)) {
				Logger.logln("  Usage) REC[ORD] VIEW INFO");
				return;
			}
			Logger.logln(getInfoString());
		} else if(type.equalsIgnoreCase("method")) {
			if(!checkArgument(viewArgs, 2, 3)) {
				Logger.logln("  Usage) REC[ORD] VIEW METHOD ThreadList [RangeExpression]");
				return;
			}
			Long[] targetThreads = getThreadIdsWithPattern(viewArgs.get(1));
			if(targetThreads == null) return;		
			Map<String,Map<Integer,RecordStackFrame>> methodResultMap;
			long from, to;
			if(viewArgs.size() == 2) {
				// without range expression
				// record view method 1,"pool-*"
				from = -1L;
				to = -1L;
			} else {			
				// with range expression
				String expStr = StringUtil.stripQuote(viewArgs.get(2), new char[]{'"','\''}, true);
				String[] range = expStr.split("~");
				if(range.length != 2) {
					Logger.logln("Invalid argument for RangeExpression. See the help of record command.");
					return;
				}
				String fromStr = range[0];
				String toStr = range[1];
				if(fromStr.endsWith("ms") && NumberUtil.isNumber(fromStr.substring(0, fromStr.length()-2)) && toStr.endsWith("ms") && NumberUtil.isNumber(toStr.substring(0, toStr.length()-2))) {
					// record view method 1,"pool-*" 1000ms~5000ms
					int from_ms = Integer.parseInt(fromStr.substring(0, fromStr.length()-2));
					int to_ms = Integer.parseInt(toStr.substring(0, toStr.length()-2));
					long startTime = result.startTime;
					long endTime = result.endTime;
					from = startTime + from_ms;
					to = startTime + to_ms;
					if(from < startTime || from > endTime || to < startTime || to > endTime) {
						Logger.logln("The relative time(" + expStr + ") is out of range. It must be 0 to " + (endTime-startTime) + "ms");
						return;
					}				
				} else if(NumberUtil.isNumber(fromStr) && NumberUtil.isNumber(toStr)) {
					// record view method 1,"pool-*" 10~20
					int fromIdx = Integer.parseInt(fromStr);
					int toIdx = Integer.parseInt(toStr);
					if(fromIdx < 1 || fromIdx > result.sampleCount || toIdx < 1 || toIdx > result.sampleCount) {
						Logger.logln("The order(" + expStr + ") is out of range. It must be 1 to " + result.sampleCount);
						return;
					}
					from = result.recordData.get(targetThreads[0]).get(fromIdx).recordStartTime;
					to = result.recordData.get(targetThreads[0]).get(toIdx).recordEndTime;
				} else {
					Date fromDt = DateUtil.stringToDate(DateUtil.FORMAT_DATETIME_SEC, fromStr);
					Date toDt = DateUtil.stringToDate(DateUtil.FORMAT_DATETIME_SEC, toStr);
					if(fromDt != null && toDt != null) {
						// record view method 1,"pool-*" 2022.10.28/12:34:17~2022.10.28/12:34:21
						String startTimeStr = DateUtil.dateToString(DateUtil.FORMAT_DATETIME_SEC, new Date(result.startTime));
						String endTimeStr = DateUtil.dateToString(DateUtil.FORMAT_DATETIME_SEC, new Date(result.endTime));
						long startTime = DateUtil.stringToDate(DateUtil.FORMAT_DATETIME_SEC, startTimeStr).getTime();
						long endTime = DateUtil.stringToDate(DateUtil.FORMAT_DATETIME_SEC, endTimeStr).getTime() + 999;
						from = fromDt.getTime();
						to = toDt.getTime() + 999;
						if(from < startTime || from > endTime || to < startTime || to > endTime) {
							Logger.logln("The absolute time(" + expStr + ") is out of range. It must be " + startTimeStr + " to " + endTimeStr);
							return;
						}
					} else {
						Logger.logln("Invalid argument for RangeExpression. See the help of record command.");
						return;
					}
				}
			}
			methodResultMap = makeMethodResultMap(result.recordData, Arrays.asList(targetThreads), from, to);
			Logger.logln(getMethodString(methodResultMap));
		} else if(type.equalsIgnoreCase("thread")) {
			if(!checkArgument(viewArgs, 2, 4)) {
				Logger.logln("  Usage) REC[ORD] VIEW THREAD ThreadList [RangeExpression] [--merge|--split]");
				return;
			}
			Long[] targetThreads = getThreadIdsWithPattern(viewArgs.get(1));
			if(targetThreads == null) return;
			boolean mergeThreads = false;
			String rangeExpression = null;
			for(int i = 2; i < viewArgs.size(); i++) {
				String optOrRange = StringUtil.stripQuote(viewArgs.get(i), new char[]{'"','\''}, true);
				if(isThreadViewMergeOption(optOrRange)) {
					mergeThreads = true;
				} else if(isThreadViewSplitOption(optOrRange)) {
					mergeThreads = false;
				} else if(rangeExpression == null) {
					rangeExpression = optOrRange;
				} else {
					Logger.logln("Invalid argument for THREAD view: " + optOrRange);
					Logger.logln("  Usage) REC[ORD] VIEW THREAD ThreadList [RangeExpression] [--merge|--split]");
					return;
				}
			}
			Map<Long,List<ThreadViewSegment>> threadViewByTid = new HashMap<Long,List<ThreadViewSegment>>();
			long from, to;
			if(rangeExpression == null) {
				// without range expression
				// record view thread 1,"pool-*"
				from = -1L;
				to = -1L;
			} else {
				// with range expression
				String expStr = rangeExpression;
				String[] range = expStr.split("~");
				if(range.length != 2) {
					Logger.logln("Invalid argument for RangeExpression. See the help of record command.");
					return;
				}
				String fromStr = range[0];
				String toStr = range[1];
				if(fromStr.endsWith("ms") && NumberUtil.isNumber(fromStr.substring(0, fromStr.length()-2)) && toStr.endsWith("ms") && NumberUtil.isNumber(toStr.substring(0, toStr.length()-2))) {
					// record view thread 1,"pool-*" 1000ms~5000ms
					int from_ms = Integer.parseInt(fromStr.substring(0, fromStr.length()-2));
					int to_ms = Integer.parseInt(toStr.substring(0, toStr.length()-2));
					long startTime = result.startTime;
					long endTime = result.endTime;
					from = startTime + from_ms;
					to = startTime + to_ms;
					if(from < startTime || from > endTime || to < startTime || to > endTime) {
						Logger.logln("The relative time(" + expStr + ") is out of range. It must be 0 to " + (endTime-startTime) + "ms");
						return;
					}				
				} else if(NumberUtil.isNumber(fromStr) && NumberUtil.isNumber(toStr)) {
					// record view thread 1,"pool-*" 10~20
					int fromIdx = Integer.parseInt(fromStr);
					int toIdx = Integer.parseInt(toStr);
					if(fromIdx < 1 || fromIdx > result.sampleCount || toIdx < 1 || toIdx > result.sampleCount) {
						Logger.logln("The order(" + expStr + ") is out of range. It must be 1 to " + result.sampleCount);
						return;
					}
					from = result.recordData.get(targetThreads[0]).get(fromIdx).recordStartTime;
					to = result.recordData.get(targetThreads[0]).get(toIdx).recordEndTime;
				} else {
					Date fromDt = DateUtil.stringToDate(DateUtil.FORMAT_DATETIME_SEC, fromStr);
					Date toDt = DateUtil.stringToDate(DateUtil.FORMAT_DATETIME_SEC, toStr);
					if(fromDt != null && toDt != null) {
						// record view thread 1,"pool-*" 2022.10.28/12:34:17~2022.10.28/12:34:21
						String startTimeStr = DateUtil.dateToString(DateUtil.FORMAT_DATETIME_SEC, new Date(result.startTime));
						String endTimeStr = DateUtil.dateToString(DateUtil.FORMAT_DATETIME_SEC, new Date(result.endTime));
						long startTime = DateUtil.stringToDate(DateUtil.FORMAT_DATETIME_SEC, startTimeStr).getTime();
						long endTime = DateUtil.stringToDate(DateUtil.FORMAT_DATETIME_SEC, endTimeStr).getTime() + 999;
						from = fromDt.getTime();
						to = toDt.getTime() + 999;
						if(from < startTime || from > endTime || to < startTime || to > endTime) {
							Logger.logln("The absolute time(" + expStr + ") is out of range. It must be " + startTimeStr + " to " + endTimeStr);
							return;
						}
					} else {
						Logger.logln("Invalid argument for RangeExpression. See the help of record command.");
						return;
					}
				}
			}
			if(mergeThreads) {
				List<RecordThreadInfo> mergedViewSamples = new ArrayList<RecordThreadInfo>();
				for(Long tid : targetThreads) {
					List<RecordThreadInfo> fullList = result.recordData.get(tid);
					if(recordViewMode == RecordViewMode.NO_REQUEST_WAIT) {
						for(List<RecordThreadInfo> viewSeg : splitIntoNonWaitViewSegments(fullList, from, to)) {
							mergedViewSamples.addAll(viewSeg);
						}
					} else if(fullList != null) {
						mergedViewSamples.addAll(fullList);
					}
				}
				Tree<RecordStackFrame> mergedTree = makeMergedThreadResultTree(result.recordData, Arrays.asList(targetThreads), from, to);
				List<ThreadViewSegment> mergedSegments = new ArrayList<ThreadViewSegment>();
				mergedSegments.add(new ThreadViewSegment(mergedTree, mergedViewSamples));
				threadViewByTid.put(-1L, mergedSegments);
				threadList.put(-1L, "Merged Threads(" + targetThreads.length + ")");
			} else {
				for(Long tid : targetThreads) {
					List<RecordThreadInfo> fullList = result.recordData.get(tid);
					List<ThreadViewSegment> segments = new ArrayList<ThreadViewSegment>();
					if(recordViewMode == RecordViewMode.NO_REQUEST_WAIT) {
						for(List<RecordThreadInfo> viewSeg : splitIntoNonWaitViewSegments(fullList, from, to)) {
							segments.add(new ThreadViewSegment(makeThreadResultTree(viewSeg, from, to), viewSeg));
						}
					} else {
						segments.add(new ThreadViewSegment(makeThreadResultTree(fullList, from, to), Collections.<RecordThreadInfo>emptyList()));
					}
					threadViewByTid.put(tid, segments);
				}
			}
			Logger.logln(getThreadString(threadViewByTid));
		} else if(type.equalsIgnoreCase("stack")) {
			if(!checkArgument(viewArgs, 3, 3)) {
				Logger.logln("  Usage) REC[ORD] VIEW STACK ThreadList PointExpression");
				return;
			}
			Long[] targetThreads = getThreadIdsWithPattern(viewArgs.get(1));
			if(targetThreads == null) return;
			String expStr = StringUtil.stripQuote(viewArgs.get(2), new char[]{'"','\''}, true);
			if(expStr.endsWith("ms") && expStr.length() > 2 && NumberUtil.isNumber(expStr.substring(0, expStr.length()-2))) {
				int ms = Integer.parseInt(expStr.substring(0, expStr.length()-2));
				long targetTime = result.startTime + ms;
				if(targetTime < result.startTime || targetTime > result.endTime) {
					Logger.logln("The relative time(" + expStr + ") is out of range. It must be 0 to " + (result.endTime-result.startTime) + "ms");
					return;
				}
			} else if(!NumberUtil.isNumber(expStr)) {
				Date dt = DateUtil.stringToDate(DateUtil.FORMAT_DATETIME_SEC, expStr);
				if(dt == null) {
					Logger.logln("Invalid argument for PointExpression. See the help of record command.");
					return;
				}
				String startTimeStr = DateUtil.dateToString(DateUtil.FORMAT_DATETIME_SEC, new Date(result.startTime));
				String endTimeStr = DateUtil.dateToString(DateUtil.FORMAT_DATETIME_SEC, new Date(result.endTime));
				long startTime = DateUtil.stringToDate(DateUtil.FORMAT_DATETIME_SEC, startTimeStr).getTime();
				long endTime = DateUtil.stringToDate(DateUtil.FORMAT_DATETIME_SEC, endTimeStr).getTime() + 999;
				long rangeTime_from = dt.getTime();
				long rangeTime_to = rangeTime_from + 999;
				if(rangeTime_from < startTime || rangeTime_from > endTime || rangeTime_to < startTime || rangeTime_to > endTime) {
					Logger.logln("The absolute time(" + expStr + ") is out of range. It must be " + DateUtil.dateToString(DateUtil.FORMAT_DATETIME_SEC, new Date(startTime)) + " to " + DateUtil.dateToString(DateUtil.FORMAT_DATETIME_SEC, new Date(endTime)));
					return;
				}
			}
			StringBuilder sb = new StringBuilder();
			boolean firstThread = true;
			for(Long tid : targetThreads) {
				List<RecordThreadInfo> recThrInfoList = filterSamples(result.recordData.get(tid));
				String str = resolveStackStringForPoint(recThrInfoList, expStr);
				if(str == null) {
					continue;
				}
				if(firstThread) {
					firstThread = false;
				} else {
					sb.append("\n\n");
				}
				sb.append(str);
			}
			if(firstThread) {
				Logger.logln("No stack output for the selected thread(s) at the given point.");
				return;
			}
			Logger.logln(sb.toString());
		} else if(type.equalsIgnoreCase("search")) {
			if(!checkArgument(viewArgs, 3, 3)) {
				Logger.logln("  Usage) REC[ORD] VIEW SEARCH ThreadList NameExpression");
				return;
			}
			Long[] targetThreads = getThreadIdsWithPattern(viewArgs.get(1));
			if(targetThreads == null) return;		
			if(viewArgs.size() == 3) {
				// with name expression
				// record view search 1,"pool-*" "*Executor.getTask*"
				String expStr = StringUtil.stripQuote(viewArgs.get(2), new char[]{'"','\''}, true);
				if(expStr.length() < 3) {
					Logger.logln("Invalid argument for NameExpression. It requires at least 3 characters.");
					return;
				}
				Map<Long,List<RecordSearch>> foundSearchMap = new HashMap<Long,List<RecordSearch>>();
				for(Long tid : targetThreads) {
					List<RecordThreadInfo> recThrInfoList = filterSamples(result.recordData.get(tid));
					List<RecordSearch> recSearchList = new ArrayList<RecordSearch>();
					for(int i = 0; i < recThrInfoList.size(); i++) {
						RecordThreadInfo recThrInfo = recThrInfoList.get(i);
						for(StackTraceElement el : recThrInfo.stackTrace) {
							if(StringUtil.isMatchStringWithPattern(el.getClassName()+"."+el.getMethodName(), expStr)) {
								boolean match = false;
								for(RecordSearch recSearch : recSearchList) {
									if(recSearch.foundString.equals(el.toString()) && recSearch.toIndex+1 >= i) {
										recSearch.toIndex = i;
										recSearch.count++;
										match = true;
									}
								}
								if(!match) {
									RecordSearch search = new RecordSearch(el.toString(), i);
									recSearchList.add(search);
								}
							}
						}
					}
					if(recSearchList.size() > 0) {
						foundSearchMap.put(tid, recSearchList);
					}
				}
				if(foundSearchMap.size() > 0) {
					Logger.logln(getSearchString(foundSearchMap));					
				} else {
					Logger.logln("The thread could not find a stack matching the pattern \"" + expStr + "\"");
				}
			}			
		} else {
			Logger.logln("Invalid argument for ViewType. Available ViewTypes are 'INFO','METHOD','THREAD','STACK' and 'SEARCH'.");
		}
	}

	/**
	 * Stack dump for one thread at {@code expStr}. Global range for relative ms / absolute time must be validated by the caller.
	 * @return text to print, or {@code null} if this thread has no data or the point cannot be resolved (errors logged).
	 */
	private String resolveStackStringForPoint(List<RecordThreadInfo> recThrInfoList, String expStr) {
		if(recThrInfoList == null || recThrInfoList.isEmpty()) {
			Logger.logln("No stacktrace samples for this thread.");
			return null;
		}
		String str = null;
		if(expStr.endsWith("ms") && expStr.length() > 2 && NumberUtil.isNumber(expStr.substring(0, expStr.length()-2))) {
			int ms = Integer.parseInt(expStr.substring(0, expStr.length()-2));
			long targetTime = result.startTime + ms;
			for(int i = recThrInfoList.size()-1; i >= 0; i--) {
				RecordThreadInfo recThrInfo = recThrInfoList.get(i);
				if(targetTime < recThrInfo.recordStartTime) {
					continue;
				}
				if(targetTime >= recThrInfo.recordStartTime && targetTime <= recThrInfo.recordEndTime) {
					str = getStackString(new RecordThreadInfo[]{recThrInfo});
				} else {
					str = getStackString(new RecordThreadInfo[]{recThrInfo, recThrInfoList.get(i+1)});
				}
				break;
			}
			if(str == null) {
				Logger.logln("Could not resolve stack at relative time " + expStr + " for thread id " + recThrInfoList.get(0).getThreadId() + ".");
				return null;
			}
		} else if(NumberUtil.isNumber(expStr)) {
			int order = Integer.parseInt(expStr);
			int total = recThrInfoList.size();
			if(order < 1 || order > total) {
				Logger.logln("The order(" + order + ") is out of range for thread id " + recThrInfoList.get(0).getThreadId() + ". It must be 1 to " + total);
				return null;
			}
			RecordThreadInfo recThrInfo = recThrInfoList.get(order-1);
			str = getStackString(new RecordThreadInfo[]{recThrInfo});
		} else {
			Date dt = DateUtil.stringToDate(DateUtil.FORMAT_DATETIME_SEC, expStr);
			long rangeTime_from = dt.getTime();
			long rangeTime_to = rangeTime_from + 999;
			List<RecordThreadInfo> matchList = new ArrayList<RecordThreadInfo>();
			for(RecordThreadInfo recThrInfo : recThrInfoList) {
				if(recThrInfo.recordStartTime >= rangeTime_from && recThrInfo.recordEndTime <= rangeTime_to) {
					matchList.add(recThrInfo);
				}
			}
			str = getStackString(matchList.toArray(new RecordThreadInfo[]{}));
		}
		return str;
	}

	
	private String getPercentString(double d) {
		return NumberUtil.numberToString(d*100, "0.###'%'"); 
	}

	private int getIncludedSampleCount() {
		int count = 0;
		for(List<RecordThreadInfo> recThrInfoList : result.recordData.values()) {
			for(RecordThreadInfo recThrInfo : recThrInfoList) {
				if(includeSample(recThrInfo)) {
					count++;
				}
			}
		}
		return count;
	}

	private int getTotalSampleCount() {
		int count = 0;
		for(List<RecordThreadInfo> recThrInfoList : result.recordData.values()) {
			count += recThrInfoList.size();
		}
		return count;
	}
	
	private int getTotalCount(List<RecordThreadInfo> recThrInfoList, long from, long to) {
		int totalCount = 0;
		for(RecordThreadInfo recThrInfo : recThrInfoList) {
			if(!includeSample(recThrInfo)) {
				continue;
			}
			if(from <= recThrInfo.recordStartTime && to >= recThrInfo.recordEndTime) {
				totalCount++;
			}
		}
		return totalCount;
	}
	
	private long getTotalRecordTime(List<RecordThreadInfo> recThrInfoList, long from, long to) {
		long totalRecordTime = 0L;
		for(int idx = 0; idx < recThrInfoList.size(); idx++) {
			RecordThreadInfo recThrInfo = recThrInfoList.get(idx);
			if(!includeSample(recThrInfo)) {
				continue;
			}
			if((from > -1 && from > recThrInfo.recordStartTime) || (to > -1 && to < recThrInfo.recordEndTime)) {
				continue;
			}
			long realTime = recThrInfo.recordEndTime-recThrInfo.recordStartTime;
			totalRecordTime += getSampleTime(recThrInfoList, idx, realTime);
		}
		return totalRecordTime;
	}
	
	private Map<String,Map<Integer,RecordStackFrame>> makeMethodResultMap(Map<Long,List<RecordThreadInfo>> recThrInfoListMap, List<Long> targetThreadList, long from, long to) {
		Map<String,Map<Integer,RecordStackFrame>> map = new HashMap<String,Map<Integer,RecordStackFrame>>();
		int totalCount = 0;
		long totalRecordTime = 0;
		for(Long tid : recThrInfoListMap.keySet()) {
			if(!targetThreadList.contains(tid)) {
				continue;
			}
			List<RecordThreadInfo> recThrInfoList = recThrInfoListMap.get(tid);
			for(int idx = 0; idx < recThrInfoList.size(); idx++) {
				RecordThreadInfo recThrInfo = recThrInfoList.get(idx);
				if(!includeSample(recThrInfo)) {
					continue;
				}
				if((from > -1 && from > recThrInfo.recordStartTime) || (to > -1 && to < recThrInfo.recordEndTime)) {
					continue;
				}				
				long realTime = recThrInfo.recordEndTime-recThrInfo.recordStartTime;
				long sampleTime = getSampleTime(recThrInfoList, idx, realTime);
				for(int i = recThrInfo.stackTrace.length-1; i >= 0; i--) {
					StackTraceElement el = recThrInfo.stackTrace[i];
					String key = el.getClassName() + "." + el.getMethodName() + "()"; 
					Map<Integer,RecordStackFrame> stackFrameMap = map.get(key);
					if(stackFrameMap == null) {
						stackFrameMap = new HashMap<Integer,RecordStackFrame>();
						RecordStackFrame stackFrame = new RecordStackFrame(el);
						stackFrameMap.put(Integer.MIN_VALUE, stackFrame);
						map.put(key, stackFrameMap);
					}
					RecordStackFrame rootStackFrame = stackFrameMap.get(Integer.MIN_VALUE);
					rootStackFrame.threadSet.add(tid);
					rootStackFrame.hit(realTime, sampleTime);
					totalCount++;
					totalRecordTime += sampleTime;
					RecordStackFrame lineStackFrame = stackFrameMap.get(el.getLineNumber());
					if(lineStackFrame == null) {
						lineStackFrame = new RecordStackFrame(el);
						stackFrameMap.put(el.getLineNumber(), lineStackFrame);
					}
					lineStackFrame.threadSet.add(tid);
					lineStackFrame.hit(realTime, sampleTime);
				}
			}
		}
		for(String key : map.keySet()) {
			Map<Integer,RecordStackFrame> stackFrameMap = map.get(key);
			for(Integer line : stackFrameMap.keySet()) {
				RecordStackFrame stackFrame = stackFrameMap.get(line);
				stackFrame.setTotal(totalCount, totalRecordTime);
			}
		}
		return map;
	}
	
	private String getMethodString(Map<String,Map<Integer,RecordStackFrame>> methodResultMap) {
		StringBuilder sb = new StringBuilder();
		if(methodResultMap == null || methodResultMap.size() < 1) {
			return "No method data.";
		}
		sb.append("Aggregation of the record results for " + methodResultMap.size() + " method(s).\n\n");
		List<Map.Entry<String,Map<Integer,RecordStackFrame>>> entryList = new LinkedList<Map.Entry<String,Map<Integer,RecordStackFrame>>>(methodResultMap.entrySet());
		Collections.sort(entryList, new Comparator<Map.Entry<String,Map<Integer,RecordStackFrame>>>() {
			@Override
			public int compare(Entry<String,Map<Integer,RecordStackFrame>> o1, Entry<String,Map<Integer,RecordStackFrame>> o2) {
				Map<Integer,RecordStackFrame> v1 = o1.getValue();
				Map<Integer,RecordStackFrame> v2 = o2.getValue();
				int x = v1.get(Integer.MIN_VALUE).hitCount;
				int y = v2.get(Integer.MIN_VALUE).hitCount;
				return (x > y) ? -1 : ((x == y) ? 0 : 1);
			}
		});
		boolean first = true;
		for(Map.Entry<String,Map<Integer,RecordStackFrame>> entry : entryList) {
			String key = entry.getKey();
			Map<Integer,RecordStackFrame> stackFrameMap = entry.getValue();
			RecordStackFrame rootStackFrame = stackFrameMap.get(Integer.MIN_VALUE);
			if(first) {
				sb.append("    " + rootStackFrame.toMethodHeaderString() + "\n");
				first = false;
			}
			sb.append("    " + rootStackFrame.toMethodRootString(key) + "\n");
			if(stackFrameMap.size() > 2) {
				sb.append("      line stats\n");
				List<Map.Entry<Integer,RecordStackFrame>> subEntryList = new LinkedList<Map.Entry<Integer,RecordStackFrame>>(stackFrameMap.entrySet());
				Collections.sort(subEntryList, new Comparator<Map.Entry<Integer,RecordStackFrame>>() {
					@Override
					public int compare(Entry<Integer,RecordStackFrame> o1, Entry<Integer,RecordStackFrame> o2) {
						RecordStackFrame v1 = o1.getValue();
						RecordStackFrame v2 = o2.getValue();
						int x = v1.hitCount;
						int y = v2.hitCount;
						return (x > y) ? -1 : ((x == y) ? 0 : 1);
					}
				});
				for(Map.Entry<Integer,RecordStackFrame> subEntry : subEntryList) {
					Integer line = subEntry.getKey();
					if(line == Integer.MIN_VALUE) continue;					
					RecordStackFrame stackFrame = subEntry.getValue();
					sb.append("    " + stackFrame.toMethodLineString() + "\n");
				}
			}
		}
		return sb.toString().trim();
	}

	private Tree<RecordStackFrame> makeThreadResultTree(List<RecordThreadInfo> recThrInfoList, long from, long to) {
		int totalCount = 0;
		long totalRecordTime = 0;
		if(from > -1 && to > -1) {
			totalCount = getTotalCount(recThrInfoList, from, to);
			totalRecordTime = getTotalRecordTime(recThrInfoList, from, to);
		} else {
			totalCount = getTotalCount(recThrInfoList, Long.MIN_VALUE, Long.MAX_VALUE);
			totalRecordTime = getTotalRecordTime(recThrInfoList, Long.MIN_VALUE, Long.MAX_VALUE);
		}
		Tree<RecordStackFrame> tree = new Tree<RecordStackFrame>(new RecordStackFrame(null));
		for(int idx = 0; idx < recThrInfoList.size(); idx++) {
			RecordThreadInfo recThrInfo = recThrInfoList.get(idx);
			if(!includeSample(recThrInfo)) {
				continue;
			}
			if((from > -1 && from > recThrInfo.recordStartTime) || (to > -1 && to < recThrInfo.recordEndTime)) {
				continue;
			}				
			long realTime = recThrInfo.recordEndTime-recThrInfo.recordStartTime;
			long sampleTime = getSampleTime(recThrInfoList, idx, realTime);
			tree.toRoot();
			for(int i = recThrInfo.stackTrace.length-1; i >= 0; i--) {
				StackTraceElement el = recThrInfo.stackTrace[i];
				RecordStackFrame nodeData;
				boolean match = false;
				for(TreeNode<RecordStackFrame> node : tree.getCurrent().getChilds()) {
					nodeData = node.getData();
					if(nodeData.stackTraceElement.getClassName().equals(el.getClassName()) && nodeData.stackTraceElement.getMethodName().equals(el.getMethodName()) && nodeData.stackTraceElement.getLineNumber() == el.getLineNumber()) {
						nodeData.hit(realTime, sampleTime);
						tree.setCurrent(node);
						match = true;
					}	
				}
				if(!match) {
					nodeData = new RecordStackFrame(el, totalCount, totalRecordTime);
					nodeData.threadSet.add(recThrInfo.threadId);
					nodeData.hit(realTime, sampleTime);
					tree.setCurrent(tree.getCurrent().addChild(nodeData));
				}
			}
		}
		return tree;
	}

	private Tree<RecordStackFrame> makeMergedThreadResultTree(Map<Long,List<RecordThreadInfo>> recThrInfoListMap, List<Long> targetThreadList, long from, long to) {
		int totalCount = 0;
		long totalRecordTime = 0L;
		for(Long tid : targetThreadList) {
			List<RecordThreadInfo> recThrInfoList = recThrInfoListMap.get(tid);
			if(recThrInfoList == null) {
				continue;
			}
			if(from > -1L && to > -1L) {
				totalCount += getTotalCount(recThrInfoList, from, to);
				totalRecordTime += getTotalRecordTime(recThrInfoList, from, to);
			} else {
				totalCount += getTotalCount(recThrInfoList, Long.MIN_VALUE, Long.MAX_VALUE);
				totalRecordTime += getTotalRecordTime(recThrInfoList, Long.MIN_VALUE, Long.MAX_VALUE);
			}
		}
		Tree<RecordStackFrame> tree = new Tree<RecordStackFrame>(new RecordStackFrame(null));
		for(Long tid : targetThreadList) {
			List<RecordThreadInfo> recThrInfoList = recThrInfoListMap.get(tid);
			if(recThrInfoList == null) {
				continue;
			}
			for(int idx = 0; idx < recThrInfoList.size(); idx++) {
				RecordThreadInfo recThrInfo = recThrInfoList.get(idx);
				if(!includeSample(recThrInfo)) {
					continue;
				}
				if((from > -1L && from > recThrInfo.recordStartTime) || (to > -1L && to < recThrInfo.recordEndTime)) {
					continue;
				}
				long realTime = recThrInfo.recordEndTime-recThrInfo.recordStartTime;
				long sampleTime = getSampleTime(recThrInfoList, idx, realTime);
				tree.toRoot();
				for(int i = recThrInfo.stackTrace.length-1; i >= 0; i--) {
					StackTraceElement el = recThrInfo.stackTrace[i];
					RecordStackFrame nodeData;
					boolean match = false;
					for(TreeNode<RecordStackFrame> node : tree.getCurrent().getChilds()) {
						nodeData = node.getData();
						if(nodeData.stackTraceElement.getClassName().equals(el.getClassName()) && nodeData.stackTraceElement.getMethodName().equals(el.getMethodName()) && nodeData.stackTraceElement.getLineNumber() == el.getLineNumber()) {
							nodeData.threadSet.add(recThrInfo.threadId);
							nodeData.hit(realTime, sampleTime);
							tree.setCurrent(node);
							match = true;
						}
					}
					if(!match) {
						nodeData = new RecordStackFrame(el, totalCount, totalRecordTime);
						nodeData.threadSet.add(recThrInfo.threadId);
						nodeData.hit(realTime, sampleTime);
						tree.setCurrent(tree.getCurrent().addChild(nodeData));
					}
				}
			}
		}
		return tree;
	}

	/**
	 * 스레드별 스택 트리의 모든 프레임을 순회해, THREAD 뷰에서 (hit/total)·(sample/total) 숫자 우측 정렬에 쓸 공통 자릿수를 구한다.
	 */
	private void collectThreadTreeStatWidths(TreeNode<RecordStackFrame> node, int[] out) {
		if(node == null) {
			return;
		}
		RecordStackFrame f = node.getData();
		if(f != null && f.stackTraceElement != null) {
			int cn = Math.max(String.valueOf(f.hitCount).length(), String.valueOf(f.totalCount).length());
			int tn = Math.max(String.valueOf(f.sampleRecordTime).length(), String.valueOf(f.totalRecordTime).length());
			out[0] = Math.max(out[0], cn);
			out[1] = Math.max(out[1], tn);
		}
		for(TreeNode<RecordStackFrame> ch : node.getChilds()) {
			collectThreadTreeStatWidths(ch, out);
		}
	}

	private String getThreadString(Map<Long,List<ThreadViewSegment>> threadViewByTid) {
		StringBuilder sb = new StringBuilder();
		if(threadViewByTid == null || threadViewByTid.size() < 1) {
			return "No thread data.";
		}
		int shownThreadCount = 0;
		Iterator<Long> iter = threadViewByTid.keySet().iterator();
		while(iter.hasNext()) {
			Long tid = iter.next();
			List<ThreadViewSegment> segments = threadViewByTid.get(tid);
			List<RecordThreadInfo> recThrInfoList = new ArrayList<RecordThreadInfo>();
			if(tid != null && tid.longValue() >= 0L) {
				List<RecordThreadInfo> byTid = result.recordData.get(tid);
				if(byTid != null) {
					recThrInfoList = filterSamples(byTid);
				}
			} else if(segments != null) {
				for(ThreadViewSegment seg : segments) {
					if(seg.viewSamples != null) {
						recThrInfoList.addAll(seg.viewSamples);
					}
				}
			}
			boolean hasData = false;
			if(segments != null) {
				for(ThreadViewSegment seg : segments) {
					if(seg.tree.getRoot().getChild() != null) {
						hasData = true;
						break;
					}
				}
			}
			if(!showEmptyThread && !hasData) {
				continue;
			}
			shownThreadCount++;
			String threadName = (tid != null && tid.longValue() >= 0L && recThrInfoList.size() > 0) ? recThrInfoList.get(0).getThreadName() : threadList.get(tid);
			int totalFoundStacktraceCount = recThrInfoList.size();
			sb.append("  @ \"" + threadName + "\"" + " Id=" + tid + " (FoundStacktrace=" + totalFoundStacktraceCount + ")\n");
			if(!hasData || segments == null || segments.isEmpty()) {
				sb.append("    No stacktrace data.\n");
				sb.append("\n");
				continue;
			}
			int[] maxStatWidths = new int[]{1, 1};
			for(ThreadViewSegment seg : segments) {
				TreeNode<RecordStackFrame> r = seg.tree.getRoot().getChild();
				if(r == null) {
					continue;
				}
				int[] w = new int[]{1, 1};
				collectThreadTreeStatWidths(r, w);
				maxStatWidths[0] = Math.max(maxStatWidths[0], w[0]);
				maxStatWidths[1] = Math.max(maxStatWidths[1], w[1]);
			}
			TreeNode<RecordStackFrame> headerNode = null;
			for(ThreadViewSegment seg : segments) {
				if(seg.tree.getRoot().getChild() != null) {
					headerNode = seg.tree.getRoot().getChild();
					break;
				}
			}
			if(headerNode != null) {
				sb.append("    " + headerNode.getData().toThreadHeaderString(maxStatWidths[0], maxStatWidths[1]) + "\n");
			}
			boolean firstPrintedTree = true;
			for(ThreadViewSegment seg : segments) {
				TreeNode<RecordStackFrame> threadRoot = seg.tree.getRoot().getChild();
				boolean treeHasData = threadRoot != null;
				if(!showEmptyThread && !treeHasData) {
					continue;
				}
				if(!firstPrintedTree) {
					sb.append("\n");
				}
				firstPrintedTree = false;
				if(!treeHasData) {
					sb.append("    No stacktrace data.\n");
					continue;
				}
				String rootSuffix = "";
				if(recordViewMode == RecordViewMode.NO_REQUEST_WAIT && seg.viewSamples != null && !seg.viewSamples.isEmpty()) {
					rootSuffix = "  |  stacktrace time: " + formatSegmentStackTraceTime(seg.viewSamples);
				}
				appendThreadBranchString(sb, threadRoot, new ArrayList<Boolean>(), true, true, maxStatWidths[0], maxStatWidths[1], rootSuffix);
			}
			sb.append("\n");
		}
		if(shownThreadCount < 1) {
			return "No thread data.";
		}
		sb.insert(0, "Aggregation of the record results for " + shownThreadCount + " thread(s).\n\n");
		return sb.toString().trim();
	}
	
	private void appendThreadBranchString(StringBuilder sb, TreeNode<RecordStackFrame> branchRoot, List<Boolean> ancestorsHasNext, boolean isLastBranch, boolean rootBranch, int countNumW, int timeNumW, String rootStatSuffix) {
		if(branchRoot == null) return;
		if(rootBranch) {
			String rootLine = branchRoot.getData().toThreadBranchString(countNumW, timeNumW);
			if(rootStatSuffix != null && rootStatSuffix.length() > 0) {
				rootLine = rootLine + rootStatSuffix;
			}
			sb.append("    " + rootLine + "\n");
		} else {
			appendThreadTreeLine(sb, ancestorsHasNext, isLastBranch, branchRoot.getData().toThreadBranchString(countNumW, timeNumW));
		}
		List<Boolean> pathAncestors = new ArrayList<Boolean>(ancestorsHasNext);
		if(!rootBranch) {
			pathAncestors.add(!isLastBranch);
		}
		List<TreeNode<RecordStackFrame>> pathNodes = new ArrayList<TreeNode<RecordStackFrame>>();
		TreeNode<RecordStackFrame> branchPoint = branchRoot;
		while(branchPoint != null) {
			pathNodes.add(branchPoint);
			List<TreeNode<RecordStackFrame>> childList = getSortedChildList(branchPoint);
			if(childList.size() == 1) {
				branchPoint = childList.get(0);
			} else {
				break;
			}
		}
		for(int i = 0; i < pathNodes.size(); i++) {
			TreeNode<RecordStackFrame> pathNode = pathNodes.get(i);
			boolean isLastPath = (i == pathNodes.size()-1);
			appendThreadTreeLine(sb, pathAncestors, isLastPath, pathNode.getData().toThreadPathString());
		}
		List<TreeNode<RecordStackFrame>> branchChildren = getSortedChildList(pathNodes.get(pathNodes.size()-1));
		if(branchChildren.size() > 1) {
			List<Boolean> childAncestors = new ArrayList<Boolean>(pathAncestors);
			childAncestors.add(false);
			for(int i = 0; i < branchChildren.size(); i++) {
				TreeNode<RecordStackFrame> child = branchChildren.get(i);
				boolean isLastChildBranch = (i == branchChildren.size()-1);
				appendThreadBranchString(sb, child, childAncestors, isLastChildBranch, false, countNumW, timeNumW, null);
			}
		}
	}

	private void appendThreadTreeLine(StringBuilder sb, List<Boolean> ancestorsHasNext, boolean last, String content) {
		sb.append("    ");
		for(Boolean hasNext : ancestorsHasNext) {
			sb.append(Boolean.TRUE.equals(hasNext) ? "│   " : "    ");
		}
		sb.append(last ? "└── " : "├── ");
		sb.append(content);
		sb.append("\n");
	}

	private List<TreeNode<RecordStackFrame>> getSortedChildList(TreeNode<RecordStackFrame> node) {
		List<TreeNode<RecordStackFrame>> childList = node.getChilds();
		Collections.sort(childList, new Comparator<TreeNode<RecordStackFrame>>() {
			@Override
			public int compare(TreeNode<RecordStackFrame> o1, TreeNode<RecordStackFrame> o2) {
				int x = (Integer)o1.getData().hitCount;
				int y = (Integer)o2.getData().hitCount;
				return (x > y) ? -1 : ((x == y) ? 0 : 1);
			}
		});
		return childList;
	}
	
	private String getSearchString(Map<Long,List<RecordSearch>> foundSearchMap) {
		StringBuilder sb = new StringBuilder();
		if(foundSearchMap == null || foundSearchMap.size() < 1) {
			return "No search data.";
		}
		sb.append("Found a stack matching the pattern in " + foundSearchMap.size() + " thread(s).\n\n");
		Iterator<Long> iter = foundSearchMap.keySet().iterator();
		while(iter.hasNext()) {
			Long tid = iter.next();
			List<RecordThreadInfo> recThrInfoList = filterSamples(result.recordData.get(tid));
			List<RecordSearch> searchList = foundSearchMap.get(tid);
			sb.append("  @ \"" + recThrInfoList.get(0).getThreadName() + "\"" + " Id=" + tid + "\n");
			for(RecordSearch recSearch : searchList) {
				long startTime = recThrInfoList.get(recSearch.fromIndex).recordStartTime;
				long endTime = recThrInfoList.get(recSearch.toIndex).recordEndTime;
				String from = DateUtil.dateToString(DateUtil.FORMAT_DATETIME_MSEC, new Date(startTime));
				String to = DateUtil.dateToString(DateUtil.FORMAT_DATETIME_MSEC, new Date(endTime));
				sb.append("      - Found Stack Frame : " + recSearch.foundString + "\n");
				sb.append("          . Period  : " + from + "~" + to + " (" + (startTime-result.startTime) + "~" + (endTime-result.startTime) + "ms)\n");
				sb.append("          . Count   : " + recSearch.count + "/" + recThrInfoList.size() + " (" + getPercentString((double)recSearch.count/recThrInfoList.size()) + ")\n");
				sb.append("          . Indexes : " + (recSearch.fromIndex+1) + ((recSearch.fromIndex!=recSearch.toIndex)?(" ~ "+(recSearch.toIndex+1)):"") + "\n");
			}
			if(iter.hasNext()) {
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	public String getStackString(RecordThreadInfo[] recThrInfos) {
		StringBuilder sb = new StringBuilder();
		if(recThrInfos.length < 1) {
			return "No stack data.";
		}
		RecordThreadInfo first = recThrInfos[0];
		RecordThreadInfo last = recThrInfos[recThrInfos.length-1];
		sb.append("@ \"" + first.getThreadName() + "\"" + " Id=" + first.getThreadId() + "\n");
		String time = DateUtil.dateToString(DateUtil.FORMAT_DATETIME_MSEC, new Date(first.recordStartTime));
		sb.append("  - Time    : " + time + " (" + (first.recordStartTime-result.startTime) + "ms)\n");
		if(recThrInfos.length == 1) {
			sb.append("  - State   : \n");
			sb.append("      . Blocked : " +  first.blockedCount + "(" + first.blockedTime + "ms)\n");
			sb.append("      . Waited  : " +  first.waitedCount + "(" + first.waitedTime + "ms)\n");
		} else {
			sb.append("  - Count   : " + recThrInfos.length + " samples\n");
			sb.append("  - State   : \n");
			sb.append("      . Blocked : " +  first.blockedCount + "(" + first.blockedTime + "ms) ~ " + last.blockedCount + "(" + last.blockedTime + "ms)\n");
			sb.append("      . Waited  : " +  first.waitedCount + "(" + first.waitedTime + "ms) ~ " + last.waitedCount + "(" + last.waitedTime + "ms)\n");
		}
		for(RecordThreadInfo recThrInfo : recThrInfos) {
			if(recThrInfos.length > 1) {
				String stackFrom = DateUtil.dateToString(DateUtil.FORMAT_DATETIME_MSEC, new Date(recThrInfo.recordStartTime));
				String stackTo = DateUtil.dateToString(DateUtil.FORMAT_DATETIME_MSEC, new Date(recThrInfo.recordEndTime));
				sb.append("  - Traces  : " + stackFrom + "~" + stackTo + " (" + (recThrInfo.recordStartTime-result.startTime) + "~" + (recThrInfo.recordEndTime-result.startTime) + "ms)\n");
			} else {
				sb.append("  - Traces  : \n");
			}
			for(StackTraceElement el : recThrInfo.stackTrace) {
				sb.append("       at " + el.toString() + "\n");
			}			
		}
		return sb.toString();
	}
	
	public String getInfoString() {
		StringBuilder sb = new StringBuilder();		
		sb.append("Information of the recorded data.\n\n");
		sb.append("  - Record Time     : " + DateUtil.dateToString(STACK_TRACE_ABS_TIME_FORMAT, new Date(result.startTime)) + " ~ " + DateUtil.dateToString(STACK_TRACE_ABS_TIME_FORMAT, new Date(result.endTime)) + "\n");
		sb.append("  - Record Duration : " + (result.endTime-result.startTime) + "ms\n");
		sb.append("  - Record Interval : " + result.recordIntervalMS + "ms\n");
		sb.append("  - Sample Count    : " + result.sampleCount + "\n");
		sb.append("  - View Mode       : " + recordViewMode.name() + "\n");
		if(recordViewMode == RecordViewMode.NO_REQUEST_WAIT) {
			sb.append("  - View Stacktrace : " + getIncludedSampleCount() + " / " + getTotalSampleCount() + "\n");
		} else {
			sb.append("  - View Stacktrace : " + getTotalSampleCount() + "\n");
		}
		sb.append("  - Dump File Path  : " + (result.dmpFilePath==null?"N/A(in-memory)":result.dmpFilePath) + "\n");
		sb.append("  - Tool VM\n");
		sb.append("      . Version : " + result.toolInfo.get("version") + "\n");
		sb.append("      . OS      : " + result.toolInfo.get("os") + "\n");
		sb.append("      . Java    : " + result.toolInfo.get("java") + "\n");
		sb.append("      . PID     : " + result.toolInfo.get("pid") + "\n");
		sb.append("      . TIME    : " + DateUtil.dateToString(STACK_TRACE_ABS_TIME_FORMAT, new Date(Long.parseLong(result.toolInfo.get("time")))) + "\n");
		sb.append("  - Target VM\n");
		sb.append("      . OS      : " + result.vmInfo.get("os") + "\n");
		sb.append("      . Java    : " + result.vmInfo.get("java") + "\n");
		sb.append("      . PID     : " + result.vmInfo.get("pid") + "\n");
		sb.append("      . Name    : " + result.vmInfo.get("name") + "\n");
		sb.append("      . TIME    : " + DateUtil.dateToString(STACK_TRACE_ABS_TIME_FORMAT, new Date(Long.parseLong(result.vmInfo.get("time")))) + "\n");
		sb.append("  - Record Thread List (" + result.recordData.size() + " threads)\n");
		for(Long id : new TreeSet<Long>(result.recordData.keySet())) {
			List<RecordThreadInfo> threadInfoList = result.recordData.get(id);
			if(threadInfoList != null && threadInfoList.size() > 0) {
				RecordThreadInfo threadInfo = threadInfoList.get(0);
				ResourceUsage resourceUsage = result.resourceData.get(id);
				String thrStr = "\"" + StringUtil.shortenStringWithSuffix(resourceUsage.threadName, 38, "...") + "\"";
				long totalCpu = resourceUsage.currCpu - resourceUsage.startCpu;
				long totalMem = resourceUsage.currMem - resourceUsage.startMem;
				String cpuStr = "Unknown";
				String memStr = "Unknown";
				if(totalCpu > -1) {
					cpuStr = String.valueOf(totalCpu);
				}
				if(totalCpu > -1) {
					memStr = String.valueOf(totalMem);
				}
				sb.append(String.format("      . tid [%5d] : %-40s : CpuTime=%-12s, AllocatedBytes=%s", threadInfo.getThreadId(), thrStr, cpuStr, memStr) + "\n");
			}
		}		
		return sb.toString();
	}
	
}
