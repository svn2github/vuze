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
 * BitTorrent have message.
 */
public class BTHave implements BTMessage {
  private DirectByteBuffer buffer = null;
  private String description = null;
  
  private final int piece_number;

  
  public BTHave( int piece_number ) {
    this.piece_number = piece_number;
  }
  
  
  public int getPieceNumber() {  return piece_number;  }
  

  public String getID() {  return BTMessage.ID_BT_HAVE;  }
  
  public byte getVersion() {  return BTMessage.BT_DEFAULT_VERSION;  }
  
  public int getType() {  return Message.TYPE_PROTOCOL_PAYLOAD;  }
    
  
  public String getDescription() {
    if( description == null ) {
      description = BTMessage.ID_BT_HAVE + " piece #" + piece_number;
    }
    
    return description;
  }
  
  
  public DirectByteBuffer[] getData() {
    if( buffer == null ) {
      buffer = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_MSG_BT_HAVE, 4 );
      buffer.putInt( DirectByteBuffer.SS_MSG, piece_number );
      buffer.flip( DirectByteBuffer.SS_MSG );
    }
    
    return new DirectByteBuffer[]{ buffer };
  }
  
  
  public Message deserialize( DirectByteBuffer data ) throws MessageException {    
    if( data == null ) {
      throw new MessageException( "[" +getID() + ":" +getVersion()+ "] decode error: data == null" );
    }
    
    if( data.remaining( DirectByteBuffer.SS_MSG ) != 4 ) {
      throw new MessageException( "[" +getID() + ":" +getVersion()+ "] decode error: payload.remaining[" +data.remaining( DirectByteBuffer.SS_MSG )+ "] != 4" );
    }
    
    int number = data.getInt( DirectByteBuffer.SS_MSG );
    
    if( number < 0 ) {
      throw new MessageException( "[" +getID() + ":" +getVersion()+ "] decode error: number < 0" );
    }
    
    data.returnToPool();
    
    return new BTHave( number );
  }

  
  public void destroy() {
    if( buffer != null )  buffer.returnToPool();
  }
  
}
