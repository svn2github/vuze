/*
 * Created on Jan 12, 2005
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

package com.aelitis.azureus.core.peermanager.messaging;

import java.nio.ByteBuffer;
import java.util.*;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.peermanager.messaging.bittorrent.*;


/**
 * Provides factory methods for creating different types of raw,
 * wire-format messages from an original basic message.
 */
public class RawMessageFactory {
  
  private static final Map legacy_data = new HashMap();
  static {
    legacy_data.put( BTMessage.ID_BT_CHOKE, new LegacyData( RawMessage.PRIORITY_HIGH, true, new Message[]{new BTUnchoke(), new BTPiece()}, (byte)0 ) );
    legacy_data.put( BTMessage.ID_BT_UNCHOKE, new LegacyData( RawMessage.PRIORITY_NORMAL, true, new Message[]{new BTChoke()}, (byte)1 ) );
    legacy_data.put( BTMessage.ID_BT_INTERESTED, new LegacyData( RawMessage.PRIORITY_HIGH, true, new Message[]{new BTUninterested()}, (byte)2 ) );
    legacy_data.put( BTMessage.ID_BT_UNINTERESTED, new LegacyData( RawMessage.PRIORITY_NORMAL, false, new Message[]{new BTInterested()}, (byte)3 ) );
    legacy_data.put( BTMessage.ID_BT_HAVE, new LegacyData( RawMessage.PRIORITY_LOW, false, null, (byte)4 ) );
    legacy_data.put( BTMessage.ID_BT_BITFIELD, new LegacyData( RawMessage.PRIORITY_HIGH, true, null, (byte)5 ) );
    legacy_data.put( BTMessage.ID_BT_REQUEST, new LegacyData( RawMessage.PRIORITY_NORMAL, true, null, (byte)6 ) );
    legacy_data.put( BTMessage.ID_BT_PIECE, new LegacyData( RawMessage.PRIORITY_LOW, false, null, (byte)7 ) );
    legacy_data.put( BTMessage.ID_BT_CANCEL, new LegacyData( RawMessage.PRIORITY_HIGH, true, null, (byte)8 ) );
  }
  
  
  /**
   * Creates a standard raw message from the given source message
   * using default return values for the advanced queue functionality.
   * NOTE: wire format: [id length] + [id bytes] + [version byte] + [payload length] + [payload bytes]
   * @param message original
   * @return new raw message
   */
  protected static RawMessage createRawMessage( Message message ) {
    byte[] id_bytes = message.getID().getBytes();
    DirectByteBuffer[] payload = message.getData();
    
    int payload_size = 0;
    for( int i=0; i < payload.length; i++ ) {
      payload_size += payload[i].remaining( DirectByteBuffer.SS_MSG );
    }
    
    //create and fill header buffer
    DirectByteBuffer header = new DirectByteBuffer( ByteBuffer.allocate( 9 + id_bytes.length ) );
    header.putInt( DirectByteBuffer.SS_MSG, id_bytes.length );
    header.put( DirectByteBuffer.SS_MSG, id_bytes );
    header.put( DirectByteBuffer.SS_MSG, message.getVersion() );
    header.putInt( DirectByteBuffer.SS_MSG, payload_size );
    header.flip( DirectByteBuffer.SS_MSG );
    
    DirectByteBuffer[] raw_buffs = new DirectByteBuffer[ payload.length + 1 ];
    raw_buffs[0] = header;
    for( int i=0; i < payload.length; i++ ) {
      raw_buffs[i+1] = payload[i];
    }
    
    //TODO bt protocol message special queue values
    
    return new RawMessageImpl( message, raw_buffs, RawMessage.PRIORITY_NORMAL, true, null );
  }
  
   
  
  /**
   * Creates a legacy (i.e. traditional BitTorrent wire protocol) raw message
   * from the given source message using return values for the advanced
   * queue functionality based on the type of legacy message it is.
   * If the given message id type isn't one of the known pre-configured
   * types, null is returned.
   * NOTE: wire format: [total message length] + [message id byte] + [payload bytes]
   * @param message original
   * @return new legacy message, or null if error
   */
  protected static RawMessage createLegacyRawMessage( Message message ) {
    if( message instanceof RawMessage ) {  //used for handshake and keep-alive messages
      return (RawMessage)message;
    }
    
    LegacyData ld = (LegacyData)legacy_data.get( message.getID() );
    
    if( ld == null ) {
      Debug.out( "legacy message type id not found!" );
      return null;  //message id type not found
    }
    
    DirectByteBuffer[] payload = message.getData();
    
    int payload_size = 0;
    for( int i=0; i < payload.length; i++ ) {
      payload_size += payload[i].remaining( DirectByteBuffer.SS_MSG );
    }  
        
    DirectByteBuffer header = new DirectByteBuffer( ByteBuffer.allocate( 5 ) );
    header.putInt( DirectByteBuffer.SS_MSG, 1 + payload_size );
    header.put( DirectByteBuffer.SS_MSG, ld.bt_id );
    header.flip( DirectByteBuffer.SS_MSG );
    
    DirectByteBuffer[] raw_buffs = new DirectByteBuffer[ payload.length + 1 ];
    raw_buffs[0] = header;
    for( int i=0; i < payload.length; i++ ) {
      raw_buffs[i+1] = payload[i];
    }
    
    return new RawMessageImpl( message, raw_buffs, ld.priority, ld.is_no_delay, ld.to_remove );
  }
  
  

  
  private static class LegacyData {
    private final int priority;
    private final boolean is_no_delay;
    private final Message[] to_remove;
    private final byte bt_id;
    
    private LegacyData( int prio, boolean no_delay, Message[] remove, byte btid ) {
      this.priority = prio;
      this.is_no_delay = no_delay;
      this.to_remove = remove;
      this.bt_id = btid;
    }
  }

}
