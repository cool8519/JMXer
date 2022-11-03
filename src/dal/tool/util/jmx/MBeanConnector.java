package dal.tool.util.jmx;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

import dal.tool.cli.Logger;


/**
 * JMX 호출에 필요한 접속정보,연결세션 등을 관리하는 클래스<br/>
 * 인스턴스 당 JMX 세션 1개가 생성된다.
 * @author 권영달
 *
 */
public class MBeanConnector {

	/**
	 * JMX 연결 방식<br/>
	 *  - NOT_SET : 미설정<br/>
	 *  - ATTACH_BY_PID : Local JVM 중 ProcessID로 Attach 함. JDK 1.6 이상에서 가능.<br/>
	 *  - ATTACH_BY_NAME : Local JVM 중 ProcessName으로 Attach 함. JDK 1.6 이상에서 가능.<br/>
	 *  - RMI_BY_ADDRESS : IP와 Port로 RMI 접속(Local/Remote 및 하위버전 JDK에서 사용 가능)<br/>
	 *  - UNKNOWN : 알수 없음<br/>
	 */
	public static enum ConnectType { NOT_SET, ATTACH_BY_PID, ATTACH_BY_NAME, RMI_BY_ADDRESS, UNKNOWN };

	private ConnectType connectType = ConnectType.NOT_SET;
	private String targetName = null;
	private String authUser = null;
	private String authPass = null;
	private boolean initialized = false;
	
	private JMXConnector jmxConnector = null;
	private MBeanServerConnection mbeanConnection = null;
	

    /**
     * MBeanConnector 객체를 생성한다.
     * @param connectType 연결 방식
     * @param target 연결 대상 문자열. connectType에 따라 의미가 다르다.
	 * @param auth 인증 문자열 배열(user/password)
     */
    public MBeanConnector(ConnectType connectType, String target, String[] auth) {
    	this.connectType = connectType;
    	this.targetName = target;
    	if(auth != null && auth.length == 2) {
    		authUser = auth[0];
    		authPass = auth[1];
    	}
    }
    
    /**
     * MBeanConnector 객체를 생성한다.
     * @param connectType 연결 방식
     * @param target 연결 대상. connectType에 따라 의미가 다르다.
     * @param authUser 인증 User 문자열
     * @param authPass 인증 Password 문자열
     */
    public MBeanConnector(ConnectType connectType, String target, String authUser, String authPass) {
    	this.connectType = connectType;
    	this.targetName = target;
    	this.authUser = authUser;
    	this.authPass = authPass;
	}

    /**
     * 연결을 끊고, 세션을 정리한다.
     */
    public void close() {
		initialized = false;
		
		if(jmxConnector != null) {
			try {
				jmxConnector.close();
			} catch(Exception e) {
				Logger.logln(Logger.Level.ERROR, "Failed to close JMX connection.");
			} finally {
				jmxConnector = null;
			}
		}

		if(mbeanConnection != null) {
			mbeanConnection = null;
		}    
    }

    /**
     * 이전 세션을 정리하고, 새로 JVM에 접속한 뒤 세션을 저장한다.
     * @throws Exception 수행 과정에서 발생한 모든 Exception
     */
    public void connect() throws Exception {
    	close();
    	try {
    		Logger.logln("Connecting to JVM(" + connectType.name() + ":" + targetName + ") ...");
			jmxConnector = JMXHelper.connectToVM(connectType, targetName, new String[]{authUser, authPass});
    		mbeanConnection = jmxConnector.getMBeanServerConnection();
    		initialized = true;
    		Logger.logln("Successfully connected to JVM.");
    		Logger.logln("");
    	} catch(Exception e) {
    		initialized = false;
    		throw e;
    	}
    }
    

	/**
	 * 연결 방식을 가져온다.
	 * @return 연결 방식
	 */
	public ConnectType getConnectType() {
		return connectType;
	}

	/**
	 * 연결 방식을 설정한다.
	 * @param connectType 연결 방식
	 */
	public void setConnectType(ConnectType connectType) {
		this.connectType = connectType;
	}

	/**
	 * 연결 대상을 가져온다.
	 * @return 연결 대상 문자열
	 */
	public String getTargetName() {
		return targetName;
	}

	/**
	 * 연결 대상을 설정한다.
	 * @param targetName 연결 대상 문자열
	 */
	public void setTargetName(String targetName) {
		this.targetName = targetName;
	}

	/**
	 * 인증 User를 가져온다.
	 * @return 인증 User 문자열
	 */
	public String getAuthUser() {
		return authUser;
	}

	/**
	 * 인증 User를 설정한다.
	 * @param authUser 인증 User 문자열
	 */
	public void setAuthUser(String authUser) {
		this.authUser = authUser;
	}

	/**
	 * 인증 Password를 가져온다.
	 * @return 인증 Password 문자열
	 */
	public String getAuthPass() {
		return authPass;
	}

	/**
	 * 인증 Password를 설정한다.
	 * @param authPass 인증 Password 문자열
	 */
	public void setAuthPass(String authPass) {
		this.authPass = authPass;
	}

	/**
	 * JMX 통신을 위한 JMXConnector 객체를 가져온다.
	 * @return JMXConnector 객체
	 */
	public JMXConnector getJMXConnector() {
		return jmxConnector;
	}

	/**
	 * JMX 통신을 위한 MBeanConnection을 가져온다.
	 * @return MBeanConnection 객체
	 */
	public MBeanServerConnection getMBeanConnection() {
    	return mbeanConnection;
    }
    
    /**
     * 정상 연결 여부를 확인한다. 
     * @return 정상 연결이 되었으면 true
     */
    public boolean isInitialized() {
    	return initialized;
    }

}
