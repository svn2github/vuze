/* Written and copyright 2001-2003 Tobias Minich.
 * Distributed under the GNU General Public License; see the README file.
 * This code comes with NO WARRANTY.
 *
 *
 * Main.java
 *
 * Created on 22. August 2003, 00:04
 */

package org.gudy.azureus2.ui.console;

import org.apache.log4j.Logger;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.ILocaleUtilChooser;
import org.gudy.azureus2.core3.internat.LocaleUtil;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderFactory;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.ui.common.IUserInterface;
import org.gudy.azureus2.ui.common.LocaleUtilHeadless;

/**
 *
 * @author  Tobias Minich
 */
public class UI extends org.gudy.azureus2.ui.common.UITemplate implements ILocaleUtilChooser,IUserInterface {
  
  private ConsoleInput console = null;
  
  /** Creates a new instance of Main */
  /*public UI() {
  }*/
  
  public void init(boolean first, boolean others) {
    super.init(first,others);
    System.setProperty("java.awt.headless", "true");
  }
  
  public String[] processArgs(String[] args) {
    return args;
  }
  
  public void startUI() {
    super.startUI();
    TorrentDownloaderFactory.initManager(org.gudy.azureus2.ui.common.Main.GM, true, true);
    if ((!isStarted()) || (console == null) || ((console!=null) && (!console.isAlive()))) {
      ConsoleInput.printconsolehelp(System.out);
      System.out.println();
      console = new ConsoleInput("Main", org.gudy.azureus2.ui.common.Main.GM, System.in, System.out, true);
    }
  }
  
  public void openTorrent(String fileName) {
    if (!FileUtil.getCanonicalFileName(fileName).endsWith(".torrent")) {//$NON-NLS-1$
      Logger.getLogger("azureus2.ui.console").error(fileName+" doesn't seem to be a torrent file. Not added.");
      return;
    }
    if (org.gudy.azureus2.ui.common.Main.GM!=null) {
      try {
        org.gudy.azureus2.ui.common.Main.GM.addDownloadManager(fileName, COConfigurationManager.getDirectoryParameter("General_sDefaultSave_Directory"));
      } catch (Exception e) {
        Logger.getLogger("azureus2.ui.console").error("The torrent "+fileName+" could not be added.", e);
      }
    }
  }
  
}
