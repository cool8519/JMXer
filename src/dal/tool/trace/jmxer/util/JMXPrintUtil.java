package dal.tool.trace.jmxer.util;

import java.lang.management.ThreadInfo;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.Descriptor;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanFeatureInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;

import dal.tool.cli.Logger;
import dal.tool.cli.util.PrintUtil;
import dal.tool.util.ObjectDataUtil;
import dal.tool.util.PatternUtil;
import dal.tool.util.PatternUtil.PATTERN_TYPE;
import dal.tool.util.StringUtil;
import dal.tool.util.TypeUtil;
import dal.tool.util.jmx.JMXHelper;
import dal.tool.util.jmx.JMXUtil;
import dal.tool.util.jmx.MBeanConnector.ConnectType;
import dal.tool.util.jmx.MBeanNotFoundException;
import dal.tool.util.jmx.ThreadUtil;

/**
 * JMX 호출 결과출력을 위해 필요한 Utility 클래스
 * @author 권영달
 *
 */
public class JMXPrintUtil {

	/**
	 * JVM에 접속하여 JMX를 호출하고, 결과를 출력한다.
	 * @param connect_type 연결 방식
	 * @param connect_value 연결 대상 문자열. connect_type에 따라 의미가 다르다.
	 * @param authenticate 인증 문자열 배열(user/password)
	 * @param object_name MBean ObjectName 문자열
	 * @param target_name 호출 대상 이름. 가능한 값은 다음과 같다.
	 * <xmp>
	 *         - Attribute명
	 *         - Operation명
	 *         - "?" 전체 Attribute 및 Operation 정보
	 *         - "#" 전체 Attribute 값
	 * </xmp>
	 * @param target_values 호출 대상 값. target_name에 따라 의미가 다르다.
	 * <xmp>
	 *         - target_name이 Attribute인 경우, 변경할 Attribute 값
	 *         - target_name이 Operation인 경우, Operation Arguments 문자열 배열
	 *         - target_name이 "?" 또는 "#"인 경우, null이어야 함
	 * </xmp>
	 * @param prettyPrint 결과 출력시 newline 및 indentation을 포함하려면 true        
	 * @throws Exception 호출 과정에서 발생한 모든 Exception
	 */
	public static void callJMX(ConnectType connect_type, String connect_value, String[] authenticate, String object_name, String target_name, String[] target_values, boolean prettyPrint) throws Exception {
		JMXConnector connector = JMXHelper.connectToVM(connect_type, connect_value, authenticate);
		MBeanServerConnection mbeanConn = connector.getMBeanServerConnection();
		callJMX(mbeanConn, object_name, target_name, target_values, true);
	}

	/**
	 * JVM에 접속하여 JMX를 호출하고, 결과를 출력한다.
	 * @param mbeanConn 접속할 JVM의 MBeanServerConnection 객체 
	 * @param object_name MBean ObjectName 문자열
	 * @param target_name 호출 대상 이름. 가능한 값은 다음과 같다.
	 * <xmp>
	 *         - Attribute명
	 *         - Operation명
	 *         - "?" 전체 Attribute 및 Operation 정보
	 *         - "#" 전체 Attribute 값
	 * </xmp>
	 * @param target_values 호출 대상 값. target_name에 따라 의미가 다르다.
	 * <xmp>
	 *         - target_name이 Attribute인 경우, 변경할 Attribute 값
	 *         - target_name이 Operation인 경우, Operation Arguments 문자열 배열
	 *         - target_name이 "?" 또는 "#"인 경우, null이어야 함
	 * </xmp>
	 * @param prettyPrint 결과 출력시 newline 및 indentation을 포함하려면 true        
	 * @throws MBeanNotFoundException object_name을 찾을 수 없을 때 발생
	 * @throws Exception 호출 과정에서 발생한 모든 Exception
	 */
	public static void callJMX(MBeanServerConnection mbeanConn, String object_name, String target_name, String[] target_values, boolean prettyPrint) throws MBeanNotFoundException, Exception {
		if(object_name == null) {
			Logger.logln("No ObjectName input.");
			return;
		}

		ObjectName inputOName = null;
		if(object_name != null && "?".equals(object_name) == false) {
			inputOName = new ObjectName(object_name);
		}

		Set<ObjectName> queryNames = mbeanConn.queryNames(inputOName, null);
		if(queryNames.size() == 0) {
			throw new MBeanNotFoundException("ObjectName '" + inputOName + "' not found");
		}
		Iterator<ObjectName> it = queryNames.iterator();
		while(it.hasNext()) {
			ObjectName oName = it.next();
			Logger.logln("ObjectName : " + oName);
			Logger.logln("");
			if(target_name == null || "??".equals(target_name)) {
				// print all info
				printMBeanInfo(mbeanConn, null, null);
			} else if("?".equals(target_name)) {
				if(target_values != null) {
					throw new Exception("To view all info, argument should be null. To invoke a operation, select a name of Operation or Attribute.");
				}
				// print all attributes and operations
				printAttributeAndOperationList(mbeanConn, oName, null);
			} else if("#".equals(target_name)) {
				if(target_values != null) {
					throw new Exception("To view all of attribute values, argument should be null. To invoke a operation, select a name of Operation or Attribute.");
				}
				// print all value
				printAttributeValue(mbeanConn, oName, null);
			} else {
				if(inputOName == null) {
					throw new Exception("To invoke a operation, select an ObjectName.");
				}
				// get attribute or invoke operation
				Object opRet = JMXUtil.invokeAttributeOrOperation(mbeanConn, oName, target_name, target_values);
				if(opRet != null) {
					Logger.logln("################################## Return Value ##################################");
					Logger.logln(ObjectDataUtil.toString(opRet, prettyPrint));
					Logger.logln("##################################################################################");
				}
			}
			if(it.hasNext()) {
				Logger.logln("----------------------------------------------------------------------------------");
			}
		}
	}

	/**
	 * 전체 MBean ObjectName을 출력한다.
	 * @param mbeanConn 접속할 JVM의 MBeanServerConnection 객체
	 * @param objectNamePattern 출력할 ObjectName 이름 패턴. null이면 전체 목록 출력 
	 * @throws Exception 수행 과정에서 발생한 모든 Exception
	 */
	public static void printObjectNames(MBeanServerConnection mbeanConn, String objectNamePattern) throws Exception {
		printObjectNames(JMXUtil.getObjectNames(mbeanConn, objectNamePattern), objectNamePattern);
	}

	/**
	 * 전달받은 MBean ObjectName을 출력한다.
	 * @param objectNameList MBean ObjectName 객체 목록
	 * @param objectNamePattern 출력할 ObjectName 이름 패턴. 안내 메시지에 보여질 패턴이며, null이면 출력하지 않는다.
	 */
	public static void printObjectNames(List<ObjectName> objectNameList, String objectNamePattern) {
		Logger.logln("");
		if(objectNameList.size() < 1) {
			Logger.logln("No ObjectName exists" + ((objectNamePattern == null) ? "" : (" : " + objectNamePattern)));
		} else {
			Logger.logln("List of MBean ObjectName" + ((objectNamePattern == null) ? "" : (" : " + objectNamePattern)));
			ObjectName oname;
			for(int i = 0; i < objectNameList.size(); i++) {
				oname = objectNameList.get(i);
				Logger.logln(" - " + oname);
			}
		}
	}
	
	/**
	 * 전달받은 MBean ObjectName을 Index와 함께 출력한다.
	 * @param objectNameStringList 정렬된 MBean ObjectName 이름 목록
	 * @param objectNamePrintList 출력할 ObjectName 객체 목록. null이면 전체 목록을 출력한다.
	 * @param objectNamePattern 출력할 ObjectName 이름 패턴. 안내 메시지에 보여질 패턴이며, null이면 출력하지 않는다.
	 */
	public static void printObjectNames(List<String> objectNameStringList, List<ObjectName> objectNamePrintList, String objectNamePattern) {
		Logger.logln("");
		if(objectNamePrintList != null && objectNamePrintList.size() < 1) {
			Logger.logln("No ObjectName exists" + ((objectNamePattern == null) ? "" : (" : " + objectNamePattern)));
		} else {
			Logger.logln("List of MBean ObjectName" + ((objectNamePattern == null) ? "" : (" : " + objectNamePattern)));
			for(int i = 1; i <= objectNameStringList.size(); i++) {
				String name = objectNameStringList.get(i-1);
				if(objectNamePrintList == null) {
					Logger.logln(String.format(" #%-2d - %s", i, name));
				} else {
					for(ObjectName oname : objectNamePrintList) {
						if(name.equals(oname.toString())) {
							Logger.logln(String.format(" #%-2d - %s", i, name));
						}
					}
				}
			}
		}
	}

	/**
	 * MBean 상세 정보를 출력한다.
	 * @param mbeanConn 접속할 JVM의 MBeanServerConnection 객체 
	 * @param oName MBean ObjectName 객체
	 * @param name 출력할 대상의 이름
	 * @throws Exception 수행 과정에서 발생한 모든 Exception
	 */
	public static void printMBeanInfo(MBeanServerConnection mbeanConn, ObjectName oName, String name) throws Exception {
		MBeanInfo mbeanInfo = mbeanConn.getMBeanInfo(oName);

		if(name == null) {
			Logger.logln("Description : " + mbeanInfo.getDescription());

			StringBuffer desc = new StringBuffer();
			Descriptor mbeanDesc = mbeanInfo.getDescriptor();
			for(String descName : mbeanDesc.getFieldNames()) {
				Object descValue = mbeanDesc.getFieldValue(descName);
				desc.append("\n - " + descName + " : " + descValue);
			}
			Logger.logln("Descriptor : " + desc);
		}

		if(mbeanInfo.getConstructors().length > 0) {
			StringBuffer constr = new StringBuffer();
			for(MBeanConstructorInfo info : mbeanInfo.getConstructors()) {
				if(name == null || name.equals(info.getName())) {
					constr.append("\n - " + info.getName());
					constr.append("\n   . Description : " + info.getDescription());
					constr.append("\n   . Parameters  : " + JMXHelper.toTypeString(info.getSignature()));
				}
			}
			Logger.logln("Constructors : " + constr);
		}

		if(mbeanInfo.getAttributes().length > 0) {
			StringBuffer attrs = new StringBuffer();
			for(MBeanAttributeInfo info : mbeanInfo.getAttributes()) {
				if(name == null || name.equals(info.getName())) {
					attrs.append("\n - " + info.getName());
					attrs.append("\n   . Description : " + info.getDescription());
					attrs.append("\n   . Type        : " + JMXHelper.toTypeString(info.getType()));
					attrs.append("\n   . Permission  : " + JMXHelper.getPermissionString(info));
				}
			}
			Logger.logln("Attributes : " + attrs);
		}

		if(mbeanInfo.getOperations().length > 0) {
			StringBuffer opers = new StringBuffer();
			for(MBeanOperationInfo info : mbeanInfo.getOperations()) {
				if(name == null || name.equals(info.getName())) {
					opers.append("\n - " + info.getName());
					opers.append("\n   . Description : " + info.getDescription());
					opers.append("\n   . Parameters  : " + JMXHelper.toNameAndTypeString(info.getSignature()));
					opers.append("\n   . Return      : " + JMXHelper.toTypeString(info.getReturnType()));
					opers.append("\n   . Impact      : " + JMXHelper.getOperationImpactString(info.getImpact()));
				}
			}
			Logger.logln("Operations : " + opers);
		}

		if(mbeanInfo.getNotifications().length > 0) {
			StringBuffer notis = new StringBuffer();
			for(MBeanNotificationInfo info : mbeanInfo.getNotifications()) {
				if(name == null || name.equals(info.getName())) {
					notis.append("\n - " + info.getName());
					notis.append("\n   . Description : " + info.getDescription());
					notis.append("\n   . Types       : " + JMXHelper.toTypeString(info.getNotifTypes()));
				}
			}
			Logger.logln("Notifications : " + notis);
		}
	}

	/**
	 * MBean Attribute 및 Operation 목록을 출력한다.
	 * @param mbeanConn 접속할 JVM의 MBeanServerConnection 객체 
	 * @param oName MBean ObjectName 객체
	 * @param namePattern 출력할 Attribute 또는 Operation 이름 패턴. null이면 전체 목록 출력 
	 * @throws Exception 수행 과정에서 발생한 모든 Exception
	 */
	public static void printAttributeAndOperationList(MBeanServerConnection mbeanConn, ObjectName oName, String namePattern) throws Exception {
		printAttributeAndOperationList(JMXUtil.getAttributeAndOperationList(mbeanConn, oName, namePattern), oName.toString(), namePattern);
	}

	/**
	 * 주어진 MBean Attribute 및 Operation 목록을 출력한다.
	 * @param featureList MBeanFeatureInfo 객체 목록 
	 * @param objectName MBean ObjectName 문자열. 안내 메시지에 보여질 패턴이며, null이 될 수 없다.
	 * @param namePattern 출력할 Attribute 또는 Operation 이름 패턴. 안내 메시지에 보여질 패턴이며, null이면 출력하지 않는다. 
	 */
	public static void printAttributeAndOperationList(List<MBeanFeatureInfo> featureList, String objectName, String namePattern) {
		if(featureList.size() < 1) {
			Logger.logln("No Attribute and Operation exists (" + objectName + ")" + ((namePattern == null) ? "" : (" : " + namePattern)));
		} else {
			Logger.logln("List of Attribute and Operation (" + objectName + ")" + ((namePattern == null) ? "" : (" : " + namePattern)));
			for(Object info : featureList) {
				if(info instanceof MBeanAttributeInfo) {
					MBeanAttributeInfo attr = (MBeanAttributeInfo)info;
					if(namePattern == null || PatternUtil.isMatchPattern(PATTERN_TYPE.STRING, attr.getName(), namePattern)) {
						Logger.logln(" - Attribute(" + JMXHelper.getPermissionStringSimple(attr) + ") " + attr.getName() + " : type=" + JMXHelper.toTypeString(attr.getType()));
					}
				}
			}
			for(Object info : featureList) {
				if(info instanceof MBeanOperationInfo) {
					MBeanOperationInfo oper = (MBeanOperationInfo)info;
					if(namePattern == null || PatternUtil.isMatchPattern(PATTERN_TYPE.STRING, oper.getName(), namePattern)) {
						MBeanParameterInfo[] paramInfos = oper.getSignature();
						Logger.logln(" - Operation     " + oper.getName() + " : " + ((paramInfos.length > 0) ? ("argumentType=" + JMXHelper.toTypeString(paramInfos) + ", ") : "") + "returnType=" + JMXHelper.toTypeString(oper.getReturnType()));
					}
				}
			}
		}
	}

	/**
	 * MBean Attribute 값을 출력한다.
	 * @param mbeanConn 접속할 JVM의 MBeanServerConnection 객체 
	 * @param oName MBean ObjectName 객체
	 * @param targetName Attribute명 또는 Operation명. null이면 전체 정보를 출력한다.
	 * @throws Exception 수행 과정에서 발생한 모든 Exception
	 */
	public static void printAttributeValue(MBeanServerConnection mbeanConn, ObjectName oName, String targetName) throws Exception {
		Map<MBeanAttributeInfo, Object> attrValueMap = JMXUtil.getAttributeValuesWithInfo(mbeanConn, oName, targetName);
		if(attrValueMap.size() < 1) {
			Logger.logln("No Attribute exists (" + oName + ")");
		} else {
			Logger.logln("Current Value of Attributes (" + oName + ")");
			Iterator<MBeanAttributeInfo> iter = attrValueMap.keySet().iterator();
			while(iter.hasNext()) {
				MBeanAttributeInfo info = iter.next();
				if(!info.isReadable()) {
					continue;
				}
				String perm = JMXHelper.getPermissionStringSimple(info);
				String name = info.getName();
				String value = "#UNKNOWN";
				Object valueObj = attrValueMap.get(info);
				if(valueObj == null || !(valueObj instanceof Exception)) {
					value = ObjectDataUtil.toString(valueObj, true);
				} else if(valueObj instanceof Exception) {
					value = "#ERROR(" + ((Exception)valueObj).getMessage();
				}
				if(targetName == null) {
					Logger.logln(" - (" + perm + ") " + name + "=" + value);
				} else {
					Logger.logln("Attribute Name  : " + targetName);
					Logger.logln("Attribute Value : " + value);
					Logger.logln("Permission      : " + perm);
					return;
				}
			}

		}
	}

	/**
	 * Thread의 StackTrace 정보를 출력한다.
	 * @param mbeanConn 접속할 JVM의 MBeanServerConnection 객체 
	 * @param threadIds stacktrace를 출력할 threadId 문자열 배열. "*"인 경우, 모든 쓰레드의 stacktrace를 출력한다.
	 * @param maxDepth stacktrace 출력할 엔트리의 최대 개수. 1보다 작으면 전체 엔트리 출력.
	 * @throws Exception 수행 과정에서 발생한 모든 Exception
	 */
	public static void printStackTrace(MBeanServerConnection mbeanConn, String[] threadIds, int maxDepth) throws Exception {
		if(threadIds.length < 1) {
			return;
		}
		Object resultData = null;
		boolean allThreads = false;
		if(threadIds.length == 1 && threadIds[0].equals("*")) {
			allThreads = true;
			resultData = JMXUtil.getJMXResult(mbeanConn, "java.lang:type=Threading", "dumpAllThreads", new String[] { "false", "false" });
		} else {
			String threadIdsString = StringUtil.arrayToString(threadIds, ",");
			String maxDepthString = (maxDepth < 1) ? String.valueOf(Integer.MAX_VALUE) : String.valueOf(maxDepth);
			resultData = JMXUtil.getJMXResult(mbeanConn, "java.lang:type=Threading", "getThreadInfo(long[],int)", new String[] { threadIdsString, maxDepthString });
		}

		CompositeData[] resultDataArr = null;
		if(resultData == null) {
			throw new Exception("Could not get thread info.");
		} else {
			resultDataArr = (CompositeData[])resultData;
			if(allThreads) {
				Arrays.sort(resultDataArr, new Comparator<CompositeData>() {
					@Override
					public int compare(CompositeData cd1, CompositeData cd2) {
						return (int)(ThreadInfo.from(cd1).getThreadId() - ThreadInfo.from(cd2).getThreadId());
					}
				});
			}
			if(resultDataArr == null || resultDataArr.length < 1) {
				throw new Exception("Could not get thread info.");
			}
		}

		int cnt = 0;
		for(CompositeData cd : resultDataArr) {
			if(cd == null) {
				continue;
			}
			Logger.logln("");
			ThreadInfo threadInfo = ThreadInfo.from(cd);
			List<String> dump = ThreadUtil.getThreadDump(threadInfo);
			dump.set(0, "@ " + dump.get(0));
			for(String line : dump) {
				Logger.logln(line);
			}
			cnt++;
		}
		if(cnt == 0) {
			Logger.logln("Could not get thread info.");
		} else {
			Logger.logln("");
		}
	}

	/**
	 * Thread의 정보를 출력한다.
	 * @param mbeanConn 접속할 JVM의 MBeanServerConnection 객체 
	 * @param threadIds 쓰레드 정보를 출력할 threadId 문자열 배열. "*"인 경우, 모든 쓰레드의 정보를 출력한다.
	 * @throws Exception 수행 과정에서 발생한 모든 Exception
	 */
	public static void printThreadInfo(MBeanServerConnection mbeanConn, String[] threadIds) throws Exception {
		if(threadIds.length < 1) {
			return;
		}
		Object resultData = null;
		String threadIdsString = null;
		if(threadIds.length == 1 && threadIds[0].equals("*")) {
			Object tempData = JMXUtil.getJMXResult(mbeanConn, "java.lang:type=Threading", "AllThreadIds", null);
			Object[] tempDataArr = TypeUtil.getObjectArray(tempData);
			Arrays.sort(tempDataArr);
			threadIdsString = StringUtil.arrayToString(tempDataArr, ",");
		} else {
			threadIdsString = StringUtil.arrayToString(threadIds, ",");
		}
		resultData = JMXUtil.getJMXResult(mbeanConn, "java.lang:type=Threading", "getThreadInfo(long[])", new String[] { threadIdsString });

		CompositeData[] resultDataArr = null;
		if(resultData == null) {
			throw new Exception("Could not get thread info.");
		} else {
			resultDataArr = (CompositeData[])resultData;
			if(resultDataArr == null || resultDataArr.length < 1) {
				throw new Exception("Could not get thread info.");
			}
		}

		int cnt = 0;
		for(CompositeData cd : resultDataArr) {
			if(cd == null) {
				continue;
			}
			Logger.logln("");
			ThreadInfo threadInfo = ThreadInfo.from(cd);
			Logger.logln("Thread id " + threadInfo.getThreadId());
			Logger.logln(" - Name      : " + threadInfo.getThreadName());
			Logger.logln(" - State     : " + threadInfo.getThreadState());
			Logger.logln(" - Suspend   : " + threadInfo.isSuspended());
			Logger.logln(" - InNative  : " + threadInfo.isInNative());
			Logger.logln(" - Blocked   : " + threadInfo.getBlockedCount() + " times / " + ((threadInfo.getBlockedTime() < 0) ? "currently not blocked" : ("blocked for " + threadInfo.getBlockedTime() + " ms")));
			Logger.logln(" - Waiting   : " + threadInfo.getWaitedCount() + " times / " + ((threadInfo.getWaitedTime() < 0) ? "currently not waiting" : ("wating for " + threadInfo.getWaitedTime() + " ms")));
			Logger.logln(" - Lock      : " + threadInfo.getLockName());
			Logger.logln(" - LockOwner : " + threadInfo.getLockOwnerName());
			Logger.logln(" - Locked");
			Logger.logln("   . Monitors      : " + PrintUtil.toString(threadInfo.getLockedMonitors(), true, "     "));
			Logger.logln("   . Synchronizers : " + PrintUtil.toString(threadInfo.getLockedSynchronizers(), true, "     "));
			cnt++;
		}
		if(cnt == 0) {
			Logger.logln("Could not get thread info.");
		} else {
			Logger.logln("");
		}
	}

	/**
	* Thread 목록을 출력한다.
	* @param mbeanConn 접속할 JVM의 MBeanServerConnection 객체
	* @param threadIds 출력할 ThreadID 문자열 배열. null이면 모든 Thread를 가져온다. 
	* @throws Exception 수행 과정에서 발생한 모든 Exception
	*/
	public static void printThreadList(MBeanServerConnection mbeanConn, String[] threadIds) throws Exception {
		ObjectName objectName = new ObjectName("java.lang:type=Threading");

		String threadIdsString;
		if(threadIds == null) {
			long[] tids = JMXUtil.getAllThreadIds(mbeanConn, true);
			String[] tidStrArr = new String[tids.length];
			for(int i = 0; i < tids.length; i++) {
				tidStrArr[i] = String.valueOf(tids[i]);
			}
			threadIdsString = StringUtil.arrayToString(tidStrArr, ",");
		} else {
			threadIdsString = StringUtil.arrayToString(threadIds, ",");
		}
		Object resultData = JMXUtil.invokeAttributeOrOperation(mbeanConn, objectName, "getThreadInfo(long[])", new String[] { threadIdsString });

		CompositeData[] resultDataArr = null;
		if(resultData == null) {
			throw new Exception("Could not get thread info.");
		} else {
			resultDataArr = (CompositeData[])resultData;
			if(resultDataArr == null || resultDataArr.length < 1) {
				throw new Exception("Could not get thread info.");
			}
		}

		int cnt = 0;
		Logger.logln("List of Thread");
		for(CompositeData cd : resultDataArr) {
			if(cd == null) {
				continue;
			}
			ThreadInfo threadInfo = ThreadInfo.from(cd);
			Logger.logln(String.format(" - tid [%4d]  %-13s  :  \"%s\"", threadInfo.getThreadId(), threadInfo.getThreadState(), threadInfo.getThreadName()));
			cnt++;
		}

		Logger.logln("");
		Logger.logln("Total " + cnt + " thread(s).");
	}

	/**
	* 전체 Thread 목록을 출력한다.
	* @param mbeanConn 접속할 JVM의 MBeanServerConnection 객체 
	* @throws Exception 수행 과정에서 발생한 모든 Exception
	*/
	public static void printAllThreadList(MBeanServerConnection mbeanConn) throws Exception {
		printThreadList(mbeanConn, null);
	}

}
