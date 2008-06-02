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

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;

import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.AddressUtils;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.LightHashMap;
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

	
	private BuddyPlugin		plugin;
	private int				subsystem;
	private boolean			authorised;
	private String			public_key;
	private String			nick_name;
	private List			recent_ygm;
	
	private int				last_status_seq;
	
	private long			post_time;
	private InetAddress		ip;
	private int				tcp_port;
	private int				udp_port;
		
	private boolean			online;
	private long			last_time_online;
	
	private long			status_check_count;
	private long			last_status_check_time;
	
	private boolean			check_active;
		
	private List						connections	= new ArrayList();
	private List						messages	= new ArrayList();
	private buddyMessage				current_message;
	
	private int	next_connection_id;
	private int	next_message_id;
	
	private boolean	ygm_active;
	private boolean	ygm_pending;
	
	private long 	latest_ygm_time;
	private String	last_message_received;
	
	private Set		offline_seq_set;
	
	private int		message_out_count;
	private int		message_in_count;
	private int		message_out_bytes;
	private int		message_in_bytes;
	
	private String	received_frag_details = "";
	
	private BuddyPluginBuddyMessageHandler		persistent_msg_handler;

	private Map			user_data = new LightHashMap();
	
	private volatile boolean	closing;
	private volatile boolean	destroyed;
	
	protected
	BuddyPluginBuddy(
		BuddyPlugin	_plugin,
		int			_subsystem,
		boolean		_authorised,
		String		_pk,
		String		_nick_name,
		int			_last_status_seq,
		long		_last_time_online,
		List		_recent_ygm )
	{
		plugin				= _plugin;
		subsystem			= _subsystem;
		authorised			= _authorised;
		public_key 			= _pk;
		nick_name			= _nick_name;
		last_status_seq		= _last_status_seq;
		last_time_online	= _last_time_online;
		recent_ygm			= _recent_ygm;
		
		persistent_msg_handler = new BuddyPluginBuddyMessageHandler( this, new File(plugin.getBuddyConfigDir(), public_key ));
	}
	
	protected BuddyPlugin
	getPlugin()
	{
		return( plugin );
	}
	
	public BuddyPluginBuddyMessageHandler
	getMessageHandler()
	{
		return( persistent_msg_handler );
	}
	
	protected void
	persistentDispatchPending()
	{
		plugin.persistentDispatchPending( this );
	}
	
	protected void
	checkPersistentDispatch()
	{
		persistent_msg_handler.checkPersistentDispatch();
	}
	
	protected void
	persistentDispatch()
	{
		persistent_msg_handler.persistentDispatch();
	}
	
	public Map
	readConfigFile(
		File		name )
	{
		return( plugin.readConfigFile( name ));
	}
	
	public boolean
	writeConfigFile(
		File		name,
		Map			data )
	{
		return( plugin.writeConfigFile( name, data ));
	}
	
	public int
	getSubsystem()
	{
		return( subsystem );
	}
	
	public void
	setSubsystem(
		int		_s )
	{
		if ( _s != subsystem ){
			
			subsystem	= _s;
			
			plugin.saveConfig( true );
		}
	}
	
	public boolean
	isAuthorised()
	{
		return( authorised );
	}
	
	protected void
	setAuthorised(
		boolean		_a )
	{
		authorised = _a;
	}
	
	public String
	getPublicKey()
	{
		return( public_key );
	}
	
	protected byte[]
	getRawPublicKey()
	{
		return( Base32.decode( public_key ));
	}
	
	protected String
	getShortString()
	{
		return( public_key.substring( 0, 16 ) + "..." );
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
		
		return( getShortString());
	}
	
	public void
	remove()
	{
		persistent_msg_handler.destroy();
		
		plugin.removeBuddy( this, authorised );
	}
	
	public InetAddress
	getIP()
	{
		return( ip );
	}
	
	public InetAddress
	getAdjustedIP()
	{
		if ( ip == null ){
			
			return( null );
		}
		
		InetSocketAddress address = new InetSocketAddress( ip, tcp_port );
		
		InetSocketAddress adjusted_address = AddressUtils.adjustTCPAddress( address, true );
		
		if ( adjusted_address != address ){
			
			return( adjusted_address.getAddress());
		}
		
		address = new InetSocketAddress( ip, udp_port );
		
		adjusted_address = AddressUtils.adjustUDPAddress( address, true );
		
		if ( adjusted_address != address ){
			
			return( adjusted_address.getAddress());
		}
	
		return( ip );
	}
	
	public List
	getAdjustedIPs()
	{
		List	result = new ArrayList();
		
		if ( ip == null ){
			
			return( result );
		}
		
		InetAddress adjusted = getAdjustedIP();
		
		if ( adjusted == ip ){
			
			result.add( ip );
			
		}else{
			
			List l = AddressUtils.getLANAddresses( adjusted.getHostAddress());
			
			for (int i=0;i<l.size();i++){
				
				try{
					result.add( InetAddress.getByName((String)l.get(i)));
					
				}catch( Throwable e ){
					
				}
			}
		}
		
		return( result );
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
	
	protected boolean
	isIdle()
	{
		synchronized( this ){
		
			return( connections.size() == 0 );
		}
	}
	
	public long
	getLastTimeOnline()
	{
		return( last_time_online );
	}
	
	public BuddyPlugin.cryptoResult
	encrypt(
		byte[]		payload )
	
		throws BuddyPluginException
	{
		return( plugin.encrypt( this, payload ));
	}
	
	public BuddyPlugin.cryptoResult
	decrypt(
		byte[]		payload )
	
		throws BuddyPluginException
	{
		return( plugin.decrypt( this, payload, getName() ));
	}
	
	public boolean
	verify(
		byte[]		payload,
		byte[]		signature )
	
		throws BuddyPluginException
	{
		
		return( plugin.verify( this, payload, signature ));
	}
	
	public void
	setMessagePending()
	
		throws BuddyPluginException
	{
		synchronized( this ){
			
			if ( ygm_active ){
				
				ygm_pending = true;
				
				return;
			}
			
			ygm_active = true;
		}
		
		plugin.setMessagePending( 
			this,
			new BuddyPlugin.operationListener()
			{
				public void 
				complete() 
				{
					boolean	retry;
					
					synchronized( this ){
						
						ygm_active = false;
						
						retry = ygm_pending;
						
						ygm_pending = false;
					}
					
					if ( retry ){
						
						try{
							setMessagePending();
							
						}catch( BuddyPluginException e ){
							
							log( "Failed to send YGM", e );
						}
					}
				}	
			});
	}
	
	public long
	getLastMessagePending()
	{
		return( latest_ygm_time );
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
			
			latest_ygm_time = SystemTime.getCurrentTime();
		}
		
		plugin.setConfigDirty();
		
		plugin.fireDetailsChanged( this );
		
		return( true );
	}
	
	protected void
	setLastMessageReceived(
		String		str )
	{
		last_message_received = str;
		
		plugin.fireDetailsChanged( this );
	}
	
	public String
	getLastMessageReceived()
	{
		return( last_message_received );
	}
	
	protected List
	getYGMMarkers()
	{
		return( recent_ygm );
	}
	
	protected int
	getLastStatusSeq()
	{
		return( last_status_seq );
	}
	
	protected void
	buddyConnectionEstablished()
	{
		buddyActive();
	}
	
	protected void
	buddyMessageSent(
		int			size,
		boolean		record_active )
	{
		message_out_count++;
		message_out_bytes += size;
		
		if ( record_active ){
		
			buddyActive();
		}
	}
	
	protected void
	buddyMessageReceived(
		int		size )
	{		
		message_in_count++;
		message_in_bytes += size;
		
		received_frag_details = "";
		
		buddyActive();
	}
	
	protected void
	buddyMessageFragmentReceived(
		int		num_received,
		int		total )
	{
		received_frag_details = num_received + "/" + total;
		
		plugin.fireDetailsChanged( this );
	}
	
	public String
	getMessageInFragmentDetails()
	{
		return( received_frag_details );
	}
	
	public int
	getMessageInCount()
	{
		return( message_in_count );
	}
	
	public int
	getMessageOutCount()
	{
		return( message_out_count );
	}
	
	public int
	getBytesInCount()
	{
		return( message_in_bytes );
	}
	
	public int
	getBytesOutCount()
	{
		return( message_out_bytes );
	}
	
	protected void
	buddyActive()
	{
		long	now = SystemTime.getCurrentTime();
		
		synchronized( this ){
			
			last_time_online			= now;
			online						= true;
		}
			
		persistentDispatchPending();
		
		plugin.fireDetailsChanged( this );
	}
	
	public void
	ping()
		throws BuddyPluginException
	{
		plugin.checkAvailable();
		
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
						log( "Ping reply received:" + reply );
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
						
			throw( new BuddyPluginException( "Ping failed", e ));
		}
	}
	
	protected void
	sendCloseRequest(
		boolean		restarting )
	{
		List	to_send = new ArrayList();
	
		synchronized( this ){
				
			closing	= true;
			
			for (int i=0;i<connections.size();i++){
				
				buddyConnection c = (buddyConnection)connections.get(i);
				
				if ( c.isConnected() && !c.hasFailed() && !c.isActive()){
					
					to_send.add( c );
				}
			}
		}
		
		for (int i=0;i<to_send.size();i++){
			
			buddyConnection c = (buddyConnection)to_send.get(i);
			
			try{
				Map	close_request = new HashMap();
				
				close_request.put( "type", new Long( BuddyPlugin.RT_INTERNAL_REQUEST_CLOSE ));
				
				close_request.put( "r", new Long( restarting?1:0));
				
				close_request.put( "os", new Long( plugin.getCurrentStatusSeq()));
				
				final buddyMessage	message = 
					new buddyMessage( BuddyPlugin.SUBSYSTEM_INTERNAL, close_request, 60*1000 );

				message.setListener(
						new BuddyPluginBuddyReplyListener()
						{
							public void
							replyReceived(
								BuddyPluginBuddy	from_buddy,
								Map					reply )
							{
								log( "Close reply received:" + reply );
							}
							
							public void
							sendFailed(
								BuddyPluginBuddy		to_buddy,
								BuddyPluginException	cause )
							{
								log( "Close failed to " + getString(), cause );
							}
						});
				
				c.sendCloseMessage( message );

			}catch( Throwable e ){
							
				log( "Close request failed", e );
			}
		}
	}
	
	protected void
	receivedCloseRequest(
		Map		request )
	{
		try{
			boolean	restarting = ((Long)request.get( "r" )).longValue() == 1;
			
			if ( restarting ){
				
				logMessage( "restarting" );
				
			}else{
				
				logMessage( "going offline" );
				
				boolean	details_change = false;
				
				synchronized( this ){
					
					if ( offline_seq_set == null ){
						
						offline_seq_set = new HashSet();
					}
					
					offline_seq_set.add( new Long( last_status_seq ));
					
					offline_seq_set.add((Long)request.get( "os" ));
					
					if ( online ){
						
						online			= false;
						details_change	= true;
					}
				}
				
				if ( details_change ){
					
					plugin.fireDetailsChanged( this );
				}
			}
		}catch( Throwable e ){
			
			Debug.out( "Failed to decode close request", e );
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
								
								Debug.out( "Inconsistent: reply received not for current message" );
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
							// only try and reconcile this failure with the current message if
							// the message has actually been sent
						
						boolean	was_active;
						
						if ( cause instanceof BuddyPluginTimeoutException ){
							
							was_active = ((BuddyPluginTimeoutException)cause).wasActive();
							
						}else{
							
							was_active = true;
						}
						
						if ( was_active ){
							
							synchronized( BuddyPluginBuddy.this ){
								
								if ( current_message != message ){
									
									Debug.out( "Inconsistent: error received not for current message" );
								}
								
								current_message = null;
							}
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
		
		buddyMessage 	allocated_message 	= null;
		Throwable		failed_msg_error 	= null;
		
		synchronized( this ){
	
			if ( current_message != null || messages.size() == 0 || closing ){
				
				return;
			}
			
			allocated_message = current_message = (buddyMessage)messages.remove( 0 );
			
			for (int i=0;i<connections.size();i++){
				
				buddyConnection c = (buddyConnection)connections.get(i);
				
				if ( !c.hasFailed()){
					
					bc	= c;
				}
			}
			
			if ( bc == null ){
				
				try{
					if ( destroyed ){
						
						throw( new BuddyPluginException( "Friend destroyed" ));
					}
					
					if ( connections.size() >= MAX_ACTIVE_CONNECTIONS ){
						
						throw( new BuddyPluginException( "Too many active connections" ));
					}
					
					GenericMessageConnection generic_connection = outgoingConnection();
					
					bc = new buddyConnection( generic_connection , true );
					
					connections.add( bc );
					
					logMessage( "Con " + bc.getString() + " added: num=" + connections.size() );

				}catch( Throwable e ){
			
					failed_msg_error	= e;
				}
			}
		}
		
			// take outside sync block
		
		if ( failed_msg_error != null ){
			
			allocated_message.reportFailed( failed_msg_error );
			
			return;
		}
		
		try{
			logMessage( "Allocating msg " + allocated_message.getString() + " to con " + bc.getString());

			bc.sendMessage( allocated_message );
		
		}catch( BuddyPluginException e ){
			
			allocated_message.reportFailed( e );
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
		
		plugin.fireDetailsChanged( this );
		
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
		boolean	details_change = false;

		synchronized( this ){

			try{
				if ( online ){
					
					online			= false;
					details_change	= true;
				}
			}finally{
			
				status_check_count++;
				
				check_active = false;
			}
		}
			
		if ( details_change ){
			
			plugin.fireDetailsChanged( this );
		}
	}
	
	protected void
	statusCheckComplete(
		long			_post_time,
		InetAddress		_ip,
		int				_tcp_port,
		int				_udp_port,
		String			_nick_name,
		int				_status_seq )
	{
		boolean	details_change 	= false;
		boolean	config_dirty 	= false;

		long	now = SystemTime.getCurrentTime();
		
		if ( now < last_time_online ){
			
			last_time_online = now;
		}
		
		synchronized( this ){

			try{
					// do we explicitly know that this sequence number denotes an offline buddy
				
				if ( offline_seq_set != null ){
					
					if ( offline_seq_set.contains(new Long( _status_seq ))){
						
						return;
						
					}else{
						
						offline_seq_set = null;
					}
				}
				
				boolean	seq_change = _status_seq != last_status_seq;
				
				boolean timed_out;

					// change in sequence means we're online
				
				if ( seq_change ){
					
					last_status_seq		= _status_seq;
					last_time_online	= now;
					
					timed_out 			= false;
					details_change		= true;
					
				}else{
				
					timed_out =  now - last_time_online >= BuddyPlugin.STATUS_REPUBLISH_PERIOD * 3 ;
				}	
				
				if ( online ){
					
					if ( timed_out ){
							
						online			= false;
						details_change	= true;
					}
				}else{	
					
					if ( seq_change || !timed_out ){
						
						online			= true;
						details_change	= true;
					}
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
			}finally{
			
				status_check_count++;
					
				check_active = false;
			}
		}
		
		if ( config_dirty ){
			
			plugin.setConfigDirty();
		}
		
		if ( details_change ){
			
			if ( online ){
				
				persistentDispatchPending();
			}
			
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
				
				((buddyMessage)failed.get(i)).reportFailed( new BuddyPluginTimeoutException( "Timeout", false ));
			}
		}
	}
	
	public String
	getConnectionsString()
	{
		synchronized( this ){
			
			String	str = "";
			
			for (int i=0;i<connections.size();i++){
				
				str += (str.length()==0?"":",") + ((buddyConnection)connections.get(i)).getString( true );
			}
			
			return( str );
		}
	}
	
	protected boolean
	isClosing()
	{
		return( closing );
	}
	
	protected void
	destroy()
	{
		List	to_close = new ArrayList();
		
		synchronized( this ){
			
			destroyed = true;
			
			to_close.addAll( connections );
		}
		
		for (int i=0;i<to_close.size();i++){
			
			((buddyConnection)to_close.get(i)).close();
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
						
			throw( new BuddyPluginException( "Friend offline (no usable IP address)" ));
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
						
			throw( new BuddyPluginException( "Friend offline (no usable protocols)" ));
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
				
			plugin.addRateLimiters( con );
			
			String reason = "Friend: Outgoing connection establishment";
	
			SESecurityManager sec_man = plugin.getSecurityManager();
			
			con = sec_man.getSTSConnection( 
					con, 
					sec_man.getPublicKey( SEPublicKey.KEY_TYPE_ECC_192, reason ),
	
					new SEPublicKeyLocator()
					{
						public boolean
						accept(
							Object			context,
							SEPublicKey		other_key )
						{
							String	other_key_str = Base32.encode( other_key.encodeRawPublicKey());

							if ( other_key_str.equals( public_key )){
								
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
	
		throws BuddyPluginException
	{
		addConnection( _connection );
	}
	
	protected void
	addConnection(
		GenericMessageConnection		_connection )
	
		throws BuddyPluginException
	{
		int	size;
		
		buddyConnection bc = new buddyConnection( _connection, false );
		
		synchronized( this ){
			
			if ( destroyed ){
				
				throw( new BuddyPluginException( "Friend has been destroyed" ));
			}
			
			connections.add( bc );
			
			size = connections.size();
		}
		
		logMessage( "Con " + bc.getString() + " added: num=" + size );
	}
	
	public void
	setUserData(
		Object		key,
		Object		value )
	{
		synchronized( user_data ){
			
			user_data.put(key, value);
		}
	}
	
	public Object
	getUserData(
		Object		key )
	{
		synchronized( user_data ){

			return( user_data.get( key ));
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
		return( "pk=" +  getShortString() + (nick_name==null?"":(",nick=" + nick_name)) + ",ip=" + ip + ",tcp=" + tcp_port + ",udp=" + udp_port + ",online=" + online + ",age=" + (SystemTime.getCurrentTime() - post_time ));
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
		setDontRetry()
		{
			retry_count = 99;
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
				
					listener.sendFailed(  BuddyPluginBuddy.this, new BuddyPluginException( "",  error ));
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
		implements fragmentHandlerReceiver
	{
		private fragmentHandler					fragment_handler;
		private int								connection_id;
		private boolean							outgoing;
		
		private String							dir_str;
		
		
		private volatile buddyMessage	active_message;
		
		private volatile boolean			connected;
		private volatile boolean			closing;
		private volatile boolean			failed;
		
		private long			last_active	= SystemTime.getCurrentTime();
		
		protected
		buddyConnection(
			GenericMessageConnection		_connection,
			boolean							_outgoing )
		{
			fragment_handler	= new fragmentHandler( _connection, this );
			
			outgoing	= _outgoing;
			
			synchronized( BuddyPluginBuddy.this ){
				
				connection_id = next_connection_id++;
			}
			
			dir_str = outgoing?"Outgoing":"Incoming";
			
			if ( !outgoing ){
				
				connected = true;
				
				buddyConnectionEstablished();
			}
			
			fragment_handler.start();
		}
		
		protected boolean
		isConnected()
		{
			return( connected );
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
			BuddyPluginException	failed_error = null;

			buddyMessage	msg_to_send			= null;

			synchronized( this ){
				
				if ( BuddyPluginBuddy.this.isClosing()){
					
					throw( new BuddyPluginException( "Close in progress" ));
				}
				
				if ( active_message != null ){
					
					Debug.out( "Inconsistent: active message already set" );
					
					failed_error = new BuddyPluginException( "Inconsistent state" );
					
				}else if ( failed || closing ){
					
					throw( new BuddyPluginException( "Connection failed" ));
					
				}else{
										
					active_message = message;
					
					if ( connected ){
						
						msg_to_send = active_message;
					}
				}
			}
			
			if ( failed_error != null ){
				
				failed( failed_error );
				
				throw( failed_error );
			}
			
			if ( msg_to_send != null ){
			
				send( msg_to_send );
			}
		}
		
		protected void
		sendCloseMessage(
			buddyMessage	message )	
		{			
			boolean	ok_to_send;
			
			synchronized( this ){
				
				ok_to_send = active_message == null && connected && !failed && !closing;
			}
			
			if ( ok_to_send ){
			
				send( message );
			}
		}
		
		public boolean
		isActive()
		{
			return( active_message != null );
		}
		
		public void
		connected()
		{
			if ( TRACE ){
				System.out.println( dir_str + " connected" );
			}
			
			buddyMessage	msg_to_send = null;
			
			synchronized( this ){
				
				last_active	= SystemTime.getCurrentTime();
				
				connected = true;
				
				msg_to_send = active_message;
			}
			
			buddyConnectionEstablished();
			
			if ( msg_to_send != null  ){
			
				send( msg_to_send );
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
				
				bm.reportFailed( new BuddyPluginTimeoutException( "Timeout", true ));
			}

			if ( close ){
				
				close();
			}
		}
		
		protected void
		send(
			buddyMessage		msg )
		{
			Map request = msg.getRequest();
			
			Map	send_map = new HashMap();
			
			send_map.put( "type", new Long( RT_REQUEST_DATA ));
			send_map.put( "req", request );
			send_map.put( "ss", new Long( msg.getSubsystem()));
			send_map.put( "id", new Long( msg.getID()));
						
			try{
				logMessage( "Sending " + msg.getString() + " to " + getString());

				fragment_handler.send( send_map, true, true );
							
				synchronized( this ){
					
					last_active	= SystemTime.getCurrentTime();;
				}
			}catch( BuddyPluginException e ){
			
				try{			
					failed( e );
				
				}catch( Throwable f ){
				
					Debug.printStackTrace(f);
				}
			}
		}
		
		public void
		receive(
			Map			data_map )
		{
			synchronized( this ){
				
				last_active	= SystemTime.getCurrentTime();;
			}
			
			if ( TRACE ){
				System.out.println( dir_str + " receive: " + data_map );
			}

			try{
				int	type = ((Long)data_map.get("type")).intValue();
				
				if ( type == RT_REQUEST_DATA ){
					
					logMessage( "Received type=" + type + " from " + getString());
				
					Long	subsystem = (Long)data_map.get( "ss" );
					
					Map	reply;
					
					int	reply_type;
					
					Map request = (Map)data_map.get( "req" );

					String	error = null;
					
					if ( request == null || subsystem == null ){
						
						reply	= null;
						
					}else{
						
						try{
						
							reply = plugin.requestReceived( BuddyPluginBuddy.this, subsystem.intValue(), request );
							
						}catch( Throwable e ){
							
							error = Debug.getNestedExceptionMessage(e);
							
							reply = null;
						}
					}
					
					if ( reply == null ){
						
						reply_type = RT_REPLY_ERROR;
						
						reply = new HashMap();
						
						reply.put( "error", error==null?"No handlers available to process request":error );
						
					}else{
						
						reply_type = RT_REPLY_DATA;
					}
					
					Map reply_map = new HashMap();
					
					reply_map.put( "ss", subsystem );
					reply_map.put( "type", new Long( reply_type ));																
					reply_map.put( "id", data_map.get( "id" ) );

					reply_map.put( "rep", reply );
					
						// don't record as active here as (1) we recorded as active above when 
						// receiving request (2) we may be replying to a 'closing' message and
						// we don't want the reply to mark as online 

					fragment_handler.send( reply_map, false, false );
					
				}else if ( type == RT_REPLY_DATA || type == RT_REPLY_ERROR ){
					
					long	id = ((Long)data_map.get( "id" )).longValue();
					
					buddyMessage	bm;
					
					synchronized( this ){
						
						if ( 	active_message != null &&
								active_message.getID() == id ){
								
							bm = active_message;
		
							active_message = null;
							
						}else{
								
							bm = null;
						}
					}
					
					Map	reply = (Map)data_map.get( "rep" );
					
					if ( bm == null ){
						
						logMessage( "reply discarded as no matching request: " + reply );
						
					}else{
						
						if ( type == RT_REPLY_ERROR ){
							
							bm.setDontRetry();
							
							bm.reportFailed( new BuddyPluginException(new String((byte[])reply.get( "error" ))));
							
						}else{
							
							bm.reportComplete( reply );
						}
					}				
				}else{
					
						// ignore unknown message types
				}
			}catch( Throwable e ){
				
				failed( e );	
			}
		}
		
		protected void
		close()
		{
			closing = true;
			
			failed( new BuddyPluginException( "Closing" ));
		}
		
		public void
		failed(
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
				}
				
				fragment_handler.close();

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
			return( getString( false ));
		}
		
		protected String
		getString(
			boolean	short_form )
		{
			if ( short_form ){
				
				return( fragment_handler.getString());
				
			}else{
				
				return("id=" + connection_id + ",dir=" + ( outgoing?"out":"in" ));
			}
		}
	}
	
	protected class
	fragmentHandler
		implements GenericMessageConnectionListener
	{
		private GenericMessageConnection	connection;
		private fragmentHandlerReceiver		receiver;
		
		private int	next_fragment_id	= 0;
		
		private fragmentAssembly	current_request_frag;
		private fragmentAssembly	current_reply_frag;
		
		private int					send_count;
		private int					recv_count;
		
		protected
		fragmentHandler(
			GenericMessageConnection	_connection,
			fragmentHandlerReceiver		_receiver )
		{
			connection	= _connection;
			receiver	= _receiver;	
		}
	
		public void
		start()
		{
			connection.addListener( this );
		}
		
		public void
		connected(
			GenericMessageConnection	connection )
		{
			receiver.connected();
		}
		
		public void
		failed(
			GenericMessageConnection	connection,
			Throwable 					error )
		
			throws MessageException
		{
			receiver.failed( error );
		}
		
		protected void
		send(
			Map			data_map,
			boolean		is_request,
			boolean		record_active )
			
			throws BuddyPluginException
		{
			try{
				byte[] data = BEncoder.encode( data_map );
				
				int	data_length = data.length;
				
				plugin.checkMaxMessageSize( data_length );
				
				int	max_chunk = connection.getMaximumMessageSize() - 1024;
								
				if ( data_length > max_chunk ){
					
					int	fragment_id;
					
					synchronized( this ){
						
						fragment_id = next_fragment_id++;
					}
					
					int chunk_num = 0;
					
					for (int i=0;i<data_length;i+=max_chunk){
						
						int	end = Math.min( data_length, i + max_chunk );
						
						if ( end > i ){
							
							byte[]	chunk = new byte[ end-i ];
							
							System.arraycopy( data, i, chunk, 0, chunk.length );
							
							Map	chunk_map = new HashMap();
							
							chunk_map.put( "type", new Long( BuddyPlugin.RT_INTERNAL_FRAGMENT ));
							chunk_map.put( "f", new Long( fragment_id ));
							chunk_map.put( "l", new Long( data_length ));
							chunk_map.put( "c", new Long( max_chunk ));
							chunk_map.put( "i", new Long( chunk_num ));
							chunk_map.put( "q", new Long( is_request?1:0 ));
							chunk_map.put( "d", chunk );
							
							byte[] chunk_data = BEncoder.encode( chunk_map );

							PooledByteBuffer chunk_buffer = 
								plugin.getPluginInterface().getUtilities().allocatePooledByteBuffer( chunk_data );
						
							try{									
								connection.send( chunk_buffer );
							
								chunk_buffer = null;
								
							}finally{
								
								if ( chunk_buffer != null ){
									
									chunk_buffer.returnToPool();
								}
							}
						}
						
						chunk_num++;
					}
				}else{
					
					PooledByteBuffer buffer = 
						plugin.getPluginInterface().getUtilities().allocatePooledByteBuffer( data );
				
					try{			
					
						connection.send( buffer );
					
						buffer = null;
						
					}finally{
						
						if ( buffer != null ){
							
							buffer.returnToPool();
						}
					}
				}
				
				buddyMessageSent( data.length, record_active );
				
				send_count++;
				
			}catch( Throwable e ){
				
				throw( new BuddyPluginException( "Send failed", e ));
			}
		}
		
		public void
		receive(
			GenericMessageConnection	connection,
			PooledByteBuffer			message )
		
			throws MessageException
		{
			try{
					// while in unauth state we only allow a few messages. max should be 1
					// for an 'accept request' but I feel generous
				
				if ( recv_count >= 4 && !isAuthorised()){
				
					throw( new MessageException( "Too many messages received while in unauthorised state" ));
				}
				
				byte[]	content = message.toByteArray();
				
				Map	data_map = BDecoder.decode( content );
				
				if (((Long)data_map.get( "type" )).intValue() == BuddyPlugin.RT_INTERNAL_FRAGMENT ){
					
					Map	chunk_map = data_map;
					
					int	fragment_id = ((Long)chunk_map.get( "f" )).intValue();
					int	data_length = ((Long)chunk_map.get( "l" )).intValue();
					int	chunk_size 	= ((Long)chunk_map.get( "c" )).intValue();
					int	chunk_num 	= ((Long)chunk_map.get( "i" )).intValue();
					
					boolean	is_request = ((Long)chunk_map.get("q")).intValue() == 1;
					
					byte[]	chunk_data = (byte[])chunk_map.get("d" );
					
					plugin.checkMaxMessageSize( data_length );
					
					fragmentAssembly assembly;
					
					if ( is_request ){
						
						if ( current_request_frag == null ){
							
							current_request_frag = new fragmentAssembly( fragment_id, data_length, chunk_size );
						}
						
						assembly = current_request_frag;
						
					}else{
						
						if ( current_reply_frag == null ){
							
							current_reply_frag = new fragmentAssembly( fragment_id, data_length, chunk_size );
						}
						
						assembly = current_reply_frag;
					}
					
					if ( assembly.getID() != fragment_id ){
							
						throw( new BuddyPluginException( "Fragment receive error: concurrent decode not supported" ));
					}
					
					if ( assembly.receive( chunk_num, chunk_data )){
										
						if ( is_request ){

							current_request_frag = null;
							
						}else{
							
							current_reply_frag = null;
						}
						
						buddyMessageReceived( data_length );

						recv_count++;
						
						receiver.receive( BDecoder.decode( assembly.getData()));
						
					}else{
						
						buddyMessageFragmentReceived( assembly.getChunksReceived(), assembly.getTotalChunks());
					}
				}else{
				
					buddyMessageReceived( content.length );

					recv_count++;
					
					receiver.receive( data_map );					
				}
			}catch( Throwable e ){
				
				receiver.failed( e );
				
			}finally{
				
				message.returnToPool();
			}
		}
		
		protected void
		close()
		{
			try{
			
				connection.close();
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
		
		protected String
		getString()
		{
			return( connection.getType());
		}
		
		protected class
		fragmentAssembly
		{
			private int		id;
			private byte[]	data;
			private int		chunk_size;
			
			private int		num_chunks;
			private Set		chunks_received = new HashSet();
			
			protected
			fragmentAssembly(
				int		_id,
				int		_length,
				int		_chunk_size )
			{
				id			= _id;
				chunk_size	= _chunk_size;
				
				data		= new byte[_length];
				
				num_chunks = (_length + chunk_size - 1 )/chunk_size;
			}
			
			protected int
			getID()
			{
				return( id );
			}
			
			protected int
			getChunksReceived()
			{
				return( chunks_received.size());
			}
			
			protected int
			getTotalChunks()
			{
				return( num_chunks );
			}
			
			protected boolean
			receive(
				int		chunk_num,
				byte[]	chunk )
			{
				// System.out.println( "received chunk " + chunk_num + " of " + num_chunks );
				
				Integer	i = new Integer( chunk_num );
				
				if ( chunks_received.contains( i )){
					
					return( false );
				}
				
				chunks_received.add( i );
				
				System.arraycopy( chunk, 0, data, chunk_num*chunk_size, chunk.length );
				
				return( chunks_received.size() == num_chunks );
			}
			
			protected byte[]
			getData()
			{
				return( data );
			}
		}
	}	
	
	interface
	fragmentHandlerReceiver
	{
		public void
		connected();
		
		public void
		receive(
			Map			data );
		
		public void
		failed(
			Throwable	error );
	}
}
