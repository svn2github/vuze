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

import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.dht.transport.*;
import com.aelitis.azureus.core.dht.transport.udp.*;
import com.aelitis.azureus.core.dht.transport.util.DHTTransportRequestCounter;
import com.aelitis.azureus.core.dht.transport.util.DHTTransportStatsImpl;
import com.aelitis.net.udp.*;

/**
 * @author parg
 *
 */

public class 
DHTTransportUDPImpl 
	implements DHTTransportUDP, PRUDPRequestHandler
{
	static{
		
		DHTUDPPacket.registerCodecs();
	}
	
	private int					max_fails;
	
	private PRUDPPacketHandler			packet_handler;
	
	private DHTTransportRequestHandler	request_handler;
	
	private DHTTransportUDPContactImpl		local_contact;
	
	private DHTTransportStatsImpl	stats = new DHTTransportStatsImpl();

		// TODO: secure enough?
	
	private	Random		random = new Random( SystemTime.getCurrentTime());
	
	public
	DHTTransportUDPImpl(
		int		_port,
		int		_max_fails )
	{
		max_fails	= _max_fails;
		
			// DHTPRUDPPacket relies on the request-handler being an instanceof THIS so watch out
			// if you change it :)
		
		packet_handler = PRUDPPacketHandlerFactory.getHandler( _port, this );		

			// TODO: ascertain correct external IP address
		
		local_contact = new DHTTransportUDPContactImpl( this, new InetSocketAddress( "127.0.0.1", _port ));
	}
	
	protected int
	getMaxFailCount()
	{
		return( max_fails );
	}
	
	public DHTTransportContact
	getLocalContact()
	{
		return( local_contact );
	}
	
	public void
	importContact(
		DataInputStream		is )
	
		throws IOException
	{
		request_handler.contactImported( 
				DHTUDPUtils.deserialiseContact( this, is ));
	}
	
	public void
	importContact(
		InetSocketAddress	address )
	{
		request_handler.contactImported( 
			new DHTTransportUDPContactImpl( this, address ));
	}
	
	public void
	exportContact(
		DHTTransportContact	contact,
		DataOutputStream	os )
	
		throws IOException
	{
		DHTUDPUtils.serialiseContact( os, contact );
	}
	
	public void
	setRequestHandler(
		DHTTransportRequestHandler	_request_handler )
	{
		request_handler = new DHTTransportRequestCounter( _request_handler, stats );
	}
	
	public DHTTransportStats
	getStats()
	{
		return( stats );
	}
	
	protected void
	dispatch(
		Runnable	r )
	{
		// TODO: request queue limits?
		
		r.run();
	}
	
	protected void
	sendPing(
		final DHTTransportUDPContactImpl	contact,
		final DHTTransportReplyHandler		handler )
	{
		AERunnable	runnable = 
			new AERunnable()
			{
				public void
				runSupport()
				{
					sendPingSupport( contact, handler );
				}
			};
		
		dispatch( runnable );
	}
	
	protected void
	sendPingSupport(
		final DHTTransportUDPContactImpl	contact,
		final DHTTransportReplyHandler		handler )
	{
		stats.pingSent();

		final long	connection_id = getConnectionID();
		
		try{
			packet_handler.sendAndReceive(
				new DHTUDPPacketRequestPing( connection_id ),
				contact.getAddress(),
				new PRUDPPacketReceiver()
				{
					public void
					packetReceived(
						PRUDPPacket			packet,
						InetSocketAddress	from_address )
					{
						try{
							DHTUDPPacketReplyPing	reply = (DHTUDPPacketReplyPing)packet;
							
							if ( reply.getConnectionId() != connection_id ){
								
								throw( new Exception( "connection id mismatch" ));
							}
						
							stats.pingOK();
							
							handler.pingReply( contact );
							
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
							
							stats.pingFailed();
							
							handler.failed( contact );
						}
					}
					
					public void
					error(
						PRUDPPacketHandlerException	e )
					{
						stats.pingFailed();
						
						handler.failed( contact );
					}
				});
			
		}catch( Throwable e ){
			
			stats.pingFailed();
			
			handler.failed( contact );
		}
	}
		
		// STORE
	
	public void
	sendStore(
		final DHTTransportUDPContactImpl	contact,
		final DHTTransportReplyHandler		handler,
		final byte[]						key,
		final DHTTransportValue				value )
	{
		AERunnable	runnable = 
			new AERunnable()
			{
				public void
				runSupport()
				{
					sendStoreSupport( contact, handler, key, value );
				}
			};
		
		dispatch( runnable );
	}
	
	public void
	sendStoreSupport(
		final DHTTransportUDPContactImpl	contact,
		final DHTTransportReplyHandler		handler,
		byte[]								key,
		DHTTransportValue					value )
	{
		stats.storeSent();
		
		final long	connection_id = getConnectionID();
		
		try{
			DHTUDPPacketRequestStore	request = new DHTUDPPacketRequestStore( connection_id );
			
			request.setKey( key );
			
			request.setValue( value );
			
			packet_handler.sendAndReceive(
				request,
				contact.getAddress(),
				new PRUDPPacketReceiver()
				{
					public void
					packetReceived(
						PRUDPPacket			packet,
						InetSocketAddress	from_address )
					{
						try{
							DHTUDPPacketReplyStore	reply = (DHTUDPPacketReplyStore)packet;
							
							if ( reply.getConnectionId() != connection_id ){
								
								throw( new Exception( "connection id mismatch" ));
							}
						
							stats.storeOK();
							
							handler.storeReply( contact );
							
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
							
							stats.storeFailed();
							
							handler.failed( contact );
						}
					}
					
					public void
					error(
						PRUDPPacketHandlerException	e )
					{
						stats.storeFailed();
						
						handler.failed( contact );
					}
				});
			
		}catch( Throwable e ){
			
			stats.storeFailed();
			
			handler.failed( contact );
		}
	}
	
		// FIND NODE
	
	public void
	sendFindNode(
		final DHTTransportUDPContactImpl	contact,
		final DHTTransportReplyHandler		handler,
		final byte[]						nid )
	{
		AERunnable	runnable = 
			new AERunnable()
			{
				public void
				runSupport()
				{
					sendFindNodeSupport( contact, handler, nid );
				}
			};
		
		dispatch( runnable );
	}
	
	public void
	sendFindNodeSupport(
		final DHTTransportUDPContactImpl	contact,
		final DHTTransportReplyHandler		handler,
		byte[]								nid )
	{
		stats.findNodeSent();
		
		final long	connection_id = getConnectionID();
		
		try{
			DHTUDPPacketRequestFindNode	request = new DHTUDPPacketRequestFindNode( connection_id );
			
			request.setID( nid );
			
			packet_handler.sendAndReceive(
				request,
				contact.getAddress(),
				new PRUDPPacketReceiver()
				{
					public void
					packetReceived(
						PRUDPPacket			packet,
						InetSocketAddress	from_address )
					{
						try{
							DHTUDPPacketReplyFindNode	reply = (DHTUDPPacketReplyFindNode)packet;
							
							if ( reply.getConnectionId() != connection_id ){
								
								throw( new Exception( "connection id mismatch" ));
							}
						
							stats.findNodeOK();
							
							handler.findNodeReply( contact, reply.getContacts());
							
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
							
							stats.findNodeFailed();
							
							handler.failed( contact );
						}
					}
					
					public void
					error(
						PRUDPPacketHandlerException	e )
					{
						stats.findNodeFailed();
						
						handler.failed( contact );
					}
				});
			
		}catch( Throwable e ){
			
			stats.findNodeFailed();
			
			handler.failed( contact );
		}
	}
	
		// FIND VALUE
	
	public void
	sendFindValue(
		final DHTTransportUDPContactImpl	contact,
		final DHTTransportReplyHandler		handler,
		final byte[]						key )
	{
		AERunnable	runnable = 
			new AERunnable()
			{
				public void
				runSupport()
				{
					sendFindValueSupport( contact, handler, key );
				}
			};
		
		dispatch( runnable );
	}
	
	public void
	sendFindValueSupport(
		final DHTTransportUDPContactImpl	contact,
		final DHTTransportReplyHandler		handler,
		byte[]								key )
	{
		stats.findValueSent();

		final long	connection_id = getConnectionID();
		
		try{
			DHTUDPPacketRequestFindValue	request = new DHTUDPPacketRequestFindValue( connection_id );
			
			request.setID( key );
			
			packet_handler.sendAndReceive(
				request,
				contact.getAddress(),
				new PRUDPPacketReceiver()
				{
					public void
					packetReceived(
						PRUDPPacket			packet,
						InetSocketAddress	from_address )
					{
						try{
							DHTUDPPacketReplyFindValue	reply = (DHTUDPPacketReplyFindValue)packet;
							
							if ( reply.getConnectionId() != connection_id ){
								
								throw( new Exception( "connection id mismatch" ));
							}
						
							stats.findValueOK();
							
							DHTTransportValue	res = reply.getValue();
							
							if ( res != null ){
								
								handler.findValueReply( contact, res );
								
							}else{
								
								handler.findValueReply( contact, reply.getContacts());
							}
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
							
							stats.findValueFailed();
							
							handler.failed( contact );
						}
					}
					
					public void
					error(
						PRUDPPacketHandlerException	e )
					{
						stats.findValueFailed();
						
						handler.failed( contact );
					}
				});
			
		}catch( Throwable e ){
			
			stats.findValueFailed();
			
			handler.failed( contact );
		}
	}
	
	public void
	process(
		PRUDPPacketRequest	request )
	{
		try{
			
			DHTTransportContact	originating_contact = new DHTTransportUDPContactImpl( this, request.getAddress());
			
			if ( request instanceof DHTUDPPacketRequestPing ){
				
				request_handler.pingRequest( originating_contact );
				
				DHTUDPPacketReplyPing	reply = 
					new DHTUDPPacketReplyPing(
							request.getTransactionId(),
							request.getConnectionId());
				
				packet_handler.send( reply, request.getAddress());
				
			}else if ( request instanceof DHTUDPPacketRequestStore ){
				
				DHTUDPPacketRequestStore	store_request = (DHTUDPPacketRequestStore)request;
				
				request_handler.storeRequest(
						originating_contact, 
						store_request.getKey(), 
						store_request.getValue());
				
				DHTUDPPacketReplyStore	reply = 
					new DHTUDPPacketReplyStore(
							request.getTransactionId(),
							request.getConnectionId());
				
				packet_handler.send( reply, request.getAddress());
				
			}else if ( request instanceof DHTUDPPacketRequestFindNode ){
				
				DHTUDPPacketRequestFindNode	find_request = (DHTUDPPacketRequestFindNode)request;
				
				DHTTransportContact[] res = 
					request_handler.findNodeRequest(
								originating_contact,
								find_request.getID());
				
				DHTUDPPacketReplyFindNode	reply = 
					new DHTUDPPacketReplyFindNode(
							request.getTransactionId(),
							request.getConnectionId());
							
				reply.setContacts( res );
				
				packet_handler.send( reply, request.getAddress());
				
			}else if ( request instanceof DHTUDPPacketRequestFindValue ){
				
				DHTUDPPacketRequestFindValue	find_request = (DHTUDPPacketRequestFindValue)request;
			
				Object res = 
					request_handler.findValueRequest(
								originating_contact,
								find_request.getID());
				
				DHTUDPPacketReplyFindValue	reply = 
					new DHTUDPPacketReplyFindValue(
						request.getTransactionId(),
						request.getConnectionId());
				
				if ( res instanceof DHTTransportValue ){
					
					reply.setValue((DHTTransportValue)res);

				}else{
					
					reply.setContacts((DHTTransportContact[])res );
				}
				
				packet_handler.send( reply, request.getAddress());
				
			}else{
				
				Debug.out( "Unexpected packet:" + request.toString());
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	protected long
	getConnectionID()
	{
			// unfortunately, to reuse the UDP port with the tracker protocol we 
			// have to distinguish our connection ids by setting the MSB. This allows
			// the decode to work as there is no common header format for the request
			// and reply packets
		
			// note that tracker usage of UDP via this handler is only for outbound
			// messages, hence for that use a request will never be received by the
			// handler
		
		return( 0x8000000000000000L | random.nextLong());
	}
}
