package dal.tool.cli.util;

import java.io.IOException;
import java.text.SimpleDateFormat;

import dal.tool.cli.Logger;
import dal.tool.cli.Logger.Level;


/**
 * Prompt Client에서 사용하는 로깅/화면/문자열 조작 관련  Utility 클래스
 * @author 권영달
 *
 */
public class IOUtil {

    private static final int SCREEN_LINE = 150;
    private static SimpleDateFormat defaultTimeFormat = new SimpleDateFormat("HH:mm:ss");

    /**
     * 스레드별 줄 입력 리더(TUI 키맵 경로). 설정되면 readLine이 System.in 대신 여기로 위임한다.
     * CLI 모드에서는 리더를 설치하지 않는다.
     */
    public interface LineReader {
        String readLine(String prompt, Level level);
    }

    private static final ThreadLocal<LineReader> tuiLineReader = new ThreadLocal<LineReader>();

    public static void setTuiLineReader(LineReader reader) {
        if(reader == null) {
            tuiLineReader.remove();
        } else {
            tuiLineReader.set(reader);
        }
    }

    public static void clearTuiLineReader() {
        tuiLineReader.remove();
    }


    public static void logln(Level level, Object msg) {
        Logger.logln(level, msg);
    }


    public static void log(Level level, Object msg) {
    	Logger.log(level, msg);
    }


    
    public static String getCurrentTime() {
        return ((SimpleDateFormat)defaultTimeFormat.clone()).format(new java.util.Date());
    }


    public static String getCurrentTime(String format) {
        return (new SimpleDateFormat(format)).format(new java.util.Date());
    }

    /**
     * 프롬프트를 화면과 스풀에 출력하고 개행문자(\n)가 올때까지 문자를 입력받아 전달한다.
     * @param prompt 출력할 프롬프트 문자열
     * @param level 로그 레벨
     * @return 입력받은 문자열. 마지막 개행문자(\r\n)는 제거되어 리턴된다.
     */
    public static String readLine(String prompt, Level level) {
        LineReader reader = tuiLineReader.get();
        if(reader != null) {
            return reader.readLine(prompt, level);
        }
        return readLineFromSystemIn(prompt, level);
    }

    private static String readLineFromSystemIn(String prompt, Level level) {
        StringBuffer buffer = new StringBuffer("");
        beginTuiPanelCaptureReadLinePrompt(prompt, level);
        int c = ' ';
        try {
            do {
                c = System.in.read();
                buffer.append((char)c);
            } while(c != '\n');

            String ret = buffer.toString();
        	ret = (ret.endsWith("\n")) ? ret.substring(0, ret.length()-1) : ret;
        	ret = (ret.endsWith("\r")) ? ret.substring(0, ret.length()-1) : ret;
            if(Logger.isTuiActive() && Logger.isTuiPanelCapture()) {
                Logger.logOnlySpool(ret + "\n", false);
            } else {
                Logger.logOnlySpool(buffer.toString(), false);
            }
            return ret;
        } catch(IOException e) {
            return "";
        }
    }

    /**
     * TUI Output 캡처용 readLine 프롬프트: 프롬프트 줄을 출력한 뒤, 인라인 진행(녹화 점 등) 전에 빈 행을 확보한다.
     */
    public static void beginTuiPanelCaptureReadLinePrompt(String prompt, Level level) {
        log(level, prompt);
        if(Logger.isTuiActive() && Logger.isTuiPanelCapture()) {
            Logger.log("", true);
            if(isStopRecordingWaitPrompt(prompt)) {
                Logger.logBlankLine();
            }
        }
    }

    private static boolean isStopRecordingWaitPrompt(String prompt) {
        if(prompt == null) {
            return false;
        }
        String lower = prompt.toLowerCase();
        return lower.indexOf("press enter") >= 0 && lower.indexOf("stop") >= 0;
    }

    /**
     * 입력받은 문자열이 비교 문자열과 맞는지 확인한다.<br/>
     * 비교 문자열은 필수 문자열과 선택 문자열로 나누어 입력한다. 필수 문자열은 반드시 입력되어야 할 문자열이며, 선택 문자열은 필수 문자열 뒤에 붙을 수 있는 문자열이다.<br/>
     * 예) 필수 문자열이 "hi"이고 선택 문자열이 "story"이면, 임력받은 문자열이 "hi","his","histo","history"인 경우 모두 만족한다.
     * @param str 확인할 문자열
     * @param essential 필수 문자열
     * @param optional 선택 문자열
     * @return 비교 문자열에 만족할 경우 true
     */
    public static boolean isIncludeEquals(String str, String essential, String optional) {
        if(essential.getBytes().length > str.getBytes().length) {
            return false;
        }

        String first = str.substring(0, essential.length());
        if(!first.equals(essential)) {
            return false;
        }

        String second = str.substring(essential.length());
        if(second.length() < 1 || optional == null) {
            return true;
        }

        if(optional.getBytes().length < second.getBytes().length) {
            return false;
        }

        if(optional.startsWith(second)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * isIncludeEquals의 대소문자 무시 버전. 명령 키워드·약어 매칭용.
     * 사용자 데이터(ObjectName, 따옴표 쓰레드명, 경로)에는 사용하지 않는다.
     */
    public static boolean isIncludeEqualsIgnoreCase(String str, String essential, String optional) {
        if(str == null || essential == null) {
            return false;
        }
        if(optional != null) {
            return isIncludeEquals(str.toLowerCase(), essential.toLowerCase(), optional.toLowerCase());
        }
        return isIncludeEquals(str.toLowerCase(), essential.toLowerCase(), null);
    }


    /**
     * 출력할 문자열을 컬럼 형식의 문자열로 가져온다.
     * @param str 출력할 문자열
     * @param columnLength 컬럼 크기
     * @param blankChar 공백 문자(컬럼 크기가 문자열보다 클 경우, 공백 문자가 채워짐)
     * @param endStr 마지막에 추가될 문자열
     * @param leftPad 공백 문자를 왼쪽에 추가하려면 true
     */
    public static String getColumnString(String str, int columnLength, String blankChar, String endStr, boolean leftPad) {
        StringBuffer result = new StringBuffer("");
        str = (str==null)?"":str;
        if(leftPad) {
            result.append(str);
            for(int i = 0; i < (columnLength-str.getBytes().length); i++) {
                result.append(blankChar);
            }
        } else {
            for(int i = 0; i < (columnLength-str.getBytes().length); i++) {
                result.append(blankChar);
            }
            result.append(str);
        }
        if(endStr != null) {
            result.append(endStr);
        }
        return result.toString();
    }


    public static void printColumn(String str, int columnLength, String blankChar, String endStr, boolean leftPad) {
        log(Level.RESULT, getColumnString(str, columnLength, blankChar, endStr, leftPad));
    }


    public static void clearScreen() {
        Logger.clearScreenDisplay();
    }

}
