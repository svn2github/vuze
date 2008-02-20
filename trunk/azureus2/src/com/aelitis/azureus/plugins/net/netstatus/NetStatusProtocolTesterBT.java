/*
 * Created on Feb 15, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.plugins.net.netstatus;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.*;

import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.networkmanager.ConnectionEndpoint;
import com.aelitis.azureus.core.networkmanager.IncomingMessageQueue;
import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.OutgoingMessageQueue;
import com.aelitis.azureus.core.networkmanager.ProtocolEndpoint;
import com.aelitis.azureus.core.networkmanager.impl.tcp.ProtocolEndpointTCP;
import com.aelitis.azureus.core.peermanager.PeerManager;
import com.aelitis.azureus.core.peermanager.PeerManagerRegistration;
import com.aelitis.azureus.core.peermanager.PeerManagerRegistrationAdapter;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTHandshake;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTMessage;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTMessageDecoder;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTMessageEncoder;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTMessageFactory;

public class 
NetStatusProtocolTesterBT 
{
	private static Random	random = new SecureRandom();
	
	private NetStatusProtocolTester	tester;
	
	private byte[]		my_hash;
	private byte[]		peer_id;
	
	private PeerManagerRegistration		pm_reg;

	private long		start_time	= SystemTime.getCurrentTime();
	
	private List		connections	= new ArrayList();
		
	private boolean		active;
	private boolean		destroyed;
	
	protected
	NetStatusProtocolTesterBT(
		NetStatusProtocolTester		_tester )
	{
		tester	= _tester;
		
		my_hash = new byte[20];
		
		random.nextBytes( my_hash );
		
		peer_id = new byte[20];
		
		random.nextBytes( peer_id );
		

		pm_reg = PeerManager.getSingleton().registerLegacyManager(
			new HashWrapper( my_hash ),
			new PeerManagerRegistrationAdapter()
			{
				public byte[][]
	          	getSecrets()
				{
					return( new byte[][]{ my_hash });
				}
	          	
	          	public boolean
	          	manualRoute(
	          		NetworkConnection		connection )
	          	{
	          		log( "Got incoming connection from " + connection.getEndpoint().getNotionalAddress());
	          		
	          		initialiseConnection( connection, null );
	          		
	          		return( true );
	          	}
	          	
	          	public boolean
	          	isPeerSourceEnabled(
	          		String					peer_source )
	          	{
	          		return( true );
	          	}
	          	
	          	public boolean
	          	activateRequest(
	          		InetSocketAddress		remote_address )
	          	{
	          		return( true );
	          	}
	          	
	          	public void
	          	deactivateRequest(
	          		InetSocketAddress		remote_address )
	          	{
	          	}
	          	
	          	public String
	          	getDescription()
	          	{
	          		return( "NetStatusPlugin - router" );
	          	}

			});
		
		log( "Incoming routing established for " + ByteFormatter.encodeString( my_hash ));
	}
	
	protected byte[]
	getServerHash()
	{
		return( my_hash );
	}
	
	protected long
	getStartTime(
		long	now )
	{
		if ( now < start_time ){
			
			start_time = now;
		}
		
		return( start_time );
	}
	
	protected void
	testOutbound(
		InetSocketAddress		address,
		final byte[]			their_hash,
		boolean					use_crypto )
	{
		active	= true;
		
		log( "Making outbound connection to " + address );
		
		boolean	allow_fallback	= false;
		
		ProtocolEndpoint	pe = new ProtocolEndpointTCP( address );
		
		ConnectionEndpoint connection_endpoint	= new ConnectionEndpoint( address );

		connection_endpoint.addProtocol( pe );

		final NetworkConnection connection = 
			NetworkManager.getSingleton().createConnection(
					connection_endpoint, 
					new BTMessageEncoder(), 
					new BTMessageDecoder(), 
					use_crypto, 
					allow_fallback, 
					new byte[][]{ their_hash });
	
		connection.connect( 
				true,
				new NetworkConnection.ConnectionListener() 
				{
					public final void 
					connectStarted() 
					{
						log( "Outbound connect start" );
					}

					public final void 
					connectSuccess( 
						ByteBuffer remaining_initial_data ) 
					{
						log( "Outbound connect success" );
						
						initialiseConnection( connection, their_hash );
					}

					public final void 
					connectFailure( 
						Throwable e ) 
					{
						log( "Outbound connect fail", e );
						
						closeConnection( connection );
					}

					public final void 
					exceptionThrown( 
						Throwable e ) 
					{
						log( "Outbound connect fail", e );
						
						closeConnection( connection );
					}
    			
					public String
					getDescription()
					{
						return( "NetStatusPlugin - outbound" );
					}
				});
	}
	
	protected void
	initialiseConnection(
		NetworkConnection	connection,
		byte[]				info_hash )
	{
		synchronized( this ){
			
			if ( destroyed ){
				
				log( "Already destroyed" );
				
				connection.close();
				
				return;
				
			}else{
				
				connections.add( connection );
			}
		}
		
		new Session( connection, info_hash );
	}
	
	protected void
	closeConnection(
		NetworkConnection	c )
	{
		synchronized( this ){

			connections.remove( c );
		}
		
		c.close();
	}
	
	protected boolean
	isActive()
	{
		return( active );
	}
	
	protected void
	destroy()
	{
		List	to_close	= new ArrayList();
		
		synchronized( this ){
			
			if ( destroyed ){
				
				return;
			}
			
			destroyed = true;
			
			to_close.addAll( connections );
			
			connections.clear();
		}
		
		for (int i=0;i<to_close.size();i++){
			
			NetworkConnection connection = (NetworkConnection)to_close.get(i);
			
			connection.close();
		}
		
		pm_reg.unregister();
		
		log( "Incoming routing destroyed for " + ByteFormatter.encodeString( my_hash ));

	}
	
	protected boolean
	isDestroyed()
	{
		return( destroyed );
	}
	
	protected void
	log(
		String	str )
	{
		tester.log( str );
	}
	
	protected void
	log(
		String		str,
		Throwable	e )
	{
		tester.log( str, e );
	}
	
	protected class
	Session
	{
		private NetworkConnection		connection;
		private boolean					initiator;
		private byte[]					info_hash;
		
		private boolean 	handshake_sent;


		protected
		Session(
			NetworkConnection		_connection,
			byte[]					_info_hash )
		{
			connection	= _connection;
			info_hash	= _info_hash;
			
			initiator 	= info_hash != null;
			
			connection.getIncomingMessageQueue().registerQueueListener(
				new IncomingMessageQueue.MessageQueueListener() 
				{
					
					public boolean 
					messageReceived(
						Message message ) 
					{               
						try{
							String	message_id = message.getID();
	
							log( "Incoming message received: " + message.getID());
							
					        if ( message_id.equals( BTMessage.ID_BT_HANDSHAKE )){
						
					        	if ( !handshake_sent ){
					        		
					        		BTHandshake handshake = (BTHandshake)message;
					        		
					        		info_hash = handshake.getDataHash();
					        		
					        		sendHandshake();
					        	}
							}
					        
					        return( true );
					        
						}finally{
							
							message.destroy();
						}
					}
	  

					public final void 
					protocolBytesReceived(
						int byte_count ) 
					
					{
					}

					public final void 
					dataBytesReceived( 
						int byte_count ) 
					{
					}
				});

			connection.getOutgoingMessageQueue().registerQueueListener( 
				new OutgoingMessageQueue.MessageQueueListener() 
				{
					public final boolean 
					messageAdded( 
						Message message )
					{
						return( true );
					}
		
					public final void 
					messageQueued( 
						Message message )
					{
					}
		
					public final void 
					messageRemoved( 
						Message message )
					{
						
					}
		
					public final void 
					messageSent( 
						Message message ) 
					{
						log( "Outgoing message sent: " + message.getID());
					}
		
					public final void 
					protocolBytesSent( 
						int byte_count ) 
					{
					}
		
					public final void 
					dataBytesSent( 
						int byte_count ) 
					{
					}
			});

			connection.startMessageProcessing();
			
			if ( initiator ){
				
				sendHandshake();
			}
		}
		
		protected void
		sendHandshake()
		{
			handshake_sent = true;
			
			connection.getOutgoingMessageQueue().addMessage(
				new BTHandshake( info_hash, peer_id, false, BTMessageFactory.MESSAGE_VERSION_INITIAL ),
				false );
		}
	}
}
