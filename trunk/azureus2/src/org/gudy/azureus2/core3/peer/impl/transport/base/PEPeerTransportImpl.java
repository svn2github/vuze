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

	private SocketChannel 		socket 			= null;
	private volatile			DataReader		data_reader;
	
	
	  /**
	   * The Default Contructor for outgoing connections.
	   * @param manager the manager that will handle this PeerTransport
	   * @param ip the peer Ip Address
	   * @param port the peer port
	   */
  
  	public 
  	PEPeerTransportImpl(
  		PEPeerControl 	manager,
  		String 			ip, 
  		int 			port ) 
 	{
    	super(manager, ip, port, false, null, null );
  	}


	  /**
	   * The default Contructor for incoming connections
	   * @param manager the manager that will handle this PeerTransport
	   * @param sck the SocketChannel that handles the connection
	   */
	  
  	public 
  	PEPeerTransportImpl(
  		PEPeerControl 	manager, 
  		SocketChannel 	sck,
  		byte[]			_leading_data ) 
  	{
    	super( 	manager, 
    			sck.socket().getInetAddress().getHostAddress(), 
    			sck.socket().getPort(),
    			true,
				sck,
    			_leading_data );
      
     	socket 			= sck;
  	}
  
  	protected void
	setupSpeedLimiter()
  	{
  		try{
  			this_mon.enter();
  		
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
  		}finally{
  			
  			this_mon.exit();
  		}
  	}
  	
	public PEPeerTransport
	getRealTransport()
	{
		return( this );
	}
	
  
  
  //TODO
	protected void startConnectionX() {
	  setupSpeedLimiter();
	}


  //TODO
	protected void closeConnectionX() {
    if( data_reader != null ) {
      data_reader.destroy();
      data_reader = null;
    }
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
			
			int	pos = buffer.position(DirectByteBuffer.SS_PEER);
			
			int	len = data_reader_copy.read( socket_copy, buffer );
			
			if ( len > 0 ){
				
				byte[]	trace = new byte[len];
				
				buffer.position(DirectByteBuffer.SS_PEER,pos);
				
				buffer.get( DirectByteBuffer.SS_PEER,trace );
				
				System.out.println( "readData:" + ByteFormatter.nicePrint( trace ));
			}
			
			return( len );
		}else{
			
			return(  data_reader_copy.read( socket_copy, buffer ));
		}

	}

  protected void setChannel( SocketChannel channel ) {
    this.socket = channel;
  }
  
  
}
