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

  //  private static final int MAX_CONNECTIONS = 50;
  //private int port;
  int TCPListenPort;
  private ServerSocketChannel sck;
  private boolean bContinue;
  private PEPeerServerAdapter adapter;

  //private static int instanceCount = 0;

  /*
   public static boolean portsFree() {
    int lp = COConfigurationManager.getIntParameter("Low Port", 6881);
	
	boolean sp = COConfigurationManager.getBooleanParameter("Server.shared.port", true);

    int hp;
    
    if ( sp ){
    	hp = lp;
    }else{
    	hp = COConfigurationManager.getIntParameter("High Port", 6889);
    }
    
    int lowPort = Math.min(lp, hp);
    int highPort = Math.max(lp, hp);
	 return Math.abs(lowPort - highPort) + 1 > instanceCount;
   }
   */

	 public static PEPeerServer
	 create()
	 {
		 synchronized( PEPeerServerImpl.class ){
			
			 //if ( portsFree()){
				
				 return( new PEPeerServerImpl());
				
			 //}else{
				
			 //	 return( null );
			 //}
		 }
	 }
	
  public PEPeerServerImpl() {
    super("PEPeerServer");
    //Will create a Server on any socket from 6881 to 6889
    String bindIP = COConfigurationManager.getStringParameter("Bind IP", "");
    TCPListenPort = COConfigurationManager.getIntParameter("TCP.Listen.Port", 6881);
    
    //int lp = COConfigurationManager.getIntParameter("Low Port", 6881);
    //int hp = COConfigurationManager.getIntParameter("High Port", 6889);
    //int lowPort = Math.min(lp, hp);
    //int highPort = Math.max(lp, hp);
    //if (COConfigurationManager.getBooleanParameter("Server.shared.port", true)) {
      //Use real Low Port even if it's not the real lowest port
      //port = lp;
    //} else  {
      //Otherwise, use the lowest port
    	//port = lowPort;
    //}
    sck = null;
    bContinue = true;
    setPriority(Thread.MIN_PRIORITY);
    //while (sck == null && port <= highPort) {
      try {
        sck = ServerSocketChannel.open();
        
        //this should only be set when using a single shared port config
        //if (COConfigurationManager.getBooleanParameter("Server.shared.port", true)) {
          // Allow the server socket to be immediately re-used, if not yet released by the OS
          sck.socket().setReuseAddress(true);
        //}
        
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
        //port++;
        sck = null;
      }
    //}

    if (sck != null) {
      LGLogger.log(componentID, evtLyfeCycle, LGLogger.INFORMATION, "PEPeerServer is bound on port " + TCPListenPort);
      //instanceCount++;
    }
    else {
      LGLogger.log(
        componentID,
        evtLyfeCycle,
        LGLogger.INFORMATION,
        "BT was unable to bind to port " + TCPListenPort);
      //port = 0;
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
            LGLogger.INFORMATION,
            "PEPeerServer has accepted an incoming connection from : "
            + sckClient.socket().getInetAddress().getHostAddress());
          
          sckClient.configureBlocking(false);
                    
          adapter.addPeerTransport(sckClient);
        }
        else {
          LGLogger.log(
              componentID,
              evtLyfeCycle,
				  LGLogger.INFORMATION,
				  "PEPeerServer SocketChannel is null");
        }
      } 
    }
    catch (Exception e) {
      if (bContinue)
        LGLogger.log(componentID, evtErrors, LGLogger.ERROR, "PEPeerServer has catched an error : " + e);
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
    	
    	//instanceCount--;
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
}