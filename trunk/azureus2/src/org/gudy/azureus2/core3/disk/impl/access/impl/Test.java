/*
 * Created on 08-Aug-2004
 * Created by Paul Gardner
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.core3.disk.impl.access.impl;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import org.gudy.azureus2.core3.category.Category;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.download.impl.DownloadManagerDefaultPaths;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.peer.impl.PEPeerTransport;
import org.gudy.azureus2.core3.peer.util.PeerIdentityDataID;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.download.DownloadAnnounceResult;
import org.gudy.azureus2.plugins.download.DownloadScrapeResult;
import org.gudy.azureus2.plugins.peers.PeerDescriptor;

import com.aelitis.azureus.core.diskmanager.cache.CacheFileManagerFactory;
import com.aelitis.azureus.core.diskmanager.cache.CacheFileManagerStats;
import com.aelitis.azureus.core.networkmanager.LimitedRateGroup;
import com.aelitis.azureus.core.peermanager.peerdb.PeerExchangerItem;
import com.aelitis.azureus.core.peermanager.piecepicker.PiecePicker;


/**
 * @author parg
 * @author MjrTom
 *			2005/Oct/08: pieceAdded => addPiece, getAvgAvail
 *
 */

public class 
Test 
{
	final int		CACHE_SIZE		= 16;
	final boolean	CACHE_ENABLE	= true;
	
	final int 		BLOCK_SIZE		= 16*1024;
	final boolean	READ_TEST		= true;
	final boolean	RANDOM_TEST		= false;
	final boolean	CHECK_WRITES	= false;

	protected
	Test()
	{
		try{						
			COConfigurationManager.initialiseFromMap( new HashMap() );
			
			COConfigurationManager.setParameter( "diskmanager.perf.cache.enable", CACHE_ENABLE );
			
			COConfigurationManager.setParameter( "diskmanager.perf.cache.size", CACHE_SIZE );

			if ( false ){
				
				System.setProperty("azureus.log.stdout","1");
				
				Logger.addListener(new ILogEventListener() {
					public void log(LogEvent event) {
						System.out.println(event.text);
					}
				});
				
			}
			
			File	file = new File( "C:\\temp\\dmtest.dat" );

			file.deleteOnExit();
			
			RandomAccessFile raf = new RandomAccessFile( file, "rwd" );
			
			raf.setLength( 100*1024*1024 );

			raf.close();
			
			TOTorrentCreator creator = TOTorrentFactory.createFromFileOrDirWithComputedPieceLength( file, new URL("http://dummy/" ));
			
			final TOTorrent	torrent = creator.create();
			
			File	torrent_file = new File( "C:\\temp\\dmtest.torrent" );
			
			TorrentUtils.writeToFile( torrent, torrent_file );
			
			DownloadManagerState	download_manager_state = DownloadManagerStateFactory.getDownloadState( torrent ); 
				
			TorrentUtils.setResumeDataCompletelyValid( download_manager_state );
			
			download_manager_state.save();
			
			PEPeerManagerDummy	pm = new PEPeerManagerDummy();
			
			final DiskManager dm = DiskManagerFactory.create( torrent, new DownloadManagerDummy( file, pm ));
					
			pm.setDiskManager( dm );
			
			dm.addListener(
				new DiskManagerListener()
				{
					public void
					stateChanged(
						int oldState, 
						int	newState )
					{
						System.out.println( "dm state change: " + newState );
						
						if ( newState != DiskManager.READY ){
							
							return;
						}

						try{
							final CacheFileManagerStats	cache_stats = CacheFileManagerFactory.getSingleton().getStats();
														
							if ( !READ_TEST ){
								
								DiskManagerPiece[]	x = dm.getPieces();
								
								for (int i=0;i<x.length;i++){
									
									x[i].setDone( false );
								}
							}
							
							int	pieces  	= torrent.getNumberOfPieces();
							int	piece_size	= (int)torrent.getPieceLength();
							
							int	blocks_per_piece	= piece_size / BLOCK_SIZE;
							
							final 	int[]	done = {0};
							final	long[]	bytes = {0};
							
							final long	start = System.currentTimeMillis();
							
							for (int i=0;i<100000;i++){
								
								for (int j=0;j<blocks_per_piece;j++){
									
									int	piece_number;
									int	block_number;
									
									if ( RANDOM_TEST ){
										
										piece_number	= (int)(Math.random() * pieces);
										block_number	= (int)(Math.random() * blocks_per_piece);
									}else{
										
										piece_number 	= i%pieces;
										block_number	= j;
									}
									
									if ( READ_TEST ){
										
										DiskManagerReadRequest	res = dm.createReadRequest( piece_number, BLOCK_SIZE*block_number, BLOCK_SIZE );
										
										dm.enqueueReadRequest(
											res,
											new DiskManagerReadRequestListener()
											{
												public void 
												readCompleted( 
													DiskManagerReadRequest 	request, 
													DirectByteBuffer 	data )
												{
												//	System.out.println( "request complete" );
													
													data.returnToPool();
													
													done[0]++;
													
													bytes[0] += BLOCK_SIZE;
													
													if ( done[0] %1000 == 0 ){
														
														long	now = System.currentTimeMillis();
		
														float	bps = (float)(bytes[0]*1000 / (now - start ))/1024;
														
														System.out.println( "done = " + done[0] + ", bps = " + bps );
														
														System.out.println( "    " + cache_stats.getBytesReadFromCache() + "/" + cache_stats.getBytesReadFromFile());
													}
												}
												
												  public void 
												  readFailed( 
												  		DiskManagerReadRequest 	request, 
														Throwable		 		cause )
												  {
													  Debug.out( cause );
												  }
											});	
									}else{
										
										DirectByteBuffer b = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_OTHER, BLOCK_SIZE );
										
										dm.enqueueWriteRequest( 
												dm.createWriteRequest( piece_number, BLOCK_SIZE*block_number, b, null ),
												null );
									}
								}
								
								if ( i %1000 == 0 ){
										
									System.out.println( i );
									
									System.out.println( "    " + cache_stats.getBytesWrittenToCache() + "/" + cache_stats.getBytesWrittenToFile() +
											": av = " + cache_stats.getAverageBytesWrittenToCache() + "/" + cache_stats.getAverageBytesWrittenToFile());

								}
							}
						}catch( Throwable e ){
						
							Debug.printStackTrace( e );
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
			
	
			
			Thread.sleep( 10000000 );
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
	}
	public static void
	main(
		String[]	args )
	{
		new Test();
	}

	protected class
	DownloadManagerDummy
		implements DownloadManager
	{
		protected File				file;
		protected PEPeerManager		peer_manager;
		protected
		DownloadManagerDummy(
			File			_file,
			PEPeerManager	_pm )
		{
			file				= _file;
			peer_manager		= _pm;
		}
		
		public void
		initialize()
		{
			
		}
		
		public void
		destroy( boolean is_duplicate )
		{
		}
		
		public void
		generateEvidence(
			IndentWriter		writer )
		{
			
		}

		public int
		getState()
		{
			return(0);
		}
		
		public int
		getSubState()
		{
			return( 0 );
		}
		
		public void
		setStateWaiting()
		{
			
		}
		
		public void
		setStateDownloading()
		{
			
		}
		
		public void
		setStateStopped()
		{
			
		}
		
		public void
		setStateFinishing()
		{
			
		}
		
		public void
		setStateSeeding()
		{
			
		}
		
		public void
		setStateQueued()
		{
			
		}
		
		public void downloadRemoved() {}
		
		public DownloadManagerState 
		getDownloadState()
		{
			return( null );
		}
		
		public void
		startDownload()
		{
			
		}
		
		public void
		startDownloadInitialized(
			boolean		initStoppedDownloads )
		{
			
		}
		
		public boolean
		seedPieceRecheck()
		{
			return( false );
		}
		
    public void stopIt(final int _stateAfterStopping, final boolean remove_torrent, final boolean remove_data ) {
      
    }
    
	public boolean
	pause()
	{
		return( false );
	}
	
	public boolean
	isPaused()
	{
		return( false );
	}
	
	public void
	resume()
	{
	}
    
    public boolean isAZMessagingEnabled() { return true;  }

    public void setAZMessagingEnabled( boolean enable ){}
    

		public GlobalManager
		getGlobalManager()
		{
			return( null );
			
		}
		
		public DiskManager
		getDiskManager()
		{
			return( null );
		}
		
		public DiskManagerFileInfo[]
       	getDiskManagerFileInfo()
		{
			return( null );
		}

		public PEPeerManager
		getPeerManager()
		{
			return( peer_manager );
			
		}
		public void
		setAnnounceResult(
			DownloadAnnounceResult	result )
		{
		}
		
		public void
		setScrapeResult(
			DownloadScrapeResult	result )
		{
		}
		
		public void
		addListener(
				DownloadManagerListener	listener )
		{
			
		}
		
		public void
		removeListener(
				DownloadManagerListener	listener )
		{
			
		}
		
		public void
		addTrackerListener(
			DownloadManagerTrackerListener	listener )
		{
			
		}
		
		public void
		removeTrackerListener(
			DownloadManagerTrackerListener	listener )
		{
			
		}
		public void
		addDiskListener(
			DownloadManagerDiskListener	listener )
		{
		}
			
		public void
		removeDiskListener(
			DownloadManagerDiskListener	listener )
		{
		}
		public void addActivationListener(DownloadManagerActivationListener listener) {
			// TODO Auto-generated method stub
			
		}
		public void removeActivationListener(DownloadManagerActivationListener listener) {
			// TODO Auto-generated method stub
			
		}
		
		public int getActivationCount() {
			// TODO Auto-generated method stub
			return 0;
		}
		public void
		addPeerListener(
			DownloadManagerPeerListener	listener )
		{
			
		}
			
		public void
		removePeerListener(
			DownloadManagerPeerListener	listener )
		{
			
		}
			
		public void
		addPeer(
			PEPeer 		peer )
		{
			
		}
			
		public void
		removePeer(
			PEPeer		peer )
		{
			
		}
			
		public void
		addPiece(
			PEPiece 	piece )
		{
			
		}
			
		public void
		removePiece(
			PEPiece		piece )
		{
			
		}
			
		public TOTorrent
		getTorrent()
		{
			return( null );
			
		}
		
		public TRTrackerAnnouncer
		getTrackerClient()
		{
			return( null );
			
		}
		
		public void
		requestTrackerAnnounce(
			boolean now )
		{
			
		}
		
		public void
		requestTrackerScrape(
			boolean now )
		{
			
		}
		
		public TRTrackerScraperResponse
		getTrackerScrapeResponse()
		{
			return( null );
			
		}
		
		public void
		setTrackerScrapeResponse(
			TRTrackerScraperResponse	response )
		{
			
		}
		
		public String
		getDisplayName()
		{
			return( null );
			
		}
	  
		public String
		getInternalName()
		{
			return( null );
			
		}
		public long
		getSize()
		{
			return( 9 );
			
		}
		
		public String
		getTorrentFileName()
		{
			return( null );
			
		}
	  
	    public void 
		setTorrentFileName(String string)
		{
			
		}
		
		public File
		getSaveLocation()
		{
			return( file );
			
		}
		
		public File
		getAbsoluteSaveLocation()
		{
			return( file );
			
		}
	 	public void 
		setTorrentSaveDir(
			String sPath )
		{
			
		}
		
		public int
		getPriority()
		{
			return( 9 );
			
		}
	  
	  	public boolean 
		isForceStart()
		{
			return( false );
		
		}
	  
	  	public void setForceStart(boolean forceStart)
		{
			
		}
	  
	  	public boolean
	  	isPersistent()
		{
			return( false );
		
		}
	  	
	 	public boolean
		isDownloadComplete()
		{
			return( false );
			
		}
		public boolean
		isDownloadComplete(boolean b)
		{
			return( false );
		}
		
		public String
		getTrackerStatus()
		{
			return( null );
			
		}
		
		public int
		getTrackerTime()
		{
			return( 0 );
			
		}
		
		public String
		getTorrentComment()
		{
			return( null );
			
		}
		
		public String
		getTorrentCreatedBy()
		{
			return( null );
			
		}
		
		public long
		getTorrentCreationDate()
		{
			return( 0 );
			
		}
		
		public int
		getNbPieces()
		{
			return( 0 );
			
		}
		
		public String
		getPieceLength()
		{
			return( null );
			
		}
		
		public int
		getNbSeeds()
		{
			return( 0 );
			
		}
		
		public int
		getNbPeers()
		{
			return( 0 );
			
		}
		
		public boolean 
		filesExist()
		{
			return( false );
			
		}
		
		public String
		getErrorDetails()
		{
			return( null );
		}
		
		public void
		setErrorDetail(
			String	str )
		{
			
		}
					
			// what are these doing here?
			
		public int
		getIndex()
		{
			return( 0 );
			
		}
		
		public boolean
		isMoveableDown()
		{
			return( false );
			
		}
		
		public boolean
		isMoveableUp()
		{
			return( false );
			
		}
		
		public void
		moveDown()
		{
			
		}
		
		public void
		moveUp()
		{
			
		}
		
		public DownloadManagerStats
		getStats()
		{
			return( null );
			
		}
	  
		public int
		getPosition()
		{
			return( 0 );
			
		}
		
		public void
		setPosition(
			int		newPosition )
		{
			
		}

		public boolean
		getAssumedComplete()
		{
			return( false );
			
		}
		
		public void
		setOnlySeeding(boolean onlySeeding)
		{
			
		}

		public void
	   restartDownload(boolean use_fast_resume)
		{
			
		}

		public int getPrevState()
		{
			return( 0 );
			
		}

		public void setPrevState(int state)
		{
			
		}
	  
	    public DiskManager initializeDiskManager()
		{
			return( null );
			
		}
	  
	    public boolean canForceRecheck()
		{
			return( false );
			
		}

	    public void forceRecheck()
		{
			
		}

	    public void
	    resetFile(
	    	DiskManagerFileInfo		file )
	    {
	    }
	    
	    public void
	    recheckFile(
	    	DiskManagerFileInfo		file )
	    {	
	    }
	    
		public int getHealthStatus()
		{
			return( 0 );
			
		}
		public int
		getNATStatus()
		{
			return( 0 );
		}
		
		public Category getCategory()
		{
			return( null );
			
		}
		
		public void setCategory(Category cat)
		{
			
		}
		
		public void deleteDataFiles()
		{
			
		}
		

		
		public void
		mergeTorrentDetails(
		  	DownloadManager	other_manager )
		{	
		}
		  
		
		public void
		saveResumeData()		
		{
			
		}

		public void
		saveDownload()
		{
		}
		 
		public void
		moveDataFiles(
			File	new_parent_dir )
		{
		}
		
		public void renameDownload(String name){}
		
		 
		public void
		moveTorrentFile(
			File	new_parent_dir )
		{
		}
		
		public File[] calculateDefaultPaths(boolean for_moving) {
			  return null;
		}
		
	  public Object getData (String key)
	  {
		  return( null );	
	  }
	 
	  public void setData (String key, Object value)
		{
		
	}
	  
	  
	
	  public boolean isDataAlreadyAllocated()
		{
		return( false );
		
	}
	  
	
	  public void setDataAlreadyAllocated( boolean already_allocated )
		{
		
	}
	  
	
	  public long
	  getCreationTime()
		{
		return( 0 );
		
	}

	  public void
	  setCreationTime(
	  	long		t )
		{
		
	}

		public void setSeedingRank(int rank) {
		
			
		}

		public int getSeedingRank() {
			
			return 0;
		}

		public void addPeerListener(DownloadManagerPeerListener listener, boolean bDispatchForExisting) {
			
			
		}

		public PEPiece[] getCurrentPieces() {
		
			return null;
		}

		public PEPeer[] getCurrentPeers() {
		
			return null;
		}

		public int getEffectiveMaxUploads() {
			// TODO Auto-generated method stub
			return 0;
		}
		
		public int getMaxUploads() {
			// TODO Auto-generated method stub
			return 0;
		}
		
		public void setMaxUploads(int max_slots) {
			// TODO Auto-generated method stub
			
		}
		
		public int getEffectiveUploadRateLimitBytesPerSecond() {
		
			return 0;
		}

		public boolean requestAssumedCompleteMode() {
			// TODO Auto-generated method stub
			return false;
		}
	}
	
	protected class
	PEPeerManagerDummy
		implements PEPeerManager
	{
		protected DiskManager	dm;
		
		protected void
		setDiskManager(
			DiskManager	_dm )
		{
			dm	= _dm;
		}
		
		
	 	public DiskManager
		getDiskManager()
	 	{
	 		return( null );
	 	}
		public void
		generateEvidence(
			IndentWriter		writer )
		{
		}
		
	 	public PiecePicker getPiecePicker()
	 	{
	 		return null;
	 	}
	 	
	 	public String
	 	getDisplayName()
	 	{
	 		return( null );
	 	}
    public LimitedRateGroup getUploadLimitedRateGroup() {  return null;  }
    public LimitedRateGroup getDownloadLimitedRateGroup() {  return null;  }
    
		public void
		start()
		{
			
		}
			
    public PeerExchangerItem createPeerExchangeConnection( PEPeerTransport base_peer ) {  return null;  }
    
    public void peerVerifiedAsSelf( PEPeerTransport self ){}
    
		public void
		stopAll()
		{
			
		}
    
		  public boolean
		  seedPieceRecheck()
		  {
			  return( false );
		  }
		  
    public void peerConnectionClosed( PEPeerTransport peer, boolean connect_failed ) {
      
    }
    
    public boolean isInEndGameMode() {  return false;  }
    
			
    public int getAverageCompletionInThousandNotation(){  return 0;  }
    
		public byte[]
		getHash()
		{
			return( null );
		}
		
		public PeerIdentityDataID
		getPeerIdentityDataID()
		{
			return( null );
		}

		public byte[]
		getPeerId()
		{
			return( null );
		}
		
		public void 
		blockWritten(
			int pieceNumber, 
			int offset,
			PEPeer peer)
		{
			if ( !RANDOM_TEST ){
				
				if ( CHECK_WRITES ){
					
					if ( offset == 0 ){
						
						dm.enqueueCheckRequest( dm.createCheckRequest( pieceNumber, null ), null );
					}
				}
			}
	
		}
		
		public void 
		asyncPieceChecked(
			int pieceNumber, 
			boolean result )
		{
			dm.getPiece(pieceNumber).setDone( false );
		}

		public int[] getAvailability()
		{
			return( null );
		}

		public int calcAvailability(int pieceNumber)
		{
			return( 0 );
		}
		
		public int getAvailability(int pieceNumber)
		{
			return( 0 );
		}
		
	    public float getMinAvailability()
		{
	    	return( 0 );
		}


        public boolean isPieceActive(int pieceNumber)
        {
            return false;
        }

		public PEPiece getPiece(int pieceNumber)
		{
			return null;
		}

		public boolean
		hasDownloadablePiece()
		{
			return( false );
		}
		public PEPiece[] getPieces()
		{
			return( null );
		}


		public PEPeerManagerStats
		getStats()
		{
			return( null );
		}


		public void
		processTrackerResponse(
			TRTrackerAnnouncerResponse	response )
		{
			
		}

			
		public int getNbPeers()
		{
			return( 0 );
		}


		public int getNbSeeds()
		{
			return( 0 );
		}

		public int getNbHashFails()
		{
			return( 0 );
		}

	  
		public void setNbHashFails(int fails)
		{
			
		}


		public int getPiecesNumber()
		{
			return( 0 );
		}


	  public int getPieceLength(int pieceNumber)
		{
	  	return( 0 );
	}

			
		public long getRemaining()
		{
			return( 0 );
		}


		public int getDownloadPriority()
		{
			return( 0 );
		}


		public long getETA()
		{
			return( 0 );
		}


		public String getElapsedTime()
		{
			return( null );
		}

		
		// Time Started in ms
		public long getTimeStarted()
		{
			return( 0 );
		}


		public long getTimeStartedSeeding()
		{
			return( 0 );
		}

		
		public void
		addListener(
			PEPeerManagerListener	l )
		{
			
		}

			
		public void
		removeListener(
			PEPeerManagerListener	l )
		{
			
		}

	  
	  public boolean needsMD5CheckOnCompletion(int pieceNumber)
		{
	  	return( false );
	}

	  public boolean
	  isSeeding()
	  {
		  return( false );
	  }
	  
	  public boolean
	  isSuperSeedMode()
		{
	  	return( false );
	}

	  
	  public int getNbRemoteConnections()
		{
	  	return( 0 );
	}

	  public long getLastRemoteConnectionTime()
	  {
		  return(0);
	  }
	  
	  public int getMaxNewConnectionsAllowed()
		{
	  	return( 0 );
	}
		public void
		dataBytesReceived(
			int		l )
		{
			
		}

		
		public void
		dataBytesSent(
			int		l, boolean LAN )
		{
			
		}

    public void protocolBytesSent( int length, boolean LAN ) {}
    
    public void protocolBytesReceived( int length ) {}
		
		public void
		discarded(
			int		l )
		{
			
		}

		
		public PEPeerStats
		createPeerStats()
		{
			return( null );
		}

		
		
		public List
		getPeers()
		{
			return( null );
		}

		public List
		getPeers(
			String	address )
		{
			return( null );
		}
		
		public PeerDescriptor[]
		getPendingPeers(
			String	address )
		{
			return( null );
		}
		
		public void
		addPeer(
			PEPeer	peer )
		{			
		}

		public void 
		addPeer( 
			String ip_address, 
			int port, 
			int udp_port, 
			boolean use_crypto ) 
		{
		}
		
		public void
		removePeer(
			PEPeer	peer )
		{	
		}
		
		public void
		removePeer(
			PEPeer	peer,
			String	reason )
		{	
		}
		
		public void 
		peerAdded(PEPeer pc)
		{	
		}

		public void 
		peerRemoved(PEPeer pc)
		{
			
		}

		
		public DiskManagerReadRequest
		createDiskManagerRequest(
		   int pieceNumber,
		   int offset,
		   int length )
		{
			return( null );	
		}

		
		public void
		requestCanceled(
			DiskManagerReadRequest	item )
		{
			
		}

			
		public boolean 
		validatePieceReply(
			int 		pieceNumber, 
			int 		offset, 
			DirectByteBuffer 	data )
		{
			return( false );
		}

		
		public void 
		writeBlock(
			int 		pieceNumber, 
			int 		offset, 
			DirectByteBuffer 	data,
			PEPeer 		sender,
            boolean     cancel)
		{
			
		}

	  
//	  public void writeBlockAndCancelOutstanding(int pieceNumber, int offset, DirectByteBuffer data,PEPeer sender)
//		{
//		
//	}

	  
	  public boolean isWritten( int piece_number, int offset )
		{
		return( false );
		}
		 public Object getData (String key){return( null );}
		  /** To store arbitrary objects against this object. */
		  public void setData (String key, Object value){}

	public void addPiece(PEPiece piece, int pieceNumber)
	{
	}


	public float getAvgAvail()
	{
		return 0;
	}

	  
	public PEPeerTransport getTransportFromIdentity( byte[] peer_id ) { return null;  }
	  
	public PEPeerTransport getTransportFromAddress(String peer) { return null;  }
	}
	/*
	protected class
	DiskManagerDummy
		implements DiskManagerHelper
	{
		public void
		start()
		{
		}
		
		public boolean
		filesExist()
		{
			
		}

		public void 
		setPeerManager(
			PEPeerManager manager )
		{
			
		}


		public void 
		writeBlock(
			int 		pieceNumber, 
			int 		offset, 
			DirectByteBuffer 	data,
			PEPeer sender)
		{
			
		}


		public boolean 
		checkBlock(
			int 		pieceNumber, 
			int 		offset, 
			DirectByteBuffer 	data )
		{
			
		}


		public boolean 
		checkBlock(
			int pieceNumber, 
			int offset, 
			int length )
		{
			
		}

			
		public DiskManagerRequest
		createRequest(
			int pieceNumber,
			int offset,
			int length );
		

		public void enqueueReadRequest( DiskManagerRequest request, ReadRequestListener listener );

		public void
		stopIt();

		public void
	    dumpResumeDataToDisk(
	    	boolean savePartialPieces, 
			boolean force_recheck )
			
			throws Exception;

		public void
		computePriorityIndicator();
		
		public PEPiece[] 
		getRecoveredPieces();

		public void
		aSyncCheckPiece(
			int	pieceNumber );
	  
		public int 
		getPieceNumberToDownload(BitFlags candidatePieces);
		
		public boolean[] 
		getPiecesDone();

		public int 
		getNumberOfPieces();

		public DiskManagerFileInfo[]
		getFiles();

		public int
		getState();
		
		public long
		getTotalLength();
		
		public int
		getPieceLength();
		
		public int 
		getLastPieceLength();

		public long
		getRemaining();
		
		public int
		getPercentDone();
		
		public String
		getErrorMessage();
	  
		public String
		moveCompletedFiles();

		public boolean isChecking();
	  
	   
		public void
		addListener(
			DiskManagerListener	l );
		
		public void
		removeListener(
			DiskManagerListener	l );
	  
	  
	  public void storeFilePriorities();
	  
	  public DownloadManager getDownloadManager();
	  
	  public PEPeerManager getPeerManager();
		
		
		public PieceList
		getPieceList(
			int	piece_number )
		{
			return(null);
		}
		
		public byte[]
		getPieceHash(
			int	piece_number )
		{
			return(null);
		}
		
		public void
		setState(
			int	state )
		{
		}
		
		public void
		setErrorMessage(
			String	str )
		{
		}
		
		public long
		getAllocated()
		{
			return(0);
			
		}
		
		public void
		setAllocated(
			long		num )
		{
		}

		
		public void
		setRemaining(
			long		num )
		{
		}

		
		public void
		setPercentDone(
			int			num )
		{
		}

		
		public void 
		computeFilesDone(
			int pieceNumber )
		{
		}

		
		public TOTorrent
		getTorrent()
		{
			return(null);
		}

	}
*/
}
