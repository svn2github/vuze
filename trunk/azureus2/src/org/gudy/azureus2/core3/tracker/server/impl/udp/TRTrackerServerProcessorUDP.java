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
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.tracker.server.*;
import org.gudy.azureus2.core3.tracker.server.impl.*;

import org.gudy.azureus2.core3.tracker.protocol.udp.*;

public class 
TRTrackerServerProcessorUDP 
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
		System.out.println( "UDPProcessor: packet length = " + packet.getLength() + ", address = " + packet.getAddress() + ", port = " + packet.getPort());
		
		DataInputStream is = new DataInputStream(new ByteArrayInputStream(packet.getData()));
		
		try{
			PRUDPPacketRequest	request = PRUDPPacketRequest.deserialiseRequest( is );
			
			System.out.println( "UDPRequest:" + request.getString());
						
			InetAddress address = packet.getAddress();
			
			ByteArrayOutputStream	baos = new ByteArrayOutputStream();
			
			DataOutputStream os = new DataOutputStream( baos );
			
			long	conn_id = 232323;
			
			PRUDPPacket data_packet = new PRUDPPacketReplyConnect(request.getTransactionId(), conn_id );
			
			data_packet.serialise(os);
			
			byte[]	buffer = baos.toByteArray();
			
			DatagramPacket reply_packet = new DatagramPacket(buffer, buffer.length,address,packet.getPort());
			
			System.out.println( "sending packet");
			
			socket.send( reply_packet );
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
}
