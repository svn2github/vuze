/*
 * Created on 15-Mar-2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.util;

import java.util.*;

public class 
CopyOnWriteList 
{
	private List	list = new ArrayList();
	
	private boolean	visible = false;
	
	public void
	add(
		Object	obj )
	{
		synchronized( this ){
			
			if ( visible ){
				
				List	new_list = new ArrayList( list );
				
				new_list.add( obj );
			
				list	= new_list;
			
				visible = false;
				
			}else{
				
				list.add( obj );
			}
		}
	}
	
	public boolean
	remove(
		Object	obj )
	{
		synchronized( this ){
			
			if ( visible ){

				List	new_list = new ArrayList( list );
				
				boolean result = new_list.remove( obj );
			
				list	= new_list;
						
				visible = false;
				
				return( result );
				
			}else{
				
				return( list.remove( obj ));
			}
		}
	}
	
	public void
	clear()
	{
		synchronized( this ){
								
			list	= new ArrayList();
			
			visible = false;
		}
	}
	
	public boolean
	contains(
		Object	obj )
	{
		synchronized( this ){

			return( list.contains( obj ));
		}
	}
	
	public Iterator
	iterator()
	{
		synchronized( this ){

			visible = true;
			
			return( new CopyOnWriteListIterator( list.iterator()));
		}
	}
	
	public List
	getList()
	{
			// TODO: we need to either make this a read-only-list or obey the copy-on-write semantics correctly...
		
		synchronized( this ){

			visible = true;
			
			return( list );
		}
	}
	
	public int
	size()
	{
		synchronized( this ){

			return( list.size());
		}
	}
	
	public boolean 
	isEmpty() 
	{
		synchronized( this ){

			return list.isEmpty();
		}
	}
	
	public Object[]
	toArray()
	{
		synchronized( this ){

			return( list.toArray());
		}
	}
	
	public Object[]
  	toArray(
  		Object[]	 x )
  	{
		synchronized( this ){

			return( list.toArray(x));
		}
  	}
	
	private class
	CopyOnWriteListIterator
		implements Iterator
	{
		private Iterator	it;
		private Object		last;
		
		protected
		CopyOnWriteListIterator(
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
			
			CopyOnWriteList.this.remove( last );
		}
	}
}
