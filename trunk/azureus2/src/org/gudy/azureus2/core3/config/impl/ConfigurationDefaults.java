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
import java.util.Map;
import java.util.Set;

import org.gudy.azureus2.core3.util.SystemProperties;
;

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
    def.put("General_sDefaultSave_Directory", SystemProperties.getUserPath()+"download");
    // Default torrent directory
    def.put("General_sDefaultTorrent_Directory", SystemProperties.getUserPath()+"torrents");
    
    /** Core settings **/
    // (currently only a reference list for me)
    // Override Ip ("Override Ip")
    //def.put("Core_sOverrideIP", "");
    def.put("Override Ip", "");
    //def.put("Core_bAllocateNew", new Long(0));
    //def.put("Zero New", new Long(0));
    //def.put("Core_bIncrementalAllocate", new Long(0)); *
    def.put("Enable incremental file creation", new Long(0));
    // Lower port to use for BT ("Low Port")
    //def.put("Core_iLowPort", new Long(6881));
    //def.put("Low Port", new Long(6881));
    //def.put("Core_iTCPListenPort", new Long(6881));
    def.put("TCP.Listen.Port", new Long(6881));
    // Upper port to use for BT ("High Port")
    //def.put("Core_iHighPort", new Long(6889));
    //def.put("High Port", new Long(6889));
    // Maximal active torrents ("max active torrents")
    //def.put("Core_iMaxActiveTorrents", new Long(4));
    def.put("max active torrents", new Long(4));
    // Maximal active downloads (torrents!=seed) ("max downloads")
    //def.put("Core_iMaxDownloads", new Long(4));
    def.put("max downloads", new Long(4));
    // Maxmail number of connections (0=unlimited) ("Max Clients")
    //def.put("Core_iMaxClients", new Long(0));
    def.put("Max Clients", new Long(100));
    // Default max uploads per torrent ("Max Uploads")
    //def.put("Core_iMaxUploads", new Long(4));
    def.put("Max Uploads", new Long(4));
    // Maximal upload speed (globally, 0=unlimited) ("Max Upload Speed")
    //def.put("Core_iMaxUploadSpeed", new Long(0));
    def.put("Max Upload Speed KBs", new Long(0));
    // Fast Resume
    //def.put("Core_bUseResume", new Long(0));
    def.put("Use Resume", new Long(1));
    // Save Resume Interval (minutes)
    //def.put("Core_iSaveResumeInterval", new Long(5));
    def.put("Save Resume Interval", new Long(5));
    // Recheck pices on completed download
    //def.put("Core_bCheckPiecesOnCompletion", new Long(0)); *
    def.put("Check Pieces on Completion", new Long(1));
    // Stop seeding when share ratio reaches
    //def.put("Core_iSeedingShareStop", new Long(0)); *
    def.put("Stop Ratio", new Long(0));
    // Stop seeding when there is at least 1 seed for X peers
    //def.put("Core_iSeedingRatioStop", new Long(0)); *
    def.put("Stop Peers Ratio", new Long(0));
    // Start seeding when there is less than 1 seed for X peers
    //def.put("Core_iSeedingRatioStart", new Long(0)); *
    def.put("Start Peers Ratio", new Long(0));
    // Disconnect Seeds on completion
    //def.put("Core_bDisconnectSeed", new Long(0)); *
    def.put("Disconnect Seed", new Long(1));
    // Set to low priority on completion
    //def.put("Core_bSwitchPriority", new Long(1)); *
    def.put("Switch Priority", new Long(0));
    // Automatically switch these extensions to high priority (eg ".exe;.txt")
    //def.put("Core_sPriorityExtensions", ""); *
    def.put("priorityExtensions", "");
    // whether priorityExtensions are case insensitive
    //def.put("Core_bPriorityExtensionsIgnoreCase", new Long(0)); *
    def.put("priorityExtensionsIgnoreCase", new Long(0));
    //Use or not the ip filtering feature
    //def.put("Core_bIpFilterEnabled", new Long(1));
    def.put("Ip Filter Enabled", new Long(1));
    //Use the filters as 'allow' rules
    //def.put("Core_bIpFilterAllow",new Long(0));
    def.put("Ip Filter Allow",new Long(0));
    //Allow for multiple peers with the same IP
    //def.put("Core_bAllowSameIPPeers",new Long(0));
    def.put("Allow Same IP Peers",new Long(0));
    //Use Super-seeding
    //def.put("Core_bUseSuperSeeding",new Long(0));
    def.put("Use Super Seeding",new Long(0));
        
    /** SWT GUI Settings **/
    //def.put("SWT_bUseCustomTab",new Long(1));    
    def.put("useCustomTab",new Long(1));    
    //def.put("SWT_iGUIRefresh",new Long(250));
    def.put("GUI Refresh",new Long(250));
    //def.put("SWT_iGraphicsUpdate",new Long(4));
    def.put("Graphics Update",new Long(4));
    //def.put("SWT_iReOrderDelay",new Long(0));
    def.put("ReOrder Delay",new Long(0));
    //def.put("SWT_bSendVersionInfo",new Long(1));
    def.put("Send Version Info",new Long(1));
    //def.put("SWT_bShowDownloadBasket",new Long(0));
    def.put("Show Download Basket",new Long(0));
    //def.put("SWT_bAlwaysRefreshMyTorrents",new Long(0));
    def.put("config.style.refreshMT",new Long(0));
    //def.put("SWT_bOpenDetails", new Long(0));
    def.put("Open Details", new Long(0));
    
    def.put("Logging Enable", new Long(0));
    def.put("Logging Dir", "");
    def.put("Logging Max Size", new Long(0));
    int[] logComponents = { 0, 1, 2, 4 };
    for (int i = 0; i < logComponents.length; i++)
      for (int j = 0; j <= 3; j++)
        def.put("bLog" + logComponents[i] + "-" + j, new Long(1));

    // Start/Stop Automation Stuff
    def.put("StartStopManager_iNumPeersAsFullCopy", new Long(0));
    def.put("StartStopManager_iFakeFullCopySeedStart", new Long(1));
    def.put("StartStopManager_iMinPeersToBoostNoSeeds", new Long(1));
    def.put("StartStopManager_iMinSpeedForActiveDL", new Long(512));
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
  
  public boolean doesParameterExist(String p) {
    return def.containsKey(p);
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

  public void addParameter(String sKey, boolean bParameter) {
    Long lParameter = new Long(bParameter ? 0 : 1);
    def.put(sKey, lParameter);
  }
  
  public void registerExternalDefaults(Map addmap) {
  	def.putAll(addmap);
  }
}
