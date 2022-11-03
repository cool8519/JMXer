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
import dal.tool.trace.jmxer.cli.command.JmxThreadCommand;
import dal.tool.trace.jmxer.cli.data.RecordResult;
import dal.tool.trace.jmxer.cli.data.RecordSearch;
import dal.tool.trace.jmxer.cli.data.RecordStackFrame;
import dal.tool.trace.jmxer.cli.data.RecordThreadInfo;
import dal.tool.trace.jmxer.cli.data.Tree;
import dal.tool.trace.jmxer.cli.data.TreeNode;
import dal.tool.util.DateUtil;
import dal.tool.util.NumberUtil;
import dal.tool.util.StringUtil;

public class RecordResultViewer {

	RecordResult result;
	HashMap<Long,String> threadList = new HashMap<Long,String>();
	
	
	public RecordResultViewer(RecordResult result) {
		this.result = result;
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
	
	private Long getThreadIdWithPattern(String pattern) {
		Long targetThread = -1L;
		try {
			Long[] targetThreads = JmxThreadCommand.getTargetThreadIds(threadList, pattern);
			if(targetThread == null || targetThreads.length == 0) {
	    		Logger.logln("No target thread specified.");
	    		return null;
			} else {
				targetThread = targetThreads[0];
				Logger.logln(Level.DEBUG, "Thread ID : " + targetThread);
				if(targetThreads.length > 1) {
		    		Logger.logln(targetThreads.length + " threads are specified. Only one thread could be printed for stacktrace.");
		    		return null;
				}
			}
			return targetThread;
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
			if(!checkArgument(viewArgs, 2, 3)) {
				Logger.logln("  Usage) REC[ORD] VIEW THREAD ThreadList [RangeExpression]");
				return;
			}
			Long[] targetThreads = getThreadIdsWithPattern(viewArgs.get(1));
			if(targetThreads == null) return;
			Map<Long,Tree<RecordStackFrame>> threadResultTreeMap = new HashMap<Long,Tree<RecordStackFrame>>();
			long from, to;
			if(viewArgs.size() == 2) {
				// without range expression
				// record view thread 1,"pool-*"
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
			for(Long tid : targetThreads) {
				threadResultTreeMap.put(tid, makeThreadResultTree(result.recordData.get(tid), from, to));
			}
			Logger.logln(getThreadString(threadResultTreeMap));
		} else if(type.equalsIgnoreCase("stack")) {
			if(!checkArgument(viewArgs, 3, 3)) {
				Logger.logln("  Usage) REC[ORD] VIEW STACK TargetThread PointExpression");
				return;
			}
			Long targetThread = getThreadIdWithPattern(viewArgs.get(1));
			if(targetThread == null) return;
			List<RecordThreadInfo> recThrInfoList = result.recordData.get(targetThread);
			if(viewArgs.size() == 3) {
				// with point expression
				String expStr = StringUtil.stripQuote(viewArgs.get(2), new char[]{'"','\''}, true);
				String str = null;
				if(expStr.endsWith("ms") && NumberUtil.isNumber(expStr.substring(0, expStr.length()-2))) {
					// record view stack 1 1500ms
					int ms = Integer.parseInt(expStr.substring(0, expStr.length()-2));
					long startTime = result.startTime;
					long endTime = result.endTime;
					long targetTime = startTime + ms;
					if(targetTime < startTime || targetTime > endTime) {
						Logger.logln("The relative time(" + expStr + ") is out of range. It must be 0 to " + (endTime-startTime) + "ms");
						return;
					}					
					for(int i = recThrInfoList.size()-1; i >= 0; i--) {
						RecordThreadInfo recThrInfo = recThrInfoList.get(i);
						if(targetTime < recThrInfo.recordStartTime) {
							continue;
						} else {
							if(targetTime >= recThrInfo.recordStartTime && targetTime <= recThrInfo.recordEndTime) {
								str = getStackString(new RecordThreadInfo[]{recThrInfo});
							} else {
								str = getStackString(new RecordThreadInfo[]{recThrInfo, recThrInfoList.get(i+1)});
							}
							break;
						}
					}					
				} else if(NumberUtil.isNumber(expStr)) {
					// record view stack 1 13
					int order = Integer.parseInt(expStr);
					int total = recThrInfoList.size();
					if(order < 1 || order > total) {
						Logger.logln("The order(" + order + ") is out of range. It must be 1 to " + total);
						return;
					}
					RecordThreadInfo recThrInfo = recThrInfoList.get(order-1);
					str = getStackString(new RecordThreadInfo[]{recThrInfo});					
				} else {
					Date dt = DateUtil.stringToDate(DateUtil.FORMAT_DATETIME_SEC, expStr);
					if(dt != null) {
						// record view stack 1 2022.10.28/12:35:01
						String startTimeStr = DateUtil.dateToString(DateUtil.FORMAT_DATETIME_SEC, new Date(result.startTime));
						String endTimeStr = DateUtil.dateToString(DateUtil.FORMAT_DATETIME_SEC, new Date(result.endTime));
						long startTime = DateUtil.stringToDate(DateUtil.FORMAT_DATETIME_SEC, startTimeStr).getTime();
						long endTime = DateUtil.stringToDate(DateUtil.FORMAT_DATETIME_SEC, endTimeStr).getTime() + 999;
						long targetTime = dt.getTime();
						long rangeTime_from = targetTime ;
						long rangeTime_to = targetTime + 999;
						if(rangeTime_from < startTime || rangeTime_from > endTime || rangeTime_to < startTime || rangeTime_to > endTime) {
							Logger.logln("The absolute time(" + expStr + ") is out of range. It must be " + DateUtil.dateToString(DateUtil.FORMAT_DATETIME_SEC, new Date(startTime)) + " to " + DateUtil.dateToString(DateUtil.FORMAT_DATETIME_SEC, new Date(endTime)));
							return;
						}
						List<RecordThreadInfo> matchList = new ArrayList<RecordThreadInfo>();
						for(RecordThreadInfo recThrInfo : recThrInfoList) {
							if(recThrInfo.recordStartTime >= rangeTime_from && recThrInfo.recordEndTime <= rangeTime_to) {
								matchList.add(recThrInfo);
							}
						}
						str = getStackString(matchList.toArray(new RecordThreadInfo[]{}));
					} else {
						Logger.logln("Invalid argument for PointExpression. See the help of record command.");
						return;
					}
				}
				Logger.logln(str);
			}			
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
					List<RecordThreadInfo> recThrInfoList = result.recordData.get(tid);
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

	
	private String getPercentString(double d) {
		return NumberUtil.numberToString(d*100, "0.###'%'"); 
	}
	
	private int getTotalCount(List<RecordThreadInfo> recThrInfoList, long from, long to) {
		int totalCount = 0;
		for(RecordThreadInfo recThrInfo : recThrInfoList) {
			if(from <= recThrInfo.recordStartTime && to >= recThrInfo.recordEndTime) {
				totalCount++;
			}
		}
		return totalCount;
	}
	
	private long getTotalRecordTime(List<RecordThreadInfo> recThrInfoList, long from, long to) {
		long totalRecordTime = 0;
		for(int i = recThrInfoList.size()-1; i >= 0; i--) {
			RecordThreadInfo recThrInfo = recThrInfoList.get(i);
			if(to >= recThrInfo.recordEndTime) {
				if(i+1 < recThrInfoList.size()) {
					totalRecordTime += recThrInfoList.get(i+1).recordStartTime;
				} else {
					totalRecordTime += recThrInfo.recordEndTime;
				}
				break;
			}
		}
		for(RecordThreadInfo recThrInfo : recThrInfoList) {
			if(from <= recThrInfo.recordStartTime) {
				totalRecordTime -= recThrInfo.recordStartTime;
				break;
			}
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
				if((from > -1 && from > recThrInfo.recordStartTime) || (to > -1 && to < recThrInfo.recordEndTime)) {
					continue;
				}				
				long realTime = recThrInfo.recordEndTime-recThrInfo.recordStartTime;
				long sampleTime = (idx+1 < recThrInfoList.size()) ? (recThrInfoList.get(idx+1).recordStartTime-recThrInfo.recordStartTime) : realTime;
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
				sb.append("    " + rootStackFrame.toMethodString(null, true) + "\n");
				first = false;
			}
			sb.append("    " + rootStackFrame.toMethodString(key, true) + "\n");
			if(stackFrameMap.size() > 2) {
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
					sb.append("    " + stackFrame.toMethodString(key, false) + "\n");
				}
			}
		}
		return sb.toString();
	}

	private Tree<RecordStackFrame> makeThreadResultTree(List<RecordThreadInfo> recThrInfoList, long from, long to) {
		int totalCount = 0;
		long totalRecordTime = 0;
		if(from > -1 && to > -1) {
			totalCount = getTotalCount(recThrInfoList, from, to);
			totalRecordTime = getTotalRecordTime(recThrInfoList, from, to);
		} else {
			totalCount = recThrInfoList.size();
			totalRecordTime = recThrInfoList.get(recThrInfoList.size()-1).recordEndTime - recThrInfoList.get(0).recordStartTime;
		}
		Tree<RecordStackFrame> tree = new Tree<RecordStackFrame>(new RecordStackFrame(null));
		for(int idx = 0; idx < recThrInfoList.size(); idx++) {
			RecordThreadInfo recThrInfo = recThrInfoList.get(idx);
			if((from > -1 && from > recThrInfo.recordStartTime) || (to > -1 && to < recThrInfo.recordEndTime)) {
				continue;
			}				
			long realTime = recThrInfo.recordEndTime-recThrInfo.recordStartTime;
			long sampleTime = (idx+1 < recThrInfoList.size()) ? (recThrInfoList.get(idx+1).recordStartTime-recThrInfo.recordStartTime) : realTime;
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

	private String getThreadString(Map<Long,Tree<RecordStackFrame>> recordResultTreeMap) {
		StringBuilder sb = new StringBuilder();
		if(recordResultTreeMap == null || recordResultTreeMap.size() < 1) {
			return "No thread data.";
		}
		sb.append("Aggregation of the record results for " + recordResultTreeMap.size() + " thread(s).\n\n");
		Iterator<Long> iter = recordResultTreeMap.keySet().iterator();
		while(iter.hasNext()) {
			Long tid = iter.next();
			List<RecordThreadInfo> recThrInfoList = result.recordData.get(tid);
			Tree<RecordStackFrame> tree = recordResultTreeMap.get(tid);
			sb.append("  @ \"" + recThrInfoList.get(0).getThreadName() + "\"" + " Id=" + tid + "\n");
			if(tree.getRoot().getChild() != null) {
				sb.append("    " + tree.getRoot().getChild().getData().toThreadString(-1, tree.getMaxDepth()) + "\n");
				appendStackFrameString(sb, tree.getRoot(), -1, tree.getMaxDepth());
			} else {
				sb.append("    No stacktrace data.\n");
			}
			if(iter.hasNext()) {
				sb.append("\n");
			}
		}
		return sb.toString();
	}
	
	private void appendStackFrameString(StringBuilder sb, TreeNode<RecordStackFrame> treeNode, int depth, int maxDepth) {
		if(treeNode == null) return;
		if(treeNode.getData().stackTraceElement != null) {
			sb.append("    " + treeNode.getData().toThreadString(treeNode.getDepth(), maxDepth) + "\n");
		}
		List<TreeNode<RecordStackFrame>> childList = treeNode.getChilds();
		Collections.sort(childList, new Comparator<TreeNode<RecordStackFrame>>() {
			@Override
			public int compare(TreeNode<RecordStackFrame> o1, TreeNode<RecordStackFrame> o2) {
				int x = (Integer)o1.getData().hitCount;
				int y = (Integer)o2.getData().hitCount;
				return (x > y) ? -1 : ((x == y) ? 0 : 1);
			}
		});
		for(TreeNode<RecordStackFrame> child : childList) {
			appendStackFrameString(sb, child, depth+1, maxDepth);
		}
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
			List<RecordThreadInfo> recThrInfoList = result.recordData.get(tid);
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
		String from = DateUtil.dateToString(DateUtil.FORMAT_DATETIME_MSEC, new Date(first.recordStartTime));
		String to = DateUtil.dateToString(DateUtil.FORMAT_DATETIME_MSEC, new Date(last.recordEndTime));
		sb.append("  - Period  : " + from + "~" + to + " (" + (first.recordStartTime-result.startTime) + "~" + (last.recordEndTime-result.startTime) + "ms)\n");
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
		sb.append("  - Record Time     : " + DateUtil.dateToString(DateUtil.FORMAT_DATETIME_SEC, new Date(result.startTime)) + " ~ " + DateUtil.dateToString(DateUtil.FORMAT_DATETIME_SEC, new Date(result.endTime)) + "\n");
		sb.append("  - Record Duration : " + (result.endTime-result.startTime) + "ms\n");
		sb.append("  - Record Interval : " + result.recordIntervalMS + "ms\n");
		sb.append("  - Sample Count    : " + result.sampleCount + "\n");
		sb.append("  - Dump File Path  : " + (result.dmpFilePath==null?"N/A(in-memory)":result.dmpFilePath) + "\n");
		sb.append("  - Tool VM\n");
		sb.append("      . Version : " + result.toolInfo.get("version") + "\n");
		sb.append("      . OS      : " + result.toolInfo.get("os") + "\n");
		sb.append("      . Java    : " + result.toolInfo.get("java") + "\n");
		sb.append("      . PID     : " + result.toolInfo.get("pid") + "\n");
		sb.append("      . TIME    : " + DateUtil.dateToString(DateUtil.FORMAT_DATETIME_SEC, new Date(Long.parseLong(result.toolInfo.get("time")))) + "\n");
		sb.append("  - Target VM\n");
		sb.append("      . OS      : " + result.vmInfo.get("os") + "\n");
		sb.append("      . Java    : " + result.vmInfo.get("java") + "\n");
		sb.append("      . PID     : " + result.vmInfo.get("pid") + "\n");
		sb.append("      . Name    : " + result.vmInfo.get("name") + "\n");
		sb.append("      . TIME    : " + DateUtil.dateToString(DateUtil.FORMAT_DATETIME_SEC, new Date(Long.parseLong(result.vmInfo.get("time")))) + "\n");
		sb.append("  - Record Thread List (" + result.recordData.size() + " threads)\n");
		for(Long id : new TreeSet<Long>(result.recordData.keySet())) {
			List<RecordThreadInfo> threadInfoList = result.recordData.get(id);
			if(threadInfoList != null && threadInfoList.size() > 0) {
				RecordThreadInfo threadInfo = threadInfoList.get(0);
				sb.append(String.format("      . tid [%-3d]  :  \"%s\"", threadInfo.getThreadId(), threadInfo.getThreadName()) + "\n");
			}
		}		
		return sb.toString();
	}
	
}
