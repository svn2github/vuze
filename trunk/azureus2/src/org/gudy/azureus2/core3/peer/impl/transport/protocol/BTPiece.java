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

package org.gudy.azureus2.core3.peer.impl.transport.protocol;

import java.nio.ByteBuffer;

import org.gudy.azureus2.core3.util.*;

/**
 * BitTorrent piece message.
 */
public class BTPiece implements BTMessage {
  
  private final DirectByteBuffer buffer;
  private final int piece_number;
  private final int piece_offset;
  private final int length;
  
  /**
   * NOTE: The passed data ByteBuffer is simply copied-from upon instantiation,
   * so it is safe for reuse.
   */
  public BTPiece( int piece_number, int piece_offset, ByteBuffer data ) {
    this.piece_number = piece_number;
    this.piece_offset = piece_offset;
    length = data.remaining();
    buffer = new DirectByteBuffer( ByteBuffer.allocate( length + 13 ) );
    
    buffer.buff.putInt( length + 9 );
    buffer.buff.put( (byte)7 );
    buffer.buff.putInt( piece_number );
    buffer.buff.putInt( piece_offset );
    buffer.buff.put( data );
    buffer.buff.position( 0 );
    buffer.buff.limit( length + 13 );
  }
  
  public int getType() {  return BTMessage.BT_PIECE;  }
  
  public DirectByteBuffer getPayload() {  return buffer;  }
  
  public String getDescription() {
    return "Piece data for #" + piece_number + ": " + piece_offset + "->" + (piece_offset + length);
  }
  
  
}
