/*
 * Created on Apr 11, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 * 
 */
package org.gudy.azureus2.core3.util.test;

import org.gudy.azureus2.core3.util.Debug;


public class SystemClockSpeedup {
	public static void main(String[] args) {
		for (int i=0; i < 20; i++) {
      new tester().start();
		}
	}
    
   
    
  public static class tester extends Thread {
    public void run() {
        try {
            int count = 0;
            while (true) {
                System.currentTimeMillis();
                count++;
                if (count == 30000) {
                    count = 0;
                    Thread.sleep(100);
                }
            }
        } catch (Exception e) {
        	Debug.printStackTrace( e );
        }
    }
  }

    
    
    
    
}
