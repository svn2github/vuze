/*
 * HostResolver.java
 *
 * Created on 12. September 2003, 22:00
 */

package org.gudy.azureus2.server;

import java.net.InetAddress;
import java.util.List;

/**
 *
 * @author  Tobias Minich
 */
public class HostResolver extends Thread {
  
  private List addto;
  private Jhttpp2Server server;
  private String host;
  
  /** Creates a new instance of HostResolver */
  public HostResolver(Jhttpp2Server _server, List _addto, String _host) {
    addto = _addto;
    server = _server;
    host = _host;
    start();
  }
  
  public void run() {
    try {
      InetAddress ad = InetAddress.getByName(host);
      addto.add(ad);
    } catch (Exception e) {
      server.loggerWeb.error("Host "+host+" not found while updating allowed dynamic hosts.", e);
    }
  }
  
}
