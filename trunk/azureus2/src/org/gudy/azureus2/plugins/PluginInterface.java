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

import org.gudy.azureus2.plugins.sharing.ShareManager;
import org.gudy.azureus2.plugins.sharing.ShareException;
import org.gudy.azureus2.plugins.tracker.Tracker;
import org.gudy.azureus2.plugins.utils.Utilities;
import org.gudy.azureus2.plugins.logging.Logger;
import org.gudy.azureus2.plugins.ipfilter.IPFilter;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.torrent.TorrentManager;
import org.gudy.azureus2.plugins.peers.protocol.PeerProtocolManager;
import org.gudy.azureus2.plugins.ui.*;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.PluginConfigUIFactory;
import org.gudy.azureus2.plugins.ui.tables.peers.PluginPeerItemFactory;
import org.gudy.azureus2.plugins.ui.tables.mytorrents.PluginMyTorrentsItemFactory;
import org.gudy.azureus2.plugins.update.UpdateManager;
import org.gudy.azureus2.plugins.utils.ShortCuts;


/**
 * Defines the communication interface between Azureus and Plugins
 * @author Olivier
 */
public interface PluginInterface {  
	
	public String
	getAzureusName();
	
	public String
	getAzureusVersion();
	
  /**
   * A Plugin might call this method to add a View to Azureus's views
   * The View will be accessible from View > Plugins > View name
   * @param view The PluginView to be added
   */
  public void addView(PluginView view);
  
  /**
   * adds a tab under the 'plugins' tab in the config view.<br>
   * Use {@link #getPluginConfigUIFactory()} to get the 
   * {@link PluginConfigUIFactory} class, from which you can create different
   * types of parameters.
   *
   * @param parameters the Parameter(s) to be edited
   * @param displayName the under which it should display.<br>
   * Azureus will look-up for ConfigView.section.plugins.<i>displayName</i>; into the lang files
   * in order to find the localized displayName. (see i18n)
   */
  public void addConfigUIParameters(Parameter[] parameters, String displayName);
  
  /** (DEPRECATED) Adds a column to the peers table.
   *
   * @param columnName the key name of the column
   * @param factory the factory responsible of creating items.
   * Azureus will look-up for PeersView.<i>columnName</i> into the lang files
   * in order to find the localized displayName. (see i18n)
   *
   * @deprecated use {@link org.gudy.azureus2.plugins.ui.tables.TableManager}
   */
  public void addColumnToPeersTable(String columnName,PluginPeerItemFactory factory);
  
  /** (DEPRECATED) Adds a column to the My Torrents table.
   *
   * @param columnName the key name of the column
   * @param factory the factory responsible of creating items.
   * Azureus will look-up for MyTorrentsView.<i>columnName</i> into the lang files
   * in order to find the localized displayName. (see i18n)
   *
   * @deprecated use {@link org.gudy.azureus2.plugins.ui.tables.TableManager}
   */
  public void addColumnToMyTorrentsTable(String columnName, PluginMyTorrentsItemFactory factory);

  /**
   * adds a ConfigSection to the config view.<p>
   * In contrast to addConfigUIParameters, this gives you total control over
   * a tab.  Please be kind and use localizable text.<BR>
   * @param section ConfigSection to be added to the Config view
   */
	public void addConfigSection(ConfigSection section);

  /**
   * Gives access to the tracker functionality
   * @return The tracker
   */
  
  public Tracker getTracker();
  
  /**
   * Gives access to the logger
   * @return The logger
   */
  
  public Logger getLogger();
  
  /**
   * Gives access to the IP filter
   * @return
   */
  
  public IPFilter
  getIPFilter();
  
  /**
   * Gives access to the download manager
   * @return
   */
  
  public DownloadManager
  getDownloadManager();
  
  /**
   * Gives access to the peer protocol manager
   * @return
   */
  
  public PeerProtocolManager
  getPeerProtocolManager();
  
  /**
   * Gives access to the sharing functionality
   * @return
   */
  
  public ShareManager
  getShareManager()
  
  	throws ShareException;
  
  /**
   * Gives access to the torrent manager
   * @return
   */
  
  public TorrentManager
  getTorrentManager();
  
  /**
   * access to various utility functions
   * @return
   */
  public Utilities
  getUtilities();
  
  /**
   * access to a set of convenience routines for doing things in a quicker, although less
   * structured, fashion
   * @return
   */
  
  public ShortCuts
  getShortCuts();
  
  /**
   * access to UI extension features 
   * @return
   */
  public UIManager
  getUIManager();
  
  /**
   * access to the update manager used to update plugins. required for non-Azureus SF hosted
   * plugins (SF ones are managed automatically)
   * @return
   */
  
  public UpdateManager
  getUpdateManager();
  
  /**
   * opens a torrent file given its name
   * @param fileName The Name of the file that azureus must open
   * @deprecated Use getDownloadManager().addDownload()
   */
  public void openTorrentFile(String fileName);
  
  /**
   * opens a torrent file given the url it's at
   * @param url The String representation of the url pointing to a torrent file
    *@deprecated Use getDownloadManager().addDownload()
  */
  public void openTorrentURL(String url);
  
  /**
   * gives access to the plugin properties
   * @return the properties from the file plugin.properties
   */
  public Properties getPluginProperties();
  
  /**
   * gives access to the plugin installation path
   * @return the full path the plugin is installed in
   */
  public String getPluginDirectoryName();
  
  /**
   * Returns the value of plugin.name if it exists in the properties file, dirctory name otherwise
   * @return
   */
  
  public String getPluginName();
  
  /**
   * Returns the version number of the plugin it if can be deduced from either the name of
   * the jar file it is loaded from or the properties file. null otherwise
   * @return
   */
  
  public String
  getPluginVersion();
  
  /**
   * Returns an identifier used to identify this particular plugin 
   * @return
   */
  
  public String
  getPluginID();
  
  /**
   * gives access to the plugin config interface
   * @return the PluginConfig object associated with this plugin
   */
  public PluginConfig getPluginconfig();
  
  
  /**
   * gives acess to the plugin Config UI Factory
   * @return the PluginConfigUIFactory associated with this plugin
   */
  public PluginConfigUIFactory getPluginConfigUIFactory();
  
  /**
   * gives access to the ClassLoader used to load the plugin
   * @return
   */
  
  public ClassLoader
  getPluginClassLoader();
  
  /**
   * Gives access to the plugin itself
   * @return
   */
  
  public Plugin
  getPlugin();
  
  /**
   * If a plugin fails to load properly (i.e. the construction of the plugin object
   * fails) it is marked as non-operational (rather than not being present at all) 
   * @return whether or not the plugin is operational or not
   */
  
  public boolean
  isOperational();
  
  public boolean
  isUnloadable();
  
  public void
  unload()
  
  	throws PluginException;
  
  public void
  reload()
  
  	throws PluginException;
  
  /**
   * gives access to the plugin manager
   * @return
   */
  
  public PluginManager
  getPluginManager();
  
  public void
  addListener(
  	PluginListener	l );
  
  public void
  removeListener(
  	PluginListener	l );
  
  public void
  addEventListener(
  	PluginEventListener	l );
  
  public void
  removeEventListener(
  	PluginEventListener	l );
}
