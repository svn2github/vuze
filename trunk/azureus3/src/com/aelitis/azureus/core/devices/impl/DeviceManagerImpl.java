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

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;

import com.aelitis.azureus.core.devices.*;
import com.aelitis.azureus.core.util.*;

public class 
DeviceManagerImpl 
	implements DeviceManager
{
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
	
	
	private CopyOnWriteList<DeviceManagerListener>	listeners	= new CopyOnWriteList<DeviceManagerListener>();
	
	protected
	DeviceManagerImpl()
	{
		new DeviceManagerUPnPImpl( this );
		
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
						
						synchronized( devices ){

							copy = new ArrayList<DeviceImpl>( devices.values() );
						}
						
						for ( DeviceImpl device: copy ){
							
							device.updateStatus();
						}
					}
				});
	}
	
	protected boolean
	addDevice(
		DeviceImpl		device )
	{
		synchronized( devices ){
			
			DeviceImpl existing = devices.get( device.getID());
			
			if ( existing != null ){
				
				existing.updateFrom( device );
								
				return( false );
			}
			
			devices.put( device.getID(), device );
		}
				
		device.alive();
		
		for ( DeviceManagerListener listener: listeners ){
		
			try{
				listener.deviceAdded( device );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
		
		return( true );
	}
	
	public Device[]
  	getDevices()
	{
		synchronized( devices ){
			
			return( devices.values().toArray( new Device[ devices.size()] ));
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
