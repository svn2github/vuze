/*
 * Created on 08-Aug-2004
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.core3.disk.impl.access.impl;

import java.io.*;
import java.net.*;
import java.util.*;

import org.gudy.azureus2.core3.category.Category;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerFactory;
import org.gudy.azureus2.core3.disk.DiskManagerReadRequest;
import org.gudy.azureus2.core3.disk.DiskManagerReadRequestListener;

import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.global.GlobalManager;

import org.gudy.azureus2.core3.logging.ILoggerListener;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPeerManagerListener;
import org.gudy.azureus2.core3.peer.PEPeerManagerStats;
import org.gudy.azureus2.core3.peer.PEPeerStats;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.peer.impl.PEPeerTransport;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.tracker.client.TRTrackerClient;
import org.gudy.azureus2.core3.tracker.client.TRTrackerResponse;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.diskmanager.cache.*;
import com.aelitis.azureus.core.peermanager.LimitedRateGroup;


/**
 * @author parg
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
				
				LGLogger.initialise();
				
				LGLogger.setListener(
					new ILoggerListener()
					{
						public void log(int componentId,int event,int color,String text)
						{
							System.out.println( text );
						}
					});
			}else{
				
				LGLogger.initialise();
				
			}
			
			File	file = new File( "C:\\temp\\dmtest.dat" );

			file.deleteOnExit();
			
			RandomAccessFile raf = new RandomAccessFile( file, "rwd" );
			
			raf.setLength( 100*1024*1024 );

			raf.close();
			
			final TOTorrent	torrent = TOTorrentFactory.createFromFileOrDirWithComputedPieceLength( file, new URL("http://dummy/" ));
			
			File	torrent_file = new File( "C:\\temp\\dmtest.torrent" );
			
			TorrentUtils.setResumeDataCompletelyValid( torrent, "C:\\temp" );
			
			TorrentUtils.writeToFile( torrent, torrent_file );
			
			
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
							
							int	pieces  	= torrent.getPieces().length;
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
											});	
									}else{
										
										DirectByteBuffer b = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_OTHER, BLOCK_SIZE );
										
										dm.enqueueWriteRequest( piece_number, BLOCK_SIZE*block_number, b, null, null );	
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
		
		public int
		getState()
		{
			return(0);
		}
		
		public void
		setState(
			int		state )
		{
			
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
			
		public void
		stopIt()
		{
			
		}
		
	    public void 
	    stopIt(final int stateAfterStopping)
		{
			
		}

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
		
		public PEPeerManager
		getPeerManager()
		{
			return( peer_manager );
			
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
		
		public TRTrackerClient
		getTrackerClient()
		{
			return( null );
			
		}
		
		public void
		checkTracker()
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
		
		public String
		getTorrentSaveDirAndFile()
		{
			return( null );
			
		}
		
		public String
		getTorrentSaveDir()
		{
			return( file.getParent() );
			
		}
		
		public String
		getTorrentSaveFile()
		{
			return( file.getName() );
			
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
		
		public boolean[]
		getPiecesStatus()
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
		getOnlySeeding()
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
	  

	    public void downloadEnded()
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

		public int getHealthStatus()
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
		
		public int
		getState()
		{
			return( 0 );
		}
		
	 	public DownloadManager
		getDownloadManager()
	 	{
	 		return( null );
	 	}
	 
    public LimitedRateGroup getUploadLimitedRateGroup() {  return null;  }
    
    
		public void
		start()
		{
			
		}
			
		public void
		stopAll()
		{
			
		}
    
    public void peerConnectionClosed( PEPeerTransport peer, boolean reconnect ) {
      
    }
    
    public boolean isInEndGameMode() {  return false;  }
    
			
		public byte[]
		getHash()
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
						
						dm.enqueueCheckRequest( pieceNumber, null, null );
					}
				}
			}
	
		}
		
		public void 
		asyncPieceChecked(
			int pieceNumber, 
			boolean result )
		{
			dm.getPieces()[pieceNumber].setDone( false );
		}

		public int[] getAvailability()
		{
			return( null );
		}

		public int getAvailability(int pieceNumber)
		{
			return( 0 );
		}
		
	    public float getMinAvailability()
		{
	    	return( 0 );
		}


		public boolean[] getPiecesStatus()
		{
			return( null );
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
			TRTrackerResponse	response )
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

	  
	  public void pieceAdded(PEPiece piece)
		{
		
	}

	  
	  public boolean needsMD5CheckOnCompletion(int pieceNumber)
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

	  
		public void
		received(
			int		l )
		{
			
		}

		
		public void
		sent(
			int		l )
		{
			
		}

    public void protocol_sent( int length ) {}
		
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

		
		public void
		addPeer(
			PEPeer	peer )
		{
			
		}

		
		public void
		removePeer(
			PEPeer	peer )
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
		checkBlock(
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
			PEPeer 		sender)
		{
			
		}

	  
	  public void writeBlockAndCancelOutstanding(int pieceNumber, int offset, DirectByteBuffer data,PEPeer sender)
		{
		
	}

	  
	  public boolean isBlockAlreadyWritten( int piece_number, int offset )
		{
		return( false );
	}

	  
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
		getPieceNumberToDownload(
			boolean[] 	_piecesRarest );
		
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
