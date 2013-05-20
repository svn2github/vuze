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

import java.util.Set;


public interface 
Tag 
{
		/**
		 * Unique type denoting this species of tag
		 * @return
		 */
	
	public TagType
	getTagType();

		/**
		 * Unique ID within this tag type 
		 * @return
		 */
	
	public int
	getTagID();
	
		/**
		 * Unique across tag types and can be used to lookup by TagManager::lookuptagByUID
		 * @return
		 */
	
	public long
	getTagUID();
	
	public String
	getTagName( 
		boolean	localize );
		
	public void
	setTagName(
		String		name )
	
		throws TagException;
	
	public int
	getTaggableTypes();
	
	public void
	setCanBePublic(
		boolean	can_be_public );
	
	public boolean
	canBePublic();
	
	public boolean
	isPublic();
	
	public void
	setPublic(
		boolean	pub );
	
	public boolean
	isVisible();
	
	public void
	setVisible(
		boolean		visible );
	
	public String
	getImageID();
	
	public void
	setImageID(
		String		id );
	
	public int[]
	getColor();
	
	public void
	setColor(
		int[]		rgb );
	
	public void
	addTaggable(
		Taggable	t );
	
	public void
	removeTaggable(
		Taggable	t );
	
	public int
	getTaggedCount();
	
	public Set<Taggable>
	getTagged();
	
	public boolean
	hasTaggable(
		Taggable	t );
	
	public void
	removeTag();
	
	public void
	addTagListener(
		TagListener	listener,
		boolean		fire_for_existing );
	
	public void
	removeTagListener(
		TagListener	listener );
}
