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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.naming.directory.DirContext;

import org.gudy.azureus2.core3.config.COConfigurationListener;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.HostNameToIPResolver;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.proxy.*;
import com.aelitis.azureus.core.util.DNSUtils;

public class 
AEProxySelectorImpl 
	extends ProxySelector
	implements AEProxySelector
{		
	private static AEProxySelectorImpl		singleton = new AEProxySelectorImpl();
	
	private static List<Proxy>		no_proxy_list = Arrays.asList( new Proxy[]{ Proxy.NO_PROXY });

	
	public static AEProxySelector
	getSingleton()
	{
		return( singleton );
	}
	
	private volatile ActiveProxy		active_proxy;
	
	private final ProxySelector			existing_selector;
		
	private List<String>				alt_dns_servers	= new ArrayList<String>();
	
	private
	AEProxySelectorImpl()
	{	
		alt_dns_servers.add( "8.8.8.8" );

		COConfigurationManager.addAndFireListener(
				new COConfigurationListener()
				{
					public void 
					configurationSaved()
					{					
						boolean	enable_proxy 	= COConfigurationManager.getBooleanParameter("Enable.Proxy");
					    boolean enable_socks	= COConfigurationManager.getBooleanParameter("Enable.SOCKS");
					    
					    String	proxy_host 	= null;
					    int		proxy_port	= -1;
					    
					    if ( enable_proxy && enable_socks ){
					    	
					    	proxy_host 		= COConfigurationManager.getStringParameter("Proxy.Host").trim();
					    	proxy_port 		= Integer.parseInt(COConfigurationManager.getStringParameter("Proxy.Port").trim());

							if ( proxy_host.length() == 0 ){
								
								proxy_host = null;
							}
							
							if ( proxy_port <= 0 || proxy_port > 65535 ){
								
								proxy_host = null;
							}
					    }

					    synchronized( AEProxySelectorImpl.this ){
					    	
					    	if ( proxy_host == null ){
					    		
					    		if ( active_proxy != null ){
					    			
					    			active_proxy = null;
					    		}
					    	}else{
						    	if ( 	active_proxy == null ||
						    			!active_proxy.sameAddress( proxy_host, proxy_port )){
						    							   
						    		active_proxy = new ActiveProxy( proxy_host, proxy_port );
						    	}
					    	}
					    }
					}
				});
				
		existing_selector = ProxySelector.getDefault();

		try{
			ProxySelector.setDefault( this );
						
		}catch( Throwable e ){
			
			Debug.out( e );
		}
	}
	
	public List<Proxy> 
	select(
		URI uri )
	{	
		List<Proxy>  result = selectSupport( uri );
		
		System.out.println( "select: " + uri + " -> " + result );
		
		return( result );
	}
	
	private List<Proxy> 
	selectSupport(
		URI uri )
	{		
		ActiveProxy active = active_proxy;
		
		if ( active == null ){
			
			if ( existing_selector == null ){
				
				return( no_proxy_list );
			}

			List<Proxy> proxies = existing_selector.select( uri );
			
			Iterator<Proxy> it = proxies.iterator();
			
			while( it.hasNext()){
				
				Proxy p = it.next();
				
				if ( p.type() == Proxy.Type.SOCKS ){
					
					it.remove();
				}
			}
			
			if ( proxies.size() > 0 ){
				
				return( proxies );
			}
			
			return( no_proxy_list );
		}
		
			// we don't want to be recursing on this!
		
		if ( alt_dns_servers.contains( uri.getHost())){
			
			return( no_proxy_list );
		}
	
			// bit mindless this but the easiest way to see if we should apply socks proxy to this URI is to hit the existing selector
			// and see if it would (take a look at http://www.docjar.com/html/api/sun/net/spi/DefaultProxySelector.java.html).... 
			// requires the existing one to be picking up socks details which requires a restart after enabling but this is no worse than current...
				
		if ( existing_selector != null ){
			
			List<Proxy> proxies = existing_selector.select( uri );
						
			boolean	apply = false;
			
			for ( Proxy p: proxies ){
				
				if ( p.type() == Proxy.Type.SOCKS ){
					
					apply = true; 
					
					break;
				}
			}
			
			if ( !apply ){
				
				return( no_proxy_list );
			}
		}
		
		return( active.select());
	}

	private void
	connectFailed(
		SocketAddress	sa,
		Throwable 		error )
	{
		ActiveProxy active = active_proxy;

		if ( active == null || !( sa instanceof InetSocketAddress )){
			
			return;
		}
		
		active.connectFailed((InetSocketAddress)sa, error );
	}
	
	public void 
	connectFailed(
		URI 			uri, 
		SocketAddress 	sa, 
		IOException 	ioe )
	{			
		connectFailed( sa, ioe  );
		
		if ( existing_selector != null ){
		
			existing_selector.connectFailed( uri, sa, ioe );
		}
	} 
	
	public Proxy
	getSOCKSProxy(
		String		host,
		int			port )
	{		
		InetSocketAddress isa = new InetSocketAddress( host, port );
		
		ActiveProxy active = active_proxy;
		
		if ( active == null ){
			
			return( new Proxy( Proxy.Type.SOCKS, isa ));
		}
		
		if ( !active.getAddress().equals( isa )){
			
			return( new Proxy( Proxy.Type.SOCKS, isa ));
		}
		
		return( active.select().get(0));
	}
	
	public boolean
	isSOCKSProxyingActive()
	{
		try{
			List<Proxy> proxies =  select( new URL( "http://www.google.com/" ).toURI());
			
			for ( Proxy p: proxies ){
				
				if ( p.type() == Proxy.Type.SOCKS ){
					
					return( true );
				}
			}
			
			return( false );
			
		}catch( Throwable e ){
			
			Debug.out( e );
			
			return( false );
		}
	}
	
	public void
	connectFailed(
		Proxy			proxy,
		Throwable		error )
	{
		connectFailed( proxy.address(), error );
	}
	
	private class
	ActiveProxy
	{
		private final String				proxy_host;
		private final int 					proxy_port;
		
		private final InetSocketAddress		address;
		
		private volatile List<MyProxy>		proxy_list_cow 	= new ArrayList<MyProxy>();

		private Boolean			alt_dns_enable;
		
		private List<String>	alt_dns_to_try		= new ArrayList<String>();
		private List<String>	alt_dns_tried		= new ArrayList<String>();
		
		private boolean			default_dns_tried	= false;
		
		private
		ActiveProxy(
			String		_proxy_host,
			int			_proxy_port )
		{
			proxy_host	= _proxy_host;
			proxy_port	= _proxy_port;
			
			address	= new InetSocketAddress( proxy_host, proxy_port );
			    								    		
    		proxy_list_cow.add( new MyProxy( address ));
    		
    		alt_dns_to_try.addAll( alt_dns_servers );
		}
		
		private boolean
		sameAddress(
			String	host,
			int		port )
		{
			return( host.equals( proxy_host ) && port == proxy_port );
		}
		
		private InetSocketAddress
		getAddress()
		{
			return( address );
		}
		
		private List<Proxy>
		select()
		{
				// only return one proxy - this avoids the Java runtime from cycling through a bunch of
				// them that fail in the same way (e.g. if the address being connected to is unreachable) and
				// thus slugging everything
			
			return( Arrays.asList( new Proxy[]{ proxy_list_cow.get( 0 )}));
		}
		
		private void
		connectFailed(
			InetSocketAddress	failed_isa,
			Throwable 			error )
		{
			String msg = Debug.getNestedExceptionMessage( error );
				
				// filter out errors that are not associated with the socks server itself but rather then destination
			
			if ( msg.toLowerCase().contains( "unreachable" )){
				
				return;
			}		

			System.out.println( "failed: " + failed_isa + " -> " + msg );

			synchronized( this ){
													
				InetAddress	failed_ia 		= failed_isa.getAddress();
				String		failed_hostname = failed_ia==null?failed_isa.getHostName():null;	// avoid reverse DNS lookup if resolved
				
				MyProxy	matching_proxy = null;
				
				List<MyProxy>	new_list = new ArrayList<MyProxy>();
				
				Set<InetAddress>	existing_addresses = new HashSet<InetAddress>();
				
					// stick the failed proxy at the end of the list
				
				boolean	all_failed = true;
				
				for ( MyProxy p: proxy_list_cow ){
										
					InetSocketAddress p_isa = (InetSocketAddress)p.address();
					
					InetAddress	p_ia 		= p_isa.getAddress();
					String		p_hostname 	= p_ia==null?p_isa.getHostName():null;	// avoid reverse DNS lookup if resolved
	
					if ( p_ia != null ){
						
						existing_addresses.add( p_ia );
					}
					
					if ( 	( failed_ia != null && failed_ia.equals( p_ia )) ||
							( failed_hostname != null && failed_hostname.equals( p_hostname ))){
						
						matching_proxy = p;
						
						matching_proxy.setFailed();
						
					}else{
						
						new_list.add( p );
					}
					
					if ( p.getFailCount() == 0 ){
						
						all_failed = false;
					}
				}
				
				if ( matching_proxy == null ){
					
					System.out.println( "No proxy match for " + failed_isa );
					
				}else{
					
						// stick it at the end of the list
					
					new_list.add( matching_proxy );
				}
				
				if ( all_failed ){
					
					DirContext	dns_to_try = null;
					
						// make sure the host isn't an IP address...
					
					if ( alt_dns_enable == null ){
					
						alt_dns_enable = HostNameToIPResolver.hostAddressToBytes( proxy_host ) == null;
					}
					
					if ( alt_dns_enable ){

						if ( !default_dns_tried ){
							
							default_dns_tried = true;
							
							if ( failed_ia != null ){
								
									// the proxy resolved so at least the name appears valid so we might as well try the system DNS before
									// moving onto possible others
																										
								try{
									dns_to_try = DNSUtils.getInitialDirContext();
									
								}catch( Throwable e ){
									
									Debug.out( e );
								}
							}
						}
						
						if ( dns_to_try == null ){
							
							if ( alt_dns_to_try.size() > 0 ){
								
								String try_dns = alt_dns_to_try.remove( 0 );
								
								alt_dns_tried.add( try_dns );
								
								try{
									dns_to_try = DNSUtils.getDirContextForServer( try_dns );
									
								}catch( Throwable e ){
									
									Debug.out( e );
								}
							}
						}
		
						if ( dns_to_try != null ){
													
							try{					
								List<InetAddress> addresses = DNSUtils.getAllByName( dns_to_try, proxy_host );
								
								System.out.println( "DNS " + dns_to_try + " returned " + addresses );
								
								Collections.shuffle( addresses );
								
								for ( InetAddress a: addresses ){
									
									if ( !existing_addresses.contains( a )){
										
										new_list.add( 0, new MyProxy( new InetSocketAddress( a, proxy_port )));
									}
								}
							}catch( Throwable e ){
								
								Debug.out( e );
							}
						}
					}
				}
				
				proxy_list_cow = new_list;
			}
		}
	}
	
	private static class
	MyProxy
		extends Proxy
	{
		private int		fail_count	= 0;
		private long	last_fail;
		
		private
		MyProxy(
			InetSocketAddress	address )
		{
			super( Proxy.Type.SOCKS, address );
		}
		
		private void
		setFailed()
		{
			fail_count++;
			
			last_fail	= SystemTime.getMonotonousTime();
		}
		
		private int
		getFailCount()
		{
			return( fail_count );
		}
	}
}
