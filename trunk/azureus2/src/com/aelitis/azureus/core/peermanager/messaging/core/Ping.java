/*
 * Created on Jan 8, 2005
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

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;


/**
 *
 */
public class Ping implements Message {
  private static final String msg_id = "PING";
  private static final byte msg_version = (byte)1;
  
  public String getID() {  return msg_id;  }

  public byte getVersion() {  return msg_version;  }
  
  public int getType() {  return Message.TYPE_PROTOCOL_PAYLOAD;  }

  public String getDescription() {  return msg_id;  }
  
  public DirectByteBuffer[] getData() {  return new DirectByteBuffer[]{};  } 
  
  public Message deserialize( String id, byte version, DirectByteBuffer data ) throws MessageException {
    if( !id.equals( getID() ) ) {
      throw new MessageException( "decode error: invalid id" );
    }
    
    if( version != getVersion()  ) {
      throw new MessageException( "decode error: invalid version" );
    }
    
    if( data != null && data.hasRemaining( DirectByteBuffer.SS_MSG ) ) {
      throw new MessageException( "decode error: payload not empty" );
    }
    
    if( data != null )  data.returnToPool();
    
    return new Ping();
  }
  
  public void destroy() { /*nothing*/ } 
}
