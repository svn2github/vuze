/*
 * File    : TorrentManagerImpl.java
 * Created : 28-Feb-2004
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

package org.gudy.azureus2.pluginsimpl.local.torrent;

/**
 * @author parg
 *
 */

import java.util.*;
import java.net.URL;
import java.io.ByteArrayInputStream;
import java.io.File;

import org.gudy.azureus2.plugins.torrent.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.torrent.*;

public class 
TorrentManagerImpl 
	implements TorrentManager, TOTorrentProgressListener
{
	protected static TorrentManagerImpl	singleton;
	
	public synchronized static TorrentManagerImpl
	getSingleton()
	{
		if ( singleton == null ){
			
			singleton = new TorrentManagerImpl();
		}
		
		return( singleton );
	}
	
	protected List		listeners = new ArrayList();
	
	protected
	TorrentManagerImpl()
	{
	}
	
	public TorrentDownloader
	getURLDownloader(
		URL		url )
	
		throws TorrentException
	{
		return( new TorrentDownloaderImpl( url ));
	}
	
	public TorrentDownloader
	getURLDownloader(
		URL		url,
		String	user_name,
		String	password )
	
		throws TorrentException
	{
		return( new TorrentDownloaderImpl( url, user_name, password ));
	}
	
	public Torrent
	createFromBEncodedFile(
		File		file )
	
		throws TorrentException
	{
		try{
			return( new TorrentImpl(TorrentUtils.readFromFile(file.toString())));
			
		}catch( TOTorrentException e ){
			
			throw( new TorrentException( "TorrentManager::createFromBEncodedFile Fails", e ));
		}
	}
	
	public Torrent
	createFromBEncodedData(
		byte[]		data )
	
		throws TorrentException
	{
		ByteArrayInputStream	is = null;
		
		try{
			is = new ByteArrayInputStream( data );
			
			return( new TorrentImpl(TOTorrentFactory.deserialiseFromBEncodedInputStream(is)));
			
		}catch( TOTorrentException e ){
			
			throw( new TorrentException( "TorrentManager::createFromBEncodedData Fails", e ));
			
		}finally{
			
			try{
				is.close();
				
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
		}
	}
	
	public Torrent
	createFromDataFile(
		File		data,
		URL			announce_url )
	
		throws TorrentException
	{
		return( createFromDataFile( data, announce_url, false ));
	}
	
	public Torrent
	createFromDataFile(
		File		data,
		URL			announce_url,
		boolean		include_other_hashes )
	
		throws TorrentException
	{
		try{
			TOTorrent t =  TOTorrentFactory.createFromFileOrDirWithComputedPieceLength( data, announce_url, include_other_hashes, this );
			
			return( new TorrentImpl(t));
			
		}catch( TOTorrentException e ){
			
			throw( new TorrentException( "TorrentManager::createFromDataFile Fails", e ));
		}	}
	
	public TorrentAttribute[]
	getDefinedAttributes()
	{
		return( new TorrentAttribute[]{ new TorrentAttributeImpl()});
	}
	
	public void
	reportProgress(
		int		percent_complete )
	{
	}
		
	public void
	reportCurrentTask(
		final String	task_description )
	{
		for (int i=0;i<listeners.size();i++){
			
			((TorrentManagerListener)listeners.get(i)).event(
					new TorrentManagerEvent()
					{
						public Object
						getData()
						{
							return( task_description );
						}
					});
		}
	}
	
	public void
	addListener(
		TorrentManagerListener	l )
	{
		listeners.add( l );
	}
		
	public void
	removeListener(
		TorrentManagerListener	l )
	{
		listeners.remove(l);
	}
}
