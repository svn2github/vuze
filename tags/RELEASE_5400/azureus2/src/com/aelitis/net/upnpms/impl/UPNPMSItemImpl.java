/*
 * Created on Dec 20, 2012
 * Created by Paul Gardner
 * 
 * Copyright 2012 Vuze, Inc.  All rights reserved.
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


package com.aelitis.net.upnpms.impl;

import java.net.URL;

import com.aelitis.net.upnpms.UPNPMSItem;

public class 
UPNPMSItemImpl
	implements UPNPMSItem
{
	private String					id;
	private String					title;
	private String					item_class;
	private long					size;
	private URL						url;
		
	protected
	UPNPMSItemImpl(
		String				_id,
		String				_title,
		String				_class,
		long				_size,
		URL					_url )
	{
		id 			= _id;
		title		= _title;
		item_class	= _class;
		size		= _size;
		url			= _url;
	}
	
	public String
	getID()
	{
		return( id );
	}
	
	public String
	getTitle()
	{
		return( title );
	}
	
	public String
	getItemClass()
	{
		return( item_class );
	}
	
	public long
	getSize()
	{
		return( size );
	}
	
	public URL
	getURL()
	{
		return( url );
	}
}
