/*
 * Created on May 6, 2008
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


package com.aelitis.azureus.core.messenger.config;

import java.util.*;

import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.json.simple.JSONArray;


import com.aelitis.azureus.core.messenger.PlatformMessage;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.core.messenger.PlatformMessengerException;
import com.aelitis.azureus.core.messenger.PlatformMessengerListener;

public class 
PlatformSubscriptionsMessenger 
{
	public static final String LISTENER_ID_TEMPLATE = "subscription";

	public static final String OP_GET_SUBS_BY_SID				= "get-subs-by-sid";
	public static final String OP_GET_POP_BY_SID				= "get-subscription-infos";


	public static subscriptionDetails 
	getSubscriptionBySID(
		byte[]		sid )
	
		throws PlatformMessengerException
	{
		Map parameters = new HashMap();
		
		parameters.put( "sid", sid );
		
		Map reply = syncInvoke(	OP_GET_SUBS_BY_SID, parameters ); 

		subscriptionDetails details = new subscriptionDetails( reply );
		
		return( details );
	}                       	
	
	public static long
	getPopularityBySID(
		byte[]		sid,
		int			version )
	
		throws PlatformMessengerException
	{
		Map parameters = new HashMap();
		
		List	sid_list = new JSONArray();
		
		sid_list.add( Base32.encode( sid ));
			
		List	version_list = new JSONArray();
		
		version_list.add( new Long( version ));
		
		parameters.put( "short_ids", sid_list );
		parameters.put( "version_numbers", version_list );
		
		Map reply = syncInvoke(	OP_GET_POP_BY_SID, parameters ); 
		
		for (int i=0;i<sid_list.size();i++){
			
			Map	map = (Map)reply.get((String)sid_list.get(i));
			
			if ( map != null ){
				
				subscriptionInfo info = new subscriptionInfo( map );
				
				return( info.getPopularity());
			}
		}
		
		throw( new PlatformMessengerException( "Unknown sid '" + ByteFormatter.encodeString(sid) + "'" ));
	}
	
	protected static Map
	syncInvoke(
		String 						operationID, 
		Map 						parameters )
	
		throws PlatformMessengerException
	{
		PlatformMessage message = 
			new PlatformMessage( 
					"AZMSG", 
					LISTENER_ID_TEMPLATE,
					operationID, 
					parameters, 
					0 );

		final AESemaphore sem = new AESemaphore( "PlatformMetaSearch:syncInvoke" );
		
		final Object[] result = { null };
		
		PlatformMessenger.queueMessage( 
			message, 
			new PlatformMessengerListener()
			{
				public void 
				messageSent(
					PlatformMessage 	message ) 
				{
				}
	
				public void 
				replyReceived(
					PlatformMessage 	message, 
					String 				replyType,
					Map 				reply )
				{
					try{
						if ( replyType.equals( PlatformMessenger.REPLY_EXCEPTION )){
							
							String		text 	= (String)reply.get( "text" );
							
							Throwable	e 		= (Throwable)reply.get( "Throwable" );
							
							if ( text == null && e == null ){
								
								result[0] = new PlatformMessengerException( "Unknown error" );
								
							}else if ( text == null ){
								
								result[0] = new PlatformMessengerException( "Failed to send RPC", e );
								
							}else if ( e == null ){
								
								result[0] = new PlatformMessengerException( text );
								
							}else{
								
								result[0] = new PlatformMessengerException( text, e );
							}
						}else{
							
							result[0] = reply;
						}
					}finally{
						
						sem.release();
					}
				}
			});
		
		sem.reserve();
		
		if ( result[0] instanceof PlatformMessengerException ){
			
			throw((PlatformMessengerException)result[0]);
		}
		
		return((Map)result[0]);
	}
	
	public static class
	subscriptionInfo
	{
		private Map		info;
		
		protected
		subscriptionInfo(
			Map		_info )
		{
			info	= _info;
		}
		
		public long
		getPopularity()
		{
			return(((Long)info.get( "popularity" )).intValue());
		}
	}
	
	public static class
	subscriptionDetails
	{
		private Map		details;
		
		protected
		subscriptionDetails(
			Map		_details )
		{	
			details = _details;
		}
		
		public String
		getName()
		{
			return( getString( "name" ));
		}
		
		protected String
		getString(
			String	key )
		{
			byte[]	bytes = (byte[])details.get( key );
			
			try{
				return( new String( bytes, "UTF-8" ));
				
			}catch( Throwable e ){
				
				return( new String( bytes ));
			}
		}
	}
}
