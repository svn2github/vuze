/*
 * Created on 1 Nov 2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package com.aelitis.azureus.core.networkmanager.admin.impl;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSocksProxy;
import com.aelitis.azureus.core.networkmanager.impl.tcp.ConnectDisconnectManager;
import com.aelitis.azureus.core.networkmanager.impl.tcp.ProtocolEndpointTCP;
import com.aelitis.azureus.core.networkmanager.impl.tcp.ProxyLoginHandler;
import com.aelitis.azureus.core.networkmanager.impl.tcp.TCPNetworkManager;
import com.aelitis.azureus.core.networkmanager.impl.tcp.TCPTransportHelperFilterFactory;
import com.aelitis.azureus.core.networkmanager.impl.tcp.TCPTransportImpl;
import com.aelitis.azureus.core.versioncheck.VersionCheckClient;

public class 
NetworkAdminSocksProxyImpl 
	implements NetworkAdminSocksProxy
{
	private final String	TARGET_HOST	= VersionCheckClient.HTTP_SERVER_ADDRESS;
	private final int		TARGET_PORT	= VersionCheckClient.HTTP_SERVER_PORT;
	
	private String		host;
	private String		port;
	
	private String		user;
	private String		password;
	
	protected
	NetworkAdminSocksProxyImpl()
	{
        host = System.getProperty( "socksProxyHost", "" ).trim();
        port = System.getProperty( "socksProxyPort", "" ).trim();
        
        user 		= System.getProperty("java.net.socks.username", "" ).trim();
        password 	= System.getProperty("java.net.socks.password", "").trim();
	}
	
	protected boolean
	isConfigured()
	{
		return( host.length() > 0 );
	}
	
	public String
	getHost()
	{
		return( host );
	}
	
	public String
	getPort()
	{
		return( port );
	}
	
	public String
	getUser()
	{
		return( user );
	}
	
	public String
	getVersion()
	{
		try{
			InetSocketAddress		socks_address = new InetSocketAddress( InetAddress.getByName( host ), Integer.parseInt(port));
			
			final InetSocketAddress	target_address = new InetSocketAddress( TARGET_HOST, TARGET_PORT );
			
			ConnectDisconnectManager.ConnectListener connect_listener = 
				new ConnectDisconnectManager.ConnectListener() 
			{
				public void 
				connectAttemptStarted() 
				{	
				}
	
				public void 
				connectSuccess( 
					SocketChannel channel ) 
				{
					final TCPTransportImpl	transport = 
						new TCPTransportImpl(
								new ProtocolEndpointTCP( target_address ), false, false, null );
					
					transport.setFilter( TCPTransportHelperFilterFactory.createTransparentFilter( channel ));
	
					new ProxyLoginHandler( 
							transport, 
							target_address, 
							new ProxyLoginHandler.ProxyListener() 
							{
								public void 
								connectSuccess() 
								{
									transport.close( "Done" );
								}
	
								public void 
								connectFailure(
									Throwable failure_msg ) 
								{
									transport.close( "Proxy login failed" );
								}
							});
				}
	
				public void 
				connectFailure( 
					Throwable failure_msg ) 
				{
		
				}
			};
	
			TCPNetworkManager.getSingleton().getConnectDisconnectManager().requestNewConnection(
					socks_address, connect_listener );
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
		
		return( null );
	}
}
