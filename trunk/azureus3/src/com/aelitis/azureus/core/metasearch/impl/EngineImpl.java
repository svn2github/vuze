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
import com.aelitis.azureus.util.JSONUtils;

public abstract class 
EngineImpl
	implements Engine
{
	protected static Engine
	importFromBEncodedMap(
		MetaSearchImpl		meta_search,
		Map					map )
	
		throws IOException
	{
		int	type = ((Long)map.get( "type" )).intValue();
		
		if ( type == Engine.ENGINE_TYPE_JSON ){
			
			return( JSONEngine.importFromBEncodedMap( meta_search, map ));
			
		}else if ( type == Engine.ENGINE_TYPE_REGEX ){
			
			return( RegexEngine.importFromBEncodedMap( meta_search, map ));
			
		}else{
			
			throw( new IOException( "Unknown engine type " + type ));
		}
	}
	
	public static Engine
	importFromJSONString(
		MetaSearchImpl	meta_search,
		int				type,
		long			id,
		long			last_updated,
		String			name,
		String			content )
	
		throws IOException
	{
		Map map = JSONUtils.decodeJSON( content );
		
		if ( type == Engine.ENGINE_TYPE_JSON ){
			
			return( JSONEngine.importFromJSONString( meta_search, id, last_updated, name, map ));
			
		}else if ( type == Engine.ENGINE_TYPE_REGEX ){
			
			return( RegexEngine.importFromJSONString( meta_search, id, last_updated, name, map ));
			
		}else{
			
			throw( new IOException( "Unknown engine type " + type ));
		}	
	}
	
	private MetaSearchImpl	meta_search;
	
	private int			type;
	private long		id;
	private long		last_updated;
	private String		name;
	
		// selection state indicates manually selected
	
	private boolean		selected;
	private boolean		selection_state_recorded	= true;
	
	private int			source	= ENGINE_SOURCE_UNKNOWN;
	
	protected
	EngineImpl(
		MetaSearchImpl	_meta_search,
		int 			_type, 
		long 			_id,
		long			_last_updated,
		String 			_name )
	{
		meta_search		= _meta_search;
		type			= _type;
		id				= _id;
		last_updated	= _last_updated;
		name			= _name;
	}
	
	protected 
	EngineImpl(
		MetaSearchImpl	_meta_search,
		Map				map )
	
		throws IOException
	{
		meta_search		= _meta_search;
		
		type			= ((Long)map.get( "type" )).intValue();
		id				= ((Long)map.get( "id")).longValue();
		last_updated	= importLong( map, "last_updated" );
		name			= importString( map, "name" );
		
		selected		= importBoolean( map, "selected" );
	}
	
	protected void
	exportToBencodedMap(
		Map		map )
	
		throws IOException
	{
		map.put( "type", new Long( type ));
		map.put( "id", new Long( id ));
		map.put( "last_updated", new Long( last_updated ));
		
		exportString( map, "name", name );
		
		exportBoolean( map, "selected", selected );
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
		
		if ( obj instanceof String ){
			
			return((String)obj);
			
		}else if ( obj instanceof byte[]){
			
			return( new String((byte[])obj, "UTF-8" ));
		}
		
		return( null );
	}
	
	protected long
	importLong(
		Map		map,
		String	key )
	
		throws IOException
	{
		Object	obj = map.get( key );
		
		if ( obj instanceof Long){
			
			return(((Long)obj).longValue());
			
		}else if ( obj instanceof String ){
			
			return( Long.parseLong((String)obj));
		}
		
		return( 0 );
	}

	protected void
	exportBoolean(
		Map		map,
		String	key,
		boolean	value )
	
		throws IOException
	{
		map.put( key, new Long( value?1:0 ));
	}
	
	protected boolean
	importBoolean(
		Map		map,
		String	key )
	
		throws IOException
	{
		return( importBoolean( map, key, false ));
	}
	
	protected boolean
	importBoolean(
		Map		map,
		String	key,
		boolean	def )
	
		throws IOException
	{
		Object	obj = map.get( key );
		
		if ( obj instanceof Long){
			
			return(((Long)obj).longValue() == 1 );
		}else if ( obj instanceof Boolean ){
			
			return(((Boolean)obj).booleanValue());
		}
		
		return( def );
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
	
	public long
	getLastUpdated()
	{
		return( last_updated );
	}
	
	public String 
	getName() 
	{
		return name;
	}
	
	public boolean
	isSelected()
	{
		return( selected );
	}
	
	public void
	setSelected(
		boolean		b )
	{
		if ( b != selected ){
		
			selected	= b;
			
			selection_state_recorded = false;
			
			configDirty();
		}
	}
	
	public boolean
	isSelectionStateRecorded()
	{
		return( selection_state_recorded );
	}
	
	public void
	setSelectionStateRecorded()
	{
		selection_state_recorded = true;
		
		configDirty();
	}
	
	public int
	getSource()
	{
		return( source );
	}
	
	public void
	setSource(
		int		_source )
	{
		source	= _source;
		
		configDirty();
	}
	
	protected void
	configDirty()
	{
		if ( meta_search != null ){
			
			meta_search.configDirty();
		}
	}
	
	protected void
	debugLog(
		String		str )
	{
		if ( id == 3 ){
			
			log( str );
		}
	}
	
	protected void
	log(
		String		str )
	{
		if ( meta_search != null ){
		
			meta_search.log( "Engine " + getId() + ": " + str );
		}
	}
	
	protected void
	log(
		String		str,
		Throwable	e )
	{
		if ( meta_search != null ){
		
			meta_search.log( "Engine " + getId() + ": " + str, e );
		}
	}
}
