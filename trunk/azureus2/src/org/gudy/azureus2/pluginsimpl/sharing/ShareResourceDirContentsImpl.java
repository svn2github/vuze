/*
 * File    : ShareResourceDirContentsImpl.java
 * Created : 02-Jan-2004
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
import java.util.Map;

import org.gudy.azureus2.plugins.sharing.*;

public class 
ShareResourceDirContentsImpl
	extends		ShareResourceImpl
	implements 	ShareResourceDirContents
{
	protected File		root;
	protected boolean	recursive;
	
	protected
	ShareResourceDirContentsImpl(
		ShareManagerImpl	_manager,
		File				_dir,
		boolean				_recursive )

		throws ShareException
	{
		super( _manager, ST_DIR_CONTENTS );
		
		root 		= _dir;
		recursive	= _recursive;
	}
	
	protected void
	checkConsistency()
	{
		// TODO:
	}
	
	protected void
	deleteInternal()
	{
		// TODO:
	}
	
	protected void
	serialiseResource(
		Map		map )
	{
		// TODO:
	}
	
	protected static ShareResourceImpl
	deserialiseResource(
		ShareManagerImpl	_manager,
		Map					_map )
	{
		// TODO:
		
		return( null );
	}
	
	public String
	getName()
	{
		return( root.toString());
	}
	
	public File
	getRoot()
	{
		return( root );
	}
	
	public boolean
	isRecursive()
	{
		return( recursive );
	}
	
	
	public ShareItem[]
	getItems()
	{
		return( null ); // TODO:
		
	}
	
	public ShareResourceDirContents[]
	getSubShares()
	{
		return( null ); // TODO:
	}
	
	public int
	compareTo(
			Object	other )
	{		
		if ( other instanceof ShareResourceDirContentsImpl ){
			
			int res = root.compareTo(((ShareResourceDirContentsImpl)other).getRoot());
			
			return( res );
			
		}else{
			
			return( 1 );
		}
	}
	
	public boolean
	equals(
			Object	other )
	{
		if ( other instanceof ShareResourceDirContentsImpl ){
			
			boolean res = root.equals(((ShareResourceDirContentsImpl)other).getRoot());
			
			return( res );
			
		}else{
			
			return( false );
		}		
	}
	
	public int
	hashCode()
	{
		return( root.hashCode());
	}
}
