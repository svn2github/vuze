/*
 * Created on Apr 30, 2004
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

package com.aelitis.azureus.core.peermanager.messaging.bittorrent;


import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;


/**
 * BitTorrent piece message.
 */
public class BTPiece implements BTMessage {
  private final DirectByteBuffer[] buffer;
  private final String description;
  
  private final int piece_number;
  private final int piece_offset;
  
  
  public BTPiece( int piece_number, int piece_offset, DirectByteBuffer data ) {
    DirectByteBuffer header = DirectByteBufferPool.getBuffer( DirectByteBuffer.SS_MSG, 8 );
    header.putInt( DirectByteBuffer.SS_BT, piece_number );
    header.putInt( DirectByteBuffer.SS_BT, piece_offset );
    header.flip( DirectByteBuffer.SS_BT );
    
    buffer = new DirectByteBuffer[] { header, data };
    
    int length = data.remaining( DirectByteBuffer.SS_BT );
    description = BTMessage.ID_BT_PIECE + " data for #" + piece_number + ": " + piece_offset + "->" + (piece_offset + length -1);
    this.piece_number = piece_number;
    this.piece_offset = piece_offset;
  }
  
  
  
  /**
   * Used for creating a lightweight message-type comparison message.
   */
  public BTPiece() {
    buffer = null;
    description = null;
    piece_number = -1;
    piece_offset = -1;
  }
  
  
  public int getPieceNumber() {  return piece_number;  }
  
  public int getPieceOffset() {  return piece_offset;  }
  
  public DirectByteBuffer getPieceData() {  return buffer[1];  }
  
  

  public String getID() {  return BTMessage.ID_BT_PIECE;  }
  
  public byte getVersion() {  return BTMessage.BT_DEFAULT_VERSION;  }
  
  public int getType() {  return Message.TYPE_DATA_PAYLOAD;  }
    
  public String getDescription() {  return description;  }
  
  public DirectByteBuffer[] getData() {  return buffer;  }
  
  public Message deserialize( DirectByteBuffer data ) throws MessageException {    
    if( data == null ) {
      throw new MessageException( "[" +getID() + ":" +getVersion()+ "] decode error: data == null" );
    }
    
    if( data.remaining( DirectByteBuffer.SS_MSG ) < 8 ) {
      throw new MessageException( "[" +getID() + ":" +getVersion()+ "] decode error: payload.remaining[" +data.remaining( DirectByteBuffer.SS_MSG )+ "] < 8" );
    }
    
    int number = data.getInt( DirectByteBuffer.SS_MSG );
    if( number < 0 ) {
      throw new MessageException( "[" +getID() + ":" +getVersion()+ "] decode error: number < 0" );
    }
    
    int offset = data.getInt( DirectByteBuffer.SS_MSG );
    if( offset < 0 ) {
      throw new MessageException( "[" +getID() + ":" +getVersion()+ "] decode error: offset < 0" );
    }
    
    return new BTPiece( number, offset, data );
  }
  
  public void destroy() {
    if( buffer != null ) {
      buffer[0].returnToPool();
      buffer[1].returnToPool();
    }
  }
}
