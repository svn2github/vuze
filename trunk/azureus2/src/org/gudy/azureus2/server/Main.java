/*
 * Main.java
 *
 * Created on 22. August 2003, 00:04
 */

package org.gudy.azureus2.server;

import java.util.Properties;

import org.gudy.azureus2.core.GlobalManager;
import org.gudy.azureus2.core.ConfigurationManager;
import org.gudy.azureus2.core.ILocaleUtilChooser;
import org.gudy.azureus2.core.LocaleUtil;

/**
 *
 * @author  Tobias Minich
 */
public class Main implements ILocaleUtilChooser {
  
  Jhttpp2Server server;
  GlobalManager gm;
  
  /** Creates a new instance of Main */
  public Main(String args[]) {
    LocaleUtil.setLocaleUtilChooser(this);
    Properties p = new Properties(System.getProperties());
    p.put("java.awt.headless", "true");
    System.setProperties(p);
    gm = new GlobalManager();
    server = new Jhttpp2Server(gm, true);
    new Thread(server).start();
    System.out.println("Running on port " + ConfigurationManager.getInstance().getIntParameter("Server_iPort"));
  }
  
  public static void main(String args[]) {
    new Main(args);
  }
  
  public org.gudy.azureus2.core.LocaleUtil getProperLocaleUtil(Object lastEncoding) {
    return new LocaleUtilServer(lastEncoding);
  }
  
}
