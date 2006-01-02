/*
 * Created on 27 juin 2003
 *
 */
package org.gudy.azureus2.core3.util;

/**
 * @author Olivier
 * 
 */
public class TimeFormatter {
  // XXX should be i18n'd
	static final String[] TIME_SUFFIXES = { "s", "m", "h", "d" };

	/**
	 * Format time in 0d 00h 00m 00s format, with all units displaying two digits,
	 * except the first
	 * 
	 * @param time time in ms
	 * @return Formatted time string
	 */
	public static String format(long time) {
		if (time >= Constants.INFINITY_AS_INT)
			return Constants.INFINITY_STRING;

		if (time < 0)
			return "";

		// secs, mins, hours, days
		int[] vals = { (int) time % 60, (int) (time / 60) % 60,
				(int) (time / 3600) % 24, (int) (time / 86400) };

		int end = vals.length - 1;
		while (vals[end] == 0 && end > 0) {
			end--;
		}
		
		// First one is bare, the rest get two digits
		String result = vals[end] + TIME_SUFFIXES[end];

		for (int i = end - 1; i >= 0; i--)
			result += " " + twoDigits(vals[i]) + TIME_SUFFIXES[i];

		return result;
	}

    public static String formatColon(long time)
    {
      if (time >= Constants.INFINITY_AS_INT) return Constants.INFINITY_STRING;
      if (time < 0) return "";

      int secs = (int) time % 60;
      int mins = (int) (time / 60) % 60;
      int hours = (int) (time /3600) % 24;
      int days = (int) (time / 86400);
      
      String result = "";
      if (days > 0) result = days + "d ";
      result += twoDigits(hours) + ":" + twoDigits(mins) + ":" + twoDigits(secs);

      return result;
    }
    
    private static String twoDigits(int i) {
      return (i < 10) ? "0" + i : String.valueOf(i);
    }
}
