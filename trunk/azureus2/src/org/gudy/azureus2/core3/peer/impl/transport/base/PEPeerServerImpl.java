/*
 * File    : PEServerImpl.java
 * Created : 21-Oct-2003
 * By      : stuff
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

package org.gudy.azureus2.core3.peer.impl.transport.base;

import java.net.*;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.peer.impl.*;

/**
 * The Bittorrent server to accept incoming connections.
 * 
 * @author Olivier
 *
 */
public class 
PEPeerServerImpl
	extends 	Thread 
	implements  PEPeerServerHelper
{
  public static final int componentID = 4;
  public static final int evtLyfeCycle = 0;
  public static final int evtNewConnection = 1;
  public static final int evtErrors = 2;

  int TCPListenPort;
  private ServerSocketChannel sck;
  private boolean bContinue;
  private PEPeerServerAdapter adapter;


	 public static PEPeerServer
	 create()
	 {
		 synchronized( PEPeerServerImpl.class ){

		 	 return( new PEPeerServerImpl());
		 }
	 }
	
  public PEPeerServerImpl() {
    super("PEPeerServer");
    String bindIP = COConfigurationManager.getStringParameter("Bind IP", "");
    TCPListenPort = COConfigurationManager.getIntParameter("TCP.Listen.Port", 6881);
    
    sck = null;
    bContinue = true;
    setPriority(Thread.MIN_PRIORITY);

    try {
    	sck = ServerSocketChannel.open();

    	sck.socket().setReuseAddress(true);
      
      String size = System.getProperty("socket.SO_RCVBUF");
      if ( size != null ) sck.socket().setReceiveBufferSize( Integer.parseInt( size ) );
      
    	if (bindIP.length() < 7) {
    		sck.socket().bind(new InetSocketAddress(TCPListenPort));
    	}
    	else {
    		sck.socket().bind(new InetSocketAddress(InetAddress.getByName(bindIP), TCPListenPort));
    	}
    }
    catch (Exception e) {
    	LGLogger.log(
    			componentID,
					evtErrors,
					LGLogger.ERROR,
					"PEPeerServer was unable to bind port " + TCPListenPort + ", reason : " + e);
    	if ( sck != null && sck.isOpen() ) {
    		try {  sck.close();  } catch (Exception ignore){}
    	}
    	sck = null;
    }


    if (sck != null) {
      LGLogger.log(componentID, evtLyfeCycle, LGLogger.INFORMATION, "PEPeerServer is bound on port " + TCPListenPort);
    }
    else {
      LGLogger.log(
        componentID,
        evtLyfeCycle,
        LGLogger.INFORMATION,
        "BT was unable to bind to port " + TCPListenPort);
    }
  }

  public void run() {
    try {
      LGLogger.log(
        componentID,
        evtLyfeCycle,
        LGLogger.INFORMATION,
        "PEPeerServer is ready to accept incoming connections");
      while (bContinue) {
        
        SocketChannel sckClient = sck.accept();
        
        if (sckClient != null) {
          LGLogger.log(
            componentID,
            evtNewConnection,
            LGLogger.RECEIVED,
            "PEPeerServer has accepted an incoming connection from : "
            + sckClient.socket().getInetAddress().getHostAddress());
          
          String size = System.getProperty("socket.SO_SNDBUF");
          if ( size != null ) sckClient.socket().setSendBufferSize( Integer.parseInt( size ) );

          String ip_tos = System.getProperty("socket.IPTOS");
          if ( ip_tos != null ) sckClient.socket().setTrafficClass( Integer.parseInt( ip_tos ) );
          //System.out.println( "iTOS=" + sckClient.socket().getTrafficClass() );
          
          sckClient.configureBlocking(false);
          
          adapter.addPeerTransport(sckClient);
          
          sckClient = null;
        }
        else {
          LGLogger.log(
              componentID,
              evtLyfeCycle,
				  LGLogger.INFORMATION,
				  "PEPeerServer SocketChannel is null");
          Thread.sleep(1000);
        }
      } 
    }
    catch (Exception e) {
      if (bContinue)
        LGLogger.log(componentID, evtErrors, LGLogger.ERROR, "PEPeerServer has caught an error : " + e);
    }

    LGLogger.log(componentID, evtLyfeCycle, LGLogger.INFORMATION, "PEPeerServer is stopped");
  }

  	public PEPeerTransport
  	createPeerTransport(
		Object		param )
	{
		return( new PEPeerTransportImpl(adapter.getControl(),(SocketChannel)param, null));
	}
	
  	public void 
  	startServer()
  	{
  		setDaemon(true);
  		
  		start();	// Thread method
  	}
  	
  	public void 
  	stopServer() 
  	{
    	bContinue = false;

    		//this will most probably raise an exception ;)
    	try{
    		
      		LGLogger.log(componentID, evtLyfeCycle, LGLogger.INFORMATION, "PEPeerServer is stopping");
      		
      		sck.close();
    	}catch (Exception e) {
    		
      		LGLogger.log(componentID, evtErrors, LGLogger.ERROR, "Error catched while stopping server : " + e);
    	}
    	
  	}
  

  public int getPort() {
    return TCPListenPort;
  }

  public void 
  setServerAdapter(
  	PEPeerServerAdapter _adapter ) 
  {
    adapter = _adapter;
  }
  
  public void
  clearServerAdapter()
  {
  	adapter	= null;
  }
}