/*
 * Created on Apr 23, 2008
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
import java.util.*;

import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.util.CopyOnWriteList;

public class 
BuddyPluginBuddyMessageHandler 
{
	private BuddyPluginBuddy		buddy;
	private File					store;
	
	private Map	config_map;
	private int	message_count;
	private int	next_message_id;
	
	private CopyOnWriteList			listeners = new CopyOnWriteList();
	
	private BuddyPluginBuddyMessage	active_message;
	
	private long					last_failure;
	
	protected
	BuddyPluginBuddyMessageHandler(
		BuddyPluginBuddy		_buddy,
		File					_store )
	{
		buddy	= _buddy;
		store	= _store;
		
		loadConfig();
		
		if ( message_count > 0 ){
			
			buddy.persistentDispatchPending();
		}
	}
	
	public BuddyPluginBuddy
	getBuddy()
	{
		return( buddy );
	}
	
	public BuddyPluginBuddyMessage
	queueMessage(
		int		subsystem,
		Map		content,
		int		timeout_millis )
	
		throws BuddyPluginException
	{
		BuddyPluginBuddyMessage	message;
		
		boolean	dispatch_pending;
		
		synchronized( this ){
			
			int	id = next_message_id++;
			
			message = 
				new BuddyPluginBuddyMessage( 
						this, id, subsystem, content, timeout_millis, SystemTime.getCurrentTime());
		
			storeMessage( message );
			
			dispatch_pending = message_count == 1;
		}
		
		Iterator it = listeners.iterator();
		
		while( it.hasNext()){
			
			try{
				((BuddyPluginBuddyMessageListener)it.next()).messageQueued( message );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
		
		if ( dispatch_pending ){
			
			buddy.persistentDispatchPending();
		}
		
		return( message );
	}	
	
	protected void
	checkPersistentDispatch()
	{
		boolean	request_dispatch;
		
		synchronized( this ){

			if ( active_message != null || message_count == 0 || last_failure == 0 ){
				
				return;
			}
			
			long	now = SystemTime.getCurrentTime();

			if ( now < last_failure ){
				
				last_failure = now;
			}
			
			request_dispatch = now - last_failure >= BuddyPlugin.PERSISTENT_MSG_RETRY_PERIOD;
		}
		
		if ( request_dispatch ){
			
			buddy.persistentDispatch();
		}
	}
	
	protected void
	persistentDispatch()
	{
		synchronized( this ){

			if ( active_message != null || message_count == 0 ){
				
				return;
			}
			
			List	messages = (List)config_map.get( "messages" );

			Map		map = (Map)messages.get(0);
			
			try{
				active_message = restoreMessage( map );
				
			}catch( Throwable e ){
				
					// should never happen...
				
				Debug.out( "Failed to restore message, deleting it", e );
				
				messages.remove(0);
				
				deleteContent( 0 );
				
				try{
					saveConfig();
					
				}catch( Throwable f ){
					
					buddy.log( "Config save failed during delete of bad message", f );
				}
			}
		}
		
		try{
			Map	content = active_message.getContent();
		
			buddy.sendMessage(
					active_message.getSubsystem(),
					content,
					active_message.getTimeout(),
					new BuddyPluginBuddyReplyListener()
					{
						public void
						replyReceived(
							BuddyPluginBuddy		from_buddy,
							Map						reply )
						{
							BuddyPluginBuddyMessage message = active_message;
							
							active_message.delete();

							boolean messages_queued;
							
							synchronized( this ){

								active_message 	= null;
								
								messages_queued = message_count > 0;
								
								last_failure	= 0;
							}
								
							Iterator it = listeners.iterator();
							
							while( it.hasNext()){
								
								try{
									((BuddyPluginBuddyMessageListener)it.next()).deliverySucceeded( message, reply );
									
								}catch( Throwable e ){
									
									Debug.printStackTrace(e);
								}
							}
							
							if ( messages_queued ){
							
								buddy.persistentDispatchPending();
							}
						}
						
						public void
						sendFailed(
							BuddyPluginBuddy		to_buddy,
							BuddyPluginException	cause )
						{
							BuddyPluginBuddyMessage message = active_message;
							
							synchronized( this ){

								active_message 	= null;
								
								last_failure	= SystemTime.getCurrentTime();
							}
							
							Iterator it = listeners.iterator();
							
							while( it.hasNext()){
								
								try{
									((BuddyPluginBuddyMessageListener)it.next()).deliveryFailed( message, cause );
									
								}catch( Throwable e ){
									
									Debug.printStackTrace(e);
								}
							}
						}
					});
					
		}catch( Throwable cause ){
			
			BuddyPluginBuddyMessage message = active_message;

			synchronized( this ){

				active_message 	= null;
				
				last_failure	= SystemTime.getCurrentTime();
			}
			
			Iterator it = listeners.iterator();
			
			while( it.hasNext()){
				
				BuddyPluginException b_cause;
				
				if ( cause instanceof BuddyPluginException ){
					
					b_cause = (BuddyPluginException)cause;
					
				}else{
					
					b_cause = new BuddyPluginException( "Failed to send message", cause );
				}
				
				try{
					((BuddyPluginBuddyMessageListener)it.next()).deliveryFailed( message, b_cause );
					
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
		}
	}
	
	public int
	getMessageCount()
	{
		return( message_count );
	}
	
	protected void
	delete(
		BuddyPluginBuddyMessage		message )
	{
		synchronized( this ){
			
			List	messages = (List)config_map.get( "messages" );

			if ( messages != null ){

				boolean	found = false;
				
				for ( int i=0;i<messages.size();i++){
					
					Map	msg = (Map)messages.get(i);
					
					if ( message.getID() == ((Long)msg.get( "id")).intValue()){
						
						messages.remove(i);
						
						found	= true;
						
						break;
					}
				}
			
				if ( found ){
				
					deleteContent( message );
					
					try{
						saveConfig();
						
					}catch( Throwable e ){
						
						buddy.log( "Config save failed during message delete", e );
					}
				}
			}
		}
	}
	
	protected void
	destroy()
	{
		synchronized( this ){
			
			config_map.clear();
			
			try{
				saveConfig();
				
			}catch( Throwable e ){
				
				buddy.log( "Config save failed during destroy", e );
			}
		}
	}
	
	protected void
	writeContent(
		BuddyPluginBuddyMessage		message,
		Map							content )
	
		throws BuddyPluginException
	{
		if ( !store.exists()){
			
			if ( !store.mkdirs()){
				
				throw( new BuddyPluginException( "Failed to create " + store ));
			}
		}
		
		File target = new File( store, message.getID() + ".dat" );
		
		try{
		
			BuddyPlugin.cryptoResult result = buddy.encrypt( BEncoder.encode( content ));
			
			Map	store_map = new HashMap();
			
			store_map.put( "pk", buddy.getPlugin().getPublicKey());
			store_map.put( "data", result.getPayload());
			
			if ( !buddy.writeConfigFile( target, store_map )){
				
				throw( new BuddyPluginException( "failed to write " + target ));
			}
			
		}catch( BuddyPluginException e ){
			
			throw( e );
			
		}catch( Throwable e ){
			
			throw( new BuddyPluginException( "Failed to write message", e ));
		}
	}
	
	protected Map
	readContent(
		BuddyPluginBuddyMessage		message )
	
		throws BuddyPluginException
	{		
		File target = new File( store, message.getID() + ".dat" );

		if ( !target.exists()){

			throw( new BuddyPluginException( "Failed to read persisted message - " + target + " doesn't exist" ));
		}

		Map	map = buddy.readConfigFile( target );
		
		if ( map.size() == 0 ){
			
			throw( new BuddyPluginException( "Failed to read persisted message file " + target ));
		}
		
		try{
			String	pk = new String((byte[])map.get("pk"));
			
			if ( !pk.equals( buddy.getPlugin().getPublicKey())){
				
				throw( new BuddyPluginException( "Can't decrypt message as key changed" ));
			}
			
			byte[]	data = (byte[])map.get( "data" );
			
			return( BDecoder.decode( buddy.decrypt( data ).getPayload()));
			
		}catch( BuddyPluginException e ){
			
			throw( e );
			
		}catch( Throwable e ){
			
			throw( new BuddyPluginException( "Failed to read message", e ));
		}
	}
	
	protected void
	deleteContent(
		BuddyPluginBuddyMessage		message )
	{
		deleteContent( message.getID());
	}
	
	protected void
	deleteContent(
		int			id  )
	{	
		File target = new File( store, id + ".dat" );
		
		if ( target.exists()){
			
			if ( !target.delete()){
				
				Debug.out( "Failed to delete " + target );
			}
		}
	}
	
	protected void
	storeMessage(
		BuddyPluginBuddyMessage		msg )
	
		throws BuddyPluginException
	{
		List	messages = (List)config_map.get( "messages" );

		if ( messages == null ){
			
			messages = new ArrayList();
			
			config_map.put( "messages", messages );
		}
		
		Map map = new HashMap();
		
		map.put( "id", new Long( msg.getID()));
		map.put( "ss", new Long( msg.getSubsystem()));
		map.put( "to", new Long( msg.getTimeout()));
		map.put( "cr", new Long( msg.getCreateTime()));
		
		messages.add( map );
				
		saveConfig();
	}
	
	protected BuddyPluginBuddyMessage
	restoreMessage(
		Map			map )
	
		throws BuddyPluginException
	{
		int	id = ((Long)map.get( "id" )).intValue();
		int	ss = ((Long)map.get( "ss" )).intValue();
		int	to = ((Long)map.get( "to" )).intValue();
		
		long	cr = ((Long)map.get( "cr" )).longValue();
		
		return( new BuddyPluginBuddyMessage( this, id, ss, null, to, cr ));
	}
	
	protected void
	loadConfig()
	{
		File	config_file = new File( store, "messages.dat" );
		
		if ( config_file.exists()){
			
			config_map = buddy.readConfigFile( config_file );
			
		}else{
			
			config_map = new HashMap();
		}
		
		List	messages = (List)config_map.get( "messages" );
		
		if ( messages != null ){
			
			message_count = messages.size();
			
			if ( message_count > 0 ){
				
				Map	last_msg = (Map)messages.get( message_count - 1 );
				
				next_message_id = ((Long)last_msg.get( "id")).intValue() + 1;
			}
		}
	}
	
	protected void
	saveConfig()
	
		throws BuddyPluginException
	{
		File	config_file = new File( store, "messages.dat" );
		
		List	messages = (List)config_map.get( "messages" );

		if ( messages == null || messages.size() == 0 ){
			
			if ( store.exists()){
			
				File[]	 files = store.listFiles();
			
				for (int i=0;i<files.length;i++ ){
					
					files[i].delete();
				}
				
				store.delete();
			}
			
			message_count = 0;
			
			next_message_id	= 0;
			
		}else{
			
			if ( !store.exists()){
				
				if ( !store.mkdirs()){
					
					throw( new BuddyPluginException( "Failed to create " + store ));
				}
			}
			
			if ( !buddy.writeConfigFile( config_file, config_map )){
				
				throw( new BuddyPluginException( "Failed to write" + config_file ));
			}
			
			message_count = messages.size();
		}
	}
	
	public void
	addListener(
		BuddyPluginBuddyMessageListener		listener )
	{
		listeners.add( listener );
	}
	
	public void
	removeListener(
		BuddyPluginBuddyMessageListener		listener )
	{
		listeners.remove( listener );
	}
}
