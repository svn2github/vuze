/*
 * HTTPDownloader.java
 *
 * Created on 17. August 2003, 22:22
 */

package org.gudy.azureus2.server;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.MalformedURLException;
/*
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
*/
/**
 *
 * @author  Tobias Minich
 */
public class HTTPDownloader {
  
  private URL url;
  private HttpURLConnection con;
  private File file;
  private FileOutputStream fileout;
  
/*  private GetMethod get;
  private static HttpClient client = null;*/
  
  /** Creates a new instance of HTTPDownloader */
  public HTTPDownloader(String _url, String _file) throws MalformedURLException, IOException {
/*    if (client == null)
      client = new HttpClient(new MultiThreadedHttpConnectionManager());*/
    this.url = new URL(_url);
    this.con = (HttpURLConnection) this.url.openConnection();
    this.con.connect();
/*    get = new GetMethod(_url);
    this.get.setFollowRedirects(true);*/
    
    File temp = new File(_file);
    if (temp.isDirectory()) {
      String filename = URLDecoder.decode(this.url.getFile(), "UTF-8");
      this.file = new File(temp, filename.substring(filename.lastIndexOf('/')+1));
    } else
      this.file = temp;
    this.file.createNewFile();
    this.fileout = new FileOutputStream(this.file, false);
  }
  
  public String download() throws IOException/*, HttpException*/ {
    InputStream in = this.con.getInputStream();
    /*this.client.executeMethod(get);
    byte[] result = this.get.getResponseBody();
    if (result == null)
      throw new HttpException("No response on "+this.url.toString());
    InputStream in = new ByteArrayInputStream(result);*/
    
    byte[] buf = new byte[1020];
    int read = 0;
    int readtotal = 0;
    do {
      try {
        read = in.read(buf);
        readtotal += read;
      } catch (IOException e) {};
      if (read>0)
        this.fileout.write(buf, 0, read);
    } while (read > 0);
    in.close();
    this.fileout.flush();
    this.fileout.close();
    return this.file.getAbsolutePath();
  }
  
}
