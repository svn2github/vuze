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

import org.gudy.azureus2.core3.util.*;

public class 
TRTrackerServerTorrent 
{
	protected TRTrackerServerImpl	server;
	protected HashWrapper			hash;

	protected Map				peer_map = new HashMap();
		
	protected
	TRTrackerServerTorrent(
		TRTrackerServerImpl	_server,
		HashWrapper			_hash )
	{
		server		= _server;
		hash		= _hash;
	}
	
	
	protected void
	peerContact(
		String		peer_id,
		int			port,
		String		ip_address )
	{
		TRTrackerServerPeer	peer = (TRTrackerServerPeer)peer_map.get( peer_id );
		
		if ( peer == null ){
							
			Iterator	it = peer_map.values().iterator();
							
			while (it.hasNext()){
							
				TRTrackerServerPeer this_peer = (TRTrackerServerPeer)it.next();
															
				if (	this_peer.getPort() == port &&
						new String(this_peer.getIP()).equals( ip_address )){
									
					System.out.println( "removing dead client '" + peer.getString());
									
					it.remove();
				}
			}
							
			peer = new TRTrackerServerPeer( peer_id.getBytes(), ip_address.getBytes(), port );
							
			peer_map.put( peer_id, peer );
		}
		
		peer.setLastContactTime( System.currentTimeMillis());
	}
	
	protected void
	exportPeersToMap(
		Map		map )
	{
		long	now = System.currentTimeMillis();
		
		List	rep_peers = new ArrayList();
			
		map.put( "peers", rep_peers );
	
		Iterator	it = peer_map.values().iterator();
					
		while(it.hasNext()){
	
			TRTrackerServerPeer	peer = (TRTrackerServerPeer)it.next();
							
			if ( (now - peer.getLastContactTime()) > server.getRetryInterval()*1000*2 ){
							
				System.out.println( "removing timedout client '" + peer.getString());
								
				it.remove();
								
			}else{
								
				Map rep_peer = new HashMap();
	
				rep_peers.add( rep_peer );
														 
				rep_peer.put( "peer id", peer.getPeerId() );
				rep_peer.put( "ip", peer.getIP() );
				rep_peer.put( "port", new Long( peer.getPort()));
			}
		}
	}
}
