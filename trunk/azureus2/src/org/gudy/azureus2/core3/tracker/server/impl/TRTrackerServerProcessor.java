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

import java.io.UnsupportedEncodingException;
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
		String						request,
		Map[]						root_out,		// output
		TRTrackerServerPeerImpl[]	peer_out,		// output
		int							request_type,
		byte[][]					hashes,
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
	
		throws TRTrackerServerException
	{
		server	= _server;
				
			// translate any 127.0.0.1 local addresses back to the tracker address
		
		client_ip_address = TRTrackerUtils.adjustHostFromHosting( client_ip_address );
		
		if ( !TRTrackerServerImpl.getAllNetworksSupported()){
		
			String	network = AENetworkClassifier.categoriseAddress( client_ip_address );
			
			String[]	permitted_networks = TRTrackerServerImpl.getPermittedNetworks();
			
			boolean ok = false;
			
			for (int i=0;i<permitted_networks.length;i++){
				
				if ( network == permitted_networks[i] ){
					
					ok = true;
					
					break;
				}
			}
			
			if ( !ok ){
				
				throw( new TRTrackerServerException( "Network '" + network + "' not supported" ));
			}
		}
		
		boolean	loopback	= client_ip_address.equals( TRTrackerUtils.getTrackerIP());
		
		TRTrackerServerTorrentImpl	torrent = null;
		
		if ( request_type != TRTrackerServerRequest.RT_FULL_SCRAPE ){
			
			if ( hashes == null || hashes.length == 0 ){
				
				throw( new TRTrackerServerException( "Hash missing from request "));
			}
						
			// System.out.println( "TRTrackerServerProcessor::request:" + request_type + ",event:" + event + " - " + client_ip_address + ":" + port );
			
			// System.out.println( "    hash = " + ByteFormatter.nicePrint(hash));
			
			if ( request_type == TRTrackerServerRequest.RT_ANNOUNCE ){
				
				if ( hashes.length != 1 ){
					
					throw( new TRTrackerServerException( "Too many hashes for announce"));
				}
				
				byte[]	hash = hashes[0];
				
				torrent = server.getTorrent( hash );
				
				if ( torrent == null ){
					
					if ( !COConfigurationManager.getBooleanParameter( "Tracker Public Enable", false )){
						
						throw( new TRTrackerServerException( "Torrent unauthorised" ));
						
					}else{
						
						try{
							
							torrent = (TRTrackerServerTorrentImpl)server.permit( hash, false );
													
						}catch( Throwable e ){
							
							throw( new TRTrackerServerException( "Torrent unauthorised", e ));								
						}
					}
				}
				
				if ( peer_id == null ){
					
					throw( new TRTrackerServerException( "peer_id missing from request"));
				}
				
				long	interval = server.getAnnounceRetryInterval( torrent );
				
				TRTrackerServerPeerImpl peer = 
					torrent.peerContact( 	
						event, peer_id, port, client_ip_address, loopback, key,
						uploaded, downloaded, left,
						interval );
				
				HashMap	pre_map = new HashMap();
				
				server.preProcess( peer, torrent, request_type, request, pre_map );
				
					// set num_want to 0 for stopped events as no point in returning peers
				
				boolean	stopped 	= event != null && event.equalsIgnoreCase("stopped");
				
				root_out[0] = torrent.exportAnnounceToMap( pre_map, peer, left > 0, stopped?0:num_want, interval, server.getMinAnnounceRetryInterval(), no_peer_id, compact );
				
				peer_out[0]	= peer;	
				
			}else{
				
				boolean	local_scrape = client_ip_address.equals( "127.0.0.1" );
				
				long	max_interval	= server.getMinScrapeRetryInterval();
				
				Map	root = new HashMap();
				
				root_out[0] = root;
				
				Map	files = new ByteEncodedKeyHashMap();
				
				root.put( "files", files );

				for (int i=0;i<hashes.length;i++){
					
					byte[]	hash = hashes[i];
					
					String	str_hash;
					
					try{
						str_hash = new String( hash, Constants.BYTE_ENCODING );

							// skip duplicates
						
						if ( i > 0 && files.get( str_hash ) != null ){
							
							continue;
						}
						
					}catch( UnsupportedEncodingException e ){
						
						continue;
					}
					
					torrent = server.getTorrent( hash );
					
					if ( torrent == null ){
						
						if ( !COConfigurationManager.getBooleanParameter( "Tracker Public Enable", false )){
							
							continue;
							
						}else{
							
							try{							
								torrent = (TRTrackerServerTorrentImpl)server.permit( hash, false );
									
							}catch( Throwable e ){
								
								continue;							
							}
						}
					}
					
					long	interval = server.getScrapeRetryInterval( torrent );				
				
					if ( interval > max_interval ){
						
						max_interval	= interval;
					}
					
					// we don't cache local scrapes as if we do this causes the hosting of
					// torrents to retrieve old values initially. Not a fatal error but not
					// the best behaviour as the (local) seed isn't initially visible.
				
					Map	hash_entry = torrent.exportScrapeToMap( !local_scrape );
										
						// System.out.println( "tracker - encoding: " + ByteFormatter.nicePrint(torrent_hash) + " -> " + ByteFormatter.nicePrint( str_hash.getBytes( Constants.BYTE_ENCODING )));
					
					files.put( str_hash, hash_entry );
				}
				
				if ( hashes.length > 1 ){
					
					torrent	= null;	// no specific torrent
				}
				
				// System.out.println( "scrape: hashes = " + hashes.length + ", files = " + files.size() + ", tim = " + max_interval );
				
				addScrapeInterval( max_interval, root );
			}
		}else{
			
			Map	files = new ByteEncodedKeyHashMap();
						
			TRTrackerServerTorrentImpl[] torrents = server.getTorrents();
			
			for (int i=0;i<torrents.length;i++){
				
				TRTrackerServerTorrentImpl	this_torrent = torrents[i];
								
				byte[]	torrent_hash = this_torrent.getHash().getHash();
				
				try{
					String	str_hash = new String( torrent_hash,Constants.BYTE_ENCODING );
					
					// System.out.println( "tracker - encoding: " + ByteFormatter.nicePrint(torrent_hash) + " -> " + ByteFormatter.nicePrint( str_hash.getBytes( Constants.BYTE_ENCODING )));
					
					Map	hash_entry = this_torrent.exportScrapeToMap( true );
					
					files.put( str_hash, hash_entry );
					
				}catch( UnsupportedEncodingException e ){
			
					throw( new TRTrackerServerException( "Encoding error", e ));

				}
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

		addScrapeInterval( interval, root );
	}
	
	protected void
	addScrapeInterval(
		long		interval,
		Map			root )
	{
		if ( interval > 0 ){
			
			Map	flags = new HashMap();
			
			flags.put("min_request_interval", new Long(interval));
			
			root.put( "flags", flags );
		}
	}
}
