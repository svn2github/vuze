/*
 * Created on Mar 23, 2013
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

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.core.tag.TagDownload;
import com.aelitis.azureus.core.tag.TagException;
import com.aelitis.azureus.core.tag.TagType;
import com.aelitis.azureus.core.tag.TaggableResolver;

public class 
TagTypeDownloadManual
	extends TagTypeWithState
{
	private AtomicInteger	next_tag_id = new AtomicInteger(0);
		
	protected
	TagTypeDownloadManual(
		TaggableResolver		resolver )
	{
		super( TagType.TT_DOWNLOAD_MANUAL, resolver, TagDownload.FEATURES, "tag.type.man" );
	}
	
	public boolean
	isTagTypePersistent()
	{
		return( true );
	}
	
	public boolean 
	isTagTypeAuto() 
	{
		return( false );
	}
	
	@Override
	public Tag
	createTag(
		String		name,
		boolean		auto_add )
	
		throws TagException
	{
		TagDownloadWithState new_tag = new TagDownloadWithState( this, next_tag_id.incrementAndGet(), name, auto_add, true, true );
				
		return( new_tag );
	}
	
	protected Tag
	createTag(
		int		tag_id,
		Map		details )
	{
		TagDownloadWithState new_tag = new TagDownloadWithState( this, tag_id, details, true, true );
		
		next_tag_id.set( Math.max( next_tag_id.get(), tag_id+1 ));
		
		return( new_tag );
	}
}
