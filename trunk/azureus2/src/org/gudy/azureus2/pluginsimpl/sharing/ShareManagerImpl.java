/*
 * File    : ShareManagerImpl.java
 * Created : 30-Dec-2003
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

public class 
ShareManagerImpl
	implements ShareManager
{
	protected static ShareManagerImpl	singleton;
	
	public synchronized static ShareManagerImpl
	getSingleton()
	{
		if ( singleton == null ){
			
			singleton = new ShareManagerImpl();
		}
		
		return( singleton );
	}
	
	
	public ShareResource[]
	getShares()
	{
		return( new ShareResource[0]);
	}
	
	public ShareResourceFile
	addFile(
		File	file )
	{
		return( null );
	}
	
	public ShareResourceDir
	addDir(
		File	dir )
	{
		return( null );
		
	}
	
	public ShareResourceDirContents
	addDirContents(
		File	dir,
		boolean	recursive )
	{
		return( null );
		
	}	
}
