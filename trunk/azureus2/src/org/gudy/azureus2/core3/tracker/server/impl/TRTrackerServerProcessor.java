/*
 * File    : TRTrackerServerProcessor.java
 * Created : 20-Jan-2004
 * By      : parg
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

import org.gudy.azureus2.core3.tracker.server.*;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.util.*;

public class 
TRTrackerServerProcessor 
{
	protected TRTrackerServerTorrentImpl
	processTrackerRequest(
		TRTrackerServerImpl		server,
		Map						root,
		int						request_type,
		byte[]					hash,
		String					peer_id,
		String					event,
		int						port,
		String					client_ip_address,
		long					downloaded,
		long					uploaded,
		long					left,
		int						num_peers,
		int						num_want )
	
		throws Exception
	{
		TRTrackerServerTorrentImpl	torrent = null;
		
		if ( request_type != TRTrackerServerRequest.RT_FULL_SCRAPE ){
			
			if ( hash == null ){
				
				throw( new Exception( "Hash missing from request "));
			}
						
			// System.out.println( "TRTrackerServerProcessor::request:" + request_type + ",event:" + event + " - " + client_ip_address + ":" + port );
			
			// System.out.println( "    hash = " + ByteFormatter.nicePrint(hash));
			
			torrent = server.getTorrent( hash );
			
			if ( torrent == null ){
				
				if ( !COConfigurationManager.getBooleanParameter( "Tracker Public Enable", false )){
					
					throw( new Exception( "Torrent unauthorised "));
					
				}else{
					
					try{
						
						server.permit( hash, false );
						
						torrent = server.getTorrent( hash );
						
					}catch( Throwable e ){
						
						throw( new Exception( "Torrent unauthorised "));								
					}
				}
			}
			
			if ( request_type == TRTrackerServerRequest.RT_ANNOUNCE ){
				
				if ( peer_id == null ){
					
					throw( new Exception( "peer_id missing from request"));
				}
				
				long	interval = server.getRetryInterval();
				
				torrent.peerContact( 	
						event, peer_id, port, client_ip_address,
						uploaded, downloaded, left, num_peers,
						interval );
				
				torrent.exportPeersToMap( root, num_want );
				
				root.put( "interval", new Long( interval ));
				
			}else{
				
				
				Map	files = new ByteEncodedKeyHashMap();
				
				Map	hash_entry = new HashMap();
				
				byte[]	torrent_hash = torrent.getHash().getHash();
				
				String	str_hash = new String( torrent_hash,Constants.BYTE_ENCODING );
				
				// System.out.println( "tracker - encoding: " + ByteFormatter.nicePrint(torrent_hash) + " -> " + ByteFormatter.nicePrint( str_hash.getBytes( Constants.BYTE_ENCODING )));
				
				files.put( str_hash, hash_entry );
				
				TRTrackerServerPeer[]	peers = torrent.getPeers();
				
				long	seeds 		= 0;
				long	non_seeds	= 0;
				
				for (int i=0;i<peers.length;i++){
					
					if ( peers[i].getAmountLeft() == 0 ){
						
						seeds++;
					}else{
						non_seeds++;
					}
				}
				
				hash_entry.put( "complete", new Long( seeds ));
				hash_entry.put( "incomplete", new Long( non_seeds ));
				
				root.put( "files", files );
			}
		}else{
			
			Map	files = new ByteEncodedKeyHashMap();
			
			Map	hash_entry = new HashMap();
			
			TRTrackerServerTorrentImpl[] torrents = server.getTorrents();
			
			for (int i=0;i<torrents.length;i++){
				
				TRTrackerServerTorrentImpl	this_torrent = torrents[i];
				
				byte[]	torrent_hash = this_torrent.getHash().getHash();
				
				String	str_hash = new String( torrent_hash,Constants.BYTE_ENCODING );
				
				// System.out.println( "tracker - encoding: " + ByteFormatter.nicePrint(torrent_hash) + " -> " + ByteFormatter.nicePrint( str_hash.getBytes( Constants.BYTE_ENCODING )));
				
				files.put( str_hash, hash_entry );
				
				TRTrackerServerPeer[]	peers = this_torrent.getPeers();
				
				long	seeds 		= 0;
				long	non_seeds	= 0;
				
				for (int j=0;j<peers.length;j++){
					
					if ( peers[j].getAmountLeft() == 0 ){
						
						seeds++;
					}else{
						non_seeds++;
					}
				}
				
				hash_entry.put( "complete", new Long( seeds ));
				hash_entry.put( "incomplete", new Long( non_seeds ));
			}
			
			root.put( "files", files );
		}
		
		return( torrent );
	}
}
