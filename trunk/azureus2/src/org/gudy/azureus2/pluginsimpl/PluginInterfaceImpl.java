/*
 * File    : PluginInterfaceImpl.java
 * Created : 12 nov. 2003
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
 
package org.gudy.azureus2.pluginsimpl;

import java.util.*;

import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginView;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.logging.Logger;
import org.gudy.azureus2.pluginsimpl.logging.LoggerImpl;
import org.gudy.azureus2.plugins.tracker.Tracker;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.PluginConfigUIFactory;
import org.gudy.azureus2.plugins.ui.tables.peers.PluginPeerItemFactory;
import org.gudy.azureus2.pluginsimpl.tracker.*;
import org.gudy.azureus2.pluginsimpl.ui.config.ParameterRepository;
import org.gudy.azureus2.pluginsimpl.ui.config.PluginConfigUIFactoryImpl;
import org.gudy.azureus2.pluginsimpl.ui.tables.peers.PeersTableExtensions;
import org.gudy.azureus2.plugins.peers.protocol.*;
import org.gudy.azureus2.pluginsimpl.peers.protocol.*;
import org.gudy.azureus2.plugins.sharing.*;
import org.gudy.azureus2.pluginsimpl.sharing.ShareManagerImpl;

import org.gudy.azureus2.ui.swt.FileDownloadWindow;
import org.gudy.azureus2.ui.swt.MainWindow;

/**
 * @author Olivier
 *
 */
public class PluginInterfaceImpl implements PluginInterface {
  
  protected PluginInitializer	initialiser;
  protected List				listeners = new ArrayList();
  String pluginConfigKey;
  Properties props;
  String pluginDir;
  PluginConfig config;

  public PluginInterfaceImpl(
  		PluginInitializer	_initialiser,
      String key,
      Properties props,
      String pluginDir) 
  {
  	initialiser	= _initialiser;
  	this.pluginConfigKey = "Plugin." + key;
    this.props = props;
    this.pluginDir = pluginDir;
    this.config = new PluginConfigImpl(pluginConfigKey);
  }

  public void addView(PluginView view)
  {
    MainWindow window = MainWindow.getWindow();
    if(window != null) {
      window.addPluginView(view);
    }
  } 

  public void openTorrentFile(String fileName) {
    MainWindow.getWindow().openTorrent(fileName);
  }

  public void openTorrentURL(String url) {
    new FileDownloadWindow(MainWindow.getWindow().getDisplay(),url);
  }
      
  public Properties getPluginProperties() {
    return props;
  }
  
  public String getPluginDirectoryName() {
    return pluginDir;
  }

  public void addConfigUIParameters(Parameter[] parameters, String displayName) {
  	ParameterRepository.getInstance().addPlugin(parameters, displayName);
  }


  public PluginConfig getPluginconfig() {
    return config;
  }


  public PluginConfigUIFactory getPluginConfigUIFactory() {
    return new PluginConfigUIFactoryImpl(pluginConfigKey);
  }
  
  public void addColumnToPeersTable(String columnName, PluginPeerItemFactory item) {
    PeersTableExtensions.getInstance().addExtension(columnName,item);
  }
  
  public Tracker getTracker() {
  	return( TrackerImpl.getSingleton( initialiser.getTrackerHost()));
  }
  
  public ShareManager
  getShareManager()
  
  	throws ShareException
  {
  	return( ShareManagerImpl.getSingleton());
  }
  
  public Logger getLogger() {
  	return( LoggerImpl.getSingleton());
  }
  
  public PeerProtocolManager
  getPeerProtocolManager()
  {
  	return( PeerProtocolManagerImpl.getSingleton());
  }
  
  protected void
  initialisationComplete()
  {
  	for (int i=0;i<listeners.size();i++){
  		
  		try{
  			((PluginListener)listeners.get(i)).initializationComplete();
  			
  		}catch( Throwable e ){
  			
  			e.printStackTrace();
  		}
  	}
  }
  
  public void
  addListener(
  	PluginListener	l )
  {
  	listeners.add(l);
  }
  
  public void
  removeListener(
  	PluginListener	l )
  {
  	listeners.remove(l);
  }
}
