/*
 * Created on 22 juil. 2003
 *
 */
package org.gudy.azureus2.core3.tracker.client.classic;

import java.util.*;
import java.net.*;

import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.util.*;

/**
 * @author Olivier
 * 
 */
public class TrackerChecker implements TRTrackerScraperListener {

  /** List of Trackers. 
   * key = Tracker URL string
   * value = TrackerStatus object
   */
  private HashMap       trackers;
  private AEMonitor 	trackers_mon 	= new AEMonitor( "TrackerChecker:trackers" );

  /** TRTrackerScraperImpl object associated with this object.
   */
  private TRTrackerScraperImpl    scraper;
  
  /* Time when next scrape needs to be performed. */
  private long lNextScrapeTime = 0;
  /* The next scrape belongs to this TrackerStatus */
  private TrackerStatus nextTrackerStatus = null;
  /* Next scrape will be for this torrent's hash.  */
  private byte[] nextTrackerHash;
  
  
  private AEMonitor this_mon 	= new AEMonitor( "TrackerChecker" );

  /** Initialize TrackerChecker.  
   *
   * @note Since there is only one TRTrackerScraperImpl, there will only be one
   *       TrackerChecker instance.
   *
   * XXX: would Timer be better? "Timer tasks should complete quickly"
   */
  protected TrackerChecker(TRTrackerScraperImpl  _scraper) {
    scraper   = _scraper;
    scraper.addListener(this);
    
    trackers  = new HashMap();
    
    Thread t = new AEThread("Tracker Scrape") {
       public void runSupport() {
        runScrapes();
      }
    };
    
    t.setDaemon(true);
    t.setPriority(Thread.MIN_PRIORITY);
    t.start();
  }
  

  /** Retrieves the last cached Scraper Response based on a TRTrackerClient's
   * current URL (announce-list entry or announce) and its torrent's hash.
   *
   * @return The cached scrape response.  Can be null.
   */
  protected 
  TRTrackerScraperResponseImpl 
  getHashData(
  	TRTrackerClient tracker_client) 
  {
    try {
      return getHashData(tracker_client.getTrackerUrl(), 
                         tracker_client.getTorrent().getHashWrapper());

    } catch (TOTorrentException e) {
    	Debug.printStackTrace( e );
      return null;
    }
  } 
  
  /** Retrieves the last cached Scraper Response based on a TOTorrent's
   * Announce URL (not announce-list) and hash.
   *
   * @return The cached scrape response.  Can be null.
   */
  protected TRTrackerScraperResponseImpl 
  getHashData(
  	TOTorrent  	torrent,
	URL			target_url )
  {
    try {
      return getHashData(target_url==null?torrent.getAnnounceURL():target_url, 
                         torrent.getHashWrapper());
      
    } catch(TOTorrentException e) {
    	Debug.printStackTrace( e );
      return null;
    }
  }  

  /** Retrieves the last cached Scraper Response for the supplied tracker URL 
   *  and hash. If no cache has exists for the hash, one is created.
   *
   * @return The cached scrape response.  Can be null.
   */
  protected TRTrackerScraperResponseImpl getHashData(URL trackerUrl,
                                                     final HashWrapper hash) {
    // can be null when first called and url not yet set up...
    if ( trackerUrl == null ){
      return( null );
    }
  
    byte[] hashBytes = hash.getHash();

    TRTrackerScraperResponseImpl data = null;

    if (trackers.containsKey(trackerUrl)) {
      final TrackerStatus ts = (TrackerStatus) trackers.get(trackerUrl);
      data = ts.getHashData(hashBytes);
      if (data == null) {
        //System.out.println("data == null: " + trackerUrl + " : " + ByteFormatter.nicePrint(hashBytes, true));
        data = ts.addHash(hashBytes);
      }
    } else {
      //System.out.println( "adding hash for " + trackerUrl + " : " + ByteFormatter.nicePrint(hashBytes, true));
      final TrackerStatus ts = new TrackerStatus(scraper,trackerUrl);
      try{
      	trackers_mon.enter();
      	
        trackers.put(trackerUrl, ts);
      }finally{
      	
      	trackers_mon.exit();
      }
      data = ts.addHash(hashBytes);
    }
    
    return data;
  }
  
  /** Removes the scrape task and data associated with the TOTorrent's
   * Announce URL, announce-list data and hash.
   */
  protected void removeHash(TOTorrent torrent) {
    try{
      removeHash(torrent.getAnnounceURL().toString(), torrent.getHashWrapper());
      
      TOTorrentAnnounceURLSet[] sets = torrent.getAnnounceURLGroup().getAnnounceURLSets();
      
      for (int i=0;i<sets.length;i++){
      	
      	URL[]	urls = sets[i].getAnnounceURLs();
      	
      	for (int j=0;j<urls.length;j++){
      		
      		removeHash(urls[j].toString(), torrent.getHashWrapper());
      	}
      }
      
      
    } catch (TOTorrentException e) {
    	Debug.printStackTrace( e );
    }
  } 

  /** Removes the scrape task and data associated with the supplied tracker
   * URL and torrent hash.
   */
  protected void removeHash(String trackerUrl, HashWrapper hash) {

    TrackerStatus ts = (TrackerStatus) trackers.get(trackerUrl);
    if (ts != null){
      //System.out.println( "removing hash for " + trackerUrl );
      ts.removeHash(hash);
    }
  }
  
  /* Forced synchronous scrape of the supplied torrent.
   */
  protected void 
  syncUpdate(
  	TOTorrent 	torrent,
	URL			target_url ) 
  {
    if (torrent == null){
      return;
    }
    
    try {
      byte[] hash = torrent.getHash();
      
      try{
      	trackers_mon.enter();
      	
        Iterator iter = trackers.values().iterator();
        
        while (iter.hasNext()){
        	
          TrackerStatus ts = (TrackerStatus) iter.next();

          if ( 	target_url == null ||
          		target_url.equals( ts.getTrackerURL())){
          	
	          Map hashmap = ts.getHashes();
	
	          if ( hashmap.get( hash ) != null ){
	          	
	            ts.updateSingleHash( hash, true, false );
	            
	            return;
	          }
          }
        }
      }finally{
      	
      	trackers_mon.exit();
      }
    } catch (TOTorrentException e) {
    	Debug.printStackTrace( e );
    }
  }
    
  
  /** Loop indefinitely, waiting for the next scrape, and scraping.
   */
  private void runScrapes() {
    while (true) {
      //System.out.println("Waiting for " + (lNextScrapeTime - SystemTime.getCurrentTime()) + "ms");
      while (lNextScrapeTime == 0 || lNextScrapeTime > SystemTime.getCurrentTime()) {
        if ( SystemTime.isErrorLast5sec() ) break;
        try { 
          Thread.sleep(1000); 
        } catch (Exception e) {/**/}
      }

      if (nextTrackerStatus != null) {
        nextTrackerStatus.updateSingleHash(nextTrackerHash, false);
        try { Thread.sleep(250); } catch (Exception e) {/**/}
      }
      
      checkForNextScrape();
    }
  }
  
  /** Finds the torrent that will be needing a scrape next.
   *
   * XXX: Timer.schedule 
   */
  private void checkForNextScrape() 
  {
  	try{
  		this_mon.enter();
  	
	    // search for the next scrape
	    TRTrackerScraperResponseImpl nextResponse = null;
	    long lNewNextScrapeTime = 0;
	
	    try{
	    	trackers_mon.enter();
	    	
	      Iterator iter = trackers.values().iterator();
	      while (iter.hasNext()) {
	        TrackerStatus ts = (TrackerStatus) iter.next();
	        Map hashmap = ts.getHashes();
	        
	        try{
	        	ts.getHashesMonitor().enter();
	        	
	          Iterator iterHashes = hashmap.values().iterator();
	          while( iterHashes.hasNext() ) {
	            TRTrackerScraperResponseImpl response = (TRTrackerScraperResponseImpl)iterHashes.next();
	            long lResponseNextScrapeTime = response.getNextScrapeStartTime();
	            if ((response.getStatus() != TRTrackerScraperResponse.ST_SCRAPING) &&
	                (nextResponse == null || lResponseNextScrapeTime < lNewNextScrapeTime) && 
	                (nextTrackerStatus != response.getTrackerStatus() ||
	                 nextTrackerHash != response.getHash())) {
	              lNewNextScrapeTime = lResponseNextScrapeTime;
	              nextResponse = response;
	            }
	          } // while hashes
	        }finally{
	        	
	        	ts.getHashesMonitor().exit();
	        }
	      } // while trackers
	    }finally{
	    	
	    	trackers_mon.exit();
	    }
	    
	    // no next scrape was found.  search again in a minute
	    if (nextResponse == null) {
	      nextTrackerStatus = null;
	      lNextScrapeTime = SystemTime.getCurrentTime() + 1000 * 60;
	    } else {
	      nextTrackerStatus = nextResponse.getTrackerStatus();
	      nextTrackerHash = nextResponse.getHash();
	      lNextScrapeTime = lNewNextScrapeTime;
	    }
  	}finally{
  		
  		this_mon.exit();
  	}
  }


  // TRTrackerScraperListener
  /** Check if the new scrape's next scrape time is next in line.
    */
  public void scrapeReceived(TRTrackerScraperResponse response) {
    long lResponseNextScrapeTime = response.getNextScrapeStartTime();
    if (lResponseNextScrapeTime < lNextScrapeTime) {
      // next in line
      nextTrackerStatus = response.getTrackerStatus();
      nextTrackerHash = response.getHash();
      lNextScrapeTime = lResponseNextScrapeTime;
      //System.out.println("Next Scrape Time set to " + lNextScrapeTime);
      // XXX Timer.schedule(timetask, ..)
    }
  }
}
