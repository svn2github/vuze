/*
 * File    : DownloadManager.java
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

import java.io.File;
import java.net.URL;

import org.gudy.azureus2.plugins.torrent.Torrent;

public interface 
DownloadManager 
{
	/**
	 * add a torrent from a file. This will prompt the user for download location etc. if required
	 * This is an async operation so no Download returned
	 * @param torrent_file
	 * @throws DownloadException
	 */
	
	public void 
	addDownload(
		File 	torrent_file )
	
		throws DownloadException;
	
	/**
	 * add a torrent from a URL. This will prompt the user for download location etc. if required
	 * This is an async operation so no Download returned
	 * @param url
	 * @throws DownloadException
	 */
	
	public void 
	addDownload(
		URL		url )
	
		throws DownloadException;
	
	/**
	 * add a torrent from a "Torrent" object and point it at the data location 
	 * @param torrent
	 * @param torrent_location
	 * @param data_location
	 * @return
	 */
	
	public Download
	addDownload(
			Torrent		torrent,
			File		torrent_location,
			File		data_location )
	
	throws DownloadException;
	
	public Download
	addNonPersistentDownload(
		Torrent		torrent,
		File		torrent_location,
		File		data_location )
	
		throws DownloadException;
	
	public Download
	getDownload(
		Torrent		torrent );
	
	public void
	addListener(
		DownloadManagerListener	l );
	
	public void
	removeListener(
		DownloadManagerListener	l );
}
