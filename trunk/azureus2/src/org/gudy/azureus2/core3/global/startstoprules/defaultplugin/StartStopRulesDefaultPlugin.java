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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;


import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.ConfigSectionSWT;
import org.gudy.azureus2.plugins.ui.tables.mytorrents.*;

import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.TimeFormater;

/** Handles Starting and Stopping of torrents */
public class
StartStopRulesDefaultPlugin
  implements Plugin
{
  // for debugging
  private static final String sStates = " WPRDS.XEQ";

  /** Do not rank completed torrents */  
  public static final int RANK_NONE = 0;
  /** Rank completed torrents using Seeds:Peer Ratio */  
  public static final int RANK_SPRATIO = 1;
  /** Rank completed torrents using Seed Count method */  
  public static final int RANK_SEEDCOUNT = 2;
  /** Rank completed torrents using a timed rotation of minTimeAlive */
  public static final int RANK_TIMED = 3;
  
  public static final int FIRSTPRIORITY_ALL = 0;
  public static final int FIRSTPRIORITY_ANY = 1;
  
  private static final int QR_INCOMPLETE_ENDS_AT      = 1000000000; // billion
  private static final int QR_TIMED_QUEUED_ENDS_AT    =   10000000;
  private static final int QR_FIRST_PRIORITY_STARTS_AT=   50000000;
  private static final int QR_NOTQUEUED       = -2;
  private static final int QR_RATIOMET        = -3;
  private static final int QR_NUMSEEDSMET     = -4;
  private static final int QR_0PEERS          = -5;
  private static final int QR_SHARERATIOMET   = -6;

  protected PluginInterface     plugin_interface;
  protected PluginConfig        plugin_config;
  protected DownloadManager     download_manager;
  protected DownloadListener      download_listener;
  protected DownloadTrackerListener download_tracker_listener;

  /** Map to relate downloadData to a Download */  
  protected Map downloadDataMap = Collections.synchronizedMap(new HashMap());

  protected volatile boolean         closingDown;
  protected volatile boolean         somethingChanged;

  protected LoggerChannel   log;
  private long lastQRcalcTime = 0;
  private long RECALC_QR_EVERY = 15 * 1000;
  private long startedOn;

  // Config Settings
  int minPeersToBoostNoSeeds;
  int minSpeedForActiveDL;
  int numPeersAsFullCopy;
  int maxActive;
  int maxDownloads;

  // Ignore torrent if seed count is at least..
  int     iIgnoreSeedCount;
  // Ignore even when First Priority
  boolean bIgnore0Peers;
  int     iIgnoreShareRatio;
  int     iIgnoreRatioPeers;

  int iRankType;
  int iFakeFullCopySeedStart;
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

    plugin_interface.addListener(
      new PluginListener()
      {
        public void
        initializationComplete()
        {
        }

        public void
        closedownInitiated()
        {
          closingDown  = true;
        }

        public void
        closedownComplete()
        {
        }
      });

    log = plugin_interface.getLogger().getChannel("StartStopRules");

    log.log( LoggerChannel.LT_INFORMATION, "Default StartStopRules Plugin Initialisation" );

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
    } catch( Exception e ){
      e.printStackTrace();
    }

    download_manager = plugin_interface.getDownloadManager();

    download_listener =
      new DownloadListener()
      {
        public void
        stateChanged(
          Download    download,
          int       old_state,
          int       new_state )
        {
          downloadData dlData = (downloadData)downloadDataMap.get(download);
          if (dlData != null) {
            if (new_state == Download.ST_SEEDING) {
              dlData.setStartedSeedingOn(System.currentTimeMillis());
            }
            if (old_state == Download.ST_PREPARING) {
              // preparing can change the completion level.
              dlData.setWasComplete(download.getStats().getDownloadCompleted(false) == 1000);
            }
            // force a QR recalc, so that it gets positiong properly next process()
            dlData.recalcQR();
          }

        }
      };

    download_manager.addListener(
        new DownloadManagerListener()
        {
          public void downloadAdded( Download  download )
          {
            if (!downloadDataMap.containsKey(download)) {
              downloadDataMap.put( download, new downloadData(download) );
            }

            download.addListener( download_listener );

            somethingChanged = true;
          }

          public void downloadRemoved( Download  download )
          {
            download.removeListener( download_listener );

            if (downloadDataMap.containsKey(download)) {
              downloadDataMap.remove(download);
            }

            somethingChanged = true;
          }
        });

      // initial implementation loops - change to event driven maybe although
      // the current rules permit loops under certain circumstances.....

    Thread  t = new Thread("StartStopRulesDefaultPlugin")
      {
        public void
        run()
        {
          while(true){
            try{
              if ( closingDown ){
                log.log( LoggerChannel.LT_INFORMATION, "System Closing - processing stopped" );
                break;
              }

              process();
            }catch( Exception e ){

              e.printStackTrace();
            }

            try{
              int sleep_period = 1000;

              if ( somethingChanged ){
                sleep_period = 100;
              }

              Thread.sleep(sleep_period);

            }catch( InterruptedException e ){

              e.printStackTrace();
            }
          }
        }
      };

    t.setDaemon(true);

    t.start();
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
    iIgnoreShareRatio = 1000 * plugin_config.getIntParameter("Stop Ratio", 0);
    iIgnoreRatioPeers = plugin_config.getIntParameter("Stop Peers Ratio", 0);

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
      
      process();
    }
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
    if (somethingChanged) {
    	somethingChanged = false;
    	recalcQR = true;
    }
	  if (recalcQR)
  	  lastQRcalcTime = System.currentTimeMillis();

    // pull the data into an local array, so we don't have to lock/synchronize
    downloadData[] dlDataArray;
    dlDataArray = (downloadData[])
      downloadDataMap.values().toArray(new downloadData[downloadDataMap.size()]);

    // Start seeding right away if there's no auto-ranking
    // Otherwise, wait a maximium of 60 seconds for scrape results to come in
    // When the first scrape result comes in, bSeedHasRanking will turn to true
    // (see logic in 1st loop)
    boolean bSeedHasRanking = (iRankType == RANK_NONE) || 
                              (System.currentTimeMillis() - startedOn > 60000);

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
      } else if (state == Download.ST_SEEDING && dl_data.isFirstPriority()) {
        totalFirstPriority++;
      }
      // since it's based on time, store in dl_data.
      // we check ActivelyDownloding later and want to use the same value
      // calculated here
      dl_data.setActivelyDownloading(bActivelyDownloading);

      if (!bSeedHasRanking && completionLevel == 1000 && qr != 0)
        bSeedHasRanking = true;

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
                        (!recalcQR);

		//log.log( LoggerChannel.LT_INFORMATION, "quitEarly="+quitEarly+" totalWaitingToSeed="+totalWaitingToSeed);
    if (quitEarly)
      return;

    if (bDebugLog)
      log.log(LoggerChannel.LT_INFORMATION, 
              "tCding="+totalSeeding+
              ";tFrcdCding="+totalForcedSeeding+
              ";tW8tingToCd="+totalWaitingToSeed+
              ";tDLing="+totalDownloading+
              ";activeDLs="+activeDLCount+
              ";tW8tingToDL="+totalWaitingToDL+
              ";tCom="+totalComplete+
              ";tComQd="+totalCompleteQueued+
              ";tIncQd="+totalIncompleteQueued+
              ";recalcQR="+recalcQR+
              ";bCdHasRank="+bSeedHasRanking+
              ";mxCdrs="+maxSeeders+
              ";t1stPr="+totalFirstPriority+
              "");

    // Sort by QR
    if (iRankType != RANK_NONE)
      Arrays.sort(dlDataArray);
    else
      Arrays.sort(dlDataArray, new Comparator () {
	          public final int compare (Object a, Object b) {
	            return ((downloadData)a).getDownloadObject().getPosition() -
	                   ((downloadData)b).getDownloadObject().getPosition();
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
          
        boolean bActivelyDownloading = dl_data.getActivelyDownloading();

        int state = download.getState();
        if (state == Download.ST_PREPARING) {
          // Don't mess with preparing torrents.  they could be in the 
          // middle of resume-data building, or file allocating.
          numWaitingOrDLing++;

        } else if (state == Download.ST_READY ||
                   state == Download.ST_DOWNLOADING ||
                   state == Download.ST_WAITING) {

          boolean bIsActiveDownload = (state == Download.ST_DOWNLOADING) &&
                                      (download.getStats().getDownloadAverage() >= minSpeedForActiveDL) ||
                                      (System.currentTimeMillis() - download.getStats().getTimeStarted() <= 30000);
          // Stop torrent if over limit
          if ((maxDownloads != 0) &&
              (numWaitingOrDLing >= maxDownloads - iExtraFPs) &&
              (bIsActiveDownload || state != Download.ST_DOWNLOADING)) {
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
          } else if (bIsActiveDownload) {
            numWaitingOrDLing++;
          }
        }

        if (state == Download.ST_QUEUED || state == Download.ST_READY)
        {
          if ((maxDownloads == 0) || ((numWaitingOrDLing < maxDownloads - iExtraFPs) &&
                                      (activeDLCount < maxDownloads - iExtraFPs))) {
            try {
              if (state == Download.ST_QUEUED) {
                if (bDebugLog)
                  log.log(LoggerChannel.LT_INFORMATION, "   restart()");
                download.restart();

                // increase counts
                totalWaitingToDL++;
              } else {
                if (bDebugLog)
                  log.log(LoggerChannel.LT_INFORMATION, "   start() activeDLCount < maxDownloads");
                download.start();

                // adjust counts
                totalWaitingToDL--;
                activeDLCount++;
              }
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
            if (bDebugLog)
              sDebugLine += "\nstopAndQueue(); > Max";

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

        // ignore when Share Ratio reaches # in config
        //0 means unlimited
        if (iIgnoreShareRatio != 0) {
          if (shareRatio > iIgnoreShareRatio && shareRatio != -1) {
            if (okToQueue) {
              try {
                if (bDebugLog)
                  sDebugLine += "\nstopAndQueue() Share Ratio Met";
                download.stopAndQueue();
                bStopAndQueued = true;
                numWaitingOrSeeding--;
              } catch (Exception ignore) {/*ignore*/}
            }
            dl_data.setQR(QR_SHARERATIOMET);
          }
        }

        if (okToQueue && (iIgnoreRatioPeers != 0)) {
          int numSeeds = calcSeedsNoUs(download, dl_data.getStartedSeedingOn());
          int numPeers = calcPeersNoUs(download);
          if (numPeersAsFullCopy != 0 && numSeeds >= iFakeFullCopySeedStart)
              numSeeds += numPeers / numPeersAsFullCopy;
          //If there are no seeds, avoid / by 0
          if (numSeeds != 0) {
            float ratio = (float) numPeers / numSeeds;
            if (ratio <= iIgnoreRatioPeers) {
              try {
                if (bDebugLog)
                  sDebugLine += "\nstopAndQueue() P:S Met";
                download.stopAndQueue();
                bStopAndQueued = true;
              } catch (Exception ignore) {/*ignore*/}
              dl_data.setQR(QR_RATIOMET);
            }
          }
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

          boolean bAnyChanged = false;
          String sDebugLineNoChange = download.getName() + "] ";
          String sDebugLineOld = "";
          String sDebugLineNew = "";
          for (int j = 0; j < debugEntries.length; j++) {
            if (debugEntries[j].equals(debugEntries2[j]))
              sDebugLineNoChange += debugEntries[j] + ";";
            else {
              sDebugLineOld += debugEntries[j] + ";";
              sDebugLineNew += debugEntries2[j] + ";";
              bAnyChanged = true;
            }
          }
          String sDebugLineOut = sDebugLineNoChange + sDebugLine +
                              (bAnyChanged ? "\nOld:"+sDebugLineOld+"\nNew:"+sDebugLineNew : "");
          log.log(LoggerChannel.LT_INFORMATION, sDebugLineOut);
        }

      } // getDownloadCompleted == 1000
    } // Loop 2/2 (Start/Stopping)
  } // process()

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

  public int calcSeedsNoUs(Download download, long seedingStartedOn) {
    int numSeeds = 0;
    DownloadScrapeResult sr = download.getLastScrapeResult();
    if (sr.getScrapeStartTime() > 0) {
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
    protected Download dl;
    protected long startedSeedingOn;
    private boolean bActivelyDownloading;
    private boolean bWasComplete;
    
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
      startedSeedingOn = -1;
      dl = _dl;
      setWasComplete(dl.getStats().getDownloadCompleted(false) == 1000);
      //recalcQR();
    }
    
    public boolean getWasComplete() {
      return bWasComplete;
    }

    public void setWasComplete(boolean b) {
      bWasComplete = b;
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

    public long getStartedSeedingOn() {
      return startedSeedingOn;
    }

    public void setStartedSeedingOn(long time) {
      startedSeedingOn = time;
    }

    /** Assign Seeding Rank based on RankType
     * @return New Seeding Rank Value
     */
    public int recalcQR() {
      DownloadStats stats = dl.getStats();
      int numCompleted = stats.getDownloadCompleted(false);

      // make undownloaded sort to top so they start can first.
      if (numCompleted < 1000) {
        setQR(QR_INCOMPLETE_ENDS_AT - dl.getPosition());
        return qr;
      }

      int shareRatio = stats.getShareRatio();

      int numPeers = calcPeersNoUs(dl);
      int numSeeds = calcSeedsNoUs(dl, startedSeedingOn);
      if (numPeersAsFullCopy != 0 && numSeeds >= iFakeFullCopySeedStart)
          numSeeds += numPeers / numPeersAsFullCopy;

      boolean bScrapeResultsOk = (numPeers > 0) || (numSeeds > 0) || scrapeResultOk(dl);

      int newQR = 0;

      // First Priority Calculations
      if (isFirstPriority()) {
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
  
        if (shareRatio > minQueueingShareRatio || shareRatio == -1) {
          if (qr == QR_SHARERATIOMET)
            return qr;
  
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
            if (ratio <= iIgnoreRatioPeers) {
              setQR(QR_RATIOMET);
              return QR_RATIOMET;
            }
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
          int iMsElapsed = (int)(System.currentTimeMillis() - startedSeedingOn);
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
        newQR += 490000000/(numSeeds + 1) +
                 ((bPreferLargerSwarms ? 1 : -1) * numPeers * 5);
        if (numSeeds == 0 && numPeers >= minPeersToBoostNoSeeds)
          newQR += 490000000;

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

      if (bScrapeResultsOk || newQR >= QR_FIRST_PRIORITY_STARTS_AT)
        setQR(newQR);

      return qr;
    } // recalcQR

    /** Does the torrent match First Priority criteria? */
    public boolean isFirstPriority() {
      // FP only applies to completed
      if (dl.getStats().getDownloadCompleted(false) < 1000)
        return false;

      if (dl.getState() == Download.ST_ERROR ||
          dl.getState() == Download.ST_STOPPED)
        return false;

      int shareRatio = dl.getStats().getShareRatio();
      boolean bLastMatched = (shareRatio != -1) && (shareRatio < minQueueingShareRatio);
      boolean bAnyMatched = bLastMatched;
      
      // don't do these rules if we were already complete (at startup)
      if (!bWasComplete) {
        if (iFirstPriorityType == FIRSTPRIORITY_ANY ||
            (iFirstPriorityType == FIRSTPRIORITY_ALL && bLastMatched)) {
          bLastMatched = (iFirstPrioritySeedingMinutes == 0);
          if (!bLastMatched) {
            long timeSeeding = (System.currentTimeMillis() - getStartedSeedingOn()) / 1000 / 60;
            bLastMatched = (timeSeeding < iFirstPrioritySeedingMinutes);
            bAnyMatched |= bLastMatched;
          }
        }
  
        if (iFirstPriorityType == FIRSTPRIORITY_ANY ||
            (iFirstPriorityType == FIRSTPRIORITY_ALL && bLastMatched)) {
          bLastMatched = (iFirstPriorityDLMinutes == 0);
          if (!bLastMatched) {
            long timeDLing = (System.currentTimeMillis() - dl.getStats().getTimeStarted())  / 1000 / 60;
            bLastMatched = (timeDLing < iFirstPriorityDLMinutes);
            bAnyMatched |= bLastMatched;
          }
        }
      }
      
      return ((iFirstPriorityType == FIRSTPRIORITY_ANY && bAnyMatched) ||
              (iFirstPriorityType == FIRSTPRIORITY_ALL && bLastMatched));
    }
  }

  // ConfigSections

  /** General Queueing options
   */
  class ConfigSectionQueue implements ConfigSectionSWT {
    public String configSectionGetParentSection() {
      return ConfigSection.SECTION_ROOT;
    }

    /**
     * Create the "Queue" Tab in the Configuration view
     */
    public Composite configSectionCreate(Composite parent) {
      GridData gridData;
      GridLayout layout;
      Label label;

      // main tab set up

      Composite gMainTab = new Composite(parent, SWT.NULL);

      gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
      gMainTab.setLayoutData(gridData);
      layout = new GridLayout();
      layout.numColumns = 2;
      layout.marginHeight = 0;
      gMainTab.setLayout(layout);

      label = new Label(gMainTab, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.maxdownloads"); //$NON-NLS-1$
      gridData = new GridData();
      gridData.widthHint = 40;
      new IntParameter(gMainTab, "max downloads").setLayoutData(gridData); //$NON-NLS-1$

      label = new Label(gMainTab, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.maxactivetorrents"); //$NON-NLS-1$
      gridData = new GridData();
      gridData.widthHint = 40;
      new IntParameter(gMainTab, "max active torrents").setLayoutData(gridData); //$NON-NLS-1$

      label = new Label(gMainTab, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.minSpeedForActiveDL"); //$NON-NLS-1$
      final String activeDLLabels[] = new String[54];
      final int activeDLValues[] = new int[54];
      int pos = 0;
      for (int i = 0; i < 1024; i += 256) {
        activeDLLabels[pos] = "" + i + " B/s";
        activeDLValues[pos] = i;
        pos++;
      }
      for (int i = 1; pos < activeDLLabels.length; i++) {
        activeDLLabels[pos] = "" + i + " KB/s";
        activeDLValues[pos] = i * 1024;
        pos++;
      }
      new IntListParameter(gMainTab, "StartStopManager_iMinSpeedForActiveDL", activeDLLabels, activeDLValues);

      label = new Label(gMainTab, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.showpopuponclose"); //$NON-NLS-1$
      new BooleanParameter(gMainTab, "Alert on close", true);

      label = new Label(gMainTab, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.queue.debuglog"); //$NON-NLS-1$
      new BooleanParameter(gMainTab, "StartStopManager_bDebugLog");

      return gMainTab;
    }

  	public String configSectionGetName() {
  		return "queue";
  	}

    public void configSectionSave() {
      reloadConfigParams();
    }

    public void configSectionDelete() {
    }
  }


  /** Seeding Automation Specific options
   */
  class ConfigSectionSeeding implements ConfigSectionSWT {
    public String configSectionGetParentSection() {
      return "queue";
    }

  	public String configSectionGetName() {
  		return "queue.seeding";
  	}

    public void configSectionSave() {
      reloadConfigParams();
    }

    public void configSectionDelete() {
    }

    public Composite configSectionCreate(Composite parent) {
      // Seeding Automation Setup
      GridData gridData;
      GridLayout layout;
      Label label;

      Composite cSeeding = new Composite(parent, SWT.NULL);

      layout = new GridLayout();
      layout.numColumns = 2;
      layout.marginHeight = 0;
      cSeeding.setLayout(layout);
      gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
      cSeeding.setLayoutData(gridData);

      // General Seeding Options

      label = new Label(cSeeding, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.disconnetseed"); //$NON-NLS-1$
      new BooleanParameter(cSeeding, "Disconnect Seed", true); //$NON-NLS-1$

      label = new Label(cSeeding, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.switchpriority"); //$NON-NLS-1$
      new BooleanParameter(cSeeding, "Switch Priority", false); //$NON-NLS-1$

      label = new Label(cSeeding, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.userSuperSeeding"); //$NON-NLS-1$
      new BooleanParameter(cSeeding, "Use Super Seeding", false);

      label = new Label(cSeeding, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.minSeedingTime");
      gridData = new GridData();
      gridData.widthHint = 40;
      new IntParameter(cSeeding, "StartStopManager_iMinSeedingTime").setLayoutData(gridData);

      label = new Label(cSeeding, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.seeding.autoReposition");
      new BooleanParameter(cSeeding, "StartStopManager_bAutoReposition");

      return cSeeding;
    }
  }

  
  /** First Priority Specific options.
   */
  class ConfigSectionSeedingFirstPriority implements ConfigSectionSWT {
    public String configSectionGetParentSection() {
      return "queue.seeding";
    }

  	public String configSectionGetName() {
  		return "queue.seeding.firstPriority";
  	}

    public void configSectionSave() {
      reloadConfigParams();
    }

    public void configSectionDelete() {
    }

    public Composite configSectionCreate(Composite parent) {
      // Seeding Automation Setup
      GridData gridData;
      GridLayout layout;
      Label label;
      Composite cArea;

      Composite cFirstPriorityArea = new Composite(parent, SWT.NULL);
      cFirstPriorityArea.addControlListener(new Utils.LabelWrapControlListener());

      layout = new GridLayout();
      layout.numColumns = 2;
      layout.marginHeight = 0;
      cFirstPriorityArea.setLayout(layout);
      gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
      cFirstPriorityArea.setLayoutData(gridData);


      label = new Label(cFirstPriorityArea, SWT.WRAP);
      gridData = new GridData(GridData.FILL_HORIZONTAL);
      gridData.horizontalSpan = 2;
      label.setLayoutData(gridData);
      Messages.setLanguageText(label, "ConfigView.label.seeding.firstPriority.info");

      // ** Begin No Touch area
      
      cArea = new Composite(cFirstPriorityArea, SWT.NULL);
      layout = new GridLayout();
      layout.marginHeight = 0;
      layout.marginWidth = 0;
      layout.numColumns = 3;
      cArea.setLayout(layout);
      gridData = new GridData();
      gridData.horizontalSpan = 2;
      cArea.setLayoutData(gridData);
      
      label = new Label(cArea, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.seeding.firstPriority");

      String fpLabels[] = { MessageText.getString("ConfigView.text.all"), 
                                 MessageText.getString("ConfigView.text.any") };
      int fpValues[] = { FIRSTPRIORITY_ALL, FIRSTPRIORITY_ANY };
      new IntListParameter(cArea, "StartStopManager_iFirstPriority_Type",
                           fpLabels, fpValues);
      
      label = new Label(cArea, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.seeding.firstPriority.following");

      // row
      label = new Label(cFirstPriorityArea, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.seeding.firstPriority.shareRatio");
      String minQueueLabels[] = new String[51];
      int minQueueValues[] = new int[51];
      minQueueLabels[0] = "1:2 (" + 0.5 + ")";
      minQueueValues[0] = 500;
      for (int i = 1; i < minQueueLabels.length; i++) {
        minQueueLabels[i] = i + ":1";
        minQueueValues[i] = i * 1000;
      }
      new IntListParameter(cFirstPriorityArea, "StartStopManager_iFirstPriority_ShareRatio",
                           minQueueLabels, minQueueValues);

      // row
      label = new Label(cFirstPriorityArea, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.seeding.firstPriority.seedingMinutes");

      String seedTimeLabels[] = new String[15];
      int seedTimeValues[] = new int[15];
      String sMinutes = MessageText.getString("ConfigView.text.minutes");
      String sHours = MessageText.getString("ConfigView.text.hours");
      seedTimeLabels[0] = MessageText.getString("ConfigView.text.ignore");
      seedTimeValues[0] = 0;
      seedTimeLabels[1] = "<= 90 " + sMinutes;
      seedTimeValues[1] = 90;
      for (int i = 2; i < seedTimeValues.length; i++) {
        seedTimeLabels[i] = "<= " + i + " " + sHours ;
        seedTimeValues[i] = i * 60;
      }
      new IntListParameter(cFirstPriorityArea, "StartStopManager_iFirstPriority_SeedingMinutes", 
                           seedTimeLabels, seedTimeValues);

      // row
      label = new Label(cFirstPriorityArea, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.seeding.firstPriority.DLMinutes");

      String dlTimeLabels[] = new String[15];
      int dlTimeValues[] = new int[15];
      dlTimeLabels[0] = MessageText.getString("ConfigView.text.ignore");
      dlTimeValues[0] = 0;
      for (int i = 1; i < dlTimeValues.length; i++) {
        dlTimeLabels[i] = "<= " + (i + 2) + " " + sHours ;
        dlTimeValues[i] = (i + 2) * 60;
      }
      new IntListParameter(cFirstPriorityArea, "StartStopManager_iFirstPriority_DLMinutes", 
                           dlTimeLabels, dlTimeValues);



      return cFirstPriorityArea;
    }
  }

  
  /** Auto Starting specific options
   */
  class ConfigSectionSeedingAutoStarting implements ConfigSectionSWT {
    public String configSectionGetParentSection() {
  		return "queue.seeding";
    }

  	public String configSectionGetName() {
  		return "queue.seeding.autoStarting";
  	}

    public void configSectionSave() {
      reloadConfigParams();
    }

    public void configSectionDelete() {
    }

    public Composite configSectionCreate(Composite parent) {
      // Seeding Automation Setup
      GridData gridData;
      GridLayout layout;
      Label label;
      Composite cArea;

      Composite gQR = new Composite(parent, SWT.NULL);
      gQR.addControlListener(new Utils.LabelWrapControlListener());

      layout = new GridLayout();
      layout.numColumns = 2;
      layout.marginHeight = 0;
      gQR.setLayout(layout);
      gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
      gQR.setLayoutData(gridData);


      // ** Begin Rank Type area
      // Rank Type area.  Encompases the 4 (or more) options groups

      Composite cRankType = new Group(gQR, SWT.NULL);
      layout = new GridLayout();
      layout.numColumns = 2;
      layout.verticalSpacing = 2;
      cRankType.setLayout(layout);
      gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
      gridData.horizontalSpan = 2;

      cRankType.setLayoutData(gridData);
      Messages.setLanguageText(cRankType, "ConfigView.label.seeding.rankType");

      // Seeds:Peer options
      RadioParameter rparamPeerSeed =
          new RadioParameter(cRankType, "StartStopManager_iRankType", RANK_SPRATIO);
      Messages.setLanguageText(rparamPeerSeed, "ConfigView.label.seeding.rankType.peerSeed");
      gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
      rparamPeerSeed.setLayoutData(gridData);

      new Label(cRankType, SWT.NULL);


      // Seed Count options
      RadioParameter rparamSeedCount =
          new RadioParameter(cRankType, "StartStopManager_iRankType", RANK_SEEDCOUNT);
      Messages.setLanguageText(rparamSeedCount, "ConfigView.label.seeding.rankType.seed");
      gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
      rparamSeedCount.setLayoutData(gridData);

      Group gSeedCount = new Group(cRankType, SWT.NULL);
      layout = new GridLayout();
      layout.marginHeight = 2;
      layout.marginWidth = 2;
      layout.numColumns = 3;
      gSeedCount.setLayout(layout);
      gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
      gridData.verticalSpan = 1;
      gSeedCount.setLayoutData(gridData);
      Messages.setLanguageText(gSeedCount, "ConfigView.label.seeding.rankType.seed.options");

      label = new Label(gSeedCount, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.seeding.rankType.seed.fallback");

      gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
      gridData.widthHint = 20;
      IntParameter intParamFallBack = new IntParameter(gSeedCount, "StartStopManager_iRankTypeSeedFallback");
      intParamFallBack.setLayoutData(gridData);

      Label labelFallBackSeeds = new Label(gSeedCount, SWT.NULL);
      label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
      Messages.setLanguageText(labelFallBackSeeds, "ConfigView.label.seeds");

      Control[] controlsSeedCount = { gSeedCount };
      rparamSeedCount.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(controlsSeedCount));


      // timed rotation ranking type
      RadioParameter rparamTimed =
          new RadioParameter(cRankType, "StartStopManager_iRankType", RANK_TIMED);
      Messages.setLanguageText(rparamTimed, "ConfigView.label.seeding.rankType.timedRotation");
      gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
      rparamTimed.setLayoutData(gridData);

      new Label(cRankType, SWT.NULL);


      // No Ranking
      RadioParameter rparamNone =
          new RadioParameter(cRankType, "StartStopManager_iRankType", RANK_NONE);
      Messages.setLanguageText(rparamNone, "ConfigView.label.seeding.rankType.none");
      gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
      rparamNone.setLayoutData(gridData);
      
      new Label(cRankType, SWT.NULL);
      
      // ** End Rank Type area


      Composite cNoTimeNone = new Composite(gQR, SWT.NULL);
      layout = new GridLayout();
      layout.numColumns = 2;
      cNoTimeNone.setLayout(layout);
      gridData = new GridData();
      layout.marginHeight = 0;
      layout.marginWidth = 0;
      gridData.horizontalSpan = 2;
      cNoTimeNone.setLayoutData(gridData);
      
      label = new Label(cNoTimeNone, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.seeding.preferLargerSwarms"); //$NON-NLS-1$
      new BooleanParameter(cNoTimeNone, "StartStopManager_bPreferLargerSwarms");



      label = new Label(cNoTimeNone, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.minPeersToBoostNoSeeds"); //$NON-NLS-1$
      final String boostQRPeersLabels[] = new String[9];
      final int boostQRPeersValues[] = new int[9];
      String peers = MessageText.getString("ConfigView.text.peers");
      for (int i = 0; i < boostQRPeersValues.length; i++) {
        boostQRPeersLabels[i] = (i+1) + " " + peers; //$NON-NLS-1$
        boostQRPeersValues[i] = (i+1);
      }
      gridData = new GridData();
      new IntListParameter(cNoTimeNone, "StartStopManager_iMinPeersToBoostNoSeeds", boostQRPeersLabels, boostQRPeersValues);


      label = new Label(cNoTimeNone, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.seeding.numPeersAsFullCopy");

      cArea = new Composite(cNoTimeNone, SWT.NULL);
      layout = new GridLayout();
      layout.marginHeight = 0;
      layout.marginWidth = 0;
      layout.numColumns = 2;
      cArea.setLayout(layout);
      gridData = new GridData();
      cArea.setLayoutData(gridData);

      gridData = new GridData();
      gridData.widthHint = 20;
      IntParameter paramFakeFullCopy = new IntParameter(cArea, "StartStopManager_iNumPeersAsFullCopy");
      paramFakeFullCopy.setLayoutData(gridData);
      final Text txtFakeFullCopy = (Text)paramFakeFullCopy.getControl();

      label = new Label(cArea, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.peers");

      label = new Label(cNoTimeNone, SWT.NULL);
      gridData = new GridData();
      gridData.horizontalIndent = 15;
      label.setLayoutData(gridData);
      Messages.setLanguageText(label, "ConfigView.label.seeding.fakeFullCopySeedStart");

      final Composite cFullCopyOptionsArea = new Composite(cNoTimeNone, SWT.NULL);
      layout = new GridLayout();
      layout.marginHeight = 0;
      layout.marginWidth = 0;
      layout.numColumns = 2;
      cFullCopyOptionsArea.setLayout(layout);
      gridData = new GridData();
      cFullCopyOptionsArea.setLayoutData(gridData);

      gridData = new GridData();
      gridData.widthHint = 20;
      new IntParameter(cFullCopyOptionsArea, "StartStopManager_iFakeFullCopySeedStart").setLayoutData(gridData);
      label = new Label(cFullCopyOptionsArea, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.seeds");
      
      final int iNumPeersAsFullCopy = StartStopRulesDefaultPlugin.this.plugin_config.getIntParameter("StartStopManager_iNumPeersAsFullCopy");
      controlsSetEnabled(cFullCopyOptionsArea.getChildren(), iNumPeersAsFullCopy != 0);

      paramFakeFullCopy.getControl().addListener(SWT.Modify, new Listener() {
          public void handleEvent(Event event) {
            try {
              Text control = (Text)event.widget;
              if (control.getEnabled()) {
                int value = Integer.parseInt(control.getText());
                boolean enabled = (value != 0);
                if (cFullCopyOptionsArea.getEnabled() != enabled) {
                  cFullCopyOptionsArea.setEnabled(enabled);
                  controlsSetEnabled(cFullCopyOptionsArea.getChildren(), enabled);
                }
              }
            }
            catch (Exception e) {}
          }
      });

      Control[] controlsNoTimeNone = { cNoTimeNone };
      rparamPeerSeed.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(controlsNoTimeNone) {
        public void performAction() {
          super.performAction();
          Event e = new Event();
          e.widget = txtFakeFullCopy;
          txtFakeFullCopy.notifyListeners(SWT.Modify, e);
        }
      });
      rparamSeedCount.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(controlsNoTimeNone) {
        public void performAction() {
          super.performAction();
          Event e = new Event();
          e.widget = txtFakeFullCopy;
          txtFakeFullCopy.notifyListeners(SWT.Modify, e);
        }
      });
      
      
      boolean enable = (StartStopRulesDefaultPlugin.this.iRankType == RANK_SPRATIO || 
                        StartStopRulesDefaultPlugin.this.iRankType == RANK_SEEDCOUNT);
      controlsSetEnabled(controlsNoTimeNone, enable);
        


      return gQR;
    }
    private void controlsSetEnabled(Control[] controls, boolean bEnabled) {
      for(int i = 0 ; i < controls.length ; i++) {
        if (controls[i] instanceof Composite)
          controlsSetEnabled(((Composite)controls[i]).getChildren(), bEnabled);
        controls[i].setEnabled(bEnabled);
      }
    }
  }


  /** Config Section for items that make us ignore torrents when seeding 
   */
  class ConfigSectionSeedingIgnore implements ConfigSectionSWT {
    public String configSectionGetParentSection() {
      return "queue.seeding";
    }

  	public String configSectionGetName() {
  		return "queue.seeding.ignore";
  	}

    public void configSectionSave() {
      reloadConfigParams();
    }

    public void configSectionDelete() {
    }

    public Composite configSectionCreate(Composite parent) {
      // Seeding Automation Setup
      GridData gridData;
      GridLayout layout;
      Label label;

      Composite cIgnore = new Composite(parent, SWT.NULL);
      cIgnore.addControlListener(new Utils.LabelWrapControlListener());

      layout = new GridLayout();
      layout.numColumns = 3;
      layout.marginHeight = 0;
      cIgnore.setLayout(layout);

      label = new Label(cIgnore, SWT.WRAP);
      gridData = new GridData(GridData.FILL_HORIZONTAL);
      gridData.horizontalSpan = 3;
      label.setLayoutData(gridData);
      Messages.setLanguageText(label, "ConfigView.label.autoSeedingIgnoreInfo"); //$NON-NLS-1$

      label = new Label(cIgnore, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.ignoreSeeds"); //$NON-NLS-1$
      gridData = new GridData();
      gridData.widthHint = 20;
      new IntParameter(cIgnore, "StartStopManager_iIgnoreSeedCount").setLayoutData(gridData);
      label = new Label(cIgnore, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.seeds");

      label = new Label(cIgnore, SWT.WRAP);
      Messages.setLanguageText(label, "ConfigView.label.seeding.ignoreRatioPeers"); //$NON-NLS-1$
      gridData = new GridData();
      gridData.widthHint = 20;
      new IntParameter(cIgnore, "Stop Peers Ratio").setLayoutData(gridData);
      label = new Label(cIgnore, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.peers");

      label = new Label(cIgnore, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.seeding.ignore0Peers");
      new BooleanParameter(cIgnore, "StartStopManager_bIgnore0Peers");
      label = new Label(cIgnore, SWT.NULL);

      label = new Label(cIgnore, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.seeding.ignoreShareRatio");
      gridData = new GridData();
      gridData.widthHint = 20;
      new IntParameter(cIgnore, "Stop Ratio").setLayoutData(gridData);
      label = new Label(cIgnore, SWT.NULL);
      label.setText(":1");

      return cIgnore;
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
                                        dlData.getStartedSeedingOn())) / 1000;
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

      return dlData.getQR();
    }
  }

} // class

