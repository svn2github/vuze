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
import org.gudy.azureus2.pluginsimpl.local.ipfilter.IPFilterImpl;
import org.gudy.azureus2.pluginsimpl.local.logging.LoggerImpl;
import org.gudy.azureus2.pluginsimpl.local.peers.protocol.*;
import org.gudy.azureus2.pluginsimpl.local.sharing.ShareManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.tracker.*;
import org.gudy.azureus2.pluginsimpl.local.ui.*;
import org.gudy.azureus2.pluginsimpl.local.ui.config.ConfigSectionRepository;
import org.gudy.azureus2.pluginsimpl.local.ui.config.ParameterRepository;
import org.gudy.azureus2.pluginsimpl.local.ui.config.PluginConfigUIFactoryImpl;
import org.gudy.azureus2.pluginsimpl.local.utils.*;
import org.gudy.azureus2.pluginsimpl.local.update.*;
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
import org.gudy.azureus2.plugins.utils.*;
import org.gudy.azureus2.plugins.update.*;

import org.gudy.azureus2.ui.swt.FileDownloadWindow;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnManager;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.logging.*;

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
  protected boolean				operational;
  protected Logger				logger;
  
  protected boolean				initialisation_complete;
  
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
    props 				= new propertyWrapper(_props );
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
  
  	protected void
	setOperational(
		boolean	b )
	{
  		operational	= b;
  	}
  	
    public boolean
    isOperational()
	{
    	return( operational );
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
    getUIManager().getSWTManager().addView(view);
  } 
  
  public void addConfigSection(ConfigSection section)
  {
  	ConfigSectionRepository.getInstance().addConfigSection(section);
  }

  /**
   * @deprecated
   */
  public void openTorrentFile(String fileName) {
    TorrentOpener.openTorrent(fileName);
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
	String	version = (String)props.get("plugin.version");
  	
  	if ( version == null ){
  		
  		version = plugin_version;
  	}
  	
  	return( version );
  }

  public String
  getPluginID()
  {
  	String	id = (String)props.get("plugin.id");
  	
  	if ( id == null ){
  		
  		id = plugin_id;
  	}
  	
  	return( id==null?"<none>":id );
  }
  
  public Properties getPluginProperties() 
  {
    return(props);
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
    return new PluginConfigUIFactoryImpl(config,pluginConfigKey);
  }
  
  public String
  getPluginConfigKey()
  {
  	return( pluginConfigKey );
  }
  
  /** @deprecated Use getUIManager().getTableManager().createColumn */
  public void addColumnToPeersTable(String columnName, PluginPeerItemFactory item) {
    TableColumnManager.getInstance().addExtension(columnName,item);
  }
  
  /** @deprecated Use getUIManager().getTableManager().createColumn */
  public void addColumnToMyTorrentsTable(String columnName, PluginMyTorrentsItemFactory item) {
    TableColumnManager.getInstance().addExtension(columnName,item);
  }

  public Tracker getTracker() {
  	return( TrackerImpl.getSingleton());
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
  
  public Logger getLogger() 
  {
  	if ( logger == null ){
  		
  		logger = new LoggerImpl();
  	}
  	
  	return( logger );
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
  
  public ShortCuts
  getShortCuts()
  {
  	return( new ShortCutsImpl(this));
  }
  
  public UIManager
  getUIManager()
  {
  	return( new UIManagerImpl( this ));
  }
  
  public UpdateManager
  getUpdateManager()
  {
  	return( UpdateManagerImpl.getSingleton());
  }

  public PeerProtocolManager
  getPeerProtocolManager()
  {
  	return( PeerProtocolManagerImpl.getSingleton());
  }
  
  public boolean
  isUnloadable()
  {
  	String dir = getPluginDirectoryName();
  	
  		// if not dir based then just test this one
  	
  	if ( dir == null || dir.length() == 0 ){
  		
  		return(getPlugin() instanceof UnloadablePlugin );
  	}
  	
 	PluginInterface[]	pis = PluginManager.getPluginInterfaces();
  	
  	for (int i=0;i<pis.length;i++){
  		
  		PluginInterface	pi = pis[i];
  		
  		String other_dir = pi.getPluginDirectoryName();
  		
  		if ( other_dir == null || other_dir.length() == 0 ){
  			
  			continue;
  		}
  		
  		if ( dir.equals( other_dir )){
  			
  			if ( !(pi.getPlugin() instanceof UnloadablePlugin )){
  		
  				return( false );
  			}  
  		}
  	}
  	
  	return( true );
  }
  
  public void
  unload()
  
  	throws PluginException
  {
  	if ( !isUnloadable()){
  		
  		throw( new PluginException( "Plugin isn't unloadable" ));
  	}
  	
 	String dir = getPluginDirectoryName();
  	
  		// if not dir based then just test this one
  	
  	if ( dir == null || dir.length() == 0 ){
  		
		((UnloadablePlugin)getPlugin()).unload();
			
		initialiser.unloadPlugin( this );
		
  	}else{
  		
		PluginInterface[]	pis = PluginManager.getPluginInterfaces();
		 
		for (int i=0;i<pis.length;i++){
	  		
	  		PluginInterfaceImpl	pi = (PluginInterfaceImpl)pis[i];
	  		
			String other_dir = pi.getPluginDirectoryName();
	  		
	  		if ( other_dir == null || other_dir.length() == 0 ){
	  			
	  			continue;
	  		}
	  		
	  		if ( dir.equals( other_dir )){
		  			
	  			((UnloadablePlugin)pi.getPlugin()).unload();
	  			
	  			initialiser.unloadPlugin( pi );
	  		}
		}
  	}
  }
  
  public void
  reload()
  
  	throws PluginException
  {
  	unload();
  		
  	initialiser.reloadPlugin( this );
  }
  
  protected void
  initialisationComplete()
  {
  	initialisation_complete	= true;
  	
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
  	
  	if ( initialisation_complete ){
  		
  		l.initializationComplete();
  	}
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
  
  	// unfortunately we need to protect ourselves against the plugin itself trying to set
  	// plugin.version and plugin.id as this screws things up if they get it "wrong".
  	// They should be setting these things in the plugin.properties file
  	// currently the RSSImport plugin does this (version 1.1 sets version as 1.0)
  
  protected class
  propertyWrapper
  	extends Properties
  {
  	protected boolean	initialising	= true;
  	
  	protected
	propertyWrapper(
		Properties	_props )
	{
  		Iterator it = _props.keySet().iterator();
  		
  		while( it.hasNext()){
  			
  			Object	key = it.next();
  			
  			put( key, _props.get(key));
  		}
  		
  		initialising	= false;
  	}
  	
  	public Object
	setProperty(
		String		str,
		String		val )
	{
  			// if its us then we probably know what we're doing :P
  		
  		if ( !plugin.getClass().getName().startsWith( "org.gudy")){
  			
	  		if ( str.equalsIgnoreCase( "plugin.id" ) || str.equalsIgnoreCase("plugin.version" )){
	  		 			
	  			LGLogger.log(LGLogger.AT_COMMENT, "Plugin '" + getPluginName() + "' tried to set property '" + str + "' - action ignored" );
	  			
	  			return( null );
	  		}
  		}
  		
  		return( super.setProperty( str, val ));
  	}
  	
  	public Object
	put(
		Object	key,
		Object	value )
	{
			// if its us then we probably know what we're doing :P
  		
  		if ( !plugin.getClass().getName().startsWith( "org.gudy")){
  			
	 		if ((!initialising ) && key instanceof String ){
	  			
	  			String	k_str = (String)key;
	  			
	  	 		if ( k_str.equalsIgnoreCase( "plugin.id" ) || k_str.equalsIgnoreCase("plugin.version" )){
	  	 	  		
	  	 			LGLogger.log(LGLogger.AT_COMMENT, "Plugin '" + getPluginName() + "' tried to set property '" + k_str + "' - action ignored" );
	  	 		 
	  	 			return( null );
	  	 	  	}
	  		}
  		}
  		
  		return( super.put( key, value ));
  	}
  	
  	public Object
	get(
		Object	key )
	{
  		return( super.get(key));
  	}
  }
}
