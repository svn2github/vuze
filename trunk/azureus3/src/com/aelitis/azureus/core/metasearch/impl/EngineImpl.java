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
import java.net.URLDecoder;
import java.util.*;


import org.gudy.azureus2.core3.util.AEDiagnostics;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.aelitis.azureus.core.messenger.config.PlatformMetaSearchMessenger;
import com.aelitis.azureus.core.metasearch.Engine;
import com.aelitis.azureus.core.metasearch.Result;
import com.aelitis.azureus.core.metasearch.ResultListener;
import com.aelitis.azureus.core.metasearch.SearchException;
import com.aelitis.azureus.core.metasearch.SearchLoginException;
import com.aelitis.azureus.core.metasearch.SearchParameter;
import com.aelitis.azureus.core.metasearch.impl.plugin.PluginEngine;
import com.aelitis.azureus.core.metasearch.impl.web.json.JSONEngine;
import com.aelitis.azureus.core.metasearch.impl.web.regex.RegexEngine;
import com.aelitis.azureus.core.metasearch.impl.web.rss.RSSEngine;
import com.aelitis.azureus.core.vuzefile.VuzeFile;
import com.aelitis.azureus.core.vuzefile.VuzeFileComponent;
import com.aelitis.azureus.core.vuzefile.VuzeFileHandler;
import com.aelitis.azureus.util.Constants;
import com.aelitis.azureus.util.ImportExportUtils;
import com.aelitis.azureus.util.JSONUtils;


public abstract class 
EngineImpl
	implements Engine
{
	protected static EngineImpl
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
			
		}else if ( type == Engine.ENGINE_TYPE_PLUGIN ){
			
			return( PluginEngine.importFromBEncodedMap( meta_search, map ));
			
		}else if ( type == Engine.ENGINE_TYPE_RSS ){
			
			return( RSSEngine.importFromBEncodedMap( meta_search, map ));
			
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
		JSONObject map = (JSONObject)JSONUtils.decodeJSON( content );
		
		if ( type == Engine.ENGINE_TYPE_JSON ){
			
			return( JSONEngine.importFromJSONString( meta_search, id, last_updated, name, map ));
			
		}else if ( type == Engine.ENGINE_TYPE_REGEX ){
			
			return( RegexEngine.importFromJSONString( meta_search, id, last_updated, name, map ));
			
		}else if ( type == Engine.ENGINE_TYPE_RSS ){
			
			return( RSSEngine.importFromJSONString( meta_search, id, last_updated, name, map ));
			
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
	
		// first mappings used to canonicalise names and map field to same field
		// typically used for categories (musak->music)
	
	List		first_level_mapping	= new ArrayList();
	
		// second mappings used to generate derived field values
		// typically used to derive content_type from category (music->AUDIO)
	
	List		second_level_mapping	= new ArrayList();

		// manual constructor
	
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
	
		// bencoded constructor
	
	protected 
	EngineImpl(
		MetaSearchImpl	_meta_search,
		Map				map )
	
		throws IOException
	{
		meta_search		= _meta_search;
		
		type			= ((Long)map.get( "type" )).intValue();
		id				= ((Long)map.get( "id")).longValue();
		last_updated	= ImportExportUtils.importLong( map, "last_updated" );
		name			= ImportExportUtils.importString( map, "name" );
		
		selection_state	= (int)ImportExportUtils.importLong( map, "selected", SEL_STATE_DESELECTED );
		
		selection_state_recorded = ImportExportUtils.importBoolean(map,"select_rec", true );
		
		source			= (int)ImportExportUtils.importLong( map, "source", ENGINE_SOURCE_UNKNOWN );
		
		first_level_mapping 	= importBEncodedMappings( map, "l1_map" );
		second_level_mapping 	= importBEncodedMappings( map, "l2_map" );
	}
	
		// bencoded export
	
	protected void
	exportToBencodedMap(
		Map		map )
	
		throws IOException
	{
		map.put( "type", new Long( type ));
		map.put( "id", new Long( id ));
		map.put( "last_updated", new Long( last_updated ));
		
		ImportExportUtils.exportString( map, "name", name );
		
		map.put( "selected", new Long( selection_state ));
		
		ImportExportUtils.exportBoolean( map, "select_rec", selection_state_recorded );
		
		map.put( "source", new Long( source ));
		
		exportBEncodedMappings( map, "l1_map", first_level_mapping );
		exportBEncodedMappings( map, "l2_map", second_level_mapping );
	}
	
		// json constructor
	
	protected 
	EngineImpl(
		MetaSearchImpl	meta_search,
		int				type,
		long			id,
		long			last_updated,
		String			name,
		JSONObject		map )
	
		throws IOException
	{
		this( meta_search, type, id, last_updated, name );
		
		first_level_mapping 	= importJSONMappings( map, "value_map", true );
		second_level_mapping 	= importJSONMappings( map, "ctype_map", false );
	}
	
		// json export
	
	protected void
	exportToJSONObject(
		JSONObject		res )
	
		throws IOException
	{
		exportJSONMappings( res, "value_map", first_level_mapping, true );
		exportJSONMappings( res, "ctype_map", second_level_mapping, false );
	}
	
	protected List
	importJSONMappings(
		JSONObject		map,
		String			str,
		boolean			level_1 )
	
		throws IOException
	{
		List	result = new ArrayList();
			
		JSONObject	field_map = (JSONObject)map.get( str );
		
		if ( field_map != null ){
			
			Iterator	it = field_map.entrySet().iterator();
			
			while( it.hasNext()){
				
				Map.Entry	entry = (Map.Entry)it.next();
				
				String	key 		= (String)entry.getKey();
				List	mappings 	= (List)entry.getValue();
				
					// limited support for the moment: 
					//		level one always maps to same field
					//		level two always maps to content type
				
				int	from_field 	= vuzeFieldToID( key );
				
				if ( from_field == -1 ){
					
					log( "Unrecognised remapping key '" + key + "'" );
					
					continue;
				
				}
				
				int	to_field	= level_1?from_field:FIELD_CONTENT_TYPE;
				
				List	frs_l = new ArrayList();
				
				for (int i=0;i<mappings.size();i++){
					
					JSONObject mapping = (JSONObject)mappings.get(i);
					
					String	from_str 	= URLDecoder.decode((String)mapping.get( level_1?"from_string":"cat_string" ),"UTF-8");

					if ( from_str == null ){
						
						log( "'from' value missing in " + mapping );
						
						continue;
					}
					
					from_str 	= URLDecoder.decode( from_str, "UTF-8" );
					
					String	to_str 		= URLDecoder.decode((String)mapping.get( level_1?"to_string":"media_type" ),"UTF-8");
					
					if ( to_str == null ){
						
						log( "'to' value missing in " + mapping );
						
						continue;
					}
					
					frs_l.add( new FieldRemapping( from_str, to_str ));
				}
				
				FieldRemapping[] frs = (FieldRemapping[])frs_l.toArray( new FieldRemapping[frs_l.size()]);

				result.add( new FieldRemapper( from_field, to_field, frs ));
			}
		}
		
		return( result );
	}
	
	protected void
	exportJSONMappings(
		JSONObject			res,
		String				str,
		List				l,
		boolean				level_1 )
	{
		JSONObject	field_map = new JSONObject();
		
		res.put( str, field_map );
		
		for (int i=0;i<l.size();i++){
			
			FieldRemapper remapper = (FieldRemapper)l.get(i);
			
			int	from_field	= remapper.getInField();
			//int	to_field	= remapper.getOutField();
			
			String from_field_str = vuzeIDToField( from_field );
			
			JSONArray	mappings = new JSONArray();
			
			field_map.put( from_field_str, mappings );
			
			FieldRemapping[] frs = remapper.getMappings();
			
			for (int j=0;j<frs.length;j++){
				
				FieldRemapping fr = frs[j];
				
				String from_str = UrlUtils.encode( fr.getMatchString());
				
				String to_str	= fr.getReplacement();
				
				JSONObject map = new JSONObject();
				
				mappings.add( map );
				
				map.put( level_1?"from_string":"cat_string", from_str );
				map.put( level_1?"to_string":"media_type", to_str );
			}
		}
	}
			
	protected List
	importBEncodedMappings(
		Map		map,
		String	name )
	
		throws IOException
	{
		List	result = new ArrayList();
		
		List	l = (List)map.get(name);
		
		if ( l != null ){
			
			for (int i=0;i<l.size();i++){
				
				Map	entry = (Map)l.get(i);
				
				int	from_field 	= ((Long)entry.get( "from" )).intValue();
				int	to_field 	= ((Long)entry.get( "to" )).intValue();
				
				List	l2 = (List)entry.get( "maps" );
				
				FieldRemapping[]	mappings = new FieldRemapping[ l2.size() ];
				
				for (int j=0;j<mappings.length;j++){
					
					Map	entry2 = (Map)l2.get(j);
					
					String	from_str 	= ImportExportUtils.importString( entry2, "from" );
					String	to_str 		= ImportExportUtils.importString( entry2, "to" );
					
					mappings[j] = new FieldRemapping( from_str, to_str );
				}
				
				result.add( new FieldRemapper( from_field, to_field, mappings ));
			}
		}
		
		return( result );
	}
	
	protected void
	exportBEncodedMappings(
		Map		map,
		String	name,
		List	mappings )
	
		throws IOException
	{
		List	l = new ArrayList();
		
		map.put( name, l );
		
		for ( int i=0;i<mappings.size();i++){
			
			FieldRemapper mapper = (FieldRemapper)mappings.get(i);
			
			Map m = new HashMap();
			
			l.add( m );
			
			m.put( "from", new Long( mapper.getInField()));
			m.put( "to", new Long( mapper.getOutField()));
			
			List l2 = new ArrayList();
			
			m.put( "maps", l2 );
			
			FieldRemapping[] frs = mapper.getMappings();
			
			for (int j=0;j<frs.length;j++){
				
				FieldRemapping fr = frs[j];
				
				Map m2 = new HashMap();
				
				l2.add( m2 );
				
				ImportExportUtils.exportString( m2, "from", fr.getMatchString());
				ImportExportUtils.exportString( m2, "to", fr.getReplacement());
			}
		}
	}
	
	public String
	exportToJSONString()
	
		throws IOException
	{
		JSONObject	obj = new JSONObject();
		
		exportToJSONObject( obj );
		
		return( obj.toString());
	}
	

	public boolean
	sameAs(
		Engine	other )
	{
		try{
			Map	m1 = exportToBencodedMap();
			Map	m2 = other.exportToBencodedMap();
		
			String[]	to_remove = {"type","id","last_updated","selected","select_rec","source"};
			
			for (int i=0;i<to_remove.length;i++){
				
				m1.remove( to_remove[i] );
				m2.remove( to_remove[i] );
			}
			
			return( BEncoder.mapsAreIdentical( m1, m2 ));
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
			
			return( false );
		}
	}
	
	public Result[]
	search(
		SearchParameter[] 	params )
	
		throws SearchException
	{
		return( searchAndMap( params, -1, null, null ));
	}
	
	public Result[]
  	search(
  		SearchParameter[] 	params,
  		String				headers )
  	
  		throws SearchException
  	{
		return( searchAndMap( params, -1, headers, null ));
  	}
	
	public void
	search(
		SearchParameter[] 	params,
		int					max_matches,
		String				headers,
		ResultListener		listener )
	{
		try{
			Result[] results = searchAndMap( params, max_matches, headers, listener) ;
			
			listener.resultsReceived( this, results );
			
			listener.resultsComplete( this );
			
		}catch( Throwable e ){
			if(e instanceof SearchLoginException) {
				listener.engineRequiresLogin(this, e);
			} else {
				listener.engineFailed( this, e);
			}
			
		}
	}
	
	protected Result[]
	searchAndMap(
		SearchParameter[] 			params,
		int							max_matches,
		String						headers,
		final ResultListener		listener )
	
		throws SearchException
	{
		 Result[] results = 
			 searchSupport( 
					params, 
					max_matches, 
					headers, 
					new ResultListener()
					{
						public void 
						contentReceived(
							Engine 		engine, 
							String 		content )
						{
							if ( listener != null ){
								listener.contentReceived(engine, content);
							}
						}
					
						public void 
						matchFound( 
							Engine 		engine, 
							String[] 	fields )
						{
							if ( listener != null ){
								listener.matchFound(engine, fields);
							}
						}
						
						public void 
						resultsReceived(
							Engine 		engine,
							Result[] 	results)
						{
							if ( listener != null ){
								listener.resultsReceived(engine, mapResults( results ));
							}
						}
						
						public void 
						resultsComplete(
							Engine engine )
						{
							if ( listener != null ){
								listener.resultsComplete(engine);
							}
						}
						
						public void 
						engineFailed(
							Engine engine, 
							Throwable cause )
						{
							log( "Search failed", cause );
							
							if ( listener != null ){
								listener.engineFailed(engine, cause);
							}
						}
						
						public void 
						engineRequiresLogin(
							Engine engine, 
							Throwable cause )
						{
							log( "Search requires login", cause );
							
							if ( listener != null ){
								listener.engineRequiresLogin(engine, cause);
							}
						}
					});
		 
		 return( mapResults( results ));
	}
	
	protected Result[]
	mapResults(
		Result[]	results )
	{
		for (int i=0;i<results.length;i++){
			
			Result result = results[i];
			
			for (int j=0;j<first_level_mapping.size();j++){
				
				FieldRemapper mapper = (FieldRemapper)first_level_mapping.get(j);
				
				mapper.remap( result );
			}
			
			for (int j=0;j<second_level_mapping.size();j++){
				
				FieldRemapper mapper = (FieldRemapper)second_level_mapping.get(j);
				
				mapper.remap( result );
			}
		}
		
		return( results );
	}
	
	protected abstract Result[]
	searchSupport(
		SearchParameter[] 	params,
		int					max_matches,
		String				headers,
		ResultListener		listener )
	
		throws SearchException;
	
	public void
	delete()
	{
		meta_search.removeEngine( this );
	}
	
	protected int
	vuzeFieldToID(
		String	field )
	{
		for (int i=0;i<FIELD_NAMES.length;i++){
			
			if ( field.equalsIgnoreCase( FIELD_NAMES[i] )){
				
				return( FIELD_IDS[i]);
			}
		}
		
		return( -1 );
	}
	
	protected String
	vuzeIDToField(
		int		id )
	{
		for (int i=0;i<FIELD_IDS.length;i++){
			
			if ( id == FIELD_IDS[i] ){
				
				return( FIELD_NAMES[i]);
			}
		}
		
		return( null );
	}

	public int
	getType()
	{
		return( type );
	}
	
	protected void
	setId(
		long		_id )
	{
		id	= _id;
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
	recordSelectionState()
	{
		selection_state_recorded = false;
		
		checkSelectionStateRecorded();
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
	
	public void
	exportToVuzeFile(
		File	target )
	
		throws IOException
	{
		VuzeFile	vf = VuzeFileHandler.getSingleton().create();
		
		vf.addComponent(
			VuzeFileComponent.COMP_TYPE_METASEARCH_TEMPLATE,
			exportToBencodedMap());
		
		vf.write( target );
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
