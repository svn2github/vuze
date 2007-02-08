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

import com.aelitis.azureus.core.dht.netcoords.DHTNetworkPosition;

public abstract class 
TRTrackerServerProcessor 
	extends ThreadPoolTask
{
	private static final boolean QUEUE_TEST	= false;
	
	static{
		if ( QUEUE_TEST ){
			System.out.println( "**** TRTrackerServerProcessor::QUEUE_TEST ****" );
		}
	}
	
	private TRTrackerServerImpl		server;
	
	protected TRTrackerServerTorrentImpl
	processTrackerRequest(
		TRTrackerServerImpl			_server,
		String						request,
		Map[]						root_out,		// output
		TRTrackerServerPeerImpl[]	peer_out,		// output
		int							request_type,
		byte[][]					hashes,
		String						link,
		String						scrape_flags,
		HashWrapper					peer_id,
		boolean						no_peer_id,
		byte						compact_mode,
		String						key,
		String						event,
		boolean						stop_to_queue,
		int							port,
		int							udp_port,
		int							http_port,
		String						real_ip_address,
		String						client_ip_address,
		long						downloaded,
		long						uploaded,
		long						left,
		int							num_want,
		byte						crypto_level,
		byte						az_ver,
		int							up_speed,
		DHTNetworkPosition			network_position )
	
		throws TRTrackerServerException
	{
		server	= _server;
			
		if ( !server.isReady()){
			
			throw( new TRTrackerServerException( "Tracker initialising, please wait" ));
		}
		
		boolean	ip_override = real_ip_address != client_ip_address;
		
		boolean	loopback	= TRTrackerUtils.isLoopback( real_ip_address );
		
		if ( loopback ){
					
				// any override is purely for routing purposes for loopback connections and we don't
				// want to apply the ip-override precedence rules against us 
						
			ip_override	= false;
		}
		
			// translate any 127.0.0.1 local addresses back to the tracker address. Note this
			// fixes up .i2p and onion addresses back to their real values when needed
		
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
				
		TRTrackerServerTorrentImpl	torrent = null;
		
		if ( request_type != TRTrackerServerRequest.RT_FULL_SCRAPE ){
									
			// System.out.println( "TRTrackerServerProcessor::request:" + request_type + ",event:" + event + " - " + client_ip_address + ":" + port );
			
			// System.out.println( "    hash = " + ByteFormatter.nicePrint(hash));
			
			if ( request_type == TRTrackerServerRequest.RT_ANNOUNCE ){
				
				if ( hashes == null || hashes.length == 0 ){
					
					throw( new TRTrackerServerException( "Hash missing from request "));
				}

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
						request,
						event, 
						peer_id, port, udp_port, http_port, crypto_level, az_ver,
						client_ip_address, ip_override, loopback, key,
						uploaded, downloaded, left,
						interval,
						up_speed, network_position );
				
				if ( stop_to_queue && ( QUEUE_TEST || !( loopback || ip_override ))){
					
					torrent.peerQueued( client_ip_address, port, udp_port, http_port, crypto_level, az_ver, (int)server.getScrapeRetryInterval( torrent ), left==0);
				}
				
				HashMap	pre_map = new HashMap();
				
				TRTrackerServerPeer	pre_process_peer = peer;
				
				if ( pre_process_peer == null ){
					
						// can be null for stop events received without a previous start
					
					pre_process_peer = new lightweightPeer(client_ip_address,port,peer_id);
				}
				
				server.preProcess( pre_process_peer, torrent, request_type, request, pre_map );
				
					// set num_want to 0 for stopped events as no point in returning peers
				
				boolean	stopped 	= event != null && event.equalsIgnoreCase("stopped");
				
				root_out[0] = torrent.exportAnnounceToMap( pre_map, peer, left > 0, stopped?0:num_want, interval, server.getMinAnnounceRetryInterval(), no_peer_id, compact_mode, crypto_level, network_position );
				
				peer_out[0]	= peer;	
				
			}else if ( request_type == TRTrackerServerRequest.RT_QUERY ){
				
				if ( link == null ){
					
					if ( hashes == null || hashes.length == 0 ){
						
						throw( new TRTrackerServerException( "Hash missing from request "));
					}
					
					if ( hashes.length != 1 ){
						
						throw( new TRTrackerServerException( "Too many hashes for query"));
					}
				
					byte[]	hash = hashes[0];
				
					torrent = server.getTorrent( hash );
					
				}else{
					
					torrent = server.getTorrent( link );
				}
				
				if ( torrent == null ){
					
					throw( new TRTrackerServerException( "Torrent unauthorised" ));
				}
				
				long	interval = server.getAnnounceRetryInterval( torrent );

				root_out[0] = torrent.exportAnnounceToMap( new HashMap(), null, true, num_want, interval, server.getMinAnnounceRetryInterval(), true, compact_mode, crypto_level, network_position );

			}else{
				
				if ( hashes == null || hashes.length == 0 ){
					
					throw( new TRTrackerServerException( "Hash missing from request "));
				}

				boolean	local_scrape = client_ip_address.equals( "127.0.0.1" );
				
				long	max_interval	= server.getMinScrapeRetryInterval();
				
				Map	root = new HashMap();
				
				root_out[0] = root;
				
				Map	files = new ByteEncodedKeyHashMap();
				
				root.put( "files", files );
				
				char[]	scrape_chars = scrape_flags==null?null:scrape_flags.toCharArray();
				
				if ( scrape_chars != null && scrape_chars.length != hashes.length ){
					
					scrape_chars	= null;
				}
				
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

					if ( scrape_chars != null && ( QUEUE_TEST || !( loopback || ip_override ))){ 
						
							// note, 'Q' is complete+queued so we set seed true below
						
						if ( scrape_chars[i] == 'Q' ){
							
							torrent.peerQueued(  client_ip_address, port, udp_port, http_port, crypto_level, az_ver, (int)interval, true );
						}
					}
					
					if ( torrent.getRedirects() != null ){
						
						if ( hashes.length > 1 ){
							
								// just drop this from the set. this will cause the client to revert
								// to single-hash scrapes and subsequently pick up the redirect
							
							continue;							
						}
					}
					
					server.preProcess( new lightweightPeer(client_ip_address,port,peer_id), torrent, request_type, request, null );

					// we don't cache local scrapes as if we do this causes the hosting of
					// torrents to retrieve old values initially. Not a fatal error but not
					// the best behaviour as the (local) seed isn't initially visible.
				
					Map	hash_entry = torrent.exportScrapeToMap( request, client_ip_address, !local_scrape );
										
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
			
			
			if ( !TRTrackerServerImpl.isFullScrapeEnabled()){
				
				throw( new TRTrackerServerException( "Full scrape disabled" ));
			}
			
			Map	files = new ByteEncodedKeyHashMap();
				
			TRTrackerServerTorrentImpl[] torrents = server.getTorrents();
			
			for (int i=0;i<torrents.length;i++){
				
				TRTrackerServerTorrentImpl	this_torrent = torrents[i];
					
				if ( this_torrent.getRedirects() != null ){
					
						// not visible to a full-scrape
					
					continue;
				}
				
				server.preProcess( new lightweightPeer(client_ip_address,port,peer_id), this_torrent, request_type, request, null );

				byte[]	torrent_hash = this_torrent.getHash().getHash();
				
				try{
					String	str_hash = new String( torrent_hash,Constants.BYTE_ENCODING );
					
					// System.out.println( "tracker - encoding: " + ByteFormatter.nicePrint(torrent_hash) + " -> " + ByteFormatter.nicePrint( str_hash.getBytes( Constants.BYTE_ENCODING )));
					
					Map	hash_entry = this_torrent.exportScrapeToMap( request, client_ip_address, true );
					
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
	
	protected static class
	lightweightPeer
		implements TRTrackerServerPeer
	{
		private String	ip;
		private int		port;
		private byte[]	peer_id;
		
		public
		lightweightPeer(
			String		_ip,
			int			_port,
			HashWrapper	_peer_id )
		{
			ip		= _ip;
			port	= _port;
			peer_id	= _peer_id==null?null:_peer_id.getBytes();
		}
		
		public long
		getUploaded()
		{
			return( -1 );
		}
		
		public long
		getDownloaded()
		{
			return( -1 );
		}
		
		public long
		getAmountLeft()
		{
			return( -1 );
		}
		
		public String
		getIP()
		{
			return( ip );
		}
		
		public String
		getIPRaw()
		{
			return( ip );
		}
	
		public byte
		getNATStatus()
		{
			return( NAT_CHECK_UNKNOWN );
		}
		
		public int
		getTCPPort()
		{
			return( port );
		}
		
		public byte[]
		getPeerID()
		{
			return( peer_id );
		}
		
		public boolean
		isBiased()
		{
			return( false );
		}
		
		public void
		setBiased(
			boolean		biased )
		{	
		}
		
		public void
		setUserData(
			Object		key,
			Object		data )
		{
		}
		
		public Object
		getUserData(
			Object		key )
		{
			return( null );
		}
	}
}
