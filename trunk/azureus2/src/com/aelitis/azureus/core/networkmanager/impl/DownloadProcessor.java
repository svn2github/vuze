/*
 * Created on Mar 14, 2005
 * Created by Alon Rohter
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.networkmanager.impl;

import java.util.HashMap;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.util.AEMonitor;

import com.aelitis.azureus.core.networkmanager.*;


/**
 *
 */
public class DownloadProcessor {  
  private int global_max_rate_bps;
  private final ByteBucket global_bucket;
  private final MultiPeerDownloader downloader;
  
  private final HashMap group_buckets = new HashMap();
  private final HashMap peer_connections = new HashMap();
  private final AEMonitor peer_connections_mon = new AEMonitor( "DownloadProcessor" );
  
  private boolean downloader_registered = false;
  
  

  public DownloadProcessor() {
    int max_rateKBs = COConfigurationManager.getIntParameter( "Max Download Speed KBs" );
    global_max_rate_bps = max_rateKBs == 0 ? NetworkManager.UNLIMITED_RATE : max_rateKBs * 1024;
    COConfigurationManager.addParameterListener( "Max Download Speed KBs", new ParameterListener() {
      public void parameterChanged( String parameterName ) {
        int rateKBs = COConfigurationManager.getIntParameter( "Max Download Speed KBs" );
        global_max_rate_bps = rateKBs == 0 ? NetworkManager.UNLIMITED_RATE : rateKBs * 1024;
      }
    });
    
    global_bucket = new ByteBucket( global_max_rate_bps ); 
    
    downloader = new MultiPeerDownloader( new RateHandler() {
      public int getCurrentNumBytesAllowed() {
        if( global_bucket.getRate() != global_max_rate_bps ) { //sync rate
          global_bucket.setRate( global_max_rate_bps );
        }
        return global_bucket.getAvailableByteCount();
      }
      
      public void bytesProcessed( int num_bytes_written ) {
        global_bucket.setBytesUsed( num_bytes_written );
      }
    });
  }
  
  
  
  /**
   * Register peer connection for download handling.
   * @param connection to register
   * @param group rate limit group
   */
  public void registerPeerConnection( NetworkConnection connection, LimitedRateGroup group ) {
    final ConnectionData conn_data = new ConnectionData();
    
    try{  peer_connections_mon.enter();
      if( !downloader_registered ) {
        NetworkManager.getSingleton().addReadEntity( downloader );  //register main download entity
        downloader_registered = true;
      }
    
      //do group registration
      GroupData group_data = (GroupData)group_buckets.get( group );
      if( group_data == null ) {
        int limit = NetworkManagerUtilities.getGroupRateLimit( group );
        group_data = new GroupData( new ByteBucket( limit ) );
        group_buckets.put( group, group_data );
      }
      group_data.group_size++;
      conn_data.group = group;
      conn_data.group_data = group_data;
      //conn_data.state = ConnectionData.STATE_NORMAL;
    
      peer_connections.put( connection, conn_data );
    }
    finally {  peer_connections_mon.exit();  }
    
    downloader.addPeerConnection( connection, new RateHandler() {
      public int getCurrentNumBytesAllowed() {
        // sync group rate
        int group_rate = NetworkManagerUtilities.getGroupRateLimit( conn_data.group );
        if( conn_data.group_data.bucket.getRate() != group_rate ) {
          conn_data.group_data.bucket.setRate( group_rate );
        }
        
        return conn_data.group_data.bucket.getAvailableByteCount();
      }

      public void bytesProcessed( int num_bytes_written ) {
        conn_data.group_data.bucket.setBytesUsed( num_bytes_written );
      }
    });
    
    //start read processing
    connection.getIncomingMessageQueue().startQueueProcessing();
  }
  
  
  
  /**
   * Cancel download handling for the given peer connection.
   * @param connection to cancel
   */
  public void deregisterPeerConnection( NetworkConnection connection ) {
    connection.getIncomingMessageQueue().stopQueueProcessing();  //stop reading incoming messages
    
    downloader.removePeerConnection( connection );

    try{  peer_connections_mon.enter();
      ConnectionData conn_data = (ConnectionData)peer_connections.remove( connection );
    
      if( conn_data != null ) {
        //do group de-registration
        if( conn_data.group_data.group_size == 1 ) {  //last of the group
          group_buckets.remove( conn_data.group ); //so remove
        }
        else {
          conn_data.group_data.group_size--;
        }
      }
    }
    finally{  peer_connections_mon.exit();  }
  }

  

  
  private static class ConnectionData {
    //private static final int STATE_NORMAL   = 0;
    //private static final int STATE_UPGRADED = 1;
    
    //private int state;
    private LimitedRateGroup group;
    private GroupData group_data;
  }

    
  private static class GroupData {
    private final ByteBucket bucket;
    private int group_size = 0;
    
    private GroupData( ByteBucket bucket ) {
      this.bucket = bucket;
    }
  }
  
}
