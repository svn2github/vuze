/*
 * File    : URLDownloader.java
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

import java.net.MalformedURLException;
import java.net.URL;
import java.io.InputStream;

import org.gudy.azureus2.plugins.utils.resourcedownloader.*;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.ResourceDownloaderFactoryImpl;

/**
 * @author Olivier Chalouhi
 *
 */
public class URLDownloader implements ResourceDownloaderListener{
  
  DownloadListener listener;
  String url_string;
  ResourceDownloader downloader;
  
  /**
   * Downloads the content at the given URL, reports percent done to the
   * listener and reports the data received as an input stream.
   * In case of an error, the reported inputStream is null.
   * Non-blocking
   * @param listener
   * @param URL
   */
  public URLDownloader(DownloadListener listener,String url) {
    this.listener = listener;
    this.url_string = url;
    try{
	    downloader = ResourceDownloaderFactoryImpl.getSingleton().create(new URL(url_string));
	    downloader.addListener(this);
	    Thread t = new Thread("URL Downloader") {
	      public void run() {
	        InputStream inputStream = null;
	        try {
	          inputStream = downloader.download();
	          URLDownloader.this.listener.reportData(inputStream);
	        } catch(Exception e) {
	          //e.printStackTrace();
	          URLDownloader.this.listener.reportData(null);
	        }        
	      }
	    };
	    t.setDaemon(true);
	    t.start();
    }catch( MalformedURLException e ){
    	e.printStackTrace();
    	listener.reportData(null);
    }
  }
  
    public void 
	reportPercentComplete(ResourceDownloader dl, int percentage) {
    	this.listener.reportPercent(percentage);
    }
  
    public void 
	reportActivity(ResourceDownloader dl, String act) {
    }
    
	public boolean
	completed(
		ResourceDownloader 	dl, 
		InputStream			data )
	{
		return( true );
	}
	
	public void
	failed(
		ResourceDownloader 			dl, 
		ResourceDownloaderException e )
	{
	}
	
	public void 
	cancel() 
	{
		downloader.cancel();
	}
}
