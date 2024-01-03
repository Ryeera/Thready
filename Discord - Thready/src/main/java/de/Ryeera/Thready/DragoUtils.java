package de.Ryeera.Thready;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DragoUtils {
	
	public static String formatTime() {
		return formatTime(System.currentTimeMillis());
	}
	
	public static String formatTime(long millisecs) {
		return formatTime(millisecs, "dd.MM.yy HH:mm:ss");
	}
	
	public static String formatTime(long millisecs, String format) {
		return new SimpleDateFormat(format).format(new Date(millisecs));
	}
	
}
