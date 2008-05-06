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

import java.util.*;

import org.gudy.azureus2.core3.util.AEDiagnostics;
import org.gudy.azureus2.core3.util.AEDiagnosticsLogger;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;

import com.aelitis.azureus.core.metasearch.*;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.util.Constants;

public class 
MetaSearchImpl
	implements MetaSearch
{
	private static final String	LOGGER_NAME = "MetaSearch";
	private static final String	CONFIG_FILE = "metasearch.config";
	
	private static final MetaSearch singleton = new MetaSearchImpl();
	
	private CopyOnWriteList 	engines = new CopyOnWriteList();
	
	public static MetaSearch
	getSingleton()
	{
		return( singleton );
	}
	
	protected 
	MetaSearchImpl() 
	{
		loadConfig();
		
		try{
			Class clazz = Class.forName( "com.aelitis.azureus.core.metasearch.impl.MetaSearchTestImpl" );
		
			clazz.getConstructor( new Class[]{ MetaSearch.class }).newInstance( new Object[]{ this });
			
		}catch( Exception e ){
			
				//Test implementation in progress, Test class not publicly available
			
			e.printStackTrace();
		}
	}
	
	public void 
	addEngine(
		Engine 	engine )
	{
		addEngine( engine, false );
	}
	
	public void 
	addEngine(
		Engine 	engine,
		boolean	loading )
	{
		synchronized( this ){
			
			Iterator	it = engines.iterator();
			
			while( it.hasNext()){
				
				Engine e = (Engine)it.next();
				
				if ( e.getId() == engine.getId()){
					
					log( "Removing old engine with same ID: " + e.getName());
					
					it.remove();
				}
			}
			
			engines.add( engine );
		}
		
		if ( !loading ){
			
			log( "Engine '" + engine.getName() + "' added" );
			
			saveConfig();
		}
	}
	
	public void 
	removeEngine(
		Engine 	engine )
	{
		if ( engines.remove( engine )){
		
			log( "Engine '" + engine.getName() + "' removed" );
			
			saveConfig();
		}
	}
	
	public Engine[] 
	getEngines() 
	{
		List l = engines.getList();
				
		return( (Engine[])l.toArray( new Engine[ l.size() ]));
	}
	
	public void 
	search(
		ResultListener 		listener,
		SearchParameter[] 	searchParameters ) 
	{
		String	str = "";
		
		for (int i=0;i<searchParameters.length;i++){
		
			SearchParameter param = searchParameters[i];
			
			str += (i==0?"":",") + param.getMatchPattern() + "->" + param.getValue();
		}
		
		log( "Search: " + str );
		
		SearchExecuter se = new SearchExecuter(listener);
		
		Iterator it  = engines.iterator();
		
		while( it.hasNext()){
			
			se.search((Engine)it.next(), searchParameters);
		}
	}
	
	public static void 
	log(
		String 		s,
		Throwable 	e )
	{
		AEDiagnosticsLogger diag_logger = AEDiagnostics.getLogger( LOGGER_NAME );
		
		diag_logger.log( s );
		diag_logger.log( e );
		
		if ( Constants.DIAG_TO_STDOUT ){
			
			System.out.println(Thread.currentThread().getName() + "|"
					+ System.currentTimeMillis() + "] " + s + ": " + Debug.getNestedExceptionMessage(e));
		}	
	}
	
	public static void 
	log(
		String 	s )
	{
		AEDiagnosticsLogger diag_logger = AEDiagnostics.getLogger( LOGGER_NAME );
		
		diag_logger.log( s );
		
		if ( Constants.DIAG_TO_STDOUT ){
			
			System.out.println(Thread.currentThread().getName() + "|"
					+ System.currentTimeMillis() + "] " + s);
		}
	}
	
	protected void
	loadConfig()
	{
		log( "Loading configuration" );
		
		synchronized( this ){
			
			Map map = FileUtil.readResilientConfigFile( CONFIG_FILE );
			
			List	l_engines = (List)map.get( "engines" );
			
			if( l_engines != null ){
				
				for (int i=0;i<l_engines.size();i++){
					
					Map	m = (Map)l_engines.get(i);
					
					try{
						Engine e = EngineFactory.importFromBEncodedMap( m );
						
						addEngine( e, true );
						
						log( "    loaded " + e.getName());
						
					}catch( Throwable e ){
						
						log( "Failed to import engine from " + m, e );
					}
				}
			}
		}
	}
	
	protected void
	saveConfig()
	{
		log( "Saving configuration" );
		
		synchronized( this ){
			
			Map map = new HashMap();
			
			List	l_engines = new ArrayList();
			
			map.put( "engines", l_engines );
			
			Iterator	it = engines.iterator();
			
			while( it.hasNext()){
				
				Engine e = (Engine)it.next();
			
				try{
					
					l_engines.add( e.exportToBencodedMap());
					
				}catch( Throwable f ){
					
					log( "Failed to export engine " + e.getName(), f );
				}
			}
			
			FileUtil.writeResilientConfigFile( CONFIG_FILE, map );
		}
	}
}
