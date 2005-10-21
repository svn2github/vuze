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

package com.aelitis.azureus.core.networkmanager.impl;

import org.gudy.azureus2.core3.util.AEDiagnostics;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.NetworkManager;


/**
 * A fast write entity backed by a single peer connection.
 */
public class SinglePeerUploader implements RateControlledEntity {
  
  private final NetworkConnection connection;
  private final RateHandler rate_handler;
  
  public SinglePeerUploader( NetworkConnection connection, RateHandler rate_handler ) {
    this.connection = connection;
    this.rate_handler = rate_handler;
  }
  
  
////////////////RateControlledWriteEntity implementation ////////////////////
  
  public boolean canProcess() {
    if( !connection.getTCPTransport().isReadyForWrite() )  {
      return false;  //underlying transport not ready
    }
    if( connection.getOutgoingMessageQueue().getTotalSize() < 1 ) {
      return false;  //no data to send
    }
    if( rate_handler.getCurrentNumBytesAllowed() < 1 ) {
      return false;  //not allowed to send any bytes
    }
    return true;
  }
  
  public boolean doProcessing() {
    if( !connection.getTCPTransport().isReadyForWrite() )  {
      Debug.out("dW:not ready");
      return false;
    }
    
    int num_bytes_allowed = rate_handler.getCurrentNumBytesAllowed();
    if( num_bytes_allowed < 1 )  {
      return false;
    }
    
    int num_bytes_available = connection.getOutgoingMessageQueue().getTotalSize();
    if( num_bytes_available < 1 ) {
      Debug.out("dW:not avail");
      return false;
    }
    
    int num_bytes_to_write = num_bytes_allowed > num_bytes_available ? num_bytes_available : num_bytes_allowed;
    
    //int mss = NetworkManager.getTcpMssSize();
    //if( num_bytes_to_write > mss )  num_bytes_to_write = mss;
    
    int written = 0;
    try {
      written = connection.getOutgoingMessageQueue().deliverToTransport( num_bytes_to_write, false );
    }
    catch( Throwable e ) {
      
      if( AEDiagnostics.TRACE_CONNECTION_DROPS ) {
        if( e.getMessage() == null ) {
          Debug.out( "null write exception message: ", e );
        }
        else {
          if( e.getMessage().indexOf( "An existing connection was forcibly closed by the remote host" ) == -1 &&
              e.getMessage().indexOf( "Connection reset by peer" ) == -1 &&
              e.getMessage().indexOf( "Broken pipe" ) == -1 &&
              e.getMessage().indexOf( "An established connection was aborted by the software in your host machine" ) == -1 ) {
            
            System.out.println( "SP: write exception [" +connection.getTCPTransport().getDescription()+ "]: " +e.getMessage() );
          }
        }
      }

      connection.notifyOfException( e );
      return false;
    }
    
    if( written < 1 )  {
      return false;
    }
    
    rate_handler.bytesProcessed( written );
    return true;
  }
  
  public int getPriority() {
    return RateControlledEntity.PRIORITY_NORMAL;
  }

/////////////////////////////////////////////////////////////////////////////
  
}
