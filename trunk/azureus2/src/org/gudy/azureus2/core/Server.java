package org.gudy.azureus2.core;

import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.peer.*;

/**
 * The Bittorrent server to accept incoming connections.
 * 
 * @author Olivier
 *
 */
public class Server extends Thread {
  public static final int componentID = 4;
  public static final int evtLyfeCycle = 0;
  public static final int evtNewConnection = 1;
  public static final int evtErrors = 2;

  //  private static final int MAX_CONNECTIONS = 50;
  private int port;
  private ServerSocketChannel sck;
  private boolean bContinue;
  private PEPeerManager manager;

  private static int instanceCount = 0;

  public Server() {
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
          manager.addPeer(sckClient);
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

  public static boolean portsFree() {
    return Math.abs(
      COConfigurationManager.getIntParameter("Low Port", 6881)
        - COConfigurationManager.getIntParameter("High Port", 6889))
      + 1
      > instanceCount;
  }

  public int getPort() {
    return port;
  }

  public void setManager(PEPeerManager manager) {
    this.manager = manager;
  }

}