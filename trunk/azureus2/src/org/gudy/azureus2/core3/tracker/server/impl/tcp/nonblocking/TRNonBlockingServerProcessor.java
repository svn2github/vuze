/*
 * Created on 02-Jan-2005
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

package org.gudy.azureus2.core3.tracker.server.impl.tcp.nonblocking;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.gudy.azureus2.core3.tracker.server.impl.tcp.TRTrackerServerProcessorTCP;
import org.gudy.azureus2.core3.tracker.server.impl.tcp.TRTrackerServerTCP;
import org.gudy.azureus2.core3.util.SystemTime;

/**
 * @author parg
 *
 */

public class 
TRNonBlockingServerProcessor 
	extends TRTrackerServerProcessorTCP
{
	private static final int			READ_BUFFER_INITIAL		= 1024;
	private static final int			READ_BUFFER_INCREMENT	= 1024;
	private static final int			READ_BUFFER_LIMIT		= 8192;
		
	private SocketChannel				socket_channel;
	
	private long						start_time;
	
	private ByteBuffer					read_buffer;
	private String						request_header;
	
	private ByteBuffer					write_buffer;
	
	protected
	TRNonBlockingServerProcessor(
		TRTrackerServerTCP		_server,
		SocketChannel			_socket )
	{
		super( _server );
		
		socket_channel		= _socket;
		
		start_time	= SystemTime.getCurrentTime(); 
		
		read_buffer = ByteBuffer.allocate( READ_BUFFER_INITIAL );
	}

	protected int
	processRead()
	{
		if ( read_buffer.remaining() == 0 ){
			
			int	capacity = read_buffer.capacity();
			
			if ( capacity == READ_BUFFER_LIMIT ){
				
				return( -1 );
				
			}else{
				
				read_buffer.position(0);
				
				byte[]	data = new byte[capacity];
				
				read_buffer.get( data );
				
				read_buffer = ByteBuffer.allocate( capacity + READ_BUFFER_INCREMENT );
				
				read_buffer.put( data );
			}
		}
		
		try{
			int	len = socket_channel.read( read_buffer );
			
			if ( len <= 0 ){
				
				return( -1 );
			}
			
			byte[]	data = read_buffer.array();
						
			for (int i=read_buffer.position()-4;i>=0;i--){
				
				if ( 	data[i]   == CR &&
						data[i+1] == FF &&
						data[i+2] == CR &&
						data[i+3] == FF ){
					
					request_header = new String(data,0,read_buffer.position());
					
					getServer().runProcessor( this );
					
					return(0);				
				}
			}
			
			return( len );
			
		}catch( IOException e ){
			
			return( -1 );
		}
	}
	
	protected int
	processWrite()
	{
		if ( write_buffer == null ){
			
			return( -1 );
		}
		
		if ( !write_buffer.hasRemaining()){
			
			return( 0 );
		}
		
		try{
			socket_channel.write( write_buffer );
			
			if ( write_buffer.hasRemaining()){
				
				return( 1 );
			}
			
			return( 0 );
			
		}catch( IOException e ){
			
			return( -1 );
		}
	}
	
	public void
	runSupport()
	{
		try{
			String	url = request_header.substring(4).trim();
			
			int	pos = url.indexOf( " " );
									
			url = url.substring(0,pos);
	
			ByteArrayOutputStream	response = new ByteArrayOutputStream(1024);
			
			processRequest( request_header,
							request_header.toLowerCase(),
							url, 
							socket_channel.socket().getInetAddress().getHostAddress(),
							true,
							new ByteArrayInputStream(new byte[0]),
							response );
			
			write_buffer = ByteBuffer.wrap( response.toByteArray());
			
		}catch( Throwable e ){
			
			
		}finally{
			
			((TRNonBlockingServer)getServer()).readyToWrite( this );
		}
	}
	
	protected SocketChannel
	getSocketChannel()
	{
		return( socket_channel );
	}
	
	protected long
	getStartTime()
	{
		return( start_time );
	}
	
	public void
	interruptTask()
	{
	}
}
