/*
 * File    : PluginInitializer.java
 * Created : 2 nov. 2003 18:59:17
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

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.tracker.host.*;
import org.gudy.azureus2.core3.logging.LGLogger;

import org.gudy.azureus2.ui.swt.SplashWindow;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.pluginsimpl.*;

import org.gudy.azureus2.core3.sharing.hoster.ShareHosterPlugin;
import org.gudy.azureus2.ui.tracker.TrackerDefaultWeb;
import org.gudy.azureus2.core3.internat.update.UpdateLanguagePlugin;

/**
 * @author Olivier
 * 
 */
public class 
PluginInitializer
	implements GlobalManagerListener
{

  private Class[]	initial_builtin_plugins = 
    	{ 		org.gudy.azureus2.core3.global.startstoprules.defaultplugin.StartStopRulesDefaultPlugin.class,
                 ShareHosterPlugin.class,
                 TrackerDefaultWeb.class,
                 UpdateLanguagePlugin.class,
        };
  
  private Class[]	final_builtin_plugins = 
  		{ 		org.gudy.azureus2.pluginsimpl.update.PluginUpdatePlugin.class,
        };
  
  private static PluginInitializer	singleton;
  
  private static List		registration_queue = new ArrayList();
   
  private SplashWindow splash;
  
  private TRHost		tracker_host;
  private GlobalManager	global_manager;
  
  private PluginInterface	default_plugin;
  private PluginManager		plugin_manager;
  
  private List		plugins				= new ArrayList();
  private List		plugin_interfaces	= new ArrayList();
  
  public static synchronized PluginInitializer
  getSingleton(
  	GlobalManager 	gm,
	SplashWindow 	splash )
  {
  	if ( singleton == null ){
  		
  		singleton = new PluginInitializer( gm, splash );
  		
  		for (int i=0;i<registration_queue.size();i++){
  			
  			try{
  				singleton.initializePluginFromClass((Class)registration_queue.get(i));
  				
  			}catch(PluginException e ){
  				
  			}
  		}
  		
  		registration_queue.clear();
  	}
  	
  	return( singleton );
  }
  
  protected static synchronized void
  queueRegistration(
  	Class	_class )
  {
  	if ( singleton == null ){
  		
  		registration_queue.add( _class );
 
  	}else{
  		
  		try{
  			singleton.initializePluginFromClass( _class );
  			
		}catch(PluginException e ){
  				
  		}
  	}
  }
  
  protected 
  PluginInitializer(
  	GlobalManager gm,
	SplashWindow splash) 
  {
  	global_manager	= gm;
  	
  	global_manager.addListener( this );
  	
    this.splash 	= splash;
    
    tracker_host	= TRHostFactory.create();
    
    plugin_manager = PluginManagerImpl.getSingleton( this );
  }
  
  public void initializePlugins() {
  	
  		// first do explicit plugins
  	  	
    File pluginDirectory = FileUtil.getUserFile("plugins");
        
    LGLogger.log("Plugin Directory is " + pluginDirectory);
    
    if ( !pluginDirectory.exists() ) {
      pluginDirectory.mkdirs();
    }
    
    if( pluginDirectory.isDirectory()){
    	
	    File[] pluginsDirectory = pluginDirectory.listFiles();
	    
	    for(int i = 0 ; i < pluginsDirectory.length ; i++) {
	    	
	      if(splash != null) {
          LGLogger.log("Initializing plugin " + pluginsDirectory[i].getName());

	      	
	        splash.setCurrentTask(MessageText.getString("splash.plugin") + pluginsDirectory[i].getName());
	      }
	      
	      try{
	      	initializePluginFromDir(pluginsDirectory[i]);
	      	
	      }catch( PluginException e ){
	      	
	      }
	      
	      if(splash != null) {
	      	float fPercentagePerTask = splash.getPercentagePerTask();
	      	if (fPercentagePerTask != 0) {
  	      	int newPercentage = splash.getPercentDone() + (int)(fPercentagePerTask / (float)pluginsDirectory.length);
	          splash.setPercentDone(newPercentage);
	        }
	      }
	    }
    }
    
    	// now do built in ones
      LGLogger.log("Initializing built-in plugins");
    
     for (int i=0;i<initial_builtin_plugins.length;i++){
    	
     	try{
     		initializePluginFromClass( initial_builtin_plugins[i] );
     		
		}catch(PluginException e ){
  				
  		}
     }
     
     for (int i=0;i<final_builtin_plugins.length;i++){
    	
     	try{
     		initializePluginFromClass( final_builtin_plugins[i] );
     		
		}catch(PluginException e ){
  				
  		}
    }
  }
  
  private void 
  initializePluginFromDir(
  	File directory)
  
  	throws PluginException
  {
  	
  	ClassLoader classLoader = null;
  	
    if(!directory.isDirectory()) return;
    String pluginName = directory.getName();
    File[] pluginContents = directory.listFiles();
    
    	// take only the highest version numbers of jars that look versioned
    
    String[]	plugin_version = {null};
    String[]	plugin_id = {null};
    
    pluginContents	= getHighestJarVersions( pluginContents, plugin_version, plugin_id );
    
    for(int i = 0 ; i < pluginContents.length ; i++) {
      classLoader = addFileToClassPath((URLClassLoader)classLoader, pluginContents[i]);
    }
    
    if ( classLoader == null ){
    	
    	classLoader = this.getClass().getClassLoader();
    }
    
    String plugin_class_string = null;
    
    try {
      Properties props = new Properties();
      
      File	properties_file = new File(directory.toString() + File.separator + "plugin.properties");
 
      try {
      	
      		// if properties file exists on its own then override any properties file
      		// potentially held within a jar
      	
      	if ( properties_file.exists()){
      	
      		props.load(new FileInputStream( properties_file ));
      		
      	}else{
      		URL url = ((URLClassLoader)classLoader).findResource("plugin.properties");
      		
      		props.load(url.openStream());
      	}
      }catch (Exception e) {
      	
      	String	msg =  "Can't read 'plugin.properties' for plugin '" + pluginName + "': file may be missing";
      	
      	LGLogger.logAlert( LGLogger.AT_ERROR, msg );
      	  
        System.out.println( msg );
        
        throw( new PluginException( msg, e ));
      }

      plugin_class_string = (String)props.get( "plugin.class");
      
      if ( plugin_class_string == null ){
      	
      	plugin_class_string = (String)props.get( "plugin.classes");
      }
      
      String	plugin_name_string = (String)props.get( "plugin.name");
      
      if ( plugin_name_string == null ){
      	
      	plugin_name_string = (String)props.get( "plugin.names");
      }
 
      int	pos1 = 0;
      int	pos2 = 0;
      
      while(true){
      		int	p1 = plugin_class_string.indexOf( ";", pos1 );
      	
      		String	plugin_class;
      	
      		if ( p1 == -1 ){
      			plugin_class = plugin_class_string.substring(pos1).trim();
      		}else{
      			plugin_class	= plugin_class_string.substring(pos1,p1).trim();
      			pos1 = p1+1;
      		}
      
      		if ( isPluginLoaded( plugin_class )){
      			
      	 		LGLogger.logAlert( LGLogger.AT_WARNING, "plugin class '" + plugin_class + "' is already loaded" );

      		}else{
      			
      		  String	plugin_name = null;
      		  
      		  if ( plugin_name_string != null ){
      		  	
      		  	int	p2 = plugin_name_string.indexOf( ";", pos2 );
              	
          	
          		if ( p2 == -1 ){
          			plugin_name = plugin_name_string.substring(pos2).trim();
          		}else{
          			plugin_name	= plugin_name_string.substring(pos2,p2).trim();
          			pos2 = p2+1;
          		}    
      		  }
      		  
      		  Properties new_props = (Properties)props.clone();
      		  
      		  new_props.put( "plugin.class", plugin_class );
      		  
      		  if ( plugin_name != null ){
      		  	
      		  	new_props.put( "plugin.name", plugin_name );
      		  }
      		  
      		  
	 	      // System.out.println( "loading plugin '" + plugin_class + "' using cl " + classLoader);
		      
		      Class c = classLoader.loadClass(plugin_class);
		      
		      Plugin plugin = (Plugin) c.newInstance();
		      
		      MessageText.integratePluginMessages((String)props.get("plugin.langfile"),classLoader);
		      
		      PluginInterfaceImpl plugin_interface = 
		      		new PluginInterfaceImpl(
		      					plugin, 
								this, 
								directory, 
								classLoader,
								directory.getName(),
								new_props,
								directory.getAbsolutePath(),
								plugin_id[0],
								plugin_version[0] );
		      
		      plugin.initialize(plugin_interface);
		      
		      plugins.add( plugin );
		      
		      plugin_interfaces.add( plugin_interface );
      	}
	      
	      if ( p1 == -1 ){
	      	break;
	      	
	      }
      }
    } catch(Throwable e) {
    	
    	if ( e instanceof PluginException ){
    		
    		throw((PluginException)e);
    	}
   
    	e.printStackTrace();
      
    	String	msg = "Error loading plugin '" + pluginName + "' / '" + plugin_class_string + "'";
 	 
    	LGLogger.logAlert( msg, e );

    	System.out.println( msg + " : " + e);
    	
    	throw( new PluginException( msg, e ));
    }
  }
  
  private URLClassLoader 
  addFileToClassPath(
  	URLClassLoader	classLoader,
	File 			f) 
  {
    if ( 	f.exists() &&
    		(!f.isDirectory())&&
    		f.getName().endsWith(".jar")){
    
    	try {
    		URL[] urls = {f.toURL()};
    		
    		if( classLoader == null ){
    			
    			classLoader = new URLClassLoader(urls, this.getClass().getClassLoader());
    			
    		}else{
    			
    			classLoader = new URLClassLoader(urls,classLoader);
    		}
    	}catch(Exception e) {
    		e.printStackTrace();
    	}
   	}
    
    return( classLoader );
  }
  
  protected void 
  initializePluginFromClass(
  	Class 	plugin_class )
  
  	throws PluginException
  {
  
  	if ( isPluginLoaded( plugin_class )){
  	
  		LGLogger.logAlert( LGLogger.AT_WARNING, "plugin class '" + plugin_class.getName() + "' is already loaded" );
  		
  		return;
  	}
  	
  	try{
  		Plugin plugin = (Plugin) plugin_class.newInstance();
  		
  		PluginInterfaceImpl plugin_interface = 
  			new PluginInterfaceImpl(
  						plugin, 
						this,
						plugin_class,
						plugin_class.getClassLoader(),
						"",
						new Properties(),
						"",
						"<intenal>",
						null );
  		
  		plugin.initialize(plugin_interface);
  		
   		plugins.add( plugin );
   		
   		plugin_interfaces.add( plugin_interface );
   		
  	}catch(Throwable e){
  		
  		e.printStackTrace();
  		
  		String	msg = "Error loading internal plugin '" + plugin_class.getName() + "'";
  		
    	LGLogger.logAlert( msg, e );

  		System.out.println(msg + " : " + e);
  		
  		throw( new PluginException( msg, e ));
  	}
  }
  
  protected void
  unloadPlugin(
  	PluginInterfaceImpl		pi )
  {
  	plugins.remove( pi.getPlugin());
  	
  	plugin_interfaces.remove( pi );
  }
  
  protected void
  reloadPlugin(
  	PluginInterfaceImpl		pi )
  
  	throws PluginException
  {
  	unloadPlugin( pi );
  	
  	Object key = pi.getInitializerKey();
  	
  	if ( key instanceof File ){
  		
  		initializePluginFromDir( (File)key );
  		
  	}else{
  		
  		initializePluginFromClass( (Class) key );
  	}
  }
 	
  protected TRHost
  getTrackerHost()
  {
  	return( tracker_host );
  }
  
  protected GlobalManager
  getGlobalManager()
  {
  	return( global_manager );
  }
  
  public static PluginInterface
  getDefaultInterface()
  {
  	return( singleton.getDefaultInterfaceSupport());
  }
  
  protected PluginInterface
  getDefaultInterfaceSupport()
  {
  
  	if ( default_plugin == null ){
  		
  		default_plugin = 
  			new PluginInterfaceImpl(
  					null,
					this,
					getClass(),
					getClass().getClassLoader(),
					"default",
					new Properties(),
					null,
					"<internal>",
					null );
  	}
  	
  	return( default_plugin );
  }
  
  public void
  downloadManagerAdded(
  	DownloadManager	dm )
  {
  }
  
  public void
  downloadManagerRemoved(
  	DownloadManager	dm )
  {
  }
  
  public void
  destroyInitiated()
  {	
  	for (int i=0;i<plugin_interfaces.size();i++){
  		
  		((PluginInterfaceImpl)plugin_interfaces.get(i)).closedownInitiated();
  	} 
  }
  
  public void
  destroyed()
  {
  	for (int i=0;i<plugin_interfaces.size();i++){
  		
  		((PluginInterfaceImpl)plugin_interfaces.get(i)).closedownComplete();
  	}  	
  }
  
  protected void
  fireEventSupport(
  	final int	type )
  {
  	PluginEvent	ev = new PluginEvent(){ public int getType(){ return( type );}};
  	
  	for (int i=0;i<plugin_interfaces.size();i++){
  		
  		((PluginInterfaceImpl)plugin_interfaces.get(i)).fireEvent(ev);
  	}  	
  }
  
  public static void
  fireEvent(
  	int		type )
  {
  	singleton.fireEventSupport(type);
  }
  
  protected void
  initialisationCompleteSupport()
  {
  	for (int i=0;i<plugin_interfaces.size();i++){
  		
  		((PluginInterfaceImpl)plugin_interfaces.get(i)).initialisationComplete();
  	}
  }
  
  
  public static void
  initialisationComplete()
  {
  	singleton.initialisationCompleteSupport();
  }
  
  public static List getPluginInterfaces() {
  	return singleton.getPluginInterfacesSupport();
  }

  protected List getPluginInterfacesSupport() {
  	return plugin_interfaces;
  }
  
  protected PluginInterface[]
  getPlugins()
  {
  	List	pis = getPluginInterfacesSupport();
  	
  	PluginInterface[]	res = new 	PluginInterface[pis.size()];
  	
  	pis.toArray(res);
  	
  	return( res );
  }
  
  protected PluginManager
  getPluginManager()
  {
  	return( plugin_manager );
  }
  
  protected boolean
  isPluginLoaded(
  	Class	cla )
  {
  	return( isPluginLoaded( cla.getName()));
  }
  
  protected boolean
  isPluginLoaded(
  	String	class_name )
  {  	
  	for (int i=0;i<plugins.size();i++){
  		
  		if ( plugins.get(i).getClass().getName().equals( class_name )){
  			
  			return( true );
  		}
  	}
  	
  	return( false );
  }
  
  	protected File[]
	getHighestJarVersions(
		File[]		files,
		String[]	version_out ,
		String[]	id_out )	// currently the version of last versioned jar found...
	{
  		List	res 		= new ArrayList();
  		Map		version_map	= new HashMap();
  		
  		for (int i=0;i<files.length;i++){
  			
  			File	f = files[i];
  			
  			String	name = f.getName().toLowerCase();
  			
  			if ( name.endsWith(".jar")){
  				
  				int	sep_pos = name.lastIndexOf("_");
  				
  				if ( sep_pos == -1 ){
  					
  					res.add( f );
  					
  				}else{
  					
  					String	prefix = name.substring(0,sep_pos);
					
					String	version = name.substring(sep_pos+1,name.length()-4);
					
					String	prev_version = (String)version_map.get(prefix);
					
					if ( prev_version == null ){
						
						version_map.put( prefix, version );
						
					}else{
					
						if ( PluginUtils.comparePluginVersions( prev_version, version ) > 0 ){
														
							version_map.put( prefix, version );
						}							
					}
  				}
  			}else{
  				
  				res.add( f );
  			}
  		}
  		
  		Iterator	it = version_map.keySet().iterator();
  		
  		while(it.hasNext()){
  			
  			String	prefix 	= (String)it.next();
  			String	version	= (String)version_map.get(prefix);
  			
  			String	target = prefix + "_" + version + ".";
  			
  			version_out[0] 	= version;
  			id_out[0]		= prefix;
  			
  			for (int i=0;i<files.length;i++){
  				
  				File	f = files[i];
  				
  				if ( f.getName().toLowerCase().startsWith( target )){
  					  					
  					res.add( f );
  					
  					break;
  				}
  			}
  		}
  		
  		File[]	res_array = new File[res.size()];
  		
  		res.toArray( res_array );
  		
  		return( res_array );
  	}
}
