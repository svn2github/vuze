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

import java.nio.channels.SocketChannel;


import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.util.SystemTime;

/**
 *
 */
public class NetworkManager {
  protected static final int UNLIMITED_WRITE_RATE = 1024 * 1024 * 100; //100 mbyte/s
  private static final NetworkManager instance = new NetworkManager();
  private int max_write_rate_bytes_per_sec = COConfigurationManager.getIntParameter( "Max Upload Speed KBs" ) == 0 ? UNLIMITED_WRITE_RATE : COConfigurationManager.getIntParameter( "Max Upload Speed KBs" ) * 1024;
  private int tcp_mss_size = COConfigurationManager.getIntParameter( "network.tcp.mtu.size" ) - 40;
  private final ConnectionPool root_connection_pool = new ConnectionPool( max_write_rate_bytes_per_sec );
  private final VirtualChannelSelector write_selector = new VirtualChannelSelector( VirtualChannelSelector.OP_WRITE );
  
  
  public static final int WRITE_WINDOW_SIZE = 64*1024;
  
  
  private NetworkManager() {
    Thread write_processing_thread = new Thread( "NetworkManager:Write" ) {
      public void run() {
        writeProcessingLoop();
      }
    };
    write_processing_thread.setDaemon( true );
    write_processing_thread.start();
    
    COConfigurationManager.addListener( new COConfigurationListener() {
      public void configurationSaved() {       
        max_write_rate_bytes_per_sec = COConfigurationManager.getIntParameter( "Max Upload Speed KBs" ) == 0 ? UNLIMITED_WRITE_RATE : COConfigurationManager.getIntParameter( "Max Upload Speed KBs" ) * 1024;
        root_connection_pool.updateBucketRates();
        
        tcp_mss_size = COConfigurationManager.getIntParameter( "network.tcp.mtu.size" ) - 40;
      }
    });
    
  }
  
  
  
  
  /**
   * Get the singleton instance of the network manager.
   * @return the network manager
   */
  public static synchronized NetworkManager getSingleton() {
    return instance;
  }
  
  
  /**
   * Get the root connection pool.
   * @return root pool
   */
  public ConnectionPool getRootConnectionPool() {  return root_connection_pool;  }
  
  
  //TODO replace with a proper async new connection establishment
  //NOTE: a connection must be added to a connection pool in order for messages to be transmitted/received
  public Connection createNewConnection( SocketChannel channel, Connection.ConnectionListener listener ) {
    Transport transport = new Transport( channel );
    Connection connection = new Connection( transport, listener );
    return connection;
  }
  
  
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
  protected int getTcpMssSize() {  return tcp_mss_size;  }
  

  private void writeProcessingLoop() {
    try {  Thread.sleep( 1000 );  }catch( Exception e ) {}
    
    while( true ) {
      try {
        //long start_time = System.currentTimeMillis();
        
        //write_selector.select( 25 );
        
        root_connection_pool.doWrites( write_selector );
      
        //long processing_time = System.currentTimeMillis() - start_time;
        //System.out.println("processing_time="+processing_time);
        //if( processing_time < 1000 ) {
        //  try{  Thread.sleep( 1000 - processing_time );  }catch( Exception e) {e.printStackTrace();}
        //}
      }
      catch( Throwable t ) {
        t.printStackTrace();
        try{ Thread.sleep( 1000 );  }catch( Exception e) {}
      }
    }
  }
  

  protected VirtualChannelSelector getWriteSelector() {  return write_selector;  }


}
