/*
 * File    : DownloadImpl.java
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

package org.gudy.azureus2.pluginsimpl.download;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.core3.global.*;
import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.torrent.*;

import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.pluginsimpl.torrent.TorrentImpl;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadStats;
import org.gudy.azureus2.plugins.download.DownloadException;

public class 
DownloadImpl
	implements Download
{
	protected DownloadManager		download_manager;
	
	protected
	DownloadImpl(
		DownloadManager		_dm )
	{
		download_manager	= _dm;
	}
	
	public int
	getState()
	{
		int	state = download_manager.getState();
		
		switch( state ){
			case DownloadManager.STATE_DOWNLOADING:
			case DownloadManager.STATE_SEEDING:
			{
				return( ST_STARTED );
			}
			default:
			{
				return( ST_STOPPED );
			}
		}
	}
	
	public Torrent
	getTorrent()
	{
		TOTorrent	torrent = download_manager.getTorrent();
		
		if ( torrent == null ){
			
			return( null );
			
		}else{
			
			return( new TorrentImpl( torrent ));
		}
	}

	public void
	start()
	
		throws DownloadException
	{
		if ( download_manager.getState() == DownloadManager.STATE_STOPPED){
			
			download_manager.setState(DownloadManager.STATE_WAITING);
			
		}else{
			
			throw( new DownloadException( "Download::start: download not stopped" ));
		}
	}
	
	public void
	stop()
	
		throws DownloadException
	{
		if ( download_manager.getState() != DownloadManager.STATE_STOPPED){
			
			download_manager.stopIt();
			
		}else{
			
			throw( new DownloadException( "Download::stop: download already stopped" ));
		}
	}
	
	public void
	remove()
	
		throws DownloadException
	{
		if ( download_manager.getState() == DownloadManager.STATE_STOPPED){
			
			GlobalManager globalManager = download_manager.getGlobalManager();
			
			globalManager.removeDownloadManager(download_manager);
			
		}else{
			
			throw( new DownloadException( "Download::remove: download not stopped" ));
		}
	}
	
	public DownloadStats
	getStats()
	{
		return( new DownloadStatsImpl( download_manager ));
	}
}
