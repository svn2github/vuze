/*
 * File    : TorrentDownloaderImpl.java
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

import java.net.*;
import java.io.*;

import org.gudy.azureus2.plugins.torrent.*;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.ResourceDownloaderFactoryImpl;

import org.gudy.azureus2.core3.torrent.*;

public class 
TorrentDownloaderImpl 
	implements TorrentDownloader
{
	protected URL						url;
	protected ResourceDownloader		downloader;
	
	protected
	TorrentDownloaderImpl(
		URL		_url )
	{
		url		= _url;
		
		downloader = ResourceDownloaderFactoryImpl.getSingleton().create( url );
	}
	
	public Torrent
	download()
	
		throws TorrentException
	{
		InputStream	is = null;
		
		try{
			is = downloader.download();
			
			TOTorrent	torrent = TOTorrentFactory.deserialiseFromBEncodedInputStream(is);
			
			return( new TorrentImpl(torrent ));
			
		}catch( Throwable e ){
			
			throw( new TorrentException( "TorrentDownloader: download fails", e ));
			
		}finally{
			
			if ( is != null ){
				
				try{
					
					is.close();
					
				}catch( IOException e ){
					
					e.printStackTrace();
				}
			}
		}
	}
}
