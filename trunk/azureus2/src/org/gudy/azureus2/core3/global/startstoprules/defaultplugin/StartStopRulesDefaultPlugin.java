/*
 * File    : StartStopRulesDefaultPlugin.java
 * Created : 12-Jan-2004
 * By      : TuxPaper
 *
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.core3.global.startstoprules.defaultplugin;

import java.util.*;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.ui.swt.views.configsections.*;
import org.gudy.azureus2.plugins.ui.tables.mytorrents.*;

import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.TimeFormater;
import org.gudy.azureus2.core3.config.COConfigurationListener;
import org.gudy.azureus2.core3.config.COConfigurationManager;

/** Handles Starting and Stopping of torrents */
public class
StartStopRulesDefaultPlugin
  implements Plugin, COConfigurationListener
{
  // for debugging
  private static final String sStates = " WPRDS.XEQ";

  /** Time to sleep in ms between start/stop checks.
   * May be stopped early by <i>somethingChanged</i>
   */
  int SLEEP_PERIOD = 2000;

  /** Do not rank completed torrents */  
  public static final int RANK_NONE = 0;
  /** Rank completed torrents using Seeds:Peer Ratio */  
  public static final int RANK_SPRATIO = 1;
  /** Rank completed torrents using Seed Count method */  
  public static final int RANK_SEEDCOUNT = 2;
  /** Rank completed torrents using a timed rotation of minTimeAlive */
  public static final int RANK_TIMED = 3;
  
  /** All of the First Priority rules must match */
  public static final int FIRSTPRIORITY_ALL = 0;
  /** Any of the First Priority rules must match */
  public static final int FIRSTPRIORITY_ANY = 1;
  
  private static final int QR_INCOMPLETE_ENDS_AT      = 1000000000; // billion
  private static final int QR_TIMED_QUEUED_ENDS_AT    =   10000000;
  private static final int QR_FIRST_PRIORITY_STARTS_AT=   50000000;
  private static final int QR_NOTQUEUED       = -2;
  private static final int QR_RATIOMET        = -3;
  private static final int QR_NUMSEEDSMET     = -4;
  private static final int QR_0PEERS          = -5;
  private static final int QR_SHARERATIOMET   = -6;

  private PluginInterface     plugin_interface;
  private PluginConfig        plugin_config;
  private DownloadManager     download_manager;

  /** Map to relate downloadData to a Download */  
  private Map downloadDataMap = Collections.synchronizedMap(new HashMap());

  private volatile boolean         closingDown;
  private volatile boolean         somethingChanged;

  private LoggerChannel   log;
  private long lastQRcalcTime = 0;
  private long RECALC_QR_EVERY = 15 * 1000;
  private long startedOn;

  // Config Settings
  int minPeersToBoostNoSeeds;
  int minSpeedForActiveDL;
  // count x peers as a full copy, but..
  int numPeersAsFullCopy;
  // don't count x peers as a full copy if seeds below
  int iFakeFullCopySeedStart;
  int maxActive;
  int maxDownloads;

  // Ignore torrent if seed count is at least..
  int     iIgnoreSeedCount;
  // Ignore even when First Priority
  boolean bIgnore0Peers;
  int     iIgnoreShareRatio;
  int     iIgnoreRatioPeers;
  int iIgnoreRatioPeers_SeedStart;

  int iRankType;
  int iRankTypeSeedFallback;
  boolean bAutoReposition;
  long minTimeAlive;
  
  boolean bPreferLargerSwarms;
  boolean bDebugLog;
  int minQueueingShareRatio;
  int iFirstPriorityType;
  int iFirstPrioritySeedingMinutes;
  int iFirstPriorityDLMinutes;

  public void
  initialize(
    PluginInterface _plugin_interface )
  {
    startedOn = System.currentTimeMillis();

    plugin_interface  = _plugin_interface;

    plugin_interface.addListener(new PluginListener() {
      public void initializationComplete() { /* not implemented */ }

      public void closedownInitiated() {
        closingDown = true;
      }

      public void closedownComplete() { /* not implemented */ }
    });

    log = plugin_interface.getLogger().getChannel("StartStopRules");
    log.log( LoggerChannel.LT_INFORMATION, "Default StartStopRules Plugin Initialisation" );

    COConfigurationManager.addListener(this);

    plugin_config = plugin_interface.getPluginconfig();
    reloadConfigParams();

    try {
      plugin_interface.addColumnToMyTorrentsTable("SeedingRank", new SeedingRankColumn());
      plugin_interface.addConfigSection(new ConfigSectionQueue());
      plugin_interface.addConfigSection(new ConfigSectionSeeding());
      plugin_interface.addConfigSection(new ConfigSectionSeedingAutoStarting());
      plugin_interface.addConfigSection(new ConfigSectionSeedingFirstPriority());
      plugin_interface.addConfigSection(new ConfigSectionSeedingIgnore());
    } catch (NoClassDefFoundError e) {
      /* Ignore. SWT probably not installed */
      log.log(LoggerChannel.LT_WARNING,
              "SWT UI Config not loaded for StartStopRulesDefaulPlugin. " +
              e.getMessage() + " not found.");
    } catch( Throwable e ){
      e.printStackTrace();
    }

    download_manager = plugin_interface.getDownloadManager();
    download_manager.addListener(new StartStopDMListener());

    // initial implementation loops - change to event driven maybe although
    // the current rules permit loops under certain circumstances.....
    Thread t = new Thread("StartStopRulesDefaultPlugin") {
      public void run() {
        while(true) {
          try {
            if ( closingDown ) {
              log.log( LoggerChannel.LT_INFORMATION, "System Closing - processing stopped" );
              break;
            }
            
            if (positionsChanged()) {
              somethingChanged = true;
            }              

            process();

          } catch( Exception e ) {
            e.printStackTrace();
          }

          try{
            // sleep for 200ms, check if somethingChanged, then sleep
            // more, until sleep_period
            for (int i = 0; i < SLEEP_PERIOD / 200; i++) {
              if (somethingChanged) {
                break;
              }
              Thread.sleep(200);
            }

          } catch( InterruptedException e ){
            e.printStackTrace();
          }
        }
      }
    };
    t.setDaemon(true);
    t.start();
  }
  
  private class StartStopDownloadListener implements DownloadListener
  {
    public void stateChanged(Download download, int old_state, int new_state) {
      downloadData dlData = (downloadData)downloadDataMap.get(download);

      if (dlData != null) {
        // force a QR recalc, so that it gets positiong properly next process()
        dlData.recalcQR();
        somethingChanged = true;
      }
    }
  }

  /** Update QR when a new scrape result comes in. 
   */
  private class StartStopDMTrackerListener implements DownloadTrackerListener
  {
  	public void scrapeResult( DownloadScrapeResult result ) {
      downloadData dlData = (downloadData)downloadDataMap.get(result.getDownload());
      if (dlData != null) {
        dlData.recalcQR();
        somethingChanged = true;
      }
  	}
  	
  	public void announceResult( DownloadAnnounceResult result ) {
  	}
  }

  /* Create/Remove downloadData object when download gets added/removed.
   * RecalcQR & process if necessary.
   */
  private class StartStopDMListener implements DownloadManagerListener
  {
    private DownloadTrackerListener download_tracker_listener;
    private DownloadListener        download_listener;
    
    public StartStopDMListener() {
      download_tracker_listener = new StartStopDMTrackerListener();
      download_listener = new StartStopDownloadListener();
    }

    public void downloadAdded( Download  download )
    {
      downloadData dlData = null;
      if (downloadDataMap.containsKey(download)) {
        dlData = (downloadData)downloadDataMap.get(download);
      } else {
        dlData = new downloadData(download);
        downloadDataMap.put( download, dlData );
        download.addListener( download_listener );
        download.addTrackerListener( download_tracker_listener );
      }

      if (dlData != null) {
        dlData.recalcQR();
        somethingChanged = true;
      }
    }

    public void downloadRemoved( Download  download )
    {
      download.removeListener( download_listener );
      download.removeTrackerListener( download_tracker_listener );

      if (downloadDataMap.containsKey(download)) {
        downloadDataMap.remove(download);
      }

      somethingChanged = true;
    }
  }

  /* Check to see if any of the torrents' positions have changed.  If changed,
   * recalcQR on torrents.
   *
   * @return Whether any positions changed
   */
  private boolean positionsChanged() {
    boolean bPositionsChanged = false;
    downloadData[] dlDataArray;
    dlDataArray = (downloadData[])
      downloadDataMap.values().toArray(new downloadData[downloadDataMap.size()]);

    for (int i = 0; i < dlDataArray.length; i++) {
      downloadData dl_data = dlDataArray[i];
      
      int iNewPosition = dl_data.getDownloadObject().getPosition();
      int iOldPosition = dl_data.getDLPosition();
      if (iNewPosition != iOldPosition) {
        dl_data.setDLPosition(iNewPosition);
        bPositionsChanged = true;
      }
    }
    // we don't need to recalc QR for RANK_NONE because it doesn't use QR for
    // sorting
    if (bPositionsChanged && iRankType != RANK_NONE) {
      for (int i = 0; i < dlDataArray.length; i++) {
        if (dlDataArray[i].getQR() > QR_INCOMPLETE_ENDS_AT - 10000 ||
            dlDataArray[i].getDownloadObject().getStats().getDownloadCompleted(false) < 1000) {
          dlDataArray[i].recalcQR();
        }
      }
    }
      
    return bPositionsChanged;
  }

  // ConfigurationListener
  public void configurationSaved() {
    reloadConfigParams();
  }

  private synchronized void reloadConfigParams() {
    int iOldIgnoreShareRatio = iIgnoreShareRatio;
    int iNewRankType = plugin_config.getIntParameter("StartStopManager_iRankType");
    minPeersToBoostNoSeeds = plugin_config.getIntParameter("StartStopManager_iMinPeersToBoostNoSeeds");
    minSpeedForActiveDL = plugin_config.getIntParameter("StartStopManager_iMinSpeedForActiveDL");
    maxActive = plugin_config.getIntParameter("max active torrents");
    maxDownloads = plugin_config.getIntParameter("max downloads");
    numPeersAsFullCopy = plugin_config.getIntParameter("StartStopManager_iNumPeersAsFullCopy");
    iFakeFullCopySeedStart = plugin_config.getIntParameter("StartStopManager_iFakeFullCopySeedStart");
    iRankTypeSeedFallback = plugin_config.getIntParameter("StartStopManager_iRankTypeSeedFallback");
    bAutoReposition = plugin_config.getBooleanParameter("StartStopManager_bAutoReposition");
    minTimeAlive = plugin_config.getIntParameter("StartStopManager_iMinSeedingTime") * 1000;
    bPreferLargerSwarms = plugin_config.getBooleanParameter("StartStopManager_bPreferLargerSwarms");
    bDebugLog = plugin_config.getBooleanParameter("StartStopManager_bDebugLog");

    // Ignore torrent if seed count is at least..
    iIgnoreSeedCount = plugin_config.getIntParameter("StartStopManager_iIgnoreSeedCount");
    bIgnore0Peers = plugin_config.getBooleanParameter("StartStopManager_bIgnore0Peers");
    iIgnoreShareRatio = (int)(1000 * plugin_config.getFloatParameter("Stop Ratio"));
    iIgnoreRatioPeers = plugin_config.getIntParameter("Stop Peers Ratio", 0);
    iIgnoreRatioPeers_SeedStart = plugin_config.getIntParameter("StartStopManager_iIgnoreRatioPeersSeedStart", 0);

    minQueueingShareRatio = plugin_config.getIntParameter("StartStopManager_iFirstPriority_ShareRatio");
    iFirstPriorityType = plugin_config.getIntParameter("StartStopManager_iFirstPriority_Type");
    iFirstPrioritySeedingMinutes = plugin_config.getIntParameter("StartStopManager_iFirstPriority_SeedingMinutes");
    iFirstPriorityDLMinutes = plugin_config.getIntParameter("StartStopManager_iFirstPriority_DLMinutes");


    if (iNewRankType != iRankType || iIgnoreShareRatio != iOldIgnoreShareRatio) {
      iRankType = iNewRankType;
      
      // shorted recalc for timed rank type, since the calculation is fast and we want to stop on the second
      if (iRankType == RANK_TIMED)
        RECALC_QR_EVERY = 1000;
      else
        RECALC_QR_EVERY = 1000 * 15;
      lastQRcalcTime = 0;
      downloadData[] dlDataArray;
      dlDataArray = (downloadData[])
        downloadDataMap.values().toArray(new downloadData[downloadDataMap.size()]);
      for (int i = 0; i < dlDataArray.length; i++) {
        dlDataArray[i].setQR(0);
      }
    }
    somethingChanged = true;
  }
  
  private int calcMaxSeeders(int iDLs) {
    // XXX put in subtraction logic here
    return (maxActive == 0) ? 99999 : maxActive - iDLs;
  }

  protected synchronized void process() {
    long  process_time = System.currentTimeMillis();

    int totalSeeding = 0;
    int totalForcedSeeding = 0;
    int totalWaitingToSeed = 0;
    int totalWaitingToDL = 0;
    int totalDownloading = 0;
    int activeDLCount = 0;
    int totalComplete = 0;
    int totalCompleteQueued = 0;
    int totalIncompleteQueued = 0;
    int totalFirstPriority = 0;

    boolean recalcQR = ((process_time - lastQRcalcTime) > RECALC_QR_EVERY);
	  if (recalcQR)
  	  lastQRcalcTime = System.currentTimeMillis();

    // pull the data into an local array, so we don't have to lock/synchronize
    downloadData[] dlDataArray;
    dlDataArray = (downloadData[])
      downloadDataMap.values().toArray(new downloadData[downloadDataMap.size()]);

    // Start seeding right away if there's no auto-ranking
    // Otherwise, wait a maximium of 90 seconds for scrape results to come in
    // When the first scrape result comes in, bSeedHasRanking will turn to true
    // (see logic in 1st loop)
    boolean bSeedHasRanking = (iRankType == RANK_NONE) || 
                              (iRankType == RANK_TIMED) || 
                              (System.currentTimeMillis() - startedOn > 90000);

    // Loop 1 of 2:
    // - Build a QR list for sorting
    // - Build Count Totals
    // - Do anything that doesn't need to be done in Queued order
    for (int i = 0; i < dlDataArray.length; i++) {
      downloadData dl_data = dlDataArray[i];
      
      int qr = (recalcQR) ? dl_data.recalcQR() : dl_data.getQR();

      Download download = dl_data.getDownloadObject();
      int completionLevel = download.getStats().getDownloadCompleted(false);

      // Count forced seedings as using a slot
      // Don't count forced downloading as using a slot
      if (completionLevel < 1000 && download.isForceStart())
        continue;

      int state = download.getState();

      boolean bActivelyDownloading = false;
      if (state == Download.ST_DOWNLOADING) {
        totalDownloading++;
        // Only increase activeDLCount if there's downloading
        // or if the torrent just recently started (ie. give it a chance to get some connections)
        if ((download.getStats().getDownloadAverage() >= minSpeedForActiveDL) ||
            (System.currentTimeMillis() - download.getStats().getTimeStarted() <= 30000)) {
          bActivelyDownloading = true;
          activeDLCount++;
        }
      }
      // since it's based on time, store in dl_data.
      // we check ActivelyDownloding later and want to use the same value
      // calculated here
      dl_data.setActivelyDownloading(bActivelyDownloading);

      if (!bSeedHasRanking && 
          (completionLevel == 1000) && 
          (qr > 0) && 
          (state == Download.ST_QUEUED ||
           state == Download.ST_READY)) {
        bSeedHasRanking = true;
      }

      // All of these are either seeding or about to be seeding
      if (completionLevel == 1000) {
        totalComplete++;
        if (state == Download.ST_READY ||
            state == Download.ST_WAITING ||
            state == Download.ST_PREPARING)
          totalWaitingToSeed++;
        else if (state == Download.ST_SEEDING) {
          totalSeeding++;
          if (download.isForceStart())
            totalForcedSeeding++;
        }
        else if (state == Download.ST_QUEUED)
          totalCompleteQueued++;

        if (dl_data.isFirstPriority()) {
          totalFirstPriority++;
          bSeedHasRanking = true;
        }
      } else {
        if (state == Download.ST_READY ||
            state == Download.ST_WAITING ||
            state == Download.ST_PREPARING) {
          totalWaitingToDL++;
        } else if (state == Download.ST_QUEUED) {
          totalIncompleteQueued++;
        }
      }
    }

    int maxSeeders = calcMaxSeeders(activeDLCount + totalWaitingToDL);
    int iExtraFPs = (maxActive != 0) && (maxDownloads != 0) && 
                    (maxDownloads + totalFirstPriority - maxActive) > 0 ? (maxDownloads + totalFirstPriority - maxActive) 
                                                                        : 0;

    // We can also quit early if:
    // - we don't have any torrents waiting (these have to either be started, queued, or stopped)
    // - We match the limits for DL & Seeding
    // - We have less than the limits for DL &/or seeding, but there are no other torrents in the queue
    boolean quitEarly = (iRankType != RANK_NONE) &&
                        ((totalSeeding == maxSeeders) ||
                         (totalSeeding < maxSeeders && totalCompleteQueued == 0)
                        ) &&
                        (totalWaitingToSeed == 0) &&
                        ((totalDownloading == maxDownloads) ||
                         (totalDownloading < maxDownloads && totalIncompleteQueued == 0)
                        ) &&
                        (totalWaitingToDL == 0) &&
                        (!recalcQR) &&
                        (!somethingChanged);

    String[] mainDebugEntries = null;
    if (bDebugLog) {
      log.log(LoggerChannel.LT_INFORMATION, ">>process()" + 
                                            (recalcQR ? " recalc all ranks" : "") +
                                            (quitEarly ? " Nothing to do, quitting early" : ""));
      mainDebugEntries = new String[] { 
              "somethingChanged="+somethingChanged,
              "bCdHasRank="+bSeedHasRanking,
              "tCding="+totalSeeding,
              "tFrcdCding="+totalForcedSeeding,
              "tW8tingToCd="+totalWaitingToSeed,
              "tDLing="+totalDownloading,
              "activeDLs="+activeDLCount,
              "tW8tingToDL="+totalWaitingToDL,
              "tCom="+totalComplete,
              "tComQd="+totalCompleteQueued,
              "tIncQd="+totalIncompleteQueued,
              "mxCdrs="+maxSeeders,
              "t1stPr="+totalFirstPriority
                      };
    }

    if (quitEarly) {
      if (bDebugLog) {
        String[] mainDebugEntries2 = new String[] { 
            "somethingChanged="+somethingChanged,
            "bCdHasRank="+bSeedHasRanking,
            "tCding="+totalSeeding,
            "tFrcdCding="+totalForcedSeeding,
            "tW8tingToCd="+totalWaitingToSeed,
            "tDLing="+totalDownloading,
            "activeDLs="+activeDLCount,
            "tW8tingToDL="+totalWaitingToDL,
            "tCom="+totalComplete,
            "tComQd="+totalCompleteQueued,
            "tIncQd="+totalIncompleteQueued,
            "mxCdrs="+maxSeeders,
            "t1stPr="+totalFirstPriority
                    };
        printDebugChanges("<<process() ", mainDebugEntries, mainDebugEntries2, "", "", true);
      }
      return;
    }

    if (somethingChanged) {
    	somethingChanged = false;
    }

    // Sort by QR
    if (iRankType != RANK_NONE)
      Arrays.sort(dlDataArray);
    else
      Arrays.sort(dlDataArray, new Comparator () {
        public final int compare (Object a, Object b) {
          Download aDL = ((downloadData)a).getDownloadObject();
          Download bDL = ((downloadData)b).getDownloadObject();
          boolean aIsComplete = aDL.getStats().getDownloadCompleted(false) == 1000;
          boolean bIsComplete = bDL.getStats().getDownloadCompleted(false) == 1000;
          if (aIsComplete && !bIsComplete)
            return 1;
          if (!aIsComplete && bIsComplete)
            return -1;
          boolean aIsFP = ((downloadData)a).isFirstPriority();
          boolean bIsFP = ((downloadData)b).isFirstPriority();
          if (aIsFP && !bIsFP)
            return -1;
          if (!aIsFP && bIsFP)
            return 1;
          return aDL.getPosition() - bDL.getPosition();
        }
      } );

    int numWaitingOrSeeding = totalForcedSeeding; // Running Count
    int numWaitingOrDLing = 0;   // Running Count
    /**
     * store whether there's a torrent higher in the list that is queued
     * We don't want to start a torrent lower in the list if there's a higherQueued
     */
    boolean higherQueued = false;
    /**
     * Tracks the position we should be at in the Completed torrents list
     * Updates position.
     */
    int posComplete = 0;
    int iSeedingPos = 0;

    // Loop 2 of 2:
    // - Start/Stop torrents based on criteria
    for (int i = 0; i < dlDataArray.length; i++) {
      downloadData dl_data = dlDataArray[i];
      Download download = dl_data.getDownloadObject();
      boolean bStopAndQueued = false;

      // Initialize STATE_WAITING torrents
      if ((download.getState() == Download.ST_WAITING) &&
          !getAlreadyAllocatingOrChecking()) {
        try{
          download.initialize();
        }catch (Exception ignore) {/*ignore*/}
      }

      if (bAutoReposition &&
          (iRankType != RANK_NONE) &&
          download.getStats().getDownloadCompleted(false) == 1000 &&
          bSeedHasRanking)
        download.setPosition(++posComplete);

      if (download.getStats().getDownloadCompleted(false) == 1000)
        dl_data.setSeedingPos(++iSeedingPos);

      // Never do anything to stopped entries
      if (download.getState() == Download.ST_STOPPING ||
          download.getState() == Download.ST_STOPPED ||
          download.getState() == Download.ST_ERROR) {
        continue;
      }

      // Handle incomplete DLs
      if (download.getStats().getDownloadCompleted(false) != 1000) {
        if (bDebugLog)
          log.log(LoggerChannel.LT_INFORMATION, 
                  ">> "+download.getTorrent().getName()+
                  "]: state="+sStates.charAt(download.getState())+
                  ";shareRatio="+download.getStats().getShareRatio()+
                  ";numW8tngorDLing="+numWaitingOrDLing+
                  ";maxCDrs="+maxSeeders+
                  ";forced="+download.isForceStart()+
                  ";forcedStart="+download.isForceStart()+
                  ";activeDLCount="+activeDLCount+
                  "");

        if (download.isForceStart())
          continue;
          
        int state = download.getState();
        if (state == Download.ST_PREPARING) {
          // Don't mess with preparing torrents.  they could be in the 
          // middle of resume-data building, or file allocating.
          numWaitingOrDLing++;

        } else if (state == Download.ST_READY ||
                   state == Download.ST_DOWNLOADING ||
                   state == Download.ST_WAITING) {

          boolean bActivelyDownloading = dl_data.getActivelyDownloading();

          // Stop torrent if over limit
          if ((maxDownloads != 0) &&
              (numWaitingOrDLing >= maxDownloads - iExtraFPs) &&
              (bActivelyDownloading || state != Download.ST_DOWNLOADING)) {
            try {
              if (bDebugLog)
                log.log(LoggerChannel.LT_INFORMATION, "   stopAndQueue() > maxDownloads");
              download.stopAndQueue();
              // reduce counts
              if (state == Download.ST_DOWNLOADING) {
                totalDownloading--;
                if (bActivelyDownloading)
                  activeDLCount--;
              } else {
                totalWaitingToDL--;
              }
              maxSeeders = calcMaxSeeders(activeDLCount + totalWaitingToDL);
            } catch (Exception ignore) {/*ignore*/}
            
            state = download.getState();
          } else if (bActivelyDownloading) {
            numWaitingOrDLing++;
          }
        }

        if (state == Download.ST_QUEUED) {
          if ((maxDownloads == 0) || (numWaitingOrDLing < maxDownloads - iExtraFPs)) {
            try {
              if (bDebugLog)
                log.log(LoggerChannel.LT_INFORMATION, "   restart()");
              download.restart();

              // increase counts
              totalWaitingToDL++;
              numWaitingOrDLing++;
              maxSeeders = calcMaxSeeders(activeDLCount + totalWaitingToDL);
            } catch (Exception ignore) {/*ignore*/}
            state = download.getState();
          }
        }

        if (state == Download.ST_READY) {
          if ((maxDownloads == 0) || (activeDLCount < maxDownloads - iExtraFPs)) {
            try {
              if (bDebugLog)
                log.log(LoggerChannel.LT_INFORMATION, "   start() activeDLCount < maxDownloads");
              download.start();

              // adjust counts
              totalWaitingToDL--;
              activeDLCount++;
              numWaitingOrDLing++;
              maxSeeders = calcMaxSeeders(activeDLCount + totalWaitingToDL);
            } catch (Exception ignore) {/*ignore*/}
            state = download.getState();
          }
        }

        if (bDebugLog)
          log.log(LoggerChannel.LT_INFORMATION, 
                  "<< "+download.getTorrent().getName()+
                  "]: state="+sStates.charAt(download.getState())+
                  ";shareRatio="+download.getStats().getShareRatio()+
                  ";numW8tngorDLing="+numWaitingOrDLing+
                  ";maxCDrs="+maxSeeders+
                  ";forced="+download.isForceStart()+
                  ";forcedStart="+download.isForceStart()+
                  ";activeDLCount="+activeDLCount+
                  "");
      }
      else if (bSeedHasRanking) { // completed
        String[] debugEntries = null;
        String sDebugLine = "";
        // Queuing process:
        // 1) Torrent is Queued (Stopped)
        // 2) Slot becomes available
        // 3) Queued Torrent changes to Waiting
        // 4) Waiting Torrent changes to Ready
        // 5) Ready torrent changes to Seeding (with startDownload)
        // 6) Trigger stops Seeding torrent
        //    a) Queue Ranking drops
        //    b) User pressed stop
        //    c) other
        // 7) Seeding Torrent changes to Queued.  Go to step 1.

        int shareRatio = download.getStats().getShareRatio();
        int state = download.getState();
        boolean okToQueue = (state == Download.ST_READY || state == Download.ST_SEEDING) &&
                            (!download.isForceStart()) &&
                            (!dl_data.isFirstPriority());
        // in RANK_TIMED mode, we use minTimeAlive for rotation time, so
        // skip check
        if (okToQueue && (state == Download.ST_SEEDING) && iRankType != RANK_TIMED) {
          long timeAlive = (System.currentTimeMillis() - download.getStats().getTimeStarted());
          okToQueue = (timeAlive >= minTimeAlive);
        }
        
        if (state == Download.ST_READY ||
            state == Download.ST_SEEDING ||
            state == Download.ST_WAITING ||
            state == Download.ST_PREPARING)
          numWaitingOrSeeding++;

        if (bDebugLog) {
          debugEntries = new String[] { "state="+sStates.charAt(state),
                           "shareR="+shareRatio,
                           "numWorCDing="+numWaitingOrSeeding,
                           "numWorDLing="+numWaitingOrDLing,
                           "okToQ="+okToQueue,
                           "qr="+dl_data.getQR(),
                           "hgherQd="+higherQueued,
                           "maxCDrs="+maxSeeders,
                           "1stPriority="+dl_data.isFirstPriority()
                          };
        }
        
        // Note: First Priority have the highest QR, so they will always start first
        
        // ignore when Share Ratio reaches # in config
        //0 means unlimited
        if (iIgnoreShareRatio != 0 && 
            shareRatio > iIgnoreShareRatio && 
            shareRatio != -1 &&
            dl_data.getQR() != QR_SHARERATIOMET) {
          if (bDebugLog)
            sDebugLine += "\nShare Ratio Met";
          dl_data.setQR(QR_SHARERATIOMET);
        }

        if (okToQueue && (iIgnoreRatioPeers != 0) && dl_data.getQR() != QR_RATIOMET) {
          int numSeeds = calcSeedsNoUs(download);
          int numPeers = calcPeersNoUs(download);
          if (numPeersAsFullCopy != 0 && numSeeds >= iFakeFullCopySeedStart)
              numSeeds += numPeers / numPeersAsFullCopy;
          //If there are no seeds, avoid / by 0
          if (numSeeds != 0 && numSeeds >= iIgnoreRatioPeers_SeedStart) {
            float ratio = (float) numPeers / numSeeds;
            if (ratio <= iIgnoreRatioPeers) {
              sDebugLine += "\nP:S Met";
              dl_data.setQR(QR_RATIOMET);
            }
          }
        }
        
        // Change to waiting if queued and we have an open slot
        if ((state == Download.ST_QUEUED) &&
            (numWaitingOrSeeding < maxSeeders) && 
            (dl_data.getQR() > -2) && 
            !higherQueued) {
          try {
            if (bDebugLog)
              sDebugLine += "\nrestart() numWaitingOrSeeding < maxSeeders";
            download.restart(); // set to Waiting
            okToQueue = false;
            totalWaitingToSeed++;
            numWaitingOrSeeding++;
          } catch (Exception ignore) {/*ignore*/}
          state = download.getState();
        }

        if (state == Download.ST_READY && totalSeeding < maxSeeders) {

          if (dl_data.getQR() > -2) {
            try {
              if (bDebugLog)
                sDebugLine += "\nstart(); totalSeeding < maxSeeders";
              download.start();
              okToQueue = false;
            } catch (Exception ignore) {/*ignore*/}
            state = download.getState();
            totalSeeding++;
          }
          else if (okToQueue) {
            // In between switching from STATE_WAITING and STATE_READY,
            // Stop Ratio was met, so move it back to Queued
            try {
              if (bDebugLog)
                sDebugLine += "\nstopAndQueue()";
              download.stopAndQueue();
              bStopAndQueued = true;
              totalWaitingToSeed--;
              numWaitingOrSeeding--;
            } catch (Exception ignore) {/*ignore*/}
            state = download.getState();
          }
        }


        // if there's more torrents waiting/seeding than our max, or if
        // there's a higher ranked torrent queued, stop this one
        if (okToQueue &&
            ((numWaitingOrSeeding > maxSeeders) || higherQueued || dl_data.getQR() <= -2)) {
          try {
            if (bDebugLog) {
              sDebugLine += "\nstopAndQueue()";
              if (numWaitingOrSeeding > maxSeeders)
                sDebugLine += "; > Max";
              if (higherQueued)
                sDebugLine += "; higherQueued (it should be seeding instead of this one)";
              if (dl_data.getQR() <= -2)
                sDebugLine += "; ignoreRule met";
            }

            if (state == Download.ST_READY)
              totalWaitingToSeed--;

            download.stopAndQueue();
            bStopAndQueued = true;
            // okToQueue only allows READY and SEEDING state.. and in both cases
            // we have to reduce counts
            numWaitingOrSeeding--;
          } catch (Exception ignore) {/*ignore*/}
          state = download.getState();
        }

        // move completed timed rank types to bottom of the list
        if (bStopAndQueued && iRankType == RANK_TIMED) {
          for (int j = 0; j < dlDataArray.length; j++) {
            int qr = dlDataArray[j].getQR();
            if (qr > 0 && qr < QR_TIMED_QUEUED_ENDS_AT) {
              // Move everyone up
              // We always start by setting QR to QR_TIMED_QUEUED_ENDS_AT - position
              // then, the torrent with the biggest starts seeding which is
              // (QR_TIMED_QUEUED_ENDS_AT - 1), leaving a gap.
              // when it's time to stop the torrent, move everyone up, and put 
              // us at the end
              dlDataArray[j].setQR(qr + 1);
            }
          }
          dl_data.setQR(QR_TIMED_QUEUED_ENDS_AT - totalComplete);
        }

        if (download.getState() == Download.ST_QUEUED && dl_data.getQR() >= 0)
          higherQueued = true;

        if (bDebugLog) {
          String[] debugEntries2 = new String[] { "state="+sStates.charAt(download.getState()),
                           "shareR="+download.getStats().getShareRatio(),
                           "numWorCDing="+numWaitingOrSeeding,
                           "numWorDLing="+numWaitingOrDLing,
                           "okToQ="+okToQueue,
                           "qr="+dl_data.getQR(),
                           "hgherQd="+higherQueued,
                           "maxCDrs="+maxSeeders,
                           "1stPriority="+dl_data.isFirstPriority()
                          };
          printDebugChanges(download.getName() + "] ", debugEntries, debugEntries2, sDebugLine, "  ", true);
        }

      } // getDownloadCompleted == 1000
    } // Loop 2/2 (Start/Stopping)
    
    if (bDebugLog) {
      String[] mainDebugEntries2 = new String[] { 
          "somethingChanged="+somethingChanged,
          "bCdHasRank="+bSeedHasRanking,
          "tCding="+totalSeeding,
          "tFrcdCding="+totalForcedSeeding,
          "tW8tingToCd="+totalWaitingToSeed,
          "tDLing="+totalDownloading,
          "activeDLs="+activeDLCount,
          "tW8tingToDL="+totalWaitingToDL,
          "tCom="+totalComplete,
          "tComQd="+totalCompleteQueued,
          "tIncQd="+totalIncompleteQueued,
          "mxCdrs="+maxSeeders,
          "t1stPr="+totalFirstPriority
                  };
      printDebugChanges("<<process() ", mainDebugEntries, mainDebugEntries2, "", "", true);
    }

  } // process()
  
  private void printDebugChanges(String sPrefixFirstLine, 
                                 String[] oldEntries, 
                                 String[] newEntries,
                                 String sDebugLine,
                                 String sPrefix, 
                                 boolean bAlwaysPrintNoChangeLine) {
      boolean bAnyChanged = false;
      String sDebugLineNoChange = sPrefixFirstLine;
      String sDebugLineOld = "";
      String sDebugLineNew = "";
      for (int j = 0; j < oldEntries.length; j++) {
        if (oldEntries[j].equals(newEntries[j]))
          sDebugLineNoChange += oldEntries[j] + ";";
        else {
          sDebugLineOld += oldEntries[j] + ";";
          sDebugLineNew += newEntries[j] + ";";
          bAnyChanged = true;
        }
      }
      String sDebugLineOut = ((bAlwaysPrintNoChangeLine || bAnyChanged) ? sDebugLineNoChange : "") +
                             (bAnyChanged ? "\nOld:"+sDebugLineOld+"\nNew:"+sDebugLineNew : "") + 
                             sDebugLine;
      if (!sDebugLineOut.equals("")) {
        String[] lines = sDebugLineOut.split("\n");
        for (int i = 0; i < lines.length; i++) {
          log.log(LoggerChannel.LT_INFORMATION, sPrefix + ((i>0)?"  ":"") + lines[i]);
        }
      }
  }

  public boolean getAlreadyAllocatingOrChecking() {
    Download[]  downloads = download_manager.getDownloads(false);
    for (int i=0;i<downloads.length;i++){
      Download  download = downloads[i];
      int state = download.getState();
      if (state == Download.ST_PREPARING)
        return true;
    }
    return false;
  }



  /*
   * Get # of peers not including us
   *
   * I don't trust AccounceResult.getReportedPeerCount because we pass
   * num_peers=50 in the URL.. which means we only get 50 back (??)
   *
  */
  public int calcPeersNoUs(Download download) {
    int numPeers = 0;
    DownloadScrapeResult sr = download.getLastScrapeResult();
    if (sr.getScrapeStartTime() > 0) {
      numPeers = sr.getNonSeedCount();
      // If we've scraped after we started downloading
      // Remove ourselves from count
      if ((numPeers > 0) &&
          (download.getState() == Download.ST_DOWNLOADING) &&
          (sr.getScrapeStartTime() > download.getStats().getTimeStarted()))
        numPeers--;
    }
    if (numPeers == 0) {
      DownloadAnnounceResult ar = download.getLastAnnounceResult();
      if (ar != null && ar.getResponseType() == DownloadAnnounceResult.RT_SUCCESS)
        numPeers = ar.getNonSeedCount();
    }
    return numPeers;
  }
  
  public boolean scrapeResultOk(Download download) {
    DownloadScrapeResult sr = download.getLastScrapeResult();
    return (sr.getResponseType() == DownloadScrapeResult.RT_SUCCESS);
  }

  public int calcSeedsNoUs(Download download) {
    int numSeeds = 0;
    DownloadScrapeResult sr = download.getLastScrapeResult();
    if (sr.getScrapeStartTime() > 0) {
      long seedingStartedOn = download.getStats().getTimeStartedSeeding();
      numSeeds = sr.getSeedCount();
      // If we've scraped after we started seeding
      // Remove ourselves from count
      if ((numSeeds > 0) &&
          (seedingStartedOn > 0) &&
          (download.getState() == Download.ST_SEEDING) &&
          (sr.getScrapeStartTime() > seedingStartedOn))
        numSeeds--;
    }
    if (numSeeds == 0) {
      DownloadAnnounceResult ar = download.getLastAnnounceResult();
      if (ar != null && ar.getResponseType() == DownloadAnnounceResult.RT_SUCCESS)
        numSeeds = ar.getSeedCount();
    }
    return numSeeds;
  }


  private class downloadData implements Comparable
  {
    protected int qr;
    protected int iSeedingPos;
    protected Download dl;
    private boolean bActivelyDownloading;
    private int iDLPos;
    
    /** Sort first by QR Descending, then by Position Ascending.
      */
    public int compareTo(Object obj)
    {
      int value = ((downloadData)obj).getQR() - qr;
      if (value == 0) {
        return dl.getPosition() -
               ((downloadData)obj).getDownloadObject().getPosition();
      }
      return value;
    }

    public downloadData(Download _dl)
    {
      dl = _dl;
      iDLPos = dl.getPosition();
      iSeedingPos = 100000 - iDLPos;
      //recalcQR();
    }
    
    public int getDLPosition() {
      return iDLPos;
    }

    public void setDLPosition(int iPos) {
      iDLPos = iPos;
    }

    public int getSeedingPos() {
      return iSeedingPos;
    }
    
    public void setSeedingPos(int iPos) {
      iSeedingPos = iPos;
    }
    
    Download getDownloadObject() {
      return dl;
    }
    
    public boolean getActivelyDownloading() {
      return bActivelyDownloading;
    }
    
    public void setActivelyDownloading(boolean bActive) {
      bActivelyDownloading = bActive;
    }

    public int getQR() {
      return qr;
    }

    public void setQR(int newQR) {
      qr = newQR;
    }

    /** Assign Seeding Rank based on RankType
     * @return New Seeding Rank Value
     */
    public synchronized int recalcQR() {
      DownloadStats stats = dl.getStats();
      int numCompleted = stats.getDownloadCompleted(false);

      // make undownloaded sort to top so they start can first.
      if (numCompleted < 1000) {
        setQR(QR_INCOMPLETE_ENDS_AT - dl.getPosition());
        return qr;
      }

      int shareRatio = stats.getShareRatio();

      int numPeers = calcPeersNoUs(dl);
      int numSeeds = calcSeedsNoUs(dl);
      if (numPeersAsFullCopy != 0 && numSeeds >= iFakeFullCopySeedStart)
          numSeeds += numPeers / numPeersAsFullCopy;

      boolean bScrapeResultsOk = (numPeers > 0) || (numSeeds > 0) || scrapeResultOk(dl);

      int newQR = 0;

      // First Priority Calculations
      if (isFirstPriority(false)) {
        newQR += QR_FIRST_PRIORITY_STARTS_AT;
      }
      

      /** 
       * Check ignore rules
       */
      // never apply ignore rules to First Priority Matches
      // (we don't want leechers circumventing the 0.5 rule)
      else {
        if (numPeers == 0 && bScrapeResultsOk && bIgnore0Peers) {
          setQR(QR_0PEERS);
          return QR_0PEERS;
        }

        if (iIgnoreShareRatio != 0 && 
            shareRatio > iIgnoreShareRatio && 
            shareRatio != -1) {
          setQR(QR_SHARERATIOMET);
          return qr;
        }
  
        //0 means disabled
        if ((iIgnoreSeedCount != 0) && (numSeeds >= iIgnoreSeedCount)) {
          setQR(QR_NUMSEEDSMET);
          return QR_NUMSEEDSMET;
        }

        // Skip if Stop Peers Ratio exceeded
        // (More Peers for each Seed than specified in Config)
        //0 means never stop
        if (iIgnoreRatioPeers != 0 && numSeeds != 0) {
          float ratio = (float) numPeers / numSeeds;
          if (ratio <= iIgnoreRatioPeers && numSeeds >= iIgnoreRatioPeers_SeedStart) {
            setQR(QR_RATIOMET);
            return QR_RATIOMET;
          }
        }
      }

      
      // Never do anything with rank type of none
      if (iRankType == RANK_NONE) {
        // everythink ok!
        setQR(newQR);
        return newQR;
      }

      if (iRankType == RANK_TIMED) {
        if (newQR >= QR_FIRST_PRIORITY_STARTS_AT) {
          setQR(newQR);
          return newQR;
        }

        int state = dl.getState();
        if (state == Download.ST_STOPPING ||
            state == Download.ST_STOPPED ||
            state == Download.ST_ERROR) {
          setQR(QR_NOTQUEUED);
          return QR_NOTQUEUED;
        } else if (state == Download.ST_SEEDING) {
          if (newQR >= QR_FIRST_PRIORITY_STARTS_AT) {
            setQR(newQR);
            return newQR;
          }

          // force sort to top
          int iMsElapsed = (int)(System.currentTimeMillis() - stats.getTimeStartedSeeding());
          if (iMsElapsed >= minTimeAlive)
            setQR(1);
          else
            setQR(QR_TIMED_QUEUED_ENDS_AT + 1 + (iMsElapsed/1000));
          return getQR();
        } else {
          if (getQR() <= 0)
            setQR(QR_TIMED_QUEUED_ENDS_AT - dl.getPosition());
          return getQR();
        }
      }



      /** 
       * Add to QR based on Rank Type
       */

      if ((iRankType == RANK_SEEDCOUNT) && 
          (iRankTypeSeedFallback == 0 || iRankTypeSeedFallback > numSeeds))
      {
        if (bScrapeResultsOk) {
          int limit = QR_FIRST_PRIORITY_STARTS_AT / 2 - 10000;
          newQR += limit/(numSeeds + 1) +
                   ((bPreferLargerSwarms ? 1 : -1) * numPeers * 5);
          if (numSeeds == 0 && numPeers >= minPeersToBoostNoSeeds)
            newQR += limit;
        }

      } else { // iRankType == RANK_SPRATIO or we are falling back
        if (numPeers != 0) {
          if (numSeeds == 0) {
            if (numPeers >= minPeersToBoostNoSeeds)
              newQR += 20000;
          }
          else { // numSeeds != 0 && numPeers != 0
            if (numPeers > numSeeds) {
              // give poor seeds:peer ratio a boost 
              newQR += 10000 - (numSeeds * 10000 / numPeers);
            }
            else { // Peers <= Seeds
              newQR += numPeers * 1000 / numSeeds;
            }
          }

          if (bPreferLargerSwarms)
            newQR += numPeers * 5;
          else
            newQR -= numPeers * 5;
        }
      }

      if (newQR < 0)
        newQR = 1;

      // Don't change the qr if we don't have scrape results
      // unless we changed to first priority
      boolean bOldQRInRange = (qr >= 0) && (qr < QR_FIRST_PRIORITY_STARTS_AT);
      boolean bNewQRInRange = (newQR >= 0) && (newQR < QR_FIRST_PRIORITY_STARTS_AT);
      if (bScrapeResultsOk || bOldQRInRange != bNewQRInRange)
        setQR(newQR);

      return qr;
    } // recalcQR

    /** Does the torrent match First Priority criteria? */
    public boolean isFirstPriority() {
      return isFirstPriority(true);
    }

    public boolean isFirstPriority(boolean bSkip0Peers) {
      // FP only applies to completed
      if (dl.getStats().getDownloadCompleted(false) < 1000)
        return false;

      if (dl.getState() == Download.ST_ERROR ||
          dl.getState() == Download.ST_STOPPED)
        return false;

      // A torrent with 0 seeds really shouldn't be the first priority
      if (bSkip0Peers && calcPeersNoUs(dl) == 0)
        return false;

      int shareRatio = dl.getStats().getShareRatio();
      boolean bLastMatched = (shareRatio != -1) && (shareRatio < minQueueingShareRatio);
      boolean bAnyMatched = bLastMatched;
      
      if (iFirstPriorityType == FIRSTPRIORITY_ANY ||
          (iFirstPriorityType == FIRSTPRIORITY_ALL && bLastMatched)) {
        bLastMatched = (iFirstPrioritySeedingMinutes == 0);
        if (!bLastMatched) {
          long timeSeeding = dl.getStats().getSecondsOnlySeeding();
          if (timeSeeding > 0) {
            bLastMatched = (timeSeeding < (iFirstPrioritySeedingMinutes * 60));
            bAnyMatched |= bLastMatched;
          }
        }
      }

      if (iFirstPriorityType == FIRSTPRIORITY_ANY ||
          (iFirstPriorityType == FIRSTPRIORITY_ALL && bLastMatched)) {
        bLastMatched = (iFirstPriorityDLMinutes == 0);
        if (!bLastMatched) {
          long timeDLing = dl.getStats().getSecondsDownloading();
          if (timeDLing > 0) {
            bLastMatched = (timeDLing < (iFirstPriorityDLMinutes * 60));
            bAnyMatched |= bLastMatched;
          }
        }
      }
      
      return ((iFirstPriorityType == FIRSTPRIORITY_ANY && bAnyMatched) ||
              (iFirstPriorityType == FIRSTPRIORITY_ALL && bLastMatched));
    }
  }

  /** A "My Torrents" column for displaying Seeding Rank.
   */
  public class SeedingRankColumn implements PluginMyTorrentsItemFactory {
    public String getName() {
      return "SeedingRank";
    }

    public String getType() {
      return PluginMyTorrentsItemFactory.TYPE_INT;
    }

    public int getDefaultSize() {
      return 80;
    }

    public int getDefaultPosition() {
      return PluginMyTorrentsItemFactory.POSITION_LAST;
    }

    public String getOrientation() {
      return PluginMyTorrentsItemFactory.ORIENT_RIGHT;
    }

    public PluginMyTorrentsItem getInstance(MyTorrentsTableItem item) {
      return new SeedRankingColumnItem(item);
    }
    
    public int getTablesVisibleIn() {
      return PluginMyTorrentsItemFactory.TABLE_COMPLETE;
    }
  }
  
  /**
   * Column in MyTorrents to display the Seeding Rank
   */  
  public class SeedRankingColumnItem implements PluginMyTorrentsItem {
    MyTorrentsTableItem tableItem;
    
    SeedRankingColumnItem(MyTorrentsTableItem item) {
      tableItem = item;
    }

    public void refresh() {
      Download dl = tableItem.getDownload();
      if (dl == null)
        return;
      
      downloadData dlData = (downloadData)downloadDataMap.get(dl);
      if (dlData == null)
        return;

      int qr = dlData.getQR();

      if (qr >= 0) {
        String sText = "";
        if (qr >= QR_FIRST_PRIORITY_STARTS_AT) {
          sText += MessageText.getString("StartStopRules.firstPriority") + " ";
          qr -= QR_FIRST_PRIORITY_STARTS_AT;
        }

        if (iRankType == RANK_TIMED) {
          if (qr > QR_TIMED_QUEUED_ENDS_AT) {
            int timeLeft = (int)(minTimeAlive - 
                                 (long)(System.currentTimeMillis() - 
                                        dl.getStats().getTimeStartedSeeding())) / 1000;
            sText += TimeFormater.format(timeLeft);
          } else if (qr > 0) {
            sText += MessageText.getString("StartStopRules.waiting");
          }
        } else if (qr > 0) {
          sText += String.valueOf(qr);
        }
        tableItem.setText(sText);
      }
      else if (qr == QR_RATIOMET)
        tableItem.setText(MessageText.getString("StartStopRules.ratioMet"));
      else if (qr == QR_NUMSEEDSMET)
        tableItem.setText(MessageText.getString("StartStopRules.numSeedsMet"));
      else if (qr == QR_NOTQUEUED)
        tableItem.setText("");
      else if (qr == QR_0PEERS)
        tableItem.setText(MessageText.getString("StartStopRules.0Peers"));
      else if (qr == QR_SHARERATIOMET)
        tableItem.setText(MessageText.getString("StartStopRules.shareRatioMet"));
      else {
        tableItem.setText("ERR" + qr);
      }
    }

    public String getStringValue() {
      return null;
    }
    
    public int getIntValue() {
      Download dl = tableItem.getDownload();
      if (dl == null)
        return 0;
      
      downloadData dlData = (downloadData)downloadDataMap.get(dl);
      if (dlData == null)
        return 0;

      return dlData.getSeedingPos();
    }
  }

} // class

