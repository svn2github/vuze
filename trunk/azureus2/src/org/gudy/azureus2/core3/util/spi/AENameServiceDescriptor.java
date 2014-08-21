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
import java.net.InetAddress;
import java.net.UnknownHostException;

import sun.net.spi.nameservice.*;
import sun.net.spi.nameservice.dns.*;

/*
 * This proxy is controlled by the setting in ConfigurationManager
 * 
 * 		System.setProperty("sun.net.spi.nameservice.provider.1","dns,aednsproxy");
 * 
 * and also requires META-INF/services/sun.net.spi.nameservice.NameServiceDescriptor to contain the text
 * 
 *  	org.gudy.azureus2.core3.util.spi.AENameServiceDescriptor
 * 
 * On OSX you will need to do the following to get things to compile:
 * 
 * Windows -> Preferences -> Java -> Compiler -> Errors/Warnings -> Deprecated and restricted API -> Forbidden reference (access rules): -> change to warning
 * 
 */

public class 
AENameServiceDescriptor 
	implements NameServiceDescriptor 
{
	private final static NameService 	delegate_ns;
	private final static Method 		delegate_ns_method_lookupAllHostAddr;
	
	private final static Object		 	delegate_iai;
	private final static Method 		delegate_iai_method_lookupAllHostAddr;

	
	private final static NameService proxy_name_service;

	static{
		NameService default_ns 					= null;
		Method		default_lookupAllHostAddr	= null;
		
		NameService new_ns = null;
		
		try{
			default_ns = new DNSNameService();
			
			if ( default_ns != null ){
				
				default_lookupAllHostAddr = default_ns.getClass().getMethod( "lookupAllHostAddr", String.class );
				
				new_ns = 
						(NameService)Proxy.newProxyInstance(
							NameService.class.getClassLoader(),
							new Class[]{ NameService.class },
							new NameServiceProxy());
			}
		}catch( Throwable e ){
			
		}
		
		/*
		 * It almost works by delegating the the DNSNameService directly apart from InetAddress.getLocalHost() - rather than returning something sensible
		 * it fails with unknown host. However, if we directly grab the InetAddressImpl and use this (which has already been set up to use the default name server)
		 * things work better :( Hacked to support both at the moment...
		 */
		
		Object	iai						= null;
		Method	iai_lookupAllHostAddr	= null;

		try{
			Field field = InetAddress.class.getDeclaredField( "impl" );
			
			field.setAccessible( true );
			
			iai = field.get( null );
			
			iai_lookupAllHostAddr = iai.getClass().getMethod( "lookupAllHostAddr", String.class );
			
			iai_lookupAllHostAddr.setAccessible( true );

		}catch( Throwable e ){
			
			System.err.println( "Issue resolving the default name service..." );
		}	
		
		proxy_name_service						= new_ns;
		delegate_ns 							= default_ns;
		delegate_ns_method_lookupAllHostAddr 	= default_lookupAllHostAddr;
		delegate_iai 							= iai;
		delegate_iai_method_lookupAllHostAddr 	= iai_lookupAllHostAddr;
	}
	
	public static boolean
	isAvailable()
	{
		return( proxy_name_service != null );
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
				
				return delegate_ns.getHostByAddr((byte[])args[0]);
				
			}else if ( method_name.equals( "lookupAllHostAddr" )){
				
				String host_name = (String)args[0];
				
				if ( host_name == null || host_name.equals( "null" )){
						
					// get quite a few of these from 3rd party libs :(
					//new Exception("Bad DNS lookup: " + host_name).printStackTrace();
										
				}else if ( host_name.endsWith( ".i2p" ) || host_name.endsWith( ".onion" )){
					
					new Exception( "Prevented DNS leak for " + host_name ).printStackTrace();
					
					throw( new UnknownHostException( host_name ));
				}
				
				// System.out.println( "DNS: " + host_name );
				
				try{
					if ( delegate_iai_method_lookupAllHostAddr != null ){
						
						try{
							return( delegate_iai_method_lookupAllHostAddr.invoke(  delegate_iai, host_name ));

						}catch( Throwable e ){
						}
					}
					
					return( delegate_ns_method_lookupAllHostAddr.invoke( delegate_ns, host_name ));

				}catch( InvocationTargetException e ){
					
					throw(((InvocationTargetException)e).getTargetException());
				}
				
			}else{		
			
				throw( new IllegalArgumentException( "Unknown method '" + method_name + "'" ));
			}
		}		
	}
}
