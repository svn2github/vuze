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
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.networkmanager.ConnectionEndpoint;
import com.aelitis.azureus.core.networkmanager.Transport;
import com.aelitis.azureus.core.networkmanager.impl.IncomingConnectionManager;
import com.aelitis.azureus.core.networkmanager.impl.ProtocolDecoder;
import com.aelitis.azureus.core.networkmanager.impl.TransportCryptoManager;
import com.aelitis.azureus.core.networkmanager.impl.TransportHelperFilter;
import com.aelitis.azureus.core.networkmanager.impl.tcp.ProtocolEndpointTCP;
import com.aelitis.azureus.core.networkmanager.impl.tcp.TCPNetworkManager;
import com.aelitis.azureus.core.networkmanager.impl.tcp.TCPTransportImpl;

public class
UDPConnectionManager
	implements NetworkGlueListener
{
	private static final LogIDs LOGID = LogIDs.NET;

	private static final Map	connections = new HashMap();
	
	private int next_connection_id;

	
	private IncomingConnectionManager	incoming_manager = IncomingConnectionManager.getSingleton();

	private NetworkGlue	network_glue = new NetworkGlueLoopBack( this );
	
	private UDPSelector		selector	= new UDPSelector( this );
	
	protected
	UDPConnectionManager()
	{
	}
	
	protected UDPSelector
	getSelector()
	{
		return( selector );
	}
	
	protected void
	poll()
	{
		synchronized( connections ){

			Iterator	it = connections.values().iterator();
			
			while( it.hasNext()){
				
				((UDPConnectionSet)it.next()).poll();
			}
		}
	}
	
	public void
	receive(
		int					local_port,
		InetSocketAddress	remote_address,
		ByteBuffer			data )
	{
		String	key = local_port + ":" + remote_address.getAddress().getHostAddress() + ":" + remote_address.getPort();
		
		UDPConnectionSet	connection_set;
		
		synchronized( connections ){
			
			connection_set = (UDPConnectionSet)connections.get( key );
			
			if ( connection_set == null ){
				
				connection_set = new UDPConnectionSet( this, local_port, remote_address );
				
				connections.put( key, connection_set );
			}
		}
		
		connection_set.receive( data );
	}
	
	public int
	send(
		int					local_port,
		InetSocketAddress	remote_address,
		ByteBuffer			data )
	
		throws IOException
	{
		return( network_glue.send( local_port, remote_address, data ));
	}
	
	protected void
	accept(
		final int				local_port,
		final InetSocketAddress	remote_address,
		final UDPConnection		connection )
	{
		final UDPTransportHelper	helper = new UDPTransportHelper( this, remote_address, connection );

		connection.setTransport( helper );
		
		TransportCryptoManager.getSingleton().manageCrypto( 
			helper, 
			null, 
			true, 
			new TransportCryptoManager.HandshakeListener() 
			{
				public void 
				handshakeSuccess( 
					ProtocolDecoder	decoder ) 
				{
					TransportHelperFilter	filter = decoder.getFilter();
					
					ConnectionEndpoint	co_ep = new ConnectionEndpoint( remote_address);

					ProtocolEndpointUDP	pe_udp = new ProtocolEndpointUDP( co_ep, remote_address );

					UDPTransport transport = new UDPTransport( pe_udp, filter );
										
					incoming_manager.addConnection( local_port, filter, transport );
        		}

				public void 
				handshakeFailure( 
            		Throwable failure_msg ) 
				{
					if (Logger.isEnabled()){
						Logger.log(new LogEvent(LOGID, "incoming crypto handshake failure: " + Debug.getNestedExceptionMessage( failure_msg )));
					}
 
					connection.close( "handshake failure: " + Debug.getNestedExceptionMessage(failure_msg));
				}
            
				public void
				gotSecret(
					byte[]				session_secret )
				{
					helper.getConnection().setSecret( session_secret );
				}
				
				public int
				getMaximumPlainHeaderLength()
				{
					return( incoming_manager.getMaxMinMatchBufferSize());
				}
    		
				public int
				matchPlainHeader(
						ByteBuffer			buffer )
				{
					IncomingConnectionManager.MatchListener	match = incoming_manager.checkForMatch( local_port, buffer, true );
    			
					if ( match == null ){
    				
						return( TransportCryptoManager.HandshakeListener.MATCH_NONE );
    				
					}else{
						
							// no fallback for UDP
    				
						return( TransportCryptoManager.HandshakeListener.MATCH_CRYPTO_NO_AUTO_FALLBACK );
    				}
    			}
        	});
	}
	
	protected UDPConnection
	registerOutgoing(
		UDPTransportHelper		helper )
	{
		int	local_port = UDPNetworkManager.getSingleton().getUDPListeningPortNumber();
		
		InetSocketAddress	address = helper.getAddress();
		
		String	key = local_port + ":" + address.getAddress().getHostAddress() + ":" + address.getPort();
		
		synchronized( connections ){
			
			UDPConnectionSet	connection_set = (UDPConnectionSet)connections.get( key );
			
			if ( connection_set == null ){
				
				connection_set = new UDPConnectionSet( this, local_port, address );
				
				connections.put( key, connection_set );
			}
			
			UDPConnection	connection = new UDPConnection( connection_set, allocationConnectionID(), helper );
			
			connection_set.add( connection );
			
			return( connection  );
		}
	}
	
	protected synchronized int
	allocationConnectionID()
	{
		return( next_connection_id++ );
	}
}
