/*
 * File    : RPObject.java
 * Created : 28-Jan-2004
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

package org.gudy.azureus2.ui.webplugin.remoteui.plugins;

import java.io.Serializable;

/**
 * @author parg
 *
 */

import java.util.*;

public abstract class 
RPObject
	implements Serializable
{
		// as long as the key is referenced by the rest of core
		// 		key refers to RPObject
		// 		RPObject refers to object_id
		//		object_id refers to RPObject
		// so neither weak map is cleared down
	
	protected static Map	object_registry			 	= new WeakHashMap();
	protected static Map	object_registry_reverse 	= new WeakHashMap();
	
	protected static long	next_key		= new Random(System.currentTimeMillis()).nextLong();
	
	protected Long	object_id;
	
	protected transient Object				_delegate;
	protected transient	RPRequestDispatcher	dispatcher;
	
	protected static RPObject
	_lookupLocal(
		Object		key )
	{
		synchronized( object_registry ){
			
			RPObject	res = (RPObject)object_registry.get(key);
			
			if ( res != null ){
				
				res._setLocal();
			}
			
			return( res );
		}
	}
			
	protected
	RPObject(
		Object		key )
	{
		synchronized( object_registry ){
			
			RPObject	existing = (RPObject)object_registry.get(key);
			
			if ( existing != null ){
				
				object_id	= existing.object_id;
				
			}else{
			
				object_id	= new Long(next_key++);
				
				object_registry.put( key, this );
				
				object_registry_reverse.put( object_id, key );
			}
		}
		
		_delegate	= key;
		
		_setDelegate( _delegate );
	}
	
	protected abstract void
	_setDelegate(
		Object		_delegate );
	
	protected Object
	_getDelegate()
	{
		return( _delegate );
	}
	
	protected void
	_fixupLocal()
	
		throws RPException
	{
		Object	res = object_registry_reverse.get( object_id );
		
		if ( res == null ){
			
			throw( new RPException( "Object no longer exists"));
		}
		
		_setDelegate( res );
	}
	
	public void
	_setRemote(
		RPRequestDispatcher		_dispatcher )
	{
		dispatcher	= _dispatcher;
	}
	
	public abstract RPReply
	_process(
		RPRequest	request	);
	
	public abstract void
	_setLocal();
	
	public void
	_refresh()
	{
		RPObject	res = (RPObject)dispatcher.dispatch( new RPRequest( this, "_refresh", null )).getResponse();
		
		_setDelegate( res );
	}
	
	public void
	notSupported()
	{
		throw( new RuntimeException( "RPObject:: method not supported"));
	}
}
