/*
 * File    : Plugin.java
 * Created : 24 oct. 2003 12:26:35
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
 
package org.gudy.core3.plugins;

import org.gudy.core3.plugins.files.data.PiecePickingData;

/**
 * This Interface is the base interface for all plugins
 * Any plugin should implement this interface.
 * 
 * @author Olivier
 * 
 */
public interface Plugin {
    
  /**
   * @return the plugin name
   */
  public String getPluginName();
  
  /**
   * Initialisation is done when plugin is loaded
   * @param pluginContext The context in which this plugin is going to work
   * @param pluginInterface The command interface
   */
  public void initPlugin(PluginContext pluginContext,PluginInterface pluginInterface);
  
  
  /**
   * Called when plugin is started. manually, or when azureus starts.  
   */  
  public void startPlugin();
  
  
  /**
   * Called when azureus plugin is stopped : manually, or when azureus quits.  
   */
  public void stopPlugin();
  
  
  //Callbacks
  
  /**
   * The CallBack for Plugins requesting a timer trigger
   */
  public void onTimerTrigger();
  
  
  /**
   * The CallBack for Plugins requesting a Tab into the Config panel
   * This method must use the PluginConfigContructorInterface
   * to contruct its config panel.
   */
  public void constructConfigPanel();
  
  
  /**
   * The CallBack for Plugins requesting to personalize piece picking.
   * @author Olivier
   */
  public void changePiecePickingOrder(PiecePickingData data);
}
