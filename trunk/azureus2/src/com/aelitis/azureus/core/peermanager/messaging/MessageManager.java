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

package com.aelitis.azureus.core.peermanager.messaging;

import java.util.*;

import org.gudy.azureus2.core3.util.*;


import com.aelitis.azureus.core.peermanager.messaging.azureus.AZMessageFactory;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTMessageFactory;




/**
 *
 */
public class MessageManager {
  private static final MessageManager instance = new MessageManager();
  
  private final HashMap message_registrations = new HashMap();

  protected AEMonitor	this_mon = new AEMonitor( "MessageManager" );
  
  private MessageManager() {
    /*nothing*/
  }
  
  
  public static MessageManager getSingleton() {  return instance;  }

  
  /**
   * Perform manager initialization.
   */
  public void initialize() {
    AZMessageFactory.init();  //register AZ message types
    BTMessageFactory.init();  //register BT message types
  }
  

  
  
  /**
   * Register the given message type with the manager for processing.
   * @param message instance to use for decoding
   * @throws MessageException if this message type has already been registered
   */
  public void registerMessageType( Message message ) throws MessageException {
  	try{
  		this_mon.enter();
  		
	    Object key = new String( message.getID() + message.getVersion() );
	    
	    if( message_registrations.containsKey( key ) ) {
	      throw new MessageException( "message type [" +message.getID()+ ":" +message.getVersion()+ "] already registered!" );
	    }
	    
	    message_registrations.put( key, message );
	    
  	}finally{
  		
  		this_mon.exit();
  	}
  }
  
  
  
  /**
   * Remove registration of given message type from manager.
   * @param message type to remove
   */
  public void deregisterMessageType( Message message ) {
    try{  this_mon.enter();
    
      Object key = new String( message.getID() + message.getVersion() );
      message_registrations.remove( key );
    }
    finally{  this_mon.exit();  }
  }
  
  
  /**
   * Construct a new message instance from the given message information.
   * @param id of message
   * @param version of message
   * @param message_data payload
   * @return decoded/deserialized message
   * @throws MessageException if message creation failed
   */
  public Message createMessage( String id, byte version, DirectByteBuffer message_data ) throws MessageException {
    Object key = new String( id + version );
    
    Message message = (Message)message_registrations.get( key );
    
    if( message == null ) {
      throw new MessageException( "message id[" +id+ "] / version[" +version+ "] not registered" );
    }
    
    return message.deserialize( message_data );    
  }
  
  
  
  /**
   * Lookup a registered message type via id and version.
   * @param id to look for
   * @param version to look for
   * @return the default registered message instance if found, otherwise returns null if this message type is not registered
   */
  public Message lookupMessage( String id, byte version ) {
    Object key = new String( id + version );
    
    return (Message)message_registrations.get( key );
  }
  
  


  /**
   * Get a list of the registered messages.
   * @return messages
   */
  public Message[] getRegisteredMessages() {
    return (Message[])message_registrations.values().toArray( new Message[0] );
  }

  
}
