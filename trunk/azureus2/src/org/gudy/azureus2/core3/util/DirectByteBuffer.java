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
	protected static final boolean	TRACE		= false;
	
	//protected List			trace_list;
	//protected StringBuffer	spare_trace_buffer;
	//protected traceWrapper	trace_wrapper;
	//protected static List		global_trace;
	//protected static Map		trace_buffer_map = new WeakHashMap();
	
	private ByteBuffer buffer;
  
	private DirectByteBufferPool	 pool;
  
	public 
	DirectByteBuffer( 
		ByteBuffer _buffer ) 
	{
		this( _buffer, null );
	}
	
	public 
	DirectByteBuffer( 
		ByteBuffer 				_buffer,
		DirectByteBufferPool	_pool ) 
	{
		buffer 	= _buffer;
		pool	= _pool;
		
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
		if ( TRACE ){
		
			/*
			Thread	t = Thread.currentThread();
			
			StringBuffer	buffer = spare_trace_buffer==null?new StringBuffer(100):spare_trace_buffer;
			
			buffer.append( t.getName());
			buffer.append("/");
			buffer.append( t.hashCode());
			buffer.append( ":" );
			buffer.append( System.currentTimeMillis());
			buffer.append( ":" );
			buffer.append( function );
			
			trace_list.add( buffer );
			
			if ( trace_list.size() > 32 ){
				
				spare_trace_buffer = (StringBuffer)trace_list.remove(0);
				
				spare_trace_buffer.setLength(0);
			}
			
			synchronized( global_trace ){
				
				global_trace.add( this );
			
				if ( global_trace.size() > 200 ){
				
					global_trace.remove(0);
				}
			}
			*/
		}
	}
	
	protected void
	dumpTrace(
		Throwable 	e )
	{
		if ( TRACE ){
			/*
			synchronized( global_trace ){
	
				e.printStackTrace();
				
				System.out.println( "**** TRACE ****" );
				
				for (int i=0;i<trace_list.size();i++){
					
					StringBuffer	f = (StringBuffer)trace_list.get(i);
					
					System.out.println( "    " + f.toString());
				}
				
				for (int i=0;i<global_trace.size();i++){
					
					DirectByteBuffer	dbb = (DirectByteBuffer)global_trace.get(i);
					
					if ( dbb != this ){
						
						if ( dbb.buffer == buffer ){
							
							System.out.println( "**** duplicate buffer ****" );
														
							List	other_trace_list = dbb.trace_list; 
								
							for (int j=0;j<other_trace_list.size();j++){
								
								StringBuffer	f = (StringBuffer)other_trace_list.get(j);
								
								System.out.println( "        " + f.toString());
							}
						}
					}
				}
			}
			*/
		}
	}
	
	public int
	limit()
	{
		if ( TRACE ){
			traceUsage("limit");
		}
		
		return( buffer.limit());
	}
  
	public void
	limit(
		int	l )
	{
		if ( TRACE ){
			
			traceUsage("limit(int)");
		}
		
		buffer.limit(l);
	}
  
	public int
	position()
	{
		if ( TRACE ){
			traceUsage("position");
		}
		
	  	return( buffer.position());
	}
  
	public void
	position(
		int	l )
	{
		if ( TRACE ){
			traceUsage("position(int)");
		}
		
		buffer.position(l);
	}
  
	public void
	clear()
	{
		if ( TRACE ){
			traceUsage("clear");
		}
		
		buffer.clear();
	}
  
	public void
	flip()
	{
		if ( TRACE ){
			traceUsage("flip");
		}
		
		buffer.flip();
	}
  
	public int
	remaining()
	{
		if ( TRACE ){
			traceUsage("remaining");
		}
		
		return( buffer.remaining());
	}
  
	public int
	capacity()
	{
		if ( TRACE ){
			traceUsage("capacity");
		}
		
		return( buffer.capacity());
	}
  
	public void
	put(
		byte[]	data )
	{
		if ( TRACE ){
			traceUsage("put(byte[])");
		}
		
		buffer.put( data );
	}
  
	public void
	put(
		DirectByteBuffer	data )
	{
		if ( TRACE ){
			traceUsage("put(DBB)");
		}
		
		buffer.put( data.buffer );
	}
  
	public void
	put(
		ByteBuffer	data )
	{
		if ( TRACE ){
			traceUsage("put(BB)");
		}
		
		buffer.put( data );
	}
  
	public void
	put(
		byte	data )
	{
		if ( TRACE ){
			traceUsage("put(byte)");
		}
		
		buffer.put( data );
	}
  
	public void
	putInt(
		int	data )
	{
		if ( TRACE ){
			traceUsage("put(int)");
		}
		
		buffer.putInt( data );
	}
  
	public byte
	get()
	{
		if ( TRACE ){
			traceUsage("get");
		}
		
		return( buffer.get());
	}
	
	public byte
	get(
		int	x )
	{
		if ( TRACE ){
			traceUsage("get(int)");
		}
		
		return( buffer.get(x));
	}
  
	public void
	get(
		byte[]	data )
	{
		if ( TRACE ){
			traceUsage("get(byte[])");
		}
		
		buffer.get(data);
	}
  
	public int
	getInt()
	{
		if ( TRACE ){
			traceUsage("getInt");
		}
		
		return( buffer.getInt());
	}
  
	public int
	getInt(
		int		x )
	{
		if ( TRACE ){
			traceUsage("getInt(int)");
		}
		
		return( buffer.getInt(x));
	}
  
	public boolean
	hasRemaining()
	{
		if ( TRACE ){
			traceUsage("hasRemaining");
		}
		
		return( buffer.hasRemaining());
	}
  
	public int
	read(
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
	getBuffer()
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
			
			if ( DirectByteBufferPool.DEBUG ){
				
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
	
	
	protected class
	traceWrapper
	{
		ByteBuffer	trace_buffer;
		
		protected
		traceWrapper(
			ByteBuffer	_buffer )
		{
			trace_buffer	= _buffer;
		}
		
		public int
		hashCode()
		{
			return( trace_buffer.hashCode());	
		}
			
		public boolean
		equals(
			Object	other )
		{
			if ( other == null ){
				
				return( false);
			}
			
			return( trace_buffer == ((traceWrapper)other).trace_buffer );
		}
	}
}
