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

package org.gudy.azureus2.pluginsimpl.torrent;

/**
 * @author parg
 *
 */

import java.net.*;
import java.io.*;

import org.gudy.azureus2.plugins.torrent.*;

import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloader2Factory;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloader2;
import org.gudy.azureus2.core3.torrent.*;

public class 
TorrentDownloaderImpl 
	implements TorrentDownloader
{
	protected URL						url;
	protected TorrentDownloader2		downloader;
	
	protected
	TorrentDownloaderImpl(
		URL		_url )
	{
		url		= _url;
		
		downloader = TorrentDownloader2Factory.create( url.toString());
	}
	
	public Torrent
	download()
	
		throws TorrentException
	{
		try{
			InputStream	is = downloader.download();
			
			TOTorrent	torrent = TOTorrentFactory.deserialiseFromBEncodedInputStream(is);
			
			return( new TorrentImpl(torrent ));
			
		}catch( Throwable e ){
			
			throw( new TorrentException( "TorrentDownloader: download fails", e ));
		}
	}
}
