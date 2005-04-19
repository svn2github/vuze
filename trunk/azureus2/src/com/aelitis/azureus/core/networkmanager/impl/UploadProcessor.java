/*
 * Created on Oct 7, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
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

import java.util.*;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.util.AEMonitor;

import com.aelitis.azureus.core.networkmanager.*;



/**
 *
 */
public class UploadProcessor {
  private int standard_max_rate_bps;
  private final ByteBucket standard_bucket;
  private final HashMap standard_peer_connections = new HashMap();
  private final AEMonitor standard_peer_connections_mon = new AEMonitor( "UploadProcessor:SPC" );
  private final UploadEntityController standard_entity_controller;
  
  private final HashMap group_buckets = new HashMap();
  private final AEMonitor group_buckets_mon = new AEMonitor( "UploadProcessor:GB" );
  
  
  public UploadProcessor() {
    int max_rateKBs = COConfigurationManager.getIntParameter( "Max Upload Speed KBs" );
    standard_max_rate_bps = max_rateKBs == 0 ? NetworkManager.UNLIMITED_RATE : max_rateKBs * 1024;
    COConfigurationManager.addParameterListener( "Max Upload Speed KBs", new ParameterListener() {
      public void parameterChanged( String parameterName ) {
        int rateKBs = COConfigurationManager.getIntParameter( "Max Upload Speed KBs" );
        standard_max_rate_bps = rateKBs == 0 ? NetworkManager.UNLIMITED_RATE : rateKBs * 1024;
      }
    });
    
    standard_bucket = new ByteBucket( standard_max_rate_bps ); 
    
    standard_entity_controller = new UploadEntityController( new RateHandler() {
      public int getCurrentNumBytesAllowed() {
        if( standard_bucket.getRate() != standard_max_rate_bps ) { //sync rate
          standard_bucket.setRate( standard_max_rate_bps );
        }
        return standard_bucket.getAvailableByteCount();
      }
      
      public void bytesWritten( int num_bytes_written ) {
        standard_bucket.setBytesUsed( num_bytes_written );
      }
    });
  }
  

    
  
  /**
   * Register peer connection for upload handling.
   * NOTE: The given max rate limit is ignored until the connection is upgraded.
   * @param connection to register
   * @param group rate limit group
   */
  public void registerPeerConnection( NetworkConnection connection, LimitedRateGroup group ) {
    final ConnectionData conn_data = new ConnectionData();

    // do group registration
    GroupData group_data;
    try {
      group_buckets_mon.enter();
      
      group_data = (GroupData)group_buckets.get( group );
      if( group_data == null ) {
        int limit = NetworkManagerUtilities.getGroupRateLimit( group );
        group_data = new GroupData( new ByteBucket( limit ) );
        group_buckets.put( group, group_data );
      }
      group_data.group_size++;
    }
    finally {  group_buckets_mon.exit();  }

    conn_data.group = group;
    conn_data.group_data = group_data;
    conn_data.state = ConnectionData.STATE_NORMAL;

    try {
      standard_peer_connections_mon.enter();
      standard_peer_connections.put( connection, conn_data );
    }
    finally {
      standard_peer_connections_mon.exit();
    }

    standard_entity_controller.registerPeerConnection( connection );
  }
  
  
  
  /**
   * Cancel upload handling for the given peer connection.
   * @param connection to cancel
   */
  public void deregisterPeerConnection( NetworkConnection connection ) {
    ConnectionData conn_data = null;
    
    try{ standard_peer_connections_mon.enter();
      conn_data = (ConnectionData)standard_peer_connections.remove( connection );
    }
    finally{ standard_peer_connections_mon.exit(); }
    
    if( conn_data != null ) {
      //do group de-registration
      if( conn_data.group_data.group_size == 1 ) {  //last of the group
        try {  group_buckets_mon.enter();
          group_buckets.remove( conn_data.group ); //so remove
        }
        finally {  group_buckets_mon.exit();  }
      }
      else {
        conn_data.group_data.group_size--;
      }
    }
    
    standard_entity_controller.cancelPeerConnection( connection );
  }
  
  
  

  /**
   * Upgrade the given connection to a high-speed transfer handler.
   * @param connection to upgrade
   */
  public void upgradePeerConnection( NetworkConnection connection ) {
    ConnectionData connection_data = null;
    
    try{ standard_peer_connections_mon.enter();
      connection_data = (ConnectionData)standard_peer_connections.get( connection );
    }
    finally{ standard_peer_connections_mon.exit(); }
    
    if( connection_data != null && connection_data.state == ConnectionData.STATE_NORMAL ) {
      final ConnectionData conn_data = connection_data;
      
      standard_entity_controller.upgradePeerConnection( connection, new RateHandler() {
        public int getCurrentNumBytesAllowed() {
          // sync global rate
          if( standard_bucket.getRate() != standard_max_rate_bps ) {
            standard_bucket.setRate( standard_max_rate_bps );
          }
          // sync group rate
          int group_rate = NetworkManagerUtilities.getGroupRateLimit( conn_data.group );
          if( conn_data.group_data.bucket.getRate() != group_rate ) {
            conn_data.group_data.bucket.setRate( group_rate );
          }

          int group_allowed = conn_data.group_data.bucket.getAvailableByteCount();
          int global_allowed = standard_bucket.getAvailableByteCount();

          // reserve bandwidth for the general pool if needed
          if( standard_entity_controller.isGeneralPoolWriteNeeded() ) {
            global_allowed -= NetworkManager.getTcpMssSize();
            if( global_allowed < 0 ) global_allowed = 0;
          }

          int allowed = group_allowed > global_allowed ? global_allowed : group_allowed;
          return allowed;
        }

        public void bytesWritten( int num_bytes_written ) {
          conn_data.group_data.bucket.setBytesUsed( num_bytes_written );
          standard_bucket.setBytesUsed( num_bytes_written );
        }
      });
      
      conn_data.state = ConnectionData.STATE_UPGRADED;
    }
  }
  
  
  /**
   * Downgrade the given connection back to a normal-speed transfer handler.
   * @param connection to downgrade
   */
  public void downgradePeerConnection( NetworkConnection connection ) {
    ConnectionData conn_data = null;
    
    try{ standard_peer_connections_mon.enter();
      conn_data = (ConnectionData)standard_peer_connections.get( connection );
    }
    finally{ standard_peer_connections_mon.exit(); }
    
    if( conn_data != null && conn_data.state == ConnectionData.STATE_UPGRADED ) {
      standard_entity_controller.downgradePeerConnection( connection );
      conn_data.state = ConnectionData.STATE_NORMAL;
    }
  }
  
  

  
  private static class ConnectionData {
    private static final int STATE_NORMAL   = 0;
    private static final int STATE_UPGRADED = 1;
    
    private int state;
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
