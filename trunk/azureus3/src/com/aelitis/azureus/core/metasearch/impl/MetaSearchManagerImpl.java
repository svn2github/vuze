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
import java.io.InputStream;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerEvent;
import org.gudy.azureus2.plugins.utils.StaticUtilities;
import org.gudy.azureus2.plugins.utils.search.SearchProvider;
import org.gudy.azureus2.pluginsimpl.local.utils.UtilitiesImpl;

import com.aelitis.azureus.core.custom.Customization;
import com.aelitis.azureus.core.custom.CustomizationManager;
import com.aelitis.azureus.core.custom.CustomizationManagerFactory;
import com.aelitis.azureus.core.messenger.config.PlatformMetaSearchMessenger;
import com.aelitis.azureus.core.metasearch.Engine;
import com.aelitis.azureus.core.metasearch.MetaSearch;
import com.aelitis.azureus.core.metasearch.MetaSearchException;
import com.aelitis.azureus.core.metasearch.MetaSearchManager;
import com.aelitis.azureus.core.vuzefile.VuzeFile;
import com.aelitis.azureus.core.vuzefile.VuzeFileComponent;
import com.aelitis.azureus.core.vuzefile.VuzeFileHandler;
import com.aelitis.azureus.core.vuzefile.VuzeFileProcessor;
import com.aelitis.azureus.util.Constants;

public class 
MetaSearchManagerImpl
	implements MetaSearchManager, UtilitiesImpl.searchManager, AEDiagnosticsEvidenceGenerator
{
	private static final boolean AUTO_MODE_DEFAULT		= false;
	
	
	private static final String	LOGGER_NAME = "MetaSearch";
	
	private static final int REFRESH_MILLIS = 23*60*60*1000;
	
	private static MetaSearchManager singleton;	
	
	public static void
	preInitialise()
	{
		VuzeFileHandler.getSingleton().addProcessor(
			new VuzeFileProcessor()
			{
				public void
				process(
					VuzeFile[]		files,
					int				expected_types )
				{
					for (int i=0;i<files.length;i++){
						
						VuzeFile	vf = files[i];
						
						VuzeFileComponent[] comps = vf.getComponents();
						
						for (int j=0;j<comps.length;j++){
							
							VuzeFileComponent comp = comps[j];
							
							if ( comp.getType() == VuzeFileComponent.COMP_TYPE_METASEARCH_TEMPLATE ){
								
								try{
									Engine e = 
										getSingleton().importEngine(
											comp.getContent(), 
											(expected_types & VuzeFileComponent.COMP_TYPE_METASEARCH_TEMPLATE) == 0 );
									
									comp.setProcessed();
									
									if ( e != null ){
										
										comp.setData( Engine.VUZE_FILE_COMPONENT_ENGINE_KEY, e );
									}
								}catch( Throwable e ){
									
									Debug.printStackTrace(e);
								}
							}
						}
					}
				}
			});		
	}
	
	public static synchronized MetaSearchManager
	getSingleton()
	{
		if ( singleton == null ){
			
			singleton = new MetaSearchManagerImpl();
		}
		return( singleton );
	}
	
	private MetaSearchImpl	meta_search;
	private AsyncDispatcher	dispatcher = new AsyncDispatcher( 10000 );
	
	private AESemaphore	initial_refresh_sem = new AESemaphore( "MetaSearch:initrefresh" );
	
	private AESemaphore	refresh_sem = new AESemaphore( "MetaSearch:refresh", 1 );
	
	private boolean	checked_customization;
	
	protected
	MetaSearchManagerImpl()
	{
		meta_search = new MetaSearchImpl( this );
		
		AEDiagnostics.addEvidenceGenerator( this );
		
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
		
		UtilitiesImpl.addSearchManager( this );
	}
	
	public void 
	addProvider(
		PluginInterface		pi,
		SearchProvider 		provider ) 
	{
		String	id = pi.getPluginID() + "." + provider.getProperty( SearchProvider.PR_NAME );
		
		try{
			meta_search.importFromPlugin( id, provider );
			
		}catch( Throwable e ){
			
			Debug.out( "Failed to add search provider '" + id + "' (" + provider + ")", e );
		}
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
	ensureEnginesUpToDate()
	{
		long timeout = meta_search.getEngineCount() == 0?(30*1000):(10*1000);
		
		if ( !initial_refresh_sem.reserve( timeout )){
			
			log( "Timeout waiting for initial refresh to complete, continuing" );
		}
	}
	
	protected void
	syncRefresh()
	
		throws MetaSearchException
	{
		boolean refresh_completed 	= false;
		boolean	first_run			= false;
		
		try{
			refresh_sem.reserve();
			
			first_run = COConfigurationManager.getBooleanParameter( "metasearch.refresh.first_run", true );
			
			if ( !checked_customization ){
				
				checked_customization = true;
				
				CustomizationManager cust_man = CustomizationManagerFactory.getSingleton();
				
				Customization cust = cust_man.getActiveCustomization();
				
				if ( cust != null ){
					
					String cust_name 	= COConfigurationManager.getStringParameter( "metasearch.custom.name", "" );
					String cust_version = COConfigurationManager.getStringParameter( "metasearch.custom.version", "0" );
					
					boolean	new_name 	= !cust_name.equals( cust.getName());
					boolean	new_version = org.gudy.azureus2.core3.util.Constants.compareVersions( cust_version, cust.getVersion() ) < 0;
					
					if ( new_name || new_version ){

						log( "Customization: checking templates for " + cust.getName() + "/" + cust.getVersion());
						
						try{
							InputStream[] streams = cust.getResources( Customization.RT_META_SEARCH_TEMPLATES );
							
							if ( streams.length > 0 && new_name ){
								
									// reset engines
								
								log( "    setting auto-mode to false" );
								
								setAutoMode( false );
								
								/*
								Engine[]	engines = meta_search.getEngines( false, false );

								for (int i=0;i<engines.length;i++){
									
									Engine engine = engines[i];
									
									if ( engine.getSelectionState()) == Engine.SEL_STATE_MANUAL_SELECTED ){
										
									}
								}
								*/
							}
							for (int i=0;i<streams.length;i++){
								
								InputStream is = streams[i];
								
								try{
									VuzeFile vf = VuzeFileHandler.getSingleton().loadVuzeFile(is);
									
									if ( vf != null ){
										
										VuzeFileComponent[] comps = vf.getComponents();
										
										for (int j=0;j<comps.length;j++){
											
											VuzeFileComponent comp = comps[j];
											
											if ( comp.getType() == VuzeFileComponent.COMP_TYPE_METASEARCH_TEMPLATE ){
												
												try{
													Engine e = 
														getSingleton().importEngine( comp.getContent(), false ); 
													
													log( "    updated " + e.getName());
													
													e.setSelectionState( Engine.SEL_STATE_MANUAL_SELECTED );
													
												}catch( Throwable e ){
													
													Debug.printStackTrace(e);
												}
											}
										}
									}
								}finally{
									
									try{
										is.close();
										
									}catch( Throwable e ){
									}
								}
							}
						}finally{
							
							COConfigurationManager.setParameter( "metasearch.custom.name", cust.getName());
							COConfigurationManager.setParameter( "metasearch.custom.version", cust.getVersion());
						}
					}
				}
			}
			
			log( "Refreshing engines" );
					
				// featured templates are always shown - can't be deselected
				// popular ones are selected if in 'auto' mode
				// manually selected ones are, well, manually selected
			
			Map		vuze_selected_ids 		= new HashMap();
			Map		vuze_preload_ids 		= new HashMap();
			
			Set		featured_ids 			= new HashSet();
			Set		popular_ids 			= new HashSet();
			Set		manual_vuze_ids 		= new HashSet();
			
			boolean		auto_mode = isAutoMode();
					
			Engine[]	engines = meta_search.getEngines( false, false );
	
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
				
				if ( auto_mode || first_run ){
					
					PlatformMetaSearchMessenger.templateInfo[] popular = PlatformMetaSearchMessenger.listTopPopularTemplates();
					
					String popular_str = "";
					String preload_str = "";
					
					for (int i=0;i<popular.length;i++){
						
						PlatformMetaSearchMessenger.templateInfo template = popular[i];
						
						if ( !template.isVisible()){
							
							continue;
						}
						
						Long	key = new Long( template.getId());
						
						if ( auto_mode ){
							
							if ( !vuze_selected_ids.containsKey( key )){
								
								vuze_selected_ids.put( 
									key, 
									new Long( template.getModifiedDate()));
								
								popular_ids.add( key );
								
								popular_str += (popular_str.length()==0?"":",") + key;
							}
						}else{
							
							if ( !vuze_preload_ids.containsKey( key )){
								
								vuze_preload_ids.put( 
									key, 
									new Long( template.getModifiedDate()));
																
								preload_str += (preload_str.length()==0?"":",") + key;
							}
						}
					}
					
					log( "Popular templates: " + popular_str );
					
					if ( preload_str.length() > 0 ){
						
						log( "Pre-load templates: " + popular_str );
					}
				}
						
					// pick up explicitly selected vuze ones
				
				String manual_str = "";
				
				for (int i=0;i<engines.length;i++){
					
					Engine	engine = engines[i];
					
					Long key = new Long( engine.getId());
					
					if ( 	engine.getSource() == Engine.ENGINE_SOURCE_VUZE &&
							engine.getSelectionState() == Engine.SEL_STATE_MANUAL_SELECTED &&
							!vuze_selected_ids.containsKey( key )){
						
						manual_vuze_ids.add( key );
					}
				}
				
				if ( manual_vuze_ids.size() > 0 ){
					
					long[]	manual_ids = new long[manual_vuze_ids.size()];
					
					Iterator it = manual_vuze_ids.iterator();
					
					int	pos = 0;
					
					while( it.hasNext()){
						
						manual_ids[pos++] = ((Long)it.next()).longValue();
					}
					
					PlatformMetaSearchMessenger.templateInfo[] manual = PlatformMetaSearchMessenger.getTemplateDetails( manual_ids );
										
					for (int i=0;i<manual.length;i++){
						
						PlatformMetaSearchMessenger.templateInfo template = manual[i];
						
						if ( !template.isVisible()){
							
							continue;
						}
						
						Long	key = new Long( template.getId());
													
						vuze_selected_ids.put( 
							key, 
							new Long( template.getModifiedDate()));
														
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
					
					vuze_preload_ids.remove( entry.getKey());
					
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
					
					if ( this_engine != null ){
							
						if ( this_engine.getSelectionState() == Engine.SEL_STATE_DESELECTED ){
						
							log( "Auto-selecting " + this_engine.getString());
							
							this_engine.setSelectionState( Engine.SEL_STATE_AUTO_SELECTED );
						}
					}
				}
				
					// do any pre-loads
				
				it = vuze_preload_ids.entrySet().iterator();
				
				while( it.hasNext()){
					
					Map.Entry entry = (Map.Entry)it.next();
					
					long	id 			= ((Long)entry.getKey()).longValue();
									
					Engine this_engine = (Engine)existing_engine_map.get( new Long(id));
						
					if ( this_engine == null ){
						
						PlatformMetaSearchMessenger.templateDetails details = PlatformMetaSearchMessenger.getTemplate( id );
	
						log( "Downloading pre-load definition of template " + id );
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
								
								this_engine.setSelectionState( Engine.SEL_STATE_DESELECTED );
								
								meta_search.addEngine( this_engine );
								
							}catch( Throwable e ){
								
								log( "Failed to import engine '" + details.getValue() + "'", e );
							}
						}			
					}
				}
				
					// deselect any not in use
				
				for (int i=0;i<engines.length;i++){
					
					Engine	engine = engines[i];
					
					if ( 	engine.getSource() == Engine.ENGINE_SOURCE_VUZE &&
							engine.getSelectionState() != Engine.SEL_STATE_DESELECTED &&
							!vuze_selected_ids.containsKey( new Long( engine.getId()))){
						
						log( "Deselecting " + engine.getString() + " as no longer visible on Vuze");
						
						engine.setSelectionState( Engine.SEL_STATE_DESELECTED );
					}
				}
				
					// finally pick up any unreported selection changes and re-affirm positive selections
				
				for (int i=0;i<engines.length;i++){
					
					Engine	engine = engines[i];
					
					if ( 	engine.getSource() == Engine.ENGINE_SOURCE_VUZE &&
							engine.getSelectionState() == Engine.SEL_STATE_MANUAL_SELECTED ){
						
						engine.recordSelectionState();
						
					}else{
						
						engine.checkSelectionStateRecorded();
					}
				}
				
				refresh_completed = true;
				
			}catch( Throwable e ){
				
				log( "Refresh failed", e );
				
				throw( new MetaSearchException( "Refresh failed", e ));
			}
		}finally{
			
			if ( first_run && refresh_completed ){
				
				COConfigurationManager.setParameter( "metasearch.refresh.first_run", false );
			}
			
			refresh_sem.release();
			
			initial_refresh_sem.releaseForever();
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
		return( COConfigurationManager.getBooleanParameter( "metasearch.auto.mode", AUTO_MODE_DEFAULT ));
	}
	
	protected void
	setAutoMode(
		boolean	auto )
	{
		COConfigurationManager.setParameter( "metasearch.auto.mode", auto );
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

			Engine[]	engines = meta_search.getEngines( false, false );
			
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
			
			engines = meta_search.getEngines( false, false );

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
	
	public Engine
	addEngine(
		long		id,
		int			type,
		String		name,
		String		json_value )
	
		throws MetaSearchException
	{
		if ( id == -1 ){
			
			id = getLocalTemplateID();
		}
		
		try{
			Engine engine = meta_search.importFromJSONString( type,	id,	SystemTime.getCurrentTime(),name, json_value );
			
			engine.setSource( Engine.ENGINE_SOURCE_LOCAL );
			
			engine.setSelectionState( Engine.SEL_STATE_MANUAL_SELECTED );
			
			meta_search.addEngine( engine );
			
			return( engine );
			
		}catch( Throwable e ){
			
			throw( new MetaSearchException( "Failed to add engine", e ));
		}
	}
	
	public Engine
	importEngine(
		Map			map,
		boolean		warn_user )
	
		throws MetaSearchException
	{
		try{
			EngineImpl engine = (EngineImpl)meta_search.importFromBEncodedMap(map);
			
			long	id = engine.getId();
			
			Engine existing = meta_search.getEngine( id );
			
			if ( existing != null ){
				
				if ( existing.sameLogicAs( engine )){
					
					if ( warn_user ){
						
						UIManager ui_manager = StaticUtilities.getUIManager( 120*1000 );
						
						String details = MessageText.getString(
								"metasearch.addtemplate.dup.desc",
								new String[]{ engine.getName() });
						
						ui_manager.showMessageBox(
								"metasearch.addtemplate.dup.title",
								"!" + details + "!",
								UIManagerEvent.MT_OK );
					}
					
					return( existing );
				}
			}
			
			if ( warn_user ){
				
				UIManager ui_manager = StaticUtilities.getUIManager( 120*1000 );
				
				String details = MessageText.getString(
						"metasearch.addtemplate.desc",
						new String[]{ engine.getName() });
				
				long res = ui_manager.showMessageBox(
						"metasearch.addtemplate.title",
						"!" + details + "!",
						UIManagerEvent.MT_YES | UIManagerEvent.MT_NO );
				
				if ( res != UIManagerEvent.MT_YES ){
					
					throw( new MetaSearchException( "User declined the template" ));
				}
			}
				// if local template then we try to use the id as is otherwise we emsure that
				// it is a local one
			
			if ( id >= 0 && id < Integer.MAX_VALUE ){
				
				id = getLocalTemplateID();
				
				engine.setId( id );
			}
			
			engine.setSource( Engine.ENGINE_SOURCE_LOCAL );
			
			engine.setSelectionState( Engine.SEL_STATE_MANUAL_SELECTED );
			
			meta_search.addEngine( engine );
			
			if ( warn_user ){
				
				UIManager ui_manager = StaticUtilities.getUIManager( 120*1000 );
				
				String details = MessageText.getString(
						"metasearch.addtemplate.done.desc",
						new String[]{ engine.getName() });
				
				ui_manager.showMessageBox(
						"metasearch.addtemplate.done.title",
						"!" + details + "!",
						UIManagerEvent.MT_OK );
			}
			
			return( engine );
			
		}catch( Throwable e ){
			
			if ( warn_user ){
				
				UIManager ui_manager = StaticUtilities.getUIManager( 120*1000 );
				
				String details = MessageText.getString(
						"metasearch.addtemplate.failed.desc",
						new String[]{ Debug.getNestedExceptionMessage(e) });
				
				ui_manager.showMessageBox(
						"metasearch.addtemplate.failed.title",
						"!" + details + "!",
						UIManagerEvent.MT_OK );
			}
			
			throw( new MetaSearchException( "Failed to add engine", e ));
		}
	}
	
	public Engine[]
	loadFromVuzeFile(
		File		file )
	{
		VuzeFile vf = VuzeFileHandler.getSingleton().loadVuzeFile( file.getAbsolutePath());
		
		if ( vf != null ){
			
			return( loadFromVuzeFile( vf ));
		}
		
		return( new Engine[0]);
	}
	
	public Engine[]
	loadFromVuzeFile(
		VuzeFile		vf )
	{
		List	result = new ArrayList();
		
		VuzeFileComponent[] comps = vf.getComponents();
		
		for (int j=0;j<comps.length;j++){
			
			VuzeFileComponent comp = comps[j];
			
			if ( comp.getType() == VuzeFileComponent.COMP_TYPE_METASEARCH_TEMPLATE ){
				
				try{
					result.add( importEngine( comp.getContent(), false ));
											
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
		}
		
		return((Engine[])result.toArray(new Engine[result.size()]));
	}
	
	public long
	getLocalTemplateID()
	{
		synchronized( this ){
			
			Random random = new Random();
			
			while( true ){
			
				long id = ((long)Integer.MAX_VALUE) + random.nextInt( Integer.MAX_VALUE );
				
				if ( meta_search.getEngine( id ) == null ){
					
					return( id );
				}
			}
		}
	}
	
	public void 
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
	
	public void 
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
	
	public void
	generate(
		IndentWriter		writer )
	{
		writer.println( "Metasearch: auto=" + isAutoMode());
			
		try{
			writer.indent();

			meta_search.generate( writer );
			
		}finally{
			
			writer.exdent();
		}
	}
}
