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

import java.nio.ByteBuffer;
import java.util.*;

import org.gudy.azureus2.core3.util.*;




/**
 *
 */
public class MessageManager {
  private static final MessageManager instance = new MessageManager();
  
  private final Map message_map = new HashMap();
  private final AEMonitor message_map_mon = new AEMonitor( "message_map" );
  
  private DirectByteBuffer message_list_payload;
  private boolean message_list_payload_dirty = true;
  
  
  private MessageManager() {
    /* nothing */
  }
  
  public static MessageManager getSingleton() {  return instance;  }
  
  

  public void registerMessage( Message message ) throws MessageException {
    MessageData md = new MessageData( message );
    
    try {  message_map_mon.enter();
    
      if( message_map.containsKey( md ) ) {
        throw new MessageException( "Message type [" +message.getID()+ ", v" +message.getVersion()+ "] already registered." );
      }
      
      message_map.put( md, null );
      message_list_payload_dirty = true;
      
    } finally {  message_map_mon.exit();  }
  }
  

  public void unregisterMessage( Message message ) throws MessageException {
    MessageData md = new MessageData( message );
    
    try {  message_map_mon.enter();
    
      Object result = message_map.remove( md );
      
      if( result == null ) {
        throw new MessageException( "Message type [" +message.getID()+ ", v" +message.getVersion()+ "] not registered." );
      }
      
      message_list_payload_dirty = true;
    
    } finally {  message_map_mon.exit();  }
  }
  

  

  
  
  
  private DirectByteBuffer constructMessageListPayload() {
    Map payload_map = new HashMap();
    DirectByteBuffer payload_data = null;
    
    try {  message_map_mon.enter();
      List message_list = new ArrayList();
      int value = 1;
      
      for( Iterator i = message_map.keySet().iterator(); i.hasNext(); ) {
        MessageData md = (MessageData)i.next();
        
        Map message = new HashMap();
        
        message.put( "id", md.id );
        message.put( "version", new Long( md.version ) );
        message.put( "value", new Long( value ) );
        
        //TODO store value-message key
        value++;
        
        message_list.add( message );
      }
      
      payload_map.put( "messages", message_list );
    
      message_list_payload_dirty = false;
    
    } finally {  message_map_mon.exit();  }
    
    try { 
      payload_data = new DirectByteBuffer( ByteBuffer.wrap( BEncoder.encode( payload_map ) ) );
    }
    catch( Throwable t ) {  t.printStackTrace();  }
    
    return payload_data;
  }
  
  
  
  
  
  private static class MessageData {
    private final Message message;
    private final String id;
    private final int version;
    private final int hashcode;
    
    private MessageData( Message message ) {
      this.message = message;
      this.id = message.getID();
      this.version = message.getVersion();
      hashcode = id.hashCode() + version;
    }
    
    
    public boolean equals( Object obj ) {
      if( this == obj )  return true;
      if( obj != null && obj instanceof MessageData ) {
        MessageData other = (MessageData)obj;
        if( this.version == other.version && this.id.equals( other.id ) ) {
          return true;
        }
      }
      return false;
    }
    
    public int hashCode() {  return hashcode;  }
    
  }
  
}
