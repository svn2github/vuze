/*
 * Created on Mar 20, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 * 
 */
package org.gudy.azureus2.core3.peer.util;

import java.net.InetAddress;
import java.util.*;


import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.peers.Peer;
import org.gudy.azureus2.plugins.utils.LocationProvider;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;

import com.aelitis.azureus.core.AzureusCoreFactory;


/**
 * Varies peer connection utility methods.
 */
public class PeerUtils {

   private static final String	CONFIG_MAX_CONN_PER_TORRENT	= "Max.Peer.Connections.Per.Torrent";
   private static final String	CONFIG_MAX_CONN_TOTAL		= "Max.Peer.Connections.Total";
   
   public static int MAX_CONNECTIONS_PER_TORRENT;
   public static int MAX_CONNECTIONS_TOTAL;

   static{
   	
   	COConfigurationManager.addParameterListener(
   		CONFIG_MAX_CONN_PER_TORRENT,
   		new ParameterListener()
		{
   			public void 
			parameterChanged(
				String parameterName )
   			{
         MAX_CONNECTIONS_PER_TORRENT = COConfigurationManager.getIntParameter(CONFIG_MAX_CONN_PER_TORRENT);
   			}
		});
   	
   	MAX_CONNECTIONS_PER_TORRENT = COConfigurationManager.getIntParameter(CONFIG_MAX_CONN_PER_TORRENT);
   	
  	COConfigurationManager.addParameterListener(
  			CONFIG_MAX_CONN_TOTAL,
  	   		new ParameterListener()
  			{
  	   			public void 
  				parameterChanged(
  					String parameterName )
  	   			{
             MAX_CONNECTIONS_TOTAL = COConfigurationManager.getIntParameter(CONFIG_MAX_CONN_TOTAL);
  	   			}
  			});
  	   	
  	MAX_CONNECTIONS_TOTAL = COConfigurationManager.getIntParameter(CONFIG_MAX_CONN_TOTAL);
   }
  /**
   * Get the number of new peer connections allowed for the given data item,
   * within the configured per-torrent and global connection limits.
   * @return max number of new connections allowed, or -1 if there is no limit
   */
  public static int numNewConnectionsAllowed( PeerIdentityDataID data_id, int specific_max ) {
    int curConnPerTorrent = PeerIdentityManager.getIdentityCount( data_id );
    int curConnTotal = PeerIdentityManager.getTotalIdentityCount();
	    
    	// specific max here will default to the global per-torrent default if not explicitly set
    	// so we don't need to consider CONFIG_MAX_CONN_PER_TORRENT seperately
    
    int	PER_TORRENT_LIMIT = specific_max;
    
    int perTorrentAllowed = -1;  //default unlimited
    if ( PER_TORRENT_LIMIT != 0 ) {  //if limited
      int allowed = PER_TORRENT_LIMIT - curConnPerTorrent;
      if ( allowed < 0 )  allowed = 0;
      perTorrentAllowed = allowed;
    }
	    
    int totalAllowed = -1;  //default unlimited
    if ( MAX_CONNECTIONS_TOTAL != 0 ) {  //if limited
      int allowed = MAX_CONNECTIONS_TOTAL - curConnTotal;
      if ( allowed < 0 )  allowed = 0;
      totalAllowed = allowed;
    }
	    
    int allowed = -1;  //default unlimited
    if ( perTorrentAllowed > -1 && totalAllowed > -1 ) {  //if both limited
      allowed = Math.min( perTorrentAllowed, totalAllowed );
    }
    else if ( perTorrentAllowed == -1 || totalAllowed == -1 ) {  //if either unlimited
      allowed = Math.max( perTorrentAllowed, totalAllowed );
    }
	    
    return allowed;
  }
  

	private static Set	ignore_peer_ports	= new HashSet();
	
	static{
		COConfigurationManager.addParameterListener(
				"Ignore.peer.ports",
				new ParameterListener()
				{
					public void 
					parameterChanged(
						String parameterName )
					{
						readIgnorePeerPorts();
					}
				});
		
		readIgnorePeerPorts();
	}
	
	private static void
	readIgnorePeerPorts()
	{
		// XXX Optimize me for ranges!!
		String	str = COConfigurationManager.getStringParameter( "Ignore.peer.ports" ).trim();
		
		ignore_peer_ports.clear();
		
		if ( str.length() > 0 ){
			
			String[] ports = str.split("\\;");
			if (ports != null && ports.length > 0) {
				for (int i = 0; i < ports.length; i++) {
					String port = ports[i];
					int spreadPos = port.indexOf('-');
					if (spreadPos > 0 && spreadPos < port.length() - 1) {
						try {
							int iMin = Integer.parseInt(port.substring(0, spreadPos).trim());
							int iMax = Integer.parseInt(port.substring(spreadPos + 1).trim());
							
							for (int j = iMin; j <= iMax; j++) {
								ignore_peer_ports.add("" + j);
							}
						} catch (Exception e) {
						}
					} else {
						ignore_peer_ports.add(port.trim());
					}
				}
			}
		}
	}
	
	public static boolean
	ignorePeerPort(
		int		port )
	{
		return( ignore_peer_ports.contains( "" + port ));
	}
	
	static final String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

	public static byte[]
	createPeerID()
	{
		byte[] peerId = new byte[20];
	
		byte[] version = Constants.VERSION_ID;
    
		for (int i = 0; i < 8; i++) {
			peerId[i] = version[i];
		}
    
	 	for (int i = 8; i < 20; i++) {
		  int pos = (int) ( Math.random() * chars.length());
		  peerId[i] = (byte)chars.charAt(pos);
		}
	 	
		// System.out.println( "generated new peer id:" + ByteFormatter.nicePrint(peerId));

	 	return( peerId );
	}
	
	public static byte[]
	createWebSeedPeerID()
	{
		byte[] peerId = new byte[20];

		peerId[0] = '-';
		peerId[1] = 'W';
		peerId[2] = 'S';
		
		for (int i = 3; i < 20; i++) {
			int pos = (int) ( Math.random() * chars.length());
			peerId[i] = (byte)chars.charAt(pos);
		}

		// System.out.println( "generated new peer id:" + ByteFormatter.nicePrint(peerId));

		return( peerId );
	}
	
	
	private static volatile LocationProvider	country_provider;
	private static long							country_provider_last_check;

	private static Object	country_key 	= new Object();
		
	private static LocationProvider
	getCountryProvider()
	{
		if ( country_provider != null ){
			
			if ( country_provider.isDestroyed()){
				
				country_provider 			= null;
				country_provider_last_check	= 0;
			}
		}
		
		if ( country_provider == null ){
			
			long	now = SystemTime.getMonotonousTime();
			
			if ( country_provider_last_check == 0 || now - country_provider_last_check > 20*1000 ){
				
				country_provider_last_check = now;
				
				java.util.List<LocationProvider> providers = AzureusCoreFactory.getSingleton().getPluginManager().getDefaultPluginInterface().getUtilities().getLocationProviders();
	
				for ( LocationProvider provider: providers ){
					
					if ( 	provider.hasCapabilities( 
								LocationProvider.CAP_ISO3166_BY_IP |
								LocationProvider.CAP_COUNTY_BY_IP )){
						
						country_provider = provider;
					}
				}
			}
		}
		
		return( country_provider );
	}
	
	public static String[]
 	getCountryDetails(
 		Peer	peer )
	{
		return( getCountryDetails( PluginCoreUtils.unwrap( peer )));
	}
	
	public static String[]
	getCountryDetails(
		PEPeer	peer )
	{
		if ( peer == null ){
			
			return( null );
		}
		
		String[] details = (String[])peer.getUserData( country_key );
		
		if ( details == null ){
			
			LocationProvider lp = getCountryProvider();
			
			if ( lp != null ){
				
				try{				
					InetAddress peer_address = InetAddress.getByName( peer.getIp());
					
					String code = lp.getISO3166CodeForIP( peer_address );
					String name = lp.getCountryNameForIP( peer_address, Locale.getDefault());
					
					if ( code != null && name != null ){
						
						details = new String[]{ code, name };
						
					}else{
						
						details = new String[0];
					}
					
					peer.setUserData( country_key, details );
					
				}catch( Throwable e ){	
				}
			}
		}
		
		return( details );
	}
}
