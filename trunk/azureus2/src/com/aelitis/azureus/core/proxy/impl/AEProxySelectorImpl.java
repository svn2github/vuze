/*
 * Created on Nov 1, 2012
 * Created by Paul Gardner
 * 
 * Copyright 2012 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.proxy.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;

import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.proxy.*;

public class 
AEProxySelectorImpl 
	extends ProxySelector
	implements AEProxySelector
{
	private static AEProxySelectorImpl		singleton = new AEProxySelectorImpl();
	
	public static AEProxySelector
	getSingleton()
	{
		return( singleton );
	}
	
	private boolean			active;
	private ProxySelector	existing_selector;
	
	private
	AEProxySelectorImpl()
	{	
		try{
			existing_selector = ProxySelector.getDefault();

			ProxySelector.setDefault( this );
			
			active = true;
			
		}catch( Throwable e ){
			
			Debug.out( e );
		}
	}
	
	public List<Proxy> 
	select(
		URI uri )
	{
		List<Proxy> proxies = existing_selector.select( uri );
		
		if ( proxies.size() > 0 ){
			
			Proxy p = proxies.get(0);
			
			if ( p.type() == Proxy.Type.SOCKS ){
			
				proxies.add( 0,  new Proxy( Proxy.Type.SOCKS, new InetSocketAddress( "proxy.btguard.com", 1025 )));
			}
		}
			
		System.out.println( uri + " -> " + proxies );
		
		return( proxies );
	}

	public void 
	connectFailed(
		URI 			uri, 
		SocketAddress 	sa, 
		IOException 	ioe )
	{
		System.out.println( uri + ", " + sa + " -> " + Debug.getNestedExceptionMessage( ioe ));
		
		existing_selector.connectFailed( uri, sa, ioe );
	} 
}
