/*
 * File    : TRHostExternalTorrent.java
 * Created : 19-Nov-2003
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

package org.gudy.azureus2.core3.tracker.host.impl;

/**
 * @author parg
 *
 */

import java.util.*;
import java.net.*;
import java.io.*;

import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.*;

public class 
TRHostExternalTorrent 
	implements TOTorrent
{
	protected byte[]	hash;
	protected URL		announce_url;
	
	protected
	TRHostExternalTorrent(
		byte[]	_hash,
		URL		_announce_url  )
	{
		hash			= _hash;
		announce_url	= _announce_url;
	}
	
	public byte[]
	getName()
	{
		return( ByteFormatter.nicePrint( hash, true ).getBytes());
	}
	
	
	public boolean
	isSimpleTorrent()
	{
		return( true );
	}
	

	public byte[]
	getComment()
	{
		return( null );
	}

	public void
	setComment(
		String		comment )
	{		
	}
	
	
	public long
	getCreationDate()
	{
		return(0);
	}
	
	public void
	setCreationDate(
		long		date )
	{
	}
	
	public byte[]
	getCreatedBy()
	{
		return( null );
	}
	
	public URL
	getAnnounceURL()
	{
		return( announce_url );
	}

	public void
	setAnnounceURL(
		URL		url )
	{
	}
		
	public TOTorrentAnnounceURLGroup
	getAnnounceURLGroup()
	{
		return( null );
	}
	
	public byte[][]
	getPieces()
	{
		return( new byte[0][] );
	}


	public long
	getPieceLength()
	{
		return( -1 );
	}

	public long
	getSize()
	{
		return( -1 );
	}
	

	public TOTorrentFile[]
	getFiles()
	{
		return( new TOTorrentFile[0]);
	}
	 
	public byte[]
	getHash()
	
		throws TOTorrentException
	{
		return( hash );
	}
	
	public void
	setAdditionalStringProperty(
		String		name,
		String		value )
	{
	}
		
	public String
	getAdditionalStringProperty(
		String		name )
	{
		return( null );
	}
		
	public void
	setAdditionalByteArrayProperty(
		String		name,
		byte[]		value )
	{
	}
		
	public byte[]
	getAdditionalByteArrayProperty(
		String		name )
	{
		return( null );
	}
	
	public void
	setAdditionalLongProperty(
		String		name,
		Long		value )
	{
	}
		
	public Long
	getAdditionalLongProperty(
		String		name )
	{
		return(null);
	}
		
	
	public void
	setAdditionalListProperty(
		String		name,
		List		value )
	{
	}
		
	public List
	getAdditionalListProperty(
		String		name )
	{
		return( null );
	}
		
	public void
	setAdditionalMapProperty(
		String		name,
		Map		value )
	{
	}
		
	public Map
	getAdditionalMapProperty(
		String		name )
	{
		return( null );
	}
		
	public Object
	getAdditionalProperty(
		String		name )
	{
		return( null );
	}
	
	public void
	removeAdditionalProperties()
	{
	}
	
	public void
	serialiseToBEncodedFile(
		File		file )
		  
		throws TOTorrentException
	{
		throw( new TOTorrentException("External Torrent", TOTorrentException.RT_WRITE_FAILS ));
	}


	public Map
	serialiseToMap()
		  
		throws TOTorrentException
	{
		throw( new TOTorrentException("External Torrent", TOTorrentException.RT_WRITE_FAILS ));
	}

   public void
   serialiseToXMLFile(
	   File		file )
		  
	   throws TOTorrentException
	{
		throw( new TOTorrentException("External Torrent", TOTorrentException.RT_WRITE_FAILS ));
	}

	public void
	print()
	{
	}
}
