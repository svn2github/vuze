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
import java.io.*;
import java.util.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.tracker.server.*;
import org.gudy.azureus2.core3.tracker.server.impl.*;

public class 
TRTrackerServerUDP
	extends 	TRTrackerServerImpl
{
	public static final int	PACKET_SIZE	= 8192;
	
	protected int		port;
	
	public
	TRTrackerServerUDP(
		int		_port )
	{
		port		= _port;
		
		try{
			DatagramSocket	socket = new DatagramSocket();
		
			String bind_ip = COConfigurationManager.getStringParameter("Bind IP", "");
			
			if ( bind_ip.length() == 0 ){
				
				socket.bind(new InetSocketAddress(port));
				
			}else{
				
				socket.bind(new InetSocketAddress(InetAddress.getByName(bind_ip), port));
			}
			
			socket.setReuseAddress(true);
			
			final DatagramSocket	f_socket = socket;
			
			Thread recv_thread = 
				new Thread("TRTrackerServerUDP:recv.loop")
				{
					public void
					run()
					{
						recvLoop( f_socket );
					}
				};
			
			recv_thread.setDaemon( true );
			
			recv_thread.start();									
			
			LGLogger.log( "TRTrackerServerUDP: listener established on port " + port ); 
			
		}catch( Throwable e ){
			
			LGLogger.log( "TRTrackerServerUDP: DatagramSocket bind failed on port " + port, e ); 
		}
	}
	
	protected void
	recvLoop(
		DatagramSocket	socket )
	{		
		while(true){
			
			try{				
				byte[] buf = new byte[PACKET_SIZE];
				
				DatagramPacket packet = new DatagramPacket( buf, buf.length );
				
				socket.receive( packet );
				
				String	ip = packet.getAddress().getHostAddress();
				
				System.out.println( "got a UDP packet: ip = " + ip);
				
				if ( !ip_filter.isInRange( ip )){
					
					System.out.println( "got a UDP packet");
					
					// thread_pool.run( new TRTrackerServerProcessorTCP( this, socket ));
				}					
				
			}catch( Throwable e ){
				
				e.printStackTrace();		
			}
		}
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
