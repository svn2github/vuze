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
    
    /** Headless Server settings **/
    // Server Name
    def.put("Server_sName", "Azureus2 WebInterface");
    // Server Admin email address (Currently only used in signature, does therefore nothing)
    def.put("Server_sAdmin", "postmaster@localhost");
    // IP to bind to
    def.put("Server_sBindIP", "");
    // Port the server runs on
    def.put("Server_iPort", new Long(8088));
    // Connection Timeout in seconds.
    def.put("Server_iTimeout", new Long(10));
    // Path to the html templates.
    def.put("Server_sTemplate_Directory", getApplicationPath()+"org/gudy/azureus2/server/template");
    // Signature on end of page. Currently a 'left over' from pws, does nothing here.
    def.put("Server_sSignature", "on");
    // Maximal simultaneous connections
    def.put("Server_iMaxHTTPConnections", new Long(5));
    // Auto-refresh torrents every (seconds)
    def.put("Server_iRefresh", new Long(20));
    // Fake hostname to access the webinterface when used in proxy mode
    def.put("Server_sAccessHost", "torrent");
    
    // Relevant for the proxy part
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
    
    // Logging relevant Stuff
    // Log to file
    def.put("Server_bLogFile", new Long(0));
    // Logfile
    def.put("Server_sLogFile", ConfigurationManager.getApplicationPath()+"webinterface.log");
    // Log HTTP Access
    def.put("Server_bLogAccess", new Long(0));
    // Verbosity
    //  0 Error only
    //  1 Torrent Infos
    //  2 Warnings
    //  3 Info
    def.put("Server_iVerbosity", new Long(1));
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
