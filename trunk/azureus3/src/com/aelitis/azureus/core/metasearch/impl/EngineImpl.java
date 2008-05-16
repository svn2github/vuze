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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import org.gudy.azureus2.core3.util.AEDiagnostics;

import com.aelitis.azureus.core.messenger.config.PlatformMetaSearchMessenger;
import com.aelitis.azureus.core.metasearch.Engine;
import com.aelitis.azureus.core.metasearch.impl.web.json.JSONEngine;
import com.aelitis.azureus.core.metasearch.impl.web.regex.RegexEngine;
import com.aelitis.azureus.util.Constants;
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
	
	
	private int			selection_state				= SEL_STATE_DESELECTED;
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
		
		selection_state	= (int)importLong( map, "selected", SEL_STATE_DESELECTED );
		source			= (int)importLong( map, "source", ENGINE_SOURCE_UNKNOWN );
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
		
		map.put( "selected", new Long( selection_state ));
		
		map.put( "source", new Long( source ));
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
		return( importLong( map, key, 0 ));
	}
	
	protected long
	importLong(
		Map		map,
		String	key,
		long	def )
	
		throws IOException
	{
		Object	obj = map.get( key );
		
		if ( obj instanceof Long){
			
			return(((Long)obj).longValue());
			
		}else if ( obj instanceof String ){
			
			return( Long.parseLong((String)obj));
		}
		
		return( def );
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
	isActive()
	{
		return(	getSelectionState() != SEL_STATE_DESELECTED );
	}
	
	public int
	getSelectionState()
	{
		return( selection_state );
	}
	
	public void
	setSelectionState(
		int		state )
	{
		if ( state != selection_state ){
		
				// only record transitions to or from manual selection for non-local templates
			
			if ( getSource() == ENGINE_SOURCE_VUZE ){
				
				if ( 	state == SEL_STATE_MANUAL_SELECTED || 
						selection_state == SEL_STATE_MANUAL_SELECTED ){
					
					selection_state_recorded = false;
					
					checkSelectionStateRecorded();
				}
			}
			
			selection_state	= state;
						
			configDirty();
		}
	}
	
	public void
	checkSelectionStateRecorded()
	{
		if ( !selection_state_recorded ){
			
			try{
				boolean selected = selection_state != SEL_STATE_DESELECTED;
				
				log( "Marking template id " + getId() + " as selected=" + selected );
				
				PlatformMetaSearchMessenger.setTemplatetSelected( getId(), Constants.AZID, selected);
				
				selection_state_recorded = true;
				
			}catch( Throwable e ){
				
				log( "Failed to record selection state", e );
			}
		}
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
	
	protected File
	getDebugFile()
	{
		return( new File( AEDiagnostics.getLogDir(), "MetaSearch_Engine_" + getId() + ".txt" ));
	}
	
	protected synchronized void
	debugStart()
	{
		getDebugFile().delete();
	}
	
	protected synchronized void
	debugLog(
		String		str )
	{
		File f = getDebugFile();
		
		PrintWriter	 pw = null;
		
		try{
			pw = new PrintWriter(new FileWriter( f, true ));
			
			pw.println( str );
			
		}catch( Throwable e ){
			
		}finally{
			
			if ( pw != null ){
				
				pw.close();
			}
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
	
	public String
	getString()
	{
		return( "id=" + getId() + ", name=" + getName() + ", source=" + ENGINE_SOURCE_STRS[getSource()] + ", selected=" + SEL_STATE_STRINGS[getSelectionState()]);
	}
}
