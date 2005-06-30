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
import org.gudy.azureus2.core3.tracker.client.impl.bt.TRTrackerBTAnnouncerImpl;
import org.gudy.azureus2.core3.tracker.host.TRHost;
import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.HostNameToIPResolver;

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
	
	private static Map			override_map;
	
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
	
		String override_ips		= COConfigurationManager.getStringParameter("Override Ip", "");
		
		StringTokenizer	tok = new StringTokenizer( override_ips, ";" );
		
		Map	new_override_map = new HashMap();
		
		while( tok.hasMoreTokens()){
			
			String	ip = tok.nextToken().trim();
			
			if ( ip.length() > 0 ){
				
				new_override_map.put( AENetworkClassifier.categoriseAddress( ip ), ip );
			}
		}
		
		override_map	= new_override_map;
		
		bind_ip 		= COConfigurationManager.getStringParameter("Bind IP", "");
	}
	
	public static boolean
	isHosting(
		URL		url_in )
	{
		return( tracker_ip.length() > 0  &&
				url_in.getHost().equalsIgnoreCase( tracker_ip ));
	}
	
	public static String
	getTrackerIP()
	{
		return( tracker_ip );
	}
	
	public static URL[][]
	getAnnounceURLs()
	{
		String	tracker_host = COConfigurationManager.getStringParameter( "Tracker IP", "" );

		List	urls = new ArrayList();
				
		if ( tracker_host.length() > 0 ){
			
			if ( COConfigurationManager.getBooleanParameter( "Tracker Port Enable", false )){
										
				int port = COConfigurationManager.getIntParameter("Tracker Port", TRHost.DEFAULT_PORT );
				
				try{
					List	l = new ArrayList();
					
					l.add( new URL( "http://" + tracker_host + ":" + port + "/announce" ));
					
					List	ports = stringToPorts( COConfigurationManager.getStringParameter("Tracker Port Backups" ));
					
					for (int i=0;i<ports.size();i++){
						
						l.add( new URL( "http://" + tracker_host + ":" + ((Integer)ports.get(i)).intValue() + "/announce" ));
					}

					urls.add( l );
					
				}catch( MalformedURLException e ){
					
					Debug.printStackTrace( e );
				}
			}
			
			if ( COConfigurationManager.getBooleanParameter( "Tracker Port SSL Enable", false )){
				
				int port = COConfigurationManager.getIntParameter("Tracker Port SSL", TRHost.DEFAULT_PORT_SSL );
				
				try{
					List	l = new ArrayList();
					
					l.add( new URL( "https://" + tracker_host + ":" + port + "/announce" ));
					
					List	ports = stringToPorts( COConfigurationManager.getStringParameter("Tracker Port SSL Backups" ));
					
					for (int i=0;i<ports.size();i++){
						
						l.add( new URL( "https://" + tracker_host + ":" + ((Integer)ports.get(i)).intValue() + "/announce" ));
					}

					urls.add( l );
					

				}catch( MalformedURLException e ){
				
					Debug.printStackTrace( e );
				}
			}
			
			if ( COConfigurationManager.getBooleanParameter( "Tracker Port UDP Enable" )){
				
				int port = COConfigurationManager.getIntParameter("Tracker Port", TRHost.DEFAULT_PORT );
				
				boolean	auth = COConfigurationManager.getBooleanParameter( "Tracker Password Enable Torrent" );
					
				try{
					List	l = new ArrayList();
					
					l.add( new URL( "udp://" + tracker_host + ":" + port + "/announce" +
										(auth?"?auth":"" )));
				
					urls.add( l );
					
				}catch( MalformedURLException e ){
				
					Debug.printStackTrace( e );
				}
			}
		}
		
		URL[][]	res = new URL[urls.size()][];
		
		for (int i=0;i<urls.size();i++){
			
			List	l = (List)urls.get(i);
			
			URL[]	u = new URL[l.size()];
			
			l.toArray( u );
			
			res[i] = u;
		}
				
		return( res );		
	}
	
	protected static List
	stringToPorts(
		String	str )
	{
		str = str.replace(',', ';' );
		
		StringTokenizer	tok = new StringTokenizer( str, ";" );
		
		List	res = new ArrayList();
		
		while( tok.hasMoreTokens()){
			
			try{
				res.add( new Integer( tok.nextToken().trim()));
				
			}catch( Throwable e ){
				
				Debug.out("Invalid port entry in '" + str + "'", e);
			}
		}
		
		return( res );
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
				
				Debug.printStackTrace( e );
			}
		}
		
		return( url_in );
	}
	
	public static String
	adjustHostFromHosting(
		String		host_in )
	{
		if ( tracker_ip.length() > 0 ){
				
			String	address_type = AENetworkClassifier.categoriseAddress( host_in );
			
			String	target_ip = (String)override_map.get( address_type );
			
			if ( target_ip == null ){
				
				target_ip	= tracker_ip;
			}
			
			if ( host_in.equals( "127.0.0.1")){
				
				//System.out.println( "adjustHostFromHosting: " + host_in + " -> " + tracker_ip );
				
				return( target_ip );
			}
			
			if ( host_in.equals( bind_ip )){
				
				//System.out.println( "adjustHostFromHosting: " + host_in +  " -> " + tracker_ip );

				return( target_ip );
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
		return( TRTrackerBTAnnouncerImpl.mergeResponseCache( map1, map2 ));
	}
}
