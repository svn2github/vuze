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


import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.util.*;

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
  
  	// SSL client defaults
  	
  private static String SSL_CERTS 		= ".certs";
  private static String SSL_PASSWORD 	= "changeit";

  public static synchronized void checkConfiguration() {
    
    System.setProperty("sun.net.inetaddr.ttl", "60");
    System.setProperty("networkaddress.cache.ttl", "60");
    System.setProperty("sun.net.client.defaultConnectTimeout", "120000");
    System.setProperty("sun.net.client.defaultReadTimeout", "60000");

		// keytool -genkey -keystore %home%\.keystore -keypass changeit -storepass changeit -keyalg rsa -alias azureus

		// keytool -export -keystore %home%\.keystore -keypass changeit -storepass changeit -alias azureus -file azureus.cer

		// keytool -import -keystore %home%\.certs -alias azureus -file azureus.cer			
	
		// debug SSL with -Djavax.net.debug=ssl
		
	System.setProperty( "javax.net.ssl.trustStore", FileUtil.getApplicationFile(SSL_CERTS).getAbsolutePath());
			
	System.setProperty( "javax.net.ssl.trustStorePassword", SSL_PASSWORD );

    
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
      uniqueId = "";
      long currentTime = System.currentTimeMillis();
      for(int i = 0 ; i < currentTime % 1000 ; i++)
        Math.random();            
      //Allocate a 10 random chars ID
      String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
      for(int i = 0 ; i < 20 ; i++) {
        int pos = (int) ( Math.random() * chars.length());
        uniqueId += chars.charAt(pos);
      }
      COConfigurationManager.setParameter("ID", uniqueId);
      changed = true;
    }
    
    if(changed) {
      COConfigurationManager.save();
    }    
  }
}
