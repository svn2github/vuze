/*
 * Created on May 8, 2004
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
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.gudy.azureus2.core3.util.Debug;



/**
 * Represents a peer transport connection (eg. a network socket).
 */
public class Transport {
  private static boolean enable_efficient_write = System.getProperty("java.version").startsWith("1.5") ? true : false;
  private SocketChannel socket_channel;
  private boolean is_connected;
  private boolean is_ready_for_write;
  private boolean is_write_select_pending = false;
  private Throwable write_select_failure = null;
  private ConnectDisconnectManager.ConnectListener connect_request_key = null;
  private String description = "<disconnected>";
  private TransportDebugger		transport_debugger;
  
  /**
   * Constructor for disconnected transport.
   */
  protected Transport( TransportOwner _owner ) {
    socket_channel = null;
    is_connected = false;
    is_ready_for_write = false;
    
    transport_debugger	= _owner.getDebugger();
  }
  
  /**
   * Constructor for connected transport.
   * @param channel connection
   */
  protected Transport( TransportOwner _owner, SocketChannel channel ) {
    this.socket_channel = channel;
    is_connected = true;
    is_ready_for_write = true;  //assume it is ready
    description = channel.socket().getInetAddress().getHostAddress() + ":" + channel.socket().getPort();
    transport_debugger = _owner.getDebugger();
  }
  
  /**
   * Get the socket channel used by the transport.
   * @return the socket channel
   */
  protected SocketChannel getSocketChannel() {  return socket_channel;  }
  
  
  /**
   * Get a textual description for this transport.
   * @return description
   */
  protected String getDescription() {  return description;  }
  
  
  /**
   * Is the transport ready to write,
   * i.e. will a write request result in >0 bytes written.
   * @return true if the transport is write ready, false if not yet ready
   */
  protected boolean isReadyForWrite() {  return is_ready_for_write;  }
  
    
  /**
   * Write data to the transport from the given buffers.
   * NOTE: Works like GatheringByteChannel.
   * @param buffers th buffers from which bytes are to be retrieved
   * @param array_offset offset within the buffer array of the first buffer from which bytes are to be retrieved
   * @param array_length maximum number of buffers to be accessed
   * @return number of bytes written
   * @throws IOException
   */
  protected long write( ByteBuffer[] buffers, int array_offset, int array_length ) throws IOException {
    if( !is_ready_for_write )  return 0;
    
    try { 
      if( write_select_failure != null )  throw new IOException( "write_select_failure: " + write_select_failure.getMessage() );
    
      if( enable_efficient_write ) {     
        try {
          long written = transport_debugger==null?
          					socket_channel.write( buffers, array_offset, array_length ):
          					transport_debugger.write( socket_channel, buffers, array_offset, array_length );
          					
          if( written < 1 )  requestWriteSelect();
          return written;
        }
        catch( IOException e ) {
          //a bug only fixed in Tiger (1.5 series):
          //http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4854354
          if( e.getMessage().equals( "A non-blocking socket operation could not be completed immediately" ) ) {
            enable_efficient_write = false;
            Debug.out( "ERROR: Multi-buffer socket write failed; switching to single-buffer mode. Upgrade to JRE 1.5 series to fix." );
          }
          throw e;
        }
      }
    
      //single-buffer mode
      long written_sofar = 0;
      for( int i=array_offset; i < array_length; i++ ) {
        int data_length = buffers[ i ].remaining();
        int written = transport_debugger==null?
        					socket_channel.write( buffers[ i ] ):
        					transport_debugger.write( socket_channel, buffers[i] );
        written_sofar += written;
        if( written < data_length ) {
          break;
        }
      }
      
      if( written_sofar < 1 )  requestWriteSelect();
      
      return written_sofar;
    }
    catch( IOException e ) {
      is_ready_for_write = false;  //once an exception is thrown on write, disable any future writing
      throw e;
    }
  }
  
  
  
  private void requestWriteSelect() {
    is_ready_for_write = false;
    is_write_select_pending = true;
    
    
    NetworkManager.getSingleton().getWriteController().getWriteSelector().register( socket_channel, new VirtualChannelSelector.VirtualSelectorListener() {
      public void selectSuccess( Object attachment ) {
        is_ready_for_write = true;
        is_write_select_pending = false;
      }

      public void selectFailure( Throwable msg ) {
        is_ready_for_write = true;  //set to true so that the next write attempt will throw an exception
        is_write_select_pending = false;
        write_select_failure = msg;
      }
    }, null);
  }
  
  
  
 
  /**
   * Request the transport connection be established.
   * @param address remote peer address to connect to
   * @param listener establishment failure/success listener
   */
  protected void establishOutboundConnection( InetSocketAddress address, final ConnectListener listener ) {
    if( is_connected ) {
      System.out.println( "transport already connected" );
      listener.connectSuccess();
      return;
    }
    
    ConnectDisconnectManager.ConnectListener connect_listener = new ConnectDisconnectManager.ConnectListener() {
      public void connectAttemptStarted() {
        listener.connectAttemptStarted();
      }
      
      public void connectSuccess( SocketChannel channel ) {
        socket_channel = channel;
        is_connected = true;
        is_ready_for_write = true;
        connect_request_key = null;
        description = channel.socket().getInetAddress().getHostAddress() + ":" + channel.socket().getPort();
        listener.connectSuccess();
      }

      public void connectFailure( Throwable failure_msg ) {
        socket_channel = null;
        is_connected = false;
        is_ready_for_write = false;
        connect_request_key = null;
        listener.connectFailure( failure_msg );
      }
    };
    
    connect_request_key = connect_listener;
    NetworkManager.getSingleton().getConnectDisconnectManager().requestNewConnection( address, connect_listener );
  }
  
  
  /**
   * Close the transport connection.
   */
  protected void close() {
    if( is_connected ) {
      is_connected = false;
      if( is_write_select_pending ) {
        NetworkManager.getSingleton().getWriteController().getWriteSelector().cancel( socket_channel );
        is_write_select_pending = false;
      }
      NetworkManager.getSingleton().getConnectDisconnectManager().closeConnection( socket_channel );
    }
    else if( connect_request_key != null ) {
      NetworkManager.getSingleton().getConnectDisconnectManager().cancelRequest( connect_request_key );
      connect_request_key = null;
    }
    
    is_ready_for_write = false;
    socket_channel = null;
  }
  
  
  
////////////////////////////////////
  /**
   * Listener for notification of connection establishment.
   */
   protected interface ConnectListener {
     /**
      * The connection establishment process has started,
      * i.e. the connection is actively being attempted.
      */
     public void connectAttemptStarted();   
     
     /**
      * The connection attempt succeeded.
      * The connection is now established.
      */
     public void connectSuccess() ;
    
    /**
     * The connection attempt failed.
     * @param failure_msg failure reason
     */
    public void connectFailure( Throwable failure_msg );
  }
///////////////////////////////////
   
   
  
}
