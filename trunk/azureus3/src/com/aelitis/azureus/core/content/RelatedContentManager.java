/*
 * Created on Jul 8, 2009
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


package com.aelitis.azureus.core.content;

import java.lang.ref.WeakReference;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.ByteArrayHashMap;
import org.gudy.azureus2.core3.util.ByteEncodedKeyHashMap;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.RandomUtils;
import org.gudy.azureus2.core3.util.SHA1Simple;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.StringInterner;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.core.util.bloom.BloomFilter;
import com.aelitis.azureus.core.util.bloom.BloomFilterFactory;
import com.aelitis.azureus.plugins.dht.DHTPlugin;
import com.aelitis.azureus.plugins.dht.DHTPluginContact;
import com.aelitis.azureus.plugins.dht.DHTPluginOperationListener;
import com.aelitis.azureus.plugins.dht.DHTPluginValue;
import com.aelitis.azureus.util.ImportExportUtils;

public class 
RelatedContentManager 
{
	private static final boolean TRACE = false;
	
	private static final int	MAX_HISTORY				= 16;
	private static final int	MAX_TITLE_LENGTH		= 80;
	private static final int	MAX_CONCURRENT_PUBLISH	= 2;
	
	private static final String	CONFIG_FILE 				= "rcm.config";
	private static final String	PERSIST_DEL_FILE 			= "rcmx.config";
	
	private static final String	CONFIG_TOTAL_UNREAD	= "rcm.numunread.cache";
	
	private static RelatedContentManager	singleton;
	private static AzureusCore				core;
	
	public static synchronized void
	preInitialise(
		AzureusCore		_core )
	{
		core		= _core;
	}
	
	public static synchronized RelatedContentManager
	getSingleton()
	
		throws ContentException
	{
		if ( singleton == null ){
			
			singleton = new RelatedContentManager();
		}
		
		return( singleton );
	}
	
	
	private PluginInterface 				plugin_interface;
	private TorrentAttribute 				ta_networks;
	private DHTPlugin						dht_plugin;

	private long	global_random_id = -1;
	
	private LinkedList<DownloadInfo>		download_infos1 	= new LinkedList<DownloadInfo>();
	private LinkedList<DownloadInfo>		download_infos2 	= new LinkedList<DownloadInfo>();
	
	private ByteArrayHashMapEx<DownloadInfo>	download_info_map	= new ByteArrayHashMapEx<DownloadInfo>();
	private Set<String>							download_priv_set	= new HashSet<String>();
	
	private boolean	enabled;
	private int		max_search_level;
	private int		max_results;
	
	private int publishing_count = 0;
	
	private CopyOnWriteList<RelatedContentManagerListener>	listeners = new CopyOnWriteList<RelatedContentManagerListener>();
	
	private AESemaphore initialisation_complete_sem = new AESemaphore( "RCM:init" );

	private static final int TIMER_PERIOD			= 30*1000;
	private static final int PUBLISH_CHECK_PERIOD	= 30*1000;
	private static final int PUBLISH_CHECK_TICKS	= PUBLISH_CHECK_PERIOD/TIMER_PERIOD;
	private static final int SECONDARY_LOOKUP_PERIOD	= 15*60*1000;
	private static final int SECONDARY_LOOKUP_TICKS		= SECONDARY_LOOKUP_PERIOD/TIMER_PERIOD;
	private static final int REPUBLISH_PERIOD			= 8*60*60*1000;
	private static final int REPUBLISH_TICKS			= REPUBLISH_PERIOD/TIMER_PERIOD;

	
	
	private static final int CONFIG_DISCARD_MILLIS	= 60*1000;
	
	private ContentCache				content_cache_ref;
	private WeakReference<ContentCache>	content_cache;
	
	private boolean		content_dirty;
	private long		last_config_access;		
	private int			content_discard_ticks;
	
	private int	total_unread = COConfigurationManager.getIntParameter( CONFIG_TOTAL_UNREAD, 0 );
	
	private AsyncDispatcher	content_change_dispatcher = new AsyncDispatcher();
	
	private static final int SECONDARY_LOOKUP_CACHE_MAX = 10;
	
	private LinkedList<SecondaryLookup> secondary_lookups = new LinkedList<SecondaryLookup>();
	
	private boolean	secondary_lookup_in_progress;
	private long	secondary_lookup_complete_time;
	
	
	protected
	RelatedContentManager()
	
		throws ContentException
	{
		try{
			if ( core == null ){
				
				throw( new ContentException( "getSingleton called before pre-initialisation" ));
			}
			
			while( global_random_id == -1 ){
				
				global_random_id = COConfigurationManager.getLongParameter( "rcm.random.id", -1 );
				
				if ( global_random_id == -1 ){
					
					global_random_id = RandomUtils.nextLong();
					
					COConfigurationManager.setParameter( "rcm.random.id", global_random_id );
				}
			}
				
			plugin_interface = core.getPluginManager().getDefaultPluginInterface();
			
			ta_networks 	= plugin_interface.getTorrentManager().getAttribute( TorrentAttribute.TA_NETWORKS );
			
			COConfigurationManager.addAndFireParameterListeners(
				new String[]{
					"rcm.enabled",
					"rcm.max_search_level",
					"rcm.max_results",
				},
				new ParameterListener()
				{
					public void 
					parameterChanged(
						String name )
					{
						enabled 			= COConfigurationManager.getBooleanParameter( "rcm.enabled", true );
						max_search_level 	= COConfigurationManager.getIntParameter( "rcm.max_search_level", 3 );
						max_results		 	= COConfigurationManager.getIntParameter( "rcm.max_results", 500 );
					}
				});
			
			SimpleTimer.addEvent(
				"rcm.delay.init",
				SystemTime.getOffsetTime( 15*1000 ),
				new TimerEventPerformer()
				{
					public void 
					perform(
						TimerEvent event )
					{
						plugin_interface.addListener(
							new PluginListener()
							{
								public void
								initializationComplete()
								{
									try{
										PluginInterface dht_pi = 
											plugin_interface.getPluginManager().getPluginInterfaceByClass(
														DHTPlugin.class );
							
										if ( dht_pi != null ){
								
											dht_plugin = (DHTPlugin)dht_pi.getPlugin();
		
											DownloadManager dm = plugin_interface.getDownloadManager();
											
											Download[] downloads = dm.getDownloads();
											
											addDownloads( downloads, true );
											
											dm.addListener(
												new DownloadManagerListener()
												{
													public void
													downloadAdded(
														Download	download )
													{
														addDownloads( new Download[]{ download }, false );
													}
													
													public void
													downloadRemoved(
														Download	download )
													{
													}
												},
												false );
											
											SimpleTimer.addPeriodicEvent(
												"RCM:publisher",
												TIMER_PERIOD,
												new TimerEventPerformer()
												{
													private int	tick_count;
													
													public void 
													perform(
														TimerEvent event ) 
													{
														if ( enabled ){
													
															tick_count++;
															
															if ( tick_count % PUBLISH_CHECK_TICKS == 0 ){
															
																publish();
															
																saveRelatedContent();
															}
															
															if ( tick_count % SECONDARY_LOOKUP_TICKS == 0 ){

																secondaryLookup();
															}
															
															if ( tick_count % REPUBLISH_TICKS == 0 ){

																republish();
															}

														}
													}
												});
										}
									}finally{
											
										initialisation_complete_sem.releaseForever();
									}
								}
								
								public void
								closedownInitiated()
								{
									saveRelatedContent();
								}
								
								public void
								closedownComplete()
								{
								}
							});
					}
				});
			
		}catch( Throwable e ){
			
			initialisation_complete_sem.releaseForever();
			
			if ( e instanceof ContentException ){
				
				throw((ContentException)e);
			}
			
			throw( new ContentException( "Initialisation failed", e ));
		}
	}
	
	public boolean
	isEnabled()
	{
		return( enabled );
	}
	
	public void
	setEnabled(
		boolean		_enabled )
	{
		COConfigurationManager.setParameter( "rcm.enabled", _enabled );
	}
	
	public int
	getMaxSearchLevel()
	{
		return( max_search_level );
	}
	
	public void
	setMaxSearchLevel(
		int		_level )
	{
		COConfigurationManager.setParameter( "rcm.max_search_level", _level );
	}
	
	public int
	getMaxResults()
	{
		return( max_results );
	}
	
	public void
	setMaxResults(
		int		_max )
	{
		COConfigurationManager.setParameter( "rcm.max_results", _max );
	}
	
	protected void
	addDownloads(
		Download[]		downloads,
		boolean			initialising )
	{
		synchronized( this ){
	
			List<DownloadInfo>	new_info = new ArrayList<DownloadInfo>( downloads.length );
			
			for ( Download download: downloads ){
				
				try{
					if ( !download.isPersistent()){
						
						continue;
					}
					
					Torrent	torrent = download.getTorrent();
	
					if ( torrent == null ){
						
						continue;
					}
					
					byte[]	hash = torrent.getHash();

					if ( download_info_map.containsKey( hash )){
						
						continue;
					}
					
					String[]	networks = download.getListAttribute( ta_networks );
					
					if ( networks == null ){
						
						continue;
					}
						
					boolean	public_net = false;
					
					for (int i=0;i<networks.length;i++){
						
						if ( networks[i].equalsIgnoreCase( "Public" )){
								
							public_net	= true;
							
							break;
						}
					}
					
					if ( public_net && !TorrentUtils.isReallyPrivate( PluginCoreUtils.unwrap( torrent ))){
						
						DownloadManagerState state = PluginCoreUtils.unwrap( download ).getDownloadState();

						if ( state.getFlag(DownloadManagerState.FLAG_LOW_NOISE )){
							
							continue;
						}
						
						long rand = global_random_id ^ state.getLongParameter( DownloadManagerState.PARAM_RANDOM_SEED );						
						
						DownloadInfo info = 
							new DownloadInfo(
								hash,
								hash,
								download.getName(),
								(int)rand,
								torrent.isPrivate()?StringInterner.intern(torrent.getAnnounceURL().getHost()):null,
								0,
								torrent.getSize());
						
						new_info.add( info );
						
						if ( initialising || download_infos1.size() == 0 ){
							
							download_infos1.add( info );
							
						}else{
							
							download_infos1.add( RandomUtils.nextInt( download_infos1.size()), info );
						}
						
						download_infos2.add( info );
						
						download_info_map.put( hash, info );
						
						if ( info.getTracker() != null ){
							
							download_priv_set.add( getPrivateInfoKey( info ));
						}
					}
				}catch( Throwable e ){
					
					Debug.out( e );
				}
			}
			
			List<Map<String,Object>> history = (List<Map<String,Object>>)COConfigurationManager.getListParameter( "rcm.dlinfo.history", new ArrayList<Map<String,Object>>());
			
			if ( initialising ){
		
				int padd = MAX_HISTORY - download_info_map.size();
				
				for ( int i=0;i<history.size() && padd > 0;i++ ){
					
					try{
						DownloadInfo info = deserialiseDI((Map<String,Object>)history.get(i), null);
						
						if ( info != null && !download_info_map.containsKey( info.getHash())){
							
							download_info_map.put( info.getHash(), info );
							
							if ( info.getTracker() != null ){
								
								download_priv_set.add( getPrivateInfoKey( info ));
							}
							
							download_infos1.add( info );
							download_infos2.add( info );
							
							padd--;
						}
					}catch( Throwable e ){
						
					}
				}
				
				Collections.shuffle( download_infos1 );
				
			}else{
				
				if ( new_info.size() > 0 ){
					
					for ( DownloadInfo info: new_info ){
						
						Map<String,Object> map = serialiseDI( info, null );
							
						if ( map != null ){
							
							history.add( map );	
						}
					}
					
					while( history.size() > MAX_HISTORY ){
						
						history.remove(0);
					}
					
					COConfigurationManager.setParameter( "rcm.dlinfo.history", history );
				}
			}
		}
	}
	
	protected void
	republish()
	{
		synchronized( this ){

			if( publishing_count > 0 ){
				
				return;
			}
			
			if ( download_infos1.isEmpty()){
				
				List<DownloadInfo> list = download_info_map.values();
				
				download_infos1.addAll( list );
				download_infos2.addAll( list );
				
				Collections.shuffle( download_infos1 );
			}
		}
	}
	
	protected void
	publish()
	{
		while( true ){
			
			DownloadInfo	info1 = null;
			DownloadInfo	info2 = null;

			synchronized( this ){
	
				if ( publishing_count >= MAX_CONCURRENT_PUBLISH ){
					
					return;
				}
				
				if ( download_infos1.isEmpty() || download_info_map.size() == 1 ){
					
					return;
				}
							
				info1 = download_infos1.removeFirst();
				
				Iterator<DownloadInfo> it = download_infos2.iterator();
				
				while( it.hasNext()){
					
					info2 = it.next();
					
					if ( info1 != info2 || download_infos2.size() == 1 ){
						
						it.remove();
						
						break;
					}
				}
				
				if ( info1 == info2 ){
									
					info2 = download_info_map.getRandomValueExcluding( info1 );
					
					if ( info2 == null || info1 == info2 ){
						
						Debug.out( "Inconsistent!" );
						
						return;
					}
				}
				
				publishing_count++;
			}
			
			try{
				publish( info1, info2 );
				
			}catch( Throwable e ){
				
				synchronized( this ){

					publishing_count--;
				}
				
				Debug.out( e );
			}
		}
	}
	
	protected void
	publishNext()
	{
		synchronized( this ){

			publishing_count--;
			
			if ( publishing_count < 0 ){
				
					// shouldn't happen but whatever
				
				publishing_count = 0;
			}
		}
		
		publish();
	}
	
	protected void
	publish(
		final DownloadInfo	from_info,
		final DownloadInfo	to_info )
	
		throws Exception
	{			
		final String from_hash	= ByteFormatter.encodeString( from_info.getHash());
		final String to_hash	= ByteFormatter.encodeString( to_info.getHash());
		
		final byte[] key_bytes	= ( "az:rcm:assoc:" + from_hash ).getBytes( "UTF-8" );
		
		String title = to_info.getTitle(); 
		
		if ( title.length() > MAX_TITLE_LENGTH ){
			
			title = title.substring( 0, MAX_TITLE_LENGTH );
		}
		
		Map<String,Object> map = new HashMap<String,Object>();
		
		map.put( "d", title );
		map.put( "r", new Long( Math.abs( to_info.getRand()%1000 )));
		
		String	tracker = to_info.getTracker();
		
		if ( tracker == null ){
			
			map.put( "h", to_info.getHash());
			
		}else{
			
			map.put( "t", tracker );
		}

		long	size = to_info.getSize();
		
		if ( size != 0 ){
			
			map.put( "s", new Long( size ));
		}
		
		final byte[] map_bytes = BEncoder.encode( map );
		
		final int max_hits = 30;
				
		dht_plugin.get(
				key_bytes,
				"Content relationship read: " + from_hash,
				DHTPlugin.FLAG_SINGLE_VALUE,
				max_hits,
				30*1000,
				false,
				false,
				new DHTPluginOperationListener()
				{
					private boolean diversified;
					private int		hits;
					
					private Set<String>	entries = new HashSet<String>();
					
					public void
					starts(
						byte[]				key )
					{
					}
					
					public void
					diversified()
					{
						diversified = true;
					}
					
					public void
					valueRead(
						DHTPluginContact	originator,
						DHTPluginValue		value )
					{
						try{
							Map<String,Object> map = (Map<String,Object>)BDecoder.decode( value.getValue());
							
							String	title = new String((byte[])map.get( "d" ), "UTF-8" );
							
							String	tracker	= null;
							
							byte[]	hash 	= (byte[])map.get( "h" );
							
							if ( hash == null ){
								
								tracker = new String((byte[])map.get( "t" ), "UTF-8" );
							}
							
							int	rand = ((Long)map.get( "r" )).intValue();
							
							String	key = title + " % " + rand;
							
							synchronized( entries ){
							
								if ( entries.contains( key )){
									
									return;
								}
								
								entries.add( key );
							}
							
							Long	l_size = (Long)map.get( "s" );
							
							long	size = l_size==null?0:l_size.longValue();
							
							analyseResponse( new DownloadInfo( from_info.getHash(), hash, title, rand, tracker, 1, size ), null );
							
						}catch( Throwable e ){							
						}
						
						hits++;
					}
					
					public void
					valueWritten(
						DHTPluginContact	target,
						DHTPluginValue		value )
					{
						
					}
					
					public void
					complete(
						byte[]				key,
						boolean				timeout_occurred )
					{
						boolean	do_it;
						
						if ( diversified ){
							
							do_it = RandomUtils.nextInt( 10 ) == 0;
							
						}else if ( hits <= 10 ){
							
							do_it = true;
							
						}else{
						
							int scaled = 10 * ( hits - 10 ) / ( max_hits - 10 );
							
							do_it = RandomUtils.nextInt( scaled ) == 0;
						}
							
						if ( do_it ){
							
							try{
								dht_plugin.put(
										key_bytes,
										"Content relationship: " +  from_hash + " -> " + to_hash,
										map_bytes,
										DHTPlugin.FLAG_ANON,
										new DHTPluginOperationListener()
										{
											public void
											diversified()
											{
											}
											
											public void 
											starts(
												byte[] 				key ) 
											{
											}
											
											public void
											valueRead(
												DHTPluginContact	originator,
												DHTPluginValue		value )
											{
											}
											
											public void
											valueWritten(
												DHTPluginContact	target,
												DHTPluginValue		value )
											{
											}
											
											public void
											complete(
												byte[]				key,
												boolean				timeout_occurred )
											{
												publishNext();
											}
										});
							}catch( Throwable e ){
								
								Debug.printStackTrace(e);
								
								publishNext();
							}
						}else{
							
							publishNext();
						}
					}
				});
	}
		
	public void
	lookupContent(
		Download							download,
		final RelatedContentLookupListener	listener )
	
		throws ContentException
	{
		Torrent t = download.getTorrent();
		
		if ( t == null ){
			
			throw( new ContentException( "Torrent not available" ));
		}

		final byte[] hash = t.getHash();
		
		if ( 	!initialisation_complete_sem.isReleasedForever() ||
				( dht_plugin != null && dht_plugin.isInitialising())){
			
			AsyncDispatcher dispatcher = new AsyncDispatcher();
	
			dispatcher.dispatch(
				new AERunnable()
				{
					public void
					runSupport()
					{
						try{
							initialisation_complete_sem.reserve();
							
							lookupContentSupport( hash, 0, listener );
							
						}catch( ContentException e ){
							
							Debug.out( e );
						}
					}
				});
		}else{
			
			lookupContentSupport( hash, 0, listener );
		}
	}
	
	private void
	lookupContentSupport(
		final byte[]						from_hash,
		final int							level,
		final RelatedContentLookupListener	listener )
	
		throws ContentException
	{
		try{
			if ( dht_plugin == null ){
				
				throw( new ContentException( "DHT plugin unavailable" ));
			}
			
			final String from_hash_str	= ByteFormatter.encodeString( from_hash );
		
			final byte[] key_bytes	= ( "az:rcm:assoc:" + from_hash_str ).getBytes( "UTF-8" );
			
			final int max_hits = 30;
			
			dht_plugin.get(
					key_bytes,
					"Content relationship read: " + from_hash_str,
					DHTPlugin.FLAG_SINGLE_VALUE,
					max_hits,
					60*1000,
					false,
					true,
					new DHTPluginOperationListener()
					{
						private Set<String>	entries = new HashSet<String>();
						
						private RelatedContentManagerListener manager_listener = 
							new RelatedContentManagerListener()
							{
							private Set<RelatedContent>	content_list = new HashSet<RelatedContent>();
							
								public void
								contentFound(
									RelatedContent[]	content )
								{
									handle( content );
								}
	
								public void
								contentChanged(
									RelatedContent[]	content )
								{
									handle( content );
								}
								
								public void 
								contentRemoved(
									RelatedContent[] 	content ) 
								{
								}
								
								public void
								contentChanged()
								{									
								}
								
								public void
								contentReset()
								{
								}
								
								private void
								handle(
									RelatedContent[]	content )
								{
									synchronized( content_list ){
										
										if ( content_list.contains( content )){
											
											return;
										}
										
										for ( RelatedContent c: content ){
										
											content_list.add( c );
										}
									}
									
									listener.contentFound( content );
								}
							};
						
						public void
						starts(
							byte[]				key )
						{
							if ( listener != null ){
								
								try{
									listener.lookupStart();
									
								}catch( Throwable e ){
									
									Debug.out( e );
								}
							}
						}
						
						public void
						diversified()
						{
						}
						
						public void
						valueRead(
							DHTPluginContact	originator,
							DHTPluginValue		value )
						{
							try{
								Map<String,Object> map = (Map<String,Object>)BDecoder.decode( value.getValue());
								
								String	title = new String((byte[])map.get( "d" ), "UTF-8" );
								
								String	tracker	= null;
								
								byte[]	hash 	= (byte[])map.get( "h" );
								
								if ( hash == null ){
									
									tracker = new String((byte[])map.get( "t" ), "UTF-8" );
								}
								
								int	rand = ((Long)map.get( "r" )).intValue();
								
								String	key = title + " % " + rand;
								
								synchronized( entries ){
								
									if ( entries.contains( key )){
										
										return;
									}
									
									entries.add( key );
								}
								
								Long	l_size = (Long)map.get( "s" );
								
								long	size = l_size==null?0:l_size.longValue();

								analyseResponse( 
									new DownloadInfo( 
										from_hash, hash, title, rand, tracker, level+1, size ),
										listener==null?null:manager_listener );
								
							}catch( Throwable e ){							
							}
						}
						
						public void
						valueWritten(
							DHTPluginContact	target,
							DHTPluginValue		value )
						{
							
						}
						
						public void
						complete(
							byte[]				key,
							boolean				timeout_occurred )
						{
							if ( listener != null ){
								
								try{
									listener.lookupComplete();
									
								}catch( Throwable e ){
									
									Debug.out( e );
								}
							}
						}				
					});
		}catch( Throwable e ){
		
			ContentException	ce;
			
			if ( ( e instanceof ContentException )){
				
				ce = (ContentException)e;
				
			}else{
				ce = new ContentException( "Lookup failed", e );
			}
			
			if ( listener != null ){
				
				try{
					listener.lookupFailed( ce );
					
				}catch( Throwable f ){
					
					Debug.out( f );
				}
			}
			
			throw( ce );
		}
	}
	
	protected void
	popuplateSecondaryLookups(
		ContentCache	content_cache )
	{
		Random rand = new Random();

		secondary_lookups.clear();
		
			// stuff in a couple primarys
		
		List<DownloadInfo> primaries = download_info_map.values();
		
		int	primary_count = primaries.size();
		
		int	primaries_to_add;
		
		if ( primary_count < 2 ){
			
			primaries_to_add = 0;
			
		}else if ( primary_count < 5 ){
			
			if ( rand.nextInt(4) == 0 ){
				
				primaries_to_add = 1;
				
			}else{
				
				primaries_to_add = 0;
			}
		}else if ( primary_count < 10 ){
			
			primaries_to_add = 1;
			
		}else{
			
			primaries_to_add = 2;
		}
		
		if ( primaries_to_add > 0 ){
			
			Set<DownloadInfo> added = new HashSet<DownloadInfo>();
			
			for (int i=0;i<primaries_to_add;i++){
				
				DownloadInfo info = primaries.get( rand.nextInt( primaries.size()));
				
				if ( !added.contains( info )){
					
					added.add( info );
					
					secondary_lookups.addLast(new SecondaryLookup(info.getHash(), info.getLevel()));
				}
			}
		}
		
		Map<String,DownloadInfo>		related_content			= content_cache.related_content;

		Iterator<DownloadInfo> it = related_content.values().iterator();
		
		List<DownloadInfo> secondary_cache_temp = new ArrayList<DownloadInfo>( related_content.size());

		while( it.hasNext()){
			
			DownloadInfo di = it.next();
			
			if ( di.getHash() != null && di.getLevel() < max_search_level ){
					
				secondary_cache_temp.add( di );
			}
		}
						
		final int cache_size = Math.min( secondary_cache_temp.size(), SECONDARY_LOOKUP_CACHE_MAX - secondary_lookups.size());
		
		if ( cache_size > 0 ){
						
			for( int i=0;i<cache_size;i++){
				
				int index = rand.nextInt( secondary_cache_temp.size());
				
				DownloadInfo x = secondary_cache_temp.get( index );
				
				secondary_cache_temp.set( index, secondary_cache_temp.get(i));
				
				secondary_cache_temp.set( i, x );
			}
			
			for ( int i=0;i<cache_size;i++){
				
				DownloadInfo x = secondary_cache_temp.get(i);
				
				secondary_lookups.addLast(new SecondaryLookup(x.getHash(), x.getLevel()));
			}
		}
	}
	
	protected void
	secondaryLookup()
	{
		SecondaryLookup sl;
		
		long	now = SystemTime.getMonotonousTime();
		
		synchronized( this ){
			
			if ( secondary_lookup_in_progress ){
				
				return;
			}
		
			if ( now - secondary_lookup_complete_time < SECONDARY_LOOKUP_PERIOD ){
				
				return;
			}
			
			if ( secondary_lookups.size() == 0 ){
			
				ContentCache cc = content_cache==null?null:content_cache.get();

				if ( cc == null ){
					
						// this will populate the cache
					
					cc = loadRelatedContent();
					
				}else{
					
					popuplateSecondaryLookups( cc );
				}
			}

			if ( secondary_lookups.size() == 0 ){
				
				return;
			}
						
			sl = secondary_lookups.removeFirst();

			secondary_lookup_in_progress = true;
		}
		
		try{
			lookupContentSupport( 
				sl.getHash(),
				sl.getLevel(),
				new RelatedContentLookupListener()
				{
					public void
					lookupStart()
					{	
					}
					
					public void
					contentFound(
						RelatedContent[]	content )
					{	
					}
					
					public void
					lookupComplete()
					{
						next();
					}
					
					public void
					lookupFailed(
						ContentException 	error )
					{
						next();
					}
					
					protected void
					next()
					{
						final SecondaryLookup next_sl;
						
						synchronized( RelatedContentManager.this ){
							
							if ( secondary_lookups.size() == 0 ){
								
								secondary_lookup_in_progress = false;
								
								secondary_lookup_complete_time = SystemTime.getMonotonousTime();
								
								return;
								
							}else{
								
								next_sl = secondary_lookups.removeFirst();
							}
						}
						
						final RelatedContentLookupListener listener = this;
						
						SimpleTimer.addEvent(
							"RCM:SLDelay",
							SystemTime.getOffsetTime( 30*1000 ),
							new TimerEventPerformer()
							{
								public void 
								perform(
									TimerEvent event ) 
								{
									try{					
										lookupContentSupport( next_sl.getHash(), next_sl.getLevel(), listener );
										
									}catch( Throwable e ){
										
										Debug.out( e );
										
										synchronized( RelatedContentManager.this ){
											
											secondary_lookup_in_progress = false;
											
											secondary_lookup_complete_time = SystemTime.getMonotonousTime();
										}
									}
								}
							});
					}
				});
			
		}catch( Throwable e ){
			
			Debug.out( e );
			
			synchronized( this ){
				
				secondary_lookup_in_progress = false;
				
				secondary_lookup_complete_time = now;
			}
		}
	}
	
	protected void
	contentChanged(
		DownloadInfo		info )
	{
		setConfigDirty();
		
		for ( RelatedContentManagerListener l: listeners ){
			
			try{
				l.contentChanged( new RelatedContent[]{ info });
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
	
	protected void
	contentChanged(
		boolean	is_dirty )
	{
		if ( is_dirty ){
		
			setConfigDirty();
		}
		
		for ( RelatedContentManagerListener l: listeners ){
			
			try{
				l.contentChanged();
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
	
	public void
	delete(
		RelatedContent[]	content )
	{
		synchronized( this ){
			
			ContentCache content_cache = loadRelatedContent();
			
			delete( content, content_cache, true );
		}
	}
	
	protected void
	delete(
		final RelatedContent[]	content,
		ContentCache			content_cache,
		boolean					persistent )
	{
		if ( persistent ){
		
			addPersistentlyDeleted( content );
		}
		
		Map<String,DownloadInfo> related_content = content_cache.related_content;

		Iterator<DownloadInfo> it = related_content.values().iterator();
		
		while( it.hasNext()){
		
			DownloadInfo di = it.next();
			
			for ( RelatedContent c: content ){
				
				if ( c == di ){
					
					it.remove();
					
					if ( di.isUnread()){
						
						decrementUnread();
					}
				}
			}
		}
		
		ByteArrayHashMapEx<ArrayList<DownloadInfo>> related_content_map = content_cache.related_content_map;
		
		List<byte[]> delete = new ArrayList<byte[]>();
		
		for ( byte[] key: related_content_map.keys()){
			
			ArrayList<DownloadInfo>	infos = related_content_map.get( key );
			
			for ( RelatedContent c: content ){

				if ( infos.remove( c )){
					
					if ( infos.size() == 0 ){
						
						delete.add( key );
						
						break;
					}
				}
			}
		}
		
		for ( byte[] key: delete ){
			
			related_content_map.remove( key );
		}
		
		setConfigDirty();
		
		content_change_dispatcher.dispatch(
				new AERunnable()
				{
					public void
					runSupport()
					{
						for ( RelatedContentManagerListener l: listeners ){
							
							try{
								l.contentRemoved( content );

							}catch( Throwable e ){
								
								Debug.out( e );
							}
						}
					}
				});
	}
	
	protected String
	getPrivateInfoKey(
		RelatedContent		info )
	{
		return( info.getTitle() + ":" + info.getTracker());
	}
	
	protected void
	analyseResponse(
		DownloadInfo						to_info,
		final RelatedContentManagerListener	listener )
	{
		try{			
			synchronized( this ){
				
				byte[] target = to_info.getHash();
				
				String	key;
				
				if ( target != null ){
					
					if ( download_info_map.containsKey( target )){
						
							// target refers to downoad we already have
						
						return;
					}
					
					key = Base32.encode( target );
					
				}else{
					
					key = getPrivateInfoKey( to_info );
					
					if ( download_priv_set.contains( key )){
						
							// target refers to downoad we already have
						
						return;
					}
				}
				
				if ( isPersistentlyDeleted( to_info )){
					
					return;
				}
				
				ContentCache	content_cache = loadRelatedContent();
				
				DownloadInfo	target_info = null;
				
				boolean	changed_content = false;
				boolean	new_content 	= false;
				
				
				target_info = content_cache.related_content.get( key );
				
				if ( target_info == null ){
								
					if ( enoughSpaceFor( content_cache, to_info )){
					
						target_info = to_info;

						content_cache.related_content.put( key, target_info );
						
						byte[] from_hash = to_info.getRelatedToHash();
						
						ArrayList<DownloadInfo> links = content_cache.related_content_map.get( from_hash );
						
						if ( links == null ){
							
							links = new ArrayList<DownloadInfo>(1);
							
							content_cache.related_content_map.put( from_hash, links );
						}
						
						links.add( target_info );
						
						links.trimToSize();
						
						target_info.setPublic( content_cache );
						
						if ( secondary_lookups.size() < SECONDARY_LOOKUP_CACHE_MAX ){
							
							byte[]	hash 	= target_info.getHash();
							int		level	= target_info.getLevel();
							
							if ( hash != null && level < max_search_level ){
								
								secondary_lookups.add( new SecondaryLookup( hash, level ));
							}
						}
						
						new_content = true;
					}
					
				}else{
					
						// we already know about this, see if new info
					
					changed_content = target_info.addInfo( to_info );
				}

				if ( target_info != null ){
					
					final RelatedContent[]	f_target 	= new RelatedContent[]{ target_info };
					final boolean			f_change	= changed_content;
					
					final boolean something_changed = changed_content || new_content;
							
					if ( something_changed ){
					
						setConfigDirty();
					}
					
					content_change_dispatcher.dispatch(
						new AERunnable()
						{
							public void
							runSupport()
							{
								if ( something_changed ){
									
									for ( RelatedContentManagerListener l: listeners ){
										
										try{
											if ( f_change ){
												
												l.contentChanged( f_target );
												
											}else{
												
												l.contentFound( f_target );
											}
										}catch( Throwable e ){
											
											Debug.out( e );
										}
									}
								}
								
								if ( listener != null ){
									
									try{
										if ( f_change ){
											
											listener.contentChanged( f_target );
											
										}else{
											
											listener.contentFound( f_target );
										}
									}catch( Throwable e ){
										
										Debug.out( e );
									}
								}
							}
						});
				}
			}
			
		}catch( Throwable e ){
			
			Debug.out( e );
		}
	}
	
	protected boolean
	enoughSpaceFor(
		ContentCache	content_cache,
		DownloadInfo	fi )
	{
		Map<String,DownloadInfo> related_content = content_cache.related_content;
		
		if ( related_content.size() < max_results ){
			
			return( true );
		}
		
		Iterator<Map.Entry<String,DownloadInfo>>	it = related_content.entrySet().iterator();
				
		int	level 		= fi.getLevel();
		
			// delete oldest at highest level >= level with minimum rank
	
		Map<Integer,DownloadInfo>	oldest_per_rank = new HashMap<Integer, DownloadInfo>();
		
		int	min_rank = Integer.MAX_VALUE;
		
		while( it.hasNext()){
			
			Map.Entry<String,DownloadInfo> entry = it.next();
			
			DownloadInfo info = entry.getValue();
			
			int	info_level = info.getLevel();
			
			if ( info_level >= level ){
				
				if ( info_level > level ){
					
					level = info_level;
					
					min_rank = Integer.MAX_VALUE;
					
					oldest_per_rank.clear();
				}
				
				int	rank = info.getRank();
				
				if ( rank < min_rank ){
					
					min_rank = rank;
				}
				
				DownloadInfo oldest = oldest_per_rank.get( rank );
				
				if ( oldest == null ){
					
					oldest_per_rank.put( rank, info );
					
				}else{
					
					if ( info.getLastSeenSecs() < oldest.getLastSeenSecs()){
						
						oldest_per_rank.put( rank, info );
					}
				}
			}
		}
		
		DownloadInfo to_remove = oldest_per_rank.get( min_rank );
		
		if ( to_remove != null ){
					
			delete( new RelatedContent[]{ to_remove }, content_cache, false );
			
			return( true );
		}
		
		return( false );
	}
	
	public RelatedContent[]
	getRelatedContent()
	{
		synchronized( this ){

			ContentCache	content_cache = loadRelatedContent();
			
			return( content_cache.related_content.values().toArray( new DownloadInfo[ content_cache.related_content.size()]));
		}
	}
	
	public void
	reset()
	{
		synchronized( this ){
			
			ContentCache cc = content_cache==null?null:content_cache.get();
			
			if ( cc == null ){
				
				FileUtil.deleteResilientConfigFile( CONFIG_FILE );
				
			}else{
			
				cc.related_content 		= new HashMap<String,DownloadInfo>();
				cc.related_content_map 	= new ByteArrayHashMapEx<ArrayList<DownloadInfo>>();
			}
			
			download_infos1.clear();
			download_infos2.clear();
			
			List<DownloadInfo> list = download_info_map.values();
			
			download_infos1.addAll( list );
			download_infos2.addAll( list );
			
			Collections.shuffle( download_infos1 );
			
			total_unread = 0;
			
			resetPersistentlyDeleted();
			
			setConfigDirty();
		}
		
		for ( RelatedContentManagerListener l: listeners ){
			
			l.contentReset();
		}
	}
	
	protected void
	setConfigDirty()
	{
		synchronized( this ){
			
			content_dirty	= true;
		}
	}
	
	protected ContentCache
	loadRelatedContent()
	{
		boolean	fire_event = false;
		
		try{
			synchronized( this ){
	
				last_config_access = SystemTime.getMonotonousTime();
	
				ContentCache cc = content_cache==null?null:content_cache.get();
				
				if ( cc == null ){
				
					if ( TRACE ){
						System.out.println( "rcm: load new" );
					}
					
					fire_event = true;
					
					cc = new ContentCache();
		
					content_cache = new WeakReference<ContentCache>( cc );
					
					try{
						int	new_total_unread = 0;
		
						if ( FileUtil.resilientConfigFileExists( CONFIG_FILE )){
											
							Map map = FileUtil.readResilientConfigFile( CONFIG_FILE );
							
							Map<String,DownloadInfo>						related_content			= cc.related_content;
							ByteArrayHashMapEx<ArrayList<DownloadInfo>>		related_content_map		= cc.related_content_map;
		
							
							Map<String,String>	rcm_map = (Map<String,String>)map.get( "rcm" );
							
							Map<String,Map<String,Object>>	rc_map 	= (Map<String,Map<String,Object>>)map.get( "rc" );
							
							if ( rc_map != null && rcm_map != null ){
								
								Map<Integer,DownloadInfo> id_map = new HashMap<Integer, DownloadInfo>();
													
								for ( Map.Entry<String,Map<String,Object>> entry: rc_map.entrySet()){
									
									try{
									
										String	key = entry.getKey();
									
										Map<String,Object>	info_map = entry.getValue();
																	
										DownloadInfo info = deserialiseDI( info_map, cc );
										
										if ( info.isUnread()){
											
											new_total_unread++;
										}
										
										related_content.put( key, info );
										
										int	id = ((Long)info_map.get( "_i" )).intValue();
			
										id_map.put( id, info );
										
									}catch( Throwable e ){
										
									}
								}
								
								if ( rcm_map.size() != 0 && rc_map.size() != 0 ){
									
									for ( String key: rcm_map.keySet()){
										
										try{
											byte[]	hash = Base32.decode( key );
											
											int[]	ids = ImportExportUtils.importIntArray( rcm_map, key );
											
											if ( ids == null || ids.length == 0 ){
												
												Debug.out( "Inconsistent - no ids" );
												
											}else{
												
												ArrayList<DownloadInfo>	di_list = new ArrayList<DownloadInfo>(ids.length);
												
												for ( int id: ids ){
													
													DownloadInfo di = id_map.get( id );
													
													if ( di == null ){
														
														Debug.out( "Inconsistent: id " + id + " missing" );
														
													}else{
														
															// we don't currently remember all originators, just one that works 
															
														di.setRelatedToHash( hash );
														
														di_list.add( di );
													}
												}
												
												if ( di_list.size() > 0 ){
													
													related_content_map.put( hash, di_list );
												}
											}
										}catch( Throwable e ){
											
											Debug.out( e );
										}
									}
								}
								
								Iterator<DownloadInfo> it = related_content.values().iterator();
								
								while( it.hasNext()){
									
									DownloadInfo di = it.next();
									
									if ( di.getRelatedToHash() == null ){
								
										Debug.out( "Inconsistent: info not referenced" );
										
										if ( di.isUnread()){
											
											new_total_unread--;
										}
										
										it.remove();
									}
								}
								
								popuplateSecondaryLookups( cc );
							}
						}
						
						if ( total_unread != new_total_unread ){
														
							Debug.out( "total_unread - inconsistent (" + total_unread + "/" + new_total_unread + ")" );
							
							total_unread = new_total_unread;
							
							COConfigurationManager.setParameter( CONFIG_TOTAL_UNREAD, total_unread );
						}
					}catch( Throwable e ){
						
						Debug.out( e );
					}
				}else{
					
					if ( TRACE ){
						System.out.println( "rcm: load existing" );
					}
				}
				
				content_cache_ref = cc;
				
				return( cc );
			}
		}finally{
			
			if ( fire_event ){
				
				contentChanged( false );
			}
		}
	}
	
	protected void
	saveRelatedContent()
	{
		synchronized( this ){
				
			COConfigurationManager.setParameter( CONFIG_TOTAL_UNREAD, total_unread );
			
			long	now = SystemTime.getMonotonousTime();;
			
			ContentCache cc = content_cache==null?null:content_cache.get();
			
			if ( !content_dirty ){
					
				if ( cc != null  ){
					
					if ( now - last_config_access > CONFIG_DISCARD_MILLIS ){
					
						if ( content_cache_ref != null ){
							
							content_discard_ticks = 0;
						}
						
						if ( TRACE ){
							System.out.println( "rcm: discard: tick count=" + content_discard_ticks++ );
						}
						
						content_cache_ref	= null;
					}
				}else{
					
					if ( TRACE ){
						System.out.println( "rcm: discarded" );
					}
				}
				
				return;
			}
			
			last_config_access = now;
			
			content_dirty	= false;
			
			if ( cc == null ){
				
				Debug.out( "RCM: cache inconsistent" );
				
			}else{

				if ( TRACE ){
					System.out.println( "rcm: save" );
				}
				
				Map<String,DownloadInfo>						related_content			= cc.related_content;
				ByteArrayHashMapEx<ArrayList<DownloadInfo>>		related_content_map		= cc.related_content_map;

				if ( related_content.size() == 0 ){
					
					FileUtil.deleteResilientConfigFile( CONFIG_FILE );
					
				}else{
					
					Map<String,Object>	map = new HashMap<String, Object>();
					
					Set<Map.Entry<String,DownloadInfo>> rcs = related_content.entrySet();
					
						// key may be non ascii so use ByteEncodedKeyHashMap to force 
						// sensible behaviour (don't ask)
					
					Map<String,Object> rc_map = new ByteEncodedKeyHashMap<String, Object>();
					
					map.put( "rc", rc_map );
					
					int		id = 0;
					
					Map<DownloadInfo,Integer>	info_map = new HashMap<DownloadInfo, Integer>();
					
					for ( Map.Entry<String,DownloadInfo> entry: rcs ){
											
						DownloadInfo	info = entry.getValue();
												
						Map<String,Object> di_map = serialiseDI( info, cc );
						
						if ( di_map != null ){
							
							info_map.put( info, id );

							di_map.put( "_i", new Long( id ));
							
							String	key = entry.getKey();

							rc_map.put( key, di_map );
	
							id++;	
						}
					}
					
					Map<String,Object> rcm_map = new HashMap<String, Object>();

					map.put( "rcm", rcm_map );
										
					for ( byte[] hash: related_content_map.keys()){
						
						List<DownloadInfo> dis = related_content_map.get( hash );
						
						int[] ids = new int[dis.size()];
						
						int	pos = 0;
						
						for ( DownloadInfo di: dis ){
							
							Integer	index = info_map.get( di );
							
							if ( index == null ){
								
								Debug.out( "inconsistent: info missing for " + di );
								
								break;
								
							}else{
								
								ids[pos++] = index;
							}
						}
						
						if ( pos == ids.length ){
						
							ImportExportUtils.exportIntArray( rcm_map, Base32.encode( hash), ids );
						}
					}
					
					FileUtil.writeResilientConfigFile( CONFIG_FILE, map );
				}
			}
		}
	}
	
	
	public int
	getNumUnread()
	{
		synchronized( this ){
		
			return( total_unread );
		}
	}
	
	public void
	setAllRead()
	{
		boolean	changed = false;
		
		synchronized( this ){
			
			DownloadInfo[] content = (DownloadInfo[])getRelatedContent();
			
			for ( DownloadInfo c: content ){
				
				if ( c.isUnread()){
				
					changed = true;
					
					c.setUnreadInternal( false );
				}
			}
			
			total_unread = 0;
		}
		
		if ( changed ){
		
			contentChanged( true );
		}
	}
	
	protected void
	incrementUnread()
	{
		synchronized( this ){
			
			total_unread++;
		}
	}
	
	protected void
	decrementUnread()
	{
		synchronized( this ){
			
			total_unread--;
			
			if ( total_unread < 0 ){
				
				Debug.out( "inconsistent" );
				
				total_unread = 0;
			}
		}
	}
	
	protected Download
	getDownload(
		byte[]	hash )
	{
		try{
			return( plugin_interface.getDownloadManager().getDownload( hash ));
			
		}catch( Throwable e ){
			
			return( null );
		}
	}
	
	private static final int PD_BLOOM_INITIAL_SIZE		= 1000;
	private static final int PD_BLOOM_INCREMENT_SIZE	= 1000;
	
	
	private BloomFilter	persist_del_bloom;
	
	protected byte[]
	getPermDelKey(
		RelatedContent	info )
	{
		byte[]	bytes = info.getHash();
		
		if ( bytes == null ){
			
			try{
				bytes = new SHA1Simple().calculateHash( getPrivateInfoKey(info).getBytes( "ISO-8859-1" ));
				
			}catch( Throwable e ){
				
				Debug.out( e );
				
				return( null );
			}
		}
		
		byte[] key = new byte[8];
		
		System.arraycopy( bytes, 0, key, 0, 8 );
		
		return( key );
	}
	
	protected void
	addPersistentlyDeleted(
		RelatedContent[]	content )
	{		
		if ( content.length == 0 ){
			
			return;
		}
	
		List<byte[]> entries = new ArrayList<byte[]>(0);
			
		if ( FileUtil.resilientConfigFileExists( PERSIST_DEL_FILE )){
				
			Map<String,Object> map = (Map<String,Object>)FileUtil.readResilientConfigFile( PERSIST_DEL_FILE );
				
			entries = (List<byte[]>)map.get( "entries" );
		}
	
		List<byte[]> new_keys = new ArrayList<byte[]>( content.length );
		
		for ( RelatedContent rc: content ){
			
			byte[] key = getPermDelKey( rc );
			
			new_keys.add( key );
			
			entries.add( key );
		}
		
		Map<String,Object>	map = new HashMap<String, Object>();
		
		map.put( "entries", entries );
		
		FileUtil.writeResilientConfigFile( PERSIST_DEL_FILE, map );
		
		if ( persist_del_bloom != null ){
			
			if ( persist_del_bloom.getSize() / ( persist_del_bloom.getEntryCount() + content.length ) < 10 ){
		
				persist_del_bloom = BloomFilterFactory.createAddOnly( Math.max( PD_BLOOM_INITIAL_SIZE, persist_del_bloom.getSize() *10 + PD_BLOOM_INCREMENT_SIZE + content.length  ));
				
				for ( byte[] k: entries ){
					
					persist_del_bloom.add( k );
				}
			}else{
				
				for ( byte[] k: new_keys ){
					
					persist_del_bloom.add( k );
				}
			}
		}
	}
	
	protected boolean
	isPersistentlyDeleted(
		RelatedContent		content )
	{
		if ( persist_del_bloom == null ){
			
			List<byte[]> entries = new ArrayList<byte[]>(0);

			if ( FileUtil.resilientConfigFileExists( PERSIST_DEL_FILE )){
				
				Map<String,Object> map = (Map<String,Object>)FileUtil.readResilientConfigFile( PERSIST_DEL_FILE );
				
				entries = (List<byte[]>)map.get( "entries" );
			}
			
			persist_del_bloom = BloomFilterFactory.createAddOnly( Math.max( PD_BLOOM_INITIAL_SIZE, entries.size()*10 + PD_BLOOM_INCREMENT_SIZE ));

			for ( byte[] k: entries ){
				
				persist_del_bloom.add( k );
			}
		}
		
		byte[]	key = getPermDelKey( content );
		
		return( persist_del_bloom.contains( key ));
	}

	protected void
	resetPersistentlyDeleted()
	{
		FileUtil.deleteResilientConfigFile( PERSIST_DEL_FILE );
		
		persist_del_bloom = BloomFilterFactory.createAddOnly( PD_BLOOM_INITIAL_SIZE );
	}
	
	public void
	addListener(
		RelatedContentManagerListener		listener )
	{
		listeners.add( listener );
	}
	
	public void
	removeListener(
		RelatedContentManagerListener		listener )
	{
		listeners.remove( listener );
	}
	
	protected static class
	ByteArrayHashMapEx<T>
		extends ByteArrayHashMap<T>
	{
	    public T
	    getRandomValueExcluding(
	    	T	excluded )
	    {
	    	int	num = RandomUtils.nextInt( size );
	    	
	    	T result = null;
	    	
	        for (int j = 0; j < table.length; j++) {
	        	
		         Entry<T> e = table[j];
		         
		         while( e != null ){
		        	 
	              	T value = e.value;
	               	
	              	if ( value != excluded ){
	              		
	              		result = value;
	              	}
	              	
	              	if ( num <= 0 && result != null ){
	              		
	              		return( result );
	              	}
	              	
	              	num--;
	              	
	              	e = e.next;
		        }
		    }
	    
	        return( result );
	    }
	}
	
	private Map<String,Object>
	serialiseDI(
		DownloadInfo			info,
		ContentCache			cc )
	{
		try{
			Map<String,Object> info_map = new HashMap<String,Object>();
			
			info_map.put( "h", info.getHash());
			
			ImportExportUtils.exportString( info_map, "d", info.getTitle());
			ImportExportUtils.exportInt( info_map, "r", info.getRand());
			ImportExportUtils.exportString( info_map, "t", info.getTracker());
			ImportExportUtils.exportLong( info_map, "z", info.getSize());
			
			if ( cc != null ){
							
				ImportExportUtils.exportBoolean( info_map, "u", info.isUnread());
				ImportExportUtils.exportIntArray( info_map, "l", info.getRandList());
				ImportExportUtils.exportInt( info_map, "s", info.getLastSeenSecs());
				ImportExportUtils.exportInt( info_map, "e", info.getLevel());
			}
			
			return( info_map );
			
		}catch( Throwable e ){
			
			return( null );
		}
	}
	
	private DownloadInfo
	deserialiseDI(
		Map<String,Object>		info_map,
		ContentCache			cc )
	{
		try{
			byte[]	hash 	= (byte[])info_map.get("h");
			String	title	= ImportExportUtils.importString( info_map, "d" );
			int		rand	= ImportExportUtils.importInt( info_map, "r" );
			String	tracker	= ImportExportUtils.importString( info_map, "t" );
			long	size	= ImportExportUtils.importLong( info_map, "z" );
			
			if ( cc == null ){
			
				return( new DownloadInfo( hash, hash, title, rand, tracker, 0, size ));
				
			}else{
				
				boolean unread = ImportExportUtils.importBoolean( info_map, "u" );
				
				int[] rand_list = ImportExportUtils.importIntArray( info_map, "l" );
				
				int	last_seen = ImportExportUtils.importInt( info_map, "s" );
				
				int	level = ImportExportUtils.importInt( info_map, "e" );
				
				return( new DownloadInfo( hash, title, rand, tracker, unread, rand_list, last_seen, level, size, cc ));
			}
		}catch( Throwable e ){
			
			return( null );
		}
	}
	
	protected class
	DownloadInfo
		extends RelatedContent
	{
		final private int		rand;
		
		private boolean			unread	= true;
		private int[]			rand_list;
		private int				last_seen;
		private int				level;
		
		private ContentCache	cc;
		
		protected
		DownloadInfo(
			byte[]		_related_to,
			byte[]		_hash,
			String		_title,
			int			_rand,
			String		_tracker,
			int			_level,
			long		_size )
		{
			super( _related_to, _title, _hash, _tracker, _size );
			
			rand		= _rand;
			level		= _level;
			
			updateLastSeen();
		}
		
		protected
		DownloadInfo(
			byte[]			_hash,
			String			_title,
			int				_rand,
			String			_tracker,
			boolean			_unread,
			int[]			_rand_list,
			int				_last_seen,
			int				_level,
			long			_size,
			ContentCache	_cc )
		{
			super( _title, _hash, _tracker, _size );
			
			rand		= _rand;
			unread		= _unread;
			rand_list	= _rand_list;
			last_seen	= _last_seen;
			level		= _level;
			cc			= _cc;
		}
		
		protected boolean
		addInfo(
			DownloadInfo		info )
		{
			boolean	result = false;
			
			synchronized( this ){
		
				updateLastSeen();
				
				int r = info.getRand();
				
				if ( rand_list == null ){
					
					rand_list = new int[]{ r };
										
					result	= true;
					
				}else{
					
					boolean	match = false;
					
					for (int i=0;i<rand_list.length;i++){
						
						if ( rand_list[i] == r ){
							
							match = true;
							
							break;
						}
					}
					
					if ( !match ){
						
						int	len = rand_list.length;
						
						int[]	new_rand_list = new int[len+1];
						
						System.arraycopy( rand_list, 0, new_rand_list, 0, len );
						
						new_rand_list[len] = r;
						
						rand_list = new_rand_list;
						
						result = true;
					}
				}
				
				if ( info.getLevel() < level ){
					
					level = info.getLevel();
					
					result = true;
				}
			}
			
			return( result );
		}
		
		public int
		getLevel()
		{
			return( level );
		}
		
		protected void
		updateLastSeen()
		{
				// persistence of this is piggy-backed on other saves to limit resource usage
				// only therefore a vague measure

			last_seen	= (int)( SystemTime.getCurrentTime()/1000 );
		}
		
		public int
		getRank()
		{
			return( rand_list==null?0:rand_list.length );
		}
		
		public boolean
		isUnread()
		{
			return( unread );
		}
		
		protected void
		setPublic(
			ContentCache	_cc )
		{
			cc	= _cc;
			
			if ( unread ){
				
				incrementUnread();
			}
			
			rand_list = new int[]{ rand };
		}
		
		public int
		getLastSeenSecs()
		{
			return( last_seen );
		}
		
		protected void
		setUnreadInternal(
			boolean	_unread )
		{
			synchronized( this ){

				unread = _unread;
			}
		}
		
		public void
		setUnread(
			boolean	_unread )
		{
			boolean	changed = false;
			
			synchronized( this ){
				
				if ( unread != _unread ){
				
					unread = _unread;
					
					changed = true;
				}
			}
			
			if ( changed ){
			
				if ( _unread ){
					
					incrementUnread();
					
				}else{
					
					decrementUnread();
				}
				
				contentChanged( this );
			}
		}
		
		protected int
		getRand()
		{
			return( rand );
		}
		
		protected int[]
		getRandList()
		{
			return( rand_list );
		}
		
		public Download 
		getRelatedToDownload() 
		{
			try{
				return( getDownload( getRelatedToHash()));
				
			}catch( Throwable e ){
				
				Debug.out( e );
				
				return( null );
			}
		}
		
		public void 
		delete() 
		{
			RelatedContentManager.this.delete( new RelatedContent[]{ this });
		}
		
		public String
		getString()
		{
			return( super.getString() + ", " + rand );
		}
	}
	
	private static class
	ContentCache
	{
		private Map<String,DownloadInfo>						related_content			= new HashMap<String, DownloadInfo>();
		private ByteArrayHashMapEx<ArrayList<DownloadInfo>>		related_content_map		= new ByteArrayHashMapEx<ArrayList<DownloadInfo>>();
	}
	
	private static class
	SecondaryLookup
	{
		final private byte[]	hash;
		final private int		level;
		
		protected
		SecondaryLookup(
			byte[]		_hash,
			int			_level )
		{
			hash	= _hash;
			level	= _level;
		}
		
		protected byte[]
		getHash()
		{
			return( hash );
		}
		
		protected int
		getLevel()
		{
			return( level );
		}
	}
}
