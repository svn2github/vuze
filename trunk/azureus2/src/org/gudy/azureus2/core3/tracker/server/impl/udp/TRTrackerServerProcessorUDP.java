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
import java.util.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.tracker.server.*;
import org.gudy.azureus2.core3.tracker.server.impl.*;

import org.gudy.azureus2.core3.tracker.protocol.udp.*;

public class 
TRTrackerServerProcessorUDP
	extends		TRTrackerServerProcessor
	implements 	Runnable
{
	protected TRTrackerServerUDP		server;
	protected DatagramSocket			socket;
	protected DatagramPacket			packet;
	
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
					
					reply = handleConnect( request );
					
				}else if (type == PRUDPPacket.ACT_REQUEST_ANNOUNCE ){
					
					reply = handleAnnounceAndScrape( client_ip_address, request, TRTrackerServerRequest.RT_ANNOUNCE );
					
				}else if ( type == PRUDPPacket.ACT_REQUEST_SCRAPE ){
					
					reply = handleAnnounceAndScrape( client_ip_address, request, TRTrackerServerRequest.RT_SCRAPE );
					
				}else{
					
					reply = new PRUDPPacketReplyError( request.getTransactionId(), "unsupported action");
				}
			}catch( Throwable e ){
				
				String	error = e.getMessage();
				
				if ( error == null ){
					
					error = e.toString();
				}
				
				reply = new PRUDPPacketReplyError( request.getTransactionId(), error );
			}
			
			InetAddress address = packet.getAddress();
			
			ByteArrayOutputStream	baos = new ByteArrayOutputStream();
			
			DataOutputStream os = new DataOutputStream( baos );
									
			reply.serialise(os);
			
			byte[]	buffer = baos.toByteArray();
			
			DatagramPacket reply_packet = new DatagramPacket(buffer, buffer.length,address,packet.getPort());
						
			socket.send( reply_packet );
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	protected PRUDPPacket
	handleConnect(
		PRUDPPacket		request )
	{
		long	conn_id = 232323; // TODO:
		
		PRUDPPacket reply = new PRUDPPacketReplyConnect(request.getTransactionId(), conn_id );
		
		return( reply );
	}
	
	protected PRUDPPacket
	handleAnnounceAndScrape(
		String			client_ip_address,
		PRUDPPacket		request,
		int				request_type )
	
		throws Exception
	{
		Map	root = new HashMap();
		
		byte[]		hash_bytes	= null;
		String		peer_id		= null;
		int			port		= 0;
		String		event		= null;
		
		long		uploaded		= 0;
		long		downloaded		= 0;
		long		left			= 0;
		int			num_peers		= 0;
		
		if ( request_type == TRTrackerServerRequest.RT_ANNOUNCE ){
			
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
			
			int	i_ip = announce.getIPAddress();
			
			if ( i_ip != 0 ){
				
				client_ip_address = PRUDPPacket.intToAddress( i_ip );
			}
		}else{
			
			// TODO:
		}
		
		processTrackerRequest( 
				server, root,
				request_type,
				hash_bytes,
				peer_id,
				event,
				port,
				client_ip_address,
				downloaded, uploaded, left,
				num_peers );
		
		PRUDPPacketReplyAnnounce reply = new PRUDPPacketReplyAnnounce(request.getTransactionId());
		
		reply.setInterval(((Long)root.get("interval")).intValue());
		
		List	peers = (List)root.get("peers");
		
		int[]	addresses 	= new int[peers.size()];
		short[]	ports		= new short[addresses.length];
		
		for (int i=0;i<addresses.length;i++){
			
			Map	peer = (Map)peers.get(i);
			
			addresses[i] 	= PRUDPPacket.addressToInt(new String((byte[])peer.get("ip")));
			
			ports[i]		= (short)((Long)peer.get("port")).shortValue();
		}
		
		reply.setPeers( addresses, ports );
		
		return( reply );
	}
}
