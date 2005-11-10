/*
 * Created on 29-Jul-2005
 * Created by Paul Gardner
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.core3.download.impl;

import java.io.File;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerFactory;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.disk.DiskManagerListener;
import org.gudy.azureus2.core3.disk.DiskManagerPiece;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerDiskListener;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPeerManagerFactory;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.ListenerManager;
import org.gudy.azureus2.core3.util.ListenerManagerDispatcher;
import org.gudy.azureus2.core3.util.NonDaemonTask;
import org.gudy.azureus2.core3.util.NonDaemonTaskRunner;

public class 
DownloadManagerController 
{
		// DISK listeners
	
	private static final int LDT_DL_ADDED		= 1;
	private static final int LDT_DL_REMOVED		= 2;

	private static ListenerManager	disk_listeners_agregator 	= ListenerManager.createAsyncManager(
			"DMC:DiskListenAgregatorDispatcher",
			new ListenerManagerDispatcher()
			{
				public void
				dispatch(
					Object		_listener,
					int			type,
					Object		value )
				{
					DownloadManagerDiskListener	listener = (DownloadManagerDiskListener)_listener;
					
					if ( type == LDT_DL_ADDED ){
						
						listener.diskManagerAdded((DiskManager)value);
						
					}else if ( type == LDT_DL_REMOVED ){
						
						listener.diskManagerRemoved((DiskManager)value);
					}
				}
			});	
	
	private ListenerManager	disk_listeners 	= ListenerManager.createManager(
			"DMC:DiskListenDispatcher",
			new ListenerManagerDispatcher()
			{
				public void
				dispatch(
					Object		listener,
					int			type,
					Object		value )
				{
					disk_listeners_agregator.dispatch( listener, type, value );
				}
			});
	
	private AEMonitor	disk_listeners_mon	= new AEMonitor( "DownloadManagerController:DL" );
	
	protected AEMonitor	this_mon	= new AEMonitor( "DownloadManagerController" );
	protected AEMonitor	state_mon	= new AEMonitor( "DownloadManagerController:State" );

	
	private DownloadManagerImpl			download_manager;
	private DownloadManagerStatsImpl	stats;
	
		// these are volatile as we want to ensure that if a state is read it is always the
		// most up to date value available (as we don't synchronize state read - see below
		// for comments)
	
	private volatile int		state_set_by_method = DownloadManager.STATE_START_OF_DAY;
	private volatile int		substate;
	private volatile boolean 	force_start;

	private DiskManager 			disk_manager;
	private DiskManagerFileInfo[]	skeleton_files;
	
	private PEPeerManager 			peer_manager;
	
	private String errorDetail;


	
	protected
	DownloadManagerController(
		DownloadManagerImpl	_download_manager )
	{
		download_manager = _download_manager;
		
		stats	= (DownloadManagerStatsImpl)download_manager.getStats();
	}
	
	protected void
	setInitialState(
		int	initial_state )
	{
			// only take note if there's been no errors
		
		if ( getState() == DownloadManager.STATE_START_OF_DAY ){
			
			setState( initial_state, true );
		}
	}

	

	public void 
	startDownload(
		TRTrackerAnnouncer	tracker_client ) 
	{
		try{
			this_mon.enter();
		
			if ( getState() != DownloadManager.STATE_READY ){
			
				Debug.out( "DownloadManagerController::startDownload state must be ready, " + getState());
				
				return;
			}
			
			if ( peer_manager != null ){
				
				Debug.out( "DownloadManagerController::startDownload: peer manager not null" );
			}
			
			if ( disk_manager == null ){
				
				Debug.out( "DownloadManagerController::startDownload: disk manager is null" );
			}
		
			setState( DownloadManager.STATE_DOWNLOADING, false );
		
		}finally{
			
			this_mon.exit();
	
			download_manager.informStateChanged();
		}
		
				// make sure it is started beore making it "visible"
		
		PEPeerManager temp = 
				PEPeerManagerFactory.create( download_manager, tracker_client, disk_manager);
		
		temp.start();
	
		peer_manager = temp;
		
		download_manager.informPeerManagerAdded( temp );
	}
  
  
  

	public void 
	initializeDiskManager(
		int	initialising_state ) 
	{
		initializeDiskManagerSupport(
			initialising_state,
			new DiskManagerListener()
	  			{
	  				public void
	  				stateChanged(
	  					int 	oldDMState,
	  					int		newDMState )
	  				{
	  					DiskManager	dm = disk_manager;
	  					
	  					if ( dm == null ){
	  						
	  							// already been cleared down
	  							
	  						return;
	  					}
	  					
	  					try{
			  				if ( newDMState == DiskManager.FAULTY ){
			  					
			  					setFailed( dm.getErrorMessage());						
			   				}
			  					
			  				if ( oldDMState == DiskManager.CHECKING ){
			  						
			  					stats.setDownloadCompleted(stats.getDownloadCompleted(true));
			  						
			  					download_manager.setOnlySeeding(dm.getRemaining() == 0);
			  				}
			  					  
			  				if ( newDMState == DiskManager.READY ){
			  						
			  					if ( 	stats.getTotalDataBytesReceived() == 0 &&
			  							stats.getTotalDataBytesSent() == 0 &&
			  							stats.getSecondsDownloading() == 0 ){

			  						int	completed = stats.getDownloadCompleted(false);
	  							
			  						if ( completed < 1000 ){
		  							
				  						// make up some sensible "downloaded" figure for torrents that have been re-added to Azureus
				  						// and resumed 
				  				
				  									  										 
			  								// assume downloaded = uploaded, optimistic but at least results in
			  								// future share ratios relevant to amount up/down from now on
			  								// see bug 1077060 
			  								
			  							long	amount_downloaded = (completed*dm.getTotalLength())/1000;
			  								
			 							stats.setSavedDownloadedUploaded( amount_downloaded, amount_downloaded );
			   						
			  						}else{		  					
			  								// see GlobalManager for comment on this
			  							
			  							int	dl_copies = COConfigurationManager.getIntParameter("StartStopManager_iAddForSeedingDLCopyCount");
			  		              
										if ( dl_copies > 0 ){
											
			  								stats.setSavedDownloadedUploaded( download_manager.getSize()*dl_copies, stats.getTotalDataBytesSent());
			  							}
										
							        	download_manager.getDownloadState().setFlag( DownloadManagerState.FLAG_ONLY_EVER_SEEDED, true );
			  						}
			  		        	}
			  				}
	  					}finally{
	  							  						
	  						download_manager.informStateChanged();
	  					}
	  				}

	                public void 
					filePriorityChanged(
						DiskManagerFileInfo	file ) 
	                {                    
	                }
	                
	               	public void
	            	pieceDoneChanged(
	            		DiskManagerPiece	piece )
	            	{           		
	            	}
	               	
	            	public void
	            	fileAccessModeChanged(
	            		DiskManagerFileInfo		file,
	            		int						old_mode,
	            		int						new_mode )
	            	{
	            	}
	  			});
	}
	
	protected void 
	initializeDiskManagerSupport(
		int						initialising_state,
		DiskManagerListener		listener ) 
	{
		try{
			this_mon.enter();
		
			int	entry_state = getState();
				
			if ( 	entry_state != DownloadManager.STATE_WAITING &&
					entry_state != DownloadManager.STATE_STOPPED &&
					entry_state != DownloadManager.STATE_QUEUED &&
					entry_state != DownloadManager.STATE_ERROR ){
					
				Debug.out( "DownloadManagerController::initializeDiskManager: Illegal initialize state, " + entry_state );
				
				return;
			}
	
			if ( disk_manager != null ){
				
				Debug.out( "DownloadManagerController::initializeDiskManager: disk manager is not null" );
				
				return;
			}
		
			errorDetail	= "";
					
			setState( initialising_state, false );
				  		
		  	DiskManager dm = DiskManagerFactory.create( download_manager.getTorrent(), download_manager);
	  	      
	  	  	setDiskManager( dm );
	  	  		
	  	  	disk_manager.addListener( listener );
	  	  	
		}finally{
			
			this_mon.exit();
		
			download_manager.informStateChanged();
		}
	}
	  	  
	public boolean 
	canForceRecheck() 
	{
	  	int state = getState();
	  	  	
	  		// gotta check error + disk manager state as error can be generated by both
	  		// an overall error or a running disk manager in faulty state
	  	
	  	return(		(state == DownloadManager.STATE_STOPPED ) ||
	  	           	(state == DownloadManager.STATE_QUEUED ) ||
	  	           	(state == DownloadManager.STATE_ERROR && disk_manager == null));
	}

	public void 
	forceRecheck() 
	{
		try{
			this_mon.enter();
		
			if ( disk_manager != null || !canForceRecheck() ){
				
				Debug.out( "DownloadManagerController::forceRecheck: illegal entry state" );
				
				return;
			}
			
			final int start_state = DownloadManagerController.this.getState();
	  								
				// remove resume data
			
	  		download_manager.getDownloadState().clearResumeData();
	  					
	  			// For extra protection from a plugin stopping a checking torrent,
	  			// fake a forced start. 
	  					
	  		final boolean wasForceStarted = force_start;
	  					
	  		force_start = true;
			
				// if a file has been deleted we want this recheck to recreate the file and mark
				// it as 0%, not fail the recheck. Otherwise the only way of recovering is to remove and
				// re-add the torrent
  					
	  		download_manager.setDataAlreadyAllocated( false );
	  					
	  		initializeDiskManagerSupport( 
	  			DownloadManager.STATE_CHECKING,
	 	  		new DiskManagerListener()
  	  			{
  	  				public void
  	  				stateChanged(
  	  					int 	oldDMState,
  	  					int		newDMState )
  	  				{
  	  					if ( newDMState == DiskManager.READY || newDMState == DiskManager.FAULTY ){
  	  						
  	  						if ( disk_manager == null ){
  	  								
  	  								// already closed down via stop
  	  							
  	  							return;
  	  						}
  	  							
	  	  					force_start = wasForceStarted;
		  					
	  	  					stats.setDownloadCompleted(stats.getDownloadCompleted(true));
		  					
	  	  					if ( newDMState == DiskManager.READY ){
		  						
	  	  						try{
	  	  							boolean	only_seeding 		= false;
	  	  							boolean	update_only_seeding	= false;
	  	  						
	  	  							try{
	  	  								this_mon.enter();
	  	  							
	  	  								if ( disk_manager != null ){
	  	  							
	  	  									disk_manager.dumpResumeDataToDisk(true, false);
		  					  		
	  	  									disk_manager.stop();
		  							
	  	  									only_seeding	= disk_manager.getRemaining() == 0;
	  	  									
	  	  									update_only_seeding	= true;
	  	  								
	  		  	  							setDiskManager( null );
		  							
		  							
	  		  	  							if ( start_state == DownloadManager.STATE_ERROR ){
		  								
	  		  	  								setState( DownloadManager.STATE_STOPPED, false );
		  								
	  		  	  							}else{
		  								
	  		  	  								setState( start_state, false );
	  		  	  							}
	  	  								}
	  	  							}finally{
	  	  								
	  	  								this_mon.exit();
	  	  							
	  	  								download_manager.informStateChanged();
	  	  							}
	  	  							
	  	  								// careful here, don't want to update seeding while holding monitor
	  	  								// as potential deadlock
	  	  							
	  	  							if ( update_only_seeding ){
	  	  								
	  	  								download_manager.setOnlySeeding( only_seeding );
	  	  							}
	  	  						
	  	  						}catch( Exception e ){
		  					  		
	  	  							setFailed( "Resume data save fails: " + Debug.getNestedExceptionMessage(e));
	  	  						}
	  	  					}else{ // Faulty
		  					  		
  	  							try{
  	  								this_mon.enter();
  	  							
  	  								DiskManager	dm = disk_manager;
  	  								
  	  								if ( dm != null ){

  	  									dm.stop();
		  					
  	  									setDiskManager( null );
		  						
  	  									setFailed( dm.getErrorMessage());	 
  	  								}
  	  							}finally{
  	  								
  	  								this_mon.exit();
  	  							}
  	  							
	  	  						download_manager.setOnlySeeding(false);
	  	  					}
	  					}
  	  				}

  	                public void 
  					filePriorityChanged(
  						DiskManagerFileInfo	file ) 
  	                {                    
  	                }
  	                
  	               	public void
  	            	pieceDoneChanged(
  	            		DiskManagerPiece	piece )
  	            	{           		
  	            	}
  	               	
  	            	public void
  	            	fileAccessModeChanged(
  	            		DiskManagerFileInfo		file,
  	            		int						old_mode,
  	            		int						new_mode )
  	            	{
  	            	}
  	  			});
	  		
		}finally{
			
			this_mon.exit();
		}
	}  	  
  
	public void 
	stopIt(
		final int 			_stateAfterStopping, 
		final boolean 		remove_torrent, 
		final boolean 		remove_data )
	{	  
		try{
			this_mon.enter();
		
			int	state = getState();
		  
			if ( 	state == DownloadManager.STATE_STOPPED ||
					( state == DownloadManager.STATE_ERROR && disk_manager == null )) {
	    
				//already in stopped state, just do removals if necessary
	    	
				if( remove_data ){
				  
					download_manager.deleteDataFiles();
				}
	      
				if( remove_torrent ){
					
					download_manager.deleteTorrentFile();
				}
	      
				setState( _stateAfterStopping, false );
	      
				return;
			}

    
			if ( state == DownloadManager.STATE_STOPPING){
    
				return;
			}
    
			setSubState( _stateAfterStopping );
			
			setState( DownloadManager.STATE_STOPPING, false );


				// this will run synchronously but on a non-daemon thread so that it will under
  				// normal circumstances complete, even if we're closing
  	

			final	AESemaphore nd_sem = new AESemaphore( "DM:DownloadManager.NDTR" );
			
			NonDaemonTaskRunner.runAsync(
				new NonDaemonTask()
				{
					public Object
					run()
					{
						nd_sem.reserve();
						
						return( null );
					}
					
				});
						
			try{
				int	stateAfterStopping = _stateAfterStopping;
				
				try{
		  								
					if ( peer_manager != null ){
						
					  stats.saveSessionTotals();
					  						  
					  peer_manager.stopAll(); 
					}
					
						// do this even if null as it also triggers tracker actions
					
					download_manager.informPeerManagerRemoved( peer_manager );
						
					peer_manager	= null;

					if ( disk_manager != null ){
						
						stats.setCompleted(stats.getCompleted());
						stats.setDownloadCompleted(stats.getDownloadCompleted(true));
			      
						if (disk_manager.getState() == DiskManager.READY){
					  	
							try{
								disk_manager.dumpResumeDataToDisk(true, false);
					  		
							}catch( Exception e ){
					  		
								errorDetail = "Resume data save fails: " + Debug.getNestedExceptionMessage(e);
							
								stateAfterStopping	= DownloadManager.STATE_ERROR;
							}
						}
			      
					  		// we don't want to update the torrent if we're seeding
					  
						if ( !download_manager.getOnlySeeding()){
					  	
							download_manager.getDownloadState().save();
						}
					  					  
						disk_manager.storeFilePriorities();
					  
						disk_manager.stop();
					  							  
						setDiskManager( null );
					}
				
				 }finally{
							  
				   force_start = false;
         
				   if( remove_data ){
				   
				   		download_manager.deleteDataFiles();
				   }
				   
				   if( remove_torrent ){
				   	
					   download_manager.deleteTorrentFile();
				   }
         
				   setState( stateAfterStopping, true );
         
				 }
			}finally{
				
				nd_sem.release();
			}
			
		}catch( Throwable e ){
  		
			Debug.printStackTrace( e );
		
		}finally{
		
			this_mon.exit();
			
			download_manager.informStateChanged();
		}
	}

	protected void
	setStateWaiting()
	{
		setState(DownloadManager.STATE_WAITING, true );
	}
  
  	protected void
  	setStateFinishing()
  	{
  		setState(DownloadManager.STATE_FINISHING, true);
  	}
  
  	protected void
  	setStateSeeding()
  	{
  		setState(DownloadManager.STATE_SEEDING, true);
  	}
  
  	protected void
  	setStateQueued()
  	{
  		setState(DownloadManager.STATE_QUEUED, true);
  	}
  
  	public int 
  	getState() 
  	{
  		if ( state_set_by_method != DownloadManager.STATE_INITIALIZED ){
		
  			return( state_set_by_method );
  		}
	
  			// we don't want to synchronize here as there are potential deadlock problems
  			// regarding the DownloadManager::addListener call invoking this method while
  			// holding the listeners monitor.
  			// 
  		DiskManager	dm = disk_manager;
   		
	  	if ( dm == null){
			
	  		return DownloadManager.STATE_INITIALIZED;
	  	}
		
  		int diskManagerState = dm.getState();

		if (diskManagerState == DiskManager.INITIALIZING){
		
			return DownloadManager.STATE_INITIALIZED;
			
		}else if (diskManagerState == DiskManager.ALLOCATING){
		  
			return DownloadManager.STATE_ALLOCATING;
			
		}else if (diskManagerState == DiskManager.CHECKING){
		  
			return DownloadManager.STATE_CHECKING;
			
		}else if (diskManagerState == DiskManager.READY){
		  
			return DownloadManager.STATE_READY;
			
		}else if (diskManagerState == DiskManager.FAULTY){
		  
			return DownloadManager.STATE_ERROR;
		}
  		
		return DownloadManager.STATE_ERROR;
  	}
  
	protected int
  	getSubState()
  	{
		if ( state_set_by_method == DownloadManager.STATE_STOPPING ){
			
			return( substate );
		}else{
			
			return( getState());
		}
  	}

	private void
	setSubState(
		int	ss )
	{
		substate	= ss;
	}
	
  	private void 
  	setState(
  		int 		_state,
  		boolean		_inform_changed )
  	{   
  			// we bring this call out of the monitor block to prevent a potential deadlock whereby we chain
  			// state_mon -> this_mon (there exist numerous dependencies this_mon -> state_mon...
  		
  		boolean	call_filesExist	= false;
  		
   		try{
  			state_mon.enter();
  		
	  		int	old_state = state_set_by_method;
		  
			// note: there is a DIFFERENCE between the state held on the DownloadManager and
		    // that reported via getState as getState incorporated DiskManager states when
		    // the DownloadManager is INITIALIZED
		  	//System.out.println( "DM:setState - " + _state );
		  
	  		if ( old_state != _state ){
	    	
	  			state_set_by_method = _state;
	      	      
	  			if (state_set_by_method == DownloadManager.STATE_QUEUED ){
	        
	  				// pick up any errors regarding missing data for queued SEEDING torrents
	    	  
	  				if (  download_manager.getOnlySeeding()){
	    		  
	  					call_filesExist	= true;
	  				}
	    	  
	  			}else if ( state_set_by_method == DownloadManager.STATE_ERROR ){
	      
		      		// the process of attempting to start the torrent may have left some empty
		      		// directories created, some users take exception to this.
		      		// the most straight forward way of remedying this is to delete such empty
		      		// folders here
	      	
	  				TOTorrent	torrent = download_manager.getTorrent();
	    	
	  				if ( torrent != null && !torrent.isSimpleTorrent()){
	
	  					File	save_dir_file	= download_manager.getAbsoluteSaveLocation();
	
	  					if ( save_dir_file.exists() && save_dir_file.isDirectory()){
		      		
	  						FileUtil.recursiveEmptyDirDelete( save_dir_file, false );
	  					}
	  				}
	  			}
	  		}
  		}finally{
  			
  			state_mon.exit();
  		}
	      
  		if ( call_filesExist ){
  			
  			filesExist();
  		}
  		
  		if ( _inform_changed ){
  			
  			download_manager.informStateChanged();
  		}
  	}
  
	 /**
	   * Stops the current download, then restarts it again.
	   */
	  
	public void 
	restartDownload(
		boolean use_fast_resume ) 
	{
		boolean	was_force_start = isForceStart();
		
		if ( !use_fast_resume ){
	      
				//invalidate resume info
	    	
			try{
				disk_manager.dumpResumeDataToDisk(false, true);
				
		  	}catch( Exception e ){
		  		
		  		setFailed( "Resume data save fails: " + Debug.getNestedExceptionMessage(e));
		  	}
		}
	    
		stopIt( DownloadManager.STATE_STOPPED, false, false );
	    
		download_manager.initialize();
	    
		if ( was_force_start ){
	    	
			setForceStart(true);
		}
	}  
	 
  	public boolean 
  	isForceStart() 
  	{
	    return( force_start );
	}

	public void 
	setForceStart(
		boolean _force_start) 
	{
		try{
			state_mon.enter();
		
			if ( force_start != _force_start ){
		    	
				force_start = _force_start;
		      
				int	state = getState();
				
				if (	force_start && 
						(	state == DownloadManager.STATE_STOPPED || 
							state == DownloadManager.STATE_QUEUED )) {
					
						// Start it!  (Which will cause a stateChanged to trigger)
					
					setState(DownloadManager.STATE_WAITING, false );		    	  
				}
		    }
		}finally{
			
			state_mon.exit();
		}
		
			// "state" includes the force-start setting
		
		download_manager.informStateChanged();
	}
	
	protected void
	setFailed(
		String		reason )
	{
		if ( reason != null ){
  		
			errorDetail = reason;
		}
  	
		stopIt( DownloadManager.STATE_ERROR, false, false );
	}

	
	public boolean 
	filesExist() 
	{
		DiskManager	dm = disk_manager;
		
		String strErrMessage = "";
		
			// currently can only seed if whole torrent exists
		
		if ( dm  == null) {
  		
			dm = DiskManagerFactory.createNoStart( download_manager.getTorrent(), download_manager);
		}
		
  		if ( dm.getState() == DiskManager.FAULTY || !dm.filesExist() ){
  			
  			strErrMessage = dm.getErrorMessage();
  		}
  
  	
  		if ( !strErrMessage.equals("")){
     
  			setFailed( MessageText.getString("DownloadManager.error.datamissing") + " " + strErrMessage );
  
  			return( false );
  		}
  		
  		return( true );
	}
	
   	public DiskManagerFileInfo[]
    getDiskManagerFileInfo()
   	{
  		DiskManager	dm = disk_manager;

   		DiskManagerFileInfo[]	res;
   		
   		if ( dm != null ){
   			
   			skeleton_files	= null;
   			
   			res = dm.getFiles();
   			
   		}else{
   			
   			res = skeleton_files;
   			
   			if ( res == null ){

   				res = DiskManagerFactory.getFileInfoSkeleton( download_manager );
   				
   				skeleton_files	= res;
   			}
   		}
   		
   		return( res );
   	}
	
	protected void
	fileInfoChanged()
	{
		skeleton_files = null;
	}
	
	protected PEPeerManager
	getPeerManager()
	{
		return( peer_manager );
	}
	
	protected DiskManager
	getDiskManager()
	{
		return( disk_manager );
	}
	
	protected String
	getErrorDetail()
	{
		return( errorDetail );
	}
	
 	protected void
  	setDiskManager(
  		DiskManager	new_disk_manager )
  	{
 	 	try{
	  		disk_listeners_mon.enter();
	  		
	  		DiskManager	old_disk_manager = disk_manager;
	  		
	  		disk_manager	= new_disk_manager;

	  		if ( new_disk_manager == null && old_disk_manager != null ){
	  				  			
	  			disk_listeners.dispatch( LDT_DL_REMOVED, old_disk_manager );
	  			
	  		}else if ( new_disk_manager != null && old_disk_manager == null ){
	  			
	  			disk_listeners.dispatch( LDT_DL_ADDED, new_disk_manager );
	  			
	  		}else{
	  		
	  			Debug.out( "inconsistent DiskManager state - " + new_disk_manager + "/" + old_disk_manager  );
	  		}
	  		
	  	}finally{
	  		
	  		disk_listeners_mon.exit();
	  	}	
  	}
  	
	public void
	addDiskListener(
		DownloadManagerDiskListener	listener )
	{
	 	try{
	  		disk_listeners_mon.enter();
	  		
	  		disk_listeners.addListener( listener );
	  		
			if ( disk_manager != null ){
		
				disk_listeners.dispatch( listener, LDT_DL_ADDED, disk_manager );
			}
	  	}finally{
	  		
	  		disk_listeners_mon.exit();
	  	}		
	}
		
	public void
	removeDiskListener(
		DownloadManagerDiskListener	listener )
	{
	 	try{
	  		disk_listeners_mon.enter();

	  		disk_listeners.removeListener( listener );
	  		
	 	}finally{
	  		
	  		disk_listeners_mon.exit();
	  	}
	}
}
