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

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.util.Debug;


/**
 *
 */
public class NetworkManager {
  private static final NetworkManager instance = new NetworkManager();

  private int tcp_mss_size = COConfigurationManager.getIntParameter( "network.tcp.mtu.size" ) - 40;
  private final ConnectDisconnectManager connect_disconnect_manager = new ConnectDisconnectManager();
  private final WriteController write_controller = new WriteController();
  

  
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
   * Create a new unconnected remote peer connection.
   * @param remote_address ip address or hostname of remote peer
   * @param remote_port to connect to
   * @return a new connection
   */
  public Connection createNewConnection( ConnectionOwner owner, String remote_address, int remote_port ) {
    if( remote_port < 0 || remote_port > 65535 ) {
      Debug.out( "remove_port invalid: " + remote_port );
      remote_port = 0;
    }
    Connection conn = new Connection( owner, new InetSocketAddress( remote_address, remote_port ) );
    return conn;
  }
  
  
  /**
   * TEMP METHOD UNTIL INBOUND CONNECTIONS ARE HANDLED INTERNALLY
   */
  public Connection createNewInboundConnection( ConnectionOwner owner, SocketChannel channel ) {
    Connection conn = new Connection( owner, channel );
    return conn;
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
   * Get the socket connect and disconnect manager.
   * @return manager
   */
  protected ConnectDisconnectManager getConnectDisconnectManager() {  return connect_disconnect_manager;  }
  
  
  /**
   * Get the socket write controller.
   * @return controller
   */
  protected WriteController getWriteController() {  return write_controller;  }
  
  
  
  /**
   * Get the configured TCP MSS (Maximum Segment Size) unit, i.e. the max (preferred) packet payload size.
   * NOTE: MSS is MTU-40bytes for TCPIP headers, usually 1460 (1500-40) for standard ethernet
   * connections, or 1452 (1492-40) for PPPOE connections.
   * @return mss size in bytes
   */
  public int getTcpMssSize() {  return tcp_mss_size;  }
  


}
