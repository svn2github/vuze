/*
 * Created on May 8, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.networkmanager.impl.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.networkmanager.*;
import com.aelitis.azureus.core.networkmanager.impl.TCPTransportHelperFilter;
import com.aelitis.azureus.core.networkmanager.impl.TCPTransportHelperFilterFactory;
import com.aelitis.azureus.core.networkmanager.impl.TransportCryptoManager;
import com.aelitis.azureus.core.networkmanager.impl.TransportStats;
import com.aelitis.azureus.core.networkmanager.impl.TransportCryptoManager.HandshakeListener;
import com.aelitis.azureus.core.networkmanager.impl.tcp.ConnectDisconnectManager.ConnectListener;
import com.aelitis.azureus.core.networkmanager.impl.tcp.ProxyLoginHandler.ProxyListener;



/**
 * Represents a peer TCP transport connection (eg. a network socket).
 */
public class TCPTransportImpl implements Transport {
	private static final LogIDs LOGID = LogIDs.NET;
  
  protected ProtocolEndpointTCP		protocol_endpoint;
  protected TCPTransportHelperFilter filter;

  protected volatile boolean is_ready_for_write = false;
  protected volatile boolean is_ready_for_read = false;
  protected Throwable write_select_failure = null;
  protected Throwable read_select_failure = null;

  
  private ConnectDisconnectManager.ConnectListener connect_request_key = null;
  private String description = "<disconnected>";
  private ByteBuffer data_already_read = null;
  private final boolean is_inbound_connection;
  
  private int transport_mode = TRANSPORT_MODE_NORMAL;

  public volatile boolean has_been_closed = false;
  
  private static final TransportStats stats = AEDiagnostics.TRACE_TCP_TRANSPORT_STATS ? new TransportStats() : null;
  
  
  private boolean 	connect_with_crypto;
  private byte[]	shared_secret;
  private int		fallback_count;
  private final boolean fallback_allowed;

  private volatile EventWaiter read_waiter;
  private volatile EventWaiter write_waiter;
  
  /**
   * Constructor for disconnected (outbound) transport.
   */
  public TCPTransportImpl( ProtocolEndpointTCP endpoint, boolean use_crypto, boolean allow_fallback, byte[] _shared_secret ) 
  {
	  protocol_endpoint = endpoint;
	  filter = null;
    is_inbound_connection = false;
    connect_with_crypto = use_crypto;
    shared_secret		= _shared_secret;
    fallback_allowed  = allow_fallback;
  }
  
  
  /**
   * Constructor for connected (inbound) transport.
   * @param channel connection
   * @param already_read bytes from the channel
   */
  public TCPTransportImpl( ProtocolEndpointTCP endpoint, TCPTransportHelperFilter	filter, ByteBuffer already_read ) 
  {
	protocol_endpoint = endpoint;
    this.filter = filter;
    this.data_already_read = already_read;   
    is_inbound_connection = true;
    connect_with_crypto = false;  //inbound connections will automatically be using crypto if necessary
    fallback_allowed = false;
    description = ( is_inbound_connection ? "R" : "L" ) + ": " + filter.getSocketChannel().socket().getInetAddress().getHostAddress() + ": " + filter.getSocketChannel().socket().getPort();
    
    registerSelectHandling();
  }
  
  
  /**
   * Inject the given already-read data back into the read stream.
   * @param bytes_already_read data
   */
  public void setAlreadyRead( ByteBuffer bytes_already_read ) {
    if( bytes_already_read != null && bytes_already_read.hasRemaining() ) {
      data_already_read = bytes_already_read;
    }
  }
  
  
  /**
   * Get the socket channel used by the transport.
   * @return the socket channel
   */
  public SocketChannel getSocketChannel() {  return filter.getSocketChannel();  }
  
  public TransportEndpoint
  getTransportEndpoint()
  {
	  return( new TransportEndpointTCP( protocol_endpoint, filter.getSocketChannel()));
  }
  
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
  public boolean isReadyForWrite( EventWaiter waiter ) {
	  write_waiter = waiter;
	  return is_ready_for_write;  }
  
  
  /**
   * Is the transport ready to read,
   * i.e. will a read request result in >0 bytes read.
   * @return true if the transport is read ready, false if not yet ready
   */
  public boolean isReadyForRead( EventWaiter waiter ) {
	  read_waiter = waiter;
	  return is_ready_for_read;  }
    
  
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
  	if( write_select_failure != null )  throw new IOException( "write_select_failure: " + write_select_failure.getMessage() );
    
  	if( filter == null )  return 0;
  	
  	long written = filter.write( buffers, array_offset, length );

  	if( stats != null )  stats.bytesWritten( (int)written );  //TODO
       
  	if( written < 1 )  requestWriteSelect();
      
  	return written;
  }
  

  
  private void requestWriteSelect() {
    is_ready_for_write = false;
    if( filter != null ){
      NetworkManager.getSingleton().getWriteSelector().resumeSelects( filter.getSocketChannel() );
    }
  }
  
  
  private void requestReadSelect() {
    is_ready_for_read = false;
    if( filter != null ){
      NetworkManager.getSingleton().getReadSelector().resumeSelects( filter.getSocketChannel() );
    }
  } 
  

  
  private void registerSelectHandling() {
    if( filter == null ) {
      Debug.out( "ERROR: registerSelectHandling():: socket_channel == null" );
      return;
    }

    //read selection
    NetworkManager.getSingleton().getReadSelector().register( filter.getSocketChannel(), new VirtualChannelSelector.VirtualSelectorListener() {
      public boolean selectSuccess( VirtualChannelSelector selector, SocketChannel sc,Object attachment ) {
    	boolean	progress = !is_ready_for_read;
        is_ready_for_read = true;
        EventWaiter rw = read_waiter;
        if ( rw != null ){
        	rw.eventOccurred();
        }
        return progress;
      }
      
      public void selectFailure( VirtualChannelSelector selector, SocketChannel sc,Object attachment, Throwable msg ) {
        read_select_failure = msg;
        is_ready_for_read = true;  //set to true so that the next read attempt will throw an exception
      }
    }, null );
    
    
    //write selection
    NetworkManager.getSingleton().getWriteSelector().register( filter.getSocketChannel(), new VirtualChannelSelector.VirtualSelectorListener() {
      public boolean selectSuccess( VirtualChannelSelector selector, SocketChannel sc,Object attachment ) {
    	boolean	progress = !is_ready_for_write;
        is_ready_for_write = true;
        EventWaiter ww = write_waiter;
        if ( ww != null ){
        	ww.eventOccurred();
        }
        return progress;
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
 
        
    long bytes_read = filter.read( buffers, array_offset, length );

    if( stats != null )  stats.bytesRead( (int)bytes_read );  //TODO
    
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
  public void connectOutbound( final ConnectListener listener ) {
    if( has_been_closed )  return;
    
    if( filter != null ) {  //already connected
      Debug.out( "socket_channel != null" );
      listener.connectSuccess( this );
      return;
    }
    
    final boolean use_proxy = COConfigurationManager.getBooleanParameter( "Proxy.Data.Enable" );
    final TCPTransportImpl transport_instance = this;    
    
    final InetSocketAddress	address = protocol_endpoint.getAddress();
    
    ConnectDisconnectManager.ConnectListener connect_listener = new ConnectDisconnectManager.ConnectListener() {
      public void connectAttemptStarted() {
        listener.connectAttemptStarted();
      }
      
      public void connectSuccess( final SocketChannel channel ) {
      	if( channel == null ) {
      		String msg = "connectSuccess:: given channel == null";
      		Debug.out( msg );
      		listener.connectFailure( new Exception( msg ) );
      		return;
      	}
      	
        if( has_been_closed ) {  //closed between select ops
          NetworkManager.getSingleton().getConnectDisconnectManager().closeConnection( channel );  //just close it
          return;
        }
        
        connect_request_key = null;
        description = ( is_inbound_connection ? "R" : "L" ) + ": " + channel.socket().getInetAddress().getHostAddress() + ": " + channel.socket().getPort();

        if( use_proxy ) {  //proxy server connection established, login
        	Logger.log(new LogEvent(LOGID,"Socket connection established to proxy server [" +description+ "], login initiated..."));
          
        		// set up a transparent filter for socks negotiation
        	
          filter = TCPTransportHelperFilterFactory.createTransparentFilter( channel );
      		
          new ProxyLoginHandler( transport_instance, address, new ProxyLoginHandler.ProxyListener() {
            public void connectSuccess() {
            	Logger.log(new LogEvent(LOGID, "Proxy [" +description+ "] login successful." ));
              handleCrypto( address, channel, listener );
            }
            
            public void connectFailure( Throwable failure_msg ) {
            	NetworkManager.getSingleton().getConnectDisconnectManager().closeConnection( channel );
              listener.connectFailure( failure_msg );
            }
          });
        }
        else {  //direct connection established, notify
        	handleCrypto( address, channel, listener );
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
  
    
  
  
  protected void handleCrypto( final InetSocketAddress address, final SocketChannel channel, final ConnectListener listener ) {  	
  	if( connect_with_crypto ) {
    	//attempt encrypted transport
    	TransportCryptoManager.getSingleton().manageCrypto( channel, shared_secret, false, new TransportCryptoManager.HandshakeListener() {
    		public void handshakeSuccess( TCPTransportHelperFilter _filter ) {    			
    			//System.out.println( description+ " | crypto handshake success [" +_filter.getName()+ "]" );     			
    			filter = _filter; 
    			if ( Logger.isEnabled()){
    		      Logger.log(new LogEvent(LOGID, "Outgoing TCP stream to " + channel.socket().getRemoteSocketAddress() + " established, type = " + filter.getName()));
    			}
    			
        	registerSelectHandling();
          listener.connectSuccess( TCPTransportImpl.this );
    		}

    		public void handshakeFailure( Throwable failure_msg ) {        	
        	if( fallback_allowed && NetworkManager.OUTGOING_HANDSHAKE_FALLBACK_ALLOWED ) {        		
        		if( Logger.isEnabled() ) Logger.log(new LogEvent(LOGID, description+ " | crypto handshake failure [" +failure_msg.getMessage()+ "], attempting non-crypto fallback." ));
        		connect_with_crypto = false;
        		fallback_count++;
        		NetworkManager.getSingleton().getConnectDisconnectManager().closeConnection( channel );  //just close it
        		close();
        		has_been_closed = false;
        		connectOutbound( listener );
        	}
        	else {
        		NetworkManager.getSingleton().getConnectDisconnectManager().closeConnection( channel );
        		listener.connectFailure( failure_msg );
        	}
        }
    		
    		public int
    		getMaximumPlainHeaderLength()
    		{
    			throw( new RuntimeException());	// this is outgoing
    		}
		
    		public int
    		matchPlainHeader(
    				ByteBuffer			buffer )
    		{
    			throw( new RuntimeException());	// this is outgoing
    		}
    	});
  	}
  	else {  //no crypto
  		//if( fallback_count > 0 ) {
  		//	System.out.println( channel.socket()+ " | non-crypto fallback successful!" );
  		//}
  		filter = TCPTransportHelperFilterFactory.createTransparentFilter( channel );
  		
		if ( Logger.isEnabled()){
		  Logger.log(new LogEvent(LOGID, "Outgoing TCP stream to " + channel.socket().getRemoteSocketAddress() + " established, type = " + filter.getName() + ", fallback = " + (fallback_count==0?"no":"yes" )));
		}
    	registerSelectHandling();
      listener.connectSuccess( this );
  	}
  }
  
  
  

  private void setTransportBuffersSize( int size_in_bytes ) {
  	if( filter == null ) {
  		Debug.out( "socket_channel == null" );
  		return;
  	}
  	
    try{
    	filter.getSocketChannel().socket().setSendBufferSize( size_in_bytes );
    	filter.getSocketChannel().socket().setReceiveBufferSize( size_in_bytes );
      
      int snd_real = filter.getSocketChannel().socket().getSendBufferSize();
      int rcv_real = filter.getSocketChannel().socket().getReceiveBufferSize();
      
      Logger.log(new LogEvent(LOGID, "Setting new transport [" + description
					+ "] buffer sizes: SND=" + size_in_bytes + " [" + snd_real
					+ "] , RCV=" + size_in_bytes + " [" + rcv_real + "]"));
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
  
  public String getEncryption(){ return( filter==null?"":filter.getName()); }

  public InetSocketAddress 
  getRemoteAddress()
  {
	  return( protocol_endpoint.getAddress());
  }
  
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

    if( filter != null ){
      NetworkManager.getSingleton().getReadSelector().cancel( filter.getSocketChannel() );
      NetworkManager.getSingleton().getWriteSelector().cancel( filter.getSocketChannel() );
      NetworkManager.getSingleton().getConnectDisconnectManager().closeConnection( filter.getSocketChannel() );
      
      filter = null;
    }
  }
     
  
}
