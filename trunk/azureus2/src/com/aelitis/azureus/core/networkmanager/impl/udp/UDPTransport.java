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

import java.nio.ByteBuffer;

import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.networkmanager.TransportEndpoint;
import com.aelitis.azureus.core.networkmanager.impl.ProtocolDecoder;
import com.aelitis.azureus.core.networkmanager.impl.TransportCryptoManager;
import com.aelitis.azureus.core.networkmanager.impl.TransportHelperFilter;
import com.aelitis.azureus.core.networkmanager.impl.TransportImpl;

public class 
UDPTransport
	extends TransportImpl
{
	private static final LogIDs LOGID = LogIDs.NET;
	
	private ProtocolEndpointUDP		endpoint;
	private byte[][]				shared_secrets;
	
	private int transport_mode = TRANSPORT_MODE_NORMAL;
	
	private volatile boolean	closed;
	
	protected
	UDPTransport(
		ProtocolEndpointUDP		_endpoint,
		byte[][]				_shared_secrets )
	{
		endpoint		= _endpoint;
		shared_secrets	= _shared_secrets;
	}

	protected
	UDPTransport(
		ProtocolEndpointUDP		_endpoint,
		TransportHelperFilter	_filter )
	{
		endpoint		= _endpoint;
	
		setFilter( _filter );
	}
	
	public TransportEndpoint 
	getTransportEndpoint()
	{
		return( new TransportEndpointUDP( endpoint ));
	}
	  
	public int
	getMssSize()
	{
	  return( UDPNetworkManager.getUdpMssSize());
	}
	 
	public String 
	getDescription()
	{
		return( endpoint.getAddress().toString());
	}
	
	public void 
	setTransportMode( 
		int mode )
	{
		transport_mode	= mode;
	}
	 
	public int 
	getTransportMode()
	{
		return( transport_mode );
	}
	
	public void
	connectOutbound(
		ByteBuffer				initial_data,
		final ConnectListener 	listener )
	{
		if ( !UDPNetworkManager.UDP_OUTGOING_ENABLED ){
			
			listener.connectFailure( new Throwable( "Outbound UDP connections disabled" ));
			
			return;
		}
		
		if ( closed ){
			
			listener.connectFailure( new Throwable( "Connection already closed" ));
			
			return;
		}
		    
		if( getFilter() != null ){
		     
			listener.connectFailure( new Throwable( "Already connected" ));
			
			return;
		}
		
		UDPTransportHelper	helper = null;

		try{
			listener.connectAttemptStarted();

			helper = 
	 			new UDPTransportHelper( 
	 					UDPNetworkManager.getSingleton().getConnectionManager(), 
	 					endpoint.getAddress(),
	 					this );
	 		
			final UDPTransportHelper f_helper = helper;
			
	    	TransportCryptoManager.getSingleton().manageCrypto( 
	    			helper, 
	    			shared_secrets, 
	    			false, 
	    			initial_data,
	    			new TransportCryptoManager.HandshakeListener() 
	    			{
	    				public void 
	    				handshakeSuccess( 
	    					ProtocolDecoder	decoder,
	    					ByteBuffer		remaining_initial_data )
	    				{
	    					TransportHelperFilter	filter = decoder.getFilter();
	    					
	    					try{
		    					setFilter( filter );
		    					
		    					if ( closed ){
		    						
		    						close( "Already closed" );
		    						
		    						listener.connectFailure( new Exception( "Connection already closed" ));
		    						
		    					}else{
		    						
			    		   			if ( Logger.isEnabled()){
			    		    		
			    		   				Logger.log(new LogEvent(LOGID, "Outgoing UDP stream to " + endpoint.getAddress() + " established, type = " + filter.getName()));
			    		    		}
			    		   			
			    		   			connectedOutbound();
			    		   			
			    		   			listener.connectSuccess( UDPTransport.this, remaining_initial_data );
		    					}
	    					}catch( Throwable e ){
	    						
	    						Debug.printStackTrace(e);
	    						
	    						close( Debug.getNestedExceptionMessageAndStack(e));
	    						
	    						listener.connectFailure( e );
	    					}
	    				}
	
	    				public void 
	    				handshakeFailure( 
	    					Throwable failure_msg )
	    				{
	    					f_helper.close( Debug.getNestedExceptionMessageAndStack(failure_msg));
	    					
	    					listener.connectFailure( failure_msg );
	    				}
	    				
	    				public void
	    				gotSecret(
							byte[]				session_secret )
	    				{
	    					f_helper.getConnection().setSecret( session_secret );
	    				}
	    				
	    				public int 
	    				getMaximumPlainHeaderLength()
	    				{
	    		   			throw( new RuntimeException());	// this is outgoing
	    				}
	    				
	    				public int 
	    				matchPlainHeader( 
	    					ByteBuffer buffer )
	    				{
	    					throw( new RuntimeException());	// this is outgoing
	    				}
	    			});
	    	
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
			
			if ( helper != null ){
			
				helper.close( Debug.getNestedExceptionMessage( e ));
			}
				
			listener.connectFailure( e );
		}
	}
	  
	public void 
	close(
		String	reason )
	{
		closed	= true;
		
		readyForRead( false );
		readyForWrite( false );

		TransportHelperFilter	filter = getFilter();
		
		if ( filter != null ){
			
			filter.getHelper().close( reason );
			
			setFilter( null );
		}
	}
}
