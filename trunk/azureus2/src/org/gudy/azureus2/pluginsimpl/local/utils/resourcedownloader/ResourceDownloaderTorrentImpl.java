/*
 * Created on 21-May-2004
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader;

/**
 * @author parg
 *
 */

import java.io.*;

import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;
import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.pluginsimpl.local.torrent.*;
import org.gudy.azureus2.pluginsimpl.local.*;

public class 
ResourceDownloaderTorrentImpl 	
	extends 	ResourceDownloaderBaseImpl
	implements	ResourceDownloaderListener
{
	public static final int	MAX_FOLLOWS = 1;
	
	protected ResourceDownloaderBaseImpl		delegate;
	protected boolean							persistent;
	
	protected long						size	= -2;
	
	protected TOTorrent					torrent;
	
	protected DownloadManager			download_manager;
	protected Download					download;
	
	protected boolean					cancelled;
	protected ResourceDownloader		current_downloader;
	protected Object					result;
	protected Semaphore					done_sem	= new Semaphore();
			
	public
	ResourceDownloaderTorrentImpl(
		ResourceDownloaderBaseImpl	_parent,
		ResourceDownloader			_delegate,
		boolean						_persistent )
	{
		super( _parent );
		
		persistent		= _persistent;
		delegate		= (ResourceDownloaderBaseImpl)_delegate;
		
		delegate.setParent( this );
		
		download_manager = PluginInitializer.getDefaultInterface().getDownloadManager();
	}
	
	public String
	getName()
	{
		return( delegate.getName() + ": torrent" );
	}
	
	public long
	getSize()
	
		throws ResourceDownloaderException
	{	
		if ( size == -2 ){
			
			try{
				size = getSizeSupport();
				
			}finally{
				
				if ( size == -2 ){
					
					size = -1;
				}
			}
		}
		
		return( size );
	}
	
	protected long
	getSizeSupport()
	
		throws ResourceDownloaderException
	{
		try{
			if ( torrent == null ){
				
				ResourceDownloader	x = delegate.getClone( this );
			
				addReportListener( x );
			
				torrent = TOTorrentFactory.deserialiseFromBEncodedInputStream( x.download());
				
				if( !torrent.isSimpleTorrent()){
					
					throw( new ResourceDownloaderException( "Only simple torrents supported" ));
				}
			}
			
			return( torrent.getSize());
			
		}catch( TOTorrentException e ){
			
			throw( new ResourceDownloaderException( "Torrent deserialisation failed", e ));
		}
	}	
	
	protected void
	setSizeAndTorrent(
		long		_size,
		TOTorrent	_torrent )
	{
		size	= _size;
		torrent	= _torrent;
	}
	
	public ResourceDownloader
	getClone(
		ResourceDownloaderBaseImpl	parent )
	{
		ResourceDownloaderTorrentImpl c = new ResourceDownloaderTorrentImpl( parent, delegate.getClone( this ), persistent);
		
		c.setSizeAndTorrent( size, torrent );
		
		return( c );
	}
	
	public InputStream
	download()
	
		throws ResourceDownloaderException
	{
		asyncDownload();
		
		done_sem.reserve();
		
		if ( result instanceof InputStream ){
			
			return((InputStream)result);
		}
		
		throw((ResourceDownloaderException)result);
	}
	
	public synchronized void
	asyncDownload()
	{
		if ( cancelled ){
			
			done_sem.release();
			
			informFailed((ResourceDownloaderException)result);
			
		}else{

			if ( torrent == null ){
				
				current_downloader = delegate.getClone( this );
				
				informActivity( getLogIndent() + "Downloading: " + getName());
	
				current_downloader.addListener( this );
				
				current_downloader.asyncDownload();
				
			}else{
				
				downloadTorrent();
			}
		}
	}
	
	protected void
	downloadTorrent()
	{
		try{
			informActivity( getLogIndent() + "Downloading: " + new String( torrent.getName(), Constants.DEFAULT_ENCODING ));
			
			final File	temp_file 	= File.createTempFile("AZU", null );
			final File	temp_dir	= temp_file.getParentFile();
			
			torrent.serialiseToBEncodedFile( temp_file );
						
			if ( persistent ){
				
				download = download_manager.addDownload( new TorrentImpl(torrent), temp_file, temp_dir );
				
			}else{
				
				download = download_manager.addNonPersistentDownload( new TorrentImpl(torrent), temp_file, temp_dir );
			}
			
			download.setPosition(1);
			
			download.setPriority( Download.PR_HIGH_PRIORITY );
			
			download.setForceStart( true );
			
			download_manager.addListener(
				new DownloadManagerListener()
				{
					public void
					downloadAdded(
						Download	download )
					{					
					}
					
					public void
					downloadRemoved(
						Download	_download )
					{
						if ( download == _download ){
							
							ResourceDownloaderTorrentImpl.this.downloadRemoved( temp_file, temp_dir );
						}
					}
				});
			
			/*
			download.addDownloadWillBeRemovedListener(
					new DownloadWillBeRemovedListener()
					{
						public void
						downloadWillBeRemoved(
							Download	dl )
						
							throws DownloadRemovalVetoException
						{
							if ( dl != download_being_removed ){
								
								throw( new DownloadRemovalVetoException(
											MessageText.getString("plugin.sharing.download.remove.veto")));
							}
						}
					});
			*/
			
			download.addListener(
				new DownloadListener()
				{
					public void
					stateChanged(
						Download		download,
						int				old_state,
						int				new_state )
					{		
						// System.out.println( "state change:" + old_state + "->" + new_state );
						
						if ( new_state == Download.ST_SEEDING ){
							
							downloadSucceeded( temp_file, temp_dir );
						}
					}

					public void
					positionChanged(
						Download	download, 
						int oldPosition,
						int newPosition )
					{
					}
				});
				
			Thread	t = 
				new Thread( "RDTorrent percentage checker")
				{
					public void
					run()
					{
						int	last_percentage = 0;
						
						while( result == null ){
														
							int	this_percentage = download.getStats().getCompleted()/10;
							
							long	total	= torrent.getSize();
														
							if ( this_percentage != last_percentage ){
								
								reportPercentComplete( ResourceDownloaderTorrentImpl.this, this_percentage );
							
								last_percentage = this_percentage;
							}
							
							try{
								Thread.sleep(1000);
								
							}catch( Throwable e ){
								
								e.printStackTrace();
							}
						}
					}
				};
				
			t.setDaemon( true );
			
			t.start();
			
				// its possible that the d/l has already occurred and it is seeding!
			
			if ( download.getState() == Download.ST_SEEDING ){
				
				downloadSucceeded( temp_file, temp_dir );
			}
		}catch( Throwable e ){
			
			failed( this, new ResourceDownloaderException( "Torrent download failed", e ));
		}
	}
	
	protected void
	downloadSucceeded(
		File		torrent_file,
		File		data_dir )
	{
		reportActivity( "Torrent download complete" );
		
			// assumption is that this is a SIMPLE torrent
		
		try{
			InputStream	data = 
				new FileInputStream( new File( data_dir, 
												new String(torrent.getFiles()[0].getPathComponents()[0])));
			
			informComplete( data );
				
			result	= data;
				
			done_sem.release();
			
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			failed( this, new ResourceDownloaderException( "Failed to read torrent data", e ));
		}
	}
	
	protected void
	downloadRemoved(
		File		torrent_file,
		File		data_dir )
	{
		reportActivity( "Torrent removed" );

		if (!( result instanceof InputStream )){
			
			failed( this, new ResourceDownloaderException( "Download did not complete" ));
		}
	}
	
	public synchronized void
	cancel()
	{
		result	= new ResourceDownloaderException( "Download cancelled");
		
		cancelled	= true;
		
		informFailed((ResourceDownloaderException)result );
		
		done_sem.release();
		
		if ( current_downloader != null ){
			
			current_downloader.cancel();
		}
	}	
	
	public boolean
	completed(
		ResourceDownloader	downloader,
		InputStream			data )
	{
		try{			
			torrent = TOTorrentFactory.deserialiseFromBEncodedInputStream( data );
			
			if( torrent.isSimpleTorrent()){
				
				downloadTorrent();
				
			}else{
				
				failed( this, new ResourceDownloaderException( "Only simple torrents supported" ));
			}
			
		}catch( TOTorrentException e ){
			
			failed( downloader, new ResourceDownloaderException( "Torrent deserialisation failed", e ));
		}
		
		return( true );
	}
	
	public void
	failed(
		ResourceDownloader			downloader,
		ResourceDownloaderException e )
	{
		result		= e;
		
		done_sem.release();

		informFailed(e);
	}
	
	public void
	reportPercentComplete(
		ResourceDownloader	downloader,
		int					percentage )
	{
		if ( downloader == this ){
			
			informPercentDone( percentage );
		}
	}
}
