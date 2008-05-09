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
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;

import com.aelitis.azureus.core.messenger.config.PlatformMetaSearchMessenger;
import com.aelitis.azureus.core.metasearch.Engine;
import com.aelitis.azureus.core.metasearch.EngineFactory;
import com.aelitis.azureus.core.metasearch.MetaSearch;
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
	
	private AsyncDispatcher	dispatcher = new AsyncDispatcher( 10000 );
	
	protected
	MetaSearchManagerImpl()
	{
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
						
						refreshSupport();
					}
				}
			});
	}
	
	protected void
	refreshSupport()
	{
		log( "Refreshing engines" );
				
			// featured templates are always shown - can't be deselected
			// popular ones are selected if in 'auto' mode - can't be deselected
			// manually selected ones are, well, manually selected
		
		Map		selected_ids 		= new HashMap();
		
		Set		featured_ids 		= new HashSet();
		Set		popular_ids 		= new HashSet();
		
		boolean		auto_mode = isAutoMode();
		
		MetaSearch meta_search = getMetaSearch();
		
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
				
				selected_ids.put( 
					key, 
					new Object[]{ new Long( template.getModifiedDate()), new Long( Engine.ENGINE_SOURCE_FEATURED )});
				
				featured_ids.add( key );
				
				featured_str += (featured_str.length()==0?"":",") + key;
			}
				
			log( "Featured templates: " + featured_str );
			
			if ( auto_mode ){
				
				PlatformMetaSearchMessenger.templateInfo[] popular = PlatformMetaSearchMessenger.listPopularTemplates();
				
				String popular_str = "";
				
				for (int i=0;i<popular.length;i++){
					
					PlatformMetaSearchMessenger.templateInfo template = popular[i];
					
					if ( !template.isVisible()){
						
						continue;
					}
					
					Long	key = new Long( template.getId());
					
					if ( !selected_ids.containsKey( key )){
						
						selected_ids.put( 
							key, 
							new Object[]{ new Long( template.getModifiedDate()), new Long( Engine.ENGINE_SOURCE_POPULAR )});
						
						popular_ids.add( key );
						
						popular_str += (popular_str.length()==0?"":",") + key;
					}
				}
				
				log( "Popular templates: " + popular_str );

			}else{
					
				String manual_str = "";
				
				for (int i=0;i<engines.length;i++){
					
					Engine	engine = engines[i];
					
					Long key = new Long( engine.getId());
					
					if ( engine.isSelected() && !selected_ids.containsKey( key )){
					
						selected_ids.put( 
							key, 
							new Object[]{ new Long( engine.getLastUpdated()), new Long( Engine.ENGINE_SOURCE_MANUAL )});
						
						manual_str += (manual_str.length()==0?"":",") + key;
					}
				}
				
				log( "Manual templates: " + manual_str );

			}
			
			Map existing_engine_map = new HashMap();
			
			String existing_str = "";
			
			for (int i=0;i<engines.length;i++){
				
				Engine	engine = engines[i];
				
				Long key = new Long( engine.getId());

				existing_engine_map.put( key, engine );
				
				existing_str += (existing_str.length()==0?"":",") + key + 
									"[source=" + engine.getSource() +
									",type=" + engine.getType() + 
									",selected=" + engine.isSelected() + "]";
			}
			
			log( "Existing templates: " + existing_str );
			
				// we've compiled a list of the engines we should have and their latest dates
			
				// update any that are out of date
			
			Iterator it = selected_ids.entrySet().iterator();
			
			while( it.hasNext()){
				
				Map.Entry entry = (Map.Entry)it.next();
				
				long		id 		= ((Long)entry.getKey()).longValue();
				Object[]	value 	= (Object[])entry.getValue();
				
				long	modified 	= ((Long)value[0]).longValue();
				int		source		= ((Long)value[1]).intValue();
				
				Engine existing = (Engine)existing_engine_map.get( new Long(id));
				
				boolean	update = existing == null || existing.getLastUpdated() < modified;

				if ( update ){
					
					log( "Downloading definition of template " + id );
					
					PlatformMetaSearchMessenger.templateDetails details = PlatformMetaSearchMessenger.getTemplate( id );
					
					if ( details.isVisible()){
						
						try{
							Engine e = 
								EngineFactory.importFromJSONString( 
									details.getType()==PlatformMetaSearchMessenger.templateDetails.ENGINE_TYPE_JSON?Engine.ENGINE_TYPE_JSON:Engine.ENGINE_TYPE_REGEX,
									details.getId(),
									details.getModifiedDate(),
									details.getName(),
									details.getValue());
							
							meta_search.addEngine( e );
							
						}catch( Throwable e ){
							
							log( "Failed to import engine '" + details.getValue() + "'", e );
						}
					}else{
						
						if ( existing != null ){
							
							existing.setSelected( false );
						}
					}
				}else{
					
						// ensure we attribute to latest source
					
					existing.setSource( source );
				}
			}
			
				// deselect any not in use
			
			for (int i=0;i<engines.length;i++){
				
				Engine	engine = engines[i];
				
				if ( !selected_ids.containsKey( new Long( engine.getId()))){
					
					engine.setSelected( false );
					
						// update selected state if not featured/popular
					
					if ( 	engine.getSource() == Engine.ENGINE_SOURCE_MANUAL && 
							!engine.isSelectionStateRecorded()){
						
						log( "Marking template id " + engine.getId() + " as selected=" + engine.isSelected());
						
						PlatformMetaSearchMessenger.setTemplatetSelected(
								engine.getId(), Constants.AZID, engine.isSelected());
						
						engine.setSelectionStateRecorded();
					}
				}
			}
			
				// finally pick up any unreported selection changes and re-affirm positive selections
			
			for (int i=0;i<engines.length;i++){
				
				Engine	engine = engines[i];
				
					// only ever report selection state for manual engines
				
				if ( engine.getSource() == Engine.ENGINE_SOURCE_MANUAL ){
					
					boolean	selected = engine.isSelected();
					
					if ( selected || !engine.isSelectionStateRecorded()){
					
						log( "Marking template id " + engine.getId() + " as selected=" + engine.isSelected());

						PlatformMetaSearchMessenger.setTemplatetSelected(
							engine.getId(), Constants.AZID, engine.isSelected());
						
						engine.setSelectionStateRecorded();
					}
				}
			}
		}catch( Throwable e ){
			
			log( "Refresh failed", e );
		}
	}
	
	public MetaSearch 
	getMetaSearch() 
	{
		return( MetaSearchImpl.getSingleton());
	}
	
	public boolean
	isAutoMode()
	{
		return( COConfigurationManager.getBooleanParameter( "metasearch.auto.mode", true ));
	}
	
	public void
	setAutoMode(
		boolean		auto )
	{
		if ( auto != isAutoMode()){
		
			COConfigurationManager.setParameter( "metasearch.auto.mode", auto );
			
			refresh();
		}
	}
	
	public void 
	listPopularTemplates() 
	{
		try{
			PlatformMetaSearchMessenger.listPopularTemplates();
			
		}catch( Throwable e ){
			
			e.printStackTrace();
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
