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

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.Collections;

import com.aelitis.azureus.core.AzureusCoreListener;

import org.gudy.azureus2.core3.global.*;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.category.CategoryManager;
import org.gudy.azureus2.core3.category.Category;

import com.aelitis.azureus.core.helpers.TorrentFolderWatcher;


/**
 * @author Olivier
 * 
 */
public class GlobalManagerImpl 
	implements 	GlobalManager, DownloadManagerListener
{
		// GlobalManagerListener support
		// Must be an async listener to support the non-synchronised invocation of
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
	
	private List 		managers		= new ArrayList();
	private AEMonitor	managers_mon	= new AEMonitor( "GM:Managers" );
	
	private Map		manager_map			= new HashMap();
		
	private Checker checker;
	private GlobalManagerStatsImpl		stats;
	private TRTrackerScraper 			trackerScraper;
	private GlobalManagerStatsWriter 	stats_writer;
	private GlobalManagerHostSupport	host_support;
  
	private Map							saved_download_manager_state	= new HashMap();
	
  private TorrentFolderWatcher torrent_folder_watcher;
  
  private ArrayList paused_list = new ArrayList();
  private final AEMonitor paused_list_mon = new AEMonitor( "GlobalManager:PL" );
  
  
  
  /* Whether the GlobalManager is active (false) or stopped (true) */
  private boolean 					isStopped = false;
  private boolean					destroyed;
  private boolean needsSaving = false;
  
  public class Checker extends AEThread {
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
      if (saveResumeInterval >= 1 && saveResumeInterval <= 90)
        saveResumeLoopCount = saveResumeInterval * 60000 / waitTime;
    }

    public void runSupport() {
      while (!finished) {

      	try{
	        loopFactor++;
	        determineSaveResumeDataInterval();

	        try{
	        	managers_mon.enter();
	        
	          if ((loopFactor % saveResumeLoopCount == 0) || needsSaving) {
	            saveDownloads();
	            needsSaving = false;
	          }

	          for (int i = 0; i < managers.size(); i++) {
	            DownloadManager manager = (DownloadManager) managers.get(i);
	            
             	if (loopFactor % saveResumeLoopCount == 0) {
            		manager.saveResumeData();
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
	        }finally{
	        	
	        	managers_mon.exit();
	        }
      	}catch( Throwable e ){
      		
      		Debug.printStackTrace( e );
      	}
      	
        try {
          Thread.sleep(waitTime);
        }
        catch (Exception e) {
        	Debug.printStackTrace( e );
        }
      }
    }

    public void stopIt() {
      finished = true;
    }
  }

  public 
  GlobalManagerImpl(
  		AzureusCoreListener listener)
  {
    //Debug.dumpThreadsLoop("Active threads");
  	
  	LGLogger.initialise();
  	
    stats = new GlobalManagerStatsImpl();
       
    try{
    	stats_writer = new GlobalManagerStatsWriter( this );
    	
    }catch( Throwable e ){
    	
    	LGLogger.log( "Stats unavailable", e );
    }
           
    if (listener != null)
      listener.reportCurrentTask(MessageText.getString("splash.initializeGM") + ": " +
                            MessageText.getString("splash.loadingTorrents"));
    loadDownloads(listener);
    if (listener != null)
      listener.reportCurrentTask(MessageText.getString("splash.initializeGM"));

    // Initialize scraper after loadDownloads so that we can merge scrapes
    // into one request per tracker
    trackerScraper = TRTrackerScraperFactory.getSingleton();
    
    trackerScraper.setClientResolver(
    	new TRTrackerScraperClientResolver()
		{
    		public TRTrackerClient
			getClient(
				byte[]	torrent_hash )
    		{
    			DownloadManager	dm = getDownloadManager(torrent_hash);
    			
    			if ( dm != null ){
    				
    				return( dm.getTrackerClient());
    			}
    			
    			return( null );
    		}
		});
    
    trackerScraper.addListener(
    	new TRTrackerScraperListener() {
    		public void scrapeReceived(TRTrackerScraperResponse response) {
    			byte[]	hash = response.getHash();
    			
    			if ( response.isValid() ){
    				DownloadManager manager = (DownloadManager)manager_map.get(new HashWrapper(hash));
    				if ( manager != null ) {
    					manager.setTrackerScrapeResponse( response );
    				}
    			}
    		}
    	});
    
    try{  
	    host_support = new GlobalManagerHostSupport( this, managers ); 

    }catch( Throwable e ){
    	
    	LGLogger.log( "Hosting unavailable", e );
    }
    
    checker = new Checker();   
       	
    checker.start();
    
    if ( stats_writer != null ){
    	
    	stats_writer.initialisationComplete();
    }
    
    torrent_folder_watcher = new TorrentFolderWatcher( this );
  }

  public DownloadManager 
  addDownloadManager(
  		String fileName, 
		String savePath) 
  {
  	return addDownloadManager(fileName, savePath, DownloadManager.STATE_WAITING, true);
  }
   
  	public DownloadManager 
	addDownloadManager(
  		String 	fileName, 
		String 	savePath, 
		int 	initialState ) 
  	{
  		return( addDownloadManager(fileName, savePath, initialState, true ));
  	}
  	
	public DownloadManager
	addDownloadManager(
	    String 		fileName,
	    String 		savePath,
	    int         initialState,
		boolean		persistent )
	{
	 	return( addDownloadManager(fileName, savePath, initialState, persistent, false ));
	}
	
  /**
   * @return true, if the download was added
   *
   * @author Rene Leonhardt
   */
	
  public DownloadManager 
  addDownloadManager(
  		String torrent_file_name, 
		String savePath, 
		int initialState, 
		boolean persistent, 
		boolean for_seeding ) 
  {
  		/* to recover the initial state for non-persistent downloads the simplest way is to do it here
  		 */
  	
  	if (!persistent){
  	
        Map	save_download_state	= (Map)saved_download_manager_state.get(torrent_file_name);
        
        if ( save_download_state != null && save_download_state.containsKey( "state" )){
        	
            int	saved_state = ((Long) save_download_state.get("state")).intValue();
            
            if ( saved_state == DownloadManager.STATE_STOPPED ){
            	
            	initialState	= saved_state;
            }

        }
  	}
  	
	File torrentDir	= null;
	File fDest		= null;
	
    try {
      File f = new File(torrent_file_name);
      
      boolean saveTorrents = persistent&&COConfigurationManager.getBooleanParameter("Save Torrent Files", true);
      if (saveTorrents) torrentDir = new File(COConfigurationManager.getDirectoryParameter("General_sDefaultTorrent_Directory"));
      else torrentDir = new File(f.getParent());

      //if the torrent is already in the completed files dir, use this
      //torrent instead of creating a new one in the default dir
      boolean moveWhenDone = COConfigurationManager.getBooleanParameter("Move Completed When Done", false);
      String completedDir = COConfigurationManager.getStringParameter("Completed Files Directory", "");
      if (moveWhenDone && completedDir.length() > 0) {
        File cFile = new File( completedDir, f.getName() );
        if ( cFile.exists() ) {
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
      
      if ( ! FileUtil.copyFile(f, fDest)) {
        throw new IOException("File copy failed");
      }
      
      String fName = fDest.getCanonicalPath();
    
      	// now do the creation!
      
      DownloadManager new_manager = DownloadManagerFactory.create(this, fName, savePath, initialState, persistent, false, for_seeding );
      
      DownloadManager manager = addDownloadManager(new_manager, true);
      
      	// if a different manager is returned then an existing manager for this torrent
      	// exists and the new one isn't needed (yuck)
      
      if ( manager == null || manager != new_manager ) {
        fDest.delete();
        File backupFile = new File(fName + ".bak");
        if(backupFile.exists())
          backupFile.delete();
      }
      return( manager );
    }
    catch (IOException e) {
      System.out.println( "DownloadManager::addDownloadManager: fails - td = " + torrentDir + ", fd = " + fDest );
      Debug.printStackTrace( e );
      DownloadManager manager = DownloadManagerFactory.create(this, torrent_file_name, savePath, initialState, persistent, false, for_seeding );
      return addDownloadManager(manager, true);
    }
    catch (Exception e) {
    	// get here on duplicate files, no need to treat as error
      DownloadManager manager = DownloadManagerFactory.create(this, torrent_file_name, savePath, initialState, persistent, false, for_seeding );
      return addDownloadManager(manager, true);
    }
  }



   protected DownloadManager 
   addDownloadManager(
   		DownloadManager 	download_manager, 
		boolean 			save) 
   {
    if (!isStopped) {
      try{
      	managers_mon.enter();
      	
      	int	existing_index = managers.indexOf( download_manager );
      	
        if (existing_index != -1) {
        	
        	DownloadManager existing = (DownloadManager)managers.get(existing_index);
                	
        	existing.mergeTorrentDetails( download_manager );
        	
        	return( existing );
        }
        
        int minQueueingShareRatio = COConfigurationManager.getIntParameter("StartStopManager_iFirstPriority_ShareRatio");
        
        DownloadManagerStats dm_stats = download_manager.getStats();

        String	torrent_file_name = download_manager.getTorrentFileName();
        
        Map	save_download_state	= (Map)saved_download_manager_state.get(torrent_file_name);
        
        if ( save_download_state != null ){
        	
        		// once the state's been used we remove it
        	
        	saved_download_manager_state.remove( torrent_file_name );
        	
	        int nbUploads = ((Long) save_download_state.get("uploads")).intValue();
	        int maxDL = save_download_state.get("maxdl")==null?0:((Long) save_download_state.get("maxdl")).intValue();
	        int maxUL = save_download_state.get("maxul")==null?0:((Long) save_download_state.get("maxul")).intValue();
	        
	        Long lDownloaded = (Long) save_download_state.get("downloaded");
	        Long lUploaded = (Long) save_download_state.get("uploaded");
	        Long lCompleted = (Long) save_download_state.get("completed");
	        Long lDiscarded = (Long) save_download_state.get("discarded");
	        Long lHashFails = (Long) save_download_state.get("hashfails");
	
	
	        dm_stats.setMaxUploads(nbUploads);
	        dm_stats.setMaxDownloadKBSpeed( maxDL );
	        dm_stats.setUploadRateLimitBytesPerSecond(maxUL);
	        
	        if (lCompleted != null) {
	          dm_stats.setDownloadCompleted(lCompleted.intValue());
	        }
	        
	        if (lDiscarded != null) {
	          dm_stats.saveDiscarded(lDiscarded.longValue());
	        }
	        if (lHashFails != null) {
	          dm_stats.saveHashFails(lHashFails.longValue());
	        }
	        
	        Long lPosition = (Long) save_download_state.get("position");
	        String sCategory = null;
	        if (save_download_state.containsKey("category")){
	        	try{
	        		sCategory = new String((byte[]) save_download_state.get("category"), Constants.DEFAULT_ENCODING);
	        	}catch( UnsupportedEncodingException e ){
	        		
	        		Debug.printStackTrace(e);
	        	}
	        }
	
	        if (sCategory != null) {
	          Category cat = CategoryManager.getCategory(sCategory);
	          if (cat != null) download_manager.setCategory(cat);
	        }
	
	        
	        boolean bCompleted = dm_stats.getDownloadCompleted(false) == 1000;
	      
	        download_manager.setOnlySeeding(bCompleted);
	        
	        if (lDownloaded != null && lUploaded != null) {
	          long lUploadedValue = lUploaded.longValue();
	          long lDownloadedValue = lDownloaded.longValue();
	          if (bCompleted && (lDownloadedValue == 0)) {
	            lDownloadedValue = download_manager.getSize();
	            	
	            if (lDownloadedValue != 0 && ((lUploadedValue * 1000) / lDownloadedValue < minQueueingShareRatio) )
	              lUploadedValue = ( download_manager.getSize()+999) * minQueueingShareRatio / 1000;
	          }
	          dm_stats.setSavedDownloadedUploaded(lDownloadedValue, lUploadedValue);
	        }
	
	        if (lPosition != null)
	        	download_manager.setPosition(lPosition.intValue());
	        // no longer needed code
	        //  else if (dm_stats.getDownloadCompleted(false) < 1000)
	        //  dm.setPosition(bCompleted ? numCompleted : numDownloading);
	
	        Long lSecondsDLing = (Long)save_download_state.get("secondsDownloading");
	        if (lSecondsDLing != null) {
	          dm_stats.setSecondsDownloading(lSecondsDLing.longValue());
	        }
	
	        Long lSecondsOnlySeeding = (Long)save_download_state.get("secondsOnlySeeding");
	        if (lSecondsOnlySeeding != null) {
	          dm_stats.setSecondsOnlySeeding(lSecondsOnlySeeding.longValue());
	        }
	        
	        Long already_allocated = (Long)save_download_state.get( "allocated" );
	        if( already_allocated != null && already_allocated.intValue() == 1 ) {
	        	download_manager.setDataAlreadyAllocated( true );
	        }
	        
	        Long creation_time = (Long)save_download_state.get( "creationTime" );
	        
	        if ( creation_time != null ){
	        	
	        	long	ct = creation_time.longValue();
	        	
	        	if ( ct < SystemTime.getCurrentTime()){
	        	
	        		download_manager.setCreationTime( ct );
	        	}
	        }
	        
	        //TODO: remove this try/catch.  should only be needed for those upgrading from previous snapshot
	        try {
	        	//load file priorities
	        	List file_priorities = (List) save_download_state.get("file_priorities");
	        	if ( file_priorities != null ) download_manager.setData( "file_priorities", file_priorities );
	        }
	        catch (Throwable t) { Debug.printStackTrace( t ); }
        }else{
        	
        		// no stats, bodge the uploaded for seeds
           
        	if ( dm_stats.getDownloadCompleted(false) == 1000 ){

	            long lUploadedValue = ( download_manager.getSize()+999 ) * minQueueingShareRatio / 1000;
	                  
		        dm_stats.setSavedDownloadedUploaded(download_manager.getSize(), lUploadedValue);
	        }
        }
        
        boolean isCompleted = download_manager.getStats().getDownloadCompleted(false) == 1000;
	   
        if (download_manager.getPosition() == -1) {
	        int endPosition = 0;
	        for (int i = 0; i < managers.size(); i++) {
	          DownloadManager dm = (DownloadManager) managers.get(i);
	          boolean dmIsCompleted = dm.getStats().getDownloadCompleted(false) == 1000;
	          if (dmIsCompleted == isCompleted)
	            endPosition++;
	        }
	        download_manager.setPosition(endPosition + 1);
	      }
	      
	      // Even though when the DownloadManager was created, onlySeeding was
	      // most likely set to true for completed torrents (via the Initializer +
	      // readTorrent), there's a chance that the torrent file didn't have the
	      // resume data.  If it didn't, but we marked it as complete in our
	      // downloads config file, we should set to onlySeeding
	      download_manager.setOnlySeeding(isCompleted);

        managers.add(download_manager);
        
        TOTorrent	torrent = download_manager.getTorrent();
        
        if ( torrent != null ){
        	
        	try{
        		manager_map.put( new HashWrapper(torrent.getHash()), download_manager );
        		
        	}catch( TOTorrentException e ){
        		
        		Debug.printStackTrace( e );
        	}
        }

        listeners.dispatch( LDT_MANAGER_ADDED, download_manager );
        
        download_manager.addListener(this);
        
        if ( save_download_state != null ){
        	
            Long lForceStart = (Long) save_download_state.get("forceStart");
            if (lForceStart == null) {
                Long lStartStopLocked = (Long) save_download_state.get("startStopLocked");
                if(lStartStopLocked != null) {
                	lForceStart = lStartStopLocked;
                }
              }     

            if(lForceStart != null) {
              if(lForceStart.intValue() == 1) {
                download_manager.setForceStart(true);
              }
            }
        }
      }finally{
      	
      	managers_mon.exit();
      }
 
      if (save)
        saveDownloads();
      
      return( download_manager );
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
  
  public AEMonitor
  getDownloadManagersMonitor()
  {
  	return( managers_mon );
  }
  
  public DownloadManager getDownloadManager(TOTorrent torrent) {
    try {
      return getDownloadManager(torrent.getHash());
    } catch (TOTorrentException e) {
      return null;
    }
  }

  protected DownloadManager 
  getDownloadManager(byte[]	hash) 
  {
      return (DownloadManager)manager_map.get(new HashWrapper(hash));
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
  	
    try{
    	managers_mon.enter();
    	
      managers.remove(manager);
      
      TOTorrent	torrent = manager.getTorrent();
      
      if ( torrent != null ){
      	
      	try{
      		manager_map.remove(new HashWrapper(torrent.getHash()));
      		
      	}catch( TOTorrentException e ){
      		
      		Debug.printStackTrace( e );
      	}
      }
    }finally{
    	
    	managers_mon.exit();
    }
    
    fixUpDownloadManagerPositions();
    listeners.dispatch( LDT_MANAGER_REMOVED, manager );
    manager.removeListener(this);
    
    saveDownloads();

    if (manager.getTorrent() != null) {

      trackerScraper.remove(manager.getTorrent());
    }

    if (manager.getCategory() != null)
      manager.setCategory(null);
  }

  /* Puts GlobalManager in a stopped state.
   * Used when closing down Azureus.
   */
  public void stopAll() {
    if (!isStopped){
    	
    	informDestroyInitiated();
    	
    	if ( host_support != null ){
    		host_support.destroy();
    	}
      
      torrent_folder_watcher.destroy();
    	
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
	    					return( null );
	    				}
	    			});
    	}catch( Throwable e ){
    		Debug.printStackTrace( e );
    	}
    	
      checker.stopIt();
      
      saveDownloads();
      
      stopAllDownloads();

      if ( stats_writer != null ){
      	
      	stats_writer.destroy();
      }
      
      managers.clear();
      manager_map.clear();
      
      isStopped = true;
      
      informDestroyed();
    }
  }

  /**
   * Stops all downloads without removing them
   *
   * @author Rene Leonhardt
   */
  public void stopAllDownloads() {
    stopAllDownloads(DownloadManager.STATE_STOPPED);
  }

  public void stopAllDownloads(int stateAfterStopping) {
    for (Iterator iter = managers.iterator(); iter.hasNext();) {
      DownloadManager manager = (DownloadManager) iter.next();
      
      int state = manager.getState();
      
      if( state != DownloadManager.STATE_STOPPED &&
          state != DownloadManager.STATE_STOPPING ) {
        
        manager.stopIt( stateAfterStopping );
      }
    }
  }
  
  
  /**
   * Starts all downloads
   */
  public void startAllDownloads() {    
    for (Iterator iter = managers.iterator(); iter.hasNext();) {
      DownloadManager manager = (DownloadManager) iter.next();

      if( manager.getState() == DownloadManager.STATE_STOPPED ) {
        manager.startDownloadInitialized(true);
      }
    }
  }
  
  
  
  public void pauseDownloads() {
    for( Iterator i = managers.iterator(); i.hasNext(); ) {
      DownloadManager manager = (DownloadManager)i.next();
      
      if ( manager.getTorrent() == null ) {
        continue;
      }
      
      int state = manager.getState();
      
      if( state != DownloadManager.STATE_STOPPED &&
          state != DownloadManager.STATE_ERROR &&
          state != DownloadManager.STATE_STOPPING ) {
        
        try {
          manager.stopIt( DownloadManager.STATE_STOPPED );
          try {  paused_list_mon.enter();
            paused_list.add( manager.getTorrent().getHashWrapper() );
          }
          finally {  paused_list_mon.exit();  }
        }
        catch( TOTorrentException e ) {  Debug.printStackTrace( e );  }
      }
    }
  }
  
  
  public boolean canPauseDownloads() {
    for( Iterator i = managers.iterator(); i.hasNext(); ) {
      DownloadManager manager = (DownloadManager)i.next();
      
      if( manager.getTorrent() == null ) {
        continue;
      }
      
      int state = manager.getState();
      
      if( state != DownloadManager.STATE_STOPPED &&
          state != DownloadManager.STATE_ERROR &&
          state != DownloadManager.STATE_STOPPING ) {
        
        return true;
      }
    }
    
    return false;
  }


  public void resumeDownloads() {
    try {  paused_list_mon.enter();
      for( int i=0; i < paused_list.size(); i++ ) {      
        HashWrapper hash = (HashWrapper)paused_list.get( i );
        DownloadManager manager = getDownloadManager( hash.getHash() );
      
        if( manager != null && manager.getState() == DownloadManager.STATE_STOPPED ) {
          manager.startDownloadInitialized( true );
        }
      }
      paused_list.clear();
    }
    finally {  paused_list_mon.exit();  }
  }


  public boolean canResumeDownloads() {
    try {  paused_list_mon.enter();
      for( int i=0; i < paused_list.size(); i++ ) {      
        HashWrapper hash = (HashWrapper)paused_list.get( i );
        DownloadManager manager = getDownloadManager( hash.getHash() );
      
        if( manager != null && manager.getState() == DownloadManager.STATE_STOPPED ) {
          return true;
        }
      }
    }
    finally {  paused_list_mon.exit();  }
    
    return false;
  }
  
  
  
  private void loadDownloads(AzureusCoreListener listener) 
  {
  	try{
       Map map = FileUtil.readResilientConfigFile("downloads.config");
      
      boolean debug = Boolean.getBoolean("debug");
 
      Iterator iter = null;
      //v2.0.3.0+ vs older mode
      List downloads = (List) map.get("downloads");
      int nbDownloads;
      if (downloads == null) {
        //No downloads entry, then use the old way
        iter = map.values().iterator();
        nbDownloads = map.size();
      }
      else {
        //New way, downloads stored in a list
        iter = downloads.iterator();
        nbDownloads = downloads.size();
      }
      int currentDownload = 0;
      while (iter.hasNext()) {
        currentDownload++;        
        Map mDownload = (Map) iter.next();
        try {
          Long	lPersistent = (Long)mDownload.get( "persistent" );
          
          boolean	persistent = lPersistent==null || lPersistent.longValue()==1;
          
          String fileName = new String((byte[]) mDownload.get("torrent"), Constants.DEFAULT_ENCODING);
          
          if( listener != null && nbDownloads > 0 ){
          	
            listener.reportPercent(100 * currentDownload / nbDownloads);
          }
          
          if(listener != null) {
          	
            listener.reportCurrentTask(MessageText.getString("splash.loadingTorrent") 
                + " " + currentDownload + " "
                + MessageText.getString("splash.of") + " " + nbDownloads
                + " : " + fileName );
          }
          
          //migration from using a single savePath to a separate dir and file entry
          String	torrent_save_dir;
          String	torrent_save_file;
          
          byte[] torrent_save_dir_bytes   = (byte[]) mDownload.get("save_dir");
          
          if ( torrent_save_dir_bytes != null ){
          	
          	byte[] torrent_save_file_bytes 	= (byte[]) mDownload.get("save_file");
          	       
          	torrent_save_dir	= new String(torrent_save_dir_bytes, Constants.DEFAULT_ENCODING);
          	  
          	if ( torrent_save_file_bytes != null ){
          		
          		torrent_save_file	= new String(torrent_save_file_bytes, Constants.DEFAULT_ENCODING);       		
          	}else{
          		
          		torrent_save_file	= null;
          	}
          }else{
            
            byte[] savePathBytes = (byte[]) mDownload.get("path");
          	torrent_save_dir 	= new String(savePathBytes, Constants.DEFAULT_ENCODING);
          	torrent_save_file	= null;
          }
          
          
          
          int state = DownloadManager.STATE_WAITING;
          if (debug){
          	
            state = DownloadManager.STATE_STOPPED;
            
          }else {
          	
            if (mDownload.containsKey("state")) {
              state = ((Long) mDownload.get("state")).intValue();
              if (state != DownloadManager.STATE_STOPPED &&
                  state != DownloadManager.STATE_QUEUED &&
                  state != DownloadManager.STATE_WAITING)
              	
                state = DownloadManager.STATE_QUEUED;
              
            }else{
            	
              int stopped = ((Long) mDownload.get("stopped")).intValue();
              
              if (stopped == 1){
              	
                state = DownloadManager.STATE_STOPPED;
              }
            } 
          }        

          saved_download_manager_state.put( fileName, mDownload );
          
          	// for non-persistent downloads the state will be picked up if the download is re-added
          	// it won't get saved unless it is picked up, hence dead data is dropped as required
          
          if ( persistent ){
          	
          	DownloadManager dm = DownloadManagerFactory.create(this, fileName, torrent_save_dir, torrent_save_file, state, true, true );
          	
            addDownloadManager(dm, false);
          }
        }
        catch (UnsupportedEncodingException e1) {
          //Do nothing and process next.
        }
        catch (Throwable e) {
          LGLogger.log("Error while loading downloads.  One download may not have been added to the list.");
          Debug.printStackTrace( e );
        }
      }
      
      //load pause/resume state
      ArrayList pause_data = (ArrayList)map.get( "pause_data" );
      if( pause_data != null ) {
        try {  paused_list_mon.enter();
          for( int i=0; i < pause_data.size(); i++ ) {
            byte[] key = (byte[])pause_data.get( i );
            paused_list.add( new HashWrapper( key ) );
          }
        }
        finally {  paused_list_mon.exit();  }
      }
      

      // Someone could have mucked with the config file and set weird positions,
      // so fix them up.
      fixUpDownloadManagerPositions();
      LGLogger.log("Loaded " + managers.size() + " torrents");
  	}catch( Throwable e ){
  			// there's been problems with corrupted download files stopping AZ from starting
  			// added this to try and prevent such foolishness
  		
  		Debug.printStackTrace( e );
  	}
  }


  private void saveDownloads() 
  {
    //    if(Boolean.getBoolean("debug")) return;

  	try{
  		managers_mon.enter();
  	
      LGLogger.log("Saving Download List (" + managers.size() + " items)");
	    Map map = new HashMap();
	    List list = new ArrayList(managers.size());
	    for (int i = 0; i < managers.size(); i++) {
	      DownloadManager dm = (DownloadManager) managers.get(i);
	      
	      	DownloadManagerStats dm_stats = dm.getStats();
		      Map dmMap = new HashMap();
		      dmMap.put("persistent", new Long(dm.isPersistent()?1:0));
		      dmMap.put("torrent", dm.getTorrentFileName());
		      dmMap.put("save_dir", dm.getTorrentSaveDir());
		      dmMap.put("save_file", dm.getTorrentSaveFile());
          
		      	//TODO: remove after later release...it makes sure older versions can load this version's downloads.config
		      
		      if ( dm.getTorrentSaveFile() == null ){
		      	
		      	dmMap.put("path", new File( dm.getTorrentSaveDir()).getAbsolutePath() );
		      	
		      }else{
		      
		      	dmMap.put("path", new File( dm.getTorrentSaveDir(), dm.getTorrentSaveFile() ).getAbsolutePath() );
		      }
		      dmMap.put("uploads", new Long(dm_stats.getMaxUploads()));
		      dmMap.put("maxdl", new Long(dm_stats.getMaxDownloadKBSpeed()));
		      dmMap.put("maxul", new Long(dm_stats.getUploadRateLimitBytesPerSecond()));
          int state = dm.getState();
          if (dm.getOnlySeeding() && !dm.isForceStart() && 
              state != DownloadManager.STATE_STOPPED) {
            state = DownloadManager.STATE_QUEUED;
          } else if (state == DownloadManager.STATE_ERROR)
            state = DownloadManager.STATE_STOPPED;
          else if (state != DownloadManager.STATE_STOPPED &&
                  state != DownloadManager.STATE_QUEUED &&
                  state != DownloadManager.STATE_WAITING)
            state = DownloadManager.STATE_WAITING;
          dmMap.put("state", new Long(state));		      
		      dmMap.put("position", new Long(dm.getPosition()));
		      dmMap.put("downloaded", new Long(dm_stats.getDownloaded()));
		      dmMap.put("uploaded", new Long(dm_stats.getUploaded()));
		      dmMap.put("completed", new Long(dm_stats.getDownloadCompleted(true)));
		      dmMap.put("discarded", new Long(dm_stats.getDiscarded()));
		      dmMap.put("hashfails", new Long(dm_stats.getHashFails()));
		      dmMap.put("forceStart", new Long(dm.isForceStart() && (dm.getState() != DownloadManager.STATE_CHECKING) ? 1 : 0));
          dmMap.put("secondsDownloading", new Long(dm_stats.getSecondsDownloading()));
          dmMap.put("secondsOnlySeeding", new Long(dm_stats.getSecondsOnlySeeding()));
		      Category category = dm.getCategory();
		      if (category != null && category.getType() == Category.TYPE_USER)
		        dmMap.put("category", category.getName());
          
		      dmMap.put( "creationTime", new Long( dm.getCreationTime()));
		      
		      //save file priorities
 
		  dm.saveDownload();
		  
          List file_priorities = (List)dm.getData( "file_priorities" );
          if ( file_priorities != null ) dmMap.put( "file_priorities" , file_priorities );

          dmMap.put( "allocated", new Long( dm.isDataAlreadyAllocated() == true ? 1 : 0 ) );

		      list.add(dmMap);
	      }
	   
	    map.put("downloads", list);
      
      //save pause/resume state
      try {  paused_list_mon.enter();
	      if( !paused_list.isEmpty() ) {
	        ArrayList pause_data = new ArrayList();
	        for( int i=0; i < paused_list.size(); i++ ) {
	          HashWrapper hash = (HashWrapper)paused_list.get( i );
	          pause_data.add( hash.getHash() );
	        }
	        map.put( "pause_data", pause_data );
	      }
      }
      finally {  paused_list_mon.exit();  }
      
        
	    FileUtil.writeResilientConfigFile("downloads.config", map );
  	}finally{
  		
  		managers_mon.exit();
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
        (COConfigurationManager.getIntParameter("StartStopManager_iRankType") != 0) &&
        (COConfigurationManager.getBooleanParameter("StartStopManager_bAutoReposition")))
      return false;

    return manager.getPosition() > 1;
  }

  public boolean isMoveableDown(DownloadManager manager) {
    if (managers == null)
      return false;

    boolean isCompleted = manager.getStats().getDownloadCompleted(false) == 1000;

    if (isCompleted &&
        (COConfigurationManager.getIntParameter("StartStopManager_iRankType") != 0) &&
        (COConfigurationManager.getBooleanParameter("StartStopManager_bAutoReposition")))
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
      try{
      	managers_mon.enter();
      
      	int newPosition = 1;
        for (int i = 0; i < manager.length; i++)
        	moveTo(manager[i], newPosition++);
      }finally{
      	
      	managers_mon.exit();
      }
  }

  public void moveEnd(DownloadManager[] manager) {
    if (managers != null)
      try{
      	managers_mon.enter();
      
        int endPosComplete = 0;
        int endPosIncomplete = 0;
        for (int j = 0; j < managers.size(); j++) {
          DownloadManager dm = (DownloadManager) managers.get(j);
          if (dm.getStats().getDownloadCompleted(false) == 1000)
            endPosComplete++;
          else
            endPosIncomplete++;
        }
        for (int i = manager.length - 1; i >= 0; i--) {
          if (manager[i].getStats().getDownloadCompleted(false) == 1000 && endPosComplete > 0) {
            moveTo(manager[i], endPosComplete--);
          } else if (endPosIncomplete > 0) {
            moveTo(manager[i], endPosIncomplete--);
          }
        }
      }finally{
      	managers_mon.exit();
      }
  }
  
  public void moveTo(DownloadManager manager, int newPosition) {
    if (newPosition < 1)
      return;

    if (managers != null)
      try{
      	managers_mon.enter();
      
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
      }finally{
      	
      	managers_mon.exit();
      }
  }
	
	public void fixUpDownloadManagerPositions() {
    if (managers != null) {
      try{
      	managers_mon.enter();
      
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
      }finally{
      	
      	managers_mon.exit();
      }
    }
	}
  
  protected void  informDestroyed() {
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
					long	start = SystemTime.getCurrentTime();
							
					while(true){
								
						try{
							Thread.sleep(2500);
						}catch( Throwable e ){
							e.printStackTrace();
						}
								
						if ( SystemTime.getCurrentTime() - start > 10000 ){
									
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
  	
  public void informDestroyInitiated()  {
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

			// Don't use Dispatch.. async is bad (esp for plugin initialization)
			try{
				managers_mon.enter();
			
				for (int i=0;i<managers.size();i++){
					
				  listener.downloadManagerAdded((DownloadManager)managers.get(i));
				}	
			}finally{
				
				managers_mon.exit();
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
        try{
        	managers_mon.enter();
        
          if(lastSelectedIndex < 0 || lastSelectedIndex >= managers.size())
            lastSelectedIndex = -1;
          lastSelectedIndex++;
          for (int i = lastSelectedIndex; i < managers.size(); i++) {
            char test = Character.toLowerCase(((DownloadManager) managers.get(i)).getDisplayName().charAt(0));
            if(test == c)
              return i;
          }
          for (int i = 0; i < lastSelectedIndex; i++) {
            char test = Character.toLowerCase(((DownloadManager) managers.get(i)).getDisplayName().charAt(0));
            if(test == c)
              return i;
          }
        }finally{
        	
        	managers_mon.exit();
        }
      }
    }
    return -1;
  }

  // DownloadManagerListener

  //make sure we update 'downloads.config' on state changes
	public void stateChanged(DownloadManager manager, int state) {
	  needsSaving = true;
	}
		
	public void downloadComplete(DownloadManager manager) { }

  public void completionChanged(DownloadManager manager, boolean bCompleted) { }
  
  public void positionChanged(DownloadManager download, int oldPosition, int newPosition) {
  };
}
