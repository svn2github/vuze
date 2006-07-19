/* Written and copyright 2001-2003 Tobias Minich.
 *
 * ConfigurationDefaults.java
 *
 * Created on 31. Juli 2003, 21:31
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.core3.config.impl;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.stats.StatsWriterPeriodic;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
import org.gudy.azureus2.core3.tracker.host.TRHost;
import org.gudy.azureus2.core3.tracker.server.TRTrackerServer;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.SystemProperties;

import com.aelitis.azureus.plugins.startstoprules.defaultplugin.DefaultRankCalculator;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


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
  private static final Long FALSE	= new Long(0);
  private static final Long TRUE	= new Long(1);
  
  private static ConfigurationDefaults configdefaults;
  private static AEMonitor				class_mon	= new AEMonitor( "ConfigDef");
  
  private HashMap def = null;
  
  public int def_int = 0;
  public float def_float = 0;
  public int def_boolean = 0;
  public String def_String = "";
  public byte[] def_bytes = null;
  
  public static ConfigurationDefaults 
  getInstance() 
  {
  	try{
  		class_mon.enter();
  	
	    if(configdefaults == null){
	    
	      configdefaults = new ConfigurationDefaults();
	    }
	    
	    return configdefaults;
	    
  	}finally{
  		
  		class_mon.exit();
  	}
  }
  
  /** Creates a new instance of Defaults */
  protected 
  ConfigurationDefaults() 
  {
    def = new HashMap();

    
    /** Core settings **/

    def.put("Override Ip", "");
    def.put("Enable incremental file creation", FALSE);
    def.put("TCP.Listen.Port", new Long( 6881 ));
    def.put("TCP.Listen.Port.Enable", TRUE );
    def.put("UDP.Listen.Port", new Long( 6881 ));
    def.put("UDP.Listen.Port.Enable", TRUE );
    def.put("UDP.NonData.Listen.Port", new Long( 6881 ));	// two effective enablers for this, dht + tracker udp client
    def.put("UDP.NonData.Listen.Port.Same", TRUE );			// control over whether non-data and data udp port are the same
    def.put("max active torrents", new Long(4));
    def.put("max downloads", new Long(4));
    def.put("Newly Seeding Torrents Get First Priority", TRUE);
    def.put("Max.Peer.Connections.Per.Torrent", new Long(COConfigurationManager.CONFIG_DEFAULT_MAX_CONNECTIONS_PER_TORRENT));
    def.put("Max.Peer.Connections.Total", new Long(COConfigurationManager.CONFIG_DEFAULT_MAX_CONNECTIONS_GLOBAL));

    def.put( "File Max Open", new Long(50));
    def.put( "Use Config File Backups", TRUE);
    
    def.put( "Max Uploads", new Long(4) );
    def.put( "Max Uploads Seeding", new Long(4));
    def.put( "enable.seedingonly.maxuploads", FALSE );
    def.put( "max.uploads.when.busy.inc.min.secs", new Long( 30 ));
    def.put( "Max Download Speed KBs", new Long(0) );
    def.put( "Max Upload Speed KBs", new Long(0));
    def.put( "Max Upload Speed Seeding KBs", new Long(0) );
    def.put( "enable.seedingonly.upload.rate", FALSE );
    
    def.put( "Auto Upload Speed Enabled", FALSE );
    def.put( "Auto Upload Speed Seeding Enabled", FALSE );
    def.put( "AutoSpeed Available", FALSE );	// informative read-only parameter
    def.put( "AutoSpeed Min Upload KBs", new Long(0) );
    def.put( "AutoSpeed Max Upload KBs", new Long(0) );
    def.put( "AutoSpeed Max Increment KBs", new Long(5));
    def.put( "AutoSpeed Max Decrement KBs", new Long(5));
    def.put( "AutoSpeed Choking Ping Millis", new Long(1000) );
    def.put( "AutoSpeed Download Adj Enable", FALSE );
    def.put( "AutoSpeed Download Adj Ratio", "1.0" );
    def.put( "AutoSpeed Latency Factor", new Long(50));
    def.put( "Auto Upload Speed Debug Enabled", FALSE );
    
    def.put( "LAN Speed Enabled", TRUE );
    def.put( "Max LAN Download Speed KBs", new Long(0) );
    def.put( "Max LAN Upload Speed KBs", new Long(0) );
    
    def.put("Use Resume", TRUE);
    def.put("On Resume Recheck All", FALSE);
    def.put("Save Resume Interval", new Long(5));
    def.put("Check Pieces on Completion", TRUE);
    def.put("Stop Ratio", new Float(0));
    def.put("Stop Peers Ratio", new Long(0));
    def.put("Disconnect Seed", TRUE);
    def.put("priorityExtensions", "");
    def.put("priorityExtensionsIgnoreCase", FALSE);
    def.put("Ip Filter Enabled", TRUE);
    def.put("Ip Filter Allow",FALSE);
    def.put("Ip Filter Enable Banning", TRUE);
    def.put("Ip Filter Ban Block Limit", new Long(4));
    def.put("Ip Filter Banning Persistent", TRUE);
    def.put("Allow Same IP Peers",FALSE);
    def.put("Use Super Seeding",FALSE);

        
    /** SWT GUI Settings **/
    
    def.put("User Mode", new Long(0));
    
    def.put("useCustomTab",TRUE);    
    def.put("GUI Refresh",new Long(1000));
    def.put("Graphics Update",new Long(4));
    def.put("ReOrder Delay",new Long(0));
    def.put("Send Version Info",TRUE);
    def.put("Show Download Basket",FALSE);
    def.put("config.style.refreshMT",new Long(0));
    def.put("Open Details", FALSE);
    def.put("IconBar.enabled", TRUE);
    
    //default data location options
    def.put("Use default data dir", FALSE);	
    def.put("Default save path", "" );
    def.put("DefaultDir.BestGuess", TRUE);
    def.put("DefaultDir.AutoUpdate", TRUE);
    
    def.put("GUI_SWT_bFancyTab", TRUE);
    def.put("GUI_SWT_bAlternateTablePainting", FALSE);
    def.put("update.start",TRUE);
    def.put("update.periodic",TRUE);
    def.put("update.opendialog",TRUE);
    def.put("Colors.progressBar.override", FALSE);
    def.put("GUI_SWT_DisableAlertSliding", FALSE);
    def.put("NameColumn.showProgramIcon", TRUE);
    
    
    def.put("Logger.Enabled", FALSE);  //logging in general
    def.put("Logging Enable", FALSE);  //file logging
    def.put("Logging Dir", "");
    def.put("Logging Max Size", new Long(5));
    int[] logComponents = { 0, 1, 2, 4 };
    for (int i = 0; i < logComponents.length; i++)
      for (int j = 0; j <= 3; j++)
        def.put("bLog" + logComponents[i] + "-" + j, TRUE);
    def.put("Logger.DebugFiles.Enabled", TRUE);
    def.put("Logging Enable UDP Transport", FALSE); 

    
    
    // Start/Stop Automation Stuff
    def.put("StartStopManager_iNumPeersAsFullCopy", new Long(0));
    def.put("StartStopManager_iFakeFullCopySeedStart", new Long(1));
    def.put("StartStopManager_iMinPeersToBoostNoSeeds", new Long(1));
    def.put("StartStopManager_iMinSpeedForActiveDL", new Long(512));
    def.put("StartStopManager_iMinSpeedForActiveSeeding", new Long(512));
    def.put("StartStopManager_iRankType", new Long(com.aelitis.azureus.plugins.startstoprules.defaultplugin.StartStopRulesDefaultPlugin.RANK_SPRATIO));
    def.put("StartStopManager_iRankTypeSeedFallback", new Long(0));
    def.put("StartStopManager_bAutoReposition", FALSE);
    def.put("StartStopManager_iMinSeedingTime", new Long(60*3));
    def.put("StartStopManager_bIgnore0Peers", TRUE);
    def.put("StartStopManager_iIgnoreSeedCount", new Long(0));
    def.put("StartStopManager_bPreferLargerSwarms", TRUE);
    def.put("StartStopManager_bDebugLog", FALSE);
    def.put("StartStopManager_iFirstPriority_Type", new Long(DefaultRankCalculator.FIRSTPRIORITY_ANY));
    def.put("StartStopManager_iFirstPriority_ShareRatio", new Long(500));
    def.put("StartStopManager_iFirstPriority_SeedingMinutes", new Long(0));
    def.put("StartStopManager_iFirstPriority_DLMinutes", new Long(0));
    def.put("StartStopManager_bAutoStart0Peers", FALSE);
	// for ignore FP rules
	def.put("StartStopManager_iFirstPriority_ignoreSPRatio", new Long(0));
	def.put("StartStopManager_bFirstPriority_ignore0Peer", FALSE);
    // for "Stop Peers Ratio" ignore rule
    def.put("StartStopManager_iIgnoreRatioPeersSeedStart", new Long(0));
    // for "Stop Ratio" ignore rule
    def.put("StartStopManager_iIgnoreShareRatioSeedStart", new Long(0));
    def.put("StartStopManager_bNewSeedsMoveTop", TRUE);
    def.put("StartStopManager_iAddForSeedingDLCopyCount", new Long(1));
    def.put("StartStopManager_iMaxActiveTorrentsWhenSeeding", new Long(0));
    def.put("StartStopManager_bMaxActiveTorrentsWhenSeedingEnabled", FALSE);
    
    	//tracker proxy defaults
    def.put( "Enable.Proxy", FALSE );
    def.put( "Enable.SOCKS", FALSE );
    def.put( "Proxy.Host", "" );
    def.put( "Proxy.Port", "" );
    def.put( "Proxy.Username", "<none>" );	// default is explicit "none", as opposed to "not defined"
    def.put( "Proxy.Password", "" );
    
    	// data proxy defaults
    def.put( "Proxy.Data.Enable", FALSE);
    def.put( "Proxy.Data.SOCKS.version", "V4" );
    def.put( "Proxy.Data.SOCKS.inform", TRUE);
    def.put( "Proxy.Data.Same", TRUE);
    def.put( "Proxy.Data.Host", "" );
    def.put( "Proxy.Data.Port", "" );
    def.put( "Proxy.Data.Username", "<none>" );
    def.put( "Proxy.Data.Password", "" );

    //old
    def.put( "Start Num Peers", new Long(-1) );
    def.put( "Max Upload Speed", new Long(-1) );
    def.put( "Max Clients", new Long(-1) );
    def.put( "Server.shared.port", TRUE );
    def.put( "Low Port", new Long(6881) );
    def.put( "Already_Migrated", FALSE );
    
    //misc
    def.put( "ID", "" );
    def.put( "Play Download Finished", FALSE );
    def.put( "Play Download Finished File", "" );
    def.put( "Close To Tray", TRUE );
    def.put( "Minimize To Tray", FALSE );
    def.put( "Watch Torrent Folder", FALSE );
    def.put( "Watch Torrent Folder Interval", new Long(1) );
    def.put( "Start Watched Torrents Stopped", FALSE );
    def.put( "Watch Torrent Folder Path", "" );
    def.put( "Prioritize First Piece", FALSE );
    def.put( "Use Lazy Bitfield", FALSE );
    def.put( "Move Completed When Done", FALSE );
    def.put( "Completed Files Directory", "" );
    def.put( "Zero New", FALSE );
    def.put( "Move Only When In Default Save Dir", TRUE );
    def.put( "Copy And Delete Data Rather Than Move", FALSE);
    def.put( "Move Torrent When Done", TRUE );
    def.put( "File.save.peers.enable", TRUE );
    def.put( "File.strict.locking", TRUE );
    def.put( "Move Deleted Data To Recycle Bin", TRUE);
    
    //default torrent directory option
    def.put( "Save Torrent Files", TRUE );
    def.put("General_sDefaultTorrent_Directory", SystemProperties.getUserPath()+"torrents");

  
    def.put( "Bind IP", "" );
    def.put( "Stats Export Peer Details", FALSE );
    def.put( "Stats XSL File", "" );
    def.put( "Stats Enable", FALSE );
    def.put( "Stats Period", new Long(StatsWriterPeriodic.DEFAULT_SLEEP_PERIOD) );
    def.put( "Stats Dir", "" );
    def.put( "Stats File", StatsWriterPeriodic.DEFAULT_STATS_FILE_NAME );
    def.put( "File.Torrent.IgnoreFiles", TOTorrent.DEFAULT_IGNORE_FILES );
    def.put( "File.save.peers.max", new Long( TRTrackerAnnouncer.DEFAULT_PEERS_TO_CACHE ) );
    
    	// tracker 
    
    def.put( "Tracker Compact Enable", TRUE );
    def.put( "Tracker Key Enable Client", TRUE );
    def.put( "Tracker Key Enable Server", TRUE );
    def.put( "Tracker Separate Peer IDs", FALSE);
    def.put( "Tracker Client Connect Timeout", new Long(120));
    def.put( "Tracker Client Read Timeout", new Long(60));
	def.put( "Tracker Client Send OS and Java Version", TRUE);
	def.put( "Tracker Client Show Warnings", TRUE);
	
    def.put( "Tracker Public Enable", FALSE );
    def.put( "Tracker Log Enable", FALSE );
    def.put( "Tracker Port Enable", FALSE );
    def.put( "Tracker Port", new Long( TRHost.DEFAULT_PORT ) );
    def.put( "Tracker Port Backups", "" );
    def.put( "Tracker Port SSL Enable", FALSE );
    def.put( "Tracker Port SSL", new Long( TRHost.DEFAULT_PORT_SSL ) );
    def.put( "Tracker Port SSL Backups", "" );
    def.put( "Tracker Port Force External", FALSE );
    def.put( "Tracker Host Add Our Announce URLs", TRUE );
    def.put( "Tracker IP", "" );
    def.put( "Tracker Port UDP Enable", FALSE );
    def.put( "Tracker Port UDP Version", new Long(2) );
    def.put( "Tracker Send Peer IDs", TRUE );
    def.put( "Tracker Max Peers Returned", new Long(100) );
    def.put( "Tracker Scrape Cache", new Long( TRTrackerServer.DEFAULT_SCRAPE_CACHE_PERIOD ) );
    def.put( "Tracker Announce Cache", new Long( TRTrackerServer.DEFAULT_ANNOUNCE_CACHE_PERIOD ) );
    def.put( "Tracker Announce Cache Min Peers", new Long( TRTrackerServer.DEFAULT_ANNOUNCE_CACHE_PEER_THRESHOLD ) );
    def.put( "Tracker Poll Interval Min", new Long( TRTrackerServer.DEFAULT_MIN_RETRY_DELAY) );
    def.put( "Tracker Poll Interval Max", new Long( TRTrackerServer.DEFAULT_MAX_RETRY_DELAY) );
    def.put( "Tracker Scrape Retry Percentage", new Long( TRTrackerServer.DEFAULT_SCRAPE_RETRY_PERCENTAGE ) );
    def.put( "Tracker Password Enable Web", FALSE );
    def.put( "Tracker Password Web HTTPS Only", FALSE);
    def.put( "Tracker Password Enable Torrent", FALSE );
    def.put( "Tracker Username", "" );
    def.put( "Tracker Password", null );
    def.put( "Tracker Poll Inc By", new Long( TRTrackerServer.DEFAULT_INC_BY ) );
    def.put( "Tracker Poll Inc Per", new Long( TRTrackerServer.DEFAULT_INC_PER ) );
    def.put( "Tracker NAT Check Enable", TRUE);
    def.put( "Tracker NAT Check Timeout", new Long(TRTrackerServer.DEFAULT_NAT_CHECK_SECS));
    def.put( "Tracker Max Seeds Retained", new Long( 0 ) );
    def.put( "Tracker Max Seeds", new Long( 0 ) );
    def.put( "Tracker Max GET Time", new Long(20));
    def.put( "Tracker Max POST Time Multiplier", new Long(1));
    def.put( "Tracker Max Threads", new Long( 48 ));
    def.put( "Tracker TCP NonBlocking", FALSE);
    def.put( "Tracker TCP NonBlocking Conc Max", new Long(2048));
    def.put( "Tracker Client Scrape Enable", TRUE);
    def.put( "Tracker Client Scrape Stopped Enable", TRUE);
    def.put( "Tracker Client Scrape Single Only", FALSE);
    def.put( "Tracker Server Full Scrape Enable", TRUE );
    
    
    def.put( "Network Selection Prompt", TRUE);
    def.put( "Network Selection Default.Public", TRUE);
    def.put( "Network Selection Default.I2P", TRUE);
    def.put( "Network Selection Default.Tor", TRUE);
    def.put( "Tracker Network Selection Default.Public", TRUE);
    def.put( "Tracker Network Selection Default.I2P", TRUE);
    def.put( "Tracker Network Selection Default.Tor", TRUE);
    
    def.put( "Peer Source Selection Default.Tracker", TRUE);
    def.put( "Peer Source Selection Default.DHT", TRUE);
    def.put( "Peer Source Selection Default.PeerExchange", TRUE);
    def.put( "Peer Source Selection Default.Plugin", TRUE);
    def.put( "Peer Source Selection Default.Incoming", TRUE);
    
    def.put( "config.style.useSIUnits", FALSE );
    def.put( "config.style.useUnitsRateBits", FALSE );
    def.put( "config.style.separateProtDataStats", FALSE );
    def.put( "config.style.dataStatsOnly", FALSE );
    
    def.put( "Save Torrent Backup", FALSE );
    
    def.put( "Sharing Protocol", "DHT" );
    def.put( "Sharing Add Hashes", FALSE );
    def.put( "Sharing Rescan Enable", FALSE);
    def.put( "Sharing Rescan Period", new Long(60));
    def.put( "Sharing Torrent Comment", "" );
    def.put( "Sharing Permit DHT", TRUE);
    def.put( "Sharing Torrent Private", FALSE);
	
    def.put( "File.Decoder.Prompt", FALSE );
    def.put( "File.Decoder.Default", "" );
    def.put( "File.Decoder.ShowLax", FALSE);
    def.put( "File.Decoder.ShowAll", FALSE);
    def.put( "Password enabled", FALSE );
    def.put( "Password", null );
    def.put( "Save detail views column widths", FALSE );
    def.put( "config.interface.checkassoc", TRUE );
    def.put( "Wizard Completed", FALSE );
    def.put( "donations.donated", FALSE );
    def.put( "donations.lastVersion", "" );
    def.put( "donations.nextAskTime", new Long(0) );
    def.put( "Color Scheme.red", new Long(0) );
    def.put( "Color Scheme.green", new Long(128) );
    def.put( "Color Scheme.blue", new Long(255) );
    def.put( "Show Splash", TRUE );
    def.put( "window.maximized", TRUE );
    def.put( "window.rectangle", "" );
    def.put( "Open Console", FALSE );
    def.put( "Open Config", FALSE );
    def.put( "Open Stats On Start", FALSE);
    def.put( "Start Minimized", FALSE );
    def.put( "Open Bar", FALSE );
    def.put( "confirmationOnExit", FALSE );
    def.put( "locale", Locale.getDefault().toString() );
    def.put( "locale.set.complete.count", new Long(0));
    def.put( "Add URL Silently", FALSE );
    def.put( "config.style.dropdiraction", "1" );
    def.put( "MyTorrents.SplitAt", new Long(30) );
    def.put( "Confirm Data Delete", TRUE );
    def.put( "Password Confirm", null );
    def.put( "Auto Update", TRUE );
    def.put( "Alert on close", TRUE );
    def.put( "diskmanager.friendly.hashchecking", FALSE );
    def.put( "diskmanager.hashchecking.smallestfirst", TRUE );    
    def.put( "Default Start Torrents Stopped", FALSE);
    def.put( "Server Enable UDP", TRUE);
    def.put( "diskmanager.perf.cache.enable", TRUE);
    def.put( "diskmanager.perf.cache.enable.read", FALSE);
    def.put( "diskmanager.perf.cache.enable.write", TRUE);
    def.put( "diskmanager.perf.cache.size", new Long(4));		// 4 MB
    def.put( "diskmanager.perf.cache.notsmallerthan", new Long(1024));	// 1024 K
    def.put( "diskmanager.perf.read.maxthreads", new Long(32));
    def.put( "diskmanager.perf.read.maxmb", new Long(5));
    def.put( "diskmanager.perf.write.maxthreads", new Long(32));
    def.put( "diskmanager.perf.write.maxmb", new Long(5));
    def.put( "diskmanager.perf.cache.trace", FALSE);
    def.put( "diskmanager.perf.cache.flushpieces", TRUE);
    def.put( "File.truncate.if.too.large", FALSE);
    def.put( "Enable System Tray", TRUE);
    def.put( "config.style.table.defaultSortOrder", new Long(0));
    def.put( "Ignore.peer.ports", "0" );
    def.put( "Security.JAR.tools.dir", "" );
    def.put( "network.max.simultaneous.connect.attempts", new Long( 8 ));
    def.put( "network.tcp.mtu.size", new Long(1500) );
    def.put( "network.udp.mtu.size", new Long(1500) );
    def.put( "network.tcp.socket.SO_SNDBUF", new Long(0) );
    def.put( "network.tcp.socket.SO_RCVBUF", new Long(0) );
    def.put( "network.tcp.socket.IPTOS", "" );
    def.put( "confirm_torrent_removal", FALSE );
    def.put( "add_torrents_silently", FALSE );
    def.put( "enable_small_osx_fonts", TRUE );
    def.put( "Message Popup Autoclose in Seconds", new Long(15));
    def.put( "Play Download Finished Announcement", FALSE);
    def.put( "Play Download Finished Announcement Text", "Download Complete");
    def.put( "Play File Finished", FALSE );
    def.put( "Play File Finished File", "" );
    def.put( "Play File Finished Announcement", FALSE);
    def.put( "Play File Finished Announcement Text", "File Complete");
    
    def.put( "BT Request Max Block Size", new Long(65536));
    def.put( "network.tcp.enable_safe_selector_mode", FALSE );
    
    def.put( "network.transport.encrypted.require", FALSE );
    def.put( "network.transport.encrypted.min_level", "RC4" );
    def.put( "network.transport.encrypted.fallback.outgoing", FALSE );
    def.put( "network.transport.encrypted.fallback.incoming", FALSE );
    def.put( "network.transport.encrypted.use.crypto.port", FALSE );
    
    def.put( "network.bind.local.port", new Long(0) );
   
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
  
  /**
   * Returns the default value as an object (String, Long, Float, Boolean)
   *  
   * @param key
   * @return default value
   */
  public Object getDefaultValueAsObject(String key) {
  	return def.get(key);
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
  
  public boolean doesParameterDefaultExist(String p) {
    return def.containsKey(p);
  }
  
  public Object
  getParameter(
	 String	key )
  {
	return( def.get( key ));  
  }
}
