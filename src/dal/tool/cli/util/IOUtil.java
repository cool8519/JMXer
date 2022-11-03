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
     * 로그를 기록한다. 마지막에 newline 문자를 추가한다.
     * @param level 로그 레벨
     * @param msg 로그 메시지
     */
    public static void logln(Level level, Object msg) {
        Logger.logln(level, msg);
    }


    /**
     * 로그를 기록한다.
     * @param level 로그 레벨
     * @param msg 로그 메시지
     */
    public static void log(Level level, Object msg) {
    	Logger.log(level, msg);
    }


    
    /**
     * 현재시간을 기본 시간형식(HH:mm:ss)의 문자열로 가져온다.
     * @return 현재시간 문자열
     */
    public static String getCurrentTime() {
        return ((SimpleDateFormat)defaultTimeFormat.clone()).format(new java.util.Date());
    }


    /**
     * 현재시간을 주어진 시간형식의 문자열로 가져온다.
     * @param format SimpleDateFormat에 전달될 시간형식 문자열 
     * @return 현재시간 문자열
     */
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
        StringBuffer buffer = new StringBuffer("");
        log(level, prompt);
        int c = ' ';
        try {
            do {
                c = System.in.read();
                buffer.append((char)c);
            } while(c != '\n');

            Logger.logOnlySpool(buffer.toString(), false);

            String ret = buffer.toString();
        	ret = (ret.endsWith("\n")) ? ret.substring(0, ret.length()-1) : ret;
        	ret = (ret.endsWith("\r")) ? ret.substring(0, ret.length()-1) : ret;
            return ret;
        } catch(IOException e) {
            return "";
        }
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


    /**
     * 출력할 문자열을 결과 형식으로 출력한다.
     * @param str 출력할 문자열
     * @param columnLength 컬럼 크기
     * @param blankChar 공백 문자(컬럼 크기가 문자열보다 클 경우, 공백 문자가 채워짐)
     * @param endStr 마지막에 추가될 문자열
     * @param leftPad 공백 문자를 왼쪽에 추가하려면 true
     */
    public static void printColumn(String str, int columnLength, String blankChar, String endStr, boolean leftPad) {
        log(Level.RESULT, getColumnString(str, columnLength, blankChar, endStr, leftPad));
    }


    /**
     * 화면을 clear한다.
     */
    public static void clearScreen() {
        for(int i = 0; i < SCREEN_LINE; i++) {
        	Logger.logWithoutSpool("", true);
        }
    }

}
