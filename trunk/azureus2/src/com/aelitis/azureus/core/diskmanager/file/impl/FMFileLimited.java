/*
 * File    : FMFileManagerLimited.java
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

//import java.nio.ByteBuffer;
import java.io.File;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.diskmanager.file.*;

public class 
FMFileLimited
	extends FMFileImpl
{
	protected FMFileManagerImpl		manager;
	
	protected
	FMFileLimited(
		FMFileOwner			_owner,
		FMFileManagerImpl	_manager,
		File				_file )
	
		throws FMFileManagerException
	{
		super( _owner, _file );
		
		manager = _manager;
	}
	
	public synchronized void
	ensureOpen()
	
		throws FMFileManagerException
	{
		if ( raf != null ){
			
			usedSlot();
			
		}else{
		
			getSlot();
		
			try{

			  super.ensureOpen();
				
			}finally{
				
				if ( raf == null ){
					
					releaseSlot();
				}
			}
		}
	}
	
	protected void
	getSlot()
	{
		manager.getSlot(this);
	}
	
	protected void
	releaseSlot()
	{
		manager.releaseSlot(this);
	}
	
	protected void
	usedSlot()
	{	
		manager.usedSlot(this);
	}
		
	public synchronized void
	setAccessMode(
		int		mode )
	
		throws FMFileManagerException
	{
		if ( mode != access_mode ){
		
			close(false);
		}
		
		access_mode		= mode;
	}
	
	public synchronized long
	getSize()
	
		throws FMFileManagerException
	{
		ensureOpen();
		
		return( getSizeSupport());
	}
	
	public synchronized long
	getLength()
	
		throws FMFileManagerException
	{
		ensureOpen();
		
		return( getLengthSupport());
	}

	public synchronized void
	setLength(
		long		length )
	
		throws FMFileManagerException
	{
		ensureOpen();
			
		setLengthSupport( length );
	}
	
	public synchronized void
	read(
		DirectByteBuffer	buffer,
		long		offset )
	
		throws FMFileManagerException
	{
		ensureOpen();
			
		readSupport( buffer, offset );
	}
	
	
	public synchronized void
	write(
		DirectByteBuffer	buffer,
		long		position )
	
		throws FMFileManagerException
	{
		ensureOpen();
			
		writeSupport( buffer, position );
	}
	
	public synchronized void
	write(
		DirectByteBuffer[]	buffers,
		long				position )
	
		throws FMFileManagerException
	{
		ensureOpen();
			
		writeSupport( buffers, position );
	}
	
	public synchronized void
	close()
	
		throws FMFileManagerException
	{
		close(true);
	}
	
	protected synchronized void
	close(
		boolean	explicit )
	
		throws FMFileManagerException
	{	
		boolean	was_open = raf != null;
		
		try{
			closeSupport( explicit );
			
		}finally{

			if ( was_open ){
				
				releaseSlot();
			}
		}
	}
	protected synchronized boolean
	isOpen()
	{
		return( raf != null );
	}
}
