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

package org.gudy.azureus2.core3.disk.file.impl;

/**
 * @author parg
 *
 */

import java.io.File;

import org.gudy.azureus2.core3.disk.file.*;
import org.gudy.azureus2.core3.util.DirectByteBuffer;

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
	
	
	public synchronized void
	setAccessMode(
		int		mode )
	
		throws FMFileManagerException
	{
		if ( mode == access_mode && raf != null ){
			
			return;
		}
		
		access_mode		= mode;
		
		if ( raf != null ){
			
			closeSupport( false );
		}
		
		openSupport();
	}
	
	public synchronized long
	getSize()
	
		throws FMFileManagerException
	{
		return( getSizeSupport());
	}
	
	public synchronized long
	getLength()
	
		throws FMFileManagerException
	{
		return( getLengthSupport());
	}

	public synchronized void
	setLength(
		long		length )
	
		throws FMFileManagerException
	{
		setLengthSupport( length );
	}
	
	public synchronized void
	read(
		DirectByteBuffer	buffer,
		long		offset )
	
		throws FMFileManagerException
	{
		readSupport( buffer, offset );
	}
	
	
	public synchronized int
	write(
		DirectByteBuffer	buffer,
		long		position )
	
		throws FMFileManagerException
	{
		return( writeSupport( buffer, position ));
	}
	
	public synchronized void
	close()
	
		throws FMFileManagerException
	{
		closeSupport( true );
	}
}
