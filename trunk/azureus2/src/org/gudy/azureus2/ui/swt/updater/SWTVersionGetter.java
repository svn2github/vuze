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
package org.gudy.azureus2.ui.swt.updater;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.swt.SWT;
import org.gudy.azureus2.core3.logging.LGLogger;

/**
 * @author Olivier Chalouhi
 *
 */
public class SWTVersionGetter implements DownloadListener {
  
  private String platform;
  private int index;
  
  private int currentVersion;
  private int latestVersion;
  
  private Object lock;
  
  public SWTVersionGetter() {
    this.platform = SWT.getPlatform();
    this.index = 0;
    this.lock = new Object();
    this.currentVersion = SWT.getVersion();
    this.latestVersion = 0;
  }
  
  public boolean needsUpdate() {
    try {
      downloadLatestVersion();
      lock.wait();
      return latestVersion > currentVersion;
    } catch(Exception e) {
      return false;
    }        
  }
  
  private void downloadLatestVersion() {
    String url = SWTUpdateChecker. swtURLProviders[index];
    String downloadURL = url + "?platform=" + platform + "&cmd=version";
    LGLogger.log("Requesting latest SWT version by opening URL : " + downloadURL);       
    new URLDownloader(this,downloadURL);
  }
  
  private void nextTry() {
    index++;
    if(index >= SWTUpdateChecker.swtURLProviders.length) {
      lock.notify();
      return;
    }
    downloadLatestVersion();
  }
    
  public void reportData(InputStream is) {
    if(is == null) {
      nextTry();
      return;
    }
    
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      String versionStr = br.readLine();
      int version = Integer.parseInt(versionStr);
      latestVersion = version;
      lock.notify();
    } catch(Exception e) {
      nextTry();
    }
  }
  
  public void reportPercent(int percent) {
   //Do nothing
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
}
