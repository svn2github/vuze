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

package org.gudy.azureus2.pluginsimpl.torrent;

/**
 * @author parg
 *
 */

import java.net.URL;
import java.io.ByteArrayInputStream;
import java.io.File;

import org.gudy.azureus2.plugins.torrent.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.torrent.*;

public class 
TorrentManagerImpl 
	implements TorrentManager
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
}
