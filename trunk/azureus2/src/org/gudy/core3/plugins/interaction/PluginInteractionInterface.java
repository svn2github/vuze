/*
 * File    : PluginInteractionInterface.java
 * Created : 25 oct. 2003 16:34:12
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
 
package org.gudy.core3.plugins.interaction;

import org.gudy.core3.plugins.Plugin;
import org.gudy.core3.plugins.interaction.ui.PluginConfigTab;
import org.gudy.core3.plugins.interaction.ui.PluginMenu;
import org.gudy.core3.plugins.interaction.ui.PluginView;
import org.gudy.core3.plugins.interaction.ui.PluginWizard;

/**
 * @author Olivier
 * 
 */
public interface PluginInteractionInterface {

  public static final int MENU_MY_TORRENTS = 1;
  public static final int MENU_FILE = 2; 
  public static final int MENU_PEERS = 3;
  
  //TODO PLUGINS : maybe change the minimum interval to a greater value ... use secs?
  /**
   * This method must be called by the plugin to set a callback on
   * a fixed interval.
   * @param plugin the plugin on which the callback is set
   * @param interval the interval in ms for the callback. Must be greater than 100ms
   * @return true if the callback is set
   */
  public boolean setTimedCallback(Plugin plugin,int interval);
  
  /**
   * This method must be called by the plugin to remove its callback.
   * All callbacks are automatically stopped when the plugin is stopped.
   * @param plugin
   * @return true is the callback was removed.
   */
  public boolean removeTimedCallback(Plugin plugin);
  
  
  /**
   * A plugin may request for its own view to be displayed in azureus
   * @param plugin the plugin adding the view
   * @param view the PluginView that the plugin wants to set
   * @return true if the view is accepted
   */
  public boolean setPluginView(Plugin plugin,PluginView view);
  
  /**
   * A plugin may request for its own tab to be displayed in azureus config
   * @param plugin the plugin adding the view
   * @param tab the PluginconfigTab to be displayed
   * @return true is the tab is accepted
   */
  public boolean setPluginConfigTab(Plugin plugin,PluginConfigTab tab);
  
  /**
   * A plugin may request for its own wizard to be added to the azureus wizards
   * @param plugin the plugin adding the view
   * @param wizard the Wizard to be added
   * @return true is the wizard is accepted
   */
  public boolean setPluginWizard(Plugin plugin,PluginWizard wizard);
  
  /**
   * A plugin may request for its own context menu to be added to some
   * of azureus menus.
   * @param destinationMenu the menu to which the pluginmenu will be added (MENU_MY_TORRENTS | MENU_FILE | MENU_PEERS)
   * @param plugin the plugin adding this menu
   * @param menu the PluginMenu to be added
   * @return true is the menu is accepted
   */
  public boolean addContextMenu(int destinationMenu,Plugin plugin, PluginMenu menu);
}
