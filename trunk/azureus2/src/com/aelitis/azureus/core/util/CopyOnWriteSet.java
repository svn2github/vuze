/*
 * Created on May 30, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.core.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class 
CopyOnWriteSet 
{
	private volatile Set	set = new HashSet();
	
	public void
	add(
		Object		o )
	{
		synchronized( set ){
			
			Set new_set = new HashSet( set );
			new_set.add( o );
			set = new_set;
		}
	}
	
	public boolean
	remove(
		Object		o )
	{
		synchronized( set ){
			
			Set new_set = new HashSet( set );
			boolean res = new_set.remove( o );
			set = new_set;
			
			return( res );
		}
	}
	
	public boolean
	contains(
		Object	o )
	{
		return( set.contains( o ));
	}
	
	public int
	size()
	{
		return( set.size());
	}
	
	public Iterator
	iterator()
	{
		return( new CopyOnWriteSetIterator( set.iterator()));
	}
	
	private class
	CopyOnWriteSetIterator
		implements Iterator
	{
		private Iterator	it;
		private Object		last;
		
		protected
		CopyOnWriteSetIterator(
			Iterator		_it )
		{
			it		= _it;
		}
		
		public boolean
		hasNext()
		{
			return( it.hasNext());
		}
		
		public Object
		next()
		{
			last	= it.next();
			
			return( last );
		}
		
		public void
		remove()
		{
				// don't actually remove it from the iterator. can't go backwards with this iterator so this is
				// not a problem
			
			if ( last == null ){
			
				throw( new IllegalStateException( "next has not been called!" ));
			}
			
			CopyOnWriteSet.this.remove( last );
		}
	}
}
