/*
 * Created on Jul 9, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.content;

import org.gudy.azureus2.core3.util.Base32;

public class 
RelatedContent 
{
	private String 		title;
	private byte[]		hash;
	private String		tracker;
	
	protected
	RelatedContent(
		String		_title,
		byte[]		_hash,
		String		_tracker )
	{
		title	= _title;
		hash	= _hash;
		tracker	= _tracker;
	}
	
	public String
	getTitle()
	{
		return( title );
	}
	
	public byte[]
	getHash()
	{
		return( hash );
	}
	
	public String
	getTracker()
	{
		return( tracker );
	}
	
	public String
	getString()
	{
		return( "title=" + title + ", hash=" + (hash==null?"null":Base32.encode( hash )) + ", tracker=" + tracker );
	}
}
