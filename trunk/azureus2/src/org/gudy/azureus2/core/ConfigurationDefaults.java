/*
 * ConfigurationDefaults.java
 *
 * Created on 31. Juli 2003, 21:31
 */

package org.gudy.azureus2.core;

import java.util.HashMap;

/**
 *
 * @author  Tobias Minich
 */

/**
 * Some (proposed) option naming conventions:
 * - Starts with a general identifier
 *   General_ for, well, general things =)
 *   Server_ for webinterface specific things
 *   GUI_ (eventually) for GUI specific things
 *   Core_ (ebentually) for core specific things
 * - Second is some identifing term. It starts with a small letter denoting
 *   the vaiable type.
 *   b Boolean
 *   i Integer
 *   s String
 * - Directory options should end with _Directory. This activates some
 *   special validity checks in the webinterface option parsing code.
 *   (Namely they are created if they don't exist and the option isn't changed
 *   with a logged error if a normal file of the same name exists)
 */

public class ConfigurationDefaults {
  
  private static ConfigurationDefaults configdefaults;
  
  private HashMap def = null;
  
  public int def_int = 0;
  public int def_boolean = 0;
  public String def_String = "";
  public byte[] def_bytes = null;
  
  public synchronized static ConfigurationDefaults getInstance() {
    if(configdefaults == null)
      configdefaults = new ConfigurationDefaults();
    return configdefaults;
  }
  /** Creates a new instance of Defaults */
  public ConfigurationDefaults() {
    def = new HashMap();
    
    /** General settings **/
    // Default save directory
    def.put("General_sDefaultSave_Directory", ConfigurationManager.getApplicationPath()+"download");
    // Default torrent directory
    def.put("General_sDefaultTorrent_Directory", ConfigurationManager.getApplicationPath()+"torrents");
    
    /** Core settings **/
    // (currently only a reference list for me)
    // Override Ip ("Override Ip")
    //def.put("Core_sOverrideIP", "");
    def.put("Override Ip", "");
    // Allocate new files ("Allocate New")
    //def.put("Core_bAllocateNew", new Long(1));
    def.put("Allocate New", new Long(1));
    // Lower port to use for BT ("Low Port")
    //def.put("Core_iLowPort", new Long(6881));
    def.put("Low Port", new Long(6881));
    // Upper port to use for BT ("High Port")
    //def.put("Core_iHighPort", new Long(6889));
    def.put("High Port", new Long(6889));
    // Maximal active torrents ("max active torrents")
    //def.put("Core_iMaxActiveTorrents", new Long(4));
    def.put("max active torrents", new Long(4));
    // Maximal active downloads (torrents!=seed) ("max downloads")
    //def.put("Core_iMaxDownloads", new Long(4));
    def.put("max downloads", new Long(4));
    // Maxmail number of connections (0=unlimited) ("Max Clients")
    //def.put("Core_iMaxClients", new Long(0));
    def.put("Max Clients", new Long(0));
    // Default max uploads per torrent ("Max Uploads")
    //def.put("Core_iMaxUploads", new Long(4));
    def.put("Max Uploads", new Long(4));
    // Maximal upload speed (globally, 0=unlimited) ("Max Upload Speed")
    //def.put("Core_iMaxUploadSpeed", new Long(0));
    def.put("Max Upload Speed", new Long(0));
    // Fast Resume
    //def.put("Core_bUseResume", new Long(0));
    def.put("Use Resume", new Long(0));
    // Save Resume Interval (minutes)
    //def.put("Core_iSaveResumeInterval, new Long(5));
    def.put("Save Resume Interval", new Long(5));
    
    /** Headless Server settings **/
    // Server Name
    def.put("Server_sName", "Azureus2 WebInterface");
    // IP to bind to
    def.put("Server_sBindIP", "");
    // Port the server runs on
    def.put("Server_iPort", new Long(8088));
    // Connection Timeout in seconds.
    def.put("Server_iTimeout", new Long(10));
    // Path to the html templates.
    def.put("Server_sTemplate_Directory", getApplicationPath()+"org/gudy/azureus2/server/template");
    // Maximal simultaneous connections
    def.put("Server_iMaxHTTPConnections", new Long(5));
    // Auto-refresh torrents every (seconds, 0 = off);
    def.put("Server_iRefresh", new Long(20));
    // Allowed static ips (space separated list)
    def.put("Server_sAllowStatic", "127.0.0.1");
    // Allowed dynamic hosts (space separated list)
    def.put("Server_sAllowDynamic", "");
    // Recheck dynamic hosts every (minutes)
    def.put("Server_iRecheckDynamic", new Long(30));
    // Be not JavaScript-dependant
    def.put("Server_bNoJavaScript", new Long(0));
    
    // Relevant for the proxy part
    // Fake hostname to access the webinterface when used in proxy mode
    def.put("Server_sAccessHost", "torrent");
    // Enable Cookies
    def.put("Server_bProxyEnableCookies", new Long(1));
    // Block certain URLs
    def.put("Server_bProxyBlockURLs", new Long(0));
    // Filter HTTP Headers (Referer and User Agent)
    def.put("Server_bProxyFilterHTTP", new Long(0));
    // User agent for outgoing connections
    def.put("Server_sProxyUserAgent", "Mozilla/4.0 (compatible; MSIE 4.0; WindowsNT 5.0)");
    // Use a downstream proxy
    def.put("Server_bUseDownstreamProxy", new Long(0));
    // Server Host Name
    def.put("Server_sDownstreamProxyHost", "127.0.0.1");
    // Port of a downstream proxy
    def.put("Server_iDownstreamProxyPort", new Long(0));
    // Grab Torrents in Proxy mode
    def.put("Server_bProxyGrabTorrents", new Long(1));
    
    
    // Logging relevant Stuff
    //  Log levels:
    //   50000 Fatal
    //   40000 Error
    //   30000 Warn
    //   20000 Info
    //   12000 HTTP (SLevel)
    //   11101 Torrent Received (SLevel)
    //   11100 Torrent Sent (SLevel)
    //   11000 Core info (SLevel)
    //   10001 Thread (SLevel)
    //   10000 Debug
    // Log to file
    def.put("Server_bLogFile", new Long(0));
    // Logfile
    def.put("Server_sLogFile", ConfigurationManager.getApplicationPath()+"webinterface.log");
    // Log Level for web interface
    def.put("Server_iLogLevelWebinterface", new Long(20000));
    // Log Level for core
    def.put("Server_iLogLevelCore", new Long(20000));
    // Number of remembered log entries
    def.put("Server_iLogCount", new Long(200));
  }
  
  public String getStringParameter(String p) throws ConfigurationParameterNotFoundException {
    if (def.containsKey(p))
      return (String) def.get(p);
    else
      throw new ConfigurationParameterNotFoundException(p);
  }
  
  public int getIntParameter(String p) throws ConfigurationParameterNotFoundException {
    if (def.containsKey(p))
      return ((Long) def.get(p)).intValue();
    else
      throw new ConfigurationParameterNotFoundException(p);
  }
  
  //TODO:: Move this to a FileManager class?
  private String getApplicationPath() {
    return System.getProperty("user.dir")+System.getProperty("file.separator");
  }
}
