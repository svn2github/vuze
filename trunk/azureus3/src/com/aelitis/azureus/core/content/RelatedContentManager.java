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
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.RandomUtils;
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
import com.aelitis.azureus.plugins.dht.DHTPlugin;
import com.aelitis.azureus.plugins.dht.DHTPluginContact;
import com.aelitis.azureus.plugins.dht.DHTPluginOperationListener;
import com.aelitis.azureus.plugins.dht.DHTPluginValue;
import com.aelitis.azureus.util.ImportExportUtils;

public class 
RelatedContentManager 
{
	private static final int	MAX_HISTORY				= 16;
	private static final int	MAX_TITLE_LENGTH		= 64;
	private static final int	MAX_CONCURRENT_PUBLISH	= 2;
	
	private static final String	CONFIG_FILE 				= "rcm.config";
	
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
	
	private int publishing_count = 0;
	
	private CopyOnWriteList<RelatedContentManagerListener>	listeners = new CopyOnWriteList<RelatedContentManagerListener>();
	
	private AESemaphore initialisation_complete_sem = new AESemaphore( "RCM:init" );

	private static final int CONFIG_DISCARD_MILLIS	= 60*1000;
	
	private ContentCache				content_cache_ref;
	private WeakReference<ContentCache>	content_cache;
	
	private boolean		content_dirty;
	private long		last_config_access;		
	private int			content_discard_ticks;
	
	private int	total_unread;
	private AsyncDispatcher	content_change_dispatcher = new AsyncDispatcher();
	
	
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
			
			COConfigurationManager.addAndFireParameterListener(
				"rcm.enabled",
				new ParameterListener()
				{
					public void 
					parameterChanged(
						String name )
					{
						enabled = COConfigurationManager.getBooleanParameter( "rcm.enabled", true );
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
												30*1000,
												new TimerEventPerformer()
												{
													public void 
													perform(
														TimerEvent event ) 
													{
														if ( enabled ){
														
															publish();
															
															saveRelatedContent();
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
								torrent.isPrivate()?StringInterner.intern(torrent.getAnnounceURL().getHost()):null );
						
						new_info.add( info );
						
						if ( initialising || download_infos1.size() == 0 ){
							
							download_infos1.add( info );
							
						}else{
							
							download_infos1.add( RandomUtils.nextInt( download_infos1.size()), info );
						}
						
						download_infos2.add( info );
						
						download_info_map.put( hash, info );
						
						if ( info.getTracker() != null ){
							
							download_priv_set.add( info.getTitle() + ":" + info.getTracker());
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
								
								download_priv_set.add( info.getTitle() + ":" + info.getTracker());
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

		final byte[] map_bytes = BEncoder.encode( map );
		
		final int max_hits = 30;
		
		final Download download = getDownload( from_info.getHash());
		
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
							
							analyseResponse( from_info, new DownloadInfo( from_info.getRelatedToHash(), hash, title, rand, tracker ), null );
							
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
		final Download						download,
		final RelatedContentLookupListener	listener )
	
		throws ContentException
	{
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
							
							lookupContentSupport( download, listener );
							
						}catch( ContentException e ){
							
							Debug.out( e );
						}
					}
				});
		}else{
			
			lookupContentSupport( download, listener );
		}
	}
	
	private void
	lookupContentSupport(
		final Download						download,
		final RelatedContentLookupListener	listener )
	
		throws ContentException
	{
		try{

			if ( dht_plugin == null ){
				
				throw( new ContentException( "DHT plugin unavailable" ));
			}
			
			final DownloadInfo	from_info;
		
			synchronized( this ){
				
				Torrent t = download.getTorrent();
				
				if ( t == null ){
					
					throw( new ContentException( "Torrent not available" ));
				}
				
				from_info = download_info_map.get( t.getHash());
				
				if ( from_info == null ){
					
					throw( new ContentException( "Unknown download" ));
				}
			}
			
			final String from_hash	= ByteFormatter.encodeString( from_info.getHash());
		
			final byte[] key_bytes	= ( "az:rcm:assoc:" + from_hash ).getBytes( "UTF-8" );
			
			
			final int max_hits = 30;
			
			dht_plugin.get(
					key_bytes,
					"Content relationship read: " + from_hash,
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
									RelatedContent	content )
								{
									handle( content );
								}
	
								public void
								contentChanged(
									RelatedContent	content )
								{
									handle( content );
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
									RelatedContent	content )
								{
									synchronized( content_list ){
										
										if ( content_list.contains( content )){
											
											return;
										}
										
										content_list.add( content );
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
								
								analyseResponse( 
									from_info, 
									new DownloadInfo( 
										from_info.getRelatedToHash(), hash, title, rand, tracker ),
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
	contentChanged(
		DownloadInfo		info )
	{
		setConfigDirty();
		
		for ( RelatedContentManagerListener l: listeners ){
			
			try{
				l.contentChanged( info );
				
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
	
	protected void
	analyseResponse(
		DownloadInfo						from_info,
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
					
					key = to_info.getTitle() + ":" + to_info.getTracker();
					
					if ( download_priv_set.contains( key )){
						
							// target refers to downoad we already have
						
						return;
					}
				}
			
				ContentCache	content_cache = loadRelatedContent();
				
				DownloadInfo	target_info = null;
				
				boolean	changed_content = false;
				boolean	new_content 	= false;
				
				
				target_info = content_cache.related_content.get( key );
				
				if ( target_info == null ){
					
					target_info = to_info;
			
					content_cache.related_content.put( key, target_info );
					
					ArrayList<DownloadInfo> links = content_cache.related_content_map.get( from_info.getHash());
					
					if ( links == null ){
						
						links = new ArrayList<DownloadInfo>(1);
						
						content_cache.related_content_map.put( from_info.getHash(), links );
					}
					
					links.add( target_info );
					
					links.trimToSize();
					
					target_info.setPublic( content_cache );
					
					new_content = true;
					
				}else{
					
						// we already know about this, see if new info
					
					changed_content = target_info.addInfo( to_info );
				}

				if ( target_info != null ){
					
					final DownloadInfo	f_target 	= target_info;
					final boolean		f_change	= changed_content;
					
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
				
					System.out.println( "rcm: load new" );
					
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
													
													di_list.add( di );
												}
											}
											
											if ( di_list.size() > 0 ){
												
													// just tag it with the first entry for the moment
												
												di_list.get(0).setRelatedToHash( hash );
												
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
						}
						
						if ( total_unread != new_total_unread ){
							
							if ( total_unread != 0 ){
							
								Debug.out( "total_unread - inconsistent (" + total_unread + "/" + new_total_unread );
							}
							
							total_unread = new_total_unread;
						}
					}catch( Throwable e ){
						
						Debug.out( e );
					}
				}else{
					
					System.out.println( "rcm: load existing" );
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
				
			long	now = SystemTime.getMonotonousTime();;
			
			ContentCache cc = content_cache==null?null:content_cache.get();
			
			if ( !content_dirty ){
					
				if ( cc != null  ){
					
					if ( now - last_config_access > CONFIG_DISCARD_MILLIS ){
					
						System.out.println( "rcm: discard: tick count=" + content_discard_ticks++ );
					
						content_cache_ref	= null;
					}
				}else{
					
					System.out.println( "rcm: discarded" );
					
					content_discard_ticks = 0;
				}
				
				return;
			}
			
			last_config_access = now;
			
			content_dirty	= false;
			
			if ( cc == null ){
				
				Debug.out( "RCM: cache inconsistent" );
				
			}else{

				System.out.println( "rcm: save" );
				
				Map<String,DownloadInfo>						related_content			= cc.related_content;
				ByteArrayHashMapEx<ArrayList<DownloadInfo>>		related_content_map		= cc.related_content_map;

				if ( related_content.size() == 0 ){
					
					FileUtil.deleteResilientConfigFile( CONFIG_FILE );
					
				}else{
					
					Map<String,Object>	map = new HashMap<String, Object>();
					
					Set<Map.Entry<String,DownloadInfo>> rcs = related_content.entrySet();
					
					Map<String,Object> rc_map = new HashMap<String, Object>();
					
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

			if ( cc == null ){
			
				info_map.put( "f", info.getRelatedToHash());
				
			}else{
				
				ImportExportUtils.exportBoolean( info_map, "u", info.isUnread());
				ImportExportUtils.exportIntArray( info_map, "l", info.getRandList());
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
			
			if ( cc == null ){
			
				byte[]	from_hash 	= (byte[])info_map.get("f");
				
				if ( from_hash == null ){
					
					return( null );
				}
				
				return( new DownloadInfo( from_hash, hash, title, rand, tracker ));
				
			}else{
				
				boolean unread = ImportExportUtils.importBoolean( info_map, "u" );
				
				int[] rand_list = ImportExportUtils.importIntArray( info_map, "l" );
				
				return( new DownloadInfo( hash, title, rand, tracker, unread, rand_list, cc ));
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
		
		private ContentCache	cc;
		
		protected
		DownloadInfo(
			byte[]		_related_to,
			byte[]		_hash,
			String		_title,
			int			_rand,
			String		_tracker )
		{
			super( _related_to, _title, _hash, _tracker );
			
			rand		= _rand;
		}
		
		protected
		DownloadInfo(
			byte[]			_hash,
			String			_title,
			int				_rand,
			String			_tracker,
			boolean			_unread,
			int[]			_rand_list,
			ContentCache	_cc )
		{
			super( _title, _hash, _tracker );
			
			rand		= _rand;
			unread		= _unread;
			rand_list	= _rand_list;
			cc			= _cc;
		}
		
		protected boolean
		addInfo(
			DownloadInfo		info )
		{
			synchronized( this ){
				
				int r = info.getRand();
				
				if ( rand_list == null ){
					
					rand_list = new int[]{ r };
										
					return( true );
					
				}else{
					
					for (int i=0;i<rand_list.length;i++){
						
						if ( rand_list[i] == r ){
							
							return( false );
						}
					}
					
					int	len = rand_list.length;
					
					int[]	new_rand_list = new int[len+1];
					
					System.arraycopy( rand_list, 0, new_rand_list, 0, len );
					
					new_rand_list[len] = r;
					
					rand_list = new_rand_list;
					
					return( true );
				}
			}
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
}
