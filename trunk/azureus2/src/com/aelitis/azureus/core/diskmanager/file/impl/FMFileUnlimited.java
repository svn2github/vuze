/*
 * File    : FMFileUnlimited.java
 * Created : 12-Feb-2004
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.aelitis.azureus.core.diskmanager.file.impl;

/**
 * @author parg
 *
 */

import java.io.File;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.diskmanager.file.*;

public class 
FMFileUnlimited
	extends FMFileImpl
{
	protected
	FMFileUnlimited(
		FMFileOwner	_owner,
		File		_file )
	
		throws FMFileManagerException
	{
		super( _owner, _file );
	}
	
	
	public void
	setAccessMode(
		int		mode )
	
		throws FMFileManagerException
	{
		try{
			this_mon.enter();
		
			if ( mode == access_mode && raf != null ){
				
				return;
			}
			
			access_mode		= mode;
			
			if ( raf != null ){
				
				closeSupport( false );
			}
			
			openSupport();
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public long
	getSize()
	
		throws FMFileManagerException
	{
		try{
			this_mon.enter();
		
			ensureOpen();
		
			return( getSizeSupport());
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public long
	getLength()
	
		throws FMFileManagerException
	{
		try{
			this_mon.enter();
		
			ensureOpen();
		
			return( getLengthSupport());
			
		}finally{
			
			this_mon.exit();
		}
	}

	public void
	setLength(
		long		length )
	
		throws FMFileManagerException
	{
		try{
			this_mon.enter();

			ensureOpen();
		
			setLengthSupport( length );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	read(
		DirectByteBuffer	buffer,
		long		offset )
	
		throws FMFileManagerException
	{
		try{
			this_mon.enter();

			ensureOpen();
		
			readSupport( buffer, offset );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	
	public void
	write(
		DirectByteBuffer	buffer,
		long		position )
	
		throws FMFileManagerException
	{
		try{
			this_mon.enter();

			ensureOpen();
		
			writeSupport( buffer, position );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	write(
		DirectByteBuffer[]	buffers,
		long				position )
	
		throws FMFileManagerException
	{
		try{
			this_mon.enter();

			ensureOpen();
		
			writeSupport( buffers, position );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	close()
	
		throws FMFileManagerException
	{
		try{
			this_mon.enter();

			closeSupport( true );
			
		}finally{
			
			this_mon.exit();
		}
	}
}
