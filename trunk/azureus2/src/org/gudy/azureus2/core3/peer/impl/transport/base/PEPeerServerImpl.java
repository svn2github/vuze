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

import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.peer.impl.PEPeerManagerImpl;

/**
 * The Bittorrent server to accept incoming connections.
 * 
 * @author Olivier
 *
 */
public class 
PEPeerServerImpl
	extends 	Thread 
	implements  PEPeerServer
{
  public static final int componentID = 4;
  public static final int evtLyfeCycle = 0;
  public static final int evtNewConnection = 1;
  public static final int evtErrors = 2;

  //  private static final int MAX_CONNECTIONS = 50;
  private int port;
  private ServerSocketChannel sck;
  private boolean bContinue;
  private PEPeerManagerImpl manager;

  private static int instanceCount = 0;

  public static boolean portsFree() {
	 return Math.abs(
	   COConfigurationManager.getIntParameter("Low Port", 6881)
		 - COConfigurationManager.getIntParameter("High Port", 6889))
	   + 1
	   > instanceCount;
   }

	 public static PEPeerServer
	 create()
	 {
		 synchronized( PEPeerServerImpl.class ){
			
			 if ( portsFree()){
				
				 return( new PEPeerServerImpl());
				
			 }else{
				
				 return( null );
			 }
		 }
	 }
	
  public PEPeerServerImpl() {
    super("Bt Server");
    //Will create a Server on any socket from 6881 to 6889
    int lowPort = COConfigurationManager.getIntParameter("Low Port", 6881);
    int highPort = COConfigurationManager.getIntParameter("High Port", 6889);
    lowPort = Math.min(lowPort, highPort);
    highPort = Math.max(lowPort, highPort);
    port = lowPort;
    sck = null;
    bContinue = true;
    setPriority(Thread.MIN_PRIORITY);
    while (sck == null && port <= highPort) {
      try {
        sck = ServerSocketChannel.open();
        sck.socket().setReuseAddress(true);
        sck.socket().bind(new InetSocketAddress(port));
      }
      catch (Exception e) {
        LGLogger.log(
          componentID,
          evtErrors,
          LGLogger.ERROR,
          "BT Server was unable to bind port " + port + ", reason : " + e);
        port++;
        sck = null;
      }
    }

    if (sck != null) {
      LGLogger.log(componentID, evtLyfeCycle, LGLogger.INFORMATION, "BT Server is bound on port " + port);
      instanceCount++;
    }
    else {
      LGLogger.log(
        componentID,
        evtLyfeCycle,
        LGLogger.INFORMATION,
        "BT was unable to bind on a port from " + lowPort + " to " + highPort);
      port = 0;
    }
  }

  public void run() {
    try {
      sck.configureBlocking(false);
      LGLogger.log(
        componentID,
        evtLyfeCycle,
        LGLogger.INFORMATION,
        "BT Server is ready to accept incoming connections");
      while (bContinue) {
        SocketChannel sckClient = sck.accept();
        if (sckClient != null) {
          LGLogger.log(
            componentID,
            evtNewConnection,
            LGLogger.INFORMATION,
            "BT Server has accepted an incoming connection from : "
              + sckClient.socket().getInetAddress().getHostAddress());
          sckClient.configureBlocking(false);
          manager.addPeerTransport(new PEPeerTransportImpl(manager,sckClient));
        }
        else {
          Thread.sleep(50);
        }    
      }    
    }
    catch (Exception e) {
      if (bContinue)
        LGLogger.log(componentID, evtErrors, LGLogger.ERROR, "BT Server has catched an error : " + e);
    }

    LGLogger.log(componentID, evtLyfeCycle, LGLogger.INFORMATION, "BT Server is stopped");
  }

  public void startServer(){
  	start();	// Thread method
  }
  public void stopServer() {
    bContinue = false;

    //this will most probably raise an exception ;)
    try {
      LGLogger.log(componentID, evtLyfeCycle, LGLogger.INFORMATION, "BT Server is stopping");
      sck.close();
    }
    catch (Exception e) {
      LGLogger.log(componentID, evtErrors, LGLogger.ERROR, "Error catched while stopping server : " + e);
    }
    instanceCount--;
  }
  

  public int getPort() {
    return port;
  }

  public void setManager(PEPeerManager manager) {
    this.manager = (PEPeerManagerImpl)manager;
  }

}