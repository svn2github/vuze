/*
 * Written and copyright 2001-2003 Tobias Minich. Distributed under the GNU
 * General Public License; see the README file. This code comes with NO
 * WARRANTY.
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

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderCallBackInterface;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloader;

/**
 * @author Tobias Minich
 */
public class TorrentDownloaderImpl extends Thread implements TorrentDownloader {

  private String _url;
  private String _file;
  
  private URL url;
  private HttpURLConnection con;
  private String error = "Ok";
  private TorrentDownloaderCallBackInterface iface;
  private int state = STATE_NON_INIT;
  private int percentDone = 0;
  private int readTotal = 0;
  private boolean cancel = false;
  private String filename, directoryname;
  private File file = null;

  public TorrentDownloaderImpl() {
    super("Torrent Downloader");
    setDaemon(true);
  }

  public void init(TorrentDownloaderCallBackInterface _iface, String _url) {
    this.setName("TorrentDownloader: " + _url);
    this.iface = _iface;
    this._url = _url;
  }

  public void init(TorrentDownloaderCallBackInterface _iface, String _url, String _file) {
    init(_iface, _url);
    this._file = _file;
  }

  public void notifyListener() {
    if (this.iface != null)
      this.iface.TorrentDownloaderEvent(this.state, this);
    else if (this.state == STATE_ERROR)
      System.err.println(this.error);
  }

  private void cleanUpFile() {
    if ((this.file != null) && this.file.exists())
      this.file.delete();
  }

  private synchronized void error(String err) {
    this.state = STATE_ERROR;
    this.setError(err);
    this.cleanUpFile();
    this.notifyListener();
  }

  public void run() {
    try {
      this.url = new URL(_url);
      this.con = (HttpURLConnection) this.url.openConnection();
      this.con.connect();

      int response = this.con.getResponseCode();
      if ((response != HttpURLConnection.HTTP_ACCEPTED) && (response != HttpURLConnection.HTTP_OK)) {
        this.error("Error on connect for '" + this.url.toString() + "': " + Integer.toString(response) + " " + this.con.getResponseMessage());
        return;
      }

      this.filename = this.con.getHeaderField("Content-Disposition");
      if ((this.filename!=null) && this.filename.toLowerCase().matches(".*attachment.*")) // Some code to handle b0rked servers.
        while (this.filename.toLowerCase().charAt(0)!='a')
          this.filename = this.filename.substring(1);
      if ((this.filename == null) || !this.filename.toLowerCase().startsWith("attachment") || (this.filename.indexOf('=') == -1)) {
        String tmp = this.url.getFile();
        if (tmp.lastIndexOf('/') != -1)
          tmp = tmp.substring(tmp.lastIndexOf('/') + 1);
        
        // remove any params in the url
        
        int	param_pos = tmp.indexOf('?');
        
        if ( param_pos != -1 ){
          tmp = tmp.substring(0,param_pos);
        }
        this.filename = URLDecoder.decode(tmp, "UTF-8");
      } else {
        this.filename = this.filename.substring(this.filename.indexOf('=') + 1);
        if (this.filename.startsWith("\"") && this.filename.endsWith("\""))
          this.filename = this.filename.substring(1, this.filename.lastIndexOf('\"'));
        File temp = new File(this.filename);
        this.filename = temp.getName();
      }

      this.directoryname = COConfigurationManager.getDirectoryParameter("General_sDefaultTorrent_Directory");
      boolean useTorrentSave = COConfigurationManager.getBooleanParameter("Save Torrent Files", true);

      if (_file != null) {
        File temp = new File(_file);

        //if we're not using a default torrent save dir
        if (!useTorrentSave || directoryname.length() == 0) {
          //if it's already a dir
          if (temp.isDirectory()) {
            //use it
            directoryname = temp.getCanonicalPath();
          }
          //it's a file
          else {
            //so use its parent dir
            directoryname = temp.getCanonicalFile().getParent();
          }
        }

        //if it's a file
        if (!temp.isDirectory()) {
          //set the file name
          filename = temp.getName();
        }
      }

      this.state = STATE_INIT;
      this.notifyListener();
    } catch (java.net.MalformedURLException e) {
      this.error("Exception while parsing URL '" + url + "':" + e.getMessage());
    } catch (java.net.UnknownHostException e) {
      this.error("Exception while initializing download of '" + url + "': Unknown Host '" + e.getMessage() + "'");
    } catch (java.io.IOException ioe) {
      this.error("I/O Exception while initializing download of '" + url + "':" + ioe.toString());
    }    
    if (this.state != STATE_ERROR) {
      this.state = STATE_START;
      notifyListener();
      this.state = STATE_DOWNLOADING;
      try {
        this.file = new File(this.directoryname, this.filename);
        this.file.createNewFile();
        FileOutputStream fileout = new FileOutputStream(this.file, false);
        InputStream in = this.con.getInputStream();

        byte[] buf = new byte[1020];
        int read = 0;
        int size = this.con.getContentLength();
		this.percentDone = -1;
        do {
          if (this.cancel)
            break;
          try {
            read = in.read(buf);
            this.readTotal += read;
            if (size != 0)
              this.percentDone = (100 * this.readTotal) / size;
            notifyListener();
          } catch (IOException e) {
          }
          if (read > 0)
            fileout.write(buf, 0, read);
        } while (read > 0);
        in.close();
        fileout.flush();
        fileout.close();
        if (this.cancel) {
          this.state = STATE_CANCELLED;
          this.cleanUpFile();
        } else {
          if (this.readTotal == 0) {
            this.error("No data contained in '" + this.url.toString() + "'");
            return;
          }
          this.state = STATE_FINISHED;
        }
        this.notifyListener();
      } catch (Exception e) {
      	e.printStackTrace();
        this.error("Exception while downloading '" + this.url.toString() + "':" + e.getMessage());
      }
    }
  }

  public boolean equals(Object obj) {
    if (this == obj)
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
    if ((!this.isAlive()) || (this.file == null))
      this.file = new File(this.directoryname, this.filename);
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

  public void cancel() {
    this.cancel = true;
  }

  public void setDownloadPath(String path, String file) {
    if (!this.isAlive()) {
      if (path != null)
        this.directoryname = path;
      if (file != null)
        this.filename = file;
    }
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.core3.torrentdownloader.TorrentDownloader#getTotalRead()
   */
  public int getTotalRead() {
    return this.readTotal;
  }

}
