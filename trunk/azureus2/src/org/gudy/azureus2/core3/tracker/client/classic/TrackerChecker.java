/*
 * Created on 22 juil. 2003
 *
 */
package org.gudy.azureus2.core3.tracker.client.classic;

import java.util.*;


import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.util.*;

/**
 * @author Olivier
 * 
 */
public class TrackerChecker {

  private HashMap 					trackers; 
  private TRTrackerScraperImpl		scraper;
  
  private Map toUpdate;  
  
  protected TrackerChecker(
  		TRTrackerScraperImpl	_scraper ) 
  {
  	scraper		= _scraper;
  	
    trackers 	= new HashMap();
    
    toUpdate = new HashMap();
    
    Thread t = new Thread("Tracker Scrape") {
       public void run() 
       {
       	runScrapes();
      }
    };
    
    t.setDaemon(true);
    t.setPriority(Thread.MIN_PRIORITY);
    t.start();
  }
  

  protected TRTrackerScraperResponseImpl 
  getHashData(
  	TRTrackerClient	tracker_client ) 
  {
  	try{
  	
  		return( getHashData( tracker_client.getTrackerUrl(), tracker_client.getTorrent().getHashWrapper()));
  		
  	}catch( TOTorrentException e ){
  		
  		e.printStackTrace();
  		
  		return( null );
  	}
  }	
  
   protected TRTrackerScraperResponseImpl 
   getHashData(
	 TOTorrent	torrent ) 
   {
	 try{
  	
		 return( getHashData( torrent.getAnnounceURL().toString(), torrent.getHashWrapper()));
  		
	 }catch( TOTorrentException e ){
  		
		 e.printStackTrace();
  		
		 return( null );
	 }
   }	

	protected void 
 	removeHash(
   		TRTrackerClient	tracker_client ) 
 	{
   		try{
  	
	   		removeHash( tracker_client.getTrackerUrl(), tracker_client.getTorrent().getHashWrapper());
  		
   		}catch( TOTorrentException e ){
  		
	   		e.printStackTrace();

		}
 	}	
  
	protected void 
  	removeHash(
		TOTorrent	torrent ) 
  	{
		try{
  	
			removeHash( torrent.getAnnounceURL().toString(), torrent.getHashWrapper());
  		
		}catch( TOTorrentException e ){
  		
			e.printStackTrace();
		}
  	}	

	protected void 
	removeHash(String trackerUrl,HashWrapper hash) 
	{
		// TODO: this doesn't handle multiple tracker torrents yet
		
	    TrackerStatus ts = (TrackerStatus) trackers.get(trackerUrl);
	    
	    if( ts != null ){
	    	
	    	//System.out.println( "removing hash for " + trackerUrl );
	    	
	      	ts.removeHash(hash);
	    }
	}
  
	protected TRTrackerScraperResponseImpl getHashData(String trackerUrl,final HashWrapper hash) 
  {
		// can be null when first called and url not yet set up...
		
	if ( trackerUrl == null ){
		return( null );
	}
	
    if (trackers.containsKey(trackerUrl)) {
      final TrackerStatus ts = (TrackerStatus) trackers.get(trackerUrl);
      TRTrackerScraperResponseImpl data = ts.getHashData(hash);
      if(data != null)
        return data;
      else {
        synchronized( toUpdate ) {
          toUpdate.put(hash, ts);
        }
        return null;
      }        
    }
    
    //System.out.println( "adding hash for " + trackerUrl );
    final TrackerStatus ts = new TrackerStatus(scraper,trackerUrl);
    synchronized (trackers) {
      trackers.put(trackerUrl, ts);
    }
    synchronized( toUpdate ) {
      toUpdate.put(hash, ts);
    }
    return null;
  }
  

  protected void asyncUpdateAll() {
    synchronized (trackers) {
      Iterator iter = trackers.values().iterator();
      while (iter.hasNext()) {
        TrackerStatus ts = (TrackerStatus) iter.next();
        Map hashmap = ts.getHashes();
        
        synchronized( hashmap ){
          Iterator iterHashes = hashmap.keySet().iterator();
          while( iterHashes.hasNext() ) {
            HashWrapper hash = (HashWrapper) iterHashes.next();
            synchronized( toUpdate ) {
              toUpdate.put(hash, ts);
            }
          }
        }
      }
    }
  }

  
  private void runScrapes() {
    while (true) {
      Map doUpdate = null;
      
      // get the list
    	synchronized(toUpdate) {
        if ( !toUpdate.isEmpty() ) {
          doUpdate = new HashMap(toUpdate);
          toUpdate.clear();
        }
      }
      
      // run the updates
      if (doUpdate != null) {
        Iterator it = doUpdate.keySet().iterator();
        while( it.hasNext() ) {
          HashWrapper hash = (HashWrapper)it.next();
          TrackerStatus ts = (TrackerStatus)doUpdate.get(hash);

          ts.updateSingleHash(hash);

          try { Thread.sleep(25); } catch (Exception e) {/**/}
        }
      }
      else {
        try { Thread.sleep(1000); } catch (Exception e) {/**/}
      }
    }
  }

          
          


}
