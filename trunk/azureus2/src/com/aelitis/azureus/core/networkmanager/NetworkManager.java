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
import org.gudy.azureus2.core3.util.*;


/**
 *
 */
public class NetworkManager {
  protected static final int UNLIMITED_WRITE_RATE = 1024 * 1024 * 100; //100 mbyte/s
  private static final NetworkManager instance = new NetworkManager();
  private int max_write_rate_bytes_per_sec = COConfigurationManager.getIntParameter( "Max Upload Speed KBs" ) == 0 ? UNLIMITED_WRITE_RATE : COConfigurationManager.getIntParameter( "Max Upload Speed KBs" ) * 1024;
  private int max_write_rate_bytes_per_sec_seeding = COConfigurationManager.getIntParameter( "Max Upload Speed Seeding KBs" ) == 0 ? UNLIMITED_WRITE_RATE : COConfigurationManager.getIntParameter( "Max Upload Speed Seeding KBs" ) * 1024;
  private int tcp_mss_size = COConfigurationManager.getIntParameter( "network.tcp.mtu.size" ) - 40;
  private final ConnectDisconnectManager connect_disconnect_manager;
  
  
  
  
  private final ConnectionPool root_connection_pool = new ConnectionPool( max_write_rate_bytes_per_sec );
  private final VirtualChannelSelector write_selector = new VirtualChannelSelector( VirtualChannelSelector.OP_WRITE );
  

  
  
  
  private NetworkManager() {
    connect_disconnect_manager = new ConnectDisconnectManager();
    
    Thread write_processing_thread = new AEThread( "NetworkManager:Write" ) {
      public void runSupport() {
        writeProcessingLoop();
      }
    };
    write_processing_thread.setDaemon( true );
    write_processing_thread.start();
    
    COConfigurationManager.addListener( new COConfigurationListener() {
      public void configurationSaved() {       
        max_write_rate_bytes_per_sec = COConfigurationManager.getIntParameter( "Max Upload Speed KBs" ) == 0 ? UNLIMITED_WRITE_RATE : COConfigurationManager.getIntParameter( "Max Upload Speed KBs" ) * 1024;
        max_write_rate_bytes_per_sec_seeding = COConfigurationManager.getIntParameter( "Max Upload Speed Seeding KBs" ) == 0 ? UNLIMITED_WRITE_RATE : COConfigurationManager.getIntParameter( "Max Upload Speed Seeding KBs" ) * 1024;
        root_connection_pool.updateBucketRates();
        
        tcp_mss_size = COConfigurationManager.getIntParameter( "network.tcp.mtu.size" ) - 40;
      }
    });
    
  }
  
  
  
  
  /**
   * Get the singleton instance of the network manager.
   * @return the network manager
   */
  public static NetworkManager getSingleton() {
    return instance;
  }
  
  
  /**
   * Create a new unconnected remote peer connection.
   * @param remote_address ip address or hostname of remote peer
   * @param remote_port to connect to
   * @return a new connection
   */
  public Connection createNewConnection( String remote_address, int remote_port ) {
    Connection conn = new Connection( new InetSocketAddress( remote_address, remote_port ) );
    return conn;
  }
  
  
  /**
   * TEMP METHOD UNTIL INBOUND CONNECTIONS ARE HANDLED INTERNALLY
   */
  public Connection createNewInboundConnection( SocketChannel channel ) {
    Connection conn = new Connection( channel );
    return conn;
  }
  
  
  
  /**
   * Get the socket connect and disconnect manager.
   * @return manager
   */
  protected ConnectDisconnectManager getConnectDisconnectManager() {  return connect_disconnect_manager;  }
  
  
  /**
   * Get the virtual selector for socket channel write readyness.
   * @return selector
   */
  protected VirtualChannelSelector getWriteSelector() {  return write_selector;  }
  
  
  
  //////////////////////////////////////////////////////////////
  
  
  /**
   * Get the root connection pool.
   * @return root pool
   */
  public ConnectionPool getRootConnectionPool() {  return root_connection_pool;  }
  
  
  
  

  

  
  
  /**
   * Get the max write rate (upload speed) in bytes per second.
   * @return bytes per sec
   */
  public int getMaxWriteRateBytesPerSec() {  return max_write_rate_bytes_per_sec;  }

  
  /**
   * Get the configured TCP MSS (Maximum Segment Size) unit, i.e. the max (preferred) packet payload size.
   * NOTE: MSS is MTU-40bytes for TCPIP headers, usually 1460 (1500-40) for standard ethernet
   * connections, or 1452 (1492-40) for PPPOE connections.
   * @return mss size in bytes
   */
  public int getTcpMssSize() {  return tcp_mss_size;  }
  

  private void writeProcessingLoop() {
    try {  Thread.sleep( 1000 );  }catch( Exception e ) {}
    
    while( true ) {
      try {
        //long start_time = System.currentTimeMillis();
        
        write_selector.select( 50 );
        root_connection_pool.doWrites();
      
        //long processing_time = System.currentTimeMillis() - start_time;
        //System.out.println("processing_time="+processing_time);
        //if( processing_time < 1000 ) {
        //  try{  Thread.sleep( 1000 - processing_time );  }catch( Exception e) {e.printStackTrace();}
        //}
      }
      catch( Throwable t ) {
      	Debug.printStackTrace(t);
        try{ Thread.sleep( 1000 );  }catch( Exception e) {}
      }
    }
  }
  

  


}
