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

package com.aelitis.azureus.core.peermanager.uploadmanager;

import org.gudy.azureus2.core3.config.*;

import com.aelitis.azureus.core.networkmanager.*;


/**
 *
 */
public class UploadManager {
  private static final int UNLIMITED_WRITE_RATE = 1024 * 1024 * 100; //100 mbyte/s
  private int max_write_rate_bytes_per_sec;
  
  private static final UploadManager instance = new UploadManager();
  
  private final ByteBucket global_bytebucket;
  
  private final PacketFillingMultiPeerUploader.RateHandler standard_uploader_handler = new PacketFillingMultiPeerUploader.RateHandler() {
    public int getCurrentNumBytesAllowed() {
      return global_bytebucket.getAvailableByteCount();
    }

    public void bytesWritten( int num_bytes_written ) {
      global_bytebucket.setBytesUsed( num_bytes_written );
    }
  };
  private final PacketFillingMultiPeerUploader standard_uploader = new PacketFillingMultiPeerUploader( standard_uploader_handler );
  
  
  
  
  private UploadManager() {
    int norm_rateKBs = COConfigurationManager.getIntParameter( "Max Upload Speed KBs" );
    max_write_rate_bytes_per_sec = norm_rateKBs == 0 ? UNLIMITED_WRITE_RATE : norm_rateKBs * 1024;
    global_bytebucket = new ByteBucket( max_write_rate_bytes_per_sec );
    global_bytebucket.setRate( max_write_rate_bytes_per_sec, max_write_rate_bytes_per_sec );  //no burst
    COConfigurationManager.addParameterListener( "Max Upload Speed KBs", new ParameterListener() {
      public void parameterChanged( String parameterName ) {
        int rateKBs = COConfigurationManager.getIntParameter( "Max Upload Speed KBs" );
        max_write_rate_bytes_per_sec = rateKBs == 0 ? UNLIMITED_WRITE_RATE : rateKBs * 1024;
        global_bytebucket.setRate( max_write_rate_bytes_per_sec, max_write_rate_bytes_per_sec );
      }
    });
    
    NetworkManager.getSingleton().addWriteEntity( standard_uploader );
  }
  

  
  /**
   * Get the singleton instance of the peer upload manager.
   * @return upload manager
   */
  public static UploadManager getSingleton() {  return instance;  }
  
  
  /**
   * Register a peer connection for upload management.
   * @param connection to add
   */
  public void registerStandardPeerConnection( Connection connection ) {
    standard_uploader.addPeerConnection( connection );
  }
  
  /**
   * Remove a peer connection from upload management.
   * @param connection to cancel
   */
  public void cancelStandardPeerConnection( Connection connection ) {
    standard_uploader.removePeerConnection( connection );
  }

}
