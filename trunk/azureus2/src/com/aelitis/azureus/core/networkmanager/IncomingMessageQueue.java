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
import java.util.*;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.peermanager.messaging.*;



/**
 * Inbound peer message queue.
 */
public class IncomingMessageQueue {
  
  private volatile ArrayList listeners = new ArrayList();  //copy-on-write
  private final AEMonitor listeners_mon = new AEMonitor( "IncomingMessageQueue" );

  private boolean is_processing_enabled = false;
  private MessageStreamDecoder stream_decoder;
  private final Connection connection;
  
  
  
  
  /**
   * Create a new incoming message queue.
   * @param connection owner to read from
   * @param stream_decoder default message stream decoder
   */
  public IncomingMessageQueue( Connection connection, MessageStreamDecoder stream_decoder ) {
    this.connection = connection;
    this.stream_decoder = stream_decoder;
  }
  
  
  /**
   * Set the message stream decoder that will be used to decode incoming messages.
   * @param stream_decoder to use
   */
  public void setDecoder( MessageStreamDecoder stream_decoder ) {
    this.stream_decoder = stream_decoder;
  }
  
  
  
  /**
   * Receive (read) message(s) data from the underlying transport.
   * @param max_bytes to read
   * @return number of bytes received
   * @throws IOException on receive error
   */
  private int receiveFromTransport( int max_bytes ) throws IOException {
    if( max_bytes < 1 ) {
      Debug.out( "max_bytes < 1: " +max_bytes );
      return 0;
    }
    
    if( listeners.isEmpty() ) {
      Debug.out( "no queue listeners registered!" );
      return 0;
    }
    
    //perform decode op
    int bytes_read = stream_decoder.performStreamDecode( connection.getTCPTransport(), max_bytes );
    
    //check if anything was decoded and notify listeners if so
    Message[] messages = stream_decoder.getDecodedMessages();
    if( messages != null ) {
      for( int i=0; i < messages.length; i++ ) {
        Message msg = messages[ i ];
        
        ArrayList listeners_ref = listeners;  //copy-on-write
        boolean handled = false;
        
        for( int x=0; x < listeners_ref.size(); x++ ) {
          MessageQueueListener mql = (MessageQueueListener)listeners_ref.get( x );
          handled = handled || mql.messageReceived( msg );
        }
        
        if( !handled ) {  //this should not happen
          Debug.out( "no registered listeners [out of " +listeners_ref.size()+ "] handled decoded message [" +msg.getDescription()+ "]" );
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
   * Start processing (reading) incoming messages.
   */
  public void startQueueProcessing() {
    if( !is_processing_enabled ) {
      connection.getTCPTransport().requestReadSelects( new TCPTransport.ReadListener() {
        public void readyToRead() {
          if( !is_processing_enabled )  return;
          
          try {
            receiveFromTransport( 1024*1024 );  //TODO do limited rate read op
          }
          catch( Throwable e ) {
            if( e.getMessage() == null ) {
              Debug.out( "null read exception message: ", e );
            }
            else {
              if( e.getMessage().indexOf( "end of stream on socket read" ) == -1 &&
                  e.getMessage().indexOf( "An existing connection was forcibly closed by the remote host" ) == -1 &&
                  e.getMessage().indexOf( "An established connection was aborted by the software in your host machine" ) == -1 ) {
                
                System.out.println( "read exception [" +connection.getTCPTransport().getDescription()+ "]: " +e.getMessage() );
              }
            }
              
            connection.notifyOfException( e );
          }
        }
      });
      
      is_processing_enabled = true;
    }
  }
  
  
  /**
   * Stop processing (reading) incoming messages.
   */
  public void stopQueueProcessing() {
    if( is_processing_enabled ) {
      is_processing_enabled = false;
      connection.getTCPTransport().cancelReadSelects();
    }
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
    is_processing_enabled = false;
    stream_decoder.destroy();
    listeners.clear();
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
