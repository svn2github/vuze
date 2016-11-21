/*
 * Created on Aug 27, 2014
 * Created by Paul Gardner
 * 
 * Copyright 2014 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or 
 * (at your option) any later version.
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


package com.aelitis.azureus.core.dht.router;

public class 
DHTRouterContactWrapper 
	implements DHTRouterContact
{
	private DHTRouterContact		delegate;
	
	public
	DHTRouterContactWrapper(
		DHTRouterContact		_contact )
	{
		delegate	= _contact;
	}
	
	protected DHTRouterContact
	getDelegate()
	{
		return( delegate );
	}
	
	public byte[]
	getID()
	{
		return( delegate.getID());
	}
	
	public DHTRouterContactAttachment
	getAttachment()
	{
		return( delegate.getAttachment());
	}
	
	public boolean
	hasBeenAlive()
	{
		return( delegate.hasBeenAlive());
	}
	
	public boolean
	isFailing()
	{
		return( delegate.isFailing());
	}
	
	public boolean
	isAlive()
	{
		return( delegate.isAlive());
	}	
	
	public long
	getTimeAlive()
	{
		return( delegate.getTimeAlive());
	}
	
	public String
	getString()
	{
		return( delegate.getString());
	}
	
	public boolean 
	isBucketEntry()
	{
		return( delegate.isBucketEntry());
	}

	public boolean 
	isReplacement()
	{
		return( delegate.isReplacement());
	}
}
