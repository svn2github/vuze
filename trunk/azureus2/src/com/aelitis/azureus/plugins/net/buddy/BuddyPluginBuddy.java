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

import org.gudy.azureus2.core3.util.AEThread2;
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
	private static final boolean TRACE = BuddyPlugin.TRACE;
	
	private static final int CONNECTION_IDLE_TIMEOUT	= 5*60*1000;
	
	private static final int MAX_ACTIVE_CONNECTIONS		= 5;
	private static final int MAX_QUEUED_MESSAGES		= 256;
	
	private static final int RT_REQUEST_DATA	= 1;
	
	private static final int RT_REPLY_DATA		= 2;	
	private static final int RT_REPLY_ERROR		= 99;

	private static Random	random = new Random();
	
	private BuddyPlugin		plugin;
	private String			public_key;
	private String			nick_name;
	private List			recent_ygm;
	
	private long			post_time;
	private InetAddress		ip;
	private int				tcp_port;
	private int				udp_port;
		
	private boolean			online;
	private long			status_check_count;
	private long			last_status_check_time;
	
	private boolean			check_active;
		
	private List						connections	= new ArrayList();
	private List						messages	= new ArrayList();
	private buddyMessage				current_message;
	
	private int	next_connection_id;
	private int	next_message_id;
	
	protected
	BuddyPluginBuddy(
		BuddyPlugin	_plugin,
		String		_pk,
		String		_nick_name,
		List		_recent_ygm )
	{
		plugin		= _plugin;
		public_key 	= _pk;
		nick_name	= _nick_name;
		recent_ygm	= _recent_ygm;
	}
	
	public String
	getPublicKey()
	{
		return( public_key );
	}
	
	protected String
	getShortString()
	{
		return( public_key.substring( 0, 6 ) + ".." );
	}
	
	public String
	getNickName()
	{
		return( nick_name );
	}
	
	public String
	getName()
	{
		if ( nick_name != null ){
			
			return( nick_name );
		}
		
		return( public_key );
	}
	
	public void
	remove()
	{
		plugin.removeBuddy( this );
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
	
	public boolean
	isOnline()
	{
		return( online );
	}
	
	public void
	setMessagePending()
	{
		plugin.setMessagePending( this );
	}
	
	protected boolean
	addYGMMarker(
		long		marker )
	{
		Long	l = new Long( marker );
		
		synchronized( this ){
		
			if ( recent_ygm == null ){
				
				recent_ygm = new ArrayList();
			}
			
			if ( recent_ygm.contains( l )){
				
				return( false );
			}
			
			recent_ygm.add( l );
			
			if ( recent_ygm.size() > 16 ){
				
				recent_ygm.remove(0);
			}
		}
		
		plugin.setConfigDirty();
		
		return( true );
	}
	
	protected List
	getYGMMarkers()
	{
		return( recent_ygm );
	}
	
	public void
	ping()
	{
		try{
			Map	ping_request = new HashMap();
			
			ping_request.put( "type", new Long( BuddyPlugin.RT_INTERNAL_REQUEST_PING ));
			
			sendMessage(
				BuddyPlugin.SUBSYSTEM_INTERNAL,
				ping_request,
				60*1000,
				new BuddyPluginBuddyReplyListener()
				{
					public void
					replyReceived(
						BuddyPluginBuddy	from_buddy,
						Map					reply )
					{
						log( "Ping received:" + reply );
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
		final int								subsystem,
		final Map								content,
		final int								timeout_millis,
		final BuddyPluginBuddyReplyListener		listener )
	
		throws BuddyPluginException
	{
		plugin.checkAvailable();
		
		boolean	wait = false;
				
		if ( ip == null ){
			
			if ( check_active ){
				
				wait	= true;
				
			}else if ( SystemTime.getCurrentTime() - last_status_check_time > 30*1000 ){
				
				plugin.updateBuddyStatus( this );
				
				wait	= true;
			}
		}
		
		if ( wait ){
			
			new AEThread2( "BuddyPluginBuddy:sendWait", true )
			{
				public void
				run()
				{
					try{
						long	start = SystemTime.getCurrentTime();
						
						for (int i=0;i<20;i++){
							
							if ( ip != null ){
								
								break;
							}
							
							Thread.sleep( 1000 );
						}
						
						long	elapsed = SystemTime.getCurrentTime() - start;
						
						int new_tm = timeout_millis;
						
						if ( elapsed > 0 && timeout_millis > 0 ){
							
							new_tm -= elapsed;
							
							if ( new_tm <= 0 ){
								
								listener.sendFailed( BuddyPluginBuddy.this, new BuddyPluginException( "Timeout" ));
								
								return;
							}
						}
						
						sendMessageSupport( content, subsystem, new_tm, listener );
						
					}catch( Throwable e ){
						
						if ( e instanceof BuddyPluginException ){
							
							listener.sendFailed( BuddyPluginBuddy.this, (BuddyPluginException)e);
						}else{
						
							listener.sendFailed( BuddyPluginBuddy.this, new BuddyPluginException( "Send failed", e ));
						}
					}
				}				
			}.start();
			
		}else{
			
			sendMessageSupport( content, subsystem, timeout_millis, listener );
		}
	}
	
	protected void
	sendMessageSupport(
		final Map								content,
		final int								subsystem,
		final int								timeout_millis,
		final BuddyPluginBuddyReplyListener		original_listener )
	
		throws BuddyPluginException
	{
		boolean too_many_messages = false;
		
		synchronized( this ){
		
			too_many_messages = messages.size() >= MAX_QUEUED_MESSAGES;
		}
		
		if ( too_many_messages ){
			
			throw( new BuddyPluginException( "Too many messages queued" ));
		}
		
		final buddyMessage	message = new buddyMessage( subsystem, content, timeout_millis );
		
		BuddyPluginBuddyReplyListener	listener_delegate = 
			new BuddyPluginBuddyReplyListener()
			{
				public void
				replyReceived(
					BuddyPluginBuddy		from_buddy,
					Map						reply )
				{
					logMessage( "Msg " + message.getString() + " ok" );
					
					try{
						synchronized( BuddyPluginBuddy.this ){
							
							if ( current_message != message ){
								
								Debug.out( "Inconsistent" );
							}
							
							current_message = null;
						}
						
						original_listener.replyReceived( from_buddy, reply );
						
					}finally{
						
						dispatchMessage();
					}
				}
				
				public void
				sendFailed(
					BuddyPluginBuddy		to_buddy,
					BuddyPluginException	cause )
				{
					logMessage( "Msg " + message.getString() + " failed: " + Debug.getNestedExceptionMessage( cause ));

					try{
						synchronized( BuddyPluginBuddy.this ){
							
							if ( current_message != message ){
								
								Debug.out( "Inconsistent" );
							}
							
							current_message = null;
						}
				
						long	now = SystemTime.getCurrentTime();
						
						int	retry_count = message.getRetryCount();
						
						if ( retry_count < 1 && !message.timedOut( now )){
							
							message.setRetry();
							
							logMessage( "Msg " + message.getString() + " retrying" );

							synchronized( this ){
								
								messages.add( 0, message );
							}
						}else{
						
							original_listener.sendFailed( to_buddy, cause );
						}
					}finally{
							
						dispatchMessage();
					}
				}
			};
			
		message.setListener( listener_delegate );
			
		int	size;
		
		synchronized( this ){
			
			messages.add( message );
			
			size = messages.size();
		}
		
		logMessage( "Msg " + message.getString() + " added: num=" + size );
		
		dispatchMessage();
	}
	
	protected void
	dispatchMessage()
	{
		buddyConnection	bc = null;
		
		synchronized( this ){
	
			if ( current_message != null || messages.size() == 0 ){
				
				return;
			}
			
			current_message = (buddyMessage)messages.remove( 0 );
			
			for (int i=0;i<connections.size();i++){
				
				buddyConnection c = (buddyConnection)connections.get(i);
				
				if ( !c.hasFailed()){
					
					bc	= c;
				}
			}
			
			if ( bc == null ){
				
				try{
					if ( connections.size() >= MAX_ACTIVE_CONNECTIONS ){
						
						throw( new BuddyPluginException( "Too many active connections" ));
					}
					
					GenericMessageConnection generic_connection = outgoingConnection();
					
					bc = new buddyConnection( generic_connection , true );
					
					connections.add( bc );
					
					logMessage( "Con " + bc.getString() + " added: num=" + connections.size() );

				}catch( Throwable e ){
			
					current_message.reportFailed( e );
					
					return;
				}
			}
		}
		
		try{
			logMessage( "Allocating msg " + current_message.getString() + " to con " + bc.getString());

			bc.sendMessage( current_message );
		
		}catch( BuddyPluginException e ){
			
			current_message.reportFailed( e );
		}
	}
	
	protected void
	removeConnection(
		buddyConnection			bc )
	{
		int	size;
		
		synchronized( this ){
			
			connections.remove( bc );
			
			size = connections.size();
		}
		
		logMessage( "Con " + bc.getString() + " removed: num=" + size );

			// connection failed, see if we need to attempt to re-establish
		
		dispatchMessage();
	}
	
	protected long
	getLastStatusCheckTime()
	{
		return( last_status_check_time );
	}
	
	protected boolean
	statusCheckActive()
	{
		synchronized( this ){

			return( check_active );
		}
	}
	
	protected boolean
	statusCheckStarts()
	{
		synchronized( this ){
			
			if ( check_active ){
				
				return( false );
			}
		
			last_status_check_time = SystemTime.getCurrentTime();
		
			check_active = true;
		}
		
		return( true );
	}
	
	protected void
	statusCheckFailed()
	{
		boolean	status_change = false;

		synchronized( this ){

			if ( online ){
				
				online			= false;
				status_change	= true;
			}
			
			status_check_count++;
				
			check_active = false;
		}
			
		if ( status_change ){
			
			plugin.fireDetailsChanged( this );
		}
		
		plugin.logMessage( public_key + ": offline" );
	}
	
	protected void
	statusCheckComplete(
		long			_post_time,
		InetAddress		_ip,
		int				_tcp_port,
		int				_udp_port,
		String			_nick_name )
	{
		boolean	details_change 	= false;
		boolean	config_dirty 	= false;
		
		synchronized( this ){

			if ( !online ){
				
				online			= true;
				details_change	= true;
			}

			post_time	= _post_time;
			
			if ( 	!addressesEqual( ip, _ip ) ||
					tcp_port != _tcp_port ||
					udp_port != _udp_port ){
				
				ip			= _ip;
				tcp_port	= _tcp_port;
				udp_port	= _udp_port;
				
				details_change	= true;
			}
			
			if ( !plugin.stringsEqual( nick_name, _nick_name )){
				
				nick_name	= _nick_name;
				
				config_dirty	= true;
				details_change	= true;
			}
			
			status_check_count++;
					
			check_active = false;
		}
		
		if ( config_dirty ){
			
			plugin.setConfigDirty();
		}
		
		if ( details_change ){
			
			plugin.fireDetailsChanged( this );
		}
		
		plugin.logMessage( getString());
	}
	
	protected boolean
	addressesEqual(
		InetAddress		ip1,
		InetAddress		ip2 )
	{
		if ( ip1 == null && ip2 == null ){
			
			return( true );
			
		}else if ( ip1 == null || ip2 == null ){
			
			return( false );
			
		}else{
	
			return( ip1.equals( ip2 ));
		}
	}
	
	protected void
	checkTimeouts()
	{
		long	now = SystemTime.getCurrentTime();
		
		List	failed = null;
		
		List	connections_to_check = null;
		
		synchronized( this ){
			
			if ( messages.size() > 0 ){
							
				Iterator	it = messages.iterator();
				
				while( it.hasNext()){
					
					buddyMessage	message = (buddyMessage)it.next();
					
					if ( message.timedOut( now )){
						
						it.remove();
						
						if ( failed == null ){
							
							failed = new ArrayList();
						}
						
						failed.add( message );
					}
				}
			}
			
			if ( connections.size() > 0 ){
				
				connections_to_check = new ArrayList( connections );
			}
		}
		
		if ( connections_to_check != null ){
			
			for (int i=0;i<connections_to_check.size();i++){
				
				((buddyConnection)connections_to_check.get(i)).checkTimeout( now );
			}
		}
		
		if ( failed != null ){
			
			for (int i=0;i<failed.size();i++){
				
				((buddyMessage)failed.get(i)).reportFailed( new BuddyPluginException( "Timeout" ));
			}
		}
	}
	
	protected void
	logMessage(
		String	str )
	{
		plugin.logMessage( getShortString() + ": " + str );
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
	incomingConnection(
		GenericMessageConnection	_connection )
	{
		addConnection( _connection );
	}
	
	protected void
	addConnection(
		GenericMessageConnection		_connection )
	{
		int	size;
		
		buddyConnection bc = new buddyConnection( _connection, false );
		
		synchronized( this ){
			
			connections.add( bc );
			
			size = connections.size();
		}
		
		logMessage( "Con " + bc.getString() + " added: num=" + size );
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
		return( "pk=" + public_key + (nick_name==null?"":(",nick=" + nick_name)) + ",ip=" + ip + ",tcp=" + tcp_port + ",udp=" + udp_port + ",online=" + online + ",age=" + (SystemTime.getCurrentTime() - post_time ));
	}

	protected class
	buddyMessage
	{
		private int									message_id;
		
		private Map									request;
		private int									subsystem;
		private BuddyPluginBuddyReplyListener		listener;
		private int									timeout_millis;
		
		private long					queue_time	= SystemTime.getCurrentTime();
		
		private boolean		timed_out;
		private int			retry_count;
		private boolean		complete;
		
		protected
		buddyMessage(
			int									_subsystem,
			Map									_request,
			int									_timeout )
		{
			synchronized( BuddyPluginBuddy.this ){
			
				message_id = next_message_id++;
			}
			
			request			= _request;
			subsystem		= _subsystem;
			timeout_millis	= _timeout;
		}
		
		protected void
		setListener(
			BuddyPluginBuddyReplyListener		_listener )
		{
			listener		= _listener;
		}
		
		protected int
		getRetryCount()
		{
			synchronized( this ){

				return( retry_count );
			}
		}
		
		protected void
		setRetry()
		{
			synchronized( this ){
				
				retry_count++;
				
				complete 	= false;
				timed_out 	= false;
				
			}
		}
		
		protected boolean
		timedOut(
			long	now )
		{
			if ( timed_out ){
				
				return( true );
			}
			
			if ( now < queue_time ){
				
				queue_time = now;
				
				return( false );
				
			}else{
				
				timed_out = now - queue_time >= timeout_millis;
				
				return( timed_out );
			}
		}
		
		protected Map
		getRequest()
		{
			return( request );
		}
		
		protected int
		getSubsystem()
		{
			return( subsystem );
		}
		
		protected int
		getID()
		{
			return( message_id );
		}
		
		protected void
		reportComplete(
			Map		reply )
		{
			synchronized( this ){
				
				if ( complete ){
					
					return;
				}
				
				complete = true;
			}
			
			try{
				listener.replyReceived(  BuddyPluginBuddy.this, reply );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
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
			
			try{
				if ( error instanceof BuddyPluginException ){
					
					listener.sendFailed( BuddyPluginBuddy.this, (BuddyPluginException)error );
					
				}else{
				
					listener.sendFailed(  BuddyPluginBuddy.this, new BuddyPluginException( "Unexpected error",  error ));
				}
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
		
		protected String
		getString()
		{
			return( "id=" + message_id + ",ss=" + subsystem + (retry_count==0?"":(",retry="+retry_count)));
		}
	}
	
	protected class
	buddyConnection
		implements GenericMessageConnectionListener
	{
		private int								connection_id;
		private GenericMessageConnection		connection;
		private boolean							outgoing;
		
		private String							dir_str;
		
		private boolean			connected;
		private buddyMessage	active_message;
		private boolean			closing;
		private boolean			failed;
		
		private long			last_active	= SystemTime.getCurrentTime();
		
		protected
		buddyConnection(
			GenericMessageConnection		_connection,
			boolean							_outgoing )
		{
			connection 	= _connection;
			outgoing	= _outgoing;
			
			synchronized( BuddyPluginBuddy.this ){
				
				connection_id = next_connection_id++;
			}
			
			dir_str = outgoing?"Outgoing":"Incoming";
			
			if ( !outgoing ){
				
				connected = true;
			}
			
			connection.addListener( this );
		}
		
		protected boolean
		hasFailed()
		{
			return( failed );
		}
		
		protected void
		sendMessage(
			buddyMessage	message )
		
			throws BuddyPluginException
		{
			boolean	send = false;
			
			synchronized( this ){
				
				if ( active_message != null ){
					
					Debug.out( "Inconsistent: active message already set" );
					
					BuddyPluginException error = new BuddyPluginException( "Inconsistent state" );
					
					failed( connection, error );
					
					throw( error );
					
				}else if ( failed || closing ){
					
					throw( new BuddyPluginException( "Connection failed" ));
					
				}else{
					
					active_message = message;
					
					send	= connected;
				}
			}
			
			if ( send ){
			
				send();
			}
		}
		
		public void
		connected(
			GenericMessageConnection	connection )
		{
			if ( TRACE ){
				System.out.println( dir_str + " connected" );
			}
			
			boolean	send = false;
			
			synchronized( this ){
				
				last_active	= SystemTime.getCurrentTime();
				
				connected = true;
				
				send = active_message != null;
			}
			
			if ( send ){
			
				send();
			}
		}
		
		protected void
		checkTimeout(
			long	now )
		{
			buddyMessage	bm = null;
			
			boolean	close = false;
			
			synchronized( this ){

				if ( active_message != null ){
					
					if ( active_message.timedOut( now )){
						
						bm	= active_message;
						
						active_message	= null;
					}
				}
								
				if ( now < last_active ){
					
					last_active = now;
				}
				
				if ( now - last_active > CONNECTION_IDLE_TIMEOUT ){
										
					close	= true;
				}
			}
			
			if ( bm != null ){
				
				bm.reportFailed( new BuddyPluginException( "Timeout" ));
			}

			if ( close ){
				
				close();
			}
		}
		
		protected void
		send()
		{
			Map request = active_message.getRequest();
			
			Map	send_map = new HashMap();
			
			send_map.put( "type", new Long( RT_REQUEST_DATA ));
			send_map.put( "req", request );
			send_map.put( "ss", new Long( active_message.getSubsystem()));
			send_map.put( "id", new Long( active_message.getID()));
			
			PooledByteBuffer	buffer	= null;
			
			try{
				byte[] data = BEncoder.encode( send_map );
				
				buffer = 
					plugin.getPluginInterface().getUtilities().allocatePooledByteBuffer( data );
			
				logMessage( "Sending " + active_message.getString() + " to " + getString());
								
				connection.send( buffer );
			
				synchronized( this ){
					
					last_active	= SystemTime.getCurrentTime();;
				}
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
		
		public void
		receive(
			GenericMessageConnection	connection,
			PooledByteBuffer			data_buffer )
		
			throws MessageException
		{
			synchronized( this ){
				
				last_active	= SystemTime.getCurrentTime();;
			}
			
			try{
				byte[]	content = data_buffer.toByteArray();
				
				if ( TRACE ){
					System.out.println( dir_str + " receive: " + content.length );
				}
				
				Map	data_map = BDecoder.decode( content );
				
				int	type = ((Long)data_map.get("type")).intValue();
				
				if ( type == RT_REQUEST_DATA ){
					
					logMessage( "Received type=" + type + " from " + getString());
				
					Long	subsystem = (Long)data_map.get( "ss" );
					
					Map	reply;
					
					int	reply_type;
					
					Map request = (Map)data_map.get( "req" );

					if ( request == null || subsystem == null ){
						
						reply	= null;
						
					}else{
						
						reply = plugin.requestReceived( BuddyPluginBuddy.this, subsystem.intValue(), request );
					}
					
					if ( reply == null ){
						
						reply_type = RT_REPLY_ERROR;
						
						reply = new HashMap();
						
						reply.put( "error", "No handlers available to process request" );
						
					}else{
						
						reply_type = RT_REPLY_DATA;
					}
					
					Map reply_map = new HashMap();
					
					reply_map.put( "ss", subsystem );
					reply_map.put( "type", new Long( reply_type ));																
					reply_map.put( "id", data_map.get( "id" ) );

					reply_map.put( "rep", reply );

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
					
				}else if ( type == RT_REPLY_DATA || type == RT_REPLY_ERROR ){
					
					buddyMessage	bm;
					
					synchronized( this ){
						
						bm = active_message;

						active_message = null;
					}
					
					if ( bm != null ){
						
						Map	reply = (Map)data_map.get( "rep" );
						
						if ( type == RT_REPLY_ERROR ){
							
							bm.reportFailed( new BuddyPluginException(new String((byte[])reply.get( "error" ))));
							
						}else{
							
							bm.reportComplete( reply );
						}
					}
										
				}else{
					
						// ignore unknown message types
				}
			}catch( Throwable e ){
				
				failed( connection, e );
				
			}finally{
				
				data_buffer.returnToPool();
			}
		}
		
		protected void
		close()
		{
			closing = true;
			
			failed( connection, new BuddyPluginException( "Closing" ));
		}
		
		public void
		failed(
			GenericMessageConnection	connection,
			Throwable 					error )
		{
			buddyMessage bm = null;
			
			synchronized( this ){
				
				if ( failed ){
					
					return;
				}
				
				failed = true;
				
				bm = active_message;
				
				active_message	 = null;
			}
			
			logMessage( "Con " + getString() + " failed: " + Debug.getNestedExceptionMessage( error ));
			
			try{
				if ( !closing ){
					
					if ( TRACE ){
						System.out.println( dir_str + " connection error:" );
					}
					
					error.printStackTrace();
				}
				
				try{
					connection.close();
					
				}catch( Throwable e ){
					
					e.printStackTrace();
				}
			}finally{
								
				removeConnection( this );
				
				if ( bm != null ){
					
					bm.reportFailed( error );
				}
			}
		}
		
		protected String
		getString()
		{
			return("id=" + connection_id + ",dir=" + ( outgoing?"out":"in" ));
		}
	}
}
