/*
 * Created on 21-Jan-2005
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.dht.transport.udp.impl;

import com.aelitis.azureus.core.dht.transport.*;
import com.aelitis.azureus.core.dht.transport.udp.*;

/**
 * @author parg
 *
 */

public class 
Test 
	implements DHTTransportRequestHandler
{
	public static void
	main(
		String[]	args )
	{
		new Test();
	}
	
	protected
	Test()
	{
		try{
			DHTTransport	udp1 = DHTTransportFactory.createUDP(6881, 5, 3, 5000, com.aelitis.azureus.core.dht.impl.Test.logger);
		
			udp1.setRequestHandler( this );
			
			DHTTransport	udp2 = DHTTransportFactory.createUDP(6882, 5, 3, 5000, com.aelitis.azureus.core.dht.impl.Test.logger);
		
			udp2.setRequestHandler( this );

			final DHTTransportUDPContact	c1 = (DHTTransportUDPContact)udp1.getLocalContact();
			
			for (int i=0;i<10;i++){
				
				final int f_i = i;
				
				c1.sendPing(
					new DHTTransportReplyHandlerAdapter()
					{
						public void
						pingReply(
							DHTTransportContact contact )
						{
							System.out.println( "ping reply: " + f_i );
						}
						
						public void
						failed(
							DHTTransportContact 	contact )
						{
							System.out.println( "ping failed" );
						}
					});
			}
	
			c1.sendStore(
					new DHTTransportReplyHandlerAdapter()
					{
						public void
						storeReply(
							DHTTransportContact contact )
						{
							System.out.println( "store reply" );
						}
						
						public void
						failed(
							DHTTransportContact 	contact )
						{
							System.out.println( "store failed" );
						}
					},
					new byte[23],
					new DHTTransportValue()
					{
						public int
						getCacheDistance()
						{
							return( 1 );
						}
						
						public long
						getCreationTime()
						{
							return( 2 );
						}
						
						public byte[]
						getValue()
						{
							return( "sdsd".getBytes());
						}
						
						public DHTTransportContact
						getOriginator()
						{
							return( c1 );
						}
						
						public int
						getFlags()
						{
							return(0);
						}
						
						public String
						getString()
						{
							return( new String(getValue()));
						}
					});
			
			c1.sendFindNode(
					new DHTTransportReplyHandlerAdapter()
					{
						public void
						findNodeReply(
							DHTTransportContact 	contact,
							DHTTransportContact[] 	contacts )
						{
							System.out.println( "findNode reply" );
						}
						
						public void
						failed(
							DHTTransportContact 	contact )
						{
							System.out.println( "findNode failed" );
						}
					},
					new byte[12]);
			
			System.out.println( "sending find value" );
			
			c1.sendFindValue(
					new DHTTransportReplyHandlerAdapter()
					{
						public void
						findValueReply(
							DHTTransportContact 	contact,
							DHTTransportContact[] 	contacts )
						{
							System.out.println( "findValue contacts reply" );
						}
						
						public void
						findValueReply(
							DHTTransportContact 	contact,
							DHTTransportValue 		value )
						{
							System.out.println( "findValue value reply" );
						}
						
						public void
						failed(
							DHTTransportContact 	contact )
						{
							System.out.println( "findValue failed" );
						}
					},
					new byte[3]);
			
			System.out.println( "sending complete" );

			Thread.sleep(1000000);
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	public void
	pingRequest(
		DHTTransportContact contact )
	{
		System.out.println( "TransportHandler: ping" );
	}
		
	public void
	storeRequest(
		DHTTransportContact contact, 
		byte[]				key,
		DHTTransportValue	value )
	{
		System.out.println( "TransportHandler: store" );
	}
	
	public DHTTransportContact[]
	findNodeRequest(
		DHTTransportContact contact, 
		byte[]				id )
	{
		System.out.println( "TransportHandler: findNode" );
		
		return( new DHTTransportContact[]{ contact } );
	}
	
	public Object
	findValueRequest(
		DHTTransportContact contact, 
		byte[]				key )
	{
		System.out.println( "TransportHandler: findValue" );
		
		return( new DHTTransportContact[]{ contact } );
	}

	public void
	contactImported(
		DHTTransportContact	contact )
	{
		
	}
	
	public DHTTransportFullStats
	statsRequest(
		DHTTransportContact	contact )
	{
		return( null );
	}
}
