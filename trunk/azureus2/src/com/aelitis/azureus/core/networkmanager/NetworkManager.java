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
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.gudy.azureus2.core3.config.*;


import com.aelitis.azureus.core.peermanager.messaging.MessageStreamDecoder;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamEncoder;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamFactory;



/**
 *
 */
public class NetworkManager {
  private static final NetworkManager instance = new NetworkManager();

  private int tcp_mss_size = COConfigurationManager.getIntParameter( "network.tcp.mtu.size" ) - 40;
  private final ConnectDisconnectManager connect_disconnect_manager = new ConnectDisconnectManager();
  private final IncomingSocketChannelManager incoming_socketchannel_manager = new IncomingSocketChannelManager();
  private final WriteController write_controller = new WriteController();
  private final ReadController read_controller = new ReadController();

  
  private NetworkManager() {
    COConfigurationManager.addParameterListener( "network.tcp.mtu.size", new ParameterListener() {
      public void parameterChanged( String parameterName ) {
        tcp_mss_size = COConfigurationManager.getIntParameter( "network.tcp.mtu.size" ) - 40;
      }
    });    
  }
  
  
  /**
   * Get the singleton instance of the network manager.
   * @return the network manager
   */
  public static NetworkManager getSingleton() {  return instance;  }
  
  
  
  /**
   * Create a new unconnected remote network connection (for outbound-initiated connections).
   * @param remote_address to connect to
   * @param encoder default message stream encoder to use for the outgoing queue
   * @param decoder default message stream decoder to use for the incoming queue
   * @return a new connection
   */
  public NetworkConnection createConnection( InetSocketAddress remote_address, MessageStreamEncoder encoder, MessageStreamDecoder decoder ) { 
    return NetworkConnectionFactory.create( remote_address, encoder, decoder );
  }
  
  
  
  /**
   * Request the acceptance and routing of new incoming connections that match the given initial byte sequence.
   * @param matcher initial byte sequence used for routing
   * @param listener for handling new inbound connections
   * @param factory to use for creating default stream encoder/decoders
   */
  public void requestIncomingConnectionRouting( ByteMatcher matcher, final RoutingListener listener, final MessageStreamFactory factory ) {
    incoming_socketchannel_manager.registerMatchBytes( matcher, new IncomingSocketChannelManager.MatchListener() {
      public void connectionMatched( SocketChannel channel, ByteBuffer read_so_far ) {
        listener.connectionRouted( NetworkConnectionFactory.create( channel, read_so_far, factory.createEncoder(), factory.createDecoder() ) );
      }
    });
  }
  
  
  /**
   * Cancel a request for inbound connection routing.
   * @param matcher byte sequence originally used to register
   */
  public void cancelIncomingConnectionRouting( ByteMatcher matcher ) {
    incoming_socketchannel_manager.deregisterMatchBytes( matcher );
  }
  

  
  
  /**
   * Add an upload entity for write processing.
   * @param entity to add
   */
  public void addWriteEntity( RateControlledWriteEntity entity ) {
    write_controller.addWriteEntity( entity );
  }
  
  
  /**
   * Remove an upload entity from write processing.
   * @param entity to remove
   */
  public void removeWriteEntity( RateControlledWriteEntity entity ) {
    write_controller.removeWriteEntity( entity );
  }
  
  
  

  /**
   * Get the manager for new incoming socket channel connections.
   * @return manager
   */
  public IncomingSocketChannelManager getIncomingSocketChannelManager() {  return incoming_socketchannel_manager;  }
  

  /**
   * Get the socket channel connect / disconnect manager.
   * @return connect manager
   */
  protected ConnectDisconnectManager getConnectDisconnectManager() {  return connect_disconnect_manager;  }
  
  
  /**
   * Asynchronously close the given socket channel.
   * @param channel to close
   */
  public void closeSocketChannel( SocketChannel channel ) {
    connect_disconnect_manager.closeConnection( channel );
  }
  
  
  
  /**
   * Get the socket write controller.
   * @return controller
   */
  public WriteController getWriteController() {  return write_controller;  }
  
  
  /**
   * Get the socket read controller.
   * @return controller
   */
  public ReadController getReadController() {  return read_controller;  }
  
  
  
  /**
   * Get the configured TCP MSS (Maximum Segment Size) unit, i.e. the max (preferred) packet payload size.
   * NOTE: MSS is MTU-40bytes for TCPIP headers, usually 1460 (1500-40) for standard ethernet
   * connections, or 1452 (1492-40) for PPPOE connections.
   * @return mss size in bytes
   */
  public int getTcpMssSize() {  return tcp_mss_size;  }
  
  
  
  
  
  
  
  /**
   * Byte stream match filter for routing.
   */
  public interface ByteMatcher {
    /**
     * Get the number of bytes this matcher requires.
     * @return size in bytes
     */
    public int size();
    
    /**
     * Check byte stream for match.
     * @param to_compare
     * @return true if a match, false if not a match
     */
    public boolean matches( ByteBuffer to_compare );
  }
  
  
  
  /**
   * Listener for routing events.
   */
  public interface RoutingListener {
    /**
     * The given incoming connection has been accepted.
     * @param connection accepted
     */
    public void connectionRouted( NetworkConnection connection );
  }
  
}
