/*  OutputHandler.java -- all functions related to output
 
    Copyright (C) 2000 - 2001 Jan De Luyck & Kris Van Hulle
 
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.
 
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
 
    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.server;

import java.net.*;
import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.logging.LogRecord;

import HTML.Template;

import org.gudy.azureus2.core.ConfigurationManager;
import org.gudy.azureus2.core.GlobalManager;
import org.gudy.azureus2.core.DownloadManager;
import org.gudy.azureus2.core.MessageText;


public class OutputHandler {
  private Socket sock;
  private BufferedOutputStream out;
  //    private PrintStream out;
  
  private Writer outStr;
  //    private PrintWriter outStr;
  
  private Map httpHeaders;
  
  private boolean sendHeaders = true;
  private boolean sendBody = true;
  
  private MainServer server;
  private GlobalManager gm;
  
  private static Locale locale = new Locale("", "");
  private static Hashtable status = null;
  
  public OutputHandler(Socket o_sock, GlobalManager _gm, MainServer _server) {
    try {
      this.sock = o_sock;
      this.server = _server;
      this.gm = _gm;
      this.out = new BufferedOutputStream(this.sock.getOutputStream());
      //            this.out = new PrintStream(this.sock.getOutputStream());
      this.outStr = new OutputStreamWriter(this.out);
      //            this.outStr = new PrintWriter(this.out, true);
      if ((this.status == null) | (locale != this.server.locale)) {
        if (status != null)
          status.clear();
        else
          status = new Hashtable();
        this.status.put(new Integer(DownloadManager.STATE_WAITING), MessageText.getString("Main.download.state.waiting"));
        this.status.put(new Integer(DownloadManager.STATE_INITIALIZING), MessageText.getString("Main.download.state.waiting"));
        this.status.put(new Integer(DownloadManager.STATE_INITIALIZED), MessageText.getString("Main.download.state.waiting"));
        this.status.put(new Integer(DownloadManager.STATE_ALLOCATING), MessageText.getString("Main.download.state.allocating"));
        this.status.put(new Integer(DownloadManager.STATE_CHECKING), MessageText.getString("Main.download.state.checking"));
        this.status.put(new Integer(DownloadManager.STATE_READY), MessageText.getString("Main.download.state.ready"));
        this.status.put(new Integer(DownloadManager.STATE_DOWNLOADING), MessageText.getString("Main.download.state.downloading"));
        this.status.put(new Integer(DownloadManager.STATE_WAITING), MessageText.getString("Main.download.state.waiting"));
        this.status.put(new Integer(DownloadManager.STATE_SEEDING), MessageText.getString("Main.download.state.seeding"));
        this.status.put(new Integer(DownloadManager.STATE_STOPPING), MessageText.getString("Main.download.state.stopped"));
        this.status.put(new Integer(DownloadManager.STATE_STOPPED), MessageText.getString("Main.download.state.stopped"));
        this.status.put(new Integer(DownloadManager.STATE_ERROR), MessageText.getString("Main.download.state.error"));
        this.status.put(new Integer(DownloadManager.STATE_DUPLICATE), "Duplicate");
        
      }
    }
    catch (IOException e) {
      this.server.putSysMessage(SLevel.WARNING, "IO Exception caught: " + e);
    }
    
  }
  
  public void setHttpHeaders(Map httpHeaders) {
    this.httpHeaders = httpHeaders;
  }
  
  public void sendHeaders(boolean value) {
    this.sendHeaders = value;
  }
  
  public void sendBody(boolean value) {
    this.sendBody = value;
  }
  
  public void close() {
    try {
      this.out.close();
      this.outStr.close();
    }
    catch (IOException e) {
      this.server.putSysMessage(SLevel.WARNING,"InputHandler: " + e);
    }
  }
  
  private void outputStatusLine(int code) {
        /* 	sends a 'statusline' to the client.
                Lines typically are HTTP/1.1 200 OK
                                                HTTP/1.1 404 Not Found
                                                        etc
         */
    String statusMessage;
    switch (code) {
      case 100: statusMessage = "Continue"; break;
      case 101: statusMessage = "Switching Protocols"; break;
      
      case 200: statusMessage = "OK"; break;
      case 201: statusMessage = "Created"; break;
      case 202: statusMessage = "Accepted"; break;
      case 203: statusMessage = "Non-Authoritative Information"; break;
      case 204: statusMessage = "No Content"; break;
      case 205: statusMessage = "Reset Content"; break;
      case 206: statusMessage = "Partial Content"; break;
      
      case 300: statusMessage = "Multiple Choices"; break;
      case 301: statusMessage = "Moved Permanently"; break;
      case 302: statusMessage = "Moved Temporarily"; break;
      case 303: statusMessage = "See Other"; break;
      case 304: statusMessage = "Not Modified"; break;
      case 305: statusMessage = "Use Proxy"; break;
      case 307: statusMessage = "Temporary Redirect"; break;
      
      case 400: statusMessage = "Bad Request"; break;
      case 401: statusMessage = "Unauthorized"; break;
      case 402: statusMessage = "Payment Required"; break;
      case 403: statusMessage = "Forbidden"; break;
      case 404: statusMessage = "Not Found"; break;
      case 405: statusMessage = "Method Not Allowed"; break;
      case 406: statusMessage = "Not Acceptable"; break;
      case 407: statusMessage = "Proxy Authentication Required"; break;
      case 408: statusMessage = "Request Time-out"; break;
      case 409: statusMessage = "Conflict"; break;
      case 410: statusMessage = "Gone"; break;
      case 411: statusMessage = "Length Required"; break;
      case 412: statusMessage = "Precondition Failed"; break;
      case 413: statusMessage = "Request Entity Too Large"; break;
      case 414: statusMessage = "Request-URI Too Large"; break;
      case 415: statusMessage = "Unsupported Media Type"; break;
      case 416: statusMessage = "Requested range not satisfiable"; break;
      case 417: statusMessage = "Expectation Failed"; break;
      
      case 500: statusMessage = "Internal Server Error"; break;
      case 501: statusMessage = "Not Implemented"; break;
      case 502: statusMessage = "Bad Gateway"; break;
      case 503: statusMessage = "Service Unavailable"; break;
      case 504: statusMessage = "Gateway Time-out"; break;
      case 505: statusMessage = "HTTP Version not supported"; break;
      
      default: statusMessage = "An unknown/undefined HTTP code!"; break;
      
    }
    
    /* this actually outputs the status line */
    this.outputHeader("HTTP/1.1 " + code + " " + statusMessage);
  }
  
  private void outputConnectionHeader() {
    if (this.httpHeaders.get("connection").toString().startsWith("keep-alive"))
      this.outputHeader("Connection: keep-alive");
    else
      this.outputHeader("Connection: close");
  }
  
  private void outputSomething(String someText) {
    /* outputs something to the socket, can be nearly anything, but no binary data */
    try {
      this.outStr.write(someText + this.server.crlf());
    }
    catch (IOException e) {
      this.server.putSysMessage(SLevel.WARNING,"Error while outputting: " + someText + " (" + e + ")");
    }
  }
  
  private void outputHeader(String aHeader) {
        /* outputs header data to the socket. If this.sendHeaders == false, it doesn't.
           (for compatibility with old (e.g. http/0.9) browsers
           This function uses outputSomething because it's basically the same.*/
    
    if (this.sendHeaders == true)
      this.outputSomething(aHeader);
  }
  
  private void outputStdHeaders() {
        /*	sends the standard additional headers to the socket
            these can normally always be trusted
         */
    SimpleDateFormat temp = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
    temp.setTimeZone(new SimpleTimeZone(0,"GMT"));
    String dateStr = temp.format(new Date());
    
    this.outputHeader("Date: " + dateStr);
    this.outputHeader("Server: " + this.server.serverName() + "/" + this.server.serverVersion());
  }
  
  public void outputError(int code, String value) {
    /*	outputs standard errors, including status lines, and body text. */
    String statusExplanation;
    
    statusExplanation = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\"><HTML><HEAD><TITLE>Error " + code + "</TITLE></HEAD><BODY>";
    
    switch (code) {
      case 404: statusExplanation += "<H1>Not Found</H1>The requested URL " + value + " was not found on this server."; break;
      case 400: statusExplanation += "<H1>Bad Request</H1>Your browser sent a request that this server could not understand.<P>Client sent HTTP/1.1 request without hostname (see RFC2068 section 9, and 14.23)"; break;
      case 501: statusExplanation += "<H1>Method Not Implemented</H1>" + value + " not supported.<P>Invalid method in request " + value + "."; break;
      case 505: statusExplanation += "<H1>HTTP Version Not Supported</H1>Your browser uses an HTTP version this server cannot serve. Please upgrade your browser to a version that uses at least HTTP/1.0."; break;
      case 601: statusExplanation += "<H1>Protocol Not Implemented</H1>Your browser sent a request using a protocol that is not implemented. <P>Client used protocol " + value + "."; break;
    }
    
    statusExplanation += giveOutputFooter() + "</BODY></HTML>";
    
    this.outputStatusLine(code);
    this.outputStdHeaders();
    
    /* for 'Last-Modified header field' */
    SimpleDateFormat temp = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
    temp.setTimeZone(new SimpleTimeZone(0,"GMT"));
    String dateStr = temp.format(new Date());
    
    this.outputHeader("Last-Modified: " + dateStr);
    this.outputHeader("Content-type: text/html");
    this.outputHeader("Content-length: " + statusExplanation.length());
    this.outputConnectionHeader();
    
    this.outputHeader("");	/* an empty line is required */
    if (this.sendBody == true)
      this.outputSomething(statusExplanation);
    this.outputFlush();
  }
  
  
  private void outputFlush() {
        /*	flushes the output stream, to make sure everything gets to the elepha... erm client
            (where did that elephant come from? */
    
    try {
      this.out.flush();
      this.outStr.flush();
      //            this.out.flush();
    }
    catch (IOException e) {
      this.server.putSysMessage(SLevel.WARNING,"Error while flushing output buffer: "+ e);
    }
  }
  
  private void ProcessConfigVars(HashMap URIvars) {
    Set keys = URIvars.keySet();
    Iterator key = keys.iterator();
    ConfigurationManager cm = ConfigurationManager.getInstance();
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
            server.putSysMessage(SLevel.SEVERE, "Configuration error. This is not a directory: "+value);
            continue;
          }
        }
        if (option.substring(option.indexOf('_')+1).startsWith("s"))
          cm.setParameter(option, value);
        else
          cm.setParameter(option, Integer.parseInt(value));
      }
    }
    cm.save();
  }
  
  private void ProcessAdd(HashMap URIvars) {
    if (URIvars.containsKey("Add_torrent") && !((String) URIvars.get("Add_torrent")).equals("")) {
      try {
        HTTPDownloader dl = new HTTPDownloader((String) URIvars.get("Add_torrent"), ConfigurationManager.getInstance().getDirectoryParameter("General_sDefaultTorrent_Directory"));
        String file = dl.download();
        gm.addDownloadManager(file, ConfigurationManager.getInstance().getDirectoryParameter("General_sDefaultSave_Directory"));
        this.server.putSysMessage(SLevel.INFO, "Download of "+(String)URIvars.get("Add_torrent")+" succeeded");
      } catch (Exception e) {
        this.server.putSysMessage(SLevel.SEVERE, "Download of "+(String)URIvars.get("Add_torrent")+" failed: "+e.getMessage());
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
    }
  }
  
  private void handleConfigInt(Template tmpl, String name) {
    tmpl.setParam("Options_"+name, ConfigurationManager.getInstance().getIntParameter(name));
  }
  
  private void handleConfigStr(Template tmpl, String name) {
    tmpl.setParam("Options_"+name, ConfigurationManager.getInstance().getStringParameter(name));
  }
  
  private void handleConfig(Template tmpl) {
    ConfigurationManager config = ConfigurationManager.getInstance();
    handleConfigStr(tmpl, "General_sDefaultSave_Directory");
    handleConfigStr(tmpl, "General_sDefaultTorrent_Directory");
    handleConfigStr(tmpl, "Server_sTemplate_Directory");
    if (config.getIntParameter("Server_iVerbosity") != this.server.verbosity())
      tmpl.setParam("Override_Verbosity", "Verbosity overridden via command line to "+this.server.verbosity());
    handleConfigInt(tmpl, "Server_iVerbosity");
    handleConfigStr(tmpl, "Server_sBindIP");
    handleConfigInt(tmpl, "Server_iPort");
    handleConfigStr(tmpl, "Server_sName");
    handleConfigInt(tmpl, "Server_iTimeout");
    handleConfigInt(tmpl, "Server_iMaxHTTPConnections");
    handleConfigInt(tmpl, "Server_iLogCount");
  }
  
  private void handleTorrents(Template tmpl) {
    List torrents = this.gm.getDownloadManagers();
    DownloadManager dm;
    int dmstate;
    Hashtable h;
    if (!torrents.isEmpty()) {
      Vector v = new Vector();
      Iterator torrent = torrents.iterator();
      while (torrent.hasNext()) {
        dm = (DownloadManager) torrent.next();
        dmstate = dm.getState();
        h = new Hashtable();
        if (dmstate == DownloadManager.STATE_STOPPED) {
          h.put("Torrents_Torrent_Command", "Resume");
          h.put("Torrents_Torrent_Stopped", Boolean.TRUE);
        } else
          h.put("Torrents_Torrent_Command", "Stop");
        if (dm.getNbSeeds() == 0)
          h.put("Torrents_Torrent_Seedless", Boolean.TRUE);
        if (dmstate == DownloadManager.STATE_INITIALIZING)
          h.put("Torrents_Torrent_Initializing", Boolean.TRUE);
        else if (dmstate == DownloadManager.STATE_ALLOCATING)
          h.put("Torrents_Torrent_Allocating", Boolean.TRUE);
        else if (dmstate == DownloadManager.STATE_CHECKING)
          h.put("Torrents_Torrent_Checking", Boolean.TRUE);
        h.put("Torrents_Torrent_PercentDone", Integer.toString(dm.getCompleted()/10));
        h.put("Torrents_Torrent_PercentLeft", Integer.toString((1000-dm.getCompleted())/10));
        h.put("Torrents_Torrent_PercentDonePrec", Long.toString(((long) dm.getCompleted())/10));
        h.put("Torrents_Torrent_PercentLeftPrec", Long.toString((1000- (long) dm.getCompleted())/10));
        h.put("Torrents_Torrent_SpeedDown", dm.getDownloadSpeed());
        h.put("Torrents_Torrent_SpeedUp", dm.getUploadSpeed());
        h.put("Torrents_Torrent_FileSize", Long.toString(dm.getSize()));
        h.put("Torrents_Torrent_FileSizeDone", Long.toString(((long) dm.getCompleted())*((long) dm.getSize())/1000));
        h.put("Torrents_Torrent_FileName", dm.getName());
        h.put("Torrents_Torrent_Status", this.status.get(new Integer(dmstate)));
        h.put("Torrents_Torrent_Seeds", Integer.toString(dm.getNbSeeds()));
        h.put("Torrents_Torrent_Peers", Integer.toString(dm.getNbPeers()));
        h.put("Torrents_Torrent_ETA", (dm.getETA()=="")?"&nbsp;":dm.getETA());
        v.addElement(h);
      }
      tmpl.setParam("Torrents_Torrents", v);
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
        LogRecord rec = (LogRecord) it.previous();
        h = new Hashtable();
        h.put("Log_Logs_TimeCode", temp.format(new Date(rec.getMillis())));
        h.put("Log_Logs_Message", rec.getLevel().getName() + " -- " + rec.getMessage());
        if (rec.getLevel().equals(SLevel.SEVERE))
          h.put("Log_Logs_LevelError", Boolean.TRUE);
        v.addElement(h);
      }
      tmpl.setParam("Log_Logs", v);
    }
  }
  
  public void ProcessAndOutput(String path, String httpMethod, HashMap httpURIVars,
  InetAddress remoteIP) {
    this.outputStatusLine(200);
    this.outputStdHeaders();
    /* for 'Last-Modified header field' */
    //        SimpleDateFormat temp = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
    //        temp.setTimeZone(new SimpleTimeZone(0,"GMT"));
    //        String dateStr = temp.format(new Date(fileToRead.lastModified()));
    //
    //        this.outputHeader("Last-Modified: " + dateStr);
    this.outputHeader("Content-type: text/html");
    //        this.outputHeader("Content-length: " + fileToRead.length());
    this.outputConnectionHeader();
    
    this.outputHeader("");	/* an empty line is required */
    this.outputFlush();
    
    if (this.sendBody == true) {
      String req=path.substring(1);
      StringTokenizer tok = new StringTokenizer(req, "?");
      if (tok.hasMoreTokens())
        req = tok.nextToken();
      if (req.equals(""))
        req = "root";
      Template tmpl = (Template) this.server.htmlTemplates.get(req);
      tmpl.clearParams();
      tmpl.setParam("Global_ServerName", this.server.serverName());
      this.handleConfig(tmpl);
      this.handleTorrents(tmpl);
      this.handleLog(tmpl);
      try {
        this.outStr.write(tmpl.output());
        //                this.outStr.println(tmpl.output());
        //                tmpl.printTo(this.outStr);
      }
      catch (IOException e) {}
      this.outputFlush();
      if (req.equals("exit"))
        this.server.quitServer();
    }
  }
  
  public void ProcessAndOutputFile(File path, String httpMethod, HashMap httpURIVars,
  InetAddress remoteIP) {
    File fileToRead = path;
    DataInputStream fileIn = null;
    
    //        String actionHandler = this.server.getActionHandler(fileToRead.getPath());
    
    try {
      //            if (actionHandler != null) {
      //                /* perform preprocessing --> start a new command interpreter, run the command.
      //                        i'm not sure if i can actually 'CHECK' on this, see if it is available. I have no
      //                        f$cking idea how to do it*/
      //                this.server.putSysMessage(0, "Preprocessing...");
      //                /* this code doesn't work at all. Removing thus until i can get a way to fix it :-( */
      //                /*Process Handler = CGI.runCGI(actionHandler, fileToRead.getPath(), httpURIVars, remoteIP.getHostAddress().toString(), remoteIP.getHostName().toString(), httpMethod, sock.getLocalAddress().getHostName());*/
      //
      //                String command = "";
      //                /* if there are spaces in the actionHandler, put " around it */
      //                if (actionHandler.indexOf(' ') != -1)
      //                    command += "\"" + actionHandler + "\"";
      //                else
      //                    command += actionHandler;
      //
      //                command += " " + fileToRead.getPath().replace('\\','/');
      //
      //                this.server.putSysMessage(0, "Executing: " + command);
      //
      //                Process Handler = Runtime.getRuntime().exec(command);
      //                if (Handler != null)
      //                    fileIn = new DataInputStream(Handler.getInputStream());
      //
      //                /* do header stuff */
      //                this.outputStatusLine(200);
      //                this.outputStdHeaders();
      //                this.outputConnectionHeader();
      //                this.outputFlush();
      //            }
      //            else {
      /* assign file, and send standard headers to output */
      fileIn = new DataInputStream(new BufferedInputStream(new FileInputStream(fileToRead)));
      
      /* output headers for this file */
      
      this.outputStatusLine(200);
      this.outputStdHeaders();
      /* for 'Last-Modified header field' */
      SimpleDateFormat temp = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
      temp.setTimeZone(new SimpleTimeZone(0,"GMT"));
      String dateStr = temp.format(new Date(fileToRead.lastModified()));
      
      this.outputHeader("Last-Modified: " + dateStr);
      this.outputHeader("Content-type: " + this.server.getContentType(fileToRead.getPath()));
      this.outputHeader("Content-length: " + fileToRead.length());
      this.outputConnectionHeader();
      
      this.outputHeader("");	/* an empty line is required */
      this.outputFlush();
      //            }
      
      if (this.sendBody == true) {
        //                if (actionHandler == null) {
        /* process this thing as a file */
        byte[] buffer = new byte[(int) fileToRead.length()];
        fileIn.readFully(buffer);
        this.out.write(buffer);
        this.outputFlush();
        //                }
        //                else {
        //                    /* process this thing as a stream */
        //                    byte[] buffer = new byte[2];
        //                    int status = 0;
        //                    while (status != -1) {
        //                        status = fileIn.read(buffer);
        //                        this.out.write(buffer);
        //                    }
        //                }
      }
    }
    catch (FileNotFoundException e) {
      this.server.putSysMessage(SLevel.WARNING, "FileRead: " +e);
    }
    catch (IOException e) {
      this.server.putSysMessage(SLevel.WARNING, "FileRead: " +e);
    }
    finally {
      try {
        if ( fileIn != null)  /* check if the file has been opened at all */
          fileIn.close();
      }
      catch (IOException e) {
        this.server.putSysMessage(SLevel.WARNING, "FileRead: " +e);
      }
      this.outputFlush();
    }
  }
  
  private String giveOutputFooter() {
    /*	returns the 'footer' for several messages, containing servername/version, ip, port... */
    boolean doOutput = false;
    String startOfFooter = "<P><HR><ADDRESS>" + this.server.serverName() + " v" + this.server.serverVersion() + " at ";
    String endOfFooter = " Port " + sock.getLocalPort() + "</ADDRESS>";
    String midOfFooter = "";
    
        /* if serversignature == email, use a mailto: string
                              == on, just put it there
                              == off, don't output anything */
    if (ConfigurationManager.getInstance().getStringParameter("Server_sSignature").equalsIgnoreCase("on")  == true) {
      doOutput = true;
      midOfFooter = ConfigurationManager.getInstance().getStringParameter("Server_sName");
    }
    else {
      if (ConfigurationManager.getInstance().getStringParameter("Server_sSignature").equalsIgnoreCase("email")  == true) {
        doOutput = true;
        midOfFooter = " <a href=\"mailto:" + ConfigurationManager.getInstance().getStringParameter("Server_sAdmin") + "\">" + ConfigurationManager.getInstance().getStringParameter("Server_sName") + "</a>";
      }
    }
    
    if (doOutput == true)
      return startOfFooter + midOfFooter + endOfFooter;
    else
      return "";
  }
  
  //    public void outputDirectoryListing(String httpURIPath) {
  //        String dirListing = this.generateDirectoryListing(httpURIPath);
  //
  //        /* output everything */
  //        this.outputStatusLine(200);
  //        this.outputStdHeaders();
  //        SimpleDateFormat temp = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
  //        temp.setTimeZone(new SimpleTimeZone(0,"GMT"));
  //        String dateStr = temp.format(new Date());
  //
  //        this.outputHeader("Last-Modified: " + dateStr);
  //        this.outputHeader("Content-type: text/html");
  //        this.outputHeader("Content-length: " + dirListing.length());
  //
  //        this.outputConnectionHeader();
  //
  //        this.outputHeader("");	/* an empty line is required */
  //
  //        if (this.sendBody == true)
  //            this.outputSomething(dirListing);
  //
  //        this.outputFlush();
  //    }
  
  
  //    private String generateDirectoryListing(String httpURIPath)
  //    /* This function creates a DirListing, and returns it in a String */
  //    {
  //        String dirListing = "";
  //        File path = new File(MainServer.getSetting("documentroot") + httpURIPath);
  //
  //        File[] Listing = path.listFiles();
  //
  //        if (Listing != null) {
  //            /*'head' the string */
  //            dirListing = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\">";
  //            dirListing += "<HTML><HEAD><TITLE>Index of " + httpURIPath +"</TITLE></HEAD><BODY><H1>Index of " + httpURIPath + "</H1><P><TABLE BORDER=\"0\" CELLPADDING=\"4\" WIDTH=\"100%\">";
  //            dirListing += "<TR><TD>Type</TD><TD>File Name</TD><TD>File Size</TD><TD>Last Modified</TD><TD></TD></TR>";
  //            dirListing += "<TR><TD COLSPAN=\"5\"><HR></TD>";
  //
  //            /* add an entry to go back 1 dir (to the parent dir) */
  //            if (path.getPath().replace('\\','/').equals(MainServer.getSetting("documentroot")) == false) {
  //                String filePath = path.getPath().substring(MainServer.getSetting("documentroot").length(),path.getPath().length() - path.getName().length()).replace('\\', '/');
  //
  //                StringTokenizer filePart = new StringTokenizer(filePath, "/");
  //                String piece = "";
  //                String EncodedfilePath = "";
  //                while (filePart.hasMoreTokens() == true) {
  //                    EncodedfilePath += "/";
  //                    piece = filePart.nextToken();
  //                    EncodedfilePath += URLEncoder.encode(piece, "US-ASCII");
  //                }
  //
  //                dirListing += "<TR><TD COLSPAN=\"4\"><a href=\"http://" + MainServer.getSetting("servername") + ":" + MainServer.getSetting("port");
  //                dirListing += EncodedfilePath + "\">Parent Directory</a></TD></TR>" + MainServer.crlf();
  //            }
  //
  //            for (int i = 0; i < Listing.length; i++) {
  //                dirListing += "<TR><TD>";
  //                if (Listing[i].isDirectory() == true)
  //                    dirListing += "[DIR]";
  //                else
  //                    dirListing += "[FILE]";
  //
  //                SimpleDateFormat temp = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss zzz");
  //                temp.setTimeZone(new SimpleTimeZone(0,"GMT"));
  //                String S_lastModified = temp.format(new Date(Listing[i].lastModified()));
  //
  //                String filePath = Listing[i].getPath().substring(MainServer.getSetting("documentroot").length() + 1).replace('\\', '/');
  //
  //                // we need to split the complete string into parts, based on '/'
  //                StringTokenizer filePart = new StringTokenizer(filePath, "/");
  //                String piece = "";
  //                String EncodedfilePath = "";
  //                while (filePart.hasMoreTokens() == true) {
  //                    EncodedfilePath += "/";
  //                    piece = filePart.nextToken();
  //                    EncodedfilePath += URLEncoder.encode(piece, "US-ASCII");
  //                }
  //
  //                dirListing += "</TD><TD><a href=\"http://" + MainServer.getSetting("servername") + ":" + MainServer.getSetting("port");
  //
  //                dirListing += EncodedfilePath + "\">" + Listing[i].getName() + "</a></TD><TD>" + Listing[i].length() + " Bytes</TD><TD>" + S_lastModified + "</TD></TR>" + MainServer.crlf();
  //            }
  //            /*'foot' the string */
  //            dirListing += "</TABLE>" + this.giveOutputFooter() + "</BODY></HTML>";
  //        }
  //        return dirListing;
  //    }
  
  public void outputTrace(String httpURI) {
    this.outputStatusLine(200);
    this.outputStdHeaders();
    
    SimpleDateFormat temp = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
    temp.setTimeZone(new SimpleTimeZone(0,"GMT"));
    String dateStr = temp.format(new Date());
    
    this.outputHeader("Last-Modified: " + dateStr);
    this.outputHeader("Content-type: text/html");
    this.outputHeader("");	/* an empty line is required */
    
    // we need to define an iterator to go through the map
    Iterator mapIterator = this.httpHeaders.keySet().iterator();
    String aHeader, aValue;
    
    while (mapIterator.hasNext()) {
      aHeader = (String) mapIterator.next();
      aValue = (String) httpHeaders.get(aHeader);
      this.outputSomething(aHeader + ": " + aValue);
    }
    
    this.outputFlush();
  }
}