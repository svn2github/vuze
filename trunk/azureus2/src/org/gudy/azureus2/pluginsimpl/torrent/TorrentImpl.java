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

/**
 * @author parg
 *
 */

import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.plugins.torrent.*;

public class 
TorrentImpl
	implements Torrent
{
	protected TOTorrent		torrent;
	
	public
	TorrentImpl(
		TOTorrent	_torrent )
	{
		torrent	= _torrent;
	}
	
	public byte[]
	getName()
	{
		return( torrent.getName());
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
}