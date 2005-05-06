/*
 * File    : PRUDPPacketReceiver.java
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

package com.aelitis.net.udp;

/**
 * @author parg
 *
 */

import java.net.*;

public interface 
PRUDPPacketHandler 
{
		/**
		 * Asynchronous send and receive
		 * @param request_packet
		 * @param destination_address
		 * @param receiver
		 * @throws PRUDPPacketHandlerException
		 */
	
	public void
	sendAndReceive(
		PRUDPPacket					request_packet,
		InetSocketAddress			destination_address,
		PRUDPPacketReceiver			receiver,
		long						timeout,
		boolean						low_priority )
	
		throws PRUDPPacketHandlerException;
	
		/**
		 * Synchronous send and receive
		 * @param auth
		 * @param request_packet
		 * @param destination_address
		 * @return
		 * @throws PRUDPPacketHandlerException
		 */
	
	public PRUDPPacket
	sendAndReceive(
		PasswordAuthentication		auth,
		PRUDPPacket					request_packet,
		InetSocketAddress			destination_address )
	
		throws PRUDPPacketHandlerException;
	
		/**
		 * Send only
		 * @param request_packet
		 * @param destination_address
		 * @throws PRUDPPacketHandlerException
		 */
	
	public void
	send(
		PRUDPPacket					request_packet,
		InetSocketAddress			destination_address )
	
		throws PRUDPPacketHandlerException;
	
	public PRUDPRequestHandler
	getRequestHandler();
	
	public void
	setDelays(
		int		send_delay,
		int		receive_delay,
		int		queued_request_timeout );
	
	public PRUDPPacketHandlerStats
	getStats();
}
