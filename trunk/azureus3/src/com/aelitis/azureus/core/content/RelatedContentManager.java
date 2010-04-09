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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.ByteArrayHashMap;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Constants;
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
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.ddb.DistributedDatabase;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseContact;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseException;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseKey;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseProgressListener;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseTransferHandler;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseTransferType;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseValue;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.utils.search.SearchException;
import org.gudy.azureus2.plugins.utils.search.SearchInstance;
import org.gudy.azureus2.plugins.utils.search.SearchObserver;
import org.gudy.azureus2.plugins.utils.search.SearchProvider;
import org.gudy.azureus2.plugins.utils.search.SearchResult;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.udp.DHTTransportUDP;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.core.util.FeatureAvailability;
import com.aelitis.azureus.core.util.bloom.BloomFilter;
import com.aelitis.azureus.core.util.bloom.BloomFilterFactory;
import com.aelitis.azureus.plugins.dht.DHTPlugin;
import com.aelitis.azureus.plugins.dht.DHTPluginContact;
import com.aelitis.azureus.plugins.dht.DHTPluginOperationListener;
import com.aelitis.azureus.plugins.dht.DHTPluginValue;
import com.aelitis.azureus.util.ImportExportUtils;
import com.aelitis.net.magneturi.MagnetURIHandler;

public class 
RelatedContentManager
	implements DistributedDatabaseTransferHandler
{
	private static final boolean 	TRACE = false;
	public static final boolean	DISABLE_ALL_UI	= !Constants.isCVSVersion() && COConfigurationManager.getStringParameter("ui", "az3").equals("az3");

	private static final int	MAX_HISTORY					= 16;
	private static final int	MAX_TITLE_LENGTH			= 80;
	private static final int	MAX_CONCURRENT_PUBLISH		= 2;
	private static final int	MAX_REMOTE_SEARCH_RESULTS	= 30;
	private static final int	MAX_REMOTE_SEARCH_CONTACTS	= 50;
	private static final int	MAX_REMOTE_SEARCH_MILLIS	= 25*1000;
	
	private static final int	TEMPORARY_SPACE_DELTA	= 50;
	
	private static final int	MAX_RANK	= 100;
	
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
	
	private final boolean	enabled;
	
	private boolean	ui_enabled;
	private int		max_search_level;
	private int		max_results;
	
	private AtomicInteger	temporary_space = new AtomicInteger();
	
	private int publishing_count = 0;
	
	private CopyOnWriteList<RelatedContentManagerListener>	listeners = new CopyOnWriteList<RelatedContentManagerListener>();
	
	private AESemaphore initialisation_complete_sem = new AESemaphore( "RCM:init" );

	private static final int TIMER_PERIOD			= 30*1000;
	private static final int CONFIG_SAVE_PERIOD		= 60*1000;
	private static final int CONFIG_SAVE_TICKS		= CONFIG_SAVE_PERIOD/TIMER_PERIOD;
	private static final int PUBLISH_CHECK_PERIOD	= 30*1000;
	private static final int PUBLISH_CHECK_TICKS	= PUBLISH_CHECK_PERIOD/TIMER_PERIOD;
	private static final int SECONDARY_LOOKUP_PERIOD	= 15*60*1000;
	private static final int SECONDARY_LOOKUP_TICKS		= SECONDARY_LOOKUP_PERIOD/TIMER_PERIOD;
	private static final int REPUBLISH_PERIOD			= 8*60*60*1000;
	private static final int REPUBLISH_TICKS			= REPUBLISH_PERIOD/TIMER_PERIOD;

	private static final int INITIAL_PUBLISH_DELAY	= 3*60*1000;
	private static final int INITIAL_PUBLISH_TICKS	= INITIAL_PUBLISH_DELAY/TIMER_PERIOD;
	
	
	
	private static final int CONFIG_DISCARD_MILLIS	= 60*1000;
	
	private ContentCache				content_cache_ref;
	private WeakReference<ContentCache>	content_cache;
	
	private boolean		content_dirty;
	private long		last_config_access;		
	private int			content_discard_ticks;
	
	private AtomicInteger	total_unread = new AtomicInteger( COConfigurationManager.getIntParameter( CONFIG_TOTAL_UNREAD, 0 ));
	
	private AsyncDispatcher	content_change_dispatcher = new AsyncDispatcher();
	
	private static final int SECONDARY_LOOKUP_CACHE_MAX = 10;
	
	private LinkedList<SecondaryLookup> secondary_lookups = new LinkedList<SecondaryLookup>();
	
	private boolean	secondary_lookup_in_progress;
	private long	secondary_lookup_complete_time;
	
	private DistributedDatabase		ddb;
	private RCMSearchXFer			transfer_type = new RCMSearchXFer();
	
	protected
	RelatedContentManager()
	
		throws ContentException
	{
		if ( !FeatureAvailability.isRCMEnabled()){
			
			enabled		= false;
			ui_enabled 	= false;
			
			return;
		}
		
		enabled = true;
		
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
					"rcm.ui.enabled",
					"rcm.max_search_level",
					"rcm.max_results",
				},
				new ParameterListener()
				{
					public void 
					parameterChanged(
						String name )
					{
						ui_enabled 			= COConfigurationManager.getBooleanParameter( "rcm.ui.enabled", true ) && !DISABLE_ALL_UI;
						max_search_level 	= COConfigurationManager.getIntParameter( "rcm.max_search_level", 3 );
						max_results		 	= COConfigurationManager.getIntParameter( "rcm.max_results", 500 );
					}
				});
			
			if ( enabled ){
				
				if ( ui_enabled ){

					try{
						plugin_interface.getUtilities().registerSearchProvider(
							new SearchProvider()
							{
								private Map<Integer,Object>	properties = new HashMap<Integer, Object>();
								
								{
									properties.put( PR_NAME, MessageText.getString( "rcm.search.provider" ));
									
									try{
										URL url = 
											MagnetURIHandler.getSingleton().registerResource(
												new MagnetURIHandler.ResourceProvider()
												{
													public String
													getUID()
													{
														return( RelatedContentManager.class.getName() + ".1" );
													}
													
													public String
													getFileType()
													{
														return( "png" );
													}
															
													public byte[]
													getData()
													{
														InputStream is = getClass().getClassLoader().getResourceAsStream( "org/gudy/azureus2/ui/icons/rcm.png" );
														
														if ( is == null ){
															
															return( null );
														}
														
														try{
															ByteArrayOutputStream	baos = new ByteArrayOutputStream();
															
															try{
																byte[]	buffer = new byte[8192];
																
																while( true ){
										
																	int	len = is.read( buffer );
													
																	if ( len <= 0 ){
																		
																		break;
																	}
											
																	baos.write( buffer, 0, len );
																}
															}finally{
																
																is.close();
															}
															
															return( baos.toByteArray());
															
														}catch( Throwable e ){
															
															return( null );
														}
													}
												});
																				
										properties.put( PR_ICON_URL, url.toExternalForm());
										
									}catch( Throwable e ){
										
										Debug.out( e );
									}
								}
								
								public SearchInstance
								search(
									Map<String,Object>	search_parameters,
									SearchObserver		observer )
								
									throws SearchException
								{
									initialisation_complete_sem.reserve();
									
									return( searchRCM( search_parameters, observer ));
								}
								
								public Object
								getProperty(
									int			property )
								{
									return( properties.get( property ));
								}
								
								public void
								setProperty(
									int			property,
									Object		value )
								{
									properties.put( property, value );
								}
							});
					}catch( Throwable e ){
						
						Debug.out( "Failed to register search provider" );
					}
				}
			}
			
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
		
											if ( !dht_plugin.isEnabled()){
												
												return;
											}
											
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
														tick_count++;

														if ( tick_count == 1 ){
															
															try{
																ddb = plugin_interface.getDistributedDatabase();
															
																ddb.addTransferHandler( transfer_type, RelatedContentManager.this );
																
															}catch( Throwable e ){
																
																Debug.out( e );
															}
														}
														
														if ( enabled ){
																
															if ( tick_count >= INITIAL_PUBLISH_TICKS ){
																
																if ( tick_count % PUBLISH_CHECK_TICKS == 0 ){
																
																	publish();
																}
																
																if ( tick_count % SECONDARY_LOOKUP_TICKS == 0 ){
	
																	secondaryLookup();
																}
																
																if ( tick_count % REPUBLISH_TICKS == 0 ){
	
																	republish();
																}
																
																if ( tick_count % CONFIG_SAVE_TICKS == 0 ){
																	
																	saveRelatedContent();
																}
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
	
	public boolean
	isUIEnabled()
	{
		return( ui_enabled );
	}
	
	public void
	setUIEnabled(
		boolean		_ui_enabled )
	{
		COConfigurationManager.setParameter( "rcm.ui.enabled", _ui_enabled );
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
		
		enforceMaxResults( false );
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
					
					TOTorrent to_torrent = PluginCoreUtils.unwrap( torrent );
					
					if ( public_net && !TorrentUtils.isReallyPrivate( to_torrent )){
						
						DownloadManagerState state = PluginCoreUtils.unwrap( download ).getDownloadState();

						if ( state.getFlag(DownloadManagerState.FLAG_LOW_NOISE )){
							
							continue;
						}
						
						long rand = global_random_id ^ state.getLongParameter( DownloadManagerState.PARAM_RANDOM_SEED );						
						
						long cache = state.getLongAttribute( DownloadManagerState.AT_SCRAPE_CACHE );

						int	seeds_leechers;
						
						if ( cache == -1 ){
							
							seeds_leechers = -1;
							
						}else{
							
							int seeds 		= (int)((cache>>32)&0x00ffffff);
							int leechers 	= (int)(cache&0x00ffffff);
							
							seeds_leechers 	= (int)((seeds<<16)|(leechers&0xffff));
						}

						DownloadInfo info = 
							new DownloadInfo(
								hash,
								hash,
								download.getName(),
								(int)rand,
								torrent.isPrivate()?StringInterner.intern(torrent.getAnnounceURL().getHost()):null,
								0,
								false,
								torrent.getSize(),
								(int)( to_torrent.getCreationDate()/(60*60)),
								seeds_leechers,
								(byte)PlatformTorrentUtils.getContentNetworkID( to_torrent ));
						
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

		if ( to_info.getLevel() == 0 ){
			
			try{
				Download d = to_info.getRelatedToDownload();
			
				if ( d != null ){
					
					Torrent torrent = d.getTorrent();
					
					if ( torrent != null ){
						
						long cnet = PlatformTorrentUtils.getContentNetworkID( PluginCoreUtils.unwrap( torrent ));
						
						if ( cnet != ContentNetwork.CONTENT_NETWORK_UNKNOWN ){
							
							map.put( "c", new Long( cnet ));
						}
						
						long secs = torrent.getCreationDate();
						
						long hours = secs/(60*60);
						
						if ( hours > 0 ){
							
							map.put( "p", new Long( hours ));
						}
					}
										
					int leechers 	= -1;
					int seeds 		= -1;
					
					long cache = PluginCoreUtils.unwrap( d ).getDownloadState().getLongAttribute( DownloadManagerState.AT_SCRAPE_CACHE );
						
					if ( cache != -1 ){
							
						seeds 		= (int)((cache>>32)&0x00ffffff);
						leechers 	= (int)(cache&0x00ffffff);
					}
					
					if ( leechers > 0 ){
						map.put( "l", new Long( leechers ));
					}
					if ( seeds > 0 ){
						map.put( "z", new Long( seeds ));
					}					
				}
			}catch( Throwable e ){		
			}
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
							
							Long	cnet	 	= (Long)map.get( "c" );
							Long	published 	= (Long)map.get( "p" );
							Long	leechers 	= (Long)map.get( "l" );
							Long	seeds	 	= (Long)map.get( "z" );
							
							// System.out.println( "p=" + published + ", l=" + leechers + ", s=" + seeds );
							
							int	seeds_leechers;
							
							if ( leechers == null && seeds == null ){
								
								seeds_leechers = -1;
								
							}else if ( leechers == null ){
								
								seeds_leechers = seeds.intValue()<<16;
								
							}else if ( seeds == null ){
								
								seeds_leechers = leechers.intValue()&0xffff;
								
							}else{
								
								seeds_leechers = (seeds.intValue()<<16)|(leechers.intValue()&0xffff);
							}
								
							analyseResponse( 
								new DownloadInfo( 
										from_info.getHash(), hash, title, rand, tracker, 1, false, size, 
										published==null?0:published.intValue(),
										seeds_leechers,
										(byte)(cnet==null?ContentNetwork.CONTENT_NETWORK_UNKNOWN:cnet.byteValue())), 
								null );
							
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
						
						// System.out.println( from_hash + ": hits=" + hits + ", div=" + diversified );
						
						if ( diversified || hits >= 10 ){
							
							do_it = false;
							
						}else if ( hits <= 5 ){
							
							do_it = true;
														
						}else{
													
							do_it = RandomUtils.nextInt( hits - 5 + 1 ) == 0;
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
		final byte[]						hash,
		final RelatedContentLookupListener	listener )
	
		throws ContentException
	{
		if ( hash == null ){
			
			throw( new ContentException( "hash is null" ));
		}

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
							
							lookupContentSupport( hash, 0, true, listener );
							
						}catch( ContentException e ){
							
							Debug.out( e );
						}
					}
				});
		}else{
			
			lookupContentSupport( hash, 0, true, listener );
		}
	}
	
	private void
	lookupContentSupport(
		final byte[]						from_hash,
		final int							level,
		final boolean						explicit,
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

								Long	cnet	 	= (Long)map.get( "c" );
								Long	published 	= (Long)map.get( "p" );
								Long	leechers 	= (Long)map.get( "l" );
								Long	seeds	 	= (Long)map.get( "z" );

								int	seeds_leechers;
								
								if ( leechers == null && seeds == null ){
									
									seeds_leechers = -1;
									
								}else if ( leechers == null ){
									
									seeds_leechers = seeds.intValue()<<16;
									
								}else if ( seeds == null ){
									
									seeds_leechers = leechers.intValue()&0xffff;
									
								}else{
									
									seeds_leechers = (seeds.intValue()<<16)|(leechers.intValue()&0xffff);
								}
								analyseResponse( 
									new DownloadInfo( 
										from_hash, hash, title, rand, tracker, level+1, explicit, size,
										published==null?0:published.intValue(),
										seeds_leechers,
										(byte)(cnet==null?ContentNetwork.CONTENT_NETWORK_UNKNOWN:cnet.byteValue())),
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
				false,
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
										lookupContentSupport( next_sl.getHash(), next_sl.getLevel(), false, listener );
										
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
		final DownloadInfo		info )
	{
		setConfigDirty();
		
		content_change_dispatcher.dispatch(
			new AERunnable()
			{
				public void
				runSupport()
				{
					for ( RelatedContentManagerListener l: listeners ){
						
						try{
							l.contentChanged( new RelatedContent[]{ info });
							
						}catch( Throwable e ){
							
							Debug.out( e );
						}
					}
				}
			});
	}
	
	protected void
	contentChanged(
		boolean	is_dirty )
	{
		if ( is_dirty ){
		
			setConfigDirty();
		}
		
		content_change_dispatcher.dispatch(
			new AERunnable()
			{
				public void
				runSupport()
				{
					for ( RelatedContentManagerListener l: listeners ){
						
						try{
							l.contentChanged();
							
						}catch( Throwable e ){
							
							Debug.out( e );
						}
					}
				}
			});
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
		
		if ( related_content.size() < max_results + temporary_space.get()){
			
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
			
			if ( info.isExplicit()){
				
				continue;
			}
			
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
		reset( true );
	}
	
	protected void
	reset(
		boolean	reset_perm_dels )
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
			
			total_unread.set( 0 );
			
			if ( reset_perm_dels ){
			
				resetPersistentlyDeleted();
			}
			
			setConfigDirty();
		}
		
		content_change_dispatcher.dispatch(
				new AERunnable()
				{
					public void
					runSupport()
					{
						for ( RelatedContentManagerListener l: listeners ){
							
							l.contentReset();
						}
					}
				});
	}
	
	protected List<RelatedContent>
	matchContent(
		String		term )
	{
			// term is made up of space separated bits - all bits must match
			// each bit can be prefixed by + or -, a leading - means 'bit doesn't match'. + doesn't mean anything
			// each bit (with prefix removed) can be "(" regexp ")"
			// if bit isn't regexp but has "|" in it it is turned into a regexp so a|b means 'a or b'
		
		List<RelatedContent>	result = new ArrayList<RelatedContent>();
		
		RelatedContent[] content = getRelatedContent();
		
		String[]	 bits = term.toLowerCase().split( " " );

		int[]		bit_types 		= new int[bits.length];
		Pattern[]	bit_patterns 	= new Pattern[bits.length];
		
		for (int i=0;i<bits.length;i++){
			
			String bit = bits[i] = bits[i].trim();
			
			if ( bit.length() > 0 ){
				
				char	c = bit.charAt(0);
				
				if ( c == '+' ){
					
					bit_types[i] = 1;
					
					bit = bits[i] = bit.substring(1);
					
				}else if ( c == '-' ){
					
					bit_types[i] = 2;
					
					bit = bits[i] = bit.substring(1);
				}
				
				if ( bit.startsWith( "(" ) && bit.endsWith((")"))){
					
					bit = bit.substring( 1, bit.length()-1 );
					
					try{
						bit_patterns[i] = Pattern.compile( bit, Pattern.CASE_INSENSITIVE );
						
					}catch( Throwable e ){
					}
				}else if ( bit.contains( "|" )){
					
					try{
						bit_patterns[i] = Pattern.compile( bit, Pattern.CASE_INSENSITIVE );
						
					}catch( Throwable e ){
					}
				}
			}
		}
			
		
		for ( final RelatedContent c: content ){
			
			String title = c.getTitle().toLowerCase();
			
			boolean	match 			= true;
			boolean	at_least_one 	= false;
			
			for (int i=0;i<bits.length;i++){
				
				String bit = bits[i];
				
				if ( bit.length() > 0 ){
					
					boolean	hit;
					
					if ( bit_patterns[i] == null ){
					
						hit = title.contains( bit );
						
					}else{
					
						hit = bit_patterns[i].matcher( title ).find();
					}
					
					int	type = bit_types[i];
					
					if ( hit ){
												
						if ( type == 2 ){
							
							match = false;
							
							break;
							
						}else{
							
							at_least_one = true;

						}
					}else{
						
						if ( type == 2 ){
						
							at_least_one = true;
							
						}else{
							
							match = false;
						
							break;
						}
					}
				}
			}
			
			if ( match && at_least_one ){
				
				result.add( c );
			}
		}
		
		return( result );
	}
	
	protected SearchInstance
	searchRCM(
		Map<String,Object>		search_parameters,
		final SearchObserver	observer )
	
		throws SearchException
	{
		final String	term = (String)search_parameters.get( SearchProvider.SP_SEARCH_TERM );
		
		final SearchInstance si = 
			new SearchInstance()
			{
				public void
				cancel()
			{
					Debug.out( "Cancelled" );
				}
			};
			
		if ( term == null ){
		
			observer.complete();
			
		}else{
		
			new AEThread2( "RCM:search", true )
			{
				public void
				run()
				{
					final Set<String>	hashes = new HashSet<String>();
					
					try{				
						List<RelatedContent>	matches = matchContent( term );
							
						for ( final RelatedContent c: matches ){
							
							final byte[] hash = c.getHash();
							
							if ( hash == null ){
								
								continue;
							}
							
							hashes.add( Base32.encode( hash ));
							
							SearchResult result = 
								new SearchResult()
								{
									public Object
									getProperty(
										int		property_name )
									{
										if ( property_name == SearchResult.PR_NAME ){
											
											return( c.getTitle());
											
										}else if ( property_name == SearchResult.PR_SIZE ){
											
											return( c.getSize());
											
										}else if ( property_name == SearchResult.PR_HASH ){
											
											return( hash );
											
										}else if ( property_name == SearchResult.PR_RANK ){
											
												// this rank isn't that accurate, scale down
											
											return( new Long( c.getRank() / 4 ));
											
										}else if ( property_name == SearchResult.PR_SEED_COUNT ){
											
											return( new Long( c.getSeeds()));
											
										}else if ( property_name == SearchResult.PR_LEECHER_COUNT ){
											
											return( new Long( c.getLeechers()));
											
										}else if ( property_name == SearchResult.PR_SUPER_SEED_COUNT ){
											
											if ( c.getContentNetwork() != ContentNetwork.CONTENT_NETWORK_UNKNOWN ){
												
												return( new Long( 1 ));
												
											}else{
												
												return( new Long( 0 ));
											}
										}else if ( property_name == SearchResult.PR_PUB_DATE ){
												
											long	date = c.getPublishDate();
											
											if ( date <= 0 ){
												
												return( null );
											}
											
											return( new Date( date ));
											
										}else if ( 	property_name == SearchResult.PR_DOWNLOAD_LINK ||
													property_name == SearchResult.PR_DOWNLOAD_BUTTON_LINK ){
											
											byte[] hash = c.getHash();
											
											if ( hash != null ){
												
												return( UrlUtils.getMagnetURI( hash ));
											}
										}
										
										return( null );
									}
								};
								
							observer.resultReceived( si, result );
						}
					}finally{
						
						try{
							DHT[]	dhts = dht_plugin.getDHTs();
	
							Set<InetSocketAddress>	addresses = new HashSet<InetSocketAddress>();
							
							for ( DHT dht: dhts ){
							
								DHTTransportContact[] contacts = dht.getTransport().getReachableContacts();
								
								for ( DHTTransportContact c: contacts ){
									
									if ( c.getProtocolVersion() >= DHTTransportUDP.PROTOCOL_VERSION_REPLICATION_CONTROL ){
										
										addresses.add( c.getAddress());
									}
								}
							}
							
							if ( addresses.size() < MAX_REMOTE_SEARCH_CONTACTS ){
								
								for ( DHT dht: dhts ){
									
									DHTTransportContact[] contacts = dht.getTransport().getRecentContacts();
	
									for ( DHTTransportContact c: contacts ){
										
										if ( c.getProtocolVersion() >= DHTTransportUDP.PROTOCOL_VERSION_REPLICATION_CONTROL ){
											
											addresses.add( c.getAddress());
											
											if ( addresses.size() >= MAX_REMOTE_SEARCH_CONTACTS ){
												
												break;
											}
										}
									}
									
									if ( addresses.size() >= MAX_REMOTE_SEARCH_CONTACTS ){
										
										break;
									}
								}
							}
							
							List<InetSocketAddress>	list = new ArrayList<InetSocketAddress>( addresses );
							
							Collections.shuffle( list );
							
							List<DistributedDatabaseContact>	ddb_contacts = new ArrayList<DistributedDatabaseContact>();
							
							for (int i=0;i<Math.min( list.size(), MAX_REMOTE_SEARCH_CONTACTS );i++){
								
								try{				
									ddb_contacts.add( ddb.importContact( list.get(i), DHTTransportUDP.PROTOCOL_VERSION_REPLICATION_CONTROL ));
									
								}catch( Throwable e ){
								}
							}
							
							long	start		= SystemTime.getMonotonousTime();
							long	max			= MAX_REMOTE_SEARCH_MILLIS;
							
							final AESemaphore	sem = new AESemaphore( "RCM:rems" );
							
							int	sent = 0;
							
							final int[]			done = {0};
							
							for (int i=0;i<ddb_contacts.size();i++){
								
								final DistributedDatabaseContact c = ddb_contacts.get( i );
																
								new AEThread2( "RCM:rems", true )
								{
									public void
									run()
									{
										try{
											sendRemoteSearch( si, hashes, c, term, observer );
																						
										}finally{
											
											synchronized( done ){
											
												done[0]++;
											}
											
											sem.release();
										}
									}
								}.start();
								
								sent++;
								
								synchronized( done ){
									
									if ( done[0] >= ddb_contacts.size() / 2 ){
										
										start		= SystemTime.getMonotonousTime();
										max			= 5*1000;
										
										break;
									}
								}
								
								if ( i > 10 ){
									
									try{
										Thread.sleep( 250 );
										
									}catch( Throwable e ){
									}
								}
							}
							
							for (int i=0;i<sent;i++){
								
								if ( done[0] > sent*4/5 ){
									
									break;
								}
								
								long	elapsed = SystemTime.getMonotonousTime() - start;
								
								if ( elapsed < max ){
									
									sem.reserve( max - elapsed );
									
								}else{
									
									break;
								}
							}
						}finally{
															
							observer.complete();
						}
					}
				}
			}.start();
		}
		
		return( si );
	}
	
	protected void
	sendRemoteSearch(
		SearchInstance					si,
		Set<String>						hashes,
		DistributedDatabaseContact		contact,
		String							term,
		SearchObserver					observer )
	{
		try{
			Map<String,Object>	request = new HashMap<String,Object>();
			
			request.put( "t", term );
		
			DistributedDatabaseKey key = ddb.createKey( BEncoder.encode( request ));
			
			DistributedDatabaseValue value = 
				contact.read( 
					new DistributedDatabaseProgressListener()
					{
						public void
						reportSize(
							long	size )
						{	
						}
						
						public void
						reportActivity(
							String	str )
						{	
						}
						
						public void
						reportCompleteness(
							int		percent )
						{
						}
					},
					transfer_type,
					key,
					10000 );
			
			// System.out.println( "search result=" + value );
			
			if ( value == null ){
				
				return;
			}
			
			Map<String,Object> reply = (Map<String,Object>)BDecoder.decode((byte[])value.getValue( byte[].class ));
			
			List<Map<String,Object>>	list = (List<Map<String,Object>>)reply.get( "l" );
			
			for ( final Map<String,Object> map: list ){
				
				final String title = ImportExportUtils.importString( map, "n" );
				
				final byte[] hash = (byte[])map.get( "h" );
				
				if ( hash == null ){
					
					continue;
				}
				
				String	hash_str = Base32.encode( hash );
					
				if ( hashes.contains( hash_str )){
					
					continue;
				}
				
				hashes.add( hash_str );

				SearchResult result = 
					new SearchResult()
					{
						public Object
						getProperty(
							int		property_name )
						{
							try{
								if ( property_name == SearchResult.PR_NAME ){
									
									return( title );
									
								}else if ( property_name == SearchResult.PR_SIZE ){
									
									return( ImportExportUtils.importLong( map, "s" ));
									
								}else if ( property_name == SearchResult.PR_HASH ){
									
									return( hash );
									
								}else if ( property_name == SearchResult.PR_RANK ){
									
									return( ImportExportUtils.importLong( map, "r" ) / 4 );
									
								}else if ( property_name == SearchResult.PR_SUPER_SEED_COUNT ){
									
									long cnet = ImportExportUtils.importLong( map, "c", ContentNetwork.CONTENT_NETWORK_UNKNOWN );
									
									if ( cnet == ContentNetwork.CONTENT_NETWORK_UNKNOWN ){
										
										return( 0L );
										
									}else{
										
										return( 1L );
									}
								}else if ( property_name == SearchResult.PR_SEED_COUNT ){
									
									return( ImportExportUtils.importLong( map, "z" ));
									
								}else if ( property_name == SearchResult.PR_LEECHER_COUNT ){
									
									return( ImportExportUtils.importLong( map, "l" ));
									
								}else if ( property_name == SearchResult.PR_PUB_DATE ){
									
									long date = ImportExportUtils.importLong( map, "p", 0 )*60*60*1000L;
									
									if ( date <= 0 ){
										
										return( null );
									}
									
									return( new Date( date ));
									
								}else if ( 	property_name == SearchResult.PR_DOWNLOAD_LINK ||
											property_name == SearchResult.PR_DOWNLOAD_BUTTON_LINK ){
									
									byte[] hash = (byte[])map.get( "h" );
									
									if ( hash != null ){
										
										return( UrlUtils.getMagnetURI( hash ));
									}
								}
							}catch( Throwable e ){
							}
							
							return( null );
						}
					};
					
				observer.resultReceived( si, result );
			}
		}catch( Throwable e ){
		}
	}
	
	protected Map<String,Object>
	receiveRemoteSearch(
		Map<String,Object>		request )
	{
		Map<String,Object>	response = new HashMap<String,Object>();
		
		try{
			String	term = ImportExportUtils.importString( request, "t" );
		
			if ( term != null ){
				
				List<RelatedContent>	matches = matchContent( term );

				if ( matches.size() > MAX_REMOTE_SEARCH_RESULTS ){
					
					Collections.sort(
						matches,
						new Comparator<RelatedContent>()
						{
							public int 
							compare(
								RelatedContent o1,
								RelatedContent o2) 
							{
								return( o2.getRank() - o1.getRank());
							}
						});
				}
				
				List<Map<String,Object>> list = new ArrayList<Map<String,Object>>();
				
				for (int i=0;i<Math.min( matches.size(),MAX_REMOTE_SEARCH_RESULTS);i++){
					
					RelatedContent	c = matches.get(i);
					
					Map<String,Object>	map = new HashMap<String, Object>();
					
					list.add( map );
					
					ImportExportUtils.exportString( map, "n", c.getTitle());
					ImportExportUtils.exportLong( map, "s", c.getSize());
					ImportExportUtils.exportLong( map, "r", c.getRank());
					ImportExportUtils.exportLong( map, "d", c.getLastSeenSecs());
					ImportExportUtils.exportLong( map, "p", c.getPublishDate()/(60*60*1000));
					ImportExportUtils.exportLong( map, "l", c.getLeechers());
					ImportExportUtils.exportLong( map, "z", c.getSeeds());
					ImportExportUtils.exportLong( map, "c", c.getContentNetwork());
					
					byte[] hash = c.getHash();
					
					if ( hash != null ){
						
						map.put( "h", hash );
					}
				}
				
				response.put( "l", list );
			}
		}catch( Throwable e ){
		}
		
		return( response );
	}
	
	public DistributedDatabaseValue
	read(
		DistributedDatabaseContact			contact,
		DistributedDatabaseTransferType		type,
		DistributedDatabaseKey				ddb_key )
	
		throws DistributedDatabaseException
	{
		Object	o_key = ddb_key.getKey();
		
		try{
			byte[]	key = (byte[])o_key;
			
				// TODO bloom
			
			Map<String,Object>	request = BDecoder.decode( key );
			
			Map<String,Object>	result = receiveRemoteSearch( request );
			
			return( ddb.createValue( BEncoder.encode( result )));
			
		}catch( Throwable e ){
			
			Debug.out( e );
			
			return( null );
		}
	}
	
	public void
	write(
		DistributedDatabaseContact			contact,
		DistributedDatabaseTransferType		type,
		DistributedDatabaseKey				key,
		DistributedDatabaseValue			value )
	
		throws DistributedDatabaseException
	{
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
							
							Object	rc_map_stuff 	= map.get( "rc" );
							
							if ( rc_map_stuff != null && rcm_map != null ){
								
								Map<Integer,DownloadInfo> id_map = new HashMap<Integer, DownloadInfo>();
									
								if ( rc_map_stuff instanceof Map ){
									
										// migration from when it was a Map with non-ascii key issues
									
									Map<String,Map<String,Object>>	rc_map 	= (Map<String,Map<String,Object>>)rc_map_stuff;

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
											
											Debug.out( e );
										}
									}
								}else{
									
									List<Map<String,Object>>	rc_map_list 	= (List<Map<String,Object>>)rc_map_stuff;

									for ( Map<String,Object> info_map: rc_map_list ){
										
										try{
										
											String	key = new String((byte[])info_map.get( "_k" ), "UTF-8" );
																												
											DownloadInfo info = deserialiseDI( info_map, cc );
											
											if ( info.isUnread()){
												
												new_total_unread++;
											}
											
											related_content.put( key, info );
											
											int	id = ((Long)info_map.get( "_i" )).intValue();
				
											id_map.put( id, info );
											
										}catch( Throwable e ){
											
											Debug.out( e );
										}
									}
								}
															
								if ( rcm_map.size() != 0 && id_map.size() != 0 ){
									
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
						
						if ( total_unread.get() != new_total_unread ){
														
							Debug.out( "total_unread - inconsistent (" + total_unread + "/" + new_total_unread + ")" );
							
							total_unread.set( new_total_unread );
							
							COConfigurationManager.setParameter( CONFIG_TOTAL_UNREAD, new_total_unread );
						}
					}catch( Throwable e ){
						
						Debug.out( e );
					}
					
					enforceMaxResults( cc, false );

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
				
			COConfigurationManager.setParameter( CONFIG_TOTAL_UNREAD, total_unread.get());
			
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
										
					List<Map<String,Object>> rc_map_list = new ArrayList<Map<String, Object>>( rcs.size());
					
					map.put( "rc", rc_map_list );
					
					int		id = 0;
					
					Map<DownloadInfo,Integer>	info_map = new HashMap<DownloadInfo, Integer>();
					
					for ( Map.Entry<String,DownloadInfo> entry: rcs ){
											
						DownloadInfo	info = entry.getValue();
												
						Map<String,Object> di_map = serialiseDI( info, cc );
						
						if ( di_map != null ){
							
							info_map.put( info, id );

							di_map.put( "_i", new Long( id ));
							di_map.put( "_k", entry.getKey());
							
							if ( rc_map_list.add( di_map ));
	
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
		return( total_unread.get());
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
			
			total_unread.set( 0 );
		}
		
		if ( changed ){
		
			contentChanged( true );
		}
	}
	
	public void
	deleteAll()
	{	
		synchronized( this ){

			ContentCache	content_cache = loadRelatedContent();
			
			addPersistentlyDeleted( content_cache.related_content.values().toArray( new DownloadInfo[ content_cache.related_content.size()]));
		
			reset( false );
		}
	}
	
	protected void
	incrementUnread()
	{
		total_unread.incrementAndGet();
	}
	
	protected void
	decrementUnread()
	{
		synchronized( this ){
			
			int val = total_unread.decrementAndGet();
			
			if ( val < 0 ){
				
				Debug.out( "inconsistent" );
				
				total_unread.set( 0 );
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
	
	protected List<byte[]>
	loadPersistentlyDeleted()
	{
		List<byte[]> entries = null;
		
		if ( FileUtil.resilientConfigFileExists( PERSIST_DEL_FILE )){
				
			Map<String,Object> map = (Map<String,Object>)FileUtil.readResilientConfigFile( PERSIST_DEL_FILE );
				
			entries = (List<byte[]>)map.get( "entries" );
		}
	
		if ( entries == null ){
			
			entries = new ArrayList<byte[]>(0);
		}
		
		return( entries );
	}
	
	protected void
	addPersistentlyDeleted(
		RelatedContent[]	content )
	{		
		if ( content.length == 0 ){
			
			return;
		}
	
		List<byte[]> entries = loadPersistentlyDeleted();
		
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
			
			List<byte[]> entries = loadPersistentlyDeleted();
			
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
	reserveTemporarySpace()
	{
		temporary_space.addAndGet( TEMPORARY_SPACE_DELTA );
	}
	
	public void
	releaseTemporarySpace()
	{
		boolean	reset_explicit = temporary_space.addAndGet( -TEMPORARY_SPACE_DELTA ) == 0;
		
		enforceMaxResults( reset_explicit );
	}
	
	protected void
	enforceMaxResults(
		boolean reset_explicit )
	{
		synchronized( this ){
	
			ContentCache	content_cache = loadRelatedContent();
			
			enforceMaxResults( content_cache, reset_explicit );
		}
	}
	
	protected void
	enforceMaxResults(
		ContentCache		content_cache,
		boolean				reset_explicit )	
	{
		Map<String,DownloadInfo>		related_content			= content_cache.related_content;

		int num_to_remove = related_content.size() - ( max_results + temporary_space.get());
		
		if ( num_to_remove > 0 ){
			
			List<DownloadInfo>	infos = new ArrayList<DownloadInfo>(related_content.values());
				
			if ( reset_explicit ){
				
				for ( DownloadInfo info: infos ){
					
					if ( info.isExplicit()){
						
						info.setExplicit( false );
					}
				}
			}
			
			Collections.sort(
				infos,
				new Comparator<DownloadInfo>()
				{
					public int 
					compare(
						DownloadInfo o1, 
						DownloadInfo o2) 
					{
						int res = o2.getLevel() - o1.getLevel();
						
						if ( res != 0 ){
							
							return( res );
						}
						
						res = o1.getRank() - o2.getRank();
						
						if ( res != 0 ){
							
							return( res );
						}
						
						return( o1.getLastSeenSecs() - o2.getLastSeenSecs());
					}
				});

			List<RelatedContent> to_remove = new ArrayList<RelatedContent>();
			
			for (int i=0;i<Math.min( num_to_remove, infos.size());i++ ){
				
				to_remove.add( infos.get(i));
			}
			
			if ( to_remove.size() > 0 ){
					
				delete( to_remove.toArray( new RelatedContent[to_remove.size()]), content_cache, false );
			}
		}
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
			
			ImportExportUtils.exportInt( info_map, "p", (int)( info.getPublishDate()/(60*60*1000)));
			ImportExportUtils.exportInt( info_map, "q", (info.getSeeds()<<16)|(info.getLeechers()&0xffff));
			ImportExportUtils.exportInt( info_map, "c", (int)info.getContentNetwork());

			if ( cc != null ){
							
				ImportExportUtils.exportBoolean( info_map, "u", info.isUnread());
				ImportExportUtils.exportIntArray( info_map, "l", info.getRandList());
				ImportExportUtils.exportInt( info_map, "s", info.getLastSeenSecs());
				ImportExportUtils.exportInt( info_map, "e", info.getLevel());
			}
			
			return( info_map );
			
		}catch( Throwable e ){
			
			Debug.out( e );
			
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
			
			int		date 			=  ImportExportUtils.importInt( info_map, "p", 0 );
			int		seeds_leechers 	=  ImportExportUtils.importInt( info_map, "q", -1 );
			byte	cnet 			=  (byte)ImportExportUtils.importInt( info_map, "c", (int)ContentNetwork.CONTENT_NETWORK_UNKNOWN );
			
			if ( cc == null ){
			
				return( new DownloadInfo( hash, hash, title, rand, tracker, 0, false, size, date, seeds_leechers, cnet ));
				
			}else{
				
				boolean unread = ImportExportUtils.importBoolean( info_map, "u" );
				
				int[] rand_list = ImportExportUtils.importIntArray( info_map, "l" );
				
				int	last_seen = ImportExportUtils.importInt( info_map, "s" );
				
				int	level = ImportExportUtils.importInt( info_map, "e" );
				
				return( new DownloadInfo( hash, title, rand, tracker, unread, rand_list, last_seen, level, size, date, seeds_leechers, cnet, cc ));
			}
		}catch( Throwable e ){
			
			Debug.out( e );
			
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
		private boolean			explicit;
		
			// we *need* this reference here to maange garbage collection correctly
		
		private ContentCache	cc;
		
		protected
		DownloadInfo(
			byte[]		_related_to,
			byte[]		_hash,
			String		_title,
			int			_rand,
			String		_tracker,
			int			_level,
			boolean		_explicit,
			long		_size,
			int			_date,
			int			_seeds_leechers,
			byte		_cnet )
		{
			super( _related_to, _title, _hash, _tracker, _size, _date, _seeds_leechers, _cnet );
			
			rand		= _rand;
			level		= _level;
			explicit	= _explicit;
			
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
			int				_date,
			int				_seeds_leechers,
			byte			_cnet,
			ContentCache	_cc )
		{
			super( _title, _hash, _tracker, _size, _date, _seeds_leechers, _cnet );
			
			rand		= _rand;
			unread		= _unread;
			rand_list	= _rand_list;
			last_seen	= _last_seen;
			level		= _level;
			cc			= _cc;
			
			if ( rand_list != null ){
				
				if ( rand_list.length > MAX_RANK ){
					
					int[] temp = new int[ MAX_RANK ];
					
					System.arraycopy( rand_list, 0, temp, 0, MAX_RANK );
						
					rand_list = temp;
				}
			}
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
					
					if ( !match && rand_list.length < MAX_RANK ){
						
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
				
				long cn =  info.getContentNetwork();
				
				if ( 	cn != ContentNetwork.CONTENT_NETWORK_UNKNOWN && 
						getContentNetwork() == ContentNetwork.CONTENT_NETWORK_UNKNOWN ){
					
					setContentNetwork( cn );
				}
				
				int sl = info.getSeedsLeechers();
				
				if ( sl != -1 && sl != getSeedsLeechers()){
					
					setSeedsLeechers( sl );
					
					result = true;
				}
				
				int	d = info.getDateHours();
				
				if ( d > 0 && d != getDateHours()){
					
					setDateHours( d );
					
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
		
		protected boolean
		isExplicit()
		{
			return( explicit );
		}
		
		protected void
		setExplicit(
			boolean		b )
		{
			explicit	= b;
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
			return( super.getString() + ", " + rand + ", rl=" + rand_list + ", last_seen=" + last_seen + ", level=" + level );
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
	
	protected class
	RCMSearchXFer
		implements DistributedDatabaseTransferType
	{	
	}
}
