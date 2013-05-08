/*
 * Created on 16 Jun 2006
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

package com.aelitis.azureus.core.networkmanager;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;

import com.aelitis.azureus.core.networkmanager.Transport.ConnectListener;


public class 
ConnectionEndpoint 
{
	private InetSocketAddress	notional_address;
	private ProtocolEndpoint[]	protocols;
	
	public
	ConnectionEndpoint(
		InetSocketAddress	_notional_address )
	{
		notional_address	= _notional_address;
	}
	
	public InetSocketAddress
	getNotionalAddress()
	{
		return( notional_address );
	}
		
	public ProtocolEndpoint[]
	getProtocols()
	{
		if ( protocols == null ){
			
			return( new ProtocolEndpoint[0] );
		}
		
		return( protocols );
	}
	
	public void
	addProtocol(
		ProtocolEndpoint	ep )
	{
		if ( protocols == null ){
			
			protocols = new ProtocolEndpoint[]{ ep };
			
		}else{
		
			for (int i=0;i<protocols.length;i++){
				
				if ( protocols[i] == ep ){
					
					return;
				}
			}
			
			ProtocolEndpoint[]	new_ep = new ProtocolEndpoint[ protocols.length + 1 ];
			
			System.arraycopy( protocols, 0, new_ep, 0, protocols.length );
			
			new_ep[ protocols.length ] = ep;
			
			protocols	= new_ep;
		}
		
		ep.setConnectionEndpoint( this );
	}
	
	public ConnectionEndpoint
	getLANAdjustedEndpoint()
	{
		ConnectionEndpoint	result = new ConnectionEndpoint( notional_address );
		
		for (int i=0;i<protocols.length;i++){

			ProtocolEndpoint ep = protocols[i];
			
			InetSocketAddress address = ep.getAdjustedAddress( true );
			
			ProtocolEndpointFactory.createEndpoint( ep.getType(), result, address );
		}
		
		return( result );
	}
	
	public ConnectionAttempt
	connectOutbound(
		final boolean			connect_with_crypto, 
		final boolean 			allow_fallback, 
		final byte[][]			shared_secrets,
		final ByteBuffer		initial_data,
		final int				priority,
		final ConnectListener 	listener )
	{
		if ( protocols.length == 1 ){
			
			ProtocolEndpoint	protocol = protocols[0];
			
			final Transport transport = protocol.connectOutbound( connect_with_crypto, allow_fallback, shared_secrets, initial_data, priority, listener );
			
			return( 
				new ConnectionAttempt()
				{
					public void 
					abandon() 
					{
						if ( transport != null ){
							
							transport.close( "Connection attempt abandoned" );
						}
					}
				});
			
		}else{
			
			final boolean[] connected = { false };
			final boolean[] abandoned = { false };
			
			final List<Transport> transports = new ArrayList<Transport>( protocols.length );
			
			final ConnectListener listener_delegate = 
				new ConnectListener()
				{
					private long		start_time; 
					private int			timeout = Integer.MIN_VALUE;
					private int			fail_count;
					
					public int 
					connectAttemptStarted(
						int default_connect_timeout) 
					{
						synchronized( connected ){
							
							if ( timeout == Integer.MIN_VALUE ){
								
								start_time = SystemTime.getCurrentTime();
								
								timeout = listener.connectAttemptStarted( default_connect_timeout );
							}
						}
						
						return( timeout);
					}
					
				    public void 
				    connectSuccess(
				    	Transport	transport, 
				    	ByteBuffer 	remaining_initial_data )
				    {
				    	boolean	disconnect;
				    	
				    	synchronized( connected ){
				    		
				    		disconnect = abandoned[0];
				    		
				    		if ( !disconnect ){
				    			
					    		if ( !connected[0] ){
					    			
					    			connected[0] = true;
					    			
					    			//System.out.println( "Connect took " + (SystemTime.getCurrentTime() - start_time ) + " for " + transport.getDescription());
					    			
					    		}else{
					    			
					    			disconnect = true;
					    		}
				    		}
				    	}
				    	
				    	if ( disconnect ){
				    						    		
				    		transport.close( "Transparent not required" );
				    		
				    	}else{
				    		
				    		listener.connectSuccess( transport, remaining_initial_data );
				    	}
				    }
				    
				    public void 
				    connectFailure( 
				    	Throwable failure_msg )
				    {
				    	boolean	inform;
				    	
				    	synchronized( connected ){
				    		
				    		fail_count++;
				    						    		
				    		inform = fail_count == protocols.length;
				    	}
				    	
				    	if ( inform ){
				    						    		
				    		listener.connectFailure(failure_msg);
				    	}
				    }
				};
			
			boolean	ok = true;
				
			if ( protocols.length != 2 ){
				
				ok = false;
				
			}else{
				ProtocolEndpoint	p1 = protocols[0];
				ProtocolEndpoint 	p2 = protocols[1];
				
				if ( p1.getType() == ProtocolEndpoint.PROTOCOL_TCP && p2.getType() == ProtocolEndpoint.PROTOCOL_UTP ){
					
				}else if ( p2.getType() == ProtocolEndpoint.PROTOCOL_TCP && p1.getType() == ProtocolEndpoint.PROTOCOL_UTP ){
					
					ProtocolEndpoint temp = p1;
					p1 = p2;
					p2 = temp;
				}else{
					
					ok = false;
				}
				
				if ( ok ){
					
						// p2 is uTP, p1 is TCP
					
					final ByteBuffer initial_data_copy;
					
					if ( initial_data != null ){
						
						initial_data_copy = initial_data.duplicate();
						
					}else{
						
						initial_data_copy = null;
					}
									
					Transport transport = 
						p2.connectOutbound( 
							connect_with_crypto, 
							allow_fallback, 
							shared_secrets, 
							initial_data, 
							priority, 
							new ConnectListenerEx( listener_delegate ));
						
					transports.add( transport );
					
					final ProtocolEndpoint tcp_ep = p1;
					
					SimpleTimer.addEvent(
						"delay:tcp:connect",
						SystemTime.getCurrentTime() + 750,
						false,
						new TimerEventPerformer()
						{
							public void 
							perform(
								TimerEvent event) 
							{
								synchronized( connected ){
									
									if ( connected[0] || abandoned[0] ){
										
										return;
									}
								}
																
								Transport transport = 
									tcp_ep.connectOutbound( 
										connect_with_crypto, 
										allow_fallback, 
										shared_secrets, 
										initial_data_copy, 
										priority, 
										new ConnectListenerEx( listener_delegate ));
								
								synchronized( connected ){
									
									if ( abandoned[0] ){
										
										transport.close( "Connection attempt abandoned" );
										
									}else{
								
										transports.add( transport );
									}
								}							}
						});
				}
			}
			
			if ( !ok ){
			
				Debug.out( "No supportified!" );
			
				listener.connectFailure( new Exception( "Not Supported" ));
			}

			return( 
					new ConnectionAttempt()
					{
						public void 
						abandon() 
						{
							List<Transport> to_kill;
						
							synchronized( connected ){
								
								abandoned[0] = true;
							
								to_kill = new ArrayList<Transport>( transports );
							}
							
							for ( Transport transport: to_kill ){
								
								transport.close( "Connection attempt abandoned" );
							}
						}
					});
		}
	}  
	
	public String
	getDescription()
	{
		String	str = "[";
		
		for (int i=0;i<protocols.length;i++){
			
			str += (i==0?"":",") + protocols[i].getDescription();
		}
		
		return( str + "]" );
	}
	
	private class
	ConnectListenerEx
		implements ConnectListener
	{
		private ConnectListener		listener;
		
		private boolean	ok;
		private boolean	failed;
		
		private 
		ConnectListenerEx(
			ConnectListener		_listener )
		{
			listener = _listener;
		}
		
		public int 
		connectAttemptStarted( 
			int 			default_connect_timeout )
		{
			return( listener.connectAttemptStarted(default_connect_timeout));
		}
		     
		public void 
		connectSuccess(
			Transport		transport, 
			ByteBuffer 		remaining_initial_data )
		{
			synchronized( this ){
				
				if ( ok || failed ){
					
					if ( ok ){
					
						Debug.out( "Double doo doo" );
					}
					
					return;
				}
				
				ok = true;
			}
			
			listener.connectSuccess( transport, remaining_initial_data );
		}

		public void 
		connectFailure( 
			Throwable 		failure_msg )
		{
			synchronized( this ){
				
				if ( ok || failed ){
										
					return;
				}
				
				failed = true;
			}
			
			listener.connectFailure( failure_msg );
		}
	}	
}
