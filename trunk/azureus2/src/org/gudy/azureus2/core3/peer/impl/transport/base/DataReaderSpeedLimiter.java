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

public class 
DataReaderSpeedLimiter 
{
	protected int	bytes_per_second = 5000;
	protected int	slot_count		 = 20;
	
	protected int[]	slots = new int[slot_count];
	protected int	bytes_per_slot	= bytes_per_second/slot_count;
	protected int	current_slot;
	
	protected int	slot_period_millis	= 1000/slot_count;
	protected long	last_read_time;
	
	
	protected static DataReaderSpeedLimiter		singleton = new DataReaderSpeedLimiter();
	
	public static DataReaderSpeedLimiter
	getSingleton()
	{
		return( singleton );
	}
	
	public DataReader
	getDataReader(
		Object		owner )
	{
		return( new unlimitedDataReader());
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
			int	bytes_available = 0;
			
			synchronized( DataReaderSpeedLimiter.this ){
				
				long	now = SystemTime.getCurrentTime();
				
				
				int		slots = (int)( ( now - last_read_time )/slot_period_millis );
				
				if ( slots > slot_count ){
					
					slots = slot_count;
				}
				
				last_read_time	= now;
			}
			
			if ( bytes_available == 0 ){
				
				return( 0 );
			}
			
			ByteBuffer	buffer = direct_buffer.buff;
				
			int	position	= buffer.position();
			int limit		= buffer.limit();
			
			if ( limit - position > bytes_available ){
				
				buffer.limit( position + bytes_available );
			}
			
			try{
				
				int	len = channel.read(buffer);
								
				return( len );
				
			}finally{
			
				buffer.limit( limit );
			}
		}
		
		public void
		destroy()
		{
		}
	}
}
