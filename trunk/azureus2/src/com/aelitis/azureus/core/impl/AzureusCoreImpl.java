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
	
	protected GlobalManager		global_manager;
	
	
	protected boolean			running;
	protected List				listeners				= new ArrayList();
	protected List				lifecycle_listeners		= new ArrayList();
	
	protected AEMonitor			this_mon	= new AEMonitor( "AzureusCore" );

	protected
	AzureusCoreImpl()
	{
		COConfigurationManager.setSystemProperties();
		
		COConfigurationManager.checkConfiguration();
		
		LGLogger.initialise();
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
				
				throw( new AzureusCoreException( "core already running" ));
			}
			
			running	= true;
			
			global_manager = GlobalManagerFactory.create( this );
			
			for (int i=0;i<lifecycle_listeners.size();i++){
				
				((AzureusCoreLifecycleListener)lifecycle_listeners.get(i)).componentCreated( this, global_manager );
			}
	
		    PluginInitializer.getSingleton(this,this).initializePlugins( this );
		        
		    LGLogger.log("Initializing Plugins complete");
	
		    new AEThread("Plugin Init Complete")
		       {
		        	public void
		        	run()
		        	{
		        		PluginInitializer.initialisationComplete();
		        		
		        		for (int i=0;i<lifecycle_listeners.size();i++){
		        			
		        			((AzureusCoreLifecycleListener)lifecycle_listeners.get(i)).started( AzureusCoreImpl.this );
		        		}
		        	}
		       }.start();
	         
	            
		    //Catch non-user-initiated VM shutdown
	      //TODO: This does not seem to catch Windows' shutdown events, despite what Sun's
	      //documentation says. See for possible fix:
	      //http://www-106.ibm.com/developerworks/ibm/library/i-signalhandling
	      //http://www.smotricz.com/kabutz/Issue043.html 
	      //http://www.geeksville.com/~kevinh/projects/javasignals/
	      
		    Runtime.getRuntime().addShutdownHook( new Thread("Shutdown Hook") {
		      public void run() {
		        if( running ) {
		          try{
	              System.out.println( "Forced VM shutdown...auto-stopping..." );
	              stop();
	            }
	            catch( Throwable e ){  e.printStackTrace();  }
		        }
		      }
		    });
		    
		}finally{
			
			this_mon.exit();
		}
         
	}
	
	public void
	stop()
	
		throws AzureusCoreException
	{
		try{
			this_mon.enter();
		
			if ( !running ){
				
				throw( new AzureusCoreException( "core not running" ));
			}		
			
			running	= false;
			
			global_manager.stopAll();
			
			for (int i=0;i<lifecycle_listeners.size();i++){
				
				((AzureusCoreLifecycleListener)lifecycle_listeners.get(i)).stopped( this );
			}
			
			NonDaemonTaskRunner.waitUntilIdle();
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	
	public void
	requestStop()
	
		throws AzureusCoreException
	{
		for (int i=0;i<lifecycle_listeners.size();i++){
			
			if (((AzureusCoreLifecycleListener)lifecycle_listeners.get(i)).stopRequested( this )){
				
				return;
			}
		}

		stop();
	}
	
	public void
	requestRestart()
	
		throws AzureusCoreException
	{
		for (int i=0;i<lifecycle_listeners.size();i++){
			
			if (((AzureusCoreLifecycleListener)lifecycle_listeners.get(i)).restartRequested( this )){
				
				return;
			}
		}
		
		throw( new AzureusCoreException("Restart request unhandled" ));
	}
	
	public GlobalManager
	getGlobalManager()
	
		throws AzureusCoreException
	{
		if ( !running ){
			
			throw( new AzureusCoreException( "core not running" ));
		}
		
		return( global_manager );
	}
	
	public TRHost
	getTrackerHost()
	
		throws AzureusCoreException
	{	
		if ( !running ){
	
			throw( new AzureusCoreException( "core not running" ));

		}
		
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
		for (int i=0;i<listeners.size();i++){
			
			((AzureusCoreListener)listeners.get(i)).reportCurrentTask( currentTask ); 
		}
	}
	  
	public void 
	reportPercent(
		int percent )
	{
		for (int i=0;i<listeners.size();i++){
			
			((AzureusCoreListener)listeners.get(i)).reportPercent( percent ); 
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
