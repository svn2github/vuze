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
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.download.DownloadRemovalVetoException;

import org.gudy.azureus2.core3.global.*;
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
	protected List			listeners		= new ArrayList();
	protected List			downloads		= new ArrayList();
	protected Map			download_map	= new HashMap();
	
	protected
	DownloadManagerImpl(
		GlobalManager	_global_manager )
	{
		global_manager	= _global_manager;
		
		global_manager.addListener(
			new GlobalManagerListener()
			{
				public void
				downloadManagerAdded(
					DownloadManager	dm )
				{
					addDownloadManager( dm );
				}
				
				public void
				downloadManagerRemoved(
					DownloadManager	dm )
				{
					synchronized( listeners ){
						
						DownloadImpl	dl = (DownloadImpl)download_map.get( dm );
						
						if ( dl == null ){
							
							System.out.println( "DownloadManager:unknown manager removed");
							
						}else{
						
							downloads.remove( dl );
							
							download_map.remove( dm );
							
							dl.destroy();
							
							for (int i=0;i<listeners.size();i++){
								
								((DownloadManagerListener)listeners.get(i)).downloadRemoved( dl );
							}
						}
					}
				}
				
				public void
				destroyInitiated()
				{
				}				
				
				public void
				destroyed()
				{	
				}
			});
		
		global_manager.addDownloadWillBeRemovedListener(
			new GlobalManagerDownloadWillBeRemovedListener()
			{
				public void
				downloadWillBeRemoved(
					DownloadManager	dm )
				
					throws GlobalManagerDownloadRemovalVetoException
				{					
					DownloadImpl	download = (DownloadImpl)download_map.get( dm );
				
					if ( download != null ){
					
						try{ 
							download.isRemovable();
							
						}catch( DownloadRemovalVetoException e ){
													
							throw( new GlobalManagerDownloadRemovalVetoException( e.getMessage()));
						}		
					}
				}
			});
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
	
	protected void
	addDownloadManager(
		DownloadManager	dm )
	{
		synchronized( listeners ){
			
			if ( download_map.get(dm) == null ){
	
				DownloadImpl	dl = new DownloadImpl(dm);
				
				downloads.add( dl );
				
				download_map.put( dm, dl );
				
				for (int i=0;i<listeners.size();i++){
					
					((DownloadManagerListener)listeners.get(i)).downloadAdded( dl );
				}
			}
		}
	}
	
	public Download
	addDownload(
		Torrent		torrent,
		File		torrent_file,
		File		data_location )
	
		throws DownloadException
	{
		DownloadManager dm = global_manager.addDownloadManager(torrent_file.toString(),data_location.toString(), false, false );
		
		if ( dm == null ){
			
			throw( new DownloadException( "DownloadManager::addDownload - failed"));
		}
		
		addDownloadManager( dm );
		
		return( getDownload( dm ));
	}

	public Download
	addNonPersistentDownload(
		Torrent		torrent,
		File		torrent_file,
		File		data_location )
	
		throws DownloadException
	{
		DownloadManager dm = global_manager.addDownloadManager(torrent_file.toString(),data_location.toString(), false, false );
		
		if ( dm == null ){
			
			throw( new DownloadException( "DownloadManager::addDownload - failed"));
		}
		
		addDownloadManager( dm );
		
		return( getDownload( dm ));
	}
	
	protected Download
	getDownload(
		DownloadManager	dm )
	
		throws DownloadException
	{
		Download dl = (Download)download_map.get(dm);
		
		if ( dl == null ){
			
			throw( new DownloadException("DownloadManager::getDownload: download not found"));
		}
		
		return( dl );
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
	
	public Download[]
	getDownloads()
	{
		synchronized( listeners ){
			
			Download[]	res = new Download[downloads.size()];
			
			downloads.toArray( res );
			
			return( res );
		}
	}
	
	public void
	addListener(
		DownloadManagerListener	l )
	{
		synchronized( listeners ){
			
			listeners.add( l );
			
			for (int i=0;i<downloads.size();i++){
				
				try{
						
					l.downloadAdded((Download)downloads.get(i));
					
				}catch( Throwable e ){
					
					e.printStackTrace();
				}
			}
		}
	}
	
	public void
	removeListener(
		DownloadManagerListener	l )
	{
		synchronized( listeners ){
			
			listeners.remove(l);
		}
	}
}
