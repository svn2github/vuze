/*
 * Created on May 8, 2004
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


package com.aelitis.azureus.core.networkmanager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.peermanager.messages.ProtocolMessage;


/**
 * Priority-based outbound peer message queue.
 */
public class OutgoingMessageQueue {
  private final List 		queue		= new LinkedList();
  private final AEMonitor	queue_mon	= new AEMonitor( "OutgoingMessageQueue:Q");

  private int total_size = 0;
  private final ArrayList add_listeners 		= new ArrayList();
  private final AEMonitor add_listeners_mon		= new AEMonitor( "OutgoingMessageQueue:AL");
  private final ArrayList sent_listeners 		= new ArrayList();
  private final AEMonitor sent_listeners_mon	= new AEMonitor( "OutgoingMessageQueue:SL");
  private final ArrayList byte_listeners		= new ArrayList();
  private final AEMonitor byte_listeners_mon	= new AEMonitor( "OutgoingMessageQueue:BL");
  private ProtocolMessage urgent_message = null;
  private volatile boolean destroyed = false;
  
  
  /**
   * Create a new message queue.
   */
  protected OutgoingMessageQueue() {
    //nothing
  }
  

  /**
   * Destroy this queue; i.e. perform cleanup actions.
   */
  protected void destroy() {
    destroyed = true;
    try{
      queue_mon.enter();
    
      while( !queue.isEmpty() ) {
      	((ProtocolMessage)queue.remove( 0 )).destroy();
      }
    }finally{
      queue_mon.exit();
    }
    total_size = 0;
  }
  
  
  /**
   * Get the total number of bytes ready to be transported.
   * @return total bytes remaining
   */
  public int getTotalSize() {  return total_size;  }
  
  
  /**
   * Whether or not an urgent message (one that needs an immediate send, i.e. a no-delay message) is queued.
   * @return true if there's a message tagged for immediate write
   */
  public boolean hasUrgentMessage() {  return urgent_message == null ? false : true;  }
  
  
  /**
   * Add a message to the message queue.
   * @param message message to add
   */
  public void addMessage( ProtocolMessage message ) {
    
    if( destroyed ) System.out.println("addMessage:: already destroyed");
    
    removeMessagesOfType( message.typesToRemove() );
    try{
      queue_mon.enter();
    
      int pos = 0;
      for( Iterator i = queue.iterator(); i.hasNext(); ) {
        ProtocolMessage msg = (ProtocolMessage)i.next();
        if( message.getPriority() > msg.getPriority() 
            && msg.getPayload().position(DirectByteBuffer.SS_NET) == 0 ) {  //but don't insert in front of a half-sent message
          break;
        }
        pos++;
      }
      if( message.isNoDelay() ) {
        urgent_message = message;
      }
      queue.add( pos, message );
      total_size += message.getPayload().remaining(DirectByteBuffer.SS_NET);
    }finally{
      queue_mon.exit();
    }
    notifyAddListeners( message );
  }
  
  
  /**
   * Remove all messages of the given types from the queue.
   * @param message_types type to remove
   */
  public void removeMessagesOfType( int[] message_types ) {
    if( message_types == null ) return;
    try{
      queue_mon.enter();
    
      for( Iterator i = queue.iterator(); i.hasNext(); ) {
        ProtocolMessage msg = (ProtocolMessage)i.next();
        for( int t=0; t < message_types.length; t++ ) {
        	if( msg.getType() == message_types[ t ] && msg.getPayload().position(DirectByteBuffer.SS_NET) == 0 ) {   //dont remove a half-sent message
            if( msg == urgent_message ) urgent_message = null;            
            total_size -= msg.getPayload().remaining(DirectByteBuffer.SS_NET);
            msg.destroy();
        		i.remove();
            break;
        	}
        }
      }
    }finally{
      queue_mon.exit();
    }
  }
  
  
  /**
   * Remove a particular message from the queue.
   * @param message
   * @return true if the message was removed, false otherwise
   */
  public boolean removeMessage( ProtocolMessage message ) {
    try{
      queue_mon.enter();
    
      int index = queue.indexOf( message );
      if( index != -1 ) {
        ProtocolMessage msg = (ProtocolMessage)queue.get( index );
        if( msg.getPayload().position(DirectByteBuffer.SS_NET) == 0 ) {  //dont remove a half-sent message
          if( msg == urgent_message ) urgent_message = null;  
          total_size -= msg.getPayload().remaining(DirectByteBuffer.SS_NET);
          msg.destroy();
          queue.remove( index );
          return true;
        }
      }
    }finally{
      queue_mon.exit();
    }
    return false;
  }
  
  
  /**
   * Deliver (write) message(s) data to the given transport.
   * @param transport to transmit over 
   * @param max_bytes maximum number of bytes to deliver
   * @return number of bytes delivered
   * @throws IOException
   */
  protected int deliverToTransport( Transport transport, int max_bytes ) throws IOException {    
    int written = 0;
    ArrayList messages_sent = new ArrayList();
    
    try{
      queue_mon.enter();
   
    	if( !queue.isEmpty() ) {
        ByteBuffer[] buffers = new ByteBuffer[ queue.size() ];
        int[] starting_pos = new int[ queue.size() ];
        int pos = 0;
    		int total_sofar = 0;
        while( total_sofar < max_bytes && pos < buffers.length ) {
          buffers[ pos ] = ((ProtocolMessage)queue.get( pos )).getPayload().getBuffer(DirectByteBuffer.SS_NET);
          total_sofar += buffers[ pos ].remaining();
          starting_pos[ pos ] = buffers[ pos ].position();
          pos++;
    		}
        pos--; //remove last while loop auto-increment
    		int orig_limit = buffers[ pos ].limit();
    		if( total_sofar > max_bytes ) {
    			buffers[ pos ].limit( orig_limit - (total_sofar - max_bytes) );
    		}
        written = new Long( transport.write( buffers, 0, pos + 1 ) ).intValue();
        buffers[ pos ].limit( orig_limit );
        pos = 0;
        while( !queue.isEmpty() ) {
          ProtocolMessage msg = (ProtocolMessage)queue.get( 0 );
          ByteBuffer bb = msg.getPayload().getBuffer(DirectByteBuffer.SS_NET);
          if( !bb.hasRemaining() ) {
            if( msg == urgent_message ) urgent_message = null;
            total_size -= bb.limit() - starting_pos[ pos ];
            queue.remove( 0 );
            LGLogger.log( LGLogger.CORE_NETWORK, "Sent " +msg.getDescription()+ " message to " + transport.getDescription() );
            messages_sent.add( msg );
          }
          else {
            total_size -= (bb.limit() - bb.remaining()) - starting_pos[ pos ];
            break;
          }
          pos++;
        }
    	}
    }finally{
      queue_mon.exit();
    }
    
    if( written > 0 ) {
      notifyByteListeners( written );
    }
    
    for( int i=0; i < messages_sent.size(); i++ ) {
      ProtocolMessage msg = (ProtocolMessage)messages_sent.get( i );
      notifySentListeners( msg );
      msg.destroy();
    }
    
    return written;
  }

  /////////////////////////////////////////////////////////////////
  
  /**
   * Receive notification when a new message is added to the queue.
   */
  public interface AddedMessageListener {
    /**
     * The given message has just been queued for sending out the transport.
     * @param message queued
     */
    public void messageAdded( ProtocolMessage message );
  }
  
  
  /**
   * Receive notification when a message has been transmitted.
   */
  public interface SentMessageListener {
    /**
     * The given message has been completely sent out through the transport.
     * @param message sent
     */
    public void messageSent( ProtocolMessage message );
  }
  
  
  /**
   * Receive notification when bytes are written to the transport.
   */
  public interface ByteListener {
    /**
     * The given number of bytes has been written to the transport.
     * @param byte_count number of bytes
     */
    public void bytesSent( int byte_count );
  }

  
  /**
   * Add a listener to be notified when a new message is added to the queue.
   * @param listener
   */
  public void registerAddedListener( AddedMessageListener listener ) {
    try{
      add_listeners_mon.enter();
    
      add_listeners.add( listener );
    }finally{
    
      add_listeners_mon.exit();
    }
  }
  
  
  /**
   * Add a listener to be notified when a message is sent.
   * @param listener
   */
  public void registerSentListener( SentMessageListener listener ) {
    try{
      sent_listeners_mon.enter();
   
      sent_listeners.add( listener );
      
    }finally{
    	
      sent_listeners_mon.exit();
    }
  }
  
  
  /**
   * Add a listener to be notified when bytes are sent.
   * @param listener
   */
  public void registerByteListener( ByteListener listener ) {
    try{
      byte_listeners_mon.enter();
    
      byte_listeners.add( listener );
    }finally{
      byte_listeners_mon.exit();
    }
  }
  
  
  private void notifyAddListeners( ProtocolMessage msg ) {
    ArrayList listeners;
    
    try{
      add_listeners_mon.enter();
      listeners = (ArrayList)add_listeners.clone();
    }
    finally{
      add_listeners_mon.exit();
    } 
    
    //notify outside the sync block using a copy of the listeners list to avoid potential deadlock
    for( int i=0; i < listeners.size(); i++ ) {
      AddedMessageListener listener = (AddedMessageListener)listeners.get( i );
      listener.messageAdded( msg );
    }
  }
  
  
  private void notifySentListeners( ProtocolMessage msg ) {
    ArrayList listeners;
    
    try{
      sent_listeners_mon.enter();
      listeners = (ArrayList)sent_listeners.clone();
    }
    finally{
      sent_listeners_mon.exit();
    } 
      
    for( int i=0; i < listeners.size(); i++ ) {
      SentMessageListener listener = (SentMessageListener)listeners.get( i );
      listener.messageSent( msg );
    }
  }
  
  
  private void notifyByteListeners( int byte_count ) {
    ArrayList listeners;

    try{
      byte_listeners_mon.enter();
      listeners = (ArrayList)byte_listeners.clone();
    }
    finally{
      byte_listeners_mon.exit();
    }
    
    for( int i=0; i < listeners.size(); i++ ) {
      ByteListener listener = (ByteListener)listeners.get( i );
      listener.bytesSent( byte_count );
    }
  }
}
