/*
 * File    : Torrent.java
 * Created : 08-Dec-2003
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

import java.net.*;

/**
 * @author parg
 *
 */

import java.io.File;

public interface 
Torrent
{
	public String
	getName();
	
	public byte[]
	getHash();
	
	/**
	 * If size is 0 then this is an "external" torrent and we only know its hash (and name 
	 * constructed from hash). e.g. we don't know file details
	 * @return
	 */
		
	public long
	getSize();
	
	public String
	getComment();
	
	public long
	getCreationDate();
	
	public String
	getCreatedBy();
		
	public long
	getPieceSize();
	
	public long
	getPieceCount();
	
	public TorrentFile[]
	getFiles();
	
	public void
	writeToFile(
		File		file )
	
		throws TorrentException;
  
  /**
   * Add a group of URLs to the end of the torrent's current set of announce urls.
   */
  public void addAnnounceURLTrackerGroup( URL[] urls );
  
}
