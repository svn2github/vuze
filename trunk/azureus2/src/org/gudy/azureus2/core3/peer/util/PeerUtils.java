/*
 * Created on Mar 20, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004 Alon Rohter, All Rights Reserved.
 * 
 */
package org.gudy.azureus2.core3.peer.util;

import org.gudy.azureus2.core3.config.*;


/**
 * Varies peer connection utility methods.
 */
public class PeerUtils {

  /**
   * Get the number of new peer connections allowed for the given data item,
   * within the configured per-torrent and global connection limits.
   * @return max number of new connections allowed, or -1 if there is no limit
   */
  public static synchronized int numNewConnectionsAllowed( byte[] data_id ) {
    int maxConnPerTorrent = COConfigurationManager.getIntParameter("Max.Peer.Connections.Per.Torrent");
    int maxConnTotal = COConfigurationManager.getIntParameter("Max.Peer.Connections.Total");
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
  

}
