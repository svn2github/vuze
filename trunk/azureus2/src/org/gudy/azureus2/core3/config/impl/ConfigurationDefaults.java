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

import org.eclipse.swt.SWT;
import org.gudy.azureus2.core3.util.SystemProperties;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.util.Constants;


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
  public float def_float = 0;
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
    def.put("Switch Priority", new Long(0));
    def.put("priorityExtensions", "");
    def.put("priorityExtensionsIgnoreCase", new Long(0));
    def.put("Ip Filter Enabled", new Long(1));
    def.put("Ip Filter Allow",new Long(0));
    def.put("Allow Same IP Peers",new Long(0));
    def.put("Use Super Seeding",new Long(0));
    def.put("Slow Connect", new Long(1));
        
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
    def.put("Default save path", "");
    def.put("GUI_SWT_bFancyTab", new Long(1));
    def.put("GUI_SWT_bAlternateTablePainting", new Long(0));
    def.put("update.start",new Long(1));
    def.put("update.periodic",new Long(1));
    def.put("update.opendialog",new Long(1));
    
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
