/*
 * File    : DownloadManager.java
 * Created : 19-Oct-2003
 * By      : parg
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
 
package org.gudy.azureus2.core3.download;

import org.gudy.azureus2.core3.global.*;
import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.tracker.client.*;

public interface
DownloadManager
{
	public static final int STATE_WAITING = 0;
	public static final int STATE_INITIALIZING = 5;
	public static final int STATE_INITIALIZED = 10;
	public static final int STATE_ALLOCATING = 20;
	public static final int STATE_CHECKING = 30;
	public static final int STATE_READY = 40;
	public static final int STATE_DOWNLOADING = 50;
	public static final int STATE_FINISHING = 55;
	public static final int STATE_SEEDING = 60;
	public static final int STATE_STOPPING = 65;
	public static final int STATE_STOPPED = 70;
	public static final int STATE_ERROR = 100;
	// indicates, that there is already a DownloadManager with the same size and hash in the list
	public static final int STATE_DUPLICATE = 200;

	public static final int LOW_PRIORITY = 1;
	public static final int HIGH_PRIORITY = 2;

	public void
	initialize();
	
	public int
	getState();
	
	public void
	setState(
		int		state );
		
	public void
	startDownload();
	
	public void
	startDownloadInitialized(
		boolean		initStoppedDownloads );
		
	public void
	stopIt();
	
	public GlobalManager
	getGlobalManager();
	
	public DiskManager
	getDiskManager();
	
	public PEPeerManager
	getPeerManager();
	
	public void
	addListener(
		DownloadManagerListener	listener );
		
	public void
	removeListener(
		DownloadManagerListener	listener );
		
	public void
	addPeer(
		PEPeer 		peer );
		
	public void
	removePeer(
		PEPeer		peer );
		
	public void
	addPiece(
		PEPiece 	piece );
		
	public void
	removePiece(
		PEPiece		piece );
		
	public TOTorrent
	getTorrent();
	
	public TRTrackerClient
	getTrackerClient();
	
	public void
	checkTracker();
	
	public TRTrackerScraperResponse
	getTrackerScrapeResponse();
	
	public String
	getName();
  
   public String getFullName();
	
	public long
	getSize();
	
	public String
	getTorrentFileName();
  
   public void setTorrentFileName(String string);
	
	public String
	getSavePath();
	
	public int
	getPriority();
	
	public void
	setPriority(
		int		priority );
  
  public boolean isPriorityLocked();
  
  public void setPriorityLocked(boolean locked);

  public boolean isStartStopLocked();
  
  public void setStartStopLocked(boolean locked);
  
	public String
	getTrackerStatus();
	
	public int
	getTrackerTime();
	
	public String
	getTorrentComment();
	
	public String
	getTorrentCreatedBy();
	
	public int
	getNbPieces();
	
	public String
	getPieceLength();
	
	boolean[]
	getPiecesStatus();
	
	public int
	getNbSeeds();
	
	public int
	getNbPeers();
	
	
	public String
	getErrorDetails();
	
	public void
	setErrorDetail(
		String	str );
				
		// what are these doing here?
		
	public int
	getIndex();
	
	public boolean
	isMoveableDown();
	
	public boolean
	isMoveableUp();
	
	public void
	moveDown();
	
	public void
	moveUp();
	
	public DownloadManagerStats
	getStats();
  
	public void
   restartDownload();

	public int getPrevState();

	public void setPrevState(int state);
  
}