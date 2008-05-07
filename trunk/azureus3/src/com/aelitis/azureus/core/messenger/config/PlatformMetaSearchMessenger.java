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

import java.util.Map;


import com.aelitis.azureus.core.messenger.PlatformMessage;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.core.messenger.PlatformMessengerListener;

public class 
PlatformMetaSearchMessenger 
{
	public static final String LISTENER_ID_TEMPLATE = "searchtemplate";

	public static final String OP_GETMOSTPOPULARTEMPLATES = "list-popular";


	public static void 
	getMostPopularTemplates()
	{
		PlatformMessage message = 
			new PlatformMessage( 
					"AZMSG", 
					LISTENER_ID_TEMPLATE,
					OP_GETMOSTPOPULARTEMPLATES, 
					new Object[0], 
					0 );

		message.setRequiresAuthorization( false );

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
}
