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

import org.gudy.azureus2.core3.util.ListenerManager;
import org.gudy.azureus2.core3.util.ListenerManagerDispatcher;

import com.aelitis.azureus.core.tag.*;
import com.aelitis.azureus.core.util.CopyOnWriteList;

public abstract class 
TagTypeNonPersistentBase
	implements TagType
{	
	private int		tag_type;
	private int		tag_type_features;
	private String	tag_type_name;
	
	private CopyOnWriteList<Tag>	tags = new CopyOnWriteList<Tag>();
	
	private static final int TTL_ADD 	= 1;
	private static final int TTL_REMOVE = 2;
	
	private ListenerManager<TagTypeListener>	tt_listeners 	= 
		ListenerManager.createManager(
			"TagTypeListeners",
			new ListenerManagerDispatcher<TagTypeListener>()
			{
				public void
				dispatch(
					TagTypeListener		listener,
					int					type,
					Object				value )
				{					
					if ( type == TTL_ADD ){
						
						listener.tagAdded((Tag)value);
						
					}else if ( type == TTL_REMOVE ){
						
						listener.tagRemoved((Tag)value);
					}
				}
			});	
			
	protected
	TagTypeNonPersistentBase(
		int			_tag_type,
		int			_tag_features,
		String		_tag_name )
	{
		tag_type			= _tag_type;
		tag_type_features	= _tag_features;
		tag_type_name		= _tag_name;
	}
	
	public int
	getTagType()
	{
		return( tag_type );
	}
	
	public String
	getTagTypeName()
	{
		return( tag_type_name );
	}
	
	public boolean
	isTagTypePersistent()
	{
		return( false );
	}
	
	public long
	getTagTypeFeatures()
	{
		return( tag_type_features );
	}
	
	public void
	addTag(
		Tag	t )
	{
		tt_listeners.dispatch( TTL_ADD, t );
	}
	
	public void
	removeTag(
		Tag	t )
	{
		tt_listeners.dispatch( TTL_REMOVE, t );
	}
	
	public List<Tag>
	getTags()
	{
		return( tags.getList());
	}
	
	public void
	removeTagType()
	{
		TagManagerImpl.getSingleton().removeTagType( this );
	}
	
	public void
	addTagTypeListener(
		TagTypeListener	listener )
	{
		tt_listeners.addListener( listener );
	}
	
	public void
	removeTagTypeListener(
		TagTypeListener	listener )
	{
		tt_listeners.removeListener( listener );
	}
}
