/*
 * Created on 19-Jan-2006
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

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;


public abstract class 
TCPTransportHelperFilterStream 
	implements TCPTransportHelperFilter
{
	private TCPTransportHelper		transport;

	private DirectByteBuffer	write_buffer_pending_db;
	private ByteBuffer			write_buffer_pending_byte;
	
	protected
	TCPTransportHelperFilterStream(
		TCPTransportHelper		_transport )
	{
		transport	= _transport;
	}
	
	public long 
	write( 
		ByteBuffer[] 	buffers, 
		int 			array_offset, 
		int 			length ) 
	
		throws IOException
	{		
			// deal with any outstanding cached crypted data first
		
		if  ( write_buffer_pending_byte != null ){
			
			if ( transport.write( write_buffer_pending_byte ) == 0 ){
				
				return( 0 );
			}
			
			write_buffer_pending_byte	= null;
		}
		
		long	total_written = 0;
		
		if ( write_buffer_pending_db != null ){
			
			ByteBuffer	write_buffer_pending = write_buffer_pending_db.getBuffer( DirectByteBuffer.SS_NET );
			
			int	max_writable = 0;
			
			for (int i=array_offset;i<array_offset+length;i++){
				
				ByteBuffer	source_buffer = buffers[i];
				
				int	position 	= source_buffer.position();
				int	limit		= source_buffer.limit();
				
				int	size = limit - position;
				
				max_writable	+= size;
			}
			
			int	pending_position 	= write_buffer_pending.position();
			int pending_limit		= write_buffer_pending.limit();
			
			int	pending_size = pending_limit - pending_position;
			
			if ( pending_size > max_writable ){
								
				pending_size = max_writable;
				
				write_buffer_pending.limit( pending_position + pending_size );
			}
			
			int	written = transport.write( write_buffer_pending );
			
			if ( written > 0 ){
				
				total_written = written;
				
				write_buffer_pending.limit( pending_limit );
				
				if ( write_buffer_pending.remaining() == 0 ){
					
					write_buffer_pending_db.returnToPool();
					
					write_buffer_pending_db	= null;
				}
				
					// skip "written" bytes in the source
				
				int skip = written;
				
				for (int i=array_offset;i<array_offset+length;i++){
					
					ByteBuffer	source_buffer = buffers[i];
					
					int	position 	= source_buffer.position();
					int	limit		= source_buffer.limit();
					
					int	size = limit - position;
					
					if ( size <= skip ){
						
						source_buffer.position( limit );
						
						skip	-= size;
						
					}else{
						
						source_buffer.position( position + skip );
						
						skip	= 0;
						
						break;
					}
				}
				
				if ( skip != 0 ){
					
					throw( new IOException( "skip inconsistent - " + skip ));
				}
			}
			
				// if write came up short or we've filled the source buffer then we can't do
				// any more 
			
			if ( total_written < pending_size || total_written == max_writable ){
				
				return( total_written );
			}
		}
		
			// problem - we must only crypt stuff once and when crypted it *has*
			// to be sent (else the stream will get out of sync).
			// so we have to turn this into single buffer operations
		
		for (int i=array_offset;i<array_offset+length;i++){
			
			ByteBuffer	source_buffer = buffers[i];
			
			int	position 	= source_buffer.position();
			int	limit		= source_buffer.limit();
			
			int	size = limit - position;
			
			if ( size == 0 ){
				
				continue;
			}
			
			DirectByteBuffer	target_buffer_db = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_NET_CRYPT,  size );
			
			try{
				ByteBuffer	target_buffer = target_buffer_db.getBuffer( DirectByteBuffer.SS_NET );
			
				write( source_buffer, target_buffer );
				
				target_buffer.position( 0 );
				
				int	written = transport.write( target_buffer );
				
				total_written += written;
				
				if ( written < size ){
					
					source_buffer.position( position + written );
					
					write_buffer_pending_db	= target_buffer_db;
					
					target_buffer_db	= null;
					
					if ( written == 0 ){
						
							// we gotta pretend at least 1 byte was written to
							// guarantee that the caller writes the rest
											
						write_buffer_pending_byte = ByteBuffer.wrap(new byte[]{target_buffer.get()});
													
						source_buffer.get();
						
						total_written++;
					}
					
					break;
				}
			}finally{
				
				if ( target_buffer_db != null ){
					
					target_buffer_db.returnToPool();
				}
			}
		}
		
		return( total_written );
	}

	public long 
	read( 
		ByteBuffer[] 	buffers, 
		int 			array_offset, 
		int 			length ) 
	
		throws IOException
	{		
		DirectByteBuffer[]	copy_db = new DirectByteBuffer[buffers.length];
		ByteBuffer[]		copy	= new ByteBuffer[buffers.length];
		
		try{
			for (int i=array_offset;i<array_offset+length;i++){
				
				ByteBuffer	buffer = buffers[i];
				
				int	size = buffer.remaining();
				
				if ( size > 0 ){
					
					copy_db[i]	= DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_NET_CRYPT, size );
					
					copy[i]	= copy_db[i].getBuffer( DirectByteBuffer.SS_NET );
				}else{
					
					copy[i] = ByteBuffer.allocate(0);
				}
			}
			
			long	read = transport.read( copy, array_offset, length );
			
			for (int i=array_offset;i<array_offset+length;i++){
	
				ByteBuffer	source_buffer	= copy[i];

				if ( source_buffer != null ){
					
					ByteBuffer	target_buffer 	= buffers[i];
					
					int	source_position	= source_buffer.position();
								
					if ( source_position > 0 ){
						
						source_buffer.flip();
						
						read( source_buffer, target_buffer );
					}
				}
			}
			
			return( read );
			
		}finally{
			
			for (int i=0;i<copy_db.length;i++){
				
				if ( copy_db[i] != null ){
					
					copy_db[i].returnToPool();
				}
			}
		}
	}
	
	protected abstract void
	write(
		ByteBuffer	source_buffer,
		ByteBuffer	target_buffer )
	
		throws IOException;
	
	protected abstract void
	read(
		ByteBuffer	source_buffer,
		ByteBuffer	target_buffer )
	
		throws IOException;
	
	public SocketChannel
	getSocketChannel()
	{
		return( transport.getSocketChannel());
	}
}
