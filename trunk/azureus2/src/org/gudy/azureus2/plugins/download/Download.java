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
	public static final int ST_STOPPED			= 7;	// stopped
	public static final int ST_ERROR			= 8;	// failed
	
		/* A download's lifecycle:
		 * initial state = WAITING
		 *   execute "initialise" method
		 * state moves through PREPARING to READY
		 *   execute "start" method
		 * state moves to SEEDING or DOWNLOADING
		 *   execute "stop" method
		 * state moves to STOPPING to STOPPED
		 *   execute "restart" method -> WAITING
		 *   execute "remove" method -> deletes the download
		 * 
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

	public String
	getErrorStateDetails();
	
	public Torrent
	getTorrent();
	
	public void
	initialize()
	
		throws DownloadException;
	
	public void
	start()
	
	throws DownloadException;
	
	public void
	stop()
	
		throws DownloadException;
	
	public void
	restart()
	
		throws DownloadException;
	
	public boolean
	isStartStopLocked();
	
	public int
	getPriority();
	
	public void
	setPriority(
		int		priority );
	
	public boolean
	isPriorityLocked();
	
	public void
	remove()
	
		throws DownloadException, DownloadRemovalVetoException;
	
	public DownloadAnnounceResult
	getLastAnnounceResult();
	
	public DownloadScrapeResult
	getLastScrapeResult();
	
	public DownloadStats
	getStats();
	
	public void
	addListener(
		DownloadListener	l );
	
	public void
	removeListener(
		DownloadListener	l );

	public void
	addTrackerListener(
		DownloadTrackerListener	l );
	
	public void
	removeTrackerListener(
		DownloadTrackerListener	l );
	
	public void
	addDownloadWillBeRemovedListener(
		DownloadWillBeRemovedListener	l );
	
	public void
	removeDownloadWillBeRemovedListener(
		DownloadWillBeRemovedListener	l );
}
