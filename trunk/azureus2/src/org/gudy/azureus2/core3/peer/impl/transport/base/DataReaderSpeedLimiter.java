/*
 * Created on 30-Apr-2004
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

package org.gudy.azureus2.core3.peer.impl.transport.base;

/**
 * @author parg
 *
 */

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.config.*;

public class 
DataReaderSpeedLimiter 
{

	protected final int	slot_period_millis	= (int)(SystemTime.TIME_GRANULARITY_MILLIS+5);
	protected final int	slot_count			= 1000/slot_period_millis;
	
	protected int	bytes_per_second = 0;	// global bytes-per-second limit

	protected int	bytes_per_slot;			// current global bytes-per-slot
	
	protected long	current_slot;			// current (last) slot used
	protected int	bytes_available;		// bytes available unused by current slot
	
	
	
	protected static DataReaderSpeedLimiter		singleton = new DataReaderSpeedLimiter();
	
	public static DataReaderSpeedLimiter
	getSingleton()
	{
		return( singleton );
	}
	
	protected
	DataReaderSpeedLimiter()
	{
		COConfigurationManager.addParameterListener(
				"Max Download Speed KBs",
				new ParameterListener()
				{
					public void
					parameterChanged(
						String	str )
					{
						bytes_per_second = COConfigurationManager.getIntParameter( "Max Download Speed KBs", 0 ) * 1024;						
					
						bytes_per_slot	= bytes_per_second/slot_count;
					}
				});
		
		bytes_per_second = COConfigurationManager.getIntParameter( "Max Download Speed KBs", 0 ) * 1024;
		
		bytes_per_slot	= bytes_per_second/slot_count;
	}
	
	public DataReader
	getDataReader(
		DataReaderOwner		owner )
	{
		return( new limitedDataReader( owner ));
	}
	
	protected class
	unlimitedDataReader
		implements DataReader
	{
		public int
		read(
			SocketChannel		channel,
			DirectByteBuffer	direct_buffer )
		
			throws IOException
		{
			return( direct_buffer.read(channel));
		}
		
		public void
		destroy()
		{
		}
	}
	
	protected class
	limitedDataReader
		implements DataReader
	{
		protected DataReaderOwner	owner;
				
		protected long	my_current_slot;		
		protected int	my_bytes_available;
		
		protected
		limitedDataReader(
		    DataReaderOwner	_owner )
		{
			owner		= _owner;
		}
		
		public int
		read(
			SocketChannel		channel,
			DirectByteBuffer	direct_buffer )
		
			throws IOException
		{
			int	my_bytes_per_second = owner.getMaximumBytesPerSecond();
						
			if ( bytes_per_second == 0 && my_bytes_per_second == 0 ){

					// unlimited
				
				return( direct_buffer.read( channel ));
			}
			
			int	position	= direct_buffer.position();
			int limit		= direct_buffer.limit();
		
			int bytes_allocated	= 0;

			int	debug_max_bytes	= -1;
			int debug_limit		= -1;
			
			try{
				synchronized( DataReaderSpeedLimiter.this ){
					
					long	now = SystemTime.getCurrentTime();
					
					long	new_slot = now/slot_period_millis;
					
						// do the global limit first
					
					if ( bytes_per_second > 0 ){
			
						long	slots = new_slot - current_slot;
						
						current_slot	= new_slot;
						
						if ( slots < 0 ){
							
							// someone must have changed the clock, reset our position in time
							
							return( 0 );
						}
						
						if ( slots > slot_count ){
							
							slots = slot_count;
						}
						
						bytes_available += slots*bytes_per_slot;
						
							// give a bit of slack for bursty transfers
						
						if ( bytes_available > (3*bytes_per_second )){
							
							bytes_available = 3*bytes_per_second;
						}
						
						if ( bytes_available == 0 ){
							
							return( 0 );
						}
					}else{
						
						bytes_available	= 0;
					}
	
						// we've got access to a "global" amount of bytes we can read
						// now apply specific policy if required
				
					if ( my_bytes_per_second > 0 ){
						
						long	my_slots = new_slot - my_current_slot;
						
						my_current_slot	= new_slot;
									
						if ( my_slots < 0 ){
										
							// someone must have changed the clock, reset our position in time
										
							return( 0 );
						}
									
						if ( my_slots > slot_count ){
										
							my_slots = slot_count;
						}
							
						int my_bytes_per_slot	= my_bytes_per_second/slot_count;
						
						my_bytes_available += my_slots*my_bytes_per_slot;
									
							// give a bit of slack for bursty transfers
									
						if (  my_bytes_available > (3*my_bytes_per_second )){
										
							my_bytes_available = 3*my_bytes_per_second;
						}
									
						if ( my_bytes_available == 0 ){
										
							return( 0 );
						}
					}else{
						
						my_bytes_available	= 0;
					}
						
						// bytes_available: 	0 -> unlimited
						// my_bytes_available: 	0 -> unlimited
					
					int	max_bytes;
					
					if ( bytes_available  == 0 && my_bytes_available == 0 ){
						
						max_bytes = 0;
						
					}else if ( bytes_available == 0 ){
						
						max_bytes = my_bytes_available;
						
					}else if ( my_bytes_available == 0 ){
						
						max_bytes = bytes_available;
						
					}else{
						
						max_bytes	= bytes_available < my_bytes_available?bytes_available:my_bytes_available;
					}
					
					int request_read_size = limit - position;
					
						// now limit the read based on any restrictions
					
					debug_max_bytes	= max_bytes;
					
					if ( max_bytes != 0 && request_read_size > max_bytes ){
					
						debug_limit = position + max_bytes;
						
						direct_buffer.limit( position + max_bytes );
						
						bytes_allocated	= max_bytes;
						
					}else{
						
						bytes_allocated = request_read_size;
					}
					
					my_bytes_available	-= bytes_allocated;
					
					if ( my_bytes_available < 0  ){
						
						my_bytes_available	= 0;
					}
					
					bytes_available		-= bytes_allocated;
					
					if ( bytes_available < 0 ){
						
						bytes_available	= 0;
					}
				}
							
				int	bytes_read = 0;
				
				try{
					
					bytes_read = direct_buffer.read(channel);
	
					return( bytes_read );
					
				}finally{
									
					if ( bytes_read < bytes_allocated ){
						
						synchronized( DataReaderSpeedLimiter.this ){
	
							bytes_available 	+= ( bytes_allocated - bytes_read );
							my_bytes_available 	+= ( bytes_allocated - bytes_read );
						}
					}
					
					direct_buffer.limit( limit );
				}
			}catch( IllegalArgumentException e ){
				
				System.out.println( "Illegal arg exception" );
				
				System.out.println( "buffer: " + direct_buffer.position() + "/" + direct_buffer.limit());
				System.out.println( "    start values:" + position + "/" + limit );
				System.out.println( "    alloc = " + bytes_allocated + ", ba = " + bytes_available + ", mba = " + my_bytes_available );
				System.out.println( "    max_bytes = " + debug_max_bytes + ", limit = " + debug_limit );
				throw( e );
			}
		}
		
		public void
		destroy()
		{
		}
	}
}
