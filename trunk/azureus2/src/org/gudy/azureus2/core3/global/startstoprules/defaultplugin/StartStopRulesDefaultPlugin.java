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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;


import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.ui.tables.mytorrents.*;

import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.TimeFormater;

public class
StartStopRulesDefaultPlugin
  implements Plugin
{
  private static final boolean DEBUG = false;
  // No auto ranking
  public static final int RANK_NONE = 0;
  // Seeds/Peer Ratio
  public static final int RANK_SPRATIO = 1;
  // Seed Count (ignore peer count)
  public static final int RANK_SEEDCOUNT = 2;
  // Timed Rotation based on  minTimeAlive
  public static final int RANK_TIMED = 3;
  
  private static final int QR_INCOMPLETE_ENDS_AT      = 1000000000; // billion
  private static final int QR_TIMED_QUEUED_ENDS_AT    =   10000000;
  private static final int QR_NOTQUEUED   = -2;
  private static final int QR_RATIOMET    = -3;
  private static final int QR_NUMSEEDSMET = -4;
  private static final int QR_0SEEDSPEERS = -5;

  protected PluginInterface     plugin_interface;
  protected PluginConfig        plugin_config;
  protected DownloadManager     download_manager;
  protected DownloadListener      download_listener;
  protected DownloadTrackerListener download_tracker_listener;

  protected Map downloadDataMap = Collections.synchronizedMap(new HashMap());

  protected volatile boolean         closingDown;
  protected volatile boolean         somethingChanged;

  protected LoggerChannel   log;
  private long lastQRcalcTime = 0;
  private long RECALC_QR_EVERY = 15 * 1000;
  private long startedOn;

  // Config Settings
  int minPeersPerSeed;
  int minPeersToBoostNoSeeds;
  int minSpeedForActiveDL;
  int minShareRatio;
  int numPeersAsFullCopy;
  int maxActive;
  int maxDownloads;
  // Ignore torrent if seed count is at least..
  int ignoreSeedCount;
  int iRankType;
  int iFakeFullCopySeedStart;
  int iRankTypeSeedFallback;
  boolean bAutoReposition;
  long minTimeAlive;

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
      plugin_interface.addConfigSection(new ConfigSectionStarting());
      plugin_interface.addConfigSection(new ConfigSectionStopping());
    } catch (NoClassDefFoundError e) {
      /* Ignore. SWT probably not installed */
      log.log(LoggerChannel.LT_WARNING,
              "UI Config not loaded for StartStopRulesDefaulPlugin. " +
              e.getMessage() + " not found.");
    } catch( Throwable e ){
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
            }catch( Throwable e ){

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
    int iNewRankType = plugin_config.getIntParameter("StartStopManager_iRankType");
    minPeersPerSeed = plugin_config.getIntParameter("Stop Peers Ratio", 0);
    minPeersToBoostNoSeeds = plugin_config.getIntParameter("StartStopManager_iMinPeersToBoostNoSeeds");
    minSpeedForActiveDL = plugin_config.getIntParameter("StartStopManager_iMinSpeedForActiveDL");
    minShareRatio = 1000 * plugin_config.getIntParameter("Stop Ratio", 0);
    maxActive = plugin_config.getIntParameter("max active torrents");
    maxDownloads = plugin_config.getIntParameter("max downloads");
    numPeersAsFullCopy = plugin_config.getIntParameter("StartStopManager_iNumPeersAsFullCopy");
    // Ignore torrent if seed count is at least..
    ignoreSeedCount = plugin_config.getIntParameter("Ignore Seed Count", 0);
    iFakeFullCopySeedStart = plugin_config.getIntParameter("StartStopManager_iFakeFullCopySeedStart");
    iRankTypeSeedFallback = plugin_config.getIntParameter("StartStopManager_iRankTypeSeedFallback");
    bAutoReposition = plugin_config.getBooleanParameter("StartStopManager_bAutoReposition");
    minTimeAlive = plugin_config.getIntParameter("StartStopManager_iMinSeedingTime") * 1000;
    // shorted recalc for timed rank type, since the calculation is fast and we want to stop on the second
    if (iRankType == RANK_TIMED)
      RECALC_QR_EVERY = 1000;

    if (iNewRankType != iRankType) {
      iRankType = iNewRankType;
      lastQRcalcTime = 0;
      downloadData[] dlDataArray;
      dlDataArray = (downloadData[])
        downloadDataMap.values().toArray(new downloadData[downloadDataMap.size()]);
      for (int i = 0; i < dlDataArray.length; i++)
        dlDataArray[i].setQR(0);
      
      process();
    }
  }

  protected void process() {
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

    boolean recalcQR = (iRankType != RANK_NONE) && ((process_time - lastQRcalcTime) > RECALC_QR_EVERY);
    if (somethingChanged) {
    	somethingChanged = false;
    	recalcQR = (iRankType != RANK_NONE);
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
      int state = download.getState();

      if (state == Download.ST_DOWNLOADING && !download.isForceStart()) {
        totalDownloading++;
        // Only increase activeDLCount if there's downloading
        // or if the torrent just recently started (ie. give it a chance to get some connections)
        if ((download.getStats().getDownloadAverage() >= minSpeedForActiveDL) ||
            (System.currentTimeMillis() - download.getStats().getTimeStarted() <= 30000))
          activeDLCount++;
      }

      int completionLevel = download.getStats().getDownloadCompleted(false);
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
            state == Download.ST_PREPARING)
          totalWaitingToDL++;
        else if (state == Download.ST_QUEUED)
          totalIncompleteQueued++;
      }
    }

    int maxSeeders = (maxActive == 0) ? 99999 : maxActive - activeDLCount;
    // XXX put in subtraction logic here

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

      //log.log( LoggerChannel.LT_INFORMATION, "["+download.getTorrent().getName()+"]: state="+download.getState()+";qr="+dl_data.getQR()+";compl="+download.getStats().getDownloadCompleted(false));
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
        // Stop torrent if over limit
        if ((download.getState() == Download.ST_READY ||
             download.getState() == Download.ST_DOWNLOADING ||
             download.getState() == Download.ST_WAITING ||
             download.getState() == Download.ST_PREPARING) &&
            (!download.isForceStart())
           ) {

            if ((maxDownloads != 0) &&
                (numWaitingOrDLing >= maxDownloads)) {
               try {
                download.stopAndQueue();
                bStopAndQueued = true;
               } catch (Exception ignore) {/*ignore*/}
            }
            else if ((download.getState() == Download.ST_DOWNLOADING) &&
                     (download.getStats().getDownloadAverage() >= minSpeedForActiveDL) ||
                     (System.currentTimeMillis() - download.getStats().getTimeStarted() <= 30000))
              numWaitingOrDLing++;
            else
              numWaitingOrDLing++;
        }

        if ((download.getState() == Download.ST_QUEUED) &&
            ((maxDownloads == 0) || (numWaitingOrDLing < maxDownloads))) {
          try {
            download.restart();
          } catch (Exception ignore) {/*ignore*/}
          numWaitingOrDLing++;
        }


        // Start if incomplete and we haven't reached our limit, or
        // if user forced start.
        // (completed torrents are started later)
        if (download.getState() == Download.ST_READY &&
            ((maxDownloads == 0) || (activeDLCount < maxDownloads))
        ) {
          try {
            download.start();
          } catch (Exception ignore) {/*ignore*/}

          if (download.getState() == Download.ST_DOWNLOADING) {
            activeDLCount++;
            maxSeeders = (maxActive == 0) ? 99999 : maxActive - activeDLCount;
            // XXX put in subtraction logic here
          }
        }
      }
      else if (bSeedHasRanking) { // completed
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
        boolean okToStop = (state == Download.ST_READY || state == Download.ST_SEEDING || state == Download.ST_QUEUED) &&
                           (!download.isForceStart()) &&
                           (shareRatio > 500 || shareRatio == -1);
        if (okToStop && (state == Download.ST_SEEDING) && iRankType != RANK_TIMED) {
          // Min time check
          long timeAlive = System.currentTimeMillis() - download.getStats().getTimeStarted();
          okToStop = (timeAlive >= minTimeAlive);
        }
        // We can stop items queued, but we can't queue items queued ;)
        boolean okToQueue = okToStop && (state != Download.ST_QUEUED);

        if (download.getState() == Download.ST_READY ||
            download.getState() == Download.ST_SEEDING ||
            download.getState() == Download.ST_WAITING ||
            download.getState() == Download.ST_PREPARING)
          numWaitingOrSeeding++;

        if (DEBUG)
          log.log(LoggerChannel.LT_INFORMATION,
                  "["+download.getTorrent().getName()+"]: state="+state+
                  ";numWaitingorSeeding="+numWaitingOrSeeding+
                  ";okToQueue="+okToQueue+
                  ";okToStop="+okToStop+
                  ";qr="+dl_data.getQR());

        // Change to waiting if queued and we have an open slot
        if ((download.getState() == Download.ST_QUEUED) &&
            (numWaitingOrSeeding < maxSeeders) &&
            (dl_data.getQR() > -2) &&
            !higherQueued) {
          try {
            download.restart(); // set to Waiting
            totalWaitingToSeed++;
            numWaitingOrSeeding++;
          } catch (Exception ignore) {/*ignore*/}
        }

        if ((download.getState() == Download.ST_READY) &&
            (totalSeeding < maxSeeders)) {
          if (dl_data.getQR() > -2) {
            try {
              download.start();
            } catch (Exception ignore) {/*ignore*/}

            totalSeeding++;
          }
          else if (okToQueue) {
            // In between switching from STATE_WAITING and STATE_READY,
            // Stop Ratio was met, so move it back to Queued
            try {
              download.stopAndQueue();
              bStopAndQueued = true;
              totalWaitingToSeed--;
              numWaitingOrSeeding--;
            } catch (Exception ignore) {/*ignore*/}
          }
        }


        // if there's more torrents waiting/seeding than our max, or if
        // there's a higher ranked torrent queued, stop this one
        if (okToQueue &&
            ((numWaitingOrSeeding > maxSeeders) || higherQueued)) {
          try {
            if (download.getState() == Download.ST_READY)
              totalWaitingToSeed--;

            download.stopAndQueue();
            bStopAndQueued = true;
            // okToQueue only allows READY and SEEDING state.. and in both cases
            // we have to reduce counts
            numWaitingOrSeeding--;
          } catch (Exception ignore) {/*ignore*/}
        }

        //STOP (no auto-starting again) when Share Ratio reaches # in config
        //0 means unlimited
        if (minShareRatio != 0) {
          if (download.getStats().getShareRatio() > minShareRatio && okToStop)
            try {
              download.stop();
            } catch (Exception ignore) {/*ignore*/}
        }

        if (okToQueue && (minPeersPerSeed != 0)) {
          int numSeeds = calcSeedsNoUs(download, dl_data.getStartedSeedingOn());
          int numPeers = calcPeersNoUs(download);
          if (numPeersAsFullCopy != 0 && numSeeds >= iFakeFullCopySeedStart)
              numSeeds += numPeers / numPeersAsFullCopy;
          //If there are no seeds, avoid / by 0
          if (numSeeds != 0) {
            float ratio = (float) numPeers / numSeeds;
            if (ratio <= minPeersPerSeed) {
              try {
                download.stopAndQueue();
                bStopAndQueued = true;
              } catch (Exception ignore) {/*ignore*/}
              dl_data.setQR(QR_RATIOMET);
              log.log(0, download.getName()+"] ratio met dude");
            }
          }
        }
        
        // move completed timed rank types to bottom of the list
        if (bStopAndQueued && iRankType == RANK_TIMED) {
          int iNewQR = QR_TIMED_QUEUED_ENDS_AT;
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

        if (download.getState() == Download.ST_QUEUED)
          higherQueued = true;

        // XXX: Old code. Do we really want to remove torrents when the error is "File Not Found"?
        if (  download.getState() == Download.ST_ERROR &&
            download.getErrorStateDetails() != null &&
            download.getErrorStateDetails().equals("File Not Found")){

          try{
          	Torrent t = download.getTorrent();
          	if (t == null)
          		log.log( LoggerChannel.LT_INFORMATION, "Removing ["+download.getName()+"]: torrent file not found" );
          	else
            	log.log( LoggerChannel.LT_INFORMATION, "Remove ["+t.getName()+"]: file not found" );

            downloadDataMap.remove(download);
            download.remove();

          }catch( DownloadRemovalVetoException e ){

            e.printStackTrace();

          }catch( DownloadException e ){

            e.printStackTrace();
          }
        }

      } // getDownloadCompleted == 1000
    } // Loop 2/2 (Start/Stopping)
  } // process()

  public boolean getAlreadyAllocatingOrChecking() {
    Download[]  downloads = download_manager.getDownloads();
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
      long downloadStartedOn = download.getStats().getTimeStarted();
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

    public int compareTo(Object obj)
    {
      return ((downloadData)obj).getQR() - qr;
    }

    public downloadData(Download _dl)
    {
      startedSeedingOn = -1;
      dl = _dl;
      //recalcQR();
    }

    Download getDownloadObject()
    {
      return dl;
    }

    public int getQR()
    {
      return qr;
    }

    public void setQR(int newQR)
    {
      qr = newQR;
    }

    public long getStartedSeedingOn() {
      return startedSeedingOn;
    }

    public void setStartedSeedingOn(long time) {
      startedSeedingOn = time;
    }

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

  //log.log( LoggerChannel.LT_INFORMATION, "["+dl.getTorrent().getName()+"]: Peers="+numPeers+"; Seeds="+numSeeds);

      if (numSeeds == 0 && numPeers == 0 && bScrapeResultsOk) {
        setQR(QR_0SEEDSPEERS);
        return QR_0SEEDSPEERS;
      }

      if ((numCompleted == 1000) &&
          (shareRatio > 500 || shareRatio == -1)) {
            
        //0 means disabled
        if ((ignoreSeedCount != 0) && (numSeeds >= ignoreSeedCount)) {
          setQR(QR_NUMSEEDSMET);
          return QR_NUMSEEDSMET;
        }

        // Skip if Stop Peers Ratio exceeded
        // (More Peers for each Seed than specified in Config)
        //0 means never stop
        if (minPeersPerSeed != 0 && numSeeds != 0) {
          float ratio = (float) numPeers / numSeeds;
          if (ratio <= minPeersPerSeed) {
            setQR(QR_RATIOMET);
            return QR_RATIOMET;
          }
        }
      }

      // The one point adjustments were added just to make the
      // Queue ranking more varied (less torrents being all at one
      // ranking)
      int newQR = 0;

      if (iRankType == RANK_TIMED) {
        if (shareRatio <= 500 && shareRatio != -1) {
          setQR(QR_INCOMPLETE_ENDS_AT - 10000);
          return getQR();
        }
        int state = dl.getState();
        if (state == Download.ST_STOPPING ||
            state == Download.ST_STOPPED ||
            state == Download.ST_ERROR) {
          setQR(QR_NOTQUEUED);
          return QR_NOTQUEUED;
        } else if (state == Download.ST_SEEDING) {
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

      // Make torrents with < 1.0 share ratio very important
      // (but not more important than unfinished downloads)
      if (shareRatio != -1) {
        if (shareRatio < 1000) {
          // Add up to 500
          newQR += (1000 - shareRatio) / 2;
          // extra hike for less than .5
          if (shareRatio <= 500)
            newQR += 10000000;
        }
        else if (shareRatio >= 2000) {
          // One point less for every 100% uploaded
          newQR -= (shareRatio / 1000) - 1;
        }
      }
      else {
        long fileSize = dl.getTorrent().getSize();
        long uploadSize = stats.getUploaded();
        // one point less for every 100% of total size uploaded
        if (fileSize > 0 && uploadSize > fileSize)
          newQR -= (uploadSize / fileSize) - 1;
      }

      if ((iRankType == RANK_SEEDCOUNT) && 
          (iRankTypeSeedFallback == 0 || iRankTypeSeedFallback > numSeeds))
      {
        int maxSeeds = ignoreSeedCount;
        if (maxSeeds == 0)
          maxSeeds = 1000;

        if (numSeeds > 0 || (numSeeds == 0 && numPeers >= minPeersToBoostNoSeeds)) {
          newQR += 9999999 - (numSeeds * 9999999 / maxSeeds);
        }
        // Note, this will "break" if we have over 2000 peers and cause the torrent
        // with more than 2000 peers to be above one with less peers
        newQR += (int) ((float)(9999999.0 / (float)maxSeeds / 2000.0) * (float)numPeers);

      } else if (iRankType == RANK_SPRATIO) {
        if (numPeers != 0) {
          if (numSeeds == 0) {
            if (numPeers >= minPeersToBoostNoSeeds)
              newQR += 2000;

            newQR += numPeers * 50;
          }
          else { // numSeeds != 0 && numPeers != 0
            if (numPeers > numSeeds) {
              // give poor seeds:peer ratio a boost of up to 1000
              newQR += 1000 - (numSeeds * 1000 / numPeers);
            }
            else { // Peers <= Seeds
              // only up to 100 points
              newQR += numPeers * 100 / numSeeds;
            }
          }
        }
      }

      if (newQR < 0)
        newQR = 1;

      if (bScrapeResultsOk || qr > (QR_INCOMPLETE_ENDS_AT - 10000))
        setQR(newQR);

      return qr;
    } // recalcQR

  }

    // ConfigSection Implementation

  class ConfigSectionQueue implements ConfigSection {
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
      final String activeDLLabels[] = new String[53];
      final int activeDLValues[] = new int[53];
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
      new IntListParameter(gMainTab, "StartStopManager_iMinSpeedForActiveDL", 512, activeDLLabels, activeDLValues);

      label = new Label(gMainTab, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.disconnetseed"); //$NON-NLS-1$
      new BooleanParameter(gMainTab, "Disconnect Seed", true); //$NON-NLS-1$

      label = new Label(gMainTab, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.switchpriority"); //$NON-NLS-1$
      new BooleanParameter(gMainTab, "Switch Priority", false); //$NON-NLS-1$

      label = new Label(gMainTab, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.showpopuponclose"); //$NON-NLS-1$
      new BooleanParameter(gMainTab, "Alert on close", true);

      label = new Label(gMainTab, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.userSuperSeeding"); //$NON-NLS-1$
      new BooleanParameter(gMainTab, "Use Super Seeding", false);

      return gMainTab;
    }

  	public String configSectionGetName() {
  		return MessageText.getString("ConfigView.section.queue");
  	}

  	public String configSectionGetID() {
  		return "ConfigView.section.queue";
  	}

    public void configSectionSave() {
      reloadConfigParams();
    }

    public void configSectionDelete() {
    }
  }


  class ConfigSectionStarting implements ConfigSection {
    public String configSectionGetParentSection() {
      return "ConfigView.section.queue";
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
      gQR.setLayout(layout);

      label = new Label(gQR, SWT.WRAP);
      gridData = new GridData(GridData.FILL_HORIZONTAL);
      gridData.horizontalSpan = 2;
      label.setLayoutData(gridData);
      Messages.setLanguageText(label, "ConfigView.label.autoSeedingInfo"); //$NON-NLS-1$

      label = new Label(gQR, SWT.WRAP);
      gridData = new GridData(GridData.FILL_HORIZONTAL);
      gridData.horizontalSpan = 2;
      label.setLayoutData(gridData);
      Messages.setLanguageText(label, "ConfigView.label.seeding.rankType");

      // Rank Type area.  Encompases the two options groups

      Composite cRankType = new Composite(gQR, SWT.NULL);
      layout = new GridLayout();
      layout.marginHeight = 0;
      layout.marginWidth = 0;
      layout.numColumns = 2;
      layout.verticalSpacing = 1;
      cRankType.setLayout(layout);
      gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
      gridData.horizontalSpan = 2;
      gridData.horizontalIndent = 15;

      cRankType.setLayoutData(gridData);

      // Seeds:Peer options
      RadioParameter rparamPeerSeed =
          new RadioParameter(cRankType, "StartStopManager_iRankType", RANK_SPRATIO);
      Messages.setLanguageText(rparamPeerSeed, "ConfigView.label.seeding.rankType.peerSeed");
      gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
      rparamPeerSeed.setLayoutData(gridData);

      new Label(cRankType, SWT.NULL);
/*
      Group gPeerSeed = new Group(cRankType, SWT.NULL);
      layout = new GridLayout();
      layout.marginHeight = 2;
      layout.marginWidth = 2;
      layout.numColumns = 3;
      gPeerSeed.setLayout(layout);
      gPeerSeed.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
      Messages.setLanguageText(gPeerSeed, "ConfigView.label.seeding.rankType.peerSeed.options");

//      Control[] controlsPeerSeed = { gPeerSeed };
//      rparamPeerSeed.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(controlsPeerSeed));
*/

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
      gSeedCount.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
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
      



      // General Seeding Options
      label = new Label(gQR, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.minSeedingTime"); //$NON-NLS-1$
      gridData = new GridData();
      gridData.widthHint = 40;
      new IntParameter(gQR, "StartStopManager_iMinSeedingTime").setLayoutData(gridData);

      label = new Label(gQR, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.minPeersToBoostNoSeeds"); //$NON-NLS-1$
      final String boostQRPeersLabels[] = new String[9];
      final int boostQRPeersValues[] = new int[9];
      String peers = MessageText.getString("ConfigView.text.peers");
      for (int i = 0; i < boostQRPeersValues.length; i++) {
        boostQRPeersLabels[i] = (i+1) + " " + peers; //$NON-NLS-1$
        boostQRPeersValues[i] = (i+1);
      }
      gridData = new GridData();
      new IntListParameter(gQR, "StartStopManager_iMinPeersToBoostNoSeeds", boostQRPeersLabels, boostQRPeersValues);

      label = new Label(gQR, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.ignoreSeeds"); //$NON-NLS-1$
      final String ignoreSeedsLabels[] = new String[30];
      final int ignoreSeedsValues[] = new int[30];
      ignoreSeedsLabels[0] = MessageText.getString("ConfigView.text.neverIgnore");
      ignoreSeedsValues[0] = 0;
      String seeds = MessageText.getString("ConfigView.label.seeds");
      for (int i = 1; i <= 20; i++) {
        ignoreSeedsLabels[i] = i + " " + seeds; //$NON-NLS-1$
        ignoreSeedsValues[i] = i;
      }
      int value = 30;
      for (int i = 21; i < ignoreSeedsValues.length; i++) {
        ignoreSeedsLabels[i] = value + " " + seeds; //$NON-NLS-1$
        ignoreSeedsValues[i] = value;
        value += 10;
      }
      // Using old config name
      new IntListParameter(gQR, "Ignore Seed Count", 0, ignoreSeedsLabels, ignoreSeedsValues);

      label = new Label(gQR, SWT.WRAP);
      Messages.setLanguageText(label, "ConfigView.label.stopRatioPeers"); //$NON-NLS-1$
      final String stopRatioPeersLabels[] = new String[15];
      final int stopRatioPeersValues[] = new int[15];
      stopRatioPeersLabels[0] = MessageText.getString("ConfigView.text.neverIgnore");
      stopRatioPeersValues[0] = 0;
      for (int i = 1; i < stopRatioPeersValues.length; i++) {
        stopRatioPeersLabels[i] = i + " " + peers; //$NON-NLS-1$
        stopRatioPeersValues[i] = i;
      }
      new IntListParameter(gQR, "Stop Peers Ratio", 0, stopRatioPeersLabels, stopRatioPeersValues);


      label = new Label(gQR, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.qr.numPeersAsFullCopy");

      cArea = new Composite(gQR, SWT.NULL);
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

      label = new Label(cArea, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.peers");

      label = new Label(gQR, SWT.NULL);
      gridData = new GridData();
      gridData.horizontalIndent = 15;
      label.setLayoutData(gridData);
      Messages.setLanguageText(label, "ConfigView.label.seeding.fakeFullCopySeedStart");

      final Composite cFullCopyOptionsArea = new Composite(gQR, SWT.NULL);
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
          boolean wasEnabled = (iNumPeersAsFullCopy != 0);
          public void handleEvent(Event event) {
            try {
              Text control = (Text)event.widget;
              int value = Integer.parseInt(control.getText());
              boolean enabled = (value != 0);
              if (wasEnabled != enabled) {
                controlsSetEnabled(cFullCopyOptionsArea.getChildren(), enabled);
                wasEnabled = enabled;
              }
            }
            catch (Exception e) {}
          }
      });

      label = new Label(gQR, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.seeding.autoReposition"); //$NON-NLS-1$
      BooleanParameter removeOnStopParam = new BooleanParameter(gQR, "StartStopManager_bAutoReposition");

      return gQR;
    }

    private void controlsSetEnabled(Control[] controls, boolean bEnabled) {
      for(int i = 0 ; i < controls.length ; i++) {
        if (controls[i] instanceof Composite)
          controlsSetEnabled(((Composite)controls[i]).getChildren(), bEnabled);
        controls[i].setEnabled(bEnabled);
      }
    }

  	public String configSectionGetName() {
  		return MessageText.getString("ConfigView.section.queue.autoSeeding");
  	}

  	public String configSectionGetID() {
  		return "ConfigView.section.queue.autoSeeding";
  	}

    public void configSectionSave() {
      reloadConfigParams();
    }

    public void configSectionDelete() {
    }
  }


  class ConfigSectionStopping implements ConfigSection {

    public String configSectionGetParentSection() {
      return "ConfigView.section.queue";
    }

    public Composite configSectionCreate(Composite parent) {
      // (Download) Stopping Automation Setup
      GridData gridData;
      GridLayout layout;
      Label label;
      Composite gStop = new Composite(parent, SWT.NONE);
      gStop.addControlListener(new Utils.LabelWrapControlListener());

      layout = new GridLayout();
      layout.numColumns = 2;
      gStop.setLayout(layout);
      gridData = new GridData(GridData.FILL_BOTH);
      gStop.setLayoutData(gridData);


      label = new Label(gStop, SWT.WRAP);
      gridData = new GridData(GridData.FILL_HORIZONTAL);
      gridData.horizontalSpan = 2;
      label.setLayoutData(gridData);
      Messages.setLanguageText(label, "ConfigView.label.autoStoppingInfo"); //$NON-NLS-1$

      label = new Label(gStop, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.stopRatio"); //$NON-NLS-1$
      final String stopRatioLabels[] = new String[11];
      final int stopRatioValues[] = new int[11];
      stopRatioLabels[0] = MessageText.getString("ConfigView.text.ignoreRule");
      stopRatioValues[0] = 0;
      for (int i = 1; i < 11; i++) {
        stopRatioLabels[i] = i + ":" + 1; //$NON-NLS-1$
        stopRatioValues[i] = i;
      }
      new IntListParameter(gStop, "Stop Ratio", 0, stopRatioLabels, stopRatioValues);

      label = new Label(gStop, SWT.WRAP);
      Messages.setLanguageText(label, "ConfigView.label.stopAfterMinutes"); //$NON-NLS-1$
      final String stopAfterLabels[] = new String[15];
      final int stopAfterValues[] = new int[15];
      String sMinutes = MessageText.getString("ConfigView.text.minutes");
      String sHours = MessageText.getString("ConfigView.text.hours");
      stopAfterLabels[0] = MessageText.getString("ConfigView.text.ignoreRule");
      stopAfterValues[0] = 0;
      stopAfterLabels[1] = "90 " + MessageText.getString("ConfigView.text.minutes");
      stopAfterValues[1] = 90;
      for (int i = 2; i < stopAfterValues.length; i++) {
        stopAfterLabels[i] = i + " " + sHours ;
        stopAfterValues[i] = i * 60;
      }
      IntListParameter stopAfterParam = new IntListParameter(gStop, "Stop After Minutes", 0,
                                                             stopAfterLabels, stopAfterValues);
      label.setEnabled(false);


      label = new Label(gStop, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.removeOnStop"); //$NON-NLS-1$
      BooleanParameter removeOnStopParam = new BooleanParameter(gStop, "Remove On Stop", false);
      label.setEnabled(false);

      return gStop;
    }

  	public String configSectionGetName() {
  		return MessageText.getString("ConfigView.section.queue.autoStopping");
  	}

  	public String configSectionGetID() {
  		return "ConfigView.section.queue.autoStopping";
  	}

    public void configSectionSave() {
      reloadConfigParams();
    }

    public void configSectionDelete() {
    }
  }

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
        if (iRankType == RANK_TIMED) {
          if (qr > QR_TIMED_QUEUED_ENDS_AT) {
            int timeLeft = (int)(minTimeAlive - 
                                 (long)(System.currentTimeMillis() - 
                                        dlData.getStartedSeedingOn())) / 1000;
            tableItem.setText(TimeFormater.format(timeLeft));
          } else {
            tableItem.setText(MessageText.getString("StartStopRules.waiting"));
          }
        } else {
          tableItem.setText(String.valueOf(qr));
        }
      }
      else if (qr == QR_RATIOMET)
        tableItem.setText(MessageText.getString("StartStopRules.ratioMet"));
      else if (qr == QR_NUMSEEDSMET)
        tableItem.setText(MessageText.getString("StartStopRules.numSeedsMet"));
      else if (qr == QR_NOTQUEUED)
        tableItem.setText("");
      else if (qr == QR_0SEEDSPEERS)
        tableItem.setText(MessageText.getString("StartStopRules.0SeedsPeers"));
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

