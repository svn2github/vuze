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

package com.aelitis.azureus.core.peermanager.messages.bittorrent;

import java.nio.ByteBuffer;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.peermanager.messages.ProtocolMessage;

/**
 * BitTorrent cancel message.
 */
public class BTCancel implements BTProtocolMessage {
  
  private final DirectByteBuffer buffer;
  private final int piece_number;
  private final int piece_offset;
  private final int length;
  private final int total_byte_size;
  
  public BTCancel( int piece_number, int piece_offset, int length ) {
    this.piece_number = piece_number;
    this.piece_offset = piece_offset;
    this.length = length;
    buffer = new DirectByteBuffer( ByteBuffer.allocate( 17 ) );
    
    buffer.putInt( DirectByteBuffer.SS_BT, 13 );
    buffer.put( DirectByteBuffer.SS_BT, (byte)8 );
    buffer.putInt( DirectByteBuffer.SS_BT, piece_number );
    buffer.putInt( DirectByteBuffer.SS_BT, piece_offset );
    buffer.putInt( DirectByteBuffer.SS_BT, length );
    buffer.position( DirectByteBuffer.SS_BT, 0 );
    buffer.limit( DirectByteBuffer.SS_BT, 17 );
    
    total_byte_size = buffer.limit(DirectByteBuffer.SS_BT);
  }
  
  public int getType() {  return BTProtocolMessage.BT_CANCEL;  }
  
  public DirectByteBuffer getPayload() {  return buffer;  }
  
  public int getTotalMessageByteSize() {  return total_byte_size;  }
  
  public String getDescription() {
    return "Cancel piece #" + piece_number + ": " + piece_offset + "->" + (piece_offset + length -1);
  }
  
  public int getPriority() {  return ProtocolMessage.PRIORITY_HIGH;  }
  
  public boolean isNoDelay() {  return true;  }
  
  public boolean isDataMessage() {  return false;  }
    
  public void destroy() { }
  
  public int[] typesToRemove() {  return null;  }
}
