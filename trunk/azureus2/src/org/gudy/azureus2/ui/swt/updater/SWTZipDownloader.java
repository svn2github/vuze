/*
 * File    : SWTZipDownloader.java
 * Created : 3 avr. 2004
 * By      : Olivier
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
package org.gudy.azureus2.ui.swt.updater;

import java.io.InputStream;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LGLogger;

/**
 * @author Olivier Chalouhi
 *
 */
public class SWTZipDownloader implements DownloadListener{
  
  String[] urls;
  SWTZipDownloadListener listener;
  int index;
  
  boolean canceled;
  URLDownloader downloader;
  
  public SWTZipDownloader(SWTZipDownloadListener listener,String[] urls) {
    this.urls = urls;
    this.listener = listener;
    this.index = 0;
    this.canceled = false;
    download();
  }
  

  public void reportData(InputStream is) {
    if(is != null) {
      this.listener.reportData(is);
      LGLogger.log("SWT Updater has downloaded the SWT package");
      UpdateLogger.log("SWT Updater has downloaded the SWT package");
    } else {
      LGLogger.log("SWT Updater : failed to download");
      UpdateLogger.log("SWT Updater : failed to download");
      index++;
      if(index < urls.length && !canceled) {
        download();
      } else {
        this.listener.reportData(null);
      }
    }
  }
  
  public void reportPercent(int percent) {
    this.listener.percentDone(percent);
  }
  
  private void download() {
    String url = urls[index];
    listener.processName(MessageText.getString("swt.updater.downloader.downloading") + "\n" + url);
    LGLogger.log("SWT Updater is downloading from " + url);
    UpdateLogger.log("SWT Updater is downloading from " + url);
    downloader = new URLDownloader(this,url);
  }
  
  public void cancel() {
    canceled = true;
    if(downloader != null) downloader.cancel();
  }
}
