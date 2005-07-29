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

import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerFactory;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.disk.DiskManagerListener;
import org.gudy.azureus2.core3.disk.DiskManagerPiece;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerDiskListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPeerManagerFactory;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
import org.gudy.azureus2.core3.util.AEMonitor;
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
	
	private int 		state_set_by_method = DownloadManager.STATE_START_OF_DAY;
	private boolean 	force_start;

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
			
			setState( initial_state );
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
			}
			
			if ( peer_manager != null ){
				
				Debug.out( "DownloadManagerController::startDownload: peer manager not null" );
			}
			
			if ( disk_manager == null ){
				
				Debug.out( "DownloadManagerController::startDownload: disk manager is null" );
			}
		
			setState( DownloadManager.STATE_DOWNLOADING );
		
				// make sure it is started beore making it "visible"
		
			PEPeerManager temp = 
				PEPeerManagerFactory.create( download_manager, tracker_client, disk_manager);
		
			temp.start();
	
			peer_manager = temp;
		
			download_manager.informPeerManagerAdded( temp );
			
		}finally{
			
			this_mon.exit();
		}
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
	  					try{	
	  						this_mon.enter();
	  						
	  						if ( disk_manager == null ){
	  						
	  								// already been cleared down
	  							
	  							return;
	  						}
	  						
		  					if ( newDMState == DiskManager.FAULTY ){
		  						
		  						setFailed( disk_manager.getErrorMessage());						
		   					}
		  					
		  					if ( oldDMState == DiskManager.CHECKING ){
		  						
		  						stats.setDownloadCompleted(stats.getDownloadCompleted(true));
		  						
		  						download_manager.setOnlySeeding(disk_manager.getRemaining() == 0);
		  					}
		  					  
		  					if ( newDMState == DiskManager.READY ){
		  						
		  							// make up some sensible "downloaded" figure for torrents that have been re-added to Azureus
		  							// and resumed 
		  					
		  						if ( 	stats.getTotalDataBytesReceived() == 0 &&
		  								stats.getTotalDataBytesSent() == 0 &&
		  								stats.getSecondsDownloading() == 0 ){
		  						
		  							int	completed = stats.getDownloadCompleted(false);
		  							
		  								// for seeds leave things as they are as they may never have been downloaded in the
		  								// first place...
		  							
		  							if ( completed < 1000 ){
		 
		  									// assume downloaded = uploaded, optimistic but at least results in
		  									// future share ratios relevant to amount up/down from now on
		  									// see bug 1077060 
		  								
		  								long	amount_downloaded = (completed*disk_manager.getTotalLength())/1000;
		  								
		 								stats.setSavedDownloadedUploaded( amount_downloaded, amount_downloaded );
		   							}
		  						}
		  					}
	  					}finally{
	  							  						
	  						download_manager.informStateChanged(getState(),isForceStart());
	  						
	  						this_mon.exit();
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
					
			setState( initialising_state );
				  		
		  	DiskManager dm = DiskManagerFactory.create( download_manager.getTorrent(), download_manager);
	  	      
	  	  	setDiskManager( dm );
	  	  		
	  	  	disk_manager.addListener( listener );
	  	  	
		}finally{
			
			this_mon.exit();
		}
	}
	  	  
	public boolean 
	canForceRecheck() 
	{
		if ( download_manager.getTorrent() == null ){
	  	  		
	  			// broken torrent, can't force recheck
	  	  		
	  		return( false );
	  	}
	  	  	
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
  	  						
  	  						try{
  	  							this_mon.enter();
  	  						
  	  							if ( disk_manager == null ){
  	  								
  	  									// already closed down via stop
  	  								
  	  								return;
  	  							}
  	  							
	  	  						force_start = wasForceStarted;
		  					
	  	  						stats.setDownloadCompleted(stats.getDownloadCompleted(true));
		  					
	  	  						if ( newDMState == DiskManager.READY ){
		  						
	  	  							try{
	  	  								disk_manager.dumpResumeDataToDisk(true, false);
		  					  		
	  	  								disk_manager.stop();
		  							
	  	  								download_manager.setOnlySeeding( disk_manager.getRemaining() == 0);
		  							
	  	  								setDiskManager( null );
		  							
	  	  								if ( start_state == DownloadManager.STATE_ERROR ){
		  								
	  	  									setState( DownloadManager.STATE_STOPPED );
		  								
	  	  								}else{
		  								
	  	  									setState( start_state );
	  	  								}
	  	  								
	  	  							}catch( Exception e ){
		  					  		
	  	  								setFailed( "Resume data save fails: " + Debug.getNestedExceptionMessage(e));
	  	  							}
	  	  						}else{ // Faulty
		  					  						
	  	  							disk_manager.stop();
		  						
	  	  							download_manager.setOnlySeeding(false);
		  						
	  	  							setDiskManager( null );
		  						
	  	  							setFailed( disk_manager.getErrorMessage());	 
	  	  						}
  	  						}finally{
  	  							
  	  							this_mon.exit();
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
	      
				setState( _stateAfterStopping );
	      
				return;
			}

    
			if ( state == DownloadManager.STATE_STOPPING){
    
				return;
			}
    
			setState( DownloadManager.STATE_STOPPING );


		
			// this will run synchronously but on a non-daemon thread so that it will under
  			// normal circumstances complete, even if we're closing
  	

							
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
		             
							   setState( stateAfterStopping );
		             
							 }
	
		}catch( Throwable e ){
  		
			Debug.printStackTrace( e );
		
		}finally{
		
			this_mon.exit();
		}
	}

	public void
	setStateWaiting()
	{
		setState(DownloadManager.STATE_WAITING);
	}
  
  	public void
  	setStateDownloading()
  	{
	  	// null operation as called on pm start + already set?
	  
  		if ( getState() != DownloadManager.STATE_DOWNLOADING ){
		  
  			Debug.out( "setStateDownloading: not dl" );
  		}
	  
  		setState( DownloadManager.STATE_DOWNLOADING );
  	}
  
  	public void
  	setStateFinishing()
  	{
  		setState(DownloadManager.STATE_FINISHING);
  	}
  
  	public void
  	setStateSeeding()
  	{
  		setState(DownloadManager.STATE_SEEDING);
  	}
  
  	public void
  	setStateQueued()
  	{
  		setState(DownloadManager.STATE_QUEUED);
  	}
  
  	public int 
  	getState() 
  	{
  		if ( state_set_by_method != DownloadManager.STATE_INITIALIZED ){
		
  			return( state_set_by_method );
  		}
	
  		try{
  			this_mon.enter();
  		
	  		if ( disk_manager == null){
			
	  			return DownloadManager.STATE_INITIALIZED;
	  		}
		
	  		int diskManagerState = disk_manager.getState();
	
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
			
  		}finally{
  			
  			this_mon.exit();
  		}
  		
		return DownloadManager.STATE_ERROR;
  	}
  

  	private void 
  	setState(
  			int _state)
  	{   
  		try{
  			state_mon.enter();
  		
	  		int	old_state = state_set_by_method;
		  
			// note: there is a DIFFERENCE between the state held on the DownloadManager and
		    // that reported via getState as getState incorporated DiskManager states when
		    // the DownloadManager is INITIALIZED
		  	//System.out.println( "DM:setState - " + _state );
		  
	  		if ( old_state != _state ){
	    	
	  			state_set_by_method = _state;
	      
	  				// sometimes, downloadEnded() doesn't get called, so we must check here too
	      
	  			if (state_set_by_method == DownloadManager.STATE_SEEDING) {
	    	  
	  				download_manager.setOnlySeeding(true);
	        
	  			}else if (state_set_by_method == DownloadManager.STATE_QUEUED ){
	        
	  				// pick up any errors regarding missing data for queued SEEDING torrents
	    	  
	  				if (  download_manager.onlySeeding ){
	    		  
	  					filesExist();
	  				}
	    	  
	  			}else if ( state_set_by_method == DownloadManager.STATE_ERROR ){
	      
		      		// the process of attempting to start the torrent may have left some empty
		      		// directories created, some users take exception to this.
		      		// the most straight forward way of remedying this is to delete such empty
		      		// folders here
	      	
	  				TOTorrent	torrent = download_manager.getTorrent();
	    	
	  				if ( torrent != null && !torrent.isSimpleTorrent()){
	
	  					File	save_dir_file	= new File( download_manager.getTorrentSaveDir(), download_manager.getTorrentSaveFile() );
	
	  					if ( save_dir_file.exists() && save_dir_file.isDirectory()){
		      		
	  						FileUtil.recursiveEmptyDirDelete( save_dir_file );
	  					}
	  				}
	  			}
	      
	  			download_manager.informStateChanged(getState(),isForceStart());
	  		}
  		}finally{
  			
  			state_mon.exit();
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
	    return force_start;
	}

	public void 
	setForceStart(
		 boolean forceStart) 
	{
		try{
			this_mon.enter();
		
			if ( force_start != forceStart ){
		    	
				force_start = forceStart;
		      
				int	state = getState();
				
				if (	force_start && 
						(	state == DownloadManager.STATE_STOPPED || 
							state == DownloadManager.STATE_QUEUED )) {
					
					// Start it!  (Which will cause a stateChanged to trigger)
					
					setState(DownloadManager.STATE_WAITING);
					
				}else{
		    	  
		    	  	// currently the "state" included "force start"
		    	  
					download_manager.informStateChanged(getState(),isForceStart());
				}
		    }
		}finally{
			
			this_mon.exit();
		}
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
