/*
 * Created on 06-Dec-2004
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
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
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.core3.proxy.impl;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.proxy.*;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.networkmanager.VirtualChannelSelector;

/**
 * @author parg
 *
 */

public class 
AEProxyImpl 
	implements AEProxy, VirtualChannelSelector.VirtualSelectorListener
{
	public static final int		THREAD_POOL_SIZE	= 16;
		
	protected int				port;
	protected long				timeout;
	
	protected VirtualChannelSelector	read_selector	 = new VirtualChannelSelector( VirtualChannelSelector.OP_READ );
	protected VirtualChannelSelector	connect_selector = new VirtualChannelSelector( VirtualChannelSelector.OP_CONNECT );
	protected VirtualChannelSelector	write_selector	 = new VirtualChannelSelector( VirtualChannelSelector.OP_WRITE );
	
	public 
	AEProxyImpl(
		long		_timeout )
	
		throws AEProxyException
	{		
		this(0, _timeout);
	}
	
	public 
	AEProxyImpl(
		int		_port,
		long	_timeout )
	
		throws AEProxyException
	{
		port	= _port;
		timeout	= _timeout;
		
		String bind_ip = COConfigurationManager.getStringParameter("Bind IP", "");
		
		try{
			
			final ServerSocketChannel	ssc = ServerSocketChannel.open();
			
			ServerSocket ss	= ssc.socket();
			
			ss.setReuseAddress(true);

			if ( bind_ip.length() < 7 ){
				
				ss.bind( new InetSocketAddress(port), 128 );
				
			}else{
				
				ss.bind(  new InetSocketAddress( InetAddress.getByName(bind_ip), port), 128 );
			}
			
			if ( port == 0 ){
				
				port	= ss.getLocalPort();
			}
				
			Thread connect_thread = 
				new AEThread("AEProxy:connect.loop")
				{
					public void
					runSupport()
					{
						selectLoop( connect_selector );
					}
				};
	
			connect_thread.setDaemon( true );
	
			connect_thread.start();
	
			Thread read_thread = 
				new AEThread("AEProxy:read.loop")
				{
					public void
					runSupport()
					{
						selectLoop( read_selector );
					}
				};
	
			read_thread.setDaemon( true );
	
			read_thread.start();
			
			Thread write_thread = 
				new AEThread("AEProxy:write.loop")
				{
					public void
					runSupport()
					{
						selectLoop( write_selector );
					}
				};
	
			write_thread.setDaemon( true );
	
			write_thread.start();
			
			Thread accept_thread = 
					new AEThread("AEProxy:accept.loop")
					{
						public void
						runSupport()
						{
							acceptLoop( ssc );
						}
					};
		
			accept_thread.setDaemon( true );
		
			accept_thread.start();									
		
			LGLogger.log( "AEProxy: listener established on port " + port ); 
			
		}catch( Throwable e){
		
			LGLogger.logUnrepeatableAlertUsingResource( 
					LGLogger.AT_ERROR,
					"Tracker.alert.listenfail",
					new String[]{ ""+port });
	
			LGLogger.log( "AEProxy: listener failed on port " + port, e ); 
						
			throw( new AEProxyException( "AEProxy: accept fails: " + e.toString()));
		}			
	}	
	
	protected void
	selectLoop(
		VirtualChannelSelector	selector )
	{
		while( true ){
			
			try{
				selector.select(100);
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	protected void
	acceptLoop(
		ServerSocketChannel	ssc )
	{		
		while(true){
			
			try{				
				final SocketChannel socket_channel = ssc.accept();
								
				if ( !socket_channel.socket().getInetAddress().isLoopbackAddress()){
					
					LGLogger.log( "AEProxy: incoming connection from '" + socket_channel.socket().getInetAddress() + "' - closed as not local" );
				
					socket_channel.close();
				}
						
				socket_channel.configureBlocking(false);

				read_selector.register( socket_channel, this, new AEProxyProcessor(this, socket_channel));
				
			}catch( Throwable e ){
				
				// e.printStackTrace();		
			}
		}
	}
	
	protected void
	requestWriteSelect(
		AEProxyProcessor	processor,
		SocketChannel 		sc )
	{
		write_selector.register( sc, this, processor );
	}
	
	protected void
	requestReadSelect(
		AEProxyProcessor	processor,
		SocketChannel 		sc )
	{
		read_selector.register( sc, this, processor );
	}
	
	protected void
	cancelReadSelect(
		AEProxyProcessor	processor,
		SocketChannel 		sc )
	{
		read_selector.cancel( sc );
	}
	
	protected void
	requestConnectSelect(
		AEProxyProcessor	processor,
		SocketChannel 		sc )
	{
		connect_selector.register( sc, this, processor );
	}
	
    public void 
	selectSuccess( 
		VirtualChannelSelector	selector, 
		SocketChannel 			sc,
		Object 					attachment )
    {
    	AEProxyProcessor	processor = (AEProxyProcessor)attachment;
    	   	
    	if ( selector == read_selector ){
    		
    		processor.read(sc);
    		
    	}else if ( selector == write_selector ){
    		
    		processor.write(sc);
    		
    	}else{
    		
    		processor.connect(sc);
    	}
    }
    
    public void 
	selectFailure( 
		VirtualChannelSelector	selector, 
		SocketChannel 			sc,
		Object 					attachment,
		Throwable 				msg )
    {
    	AEProxyProcessor	processor = (AEProxyProcessor)attachment;
    	
    	processor.failed( sc, msg );
    }
    
	public int
	getPort()
	{
		return( port );
	}
	
	protected long
	getSocketTimeout()
	{
		return( timeout );
	}
}
