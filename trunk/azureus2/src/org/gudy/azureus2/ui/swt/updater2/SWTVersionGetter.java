/*
 * Created on 20 mai 2004
 * Created by Olivier Chalouhi
 * 
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
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
 * 
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.updater2;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderFactory;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.ResourceDownloaderFactoryImpl;

/**
 * @author Olivier Chalouhi
 *
 */
public class SWTVersionGetter {
  
	 public static String[] swtURLProviders = {
	 	      "http://azureus.sourceforge.net/swt_version.php",
	 	      "http://azureus.aelitis.com/swt_version.php",
	 	      "http://www.keecall.com/azureus/swt_version.php",
	 	      "http://www.gudy.org/azureus/swt_version.php"
	 	  };
	 	  
	 public static final int	VERSION_TIMEOUT_MILLIS	= 30000;
	 
  private String platform;
  private int index;
  
  private int currentVersion;
  private int latestVersion;
  
  private String[] mirrors;
  
  public SWTVersionGetter() {
    this.platform = SWT.getPlatform();
    this.index = 0;
    this.currentVersion = SWT.getVersion();
    this.latestVersion = 0;
  }
  
  public boolean needsUpdate() {
    try {
      downloadLatestVersion();

      LGLogger.log("SWT: current version =  " + currentVersion + ", latest version = " + latestVersion );

      return latestVersion > currentVersion;
    } catch(Exception e) {
      return false;
    }        
  }
  
  private void downloadLatestVersion() {
    String url = swtURLProviders[index];
    String downloadURL = url + "?platform=" + platform + "&version=" + Constants.AZUREUS_VERSION;
    LGLogger.log("Requesting latest SWT version/mirrors by opening URL : " + downloadURL);
    try {
      ResourceDownloaderFactory rdf = ResourceDownloaderFactoryImpl.getSingleton();
      
      ResourceDownloader downloader = rdf.create(new URL(downloadURL));
      
      downloader = rdf.getTimeoutDownloader(downloader, VERSION_TIMEOUT_MILLIS );
      
      processData(downloader.download());
    } catch(Exception e) {
      nextTry();
    }
  }
  
  private void nextTry() {
    index++;
    if(index >= swtURLProviders.length) {
      return;
    }
    downloadLatestVersion();
  }
    
  public void processData(InputStream is) throws Exception{
    if(is == null) {
      nextTry();
      return;
    }
    
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      String versionStr = br.readLine();
      int version = Integer.parseInt(versionStr);
      latestVersion = version;
      String mirror = null;
      List tempList = new ArrayList();
      while((mirror = br.readLine()) != null) {
        tempList.add(mirror);
      }
      this.mirrors = (String[]) tempList.toArray(new String[tempList.size()]);
    } catch(Exception e) {
      nextTry();
    }
  }
  
  
  /**
   * @return Returns the latestVersion.
   */
  public int getLatestVersion() {
    return latestVersion;
  }
  /**
   * @return Returns the platform.
   */
  public String getPlatform() {
    return platform;
  }
  
  /**
   * @return Returns the mirrors.
   */
  public String[] getMirrors() {
    return mirrors;
  }
}
