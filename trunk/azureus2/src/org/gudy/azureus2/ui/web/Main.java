/* Written and copyright 2001-2003 Tobias Minich.
 * Distributed under the GNU General Public License; see the README file.
 * This code comes with NO WARRANTY.
 *
 *
 * Main.java
 *
 * Created on 22. August 2003, 00:04
 */

package org.gudy.azureus2.ui.web;

import java.util.Properties;

import org.gudy.azureus2.core.GlobalManager;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.internat.ILocaleUtilChooser;
import org.gudy.azureus2.core3.internat.LocaleUtil;
import org.gudy.azureus2.ui.console.ConsoleInput;

/**
 *
 * @author  Tobias Minich
 */
public class Main implements ILocaleUtilChooser {
  
  Jhttpp2Server server;
  GlobalManager gm;
  ConsoleInput ci;
  
  /** Creates a new instance of Main */
  public Main(String args[]) {
    LocaleUtil.setLocaleUtilChooser(this);
    Properties p = new Properties(System.getProperties());
    p.put("java.awt.headless", "true");
    System.setProperties(p);
    gm = new GlobalManager();
    server = new Jhttpp2Server(gm, true);
    ci = new ConsoleInput(gm, server, System.in, System.out);
    new Thread(server, "Webinterface Server").start();
    System.out.println("Running on port " + COConfigurationManager.getIntParameter("Server_iPort"));
  }
  
  public static void main(String args[]) {
    new Main(args);
  }
  
  public org.gudy.azureus2.core3.internat.LocaleUtil getProperLocaleUtil(Object lastEncoding) {
    return new LocaleUtilServer(lastEncoding);
  }
  
}
