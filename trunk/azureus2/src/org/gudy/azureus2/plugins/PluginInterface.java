/*
 * File    : PluginInterface.java
 * Created : 2 nov. 2003 18:48:47
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
 
package org.gudy.azureus2.plugins;

import java.util.Properties;

import org.gudy.azureus2.plugins.ui.Parameter;
import org.gudy.azureus2.plugins.ui.PluginConfigUIFactory;


/**
 * Defines the communication interface between Azureus and Plugins
 * @author Olivier
 */
public interface PluginInterface {  
	
  /**
   * A Plugin might call this method to add a View to Azureus's views
   * The View will be accessible from View > Plugins > View name
   * @param view The PluginView to be added
   */
  public void addView(PluginView view);
  
  /**
   * TODO: write this
   * @param fileName
   */
  public void addConfigUIParameters(Parameter parameters[], String displayName);
  
  /**
   * A Plugin might ask Azureus to open a Torrent file
   * @param fileName The Name of the file that azureus must open
   */
  public void openTorrentFile(String fileName);
  
  /**
   * A Plugin might ask Azureus to open an URL pointing to a torrent
   * @param url The String representation of the url pointing to a torrent file
   */
  public void openTorrentURL(String url);
  
  /**
   * An access to the plugin properties
   * @return te properties from the file plugin.properties
   */
  public Properties getPluginProperties();
  
  /**
   * An access to the plugin installation directory
   * @return the full path the plugin is installed in
   */
  public String getPluginDirectoryName();
  
  /**
   * An access to the plugin Config
   * @return TODO: write this
   */
  public PluginConfig getPluginconfig();
  
  
  /**
   * TODO : write this
   * @return
   */
  public PluginConfigUIFactory getPluginConfigUIFactory();
}
