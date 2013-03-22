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

import java.util.*;

import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.tag.TagManager;
import com.aelitis.azureus.core.tag.TagType;
import com.aelitis.azureus.core.tag.Taggable;
import com.aelitis.azureus.core.tag.TaggableLifecycleHandler;
import com.aelitis.azureus.core.tag.TaggableResolver;
import com.aelitis.azureus.core.util.CopyOnWriteList;

public class 
TagManagerImpl
	implements TagManager
{
	private static TagManagerImpl	singleton = new TagManagerImpl();
	
	public static TagManagerImpl
	getSingleton()
	{
		return( singleton );
	}
	
	private CopyOnWriteList<TagType>	tag_types = new CopyOnWriteList<TagType>();
	
	private Map<Integer,TagType>	tag_type_map = new HashMap<Integer, TagType>();
	
	private
	TagManagerImpl()
	{
	}
	
	public void
	addTagType(
		TagType		tag_type )
	{
		synchronized( tag_type_map ){
			
			if ( tag_type_map.put( tag_type.getTagType(), tag_type) != null ){
				
				Debug.out( "Duplicate tag type!" );
			}
		}
		
		tag_types.add( tag_type );
	}
	
	public TagType 
	getTagType(
		int 	tag_type) 
	{
		synchronized( tag_type_map ){

			return( tag_type_map.get( tag_type ));
		}
	}
	
	protected void
	removeTagType(
		TagType		tag_type )
	{
		tag_types.remove( tag_type );
	}
	
	public List<TagType>
	getTagTypes()
	{
		return( tag_types.getList());
	}
	
	public TaggableLifecycleHandler
	registerTaggableResolver(
		TaggableResolver	resolver )
	{
		return(
			new TaggableLifecycleHandler()
			{
				public void
				taggableCreated(
					Taggable	t )
				{	
				}
				
				public void
				taggableDestroyed(
					Taggable	t )
				{
				}
			});
	}
	
	protected long
	readLongAttribute(
		TagTypeBase	tag_type,
		TagBase		tag,
		String		attr,
		long		def )
	{
		return( def );
	}
	
	protected void
	writeLongAttribute(
		TagTypeBase		tag_type,
		TagBase			tag,
		String			attr,
		long			value )
	{
	}	
}
