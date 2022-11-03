package dal.tool.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Random;

public class NumberUtil {

	public static DecimalFormat DF_NO_DOT = new DecimalFormat("#");
	public static DecimalFormat DF_NO_DOT_THOUSAND = new DecimalFormat("#,###");
	public static DecimalFormat DF_ONLY_DOT3 = new DecimalFormat(".000");
	public static DecimalFormat DF_ONLY_DOT2 = new DecimalFormat(".00");
	public static DecimalFormat DF_ONLY_DOT1 = new DecimalFormat(".0");
	public static DecimalFormat DF_PERCENT_DOT3 = new DecimalFormat("##0.000");
	public static DecimalFormat DF_PERCENT_DOT2 = new DecimalFormat("##0.00");
	public static DecimalFormat DF_PERCENT_DOT1 = new DecimalFormat("##0.0");

	public static String numberToString(Object o) {
		return numberToString(o, DF_NO_DOT_THOUSAND);
	}

	public static String numberToString(Object o, NumberFormat format) {
		if(o == null || format == null) {
			return null;
		} else {
			return format.format(o);
		}
	}

	public static String numberToString(double d, NumberFormat format) {
		if(d == Double.NaN || format == null) {
			return null;
		} else {
			return format.format(d);
		}
	}

	public static String numberToString(long l, NumberFormat format) {
		if(format == null) {
			return null;
		} else {
			return format.format(l);
		}
	}

	public static String numberToString(Object o, String format_str) {
		if(o == null || format_str == null) {
			return null;
		} else {
			DecimalFormat format = new DecimalFormat(format_str);
			return format.format(o);
		}
	}

	public static String numberToString(double d, String format_str) {
		if(d == Double.NaN || format_str == null) {
			return null;
		} else {
			DecimalFormat format = new DecimalFormat(format_str);
			return format.format(d);
		}
	}

	public static String numberToString(long l, String format_str) {
		if(format_str == null) {
			return null;
		} else {
			DecimalFormat format = new DecimalFormat(format_str);
			return format.format(l);
		}
	}

	public static boolean isNumber(String str) {
		try {
			Integer.parseInt(str);
			return true;
		} catch(NumberFormatException e) {
		}
		return false;
	}

	public static int getRandomNumber(int bound) {
		if(bound < 0)
			return -1;
		Random rand = new Random();
		return rand.nextInt(bound);
	}

	public static String getTwoDigitNumber(int num) {
		return ((num < 10) ? "0" : "") + String.valueOf(num);
	}

}
