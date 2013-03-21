/*
 * Created on Mar 20, 2013
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
import java.util.concurrent.atomic.AtomicInteger;

import org.gudy.azureus2.core3.util.ListenerManager;
import org.gudy.azureus2.core3.util.ListenerManagerDispatcher;

import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.core.tag.TagListener;
import com.aelitis.azureus.core.tag.Taggable;

public abstract class 
TagImpl
	implements Tag
{
	private static AtomicInteger	next_tag_id = new AtomicInteger();
	  
	private TagTypeNonPersistentBase	tag_type;
	private int			tag_id;
	private String		tag_name;
	
	private static final int TL_ADD 	= 1;
	private static final int TL_REMOVE 	= 2;
	
	private ListenerManager<TagListener>	t_listeners 	= 
		ListenerManager.createManager(
			"TagListeners",
			new ListenerManagerDispatcher<TagListener>()
			{
				public void
				dispatch(
					TagListener			listener,
					int					type,
					Object				value )
				{					
					if ( type == TL_ADD ){
						
						listener.tagabbleAdded((Taggable)value);
						
					}else if ( type == TL_REMOVE ){
						
						listener.tagabbleRemoved((Taggable)value);
					}
				}
			});	
	
	protected
	TagImpl(
		TagTypeNonPersistentBase	_tag_type,
		String		_tag_name )
	{
		tag_type		= _tag_type;
		tag_id			= next_tag_id.incrementAndGet();
		tag_name		= _tag_name;
		
		tag_type.addTag( this );
	}
	
	public TagTypeNonPersistentBase
	getTagType()
	{
		return( tag_type );
	}
	
	public int
	getTagID()
	{
		return( tag_id );
	}
	
	public String
	getTagName()
	{
		return( tag_name );
	}
	
	public boolean
	isTagPersistent()
	{
		return( false );
	}
	
	public void
	addTaggable(
		Taggable	t )
	{
		t_listeners.dispatch( TL_ADD, t );
	}
	
	public void
	removeTaggable(
		Taggable	t )
	{
		t_listeners.dispatch( TL_REMOVE, t );
	}
	
	public abstract List<Taggable>
	getTagged();
	
	public void
	removeTag()
	{
		System.out.println( "removeTag: " + tag_name );
	}
	
	public void
	addTagListener(
		TagListener	listener )
	{
		t_listeners.addListener( listener );
	}
	
	public void
	removeTagListener(
		TagListener	listener )
	{
		t_listeners.removeListener( listener );
	}
}
