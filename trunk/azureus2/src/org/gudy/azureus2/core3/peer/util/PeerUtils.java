/*
 * Created on Mar 20, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004 Alon Rohter, All Rights Reserved.
 * 
 */
package org.gudy.azureus2.core3.peer.util;

import java.util.HashSet;
import java.util.Set;

import org.gudy.azureus2.core3.config.*;


/**
 * Varies peer connection utility methods.
 */
public class PeerUtils {

   private static final String	CONFIG_MAX_CONN_PER_TORRENT	= "Max.Peer.Connections.Per.Torrent";
   private static final String	CONFIG_MAX_CONN_TOTAL		= "Max.Peer.Connections.Total";
   
   private static int maxConnPerTorrent;
   private static int maxConnTotal;

   static{
   	
   	COConfigurationManager.addParameterListener(
   		CONFIG_MAX_CONN_PER_TORRENT,
   		new ParameterListener()
		{
   			public void 
			parameterChanged(
				String parameterName )
   			{
   				maxConnPerTorrent = COConfigurationManager.getIntParameter(CONFIG_MAX_CONN_PER_TORRENT);
   			}
		});
   	
   	maxConnPerTorrent = COConfigurationManager.getIntParameter(CONFIG_MAX_CONN_PER_TORRENT);
   	
  	COConfigurationManager.addParameterListener(
  			CONFIG_MAX_CONN_TOTAL,
  	   		new ParameterListener()
  			{
  	   			public void 
  				parameterChanged(
  					String parameterName )
  	   			{
  	   				maxConnTotal = COConfigurationManager.getIntParameter(CONFIG_MAX_CONN_TOTAL);
  	   			}
  			});
  	   	
  	maxConnTotal = COConfigurationManager.getIntParameter(CONFIG_MAX_CONN_TOTAL);
   }
  /**
   * Get the number of new peer connections allowed for the given data item,
   * within the configured per-torrent and global connection limits.
   * @return max number of new connections allowed, or -1 if there is no limit
   */
  public static int numNewConnectionsAllowed( byte[] data_id ) {
    int curConnPerTorrent = PeerIdentityManager.getIdentityCount( data_id );
    int curConnTotal = PeerIdentityManager.getTotalIdentityCount();
	    
    int perTorrentAllowed = -1;  //default unlimited
    if ( maxConnPerTorrent != 0 ) {  //if limited
      int allowed = maxConnPerTorrent - curConnPerTorrent;
      if ( allowed < 0 )  allowed = 0;
      perTorrentAllowed = allowed;
    }
	    
    int totalAllowed = -1;  //default unlimited
    if ( maxConnTotal != 0 ) {  //if limited
      int allowed = maxConnTotal - curConnTotal;
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
		String	str = COConfigurationManager.getStringParameter( "Ignore.peer.ports" ).trim();
		
		ignore_peer_ports.clear();
		
		if ( str.length() > 0 ){
			
			int	pos = 0;
			
			while(true){
				
				int	p1 = str.indexOf( ';', pos );
				
				String	bit;
				
				if ( p1 == -1 ){
					
					bit = str.substring(pos);
					
				}else{
					
					bit = str.substring(pos,p1);
					
					pos	= p1+1;
				}
				
				bit	= bit.trim();
							
				ignore_peer_ports.add( bit );
				
				if ( p1 == -1 ){
					
					break;
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
}
