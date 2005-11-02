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

package com.aelitis.azureus.core.networkmanager.impl;


import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.gudy.azureus2.core3.util.Debug;


import com.aelitis.azureus.core.networkmanager.*;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamDecoder;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamEncoder;



/**
 *
 */
public class NetworkConnectionImpl implements NetworkConnection {
  private final InetSocketAddress remote_address;
  private final TCPTransport tcp_transport;
  private ConnectionListener connection_listener;
  private boolean is_connected;

  private final OutgoingMessageQueue outgoing_message_queue;
  private final IncomingMessageQueue incoming_message_queue;
  
  
  
  
  
  /**
   * Constructor for new OUTbound connection.
   * The connection is not yet established upon instantiation; use connect() to do so.
   * @param _remote_address to connect to
   * @param encoder default message stream encoder to use for the outgoing queue
   * @param decoder default message stream decoder to use for the incoming queue
   */
  public NetworkConnectionImpl( InetSocketAddress _remote_address, MessageStreamEncoder encoder, MessageStreamDecoder decoder ) {
    remote_address = _remote_address;
    tcp_transport = TransportFactory.createTCPTransport();
    is_connected = false;
    outgoing_message_queue = new OutgoingMessageQueue( encoder, tcp_transport );
    incoming_message_queue = new IncomingMessageQueue( decoder, this );
  }
  
  
  /**
   * Constructor for new INbound connection.
   * The connection is assumed to be already established, by the given already-connected channel.
   * @param _remote_channel connected by
   * @param data_already_read bytestream already read during routing
   * @param encoder default message stream encoder to use for the outgoing queue
   * @param decoder default message stream decoder to use for the incoming queue
   */
  public NetworkConnectionImpl( SocketChannel _remote_channel, ByteBuffer data_already_read, MessageStreamEncoder encoder, MessageStreamDecoder decoder ) {
    remote_address = new InetSocketAddress( _remote_channel.socket().getInetAddress(), _remote_channel.socket().getPort() );
    tcp_transport = TransportFactory.createTCPTransport( _remote_channel, data_already_read );
    is_connected = true;
    outgoing_message_queue = new OutgoingMessageQueue( encoder, tcp_transport );
    incoming_message_queue = new IncomingMessageQueue( decoder, this );
  }
  

  
  public void connect( ConnectionListener listener ) {
    this.connection_listener = listener;
    
    if( is_connected ) {
      connection_listener.connectStarted();
      connection_listener.connectSuccess();
      return;
    }
    
    tcp_transport.establishOutboundConnection( remote_address, new TCPTransport.ConnectListener() {
      public void connectAttemptStarted() {
        connection_listener.connectStarted();
      }
      
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
  

  
  public void close() {
    NetworkManager.getSingleton().getUploadProcessor().deregisterPeerConnection( this );
    NetworkManager.getSingleton().getDownloadProcessor().deregisterPeerConnection( this );
    
    tcp_transport.close();
    incoming_message_queue.destroy();
    outgoing_message_queue.destroy();  
    is_connected = false;
  }
  

  public void notifyOfException( Throwable error ) {
    if( connection_listener != null ) {
      connection_listener.exceptionThrown( error );
    }
    else {
      Debug.out( "notifyOfException():: connection_listener == null for exception: " +error.getMessage() );
    }
  }
  

  public OutgoingMessageQueue getOutgoingMessageQueue() {  return outgoing_message_queue;  }

  public IncomingMessageQueue getIncomingMessageQueue() {  return incoming_message_queue;  }
  

  public void startMessageProcessing( LimitedRateGroup upload_group, LimitedRateGroup download_group ) {
    NetworkManager.getSingleton().getUploadProcessor().registerPeerConnection( this, upload_group );
    NetworkManager.getSingleton().getDownloadProcessor().registerPeerConnection( this, download_group );
  }
  
  
  public void enableEnhancedMessageProcessing( boolean enable ) {
    if( enable ) {
      NetworkManager.getSingleton().getUploadProcessor().upgradePeerConnection( this );
      NetworkManager.getSingleton().getDownloadProcessor().upgradePeerConnection( this );
    }
    else {
      NetworkManager.getSingleton().getUploadProcessor().downgradePeerConnection( this );
      NetworkManager.getSingleton().getDownloadProcessor().downgradePeerConnection( this );
    }
  }
  

  public TCPTransport getTCPTransport() {  return tcp_transport;  }
  

  public InetSocketAddress getAddress() {  return remote_address;  }

  
  
  public String toString() {
    return tcp_transport.getDescription();
  }
  
}
