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

/**
 * Virtual direct byte buffer given out and tracker
 * by the buffer pool.
 */
public class DirectByteBuffer {
	
  protected final ByteBuffer buffer;
  
  protected Reference ref;
  
  public 
  DirectByteBuffer( 
  	ByteBuffer _buffer ) 
  {
  	buffer = _buffer;
  }
  
  public int
  limit()
  {
  	return( buffer.limit());
  }
  
  public void
  limit(
  	int	l )
  {
  	buffer.limit(l);
  }
  
  public int
  position()
  {
  	return( buffer.position());
  }
  
  public void
  position(
  	int	l )
  {
  	buffer.position(l);
  }
  
  public void
  clear()
  {
  	buffer.clear();
  }
  
  public void
  flip()
  {
  	buffer.flip();
  }
  
  public int
  remaining()
  {
  	return( buffer.remaining());
  }
  
  public int
  capacity()
  {
  	return( buffer.capacity());
  }
  
  public void
  put(
  	byte[]	data )
  {
  	buffer.put( data );
  }
  
  public void
  put(
  	DirectByteBuffer	data )
  {
  	buffer.put( data.buffer );
  }
  
  public void
  put(
  	ByteBuffer	data )
  {
  	buffer.put( data );
  }
  
  public void
  put(
  	byte	data )
  {
  	buffer.put( data );
  }
  
  public void
  putInt(
  	int	data )
  {
  	buffer.putInt( data );
  }
  
  public byte
  get()
  {
  	return( buffer.get());
  }
  public byte
  get(
  	int	x )
  {
  	return( buffer.get(x));
  }
  
  public void
  get(
  	byte[]	data )
  {
  	buffer.get(data);
  }
  
  public int
  getInt()
  {
  	return( buffer.getInt());
  }
  
  public int
  getInt(
  	int		x )
  {
  	return( buffer.getInt(x));
  }
  
  public boolean
  hasRemaining()
  {
  	return( buffer.hasRemaining());
  }
  
  public int
  read(
  	FileChannel	chan )
  
  	throws IOException
  {
  	return( chan.read(buffer ));
  }
  
  public int
  write(
  	FileChannel	chan )
  
  	throws IOException
  {
  	return( chan.write(buffer ));
  }
  
  public int
  read(
  	SocketChannel	chan )
  
  	throws IOException
  {
  	return( chan.read(buffer ));
  }
  
  public int
  write(
  	SocketChannel	chan )
  
  	throws IOException
  {
  	return( chan.write(buffer ));
  }
  
  public ByteBuffer
  getBuffer()
  {
  	return( buffer );
  }
  
  public void 
  returnToPool() 
  {
    if ( ref != null ){
    	
      DirectByteBufferPool.registerReturn( ref );
      
      ref.enqueue();
    }
  }
}
