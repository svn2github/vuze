/* Written and copyright 2001-2003 Benjamin Kohl.
 * Distributed under the GNU General Public License; see the README file.
 * This code comes with NO WARRANTY.
 */
package org.gudy.azureus2.ui.web;

import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.spi.LoggingEvent;

import HTML.Template;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerStats;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.peer.PEPeerStats;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.common.HTTPDownloader;

/**
 * One HTTP connection
 * @file Jhttpp2HTTPSession.java
 * @author Benjamin Kohl
 */
public class Jhttpp2HTTPSession extends Thread {
  
  public static final int SC_OK=0;
  public static final int SC_CONNECTING_TO_HOST=1;
  public static final int SC_HOST_NOT_FOUND=2;
  public static final int SC_URL_BLOCKED=3;
  public static final int SC_CLIENT_ERROR=4;
  public static final int SC_INTERNAL_SERVER_ERROR=5;
  public static final int SC_NOT_SUPPORTED=6;
  public static final int SC_REMOTE_DEBUG_MODE=7;
  public static final int SC_CONNECTION_CLOSED=8;
  public static final int SC_HTTP_OPTIONS_THIS=9;
  public static final int SC_FILE_REQUEST=10;
  public static final int SC_MOVED_PERMANENTLY=11;
  public static final int SC_GRABBED_TORRENT = 12;
  
  private static Jhttpp2Server server;
  
  /** downstream connections */
  private Socket client;
  private BufferedOutputStream out;
  private Jhttpp2ClientInputStream in;
  
  /** upstream connections */
  private Socket HTTP_Socket;
  private BufferedOutputStream HTTP_out;
  private Jhttpp2ServerInputStream HTTP_in;
  
  private boolean turnserveroff = false;
  
  private static Locale locale = new Locale("", "");
  private static Hashtable status = null;
  private static Hashtable parameterlegacy = null;
  private static Hashtable messagetextmap = null;
  private static Hashtable stuff = null;
  private HashMap dls = new HashMap();
  
  public Jhttpp2HTTPSession(Jhttpp2Server server,Socket client) {
    super("HTTP Session "+((server==null)?"(serverless)":"#"+Integer.toString(server.numconnections+1)));
    try {
      in = new Jhttpp2ClientInputStream(server,this,client.getInputStream());//,true);
      out = new BufferedOutputStream(client.getOutputStream());
      Jhttpp2HTTPSession.server=server;
      this.client=client;
      if (parameterlegacy==null) {
        parameterlegacy = new LegacyHashtable();
        parameterlegacy.put("Core_sOverrideIP", "Override Ip");
        parameterlegacy.put("Core_bAllocateNew", "Zero New");
        parameterlegacy.put("Core_iLowPort", "Low Port");
        parameterlegacy.put("Core_iHighPort", "High Port");
        parameterlegacy.put("Core_iMaxActiveTorrents", "max active torrents");
        parameterlegacy.put("Core_iMaxDownloads", "max downloads");
        parameterlegacy.put("Core_iMaxClients", "Max Clients");
        parameterlegacy.put("Core_iMaxUploads", "Max Uploads");
        parameterlegacy.put("Core_iMaxUploadSpeed", "Max Upload Speed");
        parameterlegacy.put("Core_bUseResume", "Use Resume");
        parameterlegacy.put("Core_iSaveResumeInterval", "Save Resume Interval");
        parameterlegacy.put("Core_bIncrementalAllocate", "Enable incremental file creation");
        parameterlegacy.put("Core_bCheckPiecesOnCompletion", "Check Pieces on Completion");
        parameterlegacy.put("Core_iSeedingShareStop", "Stop Ratio");
        parameterlegacy.put("Core_iSeedingRatioStop", "Stop Peers Ratio");
        parameterlegacy.put("Core_iSeedingRatioStart", "Start Peers Ratio");
        parameterlegacy.put("Core_bDisconnectSeed", "Disconnect Seed");
        parameterlegacy.put("Core_bSwitchPriority", "Switch Priority");
        parameterlegacy.put("Core_sPriorityExtensions", "priorityExtensions");
        messagetextmap = new LegacyHashtable();
        messagetextmap.put("allocatenew", "zeronewfiles");
        messagetextmap.put("lowport", "serverportlow");
        messagetextmap.put("highport", "serverporthigh");
        messagetextmap.put("useresume", "usefastresume");
        messagetextmap.put("enableincrementalfilecreation", "incrementalfile");
        messagetextmap.put("checkpiecesoncompletion", "checkOncompletion");
        messagetextmap.put("stopratio", "stopRatio");
        messagetextmap.put("stoppeersratio", "stopRatioPeers");
        messagetextmap.put("startpeersratio", "startRatioPeers");
        stuff = new Hashtable();
        stuff.put("favicon.ico", "org/gudy/azureus2/ui/icons/azureus.ico");
        stuff.put("froggy.png", "org/gudy/azureus2/ui/icons/tray.png");
      }
      if ((Jhttpp2HTTPSession.status == null) || (locale != Jhttpp2HTTPSession.server.locale)) {
        if (status != null)
          status.clear();
        else
          status = new Hashtable();
        status.put(new Integer(DownloadManager.STATE_WAITING), MessageText.getString("Main.download.state.waiting"));
        status.put(new Integer(DownloadManager.STATE_INITIALIZING), MessageText.getString("Main.download.state.waiting"));
        status.put(new Integer(DownloadManager.STATE_INITIALIZED), MessageText.getString("Main.download.state.waiting"));
        status.put(new Integer(DownloadManager.STATE_ALLOCATING), MessageText.getString("Main.download.state.allocating"));
        status.put(new Integer(DownloadManager.STATE_CHECKING), MessageText.getString("Main.download.state.checking"));
        status.put(new Integer(DownloadManager.STATE_READY), MessageText.getString("Main.download.state.ready"));
        status.put(new Integer(DownloadManager.STATE_DOWNLOADING), MessageText.getString("Main.download.state.downloading"));
        status.put(new Integer(DownloadManager.STATE_WAITING), MessageText.getString("Main.download.state.waiting"));
        status.put(new Integer(DownloadManager.STATE_SEEDING), MessageText.getString("Main.download.state.seeding"));
        status.put(new Integer(DownloadManager.STATE_STOPPING), MessageText.getString("Main.download.state.stopped"));
        status.put(new Integer(DownloadManager.STATE_STOPPED), MessageText.getString("Main.download.state.stopped"));
        status.put(new Integer(DownloadManager.STATE_ERROR), MessageText.getString("Main.download.state.error"));
        status.put(new Integer(DownloadManager.STATE_DUPLICATE), "Duplicate");
        
      }
    }
    catch (IOException e_io) {
      try {
        client.close();
      }
      catch (IOException e_io2) {}
      server.loggerWeb.error("Error while creating IO-Streams", e_io);
      return;
    }
    start();
  }
  public Socket getLocalSocket() {
    return client;
  }
  public Socket getRemoteSocket() {
    return HTTP_Socket;
  }
  public boolean isTunnel() {
    return in.isTunnel();
  }
  public boolean notConnected() {
    return HTTP_Socket==null;
  }
  public void sendHeader(int a,boolean b)throws IOException {
    sendHeader(a);
    endHeader();
    out.flush();
  }
  public void sendHeader(int status, String content_type, long content_length) throws IOException {
    sendHeader(status);
    sendLine("Content-Length", String.valueOf(content_length));
    sendLine("Content-Type", content_type );
  }
  public void sendLine(String s) throws IOException {
    write(out,s + "\r\n");
  }
  public void sendLine(String header, String s) throws IOException {
    write(out,header + ": " + s + "\r\n");
  }
  public void endHeader() throws IOException {
    write(out,"\r\n");
  }
  public void run() {
    server.loggerWeb.debug("begin http session");
    server.increaseNumConnections();
    try {
      handleRequest();
    }
    catch (IOException e_handleRequest) {
      server.loggerWeb.debug(e_handleRequest.toString(), e_handleRequest);
    }
    catch (Exception e) {
      //e.printStackTrace();
      server.loggerWeb.error("Jhttpp2HTTPSession.run()", e);
    }
    try {
      // close downstream connections
      in.close(); // since 0.4.10b
      out.close();
      client.close();
      // close upstream connections (webserver or other proxy)
      if (!notConnected()) {
        HTTP_Socket.close();
        HTTP_out.close();
        HTTP_in.close();
      }
    }
    catch (IOException e_run) {
      System.out.println(e_run.getMessage());
    }
    server.decreaseNumConnections();
    server.loggerWeb.debug("end http session");
  }
  /** sends a message to the user */
  public void sendErrorMSG(int a,String info)throws IOException {
    String statuscode = sendHeader(a);
    String localhost = "localhost";
    try {
      localhost = InetAddress.getLocalHost().getHostName() + ":" + COConfigurationManager.getIntParameter("Server_iPort");
    }
    catch(UnknownHostException e_unknown_host ) {}
    String msg = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\"><html>\r"
    + "<!-- jHTTPp2 error message --><HEAD>\r"
    + "<TITLE>" + statuscode + "</TITLE>\r"
    + "<link rel=\"stylesheet\" type=\"text/css\" href=\"http://" + localhost + "/style.css\"></HEAD>\r"  // use css style sheet in htdocs
    + "<BODY BGCOLOR=\"#FFFFFF\" TEXT=\"#000000\" LINK=\"#000080\" VLINK=\"#000080\" ALINK=\"#000080\">\r"
    + "<h2 class=\"headline\">HTTP " + statuscode + " </h2>\r"
    + "<HR size=\"4\">\r"
    + "<p class=\"i30\">Your request for the following URL failed:</p>"
    + "<p class=\"tiagtext\"><a href=\"" + in.getFullURL() + "\">" + in.getFullURL() + "</A> </p>\r"
    + "<P class=\"i25\">Reason: " + info + "</P>"
    + "<HR size=\"4\">\r"
    + "<p class=\"i25\"><A HREF=\"http://jhttp2.sourceforge.net/\">jHTTPp2</A> HTTP Proxy, Version " + server.getServerVersion() + " at " + localhost
    + "<br>Copyright &copy; 2001-2003 <A HREF=\"mailto:bkohl@users.sourceforge.net\">Benjamin Kohl</A></p>\r"
    + "<p class=\"i25\"><A HREF=\"http://" + localhost + "/\">jHTTPp2 local website</A> <A HREF=\"http://" + localhost + "/" + server.WEB_CONFIG_FILE + "\">Configuration</A></p>"
    + "</BODY></HTML>";
    sendLine("Content-Length",String.valueOf(msg.length()));
    sendLine("Content-Type","text/html; charset=iso-8859-1");
    endHeader();
    write(out,msg);
    out.flush();
  }
  
  public String sendHeader(int a)throws IOException	{
    String stat;
    switch(a) {
      case 200:stat="200 OK"; break;
      case 202:stat="202 Accepted"; break;
      case 300:stat="300 Ambiguous"; break;
      case 301:stat="301 Moved Permanently"; break;
      case 400:stat="400 Bad Request"; break;
      case 401:stat="401 Denied"; break;
      case 403:stat="403 Forbidden"; break;
      case 404:stat="404 Not Found"; break;
      case 405:stat="405 Bad Method"; break;
      case 413:stat="413 Request Entity Too Large"; break;
      case 415:stat="415 Unsupported Media"; break;
      case 501:stat="501 Not Implemented"; break;
      case 502:stat="502 Bad Gateway"; break;
      case 504:stat="504 Gateway Timeout"; break;
      case 505:stat="505 HTTP Version Not Supported"; break;
      default: stat="500 Internal Server Error";
    }
    sendLine(server.getHttpVersion() + " " + stat);
    sendLine("Server",server.getServerIdentification());
    if (a==501) sendLine("Allow","GET, HEAD, POST, PUT, DELETE, CONNECT");
    sendLine("Cache-Control", "no-cache, must-revalidate");
    sendLine("Connection","close");
    return stat;
  }
  
  /** the main routine, where it all happens */
  public void handleRequest() throws Exception {
    InetAddress remote_host;
    Jhttpp2Read remote_in=null;
    int remote_port;
    byte[] b=new byte[65536];
    int numread=in.read(b);
    
    while(true) { // with this loop we support persistent connections
      if (numread==-1) { // -1 signals an error
        if (in.getStatusCode()!=SC_CONNECTING_TO_HOST) {
          switch (in.getStatusCode()) {
            case SC_CONNECTION_CLOSED: break;
            case SC_CLIENT_ERROR: sendErrorMSG(400,"Your client sent a request that this proxy could not understand. (" + in.getErrorDescription() + ")"); break;
            case SC_HOST_NOT_FOUND: sendErrorMSG(504,"Host not found.<BR>jHTTPp2 was unable to resolve the hostname of this request. <BR>Perhaps the hostname was misspelled, the server is down or you have no connection to the internet."); break;
            case SC_INTERNAL_SERVER_ERROR: sendErrorMSG(500,"Server Error! (" + in.getErrorDescription() + ")"); break;
            case SC_NOT_SUPPORTED: sendErrorMSG(501,"Your client used a HTTP method that this proxy doesn't support: (" + in.getErrorDescription() + ")"); break;
            case SC_URL_BLOCKED: sendErrorMSG(403,(in.getErrorDescription()!=null && in.getErrorDescription().length()>0?in.getErrorDescription():"The request for this URL was denied by the jHTTPp2 URL-Filter.")); break;
            //case SC_REMOTE_DEBUG_MODE: remoteDebug(); break;
            case SC_HTTP_OPTIONS_THIS: sendHeader(200); endHeader(); break;
            case SC_FILE_REQUEST: file_handler(); break;
            case SC_GRABBED_TORRENT: torrent_handler(); break;
            //case SC_CONFIG_RQ: admin_handler(b); break;
            //case SC_HTTP_TRACE:
            case SC_MOVED_PERMANENTLY:
              sendHeader(301);
              write(out,"Location: " + in.getErrorDescription() + "\r\n");
              endHeader();
              out.flush();
            default:
          }
          break; // return from main loop.
        }
        else { // also an error because we are not connected (or to the wrong host)
          // Creates a new connection to a remote host.
          if (!notConnected()) {
            try {
              HTTP_Socket.close();
            }
            catch (IOException e_close_socket) {}
          }
          numread=in.getHeaderLength(); // get the header length
          if (!COConfigurationManager.getBooleanParameter("Server_bUseDownstreamProxy")) {// sets up hostname and port
            remote_host=in.getRemoteHost();
            remote_port=in.remote_port;
          }
          else {
            remote_host=InetAddress.getByName(COConfigurationManager.getStringParameter("Server_sDownstreamProxyHost"));
            remote_port=COConfigurationManager.getIntParameter("Server_iDownstreamProxyPort");
          }
          //if (server.debug)server.writeLog("Connect: " + remote_host + ":" + remote_port);
          try {
            connect(remote_host,remote_port);
          }
          catch (IOException e_connect) {
            server.loggerWeb.debug(e_connect.toString(), e_connect);
            sendErrorMSG(502,"Error while creating a TCP connecting to [" +remote_host.getHostName()+ ":" + remote_port + "] <BR>The proxy server cannot connect to the given address or port [" + e_connect.toString() + "]");
            break;
          }
          catch (Exception e) {
            server.loggerWeb.error(e.toString(), e);
            sendErrorMSG(500,"Error: " + e.toString());
            break;
          }
          if (!in.isTunnel()  || (in.isTunnel() && COConfigurationManager.getBooleanParameter("Server_bUseDownstreamProxy"))) { // no SSL-Tunnel or SSL-Tunnel with another remote proxy: simply forward the request
            HTTP_out.write(b, 0, numread);
            HTTP_out.flush();
          }
          else { //  SSL-Tunnel with "CONNECT": creates a tunnel connection with the server
            sendLine(server.getHttpVersion() + " 200 Connection established");
            sendLine("Proxy-Agent",server.getServerIdentification());
            endHeader(); out.flush();
          }
          remote_in = new Jhttpp2Read(server,this, HTTP_in, out); // reads data from the remote server
          server.addBytesWritten(numread);
        }
      }
      while(true) { // reads data from the client
        numread=in.read(b);
        //if (server.debug)server.writeLog("Jhttpp2HTTPSession: " + numread + " Bytes read.");
        if (numread!=-1) {
          HTTP_out.write(b, 0, numread);
          HTTP_out.flush();
          server.addBytesWritten(numread);
        } else break;
      } // end of inner loop
    }// end of main loop
    out.flush();
    if (!notConnected() && remote_in != null)
      remote_in.close(); // close Jhttpp2Read thread
    return;
  }
  /** connects to the given host and port */
  public void connect(InetAddress host,int port)throws IOException {
    HTTP_Socket = new Socket(host,port);
    HTTP_in = new Jhttpp2ServerInputStream(server,this,HTTP_Socket.getInputStream(),false);
    HTTP_out = new BufferedOutputStream(HTTP_Socket.getOutputStream());
  }
  /** converts an String into a Byte-Array to write it with the OutputStream */
  public void write(BufferedOutputStream o,String p)throws IOException {
    o.write(p.getBytes(),0,p.length());
  }
  
  /**
   * Small webserver for local files in {app}/htdocs
   * @since 0.4.04
   */
  private void handleConfigInt(Template tmpl, String name) {
    String po = MessageText.getString("ConfigView.label."+messagetextmap.get(name.substring(name.indexOf('_')+2).toLowerCase()));
    if (!po.startsWith("!"))
      tmpl.setParam("Options_"+name+"_D", po);
    tmpl.setParam("Options_"+name, COConfigurationManager.getIntParameter(parameterlegacy.get(name).toString()));
  }
  
  private void handleConfigBool(Template tmpl, String name) {
    String po = MessageText.getString("ConfigView.label."+messagetextmap.get(name.substring(name.indexOf('_')+2).toLowerCase()));
    if (!po.startsWith("!"))
      tmpl.setParam("Options_"+name+"_D", po);
    if (COConfigurationManager.getBooleanParameter(parameterlegacy.get(name).toString()))
      tmpl.setParam("Options_"+name, 1);
  }
  
  private void handleConfigStr(Template tmpl, String name) {
    String po = MessageText.getString("ConfigView.label."+messagetextmap.get(name.substring(name.indexOf('_')+2).toLowerCase()));
    if (!po.startsWith("!"))
      tmpl.setParam("Options_"+name+"_D", po);
    tmpl.setParam("Options_"+name, COConfigurationManager.getStringParameter(parameterlegacy.get(name).toString()));
  }
  
  private void handleConfig(Template tmpl) {
    handleConfigStr(tmpl, "General_sDefaultSave_Directory");
    handleConfigStr(tmpl, "General_sDefaultTorrent_Directory");
    handleConfigStr(tmpl, "Core_sOverrideIP");
    handleConfigBool(tmpl,"Core_bAllocateNew");
    handleConfigInt(tmpl, "Core_iLowPort");
    handleConfigInt(tmpl, "Core_iHighPort");
    handleConfigInt(tmpl, "Core_iMaxActiveTorrents");
    handleConfigInt(tmpl, "Core_iMaxDownloads");
    handleConfigInt(tmpl, "Core_iMaxClients");
    handleConfigInt(tmpl, "Core_iMaxUploads");
    handleConfigInt(tmpl, "Core_iMaxUploadSpeed");
    handleConfigBool(tmpl,"Core_bUseResume");
    handleConfigInt(tmpl, "Core_iSaveResumeInterval");
    handleConfigBool(tmpl,"Core_bIncrementalAllocate");
    handleConfigBool(tmpl,"Core_bCheckPiecesOnCompletion");
    handleConfigInt(tmpl, "Core_iSeedingShareStop");
    handleConfigInt(tmpl, "Core_iSeedingRatioStop");
    handleConfigInt(tmpl, "Core_iSeedingRatioStart");
    handleConfigBool(tmpl,"Core_bDisconnectSeed");
    handleConfigBool(tmpl,"Core_bSwitchPriority");
    handleConfigStr(tmpl, "Core_sPriorityExtensions");
    handleConfigStr(tmpl, "Server_sName");
    handleConfigStr(tmpl, "Server_sBindIP");
    handleConfigInt(tmpl, "Server_iPort");
    handleConfigInt(tmpl, "Server_iTimeout");
    handleConfigStr(tmpl, "Server_sTemplate_Directory");
    handleConfigInt(tmpl, "Server_iMaxHTTPConnections");
    handleConfigInt(tmpl, "Server_iRefresh");
    handleConfigBool(tmpl,"Server_bNoJavaScript");
    handleConfigStr(tmpl, "Server_sAllowStatic");
    handleConfigStr(tmpl, "Server_sAllowDynamic");
    handleConfigInt(tmpl, "Server_iRecheckDynamic");
    handleConfigStr(tmpl, "Server_sAccessHost");
    handleConfigBool(tmpl,"Server_bProxyEnableCookies");
    handleConfigBool(tmpl,"Server_bProxyBlockURLs");
    handleConfigBool(tmpl,"Server_bProxyFilterHTTP");
    handleConfigStr(tmpl, "Server_sProxyUserAgent");
    handleConfigBool(tmpl,"Server_bProxyGrabTorrents");
    handleConfigBool(tmpl,"Server_bUseDownstreamProxy");
    handleConfigStr(tmpl, "Server_sDownstreamProxyHost");
    handleConfigInt(tmpl, "Server_iDownstreamProxyPort");

    handleConfigInt(tmpl, "Server_iLogCount");
    //    if (config.getIntParameter("Server_iVerbosity") != this.server.verbosity())
    //      tmpl.setParam("Override_Verbosity", "Verbosity overridden via command line to "+this.server.verbosity());
    handleConfigInt(tmpl, "Server_iLogLevelWebinterface");
    handleConfigInt(tmpl, "Server_iLogLevelCore");
    handleConfigBool(tmpl,"Server_bLogFile");
    handleConfigStr(tmpl, "Server_sLogFile");
  }
  
  private void handleTorrents(Template tmpl) {
    List torrents = server.gm.getDownloadManagers();
    DownloadManager dm;
    int dmstate;
    Hashtable h;
    if (!torrents.isEmpty()) {
      Vector v = new Vector();
      Iterator torrent = torrents.iterator();
      long totalReceived = 0;
      long totalSent = 0;
      long totalDiscarded = 0;
      int connectedSeeds = 0;
      int connectedPeers = 0;
      PEPeerStats ps;
      while (torrent.hasNext()) {
        dm = (DownloadManager) torrent.next();
        TRTrackerScraperResponse hd = dm.getTrackerScrapeResponse();
        dmstate = dm.getState();
        try {
          ps = dm.getPeerManager().getStats();
        } catch (Exception e) {ps = null;}
        if (ps != null) {
          totalReceived += ps.getTotalReceivedRaw();
          totalSent += ps.getTotalSentRaw();
          totalDiscarded += ps.getTotalDiscardedRaw();
          connectedSeeds += dm.getNbSeeds();
          connectedPeers += dm.getNbPeers();
        }
        h = new Hashtable();
        if (dmstate == DownloadManager.STATE_STOPPED) {
          h.put("Torrents_Torrent_Command", "Resume");
          h.put("Torrents_Torrent_Stopped", Boolean.TRUE);
        } else
          h.put("Torrents_Torrent_Command", "Stop");
        if ((hd == null) || (hd.getSeeds() == 0))
          h.put("Torrents_Torrent_Seedless", Boolean.TRUE);
        if (dmstate == DownloadManager.STATE_INITIALIZING)
          h.put("Torrents_Torrent_Initializing", Boolean.TRUE);
        else if (dmstate == DownloadManager.STATE_ALLOCATING)
          h.put("Torrents_Torrent_Allocating", Boolean.TRUE);
        else if (dmstate == DownloadManager.STATE_CHECKING)
          h.put("Torrents_Torrent_Checking", Boolean.TRUE);
        
        DownloadManagerStats stats = dm.getStats();
        
        try {
          h.put("Torrents_Torrent_PercentDone", Integer.toString(stats.getCompleted()/10));
          h.put("Torrents_Torrent_PercentLeft", Integer.toString((1000-stats.getCompleted())/10));
          h.put("Torrents_Torrent_PercentDonePrec", Float.toString(((float) stats.getCompleted())/10));
          h.put("Torrents_Torrent_PercentLeftPrec", Float.toString((1000- (float) stats.getCompleted())/10));
        } catch (ArithmeticException e) {}
        h.put("Torrents_Torrent_SpeedDown", stats.getDownloadSpeed());
        h.put("Torrents_Torrent_SpeedUp", stats.getUploadSpeed());
        h.put("Torrents_Torrent_FileSize", DisplayFormatters.formatByteCountToKBEtc(dm.getSize()));
        try {
          h.put("Torrents_Torrent_FileSizeDone", DisplayFormatters.formatByteCountToKBEtc((((long) stats.getCompleted())*((long) dm.getSize()))/1000));
        } catch (ArithmeticException e) {}
        if (dm.getName()==null)
          h.put("Torrents_Torrent_FileName", "?");
        else
          h.put("Torrents_Torrent_FileName", dm.getName());
        if (dmstate == DownloadManager.STATE_ERROR)
          h.put("Torrents_Torrent_Error", dm.getErrorDetails());
        h.put("Torrents_Torrent_Status", Jhttpp2HTTPSession.status.get(new Integer(dmstate)));
        if (hd == null || !hd.isValid()) {
          h.put("Torrents_Torrent_Seeds", "?");
          h.put("Torrents_Torrent_Peers", "?");
        } else {
          h.put("Torrents_Torrent_Seeds", Integer.toString(hd.getSeeds()));
          h.put("Torrents_Torrent_Peers", Integer.toString(hd.getPeers()));
        }
        h.put("Torrents_Torrent_SeedsConnected", Integer.toString(dm.getNbSeeds()));
        h.put("Torrents_Torrent_PeersConnected", Integer.toString(dm.getNbPeers()));
        h.put("Torrents_Torrent_ETA", (stats.getETA()=="")?"&nbsp;":stats.getETA());
        h.put("Torrents_Torrent_SizeDown", stats.getDownloaded());
        h.put("Torrents_Torrent_SizeUp", stats.getUploaded());
        h.put("Torrents_Torrent_Hash", ByteFormatter.nicePrintTorrentHash(dm.getTorrent(), true));
        if ((in.useragent.toUpperCase().indexOf("LYNX")!=-1) || (in.useragent.toUpperCase().indexOf("LINKS")!=-1) || COConfigurationManager.getBooleanParameter("Server_bNoJavaScript"))
          h.put("Global_NoJavaScript", Boolean.TRUE);
        v.addElement(h);
      }
      tmpl.setParam("Torrents_Torrents", v);
      tmpl.setParam("Torrents_TotalSpeedDown", server.gm.getDownloadSpeed());
      tmpl.setParam("Torrents_TotalSpeedUp", server.gm.getUploadSpeed());
      tmpl.setParam("Torrents_TotalSizeDown", DisplayFormatters.formatByteCountToKBEtc(totalReceived));
      tmpl.setParam("Torrents_TotalSizeUp", DisplayFormatters.formatByteCountToKBEtc(totalSent));
      tmpl.setParam("Torrents_TotalSizeDiscarded", DisplayFormatters.formatByteCountToKBEtc(totalDiscarded));
      tmpl.setParam("Torrents_TotalSeedsConnected", Integer.toString(connectedSeeds));
      tmpl.setParam("Torrents_TotalPeersConnected", Integer.toString(connectedPeers));
    }
  }
  
  private void handleTorrentInfo(Template tmpl) {
    if ((!in.vars.isEmpty()) && in.vars.containsKey("hash")) {
      tmpl.setParam("TorrentInfo_Hash", in.vars.get("hash").toString());
    }
  }
  
  private void handleLog(Template tmpl) {
    SimpleDateFormat temp = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
    tmpl.setParam("Log_Logtime", temp.format(new Date()));
    tmpl.setParam("Log_Starttime", temp.format(server.startTime));
    tmpl.setParam("Log_Count", server.logList.size());
    if (server.logList.size()>0) {
      ListIterator it = server.logList.listIterator(server.logList.size()-1);
      Vector v = new Vector();
      Hashtable h;
      while (it.hasPrevious()) {
        LoggingEvent rec = (LoggingEvent) it.previous();
        h = new Hashtable();
        h.put("Log_Logs_TimeCode", temp.format(new Date(rec.timeStamp)));
        h.put("Log_Logs_Message", rec.getLevel().toString() + " -- " + rec.getMessage());
        if (rec.getLevel().equals(SLevel.FATAL))
          h.put("Log_Logs_LevelError", Boolean.TRUE);
        v.addElement(h);
      }
      tmpl.setParam("Log_Logs", v);
    }
  }
  
  private void ProcessConfigVars(HashMap URIvars) {
    Set keys = URIvars.keySet();
    Iterator key = keys.iterator();
    File temp;
    while (key.hasNext()) {
      String k = (String) key.next();
      if (k.startsWith("Options_")) {
        String option = k.substring(k.indexOf('_')+1);
        String value = (String) URIvars.get(k);
        if (option.endsWith("_Directory")) {
          temp = new File(value);
          if (!temp.exists())
            temp.mkdirs();
          else if (!temp.isDirectory()) {
            server.loggerWeb.fatal("Configuration error. This is not a directory: "+value);
            continue;
          }
        }
        if (option.substring(option.indexOf('_')+1).startsWith("s"))
			COConfigurationManager.setParameter(parameterlegacy.get(option).toString(), value);
        else
			COConfigurationManager.setParameter(parameterlegacy.get(option).toString(), Integer.parseInt(value));
      }
    }
	COConfigurationManager.save();
    server.initLoggers();
    server.initAccess();
  }
  
  private void ProcessAdd(HashMap URIvars) {
    if (URIvars.containsKey("Add_torrent") && !((String) URIvars.get("Add_torrent")).equals("")) {
      try {
        HTTPDownloader dl = new HTTPDownloader((String) URIvars.get("Add_torrent"), COConfigurationManager.getDirectoryParameter("General_sDefaultTorrent_Directory"));
        String file = dl.download();
        server.gm.addDownloadManager(file, COConfigurationManager.getDirectoryParameter("General_sDefaultSave_Directory"));
        server.loggerWeb.info("Download of "+(String)URIvars.get("Add_torrent")+" succeeded");
      } catch (Exception e) {
        server.loggerWeb.error("Download of "+(String)URIvars.get("Add_torrent")+" failed", e);
      }
    }
  }
  
  private void UpdateDls () {
      dls.clear();
      List torrents = server.gm.getDownloadManagers();
      if (!torrents.isEmpty()) {
        Iterator torrent = torrents.iterator();
        while (torrent.hasNext()) {
          DownloadManager dm = (DownloadManager) torrent.next();
          dls.put(ByteFormatter.nicePrintTorrentHash(dm.getTorrent(), true), dm);
        }
      }
  }
  
  private void ProcessTorrent(HashMap URIvars) {
    if (URIvars.containsKey("subcommand")) {
      String subcommand = (String) URIvars.get("subcommand");
      if (server.loggerWeb.isDebugEnabled())
        server.loggerWeb.debug("ProcessTorrent: "+subcommand);
      List torrents = server.gm.getDownloadManagers();
      if (!torrents.isEmpty()) {
        UpdateDls();
        
        Set keys = URIvars.keySet();
        Iterator ikeys = keys.iterator();
        while (ikeys.hasNext()) {
          String key = (String) ikeys.next();
          String value = (String) URIvars.get(key);
          if (server.loggerWeb.isDebugEnabled())
            server.loggerWeb.debug("ProcessTorrent: ("+key+"/"+value+")");
          if (value.equals("1") && key.startsWith("Torrent_Hash_")) {
            String hash = key.substring(key.lastIndexOf('_')+1);
            if (server.loggerWeb.isDebugEnabled())
              server.loggerWeb.debug("ProcessTorrent: \""+hash+"\"");
            if (dls.containsKey(hash)) {
              if (server.loggerWeb.isDebugEnabled())
                server.loggerWeb.debug("ProcessTorrent: \""+hash+"\" processed");
              DownloadManager dm = (DownloadManager) dls.get(hash);
              if (subcommand.equals("Pause") && ((dm.getState()!=DownloadManager.STATE_STOPPED) || (dm.getState()!=DownloadManager.STATE_STOPPING)))
                dm.stopIt();
              else if (subcommand.equals("Start") && ((dm.getState()==DownloadManager.STATE_READY) || (dm.getState()==DownloadManager.STATE_WAITING) || (dm.getState()==DownloadManager.STATE_STOPPED)))
                dm.startDownloadInitialized(true);
              else if (subcommand.equals("Cancel")) {
                dm.stopIt();
                server.gm.removeDownloadManager(dm);
              }
            }
          }
        }
      }
    }
  }
  
  public void Process(HashMap URIvars) {
    if (URIvars.containsKey("command")) {
      String command = (String) URIvars.get("command");
      if (command.equals("Config"))
        this.ProcessConfigVars(URIvars);
      else if (command.equals("Add"))
        this.ProcessAdd(URIvars);
      else if (command.equals("Exit"))
        turnserveroff = true;
      else if (command.equals("Torrent"))
        this.ProcessTorrent(URIvars);
    }
  }
  
  public void file_handler() throws IOException {
    String filename;
    if (in.url.indexOf('?')!=-1)
      filename = in.url.substring(0, in.url.indexOf('?'));
    else
      filename = in.url;
    String sep = System.getProperty("file.separator");
    if (filename.equals("/")) filename="index"; // convert / to index.html
    else if (filename.startsWith("/")) filename=filename.substring(1);
    if (filename.endsWith("/")) filename+="index"; // add index.html, if ending with /
    File file = null;
    File fileuser = new File(COConfigurationManager.getDirectoryParameter("Server_sTemplate_Directory")+ sep + filename); // access only files in "htdocs"
    String fileres = "org/gudy/azureus2/ui/web/template/"+filename;
    //    File filedef = new File("org/gudy/azureus2/server/template/"+filename);
    boolean useres = false;
    if (filename.indexOf("..")!=-1) {
      sendErrorMSG(404,"The requested file /" + filename + " was not found or the path is invalid.");
      return;
    } else if (fileuser.exists() && fileuser.canRead()) {
      if (fileuser.isDirectory()) {
        if (ClassLoader.getSystemResource(fileres)!=null) {
          useres = true;
        } else {
          File filenew = new File(fileuser, sep+"index.tmpl");
          if (filenew.exists() && filenew.canRead() && !filenew.isDirectory()) {
            file = filenew;
            filename += "/index.tmpl";
          } else {
            filenew = new File(fileuser, sep+"index.html");
            if (filenew.exists() && filenew.canRead() && !filenew.isDirectory()) {
              file = filenew;
              filename += "/index.html";
            }
          }
        }
      } else {
        file = fileuser;
      }
    } else if (ClassLoader.getSystemResource(fileres)!=null) {
      useres = true;
    } else if (ClassLoader.getSystemResource(fileres+"/index.tmpl")!=null) {
      useres = true;
      fileres += "/index.tmpl";
      filename += "/index.tmpl";
    } else if (ClassLoader.getSystemResource(fileres+"/index.html")!=null) {
      useres = true;
      fileres += "/index.html";
      filename += "/index.html";
    } else if (filename.indexOf('.')==-1) {
      File filenew = new File(fileuser, ".tmpl");
      if (filenew.exists() && filenew.canRead() && !filenew.isDirectory()) {
        file = filenew;
        filename += ".tmpl";
      } else {
        filenew = new File(fileuser, ".html");
        if (filenew.exists() && filenew.canRead() && !filenew.isDirectory()) {
          file = filenew;
          filename += ".html";
        } else if (ClassLoader.getSystemResource(fileres+".tmpl")!=null) {
          useres = true;
          fileres += ".tmpl";
          filename += ".tmpl";
        } else if (ClassLoader.getSystemResource(fileres+".tmpl")!=null) {
          useres = true;
          fileres += ".html";
          filename += ".html";
        }
      }
    } else if (stuff.containsKey(filename)) {
      useres = true;
      fileres = (String) stuff.get(filename);
    } else if (filename.compareToIgnoreCase("TorrentInfo.png")==0) {
      if ((!in.vars.isEmpty()) && in.vars.containsKey("hash"))
        if (!dls.containsKey(in.vars.get("hash")))
          UpdateDls();
        if (dls.containsKey(in.vars.get("hash"))) {
          useres = true;
          fileres = null;
        }
    }
    if ((file == null) && !useres) {
      sendErrorMSG(404,"The requested file /" + filename + " was not found or the path is invalid.");
      return;
    }
    
    if (!in.vars.isEmpty())
      Process(in.vars);
    
    Template tmpl = null;
    int pos = filename.lastIndexOf("."); // MIME type of the specified file
    String content_type="text/plain"; // all unknown content types will be marked as text/plain
    if (pos != -1) {
      String extension = filename.substring(pos+1);
      if (extension.equalsIgnoreCase("tmpl")) {
        try {
          tmpl = TemplateCache.getInstance().get(filename);
        } catch (Exception e) {
          sendErrorMSG(404,"The requested file /" + filename + " was not found or the path is invalid. This is quite odd at this stage, so something is fubared. Exception: "+e.getMessage());
          return;
        }
      }
      if (extension.equalsIgnoreCase("htm") || extension.equalsIgnoreCase("html") || extension.equalsIgnoreCase("tmpl")) content_type="text/html; charset=iso-8859-1";
      else if (extension.equalsIgnoreCase("jpg") || (extension.equalsIgnoreCase("jpeg"))) content_type="image/jpeg";
      else if (extension.equalsIgnoreCase("gif")) content_type = "image/gif";
      else if (extension.equalsIgnoreCase("png")) content_type = "image/png";
      else if (extension.equalsIgnoreCase("ico")) content_type = "image/ico";
      else if (extension.equalsIgnoreCase("css")) content_type = "text/css";
      else if (extension.equalsIgnoreCase("pdf")) content_type = "application/pdf";
      else if (extension.equalsIgnoreCase("ps") || extension.equalsIgnoreCase("eps")) content_type = "application/postscript";
      else if (extension.equalsIgnoreCase("xml")) content_type = "text/xml";
    }
    if (tmpl == null) {
      InputStream in_st;
      if (useres) {
        if (fileres==null)
          in_st = new TorrentInfoPNGStream(in.vars, (DownloadManager) dls.get(in.vars.get("hash")));
        else
          in_st = ClassLoader.getSystemResourceAsStream(fileres);
      } else
        in_st = new FileInputStream(file);
      //BufferedInputStream file_in = new BufferedInputStream(in_st);
      InputStream file_in = in_st;
      sendHeader(200,content_type, in_st.available());
      endHeader();
      byte[] buffer=new byte[4096];
      int a=file_in.read(buffer);
      while (a!=-1) { // read until EOF
        out.write(buffer,0,a);
        a = file_in.read(buffer);
      }
      out.flush();
      file_in.close(); // finished!
      in_st.close();
    } else {
      tmpl.setParam("Global_ServerName", COConfigurationManager.getStringParameter("Server_sName"));
      if (COConfigurationManager.getIntParameter("Server_iRefresh")!=0)
        tmpl.setParam("Global_Refresh", COConfigurationManager.getIntParameter("Server_iRefresh"));
      if ((in.useragent.toUpperCase().indexOf("LYNX")!=-1) || (in.useragent.toUpperCase().indexOf("LINKS")!=-1) || COConfigurationManager.getBooleanParameter("Server_bNoJavaScript"))
        tmpl.setParam("Global_NoJavaScript", Boolean.TRUE);
      TemplateCache tc = TemplateCache.getInstance();
      if (tc.needs(filename, "Options"))
        this.handleConfig(tmpl);
      if (tc.needs(filename, "Torrents"))
        this.handleTorrents(tmpl);
      if (tc.needs(filename, "TorrentInfo"))
        this.handleTorrentInfo(tmpl);
      if (tc.needs(filename, "Log"))
        this.handleLog(tmpl);
      String output = tmpl.output();
      sendHeader(200,content_type, output.length());
      endHeader();
      out.write(output.getBytes(), 0, output.length());
      out.flush();
      if (turnserveroff)
        server.shutdownServer();
    }
  }
  
  public void torrent_handler() throws IOException {
      String fwd;
      try {
        HTTPDownloader dl = new HTTPDownloader("http://"+this.in.getRemoteHostName()+":"+Integer.toString(in.remote_port)+in.url, COConfigurationManager.getDirectoryParameter("General_sDefaultTorrent_Directory"));
        String file = dl.download();
        server.gm.addDownloadManager(file, COConfigurationManager.getDirectoryParameter("General_sDefaultSave_Directory"));
        server.loggerWeb.info("Download of "+"http://"+this.in.getRemoteHostName()+":"+Integer.toString(in.remote_port)+in.url+" succeeded");
        fwd=COConfigurationManager.getStringParameter("Server_sProxySuccessRedirect");
      } catch (Exception e) {
        server.loggerWeb.error("Download of "+"http://"+this.in.getRemoteHostName()+":"+Integer.toString(in.remote_port)+in.url+" failed", e);
        fwd="dl_fail";
      }
      sendHeader(301);
      write(out,"Location: http://" + COConfigurationManager.getStringParameter("Server_sAccessHost")+"/"+fwd + "\r\n");
      endHeader();
      out.flush();
  }
  /**
   * @since 0.4.10b
   */
  public int getStatus() {
    return in.getStatusCode();
  }
  /**
   * @since 0.4.20a
   * admin webpage
   */
  /*
  public void admin_handler(byte[] b) throws IOException {
    Jhttpp2Admin admin = null;
    String filename = in.url;
    if (in.post_data_len > 0) { // if the client used "POST" then append the data to the filename
      filename = filename + "?" + new String(b,in.getHeaderLength()-in.post_data_len,in.post_data_len);
    }
    if (filename.startsWith("/")) filename = filename.substring(1);
    String adminpage = "";
    try {
      admin = new Jhttpp2Admin( filename, server ) ;
      admin.WebAdmin();
      adminpage = admin.HTMLAdmin();
    }
    catch( Exception e) {
      e.printStackTrace();
      server.writeLog("Jhttpp2Admin Exception: " + e.getMessage());
    }
    int adminlen = adminpage.length();
    if ( adminlen < 1 ) {
      sendErrorMSG(500,"Error Message from the Web-Admin modul: " + admin.error_msg);
    }
    else {
      sendHeader(200,"text/html",adminlen);
      endHeader();
      write(out,adminpage);
    }
    out.flush();
  }*/
}

