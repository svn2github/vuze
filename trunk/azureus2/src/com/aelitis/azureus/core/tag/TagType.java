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


package com.aelitis.azureus.core.tag;

import java.util.List;


public interface 
TagType 
{
	public static final int TT_DOWNLOAD_CATEGORY	= 1;
	public static final int TT_DOWNLOAD_STATE		= 2;

		/**
		 * Unique type denoting this species of tag
		 * @return
		 */
	
	public int
	getTagType();
	
	public String
	getTagTypeName();
		
	public boolean
	isTagTypePersistent();
	
	public long
	getTagTypeFeatures();
	
	public boolean
	hasTagTypeFeature(
		long		feature );
	
	public void
	addTag(
		Tag	t );
	
	public void
	removeTag(
		Tag	t );
	
	public List<Tag>
	getTags();
	
	public void
	removeTagType();
	
	public void
	addTagTypeListener(
		TagTypeListener	listener,
		boolean			first_for_existing );
	
	public void
	removeTagTypeListener(
		TagTypeListener	listener );
}
