/*
 * File    : GMSRDefaultPlugin.java
 * Created : 12-Jan-2004
 * By      : parg
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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.TabFolder;


import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.torrent.Torrent;

import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.core3.internat.MessageText;

public class 
StartStopRulesDefaultPlugin 
  implements Plugin
{ 
  private static final boolean DEBUG = false;

  protected PluginInterface     plugin_interface;
  protected PluginConfig        plugin_config;
  protected DownloadManager     download_manager;
  protected DownloadListener      download_listener;
  protected DownloadTrackerListener download_tracker_listener;
  
  protected ArrayList           download_data = new ArrayList();
  
  protected volatile boolean         closingDown;
  protected volatile boolean         somethingChanged;
  
  protected LoggerChannel   log;
  private long lastQRcalcTime = 0;
  private static final long RECALC_QR_EVERY = 15 * 1000;
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
  boolean enableQR;
  int iFakeFullCopySeedStart;

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
          if (new_state == Download.ST_SEEDING)
            synchronized (download_data) {
              Iterator iter = download_data.iterator();
              while (iter.hasNext()) {
                downloadData dl_data  = (downloadData) iter.next();
                if (dl_data.getDownloadObject() == download) {
                  dl_data.setStartedSeedingOn(System.currentTimeMillis());
                  break;
                }
              }
            }

        }
      };
    
    download_manager.addListener(
        new DownloadManagerListener()
        {
          public void downloadAdded( Download  download )
          {
            synchronized (download_data) {
              download_data.add( new downloadData(download) );
            }

            download.addListener( download_listener );
            
            somethingChanged = true;
          }
          
          public void downloadRemoved( Download  download )
          {
            download.removeListener( download_listener );
            
            synchronized (download_data) {
              Iterator iter = download_data.iterator();
              while (iter.hasNext()) {
                downloadData dl_data  = (downloadData) iter.next();
                if (dl_data.getDownloadObject() == download) {
                  download_data.remove(dl_data);
                  break;
                }
              }
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
  
  private void reloadConfigParams() {
    enableQR = plugin_config.getBooleanParameter("bRepositionCompleted");
    minPeersPerSeed = plugin_config.getIntParameter("Stop Peers Ratio", 0);
    minPeersToBoostNoSeeds = plugin_config.getIntParameter("minPeersToBoostNoSeeds");
    minSpeedForActiveDL = plugin_config.getIntParameter("minSpeedForActiveDL");
    minShareRatio = 1000 * plugin_config.getIntParameter("Stop Ratio", 0);
    maxActive = plugin_config.getIntParameter("max active torrents");
    maxDownloads = plugin_config.getIntParameter("max downloads");
    numPeersAsFullCopy = plugin_config.getIntParameter("numPeersAsFullCopy");
    // Ignore torrent if seed count is at least..
    ignoreSeedCount = plugin_config.getIntParameter("Ignore Seed Count", 0);
    iFakeFullCopySeedStart = plugin_config.getIntParameter("iFakeFullCopySeedStart");
  }
  
  protected void process() {
    long  process_time = System.currentTimeMillis();
    
    int totalSeeding = 0;
    int totalForcedSeeding = 0;
    int totalWaitingToSeed = 0;
    int totalWaitingToDL = 0;
    int totalDownloading = 0;
    int activeDLCount = 0;
    int totalIncomplete = 0;
    int totalCompleteQueued = 0;
    int totalIncompleteQueued = 0;
    
    boolean recalcQR = enableQR && ((process_time - lastQRcalcTime) > RECALC_QR_EVERY);
    if (somethingChanged) {
    	somethingChanged = false;
    	recalcQR = enableQR;
    }
	  if (recalcQR)
  	  lastQRcalcTime = System.currentTimeMillis();
      
    boolean downloadIsPreparing = false;

    // give Complete a delay so that we have time to scrape and get the downloads going
    boolean okToProcessComplete = (System.currentTimeMillis() - startedOn) > 20000;

    synchronized (download_data) {
      Iterator iter = download_data.iterator();
      // Loop 1 of 2: 
      // - Build a QR list for sorting
      // - Build Count Totals
      // - Do anything that doesn't need to be done in Queued order
      while (iter.hasNext()) {
        downloadData dl_data  = (downloadData) iter.next();

        int qr = (recalcQR) ? dl_data.recalcQR() : dl_data.getQR();

        // We don't need to do any counting if we are quiting early
        if (downloadIsPreparing)
          continue;
  
        Download download = dl_data.getDownloadObject();
        int state = download.getState();

        //When PREPARING, getCompleted doesn't refer to "download done %"
        //it could refer to % of allocating done, or % of checking done.
        //Until we have a way of finding out if the torrent is done downloading,
        //break out of processing
        if (state == Download.ST_PREPARING) {
          downloadIsPreparing = true;
          continue;
        }

        if (state == Download.ST_DOWNLOADING && !download.isForceStart()) {
          totalDownloading++;
          // Only increase activeDLCount if there's downloading 
          // or if the torrent just recently started (ie. give it a chance to get some connections)
          if ((download.getStats().getDownloadAverage() >= minSpeedForActiveDL) ||
              (System.currentTimeMillis() - download.getStats().getTimeStarted() <= 30000))
            activeDLCount++;
        }
  
        // All of these are either seeding or about to be seeding
        if (download.getStats().getCompleted() == 1000) {
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
            
            // must remove from download_data first, otherwise we end up in infinite wait loop
            download_data.remove(dl_data);
            download.remove();
            
          }catch( DownloadRemovalVetoException e ){
            
            e.printStackTrace();
            
          }catch( DownloadException e ){
            
            e.printStackTrace();
          }
        }
      }
      
      int maxSeeders = (maxActive == 0) ? 99999 : maxActive - activeDLCount;
      // XXX put in subtraction logic here
  
      // We can also quit early if:
      // - we don't have any torrents waiting (these have to either be started, queued, or stopped)
      // - We match the limits for DL & Seeding
      // - We have less than the limits for DL &/or seeding, but there are no other torrents in the queue
      boolean quitEarly = ((totalSeeding == maxSeeders) || 
                           (totalSeeding < maxSeeders && totalCompleteQueued == 0)
                          ) &&
                          (totalWaitingToSeed == 0) &&
                          ((totalDownloading == maxDownloads) || 
                           (totalDownloading < maxDownloads && totalIncompleteQueued == 0)
                          ) &&
                          (totalWaitingToDL == 0) &&
                          (!recalcQR);

	  log.log( LoggerChannel.LT_INFORMATION, "quitEarly="+quitEarly+" DLPrep="+downloadIsPreparing+" totalWaitingToSeed="+totalWaitingToSeed);
      if (quitEarly || downloadIsPreparing){
      	
        return;
      }
      
      // Sort by QR
      Collections.sort(download_data);

      iter = download_data.iterator();
  
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
      while (iter.hasNext()) {
        downloadData dl_data  = (downloadData) iter.next();
        Download download = dl_data.getDownloadObject();
  
        //log.log( LoggerChannel.LT_INFORMATION, "["+download.getTorrent().getName()+"]: state="+download.getState()+";qr="+dl_data.getQR()+";compl="+download.getStats().getCompleted());
        // Initialize STATE_WAITING torrents
        if ((download.getState() == Download.ST_WAITING) && 
            !getAlreadyAllocatingOrChecking()) {
          try{
            download.initialize();
          }catch (Exception ignore) {/*ignore*/}
        }
        
        //See PREPARING notes in Loop 1 of 2;
        if (download.getState() == Download.ST_PREPARING) {
          break;
        }

        if (enableQR && download.getStats().getCompleted() == 1000 && okToProcessComplete)
          download.setPosition(++posComplete);

        // Never do anything to stopped entries
        if (download.getState() == Download.ST_STOPPING ||
            download.getState() == Download.ST_STOPPED ||
            download.getState() == Download.ST_ERROR) {
          continue;
        }
              
        // Handle incomplete DLs
        if (download.getStats().getCompleted() != 1000) {
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
        else if (okToProcessComplete) { // completed
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
          if (okToStop && (state == Download.ST_SEEDING)) {
            // Min time check
            long timeAlive = System.currentTimeMillis() - download.getStats().getTimeStarted();
            long minTimeAlive = plugin_config.getIntParameter("Min Seeding Time", 60*3) * 1000;
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
            //If there are no seeds, avoid / by 0
            if (numSeeds != 0) {
              int numPeers = calcPeersNoUs(download);
              float ratio = (float) numPeers / numSeeds;
              if (ratio <= minPeersPerSeed) {
                try {
                  download.stopAndQueue();
                } catch (Exception ignore) {/*ignore*/}
                dl_data.setQR(-2);
              }
            }
          }
  
          if (download.getState() == Download.ST_QUEUED)
            higherQueued = true;
        } // getCompleted == 1000
      } // Loop 2/2 (Start/Stopping)
    } // synchronize
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
    //log.log( LoggerChannel.LT_INFORMATION, "["+download.getTorrent().getName()+"]: sr.getScrapeStartTime()"+sr.getScrapeStartTime());
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
      //log.log( LoggerChannel.LT_INFORMATION, "["+dl.getTorrent().getName()+"]: qr set to "+qr);
    }
    
    public long getStartedSeedingOn() {
      return startedSeedingOn;
    }

    public void setStartedSeedingOn(long time) {
      //log.log( LoggerChannel.LT_INFORMATION, "["+dl.getTorrent().getName()+"]: startedSeedingOn set!");
      startedSeedingOn = time;
    }
    
    public int recalcQR() {
      //When PREPARING, getCompleted doesn't refer to "download done %"
      //it could refer to % of allocating done, or % of checking done.
      //Until we have a way of finding out if the torrent is done downloading,
      //break out of processing
      if (dl.getState() == Download.ST_PREPARING)
        return qr;
      
      DownloadStats stats = dl.getStats();
      int numCompleted = stats.getCompleted();
  
      // make undownloaded sort to top so they start can first.
      if (numCompleted < 1000) {
        setQR(10000 - dl.getPosition());
        return qr;
      }
  
      int shareRatio = stats.getShareRatio();
      
      int numPeers = calcPeersNoUs(dl);
      int numSeeds = calcSeedsNoUs(dl, startedSeedingOn);
      if (numPeersAsFullCopy != 0 && numSeeds >= iFakeFullCopySeedStart)
          numSeeds += numPeers / numPeersAsFullCopy;

  //log.log( LoggerChannel.LT_INFORMATION, "["+dl.getTorrent().getName()+"]: Peers="+numPeers+"; Seeds="+numSeeds);
  
      if ((numCompleted == 1000) && 
          (shareRatio > 500 || shareRatio == -1)) {
        // Skip if Stop Peers Ratio exceeded
        // (More Peers for each Seed than specified in Config)
        //0 means never stop
        if (minPeersPerSeed != 0 && numSeeds != 0) {
          float ratio = (float) numPeers / numSeeds;
          if (ratio < minPeersPerSeed) {
            setQR(-2);
            return -2;
          }
        }
        //0 means disabled
        if ((ignoreSeedCount != 0) && (numSeeds >= ignoreSeedCount)) {
          setQR(-3);
          return -3;
        }
      }
  
      // The one point adjustments were added just to make the
      // Queue ranking more varied (less torrents being all at one
      // ranking)
      int newQR = 0;
  
      // Make torrents with < 1.0 share ratio very important
      // (but not more important than unfinished downloads)
      if (shareRatio != -1) {
        if (shareRatio < 1000) {
          // Add up to 500
          newQR += (1000 - shareRatio) / 2;
          // extra hike for less than .5
          if (shareRatio <= 500)
            newQR += 2000;
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
/*
      long totalAverage = stats.getTotalAverage();
      if (totalAverage != 0) {
        int connectedPeers = getnumPeers();
        if (connectedPeers != 0) {
          int nbDLperPeer = totalAverage / connectedPeers;
          //Up to 50 added to newQR if total average DL speed is below 10k
          //XXX: 30 should be user configurable?
          final int MAXTOADD = 50;
          final int BELOWK = 10240;
          if (nbDLperPeer < BELOWK)
            newQR += MAXTOADD - (nbDLperPeer * MAXTOADD / BELOWK);
        }
      }
*/        
      if (numPeers != 0) {
        if (numSeeds == 0) {
          //XXX: Problem: 
          // torrent is stopped, and has 0 seeds.  It gets 2000 points
          // because we can't get availability.
          // torrent moves to top of newQR and starts.
          // availability data is available, and returns that we are seeing 2 copies
          // less thatn 2000 points are added instead of 2000
          // so it drops in the queue and gets stopped
          // repeat above
          
          // Solution?
          // Have a "minimum seeding time" timer. ie. Once seeding has started,
          // it doesn't automatically stop for at least x minutes.
  
          // No seeds, so check availability
          // XXX: Availability stats not available to plugins, disable for now
          int[] available = null;
          try {
            int x = 0/0;
            //available = getPeerManager().getAvailability();
          } catch (Exception e) { }
          if (available != null) {
            int numPieces = available.length;
            int minCopies = available[0];
            int adj = 0;
            
            if (dl.getState() == Download.ST_SEEDING)
              adj = -1;
            for (int x = 1; x < numPieces; x++) {
              if (available[x] < minCopies) {
                minCopies = available[x];
                if (minCopies + adj == 0)
                  break;
              }
            } // for
            
            //If one is available, it's only ours, so there is no full copy
            if (minCopies + adj == 0)
              newQR += 2000;
            else
              newQR += 1500 + (500 / (minCopies + adj + 1));
             //LGLogger.log(LGLogger.INFORMATION, "** i=" + i + "; minCopies="+minCopies+" adj="+adj+" newQR="+newQR);
          }
          else { // No Availablility Stats && numSeeds == 0 && numPeers != 0
            // Can't get availability, so make this very important
            if (numPeers >= minPeersToBoostNoSeeds)
              newQR += 2000;
            //LGLogger.log(0, getName() + " noseeds and peers = " + numPeers + " minPeersToBoostNoSeeds = " + minPeersToBoostNoSeeds);
          } // (no) availability
          
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
      
      if (newQR < 0)
        newQR = 1;
  
      // 0 usually means we have 0 seeds and 0 peers, which usually means
      // we don't have a connection to the tracker.  If that's the case,
      // skip setting QR.
      if (newQR != 0 || qr > 9000)
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
      new IntListParameter(gMainTab, "minSpeedForActiveDL", 512, activeDLLabels, activeDLValues);

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
  
      label = new Label(gQR, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.enableSeedingQR"); //$NON-NLS-1$
      new BooleanParameter(gQR, "bRepositionCompleted");
  
      label = new Label(gQR, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.minSeedingTime"); //$NON-NLS-1$    
      gridData = new GridData();
      gridData.widthHint = 40;
      new IntParameter(gQR, "Min Seeding Time", 60*3).setLayoutData(gridData);
  
      label = new Label(gQR, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.minPeersToBoostNoSeeds"); //$NON-NLS-1$    
      final String boostQRPeersLabels[] = new String[10];
      final int boostQRPeersValues[] = new int[10];
      String peers = MessageText.getString("ConfigView.text.peers");
      for (int i = 0; i < boostQRPeersValues.length; i++) {
        boostQRPeersLabels[i] = i + " " + peers; //$NON-NLS-1$
        boostQRPeersValues[i] = i;
      }
      gridData = new GridData();
      new IntListParameter(gQR, "minPeersToBoostNoSeeds", boostQRPeersLabels, boostQRPeersValues);
      
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
      
      Composite cArea = new Composite(gQR, SWT.NULL);
      layout = new GridLayout();
      layout.marginHeight = 0;
      layout.marginWidth = 0;
      layout.numColumns = 2;
      cArea.setLayout(layout);
      cArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

      gridData = new GridData();
      gridData.widthHint = 20;
      new IntParameter(cArea, "numPeersAsFullCopy").setLayoutData(gridData);
      label = new Label(cArea, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.text.peers");

      cArea = new Composite(gQR, SWT.NULL);
      layout = new GridLayout();
      layout.marginHeight = 0;
      layout.marginWidth = 0;
      layout.numColumns = 3;
      cArea.setLayout(layout);
      gridData = new GridData(GridData.FILL_HORIZONTAL);
      gridData.horizontalIndent = 15;
      gridData.horizontalSpan = 2;
      cArea.setLayoutData(gridData);
      
      label = new Label(cArea, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.qr.iFakeFullCopySeedStart");
      gridData = new GridData();
      gridData.widthHint = 20;
      new IntParameter(cArea, "iFakeFullCopySeedStart").setLayoutData(gridData);
      label = new Label(cArea, SWT.NULL);
      Messages.setLanguageText(label, "ConfigView.label.seeds");


      return gQR;
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
} // class

