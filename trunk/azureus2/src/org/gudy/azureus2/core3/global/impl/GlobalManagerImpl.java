/*
 * File    : GlobalManagerImpl.java
 * Created : 21-Oct-2003
 * By      : stuff
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

package org.gudy.azureus2.core3.global.impl;

/*
 * Created on 30 juin 2003
 *
 */

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.Collections;

import org.gudy.azureus2.core3.global.*;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.tracker.host.*;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.stats.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.category.CategoryManager;
import org.gudy.azureus2.core3.category.Category;

/**
 * @author Olivier
 * 
 */
public class GlobalManagerImpl 
	implements 	GlobalManager
{
		// GlobalManagerListener support
		// Must be an async listener to support the non-synchronized invocation of
		// listeners when a new listener is added and existing downloads need to be
		// reported
	
	private static final int LDT_MANAGER_ADDED			= 1;
	private static final int LDT_MANAGER_REMOVED		= 2;
	private static final int LDT_DESTROY_INITIATED		= 3;
	private static final int LDT_DESTROYED				= 4;
	
	private ListenerManager	listeners 	= ListenerManager.createAsyncManager(
		"GM:ListenDispatcher",
		new ListenerManagerDispatcher()
		{
			public void
			dispatch(
				Object		_listener,
				int			type,
				Object		value )
			{
				GlobalManagerListener	target = (GlobalManagerListener)_listener;
		
				if ( type == LDT_MANAGER_ADDED ){
					
					target.downloadManagerAdded((DownloadManager)value);
					
				}else if ( type == LDT_MANAGER_REMOVED ){
					
					target.downloadManagerRemoved((DownloadManager)value);
					
				}else if ( type == LDT_DESTROY_INITIATED ){
					
					target.destroyInitiated();
					
				}else if ( type == LDT_DESTROYED ){
					
					target.destroyed();
					
				}
			}
		});
	
		// GlobalManagerDownloadWillBeRemovedListener support
		// Not async (doesn't need to be and can't be anyway coz it has an exception)
	
	private static final int LDT_MANAGER_WBR			= 1;
	
	private ListenerManager	removal_listeners 	= ListenerManager.createManager(
			"GM:DLWBRMListenDispatcher",
			new ListenerManagerDispatcherWithException()
			{
				public void
				dispatchWithException(
					Object		_listener,
					int			type,
					Object		value )
				
					throws GlobalManagerDownloadRemovalVetoException
				{					
					GlobalManagerDownloadWillBeRemovedListener	target = (GlobalManagerDownloadWillBeRemovedListener)_listener;
					
					target.downloadWillBeRemoved((DownloadManager)value);
				}
			});
	
	private List 	managers			= new ArrayList();
	private Map		manager_map			= new HashMap();
	
	private TRHost	tracker_host;
	
	private Checker checker;
	private GlobalManagerStatsImpl	stats;
  private TRTrackerScraper 			trackerScraper;
  private StatsWriterPeriodic		stats_writer;
  private boolean 					isStopped = false;
  private boolean					destroyed;
  
  public class Checker extends Thread {
    boolean finished = false;
    int loopFactor;
    private static final int waitTime = 1000;
    // 5 minutes save resume data interval (default)
    private int saveResumeLoopCount = 300000 / waitTime;
    

    public Checker() {
      super("Global Status Checker");
      loopFactor = 0;
      setPriority(Thread.MIN_PRIORITY);
      //determineSaveResumeDataInterval();
    }

    private void determineSaveResumeDataInterval() {
      int saveResumeInterval = COConfigurationManager.getIntParameter("Save Resume Interval", 5);
      if (saveResumeInterval > 1 && saveResumeInterval < 21)
        saveResumeLoopCount = saveResumeInterval * 60000 / waitTime;
    }

    public void run() {
      while (!finished) {

      	try{
	        loopFactor++;
	        determineSaveResumeDataInterval();
	
	        //update tracker scrape every 10min - temporary, until scrape code gets reworked
	        if (loopFactor >= 10*60) {
	          loopFactor = 0;
	          trackerScraper.update();
	        }
	        
	
	        synchronized (managers) {
	        //  int nbStarted = 0;
	        //  int nbDownloading = 0;
	          if (loopFactor % saveResumeLoopCount == 0) {
	            saveDownloads();
	          }
	          
	
	          for (int i = 0; i < managers.size(); i++) {
	            DownloadManager manager = (DownloadManager) managers.get(i);
	            
	            //make sure we update 'downloads.config' on state changes
	            if (manager.getPrevState() != manager.getState()) {
	              saveDownloads();
	              manager.setPrevState(manager.getState());
	            }
	            
	            if (manager.getState() == DownloadManager.STATE_DOWNLOADING) {
	             	if (loopFactor % saveResumeLoopCount == 0) {
	            		manager.getDiskManager().dumpResumeDataToDisk(false, false);
	            	}
	            }
	            /*
	             * seeding rules have been moved to StartStopRulesDefaultPlugin
	             */
	            
            
	            // Handle forced starts here
	            if (manager.getState() == DownloadManager.STATE_READY &&
	                manager.isForceStart()) {
	              manager.startDownload();
	              
	              if (manager.getState() == DownloadManager.STATE_DOWNLOADING) {
	                //set previous hash fails and discarded values
	                manager.getStats().setSavedDiscarded();
	                manager.getStats().setSavedHashFails();
	              }
	            }

	          }
	        }
      	}catch( Throwable e ){
      		
      		e.printStackTrace();
      	}
      	
        try {
          Thread.sleep(waitTime);
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    public void stopIt() {
      finished = true;
    }
  }

  public GlobalManagerImpl(boolean initialiseStarted)
  {
    //Debug.dumpThreadsLoop("Active threads");
  	
  	LGLogger.initialise();
  	
    stats = new GlobalManagerStatsImpl();
        
    trackerScraper = TRTrackerScraperFactory.create();
    
    trackerScraper.addListener(
    	new TRTrackerScraperListener()
    	{
    		public void
    		scrapeReceived(
    			TRTrackerScraperResponse		response )
    		{
    			byte[]	hash = response.getHash();
    			
    			if ( hash != null ){
    				
    				DownloadManager manager = (DownloadManager)manager_map.get(new HashWrapper(hash));
    				
    				if ( manager != null ){
    				
    					manager.setTrackerScrapeResponse( response );
    				}
    			}
    		}
    	});
    
    loadDownloads();
    	
    tracker_host = TRHostFactory.create();
    
    tracker_host.initialise( 
    	new TRHostTorrentFinder()
    	{
    		public TOTorrent
    		lookupTorrent(
    			byte[]		hash )
    		{
    			for (int i=0;i<managers.size();i++){
    				
    				DownloadManager	dm = (DownloadManager)managers.get(i);
    				
    				TOTorrent t = dm.getTorrent();
    				
    				if ( t != null ){
    					
    					try{
    						if ( Arrays.equals( hash, t.getHash())){
    							
    							return( t );
    						}
    					}catch( TOTorrentException e ){
    						
    						e.printStackTrace();
    					}
    				}
    			}
    			
    			return( null );
    		}
    	});
    	
    checker = new Checker();    
    if(initialiseStarted) checker.start();
    
    stats_writer = StatsWriterFactory.createPeriodicDumper( this );
    
    stats_writer.start();
  }

  public DownloadManager addDownloadManager(String fileName, String savePath) {
  	return addDownloadManager(fileName, savePath, DownloadManager.STATE_WAITING, true);
  }
   
  public DownloadManager 
  addDownloadManager(String fileName, String savePath, int initialState ) {
  	return( addDownloadManager(fileName, savePath, initialState, true ));
  }
  	
  /**
   * @return true, if the download was added
   *
   * @author Rene Leonhardt
   */
  public DownloadManager 
  addDownloadManager(String fileName, String savePath, int initialState, boolean persistent ) {
	File torrentDir	= null;
	File fDest		= null;
	
    try {
      File f = new File(fileName);
      
      boolean saveTorrents = persistent&&COConfigurationManager.getBooleanParameter("Save Torrent Files", true);
      if (saveTorrents) torrentDir = new File(COConfigurationManager.getDirectoryParameter("General_sDefaultTorrent_Directory"));
      else torrentDir = new File(f.getParent());

      //if the torrent is already in the completed files dir, use this
      //torrent instead of creating a new one in the default dir
      boolean moveWhenDone = COConfigurationManager.getBooleanParameter("Move Completed When Done", false);
      String completedDir = COConfigurationManager.getStringParameter("Completed Files Directory", "");
      if (moveWhenDone && completedDir.length() > 0) {
        if (f.getParent().startsWith(completedDir)) {
          //set the torrentDir to the completedDir
          torrentDir = new File(completedDir);
        }
      }
        
      torrentDir.mkdirs();
      
      fDest = new File(torrentDir, f.getName().replaceAll("%20","."));
      if (fDest.equals(f)) {
        throw new Exception("Same files");
      }
      
      while (fDest.exists()) {
        fDest = new File(torrentDir, "_" + fDest.getName());
      }
      
      fDest.createNewFile();
      FileUtil.copyFile(f, fDest);
      String fName = fDest.getCanonicalPath();
      DownloadManager manager = DownloadManagerFactory.create(this, fName, savePath, initialState, persistent );
      manager = addDownloadManager(manager, true);
      if ( manager == null ) {
        fDest.delete();
        File backupFile = new File(fName + ".bak");
        if(backupFile.exists())
          backupFile.delete();
      }
      return( manager );
    }
    catch (IOException e) {
      System.out.println( "DownloadManager::addDownloadManager: fails - td = " + torrentDir + ", fd = " + fDest );
      e.printStackTrace();
      DownloadManager manager = DownloadManagerFactory.create(this, fileName, savePath, initialState, persistent);
      return addDownloadManager(manager, true);
    }
    catch (Exception e) {
    	// get here on duplicate files, no need to treat as error
      DownloadManager manager = DownloadManagerFactory.create(this, fileName, savePath, initialState, persistent);
      return addDownloadManager(manager, true);
    }
  }



   protected DownloadManager addDownloadManager(DownloadManager manager, boolean save) {
    if (!isStopped) {
      synchronized (managers) {
      	
      	int	existing_index = managers.indexOf( manager );
      	
        if (existing_index != -1) {
        	
        	DownloadManager existing = (DownloadManager)managers.get(existing_index);
          
        	TOTorrent existing_torrent 	= existing.getTorrent();
        	TOTorrent new_torrent		= manager.getTorrent();
        	
        	if ( TorrentUtils.mergeAnnounceURLs( new_torrent, existing_torrent )){
        		
        		try{
        			
        			TorrentUtils.writeToFile( existing_torrent );
        			
        			TRTrackerClient	client = existing.getTrackerClient();
        			
        			if ( client != null ){
        				
        				client.resetTrackerUrl( false );
        			}
        		}catch( Throwable e ){
        			
        			e.printStackTrace();
        		}
        	}
        	
        	return( existing );
        }
        
        boolean isCompleted = manager.getStats().getDownloadCompleted(false) == 1000;
	      if (manager.getPosition() == -1) {
	        int endPosition = 0;
	        for (int i = 0; i < managers.size(); i++) {
	          DownloadManager dm = (DownloadManager) managers.get(i);
	          boolean dmIsCompleted = dm.getStats().getDownloadCompleted(false) == 1000;
	          if (dmIsCompleted == isCompleted)
	            endPosition++;
	        }
	        manager.setPosition(endPosition + 1);
	      }
	      
	      // Even though when the DownloadManager was created, onlySeeding was
	      // most likely set to true for completed torrents (via the Initializer +
	      // readTorrent), there's a chance that the torrent file didn't have the
	      // resume data.  If it didn't, but we marked it as complete in our
	      // downloads config file, we should set to onlySeeding
	      manager.setOnlySeeding(isCompleted);

        managers.add(manager);
        
        TOTorrent	torrent = manager.getTorrent();
        
        if ( torrent != null ){
        	
        	try{
        		manager_map.put( new HashWrapper(torrent.getHash()), manager );
        		
        	}catch( TOTorrentException e ){
        		
        		e.printStackTrace();
        	}
        }

        listeners.dispatch( LDT_MANAGER_ADDED, manager );
      }
 
      if (save)
        saveDownloads();
      
      return( manager );
    }
    else {
      LGLogger.log(
        0,
        LGLogger.ERROR,
        LGLogger.ERROR,
        "Tried to add a DownloadManager after shutdown of GlobalManager.");
      return( null );
    }
  }

  public List getDownloadManagers() {
    return managers;
  }

  public void 
  canDownloadManagerBeRemoved(
  	DownloadManager manager) 
  
  	throws GlobalManagerDownloadRemovalVetoException
  {
  	try{
  		removal_listeners.dispatchWithException( LDT_MANAGER_WBR, manager );
  		
  	}catch( Throwable e ){
  		
  		throw((GlobalManagerDownloadRemovalVetoException)e);
  	}
  }
  
  public void 
  removeDownloadManager(
  	DownloadManager manager) 
  
  	throws GlobalManagerDownloadRemovalVetoException
  {
  	canDownloadManagerBeRemoved( manager );
  	
    synchronized (managers){
    	
      managers.remove(manager);
      
      TOTorrent	torrent = manager.getTorrent();
      
      if ( torrent != null ){
      	
      	try{
      		manager_map.remove(new HashWrapper(torrent.getHash()));
      		
      	}catch( TOTorrentException e ){
      		
      		e.printStackTrace();
      	}
      }
    }
    
    fixUpDownloadManagerPositions();
    listeners.dispatch( LDT_MANAGER_REMOVED, manager );
    
    saveDownloads();

    if (manager.getTrackerClient() != null) {

      trackerScraper.remove(manager.getTrackerClient());

    }
    else if (manager.getTorrent() != null) {

      trackerScraper.remove(manager.getTorrent());
    }
  }

  public void stopAll() {
    if (!isStopped){
    	
    	informDestroyInitiated();
    	
    		// kick off a non-daemon task. This will ensure that we hang around
    		// for at least LINGER_PERIOD to run other non-daemon tasks such as writing
    		// torrent resume data...
    	
    	try{
	    	NonDaemonTaskRunner.run(
	    			new NonDaemonTask()
	    			{
	    				public Object
	    				run()
	    				{	
	    					tracker_host.close();
	    					
	    					return( null );
	    				}
	    			});
    	}catch( Throwable e ){
    		e.printStackTrace();
    	}
    	
      checker.stopIt();
      
      saveDownloads();
      
      stopAllDownloads();
      
      managers.clear();
      manager_map.clear();
      
      isStopped = true;
      
      stats_writer.stop();
      
      informDestroyed();
    }
  }

  /**
   * Stops all downloads without removing them
   *
   * @author Rene Leonhardt
   */
  public void stopAllDownloads() {
 
    for (Iterator iter = managers.iterator(); iter.hasNext();) {
      DownloadManager manager = (DownloadManager) iter.next();
      manager.stopIt();
    }
  }
  
  
  /**
   * Starts all downloads
   */
  public void startAllDownloads() {    
    for (Iterator iter = managers.iterator(); iter.hasNext();) {
        DownloadManager manager = (DownloadManager) iter.next();
        manager.startDownloadInitialized(true);
    }
  }
  

  private void loadDownloads() {
    FileInputStream fin = null;
    BufferedInputStream bin = null;
    try {
      //open the file
      File configFile = FileUtil.getApplicationFile("downloads.config");
      fin = new FileInputStream(configFile);
      bin = new BufferedInputStream(fin);
      Map map = BDecoder.decode(bin);
      boolean debug = Boolean.getBoolean("debug");
      int numDownloading = 0;

      Iterator iter = null;
      //v2.0.3.0+ vs older mode
      List downloads = (List) map.get("downloads");
      if (downloads == null) {
        //No downloads entry, then use the old way
        iter = map.values().iterator();
      }
      else {
        //New way, downloads stored in a list
        iter = downloads.iterator();
      }
      while (iter.hasNext()) {
        Map mDownload = (Map) iter.next();
        try {
          String fileName = new String((byte[]) mDownload.get("torrent"), Constants.DEFAULT_ENCODING);
          String savePath = new String((byte[]) mDownload.get("path"), Constants.DEFAULT_ENCODING);
          int nbUploads = ((Long) mDownload.get("uploads")).intValue();
          int state = DownloadManager.STATE_WAITING;
          if (debug)
            state = DownloadManager.STATE_STOPPED;
          else {
            if (mDownload.containsKey("state")) {
              state = ((Long) mDownload.get("state")).intValue();
              if (state != DownloadManager.STATE_STOPPED &&
                  state != DownloadManager.STATE_QUEUED &&
                  state != DownloadManager.STATE_WAITING)
                state = DownloadManager.STATE_WAITING;
            }
            else {
              int stopped = ((Long) mDownload.get("stopped")).intValue();
              if (stopped == 1)
                state = DownloadManager.STATE_STOPPED;
            } 
          }
          Long lPriority = (Long) mDownload.get("priority");
          Long lDownloaded = (Long) mDownload.get("downloaded");
          Long lUploaded = (Long) mDownload.get("uploaded");
          Long lCompleted = (Long) mDownload.get("completed");
          Long lDiscarded = (Long) mDownload.get("discarded");
          Long lHashFails = (Long) mDownload.get("hashfails");
          Long lForceStart = (Long) mDownload.get("forceStart");
          if (lForceStart == null) {
	          Long lStartStopLocked = (Long) mDownload.get("startStopLocked");
	          if(lStartStopLocked != null) {
	          	lForceStart = lStartStopLocked;
	          }
	        }
          Long lPosition = (Long) mDownload.get("position");
          String sCategory = null;
          if (mDownload.containsKey("category"))
            sCategory = new String((byte[]) mDownload.get("category"), Constants.DEFAULT_ENCODING);
          DownloadManager dm = DownloadManagerFactory.create(this, fileName, savePath, state, true );
          dm.getStats().setMaxUploads(nbUploads);
          if (lPriority != null) {
            dm.setPriority(lPriority.intValue());
          }
          if (lDownloaded != null && lUploaded != null) {
            dm.getStats().setSavedDownloadedUploaded(lDownloaded.longValue(), lUploaded.longValue());
          }
          if (lCompleted != null) {
            dm.getStats().setDownloadCompleted(lCompleted.intValue());
            if (lCompleted.intValue() < 1000)
              numDownloading++;
          }
          else {
            numDownloading++;
          }
          
          if (lDiscarded != null) {
            dm.getStats().saveDiscarded(lDiscarded.intValue());
          }
          if (lHashFails != null) {
            dm.getStats().saveHashFails(lHashFails.intValue());
          }
          
          if (lPosition != null)
            dm.setPosition(lPosition.intValue());
          else if (dm.getStats().getDownloadCompleted(false) < 1000)
            dm.setPosition(numDownloading);
            
          if (sCategory != null) {
            Category cat = CategoryManager.getCategory(sCategory);
            if (cat != null) dm.setCategory(cat);
          }

          this.addDownloadManager(dm, false);

          if(lForceStart != null) {
            if(lForceStart.intValue() == 1) {
              dm.setForceStart(true);
            }
          }
        }
        catch (UnsupportedEncodingException e1) {
          //Do nothing and process next.
        }
      }
      // Someone could have mucked with the config file and set weird positions,
      // so fix them up.
      fixUpDownloadManagerPositions();
    }
    catch (FileNotFoundException e) {
      //Do nothing
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      try {
        if (bin != null)
          bin.close();
      }
      catch (Exception e) {}
      try {
        if (fin != null)
          fin.close();
      }
      catch (Exception e) {}
    }
  }

  private void saveDownloads() {
    //    if(Boolean.getBoolean("debug")) return;

  	synchronized( managers ){
	    Map map = new HashMap();
	    List list = new ArrayList(managers.size());
	    for (int i = 0; i < managers.size(); i++) {
	      DownloadManager dm = (DownloadManager) managers.get(i);
	      
	      if ( dm.isPersistent()){
	      	
		      Map dmMap = new HashMap();
		      dmMap.put("torrent", dm.getTorrentFileName());
		      dmMap.put("path", dm.getFullName());
		      dmMap.put("uploads", new Long(dm.getStats().getMaxUploads()));
          int state = dm.getState();
          // XXX: Add "&& SeedingQREnabled"
          if (state == DownloadManager.STATE_SEEDING)
            state = DownloadManager.STATE_QUEUED;
          else if (state == DownloadManager.STATE_ERROR)
            state = DownloadManager.STATE_STOPPED;
          else if (state != DownloadManager.STATE_STOPPED &&
                  state != DownloadManager.STATE_QUEUED &&
                  state != DownloadManager.STATE_WAITING)
            state = DownloadManager.STATE_WAITING;
          dmMap.put("state", new Long(state));
          int priority = dm.getPriority();
		      dmMap.put("priority", new Long(priority));
		      dmMap.put("position", new Long(dm.getPosition()));
		      dmMap.put("downloaded", new Long(dm.getStats().getDownloaded()));
		      dmMap.put("uploaded", new Long(dm.getStats().getUploaded()));
		      dmMap.put("completed", new Long(dm.getStats().getDownloadCompleted(true)));
		      dmMap.put("discarded", new Long(dm.getStats().getDiscarded()));
		      dmMap.put("hashfails", new Long(dm.getStats().getHashFails()));
		      dmMap.put("forceStart", new Long(dm.isForceStart() ? 1 : 0));
		      // Following 3 aren't needed, but save them so older versions still work
		      // XXX: Maybe remove them after 2.0.7.x release
		      dmMap.put("priorityLocked", new Long(0));
		      dmMap.put("startStopLocked", new Long(0));
		      dmMap.put("stopped", new Long(1));
		      Category category = dm.getCategory();
		      if (category != null && category.getType() == Category.TYPE_USER)
		        dmMap.put("category", category.getName());
		      list.add(dmMap);
	      }
	    }
	    map.put("downloads", list);
        
	    FileOutputStream fos = null;
       
	    try {
	    	//encode the data
	    	byte[] torrentData = BEncoder.encode(map);
            
         File oldFile = FileUtil.getApplicationFile("downloads.config");
         File newFile = FileUtil.getApplicationFile("downloads.config.new");
         
         //write the data out
	    	fos = new FileOutputStream(newFile);
         fos.getChannel().force(true);
	      fos.write(torrentData);
         fos.flush();
         
          //close the output stream
         fos.close();
         fos = null;
         
         //delete the old file
         if ( !oldFile.exists() || oldFile.delete() ) {
            //rename the new one
            newFile.renameTo(oldFile);
         }
                  
	    }
	    catch (Exception e) {
	      e.printStackTrace();
	    }
	    finally {
	      try {
	        if (fos != null)
	          fos.close();
	      }
	      catch (Exception e) {}
	    }
  	}
  }

  /**
   * @return
   */
  public TRTrackerScraper getTrackerScraper() {
    return trackerScraper;
  }

	public GlobalManagerStats
	getStats()
	{
		return( stats );
	}
	
  public int getIndexOf(DownloadManager manager) {
    if (managers != null && manager != null)
      return managers.indexOf(manager);
    return -1;
  }

  public boolean isMoveableUp(DownloadManager manager) {
    if (managers == null)
      return false;

    if ((manager.getStats().getDownloadCompleted(false) == 1000) &&
        (COConfigurationManager.getIntParameter("StartStopManager_iRankType") != 0))
      return false;

    return manager.getPosition() > 1;
  }

  public boolean isMoveableDown(DownloadManager manager) {
    if (managers == null)
      return false;

    boolean isCompleted = manager.getStats().getDownloadCompleted(false) == 1000;

    if (isCompleted &&
        (COConfigurationManager.getIntParameter("StartStopManager_iRankType") != 0))
      return false;

    int numInGroup = 0;
    for (int i = 0; i < managers.size(); i++) {
      DownloadManager dm = (DownloadManager) managers.get(i);
      if ((dm.getStats().getDownloadCompleted(false) == 1000) == isCompleted)
        numInGroup++;
    }
    return manager.getPosition() < numInGroup;
  }

  public void moveUp(DownloadManager manager) {
  	moveTo(manager, manager.getPosition() - 1);
  }

  public void moveDown(DownloadManager manager) {
  	moveTo(manager, manager.getPosition() + 1);
  }

  public void moveTop(DownloadManager[] manager) {
    if (managers != null)
      synchronized (managers) {
      	int newPosition = 1;
        for (int i = 0; i < manager.length; i++)
        	moveTo(manager[i], newPosition++);
      }
  }

  public void moveEnd(DownloadManager[] manager) {
    if (managers != null)
      synchronized (managers) {
        int endPosComplete = 0;
        int endPosIncomplete = 0;
        for (int j = 0; j < managers.size(); j++) {
			DownloadManager dm = (DownloadManager) managers.get(j);
			if (dm.getStats().getDownloadCompleted(false) == 1000)
				endPosComplete++;
			else
				endPosIncomplete++;
        }
		  for (int i = manager.length - 1; i >= 0; i--)
				if (manager[i].getStats().getDownloadCompleted(false) == 1000 && endPosComplete > 0)
	        		moveTo(manager[i], endPosComplete--);
	        	else if (endPosIncomplete > 0)
	        		moveTo(manager[i], endPosIncomplete--);
		}
  }
  
  public void moveTo(DownloadManager manager, int newPosition) {
    if (newPosition < 1)
      return;

    if (managers != null)
      synchronized (managers) {
        int curPosition = manager.getPosition();
        if (newPosition > curPosition) {
          // move [manager] down
          // move everything between [curPosition+1] and [newPosition] up(-) 1
          boolean curCompleted = (manager.getStats().getDownloadCompleted(false) == 1000);
          int numToMove = newPosition - curPosition;
          for (int i = 0; i < managers.size(); i++) {
            DownloadManager dm = (DownloadManager) managers.get(i);
            boolean dmCompleted = (dm.getStats().getDownloadCompleted(false) == 1000);
            if (dmCompleted == curCompleted) {
              int dmPosition = dm.getPosition();
              if ((dmPosition > curPosition) && (dmPosition <= newPosition)) {
                dm.setPosition(dmPosition - 1);
                numToMove--;
                if (numToMove <= 0)
                  break;
              }
            }
          }
          
          manager.setPosition(newPosition);
        }
        else if (newPosition < curPosition && curPosition > 1) {
          // move [manager] up
          // move everything between [newPosition] and [curPosition-1] down(+) 1
          boolean curCompleted = (manager.getStats().getDownloadCompleted(false) == 1000);
          int numToMove = curPosition - newPosition;
  
          for (int i = 0; i < managers.size(); i++) {
            DownloadManager dm = (DownloadManager) managers.get(i);
            boolean dmCompleted = (dm.getStats().getDownloadCompleted(false) == 1000);
            int dmPosition = dm.getPosition();
            if ((dmCompleted == curCompleted) &&
                (dmPosition >= newPosition) &&
                (dmPosition < curPosition)
               ) {
              dm.setPosition(dmPosition + 1);
              numToMove--;
              if (numToMove <= 0)
                break;
            }
          }
          manager.setPosition(newPosition);
        }
      }
  }
	
	public void fixUpDownloadManagerPositions() {
    if (managers != null) {
      synchronized (managers) {
      	int posComplete = 1;
      	int posIncomplete = 1;
		    Collections.sort(managers, new Comparator () {
	          public final int compare (Object a, Object b) {
	            return ((DownloadManager)a).getPosition() - ((DownloadManager)b).getPosition();
	          }
	        } );
        for (int i = 0; i < managers.size(); i++) {
          DownloadManager dm = (DownloadManager) managers.get(i);
          if (dm.getStats().getDownloadCompleted(false) == 1000)
          	dm.setPosition(posComplete++);
         	else
          	dm.setPosition(posIncomplete++);
        }
      }
    }
	}
  
  	protected void
  	informDestroyed()
  	{
  		if ( destroyed )
  		{
  			return;
  		}
  		
  		destroyed = true;
  		
  		/*
		Thread t = new Thread("Azureus: destroy checker")
			{
				public void
				run()
				{
					long	start = System.currentTimeMillis();
							
					while(true){
								
						try{
							Thread.sleep(2500);
						}catch( Throwable e ){
							e.printStackTrace();
						}
								
						if ( System.currentTimeMillis() - start > 10000 ){
									
								// java web start problem here...
								
							// Debug.dumpThreads("Azureus: slow stop - thread dump");
							
							// Debug.killAWTThreads(); doesn't work
						}
					}
				}						
			};
					
		t.setDaemon(true);
				
		t.start();
		*/

  		listeners.dispatch( LDT_DESTROYED, null );
  	}
  	
  	public void
  	informDestroyInitiated()
  	{
  		listeners.dispatch( LDT_DESTROY_INITIATED, null );		
  	}
  	
 	public void
	addListener(
		GlobalManagerListener	listener )
	{
		if ( isStopped ){
				
			listener.destroyed();
				
		}else{			
							
			listeners.addListener(listener);
			
			synchronized( managers ){
					
				for (int i=0;i<managers.size();i++){
				
					listeners.dispatch( listener, LDT_MANAGER_ADDED, managers.get(i) );
				}	
			}
		}
	}
		
	public void
 	removeListener(
		GlobalManagerListener	listener )
	{			
		listeners.removeListener(listener);
	}
	
	public void
	addDownloadWillBeRemovedListener(
		GlobalManagerDownloadWillBeRemovedListener	l )
	{
		removal_listeners.addListener( l );
	}
	
	public void
	removeDownloadWillBeRemovedListener(
		GlobalManagerDownloadWillBeRemovedListener	l )
	{
		removal_listeners.removeListener( l );
	}
	

  /**
   * @param c the character to be found 
   * @param lastSelectedIndex the highest selection index; -1 to start from the beginning
   * @return index of next item with a name beginning with c, -1 else
   *
   * @author Rene Leonhardt
   */
  public int getNextIndexForCharacter(char c, int lastSelectedIndex) {
    if(c >= '0' && c <= 'z') {
      c = Character.toLowerCase(c);
      if (managers != null) {
        synchronized (managers) {
          if(lastSelectedIndex < 0 || lastSelectedIndex >= managers.size())
            lastSelectedIndex = -1;
          lastSelectedIndex++;
          for (int i = lastSelectedIndex; i < managers.size(); i++) {
            char test = Character.toLowerCase(((DownloadManager) managers.get(i)).getName().charAt(0));
            if(test == c)
              return i;
          }
          for (int i = 0; i < lastSelectedIndex; i++) {
            char test = Character.toLowerCase(((DownloadManager) managers.get(i)).getName().charAt(0));
            if(test == c)
              return i;
          }
        }
      }
    }
    return -1;
  }
  
  public void 
  startChecker() 
  {
  	synchronized( checker ){
	  	if ( !checker.isAlive()){
	  		
    checker.start(); 
  }
}
  }
}
