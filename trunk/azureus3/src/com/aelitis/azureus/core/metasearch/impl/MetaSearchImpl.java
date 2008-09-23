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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DelayedEvent;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TimerEventPeriodic;
import org.gudy.azureus2.plugins.utils.StaticUtilities;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderFactory;
import org.gudy.azureus2.plugins.utils.search.SearchProvider;

import com.aelitis.azureus.core.messenger.config.PlatformMetaSearchMessenger;
import com.aelitis.azureus.core.metasearch.*;
import com.aelitis.azureus.core.metasearch.impl.plugin.PluginEngine;
import com.aelitis.azureus.core.metasearch.impl.web.FieldMapping;
import com.aelitis.azureus.core.metasearch.impl.web.regex.RegexEngine;
import com.aelitis.azureus.core.metasearch.impl.web.rss.RSSEngine;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.core.vuzefile.VuzeFile;
import com.aelitis.azureus.core.vuzefile.VuzeFileHandler;

public class 
MetaSearchImpl
	implements MetaSearch
{
	private static final String	CONFIG_FILE = "metasearch.config";
		
	private MetaSearchManagerImpl	manager;
	
	private CopyOnWriteList 	engines 	= new CopyOnWriteList();
	private Map					plugin_map	= new HashMap();
	
	private boolean config_dirty;
	
	private CopyOnWriteList 	listeners 	= new CopyOnWriteList();
	
	private TimerEventPeriodic	update_check_timer;
	
	private static final int 	UPDATE_CHECK_PERIOD		= 15*60*1000;
	private static final int	MIN_UPDATE_CHECK_SECS	= 10*60;
	
	private Object				MS_UPDATE_CONSEC_FAIL_KEY = new Object();
	
	protected 
	MetaSearchImpl(
		MetaSearchManagerImpl		_manager )
	{
		manager	= _manager;
		
		loadConfig();
	}
	
	protected 
	MetaSearchImpl()
	{
	}
	
	public Engine
	importFromBEncodedMap(
		Map		map )
	
		throws IOException
	{
		return( EngineImpl.importFromBEncodedMap( this, map )); 
	}
	
	public Engine
	importFromJSONString(
		int			type,
		long		id,
		long		last_updated,
		String		name,
		String		content )
	
		throws IOException
	{
		return( EngineImpl.importFromJSONString( this, type, id, last_updated, name, content ));
	}
	
	public EngineImpl
	importFromPlugin(
		String				pid,
		SearchProvider		provider )
	
		throws IOException
	{
		synchronized( this ){

			Long	l_id = (Long)plugin_map.get( pid );
			
			long	id;
			
			if ( l_id == null ){
				
				id = manager.getLocalTemplateID();
						
				plugin_map.put( pid, new Long( id ));
						
				configDirty();

			}else{
				
				id = l_id.longValue();
			}
			
			EngineImpl engine = (EngineImpl)getEngine( id );
			
			if ( engine == null ){
				
				engine = new PluginEngine( this, id, provider );
				
				engine.setSource( Engine.ENGINE_SOURCE_LOCAL );
				
				engine.setSelectionState( Engine.SEL_STATE_MANUAL_SELECTED );
				
				addEngine( engine );
				
			}else{
				
				if ( engine instanceof PluginEngine ){
					
					((PluginEngine)engine).setProvider( provider );
					
				}else{
					
					Debug.out( "Inconsistent: plugin must be a PluginEngine!" );
					
					plugin_map.remove( pid );
					
					removeEngine( engine );
					
					throw( new IOException( "Inconsistent" ));
				}
			}
			
			return( engine );
		}	
	}
	
	public Engine 
	createRSSEngine(
		String		name,
		URL 		url )
	
		throws MetaSearchException 
	{
		EngineImpl engine = 
			new RSSEngine( 
					this, 
					manager.getLocalTemplateID(), 
					SystemTime.getCurrentTime(), 
					name, 
					url.toExternalForm(), 
					false,
					null,
					new String[0] );
		
		engine.setSource( Engine.ENGINE_SOURCE_RSS );
		
		addEngine( engine, false );
				
		log( "Created RSS engine '" + url + "'" );
		
		return( engine );
	}
	
	protected void
	enableUpdateChecks()
	{
		synchronized( this ){
			
			if ( update_check_timer == null ){
				
				update_check_timer = SimpleTimer.addPeriodicEvent(
						"MS:updater",
						UPDATE_CHECK_PERIOD,
						new TimerEventPerformer()
						{
							public void 
							perform(
								TimerEvent event) 
							{
								checkUpdates();
							}
						});
			}
		}
	}
	
	protected void
	checkUpdates()
	{
		Iterator it = engines.iterator();
		
		while( it.hasNext()){
				
			EngineImpl	engine = (EngineImpl)it.next();
				
			String	update_url = engine.getUpdateURL();
			
			if ( update_url != null ){
				
				long	now				= SystemTime.getCurrentTime();
				
				long	last_check 		= engine.getLastUpdateCheck();
				
				if ( last_check > now ){
					
					last_check = now;
					
					engine.setLastUpdateCheck( now );
				}
				
				long	check_secs	= engine.getUpdateCheckSecs();
				
				if ( check_secs < MIN_UPDATE_CHECK_SECS ){
					
					log( "Update check period too small (" + check_secs + " secs) adjusting to " + MIN_UPDATE_CHECK_SECS + ": " + engine.getName());
					
					check_secs = MIN_UPDATE_CHECK_SECS;
				}
				
				long	check_millis	= check_secs*1000;
				
				long	next_check		= last_check + check_millis;
				
				Object	consec_fails_o = engine.getUserData( MS_UPDATE_CONSEC_FAIL_KEY );
				
				int	consec_fails = consec_fails_o==null?0:((Integer)consec_fails_o).intValue();
				
				if ( consec_fails > 0 ){
					
					next_check += ( UPDATE_CHECK_PERIOD << consec_fails );
				}
				
				if ( next_check < now ){
				
					if ( updateEngine( engine )){
						
						consec_fails	= 0;
						
						engine.setLastUpdateCheck( now );
						
					}else{
						
						consec_fails++;
						
						if ( consec_fails > 3 ){
							
							consec_fails	= 0;
							
								// skip to next scheduled update time
							
							engine.setLastUpdateCheck( now );
						}
					}
					
					engine.setUserData( MS_UPDATE_CONSEC_FAIL_KEY, consec_fails==0?null:new Integer( consec_fails ));
				}
			}
		}
	}
	
	protected boolean
	updateEngine(
		EngineImpl		engine )
	{
		String	update_url = engine.getUpdateURL();

		int	pos = update_url.indexOf('?');
		
		if ( pos == -1 ){
			
			update_url += "?";
			
		}else{
			
			update_url += "&";
		}
		
		update_url += 	"az_template_uid=" + engine.getUID() + 
						"&az_template_version=" + engine.getVersion() +
						"&az_version=" + Constants.AZUREUS_VERSION +
					    "&az_locale=" + MessageText.getCurrentLocale().toString() +
					    "&az_rand=" + Math.abs( new Random().nextLong());
		
		log( "Engine " + engine.getName() + ": auto-update check via " + update_url );
		
		try{
			ResourceDownloaderFactory rdf = StaticUtilities.getResourceDownloaderFactory();
			
			ResourceDownloader url_rd = rdf.create( new URL( update_url ));
			
			ResourceDownloader rd = rdf.getMetaRefreshDownloader( url_rd );
			
			InputStream is = rd.download();
			
			try{
				Map map = BDecoder.decode( new BufferedInputStream( is ));
				
				log( "    update check reply: " + map );
				
					// reply is either "response" meaning "no update" and giving possibly changed update secs
					// or Vuze file with updated template
				
				Map response = (Map)map.get( "response" );
				
				if ( response != null ){
					
					Long	update_secs = (Long)response.get( "update_url_check_secs" );
					
					if ( update_secs == null ){
						
						engine.setLocalUpdateCheckSecs( 0 );
						
					}else{
						
						int	check_secs = update_secs.intValue();
						
						if ( check_secs < MIN_UPDATE_CHECK_SECS ){
							
							log( "    update check secs for to small, min is " + MIN_UPDATE_CHECK_SECS);
							
							check_secs = MIN_UPDATE_CHECK_SECS;
						}
							
						engine.setLocalUpdateCheckSecs( check_secs );
					}
					
					return( true );
					
				}else{
					
					VuzeFile vf = VuzeFileHandler.getSingleton().loadVuzeFile( map );
					
					if ( vf == null ){
						
						log( "    failed to decode vuze file" );
						
						return( false );
					}
										
					Engine[] updated_engines = manager.loadFromVuzeFile( vf );
					
					if ( updated_engines.length > 0 ){
						
						String	existing_uid = engine.getUID();
						
						boolean	found = false;
						
						String	engine_str = "";
						
						for (int i=0;i<updated_engines.length;i++){
							
							Engine updated_engine = updated_engines[i];
							
							engine_str += (i==0?"":",") + updated_engine.getName() + ": uid=" + updated_engine.getUID() + ",version=" + updated_engine.getVersion();
							
							if ( updated_engine.getUID().equals( existing_uid )){
								
								found	= true;
							}
						}
						
						if ( !found ){
							
							log( "    existing engine not found in updated set, deleting" );
							
							engine.delete();
							
						}
							
						log( "    update complete: new engines=" + engine_str );
						
					}else{
						
						log( "    no engines found in vuze file" );
					}
					
					return( true );
				}
			}finally{
				
				is.close();
			}
		}catch( Throwable e ){
			
			log( "    update check failed", e );
			
			return( false );
		}
	}
	
	public void 
	addEngine(
		Engine 	engine )
	{
		addEngine( (EngineImpl)engine, false );
	}
	
	public Engine 
	addEngine(
		long 		id )
	
		throws MetaSearchException 
	{
		try{

			PlatformMetaSearchMessenger.templateDetails details = PlatformMetaSearchMessenger.getTemplate( id );
		
			log( "Downloading definition of template " + id );
			log( details.getValue());
		
			if ( details.isVisible()){
			
				Engine engine = 
					importFromJSONString( 
						details.getType()==PlatformMetaSearchMessenger.templateDetails.ENGINE_TYPE_JSON?Engine.ENGINE_TYPE_JSON:Engine.ENGINE_TYPE_REGEX,
						details.getId(),
						details.getModifiedDate(),
						details.getName(),
						details.getValue());
				
				engine.setSource( Engine.ENGINE_SOURCE_VUZE );
				engine.setSelectionState( Engine.SEL_STATE_DESELECTED );
				
				addEngine( engine );
				
				return( engine );
				
			}else{
				
				throw( new MetaSearchException( "Search template is not visible" ));
			}
		}catch( MetaSearchException e ){
			
			throw( e );
			
		}catch( Throwable e ){
		
			throw( new MetaSearchException( "Template load failed", e ));
		}	
	}
		
	public void 
	addEngine(
		EngineImpl 	new_engine,
		boolean		loading )
	{		
		boolean	add_op = true;
		
		synchronized( this ){
			
			Iterator	it = engines.iterator();
			
			while( it.hasNext()){
				
				Engine existing_engine = (Engine)it.next();
				
				if ( existing_engine.getId() == new_engine.getId()){
					
					log( "Updating engine with same ID " + existing_engine.getId() + ": " + existing_engine.getName() + "/" + existing_engine.getUID());
					
					it.remove();
					
					new_engine.setUID( existing_engine.getUID());
					
					if ( existing_engine.sameLogicAs( new_engine )){
						
						new_engine.setVersion( existing_engine.getVersion());

					}else{
						
						new_engine.setVersion( existing_engine.getVersion() + 1 );
						
						log( "    new version=" + new_engine.getVersion());
					}
					 
					add_op = false;
					
				}else if ( existing_engine.getUID().equals( new_engine.getUID())){
					
					log( "Removing engine with same UID " + existing_engine.getUID() + "(" + existing_engine.getName() + ")" );
					
					it.remove();
				}
			}
			
			engines.add( new_engine );
		}
		
		if ( new_engine.getUpdateURL() != null ){
			
			enableUpdateChecks();
		}
		
		if ( !loading ){
			
			log( "Engine '" + new_engine.getName() + "' added" );
			
			saveConfig();
		
			Iterator it = listeners.iterator();
			
			while( it.hasNext()){
				
				MetaSearchListener listener = (MetaSearchListener)it.next();
				
				try{
					if ( add_op ){
						
						listener.engineAdded( new_engine );
						
					}else{
						
						listener.engineUpdated( new_engine );
					}
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
		}
	}
	
	public void 
	removeEngine(
		Engine 	engine )
	{
		if ( engines.remove( engine )){
		
			log( "Engine '" + engine.getName() + "' removed" );
			
			saveConfig();
			
			Iterator it = listeners.iterator();
			
			while( it.hasNext()){
				
				try{
	
					((MetaSearchListener)it.next()).engineRemoved( engine );
	
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
		}
	}
	
	public Engine[] 
	getEngines(
		boolean		active_only,
		boolean		ensure_up_to_date )
	{
		if ( ensure_up_to_date ){
			
			manager.ensureEnginesUpToDate();
		}
		
		List l = engines.getList();
				
		List result;
		
		if ( active_only ){
			
			result = new ArrayList();
			
			for (int i=0;i<l.size();i++){
				
				Engine	e = (Engine)l.get(i);
				
				if ( e.isActive()){
					
					result.add( e );
				}
			}
		}else{
			
			result = l;
		}
		
		return( (Engine[])result.toArray( new Engine[ result.size() ]));
	}
	
	public Engine
	getEngine(
		long		id )
	{
		List l = engines.getList();
		
		for( int i=0;i<l.size();i++){
			
			Engine e = (Engine)l.get(i);
			
			if ( e.getId() == id ){
				
				return( e );
			}
		}

		return( null );
	}
	
	public Engine
	getEngineByUID(
		String	uid )
	{
		List l = engines.getList();
		
		for( int i=0;i<l.size();i++){
			
			Engine e = (Engine)l.get(i);
			
			if ( e.getUID().equals( uid )){
				
				return( e );
			}
		}

		return( null );
	}
	
	public int
	getEngineCount()
	{
		return( engines.size());
	}
	
	public Engine[] 
	search(
		final ResultListener 	original_listener,
		SearchParameter[] 		searchParameters,
		String					headers )
	{
		return( search( null, original_listener, searchParameters, headers ));
	}
	
	public Engine[] 
	search(
		Engine					engine,
		final ResultListener 	original_listener,
		SearchParameter[] 		searchParameters,
		String					headers )
	{
		String	param_str = "";
		
		for (int i=0;i<searchParameters.length;i++){
		
			SearchParameter param = searchParameters[i];
			
			param_str += (i==0?"":",") + param.getMatchPattern() + "->" + param.getValue();
		}
		
		ResultListener	listener = 
			new ResultListener()
			{
					// 	single thread listener calls
			
				private AsyncDispatcher dispatcher = new AsyncDispatcher( 5000 );

				public void 
				contentReceived(
					final Engine engine, 
					final String content ) 
				{
					dispatcher.dispatch(
							new AERunnable()
							{
								public void
								runSupport()
								{
									original_listener.contentReceived( engine, content );
								}
							});
				}
				
				public void 
				matchFound(
					final Engine 	engine, 
					final String[] 	fields )
				{
					dispatcher.dispatch(
							new AERunnable()
							{
								public void
								runSupport()
								{
									original_listener.matchFound( engine, fields );
								}
							});	
				}
				
				public void 
				resultsReceived(
					final Engine 	engine,
					final Result[] 	results )
				{
					dispatcher.dispatch(
						new AERunnable()
						{
							public void
							runSupport()
							{
								final boolean CHUNK = false;
								
								if ( CHUNK ){
									
									final int	CHUNK_SIZE 	= 25;
									final int	CHUNK_DELAY	= 500;
									
									for (int i=0;i<results.length;i+=CHUNK_SIZE){
										
										int	to_do = Math.min( CHUNK_SIZE, results.length - i );
										
										Result[] chunk = new Result[to_do];
										
										System.out.println( "sending " + i + " to " + ( i+to_do ) + " of " + results.length );
										
										System.arraycopy(results, i, chunk, 0, to_do );
										
										original_listener.resultsReceived( engine, chunk );
										
										if ( results.length - i > CHUNK_SIZE ){
											
											try{
												Thread.sleep( CHUNK_DELAY );
												
											}catch( Throwable e ){
												
											}
										}
									}
								}else{
								
									original_listener.resultsReceived( engine, results );
								}
							}
						});
				}
			
				public void 
				resultsComplete(
					final Engine engine )
				{
					dispatcher.dispatch(
							new AERunnable()
							{
								public void
								runSupport()
								{
									original_listener.resultsComplete( engine );
								}
							});
				}
			
				public void 
				engineFailed(
					final Engine 	engine,
					final Throwable	e )
				{
					dispatcher.dispatch(
							new AERunnable()
							{
								public void
								runSupport()
								{
									original_listener.engineFailed( engine, e );
								}
							});
				}
				
				public void 
				engineRequiresLogin(
					final Engine 	engine,
					final Throwable	e )
				{
					dispatcher.dispatch(
							new AERunnable()
							{
								public void
								runSupport()
								{
									original_listener.engineRequiresLogin( engine, e );
								}
							});
				}
			};
			
		SearchExecuter se = new SearchExecuter(listener);
		
		if ( engine == null ){
			
			Engine[] engines = getEngines( true, true );
	
			String	engines_str = "";
			
			for (int i=0;i<engines.length;i++){
				
				engines_str += (i==0?"":",") + engines[i].getId();
			}
			
			log( "Search: params=" + param_str + "; engines=" + engines_str );
			
	
			for (int i=0;i<engines.length;i++){
				
				se.search( engines[i], searchParameters, headers );
			}
			
			return( engines );
			
		}else{
			
			log( "Search: params=" + param_str + "; engine=" + engine.getId());

			se.search( engine, searchParameters, headers );
			
			return( new Engine[]{ engine });
		}
	}
	
	public void
	addListener(
		MetaSearchListener		listener )
	{
		listeners.add( listener );
	}
	
	public void
	removeListener(
		MetaSearchListener		listener )
	{
		listeners.remove( listener );
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
						Engine e = importFromBEncodedMap( m );
						
						addEngine( (EngineImpl)e, true );
						
						log( "    loaded " + e.getString());
						
					}catch( Throwable e ){
						
						log( "Failed to import engine from " + m, e );
					}
				}
			}
			
			Map	p_map = (Map)map.get( "plugin_map" );
			
			if ( p_map != null ){
				
				plugin_map = p_map;
			}
		}
		
		if ( update_check_timer != null ){
			
			new AsyncDispatcher().dispatch(
					new AERunnable()
					{
						public void 
						runSupport() 
						{
							checkUpdates();
						}
					});
		}
	}
	
	protected void
	configDirty()
	{
		synchronized( this ){
			
			if ( config_dirty ){
				
				return;
			}
			
			config_dirty = true;
		
			new DelayedEvent( 
				"MetaSearch:save", 5000,
				new AERunnable()
				{
					public void 
					runSupport() 
					{
						synchronized( this ){
							
							if ( !config_dirty ){

								return;
							}
							
							saveConfig();
						}	
					}
				});
		}
	}
	
	protected void
	saveConfig()
	{
		log( "Saving configuration" );
		
		synchronized( this ){
			
			config_dirty = false;
			
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
			
			if ( plugin_map != null ){
				
				map.put( "plugin_map", plugin_map );
			}
			
			FileUtil.writeResilientConfigFile( CONFIG_FILE, map );
		}
	}
	
	protected void
	log(
		String	str )
	{
		manager.log( "search :"  + str );
	}
	
	protected void
	log(
		String		str,
		Throwable 	e )
	{
		manager.log( "search :"  +  str, e );
	}
	
	public static void
	main(
		String[]		args )
	{
		try{
			MetaSearchImpl ms = new MetaSearchImpl();
			
			EngineImpl e = new RegexEngine(
					ms, 
					Integer.MAX_VALUE + 9991,
					SystemTime.getCurrentTime(),
					"UpdateTest",
					"http://localhost:1234/search=%s",
					"",
					"GMT",
					true,
					null,
					new FieldMapping[] {
						new FieldMapping("1",Engine.FIELD_CATEGORY),
						new FieldMapping("2",Engine.FIELD_CDPLINK),
						new FieldMapping("3",Engine.FIELD_NAME),
						new FieldMapping("4",Engine.FIELD_TORRENTLINK),
						new FieldMapping("5",Engine.FIELD_COMMENTS),
						new FieldMapping("6",Engine.FIELD_DATE),
						new FieldMapping("7",Engine.FIELD_SIZE),
						new FieldMapping("8",Engine.FIELD_VOTES),
						new FieldMapping("9",Engine.FIELD_SEEDS),
						new FieldMapping("10",Engine.FIELD_PEERS),
						},
					false,
					"",
					new String[] {""} );
					
			e.setUpdateURL( "http://localhost:5678/update" );
			
			e.setDefaultUpdateCheckSecs( 60 );
			
			e.setVersion( 2 );
			
			e.exportToVuzeFile( new File( "c:\\temp\\updatetest.vuze" ));
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
}
