/*
 * File    : TorrentImpl.java
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

package org.gudy.azureus2.pluginsimpl.torrent;

import java.net.*;

/**
 * @author parg
 *
 */

import java.util.Map;
import java.io.File;

import org.gudy.azureus2.core3.internat.*;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.torrent.*;
import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.pluginsimpl.download.*;

public class 
TorrentImpl
	implements Torrent
{
	protected TOTorrent				torrent;
	protected LocaleUtilDecoder		decoder;
	
	public
	TorrentImpl(
		TOTorrent	_torrent )
	{
		torrent	= _torrent;
		
		try{
			decoder = LocaleUtil.getTorrentEncoding( torrent );
			
		}catch( Throwable e ){
			
		}
	}
	
	public String
	getName()
	{
		String	name = decode( torrent.getName());
		
		name = FileUtil.convertOSSpecificChars( name );

		return( name );
	}
	
	public URL
	getAnnounceURL()
	{
		return( torrent.getAnnounceURL());
	}
	
	public void
	setAnnounceURL(
		URL		url )
	{
		torrent.setAnnounceURL( url );
		
		updated();
	}

	public TorrentAnnounceURLList
	getAnnounceURLList()
	{
		return( new TorrentAnnounceURLListImpl( this ));
	}

	public TOTorrent
	getTorrent()
	{
		return( torrent );
	}
	
	public byte[]
	getHash()
	{
		try{
			return( torrent.getHash());
			
		}catch( TOTorrentException e ){
			
			e.printStackTrace();
			
			return( null );
		}
	}
		
	public long
	getSize()
	{
		return( torrent.getSize());
	}
	
	public String
	getComment()
	{
		return( decode(torrent.getComment()));
	}
	
	public long
	getCreationDate()
	{
		return( torrent.getCreationDate());
	}
	
	public String
	getCreatedBy()
	{
		return( decode( torrent.getCreatedBy()));
	}
	

	public long
	getPieceSize()
	{
		return( torrent.getPieceLength());
	}
	
	public long
	getPieceCount()
	{
		return( torrent.getPieces().length);
	}
	
	public TorrentFile[]
	getFiles()
	{
		TOTorrentFile[]	files = torrent.getFiles();
		
		TorrentFile[]	res = new TorrentFile[files.length];
		
		for (int i=0;i<res.length;i++){
		
			TOTorrentFile	tf = files[i];
			
			byte[][]	comps = tf.getPathComponents();
			
			String	name = "";
			
			for (int j=0;j<comps.length;j++){
				
				String	comp = decode(comps[j]);
			
				comp = FileUtil.convertOSSpecificChars( comp );
				
				name += (j==0?"":File.separator)+comp;
			}
			
			res[i] = new TorrentFileImpl(name, tf.getLength());
		}
		
		return( res );
	}
	
	protected String
	decode(
		byte[]		data )
	{
		
		if ( data != null ){
			
			if ( decoder != null ){
				
				try{
					return( decoder.decodeString(data));
					
				}catch( Throwable e ){
				}
			}
			
			return( new String(data));
		}
		
		return( "" );
	}
	
	public Map
	writeToMap()
	
		throws TorrentException
	{
		try{
			return( torrent.serialiseToMap());
			
		}catch( TOTorrentException e ){
			
			throw( new TorrentException( "Torrent::writeToMap: fails", e ));
		}
	}
	
	public void
	writeToFile(
		File		file )
	
		throws TorrentException
	{
		try{
			TorrentUtils.writeToFile( torrent, file );
			
		}catch( TOTorrentException e ){
			
			throw( new TorrentException( "Torrent::writeToFile: fails", e ));
		}
	}
  
 	protected void
	updated()
	{
		try{
			DownloadImpl dm = (DownloadImpl)DownloadManagerImpl.getDownloadStatic( torrent );
		
			if ( dm != null ){
			
				dm.torrentChanged();
			}
		}catch( DownloadException e ){
			
			// torrent may not be running
		}
	}
	
	public void
	save()
		throws TorrentException
	{
		try{
			TorrentUtils.writeToFile( torrent );
			
		}catch( TOTorrentException e ){
			
			throw( new TorrentException( "Torrent::save Fails", e ));
		}	
	}
}