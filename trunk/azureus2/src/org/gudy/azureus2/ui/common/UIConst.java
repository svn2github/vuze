/*
 * Created on 10.11.2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.gudy.azureus2.ui.common;

import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;

import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.ui.common.util.LegacyHashtable;

/**
 * @author tobi
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class UIConst {
  public static Date startTime;
  public static HashMap UIS = null;
  public static GlobalManager GM = null;
  public static Hashtable parameterlegacy = null;
  
  static {
  	parameterlegacy = new LegacyHashtable();
  	parameterlegacy.put("Core_sOverrideIP", "Override Ip");
  	//parameterlegacy.put("Core_bAllocateNew", "Zero New");
  	parameterlegacy.put("Core_iTCPListenPort", "TCP.Listen.Port");
  	//parameterlegacy.put("Core_iLowPort", "TCP.Listen.Port");
  	//parameterlegacy.put("Core_iHighPort", "High Port");
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
  	parameterlegacy.put("Core_bPriorityExtensionsIgnoreCase", "priorityExtensionsIgnoreCase");
  	parameterlegacy.put("Core_bIpFilterEnabled", "Ip Filter Enabled");
  	parameterlegacy.put("Core_bIpFilterAllow", "Ip Filter Allow");
  	parameterlegacy.put("Core_bAllowSameIPPeers", "Allow Same IP Peers");
  	parameterlegacy.put("Core_bUseSuperSeeding", "Use Super Seeding");
  	parameterlegacy.put("SWT_bUseCustomTab", "useCustomTab");
  	parameterlegacy.put("SWT_iGUIRefresh", "GUI Refresh");
  	parameterlegacy.put("SWT_iGraphicsUpdate", "Graphics Update");
  	parameterlegacy.put("SWT_iReOrderDelay", "ReOrder Delay");
  	parameterlegacy.put("SWT_bSendVersionInfo", "Send Version Info");
  	parameterlegacy.put("SWT_bShowDownloadBasket", "Show Download Basket");
  	parameterlegacy.put("SWT_bAlwaysRefreshMyTorrents", "config.style.refreshMT");
  	parameterlegacy.put("SWT_bOpenDetails", "Open Details");
  }
  
  public static void shutdown() {
    Main.shutdown();
  }
  
  public static synchronized boolean startUI(String ui, String[] args) {
    if (UIS.containsKey(ui))
      return false;
    IUserInterface uif = UserInterfaceFactory.getUI(ui);
    uif.init(false, true);
    if (args!=null)
      uif.processArgs(args);
    uif.startUI();
    UIS.put(ui, uif);
    return true;
  }

}
