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

package com.aelitis.azureus.core.peermanager;

import java.util.*;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.util.AEMonitor;

import com.aelitis.azureus.core.networkmanager.*;
import com.aelitis.azureus.core.peermanager.messages.ProtocolMessage;
import com.aelitis.azureus.core.peermanager.messages.bittorrent.BTProtocolMessage;


/**
 *
 */
public class UploadManager {
  private static final int UNLIMITED_WRITE_RATE = 1024 * 1024 * 100; //100 mbyte/s
  
  private static final UploadManager instance = new UploadManager();
  
  private int standard_max_rate_bps;
  private final ByteBucket standard_bucket;
  private final HashMap standard_peer_connections = new HashMap();
  private final AEMonitor standard_peer_connections_mon = new AEMonitor( "UploadManager:SPC" );
  private final UploadEntityController standard_entity_controller;
  
  private final HashMap group_buckets = new HashMap();
  private final AEMonitor group_buckets_mon = new AEMonitor( "UploadManager:GB" );
  
  
  private UploadManager() {
    int max_rateKBs = COConfigurationManager.getIntParameter( "Max Upload Speed KBs" );
    standard_max_rate_bps = max_rateKBs == 0 ? UNLIMITED_WRITE_RATE : max_rateKBs * 1024;
    COConfigurationManager.addParameterListener( "Max Upload Speed KBs", new ParameterListener() {
      public void parameterChanged( String parameterName ) {
        int rateKBs = COConfigurationManager.getIntParameter( "Max Upload Speed KBs" );
        standard_max_rate_bps = rateKBs == 0 ? UNLIMITED_WRITE_RATE : rateKBs * 1024;
      }
    });
    
    standard_bucket = new ByteBucket( standard_max_rate_bps, standard_max_rate_bps );  //no burst
    
    standard_entity_controller = new UploadEntityController( new RateHandler() {
      public int getCurrentNumBytesAllowed() {
        if( standard_bucket.getRate() != standard_max_rate_bps ) { //sync rate
          standard_bucket.setRate( standard_max_rate_bps, standard_max_rate_bps );  //no burst
        }
        return standard_bucket.getAvailableByteCount();
      }
      
      public void bytesWritten( int num_bytes_written ) {
        standard_bucket.setBytesUsed( num_bytes_written );
      }
    });
  }
  
  
  /**
   * Get the singleton instance of the upload manager.
   * @return the upload manager
   */
  public static UploadManager getSingleton() {  return instance;  }
  


  
  public void registerStandardPeerConnection( final Connection connection, final LimitedRateGroup group ) {
    final ConnectionData conn_data = new ConnectionData();
    
    OutgoingMessageQueue.MessageQueueListener listener = new OutgoingMessageQueue.MessageQueueListener() {
      public void messageAdded( ProtocolMessage message ) {
        if( message.getType() == BTProtocolMessage.BT_PIECE ) {  //is sending piece data
          if( conn_data.state == ConnectionData.STATE_NORMAL ) {  //so upgrade it
            conn_data.state = ConnectionData.STATE_UPGRADED;

            standard_entity_controller.upgradePeerConnection( connection, new RateHandler() {
              public int getCurrentNumBytesAllowed() {
                ByteBucket group_bucket;
                try {  group_buckets_mon.enter(); 
                  group_bucket = ((GroupData)group_buckets.get( group )).bucket;
                }
                finally {  group_buckets_mon.exit();  }
                
                //sync global rate
                if( standard_bucket.getRate() != standard_max_rate_bps ) { 
                  standard_bucket.setRate( standard_max_rate_bps, standard_max_rate_bps );
                }
                //sync group rate
                int group_rate = getTranslatedLimit( group );
                if( group_bucket.getRate() != group_rate ) {
                  group_bucket.setRate( group_rate, group_rate );
                }
                
                int group_allowed = group_bucket.getAvailableByteCount();
                int global_allowed = standard_bucket.getAvailableByteCount();
                
                //reserve bandwidth for the general pool if needed
                if( standard_entity_controller.isGeneralPoolWriteNeeded() ) {
                  int mss = NetworkManager.getSingleton().getTcpMssSize();
                  global_allowed -= mss * 2;
                  if( global_allowed < 0 )  global_allowed = 0;
                }
                
                int allowed = group_allowed > global_allowed ? global_allowed : group_allowed;
                return allowed;
              }

              public void bytesWritten( int num_bytes_written ) {
                ByteBucket group_bucket;
                try {  group_buckets_mon.enter(); 
                  group_bucket = ((GroupData)group_buckets.get( group )).bucket;
                }
                finally {  group_buckets_mon.exit();  }
                
                group_bucket.setBytesUsed( num_bytes_written );
                standard_bucket.setBytesUsed( num_bytes_written );
              }
            });
          }
        }
      }

      public void messageSent( ProtocolMessage message ) {
        if( message.getType() == BTProtocolMessage.BT_CHOKE ) {  //is done sending piece data
          if( conn_data.state == ConnectionData.STATE_UPGRADED ) {  //so downgrade it
            standard_entity_controller.downgradePeerConnection( connection );
            conn_data.state = ConnectionData.STATE_NORMAL;
          }
        }
      }

      public void messageRemoved( ProtocolMessage message ) {/*nothing*/}
      
      public void bytesSent( int byte_count ) {/*nothing*/}
    };
    
    conn_data.queue_listener = listener;
    conn_data.state = ConnectionData.STATE_NORMAL;
    conn_data.group = group;
    
    //do group registration
    try {  group_buckets_mon.enter(); 
      GroupData group_data = (GroupData)group_buckets.get( group );
      if( group_data == null ) {
        int limit = getTranslatedLimit( group );
        group_data = new GroupData( new ByteBucket( limit, limit ) );
        group_buckets.put( group, group_data );
      }
      group_data.group_size++;
    }
    finally {  group_buckets_mon.exit();  }
    
    try{ standard_peer_connections_mon.enter();
      standard_peer_connections.put( connection, conn_data );
    }
    finally{ standard_peer_connections_mon.exit(); }
    
    standard_entity_controller.registerPeerConnection( connection );
    connection.getOutgoingMessageQueue().registerQueueListener( listener );
  }
  
  
  public void cancelStandardPeerConnection( Connection connection ) {
    ConnectionData conn_data = null;
    
    try{ standard_peer_connections_mon.enter();
      conn_data = (ConnectionData)standard_peer_connections.remove( connection );
    }
    finally{ standard_peer_connections_mon.exit(); }
    
    if( conn_data != null ) {
      connection.getOutgoingMessageQueue().cancelQueueListener( conn_data.queue_listener );
      
      //do group de-registration
      try {  group_buckets_mon.enter(); 
        GroupData group_data = (GroupData)group_buckets.get( conn_data.group );
        if( group_data.group_size == 1 ) { //last of the group
          group_buckets.remove( conn_data.group ); //so remove
        }
        else {
          group_data.group_size--;
        }
      }
      finally {  group_buckets_mon.exit();  }
    }
    
    standard_entity_controller.cancelPeerConnection( connection );
  }
  
  
  private int getTranslatedLimit( LimitedRateGroup group ) {
    int limit = group.getRateLimitBytesPerSecond();
    if( limit == 0 ) {  //unlimited
      limit = UNLIMITED_WRITE_RATE;
    }
    else if( limit < 0 ) {  //disabled
      limit = 0;
    }
    return limit;
  }
  
  
  
  private static class ConnectionData {
    private static final int STATE_NORMAL   = 0;
    private static final int STATE_UPGRADED = 1;
    
    private OutgoingMessageQueue.MessageQueueListener queue_listener;
    private int state;
    private LimitedRateGroup group;
  }

    
  private static class GroupData {
    private final ByteBucket bucket;
    private int group_size = 0;
    private GroupData( ByteBucket bucket ) {
      this.bucket = bucket;
    }
  }
  
}
