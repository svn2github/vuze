/*
 * Created on 1 Nov 2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package com.aelitis.azureus.core.download;

import java.net.InetAddress;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.config.impl.TransferSpeedValidator;
import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.disk.DiskManagerPiece;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.download.DownloadManagerPeerListener;
import org.gudy.azureus2.core3.download.impl.DownloadManagerAdapter;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.peer.cache.CacheDiscovery;
import com.aelitis.azureus.core.peer.cache.CachePeer;
import com.aelitis.azureus.core.peermanager.piecepicker.PiecePicker;
import com.aelitis.azureus.core.peermanager.piecepicker.PieceRTAProvider;
import com.aelitis.azureus.core.peermanager.utils.PeerClassifier;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.core.util.average.Average;
import com.aelitis.azureus.core.util.average.AverageFactory;
import com.aelitis.azureus.plugins.extseed.ExternalSeedException;
import com.aelitis.azureus.plugins.extseed.ExternalSeedManualPeer;
import com.aelitis.azureus.plugins.extseed.ExternalSeedPlugin;
import com.aelitis.azureus.util.ConstantsVuze;
import com.aelitis.azureus.util.DownloadUtils;
import com.aelitis.azureus.util.PlayUtils;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.peers.PeerManager;

import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.pluginsimpl.local.utils.PooledByteBufferImpl;

public class 
EnhancedDownloadManager 
{
	public static  int	DEFAULT_MINIMUM_INITIAL_BUFFER_SECS_FOR_ETA	= 30;
	public static  int	WMP_MINIMUM_INITIAL_BUFFER_SECS_FOR_ETA		= 60;
		
		// number of seconds of buffer required before we fall back to normal bt mode
	
	public static  int	MINIMUM_INITIAL_BUFFER_SECS;
	
	static{
		COConfigurationManager.addAndFireParameterListeners(
			new String[]{
				"filechannel.rt.buffer.millis"	
			},
			new ParameterListener()
			{
				public void 
				parameterChanged(
					String parameterName )
				{
					int channel_buffer_millis = COConfigurationManager.getIntParameter( "filechannel.rt.buffer.millis" );
					
					MINIMUM_INITIAL_BUFFER_SECS = (2 * channel_buffer_millis )/1000;
				}
			});
	}
	
	public static final int SPEED_CONTROL_INITIAL_DELAY	= 10*1000;
	public static final int SPEED_INCREASE_GRACE_PERIOD	= 3*1000;
	public static final int PEER_INJECT_GRACE_PERIOD	= 3*1000;
	public static final int IDLE_PEER_DISCONNECT_PERIOD	= 60*1000;
	public static final int IDLE_SEED_DISCONNECT_PERIOD = 60*1000;
	public static final int MIN_SEED_CONNECTION_TIME	= 60*1000;
	
	public static final int IDLE_SEED_DISCONNECT_SECS	= IDLE_SEED_DISCONNECT_PERIOD/1000;
	
	public static final int CACHE_RECONNECT_MIN_PERIOD	= 15*60*1000;
	public static final int CACHE_REQUERY_MIN_PERIOD	= 60*60*1000;
	
	public static final int TARGET_SPEED_EXCESS_MARGIN	= 2*1024;
	
	public static final int DISCONNECT_CHECK_PERIOD	= 10*1000;
	public static final int DISCONNECT_CHECK_TICKS	= DISCONNECT_CHECK_PERIOD/DownloadManagerEnhancer.TICK_PERIOD;
	
	public static final int REACTIVATE_PROVIDER_PERIOD			= 5*1000;
	public static final int REACTIVATE_PROVIDER_PERIOD_TICKS	= REACTIVATE_PROVIDER_PERIOD/DownloadManagerEnhancer.TICK_PERIOD;

	public static final int LOG_PROG_STATS_PERIOD	= 10*1000;
	public static final int LOG_PROG_STATS_TICKS	= LOG_PROG_STATS_PERIOD/DownloadManagerEnhancer.TICK_PERIOD;

	private static final String TRACKER_PROG_PREFIX	= "azprog";
	
	
	private static final String PM_SEED_TIME_KEY = "EnhancedDownloadManager:seedtime";
	private static final String PEER_CACHE_KEY = "EnhancedDownloadManager:cachepeer";

	private static int internal_content_stream_bps_increase_ratio		= 5;
	private static int internal_content_stream_bps_increase_absolute	= 0;
	
		// these are here to allow other components (e.g. a plugin) to modify behaviour
		// while we verify that things work ok
	
	public static void
	setInternalContentStreamBPSIncreaseRatio(
		String	caller_id,
		int		ratio )
	{
		internal_content_stream_bps_increase_ratio	= ratio;
	}
	
	public static void
	setInternalContentStreamBPSIncreaseAbsolute(
		String	caller_id,
		int		abs )
	{
		internal_content_stream_bps_increase_absolute	= abs;
	}
	
	private DownloadManagerEnhancer		enhancer;
	private DownloadManager				download_manager;
	
	private boolean						platform_content;
	private boolean						explicit_progressive;
	
	private transient PiecePicker		current_piece_pickler;
	
	
	
	private boolean	progressive_active	= false;
	
	private long	content_min_delivery_bps;
		
	private int		minimum_initial_buffer_secs_for_eta;
	private int		explicit_minimum_buffer_bytes;
	
	private bufferETAProvider	buffer_provider	= new bufferETAProvider();
	private boostETAProvider	boost_provider	= new boostETAProvider();

	private progressiveStats	progressive_stats;

	private boolean				progressive_informed = false;
	
	private long	time_download_started;
	private Average	download_speed_average	= AverageFactory.MovingImmediateAverage( 5 );
	
	private boolean	marked_active;
	private boolean	destroyed;

	private DownloadManagerListener dmListener;
	
	private static final int	STALLED_TIMEOUT	= 2*60*1000;
	
	private boolean		publish_handling_complete;
	private long		publish_sent		= -1;
	private long		publish_sent_time;
		
	private EnhancedDownloadManagerFile[]	enhanced_files;
	private EnhancedDownloadManagerFile 	primary_file;

	
		// ********* reset these in resetVars ***********
	
	private long	last_speed_increase;
	private long	last_peer_inject;
	private long	last_lookup_time;
	
	private LinkedList	new_peers;
	private List		cache_peers;
	private List		disconnected_cache_peers;
	
	private CachePeer[]	lookup_peers;

	private void
	resetVars()
	{
		last_speed_increase		= 0;
		last_peer_inject		= 0;
		last_lookup_time		= 0;
		
		new_peers					= null;
		cache_peers					= null;
		disconnected_cache_peers	= null;
		lookup_peers				= null;
	}
	
	protected
	EnhancedDownloadManager(
		DownloadManagerEnhancer		_enhancer,
		DownloadManager				_download_manager )
	{
		enhancer			= _enhancer;
		download_manager	= _download_manager;

		DiskManagerFileInfo[] files = download_manager.getDiskManagerFileInfo();
		
			// hack - we don't know the actual player until play starts so we just use the file name
		
			// TODO: we can probably read the registry to work out what player is associated with
			// the file extension?
		
		boolean	found_wmv = false;
		
		for (int i=0;i<files.length;i++){
		
			String	file_name = files[i].getFile(true).getName().toLowerCase();
			
			if ( file_name.endsWith( ".wmv" )){
				
				found_wmv = true;
				
				break;
			}
		}
		
		if ( found_wmv ){
			
			minimum_initial_buffer_secs_for_eta = WMP_MINIMUM_INITIAL_BUFFER_SECS_FOR_ETA;

		}else{
		
			minimum_initial_buffer_secs_for_eta = DEFAULT_MINIMUM_INITIAL_BUFFER_SECS_FOR_ETA;
		}
		
		TOTorrent	torrent = download_manager.getTorrent();

		if ( torrent != null ){
			
			content_min_delivery_bps = PlatformTorrentUtils.getContentMinimumSpeedBps( torrent );
			
			platform_content = PlatformTorrentUtils.isContent( torrent, true );
						
			enhanced_files = new EnhancedDownloadManagerFile[files.length];
			
			Map meta_data = PlatformTorrentUtils.getFileMetaData( torrent );

				// dunno why but I have a user with an ArrayList being returned here...
			
			Object o_files_info = meta_data==null?null:meta_data.get( "files" );
			
			Map files_info = o_files_info instanceof Map?(Map)o_files_info:null;
			
			long	offset = 0;
			
			for (int i=0;i<files.length;i++){
				
				DiskManagerFileInfo f = files[i];
				
				Map file_info = files_info==null?null:(Map)files_info.get( "" + i );
				
				enhanced_files[i] = new EnhancedDownloadManagerFile( f, offset, file_info );
				
				offset += f.getLength();
			}
				
			int	primary_index = PlatformTorrentUtils.getContentPrimaryFileIndex( download_manager.getTorrent());
								
			if ( primary_index >= 0 && primary_index < files.length ){
					
				primary_file = enhanced_files[primary_index];
					
			}else{
				
				primary_index = PlayUtils.getPrimaryFileIndex( download_manager );
				
				primary_file = enhanced_files[primary_index==-1?0:primary_index];
			}		
		}else{
			
			enhanced_files = new EnhancedDownloadManagerFile[0];
		}
			
		progressive_stats	= createProgressiveStats( download_manager, primary_file );

		download_manager.addPeerListener(
			new DownloadManagerPeerListener()
			{
       			public void
    			peerManagerWillBeAdded(
    				PEPeerManager	peer_manager )
       			{
       			}
       			
				public void
				peerManagerAdded(
					PEPeerManager	manager )
				{
					synchronized( EnhancedDownloadManager.this ){
					
						time_download_started = SystemTime.getCurrentTime();
						
						current_piece_pickler = manager.getPiecePicker();
						
						if ( progressive_active && current_piece_pickler != null ){
							
							buffer_provider.activate( current_piece_pickler );
							
							boost_provider.activate( current_piece_pickler );
						}
						
						resetVars();
					}
				}
				
				public void
				peerManagerRemoved(
					PEPeerManager	manager )
				{
					synchronized( EnhancedDownloadManager.this ){

						time_download_started 	= 0;

						progressive_active		= false;
						
						if ( current_piece_pickler != null ){
					
							buffer_provider.deactivate(  current_piece_pickler );
							
							boost_provider.deactivate(  current_piece_pickler );
							
							current_piece_pickler	= null;	
						}
						
						resetVars();
					}
				}
				
				public void
				peerAdded(
					PEPeer 	peer )
				{
					if ( platform_content ){
												
						synchronized( EnhancedDownloadManager.this ){
							
							if ( new_peers == null ){
								
								new_peers = new LinkedList();
							}
							
							new_peers.add( peer );
						}
					}
				}
					
				public void
				peerRemoved(
					PEPeer	peer )
				{
					if ( platform_content ){
							
						synchronized( EnhancedDownloadManager.this ){
							
							if ( new_peers != null ){
							
								new_peers.remove( peer );
								
								if ( new_peers.size() == 0 ){
									
									new_peers = null;
								}
							}
							
							if ( cache_peers != null ){
								
								cache_peers.remove( peer );
								
								if ( cache_peers.size() == 0 ){
									
									cache_peers = null;
								}
							}
							
							CachePeer	cache_peer = (CachePeer)peer.getData( PEER_CACHE_KEY );

							if ( cache_peer == null ){
								
									// we can disconnect before getting peer-id etc
								
								if ( lookup_peers != null ){
									
									for (int i=0;i<lookup_peers.length;i++){
										
										CachePeer cp = lookup_peers[i];
										                            
										if ( 	cp.getAddress().getHostAddress().equals( peer.getIp()) &&
												cp.getPort() == peer.getPort()){
										
											cache_peer = cp;
										}
									}
								}
							}
							
							if ( 	cache_peer != null && 
									cache_peer.getType() == CachePeer.PT_CACHE_LOGIC &&
									(	disconnected_cache_peers == null || !disconnected_cache_peers.contains( cache_peer ))){
								
									// lost connection very early - sign that the cache doesn't support
									// us
								
								if ( !peer.hasReceivedBitField()){
									
									cache_peer.setAutoReconnect( false );
								}
							}
						}
					}
				}
			});
	}

	public void
	setExplicitProgressive(
		int		min_initial_buffer_secs,
		long	min_bps,
		int		file_index )
	{
		if ( file_index >= 0 && file_index < enhanced_files.length ){
			
			explicit_progressive = true;

			minimum_initial_buffer_secs_for_eta = min_initial_buffer_secs;
			
			content_min_delivery_bps = min_bps;
				
			platform_content = PlatformTorrentUtils.isContent( download_manager.getTorrent(), true );
							
			primary_file = enhanced_files[file_index];
				
			progressive_stats	= createProgressiveStats( download_manager, primary_file );		
		}
	}
	
	public String
	getName()
	{
		return( download_manager.getDisplayName());
	}
	
	public byte[]
	getHash()
	{
		TOTorrent t = download_manager.getTorrent();
		
		if ( t == null ){
			
			return( null );
		}
		
		try{
			
			return( t.getHash());
			
		}catch( Throwable e ){
		
			return( null );
		}
	}
	
	public boolean
	isPlatform()
	{
		TOTorrent	torrent = download_manager.getTorrent();
		
		if ( torrent != null ){
			
			return( PlatformTorrentUtils.isContent( torrent, true ));
		}

		return( false );
	}
	
	public EnhancedDownloadManagerFile[]
	getFiles()
	{
		return( enhanced_files );
	}
	
	public void
	setMinimumBufferBytes(
		int		min )
	{
		log( "Explicit min buffer set to " + min );
		
		explicit_minimum_buffer_bytes	= min;
	}
	
	protected void
	refreshMetaData()
	{
		progressive_stats.refreshMetaData();
	}
		
	protected long
	getTimeRunning()
	{
		if ( time_download_started == 0 ){
			
			return( 0 );
		}
		
		long	now = SystemTime.getCurrentTime();
		
		if ( now < time_download_started ){
			
			time_download_started	= now;
		}
		
		return( now - time_download_started );
	}
	
	protected long
	getTargetSpeed()
	{
		long	target_speed = progressive_active?progressive_stats.getStreamBytesPerSecondMax():content_min_delivery_bps;
		
		if ( target_speed < content_min_delivery_bps ){
			
			target_speed = content_min_delivery_bps;
		}
			
		return( target_speed );
	}
	
	protected void
	updateStats(
		int		tick_count )
	{
		updateProgressiveStats( tick_count );
		
		if ( !platform_content ){
			
			return;
		}
		
		int	state = download_manager.getState();
		
		if ( state != DownloadManager.STATE_SEEDING && state != DownloadManager.STATE_DOWNLOADING ){
			
			return;
		}

		PEPeerManager	pm = download_manager.getPeerManager();
		
		if ( pm == null ){
			
			return;
		}

		long	now = SystemTime.getCurrentTime();
		
		long	target_speed = getTargetSpeed();
		
		PEPeerManagerStats stats = pm.getStats();
		
		long	download_speed = stats.getDataReceiveRate();
		
		download_speed_average.update( download_speed );
		
		long	time_downloading = getTimeRunning();
		
		int		secs_since_last_up =  pm.getStats().getTimeSinceLastDataSentInSeconds();
		
			// deal with -1 -> infinite
		
		if ( secs_since_last_up == -1 ){
			
			Long seed_time = (Long)pm.getData( PM_SEED_TIME_KEY );
			
			if ( seed_time == null ){
				
				seed_time = new Long( now );
				
				pm.setData( PM_SEED_TIME_KEY, seed_time );
			}
			
			secs_since_last_up = (int)(( now - seed_time.longValue()) / 1000);
		}
		
		List	peers_to_kick = new ArrayList();
		
		synchronized( this ){
			
			if ( new_peers != null ){
										
				Iterator it = new_peers.iterator();
				
				while( it.hasNext()){
					
					PEPeer	peer = (PEPeer)it.next();
					
					CachePeer	cache_peer = (CachePeer)peer.getData( PEER_CACHE_KEY );

					if ( cache_peer == null ){
						
						byte[]	peer_id = peer.getId();
						
						if ( peer_id != null ){
							
							try{
								cache_peer = CacheDiscovery.categorisePeer( 
												peer_id, 
												InetAddress.getByName( peer.getIp()),
												peer.getPort());
								
								peer.setData( PEER_CACHE_KEY, cache_peer );
								
								if ( cache_peer.getType() == CachePeer.PT_CACHE_LOGIC ){
									
									if ( state == DownloadManager.STATE_SEEDING ){
										
										if ( 	now - cache_peer.getCreateTime( now ) >= MIN_SEED_CONNECTION_TIME &&
												secs_since_last_up >= IDLE_SEED_DISCONNECT_SECS ){

											peers_to_kick.add( peer );
											
											addToDisconnectedCachePeers( cache_peer );
											
										}else{
											
											if ( cache_peers == null ){
												
												cache_peers = new LinkedList();
											}
											
											cache_peers.add( peer );
										}
									}else{
										
											// cache logic rely on timely have messages to control both
											// piece allocation and client-speed
										
										peer.setHaveAggregationEnabled( false );
										
										if ( target_speed <= 0 ){
										
											setPeerSpeed( peer, -1, now );
											
											peers_to_kick.add( peer );
											
											addToDisconnectedCachePeers( cache_peer );
	
										}else{
											
											long	current_speed = (long)download_speed_average.getAverage();
											
												// if we are already exceeding required speed, block
												// the cache peer download
											
											if ( current_speed + TARGET_SPEED_EXCESS_MARGIN > target_speed ){
												
												setPeerSpeed( peer, -1, now );
											}
											
											if ( cache_peers == null ){
												
												cache_peers = new LinkedList();
											}
											
											cache_peers.add( peer );
										}
									}
								}
							}catch( Throwable e ){
								
								Debug.printStackTrace(e);
							}
							
							it.remove();
						}
					}else{
						
						it.remove();
					}
				}
				
				if ( new_peers.size() == 0 ){
					
					new_peers = null;
				}
			}
		}
		
		for (int i=0;i<peers_to_kick.size();i++){
			
			pm.removePeer((PEPeer)peers_to_kick.get(i), "Cache peer not required" );
		}
				
		if ( state == DownloadManager.STATE_DOWNLOADING ){
		
			if ( time_downloading > SPEED_CONTROL_INITIAL_DELAY ){
				
				long	current_average = (long)download_speed_average.getAverage();
					
				if ( current_average < target_speed ){
					
					long	current_speed = getCurrentSpeed();
					
						// increase cache peer contribution
						// due to latencies we need to give speed increases a time to take
						// effect to see if the limits can be reached
					
					long	difference = target_speed - current_speed;
					
					if ( last_speed_increase > now || now - last_speed_increase > SPEED_INCREASE_GRACE_PERIOD ){
		
						synchronized( this ){
	
							if ( cache_peers != null ){
								
								Iterator	it = cache_peers.iterator();
								
								while( it.hasNext() && difference > 0 ){
							
									PEPeer	peer = (PEPeer)it.next();
																				
									PEPeerStats peer_stats = peer.getStats();
									
									long peer_limit = peer_stats.getDownloadRateLimitBytesPerSecond();
									
										// try simple approach - find first cache peer that is limited
										// to less than the target
									
									if ( peer_limit == 0 ){
										
									}else{
										
										if ( peer_limit < target_speed ){
											
											setPeerSpeed( peer, (int)target_speed, now );
											
											last_speed_increase = now;
											
											difference = 0;
										}
									}
								}
							}
						}
										
						if ( 	difference > 0 &&
								last_peer_inject > now || now - last_peer_inject > PEER_INJECT_GRACE_PERIOD ){
							
							Set	connected_peers = new HashSet();
							
							List	peers_to_try = new ArrayList();
	
							if ( cache_peers != null ){
								
								Iterator	it = cache_peers.iterator();
								
								while( it.hasNext() && difference > 0 ){
							
									PEPeer	peer = (PEPeer)it.next();
						
									connected_peers.add( peer.getIp() + ":" + peer.getPort());
								}
							}
							
								// if we explicitly disconnected peers in the past then reuse them first
							
							if ( disconnected_cache_peers != null ){
								
								while( disconnected_cache_peers.size() > 0 ){
									
									CachePeer	cp = (CachePeer)disconnected_cache_peers.remove(0);
									
									if ( !connected_peers.contains( cp.getAddress().getHostAddress() + ":" + cp.getPort())){
										
											// check that this peer isn't already available as a lookup result
										
										if ( lookup_peers != null ){
											
											for (int i=0;i<lookup_peers.length;i++){
												
												CachePeer	l_cp = lookup_peers[i];
												
												if ( l_cp.sameAs( cp )){
													
													cp = null;
													
													break;
												}
											}
										}
										
										if ( cp != null ){
										
											peers_to_try.add( cp );
											
											break;
										}
									}
								}
								
								if ( disconnected_cache_peers.size() == 0 ){
									
									disconnected_cache_peers = null;
								}
							}
							
							if ( peers_to_try.size() == 0 ){
								
									// can't do the job with existing cache peers, try to find some more
								
								if ( 	lookup_peers == null || 
										now < last_lookup_time ||
										now - last_lookup_time > CACHE_REQUERY_MIN_PERIOD ){
																		
									last_lookup_time = now;
	
									lookup_peers = CacheDiscovery.lookup( download_manager.getTorrent());
								}
								
								for (int i=0;i<lookup_peers.length;i++){
									
									CachePeer	cp = lookup_peers[i];
									
									if ( cp.getAutoReconnect() && now - cp.getInjectTime(now) > CACHE_RECONNECT_MIN_PERIOD ){
										
										if ( !connected_peers.contains( cp.getAddress().getHostAddress() + ":" + cp.getPort())){
										
											peers_to_try.add( cp );
										}
									}
								}
							}
							
							if ( peers_to_try.size() > 0 ){
								
								CachePeer peer = (CachePeer)peers_to_try.get((int)( Math.random() * peers_to_try.size()));
								
								// System.out.println( "Injecting cache peer " + peer.getAddress() + ":" + peer.getPort());
								
								peer.setInjectTime( now );
								
								pm.addPeer( peer.getAddress().getHostAddress(), peer.getPort(), 0, false, null );
								
								last_peer_inject = now;
							}
						}
					}				
				}else if ( current_average > target_speed + TARGET_SPEED_EXCESS_MARGIN){
					
					long	current_speed = getCurrentSpeed();
	
						// decrease cache peer contribution
					
					long	difference = current_speed - ( target_speed + TARGET_SPEED_EXCESS_MARGIN );
					
					synchronized( this ){
	
						if ( cache_peers != null ){
							
							Iterator	it = cache_peers.iterator();
							
							while( it.hasNext() && difference > 0 ){
						
								PEPeer	peer = (PEPeer)it.next();
															
								PEPeerStats peer_stats = peer.getStats();
								
								long peer_rate = peer_stats.getDataReceiveRate();
								
								long peer_limit = peer_stats.getDownloadRateLimitBytesPerSecond();
	
								if ( peer_limit == -1 ){
									
										// blocked, take into account adjustment in progress
									
									difference -= peer_rate;
									
								}else if ( peer_limit != 0 && peer_rate > peer_limit ){
									
										// adjusting
									
									difference -= peer_rate - peer_limit;
									
								}else{
									
									if ( peer_rate > difference ){
																				
										setPeerSpeed( peer, (int)( peer_rate - difference ), now );
										
										difference = 0;
										
									}else{
									
										setPeerSpeed( peer, -1, now );
																				
										difference -= peer_rate;
									}
								}
							}
						}
					}
				}
			}
		}
		
		if ( tick_count % DISCONNECT_CHECK_TICKS == 0 ){
			
			peers_to_kick.clear();
			
			synchronized( this ){
				
				if ( cache_peers != null ){
					
					Iterator	it = cache_peers.iterator();
					
					while( it.hasNext()){
				
						PEPeer	peer = (PEPeer)it.next();
							
						CachePeer	cache_peer = (CachePeer)peer.getData( PEER_CACHE_KEY );

						if ( state == DownloadManager.STATE_SEEDING ){
							
							if ( 	now - cache_peer.getCreateTime( now ) >= MIN_SEED_CONNECTION_TIME &&
									secs_since_last_up >= IDLE_SEED_DISCONNECT_SECS ){

								peers_to_kick.add( peer );
								
								addToDisconnectedCachePeers( cache_peer );
							}
						}else{
							
							PEPeerStats peer_stats = peer.getStats();
						
							if ( peer_stats.getDownloadRateLimitBytesPerSecond() == -1 ){
							
								long	time = cache_peer.getSpeedChangeTime( now );
								
								if ( now - time > IDLE_PEER_DISCONNECT_PERIOD ){
									
									peers_to_kick.add( peer );
									
									addToDisconnectedCachePeers( cache_peer );
								}
							}
						}
					}
				}
			}
			
			for (int i=0;i<peers_to_kick.size();i++){
				
				pm.removePeer((PEPeer)peers_to_kick.get(i), "Cache peer disconnect-on-idle" );
			}
		}
	}
	
	protected void
	addToDisconnectedCachePeers(
		CachePeer		cache_peer )
	{
		if ( disconnected_cache_peers == null ){
			
			disconnected_cache_peers = new ArrayList();
		}

		for (int i=0;i<disconnected_cache_peers.size();i++){
			
			CachePeer	p = (CachePeer)disconnected_cache_peers.get(i);
			
			if ( p.sameAs( cache_peer )){
				
				return;
			}
		}
		
		disconnected_cache_peers.add( cache_peer );
	}
	
	protected void
	setPeerSpeed(
		PEPeer		peer,
		int			speed,
		long		time )
	{
		CachePeer	cache_peer = (CachePeer)peer.getData( PEER_CACHE_KEY );

		cache_peer.setSpeedChangeTime( time );
		
		peer.getStats().setDownloadRateLimitBytesPerSecond( speed );
	}
	
	protected long
	getCurrentSpeed()
	{
			// gets instantaneous speed instead of longer term average
		
		PEPeerManager	pm = download_manager.getPeerManager();
		
		long	result = 0;
		
		if ( pm != null ){
	
			Iterator	it = pm.getPeers().iterator();
		
			while( it.hasNext()){
				
				result += ((PEPeer)it.next()).getStats().getDataReceiveRate();
			}
		}
		
		return( result );
	}
	
	public boolean
	supportsProgressiveMode()
	{
		TOTorrent	torrent = download_manager.getTorrent();
		
		if ( torrent == null ){
			
			return( false );
		}
		
		return( enhancer.isProgressiveAvailable() && 
				( PlatformTorrentUtils.isContentProgressive( torrent ) || explicit_progressive ));
	}
	
	public boolean
	setProgressiveMode(
		boolean		active )
	{
		return( setProgressiveMode( active, false ));
	}
		
	protected boolean
	setProgressiveMode(
		boolean		active,
		boolean		switching_progressive_downloads )
	{
		TOTorrent	torrent = download_manager.getTorrent();
		
		if ( torrent == null ){

			return( false );
		}
		
		synchronized( this ){

			if ( progressive_active == active ){
				
				return( true );
			}			

			if (active && !supportsProgressiveMode()) {
				
				Debug.out( "Attempt to set progress mode on non-progressible content - " + getName());
				
				return( false );
			}
			
			log( "Progressive mode changed to " + active );

			final GlobalManager gm = download_manager.getGlobalManager(); 
			if (active) {
				if (dmListener == null) {
					dmListener = new DownloadManagerAdapter() {
						public void downloadComplete(DownloadManager manager) {
							gm.resumeDownloads();
						}
					};
				}
				download_manager.addListener(dmListener);
				
				// Check existing downloading torrents and turn off any
				// existing progressive/downloading
				Object[] dms = gm.getDownloadManagers().toArray();
				for (int i = 0; i < dms.length; i++) {
					DownloadManager dmCheck = (DownloadManager) dms[i];
					if (dmCheck.equals(download_manager)) {
						continue;
					}

					if (!dmCheck.isDownloadComplete(false)) {
						int state = dmCheck.getState();
						if (state == DownloadManager.STATE_DOWNLOADING
								|| state == DownloadManager.STATE_QUEUED) {
							dmCheck.pause();
						}
						EnhancedDownloadManager edmCheck = enhancer.getEnhancedDownload(dmCheck);
						if (edmCheck != null && edmCheck.getProgressiveMode()) {
							edmCheck.setProgressiveMode(false, true);
						}
					}
				}
				if (download_manager.isPaused()) {
					download_manager.resume();
				}

				// Make sure download can start by moving out of stop state
				// and putting at top
				if (download_manager.getState() == DownloadManager.STATE_STOPPED) {
					download_manager.setStateWaiting();
				}

				if (download_manager.getPosition() != 1) {
					download_manager.getGlobalManager().moveTo(download_manager, 1);
				}
			} else {
				download_manager.removeListener(dmListener);
				if ( !switching_progressive_downloads ){
					gm.resumeDownloads();
				}
			}
			
			progressive_active	= active;

			if ( current_piece_pickler != null ){
		
				if ( progressive_active ){
					
					buffer_provider.activate( current_piece_pickler );
					
					boost_provider.activate( current_piece_pickler );
					
					progressive_stats.update( 0 );
					
				}else{
					
					buffer_provider.deactivate( current_piece_pickler );
					
					boost_provider.deactivate( current_piece_pickler );
					
					progressive_stats = createProgressiveStats( download_manager, primary_file );
				}
			}else{
				
				progressive_stats = createProgressiveStats( download_manager, primary_file );
			}
			
			if ( !switching_progressive_downloads ){
				
				if ( active ){
					
					RealTimeInfo.setProgressiveActive(  progressive_stats.getStreamBytesPerSecondMax());
					
				}else{
					
					RealTimeInfo.setProgressiveInactive();
				}
			}
		}
		
		if ( active && !progressive_informed ){
			
			progressive_informed	= true;
			
				// tell tracker we're progressive so it can, if required, schedule more seeds
			
			Download	plugin_dl = PluginCoreUtils.wrap( download_manager );
			
			DownloadUtils.addTrackerExtension( plugin_dl, TRACKER_PROG_PREFIX, "y" );
			
			download_manager.requestTrackerAnnounce( true );
		}
		
		return( true );
	}


	public boolean
	getProgressiveMode()
	{
		return( progressive_active );
	}
	
	public long
	getProgressivePlayETA()
	{
		return( getProgressivePlayETA( false ));
	}
	
	public long
	getProgressivePlayETA(
		boolean	ignore_min_buffer_size )
	{
		progressiveStats stats = getProgressiveStats();
		
		long	eta = stats.getETA( ignore_min_buffer_size );
				
		return( eta );
	}
	
	protected progressiveStats
	getProgressiveStats()
	{
		synchronized( this ){
			
			return( progressive_stats.getCopy());
		}
	}
	
	protected progressiveStats
	createProgressiveStats(
		DownloadManager					dm,
		EnhancedDownloadManagerFile		file )
	{
		TOTorrent torrent = download_manager.getTorrent();
		
		if ( torrent != null && ( PlatformTorrentUtils.useEMP( torrent ) || explicit_progressive )){
			
			return( new progressiveStatsInternal( dm, file ));
			
		}else{
			
			return( new progressiveStatsExternal( dm, file ));

		}
	}
	
	protected void
	updateProgressiveStats(
		int		tick_count )
	{
		if ( !progressive_active ){
			
			return;
		}
					
		synchronized( this ){
			
			if ( !progressive_active ){
				
				return;
			}			

			if ( tick_count % REACTIVATE_PROVIDER_PERIOD_TICKS == 0 ){
				
				PiecePicker piece_picker = current_piece_pickler;
				
				if ( piece_picker != null ){
				
					buffer_provider.checkActivation( piece_picker );
				}
			}
			
			progressive_stats.update( tick_count );
			
			long	current_max = progressive_stats.getStreamBytesPerSecondMax();
			
			if ( RealTimeInfo.getProgressiveActiveBytesPerSec() != current_max ){
				
				RealTimeInfo.setProgressiveActive( current_max );
			}
		}
	}
	
	protected void
	setRTA(
		boolean	active )
	{
		synchronized( this ){

			if ( marked_active && !active ){
								
				marked_active = false;

				RealTimeInfo.removeRealTimeTask();
			}
			
			if ( destroyed ){
				
				return;
			}
			
			if ( !marked_active && active ){
				
				marked_active = true;

				RealTimeInfo.addRealTimeTask();
			}
		}
	}

	public DiskManagerFileInfo
	getPrimaryFile()
	{
		return( primary_file.getFile());
	}

	public long
	getContiguousAvailableBytes(
		DiskManagerFileInfo		file )
	{
		return( getContiguousAvailableBytes( file, 0 ));
	}
	
	public long
	getContiguousAvailableBytes(
		DiskManagerFileInfo		file,
		int						file_start_offset )
	{
		if ( file == null ) {

			return( -1 );
		}

		DiskManager dm = download_manager.getDiskManager();
		
		if ( dm == null ){
			
			if ( file.getDownloaded() == file.getLength()){
				
				return( file.getLength() - file_start_offset );
			}
			
			return( -1 );
		}
		
		int	piece_size = dm.getPieceLength();
		
		DiskManagerFileInfo[]	 files = dm.getFiles();
		
		long	start_index = file_start_offset;
		
		for (int i=0;i<files.length;i++){
			
			if ( files[i].getIndex() == file.getIndex()){
				
				break;
			}
			
			start_index += files[i].getLength();
		}
		
		int	first_piece_index 	= (int)( start_index / piece_size );
		int	first_piece_offset	= (int)( start_index % piece_size );
		int	last_piece_index	= file.getLastPieceNumber();
		
		DiskManagerPiece[]	pieces = dm.getPieces();
		
		DiskManagerPiece	first_piece = pieces[first_piece_index];
				
		long	available = 0;
		
		if ( !first_piece.isDone()){
			
			boolean[] blocks = first_piece.getWritten();
						
			if ( blocks == null ){
				
				if ( first_piece.isDone()){
					
					available = first_piece.getLength() - first_piece_offset;
				}
			}else{
				
				int	piece_offset = 0;
				
				for (int j=0;j<blocks.length;j++){
				
					if ( blocks[j] ){
					
						int	block_size = first_piece.getBlockSize( j );
						
						piece_offset = piece_offset + block_size;
						
						if ( available == 0 ){
						
							if ( piece_offset > first_piece_offset ){
								
								available = piece_offset - first_piece_offset;
							}
						}else{
							
							available += block_size;
						}						
					}else{
						
						break;
					}
				}
			}	
		}else{
		
			available = first_piece.getLength() - first_piece_offset; 
		
			for (int i=first_piece_index+1;i<=last_piece_index;i++){
				
				DiskManagerPiece piece = pieces[i];
				
				if ( piece.isDone()){
					
					available += piece.getLength();
					
				}else{
				
					boolean[] blocks = piece.getWritten();
							
					if ( blocks == null ){
						
						if ( piece.isDone()){
					
							available += piece.getLength();
							
						}else{
							
							break;
						}
					}else{
						
						for (int j=0;j<blocks.length;j++){
						
							if ( blocks[j] ){
							
								available += piece.getBlockSize( j );
								
							}else{
								
								break;
							}
						}
					}
					
					break;
				}
			}
		}
		
		long	max_available = file.getLength() - file_start_offset;
		
		if ( available > max_available ){
		
			available = max_available;
		}
		
		return( available );
	}
	
	
	public void
	setViewerPosition(
		DiskManagerFileInfo 	file_info, 
		long 					bytes)
	{
		if ( file_info == null ){
			
			return;
		}
		
		setViewerPosition( file_info.getIndex(), bytes );
	}
	
	public void
	setViewerPosition(
		int						file_index, 
		long 					bytes)
	{		
  		if ( file_index < enhanced_files.length ){
  			
  			bytes += enhanced_files[file_index].getByteOffestInTorrent();
  		}
		
		progressive_stats.setViewerBytePosition( bytes );
	}
	
	public DownloadManager 
	getDownloadManager() 
	{
		return download_manager;
	}
	
	protected void 
	destroy()
	{
		synchronized( this ){
			
			setRTA( false );

			destroyed = true;
		}
	}
	
	protected void
	log(
		String	str )
	{
		log( str, true );
	}
	
	protected void
	log(
		String	str,
		boolean	to_file )
	{
		log( download_manager, str, to_file );
	}
	
	protected void
	log(
		DownloadManager	dm,
		String			str,
		boolean			to_file )
	{
		str = dm.getDisplayName() + ": " + str;
		
		if ( to_file ){
			
			AEDiagnosticsLogger diag_logger = AEDiagnostics.getLogger("v3.Stream");
			
			diag_logger.log(str);
		}
		
		if ( ConstantsVuze.DIAG_TO_STDOUT ) {
			
			System.out.println(Thread.currentThread().getName() + "|"
					+ System.currentTimeMillis() + "] " + str);
		}
	}
	
	protected class
	bufferETAProvider
		implements PieceRTAProvider
	{
		private long[]		piece_rtas;
		
		private long		last_buffer_size;
		private long		last_buffer_size_time;
		
		private boolean		active;
		
		protected void
		activate(	
			PiecePicker		picker )
		{
			log( "Activating buffer provider" );

			synchronized( EnhancedDownloadManager.this ){

				active = true;
				
				piece_rtas = new long[ picker.getNumberOfPieces()];
				
				long	now = SystemTime.getCurrentTime();
				
				for (int i=0;i<piece_rtas.length;i++){
					
						// not bothered about times here but need to use large increments to ensure
						// that pieces are picked in order even for slower peers
					
					piece_rtas[i] = now+(i*60000);
				}

				picker.addRTAProvider( this );
			}
		}
		
		protected void
		deactivate(
			PiecePicker		picker )
		{
			if ( active ){
				
   				log( "Deactivating buffer provider" );
			}
			
			synchronized( EnhancedDownloadManager.this ){
									
				picker.removeRTAProvider( this );
				
				piece_rtas	= null;
				
				active = false;
			}
		}
		
		protected void
		checkActivation(
			PiecePicker		picker )
		{
				// might need to re-enable the buffer provider if speeds change
			
   			if ( getProgressivePlayETA() > 0 ){
  
    			synchronized( EnhancedDownloadManager.this ){
    					
    				if ( piece_rtas == null ){
    						
    					activate( picker );
     				}
    			}
    		}
		}
		
		public long[]
    	updateRTAs(
    		PiecePicker		picker )
    	{
				// force linear downloading until we have enough to allow the user to 
				// potentially start playing. If they don't do so immediately then until that
				// time we'll be doing normal BT download
			
    		DiskManager	dm = download_manager.getDiskManager();

    		if ( dm != null ){

    			if ( getProgressivePlayETA() <= 0 ){
      				
     				deactivate( picker );
    			}
    		}
    		
    		long[]	rtas = piece_rtas;
    		
    		if ( rtas != null ){
    		
    			long	buffer_size = progressive_stats.getInitialBytesDownloaded();
    			
    			long	now = SystemTime.getCurrentTime();
    			
    			if ( last_buffer_size != buffer_size ){
    				
    				last_buffer_size = buffer_size;
    				
    				last_buffer_size_time = now;
    				
    			}else{
    				
    				if ( now < last_buffer_size_time ){
    					
    					last_buffer_size_time = now;
    					
    				}else{
    					
    					long	stalled_for = now - last_buffer_size_time;
    					
   						long	dl_speed = progressive_stats.getDownloadBytesPerSecond();
   					 
   						if ( dl_speed > 0 ){
   							
   							long	block_time = (DiskManager.BLOCK_SIZE * 1000) / dl_speed;
   							
   							if ( stalled_for > Math.max( 5000, 5*block_time )){
    						
   								long	target_rta = now + block_time;
   								
   								int	blocked_piece_index = (int)( buffer_size / dm.getPieceLength());
   								
   								DiskManagerPiece[] pieces = dm.getPieces();
   								  								
   								if ( blocked_piece_index < pieces.length ){
   									  									
   									if ( pieces[blocked_piece_index].isDone()){
   										
   										blocked_piece_index++;
   										
   										if ( blocked_piece_index < pieces.length ){
   											
   											if ( pieces[blocked_piece_index].isDone()){
   												
   												blocked_piece_index = -1;
   											}
   										}else{
   											
   											blocked_piece_index = -1;
   										}
   									}
   								}
   								
   								if ( blocked_piece_index >= 0 ){
   									
   									long	existing_rta = rtas[blocked_piece_index];
   									
   									if ( target_rta < existing_rta ){
   										
   										rtas[blocked_piece_index] = target_rta;
   										
   										log( "Buffer provider: reprioritising lagging piece " + blocked_piece_index + " with rta " + block_time );
   									}
   								}
   							}
   						}
    				}
    			}
    		}
    		
    		return( rtas );
    	}
    	
    	public long
    	getCurrentPosition()
    	{
    		return( 0 );
    	}
    	
      	public long
       	getStartTime()
       	{
       		return( 0 );
       	}
       	
       	public long
       	getStartPosition()
       	{
       		return( 0 );
       	}
       	
    	public long
		getBlockingPosition()
		{
			return( 0 );
		}
		
    	public void
    	setBufferMillis(
			long	seconds )
		{
		}
    	
		public String
		getUserAgent()
		{
			return( null );
		}
	}
	
	protected class
	boostETAProvider
		implements PieceRTAProvider
	{
		private long[]		piece_rtas;
		
		private long		last_recalc;
		
		private int			aggression;
		
		private boolean		active;
		
		private interventionHandler		intervention_handler = new interventionHandler();
		
		private long					last_intervention;
		
		protected void
		activate(	
			PiecePicker		picker )
		{
			if ( supportsProgressiveMode()){
				
				log( "Activating boost provider" );

				synchronized( EnhancedDownloadManager.this ){
					
					intervention_handler.activate();
					
					active	= true;
					
					picker.addRTAProvider( this );
				}
			}
		}
		
		protected void
		deactivate(
			PiecePicker		picker )
		{
			if ( active ){
			
				log( "Deactivating boost provider" );
			}
			
			synchronized( EnhancedDownloadManager.this ){
									
				picker.removeRTAProvider( this );
				
				piece_rtas	= null;
				
				active = false;
				
				intervention_handler.deactivate();
			}
		}
		
		public long[]
    	updateRTAs(
    		PiecePicker		picker )
    	{
			long	now = SystemTime.getCurrentTime();
			
			if ( now < last_recalc || now - last_recalc > 1000 ){
				
				last_recalc	= now;
								
				DiskManager	disk_manager = download_manager.getDiskManager();

					// if it'll take less time to download than watch then the channel-based rta logic
					// will do the job.
				
				progressiveStats	stats = getProgressiveStats();
				
				long	max_bps = stats.getStreamBytesPerSecondMax();
				
								
				if ( 	disk_manager == null || 
						!stats.isProviderActive() || 
						stats.getETA(false) < -MINIMUM_INITIAL_BUFFER_SECS ||
						max_bps == 0 ){
					
					if ( piece_rtas != null ){
						
						log( "Suspending boost provider" );
					}
					
					piece_rtas = null;
					
				}else{
	
					if ( piece_rtas == null ){
						
						log( "Resuming boost provider" );
					}

					long[] local_rtas = piece_rtas = new long[disk_manager.getNbPieces()];
					
						// need to force piece order - set RTAs for all outstanding pieces
					
					long	piece_size = disk_manager.getPieceLength();
					
					int		start_piece = (int)( stats.getBytePosition() / piece_size );
						
					long	bytes_offset = 0;
					
						// we need to be more aggresive if we have an explicit min buffer size
						// as the emp will auto-pause when the contiguous available bytes falls
						// below this min
				
					int	last_aggressive_piece = -1;
					
					long	time_to_stall = 0;
					
					if ( explicit_minimum_buffer_bytes > 0 ){
				
						long total_avail = getContiguousAvailableBytes( getPrimaryFile());
						
						long viewer_pos = stats.getViewerBytePosition();
						
						long avail = total_avail - viewer_pos;
						
						time_to_stall =  ( avail - explicit_minimum_buffer_bytes )*1000/max_bps;
						
						long	buffer_zone = 3*explicit_minimum_buffer_bytes;
						
						if ( avail <= buffer_zone ){
							
							if ( avail < 0 ){
								
								avail = 0;
							}
							
							if ( avail <= explicit_minimum_buffer_bytes ){
								
								aggression = 10;
								
							}else{		
								
								aggression =  (int)( ( buffer_zone - avail )*10/( buffer_zone - explicit_minimum_buffer_bytes ));
							}
							
							last_aggressive_piece = start_piece + (int)(( buffer_zone + piece_size -1 ) / piece_size);
							
						}else{
							
							aggression = 0;
						}
					}else{
						
						aggression = 0;
					}
					
					DiskManagerPiece[] dm_pieces = disk_manager.getPieces();
					
					for ( int i=start_piece;i<local_rtas.length;i++ ){
						
						int	time_factor;
						
						if ( i <= last_aggressive_piece ){
							
							time_factor = (( 10 - aggression ) * 1000 ) /10;
								
							time_factor = Math.max( time_factor, 10 );
								
							if ( aggression >= 7 && !dm_pieces[i].isDone() && time_to_stall <= 10*1000 ){
					
								if ( now < last_intervention || now - last_intervention >= 500 ){
									
									last_intervention	= now;
									
									/*
									long total_avail = getContiguousAvailableBytes( getPrimaryFile());								
									long viewer_pos = stats.getViewerBytePosition();
									long avail = total_avail - viewer_pos;
									long	buffer_zone = 3*explicit_minimum_buffer_bytes;
									
									System.out.println( "Aggression = " + aggression + ", time factor=" + time_factor + ", stall=" + time_to_stall + ", avail=" + avail + ", buffer=" + buffer_zone );
									*/
									
									intervention_handler.addPiece( i, SystemTime.getCurrentTime() + time_to_stall );
								}
							}
						}else{
							
							time_factor = 1000;
						}
						
						local_rtas[i] = now + ( time_factor* ( bytes_offset / max_bps ));
						
						bytes_offset += piece_size;					
					}
				}
			}
    		
    		return( piece_rtas );
    	}
		
	   	public long
    	getCurrentPosition()
    	{
    		return( 0 );
    	}
    	
		public long
		getBlockingPosition()
		{
			return( 0 );
		}
		
	  	public long
	   	getStartTime()
	   	{
	   		return( 0 );
	   	}
	   	
	   	public long
	   	getStartPosition()
	   	{
	   		return( 0 );
	   	}
	   	
		public void
		setBufferMillis(
			long	seconds )
		{
		}
		
		public String
		getUserAgent()
		{
			return( null );
		}
		
		protected class
		interventionHandler
		{
			private AEThread2 		thread;
			private AESemaphore		request_sem;
			private List			request_list;
			
			private List			http_peers;
			
			private boolean	borked;
			
			protected void
			activate()
			{
				synchronized( this ){
					
					active	= true;
				}
			}
			
			protected void
			deactivate()
			{
				synchronized( this ){
					
					active	= false;
					
					if ( thread != null ){
						
						thread			= null;
						request_list	= null;
						
						request_sem.release();
					}
				}
			}
			
			protected void
			addPiece(
				int		piece_number,
				long	stall_time )
			{
				synchronized( this ){

					if ( !active || borked ){
						
						return;
					}
					
					if ( thread == null ){
						
						PluginInterface pi = enhancer.getCore().getPluginManager().getPluginInterfaceByClass( ExternalSeedPlugin.class );

						if ( pi == null ){
							
							borked = true;
							
							return;
						}
						
						ExternalSeedPlugin ext_seed = (ExternalSeedPlugin)pi.getPlugin();
						
						ExternalSeedManualPeer[] peers = ext_seed.getManualWebSeeds( PluginCoreUtils.wrap( download_manager ));
						
						http_peers = null;
						
						for ( int i=0;i<peers.length;i++ ){
							
							ExternalSeedManualPeer peer = peers[i];
							
							if ( PeerClassifier.isAzureusIP( peer.getIP())){
								
								if ( http_peers == null ){
									
									http_peers = new ArrayList();
								}
								
								http_peers.add( peer );;
							}
						}
						
						request_sem		= new AESemaphore( "EDH:intervention" );
						request_list	= new ArrayList();
						
						thread = 
							new AEThread2( "EDH:intervention", true )
							{
								private AESemaphore	my_sem	= request_sem;
								private List		my_list	= request_list;
								
								private ExternalSeedManualPeer	current_peer;
								
								public void
								run()
								{
									while( true ){
										
										my_sem.reserve();
										
										int		piece_number;
										long	stall_time;
										
										synchronized( interventionHandler.this ){
											
											if ( my_list.isEmpty()){
												
												break;
											}
											
												// leave on list and remove later to prevent
												// duplicates being queued during intervention
											
											long[]	entry = (long[])my_list.get(0);
											
											piece_number 	= (int)entry[0];
											stall_time		= entry[1];
										}
										
										try{

											int remaining = (int)( stall_time - SystemTime.getCurrentTime());
												
											if ( remaining < 500 ){
											
													// no point trying to do anything, too late
												
												continue;
											}

											DiskManager		disk_manager = download_manager.getDiskManager();
											PEPeerManager 	peer_manager = download_manager.getPeerManager();

											if ( disk_manager == null || peer_manager == null ){
												
												continue;
											}
											
											DiskManagerPiece dm_piece = disk_manager.getPiece( piece_number );
												
											if ( dm_piece.isDone()){
												
												continue;
											}
												
											List http = http_peers;
																					
											if ( current_peer == null ){
												
												if ( http == null && http.size() == 0 ){

													continue;
												}
												
												current_peer = (ExternalSeedManualPeer)http.remove(0);
											}
											
											PEPiece pe_piece = peer_manager.getPiece( piece_number );
											
											boolean[]	to_do = new boolean[dm_piece.getNbBlocks()];
											
											if ( pe_piece == null ){
												
												boolean[] written = dm_piece.getWritten();
											
												if ( written == null ){
													
													Arrays.fill( to_do, true );
													
												}else{
													
													for (int i=0;i<to_do.length;i++){
														
														if ( !written[i] ){
														
															to_do[i] = true;
														}
													}
												}
											}else{
												
												boolean[] downloaded = pe_piece.getDownloaded();
												
												for (int i=0;i<to_do.length;i++){
													
													if ( !downloaded[i] ){
													
														to_do[i] = true;
													}
												}
											}
											
											int	block_pos = 0;
											
											while( true ){
											
												int	block_start 		= 0;
												int block_num			= 0;
												
												while( block_pos < to_do.length ){
													
													if ( to_do[ block_pos ] ){
														
														if ( block_num == 0 ){
															
															block_start = block_pos;
														}
														
														block_num++;
														
													}else{
														
														if ( block_num > 0 ){
															
															break;
														}
													}
													
													block_pos++;
												}
													
												if ( block_num == 0 ){
														
													break;
												}
												
												int	block_start_offset 	= 0;
												int blocks_length		= 0;
												
												for (int i=0;i<block_start+block_num;i++){
													
													int	block_size = dm_piece.getBlockSize( i );
													
													if ( i < block_start ){
													
														block_start_offset += block_size;
														
													}else{
														
														blocks_length += block_size;
													}
												}
												
												PeerManager pm = PluginCoreUtils.wrap( peer_manager );
												
												while( current_peer != null ){
													
													log( "Intervention: peer=" + current_peer.getIP() + ", piece=" + piece_number + ", block_start=" + block_start + ", block_num=" + block_num + ", offset=" + block_start_offset + ", length=" + blocks_length + ", rem=" + remaining );
													
													try{
														byte[] data = current_peer.read( piece_number, block_start_offset, blocks_length, remaining );
														
														int	data_offset = 0;
														
														for (int i=block_start;i<block_start+block_num;i++){
															
															int	block_size = dm_piece.getBlockSize( i );
															
															byte[] data_slice = new byte[ block_size ];
															
															System.arraycopy( data, data_offset, data_slice, 0, block_size );
															
															log( "    Read block " + i + ", offset=" + data_offset + ", length=" + block_size );
															
															pm.requestComplete(
																	disk_manager.createReadRequest( piece_number, block_start_offset + data_offset, block_size ),
																	new PooledByteBufferImpl( data_slice ),
																	current_peer.getDelegate());
															
															data_offset += block_size;
														}
																					
														break;
											
													}catch( Throwable e ){
											
														if ( !( e instanceof ExternalSeedException )){
															
															Debug.printStackTrace( e );
														}
														
														if ( http != null && http.size()> 0 ){
														
															current_peer = (ExternalSeedManualPeer)http.remove(0);

														}else{
														
															current_peer = null;
														}
													}
												}
											}
										}finally{
											
											synchronized( interventionHandler.this ){
												
												my_list.remove(0);
											}
										}
									}
								}
							};
							
						thread.start();
					}
								
						// don't let intervention stray too far into future
					
					if ( 	request_list.isEmpty() ||
							piece_number < ((long[])request_list.get(0))[0] + 10 ){
							
						if ( request_list.size() < 5 ){
							
							boolean	found = false;
							
							for (int i=0;i<request_list.size();i++){
								
								long[]	entry = (long[])request_list.get(i);
								
								if ( entry[0] == piece_number ){
									
									found = true;
									
									entry[1] = Math.min( stall_time, entry[1] );
								}
							}
							
							if ( !found ){
								
								log( "Intervention: queueing piece " + piece_number + ", stall_time=" + ( stall_time - SystemTime.getCurrentTime()));
								
								request_list.add( new long[]{ piece_number, stall_time });
								
								request_sem.release();
							}
						}
					}
				}
			}
		}
	}
	
	protected abstract class
	progressiveStats
		implements Cloneable
	{
		protected abstract boolean
		isProviderActive();
		
		protected abstract long
		getBytePosition();
		
		protected abstract long
		getStreamBytesPerSecondMax();
		
		protected abstract long
		getStreamBytesPerSecondMin();

		protected abstract long
		getInitialBytesDownloaded();
		
		protected abstract long
		getDownloadBytesPerSecond();
		
		protected abstract long
		getETA(
			boolean	ignore_min_buffer_size );
		
		protected abstract void
		setViewerBytePosition(
			long		bytes );
		
		protected abstract long
		getViewerBytePosition();
		
		protected abstract void
		update(
			int	tick_count );
		
		protected abstract void
		refreshMetaData();
		
		protected progressiveStats
		getCopy()
		{
			try{
				return((progressiveStats)clone());
				
			}catch( CloneNotSupportedException e ){
				
				Debug.printStackTrace(e);
				
				return( null );
			}
		}
		
		protected String
		formatBytes(
			long	l )
		{
			return( DisplayFormatters.formatByteCountToKiBEtc( l ));
		}
		
		protected String
		formatSpeed(
			long	l )
		{
			return( DisplayFormatters.formatByteCountToKiBEtcPerSec( l ));
		}

	}
	
	protected abstract class
	progressiveStatsCommon
		extends progressiveStats
	{
		private PieceRTAProvider	current_provider;
		private String				current_user_agent;
		
		protected long		total_file_length = download_manager.getSize();


		private Average		capped_download_rate_average 	= AverageFactory.MovingImmediateAverage( 10 );
		private Average		discard_rate_average 			= AverageFactory.MovingImmediateAverage( 10 );
		private long		last_discard_bytes				= download_manager.getStats().getDiscarded();
		
		private long		actual_bytes_to_download;
		private long		weighted_bytes_to_download;
		
		private long		provider_life_secs;
		private long		provider_initial_position;
		private long		provider_byte_position;
		private long		provider_last_byte_position	= -1;
		private long		provider_blocking_byte_position;
		private Average		provider_speed_average	= AverageFactory.MovingImmediateAverage( 10 );
		
		private long		last_eta	= -1;
		
		protected
		progressiveStatsCommon(
			DownloadManager					dm,
			EnhancedDownloadManagerFile		primary_file )
		{
			calculateSpeeds( dm, primary_file );
			
			setRTA( false );
			
			log( 	download_manager,
					"content_stream_bps=" + getStreamBytesPerSecondMin() +
					",primary=" + (primary_file==null?"null":primary_file.getString()),
					true );
		}
	
		protected void
		refreshMetaData()
		{
			calculateSpeeds( download_manager, primary_file );
		}
		
		protected abstract void
		calculateSpeeds(
			DownloadManager					 dm,
			EnhancedDownloadManagerFile		primary_file );
				
		protected void
		updateCurrentProvider(
			PieceRTAProvider	provider )
		{
			if ( current_provider != provider || provider == null ){
				
				current_provider 	= provider;
				current_user_agent	= null;
				
				provider_speed_average	= AverageFactory.MovingImmediateAverage( 10 );
				
				if ( current_provider == null ){
					
					provider_life_secs					= 0;
					provider_initial_position			= 0;
					provider_byte_position				= 0;
					provider_blocking_byte_position		= 0;
					provider_last_byte_position 		= -1;
					
				}else{
					
					provider_initial_position	= current_provider.getStartPosition();
					
					provider_byte_position 		= provider_initial_position;
					provider_last_byte_position	= provider_initial_position;
					
					provider_blocking_byte_position		= current_provider.getBlockingPosition();
					
					provider_life_secs = ( SystemTime.getCurrentTime() - current_provider.getStartTime()) / 1000;
					
					if ( provider_life_secs < 0 ){
						
						provider_life_secs = 0;
					}
				}
				
				setRTA( current_provider != null );
				
			}else{
				
				provider_life_secs++;
					
				if ( current_user_agent == null ){
				
					current_user_agent = current_provider.getUserAgent();
					
					if ( current_user_agent != null ){
						
						log( "Provider user agent = " + current_user_agent );
					}
				}
				
				provider_byte_position			= current_provider.getCurrentPosition();
				provider_blocking_byte_position	= current_provider.getBlockingPosition();
				
				long bytes_read = provider_byte_position - provider_last_byte_position;
					
				provider_speed_average.update( bytes_read );
	
				provider_last_byte_position = provider_byte_position;
			}
		}
		
		protected boolean
		isProviderActive()
		{
			return( current_provider != null );
		}
		
		protected long
		getInitialProviderPosition()
		{
			return( provider_initial_position );
		}
		
		protected long
		getProviderBytePosition()
		{
			return( provider_byte_position );
		}
		
		protected long
		getProviderLifeSecs()
		{
			return( provider_life_secs );
		}
		
		protected void
		update(
			int		tick_count )
		{
			long download_rate = download_manager.getStats().getDataReceiveRate();
			
			capped_download_rate_average.update( download_rate );
			
			long	discards = download_manager.getStats().getDiscarded();
			
			discard_rate_average.update( discards - last_discard_bytes );
			
			last_discard_bytes = discards;
			
			DiskManager	disk_manager = download_manager.getDiskManager();
			
			PiecePicker	picker = current_piece_pickler;

			if ( getStreamBytesPerSecondMin() > 0 && disk_manager != null && picker != null ){
				
				List	providers = picker.getRTAProviders();
				
				long	max_cp	= 0;
				
				PieceRTAProvider	best_provider = null;
				
				for (int i=0;i<providers.size();i++){
					
					PieceRTAProvider	provider = (PieceRTAProvider)providers.get(i);
					
					if ( provider.getStartTime() > 0 ){
						
						long	cp = provider.getCurrentPosition();
						
						if ( cp >= max_cp ){
							
							best_provider = provider;
							
							max_cp	= cp;
						}
					}
				}

				updateCurrentProvider( best_provider );
				
				// System.out.println( "prov_ini=" + provider_initial_position + ", life=" + provider_life_secs + ", pos=" + provider_byte_position );
				
				updateViewerPosition();
				
				if ( best_provider != null ){
							
						// only report buffer if we have a bit of slack
					
					long	buffer_secs = getViewerBufferSeconds();
					
					if ( buffer_secs < 10 ){
						
							// no point in having a very small buffer as we end up with
							// too much discard. Given we're doing a long-term stream here the
							// aggressiveness applied when rta gets close to "now" isn't needed
						
						buffer_secs = 10;
					}
					
					best_provider.setBufferMillis( buffer_secs * 1000 );
				}
				
				DiskManagerPiece[] pieces = disk_manager.getPieces();
				
				actual_bytes_to_download 	= 0;
				weighted_bytes_to_download	= 0;
				
				int	first_incomplete_piece = -1;
				
				int	piece_size = disk_manager.getPieceLength();
				
				for (int i=(int)(provider_byte_position/piece_size);i<pieces.length;i++){
					
					DiskManagerPiece piece = pieces[i];
					
					if ( piece.isDone()){
						
						continue;
					}
					
					if ( first_incomplete_piece == -1 ){
						
						first_incomplete_piece = i;
					}
					
					boolean[] blocks = piece.getWritten();
					
					int	bytes_this_piece = 0;
					
					if ( blocks == null ){
						
						bytes_this_piece = piece.getLength();
						
					}else{
						for (int j=0;j<blocks.length;j++){
							
							if ( !blocks[j] ){
								
								bytes_this_piece += piece.getBlockSize( j );
							}
						}
					}
					
					if ( bytes_this_piece > 0 ){
						
						actual_bytes_to_download += bytes_this_piece;
						
						int	diff = i - first_incomplete_piece;
						
						if ( diff == 0 ){
							
							weighted_bytes_to_download += bytes_this_piece;
							
						}else{
														
							int	weighted_bytes_done =  piece.getLength() - bytes_this_piece;
						
							weighted_bytes_done = ( weighted_bytes_done * ( pieces.length - i )) / (pieces.length - first_incomplete_piece);
						
							weighted_bytes_to_download += piece.getLength() - weighted_bytes_done;
						}
					}
				}
			}
			
			log( getString(), tick_count % LOG_PROG_STATS_TICKS == 0 );
		}
		
		protected abstract void
		updateViewerPosition();
		
		protected abstract long
		getInitialBufferBytes(
			long		dl_rate,
			boolean		ignore_min_buffer_size );
		
		protected long
		getETA(
			boolean ignore_min_buffer_size )
		{
			DiskManager dm = download_manager.getDiskManager();
			
			if ( dm == null ){
				
				return( Long.MAX_VALUE );
			}
			
			if ( dm.getRemainingExcludingDND() == 0 ){
				
				return( 0 );
			}
			
			long download_rate = getDownloadBytesPerSecond();
			
			if ( download_rate <= 0 ){
				
				return( Long.MAX_VALUE );
			}
			
			final long	min_dl	= getInitialBufferBytes( download_rate, ignore_min_buffer_size );
			
			long	initial_downloaded	= getInitialBytesDownloaded( min_dl );
			
			long rem_dl = min_dl - initial_downloaded;	// ok as initial dl is forced in order byte buffer-rta
			
			long rem_secs = rem_dl / download_rate;
			
			long	secs_to_download = getSecondsToDownload();
			
				// increase time to download a bit so we don't start streaming too soon
				// we'll always lose time due to speed variations, discards, hashfails...
			
			secs_to_download = secs_to_download + (secs_to_download/10);
			
			long	secs_to_watch = getSecondsToWatch();
			
			long eta = secs_to_download - secs_to_watch;
			
			if ( rem_secs > eta && rem_secs > 0 ){
				
				eta = rem_secs;
			}
			
			if ( !ignore_min_buffer_size ){
				
				if ( eta == 0 && last_eta != 0 ){
					
					last_eta = eta;
					
					log( "ETA=0: rate=" + DisplayFormatters.formatByteCountToKiBEtcPerSec( download_rate ) +
							",init_buff=" + min_dl +",to_dl=" + secs_to_download + ",to_watch=" + secs_to_watch );
				}
			}
			
			return( eta );
		}
	
		public long
		getInitialBytesDownloaded()
		{
			return( getInitialBytesDownloaded( Long.MAX_VALUE ));
		}
		
		protected long 
		getDownloadBytesPerSecond() 
		{
			long	original = (long)capped_download_rate_average.getAverage();
			
			long	current	= original;
			
			int	dl_limit = download_manager.getStats().getDownloadRateLimitBytesPerSecond();
			
			if ( dl_limit > 0 ){
				
				current = Math.min( current, dl_limit );
			}
			
			int global_limit = TransferSpeedValidator.getGlobalDownloadRateLimitBytesPerSecond();
			
			if ( global_limit > 0 ){
				
				current = Math.min( current, global_limit );
			}
						
			return( current );
		}
		
		public long
		getInitialBytesDownloaded(
			long	stop_counting_after )
		{
			DiskManager dm = download_manager.getDiskManager();
	
			if ( dm == null ){
				
				return( 0 );
			}
			
			long	initial_downloaded = 0;
			
			DiskManagerPiece[] pieces = dm.getPieces();
			
			for (int i=0;i<pieces.length;i++){
				
				DiskManagerPiece piece = pieces[i];
				
				if ( piece.isDone()){
					
					initial_downloaded += piece.getLength();
					
				}else{
								
					boolean[] blocks = piece.getWritten();
								
					if ( blocks == null ){
						
						break;
						
					}else{
						
						for (int j=0;j<blocks.length;j++){
						
							if ( blocks[j] ){
								
								initial_downloaded += piece.getBlockSize( j );
								
							}else{
								
								break;
							}
						}
						
						break;
					}
				}
									
				if ( initial_downloaded >= stop_counting_after ){
						
					break;
				}
			}
			
			return( initial_downloaded );
		}
		
		protected long
		getSecondsToDownload()
		{
			long download_rate = getDownloadBytesPerSecond();

			if ( download_rate == 0 ){
				
				return( Long.MAX_VALUE );
			}
			
			return( weighted_bytes_to_download / download_rate );
		}
		
		protected long
		getSecondsToWatch()
		{
			return((total_file_length - getViewerBytePosition()) / getStreamBytesPerSecondMin());
		}
		
		protected long
		getBytePosition()
		{
			return( getViewerBytePosition());
		}
				
		protected long
		getViewerBufferSeconds()
		{
			return((provider_byte_position - getViewerBytePosition() ) / getStreamBytesPerSecondMax() );
		}
				
		protected String
		getString()
		{
			long	dl_rate = getDownloadBytesPerSecond();
			
			long	init_bytes = getInitialBufferBytes(dl_rate,false);
			
			return( "play_eta=" + getETA(false) + "/d=" + getSecondsToDownload() + "/w=" + getSecondsToWatch()+ 
					", dl_rate=" + formatSpeed(dl_rate)+ ", download_rem=" + formatBytes(weighted_bytes_to_download) + "/" + formatBytes(actual_bytes_to_download) +
					", discard_rate=" + formatSpeed((long)discard_rate_average.getAverage()) +
					", init_done=" + getInitialBytesDownloaded(init_bytes) + ", init_buff=" + init_bytes +
					", viewer: byte=" + formatBytes( getViewerBytePosition()) + " secs=" + ( getViewerBytePosition()/getStreamBytesPerSecondMin() ) + 
					", prov: byte=" + formatBytes( provider_byte_position ) + " secs=" + ( provider_byte_position/getStreamBytesPerSecondMin()) + " speed=" + formatSpeed((long)provider_speed_average.getAverage()) +
					" block= " + formatBytes( provider_blocking_byte_position ) + " buffer=" + formatBytes( provider_byte_position - getViewerBytePosition() ) + "/" + getViewerBufferSeconds());
		}
	}

	protected class
	progressiveStatsExternal
		extends progressiveStatsCommon
	{
		private long	content_stream_bps_min;
		private long	content_stream_bps_max;

		private long	viewer_byte_position;
		
		protected
		progressiveStatsExternal(
			DownloadManager					download_manager,
			EnhancedDownloadManagerFile		primary_file )
		{
			super( download_manager, primary_file );
		}
		
		protected void
		calculateSpeeds(
			DownloadManager					download_manager,
			EnhancedDownloadManagerFile		primary_file )
		{
			TOTorrent	torrent = download_manager.getTorrent();

			if ( torrent == null ){
				
				return;
			}
			
			content_stream_bps_min = explicit_progressive?content_min_delivery_bps:PlatformTorrentUtils.getContentStreamSpeedBps( torrent );
			
			if ( content_stream_bps_min == 0 ){
			
					// hack in some test values for torrents that don't have a bps in them yet
				
				long	size = torrent.getSize();
				
				if ( size < 200*1024*1024 ){
				
					content_stream_bps_min = 30*1024;
					
				}else if ( size < 1000*1024*1024L ){
					
					content_stream_bps_min = 200*1024;
					
				}else{

					content_stream_bps_min = 400*1024;
				}
			}
				
					// bump it up by a bit to be conservative to deal with fluctuations, discards etc.
				
			content_stream_bps_max = content_stream_bps_min + ( content_stream_bps_min / 5 );
		}
		
		protected long
		getStreamBytesPerSecondMax()
		{
			return( content_stream_bps_max );
		}

		protected long
		getStreamBytesPerSecondMin()
		{
			return( content_stream_bps_min );
		}

		public long
		getInitialBufferBytes(
			long		download_rate,
			boolean		ignore_min_buffer_size )
		{
			long min_dl = minimum_initial_buffer_secs_for_eta * getStreamBytesPerSecondMax();
				
				// factor in any explicit minimum buffer bytes
			
			min_dl = Math.max( min_dl, ignore_min_buffer_size?0:explicit_minimum_buffer_bytes );
			
				// see if we have any stream-specific advice
			
			long advice = primary_file.getInitialBufferBytes( download_rate );
			
			min_dl = Math.max( advice, min_dl );
			
			return( min_dl );
		}
		
		protected void
		updateViewerPosition()
		{
			viewer_byte_position 	= getInitialProviderPosition() + (getStreamBytesPerSecondMax() * getProviderLifeSecs());
			
			if ( viewer_byte_position > total_file_length ){
				
				viewer_byte_position = total_file_length;
			}
			
			if ( viewer_byte_position > getProviderBytePosition()){
				
				viewer_byte_position = getProviderBytePosition();
			}
		}
		
		protected void 
		setViewerBytePosition(
			long bytes) 
		{
			// nothing for external viewer case as this doesn't get called
			
			Debug.out( "eh?" );
		}

		protected long
		getViewerBytePosition()
		{
			return( viewer_byte_position );
		}
	}
	
	protected class
	progressiveStatsInternal
		extends progressiveStatsCommon
	{
		private long	content_stream_bps_min;
		private long	content_stream_bps_max;

		private long	viewer_byte_position;
		private long	viewer_byte_position_set_time;
				
		private long	last_warning;
		
		protected
		progressiveStatsInternal(
			DownloadManager					dm,
			EnhancedDownloadManagerFile		primary_file )
		{
			super( dm, primary_file );
		}
		
		protected void
		calculateSpeeds(
			DownloadManager					download_manager,
			EnhancedDownloadManagerFile		primary_file )
		{
			TOTorrent	torrent = download_manager.getTorrent();

			if ( torrent == null ){
				
				return;
			}
						
			content_stream_bps_min = explicit_progressive?content_min_delivery_bps:PlatformTorrentUtils.getContentStreamSpeedBps( torrent );
			
			if ( content_stream_bps_min == 0 ){
			
					// hack in some test values for torrents that don't have a bps in them yet
				
				long	size = torrent.getSize();
				
				if ( size < 200*1024*1024 ){
				
					content_stream_bps_min = 30*1024;
					
				}else if ( size < 1000*1024*1024L ){
					
					content_stream_bps_min = 200*1024;
					
				}else{

					content_stream_bps_min = 400*1024;
				}
			}
				
				// bump it up by a bit to be conservative to deal with fluctuations, discards etc.
				
			content_stream_bps_min += internal_content_stream_bps_increase_absolute;
			
			content_stream_bps_max = content_stream_bps_min + ( content_stream_bps_min / internal_content_stream_bps_increase_ratio );
		}
		
		protected long
		getStreamBytesPerSecondMax()
		{
			return( content_stream_bps_max );
		}

		protected long
		getStreamBytesPerSecondMin()
		{
			return( content_stream_bps_min );
		}

		public long
		getInitialBufferBytes(
			long	download_rate,
			boolean	ignore_min_buffer_size )
		{
			long min_dl = ignore_min_buffer_size?0:explicit_minimum_buffer_bytes;
			
				// see if we have any stream-specific advice
			
			long advice = primary_file.getInitialBufferBytes( download_rate );
			
			if ( advice == 0 ){
				
					// no advice, fall back to computed min
				
				advice = minimum_initial_buffer_secs_for_eta * getStreamBytesPerSecondMax();
				
			}else{
				
					// currently the player will auto-pause if the buffer falls below the
					// explicit minimum so we need to add the explicit to the advice to
					// get a value that will prevent a stall
				
				if ( !ignore_min_buffer_size ){
					
					advice += explicit_minimum_buffer_bytes;
				}
			}
			
			min_dl = Math.max( advice, min_dl );
			
			return( min_dl );
		}
		
		protected void
		updateViewerPosition()
		{
		}
		
		protected void 
		setViewerBytePosition(
			long bytes) 
		{
			viewer_byte_position_set_time = SystemTime.getCurrentTime();
			
			viewer_byte_position = bytes;
		}

		protected long
		getViewerBytePosition()
		{
			long	now = SystemTime.getCurrentTime();
			
			if ( now < viewer_byte_position_set_time ){
				
				viewer_byte_position_set_time = now;
				
			}else if ( now - viewer_byte_position_set_time > 10000 ){
				
				if ( viewer_byte_position != 0 ){
				
					if ( now < last_warning || now - last_warning >= 1000 ){
					
						last_warning	= now;
						
						log( "No recent viewer position update (current=" + viewer_byte_position + ")" );
					}
				}
			}
			
			return( viewer_byte_position );
		}
	}
}
