/*
 * Created on Apr 16, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.core3.util;

import java.util.Arrays;

import org.gudy.azureus2.core3.logging.LGLogger;


/**
 * Utility class to retrieve current system time,
 * and catch clock backward time changes.
 */
public class SystemTime {
  
  private static final long GRANULARITY = 25;   //internal update time ms
  
  private static final SystemTime instance = new SystemTime();
  private final Thread updater;
  private volatile long currentTime = 0;
  private long prevTime = 0;
  private long errorStartTime = 0;
  private final long errorTimes[] = { 1000, 5000, 10000, 30000, 60000, 300000, 600000 };
  private volatile boolean errorStates[] = new boolean[errorTimes.length];
  
  
  private SystemTime() {
    Arrays.fill( errorStates, false );
    
    updater = new Thread("SystemTime") {
      public void run() {
        while( true ) {
          currentTime = System.currentTimeMillis();
          
          if ( currentTime < prevTime ) {   //oops, time went backwards!
            LGLogger.log(LGLogger.INFORMATION, "SystemTime: caught clock time set backwards "+(prevTime-currentTime)+ " ms");
            errorStartTime = currentTime;
            for (int i=0; i < errorStates.length; i++) errorStates[i] = true;
          }
          prevTime = currentTime;
          
          if ( errorStartTime != 0 ) {  //update the errorStates values
            for (int i=0; i < errorTimes.length; i++) {
              if ( errorStates[i] == true && currentTime - errorStartTime > errorTimes[i] ) {
                errorStates[i] = false;
                if ( i == errorTimes.length - 1 ) errorStartTime = 0;
              }
            }
          }
          
          try{  Thread.sleep( GRANULARITY );  }catch(Exception e) {e.printStackTrace();}
        }
      }
    };
    updater.setDaemon( true );
    updater.start();
  }

  
  /**
   * Get the current system time.
   * @return time like System.currentTimeMillis()
   */
  public static long getCurrentTime() {
  	return instance.currentTime;
  }
  
  /**
   * Check if there has been a time error (time went backwards)
   * within the last 1 second.
   * @return true if there has been an error, false if everything is normal
   */
  public static boolean isErrorLast1sec() {
    return instance.errorStates[0];
  }
  
  /**
   * Check if there has been a time error (time went backwards)
   * within the last 5 seconds.
   * @return true if there has been an error, false if everything is normal
   */
  public static boolean isErrorLast5sec() {
    return instance.errorStates[1];
  }
  
  /**
   * Check if there has been a time error (time went backwards)
   * within the last 10 seconds.
   * @return true if there has been an error, false if everything is normal
   */
  public static boolean isErrorLast10sec() {
    return instance.errorStates[2];
  }
  
  /**
   * Check if there has been a time error (time went backwards)
   * within the last 30 seconds.
   * @return true if there has been an error, false if everything is normal
   */
  public static boolean isErrorLast30sec() {
    return instance.errorStates[3];
  }
  
  /**
   * Check if there has been a time error (time went backwards)
   * within the last 1 minute.
   * @return true if there has been an error, false if everything is normal
   */
  public static boolean isErrorLast1min() {
    return instance.errorStates[4];
  }
  
  /**
   * Check if there has been a time error (time went backwards)
   * within the last 5 minutes.
   * @return true if there has been an error, false if everything is normal
   */
  public static boolean isErrorLast5min() {
    return instance.errorStates[5];
  }
  
  /**
   * Check if there has been a time error (time went backwards)
   * within the last 10 minutes.
   * @return true if there has been an error, false if everything is normal
   */
  public static boolean isErrorLast10min() {
    return instance.errorStates[6];
  }
  
  
}
