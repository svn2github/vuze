package org.gudy.azureus2.core;


import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * The Bittorrent server to accept incoming connections.
 * 
 * @author Olivier
 *
 */
public class Server extends Thread
{
  public static final int componentID = 4;  
  public static final int evtLyfeCycle = 0;
  public static final int evtNewConnection = 1;
  public static final int evtErrors = 2;
  
//  private static final int MAX_CONNECTIONS = 50;
  private int port;
  private ServerSocketChannel sck;
  private boolean bContinue;
  private PeerManager manager;
  
  
  public Server()
  {
    super("Bt Server");
    //Will create a Server on any socket from 6881 to 6889
    int lowPort = ConfigurationManager.getInstance().getIntParameter("Low Port",6881);
    int highPort = ConfigurationManager.getInstance().getIntParameter("High Port",6889);
    lowPort = Math.min(lowPort, highPort);
    highPort = Math.max(lowPort, highPort);
    port = lowPort;
    sck = null;
    bContinue = true;
    while (sck == null && port <= highPort) {
      try {
        sck = ServerSocketChannel.open();
        sck.socket().bind(new InetSocketAddress(port));
      } catch (Exception e) {
        Logger.getLogger().log(componentID,evtErrors,Logger.ERROR,"BT Server was unable to bind port " + port + ", reason : " + e);
        port++;
        sck = null;
      }
    }
    
    if (sck != null) {
      Logger.getLogger().log(componentID,evtLyfeCycle,Logger.INFORMATION,"BT Server is bound on port " + port);
    } else {
      Logger.getLogger().log(componentID,evtLyfeCycle,Logger.INFORMATION,"BT was unable to bind on a port from "+ lowPort + " to " + highPort);            
      port = 0;
    }
  }
  
  public void run()
  {    
    try {
      sck.configureBlocking(true);
      Logger.getLogger().log(componentID,evtLyfeCycle,Logger.INFORMATION,"BT Server is ready to accept incoming connections");
      while (bContinue) {
        SocketChannel sckClient = sck.accept();
        if (sckClient != null) {
          Logger.getLogger().log(componentID,evtNewConnection,Logger.INFORMATION,"BT Server has accepted an incoming connection from : " + sckClient.socket().getInetAddress().getHostAddress());
          sckClient.configureBlocking(false);
          manager.addPeer(sckClient);
        }
      }
    } catch (Exception e)  {               
      if (bContinue)            
        Logger.getLogger().log(componentID,evtErrors,Logger.ERROR,"BT Server has catched an error : " + e);            
    }
    
    Logger.getLogger().log(componentID,evtLyfeCycle,Logger.INFORMATION,"BT Server is stopped");
  }
  
  public void stopServer()
  {    
    bContinue = false;
    
    //this will most probably raise an exception ;)
    try{
      Logger.getLogger().log(componentID,evtLyfeCycle,Logger.INFORMATION,"BT Server is stopping");
      sck.close();
    } catch(Exception e)
    {
      Logger.getLogger().log(componentID,evtErrors,Logger.ERROR,"Error catched while stopping server : " + e);  
    }
  }
  
  public int getPort()
  {
    return port;
  }
  
  public void setManager(PeerManager manager)
  {
    this.manager = manager;
  }
  
}