/* Written and copyright 2001-2003 Tobias Minich.
 * Distributed under the GNU General Public License; see the README file.
 * This code comes with NO WARRANTY.
 *
 *
 * HTTPDownloader.java
 *
 * Created on 17. August 2003, 22:22
 */

package org.gudy.azureus2.core3.torrentdownloader.impl;

import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;

import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderCallBackInterface;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloader;

/**
 *
 * @author  Tobias Minich
 */
public class TorrentDownloaderImpl extends Thread implements TorrentDownloader {
  
  private URL url;
  private HttpURLConnection con;
  private File file;
  private FileOutputStream fileout;
  private String error = "Ok";
  private TorrentDownloaderCallBackInterface iface;
  private int state = STATE_NON_INIT;
  private int percentDone = 0;
 
  public TorrentDownloaderImpl() {}
  
  public void init(TorrentDownloaderCallBackInterface _iface, String _url) {
    this.setName("TorrentDownloader: "+_url);
    this.iface = _iface;
  }
  
  public void init(TorrentDownloaderCallBackInterface _iface, String _url, String _file) {
    init(_iface, _url);
    
    try {
        this.url = new URL(_url);
        this.con = (HttpURLConnection) this.url.openConnection();
        this.con.connect();
        this.con.getResponseCode();
        
        File temp = new File(_file);
        if (temp.isDirectory()) {
          String filename = this.con.getHeaderField("Content-Disposition");
          if ((filename == null) || !filename.toLowerCase().startsWith("attachment") || (filename.indexOf('=')==-1))
            filename = URLDecoder.decode(this.url.getFile(), "UTF-8");
          else
            filename = filename.substring(filename.indexOf('=')+1);
          this.file = new File(temp, filename.substring(filename.lastIndexOf('/')+1));
        } else
          this.file = temp;
        this.state = STATE_INIT;
        this.notifyListener();
    } catch (Exception e) {
        this.state = STATE_ERROR;
        this.error = "Exception while preparing download for '"+_url+"':"+e.getMessage();
        this.notifyListener();
    }
  }
  
  public void notifyListener() {
    if (this.iface != null)
        this.iface.TorrentDownloaderEvent(this.state, this);
    else if (this.state == STATE_ERROR)
        System.err.println(this.error);
  }
  
  public void run() {
    if (this.state != STATE_ERROR) {
      this.state = STATE_START;
      notifyListener();
      this.state = STATE_DOWNLOADING;
      try {
        this.file.createNewFile();
        this.fileout = new FileOutputStream(this.file, false);
        InputStream in = this.con.getInputStream();
    
        byte[] buf = new byte[1020];
        int read = 0;
        int readtotal = 0;
        int size = this.con.getContentLength();
        do {
          try {
            read = in.read(buf);
            readtotal += read;
            this.percentDone = (100*readtotal)/size;
            notifyListener();
          } catch (IOException e) {}
          if (read>0)
            this.fileout.write(buf, 0, read);
        } while (read > 0);
        in.close();
        this.fileout.flush();
        this.fileout.close();
        this.state = STATE_FINISHED;
        this.notifyListener();
      } catch (Exception e) {
        this.state = STATE_ERROR;
        this.error = "Exception while downloading '"+this.url.toString()+"':"+e.getMessage();
        this.notifyListener();
      }
    }
  }
  
  public boolean equals(Object obj) {
      if (this==obj)
        return true;
      if ((obj != null) && (obj instanceof TorrentDownloaderImpl)) {
        TorrentDownloaderImpl other = (TorrentDownloaderImpl) obj;
        if (other.getURL().equals(this.url.toString()) && other.getFile().getAbsolutePath().equals(this.file.getAbsolutePath()))
            return true;
      }
      return false;
  }
  
  public String getError() {
      return this.error;
  }
  
  public void setError(String err) {
      this.error = err;
  }
  
  public java.io.File getFile() {
      return this.file;
  }
  
  public int getPercentDone() {
      return this.percentDone;
  }
  
  public int getState() {
      return this.state;
  }
  
  public void setState(int state) {
      this.state = state;
  }
  
  public String getURL() {
      return this.url.toString();
  }
  
}
