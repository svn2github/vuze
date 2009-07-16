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

import java.io.IOException;
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

	private Map<String,DownloadInfo>				related_content 	= new HashMap<String,DownloadInfo>();
	private ByteArrayHashMapEx<List<DownloadInfo>>	related_content_map = new ByteArrayHashMapEx<List<DownloadInfo>>();
	private boolean									content_dirty;
	
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
						DownloadInfo info = deserialiseDI((Map<String,Object>)history.get(i));
						
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
						
						Map<String,Object> map = serialiseDI( info );
							
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
		synchronized( this ){
			
			content_dirty	= true;
		}
		
		for ( RelatedContentManagerListener l: listeners ){
			
			try{
				l.contentChanged( info );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
	
	protected void
	contentChanged()
	{
		synchronized( this ){
			
			content_dirty	= true;
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
			
				loadRelatedContent();
				
				DownloadInfo	target_info = null;
				
				boolean	changed_content = false;
				boolean	new_content 	= false;
				
				
				target_info = related_content.get( key );
				
				if ( target_info == null ){
					
					target_info = to_info;
			
					related_content.put( key, target_info );
					
					List<DownloadInfo> links = related_content_map.get( from_info.getHash());
					
					if ( links == null ){
						
						links = new ArrayList<DownloadInfo>();
						
						related_content_map.put( from_info.getHash(), links );
					}
					
					links.add( target_info );
					
					target_info.setPublic();
					
					new_content = true;
					
				}else{
					
						// we already know about this, see if new info
					
					changed_content = target_info.addInfo( to_info );
				}

				if ( target_info != null && ( changed_content || new_content )){
											
					content_dirty	= true;
					
					final DownloadInfo	f_target 	= target_info;
					final boolean		f_change	= changed_content;
					
					content_change_dispatcher.dispatch(
						new AERunnable()
						{
							public void
							runSupport()
							{
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

			loadRelatedContent();
			
			return( related_content.values().toArray( new DownloadInfo[ related_content.size()]));
		}
	}
	
	public void
	reset()
	{
		synchronized( this ){
			
			related_content.clear();
			related_content_map.clear();
			
			download_infos1.clear();
			download_infos2.clear();
			
			List<DownloadInfo> list = download_info_map.values();
			
			download_infos1.addAll( list );
			download_infos2.addAll( list );
			
			Collections.shuffle( download_infos1 );
			
			total_unread = 0;
		}
		
		for ( RelatedContentManagerListener l: listeners ){
			
			l.contentReset();
		}
	}
	
	protected void
	loadRelatedContent()
	{
		if ( related_content != null ){
			
			return;
		}
		
	}
	
	protected void
	saveRelatedContent()
	{
		
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
		synchronized( this ){
			
			DownloadInfo[] content = (DownloadInfo[])getRelatedContent();
			
			for ( DownloadInfo c: content ){
				
				c.setUnreadInternal( false );
			}
			
			total_unread = 0;
		}
		
		contentChanged();
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
	
	protected class
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
		DownloadInfo	info )
	{
		try{
			Map<String,Object> m = new HashMap<String,Object>();
			
			m.put( "h", info.getHash());
			
			ImportExportUtils.exportString( m, "d", info.getTitle());
			ImportExportUtils.exportInt( m, "r", info.getRand());
			ImportExportUtils.exportString( m, "t", info.getTracker());
			
			m.put( "f", info.getRelatedToHash());
			
			return( m );
			
		}catch( Throwable e ){
			
			return( null );
		}
	}
	
	private DownloadInfo
	deserialiseDI(
		Map<String,Object>				m )
	{
		try{
			byte[]	hash 	= (byte[])m.get("h");
			String	title	= ImportExportUtils.importString( m, "d" );
			int		rand	= ImportExportUtils.importInt( m, "r" );
			String	tracker	= ImportExportUtils.importString( m, "t" );
			
			byte[]	from_hash 	= (byte[])m.get("f");
			
			if ( from_hash == null ){
				
				return( null );
			}
			
			return( new DownloadInfo( from_hash, hash, title, rand, tracker ));
			
		}catch( Throwable e ){
			
			return( null );
		}
	}
	
	protected class
	DownloadInfo
		extends RelatedContent
	{
		final private int			rand;
		
		private boolean		unread	= true;
		
		private int[]	rand_list;
		
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
		setPublic()
		{
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
}
