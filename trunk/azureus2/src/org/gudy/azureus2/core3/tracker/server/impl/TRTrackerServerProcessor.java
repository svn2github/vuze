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
import org.gudy.azureus2.core3.tracker.util.TRTrackerUtils;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.util.*;

public abstract class 
TRTrackerServerProcessor 
	extends ThreadPoolTask
{
	protected TRTrackerServerImpl		server;
	
	protected TRTrackerServerTorrentImpl
	processTrackerRequest(
		TRTrackerServerImpl			_server,
		Map[]						root_out,		// output
		TRTrackerServerPeerImpl[]	peer_out,		// output
		int							request_type,
		byte[]						hash,
		HashWrapper					peer_id,
		boolean						no_peer_id,
		boolean						compact,
		String						key,
		String						event,
		int							port,
		String						client_ip_address,
		long						downloaded,
		long						uploaded,
		long						left,
		int							num_want )
	
		throws Exception
	{
		server	= _server;
				
			// translate any 127.0.0.1 local addresses back to the tracker address
		
		client_ip_address = TRTrackerUtils.adjustHostFromHosting( client_ip_address );
		
		boolean	loopback	= client_ip_address.equals( TRTrackerUtils.getTrackerIP());
		
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
				
				long	interval = server.getAnnounceRetryInterval( torrent );
				
				TRTrackerServerPeerImpl peer = 
					torrent.peerContact( 	
						event, peer_id, port, client_ip_address, loopback, key,
						uploaded, downloaded, left,
						interval );
				
					// set num_want to 0 for stopped events as no point in returning peers
				
				boolean	stopped 	= event != null && event.equalsIgnoreCase("stopped");
				
				root_out[0] = torrent.exportAnnounceToMap( peer, left > 0, stopped?0:num_want, interval, no_peer_id, compact );
				
				peer_out[0]	= peer;				
			}else{
				
				Map	files = new ByteEncodedKeyHashMap();
				
					// we don't cache local scrapes as if we do this causes the hosting of
					// torrents to retrieve old values initially. Not a fatal error but not
					// the best behaviour as the (local) seed isn't initially visible.
				
				boolean	local_scrape = client_ip_address.equals( "127.0.0.1" );
								
				Map	hash_entry = torrent.exportScrapeToMap( !local_scrape );
				
				byte[]	torrent_hash = torrent.getHash().getHash();
				
				String	str_hash = new String( torrent_hash,Constants.BYTE_ENCODING );
				
				// System.out.println( "tracker - encoding: " + ByteFormatter.nicePrint(torrent_hash) + " -> " + ByteFormatter.nicePrint( str_hash.getBytes( Constants.BYTE_ENCODING )));
				
				files.put( str_hash, hash_entry );
				
				Map	root = new HashMap();
				
				root_out[0] = root;
				
				addScrapeInterval( torrent, root );
				
				root.put( "files", files );
			}
		}else{
			
			Map	files = new ByteEncodedKeyHashMap();
						
			TRTrackerServerTorrentImpl[] torrents = server.getTorrents();
			
			for (int i=0;i<torrents.length;i++){
				
				TRTrackerServerTorrentImpl	this_torrent = torrents[i];
								
				byte[]	torrent_hash = this_torrent.getHash().getHash();
				
				String	str_hash = new String( torrent_hash,Constants.BYTE_ENCODING );
				
				// System.out.println( "tracker - encoding: " + ByteFormatter.nicePrint(torrent_hash) + " -> " + ByteFormatter.nicePrint( str_hash.getBytes( Constants.BYTE_ENCODING )));
				
				Map	hash_entry = this_torrent.exportScrapeToMap( true );
				
				files.put( str_hash, hash_entry );
			}
	
			Map	root = new HashMap();
			
			root_out[0] = root;
			
			addScrapeInterval( null, root );
			
			root.put( "files", files );
		}
		
		return( torrent );
	}
	
	protected void
	addScrapeInterval(
		TRTrackerServerTorrentImpl	torrent,
		Map							root )
	{
		long interval = server.getScrapeRetryInterval( torrent );
		
		if ( interval > 0 ){
			
			Map	flags = new HashMap();
			
			flags.put("min_request_interval", new Long(interval));
			
			root.put( "flags", flags );
		}
	}
}
