/*
 * File    : Main.java
 * Created : 5 Oct. 2003
 * By      : Parg 
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

package org.gudy.azureus2.core3.tracker.server.test;

import java.net.*;

import org.gudy.azureus2.core3.tracker.server.TRTrackerServerFactory;
import org.gudy.azureus2.core3.tracker.protocol.udp.*;

public class 
Main 
{
	static void
	usage()
	{
		System.err.println( "Usage:" );
		
		System.exit(1);
	}
	
	public static void
	main(
		String[]	args )
	{
		int	test_type= 0;
		
		if ( args.length != 0 ){
			
			usage();
		}
		
		
		try{
			int my_port 	= 6881;
			int their_port	= 6969;
			
			InetSocketAddress address = new InetSocketAddress("127.0.0.1",their_port);
			
			TRTrackerServerFactory.create( TRTrackerServerFactory.PR_UDP, their_port );
				
			PRUDPPacketHandler handler = PRUDPPacketHandlerFactory.getHandler( my_port );
			
			for (int i=0;i<100;i++){
				
				Thread.sleep(1000);
								
				PRUDPPacket request_packet = new PRUDPPacketRequestConnect();
				 
				PRUDPPacket reply_packet = handler.sendAndReceive( request_packet, address );
				
				System.out.println( reply_packet.getString());
			}
			
			Thread.sleep(100000);
			
		}catch( Throwable e ){
			
			e.printStackTrace();
			
		}
	}
}
