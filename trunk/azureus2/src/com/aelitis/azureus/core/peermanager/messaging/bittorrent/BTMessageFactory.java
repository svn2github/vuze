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

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.networkmanager.RawMessage;
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
  
  
  
  public static RawMessage createBTRawMessage( Message base_message ) {
    //TODO
    return null;
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
  

}
