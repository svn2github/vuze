/*
 * Created on Dec 17, 2013
 * Created by Paul Gardner
 * 
 * Copyright 2013 Azureus Software, Inc.  All rights reserved.
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

import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.*;

import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.plugins.PluginAdapter;
import org.gudy.azureus2.plugins.PluginEvent;
import org.gudy.azureus2.plugins.PluginEventListener;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ipc.IPCInterface;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.proxy.AEProxySelectorFactory;
import com.aelitis.azureus.core.proxy.AEProxyFactory.PluginHTTPProxy;
import com.aelitis.azureus.core.proxy.AEProxyFactory.PluginProxy;
import com.aelitis.azureus.core.util.CopyOnWriteList;

public class 
AEPluginProxyHandler 
{
	private static CopyOnWriteList<PluginInterface>		plugins = new CopyOnWriteList<PluginInterface>();
	
	private static final int			plugin_init_max_wait	= 30*1000;
	private static final AESemaphore 	plugin_init_complete 	= new AESemaphore( "init:waiter" );
	
	static{
		try{
			AzureusCore core = AzureusCoreFactory.getSingleton();
			
			PluginInterface default_pi = core.getPluginManager().getDefaultPluginInterface();
			
			default_pi.addEventListener(
					new PluginEventListener()
					{
						public void 
						handleEvent(
							PluginEvent ev )
						{
							int	type = ev.getType();
							
							if ( type == PluginEvent.PEV_PLUGIN_OPERATIONAL ){
								
								pluginAdded((PluginInterface)ev.getValue());
							}
							if ( type == PluginEvent.PEV_PLUGIN_NOT_OPERATIONAL ){
								
								pluginRemoved((PluginInterface)ev.getValue());
							}
						}
					});
				
				PluginInterface[] plugins = default_pi.getPluginManager().getPlugins( true );
				
				for ( PluginInterface pi: plugins ){
					
					if ( pi.getPluginState().isOperational()){
					
						pluginAdded( pi );
					}
				}
				
				default_pi.addListener(
					new PluginAdapter()
					{
						public void 
						initializationComplete() 
						{
							plugin_init_complete.releaseForever();
						}
					});
				
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	private static void
	pluginAdded(
		PluginInterface pi )
	{
		String pid = pi.getPluginID();
		
		if ( pid.equals( "aznettor" ) || pid.equals( "azneti2phelper" )){
			
			plugins.add( pi );
		}
	}
	
	private static void
	pluginRemoved(
		PluginInterface pi )
	{
		String pid = pi.getPluginID();
		
		if ( pid.equals( "aznettor" ) || pid.equals( "azneti2phelper" )){
			
			plugins.remove( pi );
		}
	}
	
	private static final Map<Proxy,WeakReference<PluginProxyImpl>>	proxy_map = new IdentityHashMap<Proxy,WeakReference<PluginProxyImpl>>();
	
	public static boolean
	hasPluginProxyForNetwork(
		String		network,
		boolean		supports_data )
	{
		plugin_init_complete.reserve( plugin_init_max_wait );

		for ( PluginInterface pi: plugins ){

			String pid = pi.getPluginID();
			
			if ( pid.equals( "aznettor" ) && network == AENetworkClassifier.AT_TOR ){
				
				if ( !supports_data ){
				
					return( true );
				}
			}
			
			if ( pid.equals( "azneti2phelper" ) && network == AENetworkClassifier.AT_I2P ){
				
				return( true );
			}
		}
		
		return( false );
	}
	
	public static boolean
	hasPluginProxy()
	{
		plugin_init_complete.reserve( plugin_init_max_wait );
	
		for ( PluginInterface pi: plugins ){
		
			try{
				IPCInterface ipc = pi.getIPC();
			
				if ( ipc.canInvoke( "testHTTPPseudoProxy", new Object[]{ TorrentUtils.getDecentralisedEmptyURL() })){
					
					return( true );
				}
			}catch( Throwable e ){				
			}
		}

		return( false );
	}
	
		/**
		 * This method should NOT BE CALLED as it is in the .impl package - unfortunately the featman plugin calls it
		 * @param reason
		 * @param target
		 * @deprecated
		 * @return
		 */
	
	public static PluginProxyImpl
	getPluginProxy(
		String	reason,
		URL		target )
	{
		return( getPluginProxy( reason, target, false ));
	}
	
	public static PluginProxyImpl
	getPluginProxy(
		String	reason,
		URL		target,
		boolean	can_wait )
	{
		Proxy system_proxy = AEProxySelectorFactory.getSelector().getActiveProxy();
		
		if ( system_proxy == null || system_proxy.equals( Proxy.NO_PROXY )){
	
			String url_protocol = target.getProtocol().toLowerCase();
			
			if ( url_protocol.startsWith( "http" ) || url_protocol.equals( "ftp" )){
				
				if ( can_wait ){
					
					plugin_init_complete.reserve();
				}
				
				for ( PluginInterface pi: plugins ){
					
					try{
						IPCInterface ipc = pi.getIPC();
						
						Object[] proxy_details = (Object[])ipc.invoke( "getProxy", new Object[]{ reason, target } );
						
						if ( proxy_details != null ){
							
							if ( proxy_details.length == 2 ){
							
									// support old plugins
								
								proxy_details = new Object[]{ proxy_details[0], proxy_details[1], target.getHost()};
							}
							
							return( new PluginProxyImpl( reason, ipc, proxy_details ));
						}
					}catch( Throwable e ){				
					}
				}
			}
		}
		
		return( null );
	}
	
	public static PluginProxyImpl
	getPluginProxy(
		String		reason,
		String		host,
		int			port )
	{
		Proxy system_proxy = AEProxySelectorFactory.getSelector().getActiveProxy();
		
		if ( system_proxy == null || system_proxy.equals( Proxy.NO_PROXY )){
			
			for ( PluginInterface pi: plugins ){
				
				try{
					IPCInterface ipc = pi.getIPC();
					
					Object[] proxy_details = (Object[])ipc.invoke( "getProxy", new Object[]{ reason, host, port });
					
					if ( proxy_details != null ){
						
						return( new PluginProxyImpl( reason, ipc, proxy_details ));
					}
				}catch( Throwable e ){	
				}
			}
		}
		
		return( null );
	}

	public static PluginProxy
	getPluginProxy(
		Proxy		proxy )
	{
		if ( proxy != null ){
					
			synchronized( proxy_map ){
			
				WeakReference<PluginProxyImpl>	ref = proxy_map.get( proxy );
			
				if ( ref != null ){
					
					return( ref.get());
				}
			}
		}
		
		return( null );
	}
	
	public static Boolean
	testPluginHTTPProxy(
		URL			url,
		boolean		can_wait )
	{
		Proxy system_proxy = AEProxySelectorFactory.getSelector().getActiveProxy();
		
		if ( system_proxy == null || system_proxy.equals( Proxy.NO_PROXY )){
			
			String url_protocol = url.getProtocol().toLowerCase();

			if ( url_protocol.startsWith( "http" )){

				if ( can_wait ){
					
					plugin_init_complete.reserve();
				}
				
				for ( PluginInterface pi: plugins ){
					
					try{
						IPCInterface ipc = pi.getIPC();
						
						return((Boolean)ipc.invoke( "testHTTPPseudoProxy", new Object[]{ url }));
						
					}catch( Throwable e ){	
					}
				}
			}else{
				
				Debug.out( "Unsupported protocol: " + url_protocol );
			}
		}
		
		return( null );
	}
	
	public static PluginHTTPProxyImpl
	getPluginHTTPProxy(
		String		reason,
		URL			url,
		boolean		can_wait )
	{
		Proxy system_proxy = AEProxySelectorFactory.getSelector().getActiveProxy();
		
		if ( system_proxy == null || system_proxy.equals( Proxy.NO_PROXY )){
			
			String url_protocol = url.getProtocol().toLowerCase();

			if ( url_protocol.startsWith( "http" )){

				if ( can_wait ){
					
					plugin_init_complete.reserve();
				}
				
				for ( PluginInterface pi: plugins ){
					
					try{
						IPCInterface ipc = pi.getIPC();
						
						Proxy proxy = (Proxy)ipc.invoke( "createHTTPPseudoProxy", new Object[]{ reason, url });
						
						if ( proxy != null ){
							
							return( new PluginHTTPProxyImpl( reason, ipc, proxy ));
						}
					}catch( Throwable e ){	
					}
				}
			}else{
				
				Debug.out( "Unsupported protocol: " + url_protocol );
			}
		}
		
		return( null );
	}
	
	public static List<PluginInterface>
	getPluginHTTPProxyProviders(
		boolean	can_wait )
	{
		if ( can_wait ){
			
			plugin_init_complete.reserve();
		}
		
		List<PluginInterface> pis = 
			AzureusCoreFactory.getSingleton().getPluginManager().getPluginsWithMethod(
				"createHTTPPseudoProxy", 
				new Class[]{ String.class, URL.class });
		
		return( pis );
	}
	

	private static class
	PluginProxyImpl
		implements PluginProxy
	{
		private long				create_time = SystemTime.getMonotonousTime();
		
		private String				reason;
		
		private IPCInterface		ipc;
		private Object[]			proxy_details;
		
		private List<PluginProxyImpl>	children = new ArrayList<AEPluginProxyHandler.PluginProxyImpl>();
		
		private
		PluginProxyImpl(
			String				_reason,
			IPCInterface		_ipc,
			Object[]			_proxy_details )
		{
			reason				= _reason;
			ipc					= _ipc;
			proxy_details		= _proxy_details;
			
			WeakReference<PluginProxyImpl>	my_ref = new WeakReference<PluginProxyImpl>( this );
			
			synchronized( proxy_map ){
				
				proxy_map.put( getProxy(), my_ref );
				
				if ( proxy_map.size() > 1024 ){
					
					long	now = SystemTime.getMonotonousTime();
					
					Iterator<WeakReference<PluginProxyImpl>>	it = proxy_map.values().iterator();
					
					while( it.hasNext()){
						
						WeakReference<PluginProxyImpl> ref = it.next();
						
						PluginProxyImpl	pp = ref.get();
						
						if ( pp == null ){
							
							it.remove();
							
						}else{
							
							if ( now - pp.create_time > 5*60*1000 ){
								
								it.remove();
							}
						}
					}
				}
			}
		}
		
		public PluginProxy 
		getChildProxy(
			String		child_reason,
			URL 		url) 
		{
			PluginProxyImpl	child = getPluginProxy( reason + " - " + child_reason, url, false );
			
			if ( child != null ){
				
				synchronized( children ){
				
					children.add( child );
				}
			}
			
			return( child );
		}
		
		public Proxy
		getProxy()
		{
			return((Proxy)proxy_details[0]);
		}
		
			// URL methods
		
		public URL
		getURL()
		{
			return((URL)proxy_details[1]);
		}
		
		public String
		getURLHostRewrite()
		{
			return((String)proxy_details[2]);
		}
		
			// host:port methods
		
		public String
		getHost()
		{
			return((String)proxy_details[1]);
		}
		
		public int
		getPort()
		{
			return((Integer)proxy_details[2]);
		}
		
		public void
		setOK(
			boolean	good )
		{
			try{
				ipc.invoke( "setProxyStatus", new Object[]{ proxy_details[0], good });
				
			}catch( Throwable e ){
			}
			
			List<PluginProxyImpl> kids;
			
			synchronized( children ){
			
				kids = new ArrayList<PluginProxyImpl>( children );
				
				children.clear();
			}
			
			for ( PluginProxyImpl child: kids ){
				
				child.setOK( good );
			}
			
			synchronized( proxy_map ){

				proxy_map.remove( getProxy());
			}
		}
	}
	
	private static class
	PluginHTTPProxyImpl
		implements PluginHTTPProxy
	{
		private String			reason;
		private IPCInterface	ipc;
		private Proxy			proxy;
		
		private
		PluginHTTPProxyImpl(
			String				_reason,
			IPCInterface		_ipc,
			Proxy				_proxy )
		{
			reason				= _reason;
			ipc					= _ipc;
			proxy				= _proxy;
		}
		
		public Proxy
		getProxy()
		{
			return( proxy );
		}
		
		public String 
		proxifyURL(
			String url ) 
		{
			try{
				URL _url = new URL( url );
								
				InetSocketAddress pa = (InetSocketAddress)proxy.address();
					
				_url = UrlUtils.setHost( _url, pa.getAddress().getHostAddress());
				_url = UrlUtils.setPort( _url, pa.getPort());
			
				url = _url.toExternalForm();	
				
				url += ( url.indexOf('?')==-1?"?":"&" ) + "_azpproxy=1";
				
				return( url );
				
			}catch( Throwable e ){
				
				Debug.out( "Failed to proxify URL: " + url, e );
				
				return( url );
			}
		}
		
		public void 
		destroy() 
		{
			try{
				
				ipc.invoke( "destroyHTTPPseudoProxy", new Object[]{ proxy });
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
}
