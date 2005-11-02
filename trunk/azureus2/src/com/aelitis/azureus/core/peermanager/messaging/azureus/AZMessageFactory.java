/*
 * Created on Feb 19, 2005
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

package com.aelitis.azureus.core.peermanager.messaging.azureus;

import java.util.*;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.networkmanager.RawMessage;
import com.aelitis.azureus.core.networkmanager.impl.RawMessageImpl;
import com.aelitis.azureus.core.peermanager.messaging.*;
import com.aelitis.azureus.core.peermanager.messaging.azureus.session.*;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.*;




/**
 * Factory for handling AZ message creation.
 * NOTE: wire format: [total message length] + [id length] + [id bytes] + [version byte] + [payload bytes]
 */
public class AZMessageFactory {
  private static final byte bss = DirectByteBuffer.SS_MSG;
  
  
  
  private static final Map legacy_data = new HashMap();
  static {
    legacy_data.put( BTMessage.ID_BT_CHOKE, new LegacyData( RawMessage.PRIORITY_HIGH, true, new Message[]{new BTUnchoke(), new BTPiece(-1, -1, null )} ) );
    legacy_data.put( BTMessage.ID_BT_UNCHOKE, new LegacyData( RawMessage.PRIORITY_NORMAL, true, new Message[]{new BTChoke()} ) );
    legacy_data.put( BTMessage.ID_BT_INTERESTED, new LegacyData( RawMessage.PRIORITY_HIGH, true, new Message[]{new BTUninterested()} ) );
    legacy_data.put( BTMessage.ID_BT_UNINTERESTED, new LegacyData( RawMessage.PRIORITY_NORMAL, false, new Message[]{new BTInterested()} ) );
    legacy_data.put( BTMessage.ID_BT_HAVE, new LegacyData( RawMessage.PRIORITY_LOW, false, null ) );
    legacy_data.put( BTMessage.ID_BT_BITFIELD, new LegacyData( RawMessage.PRIORITY_HIGH, true, null ) );
    legacy_data.put( BTMessage.ID_BT_REQUEST, new LegacyData( RawMessage.PRIORITY_NORMAL, true, null ) );
    legacy_data.put( BTMessage.ID_BT_PIECE, new LegacyData( RawMessage.PRIORITY_LOW, false, null ) );
    legacy_data.put( BTMessage.ID_BT_CANCEL, new LegacyData( RawMessage.PRIORITY_HIGH, true, null ) );
    legacy_data.put( BTMessage.ID_BT_HANDSHAKE, new LegacyData( RawMessage.PRIORITY_HIGH, true, null ) );
    legacy_data.put( BTMessage.ID_BT_KEEP_ALIVE, new LegacyData( RawMessage.PRIORITY_LOW, false, null ) );
  }
  
  
  
  /**
   * Initialize the factory, i.e. register the messages with the message manager.
   */
  public static void init() {
    try {
      MessageManager.getSingleton().registerMessageType( new AZHandshake( new byte[20], "", "", 0, 0, new String[0], new byte[0]) );
      MessageManager.getSingleton().registerMessageType( new AZPeerExchange( new byte[20], null, null ) );
      /*
      MessageManager.getSingleton().registerMessageType( new AZSessionSyn( new byte[20], -1, null) );
      MessageManager.getSingleton().registerMessageType( new AZSessionAck( new byte[20], -1, null) );
      MessageManager.getSingleton().registerMessageType( new AZSessionEnd( new byte[20], "" ) );
      MessageManager.getSingleton().registerMessageType( new AZSessionBitfield( -1, null ) );
      MessageManager.getSingleton().registerMessageType( new AZSessionCancel( -1, -1, -1, -1 ) );
      MessageManager.getSingleton().registerMessageType( new AZSessionHave( -1, new int[]{-1} ) );
      MessageManager.getSingleton().registerMessageType( new AZSessionPiece( -1, -1, -1, null ) );
      MessageManager.getSingleton().registerMessageType( new AZSessionRequest( -1, (byte)-1, -1, -1, -1 ) );
      */
    }
    catch( MessageException me ) {  me.printStackTrace();  }
  }
  
  
  /**
   * Register a generic map payload type with the factory.
   * @param type_id to register
   * @throws MessageException on registration error
   */
  public static void registerGenericMapPayloadMessageType( String type_id ) throws MessageException {
  	MessageManager.getSingleton().registerMessageType( new AZGenericMapPayload( type_id, null ) );
  }
  
  
  
  /**
   * Construct a new AZ message instance from the given message raw byte stream.
   * @param stream_payload data
   * @return decoded/deserialized AZ message
   * @throws MessageException if message creation failed.
   * NOTE: Does not auto-return given direct buffer on thrown exception.
   */
  public static Message createAZMessage( DirectByteBuffer stream_payload ) throws MessageException {
    int id_length = stream_payload.getInt( bss );

    if( id_length < 1 || id_length > 1024 || id_length > stream_payload.remaining( bss ) - 1 ) {
      byte bt_id = stream_payload.get( (byte)0, 0 );
      throw new MessageException( "invalid AZ id length given: " +id_length+ ", stream_payload.remaining(): " +stream_payload.remaining( bss )+ ", BT id?=" +bt_id );
    }
    
    byte[] id_bytes = new byte[ id_length ];
    
    stream_payload.get( bss, id_bytes );
    
    byte version = stream_payload.get( bss );
    
    return MessageManager.getSingleton().createMessage( new String( id_bytes ), version, stream_payload );
  }
  
  
  
  
  /**
   * Create the proper AZ raw message from the given base message.
   * @param base_message to create from
   * @return AZ raw message
   */
  public static RawMessage createAZRawMessage( Message base_message ) {
    byte[] id_bytes = base_message.getID().getBytes();
    DirectByteBuffer[] payload = base_message.getData();
    
    int payload_size = 0;
    for( int i=0; i < payload.length; i++ ) {
      payload_size += payload[i].remaining( bss );
    }
    
    //create and fill header buffer
    DirectByteBuffer header = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_MSG_AZ_HEADER, 9 + id_bytes.length );
    header.putInt( bss, 5 + id_bytes.length + payload_size );
    header.putInt( bss, id_bytes.length );
    header.put( bss, id_bytes );
    header.put( bss, base_message.getVersion() );
    header.flip( bss );
    
    DirectByteBuffer[] raw_buffs = new DirectByteBuffer[ payload.length + 1 ];
    raw_buffs[0] = header;
    for( int i=0; i < payload.length; i++ ) {
      raw_buffs[i+1] = payload[i];
    }
     
    LegacyData ld = (LegacyData)legacy_data.get( base_message.getID() );  //determine if a legacy BT message
    
    if( ld != null ) {  //legacy message, use pre-configured values
      return new RawMessageImpl( base_message, raw_buffs, ld.priority, ld.is_no_delay, ld.to_remove );
    }
    
    //standard message, ensure that protocol messages have wire priority over data payload messages
    int priority = base_message.getType() == Message.TYPE_DATA_PAYLOAD ? RawMessage.PRIORITY_LOW : RawMessage.PRIORITY_NORMAL;
    
    return new RawMessageImpl( base_message, raw_buffs, priority, true, null );
  }
  
  
  
  
  
  
  private static class LegacyData {
    private final int priority;
    private final boolean is_no_delay;
    private final Message[] to_remove;
    
    private LegacyData( int prio, boolean no_delay, Message[] remove ) {
      this.priority = prio;
      this.is_no_delay = no_delay;
      this.to_remove = remove;
    }
  }
  
}
