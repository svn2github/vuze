/*
 * File    : PEPeerTransportImpl
 * Created : 15-Oct-2003
 * By      : Olivier
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
 
  /*
 * Created on 4 juil. 2003
 *
 */
package org.gudy.azureus2.core3.peer.impl.transport.base;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;


import org.gudy.azureus2.core3.peer.impl.*;
import org.gudy.azureus2.core3.peer.impl.transport.*;
import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.ByteFormatter;



/**
 * @author Olivier
 *
 */
public class 
PEPeerTransportImpl 
	extends 	PEPeerTransportProtocol
{
	private static final boolean	TRACE	= false;
  
  private static boolean enable_efficient_write = true;
	
	private SocketChannel 		socket 			= null;
	private volatile boolean 	connected 		= false;
	private volatile boolean 	connect_error 	= false;
	private volatile 			String msg 		= "";
	private volatile			DataReader		data_reader;
	
	private SocketManager.OutboundConnectionListener listener = null;
  
	
	  /**
	   * The Default Contructor for outgoing connections.
	   * @param manager the manager that will handle this PeerTransport
	   * @param table the graphical table in which this PeerTransport should display its info
	   * @param peerId the other's peerId which will be checked during Handshaking
	   * @param ip the peer Ip Address
	   * @param port the peer port
	   */
  
  	public 
  	PEPeerTransportImpl(
  		PEPeerControl 	manager, 
  		byte[] 			peerId, 
  		String 			ip, 
  		int 			port, 
  		boolean 		fake )
 	{
    	super(manager, peerId, ip, port, false, null, fake);
  	}


	  /**
	   * The default Contructor for incoming connections
	   * @param manager the manager that will handle this PeerTransport
	   * @param table the graphical table in which this PeerTransport should display its info
	   * @param sck the SocketChannel that handles the connection
	   */
	  
  	public 
  	PEPeerTransportImpl(
  		PEPeerControl 	manager, 
  		SocketChannel 	sck,
  		byte[]			_leading_data ) 
  	{
    	super( 	manager, 
    			null,		// no peer id 
    			sck.socket().getInetAddress().getHostAddress(), 
    			sck.socket().getPort(),
    			true,
    			_leading_data,
    			false ) ;
    
     	socket 			= sck;
    				
		setupSpeedLimiter();
	
		connected = true;
  	}
  
  	protected synchronized void
	setupSpeedLimiter()
  	{
  		final DownloadManager	dm = getControl().getDownloadManager();
  		
  		data_reader = (DataReader)dm.getData( "PEPeerTransport::DataReader" );
  		
  		if ( data_reader == null ){
  			
  			data_reader = 
  				DataReaderSpeedLimiter.getSingleton().getDataReader(
  						new DataReaderOwner()
						{
  							public int
  							getMaximumBytesPerSecond()
  							{
  								return( dm.getStats().getMaxDownloadKBSpeed() * 1024 );
  							}
						});
  			
  			dm.setData( "PEPeerTransport::DataReader", data_reader );
  		}
  	}
  	
	public PEPeerTransport
	getRealTransport()
	{
		return( this );
	}
	
  
	protected void startConnection() {
    try {
      connected = false;
      connect_error = false;

      InetSocketAddress address = new InetSocketAddress( getIp(), getPort() );
    
      listener = new SocketManager.OutboundConnectionListener() {
        public void connectionDone( SocketChannel channel, String error_msg ) {
          if ( channel != null ) {
          	
            socket = channel;
            
    		setupSpeedLimiter();

            connected = true;
          }
          else {
            msg = error_msg;
            connect_error = true;
          }
        }
      };
    
      SocketManager.requestOutboundConnection( address, listener );
    }
    catch (Throwable t){
      msg = t.getMessage();
      connect_error = true;
    }
	}


  
	protected void closeConnection() {
    if ( connected ) {
    	if (socket == null) System.out.println("socket = null");
    	SocketManager.closeConnection( socket );
        socket = null;
		data_reader.destroy();
		data_reader = null;
        connected = false;
        return;
    }
    
    if ( !connect_error && listener != null ) {
      SocketManager.cancelOutboundRequest( listener );
      listener = null;
      return;
    }
	}

  
	protected boolean 
	completeConnection() 
		throws IOException 
	{
		if ( connect_error ) {
			throw new IOException(msg);
		}
		return connected;
	}
  
  
	protected int 
	readData( 
		DirectByteBuffer	buffer )
	
		throws IOException 
	{
		DataReader		data_reader_copy	= data_reader;
		SocketChannel	socket_copy			= socket;
		
		if ( data_reader_copy == null ){
			
			throw( new IOException( "Not connected - data reader is null" ));
		}	
		
		if ( socket_copy == null ){
			
			throw( new IOException( "Not connected - socket is null" ));
		}
		
		if ( TRACE ){
			
			int	pos = buffer.position();
			
			int	len = data_reader_copy.read( socket_copy, buffer );
			
			if ( len > 0 ){
				
				byte[]	trace = new byte[len];
				
				buffer.position(pos);
				
				buffer.get( trace );
				
				System.out.println( "readData:" + ByteFormatter.nicePrint( trace ));
			}
			
			return( len );
		}else{
			
			return(  data_reader_copy.read( socket_copy, buffer ));
		}

	}
  
  
	protected int 
	writeData( 
		DirectByteBuffer	buffer ) 
	
		throws IOException 
	{
		SocketChannel	socket_copy			= socket;
				
		if ( socket_copy == null ){
			
			throw( new IOException( "Not connected - socket is null" ));
		}		
		
		if ( TRACE ){
			
			int	pos = buffer.position();
			
			int	len = buffer.write( socket_copy );
			
			if ( len > 0 ){
				
				byte[]	trace = new byte[len];
				
				buffer.position(pos);
				
				buffer.get( trace );
				
				System.out.println( "writeData:" + ByteFormatter.nicePrint( trace ));
			}
			
			return( len );
		}else{
			
			return(  buffer.write(socket_copy));
		}
	}
  
  
  ///// implementation of PeerConnection: 
  
  public int read( ByteBuffer buffer ) throws IOException {
    return socket.read( buffer );
  }
  
  public long write( ByteBuffer[] buffers, int array_offset, int array_length ) throws IOException {
    if( enable_efficient_write ) {
      try {
        return socket.write( buffers, array_offset, array_length );
      }
      catch( IOException e ) {
        //a bug only fixed in Tiger (1.5 series):
        //http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4854354
        if( e.getMessage().equals( "A non-blocking socket operation could not be completed immediately" ) ) {
          enable_efficient_write = false;
          System.out.println( "ERROR: Multi-buffer socket write failed; switching to single-buffer mode. Upgrade to JRE 1.5 series to fix." );
        }
        throw e;
      }
    }
    
    //single-buffer mode
    long written_sofar = 0;
    for( int i=array_offset; i < array_length; i++ ) {
      int data_length = buffers[ i ].remaining();
      int written = socket.write( buffers[ i ] );
      written_sofar += written;
      if( written < data_length )  break;
    }
    return written_sofar;
  }
  
  public String getDescription() {
    return socket.socket().getInetAddress().getHostAddress() + ":" + socket.socket().getPort();
  }
}
