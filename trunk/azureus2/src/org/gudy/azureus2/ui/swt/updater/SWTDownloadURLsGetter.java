/*
 * File    : Utils.java
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.gudy.azureus2.core3.internat.MessageText;

/**
 * @author Olivier Chalouhi
 *
 */
public class SWTDownloadURLsGetter implements DownloadListener{
  
  public static String[] swtURLProviders = {
      "http://azureus.sourceforge.net/swt_version.php" ,
      "http://www.gudy.org/azureus/swt_version.php" ,
      "http://www.keecall.com/azureus/swt_version.php"
  };
  
  SWTDownloadURLsListener listener;
  String platform;
  int index;
  
  public SWTDownloadURLsGetter(SWTDownloadURLsListener listener) {
    this.listener = listener;    
    this.platform = SWT.getPlatform();
    this.index = 0;
    listener.processName(MessageText.getString("swt.updater.urlsgetter.platform") + " " + platform);
    download();
  }  
  

  public void reportData(InputStream is) {
   if(is != null) {
     processData(is);
   } else {    
     index++;
     if(index < swtURLProviders.length) {
       download();
     }
     else {
       listener.reportURLs(null);
     }
   }
  }
  
  public void reportPercent(int percent) {
    listener.percentDone(percent);
  }
  
  private void download() {
    String url = swtURLProviders[index];
    String downloadURL = url + "?platform=" + platform;
    listener.processName(MessageText.getString("swt.updater.urlsgetter.downloading") + " " + downloadURL);
    new URLDownloader(this,downloadURL);
  }
  
  private void processData(InputStream is) {
    BufferedReader br = new BufferedReader(new InputStreamReader(is));
    String line;
    ArrayList response = new ArrayList();
    try {
      while((line = br.readLine()) != null) {
        response.add(line);
      }
    } catch(IOException e) {
      e.printStackTrace();
    }
    listener.reportURLs((String[])response.toArray(new String[response.size()]));
  }
}
