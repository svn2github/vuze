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
      int secs = (int) time % 60;
      int mins = (int) (time / 60) % 60;
      int hours = (int) (time /3600) % 24;
      int days = (int) (time / 86400);
      
      String result = "";
      if(days > 0) result = days + "d ";
      if(hours > 0 || days > 0) result += hours + "h ";
      if((mins > 0 || hours > 0 || days > 0) && mins < 10) result += "0" + mins + "' ";
      if((mins > 0 || hours > 0 || days > 0) && mins >= 10) result += mins + "' ";
      if((secs > 0 || mins > 0 || hours > 0 || days > 0) && secs < 10) result += "0" + secs + "\" ";
      if((secs > 0 || mins > 0 || hours > 0 || days > 0) && secs >= 10) result += secs + "\" ";
      
      return result;
    }
}
