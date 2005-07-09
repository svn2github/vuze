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

/**
 * Utility class to retrieve current system time,
 * and catch clock backward time changes.
 */
public class SystemTime {
  
  public static final long TIME_GRANULARITY_MILLIS = 30;   //internal update time ms
  
  private static final SystemTime instance = new SystemTime();
  
  
  private final Thread updater;
  private volatile long stepped_time;
  private volatile long smoothed_time;  //we 'fake' sub-granularity by adding ticks each call
  private volatile long next_time;
  
  
  private SystemTime() {
    stepped_time = System.currentTimeMillis();

    updater = new Thread("SystemTime") {
      public void run() {
        while( true ) {
          stepped_time = System.currentTimeMillis();
          smoothed_time = stepped_time;
          next_time = stepped_time + TIME_GRANULARITY_MILLIS;  //theoretical time at next real update
          
          try{  Thread.sleep( TIME_GRANULARITY_MILLIS );  }catch(Exception e) {Debug.printStackTrace( e );}
        }
      }
    };
    updater.setDaemon( true );
    
    // we don't want this thread to lag much as it'll stuff up the upload/download rate mechanisms (for example)
    updater.setPriority(Thread.MAX_PRIORITY);
    
    updater.start();
  }

  
  /**
   * Get the current system time.
   * @return time like System.currentTimeMillis()
   */
  public static long getCurrentTime() {
    instance.smoothed_time++;  //fake tick

    if( instance.smoothed_time > instance.next_time ) {  //ensure we haven't ticked beyond next real step
      instance.smoothed_time = instance.next_time;
    }
    
  	return instance.smoothed_time;
  }

}
