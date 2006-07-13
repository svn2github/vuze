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

import java.io.File;

import org.gudy.azureus2.core3.global.*;
import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.plugins.download.DownloadAnnounceResult;
import org.gudy.azureus2.plugins.download.DownloadScrapeResult;

public interface
DownloadManager
{
    public static final int STATE_START_OF_DAY  = -1;   // should never actually see this one

    public static final int STATE_WAITING       = 0;
    public static final int STATE_INITIALIZING  = 5;
    public static final int STATE_INITIALIZED   = 10;

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



    public static final int WEALTH_STOPPED          = 1;
    public static final int WEALTH_NO_TRACKER       = 2;
    public static final int WEALTH_NO_REMOTE        = 3;
    public static final int WEALTH_OK               = 4;
    public static final int WEALTH_KO               = 5;
    public static final int WEALTH_ERROR          = 6;

    public void
    initialize();

    public int
    getState();

        /**
         * For stopping this returns the target state after stopping (stopped/queued)
         * @return
         */

    public int
    getSubState();

    public void
    setStateWaiting();

    public void
    setStateQueued();

    public void
    startDownload();

    public boolean canForceRecheck();

    public void forceRecheck();

        /**
         * Reset the file download state to totally undownloaded. Download must be stopped
         * @param file
         */

    public void
    resetFile(
        DiskManagerFileInfo     file );

        /**
         * Recheck a particular file. Download must be stopped
         * @param file
         */

    public void
    recheckFile(
        DiskManagerFileInfo     file );

  /**
   * Stop the download manager, and do any file/torrent removals.
   * @param _stateAfterStopping
   * @param remove_torrent remove the .torrent file after stopping
   * @param remove_data remove the data file after stopping
   */

    public void
    stopIt(
        int     stateAfterStopping,
        boolean remove_torrent,
        boolean remove_data );

    public boolean
    pause();

    public boolean
    isPaused();

    public void
    resume();

    public GlobalManager
    getGlobalManager();

    public DiskManager
    getDiskManager();

    public DiskManagerFileInfo[]
    getDiskManagerFileInfo();

    public PEPeerManager
    getPeerManager();

    public DownloadManagerState
    getDownloadState();

    public TOTorrent
    getTorrent();

    public TRTrackerAnnouncer
    getTrackerClient();

    public void
    requestTrackerAnnounce(
        boolean     immediate );

    public void
    requestTrackerScrape(
        boolean     immediate );

    public TRTrackerScraperResponse
    getTrackerScrapeResponse();

    public void
    setTrackerScrapeResponse(
        TRTrackerScraperResponse    response );

    public String
    getDisplayName();

        /**
         * returns a name based on the torrent hash or an empty string if torrent invalid
         * @return
         */

    public String
    getInternalName();

    public long
    getSize();

    public String
    getTorrentFileName();

    public void
    setTorrentFileName(String string);

    public File
    getAbsoluteSaveLocation();

    public File
    getSaveLocation();

        /**
         * changes the save directory. Only call this if you know what you are doing!!!!
         * @param sPath
         */

    public void
    setTorrentSaveDir(
        String sPath );

    public boolean isForceStart();

    public void setForceStart(boolean forceStart);

    public boolean
    isPersistent();

    /**
     * Retrieves whether the download is complete
     * 
     * @param bIncludingDND true- include files marked as Do Not Download.<BR>
     *                       false- don't include files marked DND.<p>
     *                       If there are DND files and you choose to include
     *                       DND in the calculation, false will always be 
     *                       returned.
     * @return whether download is complete
     */
    public boolean
    isDownloadComplete(boolean bIncludingDND);

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

    public int
    getNbSeeds();

    public int
    getNbPeers();

    /**
     * Checks if all the files the user wants to download from this torrent
     * actually exist on their filesystem.
     * <p>
     * If a file does not exist, the download will be set to error state.
     * 
     * @return Whether all the non-skipped (non-DND) files exist
     */
    public boolean
    filesExist();

    public String
    getErrorDetails();

    public DownloadManagerStats
    getStats();

    public int
    getPosition();

    public void
    setPosition(
        int     newPosition );

  	/**
  	 * Retrieve whether this download is assumed complete.
  	 * <p>
  	 * Assumed complete status is kept while the torrent is in a non-running 
  	 * state, even if it has no data.  
  	 * <p>
  	 * When the torrent starts up, the real complete
  	 * level will be checked, and if the torrent
  	 * actually does have missing data, the download will be thrown
  	 * into error state.
  	 * <p>
  	 * Only a forced-recheck should clear this flag.
  	 * 
  	 * @see {@link #requestAssumedCompleteMode()}
  	 */
    public boolean
    getAssumedComplete();

    /**
     * Will set this download to be "assumed complete" for if the download
     * is already complete (excluding DND)
     * 
     * @return true- success; false- failure, download not complete
     */
    public boolean
    requestAssumedCompleteMode();

    /**
     * @return the wealthy status of this download
     */
    public int getHealthStatus();

    /**
     * See plugin ConnectionManager.NAT_ constants for return values
     * @return
     */

     public int
     getNATStatus();

        /**
         * persist resume data
         *
         */

    public void
    saveResumeData();

        /**
         * persist any general download related information, excluding resume data which is
         * managed separately by saveResumeData
         */

    public void
    saveDownload();

      /** To retreive arbitrary objects against this object. */
    public Object getData (String key);
      /** To store arbitrary objects against this object. */
    public void setData (String key, Object value);


      /**
       * Determine whether disk allocation has already been done.
       * Used for checking if data is missing on a previously-loaded torrent.
       * @return true if data files have already been allocated
       */
    public boolean isDataAlreadyAllocated();

      /**
       * Set whether data allocation has already been done, so we know
       * when to allocate and when to throw a missing-data error message.
       * @param already_allocated
       */

    public void setDataAlreadyAllocated( boolean already_allocated );


    public void setSeedingRank(int rank);

    public int getSeedingRank();

    public void setMaxUploads( int max_slots );
    
    public int getMaxUploads();
    
	/**
	 * Returns the max uploads depending on whether the download is seeding and it has a separate
	 * rate for this
	 * @return
	 */

	public int
	getEffectiveMaxUploads();

        /**
         * returns the currently in force upload speed limit which may vary from the stats. value
         * as this gives the fixed per-torrent limit
         * @return
         */

    public int
    getEffectiveUploadRateLimitBytesPerSecond();

        /**
         * Move data files to new location. Torrent must be in stopped/error state
         * @param new_parent_dir
         * @return
         */

    public void
    moveDataFiles(
        File    new_parent_dir )

        throws DownloadManagerException;

        /**
         * Move torrent file to new location. Download must be stopped/error
         * @param new_parent_dir
         * @return
         */

    public void
    moveTorrentFile(
        File    new_parent_dir )

        throws DownloadManagerException;

        /**
         * gives the time this download was created (not the torrent but the download itself)
         * @return
         */

    public long
    getCreationTime();

    public void
    setCreationTime(
            long        t );

    public void
    setAnnounceResult(
        DownloadAnnounceResult  result );

    public void
    setScrapeResult(
        DownloadScrapeResult    result );

  /**
   * Is advanced AZ messaging enabled for this download.
   * @return true if enabled, false if disabled
   */
    public boolean isAZMessagingEnabled();

  /**
   * Enable or disable advanced AZ messaging for this download.
   * @param enable true to enabled, false to disabled
   */
    public void setAZMessagingEnabled( boolean enable );

        /**
         * Indicates that the download manager is no longer needed
         *
         */

    public void
    destroy();


    public PEPiece[]
    getCurrentPieces();

    public PEPeer[]
    getCurrentPeers();

    public void
    addListener(
            DownloadManagerListener listener );

    public void
    removeListener(
            DownloadManagerListener listener );

    public void
    addTrackerListener(
        DownloadManagerTrackerListener  listener );

    public void
    removeTrackerListener(
        DownloadManagerTrackerListener  listener );

    public void
    addPeerListener(
        DownloadManagerPeerListener listener );

    public void
    addPeerListener(
        DownloadManagerPeerListener listener,
        boolean bDispatchForExisting );

    public void
    removePeerListener(
        DownloadManagerPeerListener listener );

    public void
    addDiskListener(
        DownloadManagerDiskListener listener );

    public void
    removeDiskListener(
        DownloadManagerDiskListener listener );

    public void
    addActivationListener(
    	DownloadManagerActivationListener listener );

    public void
    removeActivationListener(
    	DownloadManagerActivationListener listener );

    
    public void
    generateEvidence(
        IndentWriter        writer );
    
    public void downloadRemoved();

}