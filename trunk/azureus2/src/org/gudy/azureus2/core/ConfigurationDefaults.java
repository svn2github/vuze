/*
 * ConfigurationDefaults.java
 *
 * Created on 31. Juli 2003, 21:31
 */

package org.gudy.azureus2.core;

import java.util.HashMap;
import java.lang.*;

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
  
  public static HashMap def = null;
  
  public static int def_int = 0;
  public static boolean def_boolean = false;
  public static String def_String = "";
  public static byte[] def_bytes = null;
  
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
    // Connection Timeout in seconds.
    def.put("Server_iTimeout", new Long(10));
    // Path to the html templates.
    def.put("Server_sTemplate_Directory", getApplicationPath()+"org/gudy/azureus2/server/template");
    // Signature on end of page. Currently a 'left over' from pws, does nothing here.
    def.put("Server_sSignature", "on");
    // Server Name
    def.put("Server_sName", "Azureus2 WebInterface");
    // Server Admin email address (Currently only used in signature, does therefore nothing)
    def.put("Server_sAdmin", "postmaster@localhost");
    // Port the server runs on
    def.put("Server_iPort", new Long(5080));
    // Maximal simultaneous connections
    def.put("Server_iMaxHTTPConnections", new Long(5));
    // Verbosity
    //  0 Error only
    //  1 Torrent Infos
    //  2 Warnings
    //  3 Info
    def.put("Server_iVerbosity", new Long(1));
    // IP to bind to
    def.put("Server_sBindIP", "");
    // Number of remembered log entries
    def.put("Server_iLogCount", new Long(200));
    // Auto-refresh torrents every (seconds)
    def.put("Server_iRefresh", new Long(20));
  }
  
  //TODO:: Move this to a FileManager class?
  private String getApplicationPath() {
    return System.getProperty("user.dir")+System.getProperty("file.separator");
  }
}
