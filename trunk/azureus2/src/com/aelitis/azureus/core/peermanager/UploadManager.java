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
  
  private int max_write_rate_bytes_per_sec;
  
  private final HashMap standard_peer_connections = new HashMap();
  private final AEMonitor standard_peer_connections_mon = new AEMonitor( "UploadManager:SPC" );
  
  private final UploadEntityController main_entity_controller = new UploadEntityController(
      new UploadEntityController.RateController() {
        public int getAllowedBytesPerSecondRate() {
          return max_write_rate_bytes_per_sec;
        }
      }
  );
    
  
  
  
  private UploadManager() {
    int norm_rateKBs = COConfigurationManager.getIntParameter( "Max Upload Speed KBs" );
    max_write_rate_bytes_per_sec = norm_rateKBs == 0 ? UNLIMITED_WRITE_RATE : norm_rateKBs * 1024;
    COConfigurationManager.addParameterListener( "Max Upload Speed KBs", new ParameterListener() {
      public void parameterChanged( String parameterName ) {
        int rateKBs = COConfigurationManager.getIntParameter( "Max Upload Speed KBs" );
        max_write_rate_bytes_per_sec = rateKBs == 0 ? UNLIMITED_WRITE_RATE : rateKBs * 1024;
      }
    });
    
  }
  
  
  /**
   * Get the singleton instance of the upload manager.
   * @return the upload manager
   */
  public static UploadManager getSingleton() {  return instance;  }
  
  
  
  public void registerStandardPeerConnection( final Connection connection ) {
    final ConnectionData data = new ConnectionData();
    
    OutgoingMessageQueue.MessageQueueListener listener = new OutgoingMessageQueue.MessageQueueListener() {
      public void messageAdded( ProtocolMessage message ) {
        if( message.getType() == BTProtocolMessage.BT_PIECE ) {
          if( data.state == ConnectionData.STATE_NORMAL ) {  //is sending piece data, so upgrade it
            data.state = ConnectionData.STATE_UPGRADED;
            main_entity_controller.upgradePeerConnection( connection, new UploadEntityController.RateController() {
              public int getAllowedBytesPerSecondRate() {
                //return UNLIMITED_WRITE_RATE;  //TODO hook in per-torrent values etc
                return 4 * 1024;
              }
            });
          }
        }
      }

      public void messageSent( ProtocolMessage message ) {
        if( message.getType() == BTProtocolMessage.BT_CHOKE ) {
          if( data.state == ConnectionData.STATE_UPGRADED ) {  //is done sending piece data, so downgrade it
            main_entity_controller.downgradePeerConnection( connection );
            data.state = ConnectionData.STATE_NORMAL;
          }
        }
      }

      public void messageRemoved( ProtocolMessage message ) {/*nothing*/}
      
      public void bytesSent( int byte_count ) {
        //TODO ?
      }
    };
    
    data.queue_listener = listener;
    data.state = ConnectionData.STATE_NORMAL;
    
    try{ standard_peer_connections_mon.enter();
      standard_peer_connections.put( connection, data );
    }
    finally{ standard_peer_connections_mon.exit(); }
    
    main_entity_controller.registerPeerConnection( connection );
    connection.getOutgoingMessageQueue().registerQueueListener( listener );
  }
  
  
  public void cancelStandardPeerConnection( Connection connection ) {
    ConnectionData data = null;
    
    try{ standard_peer_connections_mon.enter();
      data = (ConnectionData)standard_peer_connections.remove( connection );
    }
    finally{ standard_peer_connections_mon.exit(); }
    
    if( data != null ) {
      connection.getOutgoingMessageQueue().cancelQueueListener( data.queue_listener );
    }
    
    main_entity_controller.cancelPeerConnection( connection );
  }
  
  
  
  private static class ConnectionData {
    private static final int STATE_NORMAL   = 0;
    private static final int STATE_UPGRADED = 1;
    
    private OutgoingMessageQueue.MessageQueueListener queue_listener;
    private int state;
  }
  
  
  
}
