/*
 * Created on 13-Dec-2004
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

package com.aelitis.azureus.core.proxy.socks.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.proxy.AEProxyConnection;
import com.aelitis.azureus.core.proxy.socks.*;


/**
 * @author parg
 *
 */

public class 
AESocksProxyPlugableConnectionDefault
	implements AESocksProxyPlugableConnection
{
	protected AESocksProxyConnection	socks_connection;
	protected AEProxyConnection			connection;
	
	protected SocketChannel		source_channel;
	protected SocketChannel		target_channel;

	public
	AESocksProxyPlugableConnectionDefault(
		AESocksProxyConnection		_socks_connection )
	{
		socks_connection	= _socks_connection;
		connection			= socks_connection.getConnection();
		
		source_channel	= connection.getSourceChannel();
	}
	
	public String
	getName()
	{
		if ( target_channel != null ){
			
			return( target_channel.socket().getInetAddress() + ":" + target_channel.socket().getPort());
		}
		
		return( "" );
	}

	public InetAddress
	getAddress()
	{
		return( target_channel.socket().getInetAddress());
	}
	
	public int
	getPort()
	{
		return( target_channel.socket().getPort());
	}
	
	public void
	connect(
		InetSocketAddress		_address )
		
		throws IOException
	{
		new proxyStateRelayConnect( _address );
	}
	
	public void
	relayData()
	
		throws IOException
	{
		new proxyStateRelayData();
	}
	
	public void
	close()
	{
		if ( target_channel != null ){
			
			try{
				connection.cancelReadSelect( target_channel );
				
				target_channel.close();
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}	
	}
	
	
	protected class
	proxyStateRelayConnect
		extends AESocksProxyState
	{
		protected InetSocketAddress	address;
		
		protected
		proxyStateRelayConnect(
			InetSocketAddress	_address )
		
			throws IOException
		{
			super( socks_connection );
			
			address			= _address;
			
				// OK, we're almost ready to roll. Unregister the read select until we're 
				// connected
		
			connection.cancelReadSelect( source_channel );

			connection.setConnectState( this );
			
			target_channel = SocketChannel.open();
			
		    String bindIP = COConfigurationManager.getStringParameter("Bind IP", "");
		    
	        if ( bindIP.length() > 6 ){
	        	
	        	target_channel.socket().bind( new InetSocketAddress( InetAddress.getByName( bindIP ), 0 ) );
	        }
	        
	        target_channel.configureBlocking( false );
	        
	        target_channel.connect( address );
	   
	        connection.requestConnectSelect( target_channel );
		}
		
		protected void
		connectSupport(
			SocketChannel 		sc )
		
			throws IOException
		{
			if( !sc.finishConnect()){
				
				throw( new IOException( "finishConnect returned false" ));
			}
	           
			socks_connection.connected();
		}
	}
	
	protected class
	proxyStateRelayData
		extends AESocksProxyState
	{
		protected ByteBuffer		source_buffer;
		protected ByteBuffer		target_buffer;
		
		protected long				outward_bytes	= 0;
		protected long				inward_bytes	= 0;
		
		protected
		proxyStateRelayData()
		
			throws IOException
		{		
			super( socks_connection );
			
			source_buffer	= ByteBuffer.allocate( 1024 );
			target_buffer	= ByteBuffer.allocate( 1024 );
			
			connection.setReadState( this );
			
			connection.setWriteState( this );
			
			connection.requestReadSelect( source_channel );
			
			connection.requestReadSelect( target_channel );
			
			connection.setConnected();
		}
		
		protected void
		readSupport(
			SocketChannel 		sc )
		
			throws IOException
		{
			connection.setTimeStamp();
			
			SocketChannel	chan1 = sc;
			SocketChannel	chan2 = sc==source_channel?target_channel:source_channel;
			
			ByteBuffer	read_buffer = sc==source_channel?source_buffer:target_buffer;
									
			int	len = chan1.read( read_buffer );
	
			if ( len == -1 ){
				
					//means that the channel has been shutdown
				
				connection.close();
				
			}else{
				
				if ( read_buffer.position() > 0 ){
					
					read_buffer.flip();
					
					int	written = chan2.write( read_buffer );
									
					if ( chan1 == source_channel ){
						
						outward_bytes += written;
						
					}else{
						
						inward_bytes += written;
					}
					
					if ( read_buffer.hasRemaining()){
						
						connection.cancelReadSelect( chan1 );
						
						connection.requestWriteSelect( chan2 );
						
					}else{
						
						read_buffer.position(0);
						
						read_buffer.limit( read_buffer.capacity());
					}
				}
			}
		}
		
		protected void
		writeSupport(
			SocketChannel 		sc )
		
			throws IOException
		{
				// socket SX -> SY via BX
				// so if SX = source_channel then BX is target buffer
			
			SocketChannel	chan1 = sc;
			SocketChannel	chan2 = sc==source_channel?target_channel:source_channel;
			
			ByteBuffer	read_buffer = sc==source_channel?target_buffer:source_buffer;
			
			int written = chan1.write( read_buffer );
						
			if ( chan1 == target_channel ){
				
				outward_bytes += written;
				
			}else{
				
				inward_bytes += written;
			}
			
			if ( read_buffer.hasRemaining()){
								
				connection.requestWriteSelect( chan1 );
				
			}else{
				
				read_buffer.position(0);
				
				read_buffer.limit( read_buffer.capacity());
				
				connection.requestReadSelect( chan2 );
			}
		}
		
		protected void
		trace()
		{
			if ( AESocksProxyConnectionImpl.TRACE ){
				
				LGLogger.log( getName() + ":" + getStateName() + "[out=" + outward_bytes +",in=" + inward_bytes +"] " + source_buffer + " / " + target_buffer );
			}
		}
	}
}
