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



/**
 * @author Olivier
 *
 */
public class 
PEPeerTransportImpl 
	extends PEPeerTransportProtocol
{

	private SocketChannel 	socket = null;
  private volatile boolean connected = false;
  private volatile boolean connect_error = false;
  private volatile String msg = "";
  SocketManager.OutboundConnectionListener listener = null;
  
	
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
      connected = true;
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
      connected = false;
      return;
    }
    
    if ( !connect_error && listener != null ) {
      SocketManager.cancelOutboundRequest( listener );
      listener = null;
      return;
    }
	}

  
	protected boolean completeConnection() throws IOException {
    if ( connect_error ) {
      throw new IOException(msg);
    }
    return connected;
	}
  
  
	protected int readData( ByteBuffer	buffer ) throws IOException {
		return(socket.read(buffer));
	}
  
  
	protected int writeData( ByteBuffer	buffer ) throws IOException {
		return(socket.write(buffer));
	}
  
}
