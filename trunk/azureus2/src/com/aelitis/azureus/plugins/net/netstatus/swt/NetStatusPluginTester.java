/*
 * Created on Jan 31, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.plugins.net.netstatus.swt;

import java.util.*;
import java.net.*;

import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.networkmanager.admin.*;

public class 
NetStatusPluginTester 
{
	private static final int	ROUTE_TIMEOUT	=630*1000;
	private static final String	ROUTE_TARGET	= "www.google.com";
	
	private loggerProvider		logger;
	
	private volatile boolean	test_cancelled;
	
	protected
	NetStatusPluginTester(
		loggerProvider		_logger )
	{
		logger	= _logger;
	}
	
	protected void
	run()
	{
		NetworkAdmin	admin = NetworkAdmin.getSingleton();
		
		log( "Testing outbound routing for the following interfaces:" );
		
		NetworkAdminNetworkInterface[] interfaces = admin.getInterfaces();
		
		for (int i=0;i<interfaces.length;i++){
			
			NetworkAdminNetworkInterface	intf = interfaces[i];
			
			NetworkAdminNetworkInterfaceAddress[] addresses = intf.getAddresses();
			
			String	a_str = "";
			
			for (int j=0;j<addresses.length;j++){
				
				NetworkAdminNetworkInterfaceAddress address = addresses[j];
				
				InetAddress ia = address.getAddress();
				
				if ( ia.isLoopbackAddress() || ia instanceof Inet6Address ){
					
				}else{
					
					a_str += (a_str.length()==0?"":",") + ia.getHostAddress();
				}
			}
			
			if ( a_str.length() > 0 ){
				
				log( "    " + intf.getName() + "/" + intf.getDisplayName() + ": " + a_str );
			}
		}
		
		try{
			InetAddress	target_address = InetAddress.getByName( ROUTE_TARGET );
			
			final Map	active_routes = new HashMap();
			
			admin.getRoutes( 
				target_address, 
				ROUTE_TIMEOUT, 
				new NetworkAdminRoutesListener()
				{
					public boolean
					foundNode(
						NetworkAdminNetworkInterfaceAddress		intf,
						NetworkAdminNode[]						route,
						int										distance,
						int										rtt )
					{
						if ( test_cancelled ){
							
							return( false );
						}
						
						synchronized( active_routes ){
							
							active_routes.put( intf, route );
						}
						
						log( intf.getAddress().getHostAddress() + " - " + route[route.length-1].getAddress().getHostAddress() + "(" + distance + ")" );
						
						return( true );
					}
					
					public boolean
					timeout(
						NetworkAdminNetworkInterfaceAddress		intf,
						NetworkAdminNode[]						route,
						int										distance )
					{
						if ( test_cancelled ){
							
							return( false );
						}
						
						log( intf.getAddress().getHostAddress() + " - timeout (" + distance + ")" );

							// see if we're getting nowhere
						
						if ( route.length == 0 && distance >= 3 ){
						
							log( intf.getAddress().getHostAddress() + ": giving up, no responses" );
							
							return( false );
						}
						
							// see if we've got far enough
						
						if ( route.length >= 5 && distance > 6 ){
							
							log( intf.getAddress().getHostAddress() + ": truncating, sufficient responses" );

							return( false );
						}
						
						return( true );
					}
				});

			int	num_routes = active_routes.size();
			
			if ( num_routes == 0 ){
				
				log( "No active routes found!" );
				
			}else{
				
				log( "Found " + num_routes + " route(s)" );
				
				Iterator it = active_routes.entrySet().iterator();
				
				while( it.hasNext()){
					
					Map.Entry entry = (Map.Entry)it.next();
					
					NetworkAdminNetworkInterfaceAddress address = (NetworkAdminNetworkInterfaceAddress)entry.getKey();
					
					NetworkAdminNode[]	route = (NetworkAdminNode[])entry.getValue();
					
					String	node_str = "";
					
					for (int i=0;i<route.length;i++){
						
						node_str += (i==0?"":",") + route[i].getAddress().getHostAddress();
					}
					
					log( "    " + address.getInterface().getName() + "/" + address.getAddress().getHostAddress() + " - " + node_str );
				}
			}
		}catch( Throwable e ){
			
			log( "Route tracing failed: " + Debug.getNestedExceptionMessage(e));
		}
	}
	
	protected void
	cancel()
	{
		test_cancelled	= true;
	}
	
	protected boolean
	isCancelled()
	{
		return( test_cancelled );
	}
	
	protected void
	log(
		String	str )
	{
		logger.log( str );
	}
	
	protected interface
	loggerProvider
	{
		public void
		log(
			String	str );
	}
	
}
