/*
 * Created on Feb 10, 2009
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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ipc.IPCInterface;

import com.aelitis.azureus.core.devices.*;

public class 
DeviceiTunes
	extends DeviceImpl
	implements DeviceMediaRenderer
{
	private static final String UID = "a5d7869e-1ab9-6098-fef9-88476d988455";
	
	private static final int INSTALL_CHECK_PERIOD	= 60*1000;
	private static final int RUNNING_CHECK_PERIOD	= 30*1000;
	private static final int DEVICE_CHECK_PERIOD	= 10*1000;
	
	private static final int INSTALL_CHECK_TICKS	= INSTALL_CHECK_PERIOD / DeviceManagerImpl.DEVICE_UPDATE_PERIOD;
	private static final int RUNNING_CHECK_TICKS	= RUNNING_CHECK_PERIOD / DeviceManagerImpl.DEVICE_UPDATE_PERIOD;
	private static final int DEVICE_CHECK_TICKS		= DEVICE_CHECK_PERIOD / DeviceManagerImpl.DEVICE_UPDATE_PERIOD;
	

	private PluginInterface		itunes;
	
	private boolean				is_installed;
	private boolean				is_running;
	
	protected
	DeviceiTunes(
		DeviceManagerImpl	_manager,
		PluginInterface		_itunes )
	{
		super( _manager, DT_MEDIA_RENDERER, UID, "iTunes", true );
		
		itunes	= _itunes;
	}

	protected
	DeviceiTunes(
		DeviceManagerImpl	_manager,
		Map					_map )
	
		throws IOException
	{
		super( _manager, _map );
	}
	
	protected boolean
	updateFrom(
		DeviceImpl		_other )
	{
		if ( !super.updateFrom( _other )){
			
			return( false );
		}
		
		if ( !( _other instanceof DeviceiTunes )){
			
			Debug.out( "Inconsistent" );
			
			return( false );
		}
		
		DeviceiTunes other = (DeviceiTunes)_other;
		
		itunes = other.itunes;
		
		return( true );
	}
	
	@Override
	protected void
	updateStatus(
		int		tick_count )
	{
		super.updateStatus( tick_count );
		
		if ( itunes == null ){
			
			return;
		}
		
		if ( !is_installed ){
			
			if ( tick_count % INSTALL_CHECK_TICKS == 0 ){
				
				updateiTunesStatus();
				
				return;
			}
		}
		
		if ( !is_running ){
			
			if ( tick_count % RUNNING_CHECK_TICKS == 0 ){
				
				updateiTunesStatus();
				
				return;
			}
		}
		
		if ( tick_count % DEVICE_CHECK_TICKS == 0 ){

			updateiTunesStatus();
		}
	}
	
	protected void
	updateiTunesStatus()
	{
		
		IPCInterface	ipc = itunes.getIPC();
		
		try{
			Map<String,Object> properties = (Map<String,Object>)ipc.invoke( "getProperties", new Object[]{} );

			is_installed = (Boolean)properties.get( "installed" );
			
			is_running	 = (Boolean)properties.get( "running" );
			
			Throwable error = (Throwable)properties.get( "error" );
			
			if ( error != null ){
				
				throw( error );
			}
			
			List<Map<String,Object>> sources = (List<Map<String,Object>>)properties.get( "sources" );
			
			if ( sources != null ){
				
				for ( Map<String,Object> source: sources ){
					
					System.out.println( source );
				}
			}
		}catch( Throwable e ){
			
			log( "iTunes IPC failed", e );
		}
	}
	
	public boolean
	canCopyToDevice()
	{
		return( true );
	}
	
	public boolean
	canFilterFilesView()
	{
		return( false );
	}
	
	public void
	setFilterFilesView(
		boolean	filter )
	{
	}
	
	public boolean
	getFilterFilesView()
	{
		return( false );
	}
	
	public boolean 
	isBrowsable()
	{
		return( false );
	}
	
	public browseLocation[] 
	getBrowseLocations() 
	{
		return null;
	}
	
	protected void
	getDisplayProperties(
		List<String[]>	dp )
	{
		super.getDisplayProperties( dp );
		
		if ( itunes == null ){
			
			addDP( dp, "devices.comp.missing", "<null>" );

		}else{
			
			updateiTunesStatus();
			
			addDP( dp, "devices.installed", is_installed );
				
			addDP( dp, "MyTrackerView.status.started", is_running );
		}
	}
}
