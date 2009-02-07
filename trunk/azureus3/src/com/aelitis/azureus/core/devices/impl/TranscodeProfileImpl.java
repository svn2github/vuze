/*
 * Created on Feb 5, 2009
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

import com.aelitis.azureus.core.devices.TranscodeProfile;
import com.aelitis.azureus.core.devices.TranscodeProvider;

public class 
TranscodeProfileImpl 
	implements TranscodeProfile
{
	private TranscodeProvider		provider;
	private String					uid;
	private String 					name;
	private Map<String,String>		properties;
	
	protected 
	TranscodeProfileImpl(
		TranscodeProvider		_provider,			
		String					_uid,
		String					_name,
		Map<String,String>		_properties )
	{
		provider	= _provider;
		uid			= _uid;
		name		= _name;
		properties	= _properties;
	}
	
	public String
	getUID()
	{
		return( uid );
	}
	
	public String
	getName()
	{
		return( name );
	}
	
	public TranscodeProvider
	getProvider()
	{
		return( provider );
	}
}
