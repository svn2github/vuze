/*
 * File    : ShareResourceFileImpl.java
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

import java.io.File;

import org.gudy.azureus2.plugins.sharing.*;
import org.gudy.azureus2.pluginsimpl.torrent.*;

import org.gudy.azureus2.core3.torrent.*;

public class 
ShareResourceFileImpl
	extends		ShareResourceImpl
	implements 	ShareResourceFile
{
	protected File		file;
	
	protected ShareItemImpl		item;
	
	protected
	ShareResourceFileImpl(
		ShareManagerImpl	_manager,
		File				_file )
	
		throws ShareException
	{
		super( _manager, ST_FILE );
		
		file		= _file;
		
		if ( !file.exists()){
		
			throw( new ShareException( "File not found"));
		}
	
		if ( !file.isFile()){
		
			throw( new ShareException( "Not a file"));
		}
		
		try{
			TOTorrent	to_torrent = TOTorrentFactory.createFromFileOrDirWithComputedPieceLength( 
										file,
										manager.getAnnounceURL());
			
			item = new ShareItemImpl(new TorrentImpl(to_torrent));
			
		}catch( TOTorrentException e ){
			
			throw( new ShareException("Torrent create failed", e));
		}
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
}
