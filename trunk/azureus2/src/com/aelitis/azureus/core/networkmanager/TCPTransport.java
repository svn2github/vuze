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

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Debug;



/**
 * Represents a peer TCP transport connection (eg. a network socket).
 */
public class TCPTransport {
  private static boolean enable_efficient_write = System.getProperty("java.version").startsWith("1.5") ? true : false;
  private SocketChannel socket_channel;
  private boolean is_connected;
  
  private boolean is_ready_for_write;
  private boolean is_write_select_pending = false;
  private Throwable write_select_failure = null;
  
  private boolean is_read_select_enabled = false;
  private Throwable read_select_failure = null;
  
  private ConnectDisconnectManager.ConnectListener connect_request_key = null;
  private String description = "<disconnected>";
  private ByteBuffer data_already_read = null;
  private final boolean is_inbound_connection;
  
  private int zero_read_count = 0;
  
  private final AEMonitor state_mon = new AEMonitor( "TCPTransport" );
  
  
  
  
  /**
   * Constructor for disconnected transport.
   */
  public TCPTransport() {
    socket_channel = null;
    is_connected = false;
    is_ready_for_write = false;
    is_inbound_connection = false;
  }
  
  
  /**
   * Constructor for connected transport.
   * @param channel connection
   * @param already_read bytes from the channel
   */
  public TCPTransport( SocketChannel channel, ByteBuffer already_read ) {
    this.socket_channel = channel;
    this.data_already_read = already_read;   
    is_connected = true;
    is_ready_for_write = true;  //assume it is ready
    is_inbound_connection = true;  //well, true only if the given socket was actually accepted
    description = ( is_inbound_connection ? "R" : "L" ) + ": " + channel.socket().getInetAddress().getHostAddress() + ": " + channel.socket().getPort();
  }
  
  /**
   * Get the socket channel used by the transport.
   * @return the socket channel
   */
  public SocketChannel getSocketChannel() {  return socket_channel;  }
  
  
  /**
   * Get a textual description for this transport.
   * @return description
   */
  public String getDescription() {  return description;  }
  
  
  /**
   * Is the transport ready to write,
   * i.e. will a write request result in >0 bytes written.
   * @return true if the transport is write ready, false if not yet ready
   */
  protected boolean isReadyForWrite() {  return is_ready_for_write;  }
  
    
  /**
   * Write data to the transport from the given buffers.
   * NOTE: Works like GatheringByteChannel.
   * @param buffers from which bytes are to be retrieved
   * @param array_offset offset within the buffer array of the first buffer from which bytes are to be retrieved
   * @param length maximum number of buffers to be accessed
   * @return number of bytes written
   * @throws IOException on write error
   */
  public long write( ByteBuffer[] buffers, int array_offset, int length ) throws IOException {
    if( !is_ready_for_write )  return 0;
    
    if( !is_connected ) {
      return 0;
    }
    
    
    /*
    if( array_offset < 0 || array_offset >= buffers.length ) {
      Debug.out( "array_offset < 0 || array_offset >= buffers.length" );
    }
    
    if( length < 1  || length > buffers.length - array_offset ) {
      Debug.out( "length < 1  || length > buffers.length - array_offset" );
    }
    
    if( buffers.length < 1 ) {
      Debug.out( "buffers.length < 1" );
    }
    
    for( int i = array_offset; i < (array_offset + length); i++ ) {
      ByteBuffer bb = buffers[ i ];
      
      if( bb == null ) {
        Debug.out( "bb[" +i+ "] == null" );
      }
    }
    */    

    
    try { 
      if( write_select_failure != null )  throw new IOException( "write_select_failure: " + write_select_failure.getMessage() );
    
      if( enable_efficient_write ) {
        try {
          long written = socket_channel.write( buffers, array_offset, length );		
          if( written < 1 )  requestWriteSelect();
          return written;
        }
        catch( IOException e ) {
          if( e.getMessage() == null ) {
            Debug.out( "CAUGHT EXCEPTION WITH NULL MESSAGE", e );
          }
          //a bug only fixed in Tiger (1.5 series):
          //http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4854354
          else if( e.getMessage().equals( "A non-blocking socket operation could not be completed immediately" ) ) {
            enable_efficient_write = false;
            Debug.out( "ERROR: Multi-buffer socket write failed; switching to single-buffer mode. Upgrade to JRE 1.5 series to fix." );
          }
          throw e;
        }
      }
    
      //single-buffer mode
      long written_sofar = 0;
      for( int i=array_offset; i < (array_offset + length); i++ ) {
        int data_length = buffers[ i ].remaining();
        int written = socket_channel.write( buffers[ i ] );
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
      public boolean selectSuccess( VirtualChannelSelector selector, SocketChannel sc,Object attachment ) {
        is_ready_for_write = true;
        is_write_select_pending = false;
        
        return( true );
      }

      public void selectFailure( VirtualChannelSelector selector, SocketChannel sc,Object attachment, Throwable msg ) {
        is_write_select_pending = false;
        write_select_failure = msg;
        is_ready_for_write = true;  //set to true so that the next write attempt will throw an exception
      }
    }, null );
  }
  
  
  
  /**
   * Enable transport read selection.
   * @param listener to handle readiness
   */
  public void requestReadSelects( final ReadListener listener ) {
    try {  state_mon.enter();
      if( !is_read_select_enabled ) {
        is_read_select_enabled = true;
      
        if( !is_connected ) {  //socket isnt established yet
          System.out.println( "["+System.currentTimeMillis()+"] ERROR: READ SELECT REQUESTED WHILE NOT CONNECTED" );
          return;
        }

        if( socket_channel == null ) {
          System.out.println( "startReadSelects: socket_channel == null" );
          return;
        }

        NetworkManager.getSingleton().getReadController().getReadSelector().register( socket_channel, new VirtualChannelSelector.VirtualSelectorListener() {
          public boolean selectSuccess( VirtualChannelSelector selector, SocketChannel sc,Object attachment ) {
            if( !is_read_select_enabled ) {
              System.out.println( "["+System.currentTimeMillis()+"] selectSuccess() but !is_read_select_enabled" );
              return false;
            }
          
            listener.readyToRead();
            return true;
          }
          
          public void selectFailure( VirtualChannelSelector selector, SocketChannel sc,Object attachment, Throwable msg ) {
            read_select_failure = msg;
            listener.readyToRead();  //so that the resulting read attempt will throw an exception
          }
        }, null );
      }
      else  System.out.println( "READ SELECT ALREADY ENABLED !!!" );  
    }
    finally {  state_mon.exit();  }
  }

  
  /**
   * Disable transport read selection.
   */
  public void cancelReadSelects() {
    try {  state_mon.enter();
      if( is_read_select_enabled ) {
      
        if( socket_channel == null ) {
          Debug.out( "cancelReadSelects: socket_channel == null, is_connected=" +is_connected );
          return;
        }
      
        NetworkManager.getSingleton().getReadController().getReadSelector().cancel( socket_channel );
      
        is_read_select_enabled = false;
      }
    }
    finally {  state_mon.exit();  }
  }
  
  

  
  
  /**
   * Read data from the transport into the given buffers.
   * NOTE: Works like ScatteringByteChannel.
   * @param buffers into which bytes are to be placed
   * @param array_offset offset within the buffer array of the first buffer into which bytes are to be placed
   * @param length maximum number of buffers to be accessed
   * @return number of bytes read
   * @throws IOException on read error
   */
  public long read( ByteBuffer[] buffers, int array_offset, int length ) throws IOException {
    if( read_select_failure != null ) {
      throw new IOException( "read_select_failure: " + read_select_failure.getMessage() );
    }

    if( !is_connected ) {
      System.out.println( "TCPTransport read() attempted while not connected!" );
      return 0;
    }
    
    
    /*
    if( array_offset < 0 || array_offset >= buffers.length ) {
      Debug.out( "array_offset < 0 || array_offset >= buffers.length" );
    }
    
    if( length < 1  || length > buffers.length - array_offset ) {
      Debug.out( "length < 1  || length > buffers.length - array_offset" );
    }
    
    if( buffers.length < 1 ) {
      Debug.out( "buffers.length < 1" );
    }
    
    for( int i = array_offset; i < (array_offset + length); i++ ) {
      ByteBuffer bb = buffers[ i ];
      
      if( bb == null ) {
        Debug.out( "bb[" +i+ "] == null" );
      }
    }
    */
    
    
    //insert already-read data into the front of the stream
    if( data_already_read != null ) {
      int inserted = 0;
      
      for( int i = array_offset; i < (array_offset + length); i++ ) {
        ByteBuffer bb = buffers[ i ];
        
        int orig_limit = data_already_read.limit();
        
        if( data_already_read.remaining() > bb.remaining() ) {
          data_already_read.limit( data_already_read.position() + bb.remaining() ); 
        }
        
        inserted += data_already_read.remaining();
        
        bb.put( data_already_read );
        
        data_already_read.limit( orig_limit );
        
        if( !data_already_read.hasRemaining() ) {
          data_already_read = null;
          break;
        }
      }
      
      if( !buffers[ array_offset + length - 1 ].hasRemaining() ) {  //the last buffer has nothing left to read into normally
        return inserted;  //so return right away, skipping socket read
      }      
    }
 
    
    if( socket_channel == null ) {
      System.out.println( "read(): [" +description+ "]: socket_channel == null, is_connected=" +is_connected );
    }
    
    if( buffers == null ) {
      System.out.println( "read: buffers == null" );
    }
    

    
    long bytes_read = socket_channel.read( buffers, array_offset, length );
    
    if( bytes_read < 0 ) {
      throw new IOException( "end of stream on socket read" );
    }
    
    if( bytes_read == 0 ) {
      zero_read_count++;
      if( zero_read_count % 50 == 0 ) {
        System.out.println( "[" +System.currentTimeMillis()+ "] [" +description+ "] 0-byte-reads=" +zero_read_count );
      }
    }
    
    return bytes_read;
  }
  


 
  /**
   * Request the transport connection be established.
   * NOTE: Will automatically connect via configured proxy if necessary.
   * @param address remote peer address to connect to
   * @param listener establishment failure/success listener
   */
  public void establishOutboundConnection( final InetSocketAddress address, final ConnectListener listener ) {
    try {  state_mon.enter();
      if( is_connected ) {
        System.out.println( "transport already connected" );
        listener.connectSuccess();
        return;
      }
    
      final boolean use_proxy = COConfigurationManager.getBooleanParameter( "Proxy.Data.Enable" );
      final TCPTransport transport_instance = this;    
    
      ConnectDisconnectManager.ConnectListener connect_listener = new ConnectDisconnectManager.ConnectListener() {
        public void connectAttemptStarted() {
          listener.connectAttemptStarted();
        }
      
        public void connectSuccess( SocketChannel channel ) {
          try {  state_mon.enter();
            socket_channel = channel;
            is_connected = true;
            connect_request_key = null;
            description = ( is_inbound_connection ? "R" : "L" ) + ": " + channel.socket().getInetAddress().getHostAddress() + ": " + channel.socket().getPort();
            is_ready_for_write = true;           
          }
          finally {  state_mon.exit();  }
          
          if( use_proxy ) {  //proxy server connection established, login
            System.out.println( "Socket connection established to proxy server [" +description+ "], login initiated..." );
          
            new ProxyLoginHandler( transport_instance, address, new ProxyLoginHandler.ProxyListener() {
              public void connectSuccess() {
                System.out.println( "Proxy login successful." );
                listener.connectSuccess();
              }
              public void connectFailure( Throwable failure_msg ) {  listener.connectFailure( failure_msg );  }
            });
          }
          else {  //direct connection established, notify
            listener.connectSuccess();
          }
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
    
      InetSocketAddress to_connect = use_proxy ? ProxyLoginHandler.SOCKS_SERVER_ADDRESS : address;
    
      NetworkManager.getSingleton().getConnectDisconnectManager().requestNewConnection( to_connect, connect_listener );
    }
    finally {  state_mon.exit();  }
  }
  
    
  
  
  /**
   * Close the transport connection.
   */
  public void close() {
    cancelReadSelects();
    
    try {  state_mon.enter();
      is_ready_for_write = false;
    
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
    
      socket_channel = null;
    }
    finally {  state_mon.exit();  }
  }
     
  
  
  
  
  /**
   * Listener for notification of connection establishment.
   */
  public interface ConnectListener {
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
 
   
   
  /**
   * Listener for notification for transport reads.
   */
  public interface ReadListener {
    /**
     * Notification of transport read readiness.
     */
    public void readyToRead();
  }
  
}
