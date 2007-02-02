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

import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerPiece;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerPeerListener;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPeerManagerStats;
import org.gudy.azureus2.core3.peer.PEPeerStats;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.Average;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.peer.cache.CacheDiscovery;
import com.aelitis.azureus.core.peer.cache.CachePeer;
import com.aelitis.azureus.core.peermanager.piecepicker.PiecePicker;
import com.aelitis.azureus.core.peermanager.piecepicker.PiecePriorityProvider;
import com.aelitis.azureus.core.peermanager.piecepicker.PieceRTAProvider;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;

public class 
EnhancedDownloadManager 
{
	public static final int	MINIMUM_INITIAL_BUFFER_SECS	= 30;
	
	public static final int SPEED_CONTROL_INITIAL_DELAY	= 10*1000;
	public static final int SPEED_INCREASE_GRACE_PERIOD	= 3*1000;
	public static final int PEER_INJECT_GRACE_PERIOD	= 3*1000;
	
	public static final int CACHE_RECONNECT_MIN_PERIOD	= 30*60*1000;
	
	public static final int TARGET_SPEED_EXCESS_MARGIN	= 2*1024;
	
	private static final String PEER_CACHE_KEY = "EnhancedDownloadManager:cachepeer";
	
	private DownloadManagerEnhancer		enhancer;
	private DownloadManager				download_manager;
	
	private transient PiecePicker		current_piece_pickler;
	
	private long		last_eta_result	= Long.MAX_VALUE;
	private long		last_eta_time;
	
	private boolean	progressive_active	= false;
	
	private long	content_stream_bps;
	private long	content_min_bps;
	
	private int		initial_buffer_pieces;
	
	private bufferETAProvider	buffer_provider	= new bufferETAProvider();
	private boostETAProvider	boost_provider	= new boostETAProvider();
	
	private long	time_download_started;
	private Average	download_speed_average	= Average.getInstance( 1000, 5 );
	
	private long	last_speed_increase;
	private long	last_peer_inject;
	
	private LinkedList	new_peers;
	private List		cache_peers;
	
	private CachePeer[]	lookup_peers;
	
	protected
	EnhancedDownloadManager(
		DownloadManagerEnhancer		_enhancer,
		DownloadManager				_download_manager )
	{
		enhancer			= _enhancer;
		download_manager	= _download_manager;
		
		TOTorrent	torrent = download_manager.getTorrent();
				
		if ( torrent != null ){
			
			content_stream_bps = PlatformTorrentUtils.getContentStreamSpeedBps( torrent );
			
			if ( content_stream_bps == 0 ){
			
					// hack in some test values for torrents that don't have a bps in them yet
				
				long	size = torrent.getSize();
				
				if ( size < 200*1024*1024 ){
				
					content_stream_bps = 30*1024;
					
				}else if ( size < 1000*1024*1024L ){
					
					content_stream_bps = 200*1024;
					
				}else{

					content_stream_bps = 400*1024;
				}
			}
			
			content_min_bps = PlatformTorrentUtils.getContentMinimumSpeedBps( torrent );
			
			long	initial_bytes = MINIMUM_INITIAL_BUFFER_SECS * content_stream_bps;
	
			initial_buffer_pieces = (int)( initial_bytes / torrent.getPieceLength());
			
			initial_buffer_pieces = Math.min( initial_buffer_pieces, torrent.getNumberOfPieces());
			
			// setProgressiveMode( true );
		}
		
		download_manager.addPeerListener(
			new DownloadManagerPeerListener()
			{
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
					}
				}
				
				public void
				peerManagerRemoved(
					PEPeerManager	manager )
				{
					synchronized( EnhancedDownloadManager.this ){

						time_download_started = 0;

						if ( current_piece_pickler != null ){
					
							buffer_provider.deactivate(  current_piece_pickler );
							
							current_piece_pickler	= null;	
						}
					}
				}
				
				public void
				peerAdded(
					PEPeer 	peer )
				{
					synchronized( EnhancedDownloadManager.this ){
						
						if ( new_peers == null ){
							
							new_peers = new LinkedList();
						}
						
						new_peers.add( peer );
					}
				}
					
				public void
				peerRemoved(
					PEPeer	peer )
				{
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
		long	target_speed = progressive_active?content_stream_bps:content_min_bps;
		
		if ( target_speed < content_min_bps ){
			
			target_speed = content_min_bps;
		}
			
		return( target_speed );
	}
	
	protected void
	checkSpeed()
	{
		if ( download_manager.getState() != DownloadManager.STATE_DOWNLOADING ){
			
			return;
		}
		
		long	target_speed = getTargetSpeed();
		
		PEPeerManager	pm = download_manager.getPeerManager();
		
		if ( pm != null ){
			
			PEPeerManagerStats stats = pm.getStats();
			
			long	download_speed = stats.getDataReceiveRate();
			
			download_speed_average.addValue( download_speed );
			
			long	time_downloading = getTimeRunning();
			
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
										
										if ( target_speed <= 0 ){
										
											peers_to_kick.add( peer );
											
										}else{
											
											long	current_speed = download_speed_average.getAverage();
											
												// if we are already exceeding required speed, block
												// the cache peer download
											
											if ( current_speed + 2*1024 > target_speed ){
												
												peer.getStats().setDownloadRateLimitBytesPerSecond( -1 );
											}
											
											if ( cache_peers == null ){
												
												cache_peers = new LinkedList();
											}
											
											cache_peers.add( peer );
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
			
			if ( time_downloading > SPEED_CONTROL_INITIAL_DELAY ){
				
				long	current_average = download_speed_average.getAverage();
					
				if ( current_average < target_speed ){
					
					long	current_speed = getCurrentSpeed();
					
						// increase cache peer contribution
						// due to latencies we need to give speed increases a time to take
						// effect to see if the limits can be reached
					
					long	difference = target_speed - current_speed;

					long	now = SystemTime.getCurrentTime();
					
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
											
											peer_stats.setDownloadRateLimitBytesPerSecond((int)target_speed );
											
											last_speed_increase = now;
											
											difference = 0;
										}
									}
								}
							}
						}
										
						if ( 	difference > 0 &&
								last_peer_inject > now || now - last_peer_inject > PEER_INJECT_GRACE_PERIOD ){
							
								// can't do the job with existing cache peers, try to find some more
							
							if ( lookup_peers == null ){
								
								lookup_peers = CacheDiscovery.lookup( download_manager.getTorrent());
							}
							
							if ( lookup_peers.length > 0 ){
								
								Set	connected_peers = new HashSet();
								
								if ( cache_peers != null ){
									
									Iterator	it = cache_peers.iterator();
									
									while( it.hasNext() && difference > 0 ){
								
										PEPeer	peer = (PEPeer)it.next();
							
										connected_peers.add( peer.getIp() + ":" + peer.getPort());
									}
								}
								
								List	peers_to_try = new ArrayList();
								
								for (int i=0;i<lookup_peers.length;i++){
									
									CachePeer	cp = lookup_peers[i];
									
									if ( now - cp.getInjectTime(now) > CACHE_RECONNECT_MIN_PERIOD ){
										
										if ( !connected_peers.contains( cp.getAddress().getHostAddress() + ":" + cp.getPort())){
										
											peers_to_try.add( cp );
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
										
										peer_stats.setDownloadRateLimitBytesPerSecond((int)( peer_rate - difference ));
										
										difference = 0;
										
									}else{
									
										peer_stats.setDownloadRateLimitBytesPerSecond( -1 );
										
										difference -= peer_rate;
									}
								}
							}
						}
					}
				}
			}
		}
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
		
		return( content_stream_bps > 0 && enhancer.isProgressiveAvailable() && PlatformTorrentUtils.isContentProgressive( torrent ));
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

			if ( progressive_active== active ){
				
				return;
			}
			
			if ( current_piece_pickler != null ){

				progressive_active	= active;
		
				if ( progressive_active ){
					
					buffer_provider.activate( current_piece_pickler );
					
					boost_provider.activate( current_piece_pickler );
					
				}else{
					
					buffer_provider.deactivate( current_piece_pickler );
					
					boost_provider.deactivate( current_piece_pickler );
				}
			}
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
		long	now = SystemTime.getCurrentTime();
		
		if ( 	now > last_eta_time &&
				now - last_eta_time < 1000 ){
			
			return( last_eta_result );
		}
		
		long	dl_rate = download_manager.getStats().getDataReceiveRate();
		
		long	result	= Long.MAX_VALUE;
				
		DiskManager	disk_manager = download_manager.getDiskManager();
		
		if ( dl_rate > 0 && content_stream_bps > 0 && disk_manager != null ){
				
			PiecePicker	picker = current_piece_pickler;
			
			if ( picker != null ){
			
				List	providers = picker.getRTAProviders();
				
				long	max_cp	= 0;
				long	max_bp	= 0;
				
				for (int i=0;i<providers.size();i++){
					
					PieceRTAProvider	provider = (PieceRTAProvider)providers.get(i);
					
					long	cp = provider.getCurrentPosition();
					
					if ( cp >= max_cp ){
						
						max_cp	= cp;
						max_bp	= provider.getBlockingPosition();
					}
				}
				
					// max-cp 	= current streaming position
					// max-bp	= blocking position (i.e. first missing data after max-cp)
					
				long	secs_pos			= max_cp/content_stream_bps;
				
				long	secs_to_watch 		= ( disk_manager.getTotalLength() - max_cp )/ content_stream_bps;
				
				long	secs_to_download	= disk_manager.getRemainingExcludingDND() / dl_rate;
				
				result = secs_to_download - secs_to_watch;
				
				//System.out.println( "Stream readyness: watch=" + secs_to_watch + " (pos=" + secs_pos + "), dl=" + secs_to_download + ",wait=" + result );
			}
		}
		
		last_eta_result	= result;
		last_eta_time	= now;
		
		return( result );
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
				
				for (int i=0;i<initial_buffer_pieces;i++){
					
					piece_rtas[i] = now+i;
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
    		DiskManager	dm = download_manager.getDiskManager();

    		if ( dm != null ){

    			DiskManagerPiece[]	pieces = dm.getPieces();
    			
    			boolean	all_done = true;
    			
    			for (int i=0;i<initial_buffer_pieces;i++){
    				
    				if ( !pieces[i].isDone()){
    						
    					all_done = false;
    					
    					break;
    				}
    			}
    			
    			if ( all_done ){
    				
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
    	getBlockingPosition()
    	{
    		DiskManager	dm = download_manager.getDiskManager();
    		
    		if ( dm == null ){
    			
    			return( 0 );
    		}
    		
    		DiskManagerPiece[]	pieces = dm.getPieces();
    		
    		for (int i=0;i<pieces.length;i++){
    			
    			DiskManagerPiece	piece = pieces[i];
    			
    			if ( !piece.isDone()){
    				
    				long	complete = i*dm.getPieceLength();
    				
    				boolean[] written = piece.getWritten();
    				
    				if ( written == null ){
    					
    					complete += piece.getLength();
    					
    				}else{
    					
    					for (int j=0;j<written.length;j++){
    						
    						if ( written[j] ){
    							
    							complete += piece.getBlockSize( j );
    						}
    						
    						break;
    					}
    				}
    				
    				return( complete );
    			}
    		}
    		
    		return( dm.getTotalLength());
    	}
	}
	
	protected class
	boostETAProvider
		implements PiecePriorityProvider
	{
		private long[]		piece_priorities;
		
		private long		last_recalc;
		
		protected void
		activate(	
			PiecePicker		picker )
		{
			synchronized( EnhancedDownloadManager.this ){
				
				picker.addPriorityProvider( this );
			}
		}
		
		protected void
		deactivate(
			PiecePicker		picker )
		{
			synchronized( EnhancedDownloadManager.this ){
									
				picker.removePriorityProvider( this );
				
				piece_priorities	= null;
			}
		}
		
		public long[]
    	updatePriorities(
    		PiecePicker		picker )
    	{
			long	now = SystemTime.getCurrentTime();
			
			if ( now < last_recalc || now - last_recalc > 5000 ){
				
				last_recalc	= now;
				
				long	stream_delay = getProgressivePlayETA();
				
				DiskManager	disk_manager = download_manager.getDiskManager();

				if ( stream_delay <= 0 || disk_manager == null ){
					
					piece_priorities = null;
					
				}else{
					
					long	dl_rate = download_manager.getStats().getDataReceiveRate();

					if ( dl_rate > 0 && content_stream_bps > 0 ){
							
							// boost assumes streaming from start
						
						long	secs_to_watch 		= disk_manager.getTotalLength()/ content_stream_bps;
						
						long	secs_to_download	= disk_manager.getRemainingExcludingDND() / dl_rate;
						
						long 	delay = secs_to_download - secs_to_watch;
						
						if ( delay <= 0 ){
							
							piece_priorities = null;
							
						}else{
							
							long	bytes_to_boost = delay * content_stream_bps;
							
							long	pieces_to_boost = (bytes_to_boost + disk_manager.getPieceLength()-1)/ disk_manager.getPieceLength();
							
							int	num_pieces = picker.getNumberOfPieces();

							if ( pieces_to_boost >= num_pieces ){
								
									// no point in boosting entire thing
								
								// System.out.println("not boosting, too many pieces" );
								
								piece_priorities	= null;
								
							}else{
								
								// System.out.println( "boosting " + pieces_to_boost );
								
								piece_priorities = new long[num_pieces];

								for (int i=0;i<pieces_to_boost;i++){
									
									piece_priorities[i] = 20000;
								}
							}
						}
					}else{
						
						piece_priorities = null;
					}
				}
			}
    		
    		return( piece_priorities );
    	}
	}
}
