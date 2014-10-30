/*
 * Created on Oct 13, 2014
 * Created by Paul Gardner
 * 
 * Copyright 2014 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or 
 * (at your option) any later version.
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

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.ByteArrayHashMap;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TimerEventPeriodic;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.plugins.PluginEvent;
import org.gudy.azureus2.plugins.PluginEventListener;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ipc.IPCException;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;

import com.aelitis.azureus.core.proxy.impl.AEPluginProxyHandler;
import com.aelitis.azureus.core.util.CopyOnWriteList;

public class
BuddyPluginBeta 
{
	public static final String	BETA_CHAT_KEY = 	"test:beta:chat";
	
	public static final int PRIVATE_CHAT_DISABLED			= 1;
	public static final int PRIVATE_CHAT_PINNED_ONLY		= 2;
	public static final int PRIVATE_CHAT_ENABLED			= 3;
	
	private static final int MSG_STATUS_CHAT_NONE	= 0;
	private static final int MSG_STATUS_CHAT_QUIT	= 1;
	
	
	private BuddyPlugin			plugin;
	private PluginInterface		plugin_interface;
	private BooleanParameter	enabled;
	
	private AsyncDispatcher		dispatcher = new AsyncDispatcher( "BuddyPluginBeta" );
	
	private Map<String,ChatInstance>		chat_instances = new HashMap<String, BuddyPluginBeta.ChatInstance>();

	private PluginInterface azmsgsync_pi;

	private TimerEventPeriodic		timer;
	
	private String					shared_public_nickname;
	private String					shared_anon_nickname;
	private int						private_chat_state;
	private boolean					shared_anon_endpoint;

	
	protected
	BuddyPluginBeta(
		PluginInterface		_pi,
		BuddyPlugin			_plugin,

		BooleanParameter	_enabled )
	{
		plugin_interface 	= _pi;
		plugin				= _plugin;
		enabled				= _enabled;
		
		shared_public_nickname 	= COConfigurationManager.getStringParameter( "azbuddy.chat.shared_nick", "" );
		shared_anon_nickname 	= COConfigurationManager.getStringParameter( "azbuddy.chat.shared_anon_nick", "" );
		private_chat_state	 	= COConfigurationManager.getIntParameter( "azbuddy.chat.private_chat_state", PRIVATE_CHAT_ENABLED );
		
		shared_anon_endpoint	= COConfigurationManager.getBooleanParameter( "azbuddy.chat.share_i2p_endpoint", true );
	}
	
	public String
	getSharedPublicNickname()
	{
		return( shared_public_nickname );
	}
	
	public void
	setSharedPublicNickname(
		String		_nick )
	{
		if ( !_nick.equals( shared_public_nickname )){
			
			shared_public_nickname	= _nick;
		
			COConfigurationManager.setParameter( "azbuddy.chat.shared_nick", _nick );
			
			allUpdated();		
		}	
	}
	
	public String
	getSharedAnonNickname()
	{
		return( shared_anon_nickname );
	}
	
	public void
	setSharedAnonNickname(
		String		_nick )
	{
		if ( !_nick.equals( shared_anon_nickname )){
			
			shared_anon_nickname	= _nick;
		
			COConfigurationManager.setParameter( "azbuddy.chat.shared_anon_nick", _nick );
			
			allUpdated();		
		}	
	}
	
	public int
	getPrivateChatState()
	{
		return( private_chat_state );
	}
	
	public void
	setPrivateChatState(
		int		state )
	{
		if ( state !=  private_chat_state ){
			
			private_chat_state	= state;
		
			COConfigurationManager.setParameter( "azbuddy.chat.private_chat_state", state );
			
			plugin.fireUpdated();
		}	
	}
	
	public boolean
	getSharedAnonEndpoint()
	{
		return( shared_anon_endpoint );
	}
	
	public void
	setSharedAnonEndpoint(
		boolean		b )
	{
		if ( b !=  shared_anon_endpoint ){
			
			shared_anon_endpoint	= b;
		
			COConfigurationManager.setParameter( "azbuddy.chat.share_i2p_endpoint", b );
			
			plugin.fireUpdated();
		}	
	}
	
	private void
	allUpdated()
	{
		synchronized( chat_instances ){

			for ( ChatInstance chat: chat_instances.values()){
			
				chat.updated();
			}
		}
		
		plugin.fireUpdated();
	}
	
	protected void
	startup()
	{						
		plugin_interface.addEventListener(
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
		
		PluginInterface[] plugins = plugin_interface.getPluginManager().getPlugins( true );
		
		for ( PluginInterface pi: plugins ){
			
			if ( pi.getPluginState().isOperational()){
			
				pluginAdded( pi );
			}
		}
	}
	
	protected void
	closedown()
	{
		
	}
		
	private void
	pluginAdded(
		final PluginInterface	pi )
	{
		if ( pi.getPluginID().equals( "azmsgsync" )){
			
			synchronized( chat_instances ){

				azmsgsync_pi = pi;
				
				Iterator<ChatInstance>	it = chat_instances.values().iterator();
					
				while( it.hasNext()){
					
					ChatInstance inst = it.next();
					
					try{
						inst.bind( azmsgsync_pi, null );
						
					}catch( Throwable e ){
						
						Debug.out( e );
						
						it.remove();
					}
				}
			}
			
			dispatcher.dispatch(
				new AERunnable() {
					
					@Override
					public void 
					runSupport() 
					{
						try{
							if ( Constants.isCVSVersion()){
							
								//Debug.out( "PUB CHAT DISABLED!!!!" );
								
								ChatInstance chat = getChat( AENetworkClassifier.AT_PUBLIC, BETA_CHAT_KEY );
								
								chat.setPersistent();
							}	
						}catch( Throwable e ){
							
							Debug.out( e );
						}
					}
				});

		}
	}
	
	private void
	pluginRemoved(
		PluginInterface	pi )
	{
		if ( pi.getPluginID().equals( "azmsgsync" )){
			
			synchronized( chat_instances ){

				azmsgsync_pi = null;
						
				Iterator<ChatInstance>	it = chat_instances.values().iterator();
				
				while( it.hasNext()){
					
					ChatInstance inst = it.next();
					
					inst.unbind();
					
					if ( inst.isPrivateChat()){
						
						it.remove();
					}	
				}
			}
		}
	}
		
	public boolean
	isI2PAvailable()
	{
		return( AEPluginProxyHandler.hasPluginProxyForNetwork( AENetworkClassifier.AT_I2P, false ));
	}
	
	public void
	handleURI(
		String		url_str )
		
		throws Exception
	{
		BuddyPluginViewInterface ui = plugin.getSWTUI();
		
		if ( ui == null ){
			
			throw( new Exception( "UI unavailable" ));
		}
		
		int	pos = url_str.indexOf( '?' );
		
		String key = "";
		
		if ( pos != -1 ){
			
			key = UrlUtils.decode( url_str.substring( pos+1 ));
		}
		
		String network;
		
		if ( url_str.toLowerCase( Locale.US ).startsWith( "chat:anon" )){
				
			if ( !isI2PAvailable()){
				
				throw( new Exception( "I2P unavailable" ));
			}
			
			network = AENetworkClassifier.AT_I2P;
			
		}else{
			
			network = AENetworkClassifier.AT_PUBLIC;
		}
		
		ChatInstance chat = getChat(network, key);
			
		ui.openChat( chat );
	}
	
	private String
	pkToString(
		byte[]		pk )
	{		
		byte[] temp = new byte[3];
		
		if ( pk != null ){
			
			System.arraycopy( pk, 8, temp, 0, 3 );
		}
		
		return( ByteFormatter.encodeString( temp ));
	}
	
	public ChatInstance
	getChat(
		String			network,
		String			key )		
		throws Exception
	{
		return( getChat( network, key, null, null ));
	}
	
	public ChatInstance
	getChat(
		ChatParticipant		participant )
		
		throws Exception
	{
		String key = participant.getChat().getKey() + " - " + participant.getName() + " (outgoing)";
		
		return( getChat( participant.getChat().getNetwork(), key, participant, null ));
	}
	
	public ChatInstance
	getChat(
		ChatParticipant	parent_participant,
		Object			handler )
		
		throws Exception
	{
		String key = parent_participant.getChat().getKey() + " - " + parent_participant.getName() + " (incoming)";

		return( getChat( parent_participant.getChat().getNetwork(), key, null, handler ));
	}
	
	private ChatInstance
	getChat(
		String				network,
		String				key,
		ChatParticipant		private_target,
		Object				handler )
		
		throws Exception
	{
		if ( !enabled.getValue()){
			
			throw( new Exception( "Plugin not enabled" ));
		}
		
		String meta_key = network + ":" + key;
	
		synchronized( chat_instances ){
			
			ChatInstance inst = chat_instances.get( meta_key );
			
			if ( inst == null ){
							
				inst = new ChatInstance( network, key, private_target, handler );
			
				chat_instances.put( meta_key, inst );
				
				if ( azmsgsync_pi != null ){
					
					try{
						inst.bind( azmsgsync_pi, handler );
						
					}catch( Throwable e ){
						
						chat_instances.remove( meta_key );
						
						inst.destroy();
						
						if ( e instanceof Exception ){
							
							throw((Exception)e);
						}
						
						throw( new Exception( e ));
					}
				}
			}else{
				
				inst.addReference();
			}
			
			if ( timer == null ){
				
				timer = 
					SimpleTimer.addPeriodicEvent(
						"BPB:timer",
						2500,
						new TimerEventPerformer() {
							
							public void 
							perform(
								TimerEvent event ) 
							{
								synchronized( chat_instances ){
									
									for ( ChatInstance inst: chat_instances.values()){
										
										inst.update();
									}
								}
							}
						});
			}
			
			return( inst );
		}
	}
	
	public class
	ChatInstance
	{
		private static final int	MSG_HISTORY_MAX	= 512;
		
		private final String		network;
		private final String		key;
		
		private boolean				is_private_chat;
		
		private final ChatParticipant		private_target;
		
		private volatile PluginInterface		msgsync_pi;
		private volatile Object					handler;
		
		private byte[]							my_public_key;
		
		private Object	chat_lock = this;
		
		private AtomicInteger						message_uid_next = new AtomicInteger();
		
		private List<ChatMessage>					messages = new ArrayList<ChatMessage>();
		
		private ByteArrayHashMap<ChatParticipant>	participants = new ByteArrayHashMap<ChatParticipant>();
		
		private Map<String,List<ChatParticipant>>	nick_clash_map = new HashMap<String, List<ChatParticipant>>();
		
		private CopyOnWriteList<ChatListener>		listeners = new CopyOnWriteList<ChatListener>();
		
		private boolean	persistent = false;
		
		private Map<String,Object> 	status;
		
		private boolean		is_shared_nick;
		private String		instance_nick;
		
		private int			reference_count;
		
		private
		ChatInstance(
			String				_network,
			String				_key,
			ChatParticipant		_private_target,
			Object				_handler )
		{
			network 		= _network;
			key				= _key;
			
				// private chat args
			
			private_target	= _private_target;
			
			is_private_chat = private_target != null || _handler != null;
			
			String chat_key_base = "azbuddy.chat." + getNetAndKey();
			
			String shared_key 	= chat_key_base + ".shared";
			String nick_key 	= chat_key_base + ".nick";

			is_shared_nick 	= COConfigurationManager.getBooleanParameter( shared_key, true );
			instance_nick 	= COConfigurationManager.getStringParameter( nick_key, "" );
			
			addReference();
		}
		
		protected void
		addReference()
		{
			synchronized( chat_lock ){
				
				reference_count++;
			}
		}
		
		public String
		getName()
		{
			return( MessageText.getString(network==AENetworkClassifier.AT_PUBLIC?"label.public":"label.anon") + " - '" + key + "'" );
		}
		
		public String
		getNetwork()
		{
			return( network );
		}
		
		public String
		getKey()
		{
			return( key );
		}
		
		public byte[]
		getPublicKey()
		{
			return( my_public_key );
		}
		
		public boolean
		isPrivateChat()
		{
			return( is_private_chat );
		}
		
		public String
		getNetAndKey()
		{
			return( network + ": " + key );
		}
		
		public void
		setPersistent()
		{
			persistent	= true;
		}
		
		public boolean
		isSharedNickname()
		{
			return( is_shared_nick );
		}
		
		public void
		setSharedNickname(
			boolean		_shared ) 
		{
			if ( _shared != is_shared_nick ){
			
				is_shared_nick	= _shared;
			
				String chat_key_base = "azbuddy.chat." + getNetAndKey();

				String shared_key 	= chat_key_base + ".shared";

				COConfigurationManager.setParameter( shared_key, _shared );
				
				updated();
			}
		}
		
		public String
		getInstanceNickname()
		{
			return( instance_nick );
		}
		
		public void
		setInstanceNickname(
			String		_nick )
		{
			if ( !_nick.equals( instance_nick )){
				
				instance_nick	= _nick;
			
				String chat_key_base = "azbuddy.chat." + getNetAndKey();

				String nick_key 	= chat_key_base + ".nick";

				COConfigurationManager.setParameter( nick_key, _nick );
				
				updated();
			}
		}
		
		public String
		getNickname()
		{
			if ( is_shared_nick ){
				
				return( network == AENetworkClassifier.AT_PUBLIC?shared_public_nickname:shared_anon_nickname );
				
			}else{
				
				return( instance_nick );
			}
		}
		
		private Object
		getHandler()
		{
			return( handler );
		}
		
		private void
		bind(
			PluginInterface		_msgsync_pi,
			Object				_handler )
		
			throws Exception
		{	
			msgsync_pi = _msgsync_pi;

			if ( _handler != null ){
				
				handler		= _handler;
				
				try{
					Map<String,Object>		options = new HashMap<String, Object>();
							
					options.put( "handler", _handler );
					
					options.put( "addlistener", this );
							
					Map<String,Object> reply = (Map<String,Object>)msgsync_pi.getIPC().invoke( "updateMessageHandler", new Object[]{ options } );
						
					my_public_key = (byte[])reply.get( "pk" );

					for ( ChatListener l: listeners ){
						
						try{
							l.stateChanged( true );
							
						}catch( Throwable e ){
							
							Debug.out( e );
						}
					}
				}catch( Throwable e ){
					
					throw( new Exception( e ));
				}
			}else{
			
				try{
					Map<String,Object>		options = new HashMap<String, Object>();
					
					options.put( "network", network );
					options.put( "key", key.getBytes( "UTF-8" ));
						
					if ( private_target != null ){
					
						options.put( "parent_handler", private_target.getChat().getHandler());
						options.put( "target_pk", private_target.getPublicKey());
						options.put( "target_contact", private_target.getContact());
					}
					
					if ( network != AENetworkClassifier.AT_PUBLIC ){
						
						options.put( "server_id", getSharedAnonEndpoint()?"dchat_shared":"dchat" );
					}
					
					options.put( "listener", this );
							
					Map<String,Object> reply = (Map<String,Object>)msgsync_pi.getIPC().invoke( "getMessageHandler", new Object[]{ options } );
					
					handler = reply.get( "handler" );
					
					my_public_key = (byte[])reply.get( "pk" );
					
					for ( ChatListener l: listeners ){
						
						try{
							l.stateChanged( true );
							
						}catch( Throwable e ){
							
							Debug.out( e );
						}
					}
				}catch( Throwable e ){
					
					throw( new Exception( e ));
				}
			}
		}
		
		private void
		unbind()
		{
			for ( ChatListener l: listeners ){
				
				try{
					l.stateChanged( false );
					
				}catch( Throwable e ){
					
					Debug.out( e );
				}
			}
			
			handler 	= null;
			msgsync_pi	= null;
		}
		
		public boolean
		isAvailable()
		{
			return( handler != null );
		}
		
		public ChatMessage[]
		getHistory()
		{
			synchronized( chat_lock ){
				
				return( messages.toArray( new ChatMessage[ messages.size() ]));
			}
		}
		
		private void
		update()
		{
			PluginInterface		current_pi 			= msgsync_pi;
			Object 				current_handler 	= handler;
			
			if ( current_handler != null && current_pi != null ){
							
				try{
					Map<String,Object>		options = new HashMap<String, Object>();
					
					options.put( "handler", current_handler );
	
					status = (Map<String,Object>)current_pi.getIPC().invoke( "getStatus", new Object[]{ options } );
					
				}catch( Throwable e ){
					
					Debug.out( e );
				}
			}
			
			updated();
		}
		
		private void
		updated()
		{
			for ( ChatListener l: listeners ){
				
				try{	
					l.updated();
					
				}catch( Throwable e ){
				
					Debug.out( e );
				}
			}
		}
		
		public String
		getStatus()
		{
			PluginInterface		current_pi 			= msgsync_pi;
			Object 				current_handler 	= handler;

			if ( current_pi == null ){
				
				return( "Error: Message Sync Plugin not installed" );
			}
			
			if ( current_handler == null ){
				
				return( "Error: No handler" );
			}
			
			Map<String,Object> map = status;
			
			if ( map == null ){
				
				return( "No status available yet" );
				
			}else{
				int status 			= ((Number)map.get( "status" )).intValue();
				int dht_count 		= ((Number)map.get( "dht_nodes" )).intValue();
				
				int nodes_local 	= ((Number)map.get( "nodes_local" )).intValue();
				int nodes_live 		= ((Number)map.get( "nodes_live" )).intValue();
				int nodes_dying 	= ((Number)map.get( "nodes_dying" )).intValue();
				
				int req_in 			= ((Number)map.get( "req_in" )).intValue();
				double req_in_rate 	= ((Number)map.get( "req_in_rate" )).doubleValue();
				int req_out_ok 		= ((Number)map.get( "req_out_ok" )).intValue();
				int req_out_fail 	= ((Number)map.get( "req_out_fail" )).intValue();
				double req_out_rate = ((Number)map.get( "req_out_rate" )).doubleValue();
									
				if ( status == 0 || status == 1 ){
					
					String	result = "";

					if ( isPrivateChat()){
						
						result = "Private chat";
						
					}else{
						
						if ( status == 0 ){
						
							result = "Initialising: dht=" + (dht_count<0?"...":String.valueOf(dht_count));
						
						}else if ( status == 1 ){
						
							result = "dht=" + dht_count;
						}
					}
					
					result += 	", nodes=" + nodes_local+"/"+nodes_live+"/"+nodes_dying +
								", req=" + DisplayFormatters.formatDecimal(req_in_rate,1) + "/" +  DisplayFormatters.formatDecimal(req_out_rate,1);
					
					return( result );
					
				}else{
					
					return( "Destroyed" );
				}
			}
		}
		
		private TimerEvent	sort_event;
		private boolean		sort_force_changed;
		
		private void
		sortMessages(
			boolean		force_change )
		{
			synchronized( chat_lock ){
				
				if ( force_change ){
					
					sort_force_changed = true;
				}
				
				if ( sort_event != null ){
					
					return;
				}
				
				sort_event = 
					SimpleTimer.addEvent(
						"msgsort",
						SystemTime.getOffsetTime( 500 ),
						new TimerEventPerformer()
						{
							public void 
							perform(
								TimerEvent event) 
							{
								boolean	changed = false;
								
								synchronized( chat_lock ){
									
									sort_event = null;
									
									changed = sortMessagesSupport();
									
									if ( sort_force_changed ){
										
										changed = true;
												
										sort_force_changed = false;
									}
								}
								
								if ( changed ){
									
									for ( ChatListener l: listeners ){
										
										l.messagesChanged();
									}
								}
							}
						});
			}
		}
		
		private boolean
		sortMessagesSupport()
		{
			int	num_messages = messages.size();
			
			ByteArrayHashMap<ChatMessage>	id_map 		= new ByteArrayHashMap<ChatMessage>( num_messages );
			Map<ChatMessage,ChatMessage>	prev_map 	= new HashMap<ChatMessage,ChatMessage>( num_messages );
			Map<ChatMessage,ChatMessage>	next_map 	= new HashMap<ChatMessage,ChatMessage>( num_messages );
			
				// build id map so we can lookup prev messages
			
			for ( ChatMessage msg: messages ){
				
				byte[]	id = msg.getID();
				
				id_map.put( id, msg );
			}
			
				// build sets of prev/next links 
			
			for ( ChatMessage msg: messages ){
				
				byte[]	prev_id 	= msg.getPreviousID();
				
				if ( prev_id != null ){
					
					ChatMessage prev_msg = id_map.get( prev_id );
					
					if ( prev_msg != null ){
						
						msg.setPreviousID( prev_msg.getID());	// save some mem
						
						// ordering prev_msg::msg
					
						prev_map.put( msg, prev_msg );
						next_map.put( prev_msg, msg );
					}
				}
			}
			
				// a comparator to consistently order messages to ensure sorting is determinstic
			
			Comparator<ChatMessage> message_comparator = 
					new Comparator<ChatMessage>()
					{					
						public int 
						compare(
							ChatMessage o1, 
							ChatMessage o2 ) 
						{
							return( o1.getUID() - o2.getUID());
						}
					};
					
				// break any loops arbitrarily
		
			Set<ChatMessage>	linked_messages = new TreeSet<ChatMessage>(	message_comparator );
			
			linked_messages.addAll( prev_map.keySet());
			
			while( linked_messages.size() > 0 ){
				
				ChatMessage start = linked_messages.iterator().next();
					
				linked_messages.remove( start );
				
				ChatMessage current = start;
				
				int	loops = 0;
				
				while( true ){
				
					loops++;
					
					if ( loops > num_messages ){
						
						Debug.out( "infinte loop" );
						
						break;
					}
					
					ChatMessage prev_msg = prev_map.get( current );
					
					if ( prev_msg == null ){
						
						break;
						
					}else{
						
						linked_messages.remove( prev_msg );
						
						if ( prev_msg == start ){
												
								// loopage
							
							prev_map.put( current, null );
							next_map.put( prev_msg, null );
							
							break;
							
						}else{
							
							current = prev_msg;
						}
					}
				}
				
			}
				// find the heads of the various chains
			
			Set<ChatMessage>		chain_heads = new TreeSet<ChatMessage>( message_comparator );
			
			for ( ChatMessage msg: messages ){
				
				ChatMessage prev_msg = prev_map.get( msg );
				
				if ( prev_msg != null ){
					
					int	 loops = 0;
					
					while( true ){
					
						loops++;
						
						if ( loops > num_messages ){
							
							Debug.out( "infinte loop" );
							
							break;
						}
						
						ChatMessage prev_prev = prev_map.get( prev_msg );
						
						if ( prev_prev == null ){
							
							chain_heads.add( prev_msg );
							
							break;
							
						}else{
							
							prev_msg = prev_prev;
						}
					}
				}
			}
			
			Set<ChatMessage>	remainder_set = new HashSet<BuddyPluginBeta.ChatMessage>( messages );
			
			List<ChatMessage> result = null;
			
			for ( ChatMessage head: chain_heads ){
				
				List<ChatMessage>	chain = new ArrayList<ChatMessage>( num_messages );

				ChatMessage msg = head;
						
				while( msg != null ){
					
					chain.add( msg );
					
					remainder_set.remove( msg );
					
					msg = next_map.get( msg );
				}
				
				if ( result == null ){
					
					result = chain;
					
				}else{
					
					result = merge( result, chain );
				}
			}
			
			if ( remainder_set.size() > 0 ){
				
					// these are messages not part of any chain so sort based on time
				
				List<ChatMessage>	remainder = new ArrayList<ChatMessage>( remainder_set );
				
				Collections.sort(
						remainder,
						new Comparator<ChatMessage>()
						{
							public int 
							compare(
								ChatMessage m1, 
								ChatMessage m2 ) 
							{
								long l = m1.getTimeStamp() - m2.getTimeStamp();
								
								if ( l < 0 ){
									return( -1 );
								}else if ( l > 0 ){
									return( 1 );
								}else{
									return( m1.getUID() - m2.getUID());
								}
							}
						});
				
				if ( result == null ){
					
					result = remainder;
					
				}else{
					
					result = merge( result, remainder );
				}
			}
						
			if ( result == null ){
						
				return( false );
			}
			
			boolean	changed = false;

			if ( messages.size() != result.size()){
					
				Debug.out( "Inconsistent: " + messages.size() + "/" + result.size());

				changed = true;
			}
					
			Set<ChatParticipant>	participants = new HashSet<ChatParticipant>();
			
			for ( int i=0;i<result.size();i++){
				
				ChatMessage msg = result.get(i);
				
				ChatParticipant p = msg.getParticipant();
					
				participants.add( p );
				
				if ( !changed ){
						
					if ( messages.get(i) != msg ){
					
						changed = true;
					}
				}
			}
				
			if ( changed ){
				
				messages = result;
				
				for ( ChatParticipant p: participants ){
					
					p.resetMessages();
				}
				
				Set<ChatParticipant>	updated = new HashSet<ChatParticipant>();
				
				for ( ChatMessage msg: messages ){
					
					ChatParticipant p = msg.getParticipant();
					
					if ( p.replayMessage( msg )){
						
						updated.add( p );
					}
				}
				
				for ( ChatParticipant p: updated ){
					
					updated( p );
				}
			}
			
			return( changed );
		}
		
		private List<ChatMessage>
		merge(
			List<ChatMessage>		list1,
			List<ChatMessage>		list2 )
		{
			int	size1 = list1.size();
			int size2 = list2.size();
			
			List<ChatMessage>	result = new ArrayList<ChatMessage>( size1 + size2 );
			
			int	pos1 = 0;
			int pos2 = 0;
			
			while( true ){
				
				if ( pos1 == size1 ){
					
					for ( int i=pos2;i<size2;i++){
						
						result.add( list2.get(i));
					}
					
					break;
					
				}else if ( pos2 == size2 ){
					
					for ( int i=pos1;i<size1;i++){
						
						result.add( list1.get(i));
					}
					
					break;
					
				}else{
					
					ChatMessage m1 = list1.get( pos1 );
					ChatMessage m2 = list2.get( pos2 );
				
					long	t1 = m1.getTimeStamp();
					long	t2 = m2.getTimeStamp();
					
					if ( t1 < t2 || ( t1 == t2 && m1.getUID() < m2.getUID())){
						
						result.add( m1 );
						
						pos1++;
						
					}else{
						
						result.add( m2 );
						
						pos2++;
					}
				}
			}
			
			return( result );
		}
		
		public void
		messageReceived(
			Map<String,Object>			message_map )
			
			throws IPCException
		{
			ChatMessage msg = new ChatMessage( message_uid_next.incrementAndGet(), message_map );
						
			ChatParticipant	new_participant = null;
				
			boolean	sort_outstanding = false;
			
			byte[]	prev_id 	= msg.getPreviousID();
						
			synchronized( chat_lock ){
				
					// best case is that message belongs at the end
				
				int old_msgs = messages.size();

				messages.add( msg );
		
				if ( messages.size() > MSG_HISTORY_MAX ){
					
					ChatMessage removed = messages.remove(0);
					
					removed.getParticipant().removeMessage( removed );
				}
				
				byte[] pk = msg.getPublicKey();
				
				ChatParticipant participant = participants.get( pk );
								
				if ( participant == null ){
					
					new_participant = participant = new ChatParticipant( this, pk );
					
					participants.put( pk, participant );
										
					participant.addMessage( msg );
										
				}else{
										
					participant.addMessage( msg );										
				}
								
				if ( sort_event != null ){
					
					sort_outstanding = true;
					
				}else{
					
					if ( old_msgs == 0 ){	
						
					}else if ( prev_id != null && Arrays.equals( prev_id, messages.get(old_msgs-1).getID())){

						// in right place already by linkage
						
					}else if ( msg.getMessageType() != ChatMessage.MT_NORMAL ){

						// info etc always go last
						
					}else{
						
						sortMessages( true );
						
						sort_outstanding = true;
					}
				}
			}
			
			if ( new_participant != null ){
				
				for ( ChatListener l: listeners ){
					
					l.participantAdded( new_participant );
				}
			}
			
			if ( !sort_outstanding ){
				
				for ( ChatListener l: listeners ){
					
					l.messageReceived( msg );
				}
			}
		}
		
		public Map<String,Object>
		chatRequested(
			Map<String,Object>			message_map )
			
			throws IPCException
		{
			if ( private_chat_state == PRIVATE_CHAT_DISABLED ){
				
				throw( new IPCException( "Private chat disabled by recipient" ));
			}
			
			try{
				Object	new_handler 	= message_map.get( "handler" );
				
				byte[]	remote_pk 		= (byte[])message_map.get( "pk" );
				
				ChatParticipant	participant;
				
				synchronized( chat_lock ){
	
					participant = participants.get( remote_pk );
				}
				
				if ( participant == null ){
					
					throw( new IPCException( "Private chat requires you send at least one message to the main chat first" ));
				}
					
				if ( private_chat_state == PRIVATE_CHAT_PINNED_ONLY && !participant.isPinned()){
					
					throw( new IPCException( "Recipient will only accept private chats from pinned participants" ));
				}
				
				BuddyPluginViewInterface ui = plugin.getSWTUI();
				
				if ( ui == null ){
					
					throw( new IPCException( "Chat unavailable" ));
				}
									
				ChatInstance inst = getChat( participant, new_handler );

				if ( !isSharedNickname()){
					
					inst.setSharedNickname( false );
					
					inst.setInstanceNickname( getInstanceNickname());
				}
				
				ui.openChat( inst );
				
				Map<String,Object>	reply = new HashMap<String, Object>();
				
				reply.put( "nickname", participant.getName());
				
				return( reply );
				
			}catch( IPCException e ){
				
				throw( e );
				
			}catch( Throwable e ){
				
				throw( new IPCException( e ));
			}
		}
		
		AsyncDispatcher	dispatcher = new AsyncDispatcher( "sendAsync" );
		
		public void 
		sendMessage(
			final String		message )
		{
			dispatcher.dispatch(
				new AERunnable()
				{
					
					@Override
					public void 
					runSupport() 
					{
						sendMessageSupport( message, null );
					}
				});
		}
		
		private void 
		sendMessageSupport(
			String		message,
			Map			flags )
		{
			if ( handler == null || msgsync_pi == null ){
				
				Debug.out( "No handler/plugin" );
				
			}else{
				
				if ( message.equals( "!dump!" )){
					
					synchronized( chat_lock ){
						
						for ( ChatMessage msg: messages ){
							
							System.out.println( pkToString( msg.getID()) + ", " + pkToString( msg.getPreviousID()) + " - " + msg.getMessage());
						}
					}
					return;
					
				}else if ( message.equals( "!sort!" )){
					
					sortMessages( false );
					
					return;
				}
				
				try{
					ChatMessage		prev_message = null;
					
					synchronized( chat_lock ){
						
						if ( messages.size() > 0 ){
							
							prev_message = messages.get( messages.size()-1);
						}
					}
					
					Map<String,Object>		options = new HashMap<String, Object>();
					
					options.put( "handler", handler );
					
					Map<String,Object>	payload = new HashMap<String, Object>();
					
					payload.put( "msg", message.getBytes( "UTF-8" ));
					
					payload.put( "nick", getNickname().getBytes( "UTF-8" ));
					
					if ( prev_message != null ){
						
						payload.put( "pre", prev_message.getID());
					}
					
					if ( flags != null ){
						
						payload.put( "f", flags );
					}
					
					options.put( "content", BEncoder.encode( payload ));
												
					Map<String,Object> reply = (Map<String,Object>)msgsync_pi.getIPC().invoke( "sendMessage", new Object[]{ options } );
					
				}catch( Throwable e ){
					
					Debug.out( e );
				}
			}
		}
		
		public ChatParticipant[]
		getParticipants()
		{
			synchronized( chat_lock ){
				
				return( participants.values().toArray( new ChatParticipant[ participants.size()]));
			}
		}
		
		protected void
		updated(
			ChatParticipant		p )
		{
			for ( ChatListener l: listeners ){
				
				l.participantChanged( p );
			}
		}
		
		private void
		registerNick(
			ChatParticipant		p,
			String				old_nick,
			String				new_nick )
		{
			synchronized( chat_lock ){
				
				if ( old_nick != null ){
				
					List<ChatParticipant> list = nick_clash_map.get( old_nick );
					
					if ( list != null && list.remove( p )){
												
						if ( list.size() == 0 ){
							
							nick_clash_map.remove( old_nick );
							
						}else{
							
							if ( list.size() == 1 ){
							
								list.get(0).setNickClash( false );
							}
						}
					}else{
						
						Debug.out( "inconsistent" );
					}
				}
				
				List<ChatParticipant> list = nick_clash_map.get( new_nick );
	
				if ( list == null ){
					
					list = new ArrayList<BuddyPluginBeta.ChatParticipant>();
					
					nick_clash_map.put( new_nick, list );
				}
				
				if ( list.contains( p )){
					
					Debug.out( "inconsistent" );
					
				}else{
					
					list.add( p );
					
					if ( list.size() > 1 ){
						
						p.setNickClash( true );
						
						if ( list.size() == 2 ){
							
							list.get(0).setNickClash( true );
						}
					}else{
						
						p.setNickClash( false );
					}
				}
			}
		}
		
		public void
		addListener(
			ChatListener		listener )
		{
			listeners.add( listener );
		}
		
		public void
		removeListener(
			ChatListener		listener )
		{
			listeners.remove( listener );
		}
		
		public void
		destroy()
		{
			synchronized( chat_lock ){
				
				reference_count--;
				
				if ( reference_count > 0 ){
					
					return;
				}
			}
			
			if ( !persistent ){
				
				if ( handler != null ){
							
					if ( is_private_chat ){
						
						Map<String,Object>		flags = new HashMap<String, Object>();
						
						flags.put( "s", MSG_STATUS_CHAT_QUIT );
						
						sendMessageSupport( "", flags );
					}
					
					try{
						Map<String,Object>		options = new HashMap<String, Object>();
						
						options.put( "handler", handler );
													
						Map<String,Object> reply = (Map<String,Object>)msgsync_pi.getIPC().invoke( "removeMessageHandler", new Object[]{ options } );
						
					}catch( Throwable e ){
						
						Debug.out( e );
						
					}finally{
						
						String meta_key = network + ":" + key;

						synchronized( chat_instances ){
						
							chat_instances.remove( meta_key );
							
							if ( chat_instances.size() == 0 ){
								
								if ( timer != null ){
									
									timer.cancel();
									
									timer = null;
								}
							}
						}
					}
				}
			}
		}
	}
	
	public class
	ChatParticipant
	{
		private final ChatInstance		chat;
		private final byte[]			pk;
		
		private String				nickname;
		private boolean				is_ignored;
		private boolean				is_pinned;
		private boolean				nick_clash;
		
		private List<ChatMessage>	messages	= new ArrayList<ChatMessage>();
		
		private Boolean				is_me;
		
		private
		ChatParticipant(
			ChatInstance		_chat,
			byte[]				_pk )
		{
			chat	= _chat;
			pk		= _pk;
			
			nickname = pkToString( pk );
			
			is_pinned = COConfigurationManager.getBooleanParameter( getPinKey(), false );
			
			chat.registerNick( this, null, nickname );
		}
		
		public ChatInstance
		getChat()
		{
			return( chat );
		}
	
		public byte[]
		getPublicKey()
		{
			return( pk );
		}
		
		public Map<String,Object>
		getContact()
		{
			synchronized( chat.chat_lock ){
			
				return( messages.get( messages.size()-1).getContact());
			}
		}
		
		public InetSocketAddress
		getAddress()
		{
			synchronized( chat.chat_lock ){
			
				return( messages.get( messages.size()-1).getAddress());
			}
		}
		
		public boolean
		isMe()
		{
			if ( is_me != null ){
				
				return( is_me );
			}
			
			byte[] chat_key = chat.getPublicKey();
			
			if ( chat_key != null ){
			
				is_me = Arrays.equals( pk, chat_key );
			}
			
			return( is_me );
		}
		
		public String
		getName() 
		{
			return( nickname );
		}
		
		public boolean
		hasNickname()
		{
			return( !nickname.equals( pkToString( pk )));
		}
		
		private void
		addMessage(
			ChatMessage		message )
		{
			messages.add( message );
			
			message.setParticipant( this );
			
			message.setIgnored( is_ignored );
			
			String new_nickname = message.getNickName();
			
			if ( !nickname.equals( new_nickname )){
			
				chat.registerNick( this, nickname, new_nickname );

				message.setNickClash( isNickClash());
				
				nickname = new_nickname;
								
				chat.updated( this );
				
			}else{
				
				message.setNickClash( isNickClash());
			}
		}
		
		private boolean
		replayMessage(
			ChatMessage		message )
		{
			messages.add( message );
						
			message.setIgnored( is_ignored );

			String new_nickname = message.getNickName();
			
			if ( !nickname.equals( new_nickname )){
			
				chat.registerNick( this, nickname, new_nickname );

				message.setNickClash( isNickClash());
				
				nickname = new_nickname;
								
				return( true );
				
			}else{
				
				message.setNickClash( isNickClash());
				
				return( false );
			}
		}
		
		private void
		removeMessage(
			ChatMessage		message )
		{
			messages.remove( message );
		}
		
		private void
		resetMessages()
		{
			String new_nickname = pkToString( pk );
			
			if ( !nickname.equals( new_nickname )){
				
				chat.registerNick( this, nickname, new_nickname );

				nickname = new_nickname;
			}
			
			messages.clear();
		}
		
		public List<ChatMessage>
		getMessages()
		{
			synchronized( chat.chat_lock ){
				
				return( new ArrayList<ChatMessage>( messages ));
			}
		}
		
		public boolean
		isIgnored()
		{
			return( is_ignored );
		}
		
		public void
		setIgnored(
			boolean		b )
		{
			if ( b != is_ignored ){
				
				is_ignored = b;
				
				synchronized( chat.chat_lock ){

					for ( ChatMessage message: messages ){
						
						message.setIgnored( b );
					}
				}
			}
		}
		
		public boolean
		isPinned()
		{
			return( is_pinned );
		}
		
		private String
		getPinKey()
		{
			return( "azbuddy.chat.pinned." + ByteFormatter.encodeString( pk, 0, 16 ));
		}
		
		public void
		setPinned(
			boolean		b )
		{
			if ( b != is_pinned ){
			
				is_pinned = b;
				
				String key = getPinKey();
				
				if ( is_pinned ){
				
					COConfigurationManager.setParameter( key, true );
					
				}else{
				
					COConfigurationManager.removeParameter( key );
				}
				
				COConfigurationManager.setDirty();
			}
		}
		
		public boolean
		isNickClash()
		{
			return( nick_clash );
		}
		
		private void
		setNickClash(
			boolean	b )
		{
			nick_clash = b;
		}
		
		public ChatInstance
		createPrivateChat()
		
			throws Exception
		{			
			ChatInstance inst = BuddyPluginBeta.this.getChat( this );
			
			ChatInstance	parent = getChat();
			
			if ( !parent.isSharedNickname()){
				
				inst.setSharedNickname( false );
				
				inst.setInstanceNickname( parent.getInstanceNickname());
			}

			return( inst );
		}
	}
	
	public class
	ChatMessage
	{
		public static final int MT_NORMAL	= 1;
		public static final int MT_INFO		= 2;
		public static final int MT_ERROR	= 3;
		
		private final int						uid;
		private final Map<String,Object>		map;
		
		private ChatParticipant					participant;

		private final byte[]					message_id;
		
		private byte[]							previous_id;
		
		private long							timestamp;
		
		private boolean							is_ignored;
		private boolean							is_nick_clash;
		
		private
		ChatMessage(
			int						_uid,
			Map<String,Object>		_map )
		{
			uid		= _uid;
			map		= _map;
			
			message_id = (byte[])map.get( "id" );
			
			timestamp = SystemTime.getCurrentTime() - getAge()*1000;

			Map<String,Object> payload = getPayload();
			
			previous_id = (byte[])payload.get( "pre" );
		}
		
		protected int
		getUID()
		{
			return( uid );
		}
		
		private void
		setParticipant(
			ChatParticipant		p )
		{
			participant	= p;
		}
		
		public ChatParticipant
		getParticipant()
		{
			return( participant );
		}
		
		private void
		setNickClash(
			boolean	clash )
		{
			is_nick_clash = clash;
		}
		
		public boolean
		isNickClash()
		{
			return( is_nick_clash );
		}
		
		private Map<String,Object>
		getPayload()
		{
			try{
				byte[] content_bytes = (byte[])map.get( "content" );
				
				if ( content_bytes != null && content_bytes.length > 0 ){
					
					return( BDecoder.decode( content_bytes ));
				}
			}catch( Throwable e){
			}
			
			return( new HashMap<String,Object>());
		}
		
		private int
		getMessageStatus()
		{
			Map<String,Object> payload = getPayload();

			if ( payload != null ){
				
				Map<String,Object>	flags = (Map<String,Object>)payload.get( "f" );
				
				if ( flags != null ){
					
					Number status = (Number)flags.get( "s" );
					
					if ( status != null ){
						
						return( status.intValue());
					}
				}
			}
			
			return( MSG_STATUS_CHAT_NONE );
		}
		
		public String
		getMessage()
		{
			try{
				String	report = (String)map.get( "error" );
				
				if ( report != null ){
					
					if ( report.length() > 2 && report.charAt(1) == ':' ){
						
						return( report.substring( 2 ));
					}
					
					return( report );
				}
				
				if ( getMessageStatus() == MSG_STATUS_CHAT_QUIT ){
					
					return( participant.getName() + " has quit" );
				}
				
					// was just a string for a while...
				
				Map<String,Object> payload = getPayload();
					
				if ( payload != null ){
					
					byte[] msg_bytes = (byte[])payload.get( "msg" );
					
					if ( msg_bytes != null ){
					
						return( new String( msg_bytes, "UTF-8" ));
					}
				}
				
				
				
				return( new String((byte[])map.get( "content" ), "UTF-8" ));
				
			}catch( Throwable e ){
				
				Debug.out( e );
					
				return( "" );
			}
		}
		
		public int
		getMessageType()
		{
			String	report = (String)map.get( "error" );
			
			if ( report == null ){
				
				if ( getMessageStatus() == MSG_STATUS_CHAT_QUIT ){
					
					return( MT_INFO );
				}
				
				return( MT_NORMAL );
				
			}else{
				
				if ( report.length() < 2 || report.charAt(1) != ':' ){
					
					return( MT_ERROR );
				}
								
				char type = report.charAt(0);
				
				if ( type == 'i' ){
					
					return( MT_INFO );
				}else{
					
					return( MT_ERROR );
				}
			}
		}
		
		public boolean
		isIgnored()
		{
			return( is_ignored );
		}
		
		public void
		setIgnored(
			boolean		b )
		{
			is_ignored = b;
		}
		
		public byte[]
		getID()
		{
			return( message_id );
		}
		
		public byte[]
		getPreviousID()
		{
			return( previous_id );
		}
		
		private void
		setPreviousID(
			byte[]		pid )
		{
			previous_id = pid;
		}
		
		public byte[]
		getPublicKey()
		{
			return((byte[])map.get( "pk" ));
		}
		
		public Map<String,Object>
		getContact()
		{
			return((Map<String,Object>)map.get( "contact" ));
		}
		
		public InetSocketAddress
		getAddress()
		{
			return((InetSocketAddress)map.get( "address" ));
		}
		
		private int
		getAge()
		{
			return(((Number)map.get( "age" )).intValue());
		}
		
		public long
		getTimeStamp()
		{
			return( timestamp );
		}
		
		public String
		getNickName()
		{		
				// always use payload if available (for real messages)
			
			Map<String,Object> payload = getPayload();
			
			if ( payload != null ){
				
				byte[] nick = (byte[])payload.get( "nick" );
				
				if ( nick != null ){
					
					try{
						String str = new String( nick, "UTF-8" );
						
						if ( str.length() > 0 ){
						
							return( str );
						}
					}catch( Throwable e ){
					}
				}
			}
			
				// otherwise assume it is internally generated for non-normal messages
			
			if ( getMessageType() != ChatMessage.MT_NORMAL ){
				
				String nick = participant.getChat().getNickname();
				
				if ( nick.length() > 0 ){
					
					return( nick );
				}
			}

				// default when no user specified one present
			
			return( pkToString( getPublicKey()));
		}
	}
	
	public interface
	ChatListener
	{
		public void
		messageReceived(
			ChatMessage				message );
		
		public void
		messagesChanged();
		
		public void
		participantAdded(
			ChatParticipant			participant );
		
		public void
		participantChanged(
			ChatParticipant			participant );
		
		public void
		participantRemoved(
			ChatParticipant			participant );
		
		public void
		stateChanged(
			boolean					avail );
		
		public void
		updated();
	}
}
