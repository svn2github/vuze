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
	public static final int ST_STOPPED		= 1;
	public static final int ST_STARTED		= 2;
	
	/**
	 * get state from above ST_ set
	 * @return
	 */
	
	public int
	getState();

	public Torrent
	getTorrent();
	
	public void
	start()
	
		throws DownloadException;
	
	public void
	stop()
	
		throws DownloadException;
	
	public void
	remove()
	
		throws DownloadException, DownloadRemovalVetoException;
	
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
