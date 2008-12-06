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


import java.io.File;

import com.aelitis.azureus.core.cnetwork.ContentNetwork;

public class 
ContentNetworkVuze 
	extends ContentNetworkVuzeGeneric
{
	private static final String DEFAULT_ADDRESS = "www.vuze.com"; //DO NOT TOUCH !!!!  use the -Dplatform_address=ip override instead

	private static final String DEFAULT_PORT = "80";

	private static final String DEFAULT_RELAY_ADDRESS = "www.vuze.com"; //DO NOT TOUCH !!!!  use the -Drelay_address=ip override instead

	private static final String DEFAULT_RELAY_PORT = "80";

	private static final String URL_ADDRESS = System.getProperty( "platform_address", DEFAULT_ADDRESS );

	private static final String URL_PORT 	= System.getProperty( "platform_port", DEFAULT_PORT );

	private static final String URL_PREFIX = "http://" + URL_ADDRESS + ":" + URL_PORT + "/";

	
	
	private static final String DEFAULT_AUTHORIZED_RPC = "https://" + URL_ADDRESS + ":443/rpc";

	private static String URL_RELAY_RPC = System.getProperty("relay_url",
			"http://" + System.getProperty("relay_address", DEFAULT_RELAY_ADDRESS)
					+ ":" + System.getProperty("relay_port", DEFAULT_RELAY_PORT)
					+ "/msgrelay/rpc");

	private static final String URL_AUTHORIZED_RPC = System.getProperty(
			"authorized_rpc", "1").equals("1") ? DEFAULT_AUTHORIZED_RPC : URL_PREFIX
			+ "app";
	
	private static final String URL_FAQ = "http://faq.vuze.com/";

	private static final String URL_BLOG = "http://blog.vuze.com/";
	
	private static final String URL_FORUMS = "http://forum.vuze.com/";
	
	private static final String URL_WIKI = "http://wiki.vuze.com/";

	protected
	ContentNetworkVuze(
		ContentNetworkManagerImpl	manager )
	{
		super( 	manager,
				ContentNetwork.CONTENT_NETWORK_VUZE,
				1,
				"Vuze HD Network",
				URL_ADDRESS,
				URL_PREFIX,
				null,			// no icon
				URL_RELAY_RPC,
				URL_AUTHORIZED_RPC,
				URL_FAQ,
				URL_BLOG,
				URL_FORUMS,
				URL_WIKI );
	}
	
	public static void
	main(
		String[]	args )
	{
		ContentNetworkManagerImpl.getSingleton();
		
		ContentNetwork test = 
			new ContentNetworkVuzeGeneric(
				null,
				ContentNetwork.CONTENT_NETWORK_CHUN,
				1,
				"Chun's Network",
				"192.168.0.74",
				"http://192.168.0.74:8082/",
				"http://play.aelitis.com:88/parg/chun.png",
				null,
				null,
				null,
				null,
				null,
				null );
				
		try{
			test.getVuzeFile().write( new File( "C:\\temp\\rfn_chun.vuze"));
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
}
