/*
 * Created on Jul 29, 2004
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

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.peermanager.messages.ProtocolMessage;


/**
 * Represents a managed peer connection,
 * over which protocol messages can be sent and received.
 */
public class Connection {
  private final InetSocketAddress remote_address;
  private final Transport transport;
  private ConnectionListener connection_listener;
  private final OutgoingMessageQueue outgoing_message_queue = new OutgoingMessageQueue();
  private boolean is_connected;
  
  private final OutgoingMessageQueue.AddedMessageListener added_write_message_listener = new OutgoingMessageQueue.AddedMessageListener() {
    public void messageAdded( ProtocolMessage message ) {
      last_new_write_data_added_time = SystemTime.getCurrentTime();
    }
  };
  
  
  /**
   * Constructor for new OUTbound connection.
   * The connection is not yet established upon instantiation; use connect() to do so.
   * @param remote_address to connect to
   */
  protected Connection( InetSocketAddress remote_address ) {
    this.remote_address = remote_address;
    transport = new Transport();
    outgoing_message_queue.registerAddedListener( added_write_message_listener );
    is_connected = false;
  }
  
  
  /**
   * Constructor for new INbound connection.
   * The connection is assumed to be already established, by the given already-connected channel.
   * @param remote_channel connected by
   */
  protected Connection( SocketChannel remote_channel ) {
    remote_address = new InetSocketAddress( remote_channel.socket().getInetAddress(), remote_channel.socket().getPort() );
    transport = new Transport( remote_channel );
    outgoing_message_queue.registerAddedListener( added_write_message_listener );
    is_connected = true;
  }
  
  
  /**
   * Connect this connection's transport, i.e. establish the peer connection.
   * If this connection is already established (from an incoming connection for example),
   * then this provides a mechanism to register the connection listener, in which case
   * connectSuccess() will be called immediately.
   * @param listener notified on connect success or failure
   */
  public void connect( ConnectionListener listener ) {
    this.connection_listener = listener;
    
    if( is_connected ) {
      connection_listener.connectSuccess();
      return;
    }
    
    transport.establishOutboundConnection( remote_address, new Transport.ConnectListener() {
      public void connectSuccess() {
        is_connected = true;
        connection_listener.connectSuccess();
      }
      
      public void connectFailure( Throwable failure_msg ) {
        is_connected = false;
        connection_listener.connectFailure( failure_msg );
      }
    });
  }
  
  
  /**
   * Close and shutdown this connection.
   */
  public void close() {
    if( is_connected ) {
      outgoing_message_queue.destroy();
      transport.close();
      is_connected = false;
    }
  }
  
  
  /**
   * Inform connection of a thrown exception.
   * @param error exception
   */
  protected void notifyOfException( Throwable error ) {
    if( connection_listener != null ) {
      connection_listener.exceptionThrown( error );
    }
    else System.out.println( "connection_listener == null" );
  }
  
  
  
  
  /**
   * Get the connection's outgoing message queue.
   * @return outbound message queue
   */
  public OutgoingMessageQueue getOutgoingMessageQueue() {  return outgoing_message_queue;  }
  
  /**
   * Get the connection's data transport interface.
   * @return the transport
   */
  protected Transport getTransport() {  return transport;  }
  
  
  /**
   * TEMP METHOD UNTIL SOCKET READING IS HANDLED INTERNALLY
   */
  public SocketChannel getSocketChannel() {
    return transport.getSocketChannel();
  }
  
  
  
  
  ///////////////////////////////////////////////////////////
  public boolean equals( Object obj ) {
    if( this == obj )  return true;
    if( obj != null && obj instanceof Connection ) {
      Connection other = (Connection)obj;
      if( this.remote_address.equals( other.remote_address ) )  return true;
    }
    return false;
  }
  public int hashCode() {  return remote_address.hashCode();  }
  public String toString() {  return remote_address.toString();  }
  ////////////////////////////////////////////////////////////
  
  
  
  /**
   * Listener for notification of connection events.
   */
  public interface ConnectionListener {
    /**
     * The connection attempt succeeded.
     * The connection is now established.
     * NOTE: Called only during initial connect attempt.
     */
    public void connectSuccess();
    
    /**
     * The connection attempt failed.
     * NOTE: Called only during initial connect attempt.
     * @param failure_msg failure reason
     */
    public void connectFailure( Throwable failure_msg );
    
    /**
     * Handle exception thrown by this connection.
     * NOTE: Can be called at any time during connection lifetime.
     * @param error exception
     */
    public void exceptionThrown( Throwable error );
  }
  
  
  
  
  
  
  
///////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////


  
  private long last_new_write_data_added_time = 0;
  private boolean transport_ready_for_write = true;
  
  
  /**
   * Get the last time a new message (data) was added to the outbound queue.
   * @return time
   */
  protected long getLastNewWriteDataAddedTime() {  return last_new_write_data_added_time;  }
  
  /**
   * Is the underlying transport (socket) ready for a non-blocking write.
   * @return true if ready
   */
  protected boolean isTransportReadyForWrite() {  return transport_ready_for_write;  }
  
  /**
   * Set whether or not the underlying transport is ready for a non-blocking write.
   * @param is_ready
   */
  protected void setTransportReadyForWrite( boolean is_ready ) {  transport_ready_for_write = is_ready;  }
  
  

  
}
