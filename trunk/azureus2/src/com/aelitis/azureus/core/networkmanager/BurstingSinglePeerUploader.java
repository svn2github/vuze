/*
 * Created on Sep 28, 2004
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

package com.aelitis.azureus.core.networkmanager;

import java.io.IOException;



/**
 * A fast write entity backed by a single peer connection.
 */
public class BurstingSinglePeerUploader implements RateControlledWriteEntity {
  
  private final Connection connection;
  private final RateHandler rate_handler;
  
  public BurstingSinglePeerUploader( Connection connection, RateHandler rate_handler ) {
    this.connection = connection;
    this.rate_handler = rate_handler;
  }
  
  
////////////////RateControlledWriteEntity implementation ////////////////////
  
  public boolean canWrite() {
    if( connection.getOutgoingMessageQueue().getTotalSize() < 1 )  return false;  //no data to send
    if( rate_handler.getCurrentNumBytesAllowed() < 1 )  return false;  //not allowed to send any bytes
    return true;
  }
  
  public boolean doWrite() {
    if( !connection.getTransport().isReadyForWrite() )  return false;
    
    int num_bytes_allowed = rate_handler.getCurrentNumBytesAllowed();
    if( num_bytes_allowed < 1 )  return false;
    
    int num_bytes_available = connection.getOutgoingMessageQueue().getTotalSize();
    if( num_bytes_available < 1 )  return false;
    
    int num_bytes_to_write = Math.min( num_bytes_allowed, num_bytes_available );
    
    int written = 0;
    try {
      written = connection.getOutgoingMessageQueue().deliverToTransport( connection.getTransport(), num_bytes_to_write, false );
    }
    catch( IOException e ) {
      connection.notifyOfException( e );
    }
    
    if( written < 1 )  return false;
    
    rate_handler.bytesWritten( written );
    return true;
  }
  
  public int getPriority() {
    return RateControlledWriteEntity.PRIORITY_NORMAL;
  }

/////////////////////////////////////////////////////////////////////////////
  
}
