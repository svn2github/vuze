/*
 * Created on Nov 24, 2008
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


package com.aelitis.azureus.core.cnetwork.impl;

import java.util.*;

import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.UrlUtils;

import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.util.ConstantsV3;

public class 
ContentNetworkVuze 
	extends ContentNetworkImpl
{
	private static final String DEFAULT_ADDRESS = "www.vuze.com"; //DO NOT TOUCH !!!!  use the -Dplatform_address=ip override instead

	private static final String DEFAULT_PORT = "80";

	private static final String DEFAULT_RELAY_ADDRESS = "www.vuze.com"; //DO NOT TOUCH !!!!  use the -Drelay_address=ip override instead

	private static final String DEFAULT_RELAY_PORT = "80";

	private static String URL_ADDRESS = System.getProperty("platform_address",
			DEFAULT_ADDRESS);

	private static String URL_PORT = System.getProperty("platform_port",
			DEFAULT_PORT);

	private static final String URL_PREFIX = "http://" + URL_ADDRESS + ":" + URL_PORT + "/";

	private Map<Integer, String>		service_map = new HashMap<Integer, String>();
	
	
	protected
	ContentNetworkVuze()
	{
		 super( ContentNetwork.CONTENT_NETWORK_VUZE );
		 
		 addService( SERVICE_SEARCH, 	URL_PREFIX + "search?q=" );
		 addService( SERVICE_XSEARCH, 	URL_PREFIX + "xsearch?q=" );
	}
	
	protected void
	addService(
		int		type,
		String	url_str )
	{
		 service_map.put( type, url_str );
	}
	
	public String
	getServiceURL(
		int			service_type )
	{
		return( service_map.get( service_type ));
	}
	
	
	public String	
	getServiceURL(
		int			service_type,
		Object[]	params )
	{
		switch( service_type ){
		
			case SERVICE_SEARCH:{
				
				String	query = (String)params[0];
				
				return( 
					getServiceURL( ContentNetwork.SERVICE_SEARCH ) +
							UrlUtils.encode(query) + 
							"&" + ConstantsV3.URL_SUFFIX + 
							"&rand=" + SystemTime.getCurrentTime());
			}
			
			case SERVICE_XSEARCH:{
				
				String	query 			= (String)params[0];
				boolean	to_subscribe	= (Boolean)params[1];
				
				String url_str = 
					getServiceURL( ContentNetwork.SERVICE_XSEARCH ) +
							UrlUtils.encode(query) + 
							"&" + ConstantsV3.URL_SUFFIX + 
							"&rand=" + SystemTime.getCurrentTime();
				
				if ( to_subscribe ){
					
					url_str += "&createSubscription=1";
				}
				
				return( url_str );
			}
			default:{
				
				return( null );
			}
		}
	}

}
