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

import java.io.InputStream;

import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderException;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.ResourceDownloaderBaseImpl;

/**
 * @author Olivier Chalouhi
 *
 */
public class SWTDownloader extends ResourceDownloaderBaseImpl implements ResourceDownloader, SWTDownloadURLsListener , DownloadListener{
  
  private String platform;
  private String[] mirrors;
  private int size;
  
  boolean canceled;
  URLDownloader downloader;
  
  public SWTDownloader(String platform) {
    this.platform = platform;
    new SWTDownloadURLsGetter(this);
    try {
      this.wait();
    } catch(Exception e) {
      mirrors = null;
      size = -1;
    }
  }
  
  public void asyncDownload() {
    // TODO Auto-generated method stub
  }
  
  
  public void cancel() {
    // TODO Auto-generated method stub
  }
  
  public InputStream download() throws ResourceDownloaderException {
    // TODO Auto-generated method stub
    return null;
  }
  
  public String getName() {
    return "SWT Updater";
  }
  
  public long getSize() throws ResourceDownloaderException {
   return size;
  }
  
  
  public void reportActivity(String activity) {   
  }
  
  
  public void percentDone(int percent) {
    
  }
 
  
  public void processName(String name) {
   
  }

  
  public void reportURLs(String[] urls, int sz) {
    this.mirrors = urls;
    this.size = sz;
    this.notify();
  }
  
  
  //DownloadListener implementation
  public void reportData(InputStream is) {
    // TODO Auto-generated method stub
  }
  
  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.updater.DownloadListener#reportPercent(int)
   */
  public void reportPercent(int percent) {
    // TODO Auto-generated method stub
  }
  
  /* (non-Javadoc)
   * @see org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.ResourceDownloaderBaseImpl#getClone()
   */
  public ResourceDownloader getClone() {
    // TODO Auto-generated method stub
    return null;
  }
  
  //Listener notification
}
