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
import java.io.File;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.security.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.logging.LGLogger;

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
  
  private static boolean system_properties_set	= false;
  private static boolean checked 				= false;
  private static boolean changed 				= false;
  
 
  public static synchronized void
  setSystemProperties()
  {
  	if ( system_properties_set ){
  		
  		return;
  	}
  	
  	system_properties_set	= true;
    
    migrateOldConfigFiles();
  	
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
    
    // proxy
    if ( COConfigurationManager.getBooleanParameter("Enable.Proxy", false) ) {
      String host = COConfigurationManager.getStringParameter("Proxy.Host");
      String port = COConfigurationManager.getStringParameter("Proxy.Port");
      String user = COConfigurationManager.getStringParameter("Proxy.Username");
      String pass = COConfigurationManager.getStringParameter("Proxy.Password");

      if ( COConfigurationManager.getBooleanParameter("Enable.SOCKS", true) ) {
        System.setProperty("socksProxyHost", host);
        System.setProperty("socksProxyPort", port);
        
        if (user.length() > 0) {
          System.setProperty("java.net.socks.username", user);
          System.setProperty("java.net.socks.password", pass);
        }
      }
      else {
        System.setProperty("http.proxyHost", host);
        System.setProperty("http.proxyPort", port);
        System.setProperty("https.proxyHost", host);
        System.setProperty("https.proxyPort", port);
        
        if (user.length() > 0) {
          System.setProperty("http.proxyUser", user);
          System.setProperty("http.proxyPassword", pass);
        }
      }
    }
  
  	SESecurityManager.initialise();
  }
  
  public static synchronized void 
  checkConfiguration() {
   
    if(checked)
      return;
    checked = true;
    
    int nbMinSeeds = COConfigurationManager.getIntParameter("StartStopManager_iIgnoreSeedCount", -1);
    if (nbMinSeeds == -1) {
    COConfigurationManager.setParameter("StartStopManager_iIgnoreSeedCount", 0);
      // not set yet.. import from "Start Num Peers"
    int nbOldMinSeeds = COConfigurationManager.getIntParameter("Start Num Peers", -1);
    if (nbOldMinSeeds != -1)
      COConfigurationManager.setParameter("StartStopManager_iIgnoreSeedCount", nbOldMinSeeds);
    changed = true;
    }

    //migrate from older BPs setting to newer KBs setting
    int speed = COConfigurationManager.getIntParameter("Max Upload Speed", -1);
    if ( speed > -1 ) {      
      COConfigurationManager.setParameter("Max Upload Speed KBs", speed / 1024);
      COConfigurationManager.setParameter("Max Upload Speed", -1);
      changed = true;
    }
    
    
    //migrate to new dual connection limit option
    int maxclients = COConfigurationManager.getIntParameter("Max Clients", -1);
    if ( maxclients > -1 ) {      
      COConfigurationManager.setParameter("Max.Peer.Connections.Per.Torrent", maxclients);
      COConfigurationManager.setParameter("Max Clients", -1);
      changed = true;
    }
    
    
    int maxUpSpeed = COConfigurationManager.getIntParameter("Max Upload Speed KBs",0);
    if(maxUpSpeed > 0 && maxUpSpeed < COConfigurationManager.CONFIG_MIN_MAX_UPLOAD_SPEED) {
      changed = true;
      COConfigurationManager.setParameter("Max Upload Speed KBs", COConfigurationManager.CONFIG_MIN_MAX_UPLOAD_SPEED);
    }
    

    int peersRatio = COConfigurationManager.getIntParameter("Stop Peers Ratio",0);
    if(peersRatio > 14) {
      COConfigurationManager.setParameter("Stop Peers Ratio", 14);
      changed = true;
    }
    
    int minQueueingShareRatio = COConfigurationManager.getIntParameter("StartStopManager_iFirstPriority_ShareRatio");
    if (minQueueingShareRatio < 500) {
      COConfigurationManager.setParameter("StartStopManager_iFirstPriority_ShareRatio", 500);
      changed = true;
    }
    
    int iSeedingMin = COConfigurationManager.getIntParameter("StartStopManager_iFirstPriority_SeedingMinutes");
    if (iSeedingMin < 90 && iSeedingMin != 0) {
      COConfigurationManager.setParameter("StartStopManager_iFirstPriority_SeedingMinutes", 90);
      changed = true;
    }

    int iDLMin = COConfigurationManager.getIntParameter("StartStopManager_iFirstPriority_DLMinutes");
    if (iDLMin < 60*3 && iDLMin != 0) {
      COConfigurationManager.setParameter("StartStopManager_iFirstPriority_DLMinutes", 60*3);
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
     * Special Patch for OSX users
     */
    if (System.getProperty("os.name").equals("Mac OS X")) {
      boolean sound = COConfigurationManager.getBooleanParameter("Play Download Finished",true);
      boolean close = COConfigurationManager.getBooleanParameter("Close To Tray",true);
      boolean min = COConfigurationManager.getBooleanParameter("Minimize To Tray",true);
      
      if ( sound || close || min ) {
        COConfigurationManager.setParameter("Play Download Finished",false);
        COConfigurationManager.setParameter("Close To Tray",false);
        COConfigurationManager.setParameter("Minimize To Tray",false);
        changed = true;
      }
    }
    
    //if previous config did not use shared port, grab the port
    if (!COConfigurationManager.getBooleanParameter("Server.shared.port", true)) {
    	int lp = COConfigurationManager.getIntParameter("Low Port", 6881);
      COConfigurationManager.setParameter("TCP.Listen.Port", lp);
      COConfigurationManager.setParameter("Server.shared.port", true);
      changed = true;
    }
    
    
    if(changed) {
      COConfigurationManager.save();
    }    
  }
  
  public static String generatePeerId() {
    String uniqueId = "";
    long currentTime = SystemTime.getCurrentTime();
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
  
  
  /**
   * Migrates old user files/dirs from application dir to user dir
   */
  private static void migrateOldConfigFiles() {
    if ( COConfigurationManager.getBooleanParameter("Already_Migrated", false)) {
      return;
    }
    
    String successes = "";
    String failures = "";
    
    String[] fileNames = { "categories.config", "azureus.config",
        "downloads.config", "filters.config", ".certs", ".keystore",
        "azureus.statistics", "tracker.log", "tracker.config",
        "trackers.config", "sharing.config", "plugins", "shares", "web" };
    
    //migrate files/folders
    for (int i=0; i < fileNames.length; i++) {
      try {
        File oldFile = FileUtil.getApplicationFile( fileNames[i] );
        if ( oldFile.exists() ) {
          File newFile = FileUtil.getUserFile( fileNames[i] );
          boolean result = oldFile.renameTo(newFile);
          if (result) {
            successes += oldFile.toURI().getPath() + "\n---> " + newFile.toURI().getPath() + " : OK\n";
          }
          else {
            failures += oldFile.toURI().getPath() + "\n---> " + newFile.toURI().getPath() + " : FAILED\n\n";
          }
        }
      } catch (Throwable t) {
        failures += fileNames[i] + "\n---> " + t.getMessage() + ": FAILED\n\n";
        t.printStackTrace();
        LGLogger.log(t);
      }
    }
    
    //migrate from old /.azureus/ dir
    String oldLinuxAndWebStartPath = System.getProperty("user.home") + SystemProperties.SEP + ".azureus" + SystemProperties.SEP;
    File oldLinuxAndWebStartDir = new File( oldLinuxAndWebStartPath );
    if ( oldLinuxAndWebStartDir.exists() ) {
      File newDir = new File( SystemProperties.getUserPath());
      boolean result = oldLinuxAndWebStartDir.renameTo(newDir);
      if (result) {
        successes += oldLinuxAndWebStartDir.toURI().getPath() + "\n---> " + newDir.toURI().getPath() + " : OK\n";
      }
      else {
        failures += oldLinuxAndWebStartDir.toURI().getPath() + "\n---> " + newDir.toURI().getPath() + " : FAILED\n\n";
      }
    }
    
    ConfigurationManager.getInstance().load();
    COConfigurationManager.setParameter("Already_Migrated", true);
    
    if (successes.length() > 1 || failures.length() > 1) {
    	String[] params = { successes, failures };
    	LGLogger.logAlertUsingResource(LGLogger.INFORMATION, "AutoMigration.useralert", params);
    }
  }
  
  
  
}
