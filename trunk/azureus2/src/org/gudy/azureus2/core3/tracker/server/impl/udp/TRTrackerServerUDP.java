/*
 * File    : TRTrackerServerUDP.java
 * Created : 19-Jan-2004
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

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.tracker.server.*;
import org.gudy.azureus2.core3.tracker.server.impl.*;
import org.gudy.azureus2.core3.tracker.protocol.udp.*;

public class 
TRTrackerServerUDP
	extends 	TRTrackerServerImpl
{
	protected static final int THREAD_POOL_SIZE				= 10;

	protected ThreadPool	thread_pool;
	
	protected int		port;
	
	public
	TRTrackerServerUDP(
		String	_name,
		int		_port )
	{
		super( _name );
		
		port		= _port;
		
		thread_pool = new ThreadPool( "TrackerServer:UDP:"+port, THREAD_POOL_SIZE );
		
		try{
			String bind_ip = COConfigurationManager.getStringParameter("Bind IP", "");
			
			InetSocketAddress	address;
			
			DatagramSocket	socket;
			
			if ( bind_ip.length() == 0 ){
				
				address = new InetSocketAddress(InetAddress.getByName("127.0.0.1"),port);
				
				socket = new DatagramSocket( port );
				
			}else{
				
				address = new InetSocketAddress(InetAddress.getByName(bind_ip), port);

				socket = new DatagramSocket(address);
			}
			
			socket.setReuseAddress(true);
			
			final DatagramSocket	f_socket 	= socket;
			final InetSocketAddress	f_address	= address;
			
			Thread recv_thread = 
				new AEThread("TRTrackerServerUDP:recv.loop")
				{
					public void
					run()
					{
						recvLoop( f_socket, f_address );
					}
				};
			
			recv_thread.setDaemon( true );
			
			recv_thread.start();									
			
			LGLogger.log( "TRTrackerServerUDP: recv established on port " + port ); 
			
		}catch( Throwable e ){
			
			LGLogger.log( "TRTrackerServerUDP: DatagramSocket bind failed on port " + port, e ); 
		}
	}
	
	protected void
	recvLoop(
		DatagramSocket		socket,
		InetSocketAddress	address )
	{		
		while(true){
			
			try{				
				byte[] buf = new byte[PRUDPPacket.MAX_PACKET_SIZE];
								
				DatagramPacket packet = new DatagramPacket( buf, buf.length, address );
				
				socket.receive( packet );
				
				String	ip = packet.getAddress().getHostAddress();
								
				if ( !ip_filter.isInRange( ip, "Tracker" )){
										
					thread_pool.run( new TRTrackerServerProcessorUDP( this, socket, packet ));
				}					
				
			}catch( Throwable e ){
				
				e.printStackTrace();		
			}
		}
	}
	
	public int
	getPort()
	{
		return( port );
	}
	
	public String
	getHost()
	{
		return( COConfigurationManager.getStringParameter( "Tracker IP", "" ));
	}
	
	public boolean
	isSSL()
	{
		return( false );
	}
	
	public void
	addRequestListener(
		TRTrackerServerRequestListener	l )
	{
	}
	
	public void
	removeRequestListener(
		TRTrackerServerRequestListener	l )
	{
	}
}
