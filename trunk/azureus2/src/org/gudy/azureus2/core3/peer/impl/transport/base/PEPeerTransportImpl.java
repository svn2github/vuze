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
import java.nio.channels.SocketChannel;

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
		//The SocketChannel associated with this peer
		
	private SocketChannel socket;

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
    	super(manager, peerId, ip, port, false, fake);
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
  		SocketChannel 	sck ) 
  	{
    	super( 	manager, 
    			null,		// no peer id 
    			sck.socket().getInetAddress().getHostAddress(), 
    			sck.socket().getPort(),
    			true,
    			false ) ;
    
    	socket = sck;
  	}
  
	protected void 
	startConnection()
		throws IOException
	{
		   //Construct the peer's address with ip and port
		        
	   InetSocketAddress peerAddress = new InetSocketAddress(getIp(), getPort());
	   
		   //Create a new SocketChannel, left non-connected
	   
	   socket = SocketChannel.open();
	   
		   //Configure it so it's non blocking
	   
	   socket.configureBlocking(false);
	   
		   	//Initiate the connection
		   	
	   socket.connect(peerAddress);
	}

	protected void
	closeConnection()
  	{
		if (socket != null ){
			
			try {
				
		  		if (socket.isOpen() && socket.socket().isClosed()){
		  			
		  			 System.out.println("ERROR: channel is open but socket is closed"); 
		  		}
		  		
		  		if (!socket.socket().isClosed()){
		  			
		  			 socket.socket().close(); 
		  		}
		  		
		  		if (socket.isOpen()) {
		  			
			 		System.out.println("ERROR: channel still open");
			 		
			 		socket.close();
		  		}
			}catch (Exception e){
				
		  		System.out.println("PeerTransport::closeAll:: closing socket failed: " + getIp() + ":" + getPort());
			}
			
			socket = null;
	  	}
  	}
  
	protected boolean
  	completeConnection()
  
  		throws IOException
  	{
		return(socket.finishConnect());
  	}
  
  	protected int
  	readData(
  		ByteBuffer	buffer )
  	
  		throws IOException
  	{
		return(socket.read(buffer));
  	}
  
  	protected int
  	writeData(
		ByteBuffer	buffer )
  	
		throws IOException
  	{
		return(socket.write(buffer));
  	}
}
