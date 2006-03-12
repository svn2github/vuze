/*
 * Created on 22 juil. 2003
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package org.gudy.azureus2.core3.tracker.client.impl.bt;

import java.util.*;
import java.net.*;

import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.tracker.client.impl.TRTrackerScraperResponseImpl;
import org.gudy.azureus2.core3.util.*;

/**
 * @author Olivier
 * 
 */
public class TrackerChecker implements AEDiagnosticsEvidenceGenerator, SystemTime.consumer {
	private final static LogIDs LOGID = LogIDs.TRACKER;

  /** List of Trackers. 
   * key = Tracker URL string
   * value = TrackerStatus object
   */
  private HashMap       trackers;
  private AEMonitor 	trackers_mon 	= new AEMonitor( "TrackerChecker:trackers" );

  /** TRTrackerScraperImpl object associated with this object.
   */
  private TRTrackerBTScraperImpl    scraper;
    
  /** Initialize TrackerChecker.  
   *
   * @note Since there is only one TRTrackerScraperImpl, there will only be one
   *       TrackerChecker instance.
   *
   */
  
  protected TrackerChecker(TRTrackerBTScraperImpl  _scraper) {
    scraper   = _scraper;
       
    trackers  = new HashMap();
    
    Thread t = new AEThread("Tracker Scrape") {
       public void runSupport() {
        runScrapes();
      }
    };
    
    t.setDaemon(true);
    t.setPriority(Thread.MIN_PRIORITY);
    t.start();
    
    AEDiagnostics.addEvidenceGenerator( this );
    
    SystemTime.registerClockChangeListener( this );
  }
  

  /** Retrieves the last cached Scraper Response based on a TRTrackerClient's
   * current URL (announce-list entry or announce) and its torrent's hash.
   *
   * @return The cached scrape response.  Can be null.
   */
  protected 
  TRTrackerScraperResponseImpl 
  getHashData(
  	TRTrackerAnnouncer tracker_client) 
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
  protected TRTrackerScraperResponseImpl 
  getHashData(
	URL trackerUrl,
    final HashWrapper hash) 
  {
    // can be null when first called and url not yet set up...
    if ( trackerUrl == null ){
      return( null );
    }
  
    byte[] hashBytes = hash.getHash();

    TRTrackerScraperResponseImpl data = null;

    	// DON'T USE URL as a key in the trackers map, use the string version. If you
    	// use a URL then the "containsKey" method does a URL.equals test. This does not
    	// simply check on str equivalence, it tries to resolve the host name. this can
    	// result in significant hangs (several seconds....)
    
    String	url_str = trackerUrl.toString();
        
    try{
        trackers_mon.enter();
    	
        TrackerStatus ts = (TrackerStatus) trackers.get(url_str);
    
        if ( ts != null ){
	      
	      data = ts.getHashData(hashBytes);
	      
	      if (data == null) {
	    	  
	        //System.out.println("data == null: " + trackerUrl + " : " + ByteFormatter.nicePrint(hashBytes, true));
	    	  
	        data = ts.addHash(hashBytes);
	      }
	    }else{
    
	    	//System.out.println( "adding hash for " + trackerUrl + " : " + ByteFormatter.nicePrint(hashBytes, true));
      
	    	ts = new TrackerStatus(scraper.getScraper(),trackerUrl);
      
	        trackers.put(url_str, ts);

        	data = ts.addHash(hashBytes);

	        if( !ts.isTrackerScrapeUrlValid() ) {
  
		      	if (Logger.isEnabled()){
							Logger.log(new LogEvent(TorrentUtils.getDownloadManager(hashBytes), LOGID,
									LogEvent.LT_ERROR, "Can't scrape using url '" + trackerUrl
											+ "' as it doesn't end in " + "'/announce', skipping."));
		      	}
	        }
	    }
    
    }finally{
      	
        trackers_mon.exit();
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
    
      TrackerStatus matched_ts = null;
      
      try{
      	trackers_mon.enter();
      	
        Iterator iter = trackers.values().iterator();
        
        while (iter.hasNext()){
        	
          TrackerStatus ts = (TrackerStatus) iter.next();

          if ( 	target_url == null ||
          		target_url.toString().equals( ts.getTrackerURL().toString())){
          	
	          Map hashmap = ts.getHashes();
	
		      try{
		    	  ts.getHashesMonitor().enter();

		          if ( hashmap.get( new HashWrapper( hash )) != null ){
		          	
		        	matched_ts	= ts;
		        	  
		        	break;
		          }
		      }finally{
		    	  
		    	  ts.getHashesMonitor().exit();
		      }
          }
        }
      }finally{
      	
      	trackers_mon.exit();
      }
      
      if ( matched_ts != null ){
    	  
    	  matched_ts.updateSingleHash( hash, true, false );
      }
    }
    catch (Throwable e) {
      Debug.out( "scrape syncUpdate() exception", e );
    }
  }
    
  
  /** Loop indefinitely, waiting for the next scrape, and scraping.
   */
  private void 
  runScrapes() 
  {
	TRTrackerBTScraperResponseImpl next_response_to_scrape	= null;

    while( true ){

    	long	delay;
    	
      	if ( next_response_to_scrape == null ){
        
      		delay	= 60000;	// nothing going on, recheck in a min
      		
      	}else{
      		
      		long	scrape_time = next_response_to_scrape.getNextScrapeStartTime();
      		
      		long	time_to_scrape = scrape_time - SystemTime.getCurrentTime();
      		
      		if ( time_to_scrape <= 0 ){
      			
	      		try{
	      			next_response_to_scrape.getTrackerStatus().updateSingleHash(
	      					next_response_to_scrape.getHash(), false);
	        
	      			delay	= 250;	// pick up next scrape fairly quickly
	      			
	      		}catch( Throwable e ){
	      			
	      			Debug.printStackTrace(e);
	      			
	      			delay	= 30000;
	      		}
      		}else{
      			
      			delay	= time_to_scrape;
      			
      			if ( delay > 30000 ){
      				
      				delay	= 30000;	// don't sleep too long in case new hashes are added etc.
      			}
      		}
      	}
      	
      	try{ 
      		Thread.sleep(delay); 
      		
      	}catch(Exception e){
      	}
      
      	next_response_to_scrape = checkForNextScrape();
    }
  }
  
  /** Finds the torrent that will be needing a scrape next.
   *
   */
  
  private TRTrackerBTScraperResponseImpl 
  checkForNextScrape() 
  {
	    // search for the next scrape
	  
	    TRTrackerBTScraperResponseImpl next_response_to_scrape = null;
	    
	    long earliest = Long.MAX_VALUE;
	
	    try{
	    	trackers_mon.enter();
	    	
	    	Iterator iter = trackers.values().iterator();
	      
	    	while (iter.hasNext()) {
	    		
	    		TrackerStatus ts = (TrackerStatus) iter.next();
	    		
	    		if ( !ts.isTrackerScrapeUrlValid()){
	    			
	    			continue;
	    		}
	    		
	    		Map hashmap = ts.getHashes();
	    		  
	    		try{
	    			ts.getHashesMonitor().enter();
	        	
	    			Iterator iterHashes = hashmap.values().iterator();
	    			
	    			while( iterHashes.hasNext() ) {
	            
	    				TRTrackerBTScraperResponseImpl response = (TRTrackerBTScraperResponseImpl)iterHashes.next();    				
	            
	    					// ignore ones already scraping
	    				
	    				if ( response.getStatus() != TRTrackerScraperResponse.ST_SCRAPING ){
	    				
	    					if ( response.getNextScrapeStartTime() < earliest ){
	    						
	    						earliest	= response.getNextScrapeStartTime();
	    						
	    						next_response_to_scrape	= response;
	    					}
	    				}
	    			}
	    		}finally{
	        	
	    			ts.getHashesMonitor().exit();
	    		}
	    	} 
	    }finally{
	    	
	    	trackers_mon.exit();
	    }
	    
	    return( next_response_to_scrape );
  	}


  	public void
  	consume(
  		long	offset )
  	{	
  		if ( Math.abs( offset ) < 60*1000 ){
  			
  			return;
  		}
  		
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
	            
	    				TRTrackerBTScraperResponseImpl response = (TRTrackerBTScraperResponseImpl)iterHashes.next();    				
	            
	    				long	time = response.getNextScrapeStartTime();
	
	    				if ( time > 0 ){
	    						    					
	    					response.setNextScrapeStartTime( time + offset );
	    				}
	    			}
	    		}finally{
	        	
	    			ts.getHashesMonitor().exit();
	    		}
	    	} 
	    }finally{
	    	
	    	trackers_mon.exit();
	    }
  	}
  	
	public void
	generate(
		IndentWriter		writer )
	{
		writer.println( "BTScraper - now = " + SystemTime.getCurrentTime());
		
		try{
			writer.indent();

		    try{
		    	trackers_mon.enter();
			    	
			    Iterator iter = trackers.entrySet().iterator();
			    
			    while (iter.hasNext()){
			    	
			    	Map.Entry	entry = (Map.Entry)iter.next();
			    	
			        TrackerStatus 	ts = (TrackerStatus)entry.getValue();
			    	
			    	writer.println( "Tracker: " + ts.getString());   	
			        
			        try{
			        	writer.indent();
			        	
			        	ts.getHashesMonitor().enter();
			        	
				        Map hashmap = 	ts.getHashes();
				        
				        Iterator iter_hashes = hashmap.entrySet().iterator();
	
				        while (iter_hashes.hasNext()){
				        	
					    	Map.Entry	hash_entry = (Map.Entry)iter_hashes.next();
					    	
					    	TRTrackerBTScraperResponseImpl	response = (TRTrackerBTScraperResponseImpl)hash_entry.getValue();
					    	
					    	writer.println( response.getString());
				        }
			        }finally{
			        	
			        	ts.getHashesMonitor().exit();
			        	
			        	writer.exdent();
			        }
			    }
		    }finally{
		    	
		    	trackers_mon.exit();
		    }
			    
		}finally{
			
			writer.exdent();
		}
	}
}
