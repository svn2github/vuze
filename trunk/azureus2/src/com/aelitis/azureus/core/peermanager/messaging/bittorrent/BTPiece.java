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

import java.nio.ByteBuffer;

import org.gudy.azureus2.core3.util.*;


/**
 * BitTorrent piece message.
 */
public class BTPiece implements BTProtocolMessage {
  private final DirectByteBuffer[] buffer;
  private final String description;
  
  
  public BTPiece( int piece_number, int piece_offset, DirectByteBuffer data ) {
    DirectByteBuffer header = new DirectByteBuffer( ByteBuffer.allocate( 8 ) );
    header.putInt( DirectByteBuffer.SS_BT, piece_number );
    header.putInt( DirectByteBuffer.SS_BT, piece_offset );
    header.flip( DirectByteBuffer.SS_BT );
    
    buffer = new DirectByteBuffer[] { header, data };
    
    int length = data.remaining( DirectByteBuffer.SS_BT );
    description = BTProtocolMessage.ID_BT_PIECE + " data for #" + piece_number + ": " + piece_offset + "->" + (piece_offset + length -1);
  }
  
  
  
  /**
   * Used for creating a lightweight message-type comparison message.
   */
  public BTPiece() {
    buffer = null;
    description = null;
  }
  

  public String getID() {  return BTProtocolMessage.ID_BT_PIECE;  }
  
  public byte getVersion() {  return BTProtocolMessage.BT_DEFAULT_VERSION;  }
    
  public String getDescription() {  return description;  }
  
  public DirectByteBuffer[] getData() {  return buffer;  }
  

}
