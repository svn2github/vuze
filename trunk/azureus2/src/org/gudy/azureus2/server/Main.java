/*
 * Main.java
 *
 * Created on 22. August 2003, 00:04
 */

package org.gudy.azureus2.server;

import org.gudy.azureus2.core.GlobalManager;
import org.gudy.azureus2.core.ConfigurationManager;
/**
 *
 * @author  Tobias Minich
 */
public class Main {
  
  Jhttpp2Server server;
  GlobalManager gm;
  
  /** Creates a new instance of Main */
  public Main(String args[]) {
    gm = new GlobalManager();
    server = new Jhttpp2Server(gm, true);
    new Thread(server).start();
    System.out.println("Running on port " + ConfigurationManager.getInstance().getIntParameter("Server_iPort"));
  }
  
  public static void main(String args[]) {
    new Main(args);
  }
  
}
