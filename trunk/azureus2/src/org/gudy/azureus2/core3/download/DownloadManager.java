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
import org.gudy.azureus2.core3.category.Category;

public interface
DownloadManager
{
	public static final int STATE_WAITING = 0;
	public static final int STATE_INITIALIZING = 5;
	public static final int STATE_INITIALIZED = 10;
	public static final int STATE_ALLOCATING = 20;
	public static final int STATE_CHECKING = 30;
	// Ready: Resources allocated
	public static final int STATE_READY = 40;
	public static final int STATE_DOWNLOADING = 50;
	public static final int STATE_FINISHING = 55;
	public static final int STATE_SEEDING = 60;
	public static final int STATE_STOPPING = 65;
	// Stopped: can't be automatically started
	public static final int STATE_STOPPED = 70;
	// Queued: Same as stopped, except can be automatically started
	public static final int STATE_QUEUED = 75;
	public static final int STATE_ERROR = 100;

	public static final int LOW_PRIORITY = 1;
	public static final int HIGH_PRIORITY = 2;
	
	public static final int WEALTH_STOPPED    = 1;
	public static final int WEALTH_NO_TRACKER = 2;
	public static final int WEALTH_NO_REMOTE  = 3;
	public static final int WEALTH_OK  				= 4;
	public static final int WEALTH_KO 				= 5;

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
	
    public void 
    stopIt(final int stateAfterStopping);

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
	addTrackerListener(
		DownloadManagerTrackerListener	listener );
	
	public void
	removeTrackerListener(
		DownloadManagerTrackerListener	listener );
	
	public void
	addPeerListener(
		DownloadManagerPeerListener	listener );
		
	public void
	removePeerListener(
		DownloadManagerPeerListener	listener );
		
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
	
	public void
	setTrackerScrapeResponse(
		TRTrackerScraperResponse	response );
	
	public String
	getName();
  
    public String getFullName();
	
	public long
	getSize();
	
	public String
	getTorrentFileName();
  
    public void 
	setTorrentFileName(String string);
	
	public String
	getSavePath();

  public boolean 
  setSavePath(String sPath);
	
	public int
	getPriority();
	
	public void
	setPriority(
		int		priority );
  
  	public boolean isForceStart();
  
  	public void setForceStart(boolean forceStart);
  
  	public boolean
  	isPersistent();
  	
	public String
	getTrackerStatus();
	
	public int
	getTrackerTime();
	
	public String
	getTorrentComment();
	
	public String
	getTorrentCreatedBy();
	
	public long
	getTorrentCreationDate();
	
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
	
	public boolean 
	filesExist();
	
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
  
	public int
	getPosition();
	
	public void
	setPosition(
		int		newPosition );

	public boolean
	getOnlySeeding();
	
	public void
	setOnlySeeding(boolean onlySeeding);

	public void
   restartDownload(boolean use_fast_resume);

	public int getPrevState();

	public void setPrevState(int state);
  

  /**
   * Is called when a download is finished.
   * Activates alerts for the user.
   *
   * @author Rene Leonhardt
   */
    public void downloadEnded();

    public void initializeDiskManager();
  
    public boolean canForceRecheck();

    public void forceRecheck();

	/**
	 * @return the wealthy status of this download
	 */
	public int getHealthStatus();
	
	public Category getCategory();
	
	public void setCategory(Category cat);
	
	public void deleteDataFiles();
	
		/**
		 * merges the details of the torrent into the current one (e.g. announce url
		 * and cached peers)
		 * @param other_torrent
		 */
	
	  public void
	  mergeTorrentDetails(
	  	DownloadManager	other_manager );
	  
		/**
		 * persist resume data
		 *
		 */
	
	public void
	saveResumeData();

  /** To retreive arbitrary objects against a peer. */
  public Object getData (String key);
  /** To store arbitrary objects against a peer. */
  public void setData (String key, Object value);
}