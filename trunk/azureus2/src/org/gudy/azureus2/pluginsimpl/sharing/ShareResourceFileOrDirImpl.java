/*
 * File    : ShareResourceFileOrDirImpl.java
 * Created : 31-Dec-2003
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

package org.gudy.azureus2.pluginsimpl.sharing;

/**
 * @author parg
 *
 */

import java.io.*;
import java.util.*;

import org.gudy.azureus2.plugins.sharing.*;
import org.gudy.azureus2.pluginsimpl.torrent.*;

import org.gudy.azureus2.core3.torrent.*;

public abstract class 
ShareResourceFileOrDirImpl
	extends		ShareResourceImpl
{
	protected File		file;
	
	protected ShareItemImpl		item;
	
	protected
	ShareResourceFileOrDirImpl(
		ShareManagerImpl	_manager,
		int					_type,
		File				_file )
	
		throws ShareException
	{
		super( _manager, _type );
		
		file		= _file;
		
		if ( getType() == ST_FILE ){
			
			if ( !file.exists()){
			
				throw( new ShareException( "File '" + file.getName() + "' not found"));
			}
		
			if ( !file.isFile()){
			
				throw( new ShareException( "Not a file"));
			}
		}else{
			
			if ( !file.exists()){
				
				throw( new ShareException( "Dir '" + file.getName() + "' not found"));
			}
			
			if ( file.isFile()){
				
				throw( new ShareException( "Not a directory"));
			}		
		}
		
		try{
			file = file.getCanonicalFile();
						
		}catch( IOException e ){
	
			throw( new ShareException("ShareResourceFile: failed to get canonical name", e));
		}
		
		createTorrent();
	}
		
	protected
	ShareResourceFileOrDirImpl(
		ShareManagerImpl	_manager,
		int					_type,
		File				_file,
		Map					_map)
	
		throws ShareException
	{
		super( _manager, _type );
		
		file		= _file;
		
		item = ShareItemImpl.deserialiseItem( this, _map );
	}
	
	protected abstract byte[]
	getFingerPrint()
	
		throws ShareException;
	
	protected void
	createTorrent()
	
		throws ShareException
	{
		try{
			manager.reportCurrentTask( (item==null?"Creating":"Re-creating") + " torrent for '" + file.toString() + "'" );
			
			TOTorrent	to_torrent = TOTorrentFactory.createFromFileOrDirWithComputedPieceLength( 
										file,
										manager.getAnnounceURL(),
										manager );
			
			if ( item == null ){
				
				byte[] fingerprint = getFingerPrint();
			
				item = new ShareItemImpl(this, fingerprint, new TorrentImpl(to_torrent));
				
			}else{
				
				item.setTorrent( new TorrentImpl(to_torrent));
				
				item.writeTorrent();
			}
			
		}catch( TOTorrentException e ){
			
			throw( new ShareException("ShareResourceFile:Torrent create failed", e));
		}
	}
	
	protected void
	checkConsistency()
	
		throws ShareException
	{
		try{
			if ( Arrays.equals(getFingerPrint(), item.getFingerPrint())){
				
				// check torrent file still exists
				
				if ( !manager.torrentExists( item )){
					
					createTorrent();
				}
			}else{
				
				manager.addFileOrDir( file, getType(), true );
			}
		}catch( ShareException e ){
						
			manager.delete( this );
		}
	}
	
	protected static ShareResourceImpl
	deserialiseResource(
		ShareManagerImpl	manager,
		Map					map,
		int					type )
	
		throws ShareException
	{
		File file = new File(new String((byte[])map.get("file")));
		
		if ( type == ST_FILE ){
			
			return( new ShareResourceFileImpl( manager, file, map ));
			
		}else{
			return( new ShareResourceDirImpl( manager, file, map ));
			
		}
	}
	
	protected void
	serialiseResource(
		Map		map )
	{
		map.put( "type", new Long(getType()));
		
		map.put( "file", file.toString());
		
		item.serialiseItem( map );
	}
	
	protected void
	deleteInternal()
	{
		item.delete();
	}
	
	public String
	getName()
	{
		return( file.toString());
	}
	
	public File
	getFile()
	{
		return( file );
	}
	
	public ShareItem
	getItem()
	{
		return( item );
	}
	
	public int
	compareTo(
		Object	other )
	{		
		if ( other instanceof ShareResourceFileOrDirImpl ){
			
			int res = file.compareTo(((ShareResourceFileOrDirImpl)other).getFile());
						
			return( res );
					
		}else{
			
			return( 1 );
		}
	}
	
	public boolean
	equals(
		Object	other )
	{
		if ( other instanceof ShareResourceFileOrDirImpl ){
			
			boolean res = file.equals(((ShareResourceFileOrDirImpl)other).getFile());
			
			return( res );
			
		}else{
			
			return( false );
		}		
	}
	
	public int
	hashCode()
	{
		return( file.hashCode());
	}
}
