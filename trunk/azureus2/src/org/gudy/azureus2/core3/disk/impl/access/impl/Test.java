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
import org.gudy.azureus2.core3.disk.DiskManagerRequest;

import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.global.GlobalManager;

import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.tracker.client.TRTrackerClient;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.diskmanager.*;

/**
 * @author parg
 *
 */

public class 
Test 
{
	protected
	Test()
	{
		try{						
			COConfigurationManager.initialiseFromMap( new HashMap() );
			
			File	file = new File( "C:\\temp\\dmtest.dat" );

			file.deleteOnExit();
			
			RandomAccessFile raf = new RandomAccessFile( file, "rwd" );
			
			raf.setLength( 1*1024*1024 );

			raf.close();
			
			final TOTorrent	torrent = TOTorrentFactory.createFromFileOrDirWithComputedPieceLength( file, new URL("http://dummy/" ));
			
			File	torrent_file = new File( "C:\\temp\\dmtest.torrent" );
			
			TorrentUtils.writeToFile( torrent, torrent_file );
			
			final DiskManager dm = DiskManagerFactory.create( torrent, new DownloadManagerDummy( file ));
			
			dm.addListener(
				new DiskManagerListener()
				{
					public void
					stateChanged(
						int oldState, 
						int	newState )
					{
						System.out.println( "dm state change: " + newState );
						
						int	pieces  = torrent.getPieces().length;
						
						if ( newState == DiskManager.READY ){
								
							final int[]	done = {0};
							
							for (int i=0;i<10000;i++){
								
								DiskManagerRequest	res = dm.createRequest( i%pieces, 0, 10 );
								
								dm.enqueueReadRequest(
									res,
									new ReadRequestListener()
									{
										public void 
										readCompleted( 
											DiskManagerRequest 	request, 
											DirectByteBuffer 	data )
										{
										//	System.out.println( "request complete" );
											
											data.returnToPool();
											
											done[0]++;
											
											if ( done[0] %1000 == 0 ){
												
												System.out.println( "done = " + done[0]);
											}
										}
									});	
								
								if ( i %1000 == 0 ){
									
									System.out.println( i );
								}
							}
						}
					}
				});
			
	
			
			Thread.sleep( 10000000 );
			
		}catch( Throwable e ){
			
			e.printStackTrace();
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
		protected File		file;
		
		protected
		DownloadManagerDummy(
			File		_file )
		{
			file	= _file;
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
			return( null );
			
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
		
		public void
		setPriority(
			int		priority )
		{
			
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
			boolean invalidate )
			
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
