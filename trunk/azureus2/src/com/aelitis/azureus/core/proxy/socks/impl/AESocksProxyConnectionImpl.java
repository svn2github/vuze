/*
 * Created on 08-Dec-2004
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
import org.gudy.azureus2.core3.util.HostNameToIPResolver;
import org.gudy.azureus2.core3.util.HostNameToIPResolverListener;

import com.aelitis.azureus.core.proxy.*;

/**
 * @author parg
 *
 */

public class 
AESocksProxyConnectionImpl
	implements AEProxyConnectionListener
{
	public static final boolean	TRACE	= false;
	
	protected AEProxyConnection		connection;
	protected SocketChannel			source_channel;
	protected SocketChannel			target_channel;
	
	protected
	AESocksProxyConnectionImpl(
		AEProxyConnection	_connection )
	{
		connection	= _connection;
		
		connection.addListener( this );
		
		source_channel	= connection.getSourceChannel();
		
		if ( TRACE ){
			
			LGLogger.log( "AESocksProxyProcessor: " + getName());
		}
	}
	
	protected String
	getName()
	{
		String	name = connection.getName();
		
		if ( target_channel != null ){
			
			name += target_channel.socket().getInetAddress() + ":" + target_channel.socket().getPort();
		}
		
		return( name );
	}
	
	protected AEProxyState
	getInitialState()
	{
		return( new proxyStateVersion());
	}
	
	public void
	connectionClosed(
		AEProxyConnection	con )
	{
		if ( target_channel != null ){
			
			try{
				con.cancelReadSelect( target_channel );
				
				target_channel.close();
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}	
	}
	
	protected class
	proxyState
		implements AEProxyState
	{
		protected ByteBuffer	buffer;

		protected
		proxyState()
		{
			trace();
		}
		
		public String
		getStateName()
		{
			String	state = this.getClass().getName();
			
			int	pos = state.indexOf( "$");
			
			state = state.substring(pos+1);
			
			return( state );
		}
		
		public final void
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
			throw( new IOException( "Read not supported: " + sc ));
		}
		
		public final void
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
			throw( new IOException( "Write not supported: " + sc ));
		}	
		
		public final void
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
			throw( new IOException( "Connect not supported: " + sc ));
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
			connection.setReadState( this );
			
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
			
			if ( version == 5 ){
			
				new proxyStateV5MethodNumber();
				
			}else if ( version == 4 ){
				
				new proxyStateV4Request();
				
			}else{
				
				throw( new IOException( "Unsupported version " + version ));

			}
		}
	}
	
		// V4
	
	protected class
	proxyStateV4Request
		extends proxyState
	{
		boolean		got_header;
		
		protected int		port;
		protected byte[]	address;
		
		protected
		proxyStateV4Request()
		{
			connection.setReadState( this );

			buffer	= ByteBuffer.allocate(7);
		}
	
		protected void
		readSupport(
			SocketChannel 		sc )
		
			throws IOException
		{
			/*
			+----+----+----+----+----+----+----+----+----+----+....+----+
			| VN | CD | DSTPORT |      DSTIP        | USERID       |NULL|
			+----+----+----+----+----+----+----+----+----+----+....+----+
			# of bytes:	   1    1      2              4           variable       1
			*/

			if ( sc.read( buffer ) == -1 ){
				
				throw( new IOException( "read channel shutdown" ));
			}
			
			if ( buffer.hasRemaining()){
				
				return;
			}
			
			buffer.flip();
			
			if ( got_header ){
				
				if ( buffer.get() == (byte)0){
				
						// end of play
		
					if (	address[0] == 0 &&
							address[1] == 0 &&
							address[2] == 0 &&
							address[3] != 0 ){
						
							// socks 4a
						
						new proxyStateV4aRequest( port );
						
					}else{
																	
						new proxyStateRelayConnect( new InetSocketAddress( InetAddress.getByAddress( address ), port ), 4);
					}
				}else{
				
					// drop the user id byte
				}
			}else{
				
				got_header	= true;
				
				byte	command	= buffer.get();
				
				if ( command != 1 ){
					
					throw( new IOException( "SocksV4: only CONNECT supported" ));
				}
				
				port = (((int)buffer.get() & 0xff) << 8 ) + ((int)buffer.get() & 0xff);

				address = new byte[4];
				
				for (int i=0;i<address.length;i++){
				
					address[i] = buffer.get();
				}
				
					// prepare for user id
				
				buffer = ByteBuffer.allocate(1);
			}
		}
	}
	
	protected class
	proxyStateV4aRequest
		extends proxyState
	{
		protected String	dns_address;
		protected int		port;
		
		protected
		proxyStateV4aRequest(
			int		_port )
		{
			port		= _port;
			dns_address	= "";
			
			connection.setReadState( this );

			buffer	= ByteBuffer.allocate(1);
		}
	
		protected void
		readSupport(
			final SocketChannel 		sc )
		
			throws IOException
		{
				// dns name follows, null terminated

			if ( sc.read( buffer ) == -1 ){
				
				throw( new IOException( "read channel shutdown" ));
			}
			
			if ( buffer.hasRemaining()){
				
				return;
			}
			
			buffer.flip();
							
			byte data = buffer.get();
			
			if ( data == 0 ){
				
				final String	f_dns_address	= dns_address;
				
				connection.cancelReadSelect( sc );
				
				HostNameToIPResolver.addResolverRequest(
					dns_address,
					new HostNameToIPResolverListener()
					{
						public void
						hostNameResolutionComplete(
							InetAddress	address )
						{
							if ( address == null ){
								
								connection.failed( new Exception( "DNS lookup of '" + f_dns_address + "' fails" ));
								
							}else{
								
								try{
									new proxyStateRelayConnect( new InetSocketAddress( address, port ), 4);
									
										// re-activate the read select suspended while resolving
									
									connection.requestReadSelect( sc );
									
								}catch ( IOException e ){
									
									connection.failed(e);
								}
							}
						}
					});				
			}else{
				
				dns_address += (char)data;
				
				if ( dns_address.length() > 1024 ){
					
					throw( new IOException( "DNS name too long" ));
				}
				
					// ready for next byte
				
				buffer.flip();
			}
		}
	}
	
	protected class
	proxyStateV4Reply
		extends proxyState
	{
		protected
		proxyStateV4Reply()
		
			throws IOException
		{		
			/*
			+----+----+----+----+----+----+----+----+
			| VN | CD | DSTPORT |      DSTIP        |
			+----+----+----+----+----+----+----+----+
			# of bytes:	   1    1      2              4
			*/

			connection.setWriteState( this );
			
			byte[]	addr = target_channel.socket().getInetAddress().getAddress();
			int		port = target_channel.socket().getPort();
			
			buffer	= ByteBuffer.wrap(
					new byte[]{	(byte)0,(byte)90,
								(byte)((port>>8)&0xff), (byte)(port&0xff),
								addr[0],addr[1],addr[2],addr[3]});
					
			
			write( source_channel );
		}
		
		protected void
		writeSupport(
			SocketChannel 		sc )
		
			throws IOException
		{
			sc.write( buffer );
			
			if ( buffer.hasRemaining()){
				
				connection.requestWriteSelect( sc );
				
			}else{
	
				new proxyStateRelayData();
			}
		}
	}
	
		// V5
	
	protected class
	proxyStateV5MethodNumber
		extends proxyState
	{
		
		protected
		proxyStateV5MethodNumber()
		{
			connection.setReadState( this );

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
			connection.setReadState( this );

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
			
			connection.setWriteState( this );
			
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
				
				connection.requestWriteSelect( sc );
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
			connection.setReadState( this );
			
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
			
			buffer.get();		// version
			
			int	command			= buffer.get();
			
			buffer.get();		// reserved
			
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
			connection.setReadState( this );
			
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
			connection.setReadState( this );
			
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
				
				connection.cancelReadSelect( sc );
				
				HostNameToIPResolver.addResolverRequest(
					dns_address,
					new HostNameToIPResolverListener()
					{
						public void
						hostNameResolutionComplete(
							InetAddress	address )
						{
							if ( address == null ){
								
								connection.failed( new Exception( "DNS lookup of '" + f_dns_address + "' fails" ));
								
							}else{
								
								new proxyStateV5RequestPort( address);
								
								connection.requestReadSelect( sc );
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
			address	= _address;
			
			connection.setReadState( this );
			
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
						
			int	port = (((int)buffer.get() & 0xff) << 8 ) + ((int)buffer.get() & 0xff);
			
			new proxyStateRelayConnect( new InetSocketAddress( address, port ), 5);
		}
	}
	
	protected class
	proxyStateRelayConnect
		extends proxyState
	{
		protected InetSocketAddress	address;
		protected int				socks_version;
		
		protected
		proxyStateRelayConnect(
			InetSocketAddress	_address,
			int					_socks_version )
		
			throws IOException
		{
			address			= _address;
			socks_version	= _socks_version;
			
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
	           
			if( socks_version == 4 ){
				
				new proxyStateV4Reply();
				
			}else{
				
				new proxyStateV5Reply();
			}
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
			connection.setWriteState( this );
			
			byte[]	addr = target_channel.socket().getLocalAddress().getAddress();
			int		port = target_channel.socket().getLocalPort();
			
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
				
				connection.requestWriteSelect( sc );
				
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
				
				new proxyStateClose();
				
			}else{
				
				if ( read_buffer.position() > 0 ){
					
					read_buffer.flip();
					
					chan2.write( read_buffer );
										
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
			
			chan1.write( read_buffer );
						
			if ( read_buffer.hasRemaining()){
								
				connection.requestWriteSelect( chan1 );
				
			}else{
				
				read_buffer.position(0);
				
				read_buffer.limit( read_buffer.capacity());
				
				connection.requestReadSelect( chan2 );
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
			connection.close();
			
			connection.setReadState( null);
			connection.setWriteState( null);
			connection.setConnectState( null);
		}
	}
}
