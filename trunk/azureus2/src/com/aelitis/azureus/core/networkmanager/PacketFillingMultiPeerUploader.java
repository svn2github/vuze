/*
 * Created on Sep 28, 2004
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
import com.aelitis.azureus.core.peermanager.messages.ProtocolMessage;



/**
 * A rate-controlled write entity backed by multiple peer connections, with an
 * emphasis on transmitting packets with full payloads, i.e. it writes to the
 * transport in mss-sized chunks if at all possible. It also employs fair,
 * round-robin write scheduling, where connections each take turns writing a
 * single full packet per round.
 */
public class PacketFillingMultiPeerUploader implements RateControlledWriteEntity {
  private final RateHandler rate_handler;
  private boolean destroyed = false;
  
  private static final int FLUSH_CHECK_LOOP_TIME = 100;  //100ms
  private static final int FLUSH_WAIT_TIME = 3*1000;  //3sec no-new-data wait before forcing write flush
  
  private final HashMap stalled_connections = new HashMap();
  private final HashMap waiting_connections = new HashMap();
  private final LinkedList ready_connections = new LinkedList();
  private final AEMonitor lists_lock = new AEMonitor( "PacketFillingMultiPeerUploader:lists_lock" );
  

  /**
   * Create a new packet-filling multi-peer upload entity,
   * rate-controlled by the given handler.
   * @param rate_handler listener to handle upload rate limits
   */
  public PacketFillingMultiPeerUploader( RateHandler rate_handler ) {
    this.rate_handler = rate_handler;
    
    Thread flush_checker = new AEThread( "PacketFillingMultiPeerUploader:FlushChecker" ) {
      public void runSupport() {
        flushCheckLoop();
      }
    };
    flush_checker.setDaemon( true );
    flush_checker.start();
  }
  
  
  /**
   * Checks the connections in the waiting list to see if it's time to be force-flushed.
   */
  private void flushCheckLoop() {
    while( !destroyed ) {
      long current_time = SystemTime.getCurrentTime();
      
      try {
        lists_lock.enter();
        
        for( Iterator i = waiting_connections.entrySet().iterator(); i.hasNext(); ) {
          Map.Entry entry = (Map.Entry)i.next();
          PeerData peer_data = (PeerData)entry.getValue();
          if( current_time - peer_data.last_message_added_time > FLUSH_WAIT_TIME ) {  //time to force flush
            Connection conn = (Connection)entry.getKey();
            conn.getOutgoingMessageQueue().cancelQueueListener( peer_data.queue_listener ); //cancel the listener
            i.remove();  //remove from the waiting list
            addToReadyList( conn );
          }
        }
      }
      finally {
        lists_lock.exit();
      }
      
      try {
        Thread.sleep( FLUSH_CHECK_LOOP_TIME );
      }
      catch( Exception e ) {  Debug.printStackTrace( e );  }
    }
  }
  
  
  
  /**
   * Destroy this upload entity.
   * Note: Removes all peer connections in the process.
   */
  public void destroy() {
    destroyed = true;
    
    try {
      lists_lock.enter();
      
      //remove and cancel all connections
      
      for( Iterator i = stalled_connections.entrySet().iterator(); i.hasNext(); ) {
        Map.Entry entry = (Map.Entry)i.next();
        Connection conn = (Connection)entry.getKey();
        OutgoingMessageQueue.MessageQueueListener listener = (OutgoingMessageQueue.MessageQueueListener)entry.getValue();
        conn.getOutgoingMessageQueue().cancelQueueListener( listener );
      }
      stalled_connections.clear();
      
      for( Iterator i = waiting_connections.entrySet().iterator(); i.hasNext(); ) {
        Map.Entry entry = (Map.Entry)i.next();
        Connection conn = (Connection)entry.getKey();
        PeerData data = (PeerData)entry.getValue();
        conn.getOutgoingMessageQueue().cancelQueueListener( data.queue_listener );
      }
      waiting_connections.clear();
      
      for( Iterator i = ready_connections.iterator(); i.hasNext(); ) {
        PeerData peer_data = (PeerData)i.next();
        peer_data.connection.getOutgoingMessageQueue().cancelQueueListener( peer_data.queue_listener );
      }
      ready_connections.clear();
    }
    finally {
      lists_lock.exit();
    }
  }
  

  
  
  /**
   * Add the given connection to be managed by this upload entity.
   * @param peer_connection to be write managed
   */
  public void addPeerConnection( Connection peer_connection ) {    
    int num_bytes_ready = peer_connection.getOutgoingMessageQueue().getTotalSize();

    if( num_bytes_ready < 1 ) { //no data to send
      addToStalledList( peer_connection );
      return;
    }
    
    boolean has_urgent_data = peer_connection.getOutgoingMessageQueue().hasUrgentMessage();
    int mss_size = NetworkManager.getSingleton().getTcpMssSize();
    
    if( num_bytes_ready >= mss_size || has_urgent_data ) {  //has a full packet's worth, or has urgent data
      addToReadyList( peer_connection );
    }
    else {   //has data to send, but not enough for a full packet
      addToWaitingList( peer_connection );
    }
  }
  
  
  /**
   * Remove the given connection from this upload entity.
   * @param peer_connection to be removed
   */
  public void removePeerConnection( Connection peer_connection ) {
    try {
      lists_lock.enter();
      
      //look for the connection in the stalled list and cancel listener if found
      OutgoingMessageQueue.MessageQueueListener listener = (OutgoingMessageQueue.MessageQueueListener)stalled_connections.remove( peer_connection );
      if( listener != null ) {
        peer_connection.getOutgoingMessageQueue().cancelQueueListener( listener );
        return;
      }
      
      //look for the connection in the waiting list and cancel listener if found
      PeerData peer_data = (PeerData)waiting_connections.remove( peer_connection );
      if( peer_data != null ) {
        peer_connection.getOutgoingMessageQueue().cancelQueueListener( peer_data.queue_listener );
        return;
      }
      
      //look for the connection in the ready list, remove and cancel listener if found
      for( Iterator i = ready_connections.iterator(); i.hasNext(); ) {
        peer_data = (PeerData)i.next();
        if( peer_data.connection == peer_connection ) {  //found
          peer_connection.getOutgoingMessageQueue().cancelQueueListener( peer_data.queue_listener );
          i.remove();
          return;
        }
      }
    }
    finally {
      lists_lock.exit();
    }
  }
  
  
  
  //connections with zero data to write
  private void addToStalledList( final Connection conn ) {
    OutgoingMessageQueue.MessageQueueListener listener = new OutgoingMessageQueue.MessageQueueListener() {
      public void messageAdded( ProtocolMessage message ) {  //connection now has data to send
        try {
          lists_lock.enter();
          
          Object removed = stalled_connections.remove( conn );  //remove from stalled list
          if( removed == null ) {  //connection has already been removed from the stalled list
            return;  //stop further processing
          }
          
          conn.getOutgoingMessageQueue().cancelQueueListener( this ); //cancel this listener
          
          int num_bytes_ready = conn.getOutgoingMessageQueue().getTotalSize();
          boolean has_urgent_data = conn.getOutgoingMessageQueue().hasUrgentMessage();
          int mss_size = NetworkManager.getSingleton().getTcpMssSize();
          
          if( num_bytes_ready >= mss_size || has_urgent_data ) {  //has a full packet's worth, or has urgent data
            addToReadyList( conn );
          }
          else {  //has data to send, but not enough for a full packet
            addToWaitingList( conn );
          }
        }
        finally {
          lists_lock.exit();
        }
      }

      public void messageRemoved( ProtocolMessage message ) {/*ignore*/}
      public void messageSent( ProtocolMessage message ) {/*ignore*/}
      public void bytesSent( int byte_count ) {/*ignore*/}
    };
    
    try {
      lists_lock.enter();
      
      stalled_connections.put( conn, listener ); //add to stalled list
      conn.getOutgoingMessageQueue().registerQueueListener( listener );  //listen for added data
    }
    finally {
      lists_lock.exit();
    }
  }
  
  
  //connections with less than a packet's worth of data
  private void addToWaitingList( final Connection conn ) {
    final PeerData peer_data = new PeerData();
    
    OutgoingMessageQueue.MessageQueueListener listener = new OutgoingMessageQueue.MessageQueueListener() {
      public void messageAdded( ProtocolMessage message ) {  //connection now has more data to send
        try {
          lists_lock.enter();
          
          Object contains = waiting_connections.get( conn );
          if( contains == null ) {  //connection has already been removed from the waiting list
            return;  //stop further processing
          }
          
          int num_bytes_ready = conn.getOutgoingMessageQueue().getTotalSize();
          boolean has_urgent_data = conn.getOutgoingMessageQueue().hasUrgentMessage();
          int mss_size = NetworkManager.getSingleton().getTcpMssSize();
        
          if( num_bytes_ready >= mss_size || has_urgent_data ) {  //has a full packet's worth, or has urgent data
            Object removed = waiting_connections.remove( conn );  //remove from waiting list
            if( removed == null ) {  //connection has already been removed from the waiting list
              System.out.println( "waiting_connections.remove0 gave null" );
              return;  //stop further processing
            }
            
            conn.getOutgoingMessageQueue().cancelQueueListener( this ); //cancel this listener
            addToReadyList( conn );
          }
          else {  //still not enough data for a full packet
            peer_data.last_message_added_time = SystemTime.getCurrentTime();  //update last message added time
          }
        }
        finally {
          lists_lock.exit();
        }
      }

      public void messageRemoved( ProtocolMessage message ) { //connection now has less data to send
        try {
          lists_lock.enter();
          
          int num_bytes_ready = conn.getOutgoingMessageQueue().getTotalSize();
          if( num_bytes_ready < 1 ) {  //no data left to send
            Object removed = waiting_connections.remove( conn );  //remove from waiting list
            if( removed == null ) {  //connection has already been removed from the waiting list
              System.out.println( "waiting_connections.remove1 gave null" );
              return;  //stop further processing
            }
            
            conn.getOutgoingMessageQueue().cancelQueueListener( this ); //cancel this listener
            addToStalledList( conn );
          }
        }
        finally {
          lists_lock.exit();
        }
      }
      
      public void messageSent( ProtocolMessage message ) {/*ignore*/}
      public void bytesSent( int byte_count ) {/*ignore*/}
    };
    
    peer_data.queue_listener = listener;  //attach listener
    peer_data.last_message_added_time = SystemTime.getCurrentTime(); //start flush wait time
    
    try {
      lists_lock.enter();
      
      waiting_connections.put( conn, peer_data ); //add to waiting list
      conn.getOutgoingMessageQueue().registerQueueListener( listener );  //listen for added data
    }
    finally {
      lists_lock.exit();
    }
  }
  
  
  
  //connections ready to write
  private void addToReadyList( final Connection conn ) {
    final PeerData peer_data = new PeerData();
    
    OutgoingMessageQueue.MessageQueueListener listener = new OutgoingMessageQueue.MessageQueueListener() {
      public void messageRemoved( ProtocolMessage message ) { //connection now has less data to send
        try {
          lists_lock.enter();
          
          int num_bytes_ready = conn.getOutgoingMessageQueue().getTotalSize();
          boolean has_urgent_data = conn.getOutgoingMessageQueue().hasUrgentMessage();
          int mss_size = NetworkManager.getSingleton().getTcpMssSize();
          
          if( num_bytes_ready >= mss_size || has_urgent_data ) {
            return;  //do nothing, as it still has a full packet's worth, or has urgent data
          }
          
          //connection does not have enough for a full packet, so remove and place into proper list
          boolean removed = ready_connections.remove( peer_data );  //remove from ready list
          if( !removed ) {  //connection has already been removed from the ready list
            return;  //stop further processing
          }
          conn.getOutgoingMessageQueue().cancelQueueListener( this ); //cancel this listener
          
          if( num_bytes_ready < 1 ) {  //no data at all left to send
            addToStalledList( conn );
          }
          else {  //wait to send leftover data
            addToWaitingList( conn );
          }
        }
        finally {
          lists_lock.exit();
        }
      }
      
      public void messageAdded( ProtocolMessage message ) {/*ignore*/}
      public void messageSent( ProtocolMessage message ) {/*ignore*/}
      public void bytesSent( int byte_count ) {/*ignore*/}
    };
    
    peer_data.connection = conn;
    peer_data.queue_listener = listener;
    
    try {
      lists_lock.enter();
      
      ready_connections.addLast( peer_data );  //add to ready list
      conn.getOutgoingMessageQueue().registerQueueListener( listener );  //listen for added data
    }
    finally {
      lists_lock.exit();
    }
  }
  
  
  
  private int write( int num_bytes_to_write ) {    
    int mss_size = NetworkManager.getSingleton().getTcpMssSize();
    if( num_bytes_to_write < mss_size ) {
      return 0;  //don't bother doing a write if we're not allowed to send at least a full packet
    }
    
    ArrayList connections_to_notify_of_exception = new ArrayList();
    ArrayList manual_notifications = new ArrayList();
    
    int num_bytes_remaining = num_bytes_to_write;    
    
    try {
      lists_lock.enter();
      
      int num_unusable_connections = 0;
      
      while( num_bytes_remaining > 0 && num_unusable_connections < ready_connections.size() ) {
        PeerData peer_data = (PeerData)ready_connections.removeFirst();
        
        if( !peer_data.connection.getTransport().isReadyForWrite() ) {  //not yet ready for writing
          ready_connections.addLast( peer_data );  //re-add to end as currently unusable
          num_unusable_connections++;
          continue;  //move on to the next connection
        }
        
        int num_bytes_allowed = num_bytes_remaining > mss_size ? mss_size : num_bytes_remaining;  //allow a single full packet at most
        int total_size = peer_data.connection.getOutgoingMessageQueue().getTotalSize();
        int num_bytes_available = total_size > mss_size ? mss_size : total_size;  //allow a single full packet at most
        
        if( num_bytes_allowed >= num_bytes_available ) { //we're allowed enough (for either a full packet or to drain any remaining data)
          int written = 0;
          try {
            written = peer_data.connection.getOutgoingMessageQueue().deliverToTransport( peer_data.connection.getTransport(), num_bytes_available, true );
            
            if( written > 0 ) {  //register it for manual listener notification
              manual_notifications.add( peer_data );
            }
            
            int remaining = peer_data.connection.getOutgoingMessageQueue().getTotalSize();
            boolean has_urgent_data = peer_data.connection.getOutgoingMessageQueue().hasUrgentMessage();
            
            if( remaining >= mss_size || has_urgent_data ) {  //still has a full packet's worth, or has urgent data
              ready_connections.addLast( peer_data );  //re-add to end for further writing
              num_unusable_connections = 0;  //reset the unusable count so that it has a chance to try this connection again in the loop
            }
            else {  //connection does not have enough for a full packet, so remove and place into proper list
              peer_data.connection.getOutgoingMessageQueue().cancelQueueListener( peer_data.queue_listener ); //cancel the listener
              
              if( remaining < 1 ) {  //no data at all left to send
                addToStalledList( peer_data.connection );
              }
              else {  //wait to send leftover data
                addToWaitingList( peer_data.connection );
              }
            }
          }
          catch( IOException e ) {  //write exception, so completely remove from this upload entity, as no further writes are possible
            peer_data.connection.getOutgoingMessageQueue().cancelQueueListener( peer_data.queue_listener ); //cancel the listener
            peer_data.exception = e;
            connections_to_notify_of_exception.add( peer_data );  //do exception notification outside of sync'd block
          }
          
          num_bytes_remaining -= written;
        }
        else {  //we're not allowed enough to maximize the packet payload
          ready_connections.addLast( peer_data );  //re-add to end as currently unusable
          num_unusable_connections++;
          continue;  //move on to the next connection
        }
      }
    }
    finally {
      lists_lock.exit();
    }
    
    //manual queue listener notifications
    for( int i=0; i < manual_notifications.size(); i++ ) {
      PeerData peer_data = (PeerData)manual_notifications.get( i );
      peer_data.connection.getOutgoingMessageQueue().doListenerNotifications();
    }
    
    //exception notifications
    for( int i=0; i < connections_to_notify_of_exception.size(); i++ ) {
      PeerData peer_data = (PeerData)connections_to_notify_of_exception.get( i );
      peer_data.connection.notifyOfException( peer_data.exception );
    }
    
    int num_bytes_written = num_bytes_to_write - num_bytes_remaining;
    if( num_bytes_written > 0 )  rate_handler.bytesWritten( num_bytes_written );
    return num_bytes_written;
  }
  
  
  
  private static class PeerData {
    private Connection connection;
    private OutgoingMessageQueue.MessageQueueListener queue_listener;
    private long last_message_added_time;
    private IOException exception;
    
    private PeerData() {/*nothing*/}
  }
  
  
  //////////////// RateControlledWriteEntity implementation ////////////////////
  
  public boolean doWrite() {
    int written = write( rate_handler.getCurrentNumBytesAllowed() );
    return written > 0 ? true : false;
  }

 ///////////////////////////////////////////////////////////////////////////////
  
  
  /**
   * Handler to allow external control of the write rate.
   */
  public interface RateHandler {
    /**
     * Get the current number of bytes allowed to be written by the entity.
     * @return number of bytes allowed
     */
    public int getCurrentNumBytesAllowed();
    
    /**
     * Notification of any bytes written from the entity.
     * @param num_bytes_written 
     */
    public void bytesWritten( int num_bytes_written );
  }

  
}
