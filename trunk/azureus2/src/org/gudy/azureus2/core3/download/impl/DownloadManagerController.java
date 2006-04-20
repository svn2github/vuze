/*
 * Created on 29-Jul-2005
 * Created by Paul Gardner
 * Copyright (C) 2005, 2006 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.core3.download.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerFactory;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfoListener;
import org.gudy.azureus2.core3.disk.DiskManagerListener;
import org.gudy.azureus2.core3.disk.DiskManagerPiece;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerDiskListener;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPeerManagerAdapter;
import org.gudy.azureus2.core3.peer.PEPeerManagerFactory;
import org.gudy.azureus2.core3.peer.PEPeerSource;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerDataProvider;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.ListenerManager;
import org.gudy.azureus2.core3.util.ListenerManagerDispatcher;
import org.gudy.azureus2.core3.util.NonDaemonTask;
import org.gudy.azureus2.core3.util.NonDaemonTaskRunner;
import org.gudy.azureus2.plugins.network.ConnectionManager;

public class 
DownloadManagerController 
	extends LogRelation
	implements PEPeerManagerAdapter
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
	
	protected AEMonitor	this_mon		= new AEMonitor( "DownloadManagerController" );
	protected AEMonitor	state_mon		= new AEMonitor( "DownloadManagerController:State" );
	protected AEMonitor	skeleton_mon	= new AEMonitor( "DownloadManagerController:Skeletons" );

	
	private DownloadManagerImpl			download_manager;
	private DownloadManagerStatsImpl	stats;
	
		// these are volatile as we want to ensure that if a state is read it is always the
		// most up to date value available (as we don't synchronize state read - see below
		// for comments)
	
	private volatile int		state_set_by_method = DownloadManager.STATE_START_OF_DAY;
	private volatile int		substate;
	private volatile boolean 	force_start;

		// to try and ensure people don't start using disk_manager without properly considering its
		// access implications we've given it a silly name
	
	private volatile DiskManager 			disk_manager_use_accessors;
	
	private DiskManagerFileInfo[]	skeleton_files;
	private fileInfoFacade[]		files_facade		= new fileInfoFacade[0];	// default before torrent avail
	private PEPeerManager 			peer_manager;
	
	private String errorDetail;

	private GlobalManagerStats		global_stats;
	
	
	protected
	DownloadManagerController(
		DownloadManagerImpl	_download_manager )
	{
		download_manager = _download_manager;
		
		GlobalManager	gm = download_manager.getGlobalManager();
					
		global_stats = gm.getStats();
		
		stats	= (DownloadManagerStatsImpl)download_manager.getStats();
	}
	
	protected void
	setInitialState(
		int	initial_state )
	{
		files_facade	= new fileInfoFacade[download_manager.getTorrent()==null?0:download_manager.getTorrent().getFiles().length];
		
		for (int i=0;i<files_facade.length;i++){
			
			files_facade[i] = new fileInfoFacade(i);
		}
		
			// only take note if there's been no errors
		
		if ( getState() == DownloadManager.STATE_START_OF_DAY ){
			
			setState( initial_state, true );
		}
	}

	public void setStateDownloading() {
		if (getState() == DownloadManager.STATE_SEEDING) {
			setState(DownloadManager.STATE_DOWNLOADING, true);
		} else if (getState() != DownloadManager.STATE_DOWNLOADING) {
			Logger.log(new LogEvent(this, LogIDs.CORE, LogEvent.LT_WARNING,
					"Trying to set state to downloading when state is not seeding"));
		}
	}
	

	public void 
	startDownload(
		TRTrackerAnnouncer	tracker_client ) 
	{
		DiskManager	dm;
		
		try{
			this_mon.enter();
		
			if ( getState() != DownloadManager.STATE_READY ){
			
				Debug.out( "DownloadManagerController::startDownload state must be ready, " + getState());
				
				setFailed( "Inconsistent download state: startDownload, state = " + getState());

				return;
			}
			
	 		if ( tracker_client == null ){
	  			
	  			Debug.out( "DownloadManagerController:startDownload: tracker_client is null" );
	  			
	  				// one day we should really do a proper state machine for this. In the meantime...
	  				// probably caused by a "stop" during initialisation, I've reproduced it once or twice
	  				// in my life... Tidy things up so we don't sit here in a READ state that can't
	  				// be started.
	  			
	  			stopIt( DownloadManager.STATE_STOPPED, false, false );
	  			
	  			return;
	  		}
	  			
			if ( peer_manager != null ){
				
				Debug.out( "DownloadManagerController::startDownload: peer manager not null" );
				
					// try and continue....
				
				peer_manager.stopAll();
				
				peer_manager	= null;
			}
			
			dm	= getDiskManager();
			
			if ( dm == null ){
				
				Debug.out( "DownloadManagerController::startDownload: disk manager is null" );
				
				return;
			}
		
			setState( DownloadManager.STATE_DOWNLOADING, false );
		
		}finally{
			
			this_mon.exit();
	
		}
		
			// make sure it is started beore making it "visible"
		
		final PEPeerManager temp = PEPeerManagerFactory.create( tracker_client.getPeerId(), this, dm );
		
		temp.start();
	
		   //The connection to the tracker
		
		tracker_client.setAnnounceDataProvider(
	    		new TRTrackerAnnouncerDataProvider()
	    		{
	    			public String
					getName()
	    			{
	    				return( getDisplayName());
	    			}
	    			
	    			public long
	    			getTotalSent()
	    			{
	    				return(temp.getStats().getTotalDataBytesSent());
	    			}
	    			public long
	    			getTotalReceived()
	    			{
	    				long verified = 
	    					temp.getStats().getTotalDataBytesReceived() - 
	    					( temp.getStats().getTotalDiscarded() + temp.getStats().getTotalHashFailBytes());
	    				
	    				return( verified < 0?0:verified );
	    			}
	    			
	    			public long
	    			getRemaining()
	    			{
	    				return( temp.getRemaining());
	    			}
					
					public String
					getExtensions()
					{
						return( getTrackerClientExtensions());
					}
					
					public int
					getMaxNewConnectionsAllowed()
					{
						return( temp.getMaxNewConnectionsAllowed());
					}
	    		});
	    
		
		peer_manager = temp;

		// Inform only after peer_manager.start(), because it 
		// may have switched it to STATE_SEEDING (in which case we don't need to
		// inform).
		
		if (getState() == DownloadManager.STATE_DOWNLOADING) {
			
			download_manager.informStateChanged();
		}

		download_manager.informStarted( temp );
	}
  
  
  

	public void 
	initializeDiskManager(
		final boolean	open_for_seeding )
	{
		initializeDiskManagerSupport(
			DownloadManager.STATE_INITIALIZED,
			new DiskManagerListener()
	  			{
	  				public void
	  				stateChanged(
	  					int 	oldDMState,
	  					int		newDMState )
	  				{
	  					DiskManager	dm;
	  					
	  					try{
	  						this_mon.enter();
	  					
		  					dm = getDiskManager();

		  					if ( dm == null ){
	  						
	  								// already been cleared down
	  							
		  						return;
		  					}
		  					
	  					}finally{
	  						this_mon.exit();
	  					}
	  					
	  					try{
			  				if ( newDMState == DiskManager.FAULTY ){
			  					
			  					setFailed( dm.getErrorMessage());						
			   				}
			  					
			  				if ( oldDMState == DiskManager.CHECKING ){
			  						
			  					stats.setDownloadCompleted(stats.getDownloadCompleted(true));
			  						
			  					download_manager.setOnlySeeding(isDownloadCompleteExcludingDND());
			  				}
			  					  
			  				if ( newDMState == DiskManager.READY ){
			  					
			  						// good time to trigger minimum file info fixup as the disk manager's
			  						// files are now in a good state
			  					
			  					for (int i=0;i<files_facade.length;i++){
			  						
			  						files_facade[i].fixupMinimum();
			  					}
			  					
			  					if ( 	stats.getTotalDataBytesReceived() == 0 &&
			  							stats.getTotalDataBytesSent() == 0 &&
			  							stats.getSecondsDownloading() == 0 ){

			  						int	completed = stats.getDownloadCompleted(false);
	  							
			  						if ( completed < 1000 ){
		  							
			  							if ( open_for_seeding ){
			  								
			  								setFailed( "File check failed" );
			  								
			  								download_manager.getDownloadState().clearResumeData();
			  								
			  							}else{
			  								
					  						// make up some sensible "downloaded" figure for torrents that have been re-added to Azureus
					  						// and resumed 
					  				
					  									  										 
				  								// assume downloaded = uploaded, optimistic but at least results in
				  								// future share ratios relevant to amount up/down from now on
				  								// see bug 1077060 
				  								
				  							long	amount_downloaded = (completed*dm.getTotalLength())/1000;
				  								
				 							stats.setSavedDownloadedUploaded( amount_downloaded, amount_downloaded );
			  							}
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
	                	download_manager.informPriorityChange( file );
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
				
				setFailed( "Inconsistent download state: initSupport, state = " + entry_state );
				
				return;
			}
	
			DiskManager	old_dm = getDiskManager();
			 
			if ( old_dm != null ){
				
				Debug.out( "DownloadManagerController::initializeDiskManager: disk manager is not null" );
				
					// we shouldn't get here but try to recover the situation
				
				old_dm.stop();
				
				setDiskManager( null );
			}
		
			errorDetail	= "";
					
			setState( initialising_state, false );
				  		
		  	DiskManager dm = DiskManagerFactory.create( download_manager.getTorrent(), download_manager);
	  	      
	  	  	setDiskManager( dm );
	  	  		
	  	  	dm.addListener( listener );
	  	  	
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
	  	           	(state == DownloadManager.STATE_ERROR && getDiskManager() == null));
	}

	public void 
	forceRecheck() 
	{
		try{
			this_mon.enter();
		
			if ( getDiskManager() != null || !canForceRecheck() ){
				
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
  						try{
   							this_mon.enter();

   							if ( getDiskManager() == null ){
   								
	  	  							// already closed down via stop
   								
	  	  						download_manager.setOnlySeeding(false);
	  	  						
	  	  						return;
  							}
						}finally{
  								
							this_mon.exit();
  	  					}
  							
  	  					if ( newDMState == DiskManager.READY || newDMState == DiskManager.FAULTY ){
  	  						
	  	  					force_start = wasForceStarted;
		  					
	  	  					stats.setDownloadCompleted(stats.getDownloadCompleted(true));
		  					
	  	  					if ( newDMState == DiskManager.READY ){
		  						
	  	  						try{
	  	  							boolean	only_seeding 		= false;
	  	  							boolean	update_only_seeding	= false;
	  	  						
	  	  							try{
	  	  								this_mon.enter();
	  	  							
	  	  								DiskManager	dm = getDiskManager();
	  	  								
	  	  								if ( dm != null ){
	  	  									  					  		
	  	  									dm.stop();
		  							
	  	  									only_seeding	= dm.getRemainingExcludingDND() == 0;
	  	  									
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
  	  							
  	  								DiskManager	dm = getDiskManager();
  	  								
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
  	                	download_manager.informPriorityChange( file );
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
					( state == DownloadManager.STATE_ERROR && getDiskManager() == null )) {
	    
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
						
					  peer_manager.stopAll(); 
					  
					  stats.saveSessionTotals();
					}
					
						// do this even if null as it also triggers tracker actions
					
					download_manager.informStopped( peer_manager );
						
					peer_manager	= null;

					DiskManager	dm = getDiskManager();
					
					if ( dm != null ){
						
						dm.stop();

						stats.setCompleted(stats.getCompleted());
						stats.setDownloadCompleted(stats.getDownloadCompleted(true));
			      
						dm.saveState();

					  		// we don't want to update the torrent if we're seeding
					  
						if ( !download_manager.getOnlySeeding()){
					  	
							download_manager.getDownloadState().save();
						}			  					  
					  							  
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
         
				   		// only update the state if things haven't gone wrong
				   
				   if ( getState() == DownloadManager.STATE_STOPPING ){
					   
					   setState( stateAfterStopping, true );
				   }
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
  
  	public void
  	setStateFinishing()
  	{
  		setState(DownloadManager.STATE_FINISHING, true);
  	}
  
  	public void
  	setStateSeeding(
  		boolean	never_downloaded )
  	{
  		setState(DownloadManager.STATE_SEEDING, true);
  		
		download_manager.downloadEnded( never_downloaded );
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
  		DiskManager	dm = getDiskManager();
   		
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
	
	  					if ( save_dir_file != null && save_dir_file.exists() && save_dir_file.isDirectory()){
		      		
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
	restartDownload()
	{
		boolean	was_force_start = isForceStart();
			    
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
		DiskManager	dm = getDiskManager();
		
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
   		return( files_facade );
   	}
	
	protected void
	fileInfoChanged()
	{
		destroySkeletonFiles();
	}
	
	/**
	 * Determine if the download is complete, excluding DND files.  This
	 * function is mostly cached when there is a DiskManager.
	 * 
	 * @return completion state
	 */
	public boolean
	isDownloadCompleteExcludingDND()
	{
		DiskManager	dm = getDiskManager();
		
		if ( dm != null ){
			
			return( dm.getRemainingExcludingDND() == 0 );
		}
		
		// XXX Please cache me
		for (int i=0;i<files_facade.length;i++){
			
			DiskManagerFileInfo	file = files_facade[i];
			
			if ( file.isSkipped()){
				
				continue;
			}
			
			if ( file.getDownloaded() != file.getLength()){
				
				return( false );
			}
		}
		
		return( true );
	}
	
	protected PEPeerManager
	getPeerManager()
	{
		return( peer_manager );
	}
	
	protected DiskManager
	getDiskManager()
	{
		return( disk_manager_use_accessors );
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
	  		
	  		DiskManager	old_disk_manager = disk_manager_use_accessors;
	  		
	  		disk_manager_use_accessors	= new_disk_manager;

	  			// whether going from none->active or the other way, indicate that the file info
	  			// has changed
	  		
	  		fileInfoChanged();
	  		
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
	  		
	  		DiskManager	dm = getDiskManager();
	  		
			if ( dm != null ){
		
				disk_listeners.dispatch( listener, LDT_DL_ADDED, dm );
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
	
	public String
	getDisplayName()
	{
		return( download_manager.getDisplayName());
	}
	
	public int
	getUploadRateLimitBytesPerSecond()
	{
		return( stats.getUploadRateLimitBytesPerSecond());
	}
	
	public int
	getDownloadRateLimitBytesPerSecond()
	{
		return( stats.getDownloadRateLimitBytesPerSecond());
	}
	
	public int
	getMaxUploads()
	{
		return( stats.getEffectiveMaxUploads());
	}
	
	public int
	getMaxConnections()
	{
		return( download_manager.getMaxConnections());
	}
	
	public int
	getPort()
	{
		TRTrackerAnnouncer	announcer = download_manager.getTrackerClient();
		
		if ( announcer != null ){
			
			return( announcer.getPort());
		}
		
		return(0);
	}
	
	public boolean
	isAZMessagingEnabled()
	{
		return( download_manager.isAZMessagingEnabled());
	}
	
	public boolean
	isPeerExchangeEnabled()
	{
		return( download_manager.getDownloadState().isPeerSourceEnabled( PEPeerSource.PS_OTHER_PEER ));
	}
	
	public boolean
	isPeriodicRescanEnabled()
	{
		return( download_manager.getDownloadState().getFlag( DownloadManagerState.FLAG_SCAN_INCOMPLETE_PIECES ));
	}
	
	public TRTrackerScraperResponse
	getTrackerScrapeResponse()
	{
		return( download_manager.getTrackerScrapeResponse());
	}
	
	public String
	getTrackerClientExtensions()
	{
		return( download_manager.getDownloadState().getTrackerClientExtensions());
	}
	
	public void
	setTrackerRefreshDelayOverrides(
		int	percent )
	{
		download_manager.setTrackerRefreshDelayOverrides( percent );
	}
	
	public boolean
	isNATHealthy()
	{
		return( download_manager.getNATStatus() == ConnectionManager.NAT_OK );
	}
	
	public void
	addPeer(
		PEPeer	peer )
	{
		download_manager.addPeer( peer );
	}
	
	public void
	removePeer(
		PEPeer	peer )
	{	
		download_manager.removePeer( peer );
	}
	
	public void
	addPiece(
		PEPiece	piece )
	{
		download_manager.addPiece( piece );
	}
	
	public void
	removePiece(
		PEPiece	piece )
	{
		download_manager.removePiece( piece );
	}
	
	public void
	discarded(
		int	bytes )
	{
		if ( global_stats != null ){
			
			global_stats.discarded( bytes );
		}
	}
	
	public void
	protocolBytesReceived(
		int	bytes )
	{
		if ( global_stats != null ){
			
			global_stats.protocolBytesReceived( bytes );
		}
	}
	
	public void
	dataBytesReceived(
		int	bytes )
	{
		if ( global_stats != null ){
			
			global_stats.dataBytesReceived( bytes );
		}
	}
	
	public void
	protocolBytesSent(
		int		bytes,
		boolean	LAN )
	{
		if ( global_stats != null ){
			
			global_stats.protocolBytesSent( bytes, LAN );
		}
	}
	
	public void
	dataBytesSent(
		int		bytes,
		boolean	LAN )
	{
		if ( global_stats != null ){
			
			global_stats.dataBytesSent( bytes, LAN );
		}
	}
	
	public LogRelation
	getLogRelation()
	{
		return( this );
	}
	
	public String
	getRelationText() 
	{
		return( download_manager.getRelationText());
	}

	public Object[] 
	getQueryableInterfaces() 
	{
		List	interfaces = new ArrayList();
		
		Object[]	intf = download_manager.getQueryableInterfaces();
		
		for (int i=0;i<intf.length;i++){
			
			interfaces.add( intf[i] );
		}
		
		interfaces.add( download_manager );
		
		DiskManager	dm = getDiskManager();
		
		if ( dm != null ){
			
			interfaces.add( dm );
		}
		
		return( interfaces.toArray());
	}
	
	protected void
	destroySkeletonFiles()
	{
		DiskManagerFileInfo[]	files;
		
		try{
			skeleton_mon.enter();
			
			files = skeleton_files;
			
			skeleton_files = null;
			
		}finally{
			
			skeleton_mon.exit();
		}
		
		if ( files != null ){
			
			for (int i=0;i<files.length;i++){
				
				files[i].close();
			}
		}
	}
	
	protected class
	fileInfoFacade
		implements DiskManagerFileInfo
	{
		private DiskManagerFileInfo		delegate;
		private int						index;
		
		private List					listeners;
		
		protected 
		fileInfoFacade(
			int	_index )
		{
			index	= _index;
		}
		
		protected void
		fixup()
		{
			DiskManagerFileInfo	old_delegate = delegate;
			
	 		DiskManager	dm = DownloadManagerController.this.getDiskManager();

	   		DiskManagerFileInfo[]	res	= null;
	   		
	   		if ( dm != null ){
	   			
	   			int	dm_state = dm.getState();
	   			
	   				// grab the live file info if available
	   			
	   			if ( dm_state == DiskManager.CHECKING || dm_state == DiskManager.READY ){
	   			
	   				destroySkeletonFiles();
	   			
	   				res = dm.getFiles();
	   			}
	   		}
	   		
	   		if ( res == null ){
	   			
   				final List	delayed_prio_changes = new ArrayList();

	   			try{
	   				skeleton_mon.enter();
	   				   			
	   				if ( skeleton_files == null ){
	   					
		   				final boolean[]	initialising = { true };
		   				
		   					// chance of recursion with this listener as the file-priority-changed is triggered
		   					// synchronously during construction and this can cause a listener to reenter the
		   					// incomplete fixup logic here + instantiate new skeletons.....
		   				
		   				skeleton_files = DiskManagerFactory.getFileInfoSkeleton( 
	   							download_manager,
	   							new DiskManagerListener()
	   							{
	   								public void
	   								stateChanged(
	   									int oldState, 
	   									int	newState )
	   								{
	   								}
	   								
	   								public void
	   								filePriorityChanged(
	   									DiskManagerFileInfo		file )
	   								{
	   									if ( initialising[0] ){
	   										
	   										delayed_prio_changes.add( file );
	   										
	   									}else{
	   										
	   										download_manager.informPriorityChange( file );
	   									}
	   								}

	   								public void
	   								pieceDoneChanged(
	   									DiskManagerPiece		piece )
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
	   					   				
		   				initialising[0]	= false;
	   				}
	   				
   					res = skeleton_files;

	   			}finally{
	   				
	   				skeleton_mon.exit();
	   			}
	   			
	   			for (int i=0;i<delayed_prio_changes.size();i++){
	   					
	   				download_manager.informPriorityChange((DiskManagerFileInfo)delayed_prio_changes.get(i));
	   			}
	   				
	   			delayed_prio_changes.clear();
	   		}
	
	   		delegate = res[index];
	   		
	   		if ( delegate != old_delegate && listeners != null ){
	   			
	   			for (int i=0;i<listeners.size();i++){
	   				
	   				delegate.addListener((DiskManagerFileInfoListener)listeners.get(i));
	   			}
	   		}
		}
		
		protected void
		fixupMinimum()
		{
			if ( listeners != null ){
				
				fixup();
			}
		}
		
		public void 
		setPriority(
			boolean b )
		{
			fixup();
			
			delegate.setPriority(b);
		}
		
		public void 
		setSkipped(
			boolean b)
		{
			fixup();
			
			delegate.setSkipped(b);
		}
		 
		
		public boolean
		setLink(
			File	link_destination )
		{
			fixup();
		
			return( delegate.setLink( link_destination ));
		}
		
		public File
		getLink()
		{
			fixup();
			
			return( delegate.getLink());
		}
		
		public boolean
		setStorageType(
			int		type )
		{
			fixup();
			
			return( delegate.setStorageType( type ));
		}
		
		public int
		getStorageType()
		{
			fixup();
			
			return( delegate.getStorageType());
		}
		
		 	
		public int 
		getAccessMode()
		{
			fixup();
			
			return( delegate.getAccessMode());
		}
		
		public long 
		getDownloaded()
		{
			fixup();
			
			return( delegate.getDownloaded());
		}
		
		public String 
		getExtension()
		{
			fixup();
			
			return( delegate.getExtension());
		}
			
		public int 
		getFirstPieceNumber()
		{
			fixup();
			
			return( delegate.getFirstPieceNumber());
		}
	  
		public int 
		getLastPieceNumber()
		{
			fixup();
			
			return( delegate.getLastPieceNumber());
		}
		
		public long 
		getLength()
		{
			fixup();
			
			return( delegate.getLength());
		}
			
		public int 
		getNbPieces()
		{
			fixup();
			
			return( delegate.getNbPieces());
		}
				
		public boolean 
		isPriority()
		{
			fixup();
			
			return( delegate.isPriority());
		}
		
		public boolean 
		isSkipped()
		{
			fixup();
			
			return( delegate.isSkipped());
		}
		
		public int	
		getIndex()
		{
			fixup();
			
			return( delegate.getIndex());
		}
		
		public DiskManager 
		getDiskManager()
		{
			fixup();
			
			return( delegate.getDiskManager());
		}
		
		public DownloadManager	
		getDownloadManager()
		{
			return( download_manager );
		}
		
		public File 
		getFile( boolean follow_link )
		{
			fixup();
			
			return( delegate.getFile( follow_link ));
		}
		
		public TOTorrentFile
		getTorrentFile()
		{
			fixup();
			
			return( delegate.getTorrentFile());
		}
		
		public void
		flushCache()
		
			throws	Exception
		{
			fixup();
			
			delegate.flushCache();
		}
		
		public DirectByteBuffer
		read(
			long	offset,
			int		length )
		
			throws IOException
		{
			fixup();
			
			return( delegate.read( offset, length ));
		}
		
		public void
		close()
		{
			fixup();
			
			delegate.close();
		}
		
		public void
		addListener(
			DiskManagerFileInfoListener	listener )
		{
			fixup();
			
			if ( listeners == null ){
				
				listeners = new ArrayList();
			}
			
			listeners.add( listener );
			
			delegate.addListener( listener );
		}
		
		public void
		removeListener(
			DiskManagerFileInfoListener	listener )
		{
			fixup();
			
			listeners.remove( listener );
			
			delegate.removeListener( listener );
		}
	}
}
