/*
 * File    : Download.java
 * Created : 06-Jan-2004
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
 *
 */

import org.gudy.azureus2.plugins.torrent.Torrent;

public interface 
Download 
{
	public static final int ST_WAITING			= 1;	// waiting to be told to start preparing
	public static final int ST_PREPARING		= 2;	// getting files ready (allocating/checking)
	public static final int ST_READY			= 3;	// ready to be started if required
	public static final int ST_DOWNLOADING		= 4;	// downloading
	public static final int ST_SEEDING			= 5;	// seeding
	public static final int ST_STOPPING			= 6;	// stopping
	public static final int ST_STOPPED			= 7;	// stopped, don't auto-start!
	public static final int ST_ERROR			= 8;	// failed
	public static final int ST_QUEUED			= 9;	// stopped, but ready for auto-starting
	
  /* A download's lifecycle:
   * torrent gets added
   *    state -> QUEUED
   * slot becomes available, queued torrent is picked, "restart" executed
   *    state -> WAITING
   * state moves through PREPARING to READY
   *    state -> PREPARING
   *    state -> READY
   * execute "start" method
   *    state -> SEEDING -or- DOWNLOADING
   * if torrent is DOWNLOADING, and completes, state changes to SEEDING
   *
   * Path 1                                | Path 2
   * execute "stop" method                 | startstop rules are met, execute "stopandQueue"
   *    state -> STOPPING                  |     state -> STOPPING
   *    state -> STOPPED                   |     state -> STOPPED
   *                                       |     state -> QUEUED
   *
   * execute "remove" method -> deletes the download
   * a "stop" method call can be made when the download is in all states except STOPPED
   */
	
	public static final int	PR_HIGH_PRIORITY	= 1;
	public static final int	PR_LOW_PRIORITY		= 2;
	
	
	/**
	 * get state from above ST_ set
	 * @return
	 */
	
	public int
	getState();

	/**
	 * When the download state is ERROR this method returns the error details
	 * @return
	 */
	
	public String
	getErrorStateDetails();
	
	/**
	 * Downloads are kept in priority order - this method gives teh download's current position 
	 * @return	index - 0 based
	 */
	
	public int
	getIndex();
	
	/**
	 * Each download has a corresponding torrent
	 * @return	the download's torrent
	 */
	
	public Torrent
	getTorrent();
	
	/**
	 * See lifecylce description above 
	 * @throws DownloadException
	 */
	
	public void
	initialize()
	
		throws DownloadException;

	/**
	 * See lifecylce description above 
	 * @throws DownloadException
	 */
	
	
	public void
	start()
	
		throws DownloadException;
	
	/**
	 * See lifecylce description above 
	 * @throws DownloadException
	 */
	
	public void
	stop()
	
		throws DownloadException;
	
	/**
	 * See lifecylce description above 
	 * @throws DownloadException
	 */
	
	
	public void
	stopAndQueue()
	
		throws DownloadException;

	public void
	restart()
	
		throws DownloadException;
	
	
	/**
	 * When a download is "start-stop locked" it means that seeding rules shouldn't start or
	 * stop the download as it is under manual control
	 * @return
	 */
	
	public boolean
	isStartStopLocked();
	
	public boolean
	isForceStart();
	
	public void
	setForceStart(boolean forceStart);
	
	/**
	 * Downloads can either be low or high priority (see PR_ constants above)
	 * @return the download's priority
	 */
	
	public int
	getPriority();
	
	/**
	 * This method sets a download's priority
	 * @param priority the required priority, see PR_ constants above
	 */
	
	public void
	setPriority(
		int		priority );
	
	/**
	 * When a download's priority is locked this means that seeding rules should not change
	 * a downloads priority, it is under manual control
	 * @return whether it is locked or not
	 * @obsolete
	 */
	
	public boolean
	isPriorityLocked();
	
	/**
	 * Returns the name of the torrent.  Similar to Torrent.getName() and is usefull
	 * if getTorrent() returns null and you still need the name.
	 */
	public String 
	getName();
	
	/**
	 * Removes a download. The download must be stopped or in error. Removal may fail if another 
	 * component does not want the removal to occur - in this case a "veto" exception is thrown
	 * @throws DownloadException
	 * @throws DownloadRemovalVetoException
	 */
	
	public void
	remove()
	
		throws DownloadException, DownloadRemovalVetoException;
	
	/**
	 * Returns the current position in the queue
	 * Completed and Incompleted downloads have seperate position sets.  This means
	 * we can have a position x for Completed, and position x for Incompleted.
	 */
	public int
	getPosition();
	
	/**
	 * Sets the position in the queue
	 * Completed and Incompleted downloads have seperate position sets
	 */
	public void
	setPosition(
		int newPosition);

	/**
	 * Tests whether or not a download can be removed. Due to synchronization issues it is possible
	 * for a download to report OK here but still fail removal.
	 * @return
	 * @throws DownloadRemovalVetoException
	 */
	
	public boolean
	canBeRemoved()
	
		throws DownloadRemovalVetoException;
	
	/**
	 * Gives access to the last announce result received from the tracker for the download
	 * @return
	 */
	
	public DownloadAnnounceResult
	getLastAnnounceResult();
	
	/**
	 * Gives access to the last scrape result received from the tracker for the download
	 * @return
	 */
	
	public DownloadScrapeResult
	getLastScrapeResult();
	
	/**
	 * Gives access to the download's statistics
	 * @return
	 */
	
	public DownloadStats
	getStats();
	
	/**
	 * Adds a listener to the download that will be informed of changes in the download's state
	 * @param l
	 */
	
	public void
	addListener(
		DownloadListener	l );
	
	/**
	 * Removes listeners added above
	 * @param l
	 */
	public void
	removeListener(
		DownloadListener	l );

	/**
	 * Adds a listener that will be informed when the latest announce/scrape results change
	 * @param l
	 */
	
	public void
	addTrackerListener(
		DownloadTrackerListener	l );
	
	/**
	 * Removes listeners added above
	 * @param l
	 */
	
	public void
	removeTrackerListener(
		DownloadTrackerListener	l );
	
	/**
	 * Adds a listener that will be informed when a download is about to be removed. This gives
	 * the implementor the opportunity to veto the removal
	 * @param l
	 */
	
	public void
	addDownloadWillBeRemovedListener(
		DownloadWillBeRemovedListener	l );
	
	/**
	 * Removes the listener added above
	 * @param l
	 */
	
	public void
	removeDownloadWillBeRemovedListener(
		DownloadWillBeRemovedListener	l );
}
