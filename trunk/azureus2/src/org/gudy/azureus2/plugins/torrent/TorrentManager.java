/*
 * File    : TorrentManager.java
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

package org.gudy.azureus2.plugins.torrent;

/**
 * @author parg
 *
 */

import java.net.URL;
import java.io.File;

public interface 
TorrentManager 
{
	public TorrentDownloader
	getURLDownloader(
		URL		url )
	
		throws TorrentException;
	
	public TorrentDownloader
	getURLDownloader(
		URL		url,
		String	user_name,
		String	password )
	
		throws TorrentException;
	
		/**
		 * decodes a torrent encoded using the normal "bencoding" rules from a file
		 * @param file
		 * @return
		 * @throws TorrentException
		 */
	
	public Torrent
	createFromBEncodedFile(
		File		file )
	
		throws TorrentException;
	
	/**
	 * decodes a torrent encoded using the normal "bencoding" rules from a byte array
	 * @param file
	 * @return
	 * @throws TorrentException
	 */
	
	public Torrent
	createFromBEncodedData(
		byte[]		data )
	
		throws TorrentException;
	
		/**
		 * creates a new torrent from an input file
		 * @param data
		 * @return
		 * @throws TorrentException
		 */
	
	public Torrent
	createFromDataFile(
		File		data,
		URL			announce_url )
	
		throws TorrentException;
	
		/**
		 * Gives access to the currently defined torrent attributes. As of 2.1.0.2
		 * only "category" is defined, however in the future more could be added
		 * such as "quality", "bit rate" etc.
		 * @return
		 */
	
	public TorrentAttribute[]
	getDefinedAttributes();
	
	public void
	addListener(
		TorrentManagerListener	l );
	
	public void
	removeListener(
		TorrentManagerListener	l );
}
