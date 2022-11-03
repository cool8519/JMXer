package dal.tool.util;


import java.io.File;
import java.util.StringTokenizer;

/**
 * 정규 표현식 문자열 생성 및 패턴 매칭여부 확인을 쉽게 하기 위한 Util 클래스
 * @author 권영달
 *
 */
public class PatternUtil {

	/**
	 * 패턴 타입
	 * <xmp>
	 *  - FILE_PATH : 파일 경로
	 *  - CLASS_PATH : 클래스 경로(형식:"Package/ClassName)
	 *  - CLASS_AND_METHOD : 클래스 경로 및 메소드명(형식:"Package/ClassName.MethodName")
	 *  - METHOD_ARGS : 메소드 Arguments 목록(형식:[예]"int,int,boolean[],java/lang/String")
	 *  - STRING : 일반 문자열
	 * </xmp>
	 */
	public static enum PATTERN_TYPE { FILE_PATH, CLASS_PATH, CLASS_AND_METHOD, METHOD_ARGS, STRING };
	

	/**
	 * Unix식 패턴("*","**","?","."포함) 문자열을 정규 표현식으로 치환한다.
	 * @param type 패턴 타입
	 * @param pattern 치환할 패턴 문자열(Unix식 표현)
	 * @return 정규 표현식 문자열
	 */
	public static String toRegexPatternString(PATTERN_TYPE type, String pattern) {
		try {
			StringBuffer buffer = new StringBuffer("");
			if(type == PATTERN_TYPE.FILE_PATH) {
				String token, regex_pattern;
				String fsep = File.separator;
				if(File.separator.equals("/")) {
					regex_pattern = pattern.replace("\\\\", "/");
				} else {
					if(pattern.startsWith("*") || pattern.startsWith("?")) {
						pattern = "?:\\" + pattern;
					}
					regex_pattern = pattern.replace("/", "\\\\");
				}
				if(!regex_pattern.startsWith(".") && !regex_pattern.startsWith(fsep) && !(fsep.equals("\\") && regex_pattern.charAt(1) == ':')) {
					regex_pattern = "." + fsep + regex_pattern;
				}
				StringTokenizer st = new StringTokenizer(regex_pattern, fsep);
				while(st.hasMoreTokens()) {
					token = st.nextToken();
					StringTokenizer st2 = new StringTokenizer(token, ".");
					StringBuffer buffer2 = new StringBuffer("");
					String token2;
					while(st2.hasMoreTokens()) {
						token2 = st2.nextToken();
						if(token2.indexOf("**") > -1) {
							token2 = token2.replaceAll("\\*{2}", ".*");
						} else {
							if(fsep.equals("/")) {
								token2 = token2.replaceAll("\\*", "[^/]*");
							} else {
								token2 = token2.replaceAll("\\*", "[^\\\\\\\\]*");
							}
						}
						token2 = token2.replaceAll("\\?", ".").replaceAll("\\$", "\\\\\\$");
						buffer2.append(token2);
						if(st2.hasMoreTokens()) {
							buffer2.append("\\.");
						}
					}
					buffer.append(buffer2);
					if(st.hasMoreTokens()) {
						if(fsep.equals("/")) {
							buffer.append("/");
						} else {
							buffer.append("\\\\");
						}
					}
				}
			} else if(type == PATTERN_TYPE.CLASS_PATH) {
				String token, cls;
				StringTokenizer st = new StringTokenizer(pattern, "/");
				while(st.hasMoreTokens()) {
					token = st.nextToken();
					cls = token;
					if(cls.indexOf("**") > -1) {
						cls = cls.replaceAll("\\*{2}", ".*");
					} else {
						cls = cls.replaceAll("\\*", "[^/]*");
					}
					cls = cls.replaceAll("\\?", ".").replaceAll("\\$", "\\\\\\$");
					if(st.hasMoreTokens()) {
						buffer.append(cls).append("/");
					} else {
						buffer.append(cls);
					}
				}
			} else if(type == PATTERN_TYPE.CLASS_AND_METHOD) {
				String token, regex_pattern, cls, mtd;
				StringTokenizer st = new StringTokenizer(pattern, "/");
				while(st.hasMoreTokens()) {
					token = st.nextToken();
					int idx = token.indexOf(".");
					if(idx < 0) {
						cls = token;
						mtd = null;
					} else {
						cls = token.substring(0, idx);
						mtd = token.substring(idx+1);
					}
					if(cls.indexOf("**") > -1) {
						cls = cls.replaceAll("\\*{2}", ".*");
					} else {
						cls = cls.replaceAll("\\*", "[^/]*");
					}
					cls = cls.replaceAll("\\?", ".").replaceAll("\\$", "\\\\\\$");
	
					if(mtd != null) {
						mtd = mtd.replaceAll("\\*", "[^/]*").replaceAll("\\?", ".");
					}
					
					regex_pattern = cls + ((mtd!=null)?("\\."+mtd):"");
					if(st.hasMoreTokens()) {
						if(mtd != null) {
							return null;
						} else {
							buffer.append(regex_pattern).append("/");
						}
					} else {
						buffer.append(regex_pattern);
					}
				}
			} else if(type == PATTERN_TYPE.METHOD_ARGS) {
				String token, arg;
				StringTokenizer st = new StringTokenizer(pattern, ",");
				while(st.hasMoreTokens()) {
					token = st.nextToken();
					arg = token;
					if(token.equals("**")) {
						arg = ".*";
					} else {
						arg = toRegexPatternString(PATTERN_TYPE.CLASS_PATH, arg);
					}
					if(st.hasMoreTokens()) {
						buffer.append(arg).append(",");
					} else {
						buffer.append(arg);
					}
				}
			} else if(type == PATTERN_TYPE.STRING) {
				String regex_pattern = pattern.replaceAll("\\.", "\\\\.").replaceAll("\\*", ".*").replaceAll("\\?", ".");
				buffer.append(regex_pattern);
			}
			return buffer.toString();
		} catch(Exception e) {
			return null;
		}
	}
	
	
	/**
	 * 주어진 문자열이 정규 표현식에 매칭되는지 확인한다.
	 * @param str 확인할 문자열
	 * @param regex_pattern 정규 표현식 문자열
	 * @return 매칭되면 true
	 */
	public static boolean isMatchPattern(String str, String regex_pattern) {
		if(regex_pattern == null) {
			return false;
		}
		if(str != null && str.matches(regex_pattern)) {
			return true;
		}
		return false;
	}

	/**
	 * 주어진 문자열이 패턴에 매칭되는지 확인한다.
	 * @param type 패턴 타입
	 * @param str 확인할 문자열
	 * @param pattern 치환할 패턴 문자열(Unix식 표현)
	 * @return 매칭되면 true
	 */
	public static boolean isMatchPattern(PATTERN_TYPE type, String str, String pattern) {
		String regex_pattern = toRegexPatternString(type, pattern);
		return isMatchPattern(str, regex_pattern);
	}

}
