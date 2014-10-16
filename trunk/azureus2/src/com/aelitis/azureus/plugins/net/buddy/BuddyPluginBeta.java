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

import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TimerEventPeriodic;
import org.gudy.azureus2.plugins.PluginEvent;
import org.gudy.azureus2.plugins.PluginEventListener;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;

import com.aelitis.azureus.core.util.CopyOnWriteList;

public class
BuddyPluginBeta 
{
	public static final String	BETA_CHAT_KEY = 	"test:beta:chat";
	
	private PluginInterface		plugin_interface;
	private BooleanParameter	enabled;
	
	private AsyncDispatcher		dispatcher = new AsyncDispatcher( "BuddyPluginBeta" );
	
	private Map<String,ChatInstance>		chat_instances = new HashMap<String, BuddyPluginBeta.ChatInstance>();

	private PluginInterface azmsgsync_pi;

	private TimerEventPeriodic		timer;
	
	
	protected
	BuddyPluginBeta(
		PluginInterface		_pi,
		BooleanParameter	_enabled )
	{
		plugin_interface 	= _pi;
		enabled				= _enabled;
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
		
	public ChatInstance
	getChat(
		String			network,
		String			key )
		
		throws Exception
	{
		return( getChat( network, key, false ));
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
		private final String		network;
		private final String		key;
		
		private volatile PluginInterface		msgsync_pi;
		private volatile Object					handler;
		
		private List<ChatMessage>		messages = new ArrayList<ChatMessage>();
		
		private CopyOnWriteList<ChatListener>		listeners = new CopyOnWriteList<ChatListener>();
		
		private boolean	persistent = false;
		
		private Map<String,Object> 	status;
		
		private
		ChatInstance(
			String				_network,
			String				_key )
		{
			network 	= _network;
			key			= _key;
		}
		
		public String
		getName()
		{
			return( network + ": " + key );
		}
		
		public void
		setPersistent()
		{
			persistent	= true;
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
			
			if ( current_handler == null || current_pi == null ){
				
				return;
			}
			
			try{
				Map<String,Object>		options = new HashMap<String, Object>();
				
				options.put( "handler", current_handler );

				status = (Map<String,Object>)current_pi.getIPC().invoke( "getStatus", new Object[]{ options } );

				System.out.println( "status: " + status );
				
				for ( ChatListener l: listeners ){
				
					l.updated();
				}
			}catch( Throwable e ){
				
				Debug.out( e );
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
				
				return( "" );
				
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
										
				
				if ( status == 0 ){
					
					return( "Initialising: dht=" + (dht_count<0?"...":String.valueOf(dht_count)));
					
				}else if ( status == 1 ){
					
					return( "dht=" + dht_count + 
							", nodes=" + nodes_local+"/"+nodes_live+"/"+nodes_dying +
							", req=" + DisplayFormatters.formatDecimal(req_in_rate,1) + "/" +  DisplayFormatters.formatDecimal(req_out_rate,1) );
					
				}else{
					
					return( "Destroyed" );
				}
			}
		}
		
		public void
		messageReceived(
			Map			message )
		{
			ChatMessage msg = new ChatMessage( message );
			
			synchronized( this ){
				
				messages.add( msg );
			}
			
			for ( ChatListener l: listeners ){
				
				l.messageReceived( msg );
			}
		}
		
		public void 
		sendMessage(
			String		message )
		{
			if ( handler == null || msgsync_pi == null ){
				
				Debug.out( "No handler/plugin" );
				
			}else{
				
				try{
					Map<String,Object>		options = new HashMap<String, Object>();
					
					options.put( "handler", handler );
					
					Map<String,Object>	payload = new HashMap<String, Object>();
					
					payload.put( "msg", message );
					
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
			return( new ChatParticipant[0] );
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
		public String
		getName() 
		{
			return( "" );
		}
	}
	
	public class
	ChatMessage
	{
		private Map<String,Object>			map;
		
		private
		ChatMessage(
			Map		_map )
		{
			map		= _map;
		}
		
		public String
		getMessage()
		{
			try{
					// was just a string for a while...
				
				try{
					Map<String,Object> payload = BDecoder.decode((byte[])map.get( "content" ));
					
					return( new String((byte[])payload.get( "msg" ), "UTF-8" ));
					
				}catch( Throwable e ){
				}
				
				return( new String((byte[])map.get( "content" ), "UTF-8" ));
				
			}catch( Throwable e ){
				
				Debug.out( e );
					
				return( "" );
			}
		}
		
		public byte[]
		getPublicKey()
		{
			return((byte[])map.get( "pk" ));
		}
		
		public String
		getNickName()
		{
			byte[] pk = getPublicKey();
			
			byte[] temp = new byte[3];
			
			System.arraycopy( pk, 8, temp, 0, 3 );
			
			return( ByteFormatter.encodeString( temp ));
		}
	}
	
	public interface
	ChatListener
	{
		public void
		messageReceived(
			ChatMessage				message );
		
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
