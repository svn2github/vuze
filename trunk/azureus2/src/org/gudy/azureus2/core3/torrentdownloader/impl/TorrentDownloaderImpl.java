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

import javax.net.ssl.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderCallBackInterface;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloader;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.torrent.*;

/**
 * @author Tobias Minich
 */
public class TorrentDownloaderImpl extends AEThread implements TorrentDownloader {

  private String 	url_str;
  private String	referrer;
  private String 	file_str;
  
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

  private AEMonitor this_mon 	= new AEMonitor( "TorrentDownloader" );

  public TorrentDownloaderImpl() {
    super("Torrent Downloader");
    setDaemon(true);
  }

  public void 
  init(
  		TorrentDownloaderCallBackInterface	_iface, 
		String 								_url,
		String								_referrer,
		String								_file )
  {
    this.iface = _iface;
    
    //clean up accidental left-facing slashes
    _url = _url.replace( (char)92, (char)47 );
    
    // it's possible that the URL hasn't been encoded (see Bug 878990)
    _url = _url.replaceAll( " ", "%20" );

    setName("TorrentDownloader: " + _url);
    
    url_str 	= _url;
    referrer	= _referrer;
    file_str	= _file;
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

  private void error(String err) {
  	try{
  		this_mon.enter();	// what's the point of this?
  	
  		this.state = STATE_ERROR;
  		this.setError(err);
  		this.cleanUpFile();
  		this.notifyListener();
  	}finally{
  		
  		this_mon.exit();
  	}
  }

  public void runSupport() {
    try {      
      url = new URL(url_str);
      
      if ( url.getProtocol().equalsIgnoreCase("https")){
      	
      	// see ConfigurationChecker for SSL client defaults
      	
      	HttpsURLConnection ssl_con = (HttpsURLConnection)url.openConnection();
      	
      	// allow for certs that contain IP addresses rather than dns names
      	
      	ssl_con.setHostnameVerifier(
      			new HostnameVerifier()
      			{
      				public boolean
      				verify(
      					String		host,
						SSLSession	session )
      				{
      					return( true );
      				}
      			});
      	
      	con = ssl_con;
      	
      }else{
      	
      	con = (HttpURLConnection) url.openConnection();
      	
      }
      
      con.setRequestProperty("User-Agent", Constants.AZUREUS_NAME + " " + Constants.AZUREUS_VERSION);     
      
      if ( referrer != null && referrer.length() > 0 ){
      
      	con.setRequestProperty( "Referer", referrer );
      }
      
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
        this.filename = URLDecoder.decode(tmp, Constants.DEFAULT_ENCODING );
      } else {
        this.filename = this.filename.substring(this.filename.indexOf('=') + 1);
        if (this.filename.startsWith("\"") && this.filename.endsWith("\""))
          this.filename = this.filename.substring(1, this.filename.lastIndexOf('\"'));
        File temp = new File(this.filename);
        this.filename = temp.getName();
      }

      this.directoryname = COConfigurationManager.getDirectoryParameter("General_sDefaultTorrent_Directory");
      boolean useTorrentSave = COConfigurationManager.getBooleanParameter("Save Torrent Files", true);

      if (file_str != null) {
        File temp = new File(file_str);

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
    } catch( Throwable e ){
        this.error("Exception while initializing download of '" + url + "':" + e.toString());   	
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
          
          	// if the file has come down with a not-so-useful name then we try to rename
          	// it to something more useful
          
          try{
          	if ( !filename.toLowerCase().endsWith(".torrent" )){

          		TOTorrent	torrent = TorrentUtils.readFromFile( file );
          		
          		String	name = TorrentUtils.getLocalisedName( torrent ) + ".torrent";
          		
          		File	new_file	= new File( directoryname, name );
          		
          		if ( file.renameTo( new_file )){
          			
          			filename	= name;
				
          			file	= new_file;
          		}
          	}
          }catch( Throwable e ){
          		
          	Debug.printStackTrace( e );
          }
          
          this.state = STATE_FINISHED;
        }
        this.notifyListener();
      } catch (Exception e) {
      	Debug.printStackTrace( e );
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

  public int getDownloadState() {
    return this.state;
  }

  public void setDownloadState(int state) {
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
