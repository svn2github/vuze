/*
 * Created on 12-Jun-2005
 * Created by Paul Gardner
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.dht.transport.udp.impl.packethandler;

import java.net.InetSocketAddress;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.dht.transport.udp.impl.DHTUDPPacketReply;
import com.aelitis.azureus.core.dht.transport.udp.impl.DHTUDPPacketRequest;
import com.aelitis.net.udp.*;


public class 
DHTUDPPacketHandler 

{
	private int			network;
	
	private PRUDPPacketHandler		packet_handler;
	private DHTUDPRequestHandler	request_handler;
	
	private DHTUDPPacketHandlerStats	stats;
	
	protected AEMonitor		this_mon = new AEMonitor( "DHTUDPPacketHandler" );
	
	protected
	DHTUDPPacketHandler( 
		int						_network,
		PRUDPPacketHandler		_packet_handler,
		DHTUDPRequestHandler	_request_handler )
	{
		network			= _network;
		packet_handler	= _packet_handler;
		request_handler	= _request_handler;
		
		stats = new DHTUDPPacketHandlerStats( packet_handler );
	}
	
	protected DHTUDPRequestHandler
	getRequestHandler()
	{
		return( request_handler );
	}
	
	public void
	sendAndReceive(
		DHTUDPPacketRequest					request_packet,
		InetSocketAddress					destination_address,
		final DHTUDPPacketReceiver			receiver,
		long								timeout,
		boolean								low_priority )
	
		throws DHTUDPPacketHandlerException
	{
		try{
			request_packet.setNetwork( network );
			
			packet_handler.sendAndReceive( 
				request_packet, 
				destination_address, 
				new PRUDPPacketReceiver()
				{
					public void
					packetReceived(
						PRUDPPacket			packet,
						InetSocketAddress	from_address )
					{
						DHTUDPPacketReply	reply = (DHTUDPPacketReply)packet;
						
						if ( reply.getNetwork() == network ){
							
							receiver.packetReceived(reply, from_address );
							
						}else{
							
							Debug.out( "Non-matching network reply received" );
						}
					}
		
					public void
					error(
						PRUDPPacketHandlerException	e )
					{
						receiver.error( new DHTUDPPacketHandlerException( e ));
					}
				}, 
				timeout, 
				low_priority );
			
		}catch( PRUDPPacketHandlerException e ){
			
			throw( new DHTUDPPacketHandlerException(e ));
		}
	}
	
	public void
	send(
		DHTUDPPacketRequest			request_packet,
		InetSocketAddress			destination_address )
	
		throws DHTUDPPacketHandlerException

	{
		try{
			request_packet.setNetwork( network );
			
			packet_handler.send( request_packet, destination_address );
			
		}catch( PRUDPPacketHandlerException e ){
			
			throw( new DHTUDPPacketHandlerException( e ));
		}
	}
	
	public void
	send(
		DHTUDPPacketReply			reply_packet,
		InetSocketAddress			destination_address )
	
		throws DHTUDPPacketHandlerException
	{
		try{
			reply_packet.setNetwork( network );
			
			packet_handler.send( reply_packet, destination_address );
			
		}catch( PRUDPPacketHandlerException e ){
			
			throw( new DHTUDPPacketHandlerException( e ));
		}
	}
	
	public void
	setDelays(
		int		send_delay,
		int		receive_delay,
		int		queued_request_timeout )
	{
			// TODO: hmm
		
		packet_handler.setDelays( send_delay, receive_delay, queued_request_timeout );
	}
	
	public DHTUDPPacketHandlerStats
	getStats()
	{
		return( stats );
	}
}
