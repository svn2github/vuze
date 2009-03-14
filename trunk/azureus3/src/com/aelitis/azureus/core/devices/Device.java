/*
 * Created on Jan 27, 2009
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


package com.aelitis.azureus.core.devices;

import java.net.URL;

public interface 
Device 
{
	public static final int DT_UNKNOWN				= 0;
	public static final int DT_INTERNET_GATEWAY		= 1;
	public static final int DT_CONTENT_DIRECTORY	= 2;
	public static final int DT_MEDIA_RENDERER		= 3;
	public static final int DT_INTERNET				= 4;
		
	public int
	getType();
	
	public String
	getID();
	
	public String
	getName();
		
	public boolean
	isAlive();
	
	public boolean
	isManual();
	
	public void
	setHidden(
		boolean		is_hidden );
	
	public boolean
	isHidden();
	
	public boolean
	isBrowsable();
	
	public browseLocation[]
	getBrowseLocations();
		
	public void
	setTransientProperty(
		Object		key,
		Object		value );
	
	public Object
	getTransientProperty(
		Object		key );
	
		/**
		 * Array of resource strings and their associated values
		 * @return
		 */
	
	public String[][]
	getDisplayProperties();
	
	public void
	requestAttention();
	
	public void
	remove();
	
	public String
	getInfo();
	
	public String
	getError();
	
	public String
	getString();
	
	interface
	browseLocation
	{
		public String
		getName();
		
		public URL
		getURL();
	}
}
