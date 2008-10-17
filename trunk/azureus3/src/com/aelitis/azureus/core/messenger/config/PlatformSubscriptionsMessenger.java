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

import java.security.Signature;
import java.util.*;

import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.json.simple.JSONArray;


import com.aelitis.azureus.core.messenger.PlatformMessage;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.core.messenger.PlatformMessengerException;
import com.aelitis.azureus.core.messenger.PlatformMessengerListener;
import com.aelitis.azureus.core.security.CryptoECCUtils;

public class 
PlatformSubscriptionsMessenger 
{
	public static final String LISTENER_ID_TEMPLATE = "subscription";

	public static final String OP_CREATE_SUBS					= "create-subscription";
	public static final String OP_UPDATE_SUBS					= "update-subscription";
	public static final String OP_GET_SUBS_BY_SID				= "get-subscriptions";
	public static final String OP_GET_POP_BY_SID				= "get-subscription-infos";
	public static final String OP_SET_SELECTED					= "set-selected";

	public static void
	updateSubscription(
		boolean		create,
		String		name,
		byte[]		public_key,
		byte[]		private_key,
		byte[]		sid,
		int			version,
		String		content )
	
		throws PlatformMessengerException
	{
		Map parameters = new HashMap();
		
		String	sid_str = Base32.encode( sid );
		String	pk_str	= Base32.encode(public_key) ;
		
		parameters.put( "name", name );
		parameters.put( "subscription_id", sid_str );
		parameters.put( "version_number", new Long( version ));
		parameters.put( "content", content );				
		
		if ( create ){
			
			parameters.put( "public_key", pk_str );
		}
		
		try{
			Signature sig = CryptoECCUtils.getSignature( CryptoECCUtils.rawdataToPrivkey( private_key ));

			sig.update( ( name + pk_str + sid_str + version + content ).getBytes( "UTF-8" ));
			
			byte[]	sig_bytes = sig.sign();
			
			/*
			Signature verify = CryptoECCUtils.getSignature( CryptoECCUtils.rawdataToPubkey( public_key ));

			verify.update( ( name + pk_str + sid_str + version + content ).getBytes( "UTF-8" ));
			
			boolean ok = verify.verify( sig_bytes );
			*/
			
			parameters.put( "signature", Base32.encode( sig_bytes ));

			syncInvoke(	create?OP_CREATE_SUBS:OP_UPDATE_SUBS, parameters ); 
			
		}catch( Throwable e ){
			
			throw( new PlatformMessengerException( "Failed to create/update subscription", e ));
		}
	}
	
	public static String 
	getSubscriptionBySID(
		byte[]		sid )
	
		throws PlatformMessengerException
	{
		Map parameters = new HashMap();
		
		List	sid_list = new JSONArray();

		sid_list.add( Base32.encode( sid ));
		
		parameters.put( "subscription_ids", sid_list);
		
		Map reply = syncInvoke(	OP_GET_SUBS_BY_SID, parameters ); 

		for (int i=0;i<sid_list.size();i++){
			
			Map	map = (Map)reply.get((String)sid_list.get(i));
			
			if ( map != null ){
				
				subscriptionDetails details = new subscriptionDetails( map );
				
				return( details.getContent());
			}
		}
		
		throw( new PlatformMessengerException( "Unknown sid '" + ByteFormatter.encodeString(sid) + "'" ));
	}                       	
	
	public static long
	getPopularityBySID(
		byte[]		sid )
	
		throws PlatformMessengerException
	{
		Map parameters = new HashMap();
		
		List	sid_list = new JSONArray();
		
		sid_list.add( Base32.encode( sid ));
					
		parameters.put( "subscription_ids", sid_list );
		
		Map reply = syncInvoke(	OP_GET_POP_BY_SID, parameters ); 
		
		for (int i=0;i<sid_list.size();i++){
			
			Map	map = (Map)reply.get((String)sid_list.get(i));
			
			if ( map != null ){
				
				subscriptionInfo info = new subscriptionInfo( map );
				
				return( info.getPopularity());
			}
		}
		
		return( -1 );
	}
	
	public static List[] 
	setSelected(
		List	sids )
	
		throws PlatformMessengerException
	{
		Map parameters = new HashMap();
		
		List	sid_list 	= new JSONArray();		
		for (int i=0;i<sids.size();i++){
		
			sid_list.add( Base32.encode( (byte[])sids.get(i) ));
		}
		
		parameters.put( "subscription_ids", sid_list);
		
		Map reply = syncInvoke(	OP_SET_SELECTED, parameters ); 
		
		List	versions = (List)reply.get( "version_numbers" );
		
		if ( versions == null ){
			
			// test
			
			versions = new ArrayList();
			
			for (int i=0;i<sids.size();i++){
				
				versions.add( new Long(1));
			}
		}
		
		List	popularities = (List)reply.get( "popularities" );
		
		if ( popularities == null ){
			
				// migrate
			
			popularities = new ArrayList();
			
			for (int i=0;i<sids.size();i++){
				
				versions.add( new Long(-1));
			}
		}
	
		return( new List[]{ versions,popularities } );
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
							
							String		e_message 	= (String)reply.get( "message" );

							if ( e_message != null ){
								
								result[0] = new PlatformMessengerException( e_message );

							}else{
								
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
		
		public String
		getContent()
		{
			return( getString( "content" ));
		}
		
		protected String
		getString(
			String	key )
		{
			Object obj = details.get( key );
			
			if ( obj instanceof String ){
				
				return((String)obj);
				
			}else if ( obj instanceof byte[] ){
					
				byte[]	bytes = (byte[])obj;
				
				try{
					return( new String( bytes, "UTF-8" ));
					
				}catch( Throwable e ){
					
					return( new String( bytes ));
				}
			}else{
				
				return( null );
			}
		}
	}
}
