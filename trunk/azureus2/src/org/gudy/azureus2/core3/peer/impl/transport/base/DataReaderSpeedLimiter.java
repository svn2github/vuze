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
	protected int	bytes_per_second = 0;

	protected int	slot_period_millis	= (int)(SystemTime.TIME_GRANULARITY_MILLIS+5);
	protected int	slot_count			= 1000/slot_period_millis;
	
	protected int	bytes_per_slot;
	
	protected long	current_slot;
	protected int	bytes_available;
	
	
	
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
		Object		owner )
	{
		return( new limitedDataReader());
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
			ByteBuffer	buffer = direct_buffer.buff;
						
			return( channel.read(buffer));
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
		public int
		read(
			SocketChannel		channel,
			DirectByteBuffer	direct_buffer )
		
			throws IOException
		{
			ByteBuffer	buffer = direct_buffer.buff;
			
			if ( bytes_per_second == 0 ){

					// unlimited
				
				return( channel.read(buffer));
			}
			
			int	position	= buffer.position();
			int limit		= buffer.limit();
		
			int bytes_allocated;

			synchronized( DataReaderSpeedLimiter.this ){
				
				long	now = SystemTime.getCurrentTime();
				
				long	new_slot = now/slot_period_millis;
				
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
				
				int request_read_size = limit - position;
								
				if ( request_read_size > bytes_available ){
					
					buffer.limit( position + bytes_available );
					
					bytes_allocated	= bytes_available;
					
				}else{
					
					bytes_allocated = request_read_size;
				}
				
				bytes_available	-= bytes_allocated;
			}
			
			int	bytes_read = 0;
			
			try{
				
				bytes_read = channel.read(buffer);

				return( bytes_read );
				
			}finally{
				
				if ( bytes_read < bytes_allocated ){
					
					synchronized( DataReaderSpeedLimiter.this ){

						bytes_available += ( bytes_allocated - bytes_read );
					}
				}
				
				buffer.limit( limit );
			}
		}
		
		public void
		destroy()
		{
		}
	}
}
