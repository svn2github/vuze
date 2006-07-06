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
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.net.udp.uc.PRUDPPacketHandler;
import com.aelitis.net.udp.uc.PRUDPPacketHandlerException;
import com.aelitis.net.udp.uc.PRUDPPacketHandlerFactory;
import com.aelitis.net.udp.uc.PRUDPPrimordialHandler;

public class 
NetworkGlueUDP
	implements NetworkGlue, PRUDPPrimordialHandler
 
{
	private NetworkGlueListener		listener;

	private PRUDPPacketHandler handler;
	
	private LinkedList	msg_queue	= new LinkedList();
	private AESemaphore	msg_queue_sem		= new AESemaphore( "NetworkGlueUDP" );
	private AESemaphore	msg_queue_slot_sem	= new AESemaphore( "NetworkGlueUDP", 128 );
	

	protected
	NetworkGlueUDP(
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

						handler.setPrimordialHandler( NetworkGlueUDP.this );

					}
				}
			});
				
		new AEThread( "NetworkGlueUDP", true )
		{
			public void
			runSupport()
			{
				while( true ){
					
			
					InetSocketAddress	target_address 	= null;
					byte[]				data			= null;
					
					msg_queue_sem.reserve();
						
					synchronized( msg_queue ){
												
						Object[]	entry = (Object[])msg_queue.removeFirst();
								
						target_address 	= (InetSocketAddress)entry[0];
						data			= (byte[])entry[1];
					}
					
					msg_queue_slot_sem.release();
					
					try{
						handler.primordialSend( data, target_address );
						
						Thread.sleep(3);
						
					}catch( Throwable e ){
						
						Debug.printStackTrace( e );
					}
				}
			}
		}.start();
	}
	
	public boolean
	packetReceived(
		DatagramPacket	packet )
	{
		if ( packet.getLength() >= 12 ){
								
			byte[]	data = packet.getData();
			
				// first or third word must have something set in mask: 0xfffff800
			
			if ( 	(	( data[0] & 0xff ) != 0 ||
						( data[1] & 0xff ) != 0 ||
						( data[2] & 0xf8 ) != 0 ) &&
					
					(	( data[8] & 0xff ) != 0 ||
						( data[9] & 0xff ) != 0 ||
						( data[10]& 0xf8 ) != 0 )){
				
				return( listener.receive( handler.getPort(), new InetSocketAddress( packet.getAddress(), packet.getPort()), packet.getData(), packet.getLength()));
			}
		}
		
		return( false );
	}
	
	public int
	send(
		int					local_port,
		InetSocketAddress	target,
		byte[]				data )
	
		throws IOException
	{	
		msg_queue_slot_sem.reserve();
		
		synchronized( msg_queue ){
			
			msg_queue.add( new Object[]{ target, data });
		}
		
		msg_queue_sem.release();
		
		return( data.length );
	}
}
