/*
 * File    : TOTorrent.java
 * Created : 5 Oct. 2003
 * By      : Parg 
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

package org.gudy.azureus2.core3.torrent;

import java.io.*;
import java.net.*;
import java.util.*;

public interface 
TOTorrent
{
	public String
	getName();

	/**
	 * Note: setting the torrent name alters it's hash
	 * @param name	new torrent name
	 */
	
	public void
	setName(
		String		name );
		
	public boolean
	isSimpleTorrent();
	
	public String
	getComment();
	
	public URL
	getAnnounceURL();

	/**
	 * When a group of sets of trackers is defined their URLs are accessed via this method
	 * @return the group, always present, which may have 0 members
	 */
	
	public TOTorrentAnnounceURLGroup
	getAnnounceURLGroup();
	 
	public byte[][]
	getPieces();

	public long
	getPieceLength();

	public TOTorrentFile[]
	getFiles();
	
	public byte[]
	getHash()
	
		throws TOTorrentException;

	/**
	 * The additional properties are used for holding non-core data for Azureus' own user
	 * @param name		name of the property (e.g. "encoding")
	 * @param value		value. This will be encoded with default encoding
	 */
	
	public void
	setAdditionalStringProperty(
		String		name,
		String		value );
		
	public String
	getAdditionalStringProperty(
		String		name );
		
	public void
	setAdditionalByteArrayProperty(
		String		name,
		byte[]		value );
		
	public byte[]
	getAdditionalByteArrayProperty(
		String		name );
	
	public void
	setAdditionalLongProperty(
		String		name,
		Long		value );
		
	public Long
	getAdditionalLongProperty(
		String		name );
		
	
	public void
	setAdditionalListProperty(
		String		name,
		List		value );
		
	public List
	getAdditionalListProperty(
		String		name );
		
	public void
	setAdditionalMapProperty(
		String		name,
		Map		value );
		
	public Map
	getAdditionalMapProperty(
		String		name );
		
	public void
	serialiseToFile(
		File		file )
		  
		throws TOTorrentException;

	public void
	print();
}
