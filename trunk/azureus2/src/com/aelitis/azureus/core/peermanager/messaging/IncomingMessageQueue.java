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

package com.aelitis.azureus.core.peermanager.messaging;

import java.io.IOException;
import java.util.*;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.networkmanager.TCPTransport;



/**
 * Inbound peer message queue.
 */
public class IncomingMessageQueue {
  
  private volatile ArrayList listeners = new ArrayList();  //copy-on-write
  private final AEMonitor listeners_mon = new AEMonitor( "IncomingMessageQueue" );
  
  private final ProcessingHandler processing_handler;
  private boolean is_processing_enabled = false;
  
  private MessageStreamDecoder stream_decoder;
  
  private final MessageStreamDecoder.DecodeListener decode_listener = new MessageStreamDecoder.DecodeListener() {
    public void messageDecoded( Message message ) {
      ArrayList listeners_ref = listeners;  //copy-on-write
      boolean handled = false;
      
      for( int i=0; i < listeners_ref.size(); i++ ) {
        MessageQueueListener mql = (MessageQueueListener)listeners_ref.get( i );
        handled = handled || mql.messageReceived( message );
      }
      
      if( !handled ) {  //this should never happen
        Debug.out( "no registered listeners handled decoded message!" );
        DirectByteBuffer[] buffs = message.getData();
        for( int i=0; i < buffs.length; i++ ) {
          buffs[ i ].returnToPool();
        }
      }
    }
    
    public void protocolBytesDecoded( int byte_count ) {
      ArrayList listeners_ref = listeners;  //copy-on-write
      for( int i=0; i < listeners_ref.size(); i++ ) {
        MessageQueueListener mql = (MessageQueueListener)listeners_ref.get( i );
        mql.protocolBytesReceived( byte_count );
      }
    }
    
    public void dataBytesDecoded( int byte_count ) {
      ArrayList listeners_ref = listeners;  //copy-on-write
      for( int i=0; i < listeners_ref.size(); i++ ) {
        MessageQueueListener mql = (MessageQueueListener)listeners_ref.get( i );
        mql.dataBytesReceived( byte_count );
      }
    }
  };
  
  
  
  /**
   * Create a new message queue.
   */
  public IncomingMessageQueue( ProcessingHandler handler ) {
    this.processing_handler = handler;
    
    //TODO check for decoder type
    stream_decoder = new LegacyMessageDecoder( decode_listener );
  }
  
  
  
  /**
   * Receive (read) message(s) data from the given transport.
   * @param transport to receive from
   * @param max_bytes to read
   * @return number of bytes received
   * @throws IOException on receive error
   */
  public int receiveFromTransport( TCPTransport transport, int max_bytes ) throws IOException {
    if( max_bytes < 1 ) {
      Debug.out( "max_bytes < 1: " +max_bytes );
      return 0;
    }
    
    if( listeners.isEmpty() ) {
      Debug.out( "no queue listeners registered!" );
      return 0;
    }
    
    return stream_decoder.decodeStream( transport, max_bytes );    
  }
  
  
  
  /**
   * Start processing (reading) incoming messages.
   */
  public void startQueueProcessing() {
    if( !is_processing_enabled ) {
      processing_handler.enableProcessing();
      is_processing_enabled = true;
    }
  }
  
  
  /**
   * Stop processing (reading) incoming messages.
   */
  public void stopQueueProcessing() {
    if( is_processing_enabled ) {
      processing_handler.disableProcessing();
      is_processing_enabled = false;
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
    stream_decoder.destroy();
  }
  
  
  
  
  public interface ProcessingHandler {
    public void enableProcessing();
    
    public void disableProcessing();
    
    
    
  }
  

  
  
  
  /**
   * For notification of queue events.
   */
  public interface MessageQueueListener {
    /**
     * A message has been read from the transport.
     * @param message recevied
     * @return true if this message was accepted, false if not handled
     */
    public boolean messageReceived( Message message );
    
    /**
     * The given number of protocol (overhead) bytes read from the transport.
     * @param byte_count number of protocol bytes
     */
    public void protocolBytesReceived( int byte_count );
    
    /**
     * The given number of (piece) data bytes read from the transport.
     * @param byte_count number of data bytes
     */
    public void dataBytesReceived( int byte_count );
    
  }
  
  
}
