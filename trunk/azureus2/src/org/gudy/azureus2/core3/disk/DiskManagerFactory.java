/*
 * File    : DiskManagerFactory.java
 * Created : 18-Oct-2003
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

package org.gudy.azureus2.core3.disk;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.disk.impl.*;
import org.gudy.azureus2.core3.disk.impl.resume.*;
import org.gudy.azureus2.core3.download.DownloadManager;

public class 
DiskManagerFactory 
{
	public static DiskManager
	create(
		TOTorrent		torrent, 
		DownloadManager manager)
	{
		DiskManagerImpl dm = new DiskManagerImpl( torrent, manager );
		
		dm.start();
		
		return dm;
	}
	
	public static DiskManager
	createNoStart(
		TOTorrent		torrent, 
		DownloadManager manager)
	{
		return( new DiskManagerImpl( torrent, manager ));
	}

		/**
		 * Method to preset resume data to indicate completely valid file. 
		 * Doesn't save the torrent
		 * @param torrent
		 * @param path
		 */
	
	public static void
	setResumeDataCompletelyValid(
		TOTorrent	torrent,
		String		data_location )
	{
		RDResumeHandler.setTorrentResumeDataComplete( torrent, data_location );
	}

	public static boolean
	isTorrentResumeDataComplete(
		TOTorrent	torrent,
		String		torrent_save_dir,
		String		torrent_save_file)
	{
		return RDResumeHandler.isTorrentResumeDataComplete( torrent, torrent_save_dir,torrent_save_file );
	}

	public static void 
	deleteDataFiles(
		TOTorrent 	torrent, 
		String		torrent_save_dir,
		String		torrent_save_file ) 
	{
	  DiskManagerImpl.deleteDataFiles(torrent, torrent_save_dir, torrent_save_file );
	}
}
