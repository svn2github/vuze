/*
 * Created on 22 juil. 2003
 *
 */
package org.gudy.azureus2.core3.tracker.client.classic;

import java.util.HashMap;
import java.util.Iterator;


import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.tracker.client.*;

/**
 * @author Olivier
 * 
 */
public class TrackerChecker {

  private HashMap trackers;

  protected TrackerChecker() {
    trackers = new HashMap();
  }

  protected TRTrackerScraperResponseImpl 
  getHashData(
  	TRTrackerClient	tracker_client ) 
  {
  	try{
  	
  		return( getHashData( tracker_client.getTrackerUrl(), tracker_client.getTorrent().getHash()));
  		
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
  	
		 return( getHashData( torrent.getAnnounceURL().toString(), torrent.getHash()));
  		
	 }catch( TOTorrentException e ){
  		
		 e.printStackTrace();
  		
		 return( null );
	 }
   }	

   protected TRTrackerScraperResponseImpl getHashData(String trackerUrl, byte[] hash) 
	{
  		if(trackerUrl != null)
			return getHashData(trackerUrl,new Hash(hash));
  		return null;
	}

	protected void 
 	removeHash(
   		TRTrackerClient	tracker_client ) 
 	{
   		try{
  	
	   		removeHash( tracker_client.getTrackerUrl(), new Hash(tracker_client.getTorrent().getHash()));
  		
   		}catch( TOTorrentException e ){
  		
	   		e.printStackTrace();

		}
 	}	
  
	protected void 
  	removeHash(
		TOTorrent	torrent ) 
  	{
		try{
  	
			removeHash( torrent.getAnnounceURL().toString(), new Hash(torrent.getHash()));
  		
		}catch( TOTorrentException e ){
  		
			e.printStackTrace();
		}
  	}	

	protected void 
	removeHash(String trackerUrl,Hash hash) 
	{
		// TODO: this doesn't handle multiple tracker torrents yet
		
	    TrackerStatus ts = (TrackerStatus) trackers.get(trackerUrl);
	    
	    if( ts != null ){
	    	
	    	//System.out.println( "removing hash for " + trackerUrl );
	    	
	      	ts.removeHash(hash);
	    }
	}
  
	protected TRTrackerScraperResponseImpl getHashData(String trackerUrl,final Hash hash) 
  {
    if (trackers.containsKey(trackerUrl)) {
      final TrackerStatus ts = (TrackerStatus) trackers.get(trackerUrl);
      TRTrackerScraperResponseImpl data = ts.getHashData(hash);
      if(data != null)
        return data;
      else {
        ts.asyncUpdate(hash);
        return null;
      }        
    }
    
	//System.out.println( "adding hash for " + trackerUrl );

    final TrackerStatus ts = new TrackerStatus(trackerUrl);
    synchronized (trackers) {
      trackers.put(trackerUrl, ts);
    }
    ts.asyncUpdate(hash);
    return null;
  }

  public void update() {
    synchronized (trackers) {
      Iterator iter = trackers.values().iterator();
      while (iter.hasNext()) {
        TrackerStatus ts = (TrackerStatus) iter.next();
        Iterator iterHashes = ts.getHashesIterator();
        while(iterHashes.hasNext()) {              
          Hash hash = (Hash) iterHashes.next();
          ts.asyncUpdate(hash);
        }                      
      }
    }
  }

}
