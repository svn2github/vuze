/*
 * Created on Mar 21, 2013
 * Created by Paul Gardner
 * 
 * Copyright 2013 Azureus Software, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.tag.impl;

import java.util.List;

import com.aelitis.azureus.core.tag.TagException;
import com.aelitis.azureus.core.tag.Taggable;
import com.aelitis.azureus.core.util.CopyOnWriteList;

public class 
TagWithState 
	extends TagBase
{
	private CopyOnWriteList<Taggable>	objects = new CopyOnWriteList<Taggable>();
	
	public
	TagWithState(
		TagTypeBase			tt,
		int					tag_id,
		String				name )
	{
		super( tt, tag_id, name );		
	}
	
	public void 
	setTagName(
		String name )
	
		throws TagException 
	{
		super.setTagName( name );
		
		getManager().tagChanged( this );
	}
	
	public void
	addTaggable(
		Taggable	t )
	{
		//System.out.println( getTagName() + ": add " + t.getTaggableID());
		objects.add( t );
		
		super.addTaggable( t );
		
		getManager().tagContentsChanged( this );
	}
	
	public void
	removeTaggable(
		Taggable	t )
	{
		//System.out.println( getTagName() + ": rem " + t.getTaggableID());
		objects.remove( t );
		
		super.removeTaggable( t );
		
		getManager().tagContentsChanged( this );
	}
	
	public int 
	getTaggedCount() 
	{
		return( objects.size());
	}
	
	public boolean 
	hasTaggable(
		Taggable	t )
	{
		return( objects.contains( t ));
	}
	
	public List<Taggable>
	getTagged()
	{
		return( objects.getList());
	}
}
