/*
 * Created on Jan 9, 2005
 * Created by Alon Rohter
 * Copyright (C) 2004-2005 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.peermanager.messaging.core;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.peermanager.messaging.PeerMessage;


/**
 *
 */
public class Handshake implements PeerMessage {
  private static final String message_id = "HANDSHAKE";
  private static final int msg_version = 1;
  
  private final DirectByteBuffer buffer;
  private final int total_byte_size;
  
  
  //note that given payload buffer is copied, and NOT returned to pool here
  public Handshake( DirectByteBuffer message_list_payload ) {
    message_list_payload.position( DirectByteBuffer.SS_MSG, 0 );  //since it can be reused
    int size = message_list_payload.remaining( DirectByteBuffer.SS_MSG );
    buffer = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_MSG, size + 5 );
    
    buffer.putInt( DirectByteBuffer.SS_MSG, size + 1 );
    buffer.put( DirectByteBuffer.SS_MSG, (byte)20 );
    buffer.put( DirectByteBuffer.SS_MSG, message_list_payload );
    buffer.position( DirectByteBuffer.SS_MSG, 0 );
    buffer.limit( DirectByteBuffer.SS_MSG, size + 5 );
    
    total_byte_size = buffer.limit( DirectByteBuffer.SS_MSG );
  }
  
  
  public String getMessageID() {  return message_id;  }
  
  public int getVersion() {  return msg_version;  }
}
