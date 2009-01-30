/*
 * Created on Jan 27, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.core.devices.impl;

import java.util.*;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DelayedEvent;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreLifecycleAdapter;
import com.aelitis.azureus.core.devices.*;
import com.aelitis.azureus.core.util.*;

public class 
DeviceManagerImpl 
	implements DeviceManager
{
	private static final String	CONFIG_FILE = "devices.config";

	
	private static DeviceManagerImpl		singleton;
	
	public static void
	preInitialise()
	{
	}
	
	public static DeviceManager
	getSingleton()
	{
		synchronized( DeviceManagerImpl.class ){
			
			if ( singleton == null ){
				
				singleton = new DeviceManagerImpl();
			}
		}
		
		return( singleton );
	}
	
	
	
	private Map<String,DeviceImpl>		devices = new HashMap<String, DeviceImpl>();
	
	private DeviceManagerUPnPImpl	upnp_manager;
	
	private CopyOnWriteList<DeviceManagerListener>	listeners	= new CopyOnWriteList<DeviceManagerListener>();
	
	private boolean	closing;
	
	private boolean	config_unclean;
	private boolean	config_dirty;

	
	protected
	DeviceManagerImpl()
	{
		loadConfig();
		
		AzureusCoreFactory.getSingleton().addLifecycleListener(
			new AzureusCoreLifecycleAdapter()
			{
				public void
				stopping(
					AzureusCore		core )
				{					
					synchronized( DeviceManagerImpl.this ){
				
						if ( config_dirty || config_unclean ){
							
							saveConfig();
						}
						
						closing	= true;
					}
				}
			});
		
		upnp_manager = new DeviceManagerUPnPImpl( this );
		
		SimpleTimer.addPeriodicEvent(
				"DeviceManager:update",
				30*1000,
				new TimerEventPerformer()
				{
					public void 
					perform(
						TimerEvent event ) 
					{
						List<DeviceImpl> copy;
						
						synchronized( DeviceManagerImpl.this ){

							copy = new ArrayList<DeviceImpl>( devices.values() );
						}
						
						for ( DeviceImpl device: copy ){
							
							device.updateStatus();
						}
					}
				});
	}
	
	public void
	search()
	{
		upnp_manager.search();
	}
	
	protected boolean
	addDevice(
		DeviceImpl		device )
	{
		synchronized( this ){
			
			DeviceImpl existing = devices.get( device.getID());
			
			if ( existing != null ){
				
				existing.updateFrom( device );
								
				return( false );
			}
			
			devices.put( device.getID(), device );
		}
			
		device.updateStatus();
		
		device.alive();
		
		deviceAdded( device );
		
		configDirty();
		
		return( true );
	}
	
	public Device[]
  	getDevices()
	{
		synchronized( this ){
			
			return( devices.values().toArray( new Device[ devices.size()] ));
		}
	}
  		
	protected void
	loadConfig()
	{
		if ( !FileUtil.resilientConfigFileExists( CONFIG_FILE )){
			
			return;
		}
		
		log( "Loading configuration" );
				
		synchronized( this ){
			
			Map map = FileUtil.readResilientConfigFile( CONFIG_FILE );
			
			List	l_devices = (List)map.get( "devices" );
			
			if ( l_devices != null ){
				
				for (int i=0;i<l_devices.size();i++){
					
					Map	m = (Map)l_devices.get(i);
					
					try{
						DeviceImpl device = DeviceImpl.importFromBEncodedMapStatic(this,  m );
						
						devices.put( device.getID(), device );
						
						device.updateStatus();
					
						log( "    loaded " + device.getString());
						
					}catch( Throwable e ){
						
						log( "Failed to import subscription from " + m, e );
					}
				}
			}
		}
	}
	
	protected void
	configDirty(
		DeviceImpl		device,
		boolean			save_changes )
	{
		deviceChanged( device, save_changes );
	}
	
	protected void
	configDirty()
	{
		synchronized( this ){
			
			if ( config_dirty ){
				
				return;
			}
			
			config_dirty = true;
		
			new DelayedEvent( 
				"Subscriptions:save", 5000,
				new AERunnable()
				{
					public void 
					runSupport() 
					{
						synchronized( this ){
							
							if ( !config_dirty ){

								return;
							}
							
							saveConfig();
						}	
					}
				});
		}
	}
	
	protected void
	saveConfig()
	{
		log( "Saving configuration" );
		
		synchronized( this ){
			
			if ( closing ){
				
					// to late to try writing
				
				return;
			}
			
			config_dirty 	= false;
			config_unclean	= false;
			
			if ( devices.size() == 0 ){
				
				FileUtil.deleteResilientConfigFile( CONFIG_FILE );
				
			}else{
				
				Map map = new HashMap();
				
				List	l_devices = new ArrayList();
				
				map.put( "devices", l_devices );
				
				Iterator<DeviceImpl>	it = devices.values().iterator();
				
				while( it.hasNext()){
					
					DeviceImpl device = it.next();
						
					try{
						Map d = new HashMap();
						
						device.exportToBEncodedMap( d );
						
						l_devices.add( d );
						
					}catch( Throwable e ){
						
						log( "Failed to save device " + device.getString(), e );
					}
				}
				
				FileUtil.writeResilientConfigFile( CONFIG_FILE, map );
			}
		}
	}
	
	protected void
	deviceAdded(
		DeviceImpl		device )
	{
		configDirty();
		
		for ( DeviceManagerListener listener: listeners ){
			
			try{
				listener.deviceAdded( device );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
	
	
	protected void
	deviceChanged(
		DeviceImpl		device,
		boolean			save_changes )
	{
		if ( save_changes ){
			
			configDirty();
			
		}else{
			
			config_unclean = true;
		}
		
		for ( DeviceManagerListener listener: listeners ){
			
			try{
				listener.deviceChanged( device );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
	
	protected void
	deviceRemoved(
		DeviceImpl		device )
	{
		configDirty();
		
		for ( DeviceManagerListener listener: listeners ){
			
			try{
				listener.deviceRemoved( device );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
	
  	public void
  	addListener(
  		DeviceManagerListener		listener )
  	{
  		listeners.add( listener );
  	}
  	
  	public void
  	removeListener(
  		DeviceManagerListener		listener )
  	{
  		listeners.remove( listener );
  	}
  	
  	public void
  	log(
  		String		str )
  	{
  		System.out.println( str );
  	}
  	
 	public void
  	log(
  		String		str,
  		Throwable	e )
  	{
  		System.out.println( str );
  		
  		e.printStackTrace();
  	}
}
