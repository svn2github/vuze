/*
 * File    : DownloadStats.java
 * Created : 08-Jan-2004
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

package org.gudy.azureus2.plugins.download;

/**
 * @author parg
 * This class gives access to various stats associated with the download
 */

public interface 
DownloadStats 
{
	/**
	 * Returns an overall string representing the state of the download
	 * @return
	 */
	
	public String
	getStatus();
	
	/**
	 * Gives access to the directory into which the download is being saved
	 * @return
	 */
	
	public String
	getDownloadDirectory();
	
	/**
	 * Gives access to the target file or directory that the download is being saved to
	 * @return
	 */
	
	public String
	getTargetFileOrDir();
	
	/**
	 * returns an general status string for the tracker
	 * @return
	 */
	
	public String
	getTrackerStatus();
	
	/**
	 * returns a value between 0 and 1000 giving the completion status of the current download
	 * task (e.g. checking, downloading)
	 * @return
	 */
	
	public int
	getCompleted();
	
  /** Retrieve the level of download completion.
   * 
   * To understand the bLive parameter, you must know a bit about the
   * Torrent activation process:
   * 1) Torrent goes into ST_WAITING
   * 2) Torrent moves to ST_PREPARING
   * 3) Torrent moves to ST_DOWNLOADING or ST_SEEDING
   *
   * While in ST_PREPARING, Completion Level is rebuilt (either via Fast Resume
   * or via piece checking). Quite often, the download completion level before
   * ST_PREPARING and after ST_PREPARING are identical.
   *
   * Before going into ST_PREPARING, we store the download completion level.
   * If you wish to retrieve this value instead of the live "building" one,
   * pass false for the parameter.
   *
   * @param bLive true - Always returns the known completion level of the torrent
   *               false - In the case of ST_PREPARING, return completion level 
   *                       before of the torrent ST_PREPARING started.  
   *                       Otherwise, same as true.
   * @return 0 - 1000
   */
	public int
	getDownloadCompleted(boolean bLive);

	/**
	 * Gives the number of bytes downloaded
	 * @return
	 */
	
	public long
	getDownloaded();
	
	/**
	 * Gives the number of bytes uploaded
	 * @return
	 */
	
	public long
	getUploaded();

	/**
	 * Gives the number of bytes discarded
	 * @return
	 */
	
	public long
	getDiscarded();
	
	/**
	 * Gives average number of bytes downloaded in last second 
	 * @return
	 */
	
	public long
	getDownloadAverage();
	
	/**
	 * Gives average number of bytes uploaded in last second 
	 * @return
	 */
	
	public long
	getUploadAverage();
	
	/**
	 * Gives average number of bytes computed for torrent in last second 
	 * @return
	 */
	
	public long
	getTotalAverage();
	
	/**
	 * Gives the elapsed download time as a string
	 * @return
	 */
	
	public String
	getElapsedTime();
	
	/**
	 * Gives the estimated time to completion as a string
	 * @return
	 */
	
	public String
	getETA();

	/**
	 * Gives the number of bytes thrown away due to piece hash check fails
	 * @return
	 */
	
	public long
	getHashFails();
	
	/**
	 * Gives the share ratio of the torrent in 1000ths (i.e. 1000 = share ratio of 1)
	 * @return
	 */
	public int
	getShareRatio();

	// in ms
	public long
	getTimeStarted();
	
  /* Time that the torrent started seeding.
   * @return the difference, measured in milliseconds, between the torrent 
   *         started seeding and midnight, January 1, 1970 UTC.  see
   *         System.currentTimeMillis().
   *         -1 is not seeding
   */		
	public long
	getTimeStartedSeeding();

	/**
	 * Gives the currently seen availability of the torrent
	 * @return
	 */
	public float
	getAvailability();


  /* Return the # of seconds that the torrent has been downloading.  This 
   * number is totalled across sessions.
   *
   * @return -1 if it has never downloaded
   */
	public long 
	getSecondsDownloading();

  /* Return the # of seconds that the torrent has been only seeding.  This 
   * number is totalled across sessions, and does not include the time
   * seeding during the download phase.
   *
   * @return -1 if it has never seeded
   */
	public long 
	getSecondsOnlySeeding();
}
