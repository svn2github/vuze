/*
 * Created on 22 Jun 2006
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.networkmanager.impl.udp;

import java.util.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;

import org.gudy.azureus2.core3.config.COConfigurationListener;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.net.udp.uc.PRUDPPacketHandler;
import com.aelitis.net.udp.uc.PRUDPPacketHandlerFactory;
import com.aelitis.net.udp.uc.PRUDPPrimordialHandler;

public class 
NetworkGlueLoopBack
	implements NetworkGlue, PRUDPPrimordialHandler
 
{
	private NetworkGlueListener		listener;

	private PRUDPPacketHandler handler;
	
	private List	message_queue	= new ArrayList();
	
	protected
	NetworkGlueLoopBack(
		NetworkGlueListener		_listener,
		int						_udp_port )
	{
		listener	= _listener;
				
		handler = PRUDPPacketHandlerFactory.getHandler( _udp_port );

		handler.setPrimordialHandler( this );
		
		COConfigurationManager.addListener(
			new COConfigurationListener()
			{
				public void
				configurationSaved()
				{
					int	port = UDPNetworkManager.getSingleton().getUDPListeningPortNumber();
					
					if ( port != handler.getPort()){
						
						handler = PRUDPPacketHandlerFactory.getHandler( port );

						handler.setPrimordialHandler( NetworkGlueLoopBack.this );

					}
				}
			});
				
		new AEThread( "NetworkGlueLoopBack", true )
		{
			public void
			runSupport()
			{
				while( true ){
					
					try{
						Thread.sleep(1);
						
					}catch( Throwable e ){
						
					}
				
					InetSocketAddress	local_address 	= null;
					InetSocketAddress	source_address 	= null;
					byte[]				data			= null;
					
					synchronized( message_queue ){
						
						if ( message_queue.size() > 0 ){
							
							Object[]	entry = (Object[])message_queue.remove(0);
							
							source_address	= (InetSocketAddress)entry[0];
							local_address 	= (InetSocketAddress)entry[1];
							data			= (byte[])entry[2];
						}
					}
					
					if ( source_address != null ){
						
						listener.receive( local_address.getPort(), source_address, data );
					}
				}
			}
		}.start();
	}
	
	public boolean
	packetReceived(
		DatagramPacket	packet )
	{
		boolean	tracker_protocol = true;
		
		if ( packet.getLength() >= 12 ){
								
			byte[]	data = packet.getData();
			
				// mask: 0xfffff800
			
			if ( 	( data[0] & 0xff ) == 0 &&
					( data[1] & 0xff ) == 0 &&
					( data[2] & 0xf8 ) == 0 &&
			
					( data[8] & 0xff ) == 0 &&
					( data[9] & 0xff ) == 0 &&
					( data[10]& 0xf8 ) == 0 ){
				
				tracker_protocol	= false;
			}
		}
		
		
		return( !tracker_protocol );
	}
	
	public int
	send(
		int					local_port,
		InetSocketAddress	target,
		byte[]				data )
	
		throws IOException
	{	
		InetSocketAddress local_address = new InetSocketAddress( target.getAddress(), local_port );
		
		synchronized( message_queue ){
			
			message_queue.add( new Object[]{ local_address, target, data });
		}
		
		return( data.length );
	}
}
