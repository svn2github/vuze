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

import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.networkmanager.*;


/**
 * Manages upload (write) entities on behalf of peer connections.
 * Each upload entity controller has a global upload pool which
 * manages all connections by default.  Connections can also be
 * "upgraded" to a higher connection upload control level, i.e.
 * each connection has its own specialized entity.
 */
public class UploadEntityController {
  private final HashMap upgraded_connections = new HashMap();
  private final PacketFillingMultiPeerUploader global_uploader;
  
  
  
  /**
   * Create a new upload entity manager using the given rate handler.
   * @param rate_handler global write rate handler
   */
  protected UploadEntityController( RateHandler rate_handler ) {
    global_uploader = new PacketFillingMultiPeerUploader( rate_handler );
    NetworkManager.getSingleton().addWriteEntity( global_uploader );  //register upload entity
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
      BurstingSinglePeerUploader upload_entity = (BurstingSinglePeerUploader)upgraded_connections.remove( connection );  //check for it in the upgraded list
      if( upload_entity != null ) {
        NetworkManager.getSingleton().removeWriteEntity( upload_entity );  //cancel from write processing
      }
      else {
        Debug.out( "upload_entity == null" );
      }
    }
  }
  
  
  /**
   * Upgrade a peer connection from the general pool to its own upload entity.
   * @param connection to upgrade from global management
   * @param handler connection write rate handler
   */
  protected void upgradePeerConnection( Connection connection, RateHandler handler ) {
    BurstingSinglePeerUploader upload_entity = new BurstingSinglePeerUploader( connection, handler );      
    upgraded_connections.put( connection, upload_entity );  //add it to the upgraded list

    if( !global_uploader.removePeerConnection( connection ) ) {  //remove it from the general upload pool
      Debug.out( "upgradePeerConnection:: connection not found/removed !" );
    }

    NetworkManager.getSingleton().addWriteEntity( upload_entity );  //register it for write processing
  }
  
  
  /**
   * Downgrade (return) a peer connection back into the general pool.
   * @param connection to downgrade back into the global entity
   */
  protected void downgradePeerConnection( Connection connection ) {
    BurstingSinglePeerUploader upload_entity;
    upload_entity = (BurstingSinglePeerUploader)upgraded_connections.remove( connection );  //remove from the upgraded list  

    if( upload_entity != null ) {
      NetworkManager.getSingleton().removeWriteEntity( upload_entity );  //cancel from write processing
    }
    else {
      Debug.out( "upload_entity == null" );
    }
    global_uploader.addPeerConnection( connection );  //move back to the general pool
  }

  
  /**
   * Is the general pool entity in need of a write op.
   * NOTE: Because the general pool is backed by a PacketFillingMultiPeerUploader
   * entity, it requires at least MSS available bytes before it will/can perform
   * a successful write.  This method allows higher-level bandwidth allocation to
   * determine if it should reserve the necessary MSS bytes for the general pool's
   * write needs.
   * @return true of it has data to send, false if not
   */
  protected boolean isGeneralPoolWriteNeeded() {  return global_uploader.hasWriteDataAvailable();  }
}
