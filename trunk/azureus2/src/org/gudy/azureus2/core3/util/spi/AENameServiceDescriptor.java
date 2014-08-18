/*
 * Created on Aug 18, 2014
 * Created by Paul Gardner
 * 
 * Copyright 2014 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or 
 * (at your option) any later version.
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

package org.gudy.azureus2.core3.util.spi;

import java.lang.reflect.*;
import java.net.UnknownHostException;

import sun.net.spi.nameservice.*;
import sun.net.spi.nameservice.dns.*;



public class 
AENameServiceDescriptor 
	implements NameServiceDescriptor 
{
	private final static NameService 	delegate;
	private final static Method 		delegate_method_lookupAllHostAddr;
	
	private final static NameService proxy_name_service;

	static{
		NameService old_ns 					= null;
		Method		old_lookupAllHostAddr	= null;
		
		NameService new_ns = null;
		
		try{
			old_ns = new DNSNameService();
			
			if ( old_ns != null ){
				
				old_lookupAllHostAddr = old_ns.getClass().getMethod( "lookupAllHostAddr", String.class );
				
				new_ns = 
						(NameService)Proxy.newProxyInstance(
							NameService.class.getClassLoader(),
							new Class[]{ NameService.class },
							new NameServiceProxy());
			}
		}catch( Throwable e ){
			
		}
		
		proxy_name_service					= new_ns;
		delegate 							= old_ns;
		delegate_method_lookupAllHostAddr 	= old_lookupAllHostAddr;
	}
	
	public NameService
	createNameService() 
	{
		return( proxy_name_service );
	}
	
	public String
	getType()
	{
		return( "dns" );
	}
	
	public String
	getProviderName() 
	{
		return( "aednsproxy" ); 
	}

	private static class 
	NameServiceProxy 
		implements InvocationHandler 
	{
		public Object
		invoke(
			Object		proxy, 
			Method 		method, 
			Object[]	args ) 
				
			throws Throwable 
		{		
			String method_name = method.getName();
			
			if ( method_name.equals( "getHostByAddr" )){
				
				return delegate.getHostByAddr((byte[])args[0]);
				
			}else if ( method_name.equals( "lookupAllHostAddr" )){
				
				String host_name = (String)args[0];
				
				if ( host_name == null || host_name.equals( "null" )){
						
					// get quite a few of these from 3rd party libs :(
					//new Exception("Bad DNS lookup: " + host_name).printStackTrace();
										
				}else if ( host_name.endsWith( ".i2p" ) || host_name.endsWith( ".onion" )){
					
					new Exception( "Prevented DNS leak for " + host_name ).printStackTrace();
					
					throw( new UnknownHostException( host_name ));
				}
				
				System.out.println( "DNS: " + host_name );
				
				try{
					return( delegate_method_lookupAllHostAddr.invoke( delegate, host_name ));
					
				}catch( InvocationTargetException e ){
					
					throw(((InvocationTargetException)e).getTargetException());
				}
				
			}else{		
			
				throw( new IllegalArgumentException( "Unknown method '" + method_name + "'" ));
			}
		}		
	}
}
