/*
 * File    : PRUDPPacketReceiverImpl.java
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

package org.gudy.azureus2.core3.tracker.protocol.udp.impl;

/**
 * @author parg
 *
 */

import java.util.*;
import java.io.*;
import java.net.*;

import org.gudy.azureus2.core3.tracker.protocol.udp.*;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.*;

public class 
PRUDPPacketHandlerImpl
	implements PRUDPPacketHandler
{
	public static final int RECEIVE_TIMEOUT	= 15000;
	
	protected int				port;
	protected DatagramSocket	socket;
	
	protected long		last_timeout_check;
	
	protected Map		requests = new HashMap();
	
	protected
	PRUDPPacketHandlerImpl(
		int		_port )
	{
		port		= _port;
		
		final Semaphore init_sem = new Semaphore();
		
		Thread t = new Thread( "PRUDPPacketReciever:" + port )
			{
				public void
				run()
				{
					receiveLoop(init_sem);
				}
			};
		
		t.setDaemon(true);
		
		t.start();
		
		init_sem.reserve();
	}
	
	protected void
	receiveLoop(
		Semaphore	init_sem )
	{
		try{
			String bind_ip = COConfigurationManager.getStringParameter("Bind IP", "");
			
			InetSocketAddress	address;
			
			if ( bind_ip.length() == 0 ){
				
				address = new InetSocketAddress("127.0.0.1",port);
				
				socket = new DatagramSocket( port );
				
			}else{
				
				address = new InetSocketAddress(InetAddress.getByName(bind_ip), port);
				
				socket = new DatagramSocket( address );		
			}
					
			socket.setReuseAddress(true);
			
			socket.setSoTimeout( RECEIVE_TIMEOUT );
			
			init_sem.release();
			
			LGLogger.log( "PRUDPPacketReceiver: receiver established on port " + port ); 
	
			byte[] buffer = new byte[PRUDPPacket.MAX_PACKET_SIZE];
			
			while(true){
				
				try{
						
					DatagramPacket packet = new DatagramPacket( buffer, buffer.length, address );
					
					socket.receive( packet );
					
					process( packet );
				
				}catch( SocketTimeoutException e ){
										
				}catch( Throwable e ){
										
					e.printStackTrace();
					
				}finally{
					
					checkTimeouts();
				}
			}
		}catch( Throwable e ){
			
			LGLogger.log( "PRUDPPacketReceiver: DatagramSocket bind failed on port " + port, e ); 
		}
	}
	
	protected void
	checkTimeouts()
	{
		long	now = System.currentTimeMillis();
		
		if ( now - last_timeout_check >= RECEIVE_TIMEOUT ){
			
			last_timeout_check	= now;
			
			synchronized( requests ){
				
				Iterator it = requests.values().iterator();
				
				while( it.hasNext()){
					
					PRUDPPacketHandlerRequest	request = (PRUDPPacketHandlerRequest)it.next();
					
					if ( now - request.getCreateTime() >= RECEIVE_TIMEOUT ){
					
						LGLogger.log( LGLogger.ERROR, "PRUDPPacketHandler: request timeout" ); 
						
						request.setException(new PRUDPPacketHandlerException("timed out"));
					}
				}
			}
		}
		
	}
	protected void
	process(
		DatagramPacket	packet )
	
		throws IOException
	{
		byte[]	packet_data = packet.getData();
	
		
		PRUDPPacket reply = 
			PRUDPPacketReply.deserialiseReply( 
				new DataInputStream(new ByteArrayInputStream( packet_data, 0, packet.getLength())));

		LGLogger.log( "PRUDPPacketHandler: reply packet received: " + reply.getString()); 
				
		synchronized( requests ){
			
			PRUDPPacketHandlerRequest	request = (PRUDPPacketHandlerRequest)requests.get(new Integer(reply.getTransactionId()));
		
			if ( request == null ){
			
				LGLogger.log( LGLogger.ERROR, "PRUDPPacketReceiver: unmatched reply received, discarding:" + reply.getString());
			
			}else{
			
				request.setReply( reply );
			}
		}
	}
	
	public PRUDPPacket
	sendAndReceive(
		PRUDPPacket			request_packet,
		InetSocketAddress	destination_address )
	
		throws PRUDPPacketHandlerException
	{
		try{
			ByteArrayOutputStream	baos = new ByteArrayOutputStream();
			
			DataOutputStream os = new DataOutputStream( baos );
					
			request_packet.serialise(os);
			
			byte[]	buffer = baos.toByteArray();
			
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, destination_address );
			
			PRUDPPacketHandlerRequest	request = new PRUDPPacketHandlerRequest();
		
			synchronized( requests ){
					
				requests.put( new Integer( request_packet.getTransactionId()), request );
			}
			
			LGLogger.log( "PRUDPPacketHandler: request packet sent: " + request_packet.getString()); 
			
			try{
				socket.send( packet );
			
				return( request.getReply());
				
			}finally{
				
				synchronized( requests ){
					
					requests.remove( new Integer( request_packet.getTransactionId()));
				}
			}
		}catch( PRUDPPacketHandlerException e ){
			
			throw( e );
			
		}catch( Throwable e ){
			
			LGLogger.log( "PRUDPPacketHandler: sendAndReceive failed", e ); 
			
			throw( new PRUDPPacketHandlerException( "PRUDPPacketHandler:sendAndReceive failed", e ));
		}
	}
}
