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

import org.gudy.azureus2.core3.util.Debug;
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

	private static final String URL_SUFFIX	= ConstantsV3.URL_SUFFIX;
	
	private static final String DEFAULT_AUTHORIZED_RPC = "https://" + URL_ADDRESS + ":443/rpc";

	private static String URL_RELAY_RPC = System.getProperty("relay_url",
			"http://" + System.getProperty("relay_address", DEFAULT_RELAY_ADDRESS)
					+ ":" + System.getProperty("relay_port", DEFAULT_RELAY_PORT)
					+ "/msgrelay/rpc");

	private static final String URL_AUTHORIZED_RPC = System.getProperty(
			"authorized_rpc", "1").equals("1") ? DEFAULT_AUTHORIZED_RPC : URL_PREFIX
			+ "app";
	
	private Map<Integer, String>		service_map = new HashMap<Integer, String>();
	
	
	protected
	ContentNetworkVuze()
	{
		 super( ContentNetwork.CONTENT_NETWORK_VUZE );
		 
		 addService( SERVICE_SEARCH, 			URL_PREFIX + "search?q=" );
		 addService( SERVICE_XSEARCH, 			URL_PREFIX + "xsearch?q=" );
		 addService( SERVICE_RPC, 				URL_PREFIX + "rpc/" );
		 addService( SERVICE_RELAY_RPC, 		URL_RELAY_RPC );
		 addService( SERVICE_AUTH_RPC, 			URL_AUTHORIZED_RPC );
		 addService( SERVICE_BIG_BROWSE, 		URL_PREFIX + "browse.start" + "?" + URL_SUFFIX );
		 addService( SERVICE_PUBLISH, 			URL_PREFIX + "publish.start" + "?" + URL_SUFFIX );
		 addService( SERVICE_WELCOME, 			URL_PREFIX + "welcome.start" + "?" + URL_SUFFIX );
		 addService( SERVICE_PUBLISH_NEW, 		URL_PREFIX + "publishnew.start" + "?" + URL_SUFFIX );
		 addService( SERVICE_PUBLISH_ABOUT, 	URL_PREFIX + "publishinfo.start" );
		 addService( SERVICE_CONTENT_DETAILS, 	URL_PREFIX + "details/" );
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
		String	base = getServiceURL( service_type );
		
		if ( base == null ){
			
			Debug.out( "Unknown service type '" + service_type + "'" );
			
			return( null );
		}
		
		switch( service_type ){
		
			case SERVICE_SEARCH:{
				
				String	query = (String)params[0];
				
				return(	base +
						UrlUtils.encode(query) + 
						"&" + ConstantsV3.URL_SUFFIX + 
						"&rand=" + SystemTime.getCurrentTime());
			}
			
			case SERVICE_XSEARCH:{
				
				String	query 			= (String)params[0];
				boolean	to_subscribe	= (Boolean)params[1];
				
				String url_str = 
							base +
							UrlUtils.encode(query) + 
							"&" + ConstantsV3.URL_SUFFIX + 
							"&rand=" + SystemTime.getCurrentTime();
				
				if ( to_subscribe ){
					
					url_str += "&createSubscription=1";
				}
				
				return( url_str );
			}
			case SERVICE_CONTENT_DETAILS:{
				
				String	hash 		= (String)params[0];
				String	client_ref 	= (String)params[1];
				
				String url_str = base + hash + ".html?" + ConstantsV3.URL_SUFFIX;
				
				if ( client_ref != null ){
					
					url_str += "&client_ref=" + client_ref;
				}
				
				return( url_str );
			}
			
			default:{
				
				return( base );
			}
		}
	}
}
