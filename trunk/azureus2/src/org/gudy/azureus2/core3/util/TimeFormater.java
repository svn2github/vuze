/*
 * Created on 27 juin 2003
 *
 */
package org.gudy.azureus2.core3.util;

/**
 * @author Olivier
 * 
 */
public class TimeFormater {

    public static String format(long time)
    {
      if (time > 864000) return Constants.INFINITY_STRING;
      
      int secs = (int) time % 60;
      int mins = (int) (time / 60) % 60;
      int hours = (int) (time /3600) % 24;
      int days = (int) (time / 86400);
      
      String result = "";
      if (days > 0) result = days + "d ";
      if (hours > 0 || days > 0) result += hours + "h ";
      if ((days == 0) && ((mins > 0 || hours > 0) && mins < 10)) result += "0" + mins + "m ";
      if ((days == 0) && ((mins > 0 || hours > 0) && mins >= 10)) result += mins + "m ";
      if ((hours == 0 && days == 0) && ((secs > 0 || mins > 0) && secs < 10)) result += "0" + secs + "s ";
      if ((hours == 0 && days == 0) && ((secs > 0 || mins > 0) && secs >= 10)) result += secs + "s ";
      
      return result;
    }
}
