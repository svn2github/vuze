/*
 * File    : Main.java
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

/**
 * @author Olivier Chalouhi
 *
 */
public class Main implements ProgressListener,SWTDownloadURLsListener,SWTZipDownloadListener {

  public Main() {
    new SWTDownloadURLsGetter(this);
  }
  
  public void percentDone(int percent) {
    System.out.print("\r" + percent + " %   ");
  }
  

  public void processName(String name) {
    System.out.println("Status : " + name);
  }
  
  public void reportURLs(String[] urls) {
    if(urls != null) {
      System.out.println("Found " + urls.length + " download URLs");
      for(int i = 0 ; i < urls.length ; i++) {      
        System.out.println(urls[i]);
      }
      new SWTZipDownloader(this,urls);
    } else {
      System.out.println("No URL found");
    }
  }
  
  public static void main(String args[]) {
    new Main();
  }

  public void reportData(InputStream inputStream) {
    if(inputStream != null) {
      System.out.println("Data Received");
    } else {
      System.out.println("No Data Received");
    }
  }
}
