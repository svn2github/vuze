/*
 * Created on 13-Jul-2004
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.impl;

import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerFactory;
import org.gudy.azureus2.core3.internat.*;
import org.gudy.azureus2.core3.ipfilter.IpFilterManager;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.ipfilter.*;
import org.gudy.azureus2.core3.tracker.host.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;

import com.aelitis.azureus.core.*;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.peermanager.PeerManager;
import com.aelitis.azureus.core.peermanager.download.session.TorrentSessionManager;
import com.aelitis.azureus.core.update.AzureusRestarterFactory;

/**
 * @author parg
 *
 */

public class 
AzureusCoreImpl 
	implements 	AzureusCore, AzureusCoreListener
{
	protected static AzureusCore		singleton;
	protected static AEMonitor			class_mon	= new AEMonitor( "AzureusCore:class" );
	
	public static AzureusCore
	create()
	
		throws AzureusCoreException
	{
		try{
			class_mon.enter();
			
			if ( singleton != null ){
		
				throw( new AzureusCoreException( "Azureus core already instantiated" ));
			}
			
			singleton	= new AzureusCoreImpl();
			
			return( singleton );
			
		}finally{
			
			class_mon.exit();
		}
	}
	
	public static boolean
	isCoreAvailable()
	{
		return( singleton != null );
	}
	
	public static AzureusCore
	getSingleton()
	
		throws AzureusCoreException
	{
		if ( singleton == null ){
			
			throw( new AzureusCoreException( "core not instantiated"));
		}
		
		return( singleton );
	}	

	private PluginInitializer 	pi;
	private GlobalManager		global_manager;
	
	
	private boolean				running;
	private List				listeners				= new ArrayList();
	private List				lifecycle_listeners		= new ArrayList();
	
	private AEMonitor			this_mon	= new AEMonitor( "AzureusCore" );

	protected
	AzureusCoreImpl()
	{
		COConfigurationManager.initialise();
		
		LGLogger.initialise();
		
		AEDiagnostics.startup();
		
		AETemporaryFileHandler.startup();
    
			//ensure early initialization
		
		NetworkManager.getSingleton();
		
		PeerManager.getSingleton();
        
        TorrentSessionManager.getSingleton().init();
    
		pi = PluginInitializer.getSingleton(this,this);
	}
	
	public LocaleUtil
	getLocaleUtil()
	{
		return( LocaleUtil.getSingleton());
	}
	
	public void
	start()
	
		throws AzureusCoreException
	{
		try{
			this_mon.enter();
		
			if ( running ){
				
				throw( new AzureusCoreException( "Core: already running" ));
			}
			
			running	= true;
			
		}finally{
			
			this_mon.exit();
		}
         
	    LGLogger.log("Core: Loading of Plugins starts");

		pi.loadPlugins(this);
		
	    LGLogger.log("Core: Loading of Plugins complete");

		global_manager = GlobalManagerFactory.create( this );
		
		for (int i=0;i<lifecycle_listeners.size();i++){
			
			((AzureusCoreLifecycleListener)lifecycle_listeners.get(i)).componentCreated( this, global_manager );
		}

	    pi.initialisePlugins();
	        
	    LGLogger.log("Core: Initializing Plugins complete");

	    new AEThread("Plugin Init Complete")
	       {
	        	public void
	        	runSupport()
	        	{
	        		pi.initialisationComplete();
	        		
	        		for (int i=0;i<lifecycle_listeners.size();i++){
	        			
	        			((AzureusCoreLifecycleListener)lifecycle_listeners.get(i)).started( AzureusCoreImpl.this );
	        		}
	        	}
	       }.start();
         
           
       //late inits
	   NetworkManager.getSingleton().initialize(); 
           
            
	   //Catch non-user-initiated VM shutdown
       ShutdownHook.install( new ShutdownHook.Handler() {
         public void shutdown( String signal_name ) {
           LGLogger.log( "Core: Caught signal " +signal_name );
           shutdownCore();
         }
       });  
         
	   Runtime.getRuntime().addShutdownHook( new AEThread("Shutdown Hook") {
	     public void runSupport() {
           shutdownCore();
	     }
	   });
		   
	}
  
  
  
    private void shutdownCore() {
      if( running ) {
        try{
          LGLogger.log( "Core: Caught VM shutdown event; auto-stopping Azureus" );
        
          AzureusCoreImpl.this.stop();
        }
        catch( Throwable e ) {  
          Debug.printStackTrace( e );
        }
      }
    }
  
  
  
	
	private void
	runNonDaemon(
		final Runnable	r )
	
		throws AzureusCoreException
	{
		if ( !Thread.currentThread().isDaemon()){
			
			r.run();
			
		}else{
			
			final AESemaphore	sem = new AESemaphore( "AzureusCore:runNonDaemon" );
			
			final Throwable[]	error = {null};
			
			new AEThread( "AzureusCore:runNonDaemon" )
			{
				public void
				runSupport()
				{
					try{
			
						r.run();
						
					}catch( Throwable e ){
						
						error[0]	= e;
						
					}finally{
						
						sem.release();
					}
				}
			}.start();
			
			sem.reserve();
			
			if ( error[0] != null ){
	
				if ( error[0] instanceof AzureusCoreException ){
					
					throw((AzureusCoreException)error[0]);
					
				}else{
					
					throw( new AzureusCoreException( "Operation failed", error[0] ));
				}			
			}
		}
	}
	
	public void
	stop()
	
		throws AzureusCoreException
	{
		runNonDaemon( 
			new AERunnable()
			{
				public void
				runSupport()
				{
					LGLogger.log("Core: Stop operation starts");
					
					stopSupport( true );
				}
			});
	}
	
	private void
	stopSupport(
		boolean		apply_updates )
	
		throws AzureusCoreException
	{
		try{
			this_mon.enter();
		
			if ( !running ){
				
				throw( new AzureusCoreException( "Core not running" ));
			}		
			
			running	= false;
			
		}finally{
			
			this_mon.exit();
		}
		
		global_manager.stopAll();
			
		for (int i=0;i<lifecycle_listeners.size();i++){
				
			((AzureusCoreLifecycleListener)lifecycle_listeners.get(i)).stopped( this );
		}
			
		NonDaemonTaskRunner.waitUntilIdle();
		
		AEDiagnostics.shutdown();
		
		LGLogger.log("Core: Stop operation completes");

			// if any installers exist then we need to closedown via the updater
		
		if ( 	apply_updates && 
				getPluginManager().getDefaultPluginInterface().getUpdateManager().getInstallers().length > 0 ){
			
			AzureusRestarterFactory.create( this ).restart( true );
		}
	}
	
	
	public void
	requestStop()
	
		throws AzureusCoreException
	{
		runNonDaemon( 
				new AERunnable()
				{
					public void
					runSupport()
					{
				
						for (int i=0;i<lifecycle_listeners.size();i++){
							
							if ( !((AzureusCoreLifecycleListener)lifecycle_listeners.get(i)).stopRequested( AzureusCoreImpl.this )){
								
								LGLogger.log("Core: Request to stop the core has been denied");
								
								return;
							}
						}
							
						stop();
					}
				});
	}
	
	public void
	restart()
	
		throws AzureusCoreException
	{
		runNonDaemon( 
				new AERunnable()
				{
					public void
					runSupport()
					{
						LGLogger.log("Core: Restart operation starts");
							
						checkRestartSupported();
						
						stopSupport( false );
						
						LGLogger.log("Core: Restart operation: stop complete, restart initiated" );
				
						AzureusRestarterFactory.create( AzureusCoreImpl.this ).restart( false );
					}
				});
	}
	
	public void
	requestRestart()
	
		throws AzureusCoreException
	{
		runNonDaemon( 
				new AERunnable()
				{
					public void
					runSupport()
					{
						checkRestartSupported();
		
						for (int i=0;i<lifecycle_listeners.size();i++){
							
							if (!((AzureusCoreLifecycleListener)lifecycle_listeners.get(i)).restartRequested( AzureusCoreImpl.this )){
								
								LGLogger.log("Core: Request to restart the core has been denied");
								
								return;
							}
						}
						
						restart();
					}
				});
	}
	
	public void
	checkRestartSupported()
	
		throws AzureusCoreException
	{
		if ( getPluginManager().getPluginInterfaceByClass( "org.gudy.azureus2.update.UpdaterPatcher") == null ){
			
			LGLogger.logRepeatableAlert( LGLogger.AT_ERROR, "Can't restart without the 'azupdater' plugin installed" );
			
			throw( new  AzureusCoreException("Can't restart without the 'azupdater' plugin installed"));
		}
	}
	
	public GlobalManager
	getGlobalManager()
	
		throws AzureusCoreException
	{
		if ( global_manager == null ){
			
			throw( new AzureusCoreException( "Core not running" ));
		}
		
		return( global_manager );
	}
	
	public TRHost
	getTrackerHost()
	
		throws AzureusCoreException
	{	
		return( TRHostFactory.getSingleton());
	}
	
	public PluginManagerDefaults
	getPluginManagerDefaults()
	
		throws AzureusCoreException
	{
		return( PluginManager.getDefaults());
	}
	
	public PluginManager
	getPluginManager()
	
		throws AzureusCoreException
	{
			// don't test for runnign here, the restart process calls this after terminating the core...
		
		return( PluginInitializer.getDefaultInterface().getPluginManager());
	}
	
	public IpFilterManager
	getIpFilterManager()
	
		throws AzureusCoreException
	{
		return( IpFilterManagerFactory.getSingleton());
	}
	
	public void 
	reportCurrentTask(
		String currentTask )
	{
		pi.fireEvent( PluginEvent.PEV_INITIALISATION_PROGRESS_TASK, currentTask );
		
		for (int i=0;i<listeners.size();i++){
			
			try{
				((AzureusCoreListener)listeners.get(i)).reportCurrentTask( currentTask );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	  
	public void 
	reportPercent(
		int percent )
	{
		pi.fireEvent( PluginEvent.PEV_INITIALISATION_PROGRESS_PERCENT, new Integer( percent ));

		for (int i=0;i<listeners.size();i++){
			
			try{
				((AzureusCoreListener)listeners.get(i)).reportPercent( percent );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	public void
	addLifecycleListener(
		AzureusCoreLifecycleListener	l )
	{
		lifecycle_listeners.add(l);
	}
	
	public void
	removeLifecycleListener(
		AzureusCoreLifecycleListener	l )
	{
		lifecycle_listeners.remove(l);
	}
	
	public void
	addListener(
		AzureusCoreListener	l )
	{
		listeners.add( l );
	}
	
	public void
	removeListener(
		AzureusCoreListener	l )
	{
		listeners.remove( l );
	}
}
