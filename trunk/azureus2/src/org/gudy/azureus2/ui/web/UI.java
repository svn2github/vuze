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

import org.apache.log4j.Logger;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.ILocaleUtilChooser;
import org.gudy.azureus2.core3.internat.LocaleUtil;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.ui.common.IUserInterface;

/**
 *
 * @author  Tobias Minich
 */
public class UI implements ILocaleUtilChooser,IUserInterface {
  
  Jhttpp2Server server = null;
  
  /** Creates a new instance of Main */
  public UI() {
  }
  
  public org.gudy.azureus2.core3.internat.LocaleUtil getProperLocaleUtil(Object lastEncoding) {
    return new LocaleUtilServer(lastEncoding);
  }
  
  public void init(boolean first, boolean others) {
    System.setProperty("java.awt.headless", "true");
    if (first)
      LocaleUtil.setLocaleUtilChooser(this);
  }
  
  public String[] processArgs(String[] args) {
    return args;
  }
  
  public void startUI() {
    server = new Jhttpp2Server(org.gudy.azureus2.ui.common.Main.GM, true);
    new Thread(server, "Webinterface Server").start();
    System.out.println("Running on port " + COConfigurationManager.getIntParameter("Server_iPort"));
  }
  
  public void openTorrent(String fileName) {
    if (!FileUtil.getCanonicalFileName(fileName).endsWith(".torrent")) {//$NON-NLS-1$
      Logger.getLogger("azureus2.webinterface").error(fileName+" doesn't seem to be a torrent file. Not added.");
      return;
    }
    if (org.gudy.azureus2.ui.common.Main.GM!=null) {
      try {
        org.gudy.azureus2.ui.common.Main.GM.addDownloadManager(fileName, COConfigurationManager.getDirectoryParameter("General_sDefaultSave_Directory"));
      } catch (Exception e) {
        Logger.getLogger("azureus2.webinterface").error("The torrent "+fileName+" could not be added.", e);
      }
    }
  }
  
}
