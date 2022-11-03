package dal.tool.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 문자열 조작을 쉽게 하기 위한 Util 클래스
 * @author 권영달
 *
 */
public class StringUtil {

	public static final String[] QUOTES = { "\"\"", "''", "``" };
	public static final String[] DQUOTES = { "\"\"" };
	public static final String[] BRACKETS = { "[]", "()", "{}", "<>" };
	public static final String[] QUOTES_AND_BRACKETS = { "\"\"", "''", "``", "[]", "()", "{}", "<>" };

	

    /**
     * 따옴표나 괄호가 포함된 문자열을 구분자로 잘라 리턴한다.<br/>
     * 지정된 따옴표나 괄호내에서는 구분자가 무시되어 하나의 토큰으로 간주된다.<br/>
     * 결과 문자에서 따옴표나 괄호문자는 제거되어 저장된다. 
     * @param s 문자열
     * @param fs 구분 문자
     * @param bindCharacters 따옴표 또는 괄호 문자열 배열. 각 배열의 요소는 대칭되는 따옴표 및 괄호 문자여야 한다. (예: "[]", "''" 등)
     * @return 결과 문자 목록
     */
    public static List<String> getTokenList(String s, char fs, String[] bindCharacters) {
    	return getTokenList(s, fs, bindCharacters, true);
    }
    
    /**
     * 따옴표나 괄호가 포함된 문자열을 구분자로 잘라 리턴한다.<br/>
     * 지정된 따옴표나 괄호내에서는 구분자가 무시되어 하나의 토큰으로 간주된다. 
     * @param s 문자열
     * @param fs 구분 문자
     * @param bindCharacters 따옴표 또는 괄호 문자열 배열. 각 배열의 요소는 대칭되는 따옴표 및 괄호 문자여야 한다. (예: "[]", "''" 등)
     * @param stripBindCharacters 결과 문자 목록에 따옴표나 괄호 문자열을 제거하여 저장하려면 true  
     * @return 결과 문자 목록
     */
    public static List<String> getTokenList(String s, char fs, String[] bindCharacters, boolean stripBindCharacters) {
        if(s == null)
            return null;
        if(bindCharacters == null)
        	bindCharacters = new String[0];
        int idx = 0;
        if(fs == Character.UNASSIGNED) {
        	fs = ' ';
        }
    	List<String> l = new ArrayList<String>();
    	int currIdx = 0;
    	int endIdx = s.length()-1;
    	String token;
    	String bindChar = null;
    	try {
	    	while(currIdx <= endIdx) {
    			for(; s.charAt(currIdx)==' '; currIdx++);        			
        		for(int i = 0; i < bindCharacters.length; i++) {
		    		if(s.charAt(currIdx) == bindCharacters[i].charAt(0) && s.indexOf(bindCharacters[i].charAt(1),currIdx+1) > currIdx) {
		    			bindChar = bindCharacters[i];
		    			break;
		    		}
		    		bindChar = null;
	    		}
        		if(bindChar != null) {
        			int tmpIdx = currIdx+1;
        			do {
        				idx = s.indexOf(bindChar.charAt(1),tmpIdx);
        				tmpIdx = idx+1;
        			} while(idx > -1 && s.charAt(idx-1)=='\\');
        			token = s.substring(currIdx+1,idx);
        			token = replaceSpecialWithBackslash(token);
        			if(!stripBindCharacters) {
        				token = bindChar.charAt(0) + token + bindChar.charAt(1);
        			}
        			idx++;
        			if(endIdx > idx) {
        				for(; endIdx>=idx && s.charAt(idx)!=fs; idx++) {
        					token += s.charAt(idx);
        				}
        				idx++;        				
            			for(; endIdx>=idx && s.charAt(idx)==' '; idx++);        			
        				currIdx = idx;
        			} else {
            			currIdx = idx+1;
        			}
        			bindChar = null;
        		} else {
    		    	idx = s.indexOf(fs, currIdx);
    		    	if(idx > -1) {
    		    		token = s.substring(currIdx, idx);
    		        	currIdx = idx+1;
    		        	if(currIdx > endIdx) {
    		        		l.add(token);
    		        		token = null;
    		        	}
    		    	} else {
    		    		token = s.substring(currIdx);
    		    		currIdx = endIdx+1; 
    		    	}
        		}
        		if(token != null) {
        			l.add(token);
        		}
	    	}
	    	return l;
    	} catch(Exception e) {
    		e.printStackTrace();
    		return null;
    	}
    }

    
    /**
     * 역슬래시와 함께 표현된 특수문자에서 역슬래시를 제거한다.
     * @param s 원본 문자열
     * @return 역슬래시가 제거된 문자열
     */
    public static String replaceSpecialWithBackslash(String s) {
    	String ret = s;
		ret = ret.replaceAll("\\\\\\\\", "\\\\");
		ret = ret.replaceAll("\\\\\"", "\"");
		ret = ret.replaceAll("\\\\'", "'");
		ret = ret.replaceAll("\\\\`", "`");
		return ret;		
    }
    
    
    /**
     * 따옴표나 괄호가 포함된 문자열을 구분자로 잘라 리턴한다.<br/>
     * 지정된 따옴표나 괄호내에서는 구분자가 무시되어 하나의 토큰으로 간주된다. 
     * @param s 문자열
     * @param fs 구분 문자
     * @param bindCharacters 따옴표 또는 괄호 문자열 배열. 각 배열의 요소는 대칭되는 따옴표 및 괄호 문자여야 한다. (예: "[]", "''" 등)
     * @return 결과 문자 배열
     */
	public static String[] getTokenArray(String s, char fs, String[] bindCharacters) {
		List<String> tokenList = getTokenList(s, fs, bindCharacters);
		if(tokenList == null) {
			return null;
		} else {
			return tokenList.toArray(new String[tokenList.size()]);
		}
	}

    
    /**
     * 문자열을 반복하여 붙여서 리턴한다.
     * @param str 반복할 문자열
     * @param repeatCnt 반복할 횟수
     * @return 결과 문자열
     */
    public static String getRepeatString(String str, int repeatCnt) {
		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < repeatCnt; i++)
			builder.append(str);
		return builder.toString();
    }
    
    
    /**
     * 주어진 문자열을 왼쪽/오른쪽/가운데로 정렬하여 리턴한다.
     * @param alignStr 정렬 방식(가능한 값:"LEFT" 또는 "RIGHT" 또는 "CENTER")
     * @param length 리턴될 전체 문자열 길이. 이 값이 주어진 문자열 보다 클 경우는 공백으로 채워진다.
     * @param splitIfOver 주어진 문자열이 length보다 클 때, 문자열을 잘라서 리턴하려면 true. 문자열 전체를 리턴하려면 false. 
     * @param str 정렬할 문자열
     * @return 정렬된 문자열
     */
    public static String getAlignedString(String alignStr, int length, boolean splitIfOver, String str) {
		if(length <= str.length()) {
			if(splitIfOver) {
				return str.substring(0, length);
			} else {
				return str;
			}			
		}
		StringBuffer buffer = new StringBuffer();
		int blankLength = length - str.length();
    	alignStr = alignStr.toUpperCase().trim();
    	if(alignStr.equals("L") || alignStr.equals("LEFT")) {
			buffer.append(str);
			for(int i = 0; i < blankLength; i++) {
				buffer.append(' ');
			}
    	} else if(alignStr.equals("R") || alignStr.equals("RIGHT")) {
			for(int i = 0; i < blankLength; i++) {
				buffer.append(' ');
			}
			buffer.append(str);
    	} else if(alignStr.equals("C") || alignStr.equals("CENTER") || alignStr.equals("CENTRE")) {
			int lBlankLength = (blankLength%2 == 0) ? blankLength/2 : blankLength/2+1;
			int rBlankLength = (blankLength%2 == 0) ? blankLength/2 : blankLength/2;			
    		for(int i = 0; i < lBlankLength; i++) {
				buffer.append(' ');
			}
    		buffer.append(str);
    		for(int i = 0; i < rBlankLength; i++) {
				buffer.append(' ');
			}
    	}
		return buffer.toString();
    }

    
	/**
	 * 문자열 배열을 구분자를 포함하는 문자열로 리턴한다.
	 * @param arr 문자열 배열
	 * @param separator 구분자 문자열
	 * @return 구분자를 포함하는 문자열. ex) separator="|"이면, "a|b|c|d"
	 */
	public static String arrayToString(String[] arr, String separator) {
		if(arr == null || arr.length == 0) {
			return "";
		}
		StringBuffer sb = new StringBuffer("");
		for(int i = 0; i < arr.length; i++) {
			sb.append(arr[i]);
			if(i+1 < arr.length) {
				sb.append(separator);
			}
		}
		return sb.toString();
	}

	/**
	 * 객체 배열을 구분자를 포함하는 문자열로 리턴한다.
	 * @param arr 객체 배열
	 * @param separator 구분자 문자열
	 * @return 구분자를 포함하는 문자열. ex) separator="|"이면, "a|b|c|d"
	 */
	public static String arrayToString(Object[] arr, String separator) {
		if(arr == null || arr.length == 0) {
			return "";
		}
		StringBuffer sb = new StringBuffer("");
		for(int i = 0; i < arr.length; i++) {
			sb.append(arr[i]);
			if(i+1 < arr.length) {
				sb.append(separator);
			}
		}
		return sb.toString();
	}

	
	/**
	 * 문자열 목록 중에서 빈 문자열을 찾아 제거한다.
	 * @param lst 문자열 목록
	 */
	public static void removeEmptyString(List<String> lst) {
		if(lst == null) {
			return;
		}
		for(int i = lst.size()-1; i > -1; i--) {
			String s = lst.get(i);
			if(s.trim().equals("")) {
				lst.remove(i);
			}
		}
	}

    public static String NVL(String str, String def) {
    	return str != null ? str : def;
    }

    
    public static String expressSecondsAsTime(long seconds) {
    	if(seconds < 0) {
    		return "00:00:00";
    	}
    	long year = 0;
    	long day = 0;
    	long hour = 0;
    	long min = 0;
    	long sec = seconds;
		if(sec / 60 >= 1) {
			min = sec / 60;
			sec %= 60;
			if(min / 60 >= 1) {
				hour = min / 60;
				min %= 60;
				if(hour / 24 >= 1) {
					day = min / 24;
					hour %= 24;
					if(day / 365 >= 1) {
						year = day / 365;
						day %= 365;
					}
				}
            }
		}
		String result = getDigitNumberWithZero((int)hour,2) + ":" + getDigitNumberWithZero((int)min,2) + ":" + getDigitNumberWithZero((int)sec,2);
		if(day > 0) {
			result = day + ((day==1)?"day":"days") + " " + result;
		}
		if(year > 0) {
			result = year + ((year==1)?"year":"years") + " " + result;
		}
		return result;
    }

    public static String getDigitNumberWithZero(int num, int place) {
    	String result = String.valueOf(num);
    	int base = 10;
    	for(int i = 1; i < place; i++, base*=10) {
    		if(num < base) {
    			result = "0" + result;
    		}
    	}
    	return result;
    }
    
	/**
	 * 문자열이 패턴과 일치하는지 여부를 리턴한다.
	 * @param str 대상 문자열
	 * @param pattern 패턴 문자열 ('*'와 '?'만 사용 가능)
	 * @return 문자열이 패턴과 일치하면 true
	 */
	public static boolean isMatchStringWithPattern(String str, String pattern) {
		String regex_pattern = pattern;
		if(regex_pattern != null) {
			regex_pattern = regex_pattern.replaceAll("\\.", "\\\\.").replaceAll("\\*", ".*").replaceAll("\\?", ".");
		}
		if(str != null && str.matches(regex_pattern)) {
			return true;
		}
		return false;
	}

	/**
	 * 문자열내에 특정 문자가 포함된 횟수를 리턴한다.
	 * @param str 대상 문자열
	 * @param c 찾을 문자
	 * @return 문자 포함 횟수. s가 null이면 -1 리턴
	 */
	public static int countCharacters(String s, char c) {
		if(s == null) return -1;
		int cnt = 0;
		for(int i = 0; i < s.length(); i++) {
			if(s.charAt(i) == c) cnt++;
		}
		return cnt;
	}

	/**
	 * 문자열이 따옴표로 둘러쌓여 있는 경우 이를 제거하여 리턴한다.
	 * @param str 대상 문자열
	 * @param quoteChars 찾을 따옴표 문자 배열
	 * @param trim 수행 전후에 앞뒤 공백을 제거하려면 true
	 * @return 제거된 문자열
	 */
	public static String stripQuote(String s, char[] quoteChars, boolean trim) {
		if(s == null) return null;
		if(trim) s = s.trim();
		for(char quoteChar : quoteChars) {
			if(s.startsWith(""+quoteChar) && s.endsWith(""+quoteChar)) {
				return s.substring(1, s.length()-1);
			}
		}
		return trim ? s.trim() : s;
	}
}
