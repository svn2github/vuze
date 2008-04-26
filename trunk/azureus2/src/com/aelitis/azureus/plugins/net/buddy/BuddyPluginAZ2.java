/*
 * Created on Apr 10, 2008
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.ui.UIManagerEvent;

import com.aelitis.azureus.core.util.CopyOnWriteList;

public class 
BuddyPluginAZ2 
{
	public static final int RT_AZ2_REQUEST_MESSAGE		= 1;
	public static final int RT_AZ2_REPLY_MESSAGE		= 2;
	
	public static final int RT_AZ2_REQUEST_SEND_TORRENT	= 3;
	public static final int RT_AZ2_REPLY_SEND_TORRENT	= 4;

	public static final int RT_AZ2_REQUEST_CHAT			= 5;
	public static final int RT_AZ2_REPLY_CHAT			= 6;

	private static final int SEND_TIMEOUT = 2*60*1000;
	
	private BuddyPlugin		plugin;
	
	private CopyOnWriteList	listeners = new CopyOnWriteList();
	
	protected 
	BuddyPluginAZ2(
		BuddyPlugin		_plugin )
	{
		plugin	= _plugin;
		
		plugin.addRequestListener(
				new BuddyPluginBuddyRequestListener()
				{
					public Map
					requestReceived(
						BuddyPluginBuddy	from_buddy,
						int					subsystem,
						Map					request )
					
						throws BuddyPluginException
					{
						if ( subsystem == BuddyPlugin.SUBSYSTEM_AZ2 ){
							
							if ( !from_buddy.isAuthorised()){
							
								throw( new BuddyPluginException( "Unauthorised" ));
							}
						
							return( processAZ2Request( from_buddy, request ));
						}

						return( null );
					}
					
					public void
					pendingMessages(
						BuddyPluginBuddy[]	from_buddies )
					{
					}
				});
	}
	
	protected Map
	processAZ2Request(
		final BuddyPluginBuddy	from_buddy,
		Map						request )		
		
		throws BuddyPluginException
	{
		logMessage( "AZ2 request received: " + from_buddy.getString() + " -> " + request );
			
		int	type = ((Long)request.get( "type" )).intValue();
		
		Map	reply = new HashMap();
				
		if ( type == RT_AZ2_REQUEST_MESSAGE ){
			
			try{
				String	msg = new String( (byte[])request.get( "msg" ), "UTF8" );
			
				from_buddy.setLastMessageReceived( msg );
				
			}catch( Throwable e ){
				
			}
			
			reply.put( "type", new Long( RT_AZ2_REPLY_MESSAGE ));

			return( reply );

		}else if (  type == RT_AZ2_REQUEST_SEND_TORRENT ){
			
			try{
				final Torrent	torrent = plugin.getPluginInterface().getTorrentManager().createFromBEncodedData((byte[])request.get( "torrent" ));
			
				new AEThread2( "torrentAdder", true )
				{
					public void
					run()
					{
						PluginInterface pi = plugin.getPluginInterface();
						
						String msg = pi.getUtilities().getLocaleUtilities().getLocalisedMessageText(
								"azbuddy.addtorrent.msg", 
								new String[]{ from_buddy.getName(), torrent.getName() });
						
						long res = pi.getUIManager().showMessageBox(
										"azbuddy.addtorrent.title",
										"!" + msg + "!",
										UIManagerEvent.MT_YES | UIManagerEvent.MT_NO );
						
						if ( res == UIManagerEvent.MT_YES ){
						
							pi.getUIManager().openTorrent( torrent );
						}
					}
				}.start();
				
				reply.put( "type", new Long( RT_AZ2_REPLY_SEND_TORRENT ));

				return( reply );
				
			}catch( Throwable e ){
				
				throw( new BuddyPluginException( "Torrent receive failed " + type ));
			}
		}else if (  type == RT_AZ2_REQUEST_CHAT ){

			Iterator	it = listeners.iterator();
			
			Map msg = (Map)request.get( "msg" );
			
			while( it.hasNext()){
			
				try{
					((BuddyPluginAZ2Listener)it.next()).messageReceived( from_buddy, type, msg );
					
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
			
			reply.put( "type", new Long( RT_AZ2_REPLY_CHAT ));
			
			return( reply );
		}else{
			
			throw( new BuddyPluginException( "Unrecognised request type " + type ));
		}
	}
		
	public void
	sendAZ2Message(
		BuddyPluginBuddy	buddy,
		String				msg )
	{
		try{
			Map	request = new HashMap();
			
			request.put( "type", new Long( RT_AZ2_REQUEST_MESSAGE ));
			request.put( "msg", msg.getBytes());
			
			sendMessage( buddy, request );
				
		}catch( Throwable e ){
			
			logMessageAndPopup( "Send message failed", e );
		}
	}
	
	public void
	sendAZ2Chat(
		BuddyPluginBuddy	buddy,
		Map					msg )
	{
		try{
			Map	request = new HashMap();
			
			request.put( "type", new Long( RT_AZ2_REQUEST_CHAT ));
			request.put( "msg", msg );
			
			sendMessage( buddy, request );
				
		}catch( Throwable e ){
			
			logMessageAndPopup( "Send message failed", e );
		}
	}
	
	public void
	sendAZ2Torrent(
		Torrent				torrent,
		BuddyPluginBuddy	buddy )
	{
		try{
			
			Map	request = new HashMap();
			
			request.put( "type", new Long( RT_AZ2_REQUEST_SEND_TORRENT ));
			request.put( "torrent", torrent.writeToBEncodedData());
			
			sendMessage( buddy, request );
			
		}catch( Throwable e ){
			
			logMessageAndPopup( "Send torrent failed", e );
		}
	}
	
	protected void
	sendMessage(
		BuddyPluginBuddy	buddy,
		Map					request )
	
		throws BuddyPluginException
	{
		buddy.getMessageHandler().queueMessage( 
				BuddyPlugin.SUBSYSTEM_AZ2,
				request,
				SEND_TIMEOUT );		
	}
	
	public void
	addListener(
		BuddyPluginAZ2Listener		listener )
	{
		listeners.add( listener );
	}
	
	public void
	removeListener(
		BuddyPluginAZ2Listener		listener )
	{
		listeners.add( listener );
	}
	
	
	protected void
	logMessageAndPopup(
		String		str,
		Throwable	e )
	{
		logMessageAndPopup( str + ": " + Debug.getNestedExceptionMessage(e));
	}
	
	protected void
	logMessageAndPopup(
		String		str )
	{
		logMessage( str );
		
		plugin.getPluginInterface().getUIManager().showMessageBox(
			"azbuddy.msglog.title", "!" + str + "!", UIManagerEvent.MT_OK );
	}
	
	protected void
	logMessage(
		String		str )
	{
		plugin.logMessage( str );
	}
	
	protected void
	logMessage(
		String		str,
		Throwable 	e )
	{
		plugin.logMessage( str + ": " + Debug.getNestedExceptionMessage(e));
	}
}
