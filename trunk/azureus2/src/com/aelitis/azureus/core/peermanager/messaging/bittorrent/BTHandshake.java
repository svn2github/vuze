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

import com.aelitis.azureus.core.networkmanager.RawMessage;
import com.aelitis.azureus.core.peermanager.messaging.*;
import com.aelitis.azureus.core.peermanager.utils.PeerClassifier;


/**
 * BitTorrent handshake message.
 */
public class BTHandshake implements BTMessage, RawMessage {
  public static final String PROTOCOL = "BitTorrent protocol";
  //public static final byte[] RESERVED = new byte[]{ (byte)128, 0, 0, 0, 0, 0, 0, 0 };  //set high bit of first byte
  public static final byte[] RESERVED = new byte[]{ 0, 0, 0, 0, 0, 0, 0, 0 };  //set high bit of first byte
  
  private final DirectByteBuffer[] buffer;
  private final String description;
  
  private final byte[] reserved_bytes;
  private final byte[] datahash_bytes;
  private final byte[] peer_id_bytes;
  
  
  
  /**
   * Used for outgoing handshake message.
   * @param data_hash
   * @param peer_id
   */
  public BTHandshake( byte[] data_hash, byte[] peer_id ) {
    this( null, data_hash, peer_id );
  }
  
  
  private BTHandshake( byte[] reserved, byte[] data_hash, byte[] peer_id ) {
    DirectByteBuffer dbb = new DirectByteBuffer( ByteBuffer.allocate( 68 ) );
    dbb.put( DirectByteBuffer.SS_BT, (byte)PROTOCOL.length() );
    dbb.put( DirectByteBuffer.SS_BT, PROTOCOL.getBytes() );
    dbb.put( DirectByteBuffer.SS_BT, reserved == null ? RESERVED : reserved );
    dbb.put( DirectByteBuffer.SS_BT, data_hash );
    dbb.put( DirectByteBuffer.SS_BT, peer_id );
    dbb.flip( DirectByteBuffer.SS_BT );
    buffer = new DirectByteBuffer[] { dbb };    
      
    this.reserved_bytes = reserved;
    this.datahash_bytes = data_hash;
    this.peer_id_bytes = peer_id;
    
    description = BTMessage.ID_BT_HANDSHAKE + " of dataID: " +ByteFormatter.nicePrint( data_hash, true ) + " peerID: " +PeerClassifier.getPrintablePeerID( peer_id );
    
      /* for( int i=7; i >= 0; i-- ) {
           byte b = (byte) (RESERVED[0] >> i);
           int val = b & 0x01;
           System.out.print( val == 1 ? "x" : "." );
         }
         System.out.println(); */
  }
  

  
  public byte[] getReserved() {  return reserved_bytes;  }
  
  public byte[] getDataHash() {  return datahash_bytes;  }
  
  public byte[] getPeerId() {  return peer_id_bytes;  }
  
  
    
  

  // message
  public String getID() {  return BTMessage.ID_BT_HANDSHAKE;  }
  
  public byte getVersion() {  return BTMessage.BT_DEFAULT_VERSION;  }
  
  public int getType() {  return Message.TYPE_PROTOCOL_PAYLOAD;  }
    
  public String getDescription() {  return description;  }
  
  public DirectByteBuffer[] getData() {  return buffer;  }

  public Message deserialize( String id, byte version, DirectByteBuffer data ) throws MessageException {
    if( !id.equals( getID() ) ) {
      throw new MessageException( "decode error: invalid id" );
    }
    
    if( version != getVersion()  ) {
      throw new MessageException( "decode error: invalid version" );
    }
    
    if( data == null ) {
      throw new MessageException( "decode error: data == null" );
    }
    
    if( data.remaining( DirectByteBuffer.SS_MSG ) < 68 ) {
      throw new MessageException( "decode error: payload.remaining() < 68" );
    }
    
    if( data.get( DirectByteBuffer.SS_MSG ) != (byte)PROTOCOL.length() ) {
      throw new MessageException( "decode error: payload.get() != (byte)PROTOCOL.length()" );
    }
    
    byte[] header = new byte[ PROTOCOL.getBytes().length ];
    data.get( DirectByteBuffer.SS_MSG, header );
    
    if( !PROTOCOL.equals( new String( header ) ) ) {
      throw new MessageException( "decode error: !PROTOCOL.equals( new String( header ) )" );
    }
    
    byte[] reserved = new byte[ 8 ];
    data.get( DirectByteBuffer.SS_MSG, reserved );          
    
    byte[] infohash = new byte[ 20 ];
    data.get( DirectByteBuffer.SS_MSG, infohash );
    
    byte[] peerid = new byte[ 20 ];
    data.get( DirectByteBuffer.SS_MSG, peerid );
    
    data.returnToPool();
    
    return new BTHandshake( reserved, infohash, peerid );
  }
  
  
  
  // raw message
  public DirectByteBuffer[] getRawPayload() {  return buffer;  }
  
  public int getPriority() {  return RawMessage.PRIORITY_HIGH;  }

  public boolean isNoDelay() {  return true;  }
 
  public Message[] messagesToRemove() {  return null;  }

  public void destroy() {  }
  
}
