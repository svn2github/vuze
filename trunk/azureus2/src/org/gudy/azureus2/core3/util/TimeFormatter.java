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
	 * Format time into two time sections, the first chunk trimmed, the second
	 * with always with 2 digits.  Sections are *d, **h, **m, **s.  Section
	 * will be skipped if 0.   
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
		
		String result = vals[end] + TIME_SUFFIXES[end];

		// skip until we have a non-zero time section
		do {
			end--;
		} while (end >= 0 && vals[end] == 0);
		
		if (end >= 0)
			result += " " + twoDigits(vals[end]) + TIME_SUFFIXES[end];

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
