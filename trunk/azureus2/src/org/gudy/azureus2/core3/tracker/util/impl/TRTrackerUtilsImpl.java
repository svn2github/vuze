/*
 * File    : TRTrackerClientUtils.java
 * Created : 29-Feb-2004
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.core3.tracker.util.impl;

/**
 * @author parg
 *
 */

import java.util.*;
import java.net.*;
import java.io.*;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.tracker.client.classic.TRTrackerClientClassicImpl;

public class 
TRTrackerUtilsImpl 
{
	// author of MakeTorrent has requested we blacklist his site
	// as people keep embedding it as a tracker in torrents

	private static String[]	BLACKLISTED_HOSTS	=  
		{ "krypt.dyndns.org" };

	private static int[]		BLACKLISTED_PORTS	= 
		{ 81 };

	private static String		tracker_ip;
	private static String		bind_ip;
	
	static{
	
		COConfigurationManager.addListener(
			new COConfigurationListener()
			{
				public void
				configurationSaved()
				{
					readConfig();
				}
			});
		
		readConfig();
	}

	static void
	readConfig()
	{
		tracker_ip 		= COConfigurationManager.getStringParameter("Tracker IP", "");
	
		bind_ip 		= COConfigurationManager.getStringParameter("Bind IP", "");
	}
	
	public static boolean
	isHosting(
		URL		url_in )
	{
		return( tracker_ip.length() > 0  &&
				url_in.getHost().equalsIgnoreCase( tracker_ip ));
	}
	
	public static URL
	adjustURLForHosting(
		URL		url_in )
	{
		if ( isHosting( url_in )){
					
			String	url = url_in.getProtocol() + "://";
	
			if ( bind_ip.length() < 7 ){
					
				url += "127.0.0.1";
					
			}else{
					
				url += bind_ip;
			}		
			
			int	port = url_in.getPort();
			
			if ( port != -1 ){
				
				url += ":" + url_in.getPort();
			}
			
			url += url_in.getPath();
			
			String query = url_in.getQuery();
			
			if ( query != null ){
				
				url += "?" + query;
			}
							
			try{
				return( new URL( url ));
				
			}catch( MalformedURLException e ){
				
				e.printStackTrace();
			}
		}
		
		return( url_in );
	}
	
	public static String
	adjustHostFromHosting(
		String		host_in )
	{
		if ( tracker_ip.length() > 0 ){
				
			if ( host_in.equals( "127.0.0.1")){
				
				//System.out.println( "adjustHostFromHosting: " + host_in + " -> " + tracker_ip );
				
				return( tracker_ip );
			}
			
			if ( host_in.equals( bind_ip )){
				
				//System.out.println( "adjustHostFromHosting: " + host_in +  " -> " + tracker_ip );

				return( tracker_ip );
			}
		}
		
		return( host_in );
	}
	
	public static void
	checkForBlacklistedURLs(
		URL		url )
	
		throws IOException
	{
		for (int i=0;i<BLACKLISTED_HOSTS.length;i++){
 			
 			if ( 	url.getHost().equalsIgnoreCase( BLACKLISTED_HOSTS[i] ) &&
 					url.getPort() == BLACKLISTED_PORTS[i] ){
 		
 				throw( new IOException( "http://" + BLACKLISTED_HOSTS[i] +
 						":" + BLACKLISTED_PORTS[i] + "/ is not a tracker" ));
 			}
 		}
	}
	

	public static Map
	mergeResponseCache(
		Map		map1,
		Map		map2 )
	{
		return( TRTrackerClientClassicImpl.mergeResponseCache( map1, map2 ));
	}
}
