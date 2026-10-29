package dal.tool.trace.jmxer;

/**
 * JMXer 버전·프롬프트·기본 dump 경로 등 전역 상수
 *
 */
public class JMXerConstant {

	public static final String JMXER_VERSION = "2.1.0";
	public static final String JMXER_VERSION_STRING = "JMXer v" + JMXER_VERSION;
	public static final String JMXER_COPYRIGHT_STRING = "Copyright 2022. YoungDal,Kwon All rights reserved.";
	public static final String JMXER_PROMPT_STRING = "JMXer";
	public static final String JMXER_PROMPT_NOT_CONNECTED_STRING = "JMXer[NotConnected]";
	
	public static final String DEFAULT_DUMP_FILE_NAME = "JMXer_Record_Trace.dmp";
	public static final String DEFAULT_DUMP_FILE_PATH = "./" + DEFAULT_DUMP_FILE_NAME;
	public static final String DUMP_FILE_VALIDATION_HEADER = "[JMXer Record File]";

	/** 끊긴 연결에서 connect·호출 시 JMX/RMI 원격 요청 타임아웃(ms) */
	public static final int JMX_REMOTE_REQUEST_TIMEOUT_MS = 15000;

}
