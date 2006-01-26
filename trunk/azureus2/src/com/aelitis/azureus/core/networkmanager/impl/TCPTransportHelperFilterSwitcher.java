/*
 * Created on 26-Jan-2006
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

public class 
TCPTransportHelperFilterSwitcher 
	implements TCPTransportHelperFilter
{
	private TCPTransportHelperFilter	current_reader;
	private TCPTransportHelperFilter	current_writer;
	
	private TCPTransportHelperFilter	second_filter;
	
	private int	read_rem;
	private int	write_rem;
	
	public
	TCPTransportHelperFilterSwitcher(
		TCPTransportHelperFilter	_filter1,
		TCPTransportHelperFilter	_filter2,
		int							_switch_read,
		int							_switch_write )
	{
		read_rem	= _switch_read;
		write_rem	= _switch_write;
		
		current_reader	= read_rem<=0?_filter2:_filter1;
		current_writer	= write_rem<=0?_filter2:_filter1;
		
		second_filter	= _filter2;
	}
	
	public long 
	write( 
		ByteBuffer[] 	buffers, 
		int 			array_offset, 
		int 			length ) 
	
		throws IOException
	{
		long	total_written	= 0;
		
		if ( current_writer != second_filter ){
			
			int[]	limits = new int[buffers.length];
			
			int	to_write	= write_rem;
			
			for (int i=array_offset;i<array_offset+length;i++){
				
				ByteBuffer	buffer = buffers[i];
				
				limits[i]	= buffer.limit();
				
				int	rem = buffer.remaining();
				
				if ( rem > to_write ){
					
					buffer.limit( buffer.position() + to_write );
					
					to_write = 0;
					
				}else{
					
					to_write	-= rem;
				}
			}
			
			try{
				
				total_written = current_writer.write( buffers, array_offset, length );
				
				if ( total_written <= 0 ){
					
					return( total_written );
				}
			}finally{
				
				for (int i=array_offset;i<array_offset+length;i++){
					
					ByteBuffer	buffer = buffers[i];
					
					buffer.limit( limits[i] );
				}
			}
			
			write_rem -= total_written;
			
			if ( write_rem == 0 ){
				
					// writer may have data buffered up pending next write call - if so then
					// we need to get out now
				
				if ( !current_writer.isFlushed()){
					
					return( total_written );
				}
								
				current_writer	= second_filter;
				
			}else{
				
				return( total_written );
			}
		}
		
		total_written += current_writer.write( buffers, array_offset, length );
		
		return( total_written );
	}

	public long 
	read( 
		ByteBuffer[] 	buffers, 
		int 			array_offset, 
		int 			length ) 
	
		throws IOException
	{
		long	total_read	= 0;
		
		if ( current_reader != second_filter ){
			
			int[]	limits = new int[buffers.length];
			
			int	to_read	= read_rem;
			
			for (int i=array_offset;i<array_offset+length;i++){
				
				ByteBuffer	buffer = buffers[i];
				
				limits[i]	= buffer.limit();
				
				int	rem = buffer.remaining();
				
				if ( rem > to_read ){
					
					buffer.limit( buffer.position() + to_read );
					
					to_read = 0;
					
				}else{
					
					to_read	-= rem;
				}
			}
			
			try{
				
				total_read = current_reader.read( buffers, array_offset, length );
				
				if ( total_read <= 0 ){
					
					return( total_read );
				}
				
			}finally{
				
				for (int i=array_offset;i<array_offset+length;i++){
					
					ByteBuffer	buffer = buffers[i];
					
					buffer.limit( limits[i] );
				}
			}
			
			read_rem -= total_read;
			
			if ( read_rem == 0 ){
				
				current_reader	= second_filter;
				
			}else{
				
				return( total_read );
			}
		}
		
		total_read += current_reader.read( buffers, array_offset, length );
		
		return( total_read );		
	}
	
	public boolean
	isFlushed()
	{
		return( current_writer.isFlushed());
	}
	
	public SocketChannel
	getSocketChannel()
	{
		return( second_filter.getSocketChannel());
	}
	
	public String
	getName()
	{
		return( second_filter.getName());
	}
}
