/**
 * File: PluginState.java
 * Date: 19 Aug 2008
 * Author: Allan Crooks
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the COPYING file ).
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.gudy.azureus2.plugins;

/**
 * This object provides information the current state of the plugin, and
 * provides various mechanisms to query and control plugins and their integration
 * with Azureus at a low-level.
 * 
 * Most plugins will have no need to interact with this object - it is primarily
 * used by Azureus for plugin management.
 * 
 * @since 3.1.1.1
 */
public interface PluginState {

	  /**
	   * Returns <tt>true</tt> if the plugin is set to load at startup, <tt>false</tt> otherwise.
	   */
	  public boolean isLoadedAtStartup();
	  
	  /**
	   * Sets whether the plugin is loaded at startup or not.
	   */
	  public void setLoadedAtStartup(boolean load_at_startup);
	  
	  /**
	   * Returns <tt>true</tt> if there was a problem loading or initialising the plugin. 
	   */
	  public boolean hasFailed();
	  
	  /**
	   * Returns <tt>true</tt> if the plugin has been marked as disabled, and prevented
	   * from initialising.
	   */
	  public boolean
	  isDisabled();
	  
	  /**
	   * Sets whether the plugin can be loaded or not. If you are trying to affect if the plugin
	   * can be loaded at startup - use {@link #setLoadedAtStartup(boolean)} instead. This needs
	   * to be called prior to a plugin's initialisation to take effect.
	   * 
	   * @param disabled
	   */
	  public void setDisabled(boolean disabled);
	
}
