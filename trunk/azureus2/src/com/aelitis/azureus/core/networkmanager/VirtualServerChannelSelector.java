/*
 * Created on Dec 4, 2004
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

import java.net.*;
import java.nio.channels.*;

import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Debug;



/**
 * Virtual server socket channel for listening and accepting incoming connections.
 */
public class VirtualServerChannelSelector {
  private ServerSocketChannel server_channel = null;
  private final InetSocketAddress bind_address;
  private final SelectListener listener;
  private boolean running = false;
  
  
  /**
   * Create a new server listening on the given address and reporting to the given listener.
   * @param bind_address ip+port to listen on
   * @param listener to notify of incoming connections
   */
  public VirtualServerChannelSelector( InetSocketAddress bind_address, SelectListener listener ) {
    this.bind_address = bind_address;
    this.listener = listener;
  }
  
  
  /**
   * Start the server and begin accepting incoming connections.
   * 
   */
  public synchronized void start() {
    if( !running ) {
      try {
        server_channel = ServerSocketChannel.open();
        server_channel.socket().setReuseAddress( true );
        server_channel.socket().bind( bind_address, 1024 );
        
        AEThread accept_thread = new AEThread( "VServerSelector:port" + bind_address.getPort() ) {
          public void runSupport() {
            running = true;
            accept_loop();
          }
        };
        accept_thread.setDaemon( true );
        accept_thread.start();  
      }
      catch( Throwable t ) {  Debug.out( t );  }
    }
  }
  
  
  /**
   * Stop the server.
   */
  public synchronized void stop() {
    running = false;
    if( server_channel != null ) {
      try {
        server_channel.close();
        server_channel = null;
      }
      catch( Throwable t ) {  Debug.out( t );  }
    }
  }
  
  
  private void accept_loop() {
    while( running ) {
      try {
        SocketChannel client_channel = server_channel.accept();
        client_channel.configureBlocking( false );
        listener.newConnectionAccepted( client_channel );
      }
      catch( Throwable t ) {
        Debug.out( t );
        try {  Thread.sleep( 500 );  }catch( Exception e ) {  e.printStackTrace();  }
      }
      
    }
  }
  
  
  /**
   * Listener notified when a new incoming connection is accepted.
   */
  public interface SelectListener {
    /**
     * The given connection has just been accepted.
     * @param channel new connection
     */
    public void newConnectionAccepted( SocketChannel channel );
  }

  
}
