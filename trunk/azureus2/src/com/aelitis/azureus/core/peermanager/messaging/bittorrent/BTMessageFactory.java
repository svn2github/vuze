/*
 * Created on Feb 9, 2005
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

package com.aelitis.azureus.core.peermanager.messaging.bittorrent;

import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.networkmanager.RawMessage;
import com.aelitis.azureus.core.networkmanager.impl.RawMessageImpl;
import com.aelitis.azureus.core.peermanager.messaging.*;


/**
 *
 */
public class BTMessageFactory {
  
  /**
   * Initialize the factory, i.e. register the messages with the message manager.
   */
  public static void init() {
    try {
      MessageManager.getSingleton().registerMessageType( new BTBitfield( null ) );
      MessageManager.getSingleton().registerMessageType( new BTCancel( -1, -1, -1 ) );
      MessageManager.getSingleton().registerMessageType( new BTChoke() );
      MessageManager.getSingleton().registerMessageType( new BTHandshake( new byte[0], new byte[0], true ) );
      MessageManager.getSingleton().registerMessageType( new BTHave( -1 ) );
      MessageManager.getSingleton().registerMessageType( new BTInterested() );
      MessageManager.getSingleton().registerMessageType( new BTKeepAlive() );
      MessageManager.getSingleton().registerMessageType( new BTPiece( -1, -1, null ) );
      MessageManager.getSingleton().registerMessageType( new BTRequest( -1, -1 , -1 ) );
      MessageManager.getSingleton().registerMessageType( new BTUnchoke() );
      MessageManager.getSingleton().registerMessageType( new BTUninterested() );
    }
    catch( MessageException me ) {  me.printStackTrace();  }
  }

  
  
  
  private static final Map legacy_data = new HashMap();
  static {
    legacy_data.put( BTMessage.ID_BT_CHOKE, new LegacyData( RawMessage.PRIORITY_HIGH, true, new Message[]{new BTUnchoke(), new BTPiece( -1, -1, null )}, (byte)0 ) );
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
   * Construct a new BT message instance from the given message raw byte stream.
   * @param stream_payload data
   * @return decoded/deserialized BT message
   * @throws MessageException if message creation failed
   * NOTE: Does not auto-return given direct buffer on thrown exception.
   */
  public static Message createBTMessage( DirectByteBuffer stream_payload ) throws MessageException {
    byte id = stream_payload.get( DirectByteBuffer.SS_MSG );
    
    switch( id ) {
      case 0:
        return MessageManager.getSingleton().createMessage( BTMessage.ID_BT_CHOKE, BTMessage.BT_DEFAULT_VERSION, stream_payload );
        
      case 1:
        return MessageManager.getSingleton().createMessage( BTMessage.ID_BT_UNCHOKE, BTMessage.BT_DEFAULT_VERSION, stream_payload );
        
      case 2:
        return MessageManager.getSingleton().createMessage( BTMessage.ID_BT_INTERESTED, BTMessage.BT_DEFAULT_VERSION, stream_payload );
        
      case 3:
        return MessageManager.getSingleton().createMessage( BTMessage.ID_BT_UNINTERESTED, BTMessage.BT_DEFAULT_VERSION, stream_payload );
        
      case 4:
        return MessageManager.getSingleton().createMessage( BTMessage.ID_BT_HAVE, BTMessage.BT_DEFAULT_VERSION, stream_payload );
        
      case 5:
        return MessageManager.getSingleton().createMessage( BTMessage.ID_BT_BITFIELD, BTMessage.BT_DEFAULT_VERSION, stream_payload );
        
      case 6:
        return MessageManager.getSingleton().createMessage( BTMessage.ID_BT_REQUEST, BTMessage.BT_DEFAULT_VERSION, stream_payload );
        
      case 7:
        return MessageManager.getSingleton().createMessage( BTMessage.ID_BT_PIECE, BTMessage.BT_DEFAULT_VERSION, stream_payload );
        
      case 8:
        return MessageManager.getSingleton().createMessage( BTMessage.ID_BT_CANCEL, BTMessage.BT_DEFAULT_VERSION, stream_payload );
        
      case 20:
        //Clients seeing our handshake reserved bit will send us the old 'extended' messaging hello message accidentally.
        //Instead of throwing an exception and dropping the peer connection, we'll just fake it as a keep-alive :)
        if( LGLogger.isEnabled() )  LGLogger.log( "Old extended messaging hello received, ignoring and faking as keep-alive." );
        return MessageManager.getSingleton().createMessage( BTMessage.ID_BT_KEEP_ALIVE, BTMessage.BT_DEFAULT_VERSION, null );
        
      default: {  System.out.println( "Unknown BT message id [" +id+ "]" );
        					throw new MessageException( "Unknown BT message id [" +id+ "]" );
      				}
    }
  }
  
  
  
  /**
   * Create the proper BT raw message from the given base message.
   * @param base_message to create from
   * @return BT raw message
   */
  public static RawMessage createBTRawMessage( Message base_message ) {
    if( base_message instanceof RawMessage ) {  //used for handshake and keep-alive messages
      return (RawMessage)base_message;
    }
    
    LegacyData ld = (LegacyData)legacy_data.get( base_message.getID() );
    
    if( ld == null ) {
      Debug.out( "legacy message type id not found for [" +base_message.getID()+ "]" );
      return null;  //message id type not found
    }
    
    DirectByteBuffer[] payload = base_message.getData();
    
    int payload_size = 0;
    for( int i=0; i < payload.length; i++ ) {
      payload_size += payload[i].remaining( DirectByteBuffer.SS_MSG );
    }  
        
    DirectByteBuffer header = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_MSG_BT_HEADER, 5 );
    header.putInt( DirectByteBuffer.SS_MSG, 1 + payload_size );
    header.put( DirectByteBuffer.SS_MSG, ld.bt_id );
    header.flip( DirectByteBuffer.SS_MSG );
    
    DirectByteBuffer[] raw_buffs = new DirectByteBuffer[ payload.length + 1 ];
    raw_buffs[0] = header;
    for( int i=0; i < payload.length; i++ ) {
      raw_buffs[i+1] = payload[i];
    }
    
    return new RawMessageImpl( base_message, raw_buffs, ld.priority, ld.is_no_delay, ld.to_remove );
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
