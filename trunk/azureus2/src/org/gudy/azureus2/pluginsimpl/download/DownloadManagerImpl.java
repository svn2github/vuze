/*
 * File    : DownloadManagerImpl.java
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

import java.util.*;
import java.io.File;
import java.net.URL;

import org.gudy.azureus2.plugins.torrent.*;
import org.gudy.azureus2.pluginsimpl.torrent.*;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.download.Download;

import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.download.*;

import org.gudy.azureus2.ui.swt.FileDownloadWindow;
import org.gudy.azureus2.ui.swt.MainWindow;

public class 
DownloadManagerImpl
	implements org.gudy.azureus2.plugins.download.DownloadManager
{
	protected static DownloadManagerImpl	singleton;
	
	public synchronized static DownloadManagerImpl
	getSingleton(
		GlobalManager	global_manager )
	{
		if ( singleton == null ){
			
			singleton = new DownloadManagerImpl( global_manager );
		}
		
		return( singleton );
	}
	
	protected GlobalManager	global_manager;
	
	protected
	DownloadManagerImpl(
		GlobalManager	_global_manager )
	{
		global_manager	= _global_manager;
	}
	
	public void 
	addDownload(
		File fileName ) 
	{
		MainWindow.getWindow().openTorrent(fileName.toString());
	}

	public void 
	addDownload(
		URL	url) 
	{
		new FileDownloadWindow(MainWindow.getWindow().getDisplay(),url.toString());
	}
	
	public Download
	addDownload(
		Torrent		torrent,
		File		torrent_file,
		File		data_location )
	
		throws DownloadException
	{
		DownloadManager dm = global_manager.addDownloadManager(torrent_file.toString(),data_location.toString());
		
		if ( dm == null ){
			
			throw( new DownloadException( "DownloadManager::addDownload - failed"));
		}
		
		return( new DownloadImpl(dm));
	}
	
	public Download
	getDownload(
		Torrent		_torrent )
	{
		TorrentImpl	torrent = (TorrentImpl)_torrent;
		
		List	dls = global_manager.getDownloadManagers();

		for (int i=0;i<dls.size();i++){
			
			DownloadManager	man = (DownloadManager)dls.get(i);
			
				// torrent can be null if download manager torrent file read fails
			
			if ( man.getTorrent() != null ){
				
				if ( man.getTorrent().hasSameHashAs( torrent.getTorrent())){
				
					return( new DownloadImpl( man ));
				}
			}
		}
		
		return( null );
	}
}
