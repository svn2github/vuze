/* Written and copyright 2001-2003 Benjamin Kohl.
 * Distributed under the GNU General Public License; see the README file.
 * This code comes with NO WARRANTY.
 */
package org.gudy.azureus2.ui.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URLDecoder;

import java.util.HashMap;
import java.util.StringTokenizer;

import org.gudy.azureus2.core.ConfigurationManager;

/**
 * File: Jhttpp2BufferedFilterStream.java
 * @author Benjamin Kohl
 */
public class Jhttpp2ClientInputStream extends BufferedInputStream {
  private boolean filter = false;
  private String buf;
  private int lread = 0;
  /**
   * The length of the header (with body, if one)
   */
  private int header_length = 0;
  /**
   * The length of the (optional) body of the actual request
   */
  private int content_len = 0;
  /**
   * This is set to true with requests with bodies, like "POST"
   */
  private boolean body = false;
  private static Jhttpp2Server server;
  private Jhttpp2HTTPSession connection;
  private InetAddress remote_host;
  private String remote_host_name;
  private boolean ssl = false;
  private String errordescription;
  private int statuscode;
  
  public String url;
  public String method;
  public String useragent;
  public int remote_port = 0;
  public int post_data_len = 0;
  public HashMap vars = new HashMap();
  
  public int getHeaderLength() {
    return header_length;
  }
  
  public InetAddress getRemoteHost() { return remote_host; }
  public String getRemoteHostName() { return remote_host_name; }
  
  public Jhttpp2ClientInputStream(Jhttpp2Server server,Jhttpp2HTTPSession connection,InputStream a) {
    super(a);
    Jhttpp2ClientInputStream.server = server;
    this.connection=connection;
  }
  /**
   * Handler for the actual HTTP request
   * @exception IOException
   */
  public int read(byte[] a)throws IOException {
    ConfigurationManager cm = ConfigurationManager.getInstance();
    statuscode = Jhttpp2HTTPSession.SC_OK;
    if (ssl) return super.read(a);
    boolean cookies_enabled=server.enableCookiesByDefault();
    String rq="";
    header_length=0;
    post_data_len = 0;
    content_len = 0;
    boolean start_line=true;
    buf = getLine(); // reads the first line
    
    while (lread>2) {
      if (start_line) {
        start_line = false;
        int methodID = server.getHttpMethod(buf);
        switch (methodID) {
          case -1: statuscode = Jhttpp2HTTPSession.SC_NOT_SUPPORTED; break;
          case 2: ssl = true;
          default:
            InetAddress host = parseRequest(buf,methodID);
            if (statuscode != Jhttpp2HTTPSession.SC_OK) break; // error occured, go on with the next line
            
            if (!cm.getBooleanParameter("Server_bUseDownstreamProxy") && !ssl) {
              /* creates a new request without the hostname */
              buf = method + " " + url + " " + server.getHttpVersion() + "\r\n";
              lread = buf.length();
            }
            if ((cm.getBooleanParameter("Server_bUseDownstreamProxy") && connection.notConnected()) || !host.equals(remote_host)) {
              server.loggerWeb.debug("read_f: STATE_CONNECT_TO_NEW_HOST");
              statuscode = Jhttpp2HTTPSession.SC_CONNECTING_TO_HOST;
              remote_host = host;
            }
                                        /* -------------------------
                                         * url blocking (only "GET" method)
                                         * -------------------------*/
            if (cm.getBooleanParameter("Server_bProxyBlockURLs") && methodID==0 && statuscode!=Jhttpp2HTTPSession.SC_FILE_REQUEST) {
              server.loggerWeb.debug("Searching match...");
              Jhttpp2URLMatch match=server.findMatch(this.remote_host_name+url);
              if (match!=null){
                server.loggerWeb.debug("Match found!");
                cookies_enabled=match.getCookiesEnabled();
                if (match.getActionIndex()==-1) break;
                OnURLAction action=(OnURLAction)server.getURLActions().elementAt(match.getActionIndex());
                if (action.onAccesssDeny()) {
                  statuscode=Jhttpp2HTTPSession.SC_URL_BLOCKED;
                  if (action.onAccessDenyWithCustomText()) errordescription=action.getCustomErrorText();
                } else if (action.onAccessRedirect()) {
                  statuscode=Jhttpp2HTTPSession.SC_MOVED_PERMANENTLY;
                  errordescription=action.newLocation();
                }
              }//end if match!=null)
            } //end if (server.block...
        } // end switch
      }// end if(startline)
      else {
        if(buf.toString().startsWith("User-Agent"))
          this.useragent = new String(buf.toString());
                          /*-----------------------------------------------
                                 * Content-Length parsing
                                 *-----------------------------------------------*/
        if(server.startsWith(buf.toUpperCase(),"CONTENT-LENGTH")) {
          String clen=buf.substring(16);
          if (clen.indexOf("\r")!=-1) clen=clen.substring(0,clen.indexOf("\r"));
          else if(clen.indexOf("\n")!=-1) clen=clen.substring(0,clen.indexOf("\n"));
          try {
            content_len=Integer.parseInt(clen);
          }
          catch (NumberFormatException e) {
            statuscode=Jhttpp2HTTPSession.SC_CLIENT_ERROR;
          }
          if (server.loggerWeb.isDebugEnabled()) 
            server.loggerWeb.debug("read_f: content_len: " + content_len);
          if (!ssl) body=true; // Note: in HTTP/1.1 any method can have a body, not only "POST"
        }
        else if (server.startsWith(buf,"Proxy-Connection:")) {
          if (!cm.getBooleanParameter("Server_bUseDownstreamProxy")) buf=null;
          else {
            buf="Proxy-Connection: Keep-Alive\r\n";
            lread=buf.length();
          }
        }
              /*else if (server.startsWith(buf,"Connection:"))
              {
                 if (!server.use_proxy)
                 {
                   buf="Connection: Keep-Alive\r\n"; //use always keep-alive
                 lread=buf.length();
                 }
                 else buf=null;
               }*/
              /*-----------------------------------------------
               * cookie crunch section
               *-----------------------------------------------*/
        else if(server.startsWith(buf,"Cookie:")) {
          if (!cookies_enabled) buf=null;
        }
              /*------------------------------------------------
               * Http-Header filtering section
               *------------------------------------------------*/
        else if (cm.getBooleanParameter("Server_bProxyFilterHTTP")) {
          if(server.startsWith(buf,"Referer:")) {// removes "Referer"
            buf=null;
          } else if(server.startsWith(buf,"User-Agent")) // changes User-Agent
          {
            buf="User-Agent: " + server.getUserAgent() + "\r\n";
            lread=buf.length();
          }
        }
        
      }
      if (buf!=null) {
        rq+=buf;
        server.loggerWeb.debug(buf);
        header_length+=lread;
      }
      buf=getLine();
    }
    rq+=buf; //adds last line (should be an empty line) to the header String
    header_length+=lread;
    
    if (header_length==0) {
      server.loggerWeb.debug("header_length=0, setting status to SC_CONNECTION_CLOSED (buggy request)");
      statuscode=Jhttpp2HTTPSession.SC_CONNECTION_CLOSED;
    }
    
    for (int i=0;i<header_length;i++) a[i]=(byte)rq.charAt(i);
    
    // Parse GET variables
    if (url.indexOf('?') != -1) {
      String httpURIVarsTemp = url.substring(url.indexOf('?') + 1);
      if (server.loggerWeb.isDebugEnabled())
        server.loggerWeb.debug("Parsing GET Variables: "+httpURIVarsTemp);
      StringTokenizer URIVars = new StringTokenizer(httpURIVarsTemp, "&");
      while (URIVars.hasMoreTokens()) {
        String URIVar = URIVars.nextToken();
        try {
          vars.put(URLDecoder.decode(URIVar.substring(0, URIVar.indexOf('=')),"UTF-8"), URLDecoder.decode(URIVar.substring(URIVar.indexOf('=')+1),"UTF-8"));
        } catch (Exception e) {}
      }
    }
    
    if(body) {// read the body, if "Content-Length" given
      post_data_len = 0;
      String POSTdata = new String();
      while(post_data_len < content_len)  {
        a[header_length + post_data_len]=(byte)read(); // writes data into the array
        POSTdata += Character.toString((char) a[header_length + post_data_len]);
        post_data_len ++;
      }
      header_length += content_len; // add the body-length to the header-length
      if (post_data_len > 0) {
        StringTokenizer URIVars = new StringTokenizer(POSTdata, "&");
        if (server.loggerWeb.isDebugEnabled())
          server.loggerWeb.debug("Parsing POST Variables ("+Integer.toString(POSTdata.length())+"): "+POSTdata);
        while (URIVars.hasMoreTokens()) {
          String URIVar = URIVars.nextToken();
          try {
            vars.put(URLDecoder.decode(URIVar.substring(0, URIVar.indexOf('=')),"UTF-8"), URLDecoder.decode(URIVar.substring(URIVar.indexOf('=')+1),"UTF-8"));
          } catch (Exception e) {}
        }
      }
      body = false;
    }
    
    return (statuscode==Jhttpp2HTTPSession.SC_OK)?header_length:-1; // return -1 with an error
  }
  /**
   * reads a line
   * @exception IOException
   */
  public String getLine() throws IOException {
    int l=0; String line=""; lread=0;
    while(l!='\n') {
      l=read();
      if (l!=-1) {
        line+=(char)l;
        lread++;
      } else break;
    }
    return line;
  }
  /**
   * Parser for the first (!) line from the HTTP request<BR>
   * Sets up the URL, method and remote hostname.
   * @return an InetAddress for the hostname, null on errors with a statuscode!=SC_OK
   */
  public InetAddress parseRequest(String a,int method_index)  {
    ConfigurationManager cm = ConfigurationManager.getInstance();
    server.loggerWeb.debug(a);
    String f; int pos; url="";
    if (ssl) {
      f = a.substring(8);
    } else {
      method = a.substring(0,a.indexOf(" ")); //first word in the line
      pos = a.indexOf(":"); // locate first :
      if (pos == -1) { // occours with "GET / HTTP/1.1"
        url = a.substring(a.indexOf(" ")+1,a.lastIndexOf(" "));
        statuscode = Jhttpp2HTTPSession.SC_FILE_REQUEST;
        server.loggerWeb.log(SLevel.HTTP, connection.getLocalSocket().getInetAddress().getHostAddress() + " " + method + " " + getFullURL());
        return null;
      }
      f = a.substring(pos+3); //removes "http://"
    }
    pos=f.indexOf(" "); // locate space, should be the space before "HTTP/1.1"
    if (pos==-1) { // buggy request
      statuscode=Jhttpp2HTTPSession.SC_CLIENT_ERROR;
      errordescription="Your browser sent an invalid request: \""+ a + "\"";
      return null;
    }
    f = f.substring(0,pos); //removes all after space
    // if the url contains a space... it's not our mistake...(url's must never contain a space character)
    pos=f.indexOf("/"); // locate the first slash
    if (pos!=-1) {
      url=f.substring(pos); // saves path without hostname
      f=f.substring(0,pos); // reduce string to the hostname
    }
    else url="/"; // occurs with this request: "GET http://localhost HTTP/1.1"
    pos = f.indexOf(":"); // check for the portnumber
    if (pos!=-1) {
      String l_port =f.substring(pos+1);
      l_port=l_port.indexOf(" ")!=-1?l_port.substring(0,l_port.indexOf(" ")):l_port;
      int i_port=80;
      try {
        i_port = Integer.parseInt(l_port);
      }
      catch (NumberFormatException e_get_host) {
        server.loggerWeb.error("get_Host", e_get_host);
      }
      f = f.substring(0,pos);
      remote_port=i_port;
    }
    else remote_port = 80;
    remote_host_name = f;
    InetAddress address = null;
    server.loggerWeb.log(SLevel.HTTP, connection.getLocalSocket().getInetAddress().getHostAddress() + " " + method + " " + getFullURL());
    if (f.equals(cm.getStringParameter("Server_sAccessHost"))) {
      statuscode = Jhttpp2HTTPSession.SC_FILE_REQUEST;
      return null;
    }
    try {
      address = InetAddress.getByName(f);
      if (remote_port == cm.getIntParameter("Server_iPort") && address.equals(InetAddress.getLocalHost()))
        statuscode = Jhttpp2HTTPSession.SC_FILE_REQUEST;
    }
    catch (UnknownHostException e_u_host) {
      if (!cm.getBooleanParameter("Server_bUseDownstreamProxy")) statuscode = Jhttpp2HTTPSession.SC_HOST_NOT_FOUND;
    }
    if ((cm.getBooleanParameter("Server_bProxyGrabTorrents")) && (url.toUpperCase().endsWith(".TORRENT")))
      statuscode = Jhttpp2HTTPSession.SC_GRABBED_TORRENT;
    return address;
  }
  /**
   * @return boolean whether the actual connection was established with the CONNECT method.
   * @since 0.2.21
   */
  public boolean isTunnel() {
    return ssl;
  }
  /**
   * @return the full qualified URL of the actual request.
   * @since 0.4.0
   */
  public String getFullURL() {
    return "http" + (ssl?"s":"") + "://" + getRemoteHostName()
    + (remote_port!=80?(":" + remote_port):"") + url;
  }
  /**
   * @return status-code for the actual request
   * @since 0.3.5
   */
  public int getStatusCode() {
    return statuscode;
  }
  /**
   * @return the (optional) error-description for this request
   */
  public String getErrorDescription() {
    return errordescription;
  }
}
