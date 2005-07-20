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

package com.aelitis.azureus.core.peermanager.messaging.azureus.session;


import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;
import com.aelitis.azureus.core.peermanager.messaging.azureus.AZMessage;




/**
 * Torrent session bitfield message.
 */
public class AZSessionBitfield implements AZMessage {
  private final DirectByteBuffer bitfield;
  private final int session_id;
  
  public AZSessionBitfield( int session_id, DirectByteBuffer bitfield ) {
    this.session_id = session_id;
    this.bitfield = bitfield;
  }
  
  
  public int getSessionID() {  return session_id; }
  public DirectByteBuffer getBitfield() {  return bitfield;  }
  
  

  public String getID() {  return AZMessage.ID_AZ_SESSION_BITFIELD;  }
  
  public byte getVersion() {  return AZMessage.AZ_DEFAULT_VERSION;  }
  
  public int getType() {  return Message.TYPE_PROTOCOL_PAYLOAD;  }
    
  public String getDescription() {  return getID() +" session #"+ session_id;  }
  
  public DirectByteBuffer[] getData() {
    DirectByteBuffer sess = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_MSG, 4 );
    sess.putInt( DirectByteBuffer.SS_MSG, session_id );
    sess.flip( DirectByteBuffer.SS_MSG );
    
    return new DirectByteBuffer[]{ sess, bitfield };
  }

  public Message deserialize( DirectByteBuffer data ) throws MessageException {    
    if( data == null ) {
      throw new MessageException( "[" +getID() + ":" +getVersion()+ "] decode error: data == null" );
    }
        
    if( data.remaining( DirectByteBuffer.SS_MSG ) < 4 ) {
      throw new MessageException( "[" +getID() + ":" +getVersion()+ "] decode error: payload.remaining[" +data.remaining( DirectByteBuffer.SS_MSG )+ "] < 4" );
    }
    
    int id = data.getInt( DirectByteBuffer.SS_MSG );
    
    return new AZSessionBitfield( id, data );
  }
  
  public void destroy() {
    if( bitfield != null )  bitfield.returnToPool();
  }

}
