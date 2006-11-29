/*
 * Created on 1 Nov 2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package com.aelitis.azureus.core.networkmanager.admin.impl;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;

import org.gudy.azureus2.core3.util.Constants;



import com.aelitis.azureus.core.versioncheck.VersionCheckClient;
import com.aelitis.net.udp.uc.PRUDPPacketHandler;
import com.aelitis.net.udp.uc.PRUDPPacketHandlerFactory;
import com.aelitis.net.udp.uc.PRUDPReleasablePacketHandler;

public class 
NetworkAdminUDPTester 
	implements NetworkAdminProtocolTester
{
	public static final String 	UDP_SERVER_ADDRESS	= Constants.NAT_TEST_SERVER;
	public static final int		UDP_SERVER_PORT		= 2081;
	
	public InetAddress
	testOutbound(
		InetAddress		bind_ip,
		int				bind_port )
	
		throws Exception
	{
		return( VersionCheckClient.getSingleton().getExternalIpAddressUDP(bind_ip, bind_port));
	}
	
	public InetAddress
	testInbound(			
		InetAddress		bind_ip,
		int				bind_port )
	
		throws Exception
	{
		  PRUDPReleasablePacketHandler handler = PRUDPPacketHandlerFactory.getReleasableHandler( bind_port );
	  	  
		  PRUDPPacketHandler	packet_handler = handler.getHandler();
		  
		  long timeout = 20000;
		  		  
		  HashMap	data_to_send = new HashMap();
		  
		  Random 	random = new Random();
		  
		  try{
			  packet_handler.setExplicitBindAddress( bind_ip );	  
			  
			  for (int i=0;i<3;i++){
				  
				  	// connection ids for requests must always have their msb set...
				  	// apart from the original darn udp tracker spec....
				  
				  long connection_id = 0x8000000000000000L | random.nextLong();

				  NetworkAdminNATUDPRequest	request_packet = new NetworkAdminNATUDPRequest( connection_id );
				  
				  request_packet.setPayload( data_to_send );
				  
				  NetworkAdminNATUDPReply reply_packet = (NetworkAdminNATUDPReply)packet_handler.sendAndReceive( null, request_packet, new InetSocketAddress( UDP_SERVER_ADDRESS, UDP_SERVER_PORT ), timeout );
		
				  Map	reply = reply_packet.getPayload();
				  
				  System.out.println( "reply: " + reply );
			  }
			  
			  throw( new Exception( "Timeout" ));
			  
		  }finally{
			 
			  packet_handler.setExplicitBindAddress( null );

			  handler.release();
		  }
	}
}
