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

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.peermanager.messages.ProtocolMessage;

/**
 * BitTorrent handshake message.
 */
public class BTHandshake implements BTProtocolMessage {
  public static final String PROTOCOL = "BitTorrent protocol";
  public static final byte[] RESERVED = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 };
  
  private final DirectByteBuffer buffer;
  private final byte[] data_hash;
  private final byte[] peer_id;
  private final int total_byte_size;
  
  public BTHandshake( byte[] data_hash, byte[] peer_id ) {
    this.data_hash = data_hash;
    this.peer_id = peer_id;
    buffer = DirectByteBufferPool.getBuffer( 68 );
    
    buffer.put( (byte)PROTOCOL.length() );
    buffer.put( PROTOCOL.getBytes() );
    buffer.put( RESERVED );
    buffer.put( data_hash );
    buffer.put( peer_id );
    buffer.position( 0 );
    buffer.limit( 68 );
    
    total_byte_size = buffer.limit();
  }
  
  public int getType() {  return BTProtocolMessage.BT_HANDSHAKE;  }
  
  public DirectByteBuffer getPayload() {  return buffer;  }
  
  public int getTotalMessageByteSize() {  return total_byte_size;  }
  
  public String getDescription() {
    return "Handshake of DataID: " +ByteFormatter.nicePrint( data_hash, true )
                     + " PeerID: " +Identification.getPrintablePeerID( peer_id );
  }
  
  public int getPriority() {  return ProtocolMessage.PRIORITY_HIGH;  }
  
  public boolean isNoDelay() {  return true;  }
  
  public void destroy() {
    buffer.returnToPool();
  }
  
  public int[] typesToRemove() {  return null;  }
}
