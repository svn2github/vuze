/*
 * Created on Jan 27, 2005
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

import java.util.*;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTMessage;

/**
 *
 */
public class MessageFactory {
  private static final HashMap message_registrations = new HashMap();
  
  
  /**
   * Register the given message with the factory.
   * @param message instance to use for decoding
   */  
  protected static void registerMessage( Message message ) {
    Object key = new String( message.getID() + message.getVersion() );
    message_registrations.put( key, message );
  }
  
  
  /**
   * Remove registration of given message from factory
   * @param message type to remove
   */
  protected static void deregisterMessage( Message message ) {
    Object key = new String( message.getID() + message.getVersion() );
    message_registrations.remove( key );
  }
  
  
  /**
   * Construct a new message instance from the given message information.
   * @param id of message
   * @param version of message
   * @param message_data payload
   * @return decoded/deserialized message
   * @throws MessageException if message creation failed
   */
  protected static Message createMessage( String id, byte version, DirectByteBuffer message_data ) throws MessageException {
    Object key = new String( id + version );
    
    Message message = (Message)message_registrations.get( key );
    
    if( message == null ) {
      throw new MessageException( "message id/version not registered" );
    }
    
    return message.deserialize( id, version, message_data );    
  }
  

  
  /**
   * Construct a new legacy message instance from the given message information.
   * @param legacy_id byte message id
   * @param message_data payload
   * @return decoded/deserialized legacy message
   * @throws MessageException if message creation failed
   */
  protected static Message createLegacyMessage( byte legacy_id, DirectByteBuffer message_data ) throws MessageException {
    switch( legacy_id ) {
      case 0:
        return createMessage( BTMessage.ID_BT_CHOKE, BTMessage.BT_DEFAULT_VERSION, message_data );
        
      case 1:
        return createMessage( BTMessage.ID_BT_UNCHOKE, BTMessage.BT_DEFAULT_VERSION, message_data );
        
      case 2:
        return createMessage( BTMessage.ID_BT_INTERESTED, BTMessage.BT_DEFAULT_VERSION, message_data );
        
      case 3:
        return createMessage( BTMessage.ID_BT_UNINTERESTED, BTMessage.BT_DEFAULT_VERSION, message_data );
        
      case 4:
        return createMessage( BTMessage.ID_BT_HAVE, BTMessage.BT_DEFAULT_VERSION, message_data );
        
      case 5:
        return createMessage( BTMessage.ID_BT_BITFIELD, BTMessage.BT_DEFAULT_VERSION, message_data );
        
      case 6:
        return createMessage( BTMessage.ID_BT_REQUEST, BTMessage.BT_DEFAULT_VERSION, message_data );
        
      case 7:
        return createMessage( BTMessage.ID_BT_PIECE, BTMessage.BT_DEFAULT_VERSION, message_data );
        
      case 8:
        return createMessage( BTMessage.ID_BT_CANCEL, BTMessage.BT_DEFAULT_VERSION, message_data );
        
      default:
        throw new MessageException( "unknown legacy message id [" +legacy_id+ "]" );
    }
  }
  
  
  
  /**
   * Determine a message's type via id+version lookup.
   * @param id of message
   * @param version of message
   * @return message type
   * @throws MessageException if type lookup fails
   */
  protected static int determineMessageType( String id, byte version ) throws MessageException {
    Object key = new String( id + version );
    
    Message message = (Message)message_registrations.get( key );
    
    if( message == null ) {
      throw new MessageException( "message id/version not registered" );
    }
    
    return message.getType();
  }
  
  
  
  /**
   * Determine a legacy message's type via byte id lookup.
   * @param legacy_id of message
   * @return message type
   * @throws MessageException if type lookup fails
   */
  protected static int determineLegacyMessageType( byte legacy_id ) throws MessageException {
    switch( legacy_id ) {
      case 0:
        return determineMessageType( BTMessage.ID_BT_CHOKE, BTMessage.BT_DEFAULT_VERSION );
        
      case 1:
        return determineMessageType( BTMessage.ID_BT_UNCHOKE, BTMessage.BT_DEFAULT_VERSION );
        
      case 2:
        return determineMessageType( BTMessage.ID_BT_INTERESTED, BTMessage.BT_DEFAULT_VERSION );
        
      case 3:
        return determineMessageType( BTMessage.ID_BT_UNINTERESTED, BTMessage.BT_DEFAULT_VERSION );
        
      case 4:
        return determineMessageType( BTMessage.ID_BT_HAVE, BTMessage.BT_DEFAULT_VERSION );
        
      case 5:
        return determineMessageType( BTMessage.ID_BT_BITFIELD, BTMessage.BT_DEFAULT_VERSION );
        
      case 6:
        return determineMessageType( BTMessage.ID_BT_REQUEST, BTMessage.BT_DEFAULT_VERSION );
        
      case 7:
        return determineMessageType( BTMessage.ID_BT_PIECE, BTMessage.BT_DEFAULT_VERSION );
        
      case 8:
        return determineMessageType( BTMessage.ID_BT_CANCEL, BTMessage.BT_DEFAULT_VERSION );
        
      default:
        throw new MessageException( "unknown legacy message id [" +legacy_id+ "]" );
    }
  }

  
}
