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

import java.io.IOException;
import java.util.*;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.dht.transport.udp.impl.DHTUDPPacketRequest;
import com.aelitis.net.udp.*;

public class 
DHTUDPPacketHandlerFactory 
{
	private static DHTUDPPacketHandlerFactory	singleton = new DHTUDPPacketHandlerFactory();
		
	private Map 			port_map = new HashMap();
	
	protected AEMonitor	this_mon = new AEMonitor("DHTUDPPacketHandlerFactory" );

	
	
	public static DHTUDPPacketHandler 
	getHandler(
		int						network,
		int						port,
		DHTUDPRequestHandler	request_handler )
	
		throws DHTUDPPacketHandlerException
	{
		return( singleton.getHandlerSupport( network, port, request_handler ));
	}
	
	protected DHTUDPPacketHandler 
	getHandlerSupport(
		int						network,
		int						port,
		DHTUDPRequestHandler	request_handler )
	
		throws DHTUDPPacketHandlerException
	{
		try{
			this_mon.enter();
			
			Object[]	port_details = (Object[])port_map.get( new Integer( port ));
			
			if ( port_details == null ){
				
				PRUDPPacketHandler  packet_handler = 
					PRUDPPacketHandlerFactory.getHandler( 
							port, 
							new DHTUDPPacketNetworkHandler( this, port ));
							
				
				port_details = new Object[]{ packet_handler, new HashMap()};
				
				port_map.put( new Integer( port ), port_details );
			}
			
			Map					network_map 	= (Map)port_details[1];
			
			DHTUDPPacketHandler	ph = (DHTUDPPacketHandler)network_map.get( new Integer( network ));
			
			if ( ph != null ){
				
				throw( new DHTUDPPacketHandlerException( "Network already added" ));
			}
			
			ph = new DHTUDPPacketHandler( network, (PRUDPPacketHandler)port_details[0], request_handler );
			
			network_map.put( new Integer( network ), ph );
			
			return( ph );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	process(
		int					port,
		DHTUDPPacketRequest	request )
	{
		try{
			getRequestHandler( port, request.getNetwork()).process( request );
			
		}catch( IOException e ){
			
			Debug.printStackTrace( e );
		}
	}
	
	protected DHTUDPRequestHandler
	getRequestHandler(
		int		port,
		int		network )
	
		throws IOException
	{
		Object[]	port_details = (Object[])port_map.get( new Integer( port ));

		if ( port_details == null ){
			
			throw( new IOException( "Port '" + port + "' not registered" ));
		}
		
		DHTUDPPacketHandler	res = (DHTUDPPacketHandler)((Map)port_details[1]).get( new Integer( network ));
		
		if ( res == null ){
			
			throw( new IOException( "Network '" + network + "' not registered" ));
		}
		
		return( res.getRequestHandler());
	}
}
