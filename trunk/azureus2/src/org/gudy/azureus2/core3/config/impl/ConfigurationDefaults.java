/* Written and copyright 2001-2003 Tobias Minich.
 * Distributed under the GNU General Public License; see the README file.
 * This code comes with NO WARRANTY.
 *
 *
 * ConfigurationDefaults.java
 *
 * Created on 31. Juli 2003, 21:31
 */

package org.gudy.azureus2.core3.config.impl;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.io.File;

import org.eclipse.swt.SWT;
import org.gudy.azureus2.core3.stats.StatsWriterPeriodic;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.tracker.client.TRTrackerClient;
import org.gudy.azureus2.core3.tracker.host.TRHost;
import org.gudy.azureus2.core3.tracker.server.TRTrackerServer;
import org.gudy.azureus2.core3.util.SystemProperties;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.util.*;


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
 *   Core_ (eventually) for core specific things
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
  private static AEMonitor				class_mon	= new AEMonitor( "ConfigDef");
  
  private HashMap def = null;
  
  public int def_int = 0;
  public float def_float = 0;
  public int def_boolean = 0;
  public String def_String = "";
  public byte[] def_bytes = null;
  
  public static ConfigurationDefaults getInstance() 
  {
  	try{
  		class_mon.enter();
  	
	    if(configdefaults == null)
	      configdefaults = new ConfigurationDefaults();
	    return configdefaults;
  	}finally{
  		
  		class_mon.exit();
  	}
  }
  /** Creates a new instance of Defaults */
  public ConfigurationDefaults() {
    def = new HashMap();
    
    
    
    //NOTE: only used in the console UI AFAIK; replaced by "Default save path"
    def.put("General_sDefaultSave_Directory", SystemProperties.getUserPath()+"downloads");
    
    
    // Default torrent directory
    def.put("General_sDefaultTorrent_Directory", SystemProperties.getUserPath()+"torrents");
    
    /** Core settings **/

    def.put("Override Ip", "");
    def.put("Enable incremental file creation", new Long(0));
    def.put("TCP.Listen.Port", new Long(6881));
    def.put("max active torrents", new Long(4));
    def.put("max downloads", new Long(4));

    def.put("Max.Peer.Connections.Per.Torrent", new Long(COConfigurationManager.CONFIG_DEFAULT_MAX_CONNECTIONS_PER_TORRENT));
    def.put("Max.Peer.Connections.Total", new Long(COConfigurationManager.CONFIG_DEFAULT_MAX_CONNECTIONS_GLOBAL));

    def.put("File Max Open", new Long(50));

    def.put("Max Uploads", new Long(4));
    def.put("Max Upload Speed KBs", new Long(0));
    def.put("Use Resume", new Long(1));
    def.put("Save Resume Interval", new Long(5));
    def.put("Check Pieces on Completion", new Long(1));
    def.put("Stop Ratio", new Float(0));
    def.put("Stop Peers Ratio", new Long(0));
    def.put("Disconnect Seed", new Long(1));
    def.put("priorityExtensions", "");
    def.put("priorityExtensionsIgnoreCase", new Long(0));
    def.put("Ip Filter Enabled", new Long(1));
    def.put("Ip Filter Allow",new Long(0));
    def.put("Allow Same IP Peers",new Long(0));
    def.put("Use Super Seeding",new Long(0));

        
    /** SWT GUI Settings **/
    def.put("useCustomTab",new Long(1));    
    def.put("GUI Refresh",new Long(1000));
    def.put("Graphics Update",new Long(4));
    def.put("ReOrder Delay",new Long(0));
    def.put("Send Version Info",new Long(1));
    def.put("Show Download Basket",new Long(0));
    def.put("config.style.refreshMT",new Long(0));
    def.put("Open Details", new Long(0));
    def.put("General_sUpdateLanguageURL", "http://azureus.sf.net/update/langUpdate.php?lang=%s");
    def.put("General_bEnableLanguageUpdate", new Long(0));
    def.put("Use default data dir", new Long(1));
    String	default_save_path = SystemProperties.getUserPath()+"downloads";
    File	default_save_path_file = new File( default_save_path );
    if ( !default_save_path_file.exists()){
    	default_save_path_file.mkdir();
    }
    def.put("Default save path", default_save_path );
    def.put("GUI_SWT_bFancyTab", new Long(1));
    def.put("GUI_SWT_bAlternateTablePainting", new Long(0));
    def.put("update.start",new Long(1));
    def.put("update.periodic",new Long(1));
    def.put("update.opendialog",new Long(1));
    def.put("Tracker Password Enable Web", new Long(0));
    def.put("Tracker Username", "");
    def.put("Tracker Password", "");
    
    boolean bGTKTableBug = false;
    try {
      bGTKTableBug = Constants.isLinux && SWT.getPlatform().equals("gtk");
    } catch (NoClassDefFoundError e) {
      /* Ignore, SWT not installed */
    }
    
    def.put("SWT_bGTKTableBug", new Long(bGTKTableBug ? 1: 0));
    def.put("Colors.progressBar.override", new Long(0));
    
    def.put("Logging Enable", new Long(0));
    def.put("Logging Dir", "");
    def.put("Logging Max Size", new Long(5));
    int[] logComponents = { 0, 1, 2, 4 };
    for (int i = 0; i < logComponents.length; i++)
      for (int j = 0; j <= 3; j++)
        def.put("bLog" + logComponents[i] + "-" + j, new Long(1));

    // Start/Stop Automation Stuff
    def.put("StartStopManager_iNumPeersAsFullCopy", new Long(0));
    def.put("StartStopManager_iFakeFullCopySeedStart", new Long(1));
    def.put("StartStopManager_iMinPeersToBoostNoSeeds", new Long(1));
    def.put("StartStopManager_iMinSpeedForActiveDL", new Long(512));
    def.put("StartStopManager_iMinSpeedForActiveSeeding", new Long(512));
    def.put("StartStopManager_iRankType", new Long(org.gudy.azureus2.core3.global.startstoprules.defaultplugin.StartStopRulesDefaultPlugin.RANK_SPRATIO));
    def.put("StartStopManager_iRankTypeSeedFallback", new Long(0));
    def.put("StartStopManager_bAutoReposition", new Long(0));
    def.put("StartStopManager_iMinSeedingTime", new Long(60*3));
    def.put("StartStopManager_bIgnore0Peers", new Long(1));
    def.put("StartStopManager_iIgnoreSeedCount", new Long(0));
    def.put("StartStopManager_bPreferLargerSwarms", new Long(1));
    def.put("StartStopManager_bDebugLog", new Long(0));
    def.put("StartStopManager_iFirstPriority_Type", new Long(org.gudy.azureus2.core3.global.startstoprules.defaultplugin.StartStopRulesDefaultPlugin.FIRSTPRIORITY_ANY));
    def.put("StartStopManager_iFirstPriority_ShareRatio", new Long(500));
    def.put("StartStopManager_iFirstPriority_SeedingMinutes", new Long(0));
    def.put("StartStopManager_iFirstPriority_DLMinutes", new Long(0));
    def.put("StartStopManager_bAutoStart0Peers", new Long(0));
    // for "Stop Peers Ratio" ignore rule
    def.put("StartStopManager_iIgnoreRatioPeersSeedStart", new Long(0));
    // for "Stop Ratio" ignore rule
    def.put("StartStopManager_iIgnoreShareRatioSeedStart", new Long(0));
    
    	//tracker proxy defaults
    def.put( "Enable.Proxy", new Long(0) );
    def.put( "Enable.SOCKS", new Long(0) );
    def.put( "Proxy.Host", "" );
    def.put( "Proxy.Port", "" );
    def.put( "Proxy.Username", "" );
    def.put( "Proxy.Password", "" );
    
    	// data proxy defaults
    def.put( "Proxy.Data.Enable", new Long(0));
    def.put( "Proxy.Data.SOCKS.version", "V4" );
    def.put( "Proxy.Data.SOCKS.inform", new Long(1));
    def.put( "Proxy.Data.Same", new Long(1));
    def.put( "Proxy.Data.Host", "" );
    def.put( "Proxy.Data.Port", "" );
    def.put( "Proxy.Data.Username", "" );
    def.put( "Proxy.Data.Password", "" );

    //old
    def.put( "Start Num Peers", new Long(-1) );
    def.put( "Max Upload Speed", new Long(-1) );
    def.put( "Max Clients", new Long(-1) );
    def.put( "Server.shared.port", new Long(1) );
    def.put( "Low Port", new Long(6881) );
    def.put( "Already_Migrated", new Long(0) );
    
    //misc
    def.put( "ID", "" );
    def.put( "Play Download Finished", new Long(0) );
    def.put( "Play Download Finished File", "" );
    def.put( "Close To Tray", new Long(1) );
    def.put( "Minimize To Tray", new Long(0) );
    def.put( "Watch Torrent Folder", new Long(0) );
    def.put( "Watch Torrent Folder Interval", new Long(1) );
    def.put( "Start Watched Torrents Stopped", new Long(0) );
    def.put( "Watch Torrent Folder Path", "" );
    def.put( "DiskManager Write Queue Block Limit", new Long(0) );
    def.put( "DiskManager Check Queue Piece Limit", new Long(0) );
    def.put( "Prioritize First Piece", new Long(0) );
    def.put( "Move Completed When Done", new Long(0) );
    def.put( "Completed Files Directory", "" );
    def.put( "Zero New", new Long(0) );
    def.put( "Move Only When In Default Save Dir", new Long(1) );
    def.put( "Move Torrent When Done", new Long(1) );
    def.put( "File.save.peers.enable", new Long(1) );
    def.put( "Save Torrent Files", new Long(1) );
    def.put( "Old.Socket.Polling.Style", new Long(0) );
    def.put( "Max Download Speed KBs", new Long(0) );
    def.put( "Bind IP", "" );
    def.put( "Stats Export Peer Details", new Long(0) );
    def.put( "Stats XSL File", "" );
    def.put( "Stats Enable", new Long(0) );
    def.put( "Stats Period", new Long(StatsWriterPeriodic.DEFAULT_SLEEP_PERIOD) );
    def.put( "Stats Dir", "" );
    def.put( "Stats File", StatsWriterPeriodic.DEFAULT_STATS_FILE_NAME );
    def.put( "File.Torrent.IgnoreFiles", TOTorrent.DEFAULT_IGNORE_FILES );
    def.put( "Tracker Compact Enable", new Long(1) );
    def.put( "Tracker Key Enable", new Long(1) );
    def.put( "Tracker Separate Peer IDs", new Long(0));
    def.put( "File.save.peers.max", new Long( TRTrackerClient.DEFAULT_PEERS_TO_CACHE ) );
    def.put( "Tracker Public Enable", new Long(0) );
    def.put( "Tracker Log Enable", new Long(0) );
    def.put( "Tracker Port Enable", new Long(1) );
    def.put( "Tracker Port", new Long( TRHost.DEFAULT_PORT ) );
    def.put( "Tracker Port SSL Enable", new Long(0) );
    def.put( "Tracker Port SSL", new Long( TRHost.DEFAULT_PORT_SSL ) );
    def.put( "Tracker Port Force External", new Long(0) );
    def.put( "Tracker IP", "" );
    def.put( "Tracker Port UDP Enable", new Long(0) );
    def.put( "Tracker Port UDP Version", new Long(2) );
    def.put( "Tracker Send Peer IDs", new Long(1) );
    def.put( "Tracker Max Peers Returned", new Long(0) );
    def.put( "Tracker Scrape Cache", new Long( TRTrackerServer.DEFAULT_SCRAPE_CACHE_PERIOD ) );
    def.put( "Tracker Announce Cache", new Long( TRTrackerServer.DEFAULT_ANNOUNCE_CACHE_PERIOD ) );
    def.put( "Tracker Announce Cache Min Peers", new Long( TRTrackerServer.DEFAULT_ANNOUNCE_CACHE_PEER_THRESHOLD ) );
    def.put( "Tracker Poll Interval Min", new Long( TRTrackerServer.DEFAULT_MIN_RETRY_DELAY) );
    def.put( "Tracker Poll Interval Max", new Long( TRTrackerServer.DEFAULT_MAX_RETRY_DELAY) );
    def.put( "Tracker Scrape Retry Percentage", new Long( TRTrackerServer.DEFAULT_SCRAPE_RETRY_PERCENTAGE ) );
    def.put( "Tracker Password Enable Web", new Long(0) );
    def.put( "Tracker Password Enable Torrent", new Long(0) );
    def.put( "Tracker Username", "" );
    def.put( "Tracker Password", null );
    def.put( "Tracker Poll Inc By", new Long( TRTrackerServer.DEFAULT_INC_BY ) );
    def.put( "Tracker Poll Inc Per", new Long( TRTrackerServer.DEFAULT_INC_PER ) );
    def.put( "Tracker NAT Check Enable", new Long(1));
    def.put( "Tracker NAT Check Timeout", new Long(TRTrackerServer.DEFAULT_NAT_CHECK_SECS));
    def.put( "Tracker Max Seeds Retained", new Long( 0 ) );
    def.put( "config.style.useSIUnits", new Long(0) );
    def.put( "config.style.useUnitsRateBits", new Long(0) );
    def.put( "Save Torrent Backup", new Long(0) );
    def.put( "Sharing Use SSL", new Long(0) );
    def.put( "Sharing Add Hashes", new Long(0) );
    def.put( "File.Decoder.Prompt", new Long(0) );
    def.put( "File.Decoder.Default", "" );
    def.put( "File.Decoder.ShowLax", new Long(0));
    def.put( "File.Decoder.ShowAll", new Long(0));
    def.put( "Password enabled", new Long(0) );
    def.put( "Password", null );
    def.put( "Save detail views column widths", new Long(0) );
    def.put( "config.interface.checkassoc", new Long(1) );
    def.put( "Wizard Completed", new Long(0) );
    def.put( "donations.donated", new Long(0) );
    def.put( "donations.lastVersion", "" );
    def.put( "donations.nextAskTime", new Long(0) );
    def.put( "Color Scheme.red", new Long(0) );
    def.put( "Color Scheme.green", new Long(128) );
    def.put( "Color Scheme.blue", new Long(255) );
    def.put( "Show Splash", new Long(1) );
    def.put( "window.maximized", new Long(1) );
    def.put( "window.rectangle", "" );
    def.put( "Open Console", new Long(0) );
    def.put( "Open Config", new Long(0) );
    def.put( "Start Minimized", new Long(0) );
    def.put( "Open Bar", new Long(0) );
    def.put( "confirmationOnExit", new Long(0) );
    def.put( "locale", Locale.getDefault().toString() );
    def.put( "Add URL Silently", new Long(0) );
    def.put( "config.style.dropdiraction", "0" );
    def.put( "MyTorrents.SplitAt", new Long(30) );
    def.put( "Confirm Data Delete", new Long(1) );
    def.put( "Password Confirm", null );
    def.put( "Auto Update", new Long(1) );
    def.put( "Alert on close", new Long(1) );
    def.put( "diskmanager.friendly.hashchecking", new Long(0) );
    def.put( "Default Start Torrents Stopped", new Long(0));
    def.put( "Server Enable UDP", new Long(1));
    def.put( "diskmanager.perf.cache.enable", new Long(1));
    def.put( "diskmanager.perf.cache.enable.read", new Long(1));
    def.put( "diskmanager.perf.cache.enable.write", new Long(1));
    def.put( "diskmanager.perf.cache.size", new Long(4));		// 4 MB
    def.put( "network.tcp.mtu.size", new Long(1500) );
    def.put( "File.truncate.if.too.large", new Long(0));
    def.put( "diskmanager.perf.cache.trace", new Long(0));
    def.put( "Enable System Tray", new Long(1));
    def.put( "config.style.table.sortDefaultAscending", new Long(1));
    def.put( "Ignore.peer.ports", "0" );
    def.put( "Security.JAR.tools.dir", "" );
    def.put( "network.max.simultaneous.connect.attempts", new Long( 8 ));
    
  }
  
  public String getStringParameter(String p) throws ConfigurationParameterNotFoundException {
    if (def.containsKey(p)) {
      Object o = def.get(p);
      if (o instanceof Number)
        return ((Number)o).toString();

      return (String)o;
    } else
      throw new ConfigurationParameterNotFoundException(p);
  }
  
  public int getIntParameter(String p) throws ConfigurationParameterNotFoundException {
    if (def.containsKey(p))
      return ((Long) def.get(p)).intValue();
    else
      throw new ConfigurationParameterNotFoundException(p);
  }

  public float getFloatParameter(String p) throws ConfigurationParameterNotFoundException {
    if (def.containsKey(p))
      return ((Float) def.get(p)).floatValue();
    else
      throw new ConfigurationParameterNotFoundException(p);
  }
  
  public Set getAllowedParameters() {
  	return def.keySet();
  }
 
  public void addParameter(String sKey, String sParameter) {
    def.put(sKey, sParameter);
  }

  public void addParameter(String sKey, int iParameter) {
    def.put(sKey, new Long(iParameter));
  }
  public void addParameter(String sKey, byte[] bParameter) {
    def.put(sKey, bParameter);
  }

  public void addParameter(String sKey, boolean bParameter) {
    Long lParameter = new Long(bParameter ? 1 : 0);
    def.put(sKey, lParameter);
  }
  
  public void registerExternalDefaults(Map addmap) {
  	def.putAll(addmap);
  }
  
  public boolean doesParameterExist(String p) {
    return def.containsKey(p);
  }
}
