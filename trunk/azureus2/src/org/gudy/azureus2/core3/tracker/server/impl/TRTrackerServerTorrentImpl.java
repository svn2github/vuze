/*
 * File    : TRTrackerServerTorrent.java
 * Created : 26-Oct-2003
 * By      : stuff
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.core3.tracker.server.impl;

/**
 * @author parg
 *
 */

import java.util.*;
import java.io.*;

import org.gudy.azureus2.core3.tracker.server.*;
import org.gudy.azureus2.core3.util.*;

public class 
TRTrackerServerTorrentImpl 
	implements TRTrackerServerTorrent
{
	protected TRTrackerServerImpl	server;
	protected HashWrapper			hash;

	protected Map				peer_map 		= new HashMap();
	protected Map				peer_reuse_map	= new HashMap();
	
	protected Random			random		= new Random( System.currentTimeMillis());
	
	protected TRTrackerServerTorrentStatsImpl	stats;
		
	protected
	TRTrackerServerTorrentImpl(
		TRTrackerServerImpl		_server,
		HashWrapper				_hash )
	{
		server		= _server;
		hash		= _hash;
		
		stats		= new TRTrackerServerTorrentStatsImpl( this );
	}
	
	
	public synchronized void
	peerContact(
		String		event,
		String		peer_id,
		int			port,
		String		ip_address,
		long		uploaded,
		long		downloaded,
		long		left,
		int			numwant,
		long		interval_requested )
	{
		boolean	stopped 	= event != null && event.equalsIgnoreCase("stopped");
		boolean	completed 	= event != null && event.equalsIgnoreCase("completed");
		
		TRTrackerServerPeerImpl	peer = (TRTrackerServerPeerImpl)peer_map.get( peer_id );

		String	reuse_key = ip_address + ":" +port;
		
		if ( peer == null ){
			
			// check to see if this peer already has an entry against this torrent
			// and if so delete it (assumption is that the client has quit and
			// restarted with new peer id
			
			//System.out.println( "new peer" );
			
			
			TRTrackerServerPeerImpl old_peer	= (TRTrackerServerPeerImpl)peer_reuse_map.get( reuse_key );
			
			if ( old_peer != null ){
				
				peer_reuse_map.remove( reuse_key );
				
				// System.out.println( "removing dead client '" + old_peer.getString());
				
				try{
					peer_map.remove( new String( old_peer.getPeerId(), Constants.BYTE_ENCODING ));
					
				}catch( UnsupportedEncodingException e ){
					
				}
			}
			
			if ( !stopped ){			
				
				try{
					
					byte[]	peer_bytes = peer_id.getBytes( Constants.BYTE_ENCODING );
					
					peer = new TRTrackerServerPeerImpl( peer_bytes, ip_address.getBytes(), port );
					
					peer_map.put( peer_id, peer );
					
					peer_reuse_map.put( reuse_key, peer );
					
				}catch( UnsupportedEncodingException e){
					
					e.printStackTrace();
				}
			}
		}else{
			
			if ( stopped ){
				
				peer_map.remove( peer_id );
				
				peer_reuse_map.remove( reuse_key );
			}
		}
		
		if ( peer != null ){
			
			peer.setTimeout( System.currentTimeMillis() + ( interval_requested * 1000 * TRTrackerServerImpl.CLIENT_TIMEOUT_MULTIPLIER ));
			
			peer.setStats( uploaded, downloaded, left, numwant );
		}
		
		stats.addAnnounce();
		
		if ( completed ){
			
			stats.addCompleted();
		}
	}
	
	public synchronized void
	exportPeersToMap(
		Map		map,
		int		num_want )
	{
		int		max_peers	= TRTrackerServerImpl.getMaxPeersToSend();
		
			// num_want < 0 -> not supplied to give them max
		
		if ( num_want < 0 ){
			
			num_want = peer_map.size();
		}
		
			// trim back to max_peers if specified
		
		if ( max_peers > 0 && num_want > max_peers ){
			
			num_want	= max_peers;
		}
	
		// System.out.println( "exportPeersToMap: num_want = " + num_want + ", max = " + max_peers );
		
		long	now = System.currentTimeMillis();
		
		List	rep_peers = new ArrayList();
			
		map.put( "peers", rep_peers );
		
		boolean	send_peer_ids = TRTrackerServerImpl.getSendPeerIds();
		
			// if they want them all simply give them the set
		
		if ( num_want == 0 ){
			
			return;
			
		}else if ( num_want >= peer_map.size()){
	
				// if they want them all simply give them the set
			
			Iterator	it = peer_map.values().iterator();
					
			while(it.hasNext()){
		
				TRTrackerServerPeerImpl	peer = (TRTrackerServerPeerImpl)it.next();
								
				if ( now > peer.getTimeout()){
								
					// System.out.println( "removing timed out client '" + peer.getString());
									
					it.remove();
														
				}else{
									
					Map rep_peer = new HashMap();
		
					rep_peers.add( rep_peer );
											
					if ( send_peer_ids ){
						
						rep_peer.put( "peer id", peer.getPeerId() );
					}
					
					rep_peer.put( "ip", peer.getIPAsRead() );
					rep_peer.put( "port", new Long( peer.getPort()));
				}
			}
		}else{
				// randomly select the peers to return
			
			LinkedList	peers = new LinkedList( peer_map.keySet());
			
			int	added = 0;
			
			while( added < num_want && peers.size() > 0 ){
				
				String	key = (String)peers.remove(random.nextInt(peers.size()));
								
				TRTrackerServerPeerImpl	peer = (TRTrackerServerPeerImpl)peer_map.get(key);
				
				if ( now > peer.getTimeout()){
					
					// System.out.println( "removing timed out client '" + peer.getString());
					
					peer_map.remove( key );
										
				}else{
					
					added++;
					
					Map rep_peer = new HashMap();
					
					rep_peers.add( rep_peer );
					
					if ( send_peer_ids ){
						
						rep_peer.put( "peer id", peer.getPeerId() );
					}
					
					rep_peer.put( "ip", peer.getIPAsRead() );
					rep_peer.put( "port", new Long( peer.getPort()));
				}
			}
		}
	}
	
	public synchronized void
	checkTimeouts()
	{
		long	now = System.currentTimeMillis();
		
		Iterator	it = peer_map.values().iterator();
				
		while(it.hasNext()){
			
			TRTrackerServerPeerImpl	peer = (TRTrackerServerPeerImpl)it.next();
			
			if ( now > peer.getTimeout()){
				
				// System.out.println( "removing timed out client '" + peer.getString());
				
				it.remove();
			}
		}
	}
	
	protected TRTrackerServerTorrentStats
	getStats()
	{
		return( stats );
	}
	
	public synchronized TRTrackerServerPeer[]
	getPeers()
	{
		TRTrackerServerPeer[]	res = new TRTrackerServerPeer[peer_map.size()];
		
		peer_map.values().toArray( res );
		
		return( res );
	}
	
	public HashWrapper
	getHash()
	{
		return( hash );
	}
}
