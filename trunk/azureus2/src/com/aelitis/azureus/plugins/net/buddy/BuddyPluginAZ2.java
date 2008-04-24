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
import java.util.Map;

import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.ui.UIManagerEvent;

public class 
BuddyPluginAZ2 
{
	private static final int SEND_TIMEOUT = 2*60*1000;
	
	private BuddyPlugin		plugin;
	
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
		
		reply.put( "ok", new Long( BuddyPlugin.RT_AZ2_REPLY_MESSAGE  ));
		
		if ( type == BuddyPlugin.RT_AZ2_REQUEST_MESSAGE ){
			
			try{
				String	msg = new String( (byte[])request.get( "msg" ), "UTF8" );
			
				from_buddy.setLastMessageReceived( msg );
				
			}catch( Throwable e ){
				
			}
			
			return( reply );

		}else if (  type == BuddyPlugin.RT_AZ2_REQUEST_SEND_TORRENT ){
			
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
				
				return( reply );
				
			}catch( Throwable e ){
				
				throw( new BuddyPluginException( "Torrent receive failed " + type ));
			}
		}else{
			
			throw( new BuddyPluginException( "Unrecognised request type " + type ));
		}
	}
	
	public void
	queueAZ2Message(
		BuddyPluginBuddy	buddy,
		String				msg )
	{
		try{
			Map	request = new HashMap();
			
			request.put( "type", new Long( BuddyPlugin.RT_AZ2_REQUEST_MESSAGE ));
			request.put( "msg", msg.getBytes());
	
			buddy.getMessageHandler().queueMessage( 
					BuddyPlugin.SUBSYSTEM_AZ2,
					request,
					SEND_TIMEOUT );
			
		}catch( Throwable e ){
			
			logMessageAndPopup( "Send message failed", e );
		}
	}
	
	public void
	sendAZ2Message(
		BuddyPluginBuddy	buddy,
		String				msg )
	{
		try{
			Map	request = new HashMap();
			
			request.put( "type", new Long( BuddyPlugin.RT_AZ2_REQUEST_MESSAGE ));
			request.put( "msg", msg.getBytes());
			
			buddy.sendMessage(
				BuddyPlugin.SUBSYSTEM_AZ2,
				request, 
				SEND_TIMEOUT, 
				new BuddyPluginBuddyReplyListener()
				{
					public void
					replyReceived(
						BuddyPluginBuddy		from_buddy,
						Map						reply )
					{
						logMessageAndPopup( "Send message ok" );
					}
					
					public void
					sendFailed(
						BuddyPluginBuddy		to_buddy,
						BuddyPluginException	cause )
					{
						logMessageAndPopup( "Send message failed: " + Debug.getNestedExceptionMessage(cause));
					}
				});
				
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
			
			request.put( "type", new Long( BuddyPlugin.RT_AZ2_REQUEST_SEND_TORRENT ));
			request.put( "torrent", torrent.writeToBEncodedData());
			
			buddy.sendMessage(
				BuddyPlugin.SUBSYSTEM_AZ2,
				request, 
				SEND_TIMEOUT, 
				new BuddyPluginBuddyReplyListener()
				{
					public void
					replyReceived(
						BuddyPluginBuddy		from_buddy,
						Map						reply )
					{
						logMessageAndPopup( "Send torrent ok" );
					}
					
					public void
					sendFailed(
						BuddyPluginBuddy		to_buddy,
						BuddyPluginException	cause )
					{
						logMessageAndPopup( "Send torrent failed: " + Debug.getNestedExceptionMessage(cause));
					}
				});
		}catch( Throwable e ){
			
			logMessageAndPopup( "Send torrent failed", e );
		}
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
