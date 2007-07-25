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
import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.disk.DiskManagerPiece;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.download.DownloadManagerPeerListener;
import org.gudy.azureus2.core3.download.impl.DownloadManagerAdapter;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPeerManagerStats;
import org.gudy.azureus2.core3.peer.PEPeerStats;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.AEDiagnostics;
import org.gudy.azureus2.core3.util.AEDiagnosticsLogger;
import org.gudy.azureus2.core3.util.ConcurrentHasher;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;

import com.aelitis.azureus.core.peer.cache.CacheDiscovery;
import com.aelitis.azureus.core.peer.cache.CachePeer;
import com.aelitis.azureus.core.peermanager.piecepicker.PiecePicker;
import com.aelitis.azureus.core.peermanager.piecepicker.PieceRTAProvider;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.core.util.average.Average;
import com.aelitis.azureus.core.util.average.AverageFactory;
import com.aelitis.azureus.util.Constants;
import com.aelitis.azureus.util.DownloadUtils;

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
	
	public static final int LOG_PROG_STATS_PERIOD	= 10*1000;
	public static final int LOG_PROG_STATS_TICKS	= LOG_PROG_STATS_PERIOD/DownloadManagerEnhancer.TICK_PERIOD;

	private static final String TRACKER_PROG_PREFIX	= "azprog";
	
	
	private static final String PM_SEED_TIME_KEY = "EnhancedDownloadManager:seedtime";
	private static final String PEER_CACHE_KEY = "EnhancedDownloadManager:cachepeer";
	
	private DownloadManagerEnhancer		enhancer;
	private DownloadManager				download_manager;
	
	private boolean		platform_content;
	
	private transient PiecePicker		current_piece_pickler;
	
	
	
	private boolean	progressive_active	= false;
	
	private long	content_stream_bps_min;
	private long	content_stream_bps_max;
	private long	content_min_bps;
		
	private int		minimum_initial_buffer_secs_for_eta;
	
	private bufferETAProvider	buffer_provider	= new bufferETAProvider();
	private boostETAProvider	boost_provider	= new boostETAProvider();

	private progressiveStats	progressive_stats;

	private boolean				progressive_informed = false;
	
	private long	time_download_started;
	private Average	download_speed_average	= AverageFactory.MovingImmediateAverage( 5 );
	
	private boolean	marked_active;
	private boolean	destroyed;

		// ********* reset these in resetVars ***********
	
	private long	last_speed_increase;
	private long	last_peer_inject;
	private long	last_lookup_time;
	
	private LinkedList	new_peers;
	private List		cache_peers;
	private List		disconnected_cache_peers;
	
	private CachePeer[]	lookup_peers;
	private DownloadManagerListener dmListener;
	
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
		
		progressive_stats	= new progressiveStats();

		TOTorrent	torrent = download_manager.getTorrent();
				
		if ( torrent != null ){
			
			platform_content = PlatformTorrentUtils.isContent( torrent, true );
			
			calculateSpeeds();
		}
		
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
					
				public void
				pieceAdded(
					PEPiece 	piece )
				{
				}
					
				public void
				pieceRemoved(
					PEPiece		piece )
				{
				}
			});
	}

	public String
	getName()
	{
		return( download_manager.getDisplayName());
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
	
	protected void
	refreshMetaData()
	{
		calculateSpeeds();
	}
	
	protected void
	calculateSpeeds()
	{
		TOTorrent	torrent = download_manager.getTorrent();

		if ( torrent == null ){
			
			return;
		}
		
		long old_content_stream_bps_min = content_stream_bps_min;
		
		long original_stream_bps = content_stream_bps_min = PlatformTorrentUtils.getContentStreamSpeedBps( torrent );
		
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
			
				// dump it up by a bit to be conservative to deal with fluctuations, discards etc.
			
		content_stream_bps_max = content_stream_bps_min + ( content_stream_bps_min / 5 );
		
		content_min_bps = PlatformTorrentUtils.getContentMinimumSpeedBps( torrent );
					
		if ( old_content_stream_bps_min == 0 && content_stream_bps_min > 0 ){
			
			log( 	"content_stream_bps=" + content_stream_bps_min + " (orig=" + original_stream_bps + ")" +
					",content_min_bps=" + content_min_bps );
		}
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
		long	target_speed = progressive_active?content_stream_bps_max:content_min_bps;
		
		if ( target_speed < content_min_bps ){
			
			target_speed = content_min_bps;
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
								
								pm.addPeer( peer.getAddress().getHostAddress(), peer.getPort(), 0, false );
								
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
		
		return( content_stream_bps_min > 0 && enhancer.isProgressiveAvailable() && PlatformTorrentUtils.isContentProgressive( torrent ));
	}
	
	public void
	setProgressiveMode(
		boolean		active )
	{
		TOTorrent	torrent = download_manager.getTorrent();
		
		if ( torrent == null ){

			return;
		}
		
		synchronized( this ){

			if ( progressive_active == active ){
				
				return;
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
					if (dmCheck == download_manager) {
						continue;
					}

					if (!dmCheck.isDownloadComplete(false)
							&& PlatformTorrentUtils.getAdId(dmCheck.getTorrent()) == null) {
						int state = dmCheck.getState();
						if (state == DownloadManager.STATE_DOWNLOADING
								|| state == DownloadManager.STATE_QUEUED) {
							dmCheck.pause();
						}
						EnhancedDownloadManager edmCheck = enhancer.getEnhancedDownload(dmCheck);
						if (edmCheck != null && edmCheck.getProgressiveMode()) {
							edmCheck.setProgressiveMode(false);
						}
					}
				}
				if (download_manager.isPaused()) {
					download_manager.resume();
				}
			} else {
				download_manager.removeListener(dmListener);
				gm.resumeDownloads();
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
					
					progressive_stats = new progressiveStats();
				}
			}else{
				
				progressive_stats = new progressiveStats();
			}
		}
		
		if ( active && !progressive_informed ){
			
			progressive_informed	= true;
			
				// tell tracker we're progressive so it can, if required, schedule more seeds
			
			Download	plugin_dl = PluginCoreUtils.wrap( download_manager );
			
			DownloadUtils.addTrackerExtension( plugin_dl, TRACKER_PROG_PREFIX, "y" );
			
			download_manager.requestTrackerAnnounce( true );
		}
	}


	public boolean
	getProgressiveMode()
	{
		return( progressive_active );
	}
	
	public long
	getProgressivePlayETA()
	{
		progressiveStats stats = getProgressiveStats();
		
		long	eta = stats.getETA();
				
		return( eta );
	}
	
	protected progressiveStats
	getProgressiveStats()
	{
		synchronized( this ){
			
			return( progressive_stats.getCopy());
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
			
			progressive_stats.update( tick_count );
		}
	}
	
	protected void
	setRTA(
		boolean	active )
	{
		synchronized( this ){

			if ( marked_active && !active ){
								
				marked_active = false;

				ConcurrentHasher.getSingleton().removeRealTimeTask();
			}
			
			if ( destroyed ){
				
				return;
			}
			
			if ( !marked_active && active ){
				
				marked_active = true;

				ConcurrentHasher.getSingleton().addRealTimeTask();
			}
		}
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
		str = download_manager.getDisplayName() + ": " + str;
		
		if ( to_file ){
			
			AEDiagnosticsLogger diag_logger = AEDiagnostics.getLogger("v3.Stream");
			
			diag_logger.log(str);
		}
		
		if (Constants.DIAG_TO_STDOUT) {
			
			System.out.println(Thread.currentThread().getName() + "|"
					+ System.currentTimeMillis() + "] " + str);
		}
	}
	
	protected class
	bufferETAProvider
		implements PieceRTAProvider
	{
		private long[]		piece_rtas;
		
		protected void
		activate(	
			PiecePicker		picker )
		{
			synchronized( EnhancedDownloadManager.this ){

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
			synchronized( EnhancedDownloadManager.this ){
									
				picker.removeRTAProvider( this );
				
				piece_rtas	= null;
			}
		}
		
		public long[]
    	updateRTAs(
    		PiecePicker		picker )
    	{
				// be force linear downloading until we have enough to allow the user to 
				// potentially start playing. If they don't do so immediately then until that
				// time we'll be doing normal BT download
			
    		DiskManager	dm = download_manager.getDiskManager();

    		if ( dm != null ){

    			if ( getProgressivePlayETA() <= 0 ){
      				
    				deactivate( picker );
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
		
		protected void
		activate(	
			PiecePicker		picker )
		{
			if ( content_stream_bps_min > 0 ){
				
				synchronized( EnhancedDownloadManager.this ){
					
					picker.addRTAProvider( this );
				}
			}
		}
		
		protected void
		deactivate(
			PiecePicker		picker )
		{
			synchronized( EnhancedDownloadManager.this ){
									
				picker.removeRTAProvider( this );
				
				piece_rtas	= null;
			}
		}
		
		public long[]
    	updateRTAs(
    		PiecePicker		picker )
    	{
			long	now = SystemTime.getCurrentTime();
			
			if ( now < last_recalc || now - last_recalc > 2500 ){
				
				last_recalc	= now;
								
				DiskManager	disk_manager = download_manager.getDiskManager();

					// if it'll take less time to download than watch then the channel-based rta logic
					// will do the job.
				
				progressiveStats	stats = getProgressiveStats();
				
				if ( 	disk_manager == null || 
						!stats.isProviderActive() || 
						stats.getETA() < -MINIMUM_INITIAL_BUFFER_SECS  ||
						content_stream_bps_min == 0 ){
					
					piece_rtas = null;
					
				}else{
	
					piece_rtas = new long[disk_manager.getNbPieces()];
					
						// need to force piece order - set RTAs for all outstanding pieces
					
					long	piece_size = disk_manager.getPieceLength();
					
					int		start_piece = (int)( stats.getBytePosition() / piece_size );
						
					long	bytes_offset = 0;
					
					for ( int i=start_piece;i<piece_rtas.length;i++ ){
						
						piece_rtas[i] = now + ( 1000* ( bytes_offset / content_stream_bps_max ));
						
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
	}
	
	protected class
	progressiveStats
		implements Cloneable
	{
		private PieceRTAProvider	current_provider;
		private String				current_user_agent;
		
		private long		total_file_length = download_manager.getSize();


		private Average		download_rate_average 	= AverageFactory.MovingImmediateAverage( 10 );
		private Average		discard_rate_average 	= AverageFactory.MovingImmediateAverage( 10 );
		private long		last_discard_bytes		= download_manager.getStats().getDiscarded();
		
		private long		actual_bytes_to_download;
		private long		weighted_bytes_to_download;
		
		private long		viewer_byte_position;
				
		private long		provider_life_secs;
		private long		provider_initial_position;
		private long		provider_byte_position;
		private long		provider_last_byte_position	= -1;
		private long		provider_blocking_byte_position;
		private Average		provider_speed_average	= AverageFactory.MovingImmediateAverage( 10 );
			
		protected
		progressiveStats()
		{
			setRTA( false );
		}
		
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
				
				provider_byte_position	= current_provider.getCurrentPosition();
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
		
		protected void
		update(
			int		tick_count )
		{
			long download_rate = download_manager.getStats().getDataReceiveRate();
			
			download_rate_average.update( download_rate );
			
			long	discards = download_manager.getStats().getDiscarded();
			
			discard_rate_average.update( discards - last_discard_bytes );
			
			last_discard_bytes = discards;
			
			DiskManager	disk_manager = download_manager.getDiskManager();
			
			PiecePicker	picker = current_piece_pickler;

			if ( content_stream_bps_min > 0 && disk_manager != null && picker != null ){
				
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
				
				viewer_byte_position 	= provider_initial_position + (content_stream_bps_max * provider_life_secs );
				
				if ( viewer_byte_position > total_file_length ){
					
					viewer_byte_position = total_file_length;
				}
				
				if ( viewer_byte_position > provider_byte_position ){
					
					viewer_byte_position = provider_byte_position;
				}
				
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
		
		protected long
		getETA()
		{
			DiskManager dm = download_manager.getDiskManager();
			
			if ( dm == null ){
				
				return( Long.MAX_VALUE );
			}
			
			if ( dm.getRemainingExcludingDND() == 0 ){
				
				return( 0 );
			}
			
			long download_rate = (long)download_rate_average.getAverage();
			
			if ( download_rate <= 0 ){
				
				return( Long.MAX_VALUE );
			}
			
			long min_dl = minimum_initial_buffer_secs_for_eta * content_stream_bps_max;
			
				// work out number of initial bytes downloaded and stop as soon as a gap is found
			
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
					}
				}
									
				if ( initial_downloaded >= min_dl ){
						
					break;
				}
			}
			
			long rem_dl = min_dl - initial_downloaded;	// ok as initial dl is forced in order byte buffer-rta
			
			long rem_secs = rem_dl / download_rate;
			
			long	secs_to_download = getSecondsToDownload();
			
				// increase time to download a bit so we don't start streaming too soon
				// we'll always lose time due to speed variations, discards, hashfails...
			
			secs_to_download = secs_to_download + (secs_to_download/10);
			
			long eta = secs_to_download - getSecondsToWatch();
			
			if ( rem_secs > eta ){
				
				eta = rem_secs;
			}
			
			return( eta );
		}
		
		protected long
		getSecondsToDownload()
		{
			long download_rate = (long)download_rate_average.getAverage();

			if ( download_rate == 0 ){
				
				return( Long.MAX_VALUE );
			}
			
			return( weighted_bytes_to_download / download_rate );
		}
		
		protected long
		getSecondsToWatch()
		{
			return((total_file_length - viewer_byte_position ) / content_stream_bps_min );
		}
		
		protected long
		getBytePosition()
		{
			return( viewer_byte_position );
		}
		
		protected long
		getViewerBufferSeconds()
		{
			return((provider_byte_position - viewer_byte_position ) / content_stream_bps_max );
		}
		
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
		
		protected String
		getString()
		{
			return( "play_eta=" + getETA() + "/d=" + getSecondsToDownload() + "/w=" + getSecondsToWatch()+ 
					", dl_rate=" + formatSpeed((long)download_rate_average.getAverage())+ ", download_rem=" + formatBytes(weighted_bytes_to_download) + "/" + formatBytes(actual_bytes_to_download) +
					", discard_rate=" + formatSpeed((long)discard_rate_average.getAverage()) +
					", viewer: byte=" + formatBytes( viewer_byte_position ) + " secs=" + ( viewer_byte_position/content_stream_bps_min ) + 
					", prov: byte=" + formatBytes( provider_byte_position ) + " secs=" + ( provider_byte_position/content_stream_bps_min ) + " speed=" + formatSpeed((long)provider_speed_average.getAverage()) +
					" block= " + formatBytes( provider_blocking_byte_position ) + " buffer=" + formatBytes( provider_byte_position - viewer_byte_position ) + "/" + getViewerBufferSeconds());
		}
	}
}
