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
import java.net.*;


import org.gudy.azureus2.core3.peer.impl.*;
import org.gudy.azureus2.core3.peer.impl.transport.*;
import org.gudy.azureus2.core3.config.*;


/**
 * @author Olivier
 *
 */
public class 
PEPeerTransportImpl 
	extends PEPeerTransportProtocol
{
		//The SocketChannel associated with this peer
		
	private SocketChannel 	socket;
  private volatile boolean connected = false;
  private volatile boolean connect_error = false;
  private String error_msg = "";
  
	
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
  	}
  
	public PEPeerTransport
	getRealTransport()
	{
		return( this );
	}
	
  
	protected void startConnection() throws IOException {
    
		Thread connect = new Thread("Socket Connect") {
			public void run() {
				Selector sel = null;
				try {
					socket = SocketChannel.open();
					
					socket.socket().setReceiveBufferSize(PEPeerTransport.RECEIVE_BUFF_SIZE);
					
					String bindIP = COConfigurationManager.getStringParameter("Bind IP", "");
					if (bindIP.length() > 6) {
						socket.socket().bind(new InetSocketAddress(InetAddress.getByName(bindIP), 0));
					}
					
					socket.configureBlocking(false);
					
					InetSocketAddress peerAddress = new InetSocketAddress(getIp(), getPort());
					
					sel = Selector.open();
					
					socket.connect( peerAddress );
					
					socket.register( sel, SelectionKey.OP_CONNECT );
					
					int keys = sel.select(60*1000); //60 sec timout
					
					if ( keys > 0 ) { // Connection established
						sel.close();
						if ( socket.finishConnect() ) {
							connected = true;
						}
						else {
							error_msg = "finishConnect() failed";
							connect_error = true;
						}
					}
					else {
						sel.close();
						closeConnection();
						error_msg = "failed to connect within 60 sec";
						connect_error = true;
					}
					
				} catch (Throwable t) {
					if (sel != null) {
						try{  sel.close();  } catch (Exception e) { e.printStackTrace(); }
					}
					error_msg = t.getMessage();
					connect_error = true;
				}
			}
		};
		connect.setDaemon( true );
		connect.start();
    
	}

  
	protected void closeConnection() {
		try {  
      if (socket != null) {
        socket.close();
        socket = null;
      }
    }
    catch (Throwable e) { e.printStackTrace(); }
	}

  
	protected boolean completeConnection() throws IOException {
    if ( connect_error ) {
      throw new IOException(error_msg);
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
