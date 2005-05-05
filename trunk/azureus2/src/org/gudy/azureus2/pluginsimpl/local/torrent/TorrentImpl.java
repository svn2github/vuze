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

package org.gudy.azureus2.pluginsimpl.local.torrent;

import java.net.*;

/**
 * @author parg
 *
 */

import java.util.Map;
import java.io.File;

import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.download.DownloadManagerStateFactory;
import org.gudy.azureus2.core3.internat.*;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.torrent.*;
import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.pluginsimpl.local.download.*;

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
			decoder = LocaleUtil.getSingleton().getTorrentEncoding( torrent );
			
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
	
	public boolean
	isDecentralised()
	{
		return( TorrentUtils.isDecentralised( torrent ));
	}
	public boolean
	isDecentralisedBackupEnabled()
	{
		return( TorrentUtils.getDHTBackupEnabled( torrent ));
	}
	
	public void
	setDecentralisedBackupRequested(
		boolean	requested )
	{
		TorrentUtils.setDHTBackupRequested( torrent, requested );
	}
	
	public boolean
	isDecentralisedBackupRequested()
	{
		return( TorrentUtils.isDHTBackupRequested( torrent ));

	}
	public boolean
	isPrivate()
	{
		return( TorrentUtils.getPrivate( torrent ));
	}
	
	public void
	setPrivate(
		boolean	priv )
	{
		TorrentUtils.setPrivate( torrent, priv );
	}
	
	public byte[]
	getHash()
	{
		try{
			return( torrent.getHash());
			
		}catch( TOTorrentException e ){
			
			Debug.printStackTrace( e );
			
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
	
	public void
	setComment(
		String	comment )
	{
		torrent.setComment( comment );
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
		return( torrent.getNumberOfPieces());
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
	
	public String
	getEncoding()
	{
		if ( decoder != null ){
			
			return( decoder.getName());
		}
		
		return( Constants.DEFAULT_ENCODING );
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
	
	public Object
	getAdditionalProperty(
		String		name )
	{
		return( torrent.getAdditionalProperty( name ));
	}
	
	public Torrent
	removeAdditionalProperties()
	{
		try{
			TOTorrent	t = TOTorrentFactory.deserialiseFromMap(torrent.serialiseToMap());
					
			t.removeAdditionalProperties();
	
			return( new TorrentImpl( t ));
			
		}catch( TOTorrentException e ){
			
			Debug.printStackTrace(e);
			
			return( this );
		}
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
	
	public byte[]
	writeToBEncodedData()
	
		throws TorrentException
	{
		try{
			Map	map = torrent.serialiseToMap();
			
			return( BEncoder.encode( map ));
			
		}catch( Throwable e ){
			
			throw( new TorrentException( "Torrent::writeToBEncodedData: fails", e ));
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
	
	public void
	setComplete(
		File		data_dir )
	
		throws TorrentException
	{		
		try{
			LocaleUtil.getSingleton().setDefaultTorrentEncoding( torrent );
		
			DownloadManagerState	download_manager_state = 
				DownloadManagerStateFactory.getDownloadState( torrent ); 

			TorrentUtils.setResumeDataCompletelyValid( download_manager_state, data_dir.toString());

			download_manager_state.save();
			
		}catch( Throwable e ){
			
			throw( new TorrentException("encoding selection fails", e ));
		}
	}
	
}