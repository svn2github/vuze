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
 
package org.gudy.azureus2.pluginsimpl.local;

import java.util.*;
import java.io.File;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.logging.Logger;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.ipfiler.IPFilterImpl;
import org.gudy.azureus2.pluginsimpl.local.logging.LoggerImpl;
import org.gudy.azureus2.pluginsimpl.local.peers.protocol.*;
import org.gudy.azureus2.pluginsimpl.local.sharing.ShareManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.tracker.*;
import org.gudy.azureus2.pluginsimpl.local.ui.*;
import org.gudy.azureus2.pluginsimpl.local.ui.config.ConfigSectionRepository;
import org.gudy.azureus2.pluginsimpl.local.ui.config.ParameterRepository;
import org.gudy.azureus2.pluginsimpl.local.ui.config.PluginConfigUIFactoryImpl;
import org.gudy.azureus2.pluginsimpl.local.ui.tables.mytorrents.MyTorrentsTableExtensions;
import org.gudy.azureus2.pluginsimpl.local.ui.tables.peers.PeersTableExtensions;
import org.gudy.azureus2.pluginsimpl.local.utils.UtilitiesImpl;
import org.gudy.azureus2.plugins.ipfilter.IPFilter;
import org.gudy.azureus2.plugins.tracker.Tracker;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.PluginConfigUIFactory;
import org.gudy.azureus2.plugins.ui.tables.peers.PluginPeerItemFactory;
import org.gudy.azureus2.plugins.ui.tables.mytorrents.PluginMyTorrentsItemFactory;
import org.gudy.azureus2.plugins.peers.protocol.*;
import org.gudy.azureus2.plugins.sharing.*;
import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.plugins.torrent.*;
import org.gudy.azureus2.plugins.ui.*;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.utils.Utilities;

import org.gudy.azureus2.ui.swt.FileDownloadWindow;
import org.gudy.azureus2.ui.swt.MainWindow;

import org.gudy.azureus2.core3.util.*;

/**
 * @author Olivier
 *
 */
public class 
PluginInterfaceImpl 
	implements PluginInterface 
{
  protected Plugin				plugin;
  protected PluginInitializer	initialiser;
  protected Object				initialiser_key;
  protected ClassLoader			class_loader;
  protected List				listeners 		= new ArrayList();
  protected List				event_listeners	= new ArrayList();
  protected String 				pluginConfigKey;
  protected Properties 			props;
  protected String 				pluginDir;
  protected PluginConfig 		config;
  protected String				plugin_id;
  protected String				plugin_version;

  public 
  PluginInterfaceImpl(
  		Plugin				_plugin,
  		PluginInitializer	_initialiser,
		Object				_initialiser_key,
		ClassLoader			_class_loader,
		String 				_key,
		Properties 			_props,
		String 				_pluginDir,
		String				_plugin_id,
		String				_plugin_version ) 
  {
  	plugin				= _plugin;
  	initialiser			= _initialiser;
  	initialiser_key		= _initialiser_key;
  	class_loader		= _class_loader;
  	pluginConfigKey 	= "Plugin." + _key;
    props 				= _props;
    pluginDir 			= _pluginDir;
    config 				= new PluginConfigImpl(pluginConfigKey);
    plugin_id			= _plugin_id;
    plugin_version		= _plugin_version;
  }
  
  	public Plugin
	getPlugin()
	{
  		return( plugin );
	}
  
  	public Object
	getInitializerKey()
	{
  		return( initialiser_key );
  	}
  	
  	public PluginManager
	getPluginManager()
	{
  		return( initialiser.getPluginManager());
  	}
  	
	public String
	getAzureusName()
	{
		return( Constants.AZUREUS_NAME );
	}
	
	public String
	getAzureusVersion()
	{
		return( Constants.AZUREUS_VERSION );
	}
	

  public void addView(PluginView view)
  {
    MainWindow window = MainWindow.getWindow();
    if(window != null) {
      window.addPluginView(view);
    }
  } 
  
  public void addConfigSection(ConfigSection section)
  {
  	ConfigSectionRepository.getInstance().addConfigSection(section);
  }

  /**
   * @deprecated
   */
  public void openTorrentFile(String fileName) {
    MainWindow.getWindow().openTorrent(fileName);
  }

  /**
   * @deprecated
   */
  public void openTorrentURL(String url) {
    new FileDownloadWindow(MainWindow.getWindow().getDisplay(),url);
  }
      
  public String getPluginName()
  {
  	String	name = null;
  	
  	if ( props != null ){
  		
  		name = (String)props.get( "plugin.name");
  	}
  	
  	if ( name == null ){
  		
  		try{
  			
  			name = new File(pluginDir).getName();
  			
  		}catch( Throwable e ){
  			
  		}
  	}
  	
  	if ( name == null || name.length() == 0 ){
  		
  		name = plugin.getClass().getName();
  	}
  	
  	return( name );
  }

  public String
  getPluginVersion()
  {
  	return( plugin_version==null?(String)props.get("plugin.version"):plugin_version );
  }

  public String
  getPluginID()
  {
  	String	id = plugin_id==null?(String)props.get("plugin.id"):plugin_id;
  	
  	return( id==null?"<none>":id );
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
  
  public String
  getPluginConfigKey()
  {
  	return( pluginConfigKey );
  }
  
  public void addColumnToPeersTable(String columnName, PluginPeerItemFactory item) {
    PeersTableExtensions.getInstance().addExtension(columnName,item);
  }
  
  public void addColumnToMyTorrentsTable(String columnName, PluginMyTorrentsItemFactory item) {
    MyTorrentsTableExtensions.getInstance().addExtension(columnName,item);
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
  
  public DownloadManager
  getDownloadManager()
  {
  	return( DownloadManagerImpl.getSingleton(initialiser.getGlobalManager()));
  }
  
  public TorrentManager
  getTorrentManager()
  {
  	return( TorrentManagerImpl.getSingleton());
  }
  
  public Logger getLogger() {
  	return( LoggerImpl.getSingleton());
  }
  
  public IPFilter
  getIPFilter()
  {
  	return( new IPFilterImpl());
  }
  
  public Utilities
  getUtilities()
  {
  	return( new UtilitiesImpl( this ));
  }

  public UIManager
  getUIManager()
  {
  	return( new UIManagerImpl( this ));
  }
  
  public PeerProtocolManager
  getPeerProtocolManager()
  {
  	return( PeerProtocolManagerImpl.getSingleton());
  }
  
  public void
  unload()
  
  	throws PluginException
  {
  	if ( plugin instanceof UnloadablePlugin ){
  		
  		((UnloadablePlugin)plugin).unload();
  		
  	}else{
  		
  		throw( new PluginException( "Plugin isn't unloadable" ));
  	}
  }
  
  public void
  reload()
  
  	throws PluginException
  {
 	if ( plugin instanceof UnloadablePlugin ){
  		
  		((UnloadablePlugin)plugin).unload();
  		
  		initialiser.reloadPlugin( this );
  		
  	}else{
  		
  		throw( new PluginException( "Plugin isn't reloadable" ));
  	} 	
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
  
  protected void
  closedownInitiated()
  {
  	for (int i=0;i<listeners.size();i++){
  		
  		try{
  			((PluginListener)listeners.get(i)).closedownInitiated();
  			
  		}catch( Throwable e ){
  			
  			e.printStackTrace();
  		}
  	}
  }
  
  protected void
  closedownComplete()
  {
  	for (int i=0;i<listeners.size();i++){
  		
  		try{
  			((PluginListener)listeners.get(i)).closedownComplete();
  			
  		}catch( Throwable e ){
  			
  			e.printStackTrace();
  		}
  	}
  }
  
  protected void
  fireEvent(
  	PluginEvent		event )
  {
  	for (int i=0;i<event_listeners.size();i++){
  		
  		try{
  			((PluginEventListener)event_listeners.get(i)).handleEvent( event );
  			
  		}catch( Throwable e ){
  			
  			e.printStackTrace();
  		}
  	} 	
  }
  
  public ClassLoader
  getPluginClassLoader()
  {
  	return( class_loader );
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
  
  public void
  addEventListener(
  	PluginEventListener	l )
  {
  	event_listeners.add(l);
  }
  
  public void
  removeEventListener(
  	PluginEventListener	l )
  {
  	event_listeners.remove(l);
  }
}
