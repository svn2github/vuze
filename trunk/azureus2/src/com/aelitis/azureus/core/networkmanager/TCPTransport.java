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
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.networkmanager.impl.*;



/**
 * Represents a peer TCP transport connection (eg. a network socket).
 */
public class TCPTransport {
  
  public static final int TRANSPORT_MODE_NORMAL = 0;
  public static final int TRANSPORT_MODE_FAST   = 1;
  public static final int TRANSPORT_MODE_TURBO  = 2;
  
  
  private static boolean enable_efficient_io = System.getProperty("java.version").startsWith("1.5") ? true : false;
  private SocketChannel socket_channel;

  private volatile boolean is_ready_for_write = false;
  private volatile boolean is_ready_for_read = false;
  private Throwable write_select_failure = null;
  private Throwable read_select_failure = null;

  
  private ConnectDisconnectManager.ConnectListener connect_request_key = null;
  private String description = "<disconnected>";
  private ByteBuffer data_already_read = null;
  private final boolean is_inbound_connection;
  
  private int transport_mode = TRANSPORT_MODE_NORMAL;

  public volatile boolean has_been_closed = false;
  public String has_been_closed_error = null;

  
  
  /**
   * Constructor for disconnected transport.
   */
  public TCPTransport() {
    socket_channel = null;
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
    is_inbound_connection = true;
    description = ( is_inbound_connection ? "R" : "L" ) + ": " + channel.socket().getInetAddress().getHostAddress() + ": " + channel.socket().getPort();
    
    registerSelectHandling();
  }
  
  
  /**
   * Inject the given already-read data back into the read stream.
   * @param bytes_already_read data
   */
  public void setAlreadyRead( ByteBuffer bytes_already_read ) {
    if( bytes_already_read.hasRemaining() ) {
      data_already_read = bytes_already_read;
    }
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
  public boolean isReadyForWrite() {  return is_ready_for_write;  }
  
  
  /**
   * Is the transport ready to read,
   * i.e. will a read request result in >0 bytes read.
   * @return true if the transport is read ready, false if not yet ready
   */
  public boolean isReadyForRead() {  return is_ready_for_read;  }
    
  
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
    if( socket_channel == null ) {
      Debug.out( "socket_channel == null" );
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
    
      if( enable_efficient_io ) {
        try {
          long written = socket_channel.write( buffers, array_offset, length );		
          if( written < 1 )  requestWriteSelect();
          return written;
        }
        catch( IOException ioe ) {
          //a bug only fixed in Tiger (1.5 series):
          //http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4854354
          String msg = ioe.getMessage();
          if( msg != null && msg.equals( "A non-blocking socket operation could not be completed immediately" ) ) {
            enable_efficient_io = false;
            LGLogger.logUnrepeatableAlert( LGLogger.AT_WARNING, "WARNING: Multi-buffer socket write failed; switching to single-buffer mode.\nUpgrade to JRE 1.5 (5.0) series to fix this problem!" );
          }
          throw ioe;
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
    if( socket_channel != null ){
      NetworkManager.getSingleton().getWriteSelector().resumeSelects( socket_channel );
    }
  }
  
  
  private void requestReadSelect() {
    is_ready_for_read = false;
    if( socket_channel != null ){
      NetworkManager.getSingleton().getReadSelector().resumeSelects( socket_channel );
    }
  } 
  

  
  private void registerSelectHandling() {
    if( socket_channel == null ) {
      Debug.out( "ERROR: registerSelectHandling():: socket_channel == null" );
      return;
    }

    //read selection
    NetworkManager.getSingleton().getReadSelector().register( socket_channel, new VirtualChannelSelector.VirtualSelectorListener() {
      public boolean selectSuccess( VirtualChannelSelector selector, SocketChannel sc,Object attachment ) {
        is_ready_for_read = true;
        return true;
      }
      
      public void selectFailure( VirtualChannelSelector selector, SocketChannel sc,Object attachment, Throwable msg ) {
        read_select_failure = msg;
        is_ready_for_read = true;  //set to true so that the next read attempt will throw an exception
      }
    }, null );
    
    
    //write selection
    NetworkManager.getSingleton().getWriteSelector().register( socket_channel, new VirtualChannelSelector.VirtualSelectorListener() {
      public boolean selectSuccess( VirtualChannelSelector selector, SocketChannel sc,Object attachment ) {
        is_ready_for_write = true;
        return true;
      }

      public void selectFailure( VirtualChannelSelector selector, SocketChannel sc,Object attachment, Throwable msg ) {
        write_select_failure = msg;
        is_ready_for_write = true;  //set to true so that the next write attempt will throw an exception
      }
    }, null );
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
      is_ready_for_read = false;
      throw new IOException( "read_select_failure: " + read_select_failure.getMessage() );
    }

    if( socket_channel == null ) {
      Debug.out( "socket_channel == null" );
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
 
    
    if( buffers == null ) {
      Debug.out( "read: buffers == null" );
    }
    
    long bytes_read = 0;
    
    if( enable_efficient_io ) {
      try{
        bytes_read = socket_channel.read( buffers, array_offset, length );
      }
      catch( IOException ioe ) {
        is_ready_for_read = false;
        
        //a bug only fixed in Tiger (1.5 series):
        //http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4854354
        String msg = ioe.getMessage();
        if( msg != null && msg.equals( "A non-blocking socket operation could not be completed immediately" ) ) {
          enable_efficient_io = false;
          LGLogger.logUnrepeatableAlert( LGLogger.AT_WARNING, "WARNING: Multi-buffer socket read failed; switching to single-buffer mode.\nUpgrade to JRE 1.5 (5.0) series to fix this problem!" );
        }
        
        throw ioe;
      }
    }
    else {
      //single-buffer mode
      for( int i=array_offset; i < (array_offset + length); i++ ) {
        int data_length = buffers[ i ].remaining();
        int read = socket_channel.read( buffers[ i ] );
        bytes_read += read;
        if( read < data_length ) {
          break;
        }
      }
    }    

    if( bytes_read < 0 ) {
      is_ready_for_read = false;
      throw new IOException( "end of stream on socket read" );
    }
    
    if( bytes_read == 0 ) {
      requestReadSelect();
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
    if( has_been_closed ) {
      has_been_closed_error = "establishOutboundConnection():: transport has already been closed";
      Debug.out( has_been_closed_error );
    }
    
    if( socket_channel != null ) {  //already connected
      Debug.out( "socket_channel != null" );
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
        if( has_been_closed ) {  //closed between select ops
          has_been_closed_error = "connectSuccess():: transport has already been closed";
          Debug.out( has_been_closed_error );
          NetworkManager.getSingleton().getConnectDisconnectManager().closeConnection( socket_channel );  //just close it
          return;
        }
        
        socket_channel = channel;
        connect_request_key = null;
        description = ( is_inbound_connection ? "R" : "L" ) + ": " + channel.socket().getInetAddress().getHostAddress() + ": " + channel.socket().getPort();

        if( use_proxy ) {  //proxy server connection established, login
          LGLogger.log( "Socket connection established to proxy server [" +description+ "], login initiated..." );
          
          new ProxyLoginHandler( transport_instance, address, new ProxyLoginHandler.ProxyListener() {
            public void connectSuccess() {
              LGLogger.log( "Proxy [" +description+ "] login successful." );
              registerSelectHandling();
              listener.connectSuccess();
            }
            
            public void connectFailure( Throwable failure_msg ) {
              listener.connectFailure( failure_msg );
            }
          });
        }
        else {  //direct connection established, notify
          registerSelectHandling();
          listener.connectSuccess();
        }
      }

      public void connectFailure( Throwable failure_msg ) {
        connect_request_key = null;
        listener.connectFailure( failure_msg );
      }
    };
    
    connect_request_key = connect_listener;
    
    InetSocketAddress to_connect = use_proxy ? ProxyLoginHandler.SOCKS_SERVER_ADDRESS : address;
    
    NetworkManager.getSingleton().getConnectDisconnectManager().requestNewConnection( to_connect, connect_listener );
  }
  
    
  
  

  private void setTransportBuffersSize( int size_in_bytes ) {
    try{
      socket_channel.socket().setSendBufferSize( size_in_bytes );
      socket_channel.socket().setReceiveBufferSize( size_in_bytes );
      
      int snd_real = socket_channel.socket().getSendBufferSize();
      int rcv_real = socket_channel.socket().getReceiveBufferSize();
      
      LGLogger.log( "Setting new transport [" +description+ "] buffer sizes: SND=" +size_in_bytes+ " [" +snd_real+ "] , RCV=" +size_in_bytes+ " [" +rcv_real+ "]" );
    }
    catch( Throwable t ) {
      Debug.out( t );
    }
  }
  
  
  /**
   * Set the transport to the given speed mode.
   * @param mode to change to
   */
  public void setTransportMode( int mode ) {
    if( mode == transport_mode )  return;  //already in mode
    
    switch( mode ) {
      case TRANSPORT_MODE_NORMAL:
        setTransportBuffersSize( 8 * 1024 );
        break;
        
      case TRANSPORT_MODE_FAST:
        setTransportBuffersSize( 64 * 1024 );
        break;
        
      case TRANSPORT_MODE_TURBO:
        setTransportBuffersSize( 512 * 1024 );
        break;
        
      default:
        Debug.out( "invalid transport mode given: " +mode );
    }
    
    transport_mode = mode;
  }
  
 
  /**
   * Get the transport's speed mode.
   * @return current mode
   */
  public int getTransportMode() {  return transport_mode;  }
  
  

  
  /**
   * Close the transport connection.
   */
  public void close() {
    has_been_closed = true;
    
    if( connect_request_key != null ) {
      NetworkManager.getSingleton().getConnectDisconnectManager().cancelRequest( connect_request_key );
    }
    
    is_ready_for_read = false;
    is_ready_for_write = false;

    if( socket_channel != null ){
      NetworkManager.getSingleton().getReadSelector().cancel( socket_channel );
      NetworkManager.getSingleton().getWriteSelector().cancel( socket_channel );
      NetworkManager.getSingleton().getConnectDisconnectManager().closeConnection( socket_channel );
      socket_channel = null;
    }
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
