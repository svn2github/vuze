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
	
	public void
	ensureOpen()
	
		throws FMFileManagerException
	{
		try{
			this_mon.enter();
		
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
		}finally{
			
			this_mon.exit();
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
		
	public void
	setAccessMode(
		int		mode )
	
		throws FMFileManagerException
	{
		try{
			this_mon.enter();
		
			if ( mode != access_mode ){
		
				close(false);
			}
		
			access_mode		= mode;
			
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

			close(true);
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	close(
		boolean	explicit )
	
		throws FMFileManagerException
	{	
		try{
			this_mon.enter();
		
			boolean	was_open = raf != null;
		
			try{
				closeSupport( explicit );
				
			}finally{
	
				if ( was_open ){
					
					releaseSlot();
				}
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected boolean
	isOpen()
	{
		try{
			this_mon.enter();
		
			return( raf != null );
			
		}finally{
			
			this_mon.exit();
		}
	}
}
