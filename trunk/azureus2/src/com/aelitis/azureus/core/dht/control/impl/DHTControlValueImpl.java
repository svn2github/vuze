/*
 * Created on 18-Jan-2005
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.dht.control.impl;

import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

/**
 * @author parg
 *
 */

public class 
DHTControlValueImpl
	implements DHTTransportValue
{
	private long	creation_time;
	private byte[]	value;
	
	private int		distance;
	
	private long	store_time;
	
	protected
	DHTControlValueImpl(
		long		_creation_time,
		byte[]		_value,
		int			_distance )
	{
		creation_time	= _creation_time;
		value			= _value;
		distance		= _distance;
		
		reset();
	}
	
	protected void
	reset()
	{
		store_time	= SystemTime.getCurrentTime();
		
			// make sure someone hasn't sent us a stupid creation time
		
		if ( creation_time > store_time ){
			
			creation_time	= store_time;
		}	
	}
	
	protected 
	DHTControlValueImpl(
		DHTTransportValue	other,
		int					cache_offset )
	{
		this( other.getCreationTime(), other.getValue(), other.getCacheDistance()+cache_offset );
	}
	
	public long
	getCreationTime()
	{
		return( creation_time );
	}
	
	protected void
	setCreationTime()
	{
		creation_time = SystemTime.getCurrentTime();
	}
	
	protected void
	setStoreTime(
		long	l )
	{
		store_time	= l;
	}
	
	protected long
	getStoreTime()
	{
		return( store_time );
	}
	
	public int 
	getCacheDistance() 
	{
		return( distance );
	}
		
	public byte[]
	getValue()
	{
		return( value );
	}
}
