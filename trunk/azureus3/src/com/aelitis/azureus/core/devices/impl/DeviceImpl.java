/*
 * Created on Jan 28, 2009
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


package com.aelitis.azureus.core.devices.impl;

import java.util.*;

import org.gudy.azureus2.core3.util.LightHashMap;

import com.aelitis.azureus.core.devices.Device;

public class 
DeviceImpl
	implements Device
{
	private int				type;
	private String			uid;
	private String 			name;
	
	private Map<Object,Object>	transient_properties = new LightHashMap<Object, Object>(1);
	
	protected
	DeviceImpl(
		int			_type,
		String		_uid,
		String		_name )
	{
		type		= _type;
		uid			= _uid;
		name		= _name;
	}
	
	public int
	getType()
	{
		return( type );
	}
	
	public String
	getID()
	{
		return( uid );
	}
	
	public String
	getName()
	{
		return( name );
	}
	
	protected void
	alive()
	{
		// TODO:
	}
	
	protected void
	dead()
	{
		// TODO:
	}
	
	public void
	setTransientProperty(
		Object		key,
		Object		value )
	{
		synchronized( transient_properties ){
			
			transient_properties.put( key, value );
		}
	}
	
	public Object
	getTransientProperty(
		Object		key )
	{
		synchronized( transient_properties ){
			
			return( transient_properties.get( key ));
		}
	}
	
	public String
	getString()
	{
		return( "type=" + type + ",uid=" + uid + ",name=" + name );
	}
}
