/*
 * Created on 18-Jan-2006
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

package com.aelitis.azureus.core.networkmanager.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.VirtualChannelSelector;
import com.aelitis.azureus.core.networkmanager.VirtualChannelSelector.VirtualSelectorListener;

public class 
TCPProtocolDecoderInitial 
	extends TCPProtocolDecoder
{	
	private static final LogIDs LOGID = LogIDs.NWMAN;

	private static VirtualChannelSelector	read_selector	= NetworkManager.getSingleton().getReadSelector();
	private static VirtualChannelSelector	write_selector	= NetworkManager.getSingleton().getWriteSelector();

	private TCPProtocolDecoderAdapter	adapter;
	
	private TCPTransportHelperFilter	filter;
	
	private SocketChannel	channel;
	
	public static final byte[]	BT_HEADER;
	
	static{
		byte[]	bytes = "BitTorrent protocol".getBytes();
		
		BT_HEADER	= new byte[bytes.length+1];
		
		BT_HEADER[0] = (byte)bytes.length;
		
		System.arraycopy( bytes, 0, BT_HEADER, 1, bytes.length );
	}
	
	private static boolean 	REQUIRE_CRYPTO;
	
	static{
	    COConfigurationManager.addAndFireParameterListener(
	    		"network.transport.encrypted.require",
	    		new ParameterListener()
	    		{
	    			 public void 
	    			 parameterChanged(
	    				String ignore )
	    			 {
	    				 REQUIRE_CRYPTO	= COConfigurationManager.getBooleanParameter( "network.transport.encrypted.require");
	    			 }
	    		});
	}
	
	
	private ByteBuffer	decode_buffer; 
	private int			decode_read;
	
	private long	start_time	= SystemTime.getCurrentTime();
	
	private TCPProtocolDecoderPHE	phe_decoder;
	
	private long	last_read_time		= 0;
	
	private boolean processing_complete;
	
	public
	TCPProtocolDecoderInitial(
		SocketChannel				_channel,
		boolean						_outgoing,
		TCPProtocolDecoderAdapter	_adapter )
	
		throws IOException
	{
		super( true );
		
		channel	= _channel;
		adapter	= _adapter;
		
		final TCPTransportHelper transport_helper	= new TCPTransportHelper( channel);

		final TCPTransportHelperFilterTransparent transparent_filter = new TCPTransportHelperFilterTransparent( transport_helper, false );
				
		filter	= transparent_filter;
		
		if ( _outgoing ){
				
			if ( REQUIRE_CRYPTO ){
		
				if ( TCPProtocolDecoderPHE.isCryptoOK()){
					
					decodePHE( null );
					
				}else{
				
					throw( new IOException( "Crypto required but unavailable" ));
				}
			}else{
				
				complete();
			}
			
		}else{
			
			decode_buffer = ByteBuffer.allocate( BT_HEADER.length );
			
			read_selector.register(
				channel,
				new VirtualSelectorListener()
				{
					public boolean 
					selectSuccess(
						VirtualChannelSelector	selector, 
						SocketChannel			sc, 
						Object 					attachment )
					{
						try{
							int	len = transport_helper.read( decode_buffer );
							
							if ( len < 0 ){
								
								failed( new IOException( "end of stream on socket read" ));
								
							}else if ( len == 0 ){
								
								return( false );
							}
							
							last_read_time = SystemTime.getCurrentTime();
							
							decode_read += len;
							
							if ( decode_buffer.hasRemaining()){
								
								return( true );
							}
								
							read_selector.cancel( channel );
							
							decode_buffer.flip();
							
							byte[]	bytes = new byte[decode_buffer.limit()];
							
							decode_buffer.get( bytes );
							
							decode_buffer.flip();
							
							if ( Arrays.equals( bytes, BT_HEADER )){
								
								if ( REQUIRE_CRYPTO ){
								
									throw( new IOException( "Crypto required but incoming connection is plain" ));
								}
								
								transparent_filter.insertRead( decode_buffer );
								
								complete();
								
							}else{
								
								decodePHE( decode_buffer );
							}
							
							return( true );
							
						}catch( Throwable e ){
							
							selectFailure( selector, sc, attachment, e );
							
							return( false );
						}
					}

					public void 
					selectFailure(
						VirtualChannelSelector	selector, 
						SocketChannel 			sc, 
						Object 					attachment, 
						Throwable 				msg)
					{
						read_selector.cancel( channel );
						
						failed( msg );
					}
				},
				this );
		}
	}
	
	protected void
	decodePHE(
		ByteBuffer	buffer )	
	
		throws IOException
	{
		TCPProtocolDecoderAdapter	phe_adapter = 
			new TCPProtocolDecoderAdapter()
			{
				public void
				decodeComplete(
					TCPProtocolDecoder	decoder )	
				{
					filter = decoder.getFilter();
					
					complete();
				}
				
				public void
				decodeFailed(
					TCPProtocolDecoder	decoder,
					Throwable			cause )
				{
					failed( cause );
				}
			};
		
		phe_decoder = new TCPProtocolDecoderPHE( channel, buffer, phe_adapter );
	}
	
	public boolean
	isComplete(
		long	now )
	{
		if ( !processing_complete ){
		
			if ( start_time > now ){
				
				start_time	= now;
			}
			
			if ( last_read_time > now ){
				
				last_read_time	= now;
			}
			
			if ( phe_decoder != null ){
				
				last_read_time	= phe_decoder.getLastReadTime();
			}
				
			long	timeout;
			long	time;
			
			if ( last_read_time == 0 ){
				
				timeout = IncomingSocketChannelManager.CONNECT_TIMEOUT;
				time	= start_time;
				
			}else{
				
				timeout = IncomingSocketChannelManager.READ_TIMEOUT;
				time	= last_read_time;
			}
			
			if ( now - time > timeout ){
				
				try{
					read_selector.cancel( channel );
					
					write_selector.cancel( channel );
											
				}catch( Throwable e ){
					
				}
				
				String	phe_str = "";
				
				if ( phe_decoder != null ){
					
					phe_str = ", crypto: " + phe_decoder.getString();
				}
				
		       	if ( Logger.isEnabled()){
		       		
					Logger.log(new LogEvent(LOGID, "Incoming TCP connection ["
							+ channel + "] forcibly timed out after "
							+ timeout/1000 + "sec due to socket inactivity"));
		       	}
		       	
				failed( new Throwable( "Protocol decode aborted: timed out after " + timeout/1000+ "sec: " + decode_read + " bytes read" + phe_str ));
			}
		}
		
		return( processing_complete );
	}
	
	public TCPTransportHelperFilter
	getFilter()
	{
		return( filter );
	}
	
	protected void
	complete()
	{
		if ( !processing_complete ){
			
			processing_complete	= true;
			
			adapter.decodeComplete( this );
		}
	}
	
	protected void
	failed(
		Throwable	reason )
	{
		if ( !processing_complete ){
			
			processing_complete	= true;
			
			adapter.decodeFailed( this, reason );
		}
	}
}
