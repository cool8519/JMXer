package dal.tool.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 날짜/시간의 계산 및 문자열 생성을 쉽게 하기 위한 Util 클래스
 * @author 권영달
 *
 */
public class DateUtil {

	public static final String FORMAT_DATETIME_SEC = "yyyy.MM.dd/HH:mm:ss";
	public static final String FORMAT_DATETIME_MSEC = "yyyy.MM.dd/hh:mm:ss.SSS";
	public static final String FORMAT_DATE = "yyyy.MM.dd";
	public static final String FORMAT_TIME = "HH:mm:ss";

	/**
	 * 문자열을 형식패턴에 맞게 파싱한 Date 객체를 리턴한다. 
	 * @param format_str 날짜/시간을 표현하는 형식패턴 문자열
	 * @param dt 날짜/시간을 나타내는 문자열
	 * @return 파싱된 Date 객체
	 */
	public static Date stringToDate(String format_str, String dt) {
		if(format_str == null || dt == null) {
			return null;
		} else {
			try {
				SimpleDateFormat format = new SimpleDateFormat(format_str);
				return format.parse(dt);
			} catch(ParseException e) {
				return null;
			}
		}
	}
	
	/**
	 * Date 객체를 형식패턴에 맞게 변환한 문자열을 리턴한다. 
	 * @param format_str 날짜/시간을 표현하는 형식패턴 문자열
	 * @param dt Date 객체
	 * @return 날짜/시간을 나타내는 문자열
	 */
	public static String dateToString(String format_str, Date dt) {
		if(format_str == null || dt == null) {
			return null;
		} else {
			SimpleDateFormat format = new SimpleDateFormat(format_str);
			return format.format(dt);
		}
	}

	/**
	 * Date 객체를 형식패턴에 맞게 변환한 문자열을 리턴한다. 
	 * @param format_str 날짜/시간을 표현하는 DateFormat 객체
	 * @param dt Date 객체
	 * @return 날짜/시간을 나타내는 문자열
	 */
	public static String dateToString(DateFormat format, Date dt) {
		if(format == null || dt == null) {
			return null;
		} else {
			return format.format(dt);
		}
	}
	
	/**
	 * 주어진 날짜의 첫시간을 가져온다.  
	 * @param dt Date 객체
	 * @return Date 객체(주어진 날짜의 00:00:00.000)
	 */
	public static Date getFirstOfDay(Date dt) {
		Calendar c = Calendar.getInstance();
		c.setTime(dt);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		return c.getTime();
	}

	/**
	 * 주어진 날짜의 끝시간을 가져온다.  
	 * @param dt Date 객체
	 * @return Date 객체(주어진 날짜의 23:59:59.999)
	 */
	public static Date getLastOfDay(Date dt) {
		Calendar c = Calendar.getInstance();
		c.setTime(dt);
		c.set(Calendar.HOUR_OF_DAY, 23);
		c.set(Calendar.MINUTE, 59);
		c.set(Calendar.SECOND, 59);
		c.set(Calendar.MILLISECOND, 999);
		return c.getTime();
	}

	/**
	 * 주어진 날짜/시간에 특정 시간을 더하거나 뺀 시간을 가져온다.
	 * @param dt Date 객체(기준 시간)
	 * @param calendarUnit Calendar의 static field로 표현되는 시간 단위(예:시간=Calendar.HOUR, 초=Calendar.SECOND) 
	 * @param delta 더하거나 뺄 값(마이너스값 가능). calendarUnit에 따라 의미가 달라진다.
	 * @return 계산된 Date 객체
	 */
	public static Date addDateUnit(Date dt, int calendarUnit, int delta) {
		Calendar c = Calendar.getInstance();
		c.setTime(dt);
		c.add(calendarUnit, delta);
		return c.getTime();
	}

	/**
	 * 주어진 날짜/시간에 특정 시간을 더하거나 뺀 시간을 가져온다.
	 * @param cal Calendar 객체(기준 시간)
	 * @param calendarUnit Calendar의 static field로 표현되는 시간 단위(예:시간=Calendar.HOUR, 초=Calendar.SECOND) 
	 * @param delta 더하거나 뺄 값(마이너스값 가능). calendarUnit에 따라 의미가 달라진다.
	 * @return 계산된 Date 객체
	 */
	public static Calendar addCalendarUnit(Calendar cal, int calendarUnit, int delta) {
		Calendar c = Calendar.getInstance();
		c.setTime(cal.getTime());
		c.add(calendarUnit, delta);
		return c;
	}

}
