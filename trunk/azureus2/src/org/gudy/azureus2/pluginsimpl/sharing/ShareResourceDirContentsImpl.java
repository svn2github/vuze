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
		
		if ( !root.exists()){
			
			throw( new ShareException( "Dir '" + root.getName() + "' not found"));
		}
		
		if ( root.isFile()){
			
			throw( new ShareException( "Not a directory"));
		}
		
			// new resource, trigger processing
		
		checkConsistency();
	}
	
	protected
	ShareResourceDirContentsImpl(
		ShareManagerImpl	_manager,
		File				_dir,
		boolean				_recursive,
		Map					_map )

		throws ShareException
	{
		super( _manager, ST_DIR_CONTENTS );
		
		root 		= _dir;
		recursive	= _recursive;
		
		if ( !root.exists()){
			
			throw( new ShareException( "Dir '" + root.getName() + "' not found"));
		}
		
		if ( root.isFile()){
			
			throw( new ShareException( "Not a directory"));
		}
		
			// deserialised resource, checkConsistency will be called later to trigger sub-share adding
	}
	
	protected void
	checkConsistency()
	{
		// ensure all shares are defined as per dir contents and recursion flag
		
		checkConsistency(root);
	}
	
	protected void
	checkConsistency(
		File		dir )
	{
		File[]	files = dir.listFiles();
		
		for (int i=0;i<files.length;i++){
			
			File	file = files[i];
		
			String	file_name = file.getName();
			
			if (!(file_name.equals(".") || file_name.equals(".." ))){
				
				if ( file.isDirectory()){
					
					if ( recursive ){
						
						checkConsistency( file );
						
					}else{
						
						try{
							if ( manager.getDir( file ) == null ){
							
								manager.addDir( file );
							}
						}catch( ShareException e ){
							
							e.printStackTrace();
						}
					}
				}else{
	
					try{
						if ( manager.getFile( file ) == null ){
							
							manager.addFile( file );
						}
					}catch( ShareException e ){
						
						e.printStackTrace();
					}
				}
			}
		}		
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
		map.put( "recursive", new Long(recursive?1:0));
		
		map.put( "file", root.toString());
	}
	
	protected static ShareResourceImpl
	deserialiseResource(
		ShareManagerImpl	manager,
		Map					map )
	
		throws ShareException
	{
		File root = new File(new String((byte[])map.get("file")));
		
		boolean	recursive = ((Long)map.get("recursive")).longValue() == 1;
		
		return( new ShareResourceDirContentsImpl( manager, root, recursive, map ));
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
