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

package org.gudy.azureus2.pluginsimpl.local.sharing;

/**
 * @author parg
 *
 */

import java.io.*;
import java.util.*;

import org.gudy.azureus2.plugins.sharing.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.internat.*;

public class 
ShareResourceDirContentsImpl
	extends		ShareResourceImpl
	implements 	ShareResourceDirContents
{
	protected File		root;
	protected boolean	recursive;
	
	protected ShareResource[]		children;
	
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
			
			throw( new ShareException( "Dir '".concat(root.getName()).concat("' not found")));
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
			
			throw( new ShareException( "Dir '".concat(root.getName()).concat("' not found")));
		}
		
		if ( root.isFile()){
			
			throw( new ShareException( "Not a directory"));
		}
		
			// deserialised resource, checkConsistency will be called later to trigger sub-share adding
	}
	
	public boolean
	canBeDeleted()
	
		throws ShareResourceDeletionVetoException
	{
		for (int i=0;i<children.length;i++){
			
			children[i].canBeDeleted();
		}
		
		return( true );
	}
	
	protected void
	checkConsistency()
	{
		// ensure all shares are defined as per dir contents and recursion flag
		
		List	kids = checkConsistency(root);
		
		children = new ShareResource[kids.size()];
		
		kids.toArray( children );
	}
	
	protected List
	checkConsistency(
		File		dir )
	{
		List	kids = new ArrayList();
		
		File[]	files = dir.listFiles();
		
		for (int i=0;i<files.length;i++){
			
			File	file = files[i];
		
			String	file_name = file.getName();
			
			if (!(file_name.equals(".") || file_name.equals(".." ))){
				
				if ( file.isDirectory()){
					
					if ( recursive ){
						
						List	child = checkConsistency( file );
						
						kids.add( new shareNode( file, child ));
						
					}else{
						
						try{
							ShareResource res = manager.getDir( file );
							
							if ( res == null ){
							
								res = manager.addDir( file );
							}
							
							kids.add( res );
							
						}catch( Throwable e ){
							
							e.printStackTrace();
						}
					}
				}else{
	
					try{
						ShareResource res = manager.getFile( file );
						
						if ( res == null ){
							
							res = manager.addFile( file );
						}
						
						kids.add( res );
						
					}catch( Throwable e ){
						
						e.printStackTrace();
					}
				}
			}
		}
		
		for (int i=0;i<kids.size();i++){
			
			Object	o = kids.get(i);
			
			if ( o instanceof ShareResourceImpl ){
		
				((ShareResourceImpl)o).setParent(this);
			}else{
				
				((shareNode)o).setParent(this);
			}
		}
		
		return( kids );
	}
	
	protected void
	deleteInternal()
	{
		for (int i=0;i<children.length;i++){
			
			try{
				if ( children[i] instanceof ShareResourceImpl ){
				
					((ShareResourceImpl)children[i]).delete(true);
				}else{
					
					((shareNode)children[i]).delete(true);
					
				}
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
		}
	}
	
	protected void
	serialiseResource(
		Map		map )
	{
		map.put( "type", new Long(getType()));
		
		map.put( "recursive", new Long(recursive?1:0));
		
		try{
			map.put( "file", root.toString().getBytes( Constants.DEFAULT_ENCODING));
			
		}catch( UnsupportedEncodingException e ){
			
			e.printStackTrace();
		}
	}
	
	protected static ShareResourceImpl
	deserialiseResource(
		ShareManagerImpl	manager,
		Map					map )
	
		throws ShareException
	{
		try{
			File root = new File(new String((byte[])map.get("file"), Constants.DEFAULT_ENCODING));
		
			boolean	recursive = ((Long)map.get("recursive")).longValue() == 1;
		
			return( new ShareResourceDirContentsImpl( manager, root, recursive, map ));
			
		}catch( UnsupportedEncodingException e ){
			
			throw( new ShareException( "internal error", e ));
		}
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
		
	public ShareResource[]
	getChildren()
	{
		return( children );
	}
	
	protected class
	shareNode
		implements ShareResourceDirContents
	{
		protected ShareResourceDirContents	parent;
		protected File						node;
		protected ShareResource[]			children;
		
		protected
		shareNode(
			File						_node,
			List						kids )
		{
			node	=_node;
			
			children = new ShareResource[kids.size()];
			
			kids.toArray( children );
			
			for (int i=0;i<children.length;i++){
				
				Object	o = children[i];
				
				if ( o instanceof ShareResourceImpl ){
					
					((ShareResourceImpl)o).setParent( this );
				}else{
					
					((shareNode)o).setParent( this );

				}
			}
		}
		
		public ShareResourceDirContents
		getParent()
		{
			return( parent );
		}
		
		protected void
		setParent(
			ShareResourceDirContents	_parent )
		{
			parent	= _parent;
		}
		
		public int
		getType()
		{
			return( ShareResource.ST_DIR_CONTENTS );
		}
		
		public String
		getName()
		{
			return( node.toString());
		}
		
		public void
		delete()
		
			throws ShareResourceDeletionVetoException
		{
			throw( new ShareResourceDeletionVetoException( MessageText.getString("plugin.sharing.remove.veto")));
		}
		
		protected void
		delete(
			boolean	force )
		
			throws ShareException, ShareResourceDeletionVetoException
		{
			for (int i=0;i<children.length;i++){
				
				Object	o = children[i];
				
				if ( o instanceof ShareResourceImpl ){
					
					((ShareResourceImpl)o).delete(force);
				}else{
					
					((shareNode)o).delete(force);

				}
			}
		}
		
		
		public boolean
		canBeDeleted()
		
			throws ShareResourceDeletionVetoException
		{
			for (int i=0;i<children.length;i++){
				
				children[i].canBeDeleted();
			}
			
			return( true );
		}
		
		public File
		getRoot()
		{
			return( node );
		}
			
		public boolean
		isRecursive()
		{
			return( recursive );
		}
			
		public ShareResource[]
		getChildren()
		{
			return( children );
		}
		
		public void
		addDeletionListener(
			ShareResourceWillBeDeletedListener	l )
		{
		}
		
		public void
		removeDeletionListener(
			ShareResourceWillBeDeletedListener	l )
		{
		}
	}
}
