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

package org.gudy.azureus2.core3.disk.file.impl;

/**
 * @author parg
 *
 */

import java.nio.ByteBuffer;

import org.gudy.azureus2.core3.disk.file.*;

public class 
FMFileLimited
	extends FMFileImpl
{
	protected FMFileManager		manager;
	
	protected
	FMFileLimited(
		FMFileManager	_manager )
	{
		manager = _manager;
	}
	
	protected void
	ensureOpen()
	
		throws FMFileManagerException
	{
		if ( raf != null ){
			
			usedSlot();
			
		}else{
		
			getSlot();
		
			try{
				openSupport();
				
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
		//System.out.println( "FMFileLimited::getSlot: " + file.toString());
	}
	
	protected void
	releaseSlot()
	{
		//System.out.println( "FMFileLimited::releaseSlot: " + file.toString());
	}
	
	protected void
	usedSlot()
	{	
		//System.out.println( "FMFileLimited::usedSlot: " + file.toString());
	}
	
	public synchronized void
	setAccessMode(
		int		mode )
	
		throws FMFileManagerException
	{
		if ( mode != access_mode ){
		
			close();
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
		ByteBuffer	buffer,
		long		offset )
	
		throws FMFileManagerException
	{
		ensureOpen();
			
		readSupport( buffer, offset );
	}
	
	
	public synchronized int
	write(
		ByteBuffer	buffer,
		long		position )
	
		throws FMFileManagerException
	{
		ensureOpen();
			
		return( writeSupport( buffer, position ));
	}
	
	public synchronized void
	close()
	
		throws FMFileManagerException
	{
		if ( raf != null ){
			
			try{
				closeSupport();
			
			}finally{
			
				releaseSlot();
			}
		}
	}
}
