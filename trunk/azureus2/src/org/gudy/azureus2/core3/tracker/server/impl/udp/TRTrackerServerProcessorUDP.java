/*
 * File    : TRTrackerServerProcessorUDP.java
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

package org.gudy.azureus2.core3.tracker.server.impl.udp;

/**
 * @author parg
 *
 */

import java.net.*;
import java.io.*;
import java.security.SecureRandom;
import java.util.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.tracker.server.*;
import org.gudy.azureus2.core3.tracker.server.impl.*;

import org.gudy.azureus2.core3.tracker.protocol.*;
import org.gudy.azureus2.core3.tracker.protocol.udp.*;

public class 
TRTrackerServerProcessorUDP
	extends		TRTrackerServerProcessor
	implements 	Runnable
{
		// client may connect + then retry announce up to 4 times -> * 6
	
	public static final long CONNECTION_ID_LIFETIME	= PRUDPPacket.DEFAULT_UDP_TIMEOUT*6;
	
	protected TRTrackerServerUDP		server;
	protected DatagramSocket			socket;
	protected DatagramPacket			packet;
	
	protected static Map				connection_id_map 	= new LinkedHashMap();
	protected static SecureRandom		random				= new SecureRandom();
	
	protected
	TRTrackerServerProcessorUDP(
		TRTrackerServerUDP		_server,
		DatagramSocket			_socket,
		DatagramPacket			_packet )
	{
		server	= _server;
		socket	= _socket;
		packet	= _packet;
	}
	
	public void
	run()
	{		
		DataInputStream is = new DataInputStream(new ByteArrayInputStream(packet.getData(), 0, packet.getLength()));
		
		try{
			String	client_ip_address = packet.getAddress().getHostAddress();
			
			PRUDPPacketRequest	request = PRUDPPacketRequest.deserialiseRequest( is );
			
			LGLogger.log( "TRTrackerServerProcessorUDP: packet received: " + request.getString()); 
			
			PRUDPPacket	reply;
			
			try{
				int	type = request.getAction();
				
				if ( type == PRUDPPacket.ACT_REQUEST_CONNECT ){
					
					reply = handleConnect( client_ip_address, request );
					
				}else if (type == PRUDPPacket.ACT_REQUEST_ANNOUNCE ){
					
					reply = handleAnnounceAndScrape( client_ip_address, request, TRTrackerServerRequest.RT_ANNOUNCE );
					
				}else if ( type == PRUDPPacket.ACT_REQUEST_SCRAPE ){
					
					reply = handleAnnounceAndScrape( client_ip_address, request, TRTrackerServerRequest.RT_SCRAPE );
					
				}else{
					
					reply = new PRUDPPacketReplyError( request.getTransactionId(), "unsupported action");
				}
			}catch( Throwable e ){
				
				e.printStackTrace();
				
				String	error = e.getMessage();
				
				if ( error == null ){
					
					error = e.toString();
				}
				
				reply = new PRUDPPacketReplyError( request.getTransactionId(), error );
			}
			
			if ( reply != null ){
				
				InetAddress address = packet.getAddress();
				
				ByteArrayOutputStream	baos = new ByteArrayOutputStream();
				
				DataOutputStream os = new DataOutputStream( baos );
										
				reply.serialise(os);
				
				byte[]	buffer = baos.toByteArray();
				
				DatagramPacket reply_packet = new DatagramPacket(buffer, buffer.length,address,packet.getPort());
							
				socket.send( reply_packet );
			}
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	protected long
	allocateConnectionId(
		String	client_address )
	{
		synchronized( random ){
	
			long	id = random.nextLong();
			
			Long	new_key = new Long(id);
			
			connectionData	new_data = new connectionData( client_address );
			
				// check for timeouts
			
			Iterator	it = connection_id_map.keySet().iterator();
			
			while(it.hasNext()){
				
				Long	key = (Long)it.next();
				
				connectionData	data = (connectionData)connection_id_map.get(key);
			
				if ( new_data.getTime() - data.getTime() > CONNECTION_ID_LIFETIME ){
					
					// System.out.println( "TRTrackerServerProcessorUDP: conection id timeout" );
					
					it.remove();
					
				}else{
						// insertion order into map is time based - LinkedHashMap returns keys in same order
					
					break;
				}
			}	
						
			connection_id_map.put( new_key, new_data );
			
			// System.out.println( "TRTrackerServerProcessorUDP: allocated:" + id + ", conection id map size = " + connection_id_map.size());
			
			return( id );
		}
	}
	
	protected boolean
	checkConnectionId(
		String	client_address,
		long	id )
	{
		synchronized( random ){
			
			Long	key = new Long(id);
			
			connectionData data = (connectionData)connection_id_map.get( key );
			
			if ( data == null ){
				
				// System.out.println( "TRTrackerServerProcessorUDP: rejected:" + id + ", data not found" );
				
				return( false );
				
			}else{
				
					// single shot id, can't be reused
				
				connection_id_map.remove( key );
			}
			
			boolean	ok = data.getAddress().equals( client_address );
			
			// System.out.println( "TRTrackerServerProcessorUDP: tested:" + id + "/" + client_address + " -> " + ok );
			
			return( ok );
		}
	}
	
	protected PRUDPPacket
	handleConnect(
		String					client_ip_address,
		PRUDPPacketRequest		request )
	{
		long	conn_id = allocateConnectionId( client_ip_address );
		
		PRUDPPacket reply = new PRUDPPacketReplyConnect(request.getTransactionId(), conn_id );
		
		return( reply );
	}
	
	protected PRUDPPacket
	handleAnnounceAndScrape(
		String				client_ip_address,
		PRUDPPacketRequest	request,
		int					request_type )
	
		throws Exception
	{
		if ( !checkConnectionId( client_ip_address, request.getConnectionId())){
			
			return( null );
		}
				
		byte[]		hash_bytes	= null;
		String		peer_id		= null;
		int			port		= 0;
		String		event		= null;
		
		long		uploaded		= 0;
		long		downloaded		= 0;
		long		left			= 0;
		int			num_peers		= 0;
		int			num_want		= -1;
		
		String		key				= null;
		
		if ( request_type == TRTrackerServerRequest.RT_ANNOUNCE ){
			
			if ( PRUDPPacket.VERSION == 1 ){
				PRUDPPacketRequestAnnounce	announce = (PRUDPPacketRequestAnnounce)request;
				
				hash_bytes	= announce.getHash();
				
				peer_id		= new String( announce.getPeerId(), Constants.BYTE_ENCODING );
				
				port		= announce.getPort();
				
				int	i_event = announce.getEvent();
				
				switch( i_event ){
					case PRUDPPacketRequestAnnounce.EV_STARTED:
					{
						event = "started";
						break;
					}
					case PRUDPPacketRequestAnnounce.EV_STOPPED:
					{
						event = "stopped";
						break;
					}
					case PRUDPPacketRequestAnnounce.EV_COMPLETED:
					{
						event = "completed";
						break;
					}					
				}
				
				uploaded 	= announce.getUploaded();
				
				downloaded	= announce.getDownloaded();
				
				left		= announce.getLeft();
				
				num_want	= announce.getNumWant();
				
				int	i_ip = announce.getIPAddress();
				
				if ( i_ip != 0 ){
					
					client_ip_address = PRHelpers.intToAddress( i_ip );
				}
			}else{
				
				PRUDPPacketRequestAnnounce2	announce = (PRUDPPacketRequestAnnounce2)request;
				
				hash_bytes	= announce.getHash();
				
				peer_id		= new String( announce.getPeerId(), Constants.BYTE_ENCODING );
				
				port		= announce.getPort();
				
				int	i_event = announce.getEvent();
				
				switch( i_event ){
					case PRUDPPacketRequestAnnounce.EV_STARTED:
					{
						event = "started";
						break;
					}
					case PRUDPPacketRequestAnnounce.EV_STOPPED:
					{
						event = "stopped";
						break;
					}
					case PRUDPPacketRequestAnnounce.EV_COMPLETED:
					{
						event = "completed";
						break;
					}					
				}
				
				uploaded 	= announce.getUploaded();
				
				downloaded	= announce.getDownloaded();
				
				left		= announce.getLeft();
				
				num_want	= announce.getNumWant();
				
				int	i_ip = announce.getIPAddress();
				
				if ( i_ip != 0 ){
					
					client_ip_address = PRHelpers.intToAddress( i_ip );
				}
				
				key = "" + announce.getKey();
			}
		}else{
			
			PRUDPPacketRequestScrape	scrape = (PRUDPPacketRequestScrape)request;
			
			hash_bytes	= scrape.getHash();
		}
		
		Map[]						root_out = new Map[1];
		TRTrackerServerPeerImpl[]	peer_out = new TRTrackerServerPeerImpl[1];
		
		TRTrackerServerTorrentImpl torrent =
			processTrackerRequest( 
				server, root_out, peer_out, 
				request_type,
				hash_bytes,
				peer_id, false,	false, key, // currently no "no_peer_id" / "compact" in the packet and anyway they aren't returned / key
				event,
				port,
				client_ip_address,
				downloaded, uploaded, left,
				num_peers, num_want );
		
		Map	root = root_out[0];
		
		if ( request_type == TRTrackerServerRequest.RT_ANNOUNCE ){

			if ( PRUDPPacket.VERSION == 1 ){
				PRUDPPacketReplyAnnounce reply = new PRUDPPacketReplyAnnounce(request.getTransactionId());
				
				reply.setInterval(((Long)root.get("interval")).intValue());
				
				List	peers = (List)root.get("peers");
				
				int[]	addresses 	= new int[peers.size()];
				short[]	ports		= new short[addresses.length];
				
				for (int i=0;i<addresses.length;i++){
					
					Map	peer = (Map)peers.get(i);
					
					addresses[i] 	= PRHelpers.addressToInt(new String((byte[])peer.get("ip")));
					
					ports[i]		= (short)((Long)peer.get("port")).shortValue();
				}
				
				reply.setPeers( addresses, ports );
				
				return( reply );
			}else{
				
				PRUDPPacketReplyAnnounce2 reply = new PRUDPPacketReplyAnnounce2(request.getTransactionId());
				
				reply.setInterval(((Long)root.get("interval")).intValue());
				
				boolean	local_scrape = client_ip_address.equals( "127.0.0.1" );

				Map scrape_details = torrent.exportScrapeToMap( !local_scrape );
				
				int	seeders 	= ((Long)scrape_details.get("complete")).intValue();
				int leechers 	= ((Long)scrape_details.get("incomplete")).intValue();

				reply.setLeechersSeeders(leechers,seeders);
				
				List	peers = (List)root.get("peers");
				
				int[]	addresses 	= new int[peers.size()];
				short[]	ports		= new short[addresses.length];
				
				for (int i=0;i<addresses.length;i++){
					
					Map	peer = (Map)peers.get(i);
					
					addresses[i] 	= PRHelpers.addressToInt(new String((byte[])peer.get("ip")));
					
					ports[i]		= (short)((Long)peer.get("port")).shortValue();
				}
				
				reply.setPeers( addresses, ports );
				
				return( reply );
			}
			
		}else{
			
			if ( PRUDPPacket.VERSION == 1 ){
				
				PRUDPPacketReplyScrape reply = new PRUDPPacketReplyScrape(request.getTransactionId());
				
				/*
				Long	interval = (Long)root.get("interval");
				
				if ( interval != null ){
					
					reply.setInterval(interval.intValue());
				}
				*/
				
				Map	files = (Map)root.get( "files" );
				
				byte[][]	hashes 			= new byte[files.size()][];
				int[]		s_complete		= new int[hashes.length];
				int[]		s_downloaded	= new int[hashes.length];
				int[]		s_incomplete	= new int[hashes.length];
				
				Iterator it = files.keySet().iterator();
				
				int	pos = 0;
				
				while(it.hasNext()){
					
					String	hash_str = (String)it.next();
					
					hashes[pos] = hash_str.getBytes( Constants.BYTE_ENCODING );
									
					Map	details = (Map)files.get( hash_str );
					
					s_complete[pos] 	= ((Long)details.get("complete")).intValue();
					s_incomplete[pos] 	= ((Long)details.get("incomplete")).intValue();
					s_downloaded[pos] 	= ((Long)details.get("downloaded")).intValue();
					
					pos++;
				}
				
				reply.setDetails( hashes, s_complete, s_downloaded, s_incomplete );
				
				return( reply );
				
			}else{
				
				PRUDPPacketReplyScrape2 reply = new PRUDPPacketReplyScrape2(request.getTransactionId());
				
				/*
				Long	interval = (Long)root.get("interval");
				
				if ( interval != null ){
					
					reply.setInterval(interval.intValue());
				}
				*/
				
				Map	files = (Map)root.get( "files" );
				
				int[]		s_complete		= new int[files.size()];
				int[]		s_downloaded	= new int[s_complete.length];
				int[]		s_incomplete	= new int[s_complete.length];
				
				Iterator it = files.keySet().iterator();
				
				int	pos = 0;
				
				while(it.hasNext()){
					
					String	hash_str = (String)it.next();
														
					Map	details = (Map)files.get( hash_str );
					
					s_complete[pos] 	= ((Long)details.get("complete")).intValue();
					s_incomplete[pos] 	= ((Long)details.get("incomplete")).intValue();
					s_downloaded[pos] 	= ((Long)details.get("downloaded")).intValue();
					
					pos++;
				}
				
				reply.setDetails( s_complete, s_downloaded, s_incomplete );
				
				return( reply );				
			}
		}
	}
	
	protected static class
	connectionData
	{
		protected String		address;
		protected long			time;
		
		protected
		connectionData(
			String		_address )
		{
			address	= _address;
			time	= SystemTime.getCurrentTime();
		}
		
		protected String
		getAddress()
		{
			return( address );
		}
		
		protected long
		getTime()
		{
			return( time );
		}
	}
}
