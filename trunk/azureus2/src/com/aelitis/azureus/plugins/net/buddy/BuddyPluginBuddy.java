/*
 * Created on Apr 1, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.plugins.net.buddy;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;

import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.messaging.MessageException;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageConnection;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageConnectionListener;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageEndpoint;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageRegistration;
import org.gudy.azureus2.plugins.utils.PooledByteBuffer;
import org.gudy.azureus2.plugins.utils.security.SEPublicKey;
import org.gudy.azureus2.plugins.utils.security.SEPublicKeyLocator;
import org.gudy.azureus2.plugins.utils.security.SESecurityManager;


public class 
BuddyPluginBuddy 
{
	private static final int RT_REQUEST_DATA	= 1;
	private static final int RT_REPLY_DATA		= 2;
	private static final int RT_REQUEST_PING	= 3;
	private static final int RT_REPLY_PING		= 4;
	
	private static final int RT_REPLY_ERROR		= 99;

	private BuddyPlugin		plugin;
	private String			public_key;
	
	private long			post_time;
	private InetAddress		ip;
	private int				tcp_port;
	private int				udp_port;
		
	private long			last_status_check_time;
	
	private volatile boolean			check_active;
		
	private List						connections	= new ArrayList();
	private List						messages	= new ArrayList();
	
	protected
	BuddyPluginBuddy(
		BuddyPlugin	_plugin,
		String		_pk )
	{
		plugin		= _plugin;
		public_key 	= _pk;
	}
	
	public String
	getPublicKey()
	{
		return( public_key );
	}
	
	public boolean
	hasPublicKey(
		SEPublicKey	other_key )
	{
		String	other_key_str = Base32.encode( other_key.encodeRawPublicKey());
		
		return( other_key_str.equals( public_key ));
	}
	
	public InetAddress
	getIP()
	{
		return( ip );
	}
	
	public int
	getTCPPort()
	{
		return( tcp_port );
	}
	
	public int
	getUDPPort()
	{
		return( udp_port );
	}
	
	protected void
	ping()
	{
		try{
			sendMessage(
				RT_REQUEST_PING,
				"ping".getBytes(),
				60*1000,
				new BuddyPluginBuddyReplyListener()
				{
					public void
					replyReceived(
						BuddyPluginBuddy	from_buddy,
						byte[]				content )
					{
						log( "Ping received:" + new String( content ));
					}
					
					public void
					sendFailed(
						BuddyPluginBuddy		to_buddy,
						BuddyPluginException	cause )
					{
						log( "Ping failed to " + getString(), cause );
					}
				});
			
		}catch( Throwable e ){
			
			log( "Ping failed to " + getString(), e );
		}
	}
	
	public void
	sendMessage(
		byte[]							content,
		int								timeout_millis,
		BuddyPluginBuddyReplyListener	listener )
	
		throws BuddyPluginException
	{
		sendMessage( RT_REQUEST_DATA, content, timeout_millis, listener );
	}
	
	protected void
	sendMessage(
		int								type,
		byte[]							content,
		int								timeout_millis,
		BuddyPluginBuddyReplyListener	listener )
	
		throws BuddyPluginException
	{
		buddyMessage	message = new buddyMessage( type, content, timeout_millis, listener );
		
		buddyConnection connection;

		synchronized( this ){
			
			messages.add( message );
						
			if ( connections.size() == 0 ){
				
				try{
					GenericMessageConnection generic_connection = outgoingConnection();
					
					connection = new buddyConnection( generic_connection , true );
					
					connections.add( connection );
					
				}catch( Throwable e ){
			
					message.reportFailed( e );
					
					messages.remove( message );
					
					return;
				}
			}else{
				
				connection = (buddyConnection)connections.get(0);
			}
		}
		
		connection.sendMessage();
	}
	
	protected long
	getLastStatusCheckTime()
	{
		return( last_status_check_time );
	}
	
	protected boolean
	statusCheckActive()
	{
		return( check_active );
	}
	
	protected void
	statusCheckStarts()
	{
		last_status_check_time = SystemTime.getCurrentTime();
		
		check_active = true;
	}
	
	protected void
	statusCheckFailed()
	{
		plugin.logMessage( public_key + ": offline" );
		
		check_active = false;
	}
	
	protected void
	statusCheckComplete(
		long			_post_time,
		InetAddress		_ip,
		int				_tcp_port,
		int				_udp_port )
	{
		post_time	= _post_time;
		ip			= _ip;
		tcp_port	= _tcp_port;
		udp_port	= _udp_port;
		
		plugin.logMessage( public_key + ": online - ip=" + ip + ",tcp=" + tcp_port + ",udp=" + udp_port + ",age=" + (SystemTime.getCurrentTime() - post_time ));
		
		check_active = false;
	}
	
	protected void
	incomingConnection(
		GenericMessageConnection	_connection )
	{
		addConnection( _connection );
	}
	
	protected void
	addConnection(
		GenericMessageConnection		_connection )
	{
		synchronized( this ){
			
			connections.add( new buddyConnection( _connection, false ));
		}
	}
	
	protected void
	removeConnection(
		buddyConnection			_connection )
	{
		synchronized( this ){
			
			connections.remove( _connection );
		}
	}
	
	protected GenericMessageConnection
	outgoingConnection()
	
		throws BuddyPluginException
	{
		GenericMessageRegistration msg_registration = plugin.getMessageRegistration();

		if ( msg_registration == null ){
						
			throw( new BuddyPluginException( "Messaging system unavailable" ));
		}
		
		InetAddress ip = getIP();
		
		if ( ip == null ){
						
			throw( new BuddyPluginException( "Buddy offline (no usable IP address)" ));
		}
		
		InetSocketAddress	tcp_target	= null;
		InetSocketAddress	udp_target	= null;
		
		int	tcp_port = getTCPPort();
		
		if ( tcp_port > 0 ){
			
			tcp_target = new InetSocketAddress( ip, tcp_port );
		}
		
		int	udp_port = getUDPPort();
		
		if ( udp_port > 0 ){
			
			udp_target = new InetSocketAddress( ip, udp_port );
		}

		InetSocketAddress	notional_target = tcp_target;
		
		if ( notional_target == null ){
		
			notional_target = udp_target;
		}
		
		if ( notional_target == null ){
						
			throw( new BuddyPluginException( "Buddy offline (no usable protocols)" ));
		}
		
		GenericMessageEndpoint	endpoint = msg_registration.createEndpoint( notional_target );
		
		if ( tcp_target != null ){
		
			endpoint.addTCP( tcp_target );
		}
		
		if ( udp_target != null ){
		
			endpoint.addUDP( udp_target );
		}
				
		GenericMessageConnection	con = null;
		
		try{
			con = msg_registration.createConnection( endpoint );
					
			String reason = "Buddy: Outgoing connection establishment";
	
			SESecurityManager sec_man = plugin.getSecurityManager();
			
			con = sec_man.getSTSConnection( 
					con, 
					sec_man.getPublicKey( SEPublicKey.KEY_TYPE_ECC_192, reason ),
	
					new SEPublicKeyLocator()
					{
						public boolean
						accept(
							Object		context,
							SEPublicKey	other_key )
						{
							if ( hasPublicKey( other_key )){
								
								return( true );
								
							}else{
								
								log( getString() + ": connection failed due to pk mismatch" );
							
								return( false );
							}
						}
					},
					reason, 
					SESecurityManager.BLOCK_ENCRYPTION_AES );		
					
			con.connect();
			
			return( con );
			
		}catch( Throwable e ){
			
			if ( con != null ){
				
				try{
					con.close();
					
				}catch( Throwable f ){
					
					log( "Failed to close connection", f );
				}
			}
			
			throw( new BuddyPluginException( "Failed to send message", e ));
		}
	}
	
	protected void
	log(
		String		str )
	{
		plugin.log( str );
	}
	
	protected void
	log(
		String		str,
		Throwable 	e )
	{
		plugin.log( str, e );
	}
	
	public String
	getString()
	{
		return( "pk=" + public_key + ",ip=" + ip + ",tcp=" + tcp_port + ",udp=" + udp_port );
	}

	protected class
	buddyMessage
	{
		private int									type;
		private byte[]								request;
		private BuddyPluginBuddyReplyListener		listener;
		private int									timeout;
		
		private boolean	complete;
		
		protected
		buddyMessage(
			int									_type,
			byte[]								_request,
			int									_timeout,
			BuddyPluginBuddyReplyListener		_listener )
		{
			type		= _type;
			request		= _request;
			listener	= _listener;
			timeout		= _timeout;
		}
		
		protected int
		getType()
		{
			return( type );
		}
		
		protected byte[]
		getRequest()
		{
			return( request );
		}
		
		protected void
		reportComplete(
			byte[]		reply )
		{
			synchronized( this ){
				
				if ( complete ){
					
					return;
				}
				
				complete = true;
			}
			
			listener.replyReceived(  BuddyPluginBuddy.this, reply );
		}
		
		protected void
		reportFailed(
			Throwable	error )
		{
			synchronized( this ){
				
				if ( complete ){
					
					return;
				}
				
				complete = true;
			}
			
			if ( error instanceof BuddyPluginException ){
				
				listener.sendFailed( BuddyPluginBuddy.this, (BuddyPluginException)error );
				
			}else{
			
				listener.sendFailed(  BuddyPluginBuddy.this, new BuddyPluginException( "Unexpected error",  error ));
			}
		}
	}
	
	protected class
	buddyConnection
		implements GenericMessageConnectionListener
	{
		private GenericMessageConnection		connection;
		private boolean							outgoing;
		
		private String							dir_str;
		
		private boolean			connected;
		private buddyMessage	active_message;
		private boolean			failed;
		
		protected
		buddyConnection(
			GenericMessageConnection		_connection,
			boolean							_outgoing )
		{
			connection 	= _connection;
			outgoing	= _outgoing;
			
			dir_str = outgoing?"Outgoing":"Incoming";
			
			if ( !outgoing ){
				
				connected = true;
			}
			
			connection.addListener( this );
		}
		
		protected void
		sendMessage()
		{
			boolean	send = false;
			
			synchronized( BuddyPluginBuddy.this ){
				
				if ( connected && active_message == null && messages.size() > 0 ){
					
					active_message = (buddyMessage)messages.remove(0);
					
					send	= true;
				}
			}
			
			if ( send ){
			
				byte[] request = active_message.getRequest();
				
				Map	map = new HashMap();
				
				map.put( "type", new Long( active_message.getType()));
				map.put( "data", request );
				
				PooledByteBuffer	buffer	= null;
				
				try{
					byte[] data = BEncoder.encode( map );
					
					buffer = 
						plugin.getPluginInterface().getUtilities().allocatePooledByteBuffer( data );
				
					connection.send( buffer );
				
				}catch( Throwable e ){
				
					if ( buffer != null ){
						
						buffer.returnToPool();
					}
					
					try{			
						failed( connection, e );
					
					}catch( Throwable f ){
					
						Debug.printStackTrace(f);
					}
				}
			}
		}
		
		public void
		connected(
			GenericMessageConnection	connection )
		{
			System.out.println( dir_str + " connected" );
			
			synchronized( this ){
				
				connected = true;
			}
			
			sendMessage();
		}
		
		public void
		receive(
			GenericMessageConnection	connection,
			PooledByteBuffer			request_buffer )
		
			throws MessageException
		{
			try{
				byte[]	content = request_buffer.toByteArray();
				
				System.out.println( dir_str + " receive: " + content.length );
				
				Map	request_map = BDecoder.decode( content );
				
				int	type = ((Long)request_map.get("type")).intValue();
				
				if ( type == RT_REQUEST_DATA || type == RT_REQUEST_PING ){
					
					byte[]	reply;
					
					int	reply_type;
					
					if ( type == RT_REQUEST_PING ){
						
						reply = "pong".getBytes();
											
						reply_type = RT_REPLY_PING;
						
					}else{
						
						byte[]	data = (byte[])request_map.get( "data" );

						if ( data == null ){
							
							reply	= null;
							
						}else{
							
							reply = plugin.requestReceived( BuddyPluginBuddy.this, data );
						}
						
						if ( reply == null ){
							
							reply_type = RT_REPLY_ERROR;
							
							reply	= "No handlers available to process request".getBytes();
							
						}else{
							
							reply_type = RT_REPLY_DATA;
						}
					}
					
					Map reply_map = new HashMap();
					
					reply_map.put( "type", new Long( reply_type ));
					reply_map.put( "data", reply );
					
					PooledByteBuffer	reply_buffer = 
						plugin.getPluginInterface().getUtilities().allocatePooledByteBuffer( BEncoder.encode( reply_map ));
						
					boolean	ok = false;
					
					try{
					
						connection.send( reply_buffer );
						
						ok = true;
						
					}finally{
						
						if ( !ok ){
							
							reply_buffer.returnToPool();
						}
					}
					
				}else if ( type == RT_REPLY_DATA || type == RT_REPLY_PING || type == RT_REPLY_ERROR ){
					
					buddyMessage	bm;
					
					synchronized( this ){
						
						bm = active_message;

						active_message = null;
					}
					
					if ( type == RT_REPLY_ERROR ){
						
						bm.reportFailed( new BuddyPluginException(new String((byte[])request_map.get( "data" ))));
						
					}else{
						
						bm.reportComplete((byte[])request_map.get( "data" ));
					}
					
					sendMessage();
					
				}else{
					
						// ignore unknown message types
					
					sendMessage();
				}
			}catch( Throwable e ){
				
				failed( connection, e );
				
			}finally{
				
				request_buffer.returnToPool();
			}
		}
		
		public void
		failed(
			GenericMessageConnection	connection,
			Throwable 					error )
		
			throws MessageException
		{
			synchronized( this ){
				
				if ( failed ){
					
					return;
				}
				
				failed = true;
			}
			
			try{
				System.out.println( dir_str + " connection error:" );
				
				error.printStackTrace();
				
				connection.close();
				
			}finally{
				
					// TODO: what to do if queued messages
				
				removeConnection( this );
			}
		}
	}
}
