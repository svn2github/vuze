/*
 * File    : UpdateLanguagePlugin.java
 * Created : 24-Mar-2004
 * By      : TuxPaper
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

package org.gudy.azureus2.core3.internat.update;

import java.io.*;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.ConfigSectionSWT;

import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloader;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderCallBackInterface;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderFactory;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;

/* Simple plugin to retrieve a new MessagesBundle file from the website.
 *
 * The process:
 * 1) Open a URL on the website passing the programmic name for the entire 
 *    locale (see Locale.toString()).  The web page send back two lines of 
 *    text:
 *      Line 1: Latest revision
 *      Line 2: URL to the properties file
 *    -or- 
 *      a 0 length file, a 404 error (not tested) of the locale does not exist.
 *
 * 2) Compare current revision with the latest revision. Quit early if it's
 *    not newer.
 *
 * 3) Open the URL sent on Line 2 and save it to the User directory. Two lines 
 *    are added to the new bundle (to allow for comparing in step 2):
 *      Line 1: The revision of the bundle
 *      Line 2: Comment for users about the above line.
 *
 * 4) Refresh Azureus
 *
 * If there was already a bundle in the user's directory that did not have a
 * revision number on the first line, it is renamed, and a "User.x" is placed
 * in its variant name.
 */
public class
UpdateLanguagePlugin
  implements Plugin, TorrentDownloaderCallBackInterface
{
  private static final int STATE_UPDATEURL   = 0;
  private static final int STATE_LANGUAGEURL = 1;
  protected PluginInterface   plugin_interface;
  protected PluginConfig      plugin_config;
  protected LoggerChannel     log;

  private TorrentDownloader downloader;
  private String sCurLocale;
  private String sUpdateURL;
  private File  fLanguageFile;
  private float fCurrentRevision;
  private float fNewestRevision = 1;
  private int state = STATE_UPDATEURL;

  public void initialize(PluginInterface _plugin_interface) {
    plugin_interface  = _plugin_interface;

    log = plugin_interface.getLogger().getChannel("UpdateLanguage");
    log.log(LoggerChannel.LT_INFORMATION, "UpdateLanguage Plugin Initialisation");

    plugin_config = plugin_interface.getPluginconfig();

    try {
      plugin_interface.addConfigSection(new ConfigSectionUpdateLang());
    } catch (NoClassDefFoundError e) {
      /* Ignore. SWT probably not installed */
      log.log(LoggerChannel.LT_WARNING,
              "SWT UI Config not loaded for UpdateLanguagePlugin. " +
              e.getMessage() + " not found.");
    } catch( Throwable e ){
      e.printStackTrace();
    }
    
    if (plugin_config.getBooleanParameter("General_bEnableLanguageUpdate")) {
      try {
        updateLanguage();
      } catch( Exception e ) {
        e.printStackTrace();
      }
    }
  }
  
  /* Main procedure to update the current language file. */
  private void updateLanguage() {
    String sCurrentRevision = "0";

    sUpdateURL = plugin_config.getStringParameter("General_sUpdateLanguageURL"); 
    
    // get Current Revision from first line of .properties file
    sCurLocale = Locale.getDefault().toString();
    if (sCurLocale == "en")
      sCurLocale = "";
    
    sUpdateURL = sUpdateURL.replaceAll("%s", sCurLocale);

    if (!sCurLocale.equals(""))
      sCurLocale = "_" + sCurLocale;

    BufferedReader in = null;
    try {
      fLanguageFile = FileUtil.getUserFile("MessagesBundle" + sCurLocale + ".properties");
      in = new BufferedReader(new FileReader(fLanguageFile));
      sCurrentRevision = in.readLine();
      if (sCurrentRevision.startsWith("#")) {
        sCurrentRevision = sCurrentRevision.substring(1);
      }
    } catch (FileNotFoundException e) {
      //Do nothing

    } catch (Exception e) {
      e.printStackTrace();

    } finally {
      try {
        if (in != null)
          in.close();
      } catch (Exception e) {}
    }
    
    try {
      fCurrentRevision = Float.valueOf(sCurrentRevision).floatValue();
    } catch (Exception e) {
      fCurrentRevision = 0;
    }

    downloader = TorrentDownloaderFactory.download(this, sUpdateURL);
    downloader.start();
    log.log(LoggerChannel.LT_INFORMATION, 
            "Current local revision is " + fCurrentRevision + 
            ". Starting download using " + sUpdateURL);
  }

  /* Handle responses from the TorrentDownloader */
  public void TorrentDownloaderEvent(int stateDownloader, TorrentDownloader inf) {   
    if (state == STATE_UPDATEURL) {
      if (stateDownloader == TorrentDownloader.STATE_FINISHED) {
        checkRevision();
      }
    } else if (state == STATE_LANGUAGEURL) {
      if (stateDownloader == TorrentDownloader.STATE_FINISHED) {
        moveInNewRevision();
      }
    }
    if (stateDownloader == TorrentDownloader.STATE_ERROR) {
      log.log(LoggerChannel.LT_ERROR, "Error:" + downloader.getError());
    } else if (stateDownloader == TorrentDownloader.STATE_CANCELLED) {
      log.log(LoggerChannel.LT_INFORMATION, "Update cancelled by user");
    }
  }

  /* Retrieve the revision number, and if newer, update bundle */
  private void checkRevision() {
    log.log(LoggerChannel.LT_INFORMATION, "Revision info download complete");
    String sLanguageURL = null;

    // retrieve the revision number and Language URL from file
    BufferedReader in = null;
    try {
      File fUpdateInfo = new File(downloader.getFile().getAbsolutePath());
      in = new BufferedReader(new FileReader(fUpdateInfo));
      String sLine = in.readLine();

      try {
        fNewestRevision = Float.valueOf(sLine).floatValue();
      } catch (Exception e) { }
      
      sLanguageURL = in.readLine();
    } catch (FileNotFoundException e) {
      //Do nothing

    } catch (Exception e) {
      e.printStackTrace();

    } finally {
      try {
        if (in != null)
          in.close();
      } catch (Exception e) {}
    }
    
    if (sLanguageURL == null) {
      log.log(LoggerChannel.LT_WARNING, "Could not retrieve newest language file revision number");
    } else {
      if (fNewestRevision > fCurrentRevision && sLanguageURL != null) {
        log.log(LoggerChannel.LT_INFORMATION, "Latest revision is " + fNewestRevision);
        if (okToGetLatest()) {
          try {
            File fDel = FileUtil.getUserFile("lang.tmp");
            if (fDel.exists())
              fDel.delete();
            
            state = STATE_LANGUAGEURL;
            downloader = TorrentDownloaderFactory.download(this, sLanguageURL, 
                                                           SystemProperties.getUserPath() + "lang.tmp");
            downloader.start();
            log.log(LoggerChannel.LT_INFORMATION, "Downloading Rev. " + fNewestRevision + " from " + sLanguageURL);
          } catch (Exception e) {
            log.log("Deleting Old lang.tmp File", e);
            e.printStackTrace();
          }
        }
      } else {
        log.log(LoggerChannel.LT_INFORMATION, "You have the latest revision (" + fNewestRevision + ")");
      }
    }
  }    
  
  /* Overwrite the old bundle with the new Revision. */
  private void moveInNewRevision() {
    log.log(LoggerChannel.LT_INFORMATION, "Language File download complete");
    // overwrite user language file
    File fInBundle = new File(downloader.getFile().getAbsolutePath());

    InputStream is = null;
    OutputStream os = null;
		try{
      File fOutBundle = fLanguageFile;
			is = new FileInputStream(fInBundle);
			os = new FileOutputStream(fOutBundle);
			
			byte[] buf = new byte[32*1024];
			int nbRead;
			
      String s = "#" + String.valueOf(fNewestRevision) + "\n";
      
			os.write(s.getBytes());
			s = "# This file will get overridden unless the line above is removed\n";
			os.write(s.getBytes());
				
			while ((nbRead = is.read(buf)) > 0){
				os.write(buf, 0, nbRead);
			}

    } catch (FileNotFoundException e) {
      //Do nothing

    } catch (Exception e) {
      log.log("Writing new language File", e);
      e.printStackTrace();

		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Exception e) { }
			}
			if (os != null) {
				try {
					os.close();
				} catch (Exception e) { }
			}
		}
		
		try {
  		fInBundle.delete();
    } catch (Exception e) { }
    
    log.log(LoggerChannel.LT_INFORMATION, "Written to " + fLanguageFile.getAbsolutePath());
    // refresh Azureus
    MainWindow.getWindow().getMenu().refreshLanguage();
  }
  
  /* Checks to see if it's okay to retreive the latest bundle.
   * Currently consists of renaming old file if exists.
   *
   * @return whether it's okay to retrieve the latest bundle
   */
  private boolean okToGetLatest() {
    if (!fLanguageFile.exists()) {
      return true;
    }

    boolean okToProceed = true;
    String sNewLocale;
    if (sCurLocale.length() > 6) {
      sNewLocale = sCurLocale + ".%x";
    } else if (sCurLocale.length() == 3) {
      sNewLocale = sCurLocale + "__User.%x";
    } else if (sCurLocale.length() == 0) {
      sNewLocale = sCurLocale + "___User.%x";
    } else {
      sNewLocale = sCurLocale + "_User.%x";
    }
    File fRenameTo = null;
    for (int i = 1; i < 1000; i++) {
      fRenameTo = FileUtil.getUserFile("MessagesBundle" + sNewLocale.replaceAll("%x", String.valueOf(i)) + ".properties");
      if (!fRenameTo.exists())
        break;
    }
    if (fRenameTo != null && !fRenameTo.exists()) {
      okToProceed = fLanguageFile.renameTo(fRenameTo);
    } else {
      okToProceed = false;
    }
    log.log(okToProceed ? LoggerChannel.LT_INFORMATION : LoggerChannel.LT_ERROR, 
            "Renaming old MessagesBundle" + sCurLocale + ".properties " +
            (okToProceed ? "succeeded" : "failed"));

    return okToProceed;  
  }

  private File getCurLocaleFile() {
    String sCurLocale = Locale.getDefault().toString();
    if (sCurLocale == "en")
      sCurLocale = "";
    if (!sCurLocale.equals(""))
      sCurLocale = "_" + sCurLocale;
    return FileUtil.getUserFile("MessagesBundle" + sCurLocale + ".properties");
  }

  /* Configuration.
   */
  class ConfigSectionUpdateLang implements ConfigSectionSWT {
    public String configSectionGetParentSection() {
      return ConfigSection.SECTION_INTERFACE;
    }
  
    /* Name of section will be pulled from 
     * ConfigView.section.<i>configSectionGetName()</i>
     */
  	public String configSectionGetName() {
  		return "language";
  	}
  
    public void configSectionSave() {
    }
  
    public void configSectionDelete() {
    }
    
  
    public Composite configSectionCreate(final Composite parent) {
      GridData gridData;
      GridLayout layout;
  
      Composite cSection = new Composite(parent, SWT.NULL);
      cSection.addControlListener(new Utils.LabelWrapControlListener());
      gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
      cSection.setLayoutData(gridData);
      layout = new GridLayout();
      layout.numColumns = 2;
      cSection.setLayout(layout);

      Label label = new Label(cSection, SWT.WRAP);
      gridData = new GridData(GridData.FILL_HORIZONTAL);
      gridData.horizontalSpan = 2;
      label.setLayoutData(gridData);
      Messages.setLanguageText(label, "ConfigView.section.language.info");
      
      gridData = new GridData();
      gridData.horizontalSpan = 2;
      new BooleanParameter(cSection, "General_bEnableLanguageUpdate", 
                           "ConfigView.section.language.enableUpdate").setLayoutData(gridData);

      label = new Label(cSection, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.section.language.UpdateURL");
      gridData = new GridData(GridData.FILL_HORIZONTAL);
      new StringParameter(cSection, "General_sUpdateLanguageURL").setLayoutData(gridData);

      Button btnUpdateNow = new Button(cSection, SWT.PUSH);
      Messages.setLanguageText(btnUpdateNow, "ConfigView.section.language.UpdateNow");
	    btnUpdateNow.addListener(SWT.Selection, new Listener() {
	      public void handleEvent(Event event) {
	        ((Button)event.widget).setEnabled(false);
	        UpdateLanguagePlugin.this.updateLanguage();
	      }
	    });
  
      Button btnRevert = new Button(cSection, SWT.PUSH);
      Messages.setLanguageText(btnRevert, "Button.revert");
	    btnRevert.addListener(SWT.Selection, new Listener() {
	      public void handleEvent(Event event) {
	        File file = UpdateLanguagePlugin.this.getCurLocaleFile();
    	    if (file.exists()) {
            try {
              file.delete();
            } catch (Exception e) {}
          }
          ((Button)event.widget).setEnabled(file.exists());
          MainWindow.getWindow().getMenu().refreshLanguage();
	      }
	    });
      btnRevert.setEnabled(UpdateLanguagePlugin.this.getCurLocaleFile().exists());

      return cSection;
    }
  }
}
