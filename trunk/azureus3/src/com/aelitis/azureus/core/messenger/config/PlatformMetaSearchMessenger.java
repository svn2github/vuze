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


import com.aelitis.azureus.core.messenger.PlatformMessage;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.core.messenger.PlatformMessengerException;
import com.aelitis.azureus.core.messenger.PlatformMessengerListener;

public class 
PlatformMetaSearchMessenger 
{
	private static final int MAX_TEMPLATE_LIST		= 64;
	
	public static final String LISTENER_ID_TEMPLATE = "searchtemplate";

	public static final String OP_GET_TEMPLATE					= "get-template";
	public static final String OP_LIST_POPULAR_TEMPLATES 		= "list-popular";
	public static final String OP_LIST_FEATURED_TEMPLATES 		= "list-featured";
	public static final String OP_TEMPLATE_EXISTS				= "template-exists";
	public static final String OP_TEMPLATE_SELECTED				= "template-selected";
	public static final String OP_TEMPLATE_UNSELECTED			= "template-unselected";


	public static void 
	getTemplate(
		long	template_id )
	
		throws PlatformMessengerException
	{
		Map reply = syncInvoke(	OP_GET_TEMPLATE, getParameter( template_id ) ); 

	}
	
	public static void 
	listPopularTemplates()
	
		throws PlatformMessengerException
	{
		Map parameters = new HashMap();
		
		parameters.put( "page-num", new Long( 1 ));
		parameters.put( "items-per-page", new Long( MAX_TEMPLATE_LIST ));

		Map reply = syncInvoke(	OP_LIST_POPULAR_TEMPLATES, parameters ); 

		List	templates = (List)reply.get( "templates" );
		
		for (int i=0;i<templates.size();i++){
			
			Map m = (Map)templates.get(i);
			
			m.get( "id" );
		}
	}
	
	public static void 
	listFeaturedTemplates()
	
		throws PlatformMessengerException
	{
		Map parameters = new HashMap();
		
		parameters.put( "page-num", new Long( 1 ));
		parameters.put( "items-per-page", new Long( MAX_TEMPLATE_LIST ));

		Map reply = syncInvoke(	OP_LIST_FEATURED_TEMPLATES, parameters ); 

		List	templates = (List)reply.get( "templates" );
		
		for (int i=0;i<templates.size();i++){
			
			Map m = (Map)templates.get(i);
			
			m.get( "id" );
		}
	}
	
	public static void 
	templateExists(
		long	template_id )
	
		throws PlatformMessengerException
	{
		Map reply = syncInvoke(	OP_LIST_FEATURED_TEMPLATES, getParameter( template_id ) ); 
	}
	
	public static void 
	templateSelected(
		long	template_id )
	
		throws PlatformMessengerException
	{
		Map reply = syncInvoke(	OP_TEMPLATE_SELECTED, getParameter( template_id ) ); 
	}
	
	public static void 
	templateUnselected(
		long	template_id )
	
		throws PlatformMessengerException
	{
		Map reply = syncInvoke(	OP_TEMPLATE_UNSELECTED, getParameter( template_id ) ); 
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

	protected static Map
	getParameter(
		long		template_id )
	{
		Map parameters = new HashMap();
		
		parameters.put( "templateId", new Long( template_id ));

		return( parameters );
	}
}
