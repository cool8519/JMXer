package dal.tool.cli;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;


/**
 * 화면/파일 로깅을 담당하는 클래스
 * @author 권영달
 *
 */
public class Logger extends Object {

	/**
	 * 로그 레벨
	 * <xmp>
	 *  - RESULT : 결과 기록
	 *  - ERROR  : 결과,에러 기록
	 *  - INFO   : 결과,에러,정보 기록
	 *  - DEBUG  : 결과,에러,정보,상세 기록
	 * </xmp>
	 */
	public static enum Level { RESULT, ERROR, WARNING, INFO, DEBUG }
	
    public static final int BUFFER_SIZE = 8192;

    private static String spoolName = null;
    private static File f = null;
    private static FileWriter fw = null;
    private static BufferedWriter bw = null;
    private static boolean silent = false;
    private static Level showLevel = Level.WARNING;


    /**
     * 로그 기록 여부를 가져온다.
     * @return 기록하지 않으면 true
     */
    public static boolean getSilent() {
        return silent;
    }


    /**
     * 로그 기록 여부를 설정한다.
     * @param s 기록하지 않으려면 true
     */
    public static void setSilent(boolean s) {
        silent = s;
    }


    /**
     * 로그 레벨을 설정한다.<br/>
     * 설정된 레벨보다 낮은 로그 메시지는 무시된다.
     * @param level 로그 레벨
     */
    public static void setShowLevel(Level level) {
        showLevel = level;
    }

    
    /**
     * 로그 레벨을 가져온다.
     * @return 로그 레벨
     */
    public static Level getShowLevel() {
    	return showLevel;
    }
    
    
    /**
     * 입력받은 로그 레벨이 출력가능한 지 확인한다.<br/>
     * @param level 로그 레벨
     * @return 출력 가능하면 true
     */
    public static boolean isShowLevel(Level level) {
        if(!silent && level.ordinal() <= showLevel.ordinal()) {
        	return true;
        } else {
        	return false;
        }
    }

    
    /**
     * 스풀 파일명을 가져온다.
     * @return 파일명
     */
    public static String getSpoolName() {
        return spoolName;
    }

    
    /**
     * 스풀 파일명을 설정한다. 이후 로깅은 스풀 파일에 기록된다.
     * @param fname 파일명(경로 포함)
     * @param append append 모드이면 true
     */
    public static void setSpoolName(String fname, boolean append) {
        try {
            if(spoolName != null) {
                setSpoolOff();
            }
            spoolName = fname;
            f = new File(spoolName);
            fw = new FileWriter(f, append);
            bw = new BufferedWriter(fw, BUFFER_SIZE);
        } catch(Exception e) {}
    }


    /**
     * 스풀 파일 기록을 중지한다.
     */
    public static void setSpoolOff() {
        try {
            if(spoolName != null) {
                bw.flush();
                bw.close();
            }
            spoolName = null;
        } catch(Exception e) {}
    }


    /**
     * 로그를 기록한다.
     * @param msg 로그 메시지
     */
    public static void log(Object msg) {
        log(Level.RESULT, msg);
    }


    /**
     * 로그를 기록한다. 마지막에 newline 문자를 추가한다.
     * @param msg 로그 메시지
     */
    public static void logln(Object msg) {
        logln(Level.RESULT, msg);
    }

    
    /**
     * 로그를 기록한다.
     * @param level 로그 레벨
     * @param msg 로그 메시지
     */
    public static void log(Level level, Object msg) {
        if(isShowLevel(level)) {
            log(((level == Level.RESULT)?"":("["+level.name()+"] ")) + msg, false);
        }
    }


    /**
     * 로그를 기록한다. 마지막에 newline 문자를 추가한다.
     * @param level 로그 레벨
     * @param msg 로그 메시지
     */
    public static void logln(Level level, Object msg) {
        if(isShowLevel(level)) {
            log(((level == Level.RESULT)?"":("["+level.name()+"] ")) + msg, true);
        }
    }


    /**
     * 화면과 스풀에 로그를 기록한다.
     * @param msg 로그 메시지
     * @param newline newline 문자를 추가하려면 true
     */
    public static void log(Object msg, boolean newline) {
        try {
            if(newline) {
                System.out.println(msg);
            } else {
                System.out.print(msg);
            }

            logOnlySpool(msg, newline);
        } catch(Exception e) {}
    }


    /**
     * 화면없이 스풀 로그만 기록한다.
     * @param msg 로그 메시지
     * @param newline newline 문자를 추가하려면 true
     */
    public static void logOnlySpool(Object msg, boolean newline) {
        String str = "";
        try {
            if(spoolName != null) {
                str = (msg instanceof String)?(String)msg:msg.toString();
                str = str.replaceAll("\r\n", "\n").replaceAll("\n", "\r\n");
                if(newline) {
                    bw.write(str);
                    bw.newLine();
                } else {
                    bw.write(str);
                }
            }
        } catch(Exception e) {}
    }

    
    /**
     * 스풀없이 화면 로그만 기록한다.
     * @param msg 로그 메시지
     * @param newline newline 문자를 추가하려면 true
     */
    public static void logWithoutSpool(Object msg, boolean newline) {
        try {
            if(newline) {
                System.out.println(msg);
            } else {
                System.out.print(msg);
            }
        } catch(Exception e) {}
    }

}