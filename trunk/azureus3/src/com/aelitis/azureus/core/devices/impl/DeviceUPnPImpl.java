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

import java.net.URL;
import java.util.List;

import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.net.upnp.UPnPDevice;
import com.aelitis.net.upnp.UPnPRootDevice;

public abstract class 
DeviceUPnPImpl
	extends DeviceImpl
{
	private UPnPDevice		device;
	
	protected
	DeviceUPnPImpl(
		UPnPDevice					_device,
		int							_type )
	{
		super( _type, _type + "/" + _device.getRootDevice().getUSN(), _device.getFriendlyName());
		
		device = _device;
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
		
		device	= other.device;
		
		return( true );
	}
	
	protected void
	getDisplayProperties(
		List<String[]>	dp )
	{
		super.getDisplayProperties( dp );
		
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
	}
}
