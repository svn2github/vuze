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

import java.util.*;

import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.devices.*;
import com.aelitis.net.upnp.UPnPDevice;
import com.aelitis.net.upnp.services.UPnPWANConnection;

public class 
DeviceInternetGatewayImpl
	extends DeviceUPnPImpl
	implements DeviceInternetGateway
{
	private UPnPDevice				device;
	
	protected
	DeviceInternetGatewayImpl(
		UPnPDevice					_device,
		List<UPnPWANConnection>		_connections )
	{
		super( _device, Device.DT_INTERNET_GATEWAY );
		
		device	= _device;
	}
	
	protected boolean
	updateFrom(
		DeviceImpl		_other )
	{
		if ( !super.updateFrom( _other )){
			
			return( false );
		}
		
		if ( !( _other instanceof DeviceInternetGatewayImpl )){
			
			Debug.out( "Inconsistent" );
			
			return( false );
		}
		
		DeviceInternetGatewayImpl other = (DeviceInternetGatewayImpl)_other;
		
		return( true );
	}
}
