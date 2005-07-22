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

import java.util.*;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.peermanager.messaging.*;
import com.aelitis.azureus.core.peermanager.messaging.azureus.AZMessage;



/**
 * Sent when a torrent session ends/fails.
 */
public class AZSessionEnd implements AZMessage {  
  private DirectByteBuffer buffer = null;
  private String description = null;
  
  private final byte[] infohash;
  private final String reason;
  

  public AZSessionEnd( byte[] infohash, String reason ) {
    this.infohash = infohash;
    this.reason = reason;
  }
  
  
  public byte[] getInfoHash() {  return infohash;  }
  public String getEndReason() {  return reason;  }
  
    
  public String getID() {  return AZMessage.ID_AZ_SESSION_END;  }
  
  public byte getVersion() {  return AZMessage.AZ_DEFAULT_VERSION;  }
  
  public int getType() {  return Message.TYPE_PROTOCOL_PAYLOAD;  }
    
  
  public String getDescription() {
    if( description == null ) {
      description = getID()+ " for infohash " +ByteFormatter.nicePrint( infohash, true )+ " because " +reason;
    }
    return description;
  }
  
  
  public DirectByteBuffer[] getData() {
    if( buffer == null ) {
      Map payload_map = new HashMap();
      
      payload_map.put( "infohash", infohash );
      payload_map.put( "reason", reason );
      
      buffer = MessagingUtil.convertPayloadToBencodedByteStream( payload_map, DirectByteBuffer.AL_MSG );
    }
    
    return new DirectByteBuffer[]{ buffer };
  }
  
  
  public Message deserialize( DirectByteBuffer data ) throws MessageException {    
    Map root = MessagingUtil.convertBencodedByteStreamToPayload( data, 20, getID(), getVersion() );

    byte[] hash = (byte[])root.get( "infohash" );
    if( hash == null )  throw new MessageException( "hash == null" );
    if( hash.length != 20 )  throw new MessageException( "hash.length != 20: " +hash.length );
    
    byte[] reason_raw = (byte[])root.get( "reason" );
    if( reason_raw == null )  throw new MessageException( "reason_raw == null" );
    String res = new String( reason_raw );
    
    return new AZSessionEnd( hash, res );
  }
  
  
  public void destroy() {
    if( buffer != null )  buffer.returnToPool();
  }
  
}
