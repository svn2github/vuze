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
 
package org.gudy.azureus2.core;

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
  
  public static synchronized void checkConfiguration() {
    if(checked)
      return;
    checked = true;
    ConfigurationManager cm = ConfigurationManager.getInstance();
    int maxUpSpeed = cm.getIntParameter("Max Upload Speed",0);
    if(maxUpSpeed > 0 && maxUpSpeed < 1024 * 5) {
      changed = true;
      cm.setParameter("Max Upload Speed", 5 * 1024);
    }
    
    if(changed) {
      cm.save();
    }    
  }
  
}
