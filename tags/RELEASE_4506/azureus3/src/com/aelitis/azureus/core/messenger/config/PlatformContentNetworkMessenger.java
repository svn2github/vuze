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

import org.gudy.azureus2.core3.util.Constants;
import org.json.simple.JSONObject;

import com.aelitis.azureus.core.messenger.*;



public class 
PlatformContentNetworkMessenger 
{
	private static final String LISTENER_ID = "cnetworks";

	private static final PlatformMessengerConfig	dispatcher = 
			new PlatformMessengerConfig( LISTENER_ID, true );

	private static final String OP_LIST_CNETORKS	= "list-networks";


	
	public static List<contentNetworkDetails> 
	listNetworks()
	
		throws PlatformMessengerException
	{
		if (true) {
			return new ArrayList<contentNetworkDetails>(0);
		}

		JSONObject parameters = new JSONObject();
		
		parameters.put( "azver", Constants.AZUREUS_VERSION );
		
		Map reply = dispatcher.syncInvoke( OP_LIST_CNETORKS, parameters ); 

		List<Map>	networks = (List<Map>)reply.get( "networks" );
		
		if ( networks == null ){
			
			throw( new PlatformMessengerException( "No networks returned" ));
		}
		
		List<contentNetworkDetails> result = new ArrayList<contentNetworkDetails>();
		
		for ( Map map: networks ){
			
			result.add( new contentNetworkDetails( map ));
		}
		
		return( result );
	}                       	
	
	public static void 
	listNetworksAync(final listNetworksListener l, int maxDelayMS)
	{
		if (l != null) {
			l.networkListReturned(new ArrayList<contentNetworkDetails>(0));
			return;
		}

		JSONObject parameters = new JSONObject();

		parameters.put("azver", Constants.AZUREUS_VERSION);

		PlatformMessage message = new PlatformMessage("AZMSG", LISTENER_ID,
				OP_LIST_CNETORKS, parameters, maxDelayMS);

		PlatformMessengerListener listener = new PlatformMessengerListener() {
			// @see com.aelitis.azureus.core.messenger.PlatformMessengerListener#replyReceived(com.aelitis.azureus.core.messenger.PlatformMessage, java.lang.String, java.util.Map)
			public void replyReceived(PlatformMessage message, String replyType,
					Map reply) {
				List<Map> networks = (List<Map>) reply.get("networks");

				List<contentNetworkDetails> result;

				if (networks == null) {
					result = null;
				} else {
  
  				result = new ArrayList<contentNetworkDetails>();
  
  				for (Map map : networks) {
  
  					result.add(new contentNetworkDetails(map));
  				}
				}

				if (l != null) {
					l.networkListReturned(result);
				}
			}

			// @see com.aelitis.azureus.core.messenger.PlatformMessengerListener#messageSent(com.aelitis.azureus.core.messenger.PlatformMessage)
			public void messageSent(PlatformMessage message) {
			}
		};

		PlatformMessenger.queueMessage(message, listener);
	}                       	
	
	public static class
	contentNetworkDetails
	{
		private Map		details;
		
		protected
		contentNetworkDetails(
			Map		_details )
		{
			details	= _details;
		}
		
		public long
		getID()
		{
			return(((Long)details.get( "id" )).longValue());
		}
		
		public long
		getVersion()
		{
			return(((Long)details.get( "version" )).longValue());
		}
		
		public String
		getName()
		{
			return((String)details.get( "name" ));
		}
		
		public String
		getIconURL()
		{
			return((String)details.get( "iconUrl" ));
		}
		
		public String
		getMainURL()
		{
			return((String)details.get( "baseUrl" ));
		}
		
		public String
		getString()
		{
			return( "id=" + getID() + ";version=" + getVersion() + ";name=" + getName());
		}
	}
	
	public static interface
	listNetworksListener
	{
		public void networkListReturned(List<contentNetworkDetails> list);		
	}
}
