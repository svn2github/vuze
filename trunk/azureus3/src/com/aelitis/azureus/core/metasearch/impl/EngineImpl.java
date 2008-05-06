/*
 * Created on May 6, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.metasearch.impl;

import java.io.IOException;
import java.util.Map;

import com.aelitis.azureus.core.metasearch.Engine;
import com.aelitis.azureus.core.metasearch.impl.web.json.JSONEngine;
import com.aelitis.azureus.core.metasearch.impl.web.regex.RegexEngine;

public abstract class 
EngineImpl
	implements Engine
{
	protected static Engine
	importFromBEncodedMap(
		Map		map )
	
		throws IOException
	{
		int	type = ((Long)map.get( "type" )).intValue();
		
		if ( type == Engine.ENGINE_TYPE_JSON ){
			
			return( JSONEngine.importFromBEncodedMap( map ));
			
		}else if ( type == Engine.ENGINE_TYPE_REGEX ){
			
			return( RegexEngine.importFromBEncodedMap( map ));
			
		}else{
			
			throw( new IOException( "Unknown engine type " + type ));
		}
	}
	
	private int			type;
	private long		id;
	private String		name;
	
	
	protected
	EngineImpl(
		int 	_type, 
		long 	_id,
		String 	_name )
	{
		type		= _type;
		id			= _id;
		name		= _name;
	}
	
	protected 
	EngineImpl(
		Map		map )
	
		throws IOException
	{
		type		= ((Long)map.get( "type" )).intValue();
		id			= ((Long)map.get( "id")).longValue();
		name		= new String((byte[])map.get( "name" ), "UTF-8" );
	}
	
	protected void
	exportString(
		Map		map,
		String	key,
		String	value )
	
		throws IOException
	{
		if ( value != null ){
	
			map.put( key, value.getBytes( "UTF-8" ));
		}
	}
	
	protected String
	importString(
		Map		map,
		String	key )
	
		throws IOException
	{
		Object	obj = map.get( key );
		
		if ( obj instanceof byte[]){
			
			return( new String((byte[])obj, "UTF-8" ));
		}
		
		return( null );
	}
	
	protected void
	exportToBencodedMap(
		Map		map )
	
		throws IOException
	{
		map.put( "type", new Long( type ));
		map.put( "id", new Long( id ));
		map.put( "name", name.getBytes( "UTF-8" ));
	}
	
	public int
	getType()
	{
		return( type );
	}
	
	public long 
	getId()
	{
		return id;
	}
	
	public String 
	getName() 
	{
		return name;
	}
}
