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
import java.util.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.devices.*;
import com.aelitis.azureus.plugins.upnp.UPnPMapping;
import com.aelitis.azureus.plugins.upnp.UPnPPlugin;
import com.aelitis.azureus.plugins.upnp.UPnPPluginService;
import com.aelitis.net.upnp.UPnPDevice;
import com.aelitis.net.upnp.services.UPnPWANConnection;

public class 
DeviceInternetGatewayImpl
	extends DeviceUPnPImpl
	implements DeviceInternetGateway
{
	private static UPnPPlugin						upnp_plugin;
	
	static{
		try{
		    PluginInterface pi_upnp = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByClass( UPnPPlugin.class );

		    if ( pi_upnp != null ){

		    	upnp_plugin = (UPnPPlugin)pi_upnp.getPlugin();
		    }
		}catch( Throwable e ){		
		}
	}
	
	private static List<DeviceInternetGatewayImpl>	igds;
		
	private boolean		mapper_enabled;
	
	private UPnPPluginService[]	current_services;
	private UPnPMapping[]		current_mappings;
	
	protected
	DeviceInternetGatewayImpl(
		DeviceManagerImpl			_manager,
		UPnPDevice					_device,
		List<UPnPWANConnection>		_connections )
	{
		super( _manager, _device, Device.DT_INTERNET_GATEWAY );
		
		updateStatus();
	}
	
	protected
	DeviceInternetGatewayImpl(
		DeviceManagerImpl	_manager,
		Map					_map )
	
		throws IOException
	{
		super(_manager, _map );
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
	
	protected void
	updateStatus()
	{
		super.updateStatus();
		
		mapper_enabled = upnp_plugin != null && upnp_plugin.isEnabled();
			
		UPnPDevice	device = getDevice();
		
		if ( mapper_enabled && device != null ){
		
			current_services = upnp_plugin.getServices( device );
			
			current_mappings = upnp_plugin.getMappings();
		}
	}
	
	protected Set<mapping>
	getRequiredMappings()
	{
		Set<mapping>	res = new TreeSet<mapping>();
			
		UPnPMapping[]		required_mappings 	= current_mappings;

		if ( required_mappings != null ){
			
			for ( UPnPMapping mapping: required_mappings ){
				
				if ( mapping.isEnabled()){
				
					res.add( new mapping( mapping ));
				}
			}
		}
		
		return( res );
	}
	
	protected void
	getDisplayProperties(
		List<String[]>	dp )
	{
		super.getDisplayProperties( dp );

		addDP(dp, "device.router.is_mapping", mapper_enabled );
		
		UPnPPluginService[]	services 			= current_services;
					
		String	req_map_str = "";
				
		Set<mapping> required = getRequiredMappings();
		
		for ( mapping m: required ){
			
			req_map_str += (req_map_str.length()==0?"":",") + m.getString();
		}
			
		addDP( dp, "device.router.req_map", req_map_str );
		
		if ( services != null ){
			
			for ( UPnPPluginService service: services ){
								
				UPnPPluginService.serviceMapping[] actual_mappings = service.getMappings();
				
				String	map_str = "";
				
				for ( UPnPPluginService.serviceMapping act_mapping: actual_mappings ){
											
					map_str += (map_str.length()==0?"":",") + ( act_mapping.isTCP()?"TCP":"UDP" ) + " " + act_mapping.getPort();
				}
				
				String service_name = MessageText.getString( "device.router.con_type", new String[]{ service.getService().getConnectionType() });
				
				addDP( dp, "!    " + service_name + "!", map_str );
			}
		}
	}
	
	protected static class
	mapping
		implements Comparable<mapping>
	{
		private boolean	is_tcp;
		private int		port;
		
		protected
		mapping(
			UPnPMapping m )
		{
			is_tcp		= m.isTCP();
			port		= m.getPort();
		}
		
		public int 
		compareTo(
			mapping o ) 
		{
			int res = port - o.port;
			
			if ( res == 0 ){
				
				res = (is_tcp?1:0) - (o.is_tcp?1:0);
			}
			
			return( res );
		}
		
		public boolean
		equals(
			Object	_other )
		{
			if ( _other instanceof mapping ){
		
				mapping other = (mapping)_other;
				
				return( is_tcp == other.is_tcp && port == other.port );
				
			}else{
				
				return( false );
			}
		}
		
		public int 
		hashCode() 
		{
			return((port<<16) + (is_tcp?1:0));
		}
		
		public String
		getString()
		{
			return( (is_tcp?"TCP":"UDP") + " " + port );
		}
	}
}
