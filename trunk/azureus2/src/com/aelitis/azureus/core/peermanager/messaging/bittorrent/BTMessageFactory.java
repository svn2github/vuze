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

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.networkmanager.RawMessage;
import com.aelitis.azureus.core.networkmanager.RawMessageImpl;
import com.aelitis.azureus.core.peermanager.messaging.*;


/**
 *
 */
public class BTMessageFactory {
  
  /**
   * Initialize the factory, i.e. register the messages with the message manager.
   */
  public static void init() {
    MessageManager.getSingleton().registerMessage( new BTBitfield( null ) );
    MessageManager.getSingleton().registerMessage( new BTCancel( -1, -1, -1 ) );
    MessageManager.getSingleton().registerMessage( new BTChoke() );
    MessageManager.getSingleton().registerMessage( new BTHandshake( new byte[0], new byte[0] ) );
    MessageManager.getSingleton().registerMessage( new BTHave( -1 ) );
    MessageManager.getSingleton().registerMessage( new BTInterested() );
    MessageManager.getSingleton().registerMessage( new BTKeepAlive() );
    MessageManager.getSingleton().registerMessage( new BTPiece() );
    MessageManager.getSingleton().registerMessage( new BTRequest( -1, -1 , -1 ) );
    MessageManager.getSingleton().registerMessage( new BTUnchoke() );
    MessageManager.getSingleton().registerMessage( new BTUninterested() );
  }

  
  
  
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
   * Construct a new bt message instance from the given message information.
   * @param bt_id bt byte message id
   * @param message_data payload
   * @return decoded/deserialized legacy message
   * @throws MessageException if message creation failed
   */
  public static Message createBTMessage( byte bt_id, DirectByteBuffer message_data ) throws MessageException {
    switch( bt_id ) {
      case 0:
        return MessageManager.getSingleton().createMessage( BTMessage.ID_BT_CHOKE, BTMessage.BT_DEFAULT_VERSION, message_data );
        
      case 1:
        return MessageManager.getSingleton().createMessage( BTMessage.ID_BT_UNCHOKE, BTMessage.BT_DEFAULT_VERSION, message_data );
        
      case 2:
        return MessageManager.getSingleton().createMessage( BTMessage.ID_BT_INTERESTED, BTMessage.BT_DEFAULT_VERSION, message_data );
        
      case 3:
        return MessageManager.getSingleton().createMessage( BTMessage.ID_BT_UNINTERESTED, BTMessage.BT_DEFAULT_VERSION, message_data );
        
      case 4:
        return MessageManager.getSingleton().createMessage( BTMessage.ID_BT_HAVE, BTMessage.BT_DEFAULT_VERSION, message_data );
        
      case 5:
        return MessageManager.getSingleton().createMessage( BTMessage.ID_BT_BITFIELD, BTMessage.BT_DEFAULT_VERSION, message_data );
        
      case 6:
        return MessageManager.getSingleton().createMessage( BTMessage.ID_BT_REQUEST, BTMessage.BT_DEFAULT_VERSION, message_data );
        
      case 7:
        return MessageManager.getSingleton().createMessage( BTMessage.ID_BT_PIECE, BTMessage.BT_DEFAULT_VERSION, message_data );
        
      case 8:
        return MessageManager.getSingleton().createMessage( BTMessage.ID_BT_CANCEL, BTMessage.BT_DEFAULT_VERSION, message_data );
        
      default:
        throw new MessageException( "unknown legacy message id [" +bt_id+ "]" );
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
        
    DirectByteBuffer header = new DirectByteBuffer( ByteBuffer.allocate( 5 ) );
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
  
  
  

  
  /**
   * Determine a bt message's type via byte id lookup.
   * @param bt_id of legacy message
   * @return message type
   * @throws MessageException if type lookup fails
   */
  public static int determineBTMessageType( byte bt_id ) throws MessageException {
    switch( bt_id ) {
      case 0:
        return MessageManager.getSingleton().determineMessageType( BTMessage.ID_BT_CHOKE, BTMessage.BT_DEFAULT_VERSION );
        
      case 1:
        return MessageManager.getSingleton().determineMessageType( BTMessage.ID_BT_UNCHOKE, BTMessage.BT_DEFAULT_VERSION );
        
      case 2:
        return MessageManager.getSingleton().determineMessageType( BTMessage.ID_BT_INTERESTED, BTMessage.BT_DEFAULT_VERSION );
        
      case 3:
        return MessageManager.getSingleton().determineMessageType( BTMessage.ID_BT_UNINTERESTED, BTMessage.BT_DEFAULT_VERSION );
        
      case 4:
        return MessageManager.getSingleton().determineMessageType( BTMessage.ID_BT_HAVE, BTMessage.BT_DEFAULT_VERSION );
        
      case 5:
        return MessageManager.getSingleton().determineMessageType( BTMessage.ID_BT_BITFIELD, BTMessage.BT_DEFAULT_VERSION );
        
      case 6:
        return MessageManager.getSingleton().determineMessageType( BTMessage.ID_BT_REQUEST, BTMessage.BT_DEFAULT_VERSION );
        
      case 7:
        return MessageManager.getSingleton().determineMessageType( BTMessage.ID_BT_PIECE, BTMessage.BT_DEFAULT_VERSION );
        
      case 8:
        return MessageManager.getSingleton().determineMessageType( BTMessage.ID_BT_CANCEL, BTMessage.BT_DEFAULT_VERSION );
        
      default:
        throw new MessageException( "unknown legacy message id [" +bt_id+ "]" );
    }
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
