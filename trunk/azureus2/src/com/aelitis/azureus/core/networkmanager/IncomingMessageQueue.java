/*
 * Created on Oct 17, 2004
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

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.peermanager.messaging.*;



/**
 * Inbound peer message queue.
 */
public class IncomingMessageQueue {
  
  private volatile ArrayList listeners = new ArrayList();  //copy-on-write
  private final AEMonitor listeners_mon = new AEMonitor( "IncomingMessageQueue:listeners" );

  private MessageStreamDecoder stream_decoder;
  private final NetworkConnection connection;

  
  /**
   * Create a new incoming message queue.
   * @param stream_decoder default message stream decoder
   * @param connection owner to read from
   */
  public IncomingMessageQueue( MessageStreamDecoder stream_decoder, NetworkConnection connection ) {
    this.connection = connection;
    this.stream_decoder = stream_decoder;
  }
  
  
  /**
   * Set the message stream decoder that will be used to decode incoming messages.
   * @param new_stream_decoder to use
   */
  public void setDecoder( MessageStreamDecoder new_stream_decoder ) {
    ByteBuffer already_read = stream_decoder.destroy();
    connection.getTCPTransport().setAlreadyRead( already_read );
    stream_decoder = new_stream_decoder;
    stream_decoder.resumeDecoding();
  }
  
  
  
  /**
   * Get the percentage of the current message that has already been received.
   * @return percentage complete (0-99), or -1 if no message is currently being received
   */
  public int getPercentDoneOfCurrentMessage() {
    return stream_decoder.getPercentDoneOfCurrentMessage();
  }
  
  
  
  /**
   * Receive (read) message(s) data from the underlying transport.
   * @param max_bytes to read
   * @return number of bytes received
   * @throws IOException on receive error
   */
  public int receiveFromTransport( int max_bytes ) throws IOException {
    if( max_bytes < 1 ) {
      Debug.out( "max_bytes < 1: " +max_bytes );
      return 0;
    }
    
    if( listeners.isEmpty() ) {
      Debug.out( "no queue listeners registered!" );
      throw new IOException( "no queue listeners registered!" );
    }
    
    //perform decode op
    int bytes_read = stream_decoder.performStreamDecode( connection.getTCPTransport(), max_bytes );
    
    //check if anything was decoded and notify listeners if so
    Message[] messages = stream_decoder.removeDecodedMessages();
    if( messages != null ) {
      for( int i=0; i < messages.length; i++ ) {
        Message msg = messages[ i ];
        
        if( msg == null ) {
        	System.out.println( "received msg == null [messages.length=" +messages.length+ ", #" +i+ "]: " +connection.getTCPTransport().getDescription() );
        	continue;
        }
        
        ArrayList listeners_ref = listeners;  //copy-on-write
        boolean handled = false;
        
        for( int x=0; x < listeners_ref.size(); x++ ) {
          MessageQueueListener mql = (MessageQueueListener)listeners_ref.get( x );
          handled = handled || mql.messageReceived( msg );
        }
        
        if( !handled ) {
          if( listeners_ref.size() > 0 ) {
            System.out.println( "no registered listeners [out of " +listeners_ref.size()+ "] handled decoded message [" +msg.getDescription()+ "]" );
          }
          
          DirectByteBuffer[] buffs = msg.getData();
          for( int x=0; x < buffs.length; x++ ) {
            buffs[ x ].returnToPool();
          }
        }
      }
    }
    
    int protocol_read = stream_decoder.getProtocolBytesDecoded();
    if( protocol_read > 0 ) {
      ArrayList listeners_ref = listeners;  //copy-on-write
      for( int i=0; i < listeners_ref.size(); i++ ) {
        MessageQueueListener mql = (MessageQueueListener)listeners_ref.get( i );
        mql.protocolBytesReceived( protocol_read );
      }
    }
    
    int data_read = stream_decoder.getDataBytesDecoded();
    if( data_read > 0 ) {
      ArrayList listeners_ref = listeners;  //copy-on-write
      for( int i=0; i < listeners_ref.size(); i++ ) {
        MessageQueueListener mql = (MessageQueueListener)listeners_ref.get( i );
        mql.dataBytesReceived( data_read );
      }
    }
    
    return bytes_read;   
  }
  

  
  
  /**
   * Notifty the queue (and its listeners) of a message received externally on the queue's behalf.
   * @param message received externally
   */
  public void notifyOfExternallyReceivedMessage( Message message ) {
    ArrayList listeners_ref = listeners;  //copy-on-write
    boolean handled = false;

    DirectByteBuffer[] dbbs = message.getData();
    int size = 0;
    for( int i=0; i < dbbs.length; i++ ) {
      size += dbbs[i].remaining( DirectByteBuffer.SS_NET );
    }
    
    
    for( int x=0; x < listeners_ref.size(); x++ ) {
      MessageQueueListener mql = (MessageQueueListener)listeners_ref.get( x );
      handled = handled || mql.messageReceived( message );
      
      if( message.getType() == Message.TYPE_DATA_PAYLOAD ) {
        mql.dataBytesReceived( size );
      }
      else {
        mql.protocolBytesReceived( size );
      }
    }
    
    if( !handled ) {
      if( listeners_ref.size() > 0 ) {
        System.out.println( "no registered listeners [out of " +listeners_ref.size()+ "] handled decoded message [" +message.getDescription()+ "]" );
      }
      
      DirectByteBuffer[] buffs = message.getData();
      for( int x=0; x < buffs.length; x++ ) {
        buffs[ x ].returnToPool();
      }
    }
  }
  
 
  
  /**
   * Manually resume processing (reading) incoming messages.
   * NOTE: Allows us to resume docoding externally, in case it was auto-paused internally.
   */
  public void resumeQueueProcessing() {
    stream_decoder.resumeDecoding();
  }
  

  
  /**
   * Add a listener to be notified of queue events.
   * @param listener
   */
  public void registerQueueListener( MessageQueueListener listener ) {
    try{  listeners_mon.enter();
      //copy-on-write
      ArrayList new_list = new ArrayList( listeners.size() + 1 );
      new_list.addAll( listeners );
      new_list.add( listener );
      listeners = new_list;
    }
    finally{  listeners_mon.exit();  }
  }
  
  
  /**
   * Cancel queue event notification listener.
   * @param listener
   */
  public void cancelQueueListener( MessageQueueListener listener ) {
    try{  listeners_mon.enter();
      //copy-on-write
      ArrayList new_list = new ArrayList( listeners );
      new_list.remove( listener );
      listeners = new_list;
    }
    finally{  listeners_mon.exit();  }
  }
  
  
  
  
  /**
   * Destroy this queue.
   */
  public void destroy() {
    stream_decoder.destroy();
  }
  
  

  
  /**
   * For notification of queue events.
   */
  public interface MessageQueueListener {
    /**
     * A message has been read from the connection.
     * @param message recevied
     * @return true if this message was accepted, false if not handled
     */
    public boolean messageReceived( Message message );
    
    /**
     * The given number of protocol (overhead) bytes read from the connection.
     * @param byte_count number of protocol bytes
     */
    public void protocolBytesReceived( int byte_count );
    
    /**
     * The given number of (piece) data bytes read from the connection.
     * @param byte_count number of data bytes
     */
    public void dataBytesReceived( int byte_count );
    
  }
  
  
}
