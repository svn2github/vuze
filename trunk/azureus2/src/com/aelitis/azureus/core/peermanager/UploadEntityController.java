/*
 * Created on Sep 23, 2004
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

import org.gudy.azureus2.core3.util.AEMonitor;

import com.aelitis.azureus.core.networkmanager.*;


/**
 * Manages upload (write) entities on behalf of peer connections.
 * Each upload entity controller has a global upload pool which
 * manages all connections by default.  Connections can also be
 * "upgraded" to a higher connection upload control level, i.e.
 * each connection has its own specialized entity.
 */
public class UploadEntityController {
  private final RateController rate_controller;
  private final ByteBucket global_bytebucket;
  private final HashMap upgraded_connections = new HashMap();
  private final AEMonitor upgraded_connections_mon = new AEMonitor( "UploadEntityController:UC" );
  
  private final PacketFillingMultiPeerUploader global_uploader = new PacketFillingMultiPeerUploader( new RateHandler() {
    public int getCurrentNumBytesAllowed() {
      syncBucketRate( global_bytebucket, rate_controller, false );
      return global_bytebucket.getAvailableByteCount();
    }

    public void bytesWritten( int num_bytes_written ) {
      global_bytebucket.setBytesUsed( num_bytes_written );
    }
  });
  
  
  
  /**
   * Create a new upload entity manager using the given rate handler.
   * @param rate_controller write rate handler
   */
  protected UploadEntityController( RateController rate_controller ) {
    this.rate_controller = rate_controller;
    int rate = rate_controller.getAllowedBytesPerSecondRate();
    global_bytebucket = new ByteBucket( rate, rate );  //no burst
    
    NetworkManager.getSingleton().addWriteEntity( global_uploader );  //register upload entity
  }
  
  
  private void syncBucketRate( ByteBucket bucket, RateController controller, boolean allow_burst ) {
    int current_rate = controller.getAllowedBytesPerSecondRate();
    if( bucket.getRate() != current_rate ) { //the allowed rate has changed
      if( allow_burst )  bucket.setRate( current_rate );  //so update it
      else bucket.setRate( current_rate, current_rate );
    }
  }
  
  
  
  /**
   * Register a peer connection for upload management by the controller.
   * @param connection to add to the global pool
   */
  protected void registerPeerConnection( Connection connection ) {
    global_uploader.addPeerConnection( connection );
  }
  
  
  /**
   * Remove a peer connection from the upload entity controller.
   * @param connection to cancel
   */
  protected void cancelPeerConnection( Connection connection ) {
    if( !global_uploader.removePeerConnection( connection ) ) {  //if not found in the pool entity
      
      BurstingSinglePeerUploader upload_entity = null;
      try {
        upgraded_connections_mon.enter();
        
        upload_entity = (BurstingSinglePeerUploader)upgraded_connections.remove( connection );  //check for it in the upgraded list
      }
      finally {
        upgraded_connections_mon.exit();
      }
      
      if( upload_entity != null ) {
        NetworkManager.getSingleton().removeWriteEntity( upload_entity );  //cancel from write processing
      }
    }
  }
  
  
  /**
   * Upgrade a peer connection from the general pool to its own upload entity.
   * NOTE: The rate controller allows for fine-tuning of the connection's upload
   * speed, but it is still subject to the global rate limit of this controller.
   * @param connection to upgrade from global management
   * @param controller write rate handler
   */
  protected void upgradePeerConnection( Connection connection, final RateController controller ) {
    global_uploader.removePeerConnection( connection );  //remove it from the general upload pool

    int global_rate = rate_controller.getAllowedBytesPerSecondRate();
    int local_rate = controller.getAllowedBytesPerSecondRate();
    int allowed_rate = local_rate > global_rate ? global_rate : local_rate;
    final ByteBucket bucket = new ByteBucket( allowed_rate );
    
    BurstingSinglePeerUploader upload_entity = new BurstingSinglePeerUploader( connection, new RateHandler() {
      public int getCurrentNumBytesAllowed() {
        syncBucketRate( global_bytebucket, rate_controller, false );  //sync global
        syncBucketRate( bucket, controller, true );  //sync local
        
        int global_avail = global_bytebucket.getAvailableByteCount();
        int local_avail = bucket.getAvailableByteCount();
        
        int allowed_bytes = local_avail < global_avail ? local_avail : global_avail;
        return allowed_bytes;
      }

      public void bytesWritten( int num_bytes_written ) {
        bucket.setBytesUsed( num_bytes_written );
        global_bytebucket.setBytesUsed( num_bytes_written );
      }
    });
        
    try {
      upgraded_connections_mon.enter();
      
      upgraded_connections.put( connection, upload_entity );  //add it to the upgraded list
    }
    finally {
      upgraded_connections_mon.exit();
    }
    
    NetworkManager.getSingleton().addWriteEntity( upload_entity );  //register it for write processing
    
    //System.out.println( "upgraded: " + upgraded_connections.size());
  }
  
  
  /**
   * Downgrade (return) a peer connection back into the general pool.
   * @param connection to downgrade back into the global entity
   */
  protected void downgradePeerConnection( Connection connection ) {
    BurstingSinglePeerUploader upload_entity = null;
    try {
      upgraded_connections_mon.enter();
      
      upload_entity = (BurstingSinglePeerUploader)upgraded_connections.remove( connection );  //remove from the upgraded list
    }
    finally {
      upgraded_connections_mon.exit();
    }
    
    if( upload_entity != null ) {
      NetworkManager.getSingleton().removeWriteEntity( upload_entity );  //cancel from write processing
    }
    global_uploader.addPeerConnection( connection );  //move back to the general pool
    
    //System.out.println( "downgraded: " + upgraded_connections.size());
  }
  

  
  /**
   * Rate control listener.
   */
  public interface RateController {
    /**
     * Get the curent allowed BPS upload rate. 
     * @return rate in bytes per second
     */
    public int getAllowedBytesPerSecondRate();
  }

}
