/*
 * File    : ConfigurationChecker.java
 * Created : 8 oct. 2003 23:04:14
 * By      : Olivier 
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
 
package org.gudy.azureus2.core3.config.impl;


import java.util.HashMap;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.security.*;

/**
 * 
 * The purpose of this class is to provide a way of checking that the config file
 * contains valid values when azureus is started.
 * 
 * 
 * @author Olivier
 * 
 */
public class ConfigurationChecker {
  
  private static boolean checked = false;
  public static boolean changed = false;
  
 
  public static void
  setSystemProperties()
  {
  	String	handlers = System.getProperty( "java.protocol.handler.pkgs" );
  	
  	if ( handlers == null ){
  		
  		handlers = "org.gudy.azureus2.core3.util.protocol";
  	}else{
  		
  		handlers += "|org.gudy.azureus2.core3.util.protocol";
  	}
  	
  	System.setProperty( "java.protocol.handler.pkgs", handlers );
  	 	
  		// DNS cache timeouts
  	
  	System.setProperty("sun.net.inetaddr.ttl", "60");
  	System.setProperty("networkaddress.cache.ttl", "60");
  	
  		// socket connect/read timeouts
  	
  	System.setProperty("sun.net.client.defaultConnectTimeout", "120000");
  	System.setProperty("sun.net.client.defaultReadTimeout", "60000");
  
  	SESecurityManager.initialise();
  }
  
  public static synchronized void 
  checkConfiguration() {
   
    if(checked)
      return;
    checked = true;
    
    int maxUpSpeed = COConfigurationManager.getIntParameter("Max Upload Speed",0);
    if(maxUpSpeed > 0 && maxUpSpeed < 1024 * 5) {
      changed = true;
      COConfigurationManager.setParameter("Max Upload Speed", 5 * 1024);
    }
    
    int peersRatio = COConfigurationManager.getIntParameter("Stop Peers Ratio",0);
    if(peersRatio > 4) {
      COConfigurationManager.setParameter("Stop Peers Ratio", 4);
      changed = true;
    }
    
    int stopRatio = COConfigurationManager.getIntParameter("Stop Ratio",0);
    if(stopRatio < 0) {
       COConfigurationManager.setParameter("Stop Ratio", 0);
       changed = true;
    }
    
    String uniqueId = COConfigurationManager.getStringParameter("ID",null);
    if(uniqueId == null || uniqueId.length() != 20) {
      uniqueId = generatePeerId();      
      COConfigurationManager.setParameter("ID", uniqueId);
      changed = true;
    }
    
    /**
     * Patch to insure that this option is disabled
     */    
    boolean astf = COConfigurationManager.getBooleanParameter("Always Show Torrent Files",true);
    if(astf) {
      COConfigurationManager.setParameter("Always Show Torrent Files",false);
      changed = true;
    }    
    
    /**
     * Special Patch for OSX users, do not play sound when done
     */
    if(System.getProperty("os.name").equals("Mac OS X")) {
      boolean sound = COConfigurationManager.getBooleanParameter("Play Download Finished",true);
      if(sound) {
        COConfigurationManager.setParameter("Play Download Finished",false);
        changed = true;
      }
    }
    
    if(changed) {
      COConfigurationManager.save();
    }    
  }
  
  public static String generatePeerId() {
    String uniqueId = "";
    long currentTime = System.currentTimeMillis();
    for(int i = 0 ; i < currentTime % 1000 ; i++)
      Math.random();            
    //Allocate a 10 random chars ID
    String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    for(int i = 0 ; i < 20 ; i++) {
      int pos = (int) ( Math.random() * chars.length());
      uniqueId += chars.charAt(pos);
    }
    return uniqueId;
  }
  
  public static void main(String args[]) {
    Integer obj = new Integer(1);
    HashMap test = new HashMap();
    int collisions = 0;
    for(int i = 0 ; i < 1000000 ; i++) {
      String id = generatePeerId();
      if(test.containsKey(id)) {
        collisions++;
      } else {
        test.put(id,obj);
      }
      if(i%1000 == 0) {
        System.out.println(i + " : " + id + " : " + collisions);
      }
    }
    System.out.println("\n" + collisions);
  }
}
