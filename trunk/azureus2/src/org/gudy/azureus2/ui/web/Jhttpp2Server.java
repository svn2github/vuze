/* Written and copyright 2001-2003 Benjamin Kohl.
 * Distributed under the GNU General Public License; see the README file.
 * This code comes with NO WARRANTY.
 * More Information and documentation: HTTP://jhttp2.sourceforge.net/
 */
package org.gudy.azureus2.ui.web;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.BindException;

import java.io.*;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;

import org.gudy.azureus2.core.GlobalManager;
import org.gudy.azureus2.core.ILoggerListener;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.util.Constants;

public class Jhttpp2Server implements Runnable, ILoggerListener {
  private static final String CRLF="\r\n";
  private final String VERSION = "0.1";
  private final String VERSION_JHTTPP2 = "0.4.62";
  private final String V_SPECIAL = " 2003-05-20";
  private final String HTTP_VERSION = "HTTP/1.1";
  
  private final String MAIN_LOGFILE = "server.log";
  private final String DATA_FILE = "server.data";
  private final String SERVER_PROPERTIES_FILE = "server.properties";
  
  //private String http_useragent = "Mozilla/4.0 (compatible; MSIE 4.0; WindowsNT 5.0)";
  private ServerSocket listen;
  /*private BufferedWriter logfile;
  private BufferedWriter access_logfile;
  private Properties serverproperties = null;*/
  
  private long bytesread;
  private long byteswritten;
  public int numconnections;
  
  //private boolean enable_cookies_by_default=true;
  private WildcardDictionary dic = new WildcardDictionary();
  private Vector urlactions = new Vector();
  
  //public final int DEFAULT_SERVER_PORT = 8088;
  public final String WEB_CONFIG_FILE = "admin/jp2-config";
  
  //public int port = DEFAULT_SERVER_PORT;
  //public InetAddress proxy;
  //public int proxy_port = 0;
  
  public long config_auth = 0;
  public long config_session_id = 0;
  //public String config_user = "root";
  //public String config_password = "geheim";
  
  //public static boolean error;
  //public static String error_msg;
  
  //public boolean use_proxy=false;
  //public static boolean block_urls=false;
  //public boolean filter_http=false;
  //public boolean debug=false;
  //public boolean log_access = false;
  //public String log_access_filename="paccess.log";
  //public boolean webconfig = true;
  //public boolean www_server = true;
  
  public GlobalManager gm;
  public Locale locale = new Locale("", "");
  public Date startTime = new Date();
  public Logger loggerWeb = Logger.getLogger("azureus2.webinterface");
  public Logger loggerCore = Logger.getLogger("azureus2.core");
  public List logList = new LinkedList();
  private List allowedIPs;
  private List staticIPs;
  private List dynamicHosts;
  private List dynamicHostResolvers = null;
  private Date dynamicHostUpdate;
  
  void init(GlobalManager _gm) {
    
    gm = _gm;
    initLoggers();
    initAccess();
    /*
    try {
      logfile=new BufferedWriter(new FileWriter(MAIN_LOGFILE,true));
    }
    catch (Exception e_logfile) {
      loggerWeb.error("Unable to open the main log file.", e_logfile);
      if (logfile==null)
        loggerWeb.error("jHTTPp2 need write permission for the file " + MAIN_LOGFILE);
    }*/
    loggerWeb.info("server startup...");
    
    try {
      restoreSettings();
    } catch (Exception e_load) {
      loggerWeb.error("Error while resoring settings", e_load);
    }
    try {
      if (COConfigurationManager.getStringParameter("Server_sBindIP").equals(""))
        listen = new ServerSocket(COConfigurationManager.getIntParameter("Server_iPort"));
      else
        listen = new ServerSocket(COConfigurationManager.getIntParameter("Server_iPort"),0,InetAddress.getByName(COConfigurationManager.getStringParameter("Server_sBindIP")));
    } catch (BindException e_bind_socket) {
      loggerWeb.fatal("Socket " + COConfigurationManager.getIntParameter("Server_iPort") + " is already in use (Another jHTTPp2 proxy running?)", e_bind_socket);
      shutdownServer();
    }
    catch (IOException e_io_socket) {
      loggerWeb.fatal("IO Exception while creating server socket on port " + COConfigurationManager.getIntParameter("Server_iPort") + ".", e_io_socket);
      shutdownServer();
    }
    
/*    if (error) {
      writeLog(error_msg);
      return;
    }*/
    //if (debug) remote_debug_vector=new Vector();
    //remote_debug=false;
  }
  public void initLoggers() {
    Logger.getRootLogger().removeAllAppenders();
    //BasicConfigurator.configure();
    Appender app;
    app = new ConsoleAppender(new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN));
    app.setName("ConsoleAppender");
    Logger.getRootLogger().addAppender(app);
    app = new WebLogAppender(logList);
    app.setName("WebLogAppender");
    Logger.getRootLogger().addAppender(app);
    if (COConfigurationManager.getBooleanParameter("Server_bLogFile")) {
      try{
        app = new FileAppender(new PatternLayout(), COConfigurationManager.getStringParameter("Server_sLogFile"),true);
        app.setName("LogFileAppender");
        Logger.getRootLogger().addAppender(app);
      }catch (Exception e){}
    }
    org.gudy.azureus2.core.Logger.getLogger().setListener(this);
    loggerCore.setLevel(SLevel.toLevel(COConfigurationManager.getIntParameter("Server_iLogLevelCore")));
    loggerWeb.setLevel(SLevel.toLevel(COConfigurationManager.getIntParameter("Server_iLogLevelWebinterface")));
  }
  
  public void initAccess() {
    staticIPs = new LinkedList();
    String theip = "";
    StringTokenizer tok = new StringTokenizer(COConfigurationManager.getStringParameter("Server_sAllowStatic"), " ");
    while (tok.hasMoreTokens()) {
      try {
        theip = tok.nextToken();
        staticIPs.add(InetAddress.getByName(theip));
      } catch (Exception e) {
        loggerWeb.error("Host "+theip+" not found while updating allowed static hosts.", e);
      }
    }
    tok = new StringTokenizer(COConfigurationManager.getStringParameter("Server_sAllowDynamic"), " ");
    if (tok.hasMoreTokens()) {
      dynamicHosts = new LinkedList();
      while (tok.hasMoreTokens()) {
        dynamicHosts.add(tok.nextToken());
      }
    } else
      dynamicHosts = null;
    rebuildAccess();
  }
  
  private void rebuildAccess() {
    allowedIPs = (LinkedList) ((LinkedList)staticIPs).clone();
    Iterator it;
    if (dynamicHostResolvers != null) {
      it = dynamicHostResolvers.iterator();
      while (it.hasNext()) {
        HostResolver res = (HostResolver) it.next();
        if (res.isAlive())
          res.interrupt();
      }
    }
    
    if (dynamicHosts != null) {
      dynamicHostResolvers = new LinkedList();
      it = dynamicHosts.iterator();
      while (it.hasNext()) {
        dynamicHostResolvers.add(new HostResolver(this, allowedIPs, (String) it.next()));
      }
    }
    dynamicHostUpdate = new Date();
  }
  
  public Jhttpp2Server(GlobalManager _gm) {
    init(_gm);
  }
  public Jhttpp2Server(GlobalManager _gm, boolean b) {
    System.out.println("Azureus "+Constants.AZUREUS_VERSION+" WebInterface\r\n"
    +"Copyright (c) 2001-2003 by the Azureus Developer Team\r\n"
    +"This software comes with ABSOLUTELY NO WARRANTY OF ANY KIND.\r\n"
    +"http://azureus.sourceforge.net/\r\n\r\n"
    +"The webinterface is based upon\r\n"
    +"jHTTPp2 HTTP Proxy Server Release " + getServerJhttp2Version() + "\r\n"
    +"Copyright (c) 2001-2003 Benjamin Kohl <bkohl@users.sourceforge.net>\r\n"
    +"This software comes with ABSOLUTELY NO WARRANTY OF ANY KIND.\r\n"
    +"http://jhttp2.sourceforge.net/");
    System.out.println();
    init(_gm);
  }
  /** calls init(), sets up the serverport and starts for each connection
   * new Jhttpp2Connection
   */
  void serve() {
    loggerWeb.info("Server running.");
    try {
      while(true) {
        Socket client = listen.accept();
        
        if ((dynamicHosts != null) && ((new Date()).getTime() > (dynamicHostUpdate.getTime()+(COConfigurationManager.getIntParameter("Server_iRecheckDynamic")*60000)))) {
          rebuildAccess();
        }
        
        if (allowedIPs.contains(client.getInetAddress()))
          new Jhttpp2HTTPSession(this,client);
        else
          try {
            loggerWeb.log(SLevel.ACCESS_VIOLATION, "Denied access for host "+client.getInetAddress().toString());
            client.close();
          } catch (Exception e) {}
      }
    }
    catch (Exception e) {
      loggerWeb.fatal("Exception in Jhttpp2Server.serve()", e);
    }
  }
  public void run() {
    serve();
  }
/*  public void setErrorMsg(String a) {
    error=true;
    error_msg=a;
  }*/
  /**
   * Tests what method is used with the reqest
   * @return -1 if the server doesn't support the method
   */
  public int getHttpMethod(String d) {
    if (startsWith(d,"GET")  || startsWith(d,"HEAD")) return 0;
    if (startsWith(d,"POST") || startsWith(d,"PUT")) return 1;
    if (startsWith(d,"CONNECT")) return 2;
    if (startsWith(d,"OPTIONS")) return 3;
    
    return -1;/* No match...
               
    Following methods are not implemented:
    || startsWith(d,"TRACE") */
  }
  public boolean startsWith(String a,String what) {
    int l=what.length();
    int l2=a.length();
    return l2>=l?a.substring(0,l).equals(what):false;
  }
  /**
   *@return the Server response-header field
   */
  public String getServerIdentification() {
    return "jHTTPp2/" + getServerVersion();
  }
  public String getServerJhttp2Version() {
    return VERSION_JHTTPP2 + V_SPECIAL;
  }
  public String getServerVersion() {
    return VERSION;
  }
  /**
   *  saves all settings with a ObjectOutputStream into a file
   * @since 0.2.10
   */
  public void saveSettings()throws IOException {
    //    serverproperties.setProperty("server.http-proxy",new Boolean(use_proxy).toString());
    //    serverproperties.setProperty("server.http-proxy.hostname",proxy.getHostAddress());
    //    serverproperties.setProperty("server.http-proxy.port",new Integer(proxy_port).toString());
    //    serverproperties.setProperty("server.filter.http",new Boolean(filter_http).toString());
    //    serverproperties.setProperty("server.filter.url",new Boolean(block_urls).toString());
    //    serverproperties.setProperty("server.filter.http.useragent",http_useragent);
    //    serverproperties.setProperty("server.enable-cookies-by-default",new Boolean(enable_cookies_by_default).toString());
    //    serverproperties.setProperty("server.debug-logging",new Boolean(debug).toString());
    //    serverproperties.setProperty("server.port",new Integer(port).toString());
    //    serverproperties.setProperty("server.access.log",new Boolean(log_access).toString());
    //    serverproperties.setProperty("server.access.log.filename",log_access_filename);
    //    serverproperties.setProperty("server.webconfig",new Boolean(webconfig).toString());
    //    serverproperties.setProperty("server.www",new Boolean(www_server).toString());
    //    serverproperties.setProperty("server.webconfig.username",config_user);
    //    serverproperties.setProperty("server.webconfig.password",config_password);
    //storeServerProperties();
    /*
    ObjectOutputStream file=new ObjectOutputStream(new FileOutputStream(DATA_FILE));
    file.writeObject(dic);
    file.writeObject(urlactions);
    file.close();*/
  }
  /** restores all Jhttpp2 options from "settings.dat"
   * @since 0.2.10
   */
  public void restoreSettings()//throws Exception
  {
    //getServerProperties();
    //    use_proxy = new Boolean(serverproperties.getProperty("server.http-proxy","false")).booleanValue();
    //    try { proxy = InetAddress.getByName(ConfigurationManager.getInstance().getStringParameter("Server_sDownstreamProxyHost"));
    //    } catch ( UnknownHostException e) {}
    //    proxy_port = new Integer(serverproperties.getProperty("server.http-proxy.port","8080")).intValue();
    //    block_urls = new Boolean(serverproperties.getProperty("server.filter.url","false")).booleanValue();
    //    http_useragent = serverproperties.getProperty("server.filter.http.useragent","Mozilla/4.0 (compatible; MSIE 4.0; WindowsNT 5.0)");
    //    filter_http = new Boolean(serverproperties.getProperty("server.filter.http","false")).booleanValue();
    //    enable_cookies_by_default=  new Boolean(serverproperties.getProperty("server.enable-cookies-by-default","true")).booleanValue();
    //    debug = new Boolean(serverproperties.getProperty("server.debug-logging","false")).booleanValue();
    //    port = new Integer(serverproperties.getProperty("server.port","8088")).intValue();
    //    log_access = new Boolean(serverproperties.getProperty("server.access.log","false")).booleanValue();
    //    log_access_filename = serverproperties.getProperty("server.access.log.filename","paccess.log");
    //    webconfig = new Boolean(serverproperties.getProperty("server.webconfig","true")).booleanValue();
    //    www_server = new Boolean(serverproperties.getProperty("server.www","true")).booleanValue();
    //    config_user = serverproperties.getProperty("server.webconfig.username","root");
    //    config_password = serverproperties.getProperty("server.webconfig.password","geheim");
    /*
    try {
      
      access_logfile = new BufferedWriter(new FileWriter(ConfigurationManager.getInstance().getStringParameter("Server_sLogFile"), true));
      // Restore the WildcardDioctionary and the URLActions with the ObjectInputStream (settings.dat)...
      ObjectInputStream obj_in;
      File file=new File(DATA_FILE);
      if (!file.exists()) {
        if (!file.createNewFile() || !file.canWrite()) {
          loggerWeb.error("Can't create or write to file " + file.toString());
        } else  saveSettings();
      }
      
      obj_in = new ObjectInputStream(new FileInputStream(file));
      dic = (WildcardDictionary)obj_in.readObject();
      urlactions = (Vector)obj_in.readObject();
      obj_in.close();
    } catch (IOException e) {
      loggerWeb.error("restoreSettings()", e);
    } catch (ClassNotFoundException e_class_not_found) {
    }*/
  }
  /**
   * @return the HTTP version used by jHTTPp2
   */
  public String getHttpVersion() {
    return HTTP_VERSION;
  }
  /** the User-Agent header field
   * @since 0.2.17
   * @return User-Agent String
   */
  public String getUserAgent() {
    return COConfigurationManager.getStringParameter("Server_sProxyUserAgent");
  }
  public void setUserAgent(String ua) {
	COConfigurationManager.setParameter("Server_sProxyUserAgent", ua);
  }
  public void log(int componentId,int event,int color,String text) {
    if (event == org.gudy.azureus2.core.Logger.ERROR)
      loggerCore.error(text);
    else if (event == org.gudy.azureus2.core.Logger.RECEIVED)
      loggerCore.log(SLevel.TORRENT_RECEIVED, text);
    else if (event == org.gudy.azureus2.core.Logger.SENT)
      loggerCore.log(SLevel.TORRENT_SENT, text);
    else {
      if (color==0)
        loggerCore.info(text);
      else
        loggerCore.log(SLevel.CORE_INFO, text);
    }
  }
  /**
   * writes into the server log file and adds a new line
   * @since 0.2.21
   */
/*  public void writeLog(String s) {
    writeLog(s,true);
  } */
  /** writes to the server log file
   * @since 0.2.21
   */
/*  public void writeLog(String s,boolean b) {
    try {
      s=new Date().toString() + " " + s;
      logfile.write(s,0,s.length());
      if (b) logfile.newLine();
      logfile.flush();
//      if (debug)System.out.println(s);
    } catch (Exception e) {
      e.printStackTrace();
    }
  } */
  
/*  public void closeLog() {
    try {
      writeLog("Server shutdown.");
      logfile.flush();
      logfile.close();
      access_logfile.close();
    }
    catch (Exception e) {}
  }*/
  
  public void addBytesRead(long read) {
    bytesread+=read;
  }
  /**
   * Functions for the jHTTPp2 statistics:
   * How many connections
   * Bytes read/written
   * @since 0.3.0
   */
  public void addBytesWritten(int written) {
    byteswritten+=written;
  }
  public int getServerConnections() {
    return numconnections;
  }
  public long getBytesRead() {
    return bytesread;
  }
  public long getBytesWritten() {
    return byteswritten;
  }
  public void increaseNumConnections() {
    numconnections++;
  }
  public void decreaseNumConnections() {
    numconnections--;
  }
  public void AuthenticateUser(String u,String p) {
    //if (config_user.equals(u) && config_password.equals(p)) {
    config_auth = 1;
    //} else config_auth = 0;
  }
  public String getGMTString() {
    return new Date().toString();
  }
  public Jhttpp2URLMatch findMatch(String url) {
    return (Jhttpp2URLMatch)dic.get(url);
  }
  public WildcardDictionary getWildcardDictionary() {
    return dic;
  }
  public Vector getURLActions() {
    return urlactions;
  }
  public boolean enableCookiesByDefault() {
    return COConfigurationManager.getBooleanParameter("Server_bProxyEnableCookies");
  }
  public void enableCookiesByDefault(boolean a) {
	COConfigurationManager.setParameter("Server_bProxyEnableCookies", a);
  }
  public void resetStat() {
    bytesread=0;
    byteswritten=0;
  }
  /**
   * @since 0.4.10a
   *
  public Properties getServerProperties() {
    if (serverproperties == null) {
      serverproperties = new Properties();
      try {
        serverproperties.load(new DataInputStream(new FileInputStream(SERVER_PROPERTIES_FILE)));
      } catch (IOException e) {
        loggerWeb.error("getServerProperties()", e);
      }
    }
    return serverproperties;
  }*/
  /**
   * @since 0.4.10a
   *
  public void storeServerProperties() {
    if (serverproperties==null) return;
    try {
      serverproperties.store(new FileOutputStream(SERVER_PROPERTIES_FILE),"Jhttpp2Server main properties. Look at the README file for further documentation.");
    } catch (IOException e) {
      loggerWeb.error("storeServerProperties()", e);
    }
  }
  /**
   * @since 0.4.10a
   */
/*  public void logAccess(String s) {
    try {
      access_logfile.write("[" + new Date().toString() + "] " + s + "\r\n");
      access_logfile.flush();
    } catch (Exception e) {
      writeLog("Jhttpp2Server.access(String): " + e.getMessage());
    }
  }*/
  public void shutdownServer() {
    loggerWeb.info("Server shutdown.");
    org.gudy.azureus2.ui.common.Main.shutdown();
  }
  
}