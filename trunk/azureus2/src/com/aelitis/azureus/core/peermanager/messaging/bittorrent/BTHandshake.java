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

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.RawMessage;
import com.aelitis.azureus.core.peermanager.utils.PeerClassifier;


/**
 * BitTorrent handshake message.
 */
public class BTHandshake implements BTProtocolMessage, RawMessage {
  public static final String PROTOCOL = "BitTorrent protocol";
  public static final byte[] RESERVED = new byte[]{ (byte)128, 0, 0, 0, 0, 0, 0, 0 };  //set high bit of first byte
  
  
  private final DirectByteBuffer[] buffer;
  private final String description;
  
  
  public BTHandshake( byte[] data_hash, byte[] peer_id ) {
    DirectByteBuffer dbb = new DirectByteBuffer( ByteBuffer.allocate( 68 ) );
    dbb.put( DirectByteBuffer.SS_BT, (byte)PROTOCOL.length() );
    dbb.put( DirectByteBuffer.SS_BT, PROTOCOL.getBytes() );
    dbb.put( DirectByteBuffer.SS_BT, RESERVED );
    dbb.put( DirectByteBuffer.SS_BT, data_hash );
    dbb.put( DirectByteBuffer.SS_BT, peer_id );
    dbb.flip( DirectByteBuffer.SS_BT );
    buffer = new DirectByteBuffer[] { dbb };

    description = BTProtocolMessage.ID_BT_HANDSHAKE +
                  " of dataID: " +ByteFormatter.nicePrint( data_hash, true ) +
                  " peerID: " +PeerClassifier.getPrintablePeerID( peer_id );
    
    /* for( int i=7; i >= 0; i-- ) {
         byte b = (byte) (RESERVED[0] >> i);
         int val = b & 0x01;
         System.out.print( val == 1 ? "x" : "." );
       }
       System.out.println();  */
  }
  


  // message
  public String getID() {  return BTProtocolMessage.ID_BT_HANDSHAKE;  }
  
  public byte getVersion() {  return BTProtocolMessage.BT_DEFAULT_VERSION;  }
    
  public String getDescription() {  return description;  }
  
  public DirectByteBuffer[] getData() {  return buffer;  }

  
  
  // raw message
  public DirectByteBuffer[] getRawPayload() {  return buffer;  }
  
  public int getPriority() {  return RawMessage.PRIORITY_HIGH;  }

  public boolean isNoDelay() {  return true;  }

  public boolean isDataMessage() {  return false;  }
 
  public Message[] messagesToRemove() {  return null;  }

  public void destroy() {  }
  
}
