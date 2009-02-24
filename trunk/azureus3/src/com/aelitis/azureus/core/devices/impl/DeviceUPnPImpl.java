/*
 * Created on Jan 28, 2009
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
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.ipc.IPCInterface;

import com.aelitis.azureus.core.content.AzureusContentFile;
import com.aelitis.azureus.core.devices.TranscodeFile;
import com.aelitis.azureus.core.devices.TranscodeTargetListener;
import com.aelitis.azureus.core.download.DiskManagerFileInfoFile;
import com.aelitis.azureus.core.util.UUIDGenerator;
import com.aelitis.net.upnp.UPnPDevice;
import com.aelitis.net.upnp.UPnPRootDevice;

public abstract class 
DeviceUPnPImpl
	extends DeviceImpl
	implements TranscodeTargetListener
{
	private static final String	TP_IP_ADDRESS = "DeviceUPnPImpl:ip";
	
	private static final Object UPNPAV_FILE_KEY = new Object();
	
	protected static String
	getDisplayName(
		UPnPDevice		device )
	{
		UPnPDevice	root = device.getRootDevice().getDevice();
		
		String fn = root.getFriendlyName();
		
		if ( fn == null || fn.length() == 0 ){
			
			fn = device.getFriendlyName();
		}
		
		String	dn = root.getModelName();
		
		if ( dn == null || dn.length() == 0 ){
		
			dn = device.getModelName();
		}
		
		if ( dn != null && dn.length() > 0 ){
			
			if ( !fn.contains( dn ) && ( !dn.contains( "Azureus" ) || dn.contains( "Vuze" ))){
			
				fn += " (" + dn + ")";
			}
		}
		
		return( fn );
	}
	
	
	
	private final DeviceManagerUPnPImpl	upnp_manager;
	private volatile UPnPDevice		device_may_be_null;
	
	private boolean	upnpav_integrated;
	
	protected
	DeviceUPnPImpl(
		DeviceManagerImpl		_manager,
		UPnPDevice				_device,
		int						_type )
	{
		super( _manager, _type, _type + "/" + _device.getRootDevice().getUSN(), getDisplayName( _device ), false );
		
		upnp_manager		= _manager.getUPnPManager();
		device_may_be_null 	= _device;
	}	
	

	
	protected
	DeviceUPnPImpl(
		DeviceManagerImpl	_manager,
		int					_type,
		String				_name )

	{
		super( _manager, _type, UUIDGenerator.generateUUIDString(), _name, true );
		
		upnp_manager		= _manager.getUPnPManager();
	}
	
	protected
	DeviceUPnPImpl(
		DeviceManagerImpl	_manager,
		int					_type,
		String				_uuid,
		String				_name,
		boolean				_manual )

	{
		super( _manager, _type, _uuid, _name, _manual );
		
		upnp_manager		= _manager.getUPnPManager();
	}
	
	protected
	DeviceUPnPImpl(
		DeviceManagerImpl	_manager,
		Map					_map )
	
		throws IOException
	{
		super(_manager, _map );
		
		upnp_manager		= _manager.getUPnPManager();
	}
	
	protected boolean
	updateFrom(
		DeviceImpl		_other )
	{
		if ( !super.updateFrom( _other )){
			
			return( false );
		}
		
		if ( !( _other instanceof DeviceUPnPImpl )){
			
			Debug.out( "Inconsistent" );
			
			return( false );
		}
		
		DeviceUPnPImpl other = (DeviceUPnPImpl)_other;
		
		device_may_be_null	= other.device_may_be_null;
		
		return( true );
	}
	
	protected UPnPDevice
	getUPnPDevice()
	{
		return( device_may_be_null );
	}
	
	public boolean
	isBrowsable()
	{
		return( true );
	}
	
	public browseLocation[]
	getBrowseLocations()
	{
		List<browseLocation>	locs = new ArrayList<browseLocation>();
	
		UPnPDevice device = device_may_be_null;
		
		if ( device != null ){
			
			URL		presentation = getPresentationURL( device );

			if ( presentation != null ){
					
				locs.add( new browseLocationImpl( "device.upnp.present_url", presentation ));
			}
			
			locs.add( new browseLocationImpl( "device.upnp.desc_url", device.getRootDevice().getLocation()));
		}
		
		return( locs.toArray( new browseLocation[ locs.size() ]));
	}
	
	protected URL
	getLocation()
	{
		UPnPDevice device = device_may_be_null;
		
		if ( device != null ){
			
			UPnPRootDevice root = device.getRootDevice();
			
			return( root.getLocation());
		}
		
		return( null );
	}
	
	protected InetAddress
	getAddress()
	{
		try{

			UPnPDevice device = device_may_be_null;
	
			if ( device != null ){
				
				UPnPRootDevice root = device.getRootDevice();
				
				URL location = root.getLocation();
				
				return( InetAddress.getByName( location.getHost() ));
				
			}else{
				
				InetAddress address = (InetAddress)getTransientProperty( TP_IP_ADDRESS );
				
				if ( address != null ){
					
					return( address );
				}
				
				String last = getPersistentStringProperty( TP_IP_ADDRESS );
				
				if ( last != null && last.length() > 0 ){
					
					return( InetAddress.getByName( last ));
				}
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		
		}
		
		return( null );
	}
	
	protected void
	setAddress(
		InetAddress	address )
	{
		setTransientProperty( DeviceUPnPImpl.TP_IP_ADDRESS, address );
		
		setPersistentStringProperty( DeviceUPnPImpl.TP_IP_ADDRESS, address.getHostAddress());
	}
	
	protected void
	browseReceived()
	{
		IPCInterface ipc = upnp_manager.getUPnPAVIPC();
		
		if ( ipc == null ){
			
			return;
		}
		
		synchronized( this ){
			
			if ( upnpav_integrated ){
				
				return;
			}
			
			upnpav_integrated = true;
			
			addListener( this );
		}
		
		TranscodeFile[]	transcode_files = getFiles();
		
		for ( TranscodeFile file: transcode_files ){
			
			fileAdded( file );
		}
	}
	
	protected boolean
	isVisible(
		AzureusContentFile		file )
	{
		return( file.getProperties().get( "DeviceUPnPImpl:device" ) == this );
	}
	
	public void
	fileAdded(
		final TranscodeFile		file )
	{
		IPCInterface ipc = upnp_manager.getUPnPAVIPC();
		
		if ( ipc == null ){
			
			return;
		}

		synchronized( this ){
			
			if ( !upnpav_integrated ){

				return;
			}
			
			if ( !file.isComplete()){
				
				return;
			}
			
			AzureusContentFile acf = (AzureusContentFile)file.getTransientProperty( UPNPAV_FILE_KEY );
			
			if ( acf != null ){
				
				return;
			}
			
			final Map<String,Object> properties =  new HashMap<String, Object>();
			
				// TODO: duration etc

			properties.put( "DeviceUPnPImpl:device", this );
			
			acf = 
				new AzureusContentFile()
				{
					private DiskManagerFileInfo f = new DiskManagerFileInfoFile( file.getFile());
				        	
					public DiskManagerFileInfo
				    getFile()
					{
						return( f );
					}
					
					public Map<String,Object>
					getProperties()
					{						
						return( properties );
					}
				};
				
			try{
				ipc.invoke( "addContent", new Object[]{ acf });
			
				file.setTransientProperty( UPNPAV_FILE_KEY, acf );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
	
	public void
	fileChanged(
		TranscodeFile		file )
	{
		if ( file.isComplete()){
			
			fileAdded( file );
		}
	}
	
	public void
	fileRemoved(
		TranscodeFile		file )
	{
		IPCInterface ipc = upnp_manager.getUPnPAVIPC();
		
		if ( ipc == null ){
			
			return;
		}

		synchronized( this ){

			AzureusContentFile acf = (AzureusContentFile)file.getTransientProperty( UPNPAV_FILE_KEY );

			if ( acf == null ){
		
				return;
			}
			
			file.setTransientProperty( UPNPAV_FILE_KEY, null );

			try{
				ipc.invoke( "removeContent", new Object[]{ acf });
			
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
	
	protected void
	resetUPNPAV()
	{		
		synchronized( this ){
			
			upnpav_integrated = false;
			
			removeListener( this );
			
			TranscodeFile[]	transcode_files = getFiles();
			
			for ( TranscodeFile file: transcode_files ){

				file.setTransientProperty( UPNPAV_FILE_KEY, null );
			}
		}
	}
	
	protected URL
	getPresentationURL(
		UPnPDevice		device )
	{
		String	presentation = device.getRootDevice().getDevice().getPresentation();
		
		if ( presentation != null ){
			
			try{
				URL url = new URL( presentation );
				
				return( url );

			}catch( Throwable e ){				
			}
		}
		
		return( null );
	}
	
	protected void
	getDisplayProperties(
		List<String[]>	dp )
	{
		super.getDisplayProperties( dp );
		
		UPnPDevice device = device_may_be_null;
		
		if ( device != null ){
			
			UPnPRootDevice root = device.getRootDevice();
			
			URL location = root.getLocation();
			
			addDP( dp, "dht.reseed.ip", location.getHost() + ":" + location.getPort()); 
	
			String	model_details 	= device.getModelName();
			String	model_url		= device.getModelURL();
			
			if ( model_url != null && model_url.length() > 0 ){
				model_details += " (" + model_url + ")";
			}
			
			String	manu_details 	= device.getManufacturer();
			String	manu_url		= device.getManufacturerURL();
			
			if ( manu_url != null && manu_url.length() > 0 ){
				manu_details += " (" + manu_url + ")";
			}
			
			addDP( dp, "device.model.desc", device.getModelDescription());
			addDP( dp, "device.model.name", model_details );
			addDP( dp, "device.model.num", device.getModelNumber());
			addDP( dp, "device.manu.desc", manu_details );
		}else{
			
			String ip = (String)getTransientProperty( TP_IP_ADDRESS );
			
			if ( ip != null ){
				
				addDP( dp, "dht.reseed.ip", ip ); 
			}
		}
	}
}
