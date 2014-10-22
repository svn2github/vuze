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

import java.util.*;

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
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;

import com.aelitis.azureus.core.proxy.impl.AEPluginProxyHandler;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.plugins.net.buddy.swt.BuddyPluginViewBetaChat;

public class
BuddyPluginBeta 
{
	public static final String	BETA_CHAT_KEY = 	"test:beta:chat";
	
	private BuddyPlugin			plugin;
	private PluginInterface		plugin_interface;
	private BooleanParameter	enabled;
	
	private AsyncDispatcher		dispatcher = new AsyncDispatcher( "BuddyPluginBeta" );
	
	private Map<String,ChatInstance>		chat_instances = new HashMap<String, BuddyPluginBeta.ChatInstance>();

	private PluginInterface azmsgsync_pi;

	private TimerEventPeriodic		timer;
	
	private String					shared_public_nickname;
	private String					shared_anon_nickname;
	
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
				
				for ( ChatInstance inst: chat_instances.values() ){
					
					inst.bind( azmsgsync_pi );
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
								
								getChat( AENetworkClassifier.AT_PUBLIC, BETA_CHAT_KEY, true );
								
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
				
				for ( ChatInstance inst: chat_instances.values() ){
					
					inst.unbind();
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
				
			if ( !plugin.getBeta().isI2PAvailable()){
				
				throw( new Exception( "I2P unavailable" ));
			}
			
			network = AENetworkClassifier.AT_I2P;
			
		}else{
			
			network = AENetworkClassifier.AT_PUBLIC;
		}
		
		ChatInstance chat = plugin.getBeta().getChat(network, key);
			
		ui.openChat( chat );
	}
	
	public ChatInstance
	getChat(
		String			network,
		String			key )
		
		throws Exception
	{
		return( getChat( network, key, false ));
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
		String			key,
		boolean			persistent )
		
		throws Exception
	{
		if ( !enabled.getValue()){
			
			throw( new Exception( "Plugin not enabled" ));
		}
		
		String meta_key = network + ":" + key;
	
		synchronized( chat_instances ){
			
			ChatInstance inst = chat_instances.get( meta_key );
			
			if ( inst == null ){
							
				inst = new ChatInstance( network, key );
			
				chat_instances.put( meta_key, inst );
				
				if ( azmsgsync_pi != null ){
					
					inst.bind( azmsgsync_pi );
				}
			}else{
				
				inst.addReference();
			}
			
			if ( persistent ){
				
				inst.setPersistent();
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
		
		private volatile PluginInterface		msgsync_pi;
		private volatile Object					handler;
		
		private List<ChatMessage>		messages = new ArrayList<ChatMessage>();
		
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
			String				_key )
		{
			network 	= _network;
			key			= _key;
			
			String chat_key_base = "azbuddy.chat." + getKey();
			
			String shared_key 	= chat_key_base + ".shared";
			String nick_key 	= chat_key_base + ".nick";

			is_shared_nick 	= COConfigurationManager.getBooleanParameter( shared_key, true );
			instance_nick 	= COConfigurationManager.getStringParameter( nick_key, "" );
			
			addReference();
		}
		
		protected void
		addReference()
		{
			synchronized( this ){
				
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
			
				String chat_key_base = "azbuddy.chat." + getKey();

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
			
				String chat_key_base = "azbuddy.chat." + getKey();

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
		
		private void
		bind(
			PluginInterface	_msgsync_pi )
		{		
			msgsync_pi = _msgsync_pi;
			
			try{
				Map<String,Object>		options = new HashMap<String, Object>();
				
				options.put( "network", network );
				options.put( "key", key.getBytes( "UTF-8" ));
					
				options.put( "listener", this );
						
				Map<String,Object> reply = (Map<String,Object>)msgsync_pi.getIPC().invoke( "getMessageHandler", new Object[]{ options } );
				
				handler = reply.get( "handler" );
				
				for ( ChatListener l: listeners ){
					
					l.stateChanged( true );
				}
			}catch( Throwable e ){
				
				Debug.out(e );
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
			synchronized( this ){
				
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
			if ( handler == null ){
				
				return( "Unavailable" );
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

					if ( status == 0 ){
					
						result = "Initialising: dht=" + (dht_count<0?"...":String.valueOf(dht_count));
					
					}else if ( status == 1 ){
					
						result = "dht=" + dht_count;
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
		
		private void
		sortMessages()
		{
			synchronized( this ){
				
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
								synchronized( ChatInstance.this ){
									
									sort_event = null;
									
									sortMessagesSupport();
								}
								
								for ( ChatListener l: listeners ){
									
									l.messagesChanged();
								}
							}
						});
			}
		}
		
		private void
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
			
				// break any loops arbitrarily
		
			Set<ChatMessage>	linked_messages = new HashSet<ChatMessage>( prev_map.keySet());
			
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
			
			Set<ChatMessage>		chain_heads = new HashSet<ChatMessage>();
			
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
				
				List<ChatMessage>	remainder = new ArrayList<ChatMessage>( remainder_set );
				
				Collections.sort(
						remainder,
						new Comparator<ChatMessage>()
						{
							public int 
							compare(
								ChatMessage o1, 
								ChatMessage o2 ) 
							{
								long l = o1.getTimeStamp() - o2.getTimeStamp();
								
								if ( l < 0 ){
									return( -1 );
								}else if ( l > 0 ){
									return( 1 );
								}else{
									return(0);
								}
							}
						});
				
				if ( result == null ){
					
					result = remainder;
					
				}else{
					
					result = merge( result, remainder );
				}
			}
			
			if ( result != null ){
				
				messages = result;
			}
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
				
					if ( m1.getTimeStamp() <= m2.getTimeStamp()){
						
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
		{
			ChatMessage msg = new ChatMessage( message_map );
						
			ChatParticipant	new_participant = null;
				
			boolean	sort_outstanding = false;
			
			byte[]	prev_id 	= msg.getPreviousID();
			
				// insert in timestamp order
			
			synchronized( this ){
				
					// best case is that message belongs at the end
				
				int old_msgs = messages.size();

				messages.add( msg );
		
				if ( messages.size() > MSG_HISTORY_MAX ){
					
					messages.remove(0);
				}
				
				byte[] pk = msg.getPublicKey();
				
				ChatParticipant participant = participants.get( pk );
				
				String		old_nick;
				String		new_nick;
				
				if ( participant == null ){
					
					new_participant = participant = new ChatParticipant( this, pk );
					
					participants.put( pk, participant );
					
					old_nick = null;
					
					participant.addMessage( msg );
					
					new_nick = participant.getName();
					
				}else{
					
					old_nick = participant.getName();
					
					participant.addMessage( msg );
					
					new_nick = participant.getName();
					
					msg.setIgnored( participant.isIgnored());
				}
				
				if ( old_nick != null && !old_nick.equals( new_nick )){
					
					List<ChatParticipant>	hits = nick_clash_map.get( old_nick );
					
					if ( hits != null ){
						
						if ( hits.remove( participant )){
							
							participant.setNickClash( false );
							
							if ( hits.size() < 2 ){
								
								for ( ChatParticipant p: hits ){
									
									p.setNickClash( false );
								}
							}
							
							if ( hits.size() == 0 ){
								
								nick_clash_map.remove( old_nick );
							}
						}
					}
				}
				
				if ( old_nick == null || !old_nick.equals( new_nick )){
					
					List<ChatParticipant>	hits = nick_clash_map.get( new_nick );
					
					if ( hits == null ){
						
						hits = new ArrayList<ChatParticipant>();
						
						nick_clash_map.put( new_nick, hits );
					}
					
					hits.add( participant );
					
					if ( hits.size() >= 2 ){
						
						for ( ChatParticipant p: hits ){
							
							p.setNickClash( true );
						}
					}
				}
				
				msg.setParticipant( participant );
				
				if ( sort_event != null ){
					
					sort_outstanding = true;
					
				}else{
					
					if ( old_msgs == 0 ){	
						
					}else if ( prev_id != null && Arrays.equals( prev_id, messages.get(old_msgs-1).getID())){
											
					}else{
						
						sortMessages();
						
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
						sendMessageSupport( message );
					}
				});
		}
		
		private void 
		sendMessageSupport(
			String		message )
		{
			if ( handler == null || msgsync_pi == null ){
				
				Debug.out( "No handler/plugin" );
				
			}else{
				
				if ( message.equals( "!dump!" )){
					
					synchronized( this ){
						
						for ( ChatMessage msg: messages ){
							
							System.out.println( pkToString( msg.getID()) + ", " + pkToString( msg.getPreviousID()) + " - " + msg.getMessage());
						}
					}
					return;
				}
				
				try{
					ChatMessage		prev_message = null;
					
					synchronized( this ){
						
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
			synchronized( this ){
				
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
			synchronized( this ){
				
				reference_count--;
				
				if ( reference_count > 0 ){
					
					return;
				}
			}
			
			if ( !persistent ){
				
				if ( handler != null ){
									
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
		private ChatInstance		chat;
		private final byte[]		pk;
		private int					message_count;
		
		private String				nickname;
		private boolean				is_ignored;
		private boolean				is_pinned;
		private boolean				nick_clash;
		
		private
		ChatParticipant(
			ChatInstance		_chat,
			byte[]				_pk )
		{
			chat	= _chat;
			pk		= _pk;
			
			nickname = pkToString( pk );
			
			is_pinned = COConfigurationManager.getBooleanParameter( getPinKey(), false );
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
			synchronized( this ){
				
				message_count++;
			}
			
			nickname = message.getNickName();
			
			chat.updated( this );
		}
		
		public int
		getMessageCount()
		{
			return( message_count );
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
		{
			return( null );
		}
	}
	
	public class
	ChatMessage
	{
		private final Map<String,Object>		map;
		
		private ChatParticipant					participant;

		private final byte[]					message_id;
		
		private byte[]							previous_id;
		
		private long							timestamp;
		
		private boolean							is_ignored;
		
		private
		ChatMessage(
			Map<String,Object>		_map )
		{
			map		= _map;
			
			message_id = (byte[])map.get( "id" );
			
			timestamp = SystemTime.getCurrentTime() - getAge()*1000;

			Map<String,Object> payload = getPayload();
			
			previous_id = (byte[])payload.get( "pre" );
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
		
		public String
		getMessage()
		{
			try{
				String	error = (String)map.get( "error" );
				
				if ( error != null ){
					
					return( error );
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
		
		public boolean
		isError()
		{
			return( map.containsKey( "error" ));
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
