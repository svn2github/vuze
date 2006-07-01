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
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.networkmanager.ConnectionEndpoint;
import com.aelitis.azureus.core.networkmanager.impl.IncomingConnectionManager;
import com.aelitis.azureus.core.networkmanager.impl.ProtocolDecoder;
import com.aelitis.azureus.core.networkmanager.impl.TransportCryptoManager;
import com.aelitis.azureus.core.networkmanager.impl.TransportHelperFilter;

public class
UDPConnectionManager
	implements NetworkGlueListener
{
	private static final LogIDs LOGID = LogIDs.NET;

	public static final int	TIMER_TICK_MILLIS				= 25;
	public static final int	THREAD_LINGER_ON_IDLE_PERIOD	= 30*1000;
	
	private static final Map	connection_sets = new HashMap();
	
	private int next_connection_id;

	
	private IncomingConnectionManager	incoming_manager = IncomingConnectionManager.getSingleton();

	private NetworkGlue	network_glue;
	
	private UDPSelector		selector;
	private ProtocolTimer	protocol_timer;
	private long			idle_start;
	
	protected
	UDPConnectionManager(
		int		udp_port )
	{		
		network_glue = new NetworkGlueLoopBack( this, udp_port );
	}
	
	protected UDPSelector
	checkThreadCreation()
	{
			// called while holding the "connections" monitor
		
		if ( selector == null ){
			
			if (Logger.isEnabled()){
				Logger.log(new LogEvent(LOGID, "UDPConnectionManager: activating" ));
			}
			
			System.out.println( "UDPConnectionManager: active" );

			selector = new UDPSelector(this );
			
			protocol_timer = new ProtocolTimer();
		}
		
		return( selector );
	}
	
	protected void
	checkThreadDeath(
		boolean		connections_running )
	{
			// called while holding the "connections" monitor

		if ( connections_running ){
			
			idle_start = 0;
			
		}else{
			
			long	now = SystemTime.getCurrentTime();
			
			if ( idle_start == 0 ){
				
				idle_start = now;
				
			}else if ( idle_start > now ){
				
				idle_start = now;
				
			}else if ( now - idle_start > THREAD_LINGER_ON_IDLE_PERIOD ){
				
				if (Logger.isEnabled()){
					Logger.log(new LogEvent(LOGID, "UDPConnectionManager: deactivating" ));
				}

				System.out.println( "UDPConnectionManager: idle" );
				
				selector.destroy();
				
				selector = null;
				
				protocol_timer.destroy();
				
				protocol_timer = null;
			}
		}
	}
	
	protected void
	poll()
	{
		synchronized( connection_sets ){

			Iterator	it = connection_sets.values().iterator();
			
			while( it.hasNext()){
				
				((UDPConnectionSet)it.next()).poll();
			}
		}
	}
	
	public void
	remove(
		UDPConnectionSet	set,
		UDPConnection		connection )
	{
		synchronized( connection_sets ){

			if ( set.remove( connection )){

				InetSocketAddress	remote_address = set.getRemoteAddress();

				String	key = set.getLocalPort() + ":" + remote_address.getAddress().getHostAddress() + ":" + remote_address.getPort();

				if ( set.hasFailed()){
					
					if ( connection_sets.remove( key ) != null ){
						
						System.out.println( "Connection set " + key + " failed" );
					}
				}
			}	                          
		}                    
	}
	
	public void
	failed(
		UDPConnectionSet	set )
	{
		synchronized( connection_sets ){

			InetSocketAddress	remote_address = set.getRemoteAddress();

			String	key = set.getLocalPort() + ":" + remote_address.getAddress().getHostAddress() + ":" + remote_address.getPort();
					
			if ( connection_sets.remove( key ) != null ){
						
				System.out.println( "Connection set " + key + " failed" );
						
			}else{
						
				Debug.out( "Connection set not found" );
			}                      
		}                    
	}
	
	protected UDPConnection
	registerOutgoing(
		UDPTransportHelper		helper )
	
		throws IOException
	{
		int	local_port = UDPNetworkManager.getSingleton().getUDPListeningPortNumber();
		
		InetSocketAddress	address = helper.getAddress();
		
		String	key = local_port + ":" + address.getAddress().getHostAddress() + ":" + address.getPort();
		
		synchronized( connection_sets ){
			
			UDPSelector	current_selector	= checkThreadCreation();
			
			UDPConnectionSet	connection_set = (UDPConnectionSet)connection_sets.get( key );
			
			if ( connection_set == null ){
				
				connection_set = new UDPConnectionSet( this, current_selector, local_port, address );
				
				System.out.println( "Created new set - " + connection_set.getName() + ", outgoing" );
				
				connection_sets.put( key, connection_set );
			}
			
			UDPConnection	connection = new UDPConnection( connection_set, allocationConnectionID(), helper );
			
			connection_set.add( connection );
			
			return( connection  );
		}
	}
	
	public void
	receive(
		int					local_port,
		InetSocketAddress	remote_address,
		byte[]				data )
	{
		String	key = local_port + ":" + remote_address.getAddress().getHostAddress() + ":" + remote_address.getPort();
		
		UDPConnectionSet	connection_set;
		
		synchronized( connection_sets ){
			
			UDPSelector	current_selector	= checkThreadCreation();
			
			connection_set = (UDPConnectionSet)connection_sets.get( key );
			
			if ( connection_set == null ){
				
				connection_set = new UDPConnectionSet( this, current_selector, local_port, remote_address );
				
				System.out.println( "Created new set - " + connection_set.getName() + ", incoming" );

				connection_sets.put( key, connection_set );
			}
		}
		
		try{
			connection_set.receive( data );
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
			
			connection_set.failed( e );
		}
	}
	
	public int
	send(
		int					local_port,
		InetSocketAddress	remote_address,
		byte[]				data )
	
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

		try{
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
								
						helper.setTransport( transport );
						
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
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
			
			helper.close( Debug.getNestedExceptionMessage(e));
		}
	}
	
	protected synchronized int
	allocationConnectionID()
	{
		int	id = next_connection_id++;
		
		if ( id < 0 ){
			
			id					= 0;
			next_connection_id	= 1;
		}
		
		return( id );
	}
	
	protected class
	ProtocolTimer
	{
		private volatile boolean	destroyed;
		
		protected 
		ProtocolTimer()
		{
			new AEThread( "UDPConnectionManager:timer", true )
			{
				public void
				runSupport()
				{
					Thread.currentThread().setPriority( Thread.NORM_PRIORITY + 1 );
					
					while( !destroyed ){
						
						try{
							Thread.sleep( TIMER_TICK_MILLIS );
							
						}catch( Throwable e ){
							
						}
								
						List	failed_sets = null;
						
						synchronized( connection_sets ){
									
							checkThreadDeath( connection_sets.size() > 0 );
								
							Iterator	it = connection_sets.values().iterator();
							
							while( it.hasNext()){
								
								UDPConnectionSet	set = (UDPConnectionSet)it.next();
								
								try{
									set.timerTick();
									
									if ( set.idleLimitExceeded()){
										
										System.out.println( "Idle limit exceeded for " + set.getName() + ", removing" );
										
										it.remove();
									}
								}catch( Throwable e ){
									
									if ( failed_sets == null ){
										
										failed_sets = new ArrayList();
									}
									
									failed_sets.add( new Object[]{ set, e });
								}
							}
						}
						
						if ( failed_sets != null ){
							
							for (int i=0;i<failed_sets.size();i++){
								
								Object[]	entry = (Object[])failed_sets.get(i);
								
								((UDPConnectionSet)entry[0]).failed((Throwable)entry[1]);
							}
						}
					}
				}
			}.start();
		}
		
		protected void
		destroy()
		{
			destroyed	= true;
		}
	}
}
