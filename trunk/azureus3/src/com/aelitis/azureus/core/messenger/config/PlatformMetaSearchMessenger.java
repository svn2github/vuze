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

import java.util.HashMap;
import java.util.Map;


import com.aelitis.azureus.core.messenger.PlatformMessage;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.core.messenger.PlatformMessengerListener;

public class 
PlatformMetaSearchMessenger 
{
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
	{
		PlatformMessage message = 
			new PlatformMessage( 
					"AZMSG", 
					OP_GET_TEMPLATE,
					OP_LIST_POPULAR_TEMPLATES, 
					getParameter( template_id ), 
					0 );

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
					System.out.println( "got reply: " + reply );
				}
			});
	}
	
	public static void 
	listPopularTemplates()
	{
		PlatformMessage message = 
			new PlatformMessage( 
					"AZMSG", 
					LISTENER_ID_TEMPLATE,
					OP_LIST_POPULAR_TEMPLATES, 
					new Object[0], 
					0 );

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
					System.out.println( "got reply: " + reply );
				}
			});
	}
	
	public static void 
	listFeaturedTemplates()
	{
		PlatformMessage message = 
			new PlatformMessage( 
					"AZMSG", 
					LISTENER_ID_TEMPLATE,
					OP_LIST_FEATURED_TEMPLATES, 
					new Object[0], 
					0 );

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
					System.out.println( "got reply: " + reply );
				}
			});
	}
	
	public static void 
	templateExists(
		long	template_id )
	{
		PlatformMessage message = 
			new PlatformMessage( 
					"AZMSG", 
					LISTENER_ID_TEMPLATE,
					OP_TEMPLATE_EXISTS, 
					getParameter( template_id ), 
					0 );

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
					System.out.println( "got reply: " + reply );
				}
			});
	}
	
	public static void 
	templateSelected(
		long	template_id )
	{
		PlatformMessage message = 
			new PlatformMessage( 
					"AZMSG", 
					LISTENER_ID_TEMPLATE,
					OP_TEMPLATE_SELECTED, 
					getParameter( template_id ), 
					0 );

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
					System.out.println( "got reply: " + reply );
				}
			});
	}
	
	public static void 
	templateUnselected(
		long	template_id )
	{
		PlatformMessage message = 
			new PlatformMessage( 
					"AZMSG", 
					LISTENER_ID_TEMPLATE,
					OP_TEMPLATE_UNSELECTED, 
					getParameter( template_id ), 
					0 );

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
					System.out.println( "got reply: " + reply );
				}
			});
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
