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
import java.net.*;

import org.gudy.azureus2.core3.peer.impl.*;
import org.gudy.azureus2.core3.peer.impl.transport.*;
import org.gudy.azureus2.core3.util.*;
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
  
	protected void 
	startConnection()
		throws IOException
	{
		   //Construct the peer's address with ip and port
		        
	   InetSocketAddress peerAddress = new InetSocketAddress(getIp(), getPort());
	   
		   //Create a new SocketChannel, left non-connected
	   
	   socket = SocketChannel.open();
     
	   String bindIP = COConfigurationManager.getStringParameter("Bind IP", "");
	   if (bindIP.length() > 6) {
	     socket.socket().bind(new InetSocketAddress(InetAddress.getByName(bindIP), 0));
	   }
	   
		   //Configure it so it's non blocking
	   
	   socket.configureBlocking(false);
	   
		   	//Initiate the connection
		   	
	   socket.connect(peerAddress);
	}

	protected void
	closeConnection() {
    
	  if (socket == null) {
	    Debug.out("socket already null");
	    return;
	  }
    
	  try {
	    Socket sck = socket.socket();

	    if (socket.isOpen()) {
	      socket.close();
         if (!sck.isInputShutdown()) {
           Debug.out("shutting down input");
           sck.shutdownInput();
         }
         if (!sck.isOutputShutdown()) {
           Debug.out("shutting down output");
           sck.shutdownOutput();
         }
	    }
      
	    if (!sck.isClosed()){
	      Debug.out("sck not already closed");
			sck.close();
		 } 
	  }
	  catch (Exception e){
	    String msg = "exception trying to close socket:\n";
       msg = msg + " cOpen=" + socket.isOpen();
       msg = msg + " cConnected=" + socket.isConnected();
       msg = msg + " cPending=" + socket.isConnectionPending();
       msg = msg + " sClosed=" + socket.socket().isClosed();
       msg = msg + " sConnected=" + socket.socket().isConnected();
       msg = msg + " sInptShtdwn=" + socket.socket().isInputShutdown();
       msg = msg + " sOutptShtdwn=" + socket.socket().isOutputShutdown();
       msg = msg + " sBound=" + socket.socket().isBound();
       msg = msg + " Exception:\n" + e.getMessage();
       Debug.out(msg);
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
      if (!socket.finishConnect()) {
        String msg = "socket.finishConnect=false::";
        msg = msg + " cOpen=" + socket.isOpen();
        msg = msg + " cConnected=" + socket.isConnected();
        msg = msg + " cPending=" + socket.isConnectionPending();
        msg = msg + " sClosed=" + socket.socket().isClosed();
        msg = msg + " sConnected=" + socket.socket().isConnected();
        Debug.out(msg);
        return -1;
      } 		
		return(socket.read(buffer));
  	}
  
  	protected int
  	writeData(
		ByteBuffer	buffer )
  	
		throws IOException
  	{
      if (!socket.finishConnect()) {
        String msg = "socket.finishConnect=false::";
        msg = msg + " cOpen=" + socket.isOpen();
        msg = msg + " cConnected=" + socket.isConnected();
        msg = msg + " cPending=" + socket.isConnectionPending();
        msg = msg + " sClosed=" + socket.socket().isClosed();
        msg = msg + " sConnected=" + socket.socket().isConnected();
        Debug.out(msg);
        return -1;
      }
		return(socket.write(buffer));
  	}
}
