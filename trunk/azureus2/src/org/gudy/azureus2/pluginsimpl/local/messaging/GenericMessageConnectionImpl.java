/*
 * Created on 9 Aug 2006
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

package org.gudy.azureus2.pluginsimpl.local.messaging;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.messaging.MessageException;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageConnection;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageConnectionListener;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageEndpoint;
import org.gudy.azureus2.plugins.utils.PooledByteBuffer;
import org.gudy.azureus2.pluginsimpl.local.utils.PooledByteBufferImpl;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.nat.NATTraversalObserver;
import com.aelitis.azureus.core.nat.NATTraverser;

public class 
GenericMessageConnectionImpl
	implements GenericMessageConnection
{
	private MessageManagerImpl					message_manager;
	
	private String								msg_id;
	private String								msg_desc;
	private GenericMessageEndpointImpl			endpoint;
	private int									stream_crypto;
	byte[]										shared_secret;
	
	private boolean								incoming;
	private GenericMessageConnectionAdapter		delegate;
	
	private List	listeners	= new ArrayList();

	
	protected
	GenericMessageConnectionImpl(
		MessageManagerImpl				_message_manager,
		GenericMessageConnectionAdapter	_delegate )
	{
		message_manager	= _message_manager;
		delegate		= _delegate;
		
		incoming	= true;
		
		delegate.setOwner( this );
	}
	
	protected 
	GenericMessageConnectionImpl(
		MessageManagerImpl			_message_manager,
		String						_msg_id,
		String						_msg_desc,
		GenericMessageEndpointImpl	_endpoint,
		int							_stream_crypto,
		byte[]						_shared_secret )
	{
		message_manager	= _message_manager;
		msg_id			= _msg_id;
		msg_desc		= _msg_desc;
		endpoint		= _endpoint;
		stream_crypto	= _stream_crypto;
		shared_secret	= _shared_secret;
		
		incoming	= false;
	}
	
	public GenericMessageEndpoint
	getEndpoint()
	{
		return( endpoint==null?delegate.getEndpoint():endpoint);
	}
	
	public boolean
	isIncoming()
	{
		return( incoming );
	}
	
	
	public void
	connect()
	
		throws MessageException
	{
		connect( null );
	}
	
		/**
		 * Outgoing connection
		 * @param initial_data
		 * @throws MessageException
		 */
	
	public void
	connect(
		ByteBuffer	initial_data )
	
		throws MessageException
	{
		InetSocketAddress	tcp_ep = endpoint.getTCP();
				
		if ( tcp_ep != null ){
			
			connectTCP( initial_data, tcp_ep );
			
		}else{
			
			InetSocketAddress	udp_ep = endpoint.getUDP();

			if ( udp_ep != null ){
				
				connectUDP( initial_data, udp_ep, false );
				
			}else{
				
				throw( new MessageException( "No protocols availabld" ));
			}
		}
	}
	
	protected void
	connectTCP(
		final ByteBuffer		initial_data,
		InetSocketAddress		tcp_ep )
	{
		System.out.println( "TCP connection attempt" );
		
		GenericMessageEndpointImpl	gen_tcp = new GenericMessageEndpointImpl( endpoint.getNotionalAddress());
		
		gen_tcp.addTCP( tcp_ep );
		
		final GenericMessageConnectionDirect tcp_delegate = new GenericMessageConnectionDirect( msg_id, msg_desc, gen_tcp, stream_crypto, shared_secret );
			
		tcp_delegate.setOwner( this );

		tcp_delegate.connect( 
				initial_data,
				new GenericMessageConnectionAdapter.ConnectionListener()
				{
					public void
					connectSuccess()
					{
						delegate = tcp_delegate;
						
						reportConnected();
					}
					
					public void 
					connectFailure( 
						Throwable failure_msg )
					{
						InetSocketAddress	udp_ep = endpoint.getUDP();

						if ( udp_ep != null ){
							
							initial_data.rewind();
							
							connectUDP( initial_data, udp_ep, true );
							
						}else{
							
							reportFailed( failure_msg );
						}
					}
				});
	}
	
	protected void
	connectUDP(
		final ByteBuffer			initial_data,
		final InetSocketAddress		udp_ep,
		boolean						nat_traversal )
	{
		System.out.println( "UDP connection attempt (nat=" + nat_traversal + ")" );

		GenericMessageEndpointImpl	gen_udp = new GenericMessageEndpointImpl( endpoint.getNotionalAddress());
		
		gen_udp.addUDP( udp_ep );
		
		final GenericMessageConnectionDirect udp_delegate = new GenericMessageConnectionDirect( msg_id, msg_desc, gen_udp, stream_crypto, shared_secret );
		
		udp_delegate.setOwner( GenericMessageConnectionImpl.this );
		
		if ( nat_traversal ){
			
			final NATTraverser	nat_traverser = message_manager.getNATTraverser();
			
			Map	request = new HashMap();
					
			nat_traverser.attemptTraversal(
					message_manager,
					udp_ep,
					request,
					false,
					new NATTraversalObserver()
					{
						public void
						succeeded(
							InetSocketAddress	rendezvous,
							InetSocketAddress	target,
							Map					reply )
						{
							try{
								Map	message = new HashMap();
								
								message.put( "fred", "bill" );
								
								Map msg_reply = nat_traverser.sendMessage( message_manager, rendezvous, target, message );
								
								System.out.println( "reply=" + msg_reply );
								
							}catch( Throwable e ){
								
								e.printStackTrace();
							}

							udp_delegate.connect( 
									initial_data,
									new GenericMessageConnectionAdapter.ConnectionListener()
									{
										public void
										connectSuccess()
										{
											delegate = udp_delegate;
											
											reportConnected();
										}
										
										public void 
										connectFailure( 
											Throwable failure_msg )
										{
												// TODO: attempt to establish connection through rendezvous 
											
											reportFailed( failure_msg );
										}
									});
						}
						
						public void
						failed(
							int			failure_type )
						{
							reportFailed( new Throwable( "UDP connection attempt failed - NAT traversal failed, type=" + failure_type ));
						}
						
						public void
						failed(
							Throwable 	cause )
						{
							reportFailed( cause );
						}
						
						public void
						disabled()
						{
							reportFailed( new Throwable( "UDP connection attempt failed as DDB is disabled" ));
						}
					});
		}else{
	
			udp_delegate.connect( 
					initial_data,
					new GenericMessageConnectionAdapter.ConnectionListener()
					{
						public void
						connectSuccess()
						{
							delegate = udp_delegate;
							
							reportConnected();
						}
						
						public void 
						connectFailure( 
							Throwable failure_msg )
						{
							initial_data.rewind();
								
							connectUDP( initial_data, udp_ep, true );
						}
					});
		}
	}
	
	
		/**
		 * Incoming connection has been accepted
		 *
		 */
	
	protected void
	accepted()
	{
		delegate.accepted();
	}
	
	public void
	send(
		PooledByteBuffer			message )
	
		throws MessageException
	{
		delegate.send( message );
	}
	
	protected void
	receive(
		GenericMessage	message )
	{
		boolean	handled = false;
		
		for (int i=0;i<listeners.size();i++){
			
			PooledByteBuffer	buffer = new PooledByteBufferImpl(message.getPayload());
			
			try{
				((GenericMessageConnectionListener)listeners.get(i)).receive( this, buffer );
				
				handled = true;
				
			}catch( Throwable f ){
				
				buffer.returnToPool();
				
				Debug.printStackTrace(f);
			}
		}
		
		if ( !handled ){
			
			Debug.out( "GenericMessage: incoming message not handled" );
		}
	}
	
	public void
	close()
	
		throws MessageException
	{
		delegate.close();
	}
	
	protected void
	reportConnected()
	{
		for (int i=0;i<listeners.size();i++){
			
			try{
				((GenericMessageConnectionListener)listeners.get(i)).connected( this );
				
			}catch( Throwable f ){
				
				Debug.printStackTrace(f);
			}
		}
	}
	
	protected void
	reportFailed(
		Throwable	e )
	{
		for (int i=0;i<listeners.size();i++){
			
			try{
				((GenericMessageConnectionListener)listeners.get(i)).failed( this, e );
				
			}catch( Throwable f ){
				
				Debug.printStackTrace(f);
			}
		}
	}
	
	public void
	addListener(
		GenericMessageConnectionListener		listener )
	{
		listeners.add( listener );
	}
	
	public void
	removeListener(
		GenericMessageConnectionListener		listener )
	{
		listeners.remove( listener );
	}
}
