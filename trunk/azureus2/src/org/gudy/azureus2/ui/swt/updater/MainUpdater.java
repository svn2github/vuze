/*
 * File    : DownloadAll.java
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.swt.SWT;
import org.gudy.azureus2.core3.logging.LGLogger;

/**
 * @author Olivier Chalouhi
 *
 */
public class MainUpdater implements SWTDownloadURLsListener,SWTZipDownloadListener {

  String platform;
  GeneralListener listener;
  
  SWTDownloadURLsGetter urlGetter;
  SWTZipDownloader zipDownloader;
  
  public MainUpdater(GeneralListener listener){
    this.listener = listener;
    this.platform = SWT.getPlatform();    
    urlGetter = new SWTDownloadURLsGetter(this);
  }
  
  public void percentDone(int percent) {
    listener.percentDone(percent);
  }
  

  public void processName(String name) {
    listener.processName(name);
  }
  
  public void reportURLs(String[] urls, int x) {
    if(urls != null) {
      LGLogger.log("SWT Updater found " + urls.length + " urls");
      UpdateLogger.log("SWT Updater found " + urls.length + " urls");
      zipDownloader = new SWTZipDownloader(this,urls);
    } else {
      LGLogger.log("SWT Updater fails : no urls found");
      UpdateLogger.log("SWT Updater fails : no urls found");
      listener.processFailed();
    }
  }

  public void reportData(InputStream inputStream) {
    if(inputStream != null) {      
      try {
        File f = new File("swtTemp.zip");
        if(f.exists()) f.delete();
        FileOutputStream fos = new FileOutputStream(f);
        byte[] buffer = new byte[4096];
        int read = 0;
        while((read = inputStream.read(buffer)) > 0) {
          fos.write(buffer,0,read);
        }
        fos.close();
        listener.processSucceeded();
      } catch(IOException e) {
        listener.processFailed();
      }
    } else {
      listener.processFailed();
    }
  }
  
  public void launchSWTUpdate() {
    RestartUtil.restartAzureus("org.gudy.azureus2.ui.swt.updater.UpdateSWT",new String[]{platform});
  }
  
  public void cancel() {
    if(urlGetter != null) urlGetter.cancel();
    if(zipDownloader != null) zipDownloader.cancel();
  }
}
