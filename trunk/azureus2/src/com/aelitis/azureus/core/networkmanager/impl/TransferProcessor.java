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

import org.gudy.azureus2.core3.util.AEMonitor;

import com.aelitis.azureus.core.networkmanager.*;



/**
 *
 */
public class TransferProcessor {
  public static final int TYPE_UPLOAD   = 0;
  public static final int TYPE_DOWNLOAD = 1;
  
  private final LimitedRateGroup max_rate;
  
  private final ByteBucket main_bucket;
  private final EntityHandler main_controller;
  
  private final HashMap group_buckets = new HashMap();
  private final HashMap connections = new HashMap();
  private final AEMonitor connections_mon;

  
  /**
   * Create new transfer processor for the given read/write type, limited to the given max rate.
   * @param processor_type read or write processor
   * @param max_rate_limit to use
   */
  public TransferProcessor( int processor_type, LimitedRateGroup max_rate_limit ) {
    this.max_rate = max_rate_limit;
    
    connections_mon = new AEMonitor( "TransferProcessor:" +processor_type );

    main_bucket = new ByteBucket( max_rate.getRateLimitBytesPerSecond() ); 

    main_controller = new EntityHandler( processor_type, new RateHandler() {
      public int getCurrentNumBytesAllowed() {
        if( main_bucket.getRate() != max_rate.getRateLimitBytesPerSecond() ) { //sync rate
          main_bucket.setRate( max_rate.getRateLimitBytesPerSecond() );
        }
        return main_bucket.getAvailableByteCount();
      }
      
      public void bytesProcessed( int num_bytes_written ) {
        main_bucket.setBytesUsed( num_bytes_written );
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

    try {  connections_mon.enter();
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
      conn_data.state = ConnectionData.STATE_NORMAL;

      connections.put( connection, conn_data );
    }
    finally {  connections_mon.exit();  }
    
    main_controller.registerPeerConnection( connection );
  }
  
  
  
  /**
   * Cancel upload handling for the given peer connection.
   * @param connection to cancel
   */
  public void deregisterPeerConnection( NetworkConnection connection ) {
    try{ connections_mon.enter();
      ConnectionData conn_data = (ConnectionData)connections.remove( connection );
      
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
    finally{ connections_mon.exit(); }
    

    main_controller.cancelPeerConnection( connection );
  }
  
  
  

  /**
   * Upgrade the given connection to a high-speed transfer handler.
   * @param connection to upgrade
   */
  public void upgradePeerConnection( NetworkConnection connection ) {
    ConnectionData connection_data = null;
    
    try{ connections_mon.enter();
      connection_data = (ConnectionData)connections.get( connection );
    }
    finally{ connections_mon.exit(); }
    
    if( connection_data != null && connection_data.state == ConnectionData.STATE_NORMAL ) {
      final ConnectionData conn_data = connection_data;
      
      main_controller.upgradePeerConnection( connection, new RateHandler() {
        public int getCurrentNumBytesAllowed() {          
          // sync global rate
          if( main_bucket.getRate() != max_rate.getRateLimitBytesPerSecond() ) {
            main_bucket.setRate( max_rate.getRateLimitBytesPerSecond() );
          }
          // sync group rate
          int group_rate = NetworkManagerUtilities.getGroupRateLimit( conn_data.group );
          if( conn_data.group_data.bucket.getRate() != group_rate ) {
            conn_data.group_data.bucket.setRate( group_rate );
          }

          int group_allowed = conn_data.group_data.bucket.getAvailableByteCount();
          int global_allowed = main_bucket.getAvailableByteCount();

          // reserve bandwidth for the general pool
          global_allowed -= NetworkManager.getTcpMssSize();
          if( global_allowed < 0 ) global_allowed = 0;
          
          int allowed = group_allowed > global_allowed ? global_allowed : group_allowed;
          return allowed;
        }

        public void bytesProcessed( int num_bytes_written ) {
          conn_data.group_data.bucket.setBytesUsed( num_bytes_written );
          main_bucket.setBytesUsed( num_bytes_written );
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
    
    try{ connections_mon.enter();
      conn_data = (ConnectionData)connections.get( connection );
    }
    finally{ connections_mon.exit(); }
    
    if( conn_data != null && conn_data.state == ConnectionData.STATE_UPGRADED ) {
      main_controller.downgradePeerConnection( connection );
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
