/*
 * Created on Apr 21, 2004
 * Created by Alon Rohter
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

package org.gudy.azureus2.core3.util;

import java.io.IOException;
import java.lang.ref.Reference;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;

import java.util.*;

/**
 * Virtual direct byte buffer given out and tracker
 * by the buffer pool.
 */

public class 
DirectByteBuffer 
{	
	public static final byte		AL_NONE			= 0;
	public static final byte		AL_EXTERNAL		= 1;
	public static final byte		AL_OTHER		= 2;
	public static final byte		AL_PT_READ		= 3;
	public static final byte		AL_PT_LENGTH	= 4;
	public static final byte		AL_CACHE_READ	= 5;
	public static final byte		AL_DM_READ		= 6;
	public static final byte		AL_DM_ZERO		= 7;
	public static final byte		AL_DM_CHECK		= 8;
	public static final byte		AL_BT_PIECE		= 9;
	
	public static final String[] AL_DESCS =
	{ "NO", "EX", "OT", "PR", "PL", "CR", "DR", "DZ", "DC", "BP" };
	
	public static final byte		SS_EXTERNAL		= 0;
	public static final byte		SS_OTHER		= 1;
	public static final byte		SS_CACHE		= 2;
	public static final byte		SS_FILE			= 3;
	public static final byte		SS_NET			= 4;
	public static final byte		SS_BT			= 4;
	public static final byte		SS_DR			= 4;
	public static final byte		SS_DW			= 4;
	public static final byte		SS_PEER			= 4;

	
	
	protected static final boolean	TRACE		= false;
	
	private ByteBuffer 				buffer;
	private DirectByteBufferPool	pool;
	private byte					allocator;
  
	public 
	DirectByteBuffer( 
		ByteBuffer 	_buffer ) 
	{
		this( AL_NONE, _buffer, null );
	}
	
	public 
	DirectByteBuffer( 
		byte					_allocator,
		ByteBuffer 				_buffer,
		DirectByteBufferPool	_pool ) 
	{
		allocator	= _allocator;
		buffer 		= _buffer;
		pool		= _pool;
		
		if ( TRACE ){
			/*
			trace_list		= new LinkedList();
			global_trace	= new LinkedList();
			
			trace_wrapper = new traceWrapper( buffer );
			*/
		}
	}
  
	protected void
	traceUsage(
		String	function )
	{
	
	}
	
	protected void
	dumpTrace(
		Throwable 	e )
	{
		if ( TRACE ){
		}
	}
	
	protected ByteBuffer
	getBufferInternal()
	{
		return( buffer );
	}
	
	protected byte
	getAllocator()
	{
		return( allocator );
	}
	
	
	
	
		// **** accessor methods  ****
	
	public int
	limit(
		byte		subsystem )
	{
		if ( TRACE ){
			traceUsage("limit");
		}
		
		return( buffer.limit());
	}
  
	public void
	limit(
		byte		subsystem,
		int			l )
	{
		if ( TRACE ){
			
			traceUsage("limit(int)");
		}
		
		buffer.limit(l);
	}
  
	public int
	position(
		byte		subsystem )
	{
		if ( TRACE ){
			traceUsage("position");
		}
		
	  	return( buffer.position());
	}
  
	public void
	position(
		byte		subsystem,
		int			l )
	{
		if ( TRACE ){
			traceUsage("position(int)");
		}
		
		buffer.position(l);
	}
  
	public void
	clear(
		byte		subsystem) 
	{
		if ( TRACE ){
			traceUsage("clear");
		}
		
		buffer.clear();
	}
  
	public void
	flip(
		byte		subsystem )
	{
		if ( TRACE ){
			traceUsage("flip");
		}
		
		buffer.flip();
	}
  
	public int
	remaining(
		byte		subsystem )
	{
		if ( TRACE ){
			traceUsage("remaining");
		}
		
		return( buffer.remaining());
	}
  
	public int
	capacity(
		 byte		subsystem )
	{
		if ( TRACE ){
			traceUsage("capacity");
		}
		
		return( buffer.capacity());
	}
  
	public void
	put(
		byte		subsystem,
		byte[]		data )
	{
		if ( TRACE ){
			traceUsage("put(byte[])");
		}
		
		buffer.put( data );
	}
  
	public void
	put(
		byte				subsystem,
		DirectByteBuffer	data )
	{
		if ( TRACE ){
			traceUsage("put(DBB)");
		}
		
		buffer.put( data.buffer );
	}
  
	public void
	put(
		byte		subsystem,
		ByteBuffer	data )
	{
		if ( TRACE ){
			traceUsage("put(BB)");
		}
		
		buffer.put( data );
	}
  
	public void
	put(
		byte	subsystem,
		byte	data )
	{
		if ( TRACE ){
			traceUsage("put(byte)");
		}
		
		buffer.put( data );
	}
  
	public void
	putInt(
		byte		subsystem,
		int			data )
	{
		if ( TRACE ){
			traceUsage("put(int)");
		}
		
		buffer.putInt( data );
	}
  
	public byte
	get(
		byte		subsystem )
	{
		if ( TRACE ){
			traceUsage("get");
		}
		
		return( buffer.get());
	}
	
	public byte
	get(
		byte	subsystem,
		int		x )
	{
		if ( TRACE ){
			traceUsage("get(int)");
		}
		
		return( buffer.get(x));
	}
  
	public void
	get(
		byte		subsystem,
		byte[]		data )
	{
		if ( TRACE ){
			traceUsage("get(byte[])");
		}
		
		buffer.get(data);
	}
  
	public int
	getInt(
		byte		subsystem )
	{
		if ( TRACE ){
			traceUsage("getInt");
		}
		
		return( buffer.getInt());
	}
  
	public int
	getInt(
		byte		subsystem,
		int			x )
	{
		if ( TRACE ){
			traceUsage("getInt(int)");
		}
		
		return( buffer.getInt(x));
	}
  
	public boolean
	hasRemaining(
		byte		subsystem )
	{
		if ( TRACE ){
			traceUsage("hasRemaining");
		}
		
		return( buffer.hasRemaining());
	}
  
	public int
	read(
		byte		subsystem,
		FileChannel	chan )
  
		throws IOException
	{
		if ( TRACE ){
			traceUsage("read(FC)");
		}
		
		try{
			return( chan.read(buffer ));
			
		}catch( IllegalArgumentException e ){
			
			dumpTrace(e);
			
			throw( e );
		}
	}
  
	public int
	write(
		byte		subsystem,
		FileChannel	chan )
  
		throws IOException
	{
		if ( TRACE ){
			traceUsage("write(FC)");
		}
		
		try{
			return( chan.write(buffer ));
			
		}catch( IllegalArgumentException e ){
			
			dumpTrace(e);
			
			throw( e );
		}
	}
  
	public int
	read(
		byte			subsystem,
		SocketChannel	chan )
  
		throws IOException
	{
		if ( TRACE ){
			traceUsage("read(SC)");
		}
		
		try{
			return( chan.read(buffer ));
			
		}catch( IllegalArgumentException e ){
			
			dumpTrace(e);
			
			throw( e );
		}
	}
  
	public int
	write(
		byte			subsystem,
		SocketChannel	chan )
  
  		throws IOException
	{
		if ( TRACE ){
			traceUsage("write(SC)");
		}
		
		try{
			return( chan.write(buffer ));
			
		}catch( IllegalArgumentException e ){
			
			dumpTrace(e);
			
			throw( e );
		}
	}
  
	public ByteBuffer
	getBuffer(
		byte		subsystem )
	{
		if ( TRACE ){
			traceUsage("getBuffer");
		}
		
		return( buffer );
	}
  

	public void 
	returnToPool() 
	{
		if ( TRACE ){
			
			traceUsage("returnToPool");
		
			// System.out.println( "free:" + buffer );

			// trace_buffer_map.remove( buffer );
		}
		
		if ( pool != null ){
			
			if ( DirectByteBufferPool.DEBUG_TRACK_HANDEDOUT ){
				
				synchronized( this ){
					
					if ( buffer == null ){
						
						Debug.out( "Buffer already returned to pool");
						
					}else{
		    	
						pool.returnBuffer( buffer );
						
						buffer	= null;
					}
				}
			}else{
				
				if ( buffer == null ){
					
					Debug.out( "Buffer already returned to pool");
					
				}else{
	    	
					pool.returnBuffer( buffer );
					
					buffer	= null;
				}
				
			}
		}
	}
}
