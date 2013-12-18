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

import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.Socket;
import java.net.URL;

import org.gudy.azureus2.plugins.PluginEvent;
import org.gudy.azureus2.plugins.PluginEventListener;
import org.gudy.azureus2.plugins.PluginInterface;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.proxy.AEProxySelectorFactory;
import com.aelitis.azureus.core.proxy.AEProxyFactory.PluginProxy;
import com.aelitis.azureus.core.util.CopyOnWriteList;

public class 
AEPluginProxyHandler 
{
	private static CopyOnWriteList<PluginInterface>		plugins = new CopyOnWriteList<PluginInterface>();
	
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
				
				PluginInterface[] plugins = default_pi.getPluginManager().getPlugins();
				
				for ( PluginInterface pi: plugins ){
					
					if ( pi.getPluginState().isOperational()){
					
						pluginAdded( pi );
					}
				}
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	private static void
	pluginAdded(
		PluginInterface pi )
	{
		if ( pi.getPluginID().equals( "aznettor" )){
			
			plugins.add( pi );
		}
	}
	
	private static void
	pluginRemoved(
		PluginInterface pi )
	{
		if ( pi.getPluginID().equals( "aznettor" )){
			
			plugins.remove( pi );
		}
	}
	
	public static PluginProxy
	getPluginProxy(
		URL		target )
	{
		Proxy system_proxy = AEProxySelectorFactory.getSelector().getActiveProxy();
		
		if ( system_proxy == null || system_proxy == Proxy.NO_PROXY ){
			
			for ( PluginInterface pi: plugins ){
				
				try{
					Proxy proxy = (Proxy)pi.getIPC().invoke( "getProxy", new Object[]{ target } );
					
					if ( proxy != null ){
						
						return( new PluginProxyImpl( proxy ));
					}
				}catch( Throwable e ){				
				}
			}
		}
		
		return( null );
	}
	
	public static PluginProxy
	getPluginProxy(
		String	host,
		int		port )
	{
		Proxy system_proxy = AEProxySelectorFactory.getSelector().getActiveProxy();
		
		if ( system_proxy == null || system_proxy == Proxy.NO_PROXY ){
			
			for ( PluginInterface pi: plugins ){
				
				try{
					Proxy proxy = (Proxy)pi.getIPC().invoke( "getProxy", new Object[]{ host, port });
					
					if ( proxy != null ){
						
						return( new PluginProxyImpl( proxy ));
					}
				}catch( Throwable e ){	
				}
			}
		}
		
		return( null );
	}

	private static class
	PluginProxyImpl
		implements PluginProxy
	{
		private Proxy		proxy;
		
		private
		PluginProxyImpl(
			Proxy		_proxy )
		{
			proxy		= _proxy;
		}
		
		public Proxy
		getProxy()
		{
			return( proxy );
		}
		
		public void
		setOK(
			boolean	good )
		{
			System.out.println( "Good->" + good );
		}
	}
}
