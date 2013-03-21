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

import com.aelitis.azureus.core.tag.TagManager;
import com.aelitis.azureus.core.tag.TagType;
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
	
	private
	TagManagerImpl()
	{
		
	}
	
	public void
	addTagType(
		TagType		tag_type )
	{
		tag_types.add( tag_type );
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
	
	public void
	registerTaggableResolver(
		TaggableResolver	resolver )
	{
		
	}
	
}
