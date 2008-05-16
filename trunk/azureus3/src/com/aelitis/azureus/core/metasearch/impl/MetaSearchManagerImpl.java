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

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.AEDiagnostics;
import org.gudy.azureus2.core3.util.AEDiagnosticsLogger;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;

import com.aelitis.azureus.core.messenger.PlatformMessengerException;
import com.aelitis.azureus.core.messenger.config.PlatformMetaSearchMessenger;
import com.aelitis.azureus.core.metasearch.Engine;
import com.aelitis.azureus.core.metasearch.MetaSearch;
import com.aelitis.azureus.core.metasearch.MetaSearchException;
import com.aelitis.azureus.core.metasearch.MetaSearchManager;
import com.aelitis.azureus.util.Constants;

public class 
MetaSearchManagerImpl
	implements MetaSearchManager
{
	private static final String	LOGGER_NAME = "MetaSearch";
	
	private static final int REFRESH_MILLIS = 23*60*60*1000;
	
	private static final MetaSearchManager	singleton = new MetaSearchManagerImpl();
	
	public static MetaSearchManager
	getSingleton()
	{
		return( singleton );
	}
	
	private MetaSearchImpl	meta_search;
	private AsyncDispatcher	dispatcher = new AsyncDispatcher( 10000 );
	
	private AESemaphore	refresh_sem = new AESemaphore( "MetaSearch:refresh", 1 );
	
	protected
	MetaSearchManagerImpl()
	{
		meta_search = new MetaSearchImpl();
		
		SimpleTimer.addPeriodicEvent(
			"MetaSearchRefresh",
			REFRESH_MILLIS,
			new TimerEventPerformer()
			{
				public void 
				perform(
					TimerEvent 	event ) 
				{
					refresh();
				}
			});
		
		refresh();
	}
	
	protected void
	refresh()
	{
		dispatcher.dispatch(
			new AERunnable()
			{
				public void 
				runSupport() 
				{
					if ( dispatcher.getQueueSize() == 0 ){
						
						try{
							syncRefresh();
							
						}catch( Throwable e ){
							
						}
					}
				}
			});
	}
	
	protected void
	syncRefresh()
	
		throws MetaSearchException
	{
		try{
			refresh_sem.reserve();
			
			log( "Refreshing engines" );
					
				// featured templates are always shown - can't be deselected
				// popular ones are selected if in 'auto' mode
				// manually selected ones are, well, manually selected
			
			Map		vuze_selected_ids 		= new HashMap();
			
			Set		featured_ids 			= new HashSet();
			Set		popular_ids 			= new HashSet();
			
			boolean		auto_mode = isAutoMode();
					
			Engine[]	engines = meta_search.getEngines( false );
	
			try{
				PlatformMetaSearchMessenger.templateInfo[] featured = PlatformMetaSearchMessenger.listFeaturedTemplates();
				
				String featured_str = "";
				
				for (int i=0;i<featured.length;i++){
					
					PlatformMetaSearchMessenger.templateInfo template = featured[i];
					
					if ( !template.isVisible()){
						
						continue;
					}
					
					Long key = new Long( template.getId());
					
					vuze_selected_ids.put( 
						key, 
						new Long( template.getModifiedDate()));
					
					featured_ids.add( key );
					
					featured_str += (featured_str.length()==0?"":",") + key;
				}
					
				log( "Featured templates: " + featured_str );
				
				if ( auto_mode ){
					
					PlatformMetaSearchMessenger.templateInfo[] popular = PlatformMetaSearchMessenger.listTopPopularTemplates();
					
					String popular_str = "";
					
					for (int i=0;i<popular.length;i++){
						
						PlatformMetaSearchMessenger.templateInfo template = popular[i];
						
						if ( !template.isVisible()){
							
							continue;
						}
						
						Long	key = new Long( template.getId());
						
						if ( !vuze_selected_ids.containsKey( key )){
							
							vuze_selected_ids.put( 
								key, 
								new Long( template.getModifiedDate()));
							
							popular_ids.add( key );
							
							popular_str += (popular_str.length()==0?"":",") + key;
						}
					}
					
					log( "Popular templates: " + popular_str );
				}
						
					// pick up explicitly selected vuze ones
				
				String manual_str = "";
				
				for (int i=0;i<engines.length;i++){
					
					Engine	engine = engines[i];
					
					Long key = new Long( engine.getId());
					
					if ( 	engine.getSource() == Engine.ENGINE_SOURCE_VUZE &&
							engine.getSelectionState() == Engine.SEL_STATE_MANUAL_SELECTED &&
							!vuze_selected_ids.containsKey( key )){
					
						vuze_selected_ids.put( 
							key, 
							new Long( engine.getLastUpdated()));
						
						manual_str += (manual_str.length()==0?"":",") + key;
					}
				}
				
				log( "Manual templates: " + manual_str );
				
				Map existing_engine_map = new HashMap();
				
				String existing_str = "";
				
				for (int i=0;i<engines.length;i++){
					
					Engine	engine = engines[i];
					
					Long key = new Long( engine.getId());
	
					existing_engine_map.put( key, engine );
					
					existing_str += (existing_str.length()==0?"":",") + key + 
										"[source=" + Engine.ENGINE_SOURCE_STRS[engine.getSource()] +
										",type=" + engine.getType() + 
										",selected=" + Engine.SEL_STATE_STRINGS[engine.getSelectionState()] + "]";
				}
				
				log( "Existing templates: " + existing_str );
				
					// we've compiled a list of the engines we should have and their latest dates
				
					// update any that are out of date
				
				Iterator it = vuze_selected_ids.entrySet().iterator();
				
				while( it.hasNext()){
					
					Map.Entry entry = (Map.Entry)it.next();
					
					long	id 			= ((Long)entry.getKey()).longValue();
					long	modified 	= ((Long)entry.getValue()).longValue();
									
					Engine this_engine = (Engine)existing_engine_map.get( new Long(id));
					
					boolean	update = this_engine == null || this_engine.getLastUpdated() < modified;
	
					if ( update ){
						
						PlatformMetaSearchMessenger.templateDetails details = PlatformMetaSearchMessenger.getTemplate( id );
	
						log( "Downloading definition of template " + id );
						log( details.getValue());
						
						if ( details.isVisible()){
							
							try{
								this_engine = 
									meta_search.importFromJSONString( 
										details.getType()==PlatformMetaSearchMessenger.templateDetails.ENGINE_TYPE_JSON?Engine.ENGINE_TYPE_JSON:Engine.ENGINE_TYPE_REGEX,
										details.getId(),
										details.getModifiedDate(),
										details.getName(),
										details.getValue());
								
								this_engine.setSource( Engine.ENGINE_SOURCE_VUZE );
								
								meta_search.addEngine( this_engine );
								
							}catch( Throwable e ){
								
								log( "Failed to import engine '" + details.getValue() + "'", e );
							}
						}
				
					}else{
						
						log( "Not updating " + this_engine.getString() + " as unchanged" );
					}
					
					if ( this_engine.getSelectionState() == Engine.SEL_STATE_DESELECTED ){
						
						this_engine.setSelectionState( Engine.SEL_STATE_AUTO_SELECTED );
					}
				}
				
					// deselect any not in use
				
				for (int i=0;i<engines.length;i++){
					
					Engine	engine = engines[i];
					
					if ( 	engine.getSource() == Engine.ENGINE_SOURCE_VUZE &&
							engine.getSelectionState() == Engine.SEL_STATE_AUTO_SELECTED &&
							!vuze_selected_ids.containsKey( new Long( engine.getId()))){
						
						engine.setSelectionState( Engine.SEL_STATE_DESELECTED );
					}
				}
				
					// finally pick up any unreported selection changes and re-affirm positive selections
				
				for (int i=0;i<engines.length;i++){
					
					Engine	engine = engines[i];
					
					engine.checkSelectionStateRecorded();
				}
			}catch( Throwable e ){
				
				log( "Refresh failed", e );
				
				throw( new MetaSearchException( "Refresh failed", e ));
			}
		}finally{
			
			refresh_sem.release();
		}
	}
	
	public MetaSearch 
	getMetaSearch() 
	{
		return( meta_search );
	}
	
	public boolean
	isAutoMode()
	{
		return( COConfigurationManager.getBooleanParameter( "metasearch.auto.mode", true ));
	}
	
	public void
	setSelectedEngines(
		long[]		ids,
		boolean		auto )
	
		throws MetaSearchException
	{
		try{
			String	s = "";
			
			for (int i=0;i<ids.length;i++){
				
				s += (i==0?"":",") + ids[i];
			}
			
			log( "setSelectedIds: " + s + ", auto=" + auto );
			
				// first update state of auto and existing engines 
			
			COConfigurationManager.setParameter( "metasearch.auto.mode", auto );

			Engine[]	engines = meta_search.getEngines( false );
			
			Map	engine_map = new HashMap();
			
			for( int i=0;i<engines.length;i++){
				
				engine_map.put( new Long( engines[i].getId()), engines[i] );
			}
				
			Set selected_engine_set = new HashSet();
			
			for (int i=0;i<ids.length;i++){
				
				long	 id = ids[i];
				
				Engine existing = (Engine)engine_map.get(new Long(id));
				
				if ( existing != null ){
					
					existing.setSelectionState( Engine.SEL_STATE_MANUAL_SELECTED );
					
					selected_engine_set.add( existing );
				}
			}
		
				// now refresh - this will pick up latest state of things
			
			syncRefresh();
			
			engines = meta_search.getEngines( false );

				// next add in any missing engines
			
			for (int i=0;i<ids.length;i++){
				
				long	 id = ids[i];
				
				Engine existing = (Engine)engine_map.get(new Long(id));
				
				if ( existing == null ){
														
					PlatformMetaSearchMessenger.templateDetails details = PlatformMetaSearchMessenger.getTemplate( id );
	
					log( "Downloading definition of template " + id );
					log( details.getValue());
					
					Engine new_engine = 
						meta_search.importFromJSONString( 
							details.getType()==PlatformMetaSearchMessenger.templateDetails.ENGINE_TYPE_JSON?Engine.ENGINE_TYPE_JSON:Engine.ENGINE_TYPE_REGEX,
							details.getId(),
							details.getModifiedDate(),
							details.getName(),
							details.getValue());
							
					new_engine.setSelectionState( Engine.SEL_STATE_MANUAL_SELECTED );
										
					new_engine.setSource( Engine.ENGINE_SOURCE_VUZE );
						
					meta_search.addEngine( new_engine );
					
					selected_engine_set.add( new_engine );
				}
			}
			
				// deselect any existing manually selected ones that are no longer selected
			
			for( int i=0;i<engines.length;i++){

				Engine e = engines[i];
				
				if ( e.getSelectionState() == Engine.SEL_STATE_MANUAL_SELECTED ){
					
					if ( !selected_engine_set.contains( e )){
				
						e.setSelectionState( Engine.SEL_STATE_DESELECTED  );
					}
				}
			}
		}catch( Throwable e ){
			
			if ( e instanceof MetaSearchException ){
				
				throw((MetaSearchException)e);
			}
			
			throw( new MetaSearchException( "Failed to set selected engines", e ));
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
	

}
