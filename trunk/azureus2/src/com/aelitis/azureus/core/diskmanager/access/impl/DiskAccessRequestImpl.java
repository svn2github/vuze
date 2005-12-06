/*
 * Created on 02-Dec-2005
 * Created by Paul Gardner
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 40,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.diskmanager.access.impl;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.diskmanager.access.DiskAccessRequest;
import com.aelitis.azureus.core.diskmanager.access.DiskAccessRequestListener;
import com.aelitis.azureus.core.diskmanager.cache.CacheFile;

public class 
DiskAccessRequestImpl
	extends AERunnable
	implements DiskAccessRequest
{
	protected static final int	OP_READ				= 1;
	protected static final int	OP_WRITE			= 2;
	protected static final int	OP_WRITE_AND_FREE	= 3;
	protected static final int	OP_READ_AND_FLUSH	= 4;
	
	
	private CacheFile					file;
	private long						offset;
	private DirectByteBuffer			buffer;
	private DiskAccessRequestListener	listener;
	private int							op;
	
	private int							size;
	
	private volatile boolean	cancelled;
	
	protected
	DiskAccessRequestImpl(
		CacheFile					_file,
		long						_offset,
		DirectByteBuffer			_buffer,
		DiskAccessRequestListener	_listener,
		int							_op )
	{
		file		= _file;
		offset		= _offset;
		buffer		= _buffer;
		listener	= _listener;
		op			= _op;
		
		size = buffer.remaining( DirectByteBuffer.SS_FILE );
	}
	
	protected int
	getSize()
	{
		return( size );
	}
	
	public void
	runSupport()
	{
		if ( cancelled ){
			
			listener.requestCancelled( this );
			
			return;
		}
		
		//System.out.println( "DiskReq:" + Thread.currentThread().getName() + ": " + op + " - " + offset );
		
		try{
			if ( op == OP_READ ){
				
				file.read( buffer, offset );
				
			}else if ( op == OP_READ_AND_FLUSH ){
				
				file.readAndFlush( buffer, offset );
				
			}else if ( op == OP_WRITE ){
				
				file.write( buffer, offset );
				
			}else{
				
				file.writeAndHandoverBuffer( buffer, offset );
			}
			
			listener.requestComplete( this );
			
		}catch( Throwable e ){
			
			listener.requestFailed( this, e );
		}
	}
	
	public CacheFile
	getFile()
	{
		return( file );
	}
	
	public long
	getOffset()
	{
		return( offset );
	}
	
	public DirectByteBuffer
	getBuffer()
	{
		return( buffer );
	}
	
	public void
	cancel()
	{
		cancelled	= true;
	}
}
