package dal.tool.util;

import java.io.File;
import java.net.URL;

/**
 * 클래스 사용을 쉽게 하기 위한 Util 클래스
 * @author 권영달
 *
 */
public class ClassUtil {


	/**
	 * 클래스 파일의 실제 파일 경로를 가져온다. 클래스 파일이 jar 내에 포함된 경우는 jar 파일 경로를 가져온다.
	 * @param className 클래스명(구분자:"/")
	 * @param fromClass 클래스 파일을 확인할 클래스로더에 존재하는 클래스. null인 경우, ClassUtil이 로딩된 클래스로더에서 확인한다.
	 * @return 파일 경로 문자열. 찾지 못한 경우는 null.
	 */
	public static String getLoadedFilePath(String className, Class<?> fromClass) {
		className = ((className.startsWith("/"))?"":"/") + className + ((className.endsWith(".class"))?"":".class");	
		URL resUrl = (fromClass==null) ? ClassUtil.class.getResource(className) : fromClass.getResource(className);
		if(resUrl == null) {
			return null;
		}
        String s = resUrl.getFile();
        s = (s.startsWith("/file:")) ? s.substring(1) : s;
        if(s.startsWith("file://localhost")) {
            s = s.substring(16);
            s = (!File.separator.equals("/")) ? s.substring(1) : s;
        } else if(s.startsWith("file:")) {
            s = s.substring(5);
            s = (!File.separator.equals("/")) ? s.substring(1) : s;
        }
        int idx = s.indexOf("!/");
        if(idx > -1) {
            s = s.substring(0, idx);
        }
        return s;
	}

}
