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

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.util.*;

/**
 * @author parg
 *
 */

public class 
AEProxyProcessor 
{
	public static final boolean	TRACE	= false;
	
	protected AEProxyImpl		server;
	protected SocketChannel		source_channel;
	protected SocketChannel		target_channel;


	protected volatile proxyState	proxy_read_state 		= null;
	protected volatile proxyState	proxy_write_state 		= null;
	protected volatile proxyState	proxy_connect_state 	= null;
	
	protected
	AEProxyProcessor(
		AEProxyImpl			_server,
		SocketChannel		_socket )
	{
		server			= _server;
		source_channel	= _socket;
		
		if ( TRACE ){
			
			LGLogger.log( "ProxyProcessor: " + getName());
		}
		
		proxy_read_state = new proxyStateVersion();
	}
	
	protected String
	getName()
	{
		String	name = source_channel.socket().getInetAddress() + ":" + source_channel.socket().getPort() + " -> ";
		
		if ( target_channel != null ){
			
			name += target_channel.socket().getInetAddress() + ":" + target_channel.socket().getPort();
		}
		
		return( name );
	}
	
	protected void
	read(
		SocketChannel 		sc )
	{
		try{
			proxy_read_state.read(sc);
			
		}catch( Throwable e ){
			
			failed(sc,e);
		}
	}
	
	protected void
	write(
		SocketChannel 		sc )
	{
		try{
			proxy_write_state.write(sc);
			
		}catch( Throwable e ){
			
			failed(sc,e);
		}
	}
	
	protected void
	connect(
		SocketChannel 		sc )
	{
		try{
			proxy_connect_state.connect(sc);
			
		}catch( Throwable e ){
			
			failed(sc,e);
		}
	}
		
	
	protected void
	requestWriteSelect(
		SocketChannel 		sc )
	{
		server.requestWriteSelect( this, sc );
	}
	
	protected void
	requestConnectSelect(
		SocketChannel 		sc )
	{
		server.requestConnectSelect( this, sc );
	}
	
	protected void
	requestReadSelect(
		SocketChannel 		sc )
	{
		server.requestReadSelect( this, sc );
	}
	
	protected void
	cancelReadSelect(
		SocketChannel 		sc )
	{
		server.cancelReadSelect( this, sc );
	}
	
	protected void
	failed(
		SocketChannel 		sc,
		Throwable			reason )
	{
		try{
			LGLogger.log( reason );
			
			sc.close();
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	protected void
	close()
	{
		try{
			cancelReadSelect( source_channel );
			
			source_channel.close();
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
		
		if ( target_channel != null ){
			
			try{
				cancelReadSelect( target_channel );
				
				target_channel.close();
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}	
	}
	
	protected class
	proxyState
	{
		protected ByteBuffer	buffer;

		protected
		proxyState()
		{
			trace();
		}
		
		protected String
		getStateName()
		{
			String	state = this.getClass().getName();
			
			int	pos = state.indexOf( "$");
			
			state = state.substring(pos+1);
			
			return( state );
		}
		
		protected final void
		read(
			SocketChannel 		sc )
		
			throws IOException
		{
			try{
				readSupport(sc);
				
			}finally{
				
				trace();
			}
		}
		
		protected void
		readSupport(
			SocketChannel 		sc )
		
			throws IOException
		{
			throw( new IOException( "Read not supported" ));
		}
		
		protected final void
		write(
			SocketChannel 		sc )
		
			throws IOException
		{
			try{
				writeSupport(sc);
				
			}finally{
				
				trace();
			}		
		}	
		
		protected void
		writeSupport(
			SocketChannel 		sc )
		
			throws IOException
		{
			throw( new IOException( "Write not supported" ));
		}	
		
		protected final void
		connect(
			SocketChannel 		sc )
		
			throws IOException
		{
			try{
				connectSupport(sc);
				
			}finally{
				
				trace();
			}
		}	
		
		protected void
		connectSupport(
			SocketChannel 		sc )
		
			throws IOException
		{
			throw( new IOException( "Connect not supported" ));
		}	
		
		protected void
		trace()
		{
			if ( TRACE ){
				
				LGLogger.log( getName() + ":" + getStateName() + ", " + buffer );
			}
		}
	}
	
	
	
	protected class
	proxyStateVersion
		extends proxyState
	{
		
		protected
		proxyStateVersion()
		{
			proxy_read_state	= this;
			
			buffer	= ByteBuffer.allocate(1);
		}
		
		protected void
		readSupport(
			SocketChannel 		sc )
		
			throws IOException
		{
			if ( sc.read( buffer ) == -1 ){
				
				throw( new IOException( "read channel shutdown" ));
			}
			
			if ( buffer.hasRemaining()){
				
				return;
			}
			
			buffer.flip();
			
			int	version	= buffer.get();
			
			if ( version != 5 ){
				
				throw( new IOException( "Only version 5 supported"));
			}
			
			new proxyStateV5MethodNumber();
		}
	}
	
	protected class
	proxyStateV5MethodNumber
		extends proxyState
	{
		
		protected
		proxyStateV5MethodNumber()
		{
			proxy_read_state	= this;

			buffer	= ByteBuffer.allocate(1);
		}
		
		protected void
		readSupport(
			SocketChannel 		sc )
		
			throws IOException
		{
			if ( sc.read( buffer ) == -1 ){
				
				throw( new IOException( "read channel shutdown" ));
			}
			
			if ( buffer.hasRemaining()){
				
				return;
			}
			
			buffer.flip();
			
			int	num_methods	= buffer.get();
			
			new proxyStateV5Methods(num_methods);
		}
	}
	
	protected class
	proxyStateV5Methods
		extends proxyState
	{
		
		protected
		proxyStateV5Methods(
			int		methods )
		{
			super();
			
			proxy_read_state	= this;

			buffer	= ByteBuffer.allocate(methods);
		}
		
		protected void
		readSupport(
			SocketChannel 		sc )
		
			throws IOException
		{
			if ( sc.read( buffer ) == -1 ){
				
				throw( new IOException( "read channel shutdown" ));
			}
			
			if ( buffer.hasRemaining()){
				
				return;
			}
			
				// we just ignore actual method values
			
			new proxyStateV5MethodsReply();
		}
	}
	
	protected class
	proxyStateV5MethodsReply
		extends proxyState
	{
		
		protected
		proxyStateV5MethodsReply()
		
			throws IOException
		{
			new proxyStateV5Request();
			
			proxy_write_state	= this;
			
			buffer	= ByteBuffer.wrap(new byte[]{(byte)5,(byte)0});
			
			write( source_channel );
		}
		
		protected void
		writeSupport(
			SocketChannel 		sc )
		
			throws IOException
		{
			sc.write( buffer );
			
			if ( buffer.hasRemaining()){
				
				requestWriteSelect( sc );
			}
		}
	}
	
	/*
    +----+-----+-------+------+----------+----------+
    |VER | CMD |  RSV  | ATYP | DST.ADDR | DST.PORT |
    +----+-----+-------+------+----------+----------+
    | 1  |  1  | X'00' |  1   | Variable |    2     |
    +----+-----+-------+------+----------+----------+

		Where:

	          o  VER    protocol version: X'05'
	          o  CMD
	             o  CONNECT X'01'
	             o  BIND X'02'
	             o  UDP ASSOCIATE X'03'
	          o  RSV    RESERVED
	          o  ATYP   address type of following address
	             o  IP V4 address: X'01'
	             o  DOMAINNAME: X'03'
	             o  IP V6 address: X'04'
	          o  DST.ADDR       desired destination address
	          o  DST.PORT desired destination port in network octet
	             order
	             */
	
	protected class
	proxyStateV5Request
		extends proxyState
	{
		
		protected
		proxyStateV5Request()
		{
			proxy_read_state	= this;
			
			buffer	= ByteBuffer.allocate(4);
		}
		
		protected void
		readSupport(
			SocketChannel 		sc )
		
			throws IOException
		{
			if ( sc.read( buffer ) == -1 ){
				
				throw( new IOException( "read channel shutdown" ));
			}
			
			if ( buffer.hasRemaining()){
				
				return;
			}
			
			buffer.flip();
			
			int	version 		= buffer.get(); 
			int	command			= buffer.get();
			int	reserved 		= buffer.get();
			int address_type	= buffer.get();
			
			if ( command != 1 ){
				
				throw( new IOException( "V5: Only connect supported"));
			}
			
			if ( address_type == 1 ){
			
				new proxyStateV5RequestIP();
				
			}else if ( address_type == 3 ){
				
				new proxyStateV5RequestDNS();
				
			}else{
				
				throw( new IOException( "V5: Unsupported address type" ));
			}
		}
	}
	
	protected class
	proxyStateV5RequestIP
		extends proxyState
	{
		
		protected
		proxyStateV5RequestIP()
		{
			proxy_read_state	= this;
			
			buffer	= ByteBuffer.allocate(4);
		}
		
		protected void
		readSupport(
			SocketChannel 		sc )
		
			throws IOException
		{
			if ( sc.read( buffer ) == -1 ){
				
				throw( new IOException( "read channel shutdown" ));
			}
			
			if ( buffer.hasRemaining()){
				
				return;
			}
			
			buffer.flip();
			
			byte[]	bytes = new byte[4];
			
			buffer.get( bytes );
			
			InetAddress inet_address = InetAddress.getByAddress( bytes );
			
			new proxyStateV5RequestPort( inet_address );
		}
	}
	
	protected class
	proxyStateV5RequestDNS
		extends proxyState
	{
		boolean	got_length	= false;
		
		protected
		proxyStateV5RequestDNS()
		{
			proxy_read_state	= this;
			
			buffer	= ByteBuffer.allocate(1);
		}
		
		protected void
		readSupport(
			final SocketChannel 		sc )
		
			throws IOException
		{
			if ( sc.read( buffer ) == -1 ){
				
				throw( new IOException( "read channel shutdown" ));
			}
			
			if ( buffer.hasRemaining()){
				
				return;
			}
			
			buffer.flip();
			
			if ( !got_length){
				
				int	length = buffer.get();
				
				buffer = ByteBuffer.allocate( length );
				
				got_length	= true;
				
			}else{
				
				String dns_address = "";
				
				while( buffer.hasRemaining()){
				
					dns_address += (char)buffer.get();
				}
					
				final String	f_dns_address	= dns_address;
				
				cancelReadSelect( sc );
				
				HostNameToIPResolver.addResolverRequest(
					dns_address,
					new HostNameToIPResolverListener()
					{
						public void
						hostNameResolutionComplete(
							InetAddress	address )
						{
							if ( address == null ){
								
								failed( source_channel, new Exception( "DNS lookup of '" + f_dns_address + "' fails" ));
								
							}else{
								
								new proxyStateV5RequestPort( address);
								
								requestReadSelect( sc );
							}
						}
					});
			}
		}
	}
	
	protected class
	proxyStateV5RequestPort
		extends proxyState
	{
		protected InetAddress	address;
		
		protected
		proxyStateV5RequestPort(
			InetAddress	_address )
		{
			super();
			
			address	= _address;
			
			proxy_read_state	= this;
			
			buffer	= ByteBuffer.allocate(2);
		}
		
		protected void
		readSupport(
			SocketChannel 		sc )
		
			throws IOException
		{
			if ( sc.read( buffer ) == -1 ){
				
				throw( new IOException( "read channel shutdown" ));
			}
			
			if ( buffer.hasRemaining()){
				
				return;
			}
			
			buffer.flip();
			
				// OK, we're almost ready to roll. Unregister the read select until we're 
				// connected
			
			cancelReadSelect( sc );
			
			int	port = (((int)buffer.get() & 0xff) << 8 ) + ((int)buffer.get() & 0xff);
			
			new proxyStateRelayConnect( new InetSocketAddress( address, port ));
		}
	}
	
	protected class
	proxyStateRelayConnect
		extends proxyState
	{
		protected InetSocketAddress	address;
		
		protected
		proxyStateRelayConnect(
			InetSocketAddress	_address )
		
			throws IOException
		{
			address	= _address;
			
			proxy_connect_state	= this;
			
			target_channel = SocketChannel.open();
			
		    String bindIP = COConfigurationManager.getStringParameter("Bind IP", "");
		    
	        if ( bindIP.length() > 6 ){
	        	
	        	target_channel.socket().bind( new InetSocketAddress( InetAddress.getByName( bindIP ), 0 ) );
	        }
	        
	        target_channel.configureBlocking( false );
	        
	        target_channel.connect( address );
	   
	        requestConnectSelect( target_channel );
		}
		
		protected void
		connectSupport(
			SocketChannel 		sc )
		
			throws IOException
		{
			if( !sc.finishConnect()){
				
				throw( new IOException( "finishConnect returned false" ));
			}
	           
			new proxyStateV5Reply();
		}
	}
	
	/*
    +----+-----+-------+------+----------+----------+
    |VER | REP |  RSV  | ATYP | BND.ADDR | BND.PORT |
    +----+-----+-------+------+----------+----------+
    | 1  |  1  | X'00' |  1   | Variable |    2     |
    +----+-----+-------+------+----------+----------+

 Where:

      o  VER    protocol version: X'05'
      o  REP    Reply field:
         o  X'00' succeeded
         o  X'01' general SOCKS server failure
         o  X'02' connection not allowed by ruleset
         o  X'03' Network unreachable
         o  X'04' Host unreachable
         o  X'05' Connection refused
         o  X'06' TTL expired
         o  X'07' Command not supported
         o  X'08' Address type not supported
         o  X'09' to X'FF' unassigned
      o  RSV    RESERVED
      o  ATYP   address type of following address

         o  IP V4 address: X'01'
         o  DOMAINNAME: X'03'
         o  IP V6 address: X'04'
      o  BND.ADDR       server bound address
      o  BND.PORT       server bound port in network octet order
      */
	
	
	protected class
	proxyStateV5Reply
		extends proxyState
	{
		protected
		proxyStateV5Reply()
		
			throws IOException
		{					
			proxy_write_state	= this;
			
			byte[]	addr = target_channel.socket().getInetAddress().getAddress();
			int		port = target_channel.socket().getPort();
			
			buffer	= ByteBuffer.wrap(
					new byte[]{(byte)5,(byte)0,(byte)0,(byte)1,
								addr[0],addr[1],addr[2],addr[3],
								(byte)((port>>8)&0xff), (byte)(port&0xff)});
					
			
			write( source_channel );
		}
		
		protected void
		writeSupport(
			SocketChannel 		sc )
		
			throws IOException
		{
			sc.write( buffer );
			
			if ( buffer.hasRemaining()){
				
				requestWriteSelect( sc );
				
			}else{
	
				new proxyStateRelayData();
			}
		}
	}
	
	
	protected class
	proxyStateRelayData
		extends proxyState
	{
		protected ByteBuffer		source_buffer;
		protected ByteBuffer		target_buffer;
		
		protected
		proxyStateRelayData()
		
			throws IOException
		{
			super();
						
			source_buffer	= ByteBuffer.allocate( 1024 );
			target_buffer	= ByteBuffer.allocate( 1024 );
			
			proxy_read_state	= this;
			proxy_write_state	= this;
			
			requestReadSelect( source_channel );
			
			requestReadSelect( target_channel );	
		}
		
		protected void
		readSupport(
			SocketChannel 		sc )
		
			throws IOException
		{
			SocketChannel	chan1 = sc;
			SocketChannel	chan2 = sc==source_channel?target_channel:source_channel;
			
			ByteBuffer	read_buffer = sc==source_channel?source_buffer:target_buffer;
									
			int	len = chan1.read( read_buffer );
	
			if ( len == -1 ){
				
					//means that the channel has been shutdown
				
				new proxyStateClose();
				
			}else{
				
				if ( read_buffer.position() > 0 ){
					
					read_buffer.flip();
					
					chan2.write( read_buffer );
										
					if ( read_buffer.hasRemaining()){
						
						cancelReadSelect( chan1 );
						
						requestWriteSelect( chan2 );
						
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
			
			chan1.write( read_buffer );
						
			if ( read_buffer.hasRemaining()){
								
				requestWriteSelect( chan1 );
				
			}else{
				
				read_buffer.position(0);
				
				read_buffer.limit( read_buffer.capacity());
				
				requestReadSelect( chan2 );
			}
		}
	}
	
	protected class
	proxyStateClose
		extends proxyState
	{
		protected
		proxyStateClose()
		
			throws IOException
		{					
			close();
			
			proxy_read_state	= null;
			proxy_write_state	= null;
			proxy_connect_state	= null;
		}
	}
}
