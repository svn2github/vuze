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
 
package org.gudy.azureus2.pluginsimpl;

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

import org.gudy.azureus2.core3.sharing.hoster.ShareHosterPlugin;
import org.gudy.azureus2.ui.tracker.TrackerWebDefaultTrackerPlugin;
import org.gudy.azureus2.ui.tracker.TrackerWebDefaultStaticPlugin;

/**
 * @author Olivier
 * 
 */
public class 
PluginInitializer
	implements GlobalManagerListener
{

  private Class[]	builtin_plugins = 
    new Class[]{ org.gudy.azureus2.core3.global.startstoprules.defaultplugin.StartStopRulesDefaultPlugin.class,
                 ShareHosterPlugin.class,
                 TrackerWebDefaultStaticPlugin.class,
                 TrackerWebDefaultTrackerPlugin.class,
                };
  
  private static PluginInitializer	singleton;
  
  private static List		registration_queue = new ArrayList();
   
  private SplashWindow splash;
  
  private TRHost		tracker_host;
  private GlobalManager	global_manager;
  
  private PluginInterface	default_plugin;
  
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
  			
  			singleton.initializePluginFromClass((Class)registration_queue.get(i));
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
  		
  		singleton.initializePluginFromClass( _class );
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
  }
  
  public void initializePlugins() {
  	
  		// first do explicit plugins
  	  	
    File pluginDirectory = FileUtil.getApplicationFile(System.getProperty("file.separator") + "plugins");
    
    LGLogger.log("Plugin Directory is " + pluginDirectory);
    
    if( pluginDirectory.isDirectory()){
    	
	    File[] pluginsDirectory = pluginDirectory.listFiles();
	    
	    for(int i = 0 ; i < pluginsDirectory.length ; i++) {
	    	
	      if(splash != null) {
          LGLogger.log("Initializing plugin " + pluginsDirectory[i].getName());

	      	
	        splash.setCurrentTask(MessageText.getString("splash.plugin") + pluginsDirectory[i].getName());
	      }
	      
	      initializePluginFromDir(pluginsDirectory[i]);
	      
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
    
     for (int i=0;i<builtin_plugins.length;i++){
    	
    	initializePluginFromClass( builtin_plugins[i] );
    }
  }
  
  private void initializePluginFromDir(File directory) {
  	
  	ClassLoader classLoader = null;
  	
    if(!directory.isDirectory()) return;
    String pluginName = directory.getName();
    File[] pluginContents = directory.listFiles();
    
    for(int i = 0 ; i < pluginContents.length ; i++) {
      classLoader = addFileToClassPath((URLClassLoader)classLoader, pluginContents[i]);
    }
    
    if ( classLoader == null ){
    	
    	classLoader = this.getClass().getClassLoader();
    }
    
    String plugin_class = null;
    
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
      	
      	
          System.out.println("Can't read plugin.properties from plug-in " + pluginName + " : file may be missing.");
          return;    
      }

      plugin_class = (String)props.get( "plugin.class");
      
      // System.out.println( "loading plugin '" + plugin_class + "' using cl " + classLoader);
      
      Class c = classLoader.loadClass(plugin_class);
      
      Plugin plugin = (Plugin) c.newInstance();
      
      MessageText.integratePluginMessages((String)props.get("plugin.langfile"),classLoader);
      
      PluginInterfaceImpl plugin_interface = new PluginInterfaceImpl(this,classLoader,directory.getName(),props,directory.getAbsolutePath());
      
      plugin.initialize(plugin_interface);
      
      plugins.add( plugin );
      plugin_interfaces.add( plugin_interface );
      
    } catch(Throwable e) {
      e.printStackTrace();
      System.out.println("Error while loading class " + plugin_class + " : " + e);      
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
    			
    			classLoader = new URLClassLoader(urls);
    			
    		}else{
    			
    			classLoader = new URLClassLoader(urls,classLoader);
    		}
    	}catch(Exception e) {
    		e.printStackTrace();
    	}
   	}
    
    return( classLoader );
  }
  
  protected void initializePluginFromClass(Class plugin_class) {
  
  	try{
  		Plugin plugin = (Plugin) plugin_class.newInstance();
  		
  		PluginInterfaceImpl plugin_interface = new PluginInterfaceImpl(this,plugin_class.getClassLoader(),"",new Properties(),"");
  		
  		plugin.initialize(plugin_interface);
  		
   		plugins.add( plugin );
   		plugin_interfaces.add( plugin_interface );
   		
  	} catch(Throwable e) {
  		e.printStackTrace();
  		System.out.println("Error while loading internal plugin class " + plugin_class + " : " + e);      
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
  		
  		default_plugin = new PluginInterfaceImpl(this,getClass().getClassLoader(),"default",new Properties(),null);
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
}
